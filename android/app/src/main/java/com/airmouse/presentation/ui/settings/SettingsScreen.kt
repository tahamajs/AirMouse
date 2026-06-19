// app/src/main/java/com/airmouse/presentation/ui/settings/SettingsScreen.kt
package com.airmouse.presentation.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.features.MouseControlFeature
import com.airmouse.presentation.navigation.NavigationActions
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
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ==================== ENUMS ====================

enum class SettingsSection(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    CURSOR("Cursor", "Movement and sensitivity", Icons.Default.Mouse),
    GESTURE("Gesture", "Click and scroll detection", Icons.Default.Gesture),
    AI("AI & Predictive", "Smart movement prediction", Icons.Default.Psychology),
    HAPTIC("Haptic & Sound", "Feedback preferences", Icons.Default.Vibration),
    DISPLAY("Display", "Theme and appearance", Icons.Default.DisplaySettings),
    CONNECTION("Connection", "Network settings", Icons.Default.Wifi),
    PRIVACY("Privacy & Data", "Your data preferences", Icons.Default.PrivacyTip),
    PRESENTATION("Presentation", "Slide control settings", Icons.Default.Slideshow),
    ABOUT("About", "App information", Icons.Default.Info)
}

enum class HapticStrength(val displayName: String, val duration: Long) {
    LIGHT("Light", 20),
    MEDIUM("Medium", 50),
    STRONG("Strong", 80)
}

// ==================== UI STATE ====================

data class SettingsUiState(
    // Cursor Settings
    val sensitivity: Float = 0.5f,
    val accelerationEnabled: Boolean = false,
    val accelerationFactor: Float = 1.5f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val smoothingEnabled: Boolean = true,
    val smoothingFactor: Float = 0.5f,

    // Gesture Settings
    val clickThreshold: Float = 10f,
    val doubleClickInterval: Long = 300,
    val scrollThreshold: Float = 5f,
    val rightClickTilt: Float = 15f,
    val rightClickDuration: Long = 500,
    val gestureDebounce: Long = 100,

    // AI & Predictive Settings
    val aiSmoothing: Boolean = false,
    val aiBlendFactor: Float = 0.7f,
    val predictive: Boolean = true,
    val predictionStrength: Float = 0.5f,
    val kalmanEnabled: Boolean = true,

    // Haptic & Feedback
    val hapticEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val soundEnabled: Boolean = false,
    val visualFeedback: Boolean = true,
    val notificationOnGesture: Boolean = false,

    // Display Settings
    val theme: String = "system",
    val useDynamicColors: Boolean = true,
    val fontSize: Float = 16f,
    val showDebugInfo: Boolean = false,
    val keepScreenOn: Boolean = false,
    val showFps: Boolean = false,

    // Connection Settings
    val autoConnect: Boolean = true,
    val reconnectAttempts: Int = 5,
    val connectionTimeout: Int = 5000,
    val useWebSocket: Boolean = true,
    val useUdpDiscovery: Boolean = true,

    // Privacy & Data
    val anonymousStats: Boolean = true,
    val crashReporting: Boolean = true,
    val clearDataOnExit: Boolean = false,

    // Presentation Settings
    val presentationModeEnabled: Boolean = false,
    val laserPointerSpeed: Float = 1.0f,
    val showPresentationTimer: Boolean = true,
    val autoHideLaser: Boolean = true,

    // Status
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val isSaving: Boolean = false
)

// ==================== EVENTS ====================

sealed class SettingsEvent {
    // Cursor Events
    data class UpdateSensitivity(val value: Float) : SettingsEvent()
    object ToggleAcceleration : SettingsEvent()
    data class UpdateAccelerationFactor(val value: Float) : SettingsEvent()
    object ToggleInvertX : SettingsEvent()
    object ToggleInvertY : SettingsEvent()
    object ToggleSmoothing : SettingsEvent()
    data class UpdateSmoothingFactor(val value: Float) : SettingsEvent()

    // Gesture Events
    data class UpdateClickThreshold(val value: Float) : SettingsEvent()
    data class UpdateDoubleClickInterval(val value: Long) : SettingsEvent()
    data class UpdateScrollThreshold(val value: Float) : SettingsEvent()
    data class UpdateRightClickTilt(val value: Float) : SettingsEvent()
    data class UpdateRightClickDuration(val value: Long) : SettingsEvent()

    // AI Events
    object ToggleAiSmoothing : SettingsEvent()
    data class UpdateAiBlendFactor(val value: Float) : SettingsEvent()
    object TogglePredictive : SettingsEvent()
    data class UpdatePredictionStrength(val value: Float) : SettingsEvent()
    object ToggleKalman : SettingsEvent()

