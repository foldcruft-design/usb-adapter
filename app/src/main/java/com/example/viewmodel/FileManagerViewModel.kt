package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileManagerRepository
import com.example.data.OtgPreferencesRepository
import com.example.data.OtgUserPreferences
import com.example.data.UsbOtgMonitor
import com.example.model.BreadcrumbEntry
import com.example.model.ClipboardMode
import com.example.model.ClipboardState
import com.example.model.EventLevel
import com.example.model.FileCategory
import com.example.model.FileNode
import com.example.model.PhoneRootMode
import com.example.model.RemovableVolumeInfo
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.model.TransferProgress
import com.example.model.UsbEventLogEntry
import com.example.model.UsbHardwareDevice
import com.example.model.ViewMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class AppTab {
    USB,
    PHONE,
    HARDWARE
}

enum class PaneTarget {
    USB,
    PHONE
}

data class BrowserPaneState(
    val rootTreeUri: Uri? = null,
    val isLocalMode: Boolean = false,
    val volumeLabel: String = "",
    val breadcrumbs: List<BreadcrumbEntry> = emptyList(),
    val files: List<FileNode> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedIds: Set<String> = emptySet(),
    val totalBytes: Long? = null,
    val freeBytes: Long? = null
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
    val canNavigateUp: Boolean get() = breadcrumbs.size > 1
    val currentBreadcrumb: BreadcrumbEntry? get() = breadcrumbs.lastOrNull()
    val visibleFiles: List<FileNode>
        get() = if (searchQuery.isBlank()) {
            files
        } else {
            val q = searchQuery.trim()
            files.filter { it.name.contains(q, ignoreCase = true) }
        }
}

