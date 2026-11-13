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
import kotlinx.coroutines.launch
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
    val bluetoothManager: com.example.bluetooth.BluetoothRemoteManager =
        com.example.bluetooth.BluetoothRemoteManager(context, scope)

    private val _isBluetoothMode = MutableStateFlow(false)
    val isBluetoothMode: StateFlow<Boolean> = _isBluetoothMode.asStateFlow()

    private val _pairingRequiredIp = MutableStateFlow<String?>(null)
    val pairingRequiredIp: StateFlow<String?> = _pairingRequiredIp.asStateFlow()

    private val _pairingMessage = MutableStateFlow<String?>(null)
    val pairingMessage: StateFlow<String?> = _pairingMessage.asStateFlow()

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
        com.example.network.AdbTvClient.init(context)
        com.example.network.AndroidTvRemoteV2Client.init(context)
        loadSavedDevices()

        // Sync connection state with Bluetooth HID profile when in Bluetooth mode
        scope.launch {
            bluetoothManager.status.collect { status ->
                if (_isBluetoothMode.value) {
                    _connectionStatus.value = when (status) {
                        com.example.bluetooth.BluetoothStatus.CONNECTED -> ConnectionStatus.CONNECTED
                        com.example.bluetooth.BluetoothStatus.CONNECTING,
                        com.example.bluetooth.BluetoothStatus.REGISTERING -> ConnectionStatus.CONNECTING
                        com.example.bluetooth.BluetoothStatus.DISCONNECTED,
                        com.example.bluetooth.BluetoothStatus.DISABLED,
                        com.example.bluetooth.BluetoothStatus.UNSUPPORTED -> ConnectionStatus.DISCONNECTED
                    }
                }
            }
        }

        scope.launch {
            bluetoothManager.connectedDeviceName.collect { name ->
                if (_isBluetoothMode.value && name != null) {
                    val current = _connectedDevice.value
                    if (current != null) {
                        _connectedDevice.value = current.copy(name = name)
                    }
                }
            }
        }

        // Auto-select initial device (default to Android TV Box 192.168.0.100 on port 5555)
        val rawInitial = _savedDevices.value.firstOrNull() ?: discoveryManager.discoveredDevices.value.firstOrNull()
        val initialDevice = if (rawInitial != null && rawInitial.ipAddress == "192.168.0.100" && rawInitial.port != 5555) {
            rawInitial.copy(port = 5555, protocol = TvProtocol.ANDROID_TV_TCP)
        } else {
            rawInitial
        }
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
        _isBluetoothMode.value = (device.protocol == TvProtocol.BLUETOOTH_HID)
        _connectedDevice.value = device
        _connectionStatus.value = ConnectionStatus.CONNECTED
        saveDevice(device)
    }

    fun setBluetoothMode(enabled: Boolean) {
        _isBluetoothMode.value = enabled
        if (enabled) {
            val devName = bluetoothManager.connectedDeviceName.value ?: "Android TV Box"
            val btDevice = TvDevice(
                id = "bt_tv_box",
                name = devName,
                ipAddress = "Bluetooth HID",
                port = 0,
                protocol = TvProtocol.BLUETOOTH_HID,
                isSaved = true
            )
            _connectedDevice.value = btDevice
            _connectionStatus.value = if (bluetoothManager.status.value == com.example.bluetooth.BluetoothStatus.CONNECTED) {
                ConnectionStatus.CONNECTED
            } else {
                ConnectionStatus.CONNECTING
            }
        } else {
            val wifiDevice = _savedDevices.value.firstOrNull { it.protocol != TvProtocol.BLUETOOTH_HID }
                ?: TvDevice(
                    id = "192.168.0.106:5555",
                    name = "TX500PRO TV Box",
                    ipAddress = "192.168.0.106",
                    port = 5555,
                    protocol = TvProtocol.ANDROID_TV_TCP,
                    isSaved = true
                )
            connectToDevice(wifiDevice)
        }
    }

    fun makePhoneDiscoverable() {
        bluetoothManager.makePhoneDiscoverable()
    }

    fun connectBluetoothAddress(address: String): Boolean {
        return bluetoothManager.connectByAddress(address)
    }

    fun disconnect() {
        _connectedDevice.value = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _recentLatency.value = null
    }

    /**
     * Executes remote control command with network or Bluetooth transmission, tactile vibration, and result logging
     */
    suspend fun executeCommand(command: RemoteCommand): CommandResult {
        triggerHapticClick()

        val device = _connectedDevice.value
        val useBluetooth = _isBluetoothMode.value || device?.protocol == TvProtocol.BLUETOOTH_HID

        // Send via Bluetooth HID if in Bluetooth mode
        if (useBluetooth) {
            val result = bluetoothManager.sendCommand(command)
            _recentLatency.value = result.latencyMs
            updateResult(result)
            return result
        }

        if (device == null) {
            val failedResult = CommandResult(
                command = command,
                success = false,
                message = "টিভি কানেক্ট করা নেই। Wi-Fi অথবা ব্লুটুথ সিলেক্ট করুন।",
                latencyMs = 0
            )
            updateResult(failedResult)
            return failedResult
        }

        // Update virtual TV state if device is virtual
        if (device.isVirtual) {
            updateVirtualTvState(command)
        }

        // Send over network (ADB socket / HTTP)
        val result = networkClient.sendCommand(device, command)
        _recentLatency.value = result.latencyMs
        updateResult(result)
        return result
    }

    suspend fun sendTypedText(text: String): CommandResult {
        triggerHapticClick()
        val device = _connectedDevice.value
        val useBluetooth = _isBluetoothMode.value || device?.protocol == TvProtocol.BLUETOOTH_HID

        if (useBluetooth) {
            val result = bluetoothManager.sendTypedText(text)
            _recentLatency.value = result.latencyMs
            updateResult(result)
            return result
        }

        if (device == null) {
            val failedResult = CommandResult(
                command = RemoteCommand.OK,
                success = false,
                message = "টিভি কানেক্ট করা নেই।",
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

    suspend fun startGoogleTvPairing(ip: String): Pair<Boolean, String> {
        val res = com.example.network.AndroidTvRemoteV2Client.startPairing(ip)
        if (res.first) {
            _pairingRequiredIp.value = ip
            _pairingMessage.value = res.second
        } else {
            _pairingMessage.value = res.second
        }
        return res
    }

    suspend fun submitGoogleTvPin(ip: String, code: String): Pair<Boolean, String> {
        val res = com.example.network.AndroidTvRemoteV2Client.completePairing(ip, code)
        if (res.first) {
            _pairingRequiredIp.value = null
            _pairingMessage.value = null
            val dev = TvDevice(
                id = "$ip:6466",
                name = "Google / Android TV ($ip)",
                ipAddress = ip,
                port = 6466,
                protocol = TvProtocol.ANDROID_TV_REMOTE_V2,
                isSaved = true
            )
            connectToDevice(dev)
        } else {
            _pairingMessage.value = res.second
        }
        return res
    }

    fun cancelGoogleTvPairing() {
        com.example.network.AndroidTvRemoteV2Client.cancelPairing()
        _pairingRequiredIp.value = null
        _pairingMessage.value = null
    }

    suspend fun autoDetectAndConnect(ip: String): Pair<Boolean, String> {
        val candidateIps = linkedSetOf<String>()
        if (ip.isNotBlank() && ip != "127.0.0.1" && ip != "Bluetooth HID") {
            candidateIps.add(ip.trim())
        }
        candidateIps.add("192.168.0.106")
        candidateIps.add("192.168.0.100")

        // 1. Fast check on known candidate IPs first (ADB 5555, Google TV 6466/6467, China ports)
        for (targetIp in candidateIps) {
            val found = probeAndConnectSingleIp(targetIp)
            if (found != null) return found
        }

        // 2. Full Wi-Fi subnet sweep (1..254) in parallel (~2s) to find any TV box whose DHCP IP changed!
        val subnetDevices = discoveryManager.scanSubnetNow()
        val firstTv = subnetDevices.firstOrNull { !it.isVirtual }
        if (firstTv != null) {
            val found = probeAndConnectSingleIp(firstTv.ipAddress)
            if (found != null) return found

            connectToDevice(firstTv)
            return Pair(true, "সফল! ${firstTv.name} পাওয়া গেছে (${firstTv.ipAddress}:${firstTv.port})।")
        }

        return Pair(
            false,
            "Wi-Fi তে কোনো খোলা রিমোট পোর্ট পাওয়া যায়নি। সমাধান:\n১) টিভির Settings ➔ About ➔ Build এ ৭ বার ক্লিক করে Developer Options ➔ USB Debugging অন করুন এবং টিভি রিস্টার্ট দিন।\n২) অথবা উপরের 'ᛒ ব্লুটুথ রিমোট' ট্যাবে টিভি বক্স পেয়ার করুন।"
        )
    }

    private suspend fun probeAndConnectSingleIp(targetIp: String): Pair<Boolean, String>? {
        // Check Google TV / Android TV Remote v2 (Ports 6466/6467)
        val openPorts = networkClient.scanTvPorts(targetIp)
        if (openPorts.isEmpty()) return null

        val hasGoogleTvRemote = openPorts.any { it.first == 6466 || it.first == 6467 }
        val hasAdb = openPorts.any { it.first == 5555 }

        if (hasAdb) {
            val adbTest = com.example.network.AdbTvClient.testAdbConnection(targetIp, 5555)
            if (adbTest.first) {
                val dev = TvDevice(
                    id = "$targetIp:5555",
                    name = "Android / China TV Box ($targetIp)",
                    ipAddress = targetIp,
                    port = 5555,
                    protocol = TvProtocol.ANDROID_TV_TCP,
                    isSaved = true
                )
                connectToDevice(dev)
                val msg = if (adbTest.second == com.example.network.AdbAuthStatus.WAITING_FOR_USER_AUTH) {
                    "পাওয়া গেছে ($targetIp:5555)! টিভি স্ক্রিনে 'Allow USB Debugging' ডায়ালগে Always allow টিক দিয়ে OK চাপুন।"
                } else {
                    "সফল! টিভি বক্স কানেক্টেড ($targetIp • ADB Port 5555)"
                }
                return Pair(true, msg)
            }
        }

        if (hasGoogleTvRemote) {
            val v2Status = com.example.network.AndroidTvRemoteV2Client.testConnection(targetIp)
            val dev = TvDevice(
                id = "$targetIp:6466",
                name = "Google / Android TV ($targetIp)",
                ipAddress = targetIp,
                port = 6466,
                protocol = TvProtocol.ANDROID_TV_REMOTE_V2,
                isSaved = true
            )
            connectToDevice(dev)
            if (v2Status.first == com.example.network.AtvV2Status.CONNECTED) {
                return Pair(true, "সফল! Google / Android TV কানেক্টেড ($targetIp:6466)")
            } else if (v2Status.first == com.example.network.AtvV2Status.NEEDS_PAIRING) {
                startGoogleTvPairing(targetIp)
                return Pair(true, "টিভি পাওয়া গেছে ($targetIp)! স্ক্রিনে আসা ৬ অক্ষরের পিন কোড দিন।")
            }
        }

        // Other open port (8008 Chromecast, 8060 Roku, 8080 REST, 55555 China socket)
        val (foundPort, serviceName) = openPorts.first()
        val proto = when (foundPort) {
            8060 -> TvProtocol.ROKU_ECP
            8008, 8080 -> TvProtocol.UNIVERSAL_REST
            else -> TvProtocol.ANDROID_TV_TCP
        }
        val autoDev = TvDevice(
            id = "$targetIp:$foundPort",
            name = "TV Box ($targetIp)",
            ipAddress = targetIp,
            port = foundPort,
            protocol = proto,
            isSaved = true
        )
        connectToDevice(autoDev)
        return Pair(true, "সফল! $serviceName পাওয়া গেছে ($targetIp:$foundPort)।")
    }

    suspend fun diagnoseTvConnection(ip: String, port: Int): String {
        val result = autoDetectAndConnect(ip)
        return result.second
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
                // Prepopulate the user's Android TV Box (TX500PRO at 192.168.0.106)
                val defaultTvBox = TvDevice(
                    id = "192.168.0.106:5555",
                    name = "TX500PRO TV Box",
                    ipAddress = "192.168.0.106",
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
                val ip = obj.optString("ip")
                var port = obj.optInt("port", 5555)
                var protocol = try {
                    TvProtocol.valueOf(obj.optString("protocol", TvProtocol.ANDROID_TV_TCP.name))
                } catch (_: Exception) {
                    TvProtocol.ANDROID_TV_TCP
                }
                if ((ip == "192.168.0.100" || ip == "192.168.0.106") && protocol == TvProtocol.ANDROID_TV_TCP) {
                    if (port == 8080 || port == 8060 || port == 0) {
                        port = 5555
                    }
                }
                list.add(
                    TvDevice(
                        id = "$ip:$port",
                        name = obj.optString("name", "Android TV Box"),
                        ipAddress = ip,
                        port = port,
                        protocol = protocol,
                        serviceType = obj.optString("serviceType", ""),
                        isSaved = true
                    )
                )
            }
            if (list.isEmpty()) {
                val defaultTvBox = TvDevice(
                    id = "192.168.0.106:5555",
                    name = "TX500PRO TV Box",
                    ipAddress = "192.168.0.106",
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
