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
    val discoveredDevices: StateFlow<List<TvDevice>> = repository.discoveryManager.discoveredDevices
    val isScanning: StateFlow<Boolean> = repository.discoveryManager.isScanning
    val recentLatency: StateFlow<Long?> = repository.recentLatency
    val commandHistory: StateFlow<List<CommandResult>> = repository.commandHistory
    val lastResult: StateFlow<CommandResult?> = repository.lastResult
    val virtualTvState: StateFlow<VirtualTvState> = repository.virtualTvState
    val savedDevices: StateFlow<List<TvDevice>> = repository.savedDevices

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

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
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun addManualDevice(name: String, ip: String, port: Int, protocol: TvProtocol) {
        val device = repository.addManualDevice(name, ip, port, protocol)
        _uiState.value = _uiState.value.copy(showSettingsDialog = false)
    }

    fun testPing(ip: String, port: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingPing = true, pingTestResult = null)
            val (success, latency) = repository.testDevicePing(ip, port)
            val message = if (success) {
                "Online! Response in ${latency}ms"
            } else {
                "Unreachable at $ip:$port (${latency}ms)"
            }
            _uiState.value = _uiState.value.copy(isTestingPing = false, pingTestResult = message)
        }
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
