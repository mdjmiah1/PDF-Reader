package com.example.ui.reader

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.DocumentEntity
import com.example.data.repository.PdfRepository
import com.example.pdf.AnnotationParser
import com.example.pdf.AnnotationTool
import com.example.pdf.NormalizedPoint
import com.example.pdf.PdfRendererEngine
import com.example.pdf.PdfSearchEngine
import com.example.pdf.SearchMatch
import com.example.pdf.UiAnnotation
import com.example.ui.theme.AnnotationIndigo
import com.example.ui.theme.AnnotationYellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val uriString: String = "",
    val document: DocumentEntity? = null,
    val pageCount: Int = 0,
    val currentPage: Int = 1,
    val zoomLevel: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val isFullscreen: Boolean = false,
    val activeTool: AnnotationTool = AnnotationTool.NONE,
    val activeColor: Color = AnnotationIndigo,
    val strokeWidth: Float = 4f,
    val isCurrentPageBookmarked: Boolean = false,
    val bookmarksForDoc: List<BookmarkEntity> = emptyList(),
    val pageAnnotations: List<UiAnnotation> = emptyList(),
    val isSearchAvailable: Boolean = false,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchMatch> = emptyList(),
    val currentMatchIndex: Int = 0,
    val isJumpDialogVisible: Boolean = false,
    val isInfoDialogVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application)
    val rendererEngine = PdfRendererEngine(application)
    private val searchEngine = PdfSearchEngine()

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // Collectors are tracked so a page turn or a new document replaces them
    // instead of piling up new ones.
    private var annotationsJob: Job? = null
    private var bookmarksJob: Job? = null

    // Page requested by the caller (e.g. a tapped bookmark) for the next load of a document.
    private var pendingStart: Pair<String, Int>? = null

    fun requestStartPage(uriString: String, page: Int) {
        pendingStart = uriString to page
    }

    fun loadDocument(uriString: String) {
        viewModelScope.launch {
            annotationsJob?.cancel()
            bookmarksJob?.cancel()

            val isNewDocument = _uiState.value.uriString != uriString
            _uiState.update {
                if (isNewDocument) {
                    // Don't carry zoom, rotation, search or annotations over from another document.
                    ReaderUiState(uriString = uriString, isLoading = true)
                } else {
                    it.copy(isLoading = true, errorMessage = null)
                }
            }

            val doc = repository.getDocumentSync(uriString)
            val openResult = rendererEngine.open(uriString)
            val requestedPage = pendingStart?.takeIf { it.first == uriString }?.second
            pendingStart = null

            openResult.onSuccess { count ->
                val startPage = (requestedPage ?: doc?.lastReadPage ?: 1)
                    .coerceIn(1, count.coerceAtLeast(1))

                // Only the generated sample PDFs have an index; real PDFs have no text index yet.
                val searchAvailable = doc?.isSample == true
                if (searchAvailable) {
                    searchEngine.indexSampleDocument(doc?.title ?: "Document", count)
                } else {
                    searchEngine.clear()
                }

                _uiState.update {
                    it.copy(
                        document = doc,
                        pageCount = count,
                        currentPage = startPage,
                        isSearchAvailable = searchAvailable,
                        isLoading = false
                    )
                }

                repository.recordDocumentOpened(uriString, startPage)
                observeBookmarks(uriString)
                observePageAnnotations(uriString, startPage)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Cannot open PDF. The file may be corrupted or permission was revoked."
                    )
                }
            }
        }
    }

    fun importAndOpen(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.importDocumentFromUri(uri)
            result.onSuccess { doc ->
                loadDocument(doc.uriString)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Cannot open document: ${error.localizedMessage ?: "Invalid PDF"}"
                    )
                }
            }
        }
    }

    private fun observeBookmarks(uri: String) {
        bookmarksJob?.cancel()
        bookmarksJob = viewModelScope.launch {
            repository.getBookmarksForDocument(uri).collect { bookmarks ->
                _uiState.update { state ->
                    state.copy(
                        bookmarksForDoc = bookmarks,
                        isCurrentPageBookmarked = bookmarks.any { it.pageNumber == state.currentPage }
                    )
                }
            }
        }
    }

    private fun observePageAnnotations(uri: String, page: Int) {
        annotationsJob?.cancel()
        annotationsJob = viewModelScope.launch {
            repository.getAnnotationsForPage(uri, page).collect { entities ->
                val uiAnnotations = entities.map { AnnotationParser.toUiAnnotation(it) }
                _uiState.update { it.copy(pageAnnotations = uiAnnotations) }
            }
        }
    }

    fun onPageChanged(page: Int) {
        val state = _uiState.value
        val target = page.coerceIn(1, state.pageCount.coerceAtLeast(1))
        if (target != state.currentPage) {
            _uiState.update {
                it.copy(
                    currentPage = target,
                    // Clear immediately so the previous page's strokes never show on the new page.
                    pageAnnotations = emptyList(),
                    isCurrentPageBookmarked = it.bookmarksForDoc.any { b -> b.pageNumber == target }
                )
            }
            val uri = state.uriString
            observePageAnnotations(uri, target)
            viewModelScope.launch {
                repository.updateReadingProgress(uri, target)
            }
        }
    }

    fun setZoom(level: Float) {
        _uiState.update { it.copy(zoomLevel = level.coerceIn(0.5f, 3.0f)) }
    }

    fun zoomIn() {
        val current = _uiState.value.zoomLevel
        setZoom(current + 0.25f)
    }

    fun zoomOut() {
        val current = _uiState.value.zoomLevel
        setZoom(current - 0.25f)
    }

    fun resetZoom() {
        setZoom(1.0f)
    }

    fun rotate() {
        _uiState.update {
            it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360)
        }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun selectTool(tool: AnnotationTool) {
        _uiState.update {
            val nextTool = if (it.activeTool == tool) AnnotationTool.NONE else tool
            val defaultColor = if (nextTool == AnnotationTool.HIGHLIGHTER) AnnotationYellow else AnnotationIndigo
            val defaultWidth = if (nextTool == AnnotationTool.HIGHLIGHTER) 24f else 4f
            it.copy(
                activeTool = nextTool,
                activeColor = defaultColor,
                strokeWidth = defaultWidth
            )
        }
    }

    fun selectColor(color: Color) {
        _uiState.update { it.copy(activeColor = color) }
    }

    fun addStrokeAnnotation(points: List<NormalizedPoint>) {
        val state = _uiState.value
        if (points.isEmpty() || state.activeTool == AnnotationTool.NONE) return

        val hex = String.format("#%06X", 0xFFFFFF and state.activeColor.hashCode())
        val typeStr = when (state.activeTool) {
            AnnotationTool.HIGHLIGHTER -> "HIGHLIGHTER"
            AnnotationTool.NOTE -> "NOTE"
            else -> "PEN"
        }

        val entity = AnnotationEntity(
            documentUri = state.uriString,
            pageNumber = state.currentPage,
            type = typeStr,
            colorHex = hex,
            strokeWidth = state.strokeWidth,
            pointsJson = AnnotationParser.serializePoints(points),
            textNote = null
        )

        viewModelScope.launch {
            repository.addAnnotation(entity)
        }
    }

    fun addNoteAnnotation(point: NormalizedPoint, noteText: String) {
        val state = _uiState.value
        val hex = String.format("#%06X", 0xFFFFFF and AnnotationYellow.hashCode())

        val entity = AnnotationEntity(
            documentUri = state.uriString,
            pageNumber = state.currentPage,
            type = "NOTE",
            colorHex = hex,
            strokeWidth = 2f,
            pointsJson = "${point.x},${point.y}",
            textNote = noteText
        )

        viewModelScope.launch {
            repository.addAnnotation(entity)
        }
    }

    fun undoAnnotation() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.undoLastAnnotation(state.uriString, state.currentPage)
        }
    }

    fun clearPageAnnotations() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.clearAnnotationsForPage(state.uriString, state.currentPage)
        }
    }

    fun toggleBookmark() {
        val state = _uiState.value
        viewModelScope.launch {
            val isBookmarked = repository.toggleBookmark(
                state.uriString,
                state.currentPage,
                title = "Page ${state.currentPage} of ${state.document?.title ?: "Document"}"
            )
            _uiState.update { it.copy(isCurrentPageBookmarked = isBookmarked) }
        }
    }

    fun toggleSearch() {
        _uiState.update {
            val willOpen = !it.isSearchOpen
            it.copy(
                isSearchOpen = willOpen,
                searchResults = if (!willOpen) emptyList() else it.searchResults
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        val results = searchEngine.search(query)
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = results,
                currentMatchIndex = 0
            )
        }
        if (results.isNotEmpty()) {
            onPageChanged(results[0].pageNumber)
        }
    }

    fun nextSearchMatch() {
        val state = _uiState.value
        if (state.searchResults.isEmpty()) return
        val nextIndex = (state.currentMatchIndex + 1) % state.searchResults.size
        _uiState.update { it.copy(currentMatchIndex = nextIndex) }
        onPageChanged(state.searchResults[nextIndex].pageNumber)
    }

    fun prevSearchMatch() {
        val state = _uiState.value
        if (state.searchResults.isEmpty()) return
        val prevIndex = if (state.currentMatchIndex - 1 < 0) state.searchResults.size - 1 else state.currentMatchIndex - 1
        _uiState.update { it.copy(currentMatchIndex = prevIndex) }
        onPageChanged(state.searchResults[prevIndex].pageNumber)
    }

    fun setJumpDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isJumpDialogVisible = visible) }
    }

    fun setInfoDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isInfoDialogVisible = visible) }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled here, so launching in it would never run.
        val engine = rendererEngine
        CoroutineScope(Dispatchers.IO).launch {
            engine.close()
        }
    }
}
