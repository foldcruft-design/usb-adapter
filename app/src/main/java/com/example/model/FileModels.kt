package com.example.model

import android.net.Uri
import android.os.storage.StorageVolume

enum class FileCategory {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    CODE_TEXT,
    APK,
    OTHER
}

data class FileNode(
    val id: String,
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val mimeType: String,
    val canWrite: Boolean,
    val canDelete: Boolean,
    val canRename: Boolean,
    val isLocalFile: Boolean,
    val category: FileCategory
)

data class BreadcrumbEntry(
    val name: String,
    val treeUri: Uri,
    val documentId: String?,
    val localPath: String? = null
)

enum class SortField(val label: String) {
    NAME("По имени"),
    SIZE("По размеру"),
    DATE("По дате"),
    TYPE("По типу")
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class ViewMode {
    LIST,
    GRID
}

enum class ClipboardMode {
    COPY,
    MOVE
}

data class ClipboardState(
    val items: List<FileNode> = emptyList(),
    val mode: ClipboardMode = ClipboardMode.COPY,
    val sourceLabel: String = "",
    val sourceTreeUri: Uri? = null
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

data class TransferProgress(
    val isActive: Boolean = false,
    val operationTitle: String = "",
    val currentFileName: String = "",
    val completedFiles: Int = 0,
    val totalFiles: Int = 0,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L
)

data class UsbHardwareDevice(
    val deviceId: Int,
    val deviceName: String,
    val manufacturerName: String,
    val productName: String,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val interfaceCount: Int,
    val hasMassStorageInterface: Boolean,
    val hasPermission: Boolean
)

data class RemovableVolumeInfo(
    val key: String,
    val description: String,
    val uuid: String?,
    val state: String,
    val isMounted: Boolean,
    val isRemovable: Boolean,
    val isPrimary: Boolean,
    val persistedTreeUri: Uri?,
    val totalBytes: Long?,
    val freeBytes: Long?,
    val storageVolume: StorageVolume?
)

enum class EventLevel {
    INFO,
    SUCCESS,
    WARNING
}

data class UsbEventLogEntry(
    val timestampMs: Long,
    val title: String,
    val detail: String,
    val level: EventLevel
)

enum class PhoneRootMode {
    WORKSPACE,
    SAF_FOLDER
}
