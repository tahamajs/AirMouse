// app/src/main/java/com/airmouse/domain/model/SensorModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Raw sensor reading from accelerometer, gyroscope, or magnetometer.
 */
@Parcelize
data class SensorReading(
    val type: SensorType,
    val values: FloatArray, // x, y, z
    val accuracy: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorReading
        if (!values.contentEquals(other.values)) return false
        if (type != other.type) return false
        if (accuracy != other.accuracy) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        result = 31 * result + timestamp.hashCode()
        return result
    }

    override fun toString(): String {
        return "SensorReading(type=$type, values=[${values.joinToString()}], accuracy=$accuracy, timestamp=$timestamp)"
    }
}

/**
 * Types of sensors available on Android devices.
 */
enum class SensorType(val displayName: String) {
    ACCELEROMETER("Accelerometer"),
    GYROSCOPE("Gyroscope"),
    MAGNETOMETER("Magnetometer"),
    ROTATION_VECTOR("Rotation Vector"),
    GAME_ROTATION_VECTOR("Game Rotation Vector"),
    GRAVITY("Gravity"),
    LINEAR_ACCELERATION("Linear Acceleration"),
    PRESSURE("Pressure"),
    TEMPERATURE("Temperature"),
    PROXIMITY("Proximity"),
    LIGHT("Light"),
    STEP_COUNTER("Step Counter"),
    STEP_DETECTOR("Step Detector"),
    HEART_RATE("Heart Rate")
}

/**
 * Orientation (roll, pitch, yaw) derived from sensor fusion.
 */
@Parcelize
data class Orientation(
    val roll: Float,   // rotation around X axis (degrees) - used for vertical cursor movement
    val pitch: Float,  // rotation around Y axis (degrees) - not directly used
    val yaw: Float,    // rotation around Z axis (degrees) - used for horizontal cursor movement
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Sensor fusion data combining all sensors.
 */
@Parcelize
data class SensorFusionData(
    val orientation: Orientation,
    val linearAcceleration: Triple<Float, Float, Float>,
    val gravity: Triple<Float, Float, Float>,
    val rotationMatrix: FloatArray,
    val quaternion: FloatArray,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorFusionData
        if (!rotationMatrix.contentEquals(other.rotationMatrix)) return false
        if (!quaternion.contentEquals(other.quaternion)) return false
        if (orientation != other.orientation) return false
        if (linearAcceleration != other.linearAcceleration) return false
        if (gravity != other.gravity) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = orientation.hashCode()
        result = 31 * result + linearAcceleration.hashCode()
        result = 31 * result + gravity.hashCode()
        result = 31 * result + rotationMatrix.contentHashCode()
        result = 31 * result + quaternion.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Device motion state analysis.
 */
@Parcelize
data class MotionState(
    val isMoving: Boolean,
    val isStable: Boolean,
    val activity: ActivityType,
    val confidence: Float,
    val lastMovementTime: Long,
    val stabilityDuration: Long
) : Parcelable

enum class ActivityType(val displayName: String) {
    IDLE("Idle"),
    WALKING("Walking"),
    RUNNING("Running"),
    GESTURE("Gesture"),
    ROTATING("Rotating"),
    SHAKING("Shaking")
}