package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.BreadcrumbEntry
import com.example.model.ClipboardMode
import com.example.model.ClipboardState
import com.example.model.FileCategory
import com.example.model.FileNode
import com.example.model.TransferProgress
import com.example.model.ViewMode
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.GraphitePrimary
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkMutedGray
import com.example.ui.theme.InkSlateGray
import com.example.ui.theme.SilverPillBg
import com.example.ui.theme.WarmAmber
import com.example.util.Formatters

@Composable
fun StorageStatusHeaderCard(
    title: String,
    subtitleBadge: String,
    isUsb: Boolean,
    itemCount: Int,
    totalBytes: Long?,
    freeBytes: Long?,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    val hasStats = totalBytes != null && freeBytes != null && totalBytes > 0L
    val usedBytes = if (hasStats) (totalBytes!! - freeBytes!!).coerceAtLeast(0L) else 0L
    val targetFraction = if (hasStats) (usedBytes.toFloat() / totalBytes!!.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(),
        label = "storage_bar"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(24.dp),
                fillAlpha = 0.46f,
                blurRadius = 8.dp,
                useLens = true,
                refractionHeight = 14.dp,
                refractionAmount = 26.dp
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.62f))
                        .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUsb) Icons.Default.Usb else Icons.Default.Folder,
                        contentDescription = null,
                        tint = InkCharcoal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = InkCharcoal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Файлов и папок: $itemCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSlateGray,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isUsb) EmeraldContainer.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = subtitleBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUsb) EmeraldReady else InkCharcoal,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (hasStats) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (animatedProgress > 0.88f) WarmAmber else GraphitePrimary,
                        trackColor = Color(0xFFC8CED8).copy(alpha = 0.65f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Занято ${Formatters.formatBytes(usedBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSlateGray,
                            maxLines = 1
                        )
                        Text(
                            text = "Свободно ${Formatters.formatBytes(freeBytes!!)} / ${Formatters.formatBytes(totalBytes!!)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkCharcoal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddActionRow(
    onPickPhotosAndVideos: () -> Unit,
    onPickDocuments: () -> Unit,
    onCreateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickGlassActionChip(
            icon = Icons.Default.PhotoLibrary,
            label = "Фото/Видео",
            onClick = onPickPhotosAndVideos,
            backdrop = backdrop,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_add_media")
        )
        QuickGlassActionChip(
            icon = Icons.Default.UploadFile,
            label = "Файлы",
            onClick = onPickDocuments,
            backdrop = backdrop,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_add_files")
        )
        QuickGlassActionChip(
            icon = Icons.Default.CreateNewFolder,
            label = "Папка",
            onClick = onCreateFolder,
            backdrop = backdrop,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_add_folder")
        )
    }
}

@Composable
private fun QuickGlassActionChip(
    icon: ImageVector,
    label: String,
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
                fillAlpha = 0.42f,
                blurRadius = 6.dp,
                useLens = true,
                refractionHeight = 10.dp,
                refractionAmount = 18.dp
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InkCharcoal,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = InkCharcoal,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BreadcrumbStrip(
    breadcrumbs: List<BreadcrumbEntry>,
    onCrumbClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, crumb ->
            val isLast = index == breadcrumbs.lastIndex
            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .liquidGlass(
                        backdrop = backdrop,
                        shape = shape,
                        fillAlpha = if (isLast) 0.62f else 0.36f,
                        blurRadius = 5.dp,
                        useLens = false
                    )
                    .clip(shape)
                    .clickable(enabled = !isLast) { onCrumbClick(index) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (index == 0) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = InkCharcoal
                        )
                    }
                    Text(
                        text = crumb.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLast) InkCharcoal else InkSlateGray,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!isLast) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = InkSlateGray,
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ExplorerToolbar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    viewMode: ViewMode,
    showHidden: Boolean,
    onToggleViewMode: () -> Unit,
    onOpenSort: () -> Unit,
    onToggleHidden: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Row 1: Full-width Liquid Glass Search Pill (never squeezes or wraps vertically)
        val searchShape = RoundedCornerShape(24.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .liquidGlass(
                    backdrop = backdrop,
                    shape = searchShape,
                    fillAlpha = 0.48f,
                    blurRadius = 8.dp,
                    useLens = true,
                    refractionHeight = 12.dp,
                    refractionAmount = 22.dp
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = InkSlateGray,
                modifier = Modifier.size(20.dp)
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Поиск по файлам и папкам…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMutedGray,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = InkCharcoal),
                    cursorBrush = SolidColor(InkCharcoal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("file_search_input")
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Очистить поиск",
                        tint = InkCharcoal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Row 2: Compact Liquid Glass Action Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarGlassButton(
                icon = Icons.AutoMirrored.Filled.Sort,
                label = "Сортировка",
                active = false,
                onClick = onOpenSort,
                backdrop = backdrop,
                modifier = Modifier
                    .weight(1f)
                    .testTag("sort_button")
            )
            ToolbarGlassButton(
                icon = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                label = if (viewMode == ViewMode.LIST) "Сетка" else "Список",
                active = false,
                onClick = onToggleViewMode,
                backdrop = backdrop,
                modifier = Modifier
                    .weight(1f)
                    .testTag("view_mode_button")
            )
            ToolbarGlassButton(
                icon = if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                label = "Скрытые",
                active = showHidden,
                onClick = onToggleHidden,
                backdrop = backdrop,
                modifier = Modifier
                    .weight(1f)
                    .testTag("hidden_files_button")
            )
            val refreshShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .liquidGlass(
                        backdrop = backdrop,
                        shape = refreshShape,
                        fillAlpha = 0.42f,
                        blurRadius = 5.dp,
                        useLens = false
                    )
                    .clip(refreshShape)
                    .clickable(onClick = onRefresh)
                    .testTag("refresh_files_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить",
                    tint = InkCharcoal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolbarGlassButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                fillAlpha = if (active) 0.65f else 0.40f,
                blurRadius = 5.dp,
                useLens = false
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InkCharcoal,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = InkCharcoal,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRowItem(
    node: FileNode,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                fillAlpha = if (isSelected) 0.68f else 0.44f,
                blurRadius = 6.dp,
                useLens = false,
                borderAlpha = if (isSelected) 1f else 0.72f
            )
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("file_row_${node.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FileThumbnailOrIcon(node = node, sizeDp = 46)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkCharcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (node.isDirectory) "Папка" else Formatters.formatBytes(node.sizeBytes),
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSlateGray
                    )
                    if (node.lastModifiedMs > 0L) {
                        Text(
                            text = "• ${Formatters.formatDateTime(node.lastModifiedMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMutedGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (selectionMode) {
                IconButton(onClick = onClick, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Выбрано" else "Выбрать",
                        tint = if (isSelected) InkCharcoal else InkSlateGray
                    )
                }
            } else {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("more_${node.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Действия с ${node.name}",
                        tint = InkSlateGray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    node: FileNode,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                fillAlpha = if (isSelected) 0.68f else 0.44f,
                blurRadius = 6.dp,
                useLens = false
            )
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("file_grid_${node.name}")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.52f)),
                contentAlignment = Alignment.Center
            ) {
                if (node.category == FileCategory.IMAGE) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(node.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = node.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val (icon, tint) = categoryVisuals(node.category)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(38.dp)
                    )
                }

                if (selectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) InkCharcoal else InkSlateGray,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                    )
                } else {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Действия",
                            tint = InkCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = node.name,
                style = MaterialTheme.typography.titleSmall,
                color = InkCharcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (node.isDirectory) "Папка" else Formatters.formatBytes(node.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = InkSlateGray
            )
        }
    }
}

