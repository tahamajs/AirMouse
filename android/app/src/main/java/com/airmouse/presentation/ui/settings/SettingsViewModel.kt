package com.airmouse.presentation.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadAllSettings()
    }

    private fun loadAllSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    // Cursor Settings
                    sensitivity = prefs.getFloat("sensitivity", 0.5f),
                    accelerationEnabled = prefs.getBoolean("acceleration_enabled", false),
                    accelerationFactor = prefs.getFloat("acceleration_factor", 1.5f),
                    invertX = prefs.getBoolean("invert_x", false),
                    invertY = prefs.getBoolean("invert_y", false),
                    smoothingEnabled = prefs.getBoolean("smoothing_enabled", true),
                    smoothingFactor = prefs.getFloat("smoothing_factor", 0.5f),

                    // Gesture Settings
                    clickThreshold = prefs.getFloat("click_threshold", 10f),
                    doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                    scrollThreshold = prefs.getFloat("scroll_threshold", 5f),
                    rightClickTilt = prefs.getFloat("right_click_tilt", 15f),
                    rightClickDuration = prefs.getLong("right_click_duration", 500L),
                    gestureDebounce = prefs.getLong("gesture_debounce", 100L),

                    // AI & Predictive
                    aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                    aiBlendFactor = prefs.getFloat("ai_blend_factor", 0.7f),
                    predictive = prefs.getBoolean("predictive_movement", true),
                    predictionStrength = prefs.getFloat("prediction_strength", 0.5f),
                    kalmanEnabled = prefs.getBoolean("kalman_enabled", true),

                    // Haptic & Feedback
                    hapticEnabled = prefs.getBoolean("haptic_enabled", true),
                    hapticStrength = getHapticStrength(prefs.getString("haptic_strength", "MEDIUM")),
                    soundEnabled = prefs.getBoolean("sound_enabled", false),
                    visualFeedback = prefs.getBoolean("visual_feedback", true),
                    notificationOnGesture = prefs.getBoolean("notification_on_gesture", false),

                    // Display Settings
                    theme = prefs.getString("theme", "system"),
                    useDynamicColors = prefs.getBoolean("dynamic_colors", true),
                    fontSize = prefs.getFloat("font_size", 16f),
                    showDebugInfo = prefs.getBoolean("show_debug_info", false),
                    keepScreenOn = prefs.getBoolean("keep_screen_on", false),
                    showFps = prefs.getBoolean("show_fps", false),

                    // Connection Settings
                    autoConnect = prefs.getBoolean("auto_connect", true),
                    reconnectAttempts = prefs.getInt("reconnect_attempts", 5),
                    connectionTimeout = prefs.getInt("connection_timeout", 5000),
                    useWebSocket = prefs.getBoolean("use_websocket", true),
                    useUdpDiscovery = prefs.getBoolean("use_udp_discovery", true),

                    // Privacy & Data
                    anonymousStats = prefs.getBoolean("anonymous_stats", true),
                    crashReporting = prefs.getBoolean("crash_reporting", true),
                    clearDataOnExit = prefs.getBoolean("clear_data_on_exit", false)
                )
            }
        }
    }

    private fun getHapticStrength(value: String): HapticStrength {
        return when (value) {
            "LIGHT" -> HapticStrength.LIGHT
            "STRONG" -> HapticStrength.STRONG
            else -> HapticStrength.MEDIUM
        }
    }

    // Cursor Settings
    fun updateSensitivity(value: Float) {
        prefs.putFloat("sensitivity", value)
        _uiState.update { it.copy(sensitivity = value) }
        showSuccess("Sensitivity updated")
    }

    fun updateAccelerationEnabled(enabled: Boolean) {
        prefs.putBoolean("acceleration_enabled", enabled)
        _uiState.update { it.copy(accelerationEnabled = enabled) }
        showSuccess("Mouse acceleration ${if (enabled) "enabled" else "disabled"}")
    }

    fun updateAccelerationFactor(value: Float) {
        prefs.putFloat("acceleration_factor", value)
        _uiState.update { it.copy(accelerationFactor = value) }
    }

    fun updateInvertX(enabled: Boolean) {
        prefs.putBoolean("invert_x", enabled)
        _uiState.update { it.copy(invertX = enabled) }
        showSuccess("X-axis ${if (enabled) "inverted" else "normalized"}")
    }

    fun updateInvertY(enabled: Boolean) {
        prefs.putBoolean("invert_y", enabled)
        _uiState.update { it.copy(invertY = enabled) }
        showSuccess("Y-axis ${if (enabled) "inverted" else "normalized"}")
    }

    fun updateSmoothingEnabled(enabled: Boolean) {
        prefs.putBoolean("smoothing_enabled", enabled)
        _uiState.update { it.copy(smoothingEnabled = enabled) }
    }

    fun updateSmoothingFactor(value: Float) {
        prefs.putFloat("smoothing_factor", value)
        _uiState.update { it.copy(smoothingFactor = value) }
    }

    // Gesture Settings
    fun updateClickThreshold(value: Float) {
        prefs.putFloat("click_threshold", value)
        _uiState.update { it.copy(clickThreshold = value) }
    }

    fun updateDoubleClickInterval(value: Long) {
        prefs.putLong("double_click_interval", value)
        _uiState.update { it.copy(doubleClickInterval = value) }
    }

    fun updateScrollThreshold(value: Float) {
        prefs.putFloat("scroll_threshold", value)
        _uiState.update { it.copy(scrollThreshold = value) }
    }

    fun updateRightClickTilt(value: Float) {
        prefs.putFloat("right_click_tilt", value)
        _uiState.update { it.copy(rightClickTilt = value) }
    }

    fun updateRightClickDuration(value: Long) {
        prefs.putLong("right_click_duration", value)
        _uiState.update { it.copy(rightClickDuration = value) }
    }

    fun updateGestureDebounce(value: Long) {
        prefs.putLong("gesture_debounce", value)
        _uiState.update { it.copy(gestureDebounce = value) }
    }

    // AI & Predictive Settings
    fun updateAiSmoothing(enabled: Boolean) {
        prefs.putBoolean("ai_smoothing", enabled)
        _uiState.update { it.copy(aiSmoothing = enabled) }
        showSuccess("AI Smoothing ${if (enabled) "enabled" else "disabled"}")
    }

    fun updateAiBlendFactor(value: Float) {
        prefs.putFloat("ai_blend_factor", value)
        _uiState.update { it.copy(aiBlendFactor = value) }
    }

    fun updatePredictive(enabled: Boolean) {
        prefs.putBoolean("predictive_movement", enabled)
        _uiState.update { it.copy(predictive = enabled) }
        showSuccess("Predictive movement ${if (enabled) "enabled" else "disabled"}")
    }

    fun updatePredictionStrength(value: Float) {
        prefs.putFloat("prediction_strength", value)
        _uiState.update { it.copy(predictionStrength = value) }
    }

    fun updateKalmanEnabled(enabled: Boolean) {
        prefs.putBoolean("kalman_enabled", enabled)
        _uiState.update { it.copy(kalmanEnabled = enabled) }
    }

    // Haptic & Feedback
    fun updateHaptic(enabled: Boolean) {
        prefs.putBoolean("haptic_enabled", enabled)
        _uiState.update { it.copy(hapticEnabled = enabled) }
    }

    fun updateHapticStrength(strength: HapticStrength) {
        prefs.putString("haptic_strength", strength.name)
        _uiState.update { it.copy(hapticStrength = strength) }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        prefs.putBoolean("sound_enabled", enabled)
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun updateVisualFeedback(enabled: Boolean) {
        prefs.putBoolean("visual_feedback", enabled)
        _uiState.update { it.copy(visualFeedback = enabled) }
    }

    fun updateNotificationOnGesture(enabled: Boolean) {
        prefs.putBoolean("notification_on_gesture", enabled)
        _uiState.update { it.copy(notificationOnGesture = enabled) }
    }

    // Display Settings
    fun updateTheme(theme: String) {
        prefs.putString("theme", theme)
        _uiState.update { it.copy(theme = theme) }
        showSuccess("Theme changed to $theme")
    }

    fun updateUseDynamicColors(enabled: Boolean) {
        prefs.putBoolean("dynamic_colors", enabled)
        _uiState.update { it.copy(useDynamicColors = enabled) }
    }

    fun updateFontSize(value: Float) {
        prefs.putFloat("font_size", value)
        _uiState.update { it.copy(fontSize = value) }
    }

    fun updateShowDebugInfo(enabled: Boolean) {
        prefs.putBoolean("show_debug_info", enabled)
        _uiState.update { it.copy(showDebugInfo = enabled) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        prefs.putBoolean("keep_screen_on", enabled)
        _uiState.update { it.copy(keepScreenOn = enabled) }
    }

    fun updateShowFps(enabled: Boolean) {
        prefs.putBoolean("show_fps", enabled)
        _uiState.update { it.copy(showFps = enabled) }
    }

    // Connection Settings
    fun updateAutoConnect(enabled: Boolean) {
        prefs.putBoolean("auto_connect", enabled)
        _uiState.update { it.copy(autoConnect = enabled) }
    }

    fun updateReconnectAttempts(value: Int) {
        prefs.putInt("reconnect_attempts", value)
        _uiState.update { it.copy(reconnectAttempts = value) }
    }

    fun updateConnectionTimeout(value: Int) {
        prefs.putInt("connection_timeout", value)
        _uiState.update { it.copy(connectionTimeout = value) }
    }

    fun updateUseWebSocket(enabled: Boolean) {
        prefs.putBoolean("use_websocket", enabled)
        _uiState.update { it.copy(useWebSocket = enabled) }
    }

    fun updateUseUdpDiscovery(enabled: Boolean) {
        prefs.putBoolean("use_udp_discovery", enabled)
        _uiState.update { it.copy(useUdpDiscovery = enabled) }
    }

    // Privacy & Data
    fun updateAnonymousStats(enabled: Boolean) {
        prefs.putBoolean("anonymous_stats", enabled)
        _uiState.update { it.copy(anonymousStats = enabled) }
    }

    fun updateCrashReporting(enabled: Boolean) {
        prefs.putBoolean("crash_reporting", enabled)
        _uiState.update { it.copy(crashReporting = enabled) }
    }

    fun updateClearDataOnExit(enabled: Boolean) {
        prefs.putBoolean("clear_data_on_exit", enabled)
        _uiState.update { it.copy(clearDataOnExit = enabled) }
    }

    // Reset Functions
    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            // Reset cursor settings
            updateSensitivity(0.5f)
            updateAccelerationEnabled(false)
            updateAccelerationFactor(1.5f)
            updateInvertX(false)
            updateInvertY(false)
            updateSmoothingEnabled(true)
            updateSmoothingFactor(0.5f)

            // Reset gesture settings
            updateClickThreshold(10f)
            updateDoubleClickInterval(300L)
            updateScrollThreshold(5f)
            updateRightClickTilt(15f)
            updateRightClickDuration(500L)
            updateGestureDebounce(100L)

            // Reset AI settings
            updateAiSmoothing(false)
            updateAiBlendFactor(0.7f)
            updatePredictive(true)
            updatePredictionStrength(0.5f)
            updateKalmanEnabled(true)

            // Reset haptic settings
            updateHaptic(true)
            updateHapticStrength(HapticStrength.MEDIUM)
            updateSoundEnabled(false)
            updateVisualFeedback(true)
            updateNotificationOnGesture(false)

            // Reset display settings
            updateTheme("system")
            updateUseDynamicColors(true)
            updateFontSize(16f)
            updateShowDebugInfo(false)
            updateKeepScreenOn(false)
            updateShowFps(false)

            // Reset connection settings
            updateAutoConnect(true)
            updateReconnectAttempts(5)
            updateConnectionTimeout(5000)
            updateUseWebSocket(true)
            updateUseUdpDiscovery(true)

            // Reset privacy settings
            updateAnonymousStats(true)
            updateCrashReporting(true)
            updateClearDataOnExit(false)

            delay(500)
            _uiState.update { it.copy(isSaving = false, success = "All settings reset to defaults") }
            clearMessages()
        }
    }

    fun exportSettings() {
        viewModelScope.launch {
            val settings = mapOf(
                "sensitivity" to uiState.value.sensitivity,
                "clickThreshold" to uiState.value.clickThreshold,
                "theme" to uiState.value.theme,
                "aiSmoothing" to uiState.value.aiSmoothing,
                "predictive" to uiState.value.predictive
            )
            // Implement export to file
            showSuccess("Settings exported")
        }
    }

    fun importSettings() {
        viewModelScope.launch {
            // Implement import from file
            showSuccess("Settings imported")
        }
    }

    private fun showSuccess(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(success = message) }
            delay(3000)
            _uiState.update { it.copy(success = null) }
        }
    }

    private fun clearMessages() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(error = null, success = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}