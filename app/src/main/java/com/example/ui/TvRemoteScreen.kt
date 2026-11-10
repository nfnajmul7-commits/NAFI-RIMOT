package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.DPadCluster
import com.example.ui.components.DeviceDiscoverySheet
import com.example.ui.components.MediaPlaybackRow
import com.example.ui.components.NumpadCluster
import com.example.ui.components.QuickAppsGrid
import com.example.ui.components.SystemNavRow
import com.example.ui.components.TactileIconButton
import com.example.ui.components.TouchpadSurface
import com.example.ui.components.TvSettingsDialog
import com.example.ui.components.VirtualTvPreview
import com.example.ui.components.VolumeChannelRockers
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentCyanGlow
import com.example.ui.theme.ButtonBorder
import com.example.ui.theme.ButtonSurface
import com.example.ui.theme.DarkBg
import com.example.ui.theme.PowerRed
import com.example.ui.theme.PowerRedGlow
import com.example.ui.theme.RemoteBody
import com.example.ui.theme.RemoteCard
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val commandHistory by viewModel.commandHistory.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val virtualTvState by viewModel.virtualTvState.collectAsState()

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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0E18),
                            Color(0xFF101726),
                            Color(0xFF0A0E18)
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar (Status Pill, Cast discovery, TV monitor toggle, Power button)
                RemoteHeaderBar(
                    connectedDevice = connectedDevice,
                    connectionStatus = connectionStatus,
                    latencyMs = recentLatency,
                    showVirtualTv = uiState.showVirtualTvScreen,
                    onOpenDiscovery = { viewModel.showDiscoverySheet(true) },
                    onOpenSettings = { viewModel.showSettingsDialog(true) },
                    onToggleVirtualTv = { viewModel.toggleVirtualTvScreen() },
                    onPowerClick = { viewModel.sendCommand(RemoteCommand.POWER) }
                )

                // Virtual TV Monitor Display (Live reactive visual preview)
                AnimatedVisibility(
                    visible = uiState.showVirtualTvScreen,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    VirtualTvPreview(
                        tvState = virtualTvState,
                        tvName = connectedDevice?.name ?: "Demo Smart TV",
                        ipAddress = connectedDevice?.ipAddress ?: "127.0.0.1",
                        latencyMs = recentLatency
                    )
                }

                // Command Feedback HUD Banner
                lastResult?.let { result ->
                    CommandFeedbackHud(result = result)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Navigation Mode Tabs
                RemoteModeTabRow(
                    selectedTab = uiState.activeTab,
                    onTabSelected = { viewModel.setActiveTab(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Active Mode Content
                when (uiState.activeTab) {
                    RemoteTab.NAVIGATION -> {
                        NavigationTabContent(
                            onCommand = { viewModel.sendCommand(it) },
                            isMuted = virtualTvState.isMuted
                        )
                    }
                    RemoteTab.NUMPAD -> {
                        NumpadCluster(
                            onCommand = { viewModel.sendCommand(it) },
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    RemoteTab.APPS -> {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "STREAMING & INPUT SHORTCUTS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                            QuickAppsGrid(
                                onCommand = { viewModel.sendCommand(it) }
                            )
                        }
                    }
                    RemoteTab.TOUCHPAD -> {
                        TouchpadSurface(
                            onCommand = { viewModel.sendCommand(it) },
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    RemoteTab.LOGS -> {
                        CommandLogsView(commandHistory = commandHistory)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
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
            onDismiss = { viewModel.showDiscoverySheet(false) }
        )
    }

    // Manual TV Settings Dialog
    if (uiState.showSettingsDialog) {
        TvSettingsDialog(
            initialIp = connectedDevice?.ipAddress ?: "192.168.1.",
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
 * Top remote control bar with TV connection status, power, cast, settings
 */
@Composable
fun RemoteHeaderBar(
    connectedDevice: com.example.model.TvDevice?,
    connectionStatus: ConnectionStatus,
    latencyMs: Long?,
    showVirtualTv: Boolean,
    onOpenDiscovery: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleVirtualTv: () -> Unit,
    onPowerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TV Connection Pill (Click to open discovery)
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenDiscovery)
                .testTag("connection_status_pill"),
            shape = RoundedCornerShape(20.dp),
            color = ButtonSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ButtonBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isOnline = connectionStatus == ConnectionStatus.CONNECTED && connectedDevice != null
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) StatusGreen else PowerRed)
                )

                Text(
                    text = connectedDevice?.name ?: "Tap to Connect TV",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )

                Icon(
                    imageVector = if (isOnline) Icons.Default.CastConnected else Icons.Default.Cast,
                    contentDescription = "Cast Discovery",
                    tint = if (isOnline) AccentCyan else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Action Buttons: Monitor toggle, Settings, Power
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // TV Screen Toggle
            IconButton(
                onClick = onToggleVirtualTv,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("toggle_virtual_tv_button")
            ) {
                Icon(
                    imageVector = if (showVirtualTv) Icons.Default.Tv else Icons.Default.TvOff,
                    contentDescription = "Toggle TV Monitor",
                    tint = if (showVirtualTv) AccentCyan else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Settings / Manual IP
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("open_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Main Power Button
            TactileIconButton(
                icon = Icons.Default.PowerSettingsNew,
                contentDescription = "Power",
                onClick = onPowerClick,
                size = 46.dp,
                iconSize = 24.dp,
                contentColor = PowerRed,
                glowColor = PowerRedGlow,
                testTag = "remote_power_button"
            )
        }
    }
}

/**
 * Animated Command Feedback Toast
 */
@Composable
fun CommandFeedbackHud(
    result: CommandResult,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (result.success) Color(0xFF132238) else Color(0xFF33151D),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (result.success) AccentCyan.copy(alpha = 0.5f) else PowerRed.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (result.success) StatusGreen else PowerRed,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = result.command.label,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• ${result.message}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${result.latencyMs}ms",
                color = AccentCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Controller Mode Tab Selector
 */
@Composable
fun RemoteModeTabRow(
    selectedTab: RemoteTab,
    onTabSelected: (RemoteTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = AccentCyan,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = AccentCyan,
                height = 2.dp
            )
        },
        modifier = modifier.testTag("remote_mode_tabs")
    ) {
        RemoteTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.label,
                        color = if (selectedTab == tab) AccentCyan else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

/**
 * Standard Navigation Layout: D-Pad, System Nav, Volume/Channel Rockers, Media Controls
 */
@Composable
fun NavigationTabContent(
    onCommand: (RemoteCommand) -> Unit,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // D-Pad Cluster
        DPadCluster(
            onCommand = onCommand,
            modifier = Modifier.padding(top = 8.dp)
        )

        // System Nav Row (Back, Home, Menu)
        SystemNavRow(onCommand = onCommand)

        Spacer(modifier = Modifier.height(4.dp))

        // Volume and Channel Rockers
        VolumeChannelRockers(
            onCommand = onCommand,
            isMuted = isMuted
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Media Playback Row
        MediaPlaybackRow(onCommand = onCommand)
    }
}

/**
 * Network Command Execution History View
 */
@Composable
fun CommandLogsView(
    commandHistory: List<CommandResult>,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "RECENT PACKET TRANSMISSIONS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        if (commandHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RemoteCard),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No commands sent yet.\nPress any remote button to transmit commands.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RemoteCard)
                    .border(1.dp, ButtonBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(commandHistory) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF141C2A)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.command.name,
                                        color = if (item.success) AccentCyan else PowerRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "• ${item.message}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = timeFormat.format(Date(item.timestamp)),
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Text(
                                text = "${item.latencyMs}ms",
                                color = if (item.success) StatusGreen else PowerRed,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
