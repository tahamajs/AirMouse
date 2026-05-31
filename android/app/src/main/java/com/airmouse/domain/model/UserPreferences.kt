// app/src/main/java/com/airmouse/domain/model/UserPreferences.kt
package com.airmouse.domain.model

/**
 * User‑configurable settings.
 */
data class UserPreferences(
    val cursorSensitivity: Float = 0.5f,
    val clickThreshold: Float = 10f,
    val doubleClickInterval: Long = 300,
    val scrollThreshold: Float = 5f,
    val rightClickTilt: Float = 15f,
    val hapticFeedbackEnabled: Boolean = true,
    val theme: String = "dark",
    val useAiSmoothing: Boolean = false,
    val usePredictiveMovement: Boolean = true
)