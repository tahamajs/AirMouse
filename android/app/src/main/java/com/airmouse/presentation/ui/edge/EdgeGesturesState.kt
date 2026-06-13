package com.airmouse.presentation.ui.edge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EdgeGesturesUiState(
    val isEnabled: Boolean = false,
    val volumeUpAction: String = "Left Click",
    val volumeDownAction: String = "Right Click",
    val longPressAction: String = "Scroll",
    val doublePressAction: String = "Double Click",
    val vibrationFeedback: Boolean = true,
    val screenEdgeSensitivity: Float = 0.2f,
    val availableActions: List<String> = listOf(
        "Left Click", "Right Click", "Double Click", "Scroll Up", "Scroll Down",
        "Volume Up", "Volume Down", "Previous Track", "Next Track", "Play/Pause",
        "None"
    )
)

@HiltViewModel
class EdgeGesturesViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EdgeGesturesUiState())
    val uiState: StateFlow<EdgeGesturesUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isEnabled = prefs.getBoolean("edge_gestures_enabled", false),
                    volumeUpAction = prefs.getString("edge_gestures_volume_up", "Left Click"),
                    volumeDownAction = prefs.getString("edge_gestures_volume_down", "Right Click"),
                    longPressAction = prefs.getString("edge_gestures_long_press", "Scroll"),
                    doublePressAction = prefs.getString("edge_gestures_double_press", "Double Click"),
                    vibrationFeedback = prefs.getBoolean("edge_gestures_vibration", true),
                    screenEdgeSensitivity = prefs.getFloat("edge_gestures_sensitivity", 0.2f)
                )
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        prefs.putBoolean("edge_gestures_enabled", enabled)
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    fun setVolumeUpAction(action: String) {
        prefs.putString("edge_gestures_volume_up", action)
        _uiState.update { it.copy(volumeUpAction = action) }
    }

    fun setVolumeDownAction(action: String) {
        prefs.putString("edge_gestures_volume_down", action)
        _uiState.update { it.copy(volumeDownAction = action) }
    }

    fun setLongPressAction(action: String) {
        prefs.putString("edge_gestures_long_press", action)
        _uiState.update { it.copy(longPressAction = action) }
    }

    fun setDoublePressAction(action: String) {
        prefs.putString("edge_gestures_double_press", action)
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

    fun resetToDefaults() {
        prefs.putBoolean("edge_gestures_enabled", false)
        prefs.putString("edge_gestures_volume_up", "Left Click")
        prefs.putString("edge_gestures_volume_down", "Right Click")
        prefs.putString("edge_gestures_long_press", "Scroll")
        prefs.putString("edge_gestures_double_press", "Double Click")
        prefs.putBoolean("edge_gestures_vibration", true)
        prefs.putFloat("edge_gestures_sensitivity", 0.2f)

        loadSettings()
    }
}package com.airmouse.presentation.ui.edge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EdgeGesturesUiState(
    val isEnabled: Boolean = false,
    val volumeUpAction: String = "Left Click",
    val volumeDownAction: String = "Right Click",
    val longPressAction: String = "Scroll",
    val doublePressAction: String = "Double Click",
    val vibrationFeedback: Boolean = true,
    val screenEdgeSensitivity: Float = 0.2f,
    val availableActions: List<String> = listOf(
        "Left Click", "Right Click", "Double Click", "Scroll Up", "Scroll Down",
        "Volume Up", "Volume Down", "Previous Track", "Next Track", "Play/Pause",
        "None"
    )
)

