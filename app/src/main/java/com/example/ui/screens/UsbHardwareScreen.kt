package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.EventLevel
import com.example.model.RemovableVolumeInfo
import com.example.model.UsbEventLogEntry
import com.example.model.UsbHardwareDevice
import com.example.ui.components.LocalGlassBackdrop
import com.example.ui.components.liquidGlass
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.GraphitePrimary
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.InkSlateGray
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberContainer
import com.example.util.Formatters

@Composable
fun UsbHardwareScreen(
    usbDevices: List<UsbHardwareDevice>,
    removableVolumes: List<RemovableVolumeInfo>,
    eventLog: List<UsbEventLogEntry>,
    onRefresh: () -> Unit,
    onRequestUsbPermission: (Int) -> Unit,
    onGrantOrOpenVolume: (RemovableVolumeInfo) -> Unit,
    onUnlinkVolume: (RemovableVolumeInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(22.dp),
                        fillAlpha = 0.48f,
                        blurRadius = 8.dp,
                        useLens = true
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("USB OTG Монитор", style = MaterialTheme.typography.headlineSmall, color = InkCharcoal)
                        Text(
                            "Диагностика адаптера, устройств и томов",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSlateGray
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hardware_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = InkCharcoal)
                    }
                }
            }
        }

        item {
            Text(
                text = "Съёмные тома (${removableVolumes.size})",
                style = MaterialTheme.typography.titleMedium,
                color = InkCharcoal
            )
        }

        if (removableVolumes.isEmpty()) {
            item {
                EmptyHardwareCard(
                    icon = Icons.Default.SdStorage,
                    title = "Нет смонтированных съёмных томов",
                    subtitle = "Вставьте USB-флешку через OTG-переходник, чтобы увидеть её файловую систему."
                )
            }
        } else {
            items(removableVolumes, key = { it.key }) { vol ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(20.dp),
                            fillAlpha = 0.46f,
                            blurRadius = 8.dp,
                            useLens = true
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SdStorage, contentDescription = null, tint = InkCharcoal)
                                Column {
                                    Text(vol.description, style = MaterialTheme.typography.titleMedium, color = InkCharcoal)
                                    Text(
                                        text = "UUID: ${vol.uuid ?: "—"} • ${vol.state}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSlateGray
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (vol.isMounted) EmeraldContainer else WarmAmberContainer
                            ) {
                                Text(
                                    text = if (vol.isMounted) "Смонтирован" else vol.state,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (vol.isMounted) EmeraldReady else WarmAmber,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (vol.totalBytes != null && vol.freeBytes != null) {
                            Text(
                                text = "Свободно ${Formatters.formatBytes(vol.freeBytes)} из ${Formatters.formatBytes(vol.totalBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSlateGray
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onGrantOrOpenVolume(vol) },
                                enabled = vol.isMounted,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GraphitePrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(if (vol.persistedTreeUri != null) "Открыть в менеджере" else "Выдать доступ")
                            }
                            if (vol.persistedTreeUri != null) {
                                OutlinedButton(
                                    onClick = { onUnlinkVolume(vol) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InkCharcoal)
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Сбросить")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "USB-устройства (UsbManager: ${usbDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                color = InkCharcoal
            )
        }

        if (usbDevices.isEmpty()) {
            item {
                EmptyHardwareCard(
                    icon = Icons.Default.Usb,
                    title = "USB-устройства не обнаружены",
                    subtitle = "Когда вы подключите флешку через OTG, здесь отобразятся её VID/PID, производитель и класс интерфейса."
                )
            }
        } else {
            items(usbDevices, key = { it.deviceId }) { dev ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(20.dp),
                            fillAlpha = 0.46f,
                            blurRadius = 8.dp,
                            useLens = true
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = InkCharcoal)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dev.productName, style = MaterialTheme.typography.titleMedium, color = InkCharcoal)
                                Text(
                                    dev.manufacturerName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSlateGray
                                )
                            }
                        }
                        Text(
                            text = "${Formatters.formatUsbIds(dev.vendorId, dev.productId)} • Интерфейсов: ${dev.interfaceCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkCharcoal
                        )
                        Text(
                            text = if (dev.hasMassStorageInterface) {
                                "Mass Storage (Флеш-накопитель)"
                            } else {
                                Formatters.describeUsbClass(dev.deviceClass)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSlateGray
                        )
                        if (!dev.hasPermission) {
                            OutlinedButton(
                                onClick = { onRequestUsbPermission(dev.deviceId) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = InkCharcoal)
                            ) {
                                Text("Запросить USB-разрешение")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Журнал событий OTG",
                style = MaterialTheme.typography.titleMedium,
                color = InkCharcoal
            )
        }

        items(eventLog) { entry ->
            val badgeColor = when (entry.level) {
                EventLevel.SUCCESS -> EmeraldReady
                EventLevel.WARNING -> WarmAmber
                EventLevel.INFO -> InkCharcoal
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(16.dp),
                        fillAlpha = 0.40f,
                        blurRadius = 5.dp,
                        useLens = false
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Formatters.formatTime(entry.timestampMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleSmall, color = InkCharcoal)
                        Text(
                            entry.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSlateGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHardwareCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val backdrop = LocalGlassBackdrop.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(20.dp),
                fillAlpha = 0.42f,
                blurRadius = 6.dp,
                useLens = false
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = InkSlateGray)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = InkCharcoal)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSlateGray)
            }
        }
    }
}
