package com.airmouse.presentation.ui.settings

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

    // Status
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val isSaving: Boolean = false
)

enum class HapticStrength(val displayName: String, val duration: Long) {
    LIGHT("Light", 20),
    MEDIUM("Medium", 50),
    STRONG("Strong", 80)
}