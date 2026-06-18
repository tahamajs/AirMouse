package com.airmouse.presentation.ui.touchpad

data class TouchpadUiState(
    val isActive: Boolean = false,
    val sensitivity: Float = 1.0f, val scrollSpeed: Float = 1.0f,
    val naturalScrolling: Boolean = true, val tapToClick: Boolean = true,
    val twoFingerScroll: Boolean = true, val threeFingerSwipe: Boolean = true,
    val edgeScrolling: Boolean = false, val hapticFeedback: Boolean = true,
    val cursorSpeed: Float = 1.0f, val accelerationEnabled: Boolean = true,
    val invertVertical: Boolean = false, val invertHorizontal: Boolean = false,
    val pointerSpeed: Int = 50, val doubleTapDelay: Int = 300,
    val scrollInertia: Boolean = true, val pinchToZoom: Boolean = true,
    val rotateToRotate: Boolean = false, val showTouchPoints: Boolean = false,
    val currentX: Float = 0f, val currentY: Float = 0f,
    val isDragging: Boolean = false, val isScrolling: Boolean = false,
    val lastGesture: String = "", val touchPoints: List<TouchPoint> = emptyList()
)

data class TouchPoint(val id: Int, val x: Float, val y: Float, val pressure: Float)

enum class TouchpadMode(val displayName: String) {
    STANDARD("Standard"), PRECISION("Precision"), GAMING("Gaming"), PRESENTATION("Presentation")
}