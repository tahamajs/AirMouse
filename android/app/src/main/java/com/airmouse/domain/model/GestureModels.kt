// app/src/main/java/com/airmouse/domain/model/GestureModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Gesture types
 */
enum class GestureType {
    NONE,
    CLICK,
    DOUBLE_CLICK,
    RIGHT_CLICK,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    CIRCLE_CW,
    CIRCLE_CCW,
    THUMBS_UP,
    THUMBS_DOWN,
    ZOOM_IN,
    ZOOM_OUT,
    SHAKE,
    PEACE,
    FIST,
    CUSTOM
}

/**
 * Gesture event
 */
data class GestureEvent(
    val type: GestureType,
    val name: String = "",
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val velocity: Float = 0f,
    val duration: Long = 0,
    val isCustom: Boolean = false
)

/**
 * Custom gesture template
 */
@Parcelize
data class CustomGestureTemplate(
    val id: String = "",
    val name: String = "",
    val type: GestureType = GestureType.CUSTOM,
    val action: String = "",
    val confidence: Float = 0.7f,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
) : Parcelable

/**
 * Gesture training statistics
 */
data class GestureTrainingStats(
    val totalGestures: Int = 0,
    val gesturesByType: Map<GestureType, Int> = emptyMap(),
    val mostUsedGesture: String = "",
    val lastGestureTime: Long = 0,
    val customGestureUsage: Map<String, Int> = emptyMap(),
    val averageConfidence: Float = 0f
)

/**
 * Gesture detection result
 */
data class GestureDetectionResult(
    val gesture: GestureType,
    val confidence: Float,
    val rawData: List<FloatArray>? = null,
    val matchedTemplate: String? = null
)