package com.example.util

import android.hardware.usb.UsbConstants
import android.webkit.MimeTypeMap
import com.example.model.FileCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object Formatters {

    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 Б"
        val units = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            .coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes Б"
        } else {
            String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return "—"
        return dateFormatter.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        return timeFormatter.format(Date(millis))
    }

    fun formatUsbIds(vendorId: Int, productId: Int): String {
        return String.format(Locale.US, "VID:%04X  PID:%04X", vendorId, productId)
    }

    fun describeUsbClass(usbClass: Int): String = when (usbClass) {
        UsbConstants.USB_CLASS_MASS_STORAGE -> "Mass Storage (Флеш-накопитель)"
        UsbConstants.USB_CLASS_PER_INTERFACE -> "Составное устройство (Per-Interface)"
        UsbConstants.USB_CLASS_HUB -> "USB Hub / Адаптер"
        UsbConstants.USB_CLASS_HID -> "HID (Ввод)"
        UsbConstants.USB_CLASS_AUDIO -> "USB Audio"
        UsbConstants.USB_CLASS_VIDEO -> "USB Video"
        else -> "USB Class $usbClass"
    }

    fun classifyFile(name: String, mimeType: String, isDirectory: Boolean): FileCategory {
        if (isDirectory) return FileCategory.FOLDER
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = mimeType.lowercase(Locale.ROOT)
        return when {
            mime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg") -> FileCategory.IMAGE
            mime.startsWith("video/") || ext in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp") -> FileCategory.VIDEO
            mime.startsWith("audio/") || ext in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus") -> FileCategory.AUDIO
            ext == "apk" || mime == "application/vnd.android.package-archive" -> FileCategory.APK
            ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") || mime.contains("zip") || mime.contains("compressed") -> FileCategory.ARCHIVE
            ext in setOf("txt", "md", "json", "xml", "csv", "log", "kt", "java", "py", "js", "ts", "html", "css", "yaml", "yml", "ini", "conf", "sh") ||
                mime.startsWith("text/") || mime == "application/json" -> FileCategory.CODE_TEXT
            ext in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "rtf") || mime.contains("pdf") || mime.contains("document") -> FileCategory.DOCUMENT
            else -> FileCategory.OTHER
        }
    }

    fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext == "md") return "text/markdown"
        if (ext == "log" || ext == "conf" || ext == "ini") return "text/plain"
        if (ext == "json") return "application/json"
        if (ext == "apk") return "application/vnd.android.package-archive"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
