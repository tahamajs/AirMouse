// app/src/main/java/com/airmouse/domain/model/SensorData.kt
package com.airmouse.domain.model

import java.util.Date

/**
 * Raw sensor reading from accelerometer, gyroscope, or magnetometer.
 */
data class SensorReading(
    val type: SensorType,
    val values: FloatArray, // x, y, z
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorReading
        if (!values.contentEquals(other.values)) return false
        if (type != other.type) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Types of sensors.
 */
enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    ROTATION_VECTOR,
    GRAVITY,
    LINEAR_ACCELERATION
}

/**
 * Orientation (roll, pitch, yaw) derived from sensor fusion.
 */
data class Orientation(
    val roll: Float,   // rotation around X axis (degrees)
    val pitch: Float,  // rotation around Y axis (degrees)
    val yaw: Float,    // rotation around Z axis (degrees)
    val timestamp: Long = System.currentTimeMillis()
)