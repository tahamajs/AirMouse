package com.airmouse.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
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
                    sensitivity = prefs.getFloat("sensitivity", 0.5f),
                    accelerationEnabled = prefs.getBoolean("acceleration_enabled", false),
                    accelerationFactor = prefs.getFloat("acceleration_factor", 1.5f),
                    invertX = prefs.getBoolean("invert_x", false),
                    invertY = prefs.getBoolean("invert_y", false),
                    smoothingEnabled = prefs.getBoolean("smoothing_enabled", true),
                    smoothingFactor = prefs.getFloat("smoothing_factor", 0.5f),
                    clickThreshold = prefs.getFloat("click_threshold", 10f),
                    doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                    scrollThreshold = prefs.getFloat("scroll_threshold", 5f),
                    rightClickTilt = prefs.getFloat("right_click_tilt", 15f),
                    rightClickDuration = prefs.getLong("right_click_duration", 500L),
                    gestureDebounce = prefs.getLong("gesture_debounce", 100L),
                    aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                    aiBlendFactor = prefs.getFloat("ai_blend_factor", 0.7f),
                    predictive = prefs.getBoolean("predictive_movement", true),
                    predictionStrength = prefs.getFloat("prediction_strength", 0.5f),
                    kalmanEnabled = prefs.getBoolean("kalman_enabled", true),
                    hapticEnabled = prefs.getBoolean("haptic_enabled", true),
                    hapticStrength = getHapticStrength(prefs.getString("haptic_strength", "MEDIUM")),
                    soundEnabled = prefs.getBoolean("sound_enabled", false),
                    visualFeedback = prefs.getBoolean("visual_feedback", true),
                    notificationOnGesture = prefs.getBoolean("notification_on_gesture", false),
                    theme = prefs.getString("theme", "system"),
                    useDynamicColors = prefs.getBoolean("dynamic_colors", true),
                    fontSize = prefs.getFloat("font_size", 16f),
                    showDebugInfo = prefs.getBoolean("show_debug_info", false),
                    keepScreenOn = prefs.getBoolean("keep_screen_on", false),
                    showFps = prefs.getBoolean("show_fps", false),
                    autoConnect = prefs.getBoolean("auto_connect", true),
                    reconnectAttempts = prefs.getInt("reconnect_attempts", 5),
                    connectionTimeout = prefs.getInt("connection_timeout", 5000),
                    useWebSocket = prefs.getBoolean("use_websocket", true),
                    useUdpDiscovery = prefs.getBoolean("use_udp_discovery", true),
                    anonymousStats = prefs.getBoolean("anonymous_stats", true),
                    crashReporting = prefs.getBoolean("crash_reporting", true),
                    clearDataOnExit = prefs.getBoolean("clear_data_on_exit", false),
                    presentationModeEnabled = prefs.getBoolean("presentation_mode_enabled", false),
                    laserPointerSpeed = prefs.getFloat("laser_pointer_speed", 1.0f),
                    showPresentationTimer = prefs.getBoolean("show_presentation_timer", true),
                    autoHideLaser = prefs.getBoolean("auto_hide_laser", true)
                )
            }
        }
    }

    private fun getHapticStrength(value: String): HapticStrength = when (value) {
        "LIGHT" -> HapticStrength.LIGHT
        "STRONG" -> HapticStrength.STRONG
        else -> HapticStrength.MEDIUM
    }

    private fun saveAndUpdate(block: () -> Unit, successMessage: String? = null) {
        block()
        successMessage?.let { showSuccess(it) }
    }

    // Cursor Settings
    fun updateSensitivity(value: Float) = saveAndUpdate({ prefs.putFloat("sensitivity", value); _uiState.update { it.copy(sensitivity = value) } })
    fun updateAccelerationEnabled(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("acceleration_enabled", enabled); _uiState.update { it.copy(accelerationEnabled = enabled) } }, "Mouse acceleration ${if (enabled) "enabled" else "disabled"}")
    fun updateAccelerationFactor(value: Float) = saveAndUpdate({ prefs.putFloat("acceleration_factor", value); _uiState.update { it.copy(accelerationFactor = value) } })
    fun updateInvertX(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("invert_x", enabled); _uiState.update { it.copy(invertX = enabled) } }, "X-axis ${if (enabled) "inverted" else "normalized"}")
    fun updateInvertY(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("invert_y", enabled); _uiState.update { it.copy(invertY = enabled) } }, "Y-axis ${if (enabled) "inverted" else "normalized"}")
    fun updateSmoothingEnabled(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("smoothing_enabled", enabled); _uiState.update { it.copy(smoothingEnabled = enabled) } })
    fun updateSmoothingFactor(value: Float) = saveAndUpdate({ prefs.putFloat("smoothing_factor", value); _uiState.update { it.copy(smoothingFactor = value) } })

    // Gesture Settings
    fun updateClickThreshold(value: Float) = saveAndUpdate({ prefs.putFloat("click_threshold", value); _uiState.update { it.copy(clickThreshold = value) } })
    fun updateDoubleClickInterval(value: Long) = saveAndUpdate({ prefs.putLong("double_click_interval", value); _uiState.update { it.copy(doubleClickInterval = value) } })
    fun updateScrollThreshold(value: Float) = saveAndUpdate({ prefs.putFloat("scroll_threshold", value); _uiState.update { it.copy(scrollThreshold = value) } })
    fun updateRightClickTilt(value: Float) = saveAndUpdate({ prefs.putFloat("right_click_tilt", value); _uiState.update { it.copy(rightClickTilt = value) } })
    fun updateRightClickDuration(value: Long) = saveAndUpdate({ prefs.putLong("right_click_duration", value); _uiState.update { it.copy(rightClickDuration = value) } })
    fun updateGestureDebounce(value: Long) = saveAndUpdate({ prefs.putLong("gesture_debounce", value); _uiState.update { it.copy(gestureDebounce = value) } })

    // AI & Predictive Settings
    fun updateAiSmoothing(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("ai_smoothing", enabled); _uiState.update { it.copy(aiSmoothing = enabled) } }, "AI Smoothing ${if (enabled) "enabled" else "disabled"}")
    fun updateAiBlendFactor(value: Float) = saveAndUpdate({ prefs.putFloat("ai_blend_factor", value); _uiState.update { it.copy(aiBlendFactor = value) } })
    fun updatePredictive(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("predictive_movement", enabled); _uiState.update { it.copy(predictive = enabled) } }, "Predictive movement ${if (enabled) "enabled" else "disabled"}")
    fun updatePredictionStrength(value: Float) = saveAndUpdate({ prefs.putFloat("prediction_strength", value); _uiState.update { it.copy(predictionStrength = value) } })
    fun updateKalmanEnabled(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("kalman_enabled", enabled); _uiState.update { it.copy(kalmanEnabled = enabled) } })

    // Haptic & Feedback
    fun updateHaptic(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("haptic_enabled", enabled); _uiState.update { it.copy(hapticEnabled = enabled) } })
    fun updateHapticStrength(strength: HapticStrength) = saveAndUpdate({ prefs.putString("haptic_strength", strength.name); _uiState.update { it.copy(hapticStrength = strength) } })
    fun updateSoundEnabled(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("sound_enabled", enabled); _uiState.update { it.copy(soundEnabled = enabled) } })
    fun updateVisualFeedback(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("visual_feedback", enabled); _uiState.update { it.copy(visualFeedback = enabled) } })
    fun updateNotificationOnGesture(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("notification_on_gesture", enabled); _uiState.update { it.copy(notificationOnGesture = enabled) } })

    // Display Settings
    fun updateTheme(theme: String) = saveAndUpdate({ prefs.putString("theme", theme); _uiState.update { it.copy(theme = theme) } }, "Theme changed to $theme")
    fun updateUseDynamicColors(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("dynamic_colors", enabled); _uiState.update { it.copy(useDynamicColors = enabled) } })
    fun updateFontSize(value: Float) = saveAndUpdate({ prefs.putFloat("font_size", value); _uiState.update { it.copy(fontSize = value) } })
    fun updateShowDebugInfo(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("show_debug_info", enabled); _uiState.update { it.copy(showDebugInfo = enabled) } })
    fun updateKeepScreenOn(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("keep_screen_on", enabled); _uiState.update { it.copy(keepScreenOn = enabled) } })
    fun updateShowFps(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("show_fps", enabled); _uiState.update { it.copy(showFps = enabled) } })

    // Connection Settings
    fun updateAutoConnect(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("auto_connect", enabled); _uiState.update { it.copy(autoConnect = enabled) } })
    fun updateReconnectAttempts(value: Int) = saveAndUpdate({ prefs.putInt("reconnect_attempts", value); _uiState.update { it.copy(reconnectAttempts = value) } })
    fun updateConnectionTimeout(value: Int) = saveAndUpdate({ prefs.putInt("connection_timeout", value); _uiState.update { it.copy(connectionTimeout = value) } })
    fun updateUseWebSocket(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("use_websocket", enabled); _uiState.update { it.copy(useWebSocket = enabled) } })
    fun updateUseUdpDiscovery(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("use_udp_discovery", enabled); _uiState.update { it.copy(useUdpDiscovery = enabled) } })

    // Privacy & Data
    fun updateAnonymousStats(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("anonymous_stats", enabled); _uiState.update { it.copy(anonymousStats = enabled) } })
    fun updateCrashReporting(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("crash_reporting", enabled); _uiState.update { it.copy(crashReporting = enabled) } })
    fun updateClearDataOnExit(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("clear_data_on_exit", enabled); _uiState.update { it.copy(clearDataOnExit = enabled) } })

    // Presentation Settings
    fun updatePresentationModeEnabled(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("presentation_mode_enabled", enabled); _uiState.update { it.copy(presentationModeEnabled = enabled) } })
    fun updateLaserPointerSpeed(value: Float) = saveAndUpdate({ prefs.putFloat("laser_pointer_speed", value); _uiState.update { it.copy(laserPointerSpeed = value) } })
    fun updateShowPresentationTimer(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("show_presentation_timer", enabled); _uiState.update { it.copy(showPresentationTimer = enabled) } })
    fun updateAutoHideLaser(enabled: Boolean) = saveAndUpdate({ prefs.putBoolean("auto_hide_laser", enabled); _uiState.update { it.copy(autoHideLaser = enabled) } })

    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateSensitivity(0.5f)
            updateAccelerationEnabled(false)
            updateAccelerationFactor(1.5f)
            updateInvertX(false)
            updateInvertY(false)
            updateSmoothingEnabled(true)
            updateSmoothingFactor(0.5f)
            updateClickThreshold(10f)
            updateDoubleClickInterval(300L)
            updateScrollThreshold(5f)
            updateRightClickTilt(15f)
            updateRightClickDuration(500L)
            updateGestureDebounce(100L)
            updateAiSmoothing(false)
            updateAiBlendFactor(0.7f)
            updatePredictive(true)
            updatePredictionStrength(0.5f)
            updateKalmanEnabled(true)
            updateHaptic(true)
            updateHapticStrength(HapticStrength.MEDIUM)
            updateSoundEnabled(false)
            updateVisualFeedback(true)
            updateNotificationOnGesture(false)
            updateTheme("system")
            updateUseDynamicColors(true)
            updateFontSize(16f)
            updateShowDebugInfo(false)
            updateKeepScreenOn(false)
            updateShowFps(false)
            updateAutoConnect(true)
            updateReconnectAttempts(5)
            updateConnectionTimeout(5000)
            updateUseWebSocket(true)
            updateUseUdpDiscovery(true)
            updateAnonymousStats(true)
            updateCrashReporting(true)
            updateClearDataOnExit(false)
            updatePresentationModeEnabled(false)
            updateLaserPointerSpeed(1.0f)
            updateShowPresentationTimer(true)
            updateAutoHideLaser(true)
            delay(500)
            _uiState.update { it.copy(isSaving = false, success = "All settings reset to defaults") }
            clearMessages()
        }
    }

    fun exportSettings() {
        val settings = buildString {
            append("Air Mouse Pro Settings Export\n")
            append("=============================\n\n")
            append("Sensitivity: ${uiState.value.sensitivity}\n")
            append("Theme: ${uiState.value.theme}\n")
            append("AI Smoothing: ${uiState.value.aiSmoothing}\n")
            append("Predictive: ${uiState.value.predictive}\n")
            append("Export Date: ${System.currentTimeMillis()}\n")
        }
        // In production, save to file
        showSuccess("Settings exported")
    }

    fun openWebsite() = openUrl("https://airmouse.io")
    fun openPrivacyPolicy() = openUrl("https://airmouse.io/privacy")
    fun openLicense() = openUrl("https://airmouse.io/license")

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        // In production, start activity with context
    }

    private fun showSuccess(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(success = message) }
            delay(3000)
            _uiState.update { it.copy(success = null) }
        }
    }

    private fun clearMessages() {
        viewModelScope.launch { delay(3000); _uiState.update { it.copy(error = null, success = null) } }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}