package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.discovery.NsdTvDiscoveryManager
import com.example.model.CommandResult
import com.example.model.RemoteCommand
import com.example.model.TvDevice
import com.example.model.TvProtocol
import com.example.model.VirtualTvState
import com.example.network.TvNetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}

class TvRemoteRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val networkClient: TvNetworkClient = TvNetworkClient()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_tv_remote_prefs", Context.MODE_PRIVATE)

    val discoveryManager: NsdTvDiscoveryManager = NsdTvDiscoveryManager(context, scope)

    private val _connectedDevice = MutableStateFlow<TvDevice?>(null)
    val connectedDevice: StateFlow<TvDevice?> = _connectedDevice.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _recentLatency = MutableStateFlow<Long?>(null)
    val recentLatency: StateFlow<Long?> = _recentLatency.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<CommandResult>>(emptyList())
    val commandHistory: StateFlow<List<CommandResult>> = _commandHistory.asStateFlow()

    private val _lastResult = MutableStateFlow<CommandResult?>(null)
    val lastResult: StateFlow<CommandResult?> = _lastResult.asStateFlow()

    private val _virtualTvState = MutableStateFlow(VirtualTvState())
    val virtualTvState: StateFlow<VirtualTvState> = _virtualTvState.asStateFlow()

    private val _savedDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val savedDevices: StateFlow<List<TvDevice>> = _savedDevices.asStateFlow()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        loadSavedDevices()
        // Auto-select initial device (default to virtual demo TV for immediate out-of-the-box readiness)
        val initialDevice = _savedDevices.value.firstOrNull() ?: discoveryManager.discoveredDevices.value.firstOrNull()
        if (initialDevice != null) {
            connectToDevice(initialDevice)
        }
    }

    fun startDiscovery() {
        discoveryManager.startDiscovery()
    }

    fun stopDiscovery() {
        discoveryManager.stopDiscovery()
    }

    fun connectToDevice(device: TvDevice) {
        _connectedDevice.value = device
        _connectionStatus.value = ConnectionStatus.CONNECTED
        saveDevice(device)
    }

    fun disconnect() {
        _connectedDevice.value = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _recentLatency.value = null
    }

    /**
     * Executes remote control command with network transmission, tactile vibration, and result logging
     */
    suspend fun executeCommand(command: RemoteCommand): CommandResult {
        triggerHapticClick()

        val device = _connectedDevice.value
        if (device == null) {
            val failedResult = CommandResult(
                command = command,
                success = false,
                message = "No TV connected. Please select or add a TV.",
                latencyMs = 0
            )
            updateResult(failedResult)
            return failedResult
        }

        // Update virtual TV state if device is virtual
        if (device.isVirtual) {
            updateVirtualTvState(command)
        }

        // Send over network (or simulation)
        val result = networkClient.sendCommand(device, command)
        _recentLatency.value = result.latencyMs
        updateResult(result)
        return result
    }

    suspend fun sendTypedText(text: String): CommandResult {
        triggerHapticClick()
        val device = _connectedDevice.value
        if (device == null) {
            val failedResult = CommandResult(
                command = RemoteCommand.OK,
                success = false,
                message = "No TV connected.",
                latencyMs = 0
            )
            updateResult(failedResult)
            return failedResult
        }

        if (device.isVirtual) {
            val s = _virtualTvState.value
            _virtualTvState.value = s.copy(
                typedText = text,
                lastCommandReceived = "TYPE: \"$text\""
            )
        }

        val result = networkClient.sendTypedText(device, text)
        _recentLatency.value = result.latencyMs
        updateResult(result)
        return result
    }

    suspend fun testDevicePing(ip: String, port: Int): Pair<Boolean, Long> {
        return networkClient.pingDevice(ip, port)
    }

    suspend fun diagnoseTvConnection(ip: String, port: Int): String {
        if (port == 5555) {
            val adbTest = com.example.network.AdbTvClient.testAdbConnection(ip, port)
            when (adbTest.second) {
                com.example.network.AdbAuthStatus.CONNECTED -> return "সফল! TV Box কানেক্টেড (Port 5555 Active)"
                com.example.network.AdbAuthStatus.WAITING_FOR_USER_AUTH -> return "টিভি স্ক্রিনে 'Allow USB Debugging' ডায়ালগে OK/Allow চাপুন!"
                com.example.network.AdbAuthStatus.PORT_UNREACHABLE -> {
                    val openPorts = scanTvPorts(ip)
                    return if (openPorts.isNotEmpty()) {
                        val first = openPorts.first()
                        "Port 5555 বন্ধ, তবে Port ${first.first} (${first.second}) পাওয়া গেছে।"
                    } else {
                        "টিভি বক্সে 192.168.0.100 পাওয়া গেছে, কিন্তু Port 5555 বন্ধ। TV Settings > About > Build এ ৭ বার ক্লিক করে Developer Options থেকে Network / USB Debugging অন করুন।"
                    }
                }
                else -> {}
            }
        }
        val (ok, ms) = testDevicePing(ip, port)
        return if (ok) "Online! Response in ${ms}ms (Port $port)" else "Port $port unreachable on $ip"
    }

    suspend fun scanTvPorts(ip: String): List<Pair<Int, String>> {
        return networkClient.scanTvPorts(ip)
    }

    fun addManualDevice(name: String, ip: String, port: Int, protocol: TvProtocol): TvDevice {
        val newDevice = TvDevice(
            id = "$ip:$port",
            name = name.ifBlank { "Smart TV ($ip)" },
            ipAddress = ip,
            port = port,
            protocol = protocol,
            isVirtual = false,
            isSaved = true
        )
        saveDevice(newDevice)
        connectToDevice(newDevice)
        return newDevice
    }

    private fun updateResult(result: CommandResult) {
        _lastResult.value = result
        val currentList = _commandHistory.value.toMutableList()
        currentList.add(0, result)
        if (currentList.size > 20) {
            currentList.removeAt(currentList.lastIndex)
        }
        _commandHistory.value = currentList
    }

    private fun updateVirtualTvState(command: RemoteCommand) {
        val s = _virtualTvState.value
        val channelNames = listOf(
            1 to "CH 01 - News 24/7",
            2 to "CH 02 - Sports World",
            3 to "CH 03 - National Geo",
            4 to "CH 04 - Music Plus",
            5 to "CH 05 - Cinema HD",
            6 to "CH 06 - HBO Premiere",
            7 to "CH 07 - Anime Hub",
            8 to "CH 08 - Tech Network"
        )

        val updated = when (command) {
            RemoteCommand.POWER -> s.copy(isPoweredOn = !s.isPoweredOn, lastCommandReceived = "POWER")
            RemoteCommand.VOLUME_UP -> s.copy(volume = (s.volume + 5).coerceAtMost(100), isMuted = false, lastCommandReceived = "VOL +")
            RemoteCommand.VOLUME_DOWN -> s.copy(volume = (s.volume - 5).coerceAtLeast(0), lastCommandReceived = "VOL -")
            RemoteCommand.MUTE -> s.copy(isMuted = !s.isMuted, lastCommandReceived = if (!s.isMuted) "MUTED" else "UNMUTED")
            RemoteCommand.CHANNEL_UP -> {
                val nextCh = if (s.currentChannel >= 8) 1 else s.currentChannel + 1
                val name = channelNames.find { it.first == nextCh }?.second ?: "CH $nextCh"
                s.copy(currentChannel = nextCh, channelName = name, activeApp = "Live TV", lastCommandReceived = "CH + ($nextCh)")
            }
            RemoteCommand.CHANNEL_DOWN -> {
                val prevCh = if (s.currentChannel <= 1) 8 else s.currentChannel - 1
                val name = channelNames.find { it.first == prevCh }?.second ?: "CH $prevCh"
                s.copy(currentChannel = prevCh, channelName = name, activeApp = "Live TV", lastCommandReceived = "CH - ($prevCh)")
            }
            RemoteCommand.DPAD_UP -> s.copy(selectedMenuIndex = (s.selectedMenuIndex - 3).coerceAtLeast(0), lastCommandReceived = "UP")
            RemoteCommand.DPAD_DOWN -> s.copy(selectedMenuIndex = (s.selectedMenuIndex + 3).coerceAtMost(8), lastCommandReceived = "DOWN")
            RemoteCommand.DPAD_LEFT -> s.copy(selectedMenuIndex = (s.selectedMenuIndex - 1).coerceAtLeast(0), lastCommandReceived = "LEFT")
            RemoteCommand.DPAD_RIGHT -> s.copy(selectedMenuIndex = (s.selectedMenuIndex + 1).coerceAtMost(8), lastCommandReceived = "RIGHT")
            RemoteCommand.OK -> s.copy(lastCommandReceived = "OK (Item #${s.selectedMenuIndex + 1})")
            RemoteCommand.HOME -> s.copy(activeApp = "Home Screen", lastCommandReceived = "HOME")
            RemoteCommand.BACK -> s.copy(lastCommandReceived = "BACK")
            RemoteCommand.MENU -> s.copy(lastCommandReceived = "SETTINGS MENU")
            RemoteCommand.INPUT -> {
                val inputs = listOf("HDMI 1", "HDMI 2", "Live TV", "AV / Console")
                val curIdx = inputs.indexOf(s.activeApp)
                val nextApp = inputs[(if (curIdx >= 0) curIdx + 1 else 0) % inputs.size]
                s.copy(activeApp = nextApp, lastCommandReceived = "INPUT -> $nextApp")
            }
            RemoteCommand.APP_NETFLIX -> s.copy(activeApp = "Netflix 4K HDR", lastCommandReceived = "APP: Netflix")
            RemoteCommand.APP_YOUTUBE -> s.copy(activeApp = "YouTube TV", lastCommandReceived = "APP: YouTube")
            RemoteCommand.APP_PRIME -> s.copy(activeApp = "Amazon Prime Video", lastCommandReceived = "APP: Prime Video")
            RemoteCommand.APP_DISNEY -> s.copy(activeApp = "Disney+", lastCommandReceived = "APP: Disney+")
            RemoteCommand.APP_SPOTIFY -> s.copy(activeApp = "Spotify Music", lastCommandReceived = "APP: Spotify")
            RemoteCommand.APP_BROWSER -> s.copy(activeApp = "Web Browser", lastCommandReceived = "APP: Browser")
            RemoteCommand.PLAY_PAUSE -> s.copy(lastCommandReceived = "PLAY / PAUSE")
            RemoteCommand.REWIND -> s.copy(lastCommandReceived = "REWIND <<")
            RemoteCommand.FAST_FORWARD -> s.copy(lastCommandReceived = "FAST FORWARD >>")
            RemoteCommand.STOP -> s.copy(lastCommandReceived = "STOP")
            else -> s.copy(lastCommandReceived = command.label)
        }
        _virtualTvState.value = updated
    }

    private fun triggerHapticClick() {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(18)
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveDevice(device: TvDevice) {
        val current = _savedDevices.value.toMutableList()
        val index = current.indexOfFirst { it.ipAddress == device.ipAddress && it.port == device.port }
        val updatedDevice = device.copy(isSaved = true, lastConnected = System.currentTimeMillis())
        if (index >= 0) {
            current[index] = updatedDevice
        } else {
            current.add(0, updatedDevice)
        }
        _savedDevices.value = current
        persistSavedDevices(current)
    }

    private fun persistSavedDevices(list: List<TvDevice>) {
        try {
            val jsonArray = JSONArray()
            list.filterNot { it.isVirtual }.forEach { device ->
                val obj = JSONObject().apply {
                    put("id", device.id)
                    put("name", device.name)
                    put("ip", device.ipAddress)
                    put("port", device.port)
                    put("protocol", device.protocol.name)
                    put("serviceType", device.serviceType)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_tv_devices", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadSavedDevices() {
        try {
            val raw = prefs.getString("saved_tv_devices", null)
            if (raw == null) {
                // Prepopulate the user's Android TV Box
                val defaultTvBox = TvDevice(
                    id = "192.168.0.100:5555",
                    name = "Android TV Box",
                    ipAddress = "192.168.0.100",
                    port = 5555,
                    protocol = TvProtocol.ANDROID_TV_TCP,
                    isSaved = true
                )
                _savedDevices.value = listOf(defaultTvBox)
                return
            }
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<TvDevice>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val protocol = try {
                    TvProtocol.valueOf(obj.optString("protocol", TvProtocol.ANDROID_TV_TCP.name))
                } catch (_: Exception) {
                    TvProtocol.ANDROID_TV_TCP
                }
                list.add(
                    TvDevice(
                        id = obj.optString("id", "${obj.optString("ip")}:${obj.optInt("port")}"),
                        name = obj.optString("name", "Saved TV"),
                        ipAddress = obj.optString("ip"),
                        port = obj.optInt("port", 5555),
                        protocol = protocol,
                        serviceType = obj.optString("serviceType", ""),
                        isSaved = true
                    )
                )
            }
            if (list.isEmpty()) {
                val defaultTvBox = TvDevice(
                    id = "192.168.0.100:5555",
                    name = "Android TV Box",
                    ipAddress = "192.168.0.100",
                    port = 5555,
                    protocol = TvProtocol.ANDROID_TV_TCP,
                    isSaved = true
                )
                list.add(defaultTvBox)
            }
            _savedDevices.value = list
        } catch (_: Exception) {}
    }
}
