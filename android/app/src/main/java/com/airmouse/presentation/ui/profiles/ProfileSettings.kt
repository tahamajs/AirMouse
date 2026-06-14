package com.airmouse.presentation.ui.profiles

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProfileSettings(
    // Cursor Settings
    val sensitivity: Float = 1.0f,
    val smoothingEnabled: Boolean = true,
    val smoothingFactor: Float = 0.5f,
    val accelerationEnabled: Boolean = true,
    val accelerationFactor: Float = 1.5f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    
    // Gesture Settings
    val clickThreshold: Float = 8f,
    val doubleClickInterval: Long = 300L,
    val scrollThreshold: Float = 6f,
    val rightClickTilt: Float = 45f,
    val rightClickDuration: Long = 500L,
    val gestureDebounce: Long = 100L,
    
    // AI & Predictive Settings
    val aiSmoothing: Boolean = false,
    val aiBlendFactor: Float = 0.7f,
    val predictiveMovement: Boolean = true,
    val predictionStrength: Float = 0.5f,
    val kalmanEnabled: Boolean = true,
    
    // Haptic & Feedback
    val hapticFeedback: Boolean = true,
    val hapticStrength: HapticStrengthType = HapticStrengthType.MEDIUM,
    val soundEnabled: Boolean = false,
    val visualFeedback: Boolean = true,
    
    // Display Settings
    val theme: String = "system",
    val fontSize: Float = 16f,
    val showDebugInfo: Boolean = false,
    val keepScreenOn: Boolean = false,
    
    // Connection Settings
    val autoConnect: Boolean = true,
    val reconnectAttempts: Int = 5,
    val connectionTimeout: Int = 5000,
    val useWebSocket: Boolean = true,
    
    // Advanced Settings
    val jitterCompensation: Boolean = true,
    val deadband: Float = 0.5f,
    val maxSpeed: Float = 100f,
    val minSpeed: Float = 0.5f
) : Parcelable

enum class HapticStrengthType(val displayName: String, val duration: Long) {
    LIGHT("Light", 20),
    MEDIUM("Medium", 50),
    STRONG("Strong", 80)
}