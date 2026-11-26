package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FileNode
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.ui.components.LocalChromeBackdrop
import com.example.ui.components.liquidGlass
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GraphitePrimary
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkSlateGray
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters
import com.example.viewmodel.TextViewerState

private val GlassSheetColor = Color(0xFFF2F4F8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionBottomSheet(
    targetLabel: String,
    onPickPhotosAndVideos: () -> Unit,
    onPickDocuments: () -> Unit,
    onCreateFolder: () -> Unit,
    onCreateTextFile: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GlassSheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Добавить в «$targetLabel»",
                style = MaterialTheme.typography.titleLarge,
                color = InkCharcoal,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
            SheetOptionItem(
                icon = Icons.Default.PhotoLibrary,
                title = "Фото и видео с телефона",
                subtitle = "Импорт снимков и роликов через системную галерею",
                onClick = { onDismiss(); onPickPhotosAndVideos() },
                tag = "add_media_option"
            )
            SheetOptionItem(
                icon = Icons.Default.UploadFile,
                title = "Любые файлы и документы",
                subtitle = "Копировать PDF, архивы, музыку, APK с телефона",
                onClick = { onDismiss(); onPickDocuments() },
                tag = "add_files_option"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFD5DBE4))
            SheetOptionItem(
                icon = Icons.Default.CreateNewFolder,
                title = "Создать папку",
                subtitle = "Новый каталог на накопителе",
                onClick = { onDismiss(); onCreateFolder() },
                tag = "create_folder_option"
            )
            SheetOptionItem(
                icon = Icons.Default.NoteAdd,
                title = "Создать текстовый файл",
                subtitle = "Новая заметка или конфигурация (.txt, .md, .json)",
                onClick = { onDismiss(); onCreateTextFile() },
                tag = "create_text_option"
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeActionSheet(
    node: FileNode,
    quickSendLabel: String?,
    onQuickSend: () -> Unit,
    onOpen: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onProperties: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GlassSheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.titleLarge,
                color = InkCharcoal,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                text = if (node.isDirectory) "Папка" else "${Formatters.formatBytes(node.sizeBytes)} • ${node.mimeType}",
                style = MaterialTheme.typography.labelMedium,
                color = InkSlateGray,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (quickSendLabel != null) {
                SheetOptionItem(
                    icon = Icons.AutoMirrored.Filled.Send,
                    title = quickSendLabel,
                    onClick = { onDismiss(); onQuickSend() }
                )
            }
            SheetOptionItem(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = if (node.isDirectory) "Открыть папку" else "Открыть / Просмотр",
                onClick = { onDismiss(); onOpen() }
            )
            if (!node.isDirectory) {
                SheetOptionItem(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = "Открыть внешним приложением",
                    onClick = { onDismiss(); onOpenExternal() }
                )
                SheetOptionItem(
                    icon = Icons.Default.Share,
                    title = "Поделиться",
                    onClick = { onDismiss(); onShare() }
                )
            }
            SheetOptionItem(
                icon = Icons.Default.ContentCopy,
                title = "Копировать",
                onClick = { onDismiss(); onCopy() }
            )
            SheetOptionItem(
                icon = Icons.Default.ContentCut,
                title = "Переместить (Вырезать)",
                onClick = { onDismiss(); onCut() }
            )
            if (node.canRename) {
                SheetOptionItem(
                    icon = Icons.Default.DriveFileRenameOutline,
                    title = "Переименовать",
                    onClick = { onDismiss(); onRename() }
                )
            }
            SheetOptionItem(
                icon = Icons.Default.Info,
                title = "Свойства",
                onClick = { onDismiss(); onProperties() }
            )
            if (node.canDelete) {
                SheetOptionItem(
                    icon = Icons.Default.Delete,
                    title = "Удалить",
                    tint = CoralDanger,
                    onClick = { onDismiss(); onDelete() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SheetOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = InkCharcoal,
    tag: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        },
        headlineContent = {
            Text(text = title, color = tint, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = subtitle?.let {
            { Text(text = it, style = MaterialTheme.typography.bodySmall, color = InkSlateGray) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOptionsSheet(
    currentField: SortField,
    currentOrder: SortOrder,
    onSelect: (SortField, SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GlassSheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Сортировка", style = MaterialTheme.typography.titleLarge, color = InkCharcoal)
            SortField.entries.forEach { field ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(field, currentOrder)
                            onDismiss()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = field == currentField,
                        onClick = {
                            onSelect(field, currentOrder)
                            onDismiss()
                        }
                    )
                    Text(field.label, style = MaterialTheme.typography.bodyLarge, color = InkCharcoal)
                }
            }
            HorizontalDivider(color = Color(0xFFD5DBE4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onSelect(currentField, SortOrder.ASCENDING); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentOrder == SortOrder.ASCENDING) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                    }
                    Text("По возрастанию")
                }
                OutlinedButton(
                    onClick = { onSelect(currentField, SortOrder.DESCENDING); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentOrder == SortOrder.DESCENDING) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                    }
                    Text("По убыванию")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSheetColor,
        title = { Text("Новая папка", color = InkCharcoal) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название папки") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("folder_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GraphitePrimary, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_create_folder")
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = InkSlateGray) }
        }
    )
}

@Composable
fun CreateTextFileDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("note.txt") }
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSheetColor,
        title = { Text("Новый текстовый файл", color = InkCharcoal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя файла") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Содержимое") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, content) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GraphitePrimary, contentColor = Color.White)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = InkSlateGray) }
        }
    )
}

