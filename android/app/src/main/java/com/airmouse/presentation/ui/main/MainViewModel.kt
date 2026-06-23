package com.airmouse.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _connectionQuality = MutableStateFlow<ConnectionQuality?>(null)
    val connectionQuality: StateFlow<ConnectionQuality?> = _connectionQuality.asStateFlow()
    
    private val _serverInfo = MutableStateFlow(Pair("", ""))
    val serverInfo: StateFlow<Pair<String, String>> = _serverInfo.asStateFlow()

    data class ConnectionQuality(
        val ping: Int,
        val signalStrength: Int,
        val isStable: Boolean
    )

    data class MainUiState(
        val controlMode: String = "motion",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isRegistered: Boolean = false,
        val isCalibrated: Boolean = false,
        val userName: String = ""
    )

    init {
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                _isConnected.value = status == ConnectionManager.ConnectionStatus.CONNECTED
            }
        }
        
        viewModelScope.launch {
            connectionManager.connectionQuality.collect { quality ->
                _connectionQuality.value = ConnectionQuality(
                    ping = quality.ping,
                    signalStrength = quality.level(),
                    isStable = quality.isHealthy()
                )
            }
        }
        
        viewModelScope.launch {
            connectionManager.serverName.collect { name ->
                _serverInfo.value = Pair(name, _serverInfo.value.second)
            }
        }
        
        viewModelScope.launch {
            connectionManager.serverVersion.collect { version ->
                _serverInfo.value = Pair(_serverInfo.value.first, version)
            }
        }

        _uiState.update {
            it.copy(
                controlMode = prefs.getString("control_mode", "motion"),
                isRegistered = !prefs.isFirstLaunch() && prefs.getUserName().isNotBlank(),
                isCalibrated = prefs.getBoolean("calibration_complete", false) ||
                        prefs.getBoolean("is_calibrated", false),
                userName = prefs.getUserName()
            )
        }
    }

    fun updateControlMode(mode: String) {
        prefs.putString("control_mode", mode)
        _uiState.update { it.copy(controlMode = mode) }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            connectionManager.disconnect()
        }
    }
    
    fun reconnect() {
        viewModelScope.launch {
            connectionManager.reconnect()
        }
    }
    
    fun showArmCalibrationDialog() {
        
        _uiState.update { it.copy(error = "Arm calibration coming soon") }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
