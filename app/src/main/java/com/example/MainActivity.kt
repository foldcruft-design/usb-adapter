package com.example

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PhoneRootMode
import com.example.model.RemovableVolumeInfo
import com.example.ui.components.LocalChromeBackdrop
import com.example.ui.components.LocalGlassBackdrop
import com.example.ui.components.SilverAmbientBackground
import com.example.ui.components.TransferProgressDialog
import com.example.ui.components.liquidGlass
import com.example.ui.screens.ImagePreviewDialog
import com.example.ui.screens.PhoneExplorerScreen
import com.example.ui.screens.TextViewerDialog
import com.example.ui.screens.UsbExplorerScreen
import com.example.ui.screens.UsbHardwareScreen
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkSlateGray
import com.example.ui.theme.OtgDriveTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.FileManagerViewModel
import com.example.viewmodel.OneShotUiEvent
import com.example.viewmodel.PaneTarget
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        if (savedInstanceState == null) {
            viewModel.handleIncomingIntent(intent)
        }
        setContent {
            OtgDriveTheme {
                OtgDriveApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHardwareAndFiles()
    }
}

@Composable
fun OtgDriveApp(viewModel: FileManagerViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val bgBackdrop = rememberLayerBackdrop()
    val chromeBackdrop = rememberLayerBackdrop()

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val usbPane by viewModel.usbPane.collectAsStateWithLifecycle()
    val phonePane by viewModel.phonePane.collectAsStateWithLifecycle()
    val phoneRootMode by viewModel.phoneRootMode.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    val transferProgress by viewModel.transferProgress.collectAsStateWithLifecycle()
    val usbDevices by viewModel.usbDevices.collectAsStateWithLifecycle()
    val removableVolumes by viewModel.removableVolumes.collectAsStateWithLifecycle()
    val eventLog by viewModel.eventLog.collectAsStateWithLifecycle()
    val imagePreviewNode by viewModel.imagePreviewNode.collectAsStateWithLifecycle()
    val textViewer by viewModel.textViewer.collectAsStateWithLifecycle()

    var pendingVolumeGrant by remember { mutableStateOf<RemovableVolumeInfo?>(null) }
    var activeImportTarget by remember { mutableStateOf(PaneTarget.USB) }
    var showAddSheetForTab by remember { mutableStateOf<PaneTarget?>(null) }

    val usbVolumeIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                viewModel.onUsbTreeUriGranted(uri, pendingVolumeGrant)
            }
        }
        pendingVolumeGrant = null
    }

    val manualUsbTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onUsbTreeUriGranted(uri, pendingVolumeGrant)
        }
        pendingVolumeGrant = null
    }

    val phoneFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhoneSafFolderGranted(uri)
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importExternalUris(activeImportTarget, uris)
        }
    }

    val documentsPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importExternalUris(activeImportTarget, uris)
        }
    }

    val launchVolumeGrant: (RemovableVolumeInfo) -> Unit = { volume ->
        pendingVolumeGrant = volume
        val intent = viewModel.buildGrantIntentForVolume(volume)
        runCatching {
            usbVolumeIntentLauncher.launch(intent)
        }.onFailure {
            manualUsbTreeLauncher.launch(null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is OneShotUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.text)
                }
                is OneShotUiEvent.RequestVolumeAccess -> {
                    launchVolumeGrant(event.volume)
                }
                is OneShotUiEvent.OpenExternalNode -> {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(event.uri, event.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(viewIntent, "Открыть с помощью"))
                    }.onFailure {
                        scope.launch {
                            snackbarHostState.showSnackbar("Нет приложения для открытия этого типа файла")
                        }
                    }
                }
                is OneShotUiEvent.ShareUris -> {
                    val shareIntent = if (event.uris.size == 1) {
                        Intent(Intent.ACTION_SEND).apply {
                            type = event.mimeType
                            putExtra(Intent.EXTRA_STREAM, event.uris.first())
                            clipData = ClipData.newRawUri("file", event.uris.first())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "*/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(event.uris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(shareIntent, "Поделиться"))
                    }.onFailure {
                        scope.launch {
                            snackbarHostState.showSnackbar("Не удалось открыть меню отправки")
                        }
                    }
                }
            }
        }
    }

    val isUsbConnected = usbPane.rootTreeUri != null && usbPane.breadcrumbs.isNotEmpty()
    val canAddOnUsb = activeTab == AppTab.USB && isUsbConnected
    val canAddOnPhone = activeTab == AppTab.PHONE &&
        (phoneRootMode == PhoneRootMode.WORKSPACE || phonePane.rootTreeUri != null)

    val mountedCount = removableVolumes.count { it.isMounted }
    val hardwareCount = usbDevices.size
    val navItems = AppNavItems(mountedCount = mountedCount, hardwareCount = hardwareCount)

    CompositionLocalProvider(
        LocalGlassBackdrop provides bgBackdrop,
        LocalChromeBackdrop provides chromeBackdrop
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            // Layer 1: Silver-Gray Atmospheric Backdrop (sampled by all content cards)
            SilverAmbientBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(bgBackdrop)
            )

            // Layer 2: Scrolling Content Layer (sampled by floating TopBar, BottomBar & FAB)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(chromeBackdrop)
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(bottom = if (useRail) 16.dp else 96.dp)
                        )
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                            .padding(top = 74.dp)
                    ) {
                        if (useRail) {
                            Spacer(modifier = Modifier.width(104.dp))
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when (activeTab) {
                                AppTab.USB -> {
                                    UsbExplorerScreen(
                                        paneState = usbPane,
                                        preferences = preferences,
                                        clipboard = clipboard,
                                        usbDevices = usbDevices,
                                        removableVolumes = removableVolumes,
                                        showAddSheet = showAddSheetForTab == PaneTarget.USB,
                                        onDismissAddSheet = { showAddSheetForTab = null },
                                        onGrantVolumeAccess = launchVolumeGrant,
                                        onPickManualTree = { manualUsbTreeLauncher.launch(null) },
                                        onRequestUsbDevicePermission = { devId ->
                                            viewModel.usbMonitor.requestUsbPermissionById(devId)
                                        },
                                        onRefreshHardware = { viewModel.refreshHardwareAndFiles() },
                                        onQuickSendToPhone = { list ->
                                            viewModel.quickTransferToOtherPane(PaneTarget.USB, list)
                                        },
                                        onNavigateUp = { viewModel.navigateUp(PaneTarget.USB) },
                                        onCrumbClick = { idx -> viewModel.navigateToBreadcrumb(PaneTarget.USB, idx) },
                                        onSearchChange = { q -> viewModel.updateSearchQuery(PaneTarget.USB, q) },
                                        onToggleViewMode = { viewModel.toggleViewMode() },
                                        onSetSort = { f, o -> viewModel.setSort(f, o) },
                                        onToggleHidden = { viewModel.setShowHiddenFiles(!preferences.showHiddenFiles) },
                                        onRefreshFiles = { viewModel.refreshPane(PaneTarget.USB) },
                                        onOpenNode = { node -> viewModel.openNode(PaneTarget.USB, node) },
                                        onToggleSelect = { id -> viewModel.toggleSelection(PaneTarget.USB, id) },
                                        onSelectAll = { viewModel.selectAll(PaneTarget.USB) },
                                        onClearSelection = { viewModel.clearSelection(PaneTarget.USB) },
                                        onCopyToClipboard = { mode, node ->
                                            viewModel.stageToClipboard(PaneTarget.USB, mode, node)
                                        },
                                        onPasteClipboard = { viewModel.pasteClipboard(PaneTarget.USB) },
                                        onClearClipboard = { viewModel.clearClipboard() },
                                        onCreateFolder = { name -> viewModel.createFolder(PaneTarget.USB, name) },
                                        onCreateTextFile = { name, content ->
                                            viewModel.createTextFile(PaneTarget.USB, name, content)
                                        },
                                        onRenameNode = { node, newName ->
                                            viewModel.renameNode(PaneTarget.USB, node, newName)
                                        },
                                        onDeleteNodes = { list -> viewModel.deleteNodes(PaneTarget.USB, list) },
                                        onOpenExternal = { node -> viewModel.openExternal(node) },
                                        onShareNodes = { list -> viewModel.shareNodes(list) },
                                        onPickPhotosAndVideos = {
                                            activeImportTarget = PaneTarget.USB
                                            mediaPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                        onPickDocuments = {
                                            activeImportTarget = PaneTarget.USB
                                            documentsPickerLauncher.launch(arrayOf("*/*"))
                                        }
                                    )
                                }

                                AppTab.PHONE -> {
                                    PhoneExplorerScreen(
                                        paneState = phonePane,
                                        phoneRootMode = phoneRootMode,
                                        isUsbConnected = isUsbConnected,
                                        preferences = preferences,
                                        clipboard = clipboard,
                                        showAddSheet = showAddSheetForTab == PaneTarget.PHONE,
                                        onDismissAddSheet = { showAddSheetForTab = null },
                                        onSwitchPhoneMode = { mode -> viewModel.switchPhoneMode(mode) },
                                        onPickPhoneSafFolder = { phoneFolderLauncher.launch(null) },
                                        onQuickSendToUsb = { list ->
                                            viewModel.quickTransferToOtherPane(PaneTarget.PHONE, list)
                                        },
                                        onNavigateUp = { viewModel.navigateUp(PaneTarget.PHONE) },
                                        onCrumbClick = { idx -> viewModel.navigateToBreadcrumb(PaneTarget.PHONE, idx) },
                                        onSearchChange = { q -> viewModel.updateSearchQuery(PaneTarget.PHONE, q) },
                                        onToggleViewMode = { viewModel.toggleViewMode() },
                                        onSetSort = { f, o -> viewModel.setSort(f, o) },
                                        onToggleHidden = { viewModel.setShowHiddenFiles(!preferences.showHiddenFiles) },
                                        onRefreshFiles = { viewModel.refreshPane(PaneTarget.PHONE) },
                                        onOpenNode = { node -> viewModel.openNode(PaneTarget.PHONE, node) },
                                        onToggleSelect = { id -> viewModel.toggleSelection(PaneTarget.PHONE, id) },
                                        onSelectAll = { viewModel.selectAll(PaneTarget.PHONE) },
                                        onClearSelection = { viewModel.clearSelection(PaneTarget.PHONE) },
                                        onCopyToClipboard = { mode, node ->
                                            viewModel.stageToClipboard(PaneTarget.PHONE, mode, node)
                                        },
                                        onPasteClipboard = { viewModel.pasteClipboard(PaneTarget.PHONE) },
                                        onClearClipboard = { viewModel.clearClipboard() },
                                        onCreateFolder = { name -> viewModel.createFolder(PaneTarget.PHONE, name) },
                                        onCreateTextFile = { name, content ->
                                            viewModel.createTextFile(PaneTarget.PHONE, name, content)
                                        },
                                        onRenameNode = { node, newName ->
                                            viewModel.renameNode(PaneTarget.PHONE, node, newName)
                                        },
                                        onDeleteNodes = { list -> viewModel.deleteNodes(PaneTarget.PHONE, list) },
                                        onOpenExternal = { node -> viewModel.openExternal(node) },
                                        onShareNodes = { list -> viewModel.shareNodes(list) },
                                        onPickPhotosAndVideos = {
                                            activeImportTarget = PaneTarget.PHONE
                                            mediaPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                        onPickDocuments = {
                                            activeImportTarget = PaneTarget.PHONE
                                            documentsPickerLauncher.launch(arrayOf("*/*"))
                                        }
                                    )
                                }

                                AppTab.HARDWARE -> {
                                    UsbHardwareScreen(
                                        usbDevices = usbDevices,
                                        removableVolumes = removableVolumes,
                                        eventLog = eventLog,
                                        onRefresh = { viewModel.refreshHardwareAndFiles() },
                                        onRequestUsbPermission = { devId ->
                                            viewModel.usbMonitor.requestUsbPermissionById(devId)
                                        },
                                        onGrantOrOpenVolume = { vol ->
                                            if (vol.persistedTreeUri != null) {
                                                viewModel.selectTab(AppTab.USB)
                                            } else {
                                                launchVolumeGrant(vol)
                                            }
                                        },
                                        onUnlinkVolume = { vol -> viewModel.unlinkVolume(vol) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Layer 3: Floating Liquid Glass TopBar (refracts content underneath)
            LiquidGlassTopBar(
                badgeCount = maxOf(mountedCount, hardwareCount),
                onHardwareClick = { viewModel.selectTab(AppTab.HARDWARE) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Floating Liquid Glass Navigation Rail on wide screens
            if (useRail) {
                LiquidGlassNavRail(
                    items = navItems,
                    activeTab = activeTab,
                    onSelectTab = { viewModel.selectTab(it) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(start = 14.dp, top = 74.dp, bottom = 16.dp)
                )
            }

            // Floating Liquid Glass "+ Добавить" Pill
            if (canAddOnUsb || canAddOnPhone) {
                val fabShape = RoundedCornerShape(28.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = if (useRail) 20.dp else 94.dp)
                        .height(54.dp)
                        .liquidGlass(
                            backdrop = chromeBackdrop,
                            shape = fabShape,
                            fillAlpha = 0.56f,
                            blurRadius = 10.dp,
                            useLens = true,
                            refractionHeight = 14.dp,
                            refractionAmount = 28.dp
                        )
                        .clip(fabShape)
                        .clickable {
                            showAddSheetForTab = if (activeTab == AppTab.USB) PaneTarget.USB else PaneTarget.PHONE
                        }
                        .padding(horizontal = 20.dp)
                        .testTag("add_files_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = InkCharcoal,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_add_to_usb),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = InkCharcoal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Floating Liquid Glass Bottom Navigation Bar (iOS 26 style)
            if (!useRail) {
                LiquidGlassBottomBar(
                    items = navItems,
                    activeTab = activeTab,
                    onSelectTab = { viewModel.selectTab(it) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }

    TransferProgressDialog(
        progress = transferProgress,
        onCancel = { viewModel.cancelTransfer() }
    )

    imagePreviewNode?.let { imgNode ->
        ImagePreviewDialog(
            node = imgNode,
            onShare = { viewModel.shareNodes(listOf(imgNode)) },
            onOpenExternal = { viewModel.openExternal(imgNode) },
            onDismiss = { viewModel.closeImagePreview() }
        )
    }

    textViewer?.let { viewerState ->
        TextViewerDialog(
            state = viewerState,
            onSave = { updated -> viewModel.saveTextViewer(updated) },
            onDismiss = { viewModel.closeTextViewer() }
        )
    }
}

@Composable
private fun LiquidGlassTopBar(
    badgeCount: Int,
    onHardwareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chromeBackdrop = LocalChromeBackdrop.current
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .liquidGlass(
                backdrop = chromeBackdrop,
                shape = shape,
                fillAlpha = 0.48f,
                blurRadius = 10.dp,
                useLens = true,
                refractionHeight = 14.dp,
                refractionAmount = 26.dp
            )
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = null,
                    tint = InkCharcoal,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = InkCharcoal
                )
            }

            IconButton(
                onClick = onHardwareClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("topbar_usb_status_button")
            ) {
                BadgedBox(
                    badge = {
                        if (badgeCount > 0) {
                            Badge {
                                Text("$badgeCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "USB статус",
                        tint = InkCharcoal
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassBottomBar(
    items: List<NavItemSpec>,
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val chromeBackdrop = LocalChromeBackdrop.current
    val barShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .liquidGlass(
                backdrop = chromeBackdrop,
                shape = barShape,
                fillAlpha = 0.48f,
                blurRadius = 10.dp,
                useLens = true,
                refractionHeight = 16.dp,
                refractionAmount = 32.dp
            )
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = activeTab == item.tab
                val itemShape = RoundedCornerShape(26.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(itemShape)
                        .background(
                            if (selected) Color.White.copy(alpha = 0.72f) else Color.Transparent,
                            itemShape
                        )
                        .clickable { onSelectTab(item.tab) }
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (item.badgeCount > 0) {
                                    Badge { Text("${item.badgeCount}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (selected) InkCharcoal else InkSlateGray,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
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
        }
    }
}

@Composable
private fun LiquidGlassNavRail(
    items: List<NavItemSpec>,
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val chromeBackdrop = LocalChromeBackdrop.current
    val railShape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .width(84.dp)
            .liquidGlass(
                backdrop = chromeBackdrop,
                shape = railShape,
                fillAlpha = 0.48f,
                blurRadius = 10.dp,
                useLens = true
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                val selected = activeTab == item.tab
                val itemShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(itemShape)
                        .background(
                            if (selected) Color.White.copy(alpha = 0.72f) else Color.Transparent,
                            itemShape
                        )
                        .clickable { onSelectTab(item.tab) }
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (item.badgeCount > 0) {
                                    Badge { Text("${item.badgeCount}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (selected) InkCharcoal else InkSlateGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) InkCharcoal else InkSlateGray,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

private data class NavItemSpec(
    val tab: AppTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int,
    val testTag: String
)

@Composable
private fun AppNavItems(
    mountedCount: Int,
    hardwareCount: Int
): List<NavItemSpec> = listOf(
    NavItemSpec(
        tab = AppTab.USB,
        label = stringResource(R.string.nav_usb_drive),
        selectedIcon = Icons.Filled.Usb,
        unselectedIcon = Icons.Outlined.Usb,
        badgeCount = mountedCount,
        testTag = "nav_usb_tab"
    ),
    NavItemSpec(
        tab = AppTab.PHONE,
        label = stringResource(R.string.nav_phone_storage),
        selectedIcon = Icons.Filled.Smartphone,
        unselectedIcon = Icons.Outlined.Smartphone,
        badgeCount = 0,
        testTag = "nav_phone_tab"
    ),
    NavItemSpec(
        tab = AppTab.HARDWARE,
        label = "USB Монитор",
        selectedIcon = Icons.Filled.Memory,
        unselectedIcon = Icons.Outlined.Memory,
        badgeCount = hardwareCount,
        testTag = "nav_hardware_tab"
    )
)
