package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * User‑configurable settings for the Air Mouse application.
 * All preferences are persisted and restored across app sessions.
 */
@Parcelize
data class UserPreferences(
    // Cursor Control Settings
    val cursorSensitivity: Float = 0.5f,
    val cursorSpeed: Float = 1.0f,
    val accelerationEnabled: Boolean = true,
    val accelerationFactor: Float = 1.5f,
    val smoothingEnabled: Boolean = true,
    val smoothingFactor: Float = 0.5f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,

    // Gesture Detection Settings
    val clickThreshold: Float = 8f,
    val doubleClickInterval: Long = 400L,
    val scrollThreshold: Float = 6f,
    val scrollDebounce: Float = 2f,
    val rightClickTilt: Float = 45f,
    val rightClickDuration: Long = 500L,
    val swipeThreshold: Float = 15f,
    val gestureCooldown: Long = 500L,

    // AI & Predictive Settings
    val useAiSmoothing: Boolean = false,
    val aiBlendFactor: Float = 0.7f,
    val usePredictiveMovement: Boolean = true,
    val predictionStrength: Float = 0.5f,
    val kalmanFilterEnabled: Boolean = true,

    // Feedback Settings
    val hapticFeedbackEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val soundEffectsEnabled: Boolean = true,
    val visualFeedbackEnabled: Boolean = true,
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
    val keepAliveInterval: Long = 30000,

    // Privacy Settings
    val anonymousStats: Boolean = true,
    val crashReporting: Boolean = true,
    val clearDataOnExit: Boolean = false,

    // Advanced Settings
    val batterySaverEnabled: Boolean = true,
    val lowLatencyMode: Boolean = false,
    val experimentalFeatures: Boolean = false,
    val developerMode: Boolean = false,

    // Metadata
    val lastUpdated: Long = System.currentTimeMillis(),
    val version: Int = 1
) : Parcelable

enum class HapticStrength(val value: Int, val displayName: String) {
    LIGHT(20, "Light"),
    MEDIUM(50, "Medium"),
    STRONG(80, "Strong")
}