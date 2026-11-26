package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.OtgUserPreferences
import com.example.model.ClipboardMode
import com.example.model.ClipboardState
import com.example.model.FileNode
import com.example.model.PhoneRootMode
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.ui.components.LocalGlassBackdrop
import com.example.ui.components.liquidGlass
import com.example.ui.theme.GraphitePrimary
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkSlateGray
import com.example.viewmodel.BrowserPaneState

@Composable
fun PhoneExplorerScreen(
    paneState: BrowserPaneState,
    phoneRootMode: PhoneRootMode,
    isUsbConnected: Boolean,
    preferences: OtgUserPreferences,
    clipboard: ClipboardState,
    showAddSheet: Boolean,
    onDismissAddSheet: () -> Unit,
    onSwitchPhoneMode: (PhoneRootMode) -> Unit,
    onPickPhoneSafFolder: () -> Unit,
    onQuickSendToUsb: (List<FileNode>?) -> Unit,
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
    val backdrop = LocalGlassBackdrop.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassModePill(
                icon = Icons.Default.Smartphone,
                label = "OTG Буфер",
                selected = phoneRootMode == PhoneRootMode.WORKSPACE,
                onClick = { onSwitchPhoneMode(PhoneRootMode.WORKSPACE) },
                backdrop = backdrop,
                modifier = Modifier
                    .weight(1f)
                    .testTag("phone_workspace_chip")
            )
            GlassModePill(
                icon = Icons.Default.FolderOpen,
                label = "Папка телефона",
                selected = phoneRootMode == PhoneRootMode.SAF_FOLDER,
                onClick = { onSwitchPhoneMode(PhoneRootMode.SAF_FOLDER) },
                backdrop = backdrop,
                modifier = Modifier
                    .weight(1f)
                    .testTag("phone_saf_chip")
            )
            if (phoneRootMode == PhoneRootMode.SAF_FOLDER && paneState.rootTreeUri != null) {
                val changeShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .liquidGlass(
                            backdrop = backdrop,
                            shape = changeShape,
                            fillAlpha = 0.45f,
                            blurRadius = 6.dp,
                            useLens = false
                        )
                        .clip(changeShape)
                        .clickable(onClick = onPickPhoneSafFolder)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Сменить",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkCharcoal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (phoneRootMode == PhoneRootMode.SAF_FOLDER && paneState.rootTreeUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(28.dp),
                            fillAlpha = 0.50f,
                            blurRadius = 10.dp,
                            useLens = true
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = InkCharcoal,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Выберите папку телефона",
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkCharcoal,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Подключите «Загрузки», «Документы» или любую папку на телефоне для прямого переноса файлов на USB-флешку.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSlateGray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onPickPhoneSafFolder,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GraphitePrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_phone_folder_button")
                        ) {
                            Text("Выбрать папку")
                        }
                    }
                }
            }
        } else {
            ExplorerBrowserPane(
                paneState = paneState,
                preferences = preferences,
                clipboard = clipboard,
                isUsb = false,
                statusBadge = if (phoneRootMode == PhoneRootMode.WORKSPACE) "Буфер" else "Папка",
                quickSendLabel = if (isUsbConnected) "Отправить на USB-флешку" else null,
                onQuickSend = onQuickSendToUsb,
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

@Composable
private fun GlassModePill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                fillAlpha = if (selected) 0.72f else 0.36f,
                blurRadius = 6.dp,
                useLens = selected,
                refractionHeight = 10.dp,
                refractionAmount = 18.dp,
                borderAlpha = if (selected) 1f else 0.65f
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InkCharcoal,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) InkCharcoal else InkSlateGray,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