    // Haptic Events
    object ToggleHaptic : SettingsEvent()
    data class UpdateHapticStrength(val strength: HapticStrength) : SettingsEvent()
    object ToggleSound : SettingsEvent()
    object ToggleVisualFeedback : SettingsEvent()
    object ToggleNotificationOnGesture : SettingsEvent()

    // Display Events
    data class UpdateTheme(val theme: String) : SettingsEvent()
    object ToggleDynamicColors : SettingsEvent()
    data class UpdateFontSize(val value: Float) : SettingsEvent()
    object ToggleDebugInfo : SettingsEvent()
    object ToggleKeepScreenOn : SettingsEvent()
    object ToggleShowFps : SettingsEvent()

    // Connection Events
    object ToggleAutoConnect : SettingsEvent()
    data class UpdateReconnectAttempts(val value: Int) : SettingsEvent()
    data class UpdateConnectionTimeout(val value: Int) : SettingsEvent()
    object ToggleUseWebSocket : SettingsEvent()
    object ToggleUdpDiscovery : SettingsEvent()

    // Privacy Events
    object ToggleAnonymousStats : SettingsEvent()
    object ToggleCrashReporting : SettingsEvent()
    object ToggleClearDataOnExit : SettingsEvent()

    // Presentation Events
    object TogglePresentationMode : SettingsEvent()
    data class UpdateLaserPointerSpeed(val value: Float) : SettingsEvent()
    object ToggleShowPresentationTimer : SettingsEvent()
    object ToggleAutoHideLaser : SettingsEvent()

    // Actions
    object SaveSettings : SettingsEvent()
    object ResetDefaults : SettingsEvent()
    object ExportSettings : SettingsEvent()
    object ImportSettings : SettingsEvent()
    object ClearCache : SettingsEvent()
    object ClearAllData : SettingsEvent()
    object OpenSystemSettings : SettingsEvent()
    object OpenAccessibilitySettings : SettingsEvent()
    object OpenWebsite : SettingsEvent()
    object OpenPrivacyPolicy : SettingsEvent()
    object OpenLicense : SettingsEvent()
    object OpenGitHub : SettingsEvent()
    object OpenSupport : SettingsEvent()

    data class ShowToast(val message: String) : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
}

