// app/src/main/java/com/airmouse/domain/model/Gesture.kt
package com.airmouse.domain.model

/**
 * Types of gestures that can be detected.
 */
enum class GestureType {
    NONE,
    CLICK,
    DOUBLE_CLICK,
    RIGHT_CLICK,
    SCROLL_UP,
    SCROLL_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    CIRCLE_CW,
    CIRCLE_CCW,
    CUSTOM
}

/**
 * Represents a detected gesture with confidence score.
 */
data class Gesture(
    val type: GestureType,
    val confidence: Float = 1.0f,
    val customName: String? = null
)

/**
 * Template for a custom user‑defined gesture.
 */
data class CustomGestureTemplate(
    val id: String,
    val name: String,
    val samples: List<List<Float>>, // sequence of sensor vectors
    val threshold: Float = 0.7f
)