package com.example.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.ContextCompat
import com.example.model.EventLevel
import com.example.model.RemovableVolumeInfo
import com.example.model.UsbEventLogEntry
import com.example.model.UsbHardwareDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsbOtgMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val prefsRepo: OtgPreferencesRepository
) {
    companion object {
        const val ACTION_USB_PERMISSION = "com.aistudio.otgmanager.USB_PERMISSION"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager

    private val _usbDevices = MutableStateFlow<List<UsbHardwareDevice>>(emptyList())
    val usbDevices: StateFlow<List<UsbHardwareDevice>> = _usbDevices.asStateFlow()

    private val _removableVolumes = MutableStateFlow<List<RemovableVolumeInfo>>(emptyList())
    val removableVolumes: StateFlow<List<RemovableVolumeInfo>> = _removableVolumes.asStateFlow()

    private val _eventLog = MutableStateFlow<List<UsbEventLogEntry>>(emptyList())
    val eventLog: StateFlow<List<UsbEventLogEntry>> = _eventLog.asStateFlow()

    private val _usbAttachRedirectEvents = MutableSharedFlow<RemovableVolumeInfo?>(extraBufferCapacity = 4)
    val usbAttachRedirectEvents: SharedFlow<RemovableVolumeInfo?> = _usbAttachRedirectEvents.asSharedFlow()

    private var mountPollJob: Job? = null
    private var isRegistered = false
    private var storageCallback: Any? = null

    private val usbAndMediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = extractUsbDevice(intent)
                    val name = device?.productName ?: device?.deviceName ?: "USB устройство"
                    appendLog("USB OTG подключён", name, EventLevel.SUCCESS)
                    scope.launch {
                        refreshAll()
                        device?.let {
                            if (hasMassStorageInterface(it) && usbManager?.hasPermission(it) == false) {
                                requestUsbPermission(it)
                            }
                        }
                        pollUntilVolumeMounts(triggerRedirect = true)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = extractUsbDevice(intent)
                    val name = device?.productName ?: device?.deviceName ?: "USB устройство"
                    appendLog("USB OTG отключён", name, EventLevel.WARNING)
                    mountPollJob?.cancel()
                    scope.launch { refreshAll() }
                }

                ACTION_USB_PERMISSION -> {
                    val device = extractUsbDevice(intent)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val name = device?.productName ?: device?.deviceName ?: "USB"
                    appendLog(
                        if (granted) "USB разрешение получено" else "USB разрешение отклонено",
                        name,
                        if (granted) EventLevel.SUCCESS else EventLevel.WARNING
                    )
                    scope.launch { refreshAll() }
                }

                Intent.ACTION_MEDIA_MOUNTED -> {
                    appendLog("Накопитель смонтирован", intent.dataString ?: "Внешний том", EventLevel.SUCCESS)
                    scope.launch {
                        refreshAll()
                        val mounted = _removableVolumes.value.firstOrNull { it.isMounted }
                        if (mounted != null) {
                            _usbAttachRedirectEvents.tryEmit(mounted)
                        }
                    }
                }

                Intent.ACTION_MEDIA_UNMOUNTED,
                Intent.ACTION_MEDIA_EJECT,
                Intent.ACTION_MEDIA_REMOVED -> {
                    appendLog("Накопитель извлечён", intent.dataString ?: "Внешний том", EventLevel.WARNING)
                    scope.launch { refreshAll() }
                }
            }
        }
    }

    fun start() {
        if (!isRegistered) {
            val usbFilter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(ACTION_USB_PERMISSION)
            }
            ContextCompat.registerReceiver(
                context,
                usbAndMediaReceiver,
                usbFilter,
                ContextCompat.RECEIVER_EXPORTED
            )

            val mediaFilter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addDataScheme("file")
            }
            ContextCompat.registerReceiver(
                context,
                usbAndMediaReceiver,
                mediaFilter,
                ContextCompat.RECEIVER_EXPORTED
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && storageManager != null) {
                val callback = object : StorageManager.StorageVolumeCallback() {
                    override fun onStateChanged(volume: StorageVolume) {
                        if (volume.isRemovable && !volume.isPrimary) {
                            val label = volume.getDescription(context)
                            appendLog("Статус тома: ${volume.state}", label, EventLevel.INFO)
                            scope.launch {
                                refreshAll()
                                if (volume.state == Environment.MEDIA_MOUNTED) {
                                    val info = _removableVolumes.value.firstOrNull { it.isMounted }
                                    _usbAttachRedirectEvents.tryEmit(info)
                                }
                            }
                        }
                    }
                }
                storageCallback = callback
                storageManager.registerStorageVolumeCallback(context.mainExecutor, callback)
            }

            isRegistered = true
            appendLog("Монитор USB OTG активен", "Ожидание подключения флешки", EventLevel.INFO)
        }
        scope.launch { refreshAll() }
    }

    fun stop() {
        if (isRegistered) {
            runCatching { context.unregisterReceiver(usbAndMediaReceiver) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                (storageCallback as? StorageManager.StorageVolumeCallback)?.let {
                    runCatching { storageManager?.unregisterStorageVolumeCallback(it) }
                }
            }
            isRegistered = false
        }
        mountPollJob?.cancel()
    }

    fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device = extractUsbDevice(intent)
            val label = device?.productName ?: device?.deviceName ?: "USB-накопитель"
            appendLog("Автозапуск по USB OTG", label, EventLevel.SUCCESS)
            scope.launch {
                refreshAll()
                val alreadyMounted = _removableVolumes.value.firstOrNull { it.isMounted }
                _usbAttachRedirectEvents.tryEmit(alreadyMounted)
                if (alreadyMounted == null) {
                    pollUntilVolumeMounts(triggerRedirect = true)
                }
            }
        }
    }

    fun requestUsbPermission(device: UsbDevice) {
        val manager = usbManager ?: return
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getBroadcast(context, device.deviceId, intent, flags)
        manager.requestPermission(device, pi)
    }

    fun requestUsbPermissionById(deviceId: Int) {
        val device = usbManager?.deviceList?.values?.firstOrNull { it.deviceId == deviceId } ?: return
        requestUsbPermission(device)
    }

    suspend fun refreshAll() {
        val devices = usbManager?.deviceList?.values?.map { device ->
            UsbHardwareDevice(
                deviceId = device.deviceId,
                deviceName = device.deviceName,
                manufacturerName = device.manufacturerName?.takeIf { it.isNotBlank() } ?: "Неизвестный производитель",
                productName = device.productName?.takeIf { it.isNotBlank() } ?: "USB Mass Storage",
                vendorId = device.vendorId,
                productId = device.productId,
                deviceClass = device.deviceClass,
                interfaceCount = device.interfaceCount,
                hasMassStorageInterface = hasMassStorageInterface(device),
                hasPermission = usbManager.hasPermission(device)
            )
        }?.sortedByDescending { it.hasMassStorageInterface } ?: emptyList()

        _usbDevices.value = devices

        val persistedUris = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission }
            .map { it.uri.toString() }
            .toSet()

        val savedMap = prefsRepo.getVolumeUriMap()
        val volumes = storageManager?.storageVolumes
            ?.filter { it.isRemovable && !it.isPrimary }
            ?.map { vol ->
                val desc = vol.getDescription(context)
                val uuid = vol.uuid
                val key = buildVolumeKey(uuid, desc)
                val mappedUriStr = savedMap[key]?.takeIf { it in persistedUris }
                    ?: findMatchingPersistedUri(uuid, persistedUris)

                var totalBytes: Long? = null
                var freeBytes: Long? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vol.state == Environment.MEDIA_MOUNTED) {
                    runCatching {
                        vol.directory?.let { dir ->
                            val stat = StatFs(dir.path)
                            totalBytes = stat.totalBytes
                            freeBytes = stat.availableBytes
                        }
                    }
                }

                RemovableVolumeInfo(
                    key = key,
                    description = desc,
                    uuid = uuid,
                    state = vol.state,
                    isMounted = vol.state == Environment.MEDIA_MOUNTED,
                    isRemovable = vol.isRemovable,
                    isPrimary = vol.isPrimary,
                    persistedTreeUri = mappedUriStr?.let { Uri.parse(it) },
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    storageVolume = vol
                )
            } ?: emptyList()

        _removableVolumes.value = volumes
    }

    private fun pollUntilVolumeMounts(triggerRedirect: Boolean) {
        mountPollJob?.cancel()
        mountPollJob = scope.launch {
            repeat(15) {
                refreshAll()
                val mounted = _removableVolumes.value.firstOrNull { it.isMounted }
                if (mounted != null) {
                    if (triggerRedirect) {
                        _usbAttachRedirectEvents.tryEmit(mounted)
                    }
                    return@launch
                }
                delay(400)
            }
        }
    }

    fun appendLog(title: String, detail: String, level: EventLevel) {
        val entry = UsbEventLogEntry(
            timestampMs = System.currentTimeMillis(),
            title = title,
            detail = detail,
            level = level
        )
        _eventLog.value = (listOf(entry) + _eventLog.value).take(50)
    }

    private fun hasMassStorageInterface(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_MASS_STORAGE) return true
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) {
                return true
            }
        }
        return false
    }

    private fun extractUsbDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private fun buildVolumeKey(uuid: String?, description: String): String {
        val raw = uuid?.takeIf { it.isNotBlank() } ?: description
        return raw.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    private fun findMatchingPersistedUri(uuid: String?, persistedUris: Set<String>): String? {
        if (uuid.isNullOrBlank()) return null
        return persistedUris.firstOrNull { it.contains(uuid, ignoreCase = true) }
    }
}
