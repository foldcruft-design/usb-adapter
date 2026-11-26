package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.model.FileCategory
import com.example.model.FileNode
import com.example.model.RemovableVolumeInfo
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.model.TransferProgress
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

class FileManagerRepository(private val context: Context) {

    private val resolver = context.contentResolver

    fun getLocalWorkspaceRoot(): File {
        val dir = File(context.filesDir, "otg_workspace")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getShareableUri(node: FileNode): Uri {
        return if (node.isLocalFile) {
            val file = File(node.id)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            node.uri
        }
    }

    fun persistTreePermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            resolver.takePersistableUriPermission(uri, flags)
        }
    }

    fun getRootDocumentId(treeUri: Uri): String {
        return DocumentsContract.getTreeDocumentId(treeUri)
    }

    fun getSafRootDisplayName(treeUri: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        runCatching {
            resolver.query(
                rootUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }
        return docId.substringBefore(':').ifBlank { "USB Накопитель" }
    }

    suspend fun listSafDirectory(
        treeUri: Uri,
        parentDocId: String?,
        showHidden: Boolean,
        sortField: SortField,
        sortOrder: SortOrder
    ): List<FileNode> = withContext(Dispatchers.IO) {
        val targetParentId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, targetParentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
        )

        val nodes = mutableListOf<FileNode>()
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val flagsIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)

            while (cursor.moveToNext()) {
                val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Без имени" else "Без имени"
                if (!showHidden && name.startsWith(".")) continue
                val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "application/octet-stream" else "application/octet-stream"
                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                val modified = if (modIdx >= 0 && !cursor.isNull(modIdx)) cursor.getLong(modIdx) else 0L
                val flags = if (flagsIdx >= 0 && !cursor.isNull(flagsIdx)) cursor.getInt(flagsIdx) else 0

                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                nodes.add(
                    FileNode(
                        id = docId,
                        uri = docUri,
                        name = name,
                        isDirectory = isDir,
                        sizeBytes = size,
                        lastModifiedMs = modified,
                        mimeType = mime,
                        canWrite = (flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0 ||
                            (flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0,
                        canDelete = (flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0,
                        canRename = (flags and DocumentsContract.Document.FLAG_SUPPORTS_RENAME) != 0,
                        isLocalFile = false,
                        category = Formatters.classifyFile(name, mime, isDir)
                    )
                )
            }
        }
        sortNodes(nodes, sortField, sortOrder)
    }

    suspend fun listLocalDirectory(
        directory: File,
        showHidden: Boolean,
        sortField: SortField,
        sortOrder: SortOrder
    ): List<FileNode> = withContext(Dispatchers.IO) {
        if (!directory.exists()) directory.mkdirs()
        val files = directory.listFiles() ?: emptyArray()
        val nodes = files.mapNotNull { file ->
            if (!showHidden && file.name.startsWith(".")) return@mapNotNull null
            val isDir = file.isDirectory
            val mime = if (isDir) DocumentsContract.Document.MIME_TYPE_DIR else Formatters.guessMimeType(file.name)
            FileNode(
                id = file.absolutePath,
                uri = Uri.fromFile(file),
                name = file.name,
                isDirectory = isDir,
                sizeBytes = if (isDir) 0L else file.length(),
                lastModifiedMs = file.lastModified(),
                mimeType = mime,
                canWrite = file.canWrite(),
                canDelete = true,
                canRename = true,
                isLocalFile = true,
                category = Formatters.classifyFile(file.name, mime, isDir)
            )
        }
        sortNodes(nodes, sortField, sortOrder)
    }

    private fun sortNodes(
        nodes: List<FileNode>,
        field: SortField,
        order: SortOrder
    ): List<FileNode> {
        val comparator = Comparator<FileNode> { a, b ->
            // Folders always stay grouped at top for clean file manager ergonomics
            if (a.isDirectory != b.isDirectory) {
                return@Comparator if (a.isDirectory) -1 else 1
            }
            val cmp = when (field) {
                SortField.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                SortField.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
                SortField.DATE -> a.lastModifiedMs.compareTo(b.lastModifiedMs)
                SortField.TYPE -> {
                    val extA = a.name.substringAfterLast('.', "")
                    val extB = b.name.substringAfterLast('.', "")
                    val typeCmp = extA.compareTo(extB, ignoreCase = true)
                    if (typeCmp != 0) typeCmp else a.name.compareTo(b.name, ignoreCase = true)
                }
            }
            if (order == SortOrder.ASCENDING) cmp else -cmp
        }
        return nodes.sortedWith(comparator)
    }

    suspend fun createDirectory(
        treeUri: Uri?,
        parentDocId: String?,
        localParentDir: File?,
        name: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = name.trim()
            require(cleanName.isNotEmpty()) { "Имя папки не может быть пустым" }
            if (localParentDir != null) {
                val target = File(localParentDir, cleanName)
                check(!target.exists()) { "Папка уже существует" }
                check(target.mkdirs()) { "Не удалось создать папку" }
            } else {
                val tree = requireNotNull(treeUri) { "Накопитель не подключён" }
                val docId = parentDocId ?: DocumentsContract.getTreeDocumentId(tree)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                val created = DocumentsContract.createDocument(
                    resolver,
                    parentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    cleanName
                )
                check(created != null) { "Не удалось создать папку на USB" }
            }
        }
    }

    suspend fun createTextFile(
        treeUri: Uri?,
        parentDocId: String?,
        localParentDir: File?,
        name: String,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = name.trim().let { if (it.contains('.')) it else "$it.txt" }
            require(cleanName.isNotEmpty()) { "Имя файла не может быть пустым" }
            if (localParentDir != null) {
                val target = uniqueLocalFile(localParentDir, cleanName)
                target.writeText(content)
            } else {
                val tree = requireNotNull(treeUri) { "Накопитель не подключён" }
                val docId = parentDocId ?: DocumentsContract.getTreeDocumentId(tree)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                val mime = Formatters.guessMimeType(cleanName)
                val createdUri = DocumentsContract.createDocument(resolver, parentUri, mime, cleanName)
                    ?: error("Не удалось создать файл на USB")
                resolver.openOutputStream(createdUri, "wt")?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                } ?: error("Не удалось записать содержимое файла")
            }
        }
    }

    suspend fun renameNode(
        treeUri: Uri?,
        node: FileNode,
        newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = newName.trim()
            require(cleanName.isNotEmpty()) { "Новое имя не может быть пустым" }
            if (node.isLocalFile) {
                val file = File(node.id)
                val dest = File(file.parentFile, cleanName)
                check(!dest.exists()) { "Файл или папка с таким именем уже существует" }
                check(file.renameTo(dest)) { "Не удалось переименовать" }
            } else {
                requireNotNull(treeUri) { "Накопитель не подключён" }
                val renamedUri = DocumentsContract.renameDocument(resolver, node.uri, cleanName)
                check(renamedUri != null) { "Не удалось переименовать на USB" }
            }
        }
    }

    suspend fun deleteNodes(nodes: List<FileNode>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var deletedCount = 0
            nodes.forEach { node ->
                coroutineContext.ensureActive()
                val ok = if (node.isLocalFile) {
                    File(node.id).deleteRecursively()
                } else {
                    DocumentsContract.deleteDocument(resolver, node.uri)
                }
                if (ok) deletedCount++
            }
            deletedCount
        }
    }

    suspend fun readTextContent(node: FileNode, maxBytes: Int = 512_000): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = if (node.isLocalFile) {
                    File(node.id).inputStream().use { input ->
                        readUpTo(input, maxBytes)
                    }
                } else {
                    resolver.openInputStream(node.uri)?.use { input ->
                        readUpTo(input, maxBytes)
                    } ?: error("Не удалось открыть файл для чтения")
                }
                String(bytes, Charsets.UTF_8)
            }
        }

    suspend fun writeTextContent(node: FileNode, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (node.isLocalFile) {
                    File(node.id).writeText(content, Charsets.UTF_8)
                } else {
                    resolver.openOutputStream(node.uri, "wt")?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть файл для записи")
                }
            }
        }

    suspend fun importExternalUris(
        sourceUris: List<Uri>,
        targetTreeUri: Uri?,
        targetParentDocId: String?,
        targetLocalDir: File?,
        onProgress: (TransferProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val totalFiles = sourceUris.size
            var completed = 0
            var totalBytesKnown = 0L
            val metas = sourceUris.map { uri ->
                val (name, size, mime) = queryOpenableMeta(uri)
                totalBytesKnown += size.coerceAtLeast(0L)
                Triple(uri, name, mime)
            }
            var transferredSoFar = 0L

            for ((uri, rawName, mime) in metas) {
                coroutineContext.ensureActive()
                onProgress(
                    TransferProgress(
                        isActive = true,
                        operationTitle = "Копирование на накопитель",
                        currentFileName = rawName,
                        completedFiles = completed,
                        totalFiles = totalFiles,
                        bytesTransferred = transferredSoFar,
                        totalBytes = totalBytesKnown
                    )
                )

                if (targetLocalDir != null) {
                    val destFile = uniqueLocalFile(targetLocalDir, rawName)
                    resolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            transferredSoFar += copyStream(input, output) { delta ->
                                onProgress(
                                    TransferProgress(
                                        isActive = true,
                                        operationTitle = "Копирование в буфер",
                                        currentFileName = rawName,
                                        completedFiles = completed,
                                        totalFiles = totalFiles,
                                        bytesTransferred = transferredSoFar + delta,
                                        totalBytes = totalBytesKnown
                                    )
                                )
                            }
                        }
                    } ?: error("Не удалось прочитать $rawName")
                } else {
                    val tree = requireNotNull(targetTreeUri)
                    val parentId = targetParentDocId ?: DocumentsContract.getTreeDocumentId(tree)
                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(tree, parentId)
                    val destUri = DocumentsContract.createDocument(resolver, parentUri, mime, rawName)
                        ?: error("Не удалось создать $rawName на USB")
                    try {
                        resolver.openInputStream(uri)?.use { input ->
                            resolver.openOutputStream(destUri, "w")?.use { output ->
                                transferredSoFar += copyStream(input, output) { delta ->
                                    onProgress(
                                        TransferProgress(
                                            isActive = true,
                                            operationTitle = "Запись на USB-флешку",
                                            currentFileName = rawName,
                                            completedFiles = completed,
                                            totalFiles = totalFiles,
                                            bytesTransferred = transferredSoFar + delta,
                                            totalBytes = totalBytesKnown
                                        )
                                    )
                                }
                            } ?: error("Не удалось открыть поток записи для $rawName")
                        } ?: error("Не удалось прочитать $rawName")
                    } catch (e: Throwable) {
                        runCatching { DocumentsContract.deleteDocument(resolver, destUri) }
                        throw e
                    }
                }
                completed++
            }
            completed
        }
    }

    suspend fun copyOrMoveNodes(
        sources: List<FileNode>,
        sourceTreeUri: Uri?,
        targetTreeUri: Uri?,
        targetParentDocId: String?,
        targetLocalDir: File?,
        isMove: Boolean,
        onProgress: (TransferProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val totalFiles = countAllFiles(sources, sourceTreeUri)
            val totalBytes = sumAllBytes(sources, sourceTreeUri)
            var completed = 0
            var bytesDone = 0L

            suspend fun copyRecursive(
                node: FileNode,
                destTree: Uri?,
                destDocId: String?,
                destLocal: File?
            ) {
                coroutineContext.ensureActive()
                if (node.isDirectory) {
                    if (destLocal != null) {
                        val subDir = uniqueLocalFile(destLocal, node.name)
                        check(subDir.mkdirs()) { "Не удалось создать папку ${node.name}" }
                        val children = if (node.isLocalFile) {
                            listLocalDirectory(File(node.id), showHidden = true, SortField.NAME, SortOrder.ASCENDING)
                        } else {
                            listSafDirectory(
                                requireNotNull(sourceTreeUri),
                                node.id,
                                showHidden = true,
                                SortField.NAME,
                                SortOrder.ASCENDING
                            )
                        }
                        children.forEach { child ->
                            copyRecursive(child, null, null, subDir)
                        }
                    } else {
                        val tree = requireNotNull(destTree)
                        val pId = destDocId ?: DocumentsContract.getTreeDocumentId(tree)
                        val parentUri = DocumentsContract.buildDocumentUriUsingTree(tree, pId)
                        val newDirUri = DocumentsContract.createDocument(
                            resolver,
                            parentUri,
                            DocumentsContract.Document.MIME_TYPE_DIR,
                            node.name
                        ) ?: error("Не удалось создать папку ${node.name}")
                        val newDirDocId = DocumentsContract.getDocumentId(newDirUri)

                        val children = if (node.isLocalFile) {
                            listLocalDirectory(File(node.id), showHidden = true, SortField.NAME, SortOrder.ASCENDING)
                        } else {
                            listSafDirectory(
                                requireNotNull(sourceTreeUri),
                                node.id,
                                showHidden = true,
                                SortField.NAME,
                                SortOrder.ASCENDING
                            )
                        }
                        children.forEach { child ->
                            copyRecursive(child, tree, newDirDocId, null)
                        }
                    }
                } else {
                    val title = if (isMove) "Перемещение файлов" else "Копирование файлов"
                    onProgress(
                        TransferProgress(
                            isActive = true,
                            operationTitle = title,
                            currentFileName = node.name,
                            completedFiles = completed,
                            totalFiles = totalFiles,
                            bytesTransferred = bytesDone,
                            totalBytes = totalBytes
                        )
                    )
                    if (destLocal != null) {
                        val destFile = uniqueLocalFile(destLocal, node.name)
                        openNodeInputStream(node).use { input ->
                            destFile.outputStream().use { output ->
                                bytesDone += copyStream(input, output) { delta ->
                                    onProgress(
                                        TransferProgress(
                                            isActive = true,
                                            operationTitle = title,
                                            currentFileName = node.name,
                                            completedFiles = completed,
                                            totalFiles = totalFiles,
                                            bytesTransferred = bytesDone + delta,
                                            totalBytes = totalBytes
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        val tree = requireNotNull(destTree)
                        val pId = destDocId ?: DocumentsContract.getTreeDocumentId(tree)
                        val parentUri = DocumentsContract.buildDocumentUriUsingTree(tree, pId)
                        val mime = node.mimeType.takeIf { it.isNotBlank() } ?: Formatters.guessMimeType(node.name)
                        val destUri = DocumentsContract.createDocument(resolver, parentUri, mime, node.name)
                            ?: error("Не удалось создать ${node.name}")
                        try {
                            openNodeInputStream(node).use { input ->
                                resolver.openOutputStream(destUri, "w")?.use { output ->
                                    bytesDone += copyStream(input, output) { delta ->
                                        onProgress(
                                            TransferProgress(
                                                isActive = true,
                                                operationTitle = title,
                                                currentFileName = node.name,
                                                completedFiles = completed,
                                                totalFiles = totalFiles,
                                                bytesTransferred = bytesDone + delta,
                                                totalBytes = totalBytes
                                            )
                                        )
                                    }
                                } ?: error("Ошибка записи ${node.name}")
                            }
                        } catch (e: Throwable) {
                            runCatching { DocumentsContract.deleteDocument(resolver, destUri) }
                            throw e
                        }
                    }
                    completed++
                }
            }

            for (src in sources) {
                copyRecursive(src, targetTreeUri, targetParentDocId, targetLocalDir)
                if (isMove) {
                    if (src.isLocalFile) {
                        File(src.id).deleteRecursively()
                    } else {
                        DocumentsContract.deleteDocument(resolver, src.uri)
                    }
                }
            }
            sources.size
        }
    }

    fun getLocalStorageStats(dir: File): Pair<Long, Long> {
        return runCatching {
            val stat = StatFs(dir.absolutePath)
            stat.totalBytes to stat.availableBytes
        }.getOrDefault(0L to 0L)
    }

    fun getVolumeStorageStats(volume: RemovableVolumeInfo?): Pair<Long, Long>? {
        val total = volume?.totalBytes
        val free = volume?.freeBytes
        return if (total != null && free != null && total > 0L) {
            total to free
        } else {
            null
        }
    }

    private fun openNodeInputStream(node: FileNode): InputStream {
        return if (node.isLocalFile) {
            File(node.id).inputStream()
        } else {
            resolver.openInputStream(node.uri) ?: error("Не удалось открыть ${node.name}")
        }
    }

    private suspend fun countAllFiles(nodes: List<FileNode>, sourceTreeUri: Uri?): Int {
        var count = 0
        for (node in nodes) {
            if (!node.isDirectory) {
                count++
            } else {
                val children = if (node.isLocalFile) {
                    listLocalDirectory(File(node.id), true, SortField.NAME, SortOrder.ASCENDING)
                } else {
                    sourceTreeUri?.let {
                        listSafDirectory(it, node.id, true, SortField.NAME, SortOrder.ASCENDING)
                    } ?: emptyList()
                }
                count += countAllFiles(children, sourceTreeUri)
            }
        }
        return count.coerceAtLeast(1)
    }

    private suspend fun sumAllBytes(nodes: List<FileNode>, sourceTreeUri: Uri?): Long {
        var total = 0L
        for (node in nodes) {
            if (!node.isDirectory) {
                total += node.sizeBytes.coerceAtLeast(0L)
            } else {
                val children = if (node.isLocalFile) {
                    listLocalDirectory(File(node.id), true, SortField.NAME, SortOrder.ASCENDING)
                } else {
                    sourceTreeUri?.let {
                        listSafDirectory(it, node.id, true, SortField.NAME, SortOrder.ASCENDING)
                    } ?: emptyList()
                }
                total += sumAllBytes(children, sourceTreeUri)
            }
        }
        return total
    }

    private fun queryOpenableMeta(uri: Uri): Triple<String, Long, String> {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                    }
                }
        }
        val mime = resolver.getType(uri) ?: Formatters.guessMimeType(name)
        return Triple(name, size, mime)
    }

    private fun uniqueLocalFile(dir: File, desiredName: String): File {
        var candidate = File(dir, desiredName)
        if (!candidate.exists()) return candidate
        val dotIdx = desiredName.lastIndexOf('.')
        val base = if (dotIdx > 0) desiredName.substring(0, dotIdx) else desiredName
        val ext = if (dotIdx > 0) desiredName.substring(dotIdx) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($counter)$ext")
            counter++
        }
        return candidate
    }

    private fun readUpTo(input: InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        var total = 0
        while (total < maxBytes) {
            val read = input.read(buffer, 0, minOf(buffer.size, maxBytes - total))
            if (read <= 0) break
            out.write(buffer, 0, read)
            total += read
        }
        return out.toByteArray()
    }

    private suspend fun copyStream(
        input: InputStream,
        output: OutputStream,
        onBytesDelta: (Long) -> Unit
    ): Long {
        val buffer = ByteArray(64 * 1024)
        var totalForFile = 0L
        var lastEmitBytes = 0L
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
            totalForFile += read
            if (totalForFile - lastEmitBytes >= 128 * 1024) {
                lastEmitBytes = totalForFile
                onBytesDelta(totalForFile)
            }
        }
        output.flush()
        return totalForFile
    }
}