// ==================== VIEWMODEL ====================

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val mouseControlFeature: MouseControlFeature
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
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

                // AI Settings
                aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                aiBlendFactor = prefs.getFloat("ai_blend_factor", 0.7f),
                predictive = prefs.getBoolean("predictive_movement", true),
                predictionStrength = prefs.getFloat("prediction_strength", 0.5f),
                kalmanEnabled = prefs.getBoolean("kalman_enabled", true),

                // Haptic Settings
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

                // Privacy Settings
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

    private suspend fun showToast(message: String) {
        _toastMessage.value = message
        delay(3000)
        _toastMessage.value = null
    }

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

    fun handleEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                // Cursor Events
                is SettingsEvent.UpdateSensitivity -> updateSensitivity(event.value)
                SettingsEvent.ToggleAcceleration -> toggleAcceleration()
                is SettingsEvent.UpdateAccelerationFactor -> updateAccelerationFactor(event.value)
                SettingsEvent.ToggleInvertX -> toggleInvertX()
                SettingsEvent.ToggleInvertY -> toggleInvertY()
                SettingsEvent.ToggleSmoothing -> toggleSmoothing()
                is SettingsEvent.UpdateSmoothingFactor -> updateSmoothingFactor(event.value)

                // Gesture Events
                is SettingsEvent.UpdateClickThreshold -> updateClickThreshold(event.value)
                is SettingsEvent.UpdateDoubleClickInterval -> updateDoubleClickInterval(event.value)
                is SettingsEvent.UpdateScrollThreshold -> updateScrollThreshold(event.value)
                is SettingsEvent.UpdateRightClickTilt -> updateRightClickTilt(event.value)
                is SettingsEvent.UpdateRightClickDuration -> updateRightClickDuration(event.value)

                // AI Events
                SettingsEvent.ToggleAiSmoothing -> toggleAiSmoothing()
                is SettingsEvent.UpdateAiBlendFactor -> updateAiBlendFactor(event.value)
                SettingsEvent.TogglePredictive -> togglePredictive()
                is SettingsEvent.UpdatePredictionStrength -> updatePredictionStrength(event.value)
                SettingsEvent.ToggleKalman -> toggleKalman()

                // Haptic Events
                SettingsEvent.ToggleHaptic -> toggleHaptic()
                is SettingsEvent.UpdateHapticStrength -> updateHapticStrength(event.strength)
                SettingsEvent.ToggleSound -> toggleSound()
                SettingsEvent.ToggleVisualFeedback -> toggleVisualFeedback()
                SettingsEvent.ToggleNotificationOnGesture -> toggleNotificationOnGesture()

                // Display Events
                is SettingsEvent.UpdateTheme -> updateTheme(event.theme)
                SettingsEvent.ToggleDynamicColors -> toggleDynamicColors()
                is SettingsEvent.UpdateFontSize -> updateFontSize(event.value)
                SettingsEvent.ToggleDebugInfo -> toggleDebugInfo()
                SettingsEvent.ToggleKeepScreenOn -> toggleKeepScreenOn()
                SettingsEvent.ToggleShowFps -> toggleShowFps()

                // Connection Events
                SettingsEvent.ToggleAutoConnect -> toggleAutoConnect()
                is SettingsEvent.UpdateReconnectAttempts -> updateReconnectAttempts(event.value)
                is SettingsEvent.UpdateConnectionTimeout -> updateConnectionTimeout(event.value)
                SettingsEvent.ToggleUseWebSocket -> toggleUseWebSocket()
                SettingsEvent.ToggleUdpDiscovery -> toggleUdpDiscovery()

                // Privacy Events
                SettingsEvent.ToggleAnonymousStats -> toggleAnonymousStats()
                SettingsEvent.ToggleCrashReporting -> toggleCrashReporting()
                SettingsEvent.ToggleClearDataOnExit -> toggleClearDataOnExit()

                // Presentation Events
                SettingsEvent.TogglePresentationMode -> togglePresentationMode()
                is SettingsEvent.UpdateLaserPointerSpeed -> updateLaserPointerSpeed(event.value)
                SettingsEvent.ToggleShowPresentationTimer -> toggleShowPresentationTimer()
                SettingsEvent.ToggleAutoHideLaser -> toggleAutoHideLaser()

                // Actions
                SettingsEvent.SaveSettings -> saveSettings()
                SettingsEvent.ResetDefaults -> resetDefaults()
                SettingsEvent.ExportSettings -> exportSettings()
                SettingsEvent.ImportSettings -> importSettings()
                SettingsEvent.ClearCache -> clearCache()
                SettingsEvent.ClearAllData -> clearAllData()
                SettingsEvent.OpenSystemSettings -> openSystemSettings()
                SettingsEvent.OpenAccessibilitySettings -> openAccessibilitySettings()
                SettingsEvent.OpenWebsite -> openUrl("https://airmouse.io")
                SettingsEvent.OpenPrivacyPolicy -> openUrl("https://airmouse.io/privacy")
                SettingsEvent.OpenLicense -> openUrl("https://airmouse.io/license")
                SettingsEvent.OpenGitHub -> openUrl("https://github.com/airmouse/airmouse-android")
                SettingsEvent.OpenSupport -> openUrl("mailto:support@airmouse.io")
                SettingsEvent.ShowToast -> showToast(event.message)
                SettingsEvent.ShowError -> _uiState.update { it.copy(error = event.message) }
            }
        }
    }

    // ==================== CURSOR METHODS ====================

    private suspend fun updateSensitivity(value: Float) {
        saveAndUpdate({
            prefs.putFloat("sensitivity", value)
            _uiState.update { it.copy(sensitivity = value) }
        })
    }

    private suspend fun toggleAcceleration() {
        val current = _uiState.value.accelerationEnabled
        saveAndUpdate({
            prefs.putBoolean("acceleration_enabled", !current)
            _uiState.update { it.copy(accelerationEnabled = !current) }
        }, "Acceleration ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updateAccelerationFactor(value: Float) {
        saveAndUpdate({
            prefs.putFloat("acceleration_factor", value)
            _uiState.update { it.copy(accelerationFactor = value) }
        })
    }

    private suspend fun toggleInvertX() {
        val current = _uiState.value.invertX
        saveAndUpdate({
            prefs.putBoolean("invert_x", !current)
            _uiState.update { it.copy(invertX = !current) }
        }, "X-axis ${if (!current) "inverted" else "normalized"}")
    }

    private suspend fun toggleInvertY() {
        val current = _uiState.value.invertY
        saveAndUpdate({
            prefs.putBoolean("invert_y", !current)
            _uiState.update { it.copy(invertY = !current) }
        }, "Y-axis ${if (!current) "inverted" else "normalized"}")
    }

    private suspend fun toggleSmoothing() {
        val current = _uiState.value.smoothingEnabled
        saveAndUpdate({
            prefs.putBoolean("smoothing_enabled", !current)
            _uiState.update { it.copy(smoothingEnabled = !current) }
        })
    }

    private suspend fun updateSmoothingFactor(value: Float) {
        saveAndUpdate({
            prefs.putFloat("smoothing_factor", value)
            _uiState.update { it.copy(smoothingFactor = value) }
        })
    }

    // ==================== GESTURE METHODS ====================

    private suspend fun updateClickThreshold(value: Float) {
        saveAndUpdate({
            prefs.putFloat("click_threshold", value)
            _uiState.update { it.copy(clickThreshold = value) }
        })
    }

    private suspend fun updateDoubleClickInterval(value: Long) {
        saveAndUpdate({
            prefs.putLong("double_click_interval", value)
            _uiState.update { it.copy(doubleClickInterval = value) }
        })
    }

    private suspend fun updateScrollThreshold(value: Float) {
        saveAndUpdate({
            prefs.putFloat("scroll_threshold", value)
            _uiState.update { it.copy(scrollThreshold = value) }
        })
    }

    private suspend fun updateRightClickTilt(value: Float) {
        saveAndUpdate({
            prefs.putFloat("right_click_tilt", value)
            _uiState.update { it.copy(rightClickTilt = value) }
        })
    }

    private suspend fun updateRightClickDuration(value: Long) {
        saveAndUpdate({
            prefs.putLong("right_click_duration", value)
            _uiState.update { it.copy(rightClickDuration = value) }
        })
    }

    // ==================== AI METHODS ====================

    private suspend fun toggleAiSmoothing() {
        val current = _uiState.value.aiSmoothing
        saveAndUpdate({
            prefs.putBoolean("ai_smoothing", !current)
            _uiState.update { it.copy(aiSmoothing = !current) }
        }, "AI Smoothing ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updateAiBlendFactor(value: Float) {
        saveAndUpdate({
            prefs.putFloat("ai_blend_factor", value)
            _uiState.update { it.copy(aiBlendFactor = value) }
        })
    }

    private suspend fun togglePredictive() {
        val current = _uiState.value.predictive
        saveAndUpdate({
            prefs.putBoolean("predictive_movement", !current)
            _uiState.update { it.copy(predictive = !current) }
        }, "Predictive ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updatePredictionStrength(value: Float) {
        saveAndUpdate({
            prefs.putFloat("prediction_strength", value)
            _uiState.update { it.copy(predictionStrength = value) }
        })
    }

    private suspend fun toggleKalman() {
        val current = _uiState.value.kalmanEnabled
        saveAndUpdate({
            prefs.putBoolean("kalman_enabled", !current)
            _uiState.update { it.copy(kalmanEnabled = !current) }
        })
    }

    // ==================== HAPTIC METHODS ====================

    private suspend fun toggleHaptic() {
        val current = _uiState.value.hapticEnabled
        saveAndUpdate({
            prefs.putBoolean("haptic_enabled", !current)
            _uiState.update { it.copy(hapticEnabled = !current) }
        }, "Haptic ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updateHapticStrength(strength: HapticStrength) {
        saveAndUpdate({
            prefs.putString("haptic_strength", strength.name)
            _uiState.update { it.copy(hapticStrength = strength) }
        }, "Haptic strength: ${strength.displayName}")
    }

    private suspend fun toggleSound() {
        val current = _uiState.value.soundEnabled
        saveAndUpdate({
            prefs.putBoolean("sound_enabled", !current)
            _uiState.update { it.copy(soundEnabled = !current) }
        }, "Sound ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun toggleVisualFeedback() {
        val current = _uiState.value.visualFeedback
        saveAndUpdate({
            prefs.putBoolean("visual_feedback", !current)
            _uiState.update { it.copy(visualFeedback = !current) }
        })
    }

    private suspend fun toggleNotificationOnGesture() {
        val current = _uiState.value.notificationOnGesture
        saveAndUpdate({
            prefs.putBoolean("notification_on_gesture", !current)
            _uiState.update { it.copy(notificationOnGesture = !current) }
        })
    }

    // ==================== DISPLAY METHODS ====================

    private suspend fun updateTheme(theme: String) {
        saveAndUpdate({
            prefs.putString("theme", theme)
            _uiState.update { it.copy(theme = theme) }
        }, "Theme: $theme")
    }

    private suspend fun toggleDynamicColors() {
        val current = _uiState.value.useDynamicColors
        saveAndUpdate({
            prefs.putBoolean("dynamic_colors", !current)
            _uiState.update { it.copy(useDynamicColors = !current) }
        })
    }

    private suspend fun updateFontSize(value: Float) {
        saveAndUpdate({
            prefs.putFloat("font_size", value)
            _uiState.update { it.copy(fontSize = value) }
        })
    }

    private suspend fun toggleDebugInfo() {
        val current = _uiState.value.showDebugInfo
        saveAndUpdate({
            prefs.putBoolean("show_debug_info", !current)
            _uiState.update { it.copy(showDebugInfo = !current) }
        })
    }

    private suspend fun toggleKeepScreenOn() {
        val current = _uiState.value.keepScreenOn
        saveAndUpdate({
            prefs.putBoolean("keep_screen_on", !current)
            _uiState.update { it.copy(keepScreenOn = !current) }
        })
    }

    private suspend fun toggleShowFps() {
        val current = _uiState.value.showFps
        saveAndUpdate({
            prefs.putBoolean("show_fps", !current)
            _uiState.update { it.copy(showFps = !current) }
        })
    }

    // ==================== CONNECTION METHODS ====================

    private suspend fun toggleAutoConnect() {
        val current = _uiState.value.autoConnect
        saveAndUpdate({
            prefs.putBoolean("auto_connect", !current)
            _uiState.update { it.copy(autoConnect = !current) }
        })
    }

    private suspend fun updateReconnectAttempts(value: Int) {
        saveAndUpdate({
            prefs.putInt("reconnect_attempts", value)
            _uiState.update { it.copy(reconnectAttempts = value) }
        })
    }

    private suspend fun updateConnectionTimeout(value: Int) {
        saveAndUpdate({
            prefs.putInt("connection_timeout", value)
            _uiState.update { it.copy(connectionTimeout = value) }
        })
    }

    private suspend fun toggleUseWebSocket() {
        val current = _uiState.value.useWebSocket
        saveAndUpdate({
            prefs.putBoolean("use_websocket", !current)
            _uiState.update { it.copy(useWebSocket = !current) }
        })
    }

    private suspend fun toggleUdpDiscovery() {
        val current = _uiState.value.useUdpDiscovery
        saveAndUpdate({
            prefs.putBoolean("use_udp_discovery", !current)
            _uiState.update { it.copy(useUdpDiscovery = !current) }
        })
    }

    // ==================== PRIVACY METHODS ====================

    private suspend fun toggleAnonymousStats() {
        val current = _uiState.value.anonymousStats
        saveAndUpdate({
            prefs.putBoolean("anonymous_stats", !current)
            _uiState.update { it.copy(anonymousStats = !current) }
        })
    }

    private suspend fun toggleCrashReporting() {
        val current = _uiState.value.crashReporting
        saveAndUpdate({
            prefs.putBoolean("crash_reporting", !current)
            _uiState.update { it.copy(crashReporting = !current) }
        })
    }

    private suspend fun toggleClearDataOnExit() {
        val current = _uiState.value.clearDataOnExit
        saveAndUpdate({
            prefs.putBoolean("clear_data_on_exit", !current)
            _uiState.update { it.copy(clearDataOnExit = !current) }
        })
    }

    // ==================== PRESENTATION METHODS ====================

    private suspend fun togglePresentationMode() {
        val current = _uiState.value.presentationModeEnabled
        saveAndUpdate({
            prefs.putBoolean("presentation_mode_enabled", !current)
            _uiState.update { it.copy(presentationModeEnabled = !current) }
        }, "Presentation mode ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updateLaserPointerSpeed(value: Float) {
        saveAndUpdate({
            prefs.putFloat("laser_pointer_speed", value)
            _uiState.update { it.copy(laserPointerSpeed = value) }
        })
    }

    private suspend fun toggleShowPresentationTimer() {
        val current = _uiState.value.showPresentationTimer
        saveAndUpdate({
            prefs.putBoolean("show_presentation_timer", !current)
            _uiState.update { it.copy(showPresentationTimer = !current) }
        })
    }

    private suspend fun toggleAutoHideLaser() {
        val current = _uiState.value.autoHideLaser
        saveAndUpdate({
            prefs.putBoolean("auto_hide_laser", !current)
            _uiState.update { it.copy(autoHideLaser = !current) }
        })
    }

    // ==================== ACTIONS ====================

    private suspend fun saveSettings() {
        saveAndUpdate({
            // All settings are already saved individually
            showToast("All settings saved successfully!")
        })
    }

    private suspend fun resetDefaults() {
        saveAndUpdate({
            val defaultState = SettingsUiState()
            _uiState.update { defaultState }
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
            // ... save all other defaults
            showToast("All settings reset to defaults!")
        })
    }

    private suspend fun exportSettings() {
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
                // ... add all other settings
            }

            file.writeText(content)
            showToast("Settings exported to: $fileName")
        })
    }

    private suspend fun importSettings() {
        showToast("Import settings - select a settings file")
    }

    private suspend fun clearCache() {
        saveAndUpdate({
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()
            showToast("Cache cleared!")
        })
    }

    private suspend fun clearAllData() {
        saveAndUpdate({
            prefs.clear()
            loadSettings()
            showToast("All data cleared!")
        })
    }

    private suspend fun openSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open system settings")
        }
    }

    private suspend fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open accessibility settings")
        }
    }

    private suspend fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open URL: ${e.message}")
        }
    }

    @Inject
    @ApplicationContext
    lateinit var context: Context
}

