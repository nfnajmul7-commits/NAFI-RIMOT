package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConnectionStatus
import com.example.model.CommandResult
import com.example.model.RemoteCommand
import com.example.ui.components.AppLauncherCarousel
import com.example.ui.components.DeviceDiscoverySheet
import com.example.ui.components.ExactBottomMediaBar
import com.example.ui.components.ExactCentralControlCluster
import com.example.ui.components.ExactHomeBackRow
import com.example.ui.components.ModeSelectorPillsRow
import com.example.ui.components.NumpadCluster
import com.example.ui.components.TopUtilityActionsRow
import com.example.ui.components.TouchpadSurface
import com.example.ui.components.TvBoxGuideDialog
import com.example.ui.components.TvSettingsDialog
import com.example.ui.components.TypeDialog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.PowerRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvRemoteScreen(
    viewModel: TvRemoteViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val recentLatency by viewModel.recentLatency.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val virtualTvState by viewModel.virtualTvState.collectAsState()

    var activeCenterMode by remember { mutableStateOf("DPAD") } // "DPAD", "TOUCHPAD", "NUMBERS"

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0C101A)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 500.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Bar: [ ← Device Name (Living Room) ] [ ● Connected • Volume + ]   [ Power (Red) ]
                ExactTopBar(
                    deviceName = connectedDevice?.name ?: "Living Room",
                    isConnected = connectionStatus == ConnectionStatus.CONNECTED && connectedDevice != null,
                    statusText = if (lastResult != null) lastResult?.command?.label ?: "Volume +" else "Volume +",
                    onBackOrDeviceClick = { viewModel.showDiscoverySheet(true) },
                    onPowerClick = { viewModel.sendCommand(RemoteCommand.POWER) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Top 4 Utility Buttons: Menu, Settings, Hold to talk, Type
                TopUtilityActionsRow(
                    onCommand = { viewModel.sendCommand(it) },
                    onOpenSettings = { viewModel.showSettingsDialog(true) },
                    onOpenType = { viewModel.showTypeDialog(true) },
                    onHoldToTalk = {
                        viewModel.showTvBoxGuide(true)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. App Launcher Carousel: YouTube, Netflix, Prime Video, Disney+, Spotify, Edit
                AppLauncherCarousel(
                    onCommand = { viewModel.sendCommand(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Mode Selector Pill Bar: [ D-Pad | Touchpad ]       [ Numbers ]
                ModeSelectorPillsRow(
                    isTouchpadActive = activeCenterMode == "TOUCHPAD",
                    isNumbersActive = activeCenterMode == "NUMBERS",
                    onSelectDpad = { activeCenterMode = "DPAD" },
                    onSelectTouchpad = { activeCenterMode = "TOUCHPAD" },
                    onSelectNumbers = { activeCenterMode = "NUMBERS" }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Main Center Controller Section (D-Pad + Rockers OR Touchpad OR Numbers)
                when (activeCenterMode) {
                    "DPAD" -> {
                        ExactCentralControlCluster(
                            onCommand = { viewModel.sendCommand(it) }
                        )
                    }
                    "TOUCHPAD" -> {
                        TouchpadSurface(
                            onCommand = { viewModel.sendCommand(it) }
                        )
                    }
                    "NUMBERS" -> {
                        NumpadCluster(
                            onCommand = { viewModel.sendCommand(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Home & Back Wide Pill Buttons: [ ⌂ Home ]   [ ← Back ]
                ExactHomeBackRow(
                    onCommand = { viewModel.sendCommand(it) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 7. Bottom Media Controls Bar: [ Mute ] [ << ] [ ▶ (Cyan) ] [ >> ] [ ⓘ ]
                ExactBottomMediaBar(
                    onCommand = { viewModel.sendCommand(it) },
                    isMuted = virtualTvState.isMuted
                )

                // Optional Command Feedback Banner
                lastResult?.let { result ->
                    Spacer(modifier = Modifier.height(6.dp))
                    CompactCommandFeedbackHud(result = result)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 8. Typing / Keyboard Dialog (টাইপিং এর ব্যবস্থা)
    if (uiState.showTypeDialog) {
        TypeDialog(
            onSendText = { text -> viewModel.sendTypedText(text) },
            onSendCommand = { cmd -> viewModel.sendCommand(cmd) },
            onDismiss = { viewModel.showTypeDialog(false) }
        )
    }

    // Device Discovery Bottom Sheet
    if (uiState.showDiscoverySheet) {
        DeviceDiscoverySheet(
            sheetState = sheetState,
            isScanning = isScanning,
            discoveredDevices = discoveredDevices,
            savedDevices = savedDevices,
            connectedDevice = connectedDevice,
            onSelectDevice = { viewModel.selectDevice(it) },
            onStartScan = { viewModel.startScanning() },
            onStopScan = { viewModel.stopScanning() },
            onOpenManualDialog = {
                viewModel.showDiscoverySheet(false)
                viewModel.showSettingsDialog(true)
            },
            onOpenTvBoxGuide = {
                viewModel.showDiscoverySheet(false)
                viewModel.showTvBoxGuide(true)
            },
            onDismiss = { viewModel.showDiscoverySheet(false) }
        )
    }

    // TV Box Setup Guide Dialog
    if (uiState.showTvBoxGuide) {
        TvBoxGuideDialog(
            subnetPrefix = viewModel.getSubnetPrefix(),
            userWifiIp = viewModel.getUserWifiIp(),
            isTestingPing = uiState.isTestingPing,
            pingResult = uiState.pingTestResult,
            onTestPing = { ip, port -> viewModel.testPing(ip, port) },
            onConnect = { name, ip, port, protocol ->
                viewModel.addManualDevice(name, ip, port, protocol)
                viewModel.showTvBoxGuide(false)
            },
            onDismiss = { viewModel.showTvBoxGuide(false) }
        )
    }

    // Manual TV Settings Dialog
    if (uiState.showSettingsDialog) {
        TvSettingsDialog(
            initialIp = connectedDevice?.ipAddress ?: viewModel.getSubnetPrefix(),
            initialPort = connectedDevice?.port ?: 8060,
            initialName = connectedDevice?.name ?: "Living Room TV",
            initialProtocol = connectedDevice?.protocol ?: com.example.model.TvProtocol.ROKU_ECP,
            isTestingPing = uiState.isTestingPing,
            pingResult = uiState.pingTestResult,
            onTestPing = { ip, port -> viewModel.testPing(ip, port) },
            onSaveAndConnect = { name, ip, port, protocol ->
                viewModel.addManualDevice(name, ip, port, protocol)
            },
            onDismiss = { viewModel.showSettingsDialog(false) }
        )
    }
}

/**
 * Top bar matching the reference image:
 * [ ← Device Name ]
 * [ ● Connected • Volume + ]
 * [ Power (Red) ]
 */
@Composable
fun ExactTopBar(
    deviceName: String,
    isConnected: Boolean,
    statusText: String,
    onBackOrDeviceClick: () -> Unit,
    onPowerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back Arrow + Device Title + Subtitle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clickable(onClick = onBackOrDeviceClick)
                .testTag("top_bar_device_selector")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Select TV",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )

            Column {
                Text(
                    text = deviceName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) StatusGreen else PowerRed)
                    )
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        color = Color(0xFF9EAFC7),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        color = Color(0xFF6B7E99),
                        fontSize = 12.sp
                    )
                    Text(
                        text = statusText,
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Right: Circular Power Button (Red)
        Surface(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(onClick = onPowerClick)
                .testTag("top_power_button"),
            shape = CircleShape,
            color = Color(0xFF1F121C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A1A28))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Power,
                    contentDescription = "Power",
                    tint = PowerRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CompactCommandFeedbackHud(
    result: CommandResult,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (result.success) Color(0xFF0F1E30) else Color(0xFF2E0F17)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (result.success) Icons.Default.CheckCircle else AppIcons.ErrorOutline,
                    contentDescription = null,
                    tint = if (result.success) StatusGreen else PowerRed,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${result.command.label}: ${result.message}",
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${result.latencyMs}ms",
                color = AccentCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
