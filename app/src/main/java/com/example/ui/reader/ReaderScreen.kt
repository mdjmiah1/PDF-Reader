package com.example.ui.reader

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentEntity
import com.example.pdf.AnnotationTool
import com.example.ui.components.DocumentInfoDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    documentUri: String,
    viewModel: ReaderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMoreMenu by remember { mutableStateOf(false) }

    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importAndOpen(it) }
    }

    LaunchedEffect(documentUri) {
        viewModel.loadDocument(documentUri)
    }

    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Content Area
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Opening document...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("reader_error_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Unable to Open PDF",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = uiState.errorMessage ?: "File could not be opened.",
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("error_back_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Back")
                                }

                                Button(
                                    onClick = { openPdfLauncher.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("error_reopen_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Select PDF")
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Main PDF View Canvas with drag page navigation
                var dragDistance by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            orientation = Orientation.Vertical,
                            enabled = uiState.activeTool == AnnotationTool.NONE,
                            state = rememberDraggableState { delta ->
                                dragDistance += delta
                            },
                            onDragStopped = {
                                if (dragDistance < -60f && uiState.currentPage < uiState.pageCount) {
                                    viewModel.onPageChanged(uiState.currentPage + 1)
                                } else if (dragDistance > 60f && uiState.currentPage > 1) {
                                    viewModel.onPageChanged(uiState.currentPage - 1)
                                }
                                dragDistance = 0f
                            }
                        )
                ) {
                    PdfPageView(
                        rendererEngine = viewModel.rendererEngine,
                        pageNumber = uiState.currentPage,
                        zoomLevel = uiState.zoomLevel,
                        rotationDegrees = uiState.rotationDegrees,
                        activeTool = uiState.activeTool,
                        activeColor = uiState.activeColor,
                        strokeWidth = uiState.strokeWidth,
                        annotations = uiState.pageAnnotations,
                        onToggleFullscreen = { viewModel.toggleFullscreen() },
                        onStrokeFinished = { points ->
                            viewModel.addStrokeAnnotation(points)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Floating Page Indicator Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = if (!uiState.isFullscreen) 54.dp else 12.dp)
                ) {
                    PageIndicatorPill(
                        currentPage = uiState.currentPage,
                        totalPages = uiState.pageCount,
                        onClick = { viewModel.setJumpDialogVisible(true) }
                    )
                }

                // Floating Bottom Reading Toolbar
                AnimatedVisibility(
                    visible = !uiState.isFullscreen,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp)
                ) {
                    ReadingBottomToolbar(
                        activeTool = uiState.activeTool,
                        activeColor = uiState.activeColor,
                        zoomLevel = uiState.zoomLevel,
                        onToolSelected = { tool -> viewModel.selectTool(tool) },
                        onColorSelected = { color -> viewModel.selectColor(color) },
                        onUndoAnnotation = { viewModel.undoAnnotation() },
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() },
                        onFitWidth = { viewModel.resetZoom() },
                        onRotate = { viewModel.rotate() }
                    )
                }
            }
        }

        // Top Bar - Always available unless in fullscreen reading
        AnimatedVisibility(
            visible = !uiState.isFullscreen,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                shadowElevation = 2.dp
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = uiState.document?.title ?: "PDF Reader",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            if (uiState.errorMessage == null && !uiState.isLoading) {
                                IconButton(
                                    onClick = { viewModel.toggleSearch() },
                                    modifier = Modifier.testTag("reader_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.toggleBookmark() },
                                    modifier = Modifier.testTag("reader_bookmark_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isCurrentPageBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Bookmark Page",
                                        tint = if (uiState.isCurrentPageBookmarked) Color(0xFF4F46E5) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showMoreMenu = true },
                                        modifier = Modifier.testTag("reader_more_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMoreMenu,
                                        onDismissRequest = { showMoreMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Document Info") },
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                            onClick = {
                                                showMoreMenu = false
                                                viewModel.setInfoDialogVisible(true)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rotate Page") },
                                            leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null) },
                                            onClick = {
                                                showMoreMenu = false
                                                viewModel.rotate()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share PDF") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                showMoreMenu = false
                                                uiState.document?.let { doc -> sharePdfFile(context, doc) }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Clear Annotations on Page") },
                                            onClick = {
                                                showMoreMenu = false
                                                viewModel.clearPageAnnotations()
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )

                    // In-Document Search Drawer
                    if (uiState.isSearchOpen && uiState.errorMessage == null) {
                        SearchDrawer(
                            searchQuery = uiState.searchQuery,
                            searchResults = uiState.searchResults,
                            currentMatchIndex = uiState.currentMatchIndex,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onNextMatch = { viewModel.nextSearchMatch() },
                            onPrevMatch = { viewModel.prevSearchMatch() },
                            onSelectMatch = { page -> viewModel.onPageChanged(page) },
                            onClose = { viewModel.toggleSearch() }
                        )
                    }
                }
            }
        }

        // Jump To Page Dialog
        if (uiState.isJumpDialogVisible) {
            JumpToPageDialog(
                currentPage = uiState.currentPage,
                totalPages = uiState.pageCount,
                onJump = { target ->
                    viewModel.onPageChanged(target)
                    viewModel.setJumpDialogVisible(false)
                },
                onDismiss = { viewModel.setJumpDialogVisible(false) }
            )
        }

        // Document Info Dialog
        if (uiState.isInfoDialogVisible && uiState.document != null) {
            DocumentInfoDialog(
                document = uiState.document!!,
                onDismiss = { viewModel.setInfoDialogVisible(false) }
            )
        }
    }
}

private fun sharePdfFile(context: Context, document: DocumentEntity) {
    try {
        val uri = if (document.uriString.startsWith("content://")) {
            Uri.parse(document.uriString)
        } else {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(document.uriString)
            )
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${document.title}"))
    } catch (_: Exception) {}
}
