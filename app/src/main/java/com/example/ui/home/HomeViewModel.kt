package com.example.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DocumentEntity
import com.example.data.repository.PdfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeTab {
    ALL,
    RECENT,
    STARRED,
    SAMPLES
}

data class HomeUiState(
    val lastOpenedDoc: DocumentEntity? = null,
    val selectedTab: HomeTab = HomeTab.ALL,
    val searchQuery: String = "",
    val allDocuments: List<DocumentEntity> = emptyList(),
    val displayedDocuments: List<DocumentEntity> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application)

    private val _selectedTab = MutableStateFlow(HomeTab.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    private data class DocsFilterData(
        val allDocs: List<DocumentEntity>,
        val lastDoc: DocumentEntity?,
        val tab: HomeTab,
        val query: String,
        val filtered: List<DocumentEntity>
    )

    private val _docsFilter = combine(
        repository.allDocuments,
        repository.lastOpenedDocument,
        _selectedTab,
        _searchQuery
    ) { allDocs, lastDoc, tab, query ->
        val filteredByTab = when (tab) {
            HomeTab.ALL -> allDocs
            HomeTab.RECENT -> allDocs.sortedByDescending { it.lastOpenedTimestamp }
            HomeTab.STARRED -> allDocs.filter { it.isFavorite }
            HomeTab.SAMPLES -> allDocs.filter { it.isSample }
        }

        val filteredBySearch = if (query.isBlank()) {
            filteredByTab
        } else {
            filteredByTab.filter { it.title.contains(query, ignoreCase = true) }
        }

        DocsFilterData(allDocs, lastDoc, tab, query, filteredBySearch)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _docsFilter,
        _isLoading,
        _message
    ) { filterData, loading, msg ->
        HomeUiState(
            lastOpenedDoc = filterData.lastDoc,
            selectedTab = filterData.tab,
            searchQuery = filterData.query,
            allDocuments = filterData.allDocs,
            displayedDocuments = filterData.filtered,
            isLoading = loading,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            _isLoading.value = true
            repository.initializeSampleDocumentsIfEmpty()
            _isLoading.value = false
        }
    }

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(doc.uriString, !doc.isFavorite)
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc.uriString)
            _message.value = "Removed ${doc.title}"
        }
    }

    fun importDocument(uri: Uri, onNavigateToReader: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.importDocumentFromUri(uri)
            _isLoading.value = false
            result.onSuccess { doc ->
                _message.value = "Imported ${doc.title}"
                onNavigateToReader(doc.uriString)
            }.onFailure { err ->
                _message.value = "Failed to import: ${err.localizedMessage ?: "Invalid PDF"}"
            }
        }
    }

    fun importDirectory(treeUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val count = repository.importAllPdfsFromTree(treeUri)
            _isLoading.value = false
            _message.value = "Imported $count PDF files from folder"
        }
    }

    fun scanEntirePhone() {
        viewModelScope.launch {
            _isLoading.value = true
            val count = repository.scanDeviceForPdfs()
            _isLoading.value = false
            _message.value = if (count > 0) "Found and added $count new PDF files" else "Scanned storage. No new PDF files found."
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