@Composable
fun FileThumbnailOrIcon(node: FileNode, sizeDp: Int) {
    val (icon, tint) = categoryVisuals(node.category)
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.68f))
            .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (node.category == FileCategory.IMAGE) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(node.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = node.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size((sizeDp * 0.52f).dp)
            )
        }
    }
}

@Composable
fun categoryVisuals(category: FileCategory): Pair<ImageVector, Color> {
    return when (category) {
        FileCategory.FOLDER -> Icons.Default.Folder to Color(0xFF475569)
        FileCategory.IMAGE -> Icons.Default.Image to Color(0xFF334155)
        FileCategory.VIDEO -> Icons.Default.VideoFile to Color(0xFF3F3F46)
        FileCategory.AUDIO -> Icons.Default.AudioFile to Color(0xFF1F7A53)
        FileCategory.DOCUMENT -> Icons.Default.Description to Color(0xFF3B495E)
        FileCategory.ARCHIVE -> Icons.Default.FolderZip to Color(0xFF7C5826)
        FileCategory.CODE_TEXT -> Icons.Default.Code to Color(0xFF2C2F36)
        FileCategory.APK -> Icons.Default.Android to Color(0xFF2E6B47)
        FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile to InkSlateGray
    }
}

@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    quickSendLabel: String?,
    onQuickSend: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                fillAlpha = 0.62f,
                blurRadius = 8.dp,
                useLens = true
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClear, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.Close, contentDescription = "Снять выделение", tint = InkCharcoal)
                }
                Text(
                    text = "Выбрано: $selectedCount",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = InkCharcoal,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSelectAll, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Выбрать всё", tint = InkCharcoal)
                }
                IconButton(onClick = onCopy, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = InkCharcoal)
                }
                IconButton(onClick = onCut, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Переместить", tint = InkCharcoal)
                }
                IconButton(onClick = onShare, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = InkCharcoal)
                }
                IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = CoralDanger
                    )
                }
            }
            if (quickSendLabel != null) {
                Button(
                    onClick = onQuickSend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GraphitePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(quickSendLabel)
                }
            }
        }
    }
}