// ==================== MAIN COMPOSABLE ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

    // Show toast messages
    toastMessage?.let { message ->
        LaunchedEffect(message) {
            // Show toast via scaffold
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.handleEvent(SettingsEvent.SaveSettings) }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (selectedSection == null) {
            SettingsMainScreen(
                uiState = uiState,
                onSectionSelected = { selectedSection = it },
                viewModel = viewModel,
                modifier = modifier.padding(paddingValues)
            )
        } else {
            SectionDetailScreen(
                section = selectedSection!!,
                uiState = uiState,
                viewModel = viewModel,
                onBack = { selectedSection = null },
                modifier = modifier.padding(paddingValues)
            )
        }
    }
}

// ==================== MAIN SCREEN ====================

@Composable
fun SettingsMainScreen(
    uiState: SettingsUiState,
    onSectionSelected: (SettingsSection) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsHeader()
        }

        items(SettingsSection.entries) { section ->
            SettingsCard(
                title = section.title,
                description = section.description,
                icon = section.icon,
                onClick = { onSectionSelected(section) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==================== HEADER ====================

@Composable
fun SettingsHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "⚙️ Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Customize your Air Mouse experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

// ==================== SETTINGS CARD ====================

@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== SECTION DETAIL SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    section: SettingsSection,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            section.icon,
                            contentDescription = section.title,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            section.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                SettingsSection.CURSOR -> item { CursorSettings(uiState, viewModel) }
                SettingsSection.GESTURE -> item { GestureSettings(uiState, viewModel) }
                SettingsSection.AI -> item { AISettings(uiState, viewModel) }
                SettingsSection.HAPTIC -> item { HapticSettings(uiState, viewModel) }
                SettingsSection.DISPLAY -> item { DisplaySettings(uiState, viewModel) }
                SettingsSection.CONNECTION -> item { ConnectionSettings(uiState, viewModel) }
                SettingsSection.PRIVACY -> item { PrivacySettings(uiState, viewModel) }
                SettingsSection.PRESENTATION -> item { PresentationSettings(uiState, viewModel) }
                SettingsSection.ABOUT -> item { AboutSection(viewModel) }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ==================== UI COMPONENTS ====================

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    formatValue: (Float) -> String = { "%.2f".format(it) },
    description: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    formatValue(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// ==================== CURSOR SETTINGS ====================

@Composable
fun CursorSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSlider(
            title = "Sensitivity",
            value = uiState.sensitivity,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateSensitivity(it)) },
            valueRange = 0.1f..2.0f,
            steps = 19,
            formatValue = { "%.1fx".format(it) },
            description = "Higher values make cursor faster"
        )

        SettingsSlider(
            title = "Smoothing Factor",
            value = uiState.smoothingFactor,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateSmoothingFactor(it)) },
            valueRange = 0f..1f,
            steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "Smooths cursor movement"
        )

        SettingsSwitch(
            title = "Acceleration",
            description = "Cursor speed increases with faster movement",
            checked = uiState.accelerationEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAcceleration) }
        )

        SettingsSwitch(
            title = "Invert X Axis",
            description = "Swap left/right movement direction",
            checked = uiState.invertX,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleInvertX) }
        )

        SettingsSwitch(
            title = "Invert Y Axis",
            description = "Swap up/down movement direction",
            checked = uiState.invertY,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleInvertY) }
        )

        SettingsSwitch(
            title = "Smoothing",
            description = "Smooth cursor movement for better control",
            checked = uiState.smoothingEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleSmoothing) }
        )
    }
}

