// app/src/main/java/com/airmouse/presentation/ui/settings/SettingsState.kt
package com.airmouse.presentation.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

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
    THEMES("Themes", "Theme presets and accents", Icons.Default.Palette),
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

// ==================== EFFECTS ====================

sealed class SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect()
    data class NavigateTo(val route: String) : SettingsEffect()
    object NavigateBack : SettingsEffect()
    data class OpenUrl(val url: String) : SettingsEffect()
}