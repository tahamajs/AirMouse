// app/src/main/java/com/airmouse/presentation/ui/accessibility/AccessibilityUiState.kt
package com.airmouse.presentation.ui.accessibility

data class AccessibilityUiState(
    val announceMovement: Boolean = false,
    val announceClicks: Boolean = false,
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reduceMotion: Boolean = false,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
    val customFontSize: Float = 16f,
    val spokenFeedbackEnabled: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val screenReaderCompatible: Boolean = false
)

enum class ColorBlindMode(val displayName: String) {
    NONE("None"),
    PROTANOPIA("Red-Blind (Protanopia)"),
    DEUTERANOPIA("Green-Blind (Deuteranopia)"),
    TRITANOPIA("Blue-Blind (Tritanopia)")
}