// ==================== GESTURE SETTINGS ====================

@Composable
fun GestureSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSlider(
            title = "Click Threshold",
            value = uiState.clickThreshold,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateClickThreshold(it)) },
            valueRange = 1f..20f,
            steps = 19,
            formatValue = { "%.1f".format(it) },
            description = "Sensitivity for click detection"
        )

        SettingsSlider(
            title = "Double Click Interval",
            value = uiState.doubleClickInterval.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateDoubleClickInterval(it.toLong())) },
            valueRange = 100f..800f,
            steps = 14,
            formatValue = { "${it.toInt()}ms" },
            description = "Max time between clicks for double click"
        )

        SettingsSlider(
            title = "Scroll Threshold",
            value = uiState.scrollThreshold,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateScrollThreshold(it)) },
            valueRange = 1f..15f,
            steps = 14,
            formatValue = { "%.1f".format(it) },
            description = "Sensitivity for scroll detection"
        )

        SettingsSlider(
            title = "Right Click Tilt",
            value = uiState.rightClickTilt,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateRightClickTilt(it)) },
            valueRange = 5f..45f,
            steps = 8,
            formatValue = { "${it.toInt()}°" },
            description = "Tilt angle to trigger right click"
        )

        SettingsSlider(
            title = "Right Click Duration",
            value = uiState.rightClickDuration.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateRightClickDuration(it.toLong())) },
            valueRange = 100f..1000f,
            steps = 9,
            formatValue = { "${it.toInt()}ms" },
            description = "How long to hold tilt for right click"
        )
    }
}

