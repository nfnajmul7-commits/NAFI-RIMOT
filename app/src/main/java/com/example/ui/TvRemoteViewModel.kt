package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConnectionStatus
import com.example.data.TvRemoteRepository
import com.example.model.CommandResult
import com.example.model.RemoteCommand
import com.example.model.TvDevice
import com.example.model.TvProtocol
import com.example.model.VirtualTvState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RemoteTab(val label: String) {
    NAVIGATION("Navigation"),
    NUMPAD("Numpad"),
    APPS("Quick Apps"),
    TOUCHPAD("Touchpad"),
    LOGS("Network Logs")
}

data class RemoteUiState(
    val activeTab: RemoteTab = RemoteTab.NAVIGATION,
    val showDiscoverySheet: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showTvBoxGuide: Boolean = false,
    val showTypeDialog: Boolean = false,
    val showVirtualTvScreen: Boolean = false,
    val isTestingPing: Boolean = false,
    val pingTestResult: String? = null
)

class TvRemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TvRemoteRepository(application, viewModelScope)

    val connectedDevice: StateFlow<TvDevice?> = repository.connectedDevice
    val connectionStatus: StateFlow<ConnectionStatus> = repository.connectionStatus
    val isBluetoothMode: StateFlow<Boolean> = repository.isBluetoothMode
    val bluetoothStatus: StateFlow<com.example.bluetooth.BluetoothStatus> = repository.bluetoothManager.status
    val bluetoothDeviceName: StateFlow<String?> = repository.bluetoothManager.connectedDeviceName
    val pairingRequiredIp: StateFlow<String?> = repository.pairingRequiredIp
    val pairingMessage: StateFlow<String?> = repository.pairingMessage
    val discoveredDevices: StateFlow<List<TvDevice>> = repository.discoveryManager.discoveredDevices
    val isScanning: StateFlow<Boolean> = repository.discoveryManager.isScanning
    val recentLatency: StateFlow<Long?> = repository.recentLatency
    val commandHistory: StateFlow<List<CommandResult>> = repository.commandHistory
    val lastResult: StateFlow<CommandResult?> = repository.lastResult
    val virtualTvState: StateFlow<VirtualTvState> = repository.virtualTvState
    val savedDevices: StateFlow<List<TvDevice>> = repository.savedDevices

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun setBluetoothMode(enabled: Boolean) {
        repository.setBluetoothMode(enabled)
    }

    fun makePhoneDiscoverable() {
        repository.makePhoneDiscoverable()
    }

    fun connectBluetoothTv(address: String = "b4:04:18:97:7b:c3") {
        repository.connectBluetoothAddress(address)
    }

    fun initBluetoothProfile() {
        repository.bluetoothManager.initHidProfile()
    }

    fun sendCommand(command: RemoteCommand) {
        viewModelScope.launch {
            repository.executeCommand(command)
        }
    }

    fun sendTypedText(text: String) {
        viewModelScope.launch {
            repository.sendTypedText(text)
        }
    }

    fun showTypeDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTypeDialog = show)
    }

    fun startScanning() {
        repository.startDiscovery()
    }

    fun stopScanning() {
        repository.stopDiscovery()
    }

    fun selectDevice(device: TvDevice) {
        repository.connectToDevice(device)
        _uiState.value = _uiState.value.copy(showDiscoverySheet = false)
        if (device.protocol == TvProtocol.ANDROID_TV_REMOTE_V2 &&
            !com.example.network.AndroidTvRemoteV2Client.isPaired(device.ipAddress)
        ) {
            startGoogleTvPairing(device.ipAddress)
        }
    }

    fun startGoogleTvPairing(ip: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingPing = true)
            val res = repository.startGoogleTvPairing(ip)
            _uiState.value = _uiState.value.copy(
                isTestingPing = false,
                pingTestResult = res.second
            )
        }
    }

    fun submitGoogleTvPin(ip: String, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingPing = true)
            val res = repository.submitGoogleTvPin(ip, code)
            _uiState.value = _uiState.value.copy(
                isTestingPing = false,
                pingTestResult = res.second
            )
        }
    }

    fun cancelGoogleTvPairing() {
        repository.cancelGoogleTvPairing()
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun addManualDevice(name: String, ip: String, port: Int, protocol: TvProtocol) {
        val device = repository.addManualDevice(name, ip, port, protocol)
        _uiState.value = _uiState.value.copy(showSettingsDialog = false)
        if (protocol == TvProtocol.ANDROID_TV_REMOTE_V2 &&
            !com.example.network.AndroidTvRemoteV2Client.isPaired(ip)
        ) {
            startGoogleTvPairing(ip)
        }
    }

    fun testPing(ip: String, port: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingPing = true, pingTestResult = null)
            val message = repository.diagnoseTvConnection(ip, port)
            _uiState.value = _uiState.value.copy(isTestingPing = false, pingTestResult = message)
        }
    }

    fun autoDetectAndConnect(ip: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingPing = true, pingTestResult = null)
            val result = repository.autoDetectAndConnect(ip)
            _uiState.value = _uiState.value.copy(isTestingPing = false, pingTestResult = result.second)
        }
    }

    fun getAllBondedDevices(): List<android.bluetooth.BluetoothDevice> {
        return repository.bluetoothManager.getAllBondedDevices()
    }

    fun setActiveTab(tab: RemoteTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun showDiscoverySheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDiscoverySheet = show)
        if (show) {
            startScanning()
        }
    }

    fun showSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettingsDialog = show)
    }

    fun toggleVirtualTvScreen() {
        _uiState.value = _uiState.value.copy(showVirtualTvScreen = !_uiState.value.showVirtualTvScreen)
    }

    fun showTvBoxGuide(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTvBoxGuide = show)
    }

    fun getSubnetPrefix(): String {
        return repository.discoveryManager.getSubnetPrefix()
    }

    fun getUserWifiIp(): String? {
        return repository.discoveryManager.getLocalWifiIp()
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopDiscovery()
    }
}