data class TextViewerState(
    val node: FileNode,
    val content: String,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed interface OneShotUiEvent {
    data class ShowMessage(val text: String) : OneShotUiEvent
    data class RequestVolumeAccess(val volume: RemovableVolumeInfo) : OneShotUiEvent
    data class OpenExternalNode(val uri: Uri, val mimeType: String) : OneShotUiEvent
    data class ShareUris(val uris: List<Uri>, val mimeType: String) : OneShotUiEvent
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = OtgPreferencesRepository(application)
    private val fileRepo = FileManagerRepository(application)
    val usbMonitor = UsbOtgMonitor(application, viewModelScope, prefsRepo)

    val preferences: StateFlow<OtgUserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, OtgUserPreferences())

    private val _activeTab = MutableStateFlow(AppTab.USB)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _usbPane = MutableStateFlow(BrowserPaneState())
    val usbPane: StateFlow<BrowserPaneState> = _usbPane.asStateFlow()

    private val _phonePane = MutableStateFlow(BrowserPaneState(isLocalMode = true))
    val phonePane: StateFlow<BrowserPaneState> = _phonePane.asStateFlow()

    private val _phoneRootMode = MutableStateFlow(PhoneRootMode.WORKSPACE)
    val phoneRootMode: StateFlow<PhoneRootMode> = _phoneRootMode.asStateFlow()

    private val _clipboard = MutableStateFlow(ClipboardState())
    val clipboard: StateFlow<ClipboardState> = _clipboard.asStateFlow()

    private val _transferProgress = MutableStateFlow(TransferProgress())
    val transferProgress: StateFlow<TransferProgress> = _transferProgress.asStateFlow()

    private val _imagePreviewNode = MutableStateFlow<FileNode?>(null)
    val imagePreviewNode: StateFlow<FileNode?> = _imagePreviewNode.asStateFlow()

    private val _textViewer = MutableStateFlow<TextViewerState?>(null)
    val textViewer: StateFlow<TextViewerState?> = _textViewer.asStateFlow()

    private val _uiEvents = MutableSharedFlow<OneShotUiEvent>(extraBufferCapacity = 8)
    val uiEvents: SharedFlow<OneShotUiEvent> = _uiEvents.asSharedFlow()

    val usbDevices: StateFlow<List<UsbHardwareDevice>> = usbMonitor.usbDevices
    val removableVolumes: StateFlow<List<RemovableVolumeInfo>> = usbMonitor.removableVolumes
    val eventLog: StateFlow<List<UsbEventLogEntry>> = usbMonitor.eventLog

    private var activeTransferJob: Job? = null
    private var pendingAutoPromptOnAttach = false

    init {
        usbMonitor.start()
        initLocalWorkspace()

        viewModelScope.launch {
            prefsRepo.preferencesFlow.collectLatest { prefs ->
                // Try restoring persisted USB or Phone SAF URI if available
                if (_usbPane.value.rootTreeUri == null && prefs.lastUsbTreeUri != null) {
                    val uri = Uri.parse(prefs.lastUsbTreeUri)
                    if (hasPersistedPermission(uri)) {
                        val mountedVolume = removableVolumes.value.firstOrNull {
                            it.isMounted && it.persistedTreeUri == uri
                        }
                        if (mountedVolume != null) {
                            mountUsbTreeUri(uri, mountedVolume)
                        }
                    }
                }
                refreshPane(PaneTarget.USB)
                refreshPane(PaneTarget.PHONE)
            }
        }

        viewModelScope.launch {
            removableVolumes.collectLatest { volumes ->
                val mountedWithUri = volumes.firstOrNull { it.isMounted && it.persistedTreeUri != null }
                val currentUri = _usbPane.value.rootTreeUri

                if (mountedWithUri != null && currentUri == null) {
                    mountUsbTreeUri(mountedWithUri.persistedTreeUri!!, mountedWithUri)
                } else if (currentUri != null) {
                    val stillMounted = volumes.any { it.isMounted && it.persistedTreeUri == currentUri }
                    // Also allow manually picked SAF tree if user picked a root manually
                    val matchesAnyMounted = volumes.any { it.isMounted }
                    if (!matchesAnyMounted && volumes.none { it.isMounted }) {
                        // Flash drive detached/unmounted — reset active USB listing if it came from a removable volume
                        if (_usbPane.value.breadcrumbs.isNotEmpty()) {
                            _usbPane.value = BrowserPaneState()
                        }
                    } else if (stillMounted) {
                        val vol = volumes.first { it.persistedTreeUri == currentUri }
                        val stats = fileRepo.getVolumeStorageStats(vol)
                        _usbPane.update {
                            it.copy(
                                totalBytes = stats?.first ?: it.totalBytes,
                                freeBytes = stats?.second ?: it.freeBytes
                            )
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            usbMonitor.usbAttachRedirectEvents.collect { volume ->
                _activeTab.value = AppTab.USB
                if (volume != null && volume.persistedTreeUri != null) {
                    mountUsbTreeUri(volume.persistedTreeUri, volume)
                    _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Флешка ${volume.description} подключена"))
                } else if (volume != null && pendingAutoPromptOnAttach) {
                    pendingAutoPromptOnAttach = false
                    _uiEvents.tryEmit(OneShotUiEvent.RequestVolumeAccess(volume))
                } else if (volume != null) {
                    _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Обнаружена флешка: ${volume.description}"))
                }
            }
        }
    }

    override fun onCleared() {
        usbMonitor.stop()
        super.onCleared()
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            pendingAutoPromptOnAttach = true
            _activeTab.value = AppTab.USB
        }
        usbMonitor.handleIncomingIntent(intent)
    }

    fun refreshHardwareAndFiles() {
        viewModelScope.launch {
            usbMonitor.refreshAll()
            refreshPane(PaneTarget.USB)
            refreshPane(PaneTarget.PHONE)
        }
    }

    fun buildGrantIntentForVolume(volume: RemovableVolumeInfo): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volume.storageVolume != null) {
            volume.storageVolume.createOpenDocumentTreeIntent()
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
    }

    fun onUsbTreeUriGranted(uri: Uri, targetVolume: RemovableVolumeInfo? = null) {
        fileRepo.persistTreePermission(uri)
        viewModelScope.launch {
            val matchedVolume = targetVolume ?: removableVolumes.value.firstOrNull { it.isMounted }
            if (matchedVolume != null) {
                prefsRepo.saveVolumeTreeUri(matchedVolume.key, uri)
            } else {
                prefsRepo.saveLastUsbTreeUri(uri)
            }
            usbMonitor.appendLog(
                "Доступ к флешке выдан",
                matchedVolume?.description ?: fileRepo.getSafRootDisplayName(uri),
                EventLevel.SUCCESS
            )
            usbMonitor.refreshAll()
            mountUsbTreeUri(uri, matchedVolume)
        }
    }

    fun unlinkVolume(volume: RemovableVolumeInfo) {
        viewModelScope.launch {
            prefsRepo.removeVolumeTreeUri(volume.key)
            if (_usbPane.value.rootTreeUri == volume.persistedTreeUri) {
                _usbPane.value = BrowserPaneState()
            }
            usbMonitor.refreshAll()
            _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Связь с томом ${volume.description} сброшена"))
        }
    }

    fun onPhoneSafFolderGranted(uri: Uri) {
        fileRepo.persistTreePermission(uri)
        viewModelScope.launch {
            prefsRepo.savePhoneSafTreeUri(uri)
            switchPhoneToSaf(uri)
        }
    }

    fun switchPhoneMode(mode: PhoneRootMode) {
        _phoneRootMode.value = mode
        if (mode == PhoneRootMode.WORKSPACE) {
            initLocalWorkspace()
        } else {
            val savedPhoneUri = preferences.value.phoneSafTreeUri?.let { Uri.parse(it) }
            if (savedPhoneUri != null && hasPersistedPermission(savedPhoneUri)) {
                switchPhoneToSaf(savedPhoneUri)
            } else {
                _phonePane.value = BrowserPaneState(isLocalMode = false)
            }
        }
    }

    private fun initLocalWorkspace() {
        val root = fileRepo.getLocalWorkspaceRoot()
        val stats = fileRepo.getLocalStorageStats(root)
        _phonePane.value = BrowserPaneState(
            rootTreeUri = null,
            isLocalMode = true,
            volumeLabel = "OTG Буфер (Телефон)",
            breadcrumbs = listOf(
                BreadcrumbEntry(
                    name = "OTG Буфер",
                    treeUri = Uri.fromFile(root),
                    documentId = null,
                    localPath = root.absolutePath
                )
            ),
            totalBytes = stats.first,
            freeBytes = stats.second
        )
        refreshPane(PaneTarget.PHONE)
    }

    private fun switchPhoneToSaf(uri: Uri) {
        _phoneRootMode.value = PhoneRootMode.SAF_FOLDER
        val rootDocId = fileRepo.getRootDocumentId(uri)
        val label = fileRepo.getSafRootDisplayName(uri)
        _phonePane.value = BrowserPaneState(
            rootTreeUri = uri,
            isLocalMode = false,
            volumeLabel = label,
            breadcrumbs = listOf(
                BreadcrumbEntry(
                    name = label,
                    treeUri = uri,
                    documentId = rootDocId
                )
            )
        )
        refreshPane(PaneTarget.PHONE)
    }

    private fun mountUsbTreeUri(uri: Uri, volume: RemovableVolumeInfo?) {
        val rootDocId = runCatching { fileRepo.getRootDocumentId(uri) }.getOrNull() ?: return
        val label = volume?.description?.takeIf { it.isNotBlank() } ?: fileRepo.getSafRootDisplayName(uri)
        val stats = fileRepo.getVolumeStorageStats(volume)
        _usbPane.value = BrowserPaneState(
            rootTreeUri = uri,
            isLocalMode = false,
            volumeLabel = label,
            breadcrumbs = listOf(
                BreadcrumbEntry(
                    name = label,
                    treeUri = uri,
                    documentId = rootDocId
                )
            ),
            totalBytes = stats?.first,
            freeBytes = stats?.second
        )
        refreshPane(PaneTarget.USB)
    }

    fun refreshPane(target: PaneTarget) {
        val stateFlow = paneFlow(target)
        val state = stateFlow.value
        val currentCrumb = state.currentBreadcrumb ?: return

        viewModelScope.launch {
            stateFlow.update { it.copy(isLoading = true, errorMessage = null) }
            val prefs = preferences.value
            val result = runCatching {
                if (state.isLocalMode && currentCrumb.localPath != null) {
                    fileRepo.listLocalDirectory(
                        File(currentCrumb.localPath),
                        prefs.showHiddenFiles,
                        prefs.sortField,
                        prefs.sortOrder
                    )
                } else {
                    val tree = state.rootTreeUri ?: return@runCatching emptyList()
                    fileRepo.listSafDirectory(
                        tree,
                        currentCrumb.documentId,
                        prefs.showHiddenFiles,
                        prefs.sortField,
                        prefs.sortOrder
                    )
                }
            }
            result.onSuccess { list ->
                stateFlow.update {
                    it.copy(
                        files = list,
                        isLoading = false,
                        errorMessage = null,
                        selectedIds = it.selectedIds.intersect(list.map { n -> n.id }.toSet())
                    )
                }
            }.onFailure { err ->
                stateFlow.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Не удалось прочитать папку"
                    )
                }
            }
        }
    }

    fun openNode(target: PaneTarget, node: FileNode) {
        val stateFlow = paneFlow(target)
        if (stateFlow.value.isSelectionMode) {
            toggleSelection(target, node.id)
            return
        }

        if (node.isDirectory) {
            val current = stateFlow.value
            val nextCrumb = if (node.isLocalFile) {
                BreadcrumbEntry(
                    name = node.name,
                    treeUri = node.uri,
                    documentId = null,
                    localPath = node.id
                )
            } else {
                val tree = current.rootTreeUri ?: return
                BreadcrumbEntry(
                    name = node.name,
                    treeUri = tree,
                    documentId = node.id
                )
            }
            stateFlow.update {
                it.copy(
                    breadcrumbs = it.breadcrumbs + nextCrumb,
                    searchQuery = "",
                    selectedIds = emptySet()
                )
            }
            refreshPane(target)
        } else {
            when (node.category) {
                FileCategory.IMAGE -> _imagePreviewNode.value = node
                FileCategory.CODE_TEXT -> openTextViewer(node)
                else -> openExternal(node)
            }
        }
    }

    fun navigateUp(target: PaneTarget): Boolean {
        val stateFlow = paneFlow(target)
        val crumbs = stateFlow.value.breadcrumbs
        if (crumbs.size <= 1) return false
        stateFlow.update {
            it.copy(
                breadcrumbs = crumbs.dropLast(1),
                searchQuery = "",
                selectedIds = emptySet()
            )
        }
        refreshPane(target)
        return true
    }

    fun navigateToBreadcrumb(target: PaneTarget, index: Int) {
        val stateFlow = paneFlow(target)
        val crumbs = stateFlow.value.breadcrumbs
        if (index !in crumbs.indices) return
        stateFlow.update {
            it.copy(
                breadcrumbs = crumbs.take(index + 1),
                searchQuery = "",
                selectedIds = emptySet()
            )
        }
        refreshPane(target)
    }

    fun updateSearchQuery(target: PaneTarget, query: String) {
        paneFlow(target).update { it.copy(searchQuery = query) }
    }

    fun toggleSelection(target: PaneTarget, nodeId: String) {
        paneFlow(target).update { state ->
            val updated = if (nodeId in state.selectedIds) {
                state.selectedIds - nodeId
            } else {
                state.selectedIds + nodeId
            }
            state.copy(selectedIds = updated)
        }
    }

    fun selectAll(target: PaneTarget) {
        paneFlow(target).update { state ->
            state.copy(selectedIds = state.visibleFiles.map { it.id }.toSet())
        }
    }

    fun clearSelection(target: PaneTarget) {
        paneFlow(target).update { it.copy(selectedIds = emptySet()) }
    }

    fun stageToClipboard(target: PaneTarget, mode: ClipboardMode, singleNode: FileNode? = null) {
        val pane = paneFlow(target).value
        val items = if (singleNode != null) {
            listOf(singleNode)
        } else {
            pane.files.filter { it.id in pane.selectedIds }
        }
        if (items.isEmpty()) return

        _clipboard.value = ClipboardState(
            items = items,
            mode = mode,
            sourceLabel = pane.volumeLabel,
            sourceTreeUri = pane.rootTreeUri
        )
        clearSelection(target)
        val actionWord = if (mode == ClipboardMode.COPY) "Скопировано" else "Вырезано"
        _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("$actionWord: ${items.size}"))
    }

    fun clearClipboard() {
        _clipboard.value = ClipboardState()
    }

    fun pasteClipboard(target: PaneTarget) {
        val clip = _clipboard.value
        if (clip.isEmpty) return
        val pane = paneFlow(target).value
        val crumb = pane.currentBreadcrumb ?: return

        activeTransferJob?.cancel()
        activeTransferJob = viewModelScope.launch {
            val result = fileRepo.copyOrMoveNodes(
                sources = clip.items,
                sourceTreeUri = clip.sourceTreeUri,
                targetTreeUri = pane.rootTreeUri,
                targetParentDocId = crumb.documentId,
                targetLocalDir = crumb.localPath?.let { File(it) },
                isMove = clip.mode == ClipboardMode.MOVE,
                onProgress = { progress -> _transferProgress.value = progress }
            )
            _transferProgress.value = TransferProgress()
            result.onSuccess { count ->
                usbMonitor.appendLog(
                    if (clip.mode == ClipboardMode.MOVE) "Перемещено файлов: $count" else "Скопировано файлов: $count",
                    "Назначение: ${pane.volumeLabel}",
                    EventLevel.SUCCESS
                )
                if (clip.mode == ClipboardMode.MOVE) {
                    _clipboard.value = ClipboardState()
                }
                refreshPane(PaneTarget.USB)
                refreshPane(PaneTarget.PHONE)
                _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Готово: обработано $count"))
            }.onFailure { err ->
                _uiEvents.tryEmit(
                    OneShotUiEvent.ShowMessage(err.localizedMessage ?: "Ошибка копирования")
                )
            }
        }
    }

    fun importExternalUris(target: PaneTarget, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val pane = paneFlow(target).value
        val crumb = pane.currentBreadcrumb ?: return

        activeTransferJob?.cancel()
        activeTransferJob = viewModelScope.launch {
            val result = fileRepo.importExternalUris(
                sourceUris = uris,
                targetTreeUri = pane.rootTreeUri,
                targetParentDocId = crumb.documentId,
                targetLocalDir = crumb.localPath?.let { File(it) },
                onProgress = { progress -> _transferProgress.value = progress }
            )
            _transferProgress.value = TransferProgress()
            result.onSuccess { count ->
                usbMonitor.appendLog(
                    "Добавлено файлов: $count",
                    "В ${crumb.name}",
                    EventLevel.SUCCESS
                )
                refreshPane(target)
                _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Добавлено файлов: $count"))
            }.onFailure { err ->
                _uiEvents.tryEmit(
                    OneShotUiEvent.ShowMessage(err.localizedMessage ?: "Не удалось добавить файлы")
                )
            }
        }
    }

    fun cancelTransfer() {
        activeTransferJob?.cancel()
        _transferProgress.value = TransferProgress()
        _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Передача отменена"))
        refreshPane(PaneTarget.USB)
        refreshPane(PaneTarget.PHONE)
    }

    fun createFolder(target: PaneTarget, name: String) {
        val pane = paneFlow(target).value
        val crumb = pane.currentBreadcrumb ?: return
        viewModelScope.launch {
            fileRepo.createDirectory(
                treeUri = pane.rootTreeUri,
                parentDocId = crumb.documentId,
                localParentDir = crumb.localPath?.let { File(it) },
                name = name
            ).onSuccess {
                refreshPane(target)
                _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Папка «$name» создана"))
            }.onFailure {
                _uiEvents.tryEmit(OneShotUiEvent.ShowMessage(it.localizedMessage ?: "Ошибка создания папки"))
            }
        }
    }

    fun createTextFile(target: PaneTarget, name: String, content: String) {
        val pane = paneFlow(target).value
        val crumb = pane.currentBreadcrumb ?: return
        viewModelScope.launch {
            fileRepo.createTextFile(
                treeUri = pane.rootTreeUri,
                parentDocId = crumb.documentId,
                localParentDir = crumb.localPath?.let { File(it) },
                name = name,
                content = content
            ).onSuccess {
                refreshPane(target)
                _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Файл создан"))
            }.onFailure {
                _uiEvents.tryEmit(OneShotUiEvent.ShowMessage(it.localizedMessage ?: "Ошибка создания файла"))
            }
        }
    }

    fun renameNode(target: PaneTarget, node: FileNode, newName: String) {
        val pane = paneFlow(target).value
        viewModelScope.launch {
            fileRepo.renameNode(pane.rootTreeUri, node, newName)
                .onSuccess {
                    refreshPane(target)
                    _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Переименовано"))
                }.onFailure {
                    _uiEvents.tryEmit(OneShotUiEvent.ShowMessage(it.localizedMessage ?: "Ошибка переименования"))
                }
        }
    }

    fun deleteNodes(target: PaneTarget, explicitNodes: List<FileNode>? = null) {
        val pane = paneFlow(target).value
        val targets = explicitNodes ?: pane.files.filter { it.id in pane.selectedIds }
        if (targets.isEmpty()) return

        viewModelScope.launch {
            fileRepo.deleteNodes(targets)
                .onSuccess { count ->
                    clearSelection(target)
                    refreshPane(target)
                    _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Удалено объектов: $count"))
                }.onFailure {
                    _uiEvents.tryEmit(OneShotUiEvent.ShowMessage(it.localizedMessage ?: "Ошибка удаления"))
                }
        }
    }

    fun openTextViewer(node: FileNode) {
        viewModelScope.launch {
            _textViewer.value = TextViewerState(node = node, content = "", isLoading = true)
            fileRepo.readTextContent(node)
                .onSuccess { text ->
                    _textViewer.value = TextViewerState(node = node, content = text, isLoading = false)
                }
                .onFailure { err ->
                    _textViewer.value = TextViewerState(
                        node = node,
                        content = "",
                        isLoading = false,
                        error = err.localizedMessage ?: "Не удалось прочитать файл"
                    )
                }
        }
    }

    fun saveTextViewer(newContent: String) {
        val current = _textViewer.value ?: return
        viewModelScope.launch {
            _textViewer.value = current.copy(isSaving = true)
            fileRepo.writeTextContent(current.node, newContent)
                .onSuccess {
                    _textViewer.value = current.copy(content = newContent, isSaving = false)
                    refreshPane(PaneTarget.USB)
                    refreshPane(PaneTarget.PHONE)
                    _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Файл сохранён"))
                }
                .onFailure { err ->
                    _textViewer.value = current.copy(isSaving = false, error = err.localizedMessage)
                }
        }
    }

    fun closeTextViewer() {
        _textViewer.value = null
    }

    fun closeImagePreview() {
        _imagePreviewNode.value = null
    }

    fun openExternal(node: FileNode) {
        val shareableUri = fileRepo.getShareableUri(node)
        _uiEvents.tryEmit(OneShotUiEvent.OpenExternalNode(shareableUri, node.mimeType))
    }

    fun shareNodes(nodes: List<FileNode>) {
        val filesOnly = nodes.filter { !it.isDirectory }
        if (filesOnly.isEmpty()) {
            _uiEvents.tryEmit(OneShotUiEvent.ShowMessage("Выберите файлы (папки нельзя отправить напрямую)"))
            return
        }
        val uris = filesOnly.map { fileRepo.getShareableUri(it) }
        val mime = if (filesOnly.size == 1) filesOnly.first().mimeType else "*/*"
        _uiEvents.tryEmit(OneShotUiEvent.ShareUris(uris, mime))
    }

    fun setSort(field: SortField, order: SortOrder) {
        viewModelScope.launch { prefsRepo.updateSort(field, order) }
    }

    fun toggleViewMode() {
        val next = if (preferences.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        viewModelScope.launch { prefsRepo.updateViewMode(next) }
    }

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch { prefsRepo.updateShowHidden(show) }
    }

    private fun paneFlow(target: PaneTarget): MutableStateFlow<BrowserPaneState> {
        return if (target == PaneTarget.USB) _usbPane else _phonePane
    }

    private fun hasPersistedPermission(uri: Uri): Boolean {
        return getApplication<Application>().contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    }
}
