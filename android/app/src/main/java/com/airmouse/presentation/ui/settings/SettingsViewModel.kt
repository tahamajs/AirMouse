// app/src/main/java/com/airmouse/presentation/ui/settings/SettingsViewModel.kt
package com.airmouse.presentation.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadAllSettings()
    }

    // ==================== LOAD SETTINGS ====================

    private fun loadAllSettings() {
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

                // AI & Predictive Settings
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
                clearDataOnExit = prefs.getBoolean("clear_data_on_exit", false),

                // Presentation Settings
                presentationModeEnabled = prefs.getBoolean("presentation_mode_enabled", false),
                laserPointerSpeed = prefs.getFloat("laser_pointer_speed", 1.0f),
                showPresentationTimer = prefs.getBoolean("show_presentation_timer", true),
                autoHideLaser = prefs.getBoolean("auto_hide_laser", true)
            )
        }
    }

    private fun getHapticStrength(value: String): HapticStrength = when (value) {
        "LIGHT" -> HapticStrength.LIGHT
        "STRONG" -> HapticStrength.STRONG
        else -> HapticStrength.MEDIUM
    }

    // ==================== TOAST HELPER ====================

    private suspend fun showToast(message: String) {
        _toastMessage.value = message
        delay(3000)
        _toastMessage.value = null
    }

    // ==================== SAVE HELPERS ====================

    private suspend fun saveAndUpdate(block: suspend () -> Unit, successMessage: String? = null) {
        try {
            _uiState.update { it.copy(isSaving = true) }
            block()
            successMessage?.let { showToast(it) }
            _uiState.update { it.copy(isSaving = false, error = null) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isSaving = false, error = e.message ?: "An error occurred")
            }
            showToast("Error: ${e.message}")
        }
    }

    // ==================== CURSOR SETTINGS ====================

    fun updateSensitivity(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("sensitivity", value)
                _uiState.update { it.copy(sensitivity = value) }
            })
        }
    }

    fun toggleAcceleration() {
        viewModelScope.launch {
            val current = _uiState.value.accelerationEnabled
            saveAndUpdate({
                prefs.putBoolean("acceleration_enabled", !current)
                _uiState.update { it.copy(accelerationEnabled = !current) }
            }, "Acceleration ${if (!current) "enabled" else "disabled"}")
        }
    }

    fun updateAccelerationFactor(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("acceleration_factor", value)
                _uiState.update { it.copy(accelerationFactor = value) }
            })
        }
    }

    fun toggleInvertX() {
        viewModelScope.launch {
            val current = _uiState.value.invertX
            saveAndUpdate({
                prefs.putBoolean("invert_x", !current)
                _uiState.update { it.copy(invertX = !current) }
            }, "X-axis ${if (!current) "inverted" else "normalized"}")
        }
    }

    fun toggleInvertY() {
        viewModelScope.launch {
            val current = _uiState.value.invertY
            saveAndUpdate({
                prefs.putBoolean("invert_y", !current)
                _uiState.update { it.copy(invertY = !current) }
            }, "Y-axis ${if (!current) "inverted" else "normalized"}")
        }
    }

    fun toggleSmoothing() {
        viewModelScope.launch {
            val current = _uiState.value.smoothingEnabled
            saveAndUpdate({
                prefs.putBoolean("smoothing_enabled", !current)
                _uiState.update { it.copy(smoothingEnabled = !current) }
            })
        }
    }

    fun updateSmoothingFactor(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("smoothing_factor", value)
                _uiState.update { it.copy(smoothingFactor = value) }
            })
        }
    }

    // ==================== GESTURE SETTINGS ====================

    fun updateClickThreshold(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("click_threshold", value)
                _uiState.update { it.copy(clickThreshold = value) }
            })
        }
    }

    fun updateDoubleClickInterval(value: Long) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putLong("double_click_interval", value)
                _uiState.update { it.copy(doubleClickInterval = value) }
            })
        }
    }

    fun updateScrollThreshold(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("scroll_threshold", value)
                _uiState.update { it.copy(scrollThreshold = value) }
            })
        }
    }

    fun updateRightClickTilt(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("right_click_tilt", value)
                _uiState.update { it.copy(rightClickTilt = value) }
            })
        }
    }

    fun updateRightClickDuration(value: Long) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putLong("right_click_duration", value)
                _uiState.update { it.copy(rightClickDuration = value) }
            })
        }
    }

    // ==================== AI & PREDICTIVE SETTINGS ====================

    fun toggleAiSmoothing() {
        viewModelScope.launch {
            val current = _uiState.value.aiSmoothing
            saveAndUpdate({
                prefs.putBoolean("ai_smoothing", !current)
                _uiState.update { it.copy(aiSmoothing = !current) }
            }, "AI Smoothing ${if (!current) "enabled" else "disabled"}")
        }
    }

    fun updateAiBlendFactor(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("ai_blend_factor", value)
                _uiState.update { it.copy(aiBlendFactor = value) }
            })
        }
    }

    fun togglePredictive() {
        viewModelScope.launch {
            val current = _uiState.value.predictive
            saveAndUpdate({
                prefs.putBoolean("predictive_movement", !current)
                _uiState.update { it.copy(predictive = !current) }
            }, "Predictive ${if (!current) "enabled" else "disabled"}")
        }
    }

    fun updatePredictionStrength(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("prediction_strength", value)
                _uiState.update { it.copy(predictionStrength = value) }
            })
        }
    }

    fun toggleKalman() {
        viewModelScope.launch {
            val current = _uiState.value.kalmanEnabled
            saveAndUpdate({
                prefs.putBoolean("kalman_enabled", !current)
                _uiState.update { it.copy(kalmanEnabled = !current) }
            })
        }
    }

    // ==================== HAPTIC & FEEDBACK SETTINGS ====================

    fun toggleHaptic() {
        viewModelScope.launch {
            val current = _uiState.value.hapticEnabled
            saveAndUpdate({
                prefs.putBoolean("haptic_enabled", !current)
                _uiState.update { it.copy(hapticEnabled = !current) }
            }, "Haptic ${if (!current) "enabled" else "disabled"}")
        }
    }

    fun updateHapticStrength(strength: HapticStrength) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putString("haptic_strength", strength.name)
                _uiState.update { it.copy(hapticStrength = strength) }
            }, "Haptic strength: ${strength.displayName}")
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            val current = _uiState.value.soundEnabled
            saveAndUpdate({
                prefs.putBoolean("sound_enabled", !current)
                _uiState.update { it.copy(soundEnabled = !current) }
            }, "Sound ${if (!current) "enabled" else "disabled"}")
        }
    }

    fun toggleVisualFeedback() {
        viewModelScope.launch {
            val current = _uiState.value.visualFeedback
            saveAndUpdate({
                prefs.putBoolean("visual_feedback", !current)
                _uiState.update { it.copy(visualFeedback = !current) }
            })
        }
    }

    fun toggleNotificationOnGesture() {
        viewModelScope.launch {
            val current = _uiState.value.notificationOnGesture
            saveAndUpdate({
                prefs.putBoolean("notification_on_gesture", !current)
                _uiState.update { it.copy(notificationOnGesture = !current) }
            })
        }
    }

    // ==================== DISPLAY SETTINGS ====================

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putString("theme", theme)
                _uiState.update { it.copy(theme = theme) }
            }, "Theme: $theme")
        }
    }

    fun toggleDynamicColors() {
        viewModelScope.launch {
            val current = _uiState.value.useDynamicColors
            saveAndUpdate({
                prefs.putBoolean("dynamic_colors", !current)
                _uiState.update { it.copy(useDynamicColors = !current) }
            })
        }
    }

    fun updateFontSize(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("font_size", value)
                _uiState.update { it.copy(fontSize = value) }
            })
        }
    }

    fun toggleDebugInfo() {
        viewModelScope.launch {
            val current = _uiState.value.showDebugInfo
            saveAndUpdate({
                prefs.putBoolean("show_debug_info", !current)
                _uiState.update { it.copy(showDebugInfo = !current) }
            })
        }
    }

    fun toggleKeepScreenOn() {
        viewModelScope.launch {
            val current = _uiState.value.keepScreenOn
            saveAndUpdate({
                prefs.putBoolean("keep_screen_on", !current)
                _uiState.update { it.copy(keepScreenOn = !current) }
            })
        }
    }

    fun toggleShowFps() {
        viewModelScope.launch {
            val current = _uiState.value.showFps
            saveAndUpdate({
                prefs.putBoolean("show_fps", !current)
                _uiState.update { it.copy(showFps = !current) }
            })
        }
    }

    // ==================== CONNECTION SETTINGS ====================

    fun toggleAutoConnect() {
        viewModelScope.launch {
            val current = _uiState.value.autoConnect
            saveAndUpdate({
                prefs.putBoolean("auto_connect", !current)
                _uiState.update { it.copy(autoConnect = !current) }
            })
        }
    }

    fun updateReconnectAttempts(value: Int) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putInt("reconnect_attempts", value)
                _uiState.update { it.copy(reconnectAttempts = value) }
            })
        }
    }

    fun updateConnectionTimeout(value: Int) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putInt("connection_timeout", value)
                _uiState.update { it.copy(connectionTimeout = value) }
            })
        }
    }

    fun toggleUseWebSocket() {
        viewModelScope.launch {
            val current = _uiState.value.useWebSocket
            saveAndUpdate({
                prefs.putBoolean("use_websocket", !current)
                _uiState.update { it.copy(useWebSocket = !current) }
            })
        }
    }

    fun toggleUdpDiscovery() {
        viewModelScope.launch {
            val current = _uiState.value.useUdpDiscovery
            saveAndUpdate({
                prefs.putBoolean("use_udp_discovery", !current)
                _uiState.update { it.copy(useUdpDiscovery = !current) }
            })
        }
    }

    // ==================== PRIVACY & DATA SETTINGS ====================

    fun toggleAnonymousStats() {
        viewModelScope.launch {
            val current = _uiState.value.anonymousStats
            saveAndUpdate({
                prefs.putBoolean("anonymous_stats", !current)
                _uiState.update { it.copy(anonymousStats = !current) }
            })
        }
    }

    fun toggleCrashReporting() {
        viewModelScope.launch {
            val current = _uiState.value.crashReporting
            saveAndUpdate({
                prefs.putBoolean("crash_reporting", !current)
                _uiState.update { it.copy(crashReporting = !current) }
            })
        }
    }

    fun toggleClearDataOnExit() {
        viewModelScope.launch {
            val current = _uiState.value.clearDataOnExit
            saveAndUpdate({
                prefs.putBoolean("clear_data_on_exit", !current)
                _uiState.update { it.copy(clearDataOnExit = !current) }
            })
        }
    }

    // ==================== PRESENTATION SETTINGS ====================

    fun togglePresentationMode() {
        viewModelScope.launch {
            val current = _uiState.value.presentationModeEnabled
            saveAndUpdate({
                prefs.putBoolean("presentation_mode_enabled", !current)
                _uiState.update { it.copy(presentationModeEnabled = !current) }
            }, "Presentation mode ${if (!current) "enabled" else "disabled"}")
        }
    }

    fun updateLaserPointerSpeed(value: Float) {
        viewModelScope.launch {
            saveAndUpdate({
                prefs.putFloat("laser_pointer_speed", value)
                _uiState.update { it.copy(laserPointerSpeed = value) }
            })
        }
    }

    fun toggleShowPresentationTimer() {
        viewModelScope.launch {
            val current = _uiState.value.showPresentationTimer
            saveAndUpdate({
                prefs.putBoolean("show_presentation_timer", !current)
                _uiState.update { it.copy(showPresentationTimer = !current) }
            })
        }
    }

    fun toggleAutoHideLaser() {
        viewModelScope.launch {
            val current = _uiState.value.autoHideLaser
            saveAndUpdate({
                prefs.putBoolean("auto_hide_laser", !current)
                _uiState.update { it.copy(autoHideLaser = !current) }
            })
        }
    }

    // ==================== ACTIONS ====================

    fun saveAllSettings() {
        viewModelScope.launch {
            saveAndUpdate({
                showToast("All settings saved successfully!")
            })
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            saveAndUpdate({
                val defaultState = SettingsUiState()
                _uiState.value = defaultState
                // Reset all preferences
                prefs.clear()
                // Save default values
                prefs.putFloat("sensitivity", defaultState.sensitivity)
                prefs.putBoolean("acceleration_enabled", defaultState.accelerationEnabled)
                prefs.putFloat("acceleration_factor", defaultState.accelerationFactor)
                prefs.putBoolean("invert_x", defaultState.invertX)
                prefs.putBoolean("invert_y", defaultState.invertY)
                prefs.putBoolean("smoothing_enabled", defaultState.smoothingEnabled)
                prefs.putFloat("smoothing_factor", defaultState.smoothingFactor)
                prefs.putFloat("click_threshold", defaultState.clickThreshold)
                prefs.putLong("double_click_interval", defaultState.doubleClickInterval)
                prefs.putFloat("scroll_threshold", defaultState.scrollThreshold)
                prefs.putFloat("right_click_tilt", defaultState.rightClickTilt)
                prefs.putLong("right_click_duration", defaultState.rightClickDuration)
                prefs.putBoolean("ai_smoothing", defaultState.aiSmoothing)
                prefs.putFloat("ai_blend_factor", defaultState.aiBlendFactor)
                prefs.putBoolean("predictive_movement", defaultState.predictive)
                prefs.putFloat("prediction_strength", defaultState.predictionStrength)
                prefs.putBoolean("kalman_enabled", defaultState.kalmanEnabled)
                prefs.putBoolean("haptic_enabled", defaultState.hapticEnabled)
                prefs.putString("haptic_strength", defaultState.hapticStrength.name)
                prefs.putBoolean("sound_enabled", defaultState.soundEnabled)
                prefs.putBoolean("visual_feedback", defaultState.visualFeedback)
                prefs.putBoolean("notification_on_gesture", defaultState.notificationOnGesture)
                prefs.putString("theme", defaultState.theme)
                prefs.putBoolean("dynamic_colors", defaultState.useDynamicColors)
                prefs.putFloat("font_size", defaultState.fontSize)
                prefs.putBoolean("show_debug_info", defaultState.showDebugInfo)
                prefs.putBoolean("keep_screen_on", defaultState.keepScreenOn)
                prefs.putBoolean("show_fps", defaultState.showFps)
                prefs.putBoolean("auto_connect", defaultState.autoConnect)
                prefs.putInt("reconnect_attempts", defaultState.reconnectAttempts)
                prefs.putInt("connection_timeout", defaultState.connectionTimeout)
                prefs.putBoolean("use_websocket", defaultState.useWebSocket)
                prefs.putBoolean("use_udp_discovery", defaultState.useUdpDiscovery)
                prefs.putBoolean("anonymous_stats", defaultState.anonymousStats)
                prefs.putBoolean("crash_reporting", defaultState.crashReporting)
                prefs.putBoolean("clear_data_on_exit", defaultState.clearDataOnExit)
                prefs.putBoolean("presentation_mode_enabled", defaultState.presentationModeEnabled)
                prefs.putFloat("laser_pointer_speed", defaultState.laserPointerSpeed)
                prefs.putBoolean("show_presentation_timer", defaultState.showPresentationTimer)
                prefs.putBoolean("auto_hide_laser", defaultState.autoHideLaser)
                showToast("All settings reset to defaults!")
            })
        }
    }

    fun exportSettings() {
        viewModelScope.launch {
            saveAndUpdate({
                val state = _uiState.value
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "airmouse_settings_$timestamp.txt"
                val file = File(context.filesDir, fileName)

                val content = buildString {
                    appendLine("=== Air Mouse Pro Settings Export ===")
                    appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                    appendLine()
                    appendLine("=== Cursor Settings ===")
                    appendLine("Sensitivity: ${state.sensitivity}")
                    appendLine("Acceleration: ${state.accelerationEnabled}")
                    appendLine("Acceleration Factor: ${state.accelerationFactor}")
                    appendLine("Invert X: ${state.invertX}")
                    appendLine("Invert Y: ${state.invertY}")
                    appendLine("Smoothing: ${state.smoothingEnabled}")
                    appendLine("Smoothing Factor: ${state.smoothingFactor}")
                    appendLine()
                    appendLine("=== Gesture Settings ===")
                    appendLine("Click Threshold: ${state.clickThreshold}")
                    appendLine("Double Click Interval: ${state.doubleClickInterval}")
                    appendLine("Scroll Threshold: ${state.scrollThreshold}")
                    appendLine("Right Click Tilt: ${state.rightClickTilt}")
                    appendLine("Right Click Duration: ${state.rightClickDuration}")
                    appendLine()
                    appendLine("=== AI & Predictive ===")
                    appendLine("AI Smoothing: ${state.aiSmoothing}")
                    appendLine("AI Blend Factor: ${state.aiBlendFactor}")
                    appendLine("Predictive: ${state.predictive}")
                    appendLine("Prediction Strength: ${state.predictionStrength}")
                    appendLine("Kalman Filter: ${state.kalmanEnabled}")
                    appendLine()
                    appendLine("=== Haptic & Feedback ===")
                    appendLine("Haptic: ${state.hapticEnabled}")
                    appendLine("Haptic Strength: ${state.hapticStrength.displayName}")
                    appendLine("Sound: ${state.soundEnabled}")
                    appendLine("Visual Feedback: ${state.visualFeedback}")
                    appendLine("Notification on Gesture: ${state.notificationOnGesture}")
                    appendLine()
                    appendLine("=== Display Settings ===")
                    appendLine("Theme: ${state.theme}")
                    appendLine("Dynamic Colors: ${state.useDynamicColors}")
                    appendLine("Font Size: ${state.fontSize}")
                    appendLine("Debug Info: ${state.showDebugInfo}")
                    appendLine("Keep Screen On: ${state.keepScreenOn}")
                    appendLine("Show FPS: ${state.showFps}")
                    appendLine()
                    appendLine("=== Connection Settings ===")
                    appendLine("Auto Connect: ${state.autoConnect}")
                    appendLine("Reconnect Attempts: ${state.reconnectAttempts}")
                    appendLine("Connection Timeout: ${state.connectionTimeout}")
                    appendLine("Use WebSocket: ${state.useWebSocket}")
                    appendLine("UDP Discovery: ${state.useUdpDiscovery}")
                    appendLine()
                    appendLine("=== Privacy & Data ===")
                    appendLine("Anonymous Stats: ${state.anonymousStats}")
                    appendLine("Crash Reporting: ${state.crashReporting}")
                    appendLine("Clear Data on Exit: ${state.clearDataOnExit}")
                    appendLine()
                    appendLine("=== Presentation Settings ===")
                    appendLine("Presentation Mode: ${state.presentationModeEnabled}")
                    appendLine("Laser Pointer Speed: ${state.laserPointerSpeed}")
                    appendLine("Show Timer: ${state.showPresentationTimer}")
                    appendLine("Auto Hide Laser: ${state.autoHideLaser}")
                }

                file.writeText(content)
                showToast("Settings exported to: $fileName")
            })
        }
    }

    fun importSettings() {
        viewModelScope.launch {
            showToast("Import settings - select a settings file")
        }
    }

    // ==================== URL OPENING ====================

    fun openWebsite() = openUrl("https://airmouse.io")
    fun openPrivacyPolicy() = openUrl("https://airmouse.io/privacy")
    fun openLicense() = openUrl("https://airmouse.io/license")
    fun openGitHub() = openUrl("https://github.com/airmouse/airmouse-android")
    fun openSupport() = openUrl("mailto:support@airmouse.io")

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModelScope.launch {
                showToast("Cannot open URL: ${e.message}")
            }
        }
    }

    // ==================== SYSTEM SETTINGS ====================

    fun openSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModelScope.launch {
                showToast("Cannot open system settings")
            }
        }
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModelScope.launch {
                showToast("Cannot open accessibility settings")
            }
        }
    }

    // ==================== DATA MANAGEMENT ====================

    fun clearCache() {
        viewModelScope.launch {
            try {
                val cacheDir = context.cacheDir
                cacheDir.deleteRecursively()
                showToast("Cache cleared")
            } catch (e: Exception) {
                showToast("Error clearing cache: ${e.message}")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                prefs.clear()
                val filesDir = context.filesDir
                filesDir.deleteRecursively()
                loadAllSettings()
                showToast("All data cleared")
            } catch (e: Exception) {
                showToast("Error clearing data: ${e.message}")
            }
        }
    }

    // ==================== STATUS & CLEANUP ====================

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(success = null) }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "Model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android" to Build.VERSION.RELEASE,
            "SDK" to Build.VERSION.SDK_INT.toString(),
            "App Version" to "3.0.0"
        )
    }

    fun updateUiState(update: SettingsUiState) {
        _uiState.value = update
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
    }
}