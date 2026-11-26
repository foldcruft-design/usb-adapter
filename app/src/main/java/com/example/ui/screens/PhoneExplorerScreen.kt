package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.OtgUserPreferences
import com.example.model.ClipboardMode
import com.example.model.ClipboardState
import com.example.model.FileNode
import com.example.model.PhoneRootMode
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.viewmodel.BrowserPaneState

@Composable
fun PhoneExplorerScreen(
    paneState: BrowserPaneState,
    phoneRootMode: PhoneRootMode,
    preferences: OtgUserPreferences,
    clipboard: ClipboardState,
    showAddSheet: Boolean,
    onDismissAddSheet: () -> Unit,
    onSwitchPhoneMode: (PhoneRootMode) -> Unit,
    onPickPhoneSafFolder: () -> Unit,
    onNavigateUp: () -> Unit,
    onCrumbClick: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onSetSort: (SortField, SortOrder) -> Unit,
    onToggleHidden: () -> Unit,
    onRefreshFiles: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = phoneRootMode == PhoneRootMode.WORKSPACE,
                onClick = { onSwitchPhoneMode(PhoneRootMode.WORKSPACE) },
                label = { Text("OTG Буфер") },
                leadingIcon = {
                    Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("phone_workspace_chip")
            )
            FilterChip(
                selected = phoneRootMode == PhoneRootMode.SAF_FOLDER,
                onClick = { onSwitchPhoneMode(PhoneRootMode.SAF_FOLDER) },
                label = { Text("Папка телефона") },
                leadingIcon = {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("phone_saf_chip")
            )
            if (phoneRootMode == PhoneRootMode.SAF_FOLDER && paneState.rootTreeUri != null) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onPickPhoneSafFolder,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Сменить")
                }
            }
        }

        if (phoneRootMode == PhoneRootMode.SAF_FOLDER && paneState.rootTreeUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Выберите папку телефона",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Подключите «Загрузки», «Документы» или любую папку на телефоне для прямого переноса файлов на USB-флешку.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onPickPhoneSafFolder,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("pick_phone_folder_button")
                    ) {
                        Text("Выбрать папку")
                    }
                }
            }
        } else {
            ExplorerBrowserPane(
                paneState = paneState,
                preferences = preferences,
                clipboard = clipboard,
                isUsb = false,
                statusBadge = if (phoneRootMode == PhoneRootMode.WORKSPACE) "Встроенный буфер" else "Папка телефона",
                onNavigateUp = onNavigateUp,
                onCrumbClick = onCrumbClick,
                onSearchChange = onSearchChange,
                onToggleViewMode = onToggleViewMode,
                onSetSort = onSetSort,
                onToggleHidden = onToggleHidden,
                onRefresh = onRefreshFiles,
                onOpenNode = onOpenNode,
                onToggleSelect = onToggleSelect,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                onCopyToClipboard = onCopyToClipboard,
                onPasteClipboard = onPasteClipboard,
                onClearClipboard = onClearClipboard,
                onCreateFolder = onCreateFolder,
                onCreateTextFile = onCreateTextFile,
                onRenameNode = onRenameNode,
                onDeleteNodes = onDeleteNodes,
                onOpenExternal = onOpenExternal,
                onShareNodes = onShareNodes,
                onPickPhotosAndVideos = onPickPhotosAndVideos,
                onPickDocuments = onPickDocuments,
                showAddSheet = showAddSheet,
                onDismissAddSheet = onDismissAddSheet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
