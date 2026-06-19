// app/src/main/java/com/airmouse/domain/model/SensorModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Sensor data
 */
@Parcelize
data class SensorData(
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Orientation data
 */
@Parcelize
data class OrientationData(
    val roll: Float,
    val pitch: Float,
    val yaw: Float,
    val rollDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val yawDeg: Float = 0f
) : Parcelable {
    constructor(roll: Float, pitch: Float, yaw: Float) : this(
        roll = roll,
        pitch = pitch,
        yaw = yaw,
        rollDeg = Math.toDegrees(roll.toDouble()).toFloat(),
        pitchDeg = Math.toDegrees(pitch.toDouble()).toFloat(),
        yawDeg = Math.toDegrees(yaw.toDouble()).toFloat()
    )
}

/**
 * Calibration status enum (moved here to avoid conflict)
 */
enum class SensorCalibrationStatus {
    NOT_CALIBRATED,
    CALIBRATING,
    CALIBRATED,
    NEEDS_RECALIBRATION
}

/**
 * Sensor info
 */
data class SensorInfo(
    val name: String,
    val vendor: String,
    val version: Int,
    val maxRange: Float,
    val resolution: Float,
    val power: Float,
    val isAvailable: Boolean
)

/**
 * Sensor event
 */
data class SensorEventData(
    val type: String,
    val values: FloatArray,
    val accuracy: Int,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorEventData

        if (type != other.type) return false
        if (!values.contentEquals(other.values)) return false
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
}