// app/src/main/java/com/airmouse/presentation/ui/home/HomeViewModel.kt
package com.airmouse.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.model.GestureType
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.domain.repository.ISettingsRepository
import com.airmouse.domain.usecase.SendMovementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectionRepo: IConnectionRepository,
    private val calibrationRepo: ICalibrationRepository,
    private val gestureRepo: IGestureRepository,
    private val settingsRepo: ISettingsRepository,
    private val sendMovementUseCase: SendMovementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeConnectionStatus()
        observeGestures()
        loadCalibrationStatus()
        loadGestureStats()
        loadLastConfig()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepo.getPreferences().collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        aiSmoothingEnabled = prefs.useAiSmoothing,
                        predictiveEnabled = prefs.usePredictiveMovement
                    )
                }
            }
        }
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            connectionRepo.connectionStatus().collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }
    }

    private fun observeGestures() {
        viewModelScope.launch {
            gestureRepo.observeGestures().collect { gesture ->
                when (gesture.type) {
                    GestureType.CLICK -> sendMovementUseCase.sendClick()
                    GestureType.DOUBLE_CLICK -> sendMovementUseCase.sendDoubleClick()
                    GestureType.RIGHT_CLICK -> sendMovementUseCase.sendRightClick()
                    GestureType.SCROLL_UP -> sendMovementUseCase.sendScroll(1)
                    GestureType.SCROLL_DOWN -> sendMovementUseCase.sendScroll(-1)
                    else -> Unit
                }
                updateGestureStats()
            }
        }
    }

    private fun loadCalibrationStatus() {
        viewModelScope.launch {
            val gyroCal = calibrationRepo.getGyroBias().let { it.x != 0f || it.y != 0f || it.z != 0f }
            val accelCal = calibrationRepo.getAccelCalibration().let { it.scaleX != 1f }
            val magCal = calibrationRepo.getMagCalibration().let { it.scaleX != 1f }
            val calibratedCount = listOf(gyroCal, accelCal, magCal).count { it }
            _uiState.update { state ->
                state.copy(
                    sensorsCalibrated = calibratedCount,
                    calibrationProgress = calibratedCount * 100 / state.totalSensors
                )
            }
        }
    }

    private fun loadGestureStats() {
        viewModelScope.launch {
            // Simulate – in real app, get from repository
            _uiState.update { state ->
                state.copy(
                    gestureStats = GestureStats(
                        clicks = 0, doubleClicks = 0, rightClicks = 0, scrolls = 0
                    )
                )
            }
        }
    }

    private fun updateGestureStats() {
        // Placeholder – would increment counters in repository
    }

    private fun loadLastConfig() {
        viewModelScope.launch {
            val config = connectionRepo.getLastConfig()
            config?.let {
                _uiState.update { state ->
                    state.copy(serverIp = it.serverIp, serverPort = it.serverPort)
                }
            }
        }
    }

    fun updateIp(ip: String) {
        _uiState.update { it.copy(serverIp = ip) }
    }

    fun updatePort(port: Int) {
        _uiState.update { it.copy(serverPort = port) }
    }

    fun connect() {
        viewModelScope.launch {
            val config = ConnectionConfig(
                serverIp = _uiState.value.serverIp,
                serverPort = _uiState.value.serverPort,
                protocol = ConnectionProtocol.TCP
            )
            connectionRepo.connect(config)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionRepo.disconnect()
        }
    }

    fun startAirMouse() {
        _uiState.update { it.copy(isActive = true) }
        // Start sensor service, etc.
    }

    fun stopAirMouse() {
        _uiState.update { it.copy(isActive = false) }
    }

    fun toggleTouchpadMode() {
        _uiState.update { state ->
            state.copy(isTouchpadMode = !state.isTouchpadMode)
        }
    }

    fun updateOrientation(yaw: Float, pitch: Float) {
        _uiState.update { state ->
            state.copy(orientationYaw = yaw, orientationPitch = pitch)
        }
    }

    fun addLogMessage(message: String) {
        _uiState.update { state ->
            val newLogs = (listOf(message) + state.logMessages).take(100)
            state.copy(logMessages = newLogs)
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logMessages = emptyList()) }
    }
}
