package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.OtgUserPreferences
import com.example.model.ClipboardMode
import com.example.model.ClipboardState
import com.example.model.FileNode
import com.example.model.ViewMode
import com.example.ui.components.BreadcrumbStrip
import com.example.ui.components.ClipboardFloatingBanner
import com.example.ui.components.ExplorerToolbar
import com.example.ui.components.FileGridItem
import com.example.ui.components.FileRowItem
import com.example.ui.components.MultiSelectActionBar
import com.example.ui.components.StorageStatusHeaderCard
import com.example.viewmodel.BrowserPaneState

@Composable
fun ExplorerBrowserPane(
    paneState: BrowserPaneState,
    preferences: OtgUserPreferences,
    clipboard: ClipboardState,
    isUsb: Boolean,
    statusBadge: String,
    onNavigateUp: () -> Unit,
    onCrumbClick: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onSetSort: (com.example.model.SortField, com.example.model.SortOrder) -> Unit,
    onToggleHidden: () -> Unit,
    onRefresh: () -> Unit,
    onOpenNode: (FileNode) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCopyToClipboard: (ClipboardMode, FileNode?) -> Unit,
    onPasteClipboard: () -> Unit,
    onClearClipboard: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateTextFile: (String, String) -> Unit,
    onRenameNode: (FileNode, String) -> Unit,
    onDeleteNodes: (List<FileNode>?) -> Unit,
    onOpenExternal: (FileNode) -> Unit,
    onShareNodes: (List<FileNode>) -> Unit,
    onPickPhotosAndVideos: () -> Unit,
    onPickDocuments: () -> Unit,
    showAddSheet: Boolean,
    onDismissAddSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = paneState.isSelectionMode || paneState.canNavigateUp) {
        if (paneState.isSelectionMode) {
            onClearSelection()
        } else {
            onNavigateUp()
        }
    }

    var activeActionNode by remember { mutableStateOf<FileNode?>(null) }
    var renameTarget by remember { mutableStateOf<FileNode?>(null) }
    var propertiesTarget by remember { mutableStateOf<FileNode?>(null) }
    var deleteConfirmTargets by remember { mutableStateOf<List<FileNode>?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateTextDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StorageStatusHeaderCard(
            title = paneState.volumeLabel.ifBlank { if (isUsb) "USB Накопитель" else "Папка телефона" },
            subtitleBadge = statusBadge,
            isUsb = isUsb,
            itemCount = paneState.files.size,
            totalBytes = paneState.totalBytes,
            freeBytes = paneState.freeBytes
        )

        if (paneState.breadcrumbs.isNotEmpty()) {
            BreadcrumbStrip(
                breadcrumbs = paneState.breadcrumbs,
                onCrumbClick = onCrumbClick
            )
        }

        if (paneState.isSelectionMode) {
            MultiSelectActionBar(
                selectedCount = paneState.selectedIds.size,
                onSelectAll = onSelectAll,
                onCopy = { onCopyToClipboard(ClipboardMode.COPY, null) },
                onCut = { onCopyToClipboard(ClipboardMode.MOVE, null) },
                onShare = {
                    val selected = paneState.files.filter { it.id in paneState.selectedIds }
                    onShareNodes(selected)
                },
                onDelete = {
                    val selected = paneState.files.filter { it.id in paneState.selectedIds }
                    deleteConfirmTargets = selected
                },
                onClear = onClearSelection
            )
        } else {
            ExplorerToolbar(
                searchQuery = paneState.searchQuery,
                onSearchQueryChange = onSearchChange,
                viewMode = preferences.viewMode,
                showHidden = preferences.showHiddenFiles,
                onToggleViewMode = onToggleViewMode,
                onOpenSort = { showSortSheet = true },
                onToggleHidden = onToggleHidden,
                onRefresh = onRefresh
            )
        }

        ClipboardFloatingBanner(
            clipboard = clipboard,
            onPasteHere = onPasteClipboard,
            onClear = onClearClipboard
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val itemsToShow = paneState.visibleFiles
            when {
                paneState.isLoading && itemsToShow.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                paneState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = paneState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onRefresh) {
                            Text("Повторить")
                        }
                    }
                }
                itemsToShow.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (paneState.searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = if (paneState.searchQuery.isNotBlank()) "Ничего не найдено" else "Папка пуста",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (paneState.searchQuery.isNotBlank()) {
                                "Попробуйте изменить поисковый запрос"
                            } else {
                                "Нажмите «+ Добавить», чтобы перенести фото, документы или создать папку"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                preferences.viewMode == ViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(itemsToShow, key = { it.id }) { node ->
                            FileRowItem(
                                node = node,
                                isSelected = node.id in paneState.selectedIds,
                                selectionMode = paneState.isSelectionMode,
                                onClick = { onOpenNode(node) },
                                onLongClick = { onToggleSelect(node.id) },
                                onMoreClick = { activeActionNode = node }
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 148.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(itemsToShow, key = { it.id }) { node ->
                            FileGridItem(
                                node = node,
                                isSelected = node.id in paneState.selectedIds,
                                selectionMode = paneState.isSelectionMode,
                                onClick = { onOpenNode(node) },
                                onLongClick = { onToggleSelect(node.id) },
                                onMoreClick = { activeActionNode = node }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddActionBottomSheet(
            targetLabel = paneState.currentBreadcrumb?.name ?: paneState.volumeLabel,
            onPickPhotosAndVideos = onPickPhotosAndVideos,
            onPickDocuments = onPickDocuments,
            onCreateFolder = { showCreateFolderDialog = true },
            onCreateTextFile = { showCreateTextDialog = true },
            onDismiss = onDismissAddSheet
        )
    }

    activeActionNode?.let { node ->
        NodeActionSheet(
            node = node,
            onOpen = { onOpenNode(node) },
            onOpenExternal = { onOpenExternal(node) },
            onShare = { onShareNodes(listOf(node)) },
            onCopy = { onCopyToClipboard(ClipboardMode.COPY, node) },
            onCut = { onCopyToClipboard(ClipboardMode.MOVE, node) },
            onRename = { renameTarget = node },
            onProperties = { propertiesTarget = node },
            onDelete = { deleteConfirmTargets = listOf(node) },
            onDismiss = { activeActionNode = null }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { name ->
                showCreateFolderDialog = false
                onCreateFolder(name)
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showCreateTextDialog) {
        CreateTextFileDialog(
            onConfirm = { name, content ->
                showCreateTextDialog = false
                onCreateTextFile(name, content)
            },
            onDismiss = { showCreateTextDialog = false }
        )
    }

    renameTarget?.let { node ->
        RenameNodeDialog(
            node = node,
            onConfirm = { newName ->
                renameTarget = null
                onRenameNode(node, newName)
            },
            onDismiss = { renameTarget = null }
        )
    }

    propertiesTarget?.let { node ->
        PropertiesDialog(
            node = node,
            onDismiss = { propertiesTarget = null }
        )
    }

    deleteConfirmTargets?.let { targets ->
        DeleteConfirmDialog(
            nodes = targets,
            onConfirm = {
                deleteConfirmTargets = null
                onDeleteNodes(targets)
            },
            onDismiss = { deleteConfirmTargets = null }
        )
    }

    if (showSortSheet) {
        SortOptionsSheet(
            currentField = preferences.sortField,
            currentOrder = preferences.sortOrder,
            onSelect = onSetSort,
            onDismiss = { showSortSheet = false }
        )
    }
}
