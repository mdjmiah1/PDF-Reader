package com.example.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentEntity
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.home.DocumentItemRow
import com.example.ui.home.HomeViewModel

enum class SortOption {
    RECENT,
    NAME,
    SIZE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    homeViewModel: HomeViewModel,
    onNavigateToReader: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    var sortOption by remember { mutableStateOf(SortOption.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedDocForInfo by remember { mutableStateOf<DocumentEntity?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }

    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { homeViewModel.importDocument(it, onNavigateToReader) }
    }

    BackHandler {
        onNavigateBack()
    }

    val sortedList = remember(uiState.allDocuments, uiState.searchQuery, sortOption) {
        val filtered = if (uiState.searchQuery.isBlank()) {
            uiState.allDocuments
        } else {
            uiState.allDocuments.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
        }

        when (sortOption) {
            SortOption.RECENT -> filtered.sortedByDescending { it.lastOpenedTimestamp }
            SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
            SortOption.SIZE -> filtered.sortedByDescending { it.fileSizeBytes }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Document Library",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { searchExpanded = !searchExpanded },
                        modifier = Modifier.testTag("library_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search documents"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("library_sort_button")
                        ) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Recently Opened") },
                                onClick = {
                                    sortOption = SortOption.RECENT
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Filename (A-Z)") },
                                onClick = {
                                    sortOption = SortOption.NAME
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by File Size") },
                                onClick = {
                                    sortOption = SortOption.SIZE
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openPdfLauncher.launch(arrayOf("application/pdf")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_library_open_pdf")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Document")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = 0.dp)
        ) {
            if (searchExpanded) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { homeViewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("library_search_field"),
                    placeholder = { Text("Search by title...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { homeViewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (sortedList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    title = "No Documents in Library",
                    description = "Open any PDF document using the plus button below to start reading and annotating.",
                    actionLabel = "Open PDF",
                    onActionClick = { openPdfLauncher.launch(arrayOf("application/pdf")) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedList, key = { it.uriString }) { doc ->
                        DocumentItemRow(
                            document = doc,
                            onClick = { onNavigateToReader(doc.uriString) },
                            onToggleFavorite = { homeViewModel.toggleFavorite(doc) },
                            onDelete = { homeViewModel.deleteDocument(doc) },
                            onShowInfo = { selectedDocForInfo = doc }
                        )
                    }
                }
            }
        }
    }

    selectedDocForInfo?.let { doc ->
        DocumentInfoDialog(
            document = doc,
            onDismiss = { selectedDocForInfo = null }
        )
    }
}