// ==================== AI SETTINGS ====================

@Composable
fun AISettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "AI Smoothing",
            description = "Use AI to smooth cursor movement",
            checked = uiState.aiSmoothing,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAiSmoothing) }
        )

        SettingsSlider(
            title = "AI Blend Factor",
            value = uiState.aiBlendFactor,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateAiBlendFactor(it)) },
            valueRange = 0f..1f,
            steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "Balance between raw and AI-smoothed movement"
        )

        SettingsSwitch(
            title = "Predictive Movement",
            description = "Predict future cursor position",
            checked = uiState.predictive,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.TogglePredictive) }
        )

        SettingsSlider(
            title = "Prediction Strength",
            value = uiState.predictionStrength,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdatePredictionStrength(it)) },
            valueRange = 0f..1f,
            steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "How much prediction to apply"
        )

        SettingsSwitch(
            title = "Kalman Filter",
            description = "Use Kalman filter for smoother tracking",
            checked = uiState.kalmanEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleKalman) }
        )
    }
}

// ==================== HAPTIC SETTINGS ====================

@Composable
fun HapticSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Haptic Feedback",
            description = "Vibration on actions",
            checked = uiState.hapticEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleHaptic) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Haptic Strength",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Intensity of vibration feedback",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HapticStrength.entries.forEach { strength ->
                        FilterChip(
                            selected = uiState.hapticStrength == strength,
                            onClick = { viewModel.handleEvent(SettingsEvent.UpdateHapticStrength(strength)) },
                            label = { Text(strength.displayName) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }

        SettingsSwitch(
            title = "Sound Feedback",
            description = "Play sounds on actions",
            checked = uiState.soundEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleSound) }
        )

        SettingsSwitch(
            title = "Visual Feedback",
            description = "Show visual indicators on actions",
            checked = uiState.visualFeedback,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleVisualFeedback) }
        )

        SettingsSwitch(
            title = "Notification on Gesture",
            description = "Show notification when gesture is detected",
            checked = uiState.notificationOnGesture,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleNotificationOnGesture) }
        )
    }
}

