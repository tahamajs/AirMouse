package com.airmouse.presentation.ui.accessibility

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// ============================================================
// UI STATE
// ============================================================

data class AccessibilityUiState(
    // Display
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reduceMotion: Boolean = false,
    val darkMode: Boolean = false,
    val customFontSize: Float = 16f,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,

    // Feedback
    val hapticFeedback: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val soundFeedback: Boolean = false,
    val voiceFeedback: Boolean = false,

    // Gesture
    val simplifiedGestures: Boolean = false,
    val screenReader: Boolean = false,
    val announceMovement: Boolean = false,
    val announceClicks: Boolean = false,
    val gestureSensitivity: Float = 1.0f,

    // Voice
    val voiceWakeWord: Boolean = true,
    val wakeWord: String = "Hey Air Mouse",
    val voiceConfirmation: Boolean = true,
    val voiceContinuousListening: Boolean = false,

    // Advanced
    val switchAccess: Boolean = false,
    val dwellClick: Boolean = false,
    val dwellTime: Int = 1000,
    val audioCues: Boolean = true,
    val flashOnClick: Boolean = false,

    // Mouse Control
    val isMouseEnabled: Boolean = true,
    val mousePointerLarge: Boolean = false,
    val mouseTrails: Boolean = false,
    val snapToDefault: Boolean = false,

    // Control Mode
    val controlMode: String = "motion",

    // UI state
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

// ============================================================
// ENUMS
// ============================================================

enum class AccessibilityCategory(
    val title: String,
    val icon: ImageVector
) {
    DISPLAY("Display", Icons.Default.DisplaySettings),
    FEEDBACK("Feedback", Icons.Default.VolumeUp),
    GESTURE("Gesture", Icons.Default.Gesture),
    VOICE("Voice", Icons.Default.Mic),
    ADVANCED("Advanced", Icons.Default.Settings),
    MOUSE("Mouse Control", Icons.Default.Computer)
}

enum class ColorBlindMode(val displayName: String) {
    NONE("None"),
    PROTANOPIA("Protanopia (Red-Blind)"),
    DEUTERANOPIA("Deuteranopia (Green-Blind)"),
    TRITANOPIA("Tritanopia (Blue-Blind)")
}

enum class HapticIntensity {
    LIGHT,
    MEDIUM,
    STRONG
}

// ============================================================
// CONTROL MODE
// ============================================================

enum class ControlMode(val displayName: String) {
    GYRO("Motion (Gyro)"),
    ACCEL("Acceleration"),
    HYBRID("Hybrid"),
    MOUSE("Mouse Control")
}

// ============================================================
// EXTENSION FUNCTIONS
// ============================================================

fun String.toControlMode(): ControlMode {
    return when (lowercase()) {
        "gyro", "motion" -> ControlMode.GYRO
        "accel", "accelerometer" -> ControlMode.ACCEL
        "hybrid" -> ControlMode.HYBRID
        "mouse" -> ControlMode.MOUSE
        else -> ControlMode.GYRO
    }
}

fun ControlMode.toDisplayString(): String = displayName
fun ControlMode.isMouseMode(): Boolean = this == ControlMode.MOUSE
fun ControlMode.isMotionMode(): Boolean = this == ControlMode.GYRO || this == ControlMode.ACCEL || this == ControlMode.HYBRID