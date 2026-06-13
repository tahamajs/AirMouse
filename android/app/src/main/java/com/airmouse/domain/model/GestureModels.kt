package com.airmouse.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Types of gestures that can be detected.
 */
enum class GestureType(val displayName: String) {
    NONE("None"),
    CLICK("Click"),
    DOUBLE_CLICK("Double Click"),
    RIGHT_CLICK("Right Click"),
    SCROLL_UP("Scroll Up"),
    SCROLL_DOWN("Scroll Down"),
    SWIPE_LEFT("Swipe Left"),
    SWIPE_RIGHT("Swipe Right"),
    SWIPE_UP("Swipe Up"),
    SWIPE_DOWN("Swipe Down"),
    CIRCLE_CW("Circle Clockwise"),
    CIRCLE_CCW("Circle Counter-Clockwise"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    CUSTOM("Custom")
}

/**
 * Represents a detected gesture with confidence score.
 */
@Parcelize
data class Gesture(
    val type: GestureType,
    val confidence: Float = 1.0f,
    val customName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Template for a custom user-defined gesture.
 */
@Entity(tableName = "custom_gestures")
@Parcelize
data class CustomGestureTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val threshold: Float = 0.7f,
    val samples: List<FloatArray> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isTrained: Boolean = false,
    val confidence: Float = 0f,
    val detectionCount: Int = 0,
    val lastDetected: Long = 0
) : Parcelable

/**
 * Individual sensor sample for gesture recording.
 */
@Parcelize
data class GestureSample(
    val timestamp: Long,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f
) : Parcelable

/**
 * Statistics for gesture usage.
 */
@Parcelize
data class GestureStatistics(
    val totalGestures: Int,
    val gesturesByType: Map<GestureType, Int>,
    val mostUsedGesture: GestureType?,
    val averageConfidence: Float,
    val lastGestureTime: Long,
    val customGestureUsage: Map<String, Int>
) : Parcelable

data class GestureTrainingResult(
    val success: Boolean,
    val message: String,
    val confidence: Float = 0f
)