@Composable
fun RenameNodeDialog(
    node: FileNode,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(node) { mutableStateOf(node.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSheetColor,
        title = { Text("Переименовать", color = InkCharcoal) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Новое имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != node.name,
                colors = ButtonDefaults.buttonColors(containerColor = GraphitePrimary, contentColor = Color.White)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = InkSlateGray) }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    nodes: List<FileNode>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val label = if (nodes.size == 1) {
        "«${nodes.first().name}»"
    } else {
        "выбранные объекты (${nodes.size})"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSheetColor,
        title = { Text("Удалить?", color = InkCharcoal) },
        text = {
            Text("Вы действительно хотите удалить $label? Действие необратимо.", color = InkSlateGray)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CoralDanger, contentColor = Color.White)
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = InkSlateGray) }
        }
    )
}

@Composable
fun PropertiesDialog(
    node: FileNode,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSheetColor,
        title = { Text("Свойства", color = InkCharcoal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropLine("Имя", node.name)
                PropLine("Тип", if (node.isDirectory) "Каталог" else node.mimeType)
                if (!node.isDirectory) {
                    PropLine("Размер", "${Formatters.formatBytes(node.sizeBytes)} (${node.sizeBytes} Б)")
                }
                PropLine("Изменён", Formatters.formatDateTime(node.lastModifiedMs))
                PropLine("Источник", if (node.isLocalFile) "Буфер телефона" else "USB OTG / SAF")
                PropLine("Запись", if (node.canWrite) "Разрешена" else "Только чтение")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = InkCharcoal) }
        }
    )
}

@Composable
private fun PropLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSlateGray)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = InkCharcoal)
    }
}

@Composable
fun ImagePreviewDialog(
    node: FileNode,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val chromeBackdrop = LocalChromeBackdrop.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF141518))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(node.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = node.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
                    .liquidGlass(
                        backdrop = chromeBackdrop,
                        shape = RoundedCornerShape(22.dp),
                        fillAlpha = 0.65f,
                        useLens = true
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = InkCharcoal)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(node.name, color = InkCharcoal, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(Formatters.formatBytes(node.sizeBytes), color = InkSlateGray, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onShare, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = InkCharcoal)
                    }
                    IconButton(onClick = onOpenExternal, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Открыть через", tint = InkCharcoal)
                    }
                }
            }
        }
    }
}

@Composable
fun TextViewerDialog(
    state: TextViewerState,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isEditing by remember(state.node.id) { mutableStateOf(false) }
    var draft by remember(state.content) { mutableStateOf(state.content) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            color = Color(0xFFF1F3F7)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = InkCharcoal)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.node.name, style = MaterialTheme.typography.titleMedium, color = InkCharcoal, maxLines = 1)
                        Text(
                            text = if (isEditing) "Редактирование" else "Просмотр • ${Formatters.formatBytes(state.node.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSlateGray
                        )
                    }
                    if (!state.isLoading && state.error == null && state.node.canWrite) {
                        if (isEditing) {
                            IconButton(
                                onClick = {
                                    onSave(draft)
                                    isEditing = false
                                },
                                enabled = !state.isSaving,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Сохранить", tint = InkCharcoal)
                            }
                        } else {
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = InkCharcoal)
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFD5DBE4))

                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                    when {
                        state.isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GraphitePrimary)
                        }
                        state.error != null -> {
                            Text(
                                text = state.error,
                                color = CoralDanger,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        isEditing -> {
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = InkCharcoal
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = state.content.ifEmpty { "(Пустой файл)" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = InkCharcoal,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
}
