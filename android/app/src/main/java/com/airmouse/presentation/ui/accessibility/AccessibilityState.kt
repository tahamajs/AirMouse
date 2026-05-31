package com.airmouse.presentation.ui.accessibility

data class AccessibilityUiState(
    val announceMovement: Boolean = false,
    val announceClicks: Boolean = false,
    val highContrast: Boolean = false,
    val largeText: Boolean = false
)