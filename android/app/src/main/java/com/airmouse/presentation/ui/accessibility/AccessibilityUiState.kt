package com.airmouse.presentation.ui.accessibility

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class AccessibilityUiState(
    
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reduceMotion: Boolean = false,
    val darkMode: Boolean = false,
    val customFontSize: Float = 16f,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,

    
    val hapticFeedback: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val soundFeedback: Boolean = false,
    val voiceFeedback: Boolean = false,

    
    val simplifiedGestures: Boolean = false,
    val screenReader: Boolean = false,
    val announceMovement: Boolean = false,
    val announceClicks: Boolean = false,
    val gestureSensitivity: Float = 1.0f,

    
    val voiceWakeWord: Boolean = true,
    val wakeWord: String = "Hey Air Mouse",
    val voiceConfirmation: Boolean = true,
    val voiceContinuousListening: Boolean = false,

    
    val switchAccess: Boolean = false,
    val dwellClick: Boolean = false,
    val dwellTime: Int = 1000,
    val audioCues: Boolean = true,
    val flashOnClick: Boolean = false,

    
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

enum class AccessibilityCategory(
    val title: String,
    val icon: ImageVector
) {
    DISPLAY("Display", Icons.Default.DisplaySettings),
    FEEDBACK("Feedback", Icons.Default.VolumeUp),
    GESTURE("Gesture", Icons.Default.Gesture),
    VOICE("Voice", Icons.Default.Mic),
    ADVANCED("Advanced", Icons.Default.Settings)
}

enum class ColorBlindMode(val displayName: String) {
    NONE("None"),
    PROTANOPIA("Protanopia (Red-Blind)"),
    DEUTERANOPIA("Deuteranopia (Green-Blind)"),
    TRITANOPIA("Tritanopia (Blue-Blind)")
}

enum class HapticIntensity {
    LIGHT, MEDIUM, STRONG
}