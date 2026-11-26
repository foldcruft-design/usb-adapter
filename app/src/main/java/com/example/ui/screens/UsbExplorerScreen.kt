package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OtgUserPreferences
import com.example.model.ClipboardMode
import com.example.model.ClipboardState
import com.example.model.FileNode
import com.example.model.RemovableVolumeInfo
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.model.UsbHardwareDevice
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberContainer
import com.example.util.Formatters
import com.example.viewmodel.BrowserPaneState

@Composable
fun UsbExplorerScreen(
    paneState: BrowserPaneState,
    preferences: OtgUserPreferences,
    clipboard: ClipboardState,
    usbDevices: List<UsbHardwareDevice>,
    removableVolumes: List<RemovableVolumeInfo>,
    showAddSheet: Boolean,
    onDismissAddSheet: () -> Unit,
    onGrantVolumeAccess: (RemovableVolumeInfo) -> Unit,
    onPickManualTree: () -> Unit,
    onRequestUsbDevicePermission: (Int) -> Unit,
    onRefreshHardware: () -> Unit,
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
    val mountedVolumeNeedingGrant = removableVolumes.firstOrNull { it.isMounted && it.persistedTreeUri == null }

    if (paneState.rootTreeUri != null && paneState.breadcrumbs.isNotEmpty()) {
        ExplorerBrowserPane(
            paneState = paneState,
            preferences = preferences,
            clipboard = clipboard,
            isUsb = true,
            statusBadge = "USB OTG подключён",
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
            modifier = modifier
        )
    } else {
        UsbOnboardingOrGrantView(
            mountedVolume = mountedVolumeNeedingGrant,
            usbDevices = usbDevices,
            onGrantVolumeAccess = onGrantVolumeAccess,
            onPickManualTree = onPickManualTree,
            onRequestUsbDevicePermission = onRequestUsbDevicePermission,
            onRefreshHardware = onRefreshHardware,
            modifier = modifier
        )
    }
}

@Composable
private fun UsbOnboardingOrGrantView(
    mountedVolume: RemovableVolumeInfo?,
    usbDevices: List<UsbHardwareDevice>,
    onGrantVolumeAccess: (RemovableVolumeInfo) -> Unit,
    onPickManualTree: () -> Unit,
    onRequestUsbDevicePermission: (Int) -> Unit,
    onRefreshHardware: () -> Unit,
    modifier: Modifier = Modifier
) {
    val massStorageHardware = usbDevices.firstOrNull { it.hasMassStorageInterface } ?: usbDevices.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_usb_otg_hero_1795699847717),
                        contentDescription = stringResource(R.string.cd_usb_hero_illustration),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (mountedVolume != null || massStorageHardware != null) EmeraldContainer else WarmAmberContainer,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (mountedVolume != null || massStorageHardware != null) EmeraldReady else WarmAmber)
                            )
                            Text(
                                text = when {
                                    mountedVolume != null -> "Том смонтирован"
                                    massStorageHardware != null -> "USB обнаружен"
                                    else -> "Ожидание OTG"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mountedVolume != null || massStorageHardware != null) EmeraldReady else WarmAmber
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (mountedVolume != null) {
                            stringResource(R.string.usb_detected_grant_title)
                        } else {
                            stringResource(R.string.usb_waiting_title)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (mountedVolume != null) {
                            "Обнаружен накопитель «${mountedVolume.description}». " +
                                stringResource(R.string.usb_detected_grant_subtitle)
                        } else {
                            stringResource(R.string.usb_waiting_subtitle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (mountedVolume != null) {
                        Button(
                            onClick = { onGrantVolumeAccess(mountedVolume) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("grant_usb_access_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.action_open_usb_drive))
                        }
                    } else {
                        Button(
                            onClick = onPickManualTree,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("pick_manual_usb_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.action_pick_folder_manually))
                        }
                    }

                    OutlinedButton(
                        onClick = onRefreshHardware,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("refresh_usb_hardware_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Проверить подключение USB")
                    }
                }
            }

            if (massStorageHardware != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = massStorageHardware.productName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${massStorageHardware.manufacturerName} • ${Formatters.formatUsbIds(massStorageHardware.vendorId, massStorageHardware.productId)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (massStorageHardware.hasPermission) "USB-доступ подтверждён" else "Требуется USB-разрешение",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (massStorageHardware.hasPermission) EmeraldReady else WarmAmber
                            )
                        }
                        if (!massStorageHardware.hasPermission) {
                            OutlinedButton(
                                onClick = { onRequestUsbDevicePermission(massStorageHardware.deviceId) }
                            ) {
                                Text("Доступ")
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Как работает автоопределение OTG",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. Вставьте флешку в OTG-адаптер и подключите к телефону.\n" +
                            "2. Система автоматически предложит открыть OTG Drive.\n" +
                            "3. Выдайте доступ к флешке один раз — далее все файлы, фото и документы можно просматривать, редактировать и переносить напрямую.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