// ==================== DISPLAY SETTINGS ====================

@Composable
fun DisplaySettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Choose app color scheme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("system", "light", "dark", "pure_black").forEach { theme ->
                        FilterChip(
                            selected = uiState.theme == theme,
                            onClick = { viewModel.handleEvent(SettingsEvent.UpdateTheme(theme)) },
                            label = {
                                Text(
                                    when (theme) {
                                        "system" -> "System"
                                        "light" -> "Light"
                                        "dark" -> "Dark"
                                        "pure_black" -> "Pure Black"
                                        else -> theme
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }

        SettingsSwitch(
            title = "Dynamic Colors",
            description = "Use Material You color scheme",
            checked = uiState.useDynamicColors,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleDynamicColors) }
        )

        SettingsSlider(
            title = "Font Size",
            value = uiState.fontSize,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateFontSize(it)) },
            valueRange = 12f..24f,
            steps = 6,
            formatValue = { "${it.toInt()}sp" },
            description = "Base font size for the app"
        )

        SettingsSwitch(
            title = "Show Debug Info",
            description = "Display debug information",
            checked = uiState.showDebugInfo,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleDebugInfo) }
        )

        SettingsSwitch(
            title = "Keep Screen On",
            description = "Prevent screen from turning off",
            checked = uiState.keepScreenOn,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleKeepScreenOn) }
        )

        SettingsSwitch(
            title = "Show FPS",
            description = "Display frames per second counter",
            checked = uiState.showFps,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleShowFps) }
        )
    }
}

