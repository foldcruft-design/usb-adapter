package com.example

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PhoneRootMode
import com.example.model.RemovableVolumeInfo
import com.example.ui.components.TransferProgressDialog
import com.example.ui.screens.ImagePreviewDialog
import com.example.ui.screens.PhoneExplorerScreen
import com.example.ui.screens.TextViewerDialog
import com.example.ui.screens.UsbExplorerScreen
import com.example.ui.screens.UsbHardwareScreen
import com.example.ui.theme.OtgDriveTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.FileManagerViewModel
import com.example.viewmodel.OneShotUiEvent
import com.example.viewmodel.PaneTarget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtgDriveApp(viewModel: FileManagerViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    val canAddOnUsb = activeTab == AppTab.USB && usbPane.rootTreeUri != null && usbPane.breadcrumbs.isNotEmpty()
    val canAddOnPhone = activeTab == AppTab.PHONE &&
        (phoneRootMode == PhoneRootMode.WORKSPACE || phonePane.rootTreeUri != null)

    val mountedCount = removableVolumes.count { it.isMounted }
    val hardwareCount = usbDevices.size

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.selectTab(AppTab.HARDWARE) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("topbar_usb_status_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (mountedCount > 0 || hardwareCount > 0) {
                                        Badge {
                                            Text("${maxOf(mountedCount, hardwareCount)}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = "USB статус"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        AppNavItems(
                            activeTab = activeTab,
                            mountedCount = mountedCount,
                            hardwareCount = hardwareCount
                        ).forEach { item ->
                            NavigationBarItem(
                                selected = activeTab == item.tab,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge { Text("${item.badgeCount}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (activeTab == item.tab) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (canAddOnUsb || canAddOnPhone) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            showAddSheetForTab = if (activeTab == AppTab.USB) PaneTarget.USB else PaneTarget.PHONE
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.action_add_to_usb)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_files_fab")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        AppNavItems(
                            activeTab = activeTab,
                            mountedCount = mountedCount,
                            hardwareCount = hardwareCount
                        ).forEach { item ->
                            NavigationRailItem(
                                selected = activeTab == item.tab,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge { Text("${item.badgeCount}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (activeTab == item.tab) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
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
                                preferences = preferences,
                                clipboard = clipboard,
                                showAddSheet = showAddSheetForTab == PaneTarget.PHONE,
                                onDismissAddSheet = { showAddSheetForTab = null },
                                onSwitchPhoneMode = { mode -> viewModel.switchPhoneMode(mode) },
                                onPickPhoneSafFolder = { phoneFolderLauncher.launch(null) },
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
    activeTab: AppTab,
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
        label = stringResource(R.string.nav_usb_hardware),
        selectedIcon = Icons.Filled.Memory,
        unselectedIcon = Icons.Outlined.Memory,
        badgeCount = hardwareCount,
        testTag = "nav_hardware_tab"
    )
)