@Composable
fun ClipboardFloatingBanner(
    clipboard: ClipboardState,
    onPasteHere: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    AnimatedVisibility(visible = !clipboard.isEmpty, modifier = modifier) {
        val shape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    backdrop = backdrop,
                    shape = shape,
                    fillAlpha = 0.58f,
                    blurRadius = 8.dp,
                    useLens = true
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    tint = InkCharcoal
                )
                Column(modifier = Modifier.weight(1f)) {
                    val modeText = if (clipboard.mode == ClipboardMode.COPY) "Копирование" else "Перемещение"
                    Text(
                        text = "$modeText (${clipboard.items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkCharcoal
                    )
                    Text(
                        text = "Из: ${clipboard.sourceLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSlateGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onPasteHere,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GraphitePrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("paste_clipboard_button")
                ) {
                    Text("Вставить")
                }
                IconButton(onClick = onClear, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.Close, contentDescription = "Отменить буфер", tint = InkCharcoal)
                }
            }
        }
    }
}

@Composable
fun TransferProgressDialog(
    progress: TransferProgress,
    onCancel: () -> Unit
) {
    if (!progress.isActive) return
    val ratio = if (progress.totalBytes > 0L) {
        (progress.bytesTransferred.toFloat() / progress.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else if (progress.totalFiles > 0) {
        progress.completedFiles.toFloat() / progress.totalFiles.toFloat()
    } else 0f

    AlertDialog(
        onDismissRequest = {},
        containerColor = Color(0xFFF3F5F9),
        title = {
            Text(progress.operationTitle, style = MaterialTheme.typography.titleLarge, color = InkCharcoal)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = progress.currentFileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkCharcoal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GraphitePrimary,
                    trackColor = SilverPillBg
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Файл ${progress.completedFiles + 1} из ${progress.totalFiles.coerceAtLeast(1)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSlateGray
                    )
                    if (progress.totalBytes > 0L) {
                        Text(
                            text = "${Formatters.formatBytes(progress.bytesTransferred)} / ${Formatters.formatBytes(progress.totalBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkCharcoal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = CoralDanger)
            ) {
                Text("Отмена")
            }
        }
    )
}
