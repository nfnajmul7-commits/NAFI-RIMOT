package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

    val isBluetoothMode by viewModel.isBluetoothMode.collectAsState()
    val bluetoothStatus by viewModel.bluetoothStatus.collectAsState()
    val bluetoothDeviceName by viewModel.bluetoothDeviceName.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.initBluetoothProfile()
            viewModel.makePhoneDiscoverable()
            viewModel.connectBluetoothTv("b4:04:18:97:7b:c3")
        }
    }

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
                // 1. Top Bar: [ ← Device Name ] [ ● Status ]   [ Power (Red) ]
                ExactTopBar(
                    deviceName = if (isBluetoothMode) (bluetoothDeviceName ?: "Android TV Box (Bluetooth)") else (connectedDevice?.name ?: "Android TV Box"),
                    isConnected = if (isBluetoothMode) (bluetoothStatus == com.example.bluetooth.BluetoothStatus.CONNECTED) else (connectionStatus == ConnectionStatus.CONNECTED && connectedDevice?.isVirtual == false),
                    statusText = if (isBluetoothMode) "Bluetooth HID" else if (connectedDevice?.isVirtual == true) "Virtual Demo" else (connectedDevice?.ipAddress ?: "192.168.0.100"),
                    onBackOrDeviceClick = { viewModel.showDiscoverySheet(true) },
                    onPowerClick = { viewModel.sendCommand(RemoteCommand.POWER) }
                )

                // 2. Dual-Mode Connection Selector Card: [ ᛒ ব্লুটুথ রিমোট (TV Box) ]  [ 📶 Wi-Fi ADB ]
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF141C2B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isBluetoothMode) {
                            if (bluetoothStatus == com.example.bluetooth.BluetoothStatus.CONNECTED) StatusGreen else AccentCyan.copy(alpha = 0.5f)
                        } else {
                            if (connectionStatus == ConnectionStatus.CONNECTED && connectedDevice?.isVirtual == false) StatusGreen else AccentCyan.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        // Switcher pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0A0F18))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Bluetooth Mode Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isBluetoothMode) AccentCyan else Color.Transparent)
                                    .clickable { viewModel.setBluetoothMode(true) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ᛒ ব্লুটুথ রিমোট (TV Box)",
                                    color = if (isBluetoothMode) Color.Black else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Wi-Fi Mode Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isBluetoothMode) AccentCyan else Color.Transparent)
                                    .clickable { viewModel.setBluetoothMode(false) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📶 Wi-Fi ADB (৫৫৫৫)",
                                    color = if (!isBluetoothMode) Color.Black else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isBluetoothMode) {
                            // BLUETOOTH VIEW
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (bluetoothStatus) {
                                                    com.example.bluetooth.BluetoothStatus.CONNECTED -> StatusGreen
                                                    com.example.bluetooth.BluetoothStatus.CONNECTING,
                                                    com.example.bluetooth.BluetoothStatus.REGISTERING -> Color(0xFFFFB300)
                                                    else -> Color(0xFF8899A6)
                                                }
                                            )
                                    )
                                    Column {
                                        Text(
                                            text = when (bluetoothStatus) {
                                                com.example.bluetooth.BluetoothStatus.CONNECTED -> "টিভিতে সংযুক্ত: ${bluetoothDeviceName ?: "Android TV"}"
                                                com.example.bluetooth.BluetoothStatus.CONNECTING -> "সংযোগ হচ্ছে..."
                                                com.example.bluetooth.BluetoothStatus.REGISTERING -> "প্রোফাইল প্রস্তুত হচ্ছে..."
                                                else -> "ব্লুটুথ প্রস্তুত (টিভি বক্সে পেয়ার করুন)"
                                            },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "TV MAC: b4:04:18:97:7b:c3 • HID Accessory",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.BLUETOOTH_CONNECT,
                                                    Manifest.permission.BLUETOOTH_ADVERTISE,
                                                    Manifest.permission.BLUETOOTH_SCAN
                                                )
                                            )
                                        } else {
                                            viewModel.makePhoneDiscoverable()
                                            viewModel.connectBluetoothTv("b4:04:18:97:7b:c3")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (bluetoothStatus == com.example.bluetooth.BluetoothStatus.CONNECTED) Color(0xFF00384D) else AccentCyan,
                                        contentColor = if (bluetoothStatus == com.example.bluetooth.BluetoothStatus.CONNECTED) AccentCyan else Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (bluetoothStatus == com.example.bluetooth.BluetoothStatus.CONNECTED) "সংযুক্ত" else "ᛒ পেয়ার করুন",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0B1422),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "👉 টিভির Remotes & Accessories মেনুতে \"Add accessory\" চাপুন। ফোনের ডিসকভার আসলে Allow দিন।",
                                    color = AccentCyan.copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        } else {
                            // WI-FI VIEW
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (connectionStatus == ConnectionStatus.CONNECTED && connectedDevice?.isVirtual == false)
                                                    StatusGreen
                                                else
                                                    PowerRed
                                            )
                                    )
                                    Column {
                                        Text(
                                            text = connectedDevice?.let { "${it.name} (${it.ipAddress})" } ?: "Android TV (192.168.0.100)",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (connectionStatus == ConnectionStatus.CONNECTED && connectedDevice?.isVirtual == false)
                                                "ADB Port ${connectedDevice?.port ?: 5555} • সংযুক্ত"
                                            else
                                                "ADB Port 5555 • টেস্ট বাটন চাপুন",
                                            color = if (connectionStatus == ConnectionStatus.CONNECTED && connectedDevice?.isVirtual == false)
                                                StatusGreen
                                            else
                                                AccentCyan,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val ip = connectedDevice?.ipAddress ?: "192.168.0.100"
                                            viewModel.autoDetectAndConnect(ip)
                                        },
                                        enabled = !uiState.isTestingPing,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00557A),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        if (uiState.isTestingPing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("⚡ অটো-ডিটেক্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val ip = connectedDevice?.ipAddress ?: "192.168.0.100"
                                            val port = connectedDevice?.port ?: 5555
                                            viewModel.testPing(ip, port)
                                        },
                                        enabled = !uiState.isTestingPing,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00384D),
                                            contentColor = AccentCyan
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("টেস্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.showTvBoxGuide(true) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentCyan,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("গাইড", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Ping Test Result Toast / Banner
                uiState.pingTestResult?.let { result ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E283D),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (result.contains("সফল") || result.contains("Online")) StatusGreen else Color(0xFFFF9100)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = result,
                            color = if (result.contains("সফল") || result.contains("Online")) StatusGreen else Color(0xFFFFCC80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