// ==================== CONNECTION SETTINGS ====================

@Composable
fun ConnectionSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Auto Connect",
            description = "Auto-connect on app start",
            checked = uiState.autoConnect,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAutoConnect) }
        )

        SettingsSwitch(
            title = "Use WebSocket",
            description = "Use WebSocket protocol (fallback to TCP)",
            checked = uiState.useWebSocket,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleUseWebSocket) }
        )

        SettingsSwitch(
            title = "UDP Discovery",
            description = "Auto-discover servers on network",
            checked = uiState.useUdpDiscovery,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleUdpDiscovery) }
        )

        SettingsSlider(
            title = "Reconnect Attempts",
            value = uiState.reconnectAttempts.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateReconnectAttempts(it.toInt())) },
            valueRange = 1f..20f,
            steps = 19,
            formatValue = { "${it.toInt()}" },
            description = "Number of reconnection attempts"
        )

        SettingsSlider(
            title = "Connection Timeout",
            value = uiState.connectionTimeout.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateConnectionTimeout(it.toInt())) },
            valueRange = 1000f..15000f,
            steps = 14,
            formatValue = { "${it.toInt()}ms" },
            description = "Timeout for connection attempts"
        )
    }
}

// ==================== PRIVACY SETTINGS ====================

@Composable
fun PrivacySettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Anonymous Statistics",
            description = "Help improve Air Mouse by sending anonymous usage data",
            checked = uiState.anonymousStats,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAnonymousStats) }
        )

        SettingsSwitch(
            title = "Crash Reporting",
            description = "Automatically report crashes to help fix issues",
            checked = uiState.crashReporting,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleCrashReporting) }
        )

        SettingsSwitch(
            title = "Clear Data on Exit",
            description = "Clear all app data when you exit",
            checked = uiState.clearDataOnExit,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleClearDataOnExit) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.handleEvent(SettingsEvent.ClearCache) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Cache")
                    }
                    OutlinedButton(
                        onClick = { viewModel.handleEvent(SettingsEvent.ExportSettings) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Data")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.handleEvent(SettingsEvent.ClearAllData) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All Data", color = Color.White)
                }
            }
        }
    }
}

// ==================== PRESENTATION SETTINGS ====================

@Composable
fun PresentationSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Presentation Mode",
            description = "Enable presentation controls",
            checked = uiState.presentationModeEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.TogglePresentationMode) }
        )

        SettingsSlider(
            title = "Laser Pointer Speed",
            value = uiState.laserPointerSpeed,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateLaserPointerSpeed(it)) },
            valueRange = 0.1f..2.0f,
            steps = 19,
            formatValue = { "%.1fx".format(it) },
            description = "Speed of laser pointer movement"
        )

        SettingsSwitch(
            title = "Show Presentation Timer",
            description = "Display timer during presentations",
            checked = uiState.showPresentationTimer,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleShowPresentationTimer) }
        )

        SettingsSwitch(
            title = "Auto-Hide Laser",
            description = "Hide laser pointer when not in use",
            checked = uiState.autoHideLaser,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAutoHideLaser) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* Start presentation */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }
                    Button(
                        onClick = { /* Stop presentation */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }
        }
    }
}

// ==================== ABOUT SECTION ====================

@Composable
fun AboutSection(viewModel: SettingsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎯", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Air Mouse Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Version 3.0.0",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Turn your phone into a wireless mouse",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🏛️ University of Tehran", style = MaterialTheme.typography.bodyMedium)
                Text("📱 Built with Kotlin & Compose", style = MaterialTheme.typography.bodySmall)
                Text(
                    "🔗 github.com/airmouse",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { viewModel.handleEvent(SettingsEvent.OpenGitHub) }
                )
                Text("© 2025 Air Mouse Team", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.handleEvent(SettingsEvent.OpenLicense) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Source Licenses")
            }
        }
    }
}
