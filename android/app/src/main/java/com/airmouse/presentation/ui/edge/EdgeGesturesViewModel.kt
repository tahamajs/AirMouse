package com.airmouse.presentation.ui.edge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EdgeGesturesViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EdgeGesturesUiState())
    val uiState: StateFlow<EdgeGesturesUiState> = _uiState.asStateFlow()
    
    private val _stats = MutableStateFlow(EdgeGesturesStats())
    val stats: StateFlow<EdgeGesturesStats> = _stats.asStateFlow()
    
    private val _gestureDetected = MutableSharedFlow<EdgeGestureEvent>()
    val gestureDetected: SharedFlow<EdgeGestureEvent> = _gestureDetected.asSharedFlow()

    init {
        loadSettings()
        loadStats()
        setupGestureDetection()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isEnabled = prefs.getBoolean("edge_gestures_enabled", false),
                    volumeUpAction = getActionFromString(prefs.getString("edge_gestures_volume_up", "LEFT_CLICK")),
                    volumeDownAction = getActionFromString(prefs.getString("edge_gestures_volume_down", "RIGHT_CLICK")),
                    longPressAction = getActionFromString(prefs.getString("edge_gestures_long_press", "SCROLL")),
                    doublePressAction = getActionFromString(prefs.getString("edge_gestures_double_press", "DOUBLE_CLICK")),
                    vibrationFeedback = prefs.getBoolean("edge_gestures_vibration", true),
                    screenEdgeSensitivity = prefs.getFloat("edge_gestures_sensitivity", 0.2f)
                )
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            _stats.update {
                it.copy(
                    totalDetections = prefs.getInt("edge_stats_total", 0),
                    volumeUpCount = prefs.getInt("edge_stats_volume_up", 0),
                    volumeDownCount = prefs.getInt("edge_stats_volume_down", 0),
                    longPressCount = prefs.getInt("edge_stats_long_press", 0),
                    doublePressCount = prefs.getInt("edge_stats_double_press", 0),
                    successfulExecutions = prefs.getInt("edge_stats_success", 0),
                    failedExecutions = prefs.getInt("edge_stats_failed", 0)
                )
            }
        }
    }

    private fun saveStats() {
        prefs.putInt("edge_stats_total", _stats.value.totalDetections)
        prefs.putInt("edge_stats_volume_up", _stats.value.volumeUpCount)
        prefs.putInt("edge_stats_volume_down", _stats.value.volumeDownCount)
        prefs.putInt("edge_stats_long_press", _stats.value.longPressCount)
        prefs.putInt("edge_stats_double_press", _stats.value.doublePressCount)
        prefs.putInt("edge_stats_success", _stats.value.successfulExecutions)
        prefs.putInt("edge_stats_failed", _stats.value.failedExecutions)
    }

    private fun setupGestureDetection() {
        // Simulate gesture detection (in production, listen to volume buttons)
        viewModelScope.launch {
            _gestureDetected.collect { event ->
                if (_uiState.value.isEnabled) {
                    handleGesture(event)
                }
            }
        }
    }

    private fun handleGesture(event: EdgeGestureEvent) {
        viewModelScope.launch {
            // Update stats
            _stats.update { stats ->
                when (event.type) {
                    GestureType.VOLUME_UP -> stats.copy(
                        totalDetections = stats.totalDetections + 1,
                        volumeUpCount = stats.volumeUpCount + 1
                    )
                    GestureType.VOLUME_DOWN -> stats.copy(
                        totalDetections = stats.totalDetections + 1,
                        volumeDownCount = stats.volumeDownCount + 1
                    )
                    GestureType.LONG_PRESS -> stats.copy(
                        totalDetections = stats.totalDetections + 1,
                        longPressCount = stats.longPressCount + 1
                    )
                    GestureType.DOUBLE_PRESS -> stats.copy(
                        totalDetections = stats.totalDetections + 1,
                        doublePressCount = stats.doublePressCount + 1
                    )
                }
            }
            saveStats()
            
            // Animate gesture detection
            _uiState.update { it.copy(gestureDetectionProgress = 1f, lastDetectedGesture = event.type.name) }
            delay(500)
            _uiState.update { it.copy(gestureDetectionProgress = 0f) }
            delay(1000)
            _uiState.update { it.copy(lastDetectedGesture = null) }
            
            // Execute action
            val action = when (event.type) {
                GestureType.VOLUME_UP -> _uiState.value.volumeUpAction
                GestureType.VOLUME_DOWN -> _uiState.value.volumeDownAction
                GestureType.LONG_PRESS -> _uiState.value.longPressAction
                GestureType.DOUBLE_PRESS -> _uiState.value.doublePressAction
            }
            
            executeAction(action)
            
            if (_uiState.value.vibrationFeedback) {
                // Vibrate feedback handled by system
            }
        }
    }

    private fun executeAction(action: EdgeAction) {
        viewModelScope.launch {
            val success = when (action) {
                EdgeAction.LEFT_CLICK -> connectionManager.sendClick("left")
                EdgeAction.RIGHT_CLICK -> connectionManager.sendClick("right")
                EdgeAction.DOUBLE_CLICK -> connectionManager.sendDoubleClick()
                EdgeAction.SCROLL_UP -> connectionManager.sendScroll(3)
                EdgeAction.SCROLL_DOWN -> connectionManager.sendScroll(-3)
                EdgeAction.VOLUME_UP -> connectionManager.sendControl("volume_up")
                EdgeAction.VOLUME_DOWN -> connectionManager.sendControl("volume_down")
                EdgeAction.PREV_TRACK -> connectionManager.sendControl("prev_track")
                EdgeAction.NEXT_TRACK -> connectionManager.sendControl("next_track")
                EdgeAction.PLAY_PAUSE -> connectionManager.sendControl("play_pause")
                EdgeAction.LOCK_SCREEN -> connectionManager.sendControl("lock_screen")
                EdgeAction.SHOW_DESKTOP -> connectionManager.sendControl("show_desktop")
                EdgeAction.TASK_VIEW -> connectionManager.sendControl("task_view")
                else -> true
            }
            
            _stats.update { stats ->
                if (success) stats.copy(successfulExecutions = stats.successfulExecutions + 1)
                else stats.copy(failedExecutions = stats.failedExecutions + 1)
            }
            saveStats()
        }
    }

    fun setEnabled(enabled: Boolean) {
        prefs.putBoolean("edge_gestures_enabled", enabled)
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    fun setVolumeUpAction(action: EdgeAction) {
        prefs.putString("edge_gestures_volume_up", action.name)
        _uiState.update { it.copy(volumeUpAction = action) }
    }

    fun setVolumeDownAction(action: EdgeAction) {
        prefs.putString("edge_gestures_volume_down", action.name)
        _uiState.update { it.copy(volumeDownAction = action) }
    }

    fun setLongPressAction(action: EdgeAction) {
        prefs.putString("edge_gestures_long_press", action.name)
        _uiState.update { it.copy(longPressAction = action) }
    }

    fun setDoublePressAction(action: EdgeAction) {
        prefs.putString("edge_gestures_double_press", action.name)
        _uiState.update { it.copy(doublePressAction = action) }
    }

    fun setVibrationFeedback(enabled: Boolean) {
        prefs.putBoolean("edge_gestures_vibration", enabled)
        _uiState.update { it.copy(vibrationFeedback = enabled) }
    }

    fun setScreenEdgeSensitivity(sensitivity: Float) {
        prefs.putFloat("edge_gestures_sensitivity", sensitivity)
        _uiState.update { it.copy(screenEdgeSensitivity = sensitivity) }
    }

    fun startGestureConfiguration(action: EdgeAction) {
        _uiState.update { it.copy(isConfiguring = true, configuringAction = action) }
        
        // Simulate gesture listening
        viewModelScope.launch {
            delay(5000) // 5 seconds listening window
            _uiState.update { it.copy(isConfiguring = false, configuringAction = null) }
        }
    }

    fun cancelConfiguration() {
        _uiState.update { it.copy(isConfiguring = false, configuringAction = null) }
    }

    fun resetToDefaults() {
        prefs.putBoolean("edge_gestures_enabled", false)
        prefs.putString("edge_gestures_volume_up", EdgeAction.LEFT_CLICK.name)
        prefs.putString("edge_gestures_volume_down", EdgeAction.RIGHT_CLICK.name)
        prefs.putString("edge_gestures_long_press", EdgeAction.SCROLL.name)
        prefs.putString("edge_gestures_double_press", EdgeAction.DOUBLE_CLICK.name)
        prefs.putBoolean("edge_gestures_vibration", true)
        prefs.putFloat("edge_gestures_sensitivity", 0.2f)
        
        // Reset stats
        prefs.putInt("edge_stats_total", 0)
        prefs.putInt("edge_stats_volume_up", 0)
        prefs.putInt("edge_stats_volume_down", 0)
        prefs.putInt("edge_stats_long_press", 0)
        prefs.putInt("edge_stats_double_press", 0)
        prefs.putInt("edge_stats_success", 0)
        prefs.putInt("edge_stats_failed", 0)
        
        loadSettings()
        loadStats()
    }

    fun resetStats() {
        _stats.update { EdgeGesturesStats() }
        saveStats()
    }

    fun simulateGesture(type: GestureType) {
        viewModelScope.launch {
            _gestureDetected.emit(EdgeGestureEvent(type))
        }
    }

    private fun getActionFromString(name: String): EdgeAction {
        return try {
            EdgeAction.valueOf(name)
        } catch (e: Exception) {
            EdgeAction.LEFT_CLICK
        }
    }
}