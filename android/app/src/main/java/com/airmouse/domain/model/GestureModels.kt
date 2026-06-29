package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

data class GestureEvent(
    val type: GestureType,
    val name: String = "",
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val velocity: Float = 0f,
    val duration: Long = 0,
    val isCustom: Boolean = false
) {
    fun isHighConfidence(threshold: Float = 0.7f): Boolean = confidence >= threshold
    fun getAction(): String = GestureActionMap.getAction(type)
}

object GestureActionMap {
    private val actions = mapOf(
        GestureType.THUMBS_UP to "play_pause",
        GestureType.THUMBS_DOWN to "stop",
        GestureType.SWIPE_LEFT to "prev_track",
        GestureType.SWIPE_RIGHT to "next_track",
        GestureType.SWIPE_UP to "volume_up",
        GestureType.SWIPE_DOWN to "volume_down",
        GestureType.CIRCLE_CW to "volume_up",
        GestureType.CIRCLE_CCW to "volume_down",
        GestureType.PEACE to "lock_screen",
        GestureType.FIST to "mute",
        GestureType.DOUBLE_CLICK to "play_pause",
        GestureType.ZOOM_IN to "zoom_in",
        GestureType.ZOOM_OUT to "zoom_out",
        GestureType.SHAKE to "undo"
    )

    fun getAction(type: GestureType): String = actions[type] ?: "none"
}

@Parcelize
data class CustomGestureTemplate(
    val id: String = "",
    val name: String = "",
    val type: GestureType = GestureType.CUSTOM,
    val action: String = "",
    val confidence: Float = 0.7f,
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0,
    val description: String = "",
    val lastUsed: Long = updatedAt,
    val isSystemGesture: Boolean = false,
    val version: Int = 1
) : Parcelable {
    fun isValid(): Boolean = name.isNotEmpty() && action.isNotEmpty()
    fun toGestureEvent(): GestureEvent = GestureEvent(
        type = type,
        name = name,
        confidence = confidence,
        isCustom = true
    )
} 

val CustomGestureTemplate.duration: Float
    get() = ((updatedAt - createdAt).coerceAtLeast(0L) / 1000f)

data class GestureTrainingStats(
    val totalGestures: Int = 0,
    val gesturesByType: Map<GestureType, Int> = emptyMap(),
    val mostUsedGesture: String = "NONE",
    val lastGestureTime: Long = 0,
    val customGestureUsage: Map<String, Int> = emptyMap(),
    val averageConfidence: Float = 0f
) {
    fun getTotalCustomGestures(): Int = gesturesByType[GestureType.CUSTOM] ?: 0
    fun resolveMostUsedGesture(): String {
        return if (mostUsedGesture != "NONE") {
            mostUsedGesture
        } else {
            gesturesByType.maxByOrNull { it.value }?.key?.name ?: "NONE"
        }
    }
    fun resolveCustomGestureUsage(): Map<String, Int> {
        return customGestureUsage
    }
}

data class GestureDetectionResult(
    val gesture: GestureType,
    val confidence: Float,
    val rawData: List<FloatArray>? = null,
    val matchedTemplate: String? = null
) {
    fun isGesture(): Boolean = gesture != GestureType.NONE
    fun isHighConfidence(threshold: Float = 0.7f): Boolean = confidence >= threshold
}
