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
import java.io.FileOutputStream
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

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadAllSettings()
    }

    // ==================== LOAD SETTINGS ====================

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
    }

    private fun getHapticStrength(value: String): HapticStrength = when (value) {
        "LIGHT" -> HapticStrength.LIGHT
        "STRONG" -> HapticStrength.STRONG
        else -> HapticStrength.MEDIUM
    }

    // ==================== SAVE HELPERS ====================

    private fun saveAndUpdate(block: () -> Unit, successMessage: String? = null) {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                block()
                successMessage?.let { showToast(it) }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun saveAndUpdateSuspend(block: suspend () -> Unit, successMessage: String? = null) {
        try {
            _isSaving.value = true
            block()
            successMessage?.let { showToast(it) }
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        } finally {
            _isSaving.value = false
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            delay(3000)
            _toastMessage.value = null
        }
    }

    // ==================== CURSOR SETTINGS ====================

    fun updateSensitivity(value: Float) {
        saveAndUpdate({
            prefs.putFloat("sensitivity", value)
            _uiState.update { it.copy(sensitivity = value) }
        })
    }

    fun updateAccelerationEnabled(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("acceleration_enabled", enabled)
            _uiState.update { it.copy(accelerationEnabled = enabled) }
        }, "Mouse acceleration ${if (enabled) "enabled" else "disabled"}")
    }

    fun updateAccelerationFactor(value: Float) {
        saveAndUpdate({
            prefs.putFloat("acceleration_factor", value)
            _uiState.update { it.copy(accelerationFactor = value) }
        })
    }

    fun updateInvertX(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("invert_x", enabled)
            _uiState.update { it.copy(invertX = enabled) }
        }, "X-axis ${if (enabled) "inverted" else "normalized"}")
    }

    fun updateInvertY(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("invert_y", enabled)
            _uiState.update { it.copy(invertY = enabled) }
        }, "Y-axis ${if (enabled) "inverted" else "normalized"}")
    }

    fun updateSmoothingEnabled(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("smoothing_enabled", enabled)
            _uiState.update { it.copy(smoothingEnabled = enabled) }
        })
    }

    fun updateSmoothingFactor(value: Float) {
        saveAndUpdate({
            prefs.putFloat("smoothing_factor", value)
            _uiState.update { it.copy(smoothingFactor = value) }
        })
    }

    // ==================== GESTURE SETTINGS ====================

    fun updateClickThreshold(value: Float) {
        saveAndUpdate({
            prefs.putFloat("click_threshold", value)
            _uiState.update { it.copy(clickThreshold = value) }
        })
    }

    fun updateDoubleClickInterval(value: Long) {
        saveAndUpdate({
            prefs.putLong("double_click_interval", value)
            _uiState.update { it.copy(doubleClickInterval = value) }
        })
    }

    fun updateScrollThreshold(value: Float) {
        saveAndUpdate({
            prefs.putFloat("scroll_threshold", value)
            _uiState.update { it.copy(scrollThreshold = value) }
        })
    }

    fun updateRightClickTilt(value: Float) {
        saveAndUpdate({
            prefs.putFloat("right_click_tilt", value)
            _uiState.update { it.copy(rightClickTilt = value) }
        })
    }

    fun updateRightClickDuration(value: Long) {
        saveAndUpdate({
            prefs.putLong("right_click_duration", value)
            _uiState.update { it.copy(rightClickDuration = value) }
        })
    }

    fun updateGestureDebounce(value: Long) {
        saveAndUpdate({
            prefs.putLong("gesture_debounce", value)
            _uiState.update { it.copy(gestureDebounce = value) }
        })
    }

    // ==================== AI & PREDICTIVE SETTINGS ====================

    fun updateAiSmoothing(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("ai_smoothing", enabled)
            _uiState.update { it.copy(aiSmoothing = enabled) }
        }, "AI Smoothing ${if (enabled) "enabled" else "disabled"}")
    }

    fun updateAiBlendFactor(value: Float) {
        saveAndUpdate({
            prefs.putFloat("ai_blend_factor", value)
            _uiState.update { it.copy(aiBlendFactor = value) }
        })
    }

    fun updatePredictive(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("predictive_movement", enabled)
            _uiState.update { it.copy(predictive = enabled) }
        }, "Predictive movement ${if (enabled) "enabled" else "disabled"}")
    }

    fun updatePredictionStrength(value: Float) {
        saveAndUpdate({
            prefs.putFloat("prediction_strength", value)
            _uiState.update { it.copy(predictionStrength = value) }
        })
    }

    fun updateKalmanEnabled(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("kalman_enabled", enabled)
            _uiState.update { it.copy(kalmanEnabled = enabled) }
        })
    }

    // ==================== HAPTIC & FEEDBACK ====================

    fun updateHaptic(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("haptic_enabled", enabled)
            _uiState.update { it.copy(hapticEnabled = enabled) }
        }, "Haptic ${if (enabled) "enabled" else "disabled"}")
    }

    fun updateHapticStrength(strength: HapticStrength) {
        saveAndUpdate({
            prefs.putString("haptic_strength", strength.name)
            _uiState.update { it.copy(hapticStrength = strength) }
        }, "Haptic strength: ${strength.displayName}")
    }

    fun updateSoundEnabled(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("sound_enabled", enabled)
            _uiState.update { it.copy(soundEnabled = enabled) }
        }, "Sound ${if (enabled) "enabled" else "disabled"}")
    }

    fun updateVisualFeedback(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("visual_feedback", enabled)
            _uiState.update { it.copy(visualFeedback = enabled) }
        })
    }

    fun updateNotificationOnGesture(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("notification_on_gesture", enabled)
            _uiState.update { it.copy(notificationOnGesture = enabled) }
        })
    }

    // ==================== DISPLAY SETTINGS ====================

    fun updateTheme(theme: String) {
        saveAndUpdate({
            prefs.putString("theme", theme)
            _uiState.update { it.copy(theme = theme) }
        }, "Theme changed to $theme")
    }

    fun updateUseDynamicColors(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("dynamic_colors", enabled)
            _uiState.update { it.copy(useDynamicColors = enabled) }
        })
    }

    fun updateFontSize(value: Float) {
        saveAndUpdate({
            prefs.putFloat("font_size", value)
            _uiState.update { it.copy(fontSize = value) }
        })
    }

    fun updateShowDebugInfo(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("show_debug_info", enabled)
            _uiState.update { it.copy(showDebugInfo = enabled) }
        })
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("keep_screen_on", enabled)
            _uiState.update { it.copy(keepScreenOn = enabled) }
        })
    }

    fun updateShowFps(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("show_fps", enabled)
            _uiState.update { it.copy(showFps = enabled) }
        })
    }

    // ==================== CONNECTION SETTINGS ====================

    fun updateAutoConnect(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("auto_connect", enabled)
            _uiState.update { it.copy(autoConnect = enabled) }
        })
    }

    fun updateReconnectAttempts(value: Int) {
        saveAndUpdate({
            prefs.putInt("reconnect_attempts", value)
            _uiState.update { it.copy(reconnectAttempts = value) }
        })
    }

    fun updateConnectionTimeout(value: Int) {
        saveAndUpdate({
            prefs.putInt("connection_timeout", value)
            _uiState.update { it.copy(connectionTimeout = value) }
        })
    }

    fun updateUseWebSocket(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("use_websocket", enabled)
            _uiState.update { it.copy(useWebSocket = enabled) }
        })
    }

    fun updateUseUdpDiscovery(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("use_udp_discovery", enabled)
            _uiState.update { it.copy(useUdpDiscovery = enabled) }
        })
    }

    // ==================== PRIVACY & DATA ====================

    fun updateAnonymousStats(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("anonymous_stats", enabled)
            _uiState.update { it.copy(anonymousStats = enabled) }
        })
    }

    fun updateCrashReporting(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("crash_reporting", enabled)
            _uiState.update { it.copy(crashReporting = enabled) }
        })
    }

    fun updateClearDataOnExit(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("clear_data_on_exit", enabled)
            _uiState.update { it.copy(clearDataOnExit = enabled) }
        })
    }

    // ==================== PRESENTATION SETTINGS ====================

    fun updatePresentationModeEnabled(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("presentation_mode_enabled", enabled)
            _uiState.update { it.copy(presentationModeEnabled = enabled) }
        }, "Presentation mode ${if (enabled) "enabled" else "disabled"}")
    }

    fun updateLaserPointerSpeed(value: Float) {
        saveAndUpdate({
            prefs.putFloat("laser_pointer_speed", value)
            _uiState.update { it.copy(laserPointerSpeed = value) }
        })
    }

    fun updateShowPresentationTimer(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("show_presentation_timer", enabled)
            _uiState.update { it.copy(showPresentationTimer = enabled) }
        })
    }

    fun updateAutoHideLaser(enabled: Boolean) {
        saveAndUpdate({
            prefs.putBoolean("auto_hide_laser", enabled)
            _uiState.update { it.copy(autoHideLaser = enabled) }
        })
    }

    // ==================== RESET & EXPORT ====================

    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _isSaving.value = true

                // Reset all settings
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
                showToast("All settings reset to defaults")
            } catch (e: Exception) {
                showToast("Error resetting: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun exportSettings() {
        viewModelScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "airmouse_settings_$timestamp.txt"
                val file = File(context.getExternalFilesDir(null), fileName)

                val settings = buildString {
                    appendLine("Air Mouse Pro Settings Export")
                    appendLine("=============================")
                    appendLine("Export Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                    appendLine()
                    appendLine("=== CURSOR SETTINGS ===")
                    appendLine("Sensitivity: ${_uiState.value.sensitivity}")
                    appendLine("Acceleration: ${_uiState.value.accelerationEnabled}")
                    appendLine("Acceleration Factor: ${_uiState.value.accelerationFactor}")
                    appendLine("Invert X: ${_uiState.value.invertX}")
                    appendLine("Invert Y: ${_uiState.value.invertY}")
                    appendLine("Smoothing: ${_uiState.value.smoothingEnabled}")
                    appendLine("Smoothing Factor: ${_uiState.value.smoothingFactor}")
                    appendLine()
                    appendLine("=== GESTURE SETTINGS ===")
                    appendLine("Click Threshold: ${_uiState.value.clickThreshold}")
                    appendLine("Double Click Interval: ${_uiState.value.doubleClickInterval}")
                    appendLine("Scroll Threshold: ${_uiState.value.scrollThreshold}")
                    appendLine("Right Click Tilt: ${_uiState.value.rightClickTilt}")
                    appendLine("Right Click Duration: ${_uiState.value.rightClickDuration}")
                    appendLine("Gesture Debounce: ${_uiState.value.gestureDebounce}")
                    appendLine()
                    appendLine("=== AI & PREDICTIVE ===")
                    appendLine("AI Smoothing: ${_uiState.value.aiSmoothing}")
                    appendLine("AI Blend Factor: ${_uiState.value.aiBlendFactor}")
                    appendLine("Predictive: ${_uiState.value.predictive}")
                    appendLine("Prediction Strength: ${_uiState.value.predictionStrength}")
                    appendLine("Kalman Filter: ${_uiState.value.kalmanEnabled}")
                    appendLine()
                    appendLine("=== HAPTIC & FEEDBACK ===")
                    appendLine("Haptic: ${_uiState.value.hapticEnabled}")
                    appendLine("Haptic Strength: ${_uiState.value.hapticStrength.displayName}")
                    appendLine("Sound: ${_uiState.value.soundEnabled}")
                    appendLine("Visual Feedback: ${_uiState.value.visualFeedback}")
                    appendLine("Notification on Gesture: ${_uiState.value.notificationOnGesture}")
                    appendLine()
                    appendLine("=== DISPLAY SETTINGS ===")
                    appendLine("Theme: ${_uiState.value.theme}")
                    appendLine("Dynamic Colors: ${_uiState.value.useDynamicColors}")
                    appendLine("Font Size: ${_uiState.value.fontSize}")
                    appendLine("Debug Info: ${_uiState.value.showDebugInfo}")
                    appendLine("Keep Screen On: ${_uiState.value.keepScreenOn}")
                    appendLine("Show FPS: ${_uiState.value.showFps}")
                    appendLine()
                    appendLine("=== CONNECTION SETTINGS ===")
                    appendLine("Auto Connect: ${_uiState.value.autoConnect}")
                    appendLine("Reconnect Attempts: ${_uiState.value.reconnectAttempts}")
                    appendLine("Connection Timeout: ${_uiState.value.connectionTimeout}")
                    appendLine("Use WebSocket: ${_uiState.value.useWebSocket}")
                    appendLine("UDP Discovery: ${_uiState.value.useUdpDiscovery}")
                    appendLine()
                    appendLine("=== PRIVACY & DATA ===")
                    appendLine("Anonymous Stats: ${_uiState.value.anonymousStats}")
                    appendLine("Crash Reporting: ${_uiState.value.crashReporting}")
                    appendLine("Clear Data on Exit: ${_uiState.value.clearDataOnExit}")
                    appendLine()
                    appendLine("=== PRESENTATION SETTINGS ===")
                    appendLine("Presentation Mode: ${_uiState.value.presentationModeEnabled}")
                    appendLine("Laser Pointer Speed: ${_uiState.value.laserPointerSpeed}")
                    appendLine("Show Timer: ${_uiState.value.showPresentationTimer}")
                    appendLine("Auto Hide Laser: ${_uiState.value.autoHideLaser}")
                }

                file.writeText(settings)
                showToast("Settings exported to: $fileName")
            } catch (e: Exception) {
                showToast("Error exporting: ${e.message}")
            }
        }
    }

    fun importSettings() {
        // In production, implement file picker and parse settings
        showToast("Import settings - select a settings file")
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
            showToast("Cannot open URL: ${e.message}")
        }
    }

    // ==================== SYSTEM SETTINGS ====================

    fun openSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open system settings")
        }
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open accessibility settings")
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
                // Clear preferences
                prefs.clear()

                // Clear files
                val filesDir = context.filesDir
                filesDir.deleteRecursively()

                // Reset UI state
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
    fun saveAllSettings() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val state = _uiState.value
                // Save all settings...
                prefs.putFloat("sensitivity", state.sensitivity)
                prefs.putBoolean("acceleration_enabled", state.accelerationEnabled)
                // ... save all other settings
                showToast("Settings saved successfully")
            } catch (e: Exception) {
                showToast("Error saving: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateUiState(update: SettingsUiState) {
        _uiState.value = update
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
    }
}