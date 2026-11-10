package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import com.example.ui.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvDevice
import com.example.model.TvProtocol
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ButtonBorder
import com.example.ui.theme.ButtonSurface
import com.example.ui.theme.ButtonSurfacePressed
import com.example.ui.theme.RemoteBody
import com.example.ui.theme.RemoteCard
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoverySheet(
    sheetState: SheetState,
    isScanning: Boolean,
    discoveredDevices: List<TvDevice>,
    savedDevices: List<TvDevice>,
    connectedDevice: TvDevice?,
    onSelectDevice: (TvDevice) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onOpenManualDialog: () -> Unit,
    onOpenTvBoxGuide: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RemoteBody,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart TV Discovery",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isScanning) "Scanning local Wi-Fi & mDNS..." else "Select a TV to connect",
                        color = if (isScanning) AccentCyan else TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "spin")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "spin_angle"
                    )

                    IconButton(
                        onClick = {
                            if (isScanning) onStopScan() else onStartScan()
                        },
                        modifier = Modifier.testTag("refresh_discovery_button")
                    ) {
                        Icon(
                            imageVector = AppIcons.Refresh,
                            contentDescription = "Scan",
                            tint = AccentCyan,
                            modifier = if (isScanning) Modifier.rotate(angle) else Modifier
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row: TV Box Easy Connect & Manual IP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenTvBoxGuide,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_box_guide_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00384D),
                        contentColor = AccentCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = AppIcons.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("টিভি বক্স সহজ কানেক্ট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenManualDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_ip_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonSurface,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ButtonBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual IP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // List of Devices
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(discoveredDevices) { device ->
                    val isConnected = connectedDevice?.id == device.id
                    DeviceListItem(
                        device = device,
                        isConnected = isConnected,
                        onClick = { onSelectDevice(device) }
                    )
                }

                if (discoveredDevices.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = AppIcons.Wifi,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Searching for TVs on Wi-Fi...",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Make sure your Smart TV is connected to the same Wi-Fi network",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DeviceListItem(
    device: TvDevice,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("device_item_${device.id}"),
        shape = RoundedCornerShape(12.dp),
        color = if (isConnected) Color(0xFF1B2A40) else RemoteCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConnected) AccentCyan else ButtonBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF00384D) else ButtonSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Tv,
                        contentDescription = null,
                        tint = if (isConnected) AccentCyan else TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = device.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${device.ipAddress}:${device.port}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "•",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = device.protocol.displayName,
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isConnected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusGreen)
                    )
                    Text(
                        text = "Connected",
                        color = StatusGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
