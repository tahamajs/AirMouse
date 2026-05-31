// app/src/main/java/com/airmouse/data/model/SensorData.kt
package com.airmouse.data.model

/**
 * DTO for raw sensor readings from the phone.
 */
data class SensorData(
    val timestamp: Long = System.currentTimeMillis(),
    val accelerometer: Vector3? = null,
    val gyroscope: Vector3? = null,
    val magnetometer: Vector3? = null,
    val rotationVector: FloatArray? = null,
    val gravity: Vector3? = null,
    val linearAcceleration: Vector3? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorData
        if (timestamp != other.timestamp) return false
        if (accelerometer != other.accelerometer) return false
        if (gyroscope != other.gyroscope) return false
        if (magnetometer != other.magnetometer) return false
        if (rotationVector != null) {
            if (other.rotationVector == null) return false
            if (!rotationVector.contentEquals(other.rotationVector)) return false
        } else if (other.rotationVector != null) return false
        if (gravity != other.gravity) return false
        if (linearAcceleration != other.linearAcceleration) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + (accelerometer?.hashCode() ?: 0)
        result = 31 * result + (gyroscope?.hashCode() ?: 0)
        result = 31 * result + (magnetometer?.hashCode() ?: 0)
        result = 31 * result + (rotationVector?.contentHashCode() ?: 0)
        result = 31 * result + (gravity?.hashCode() ?: 0)
        result = 31 * result + (linearAcceleration?.hashCode() ?: 0)
        return result
    }
}

/**
 * 3D vector data class.
 */
data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun toFloatArray(): FloatArray = floatArrayOf(x, y, z)

    companion object {
        fun fromFloatArray(arr: FloatArray): Vector3 = Vector3(arr[0], arr[1], arr[2])
        fun zero(): Vector3 = Vector3(0f, 0f, 0f)
    }
}

/**
 * DTO for orientation (roll, pitch, yaw) derived from sensor fusion.
 */
data class OrientationData(
    val roll: Float,   // degrees
    val pitch: Float,  // degrees
    val yaw: Float,    // degrees
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * DTO for processed movement (delta values) sent to the server.
 */
data class MovementData(
    val deltaX: Float,
    val deltaY: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * DTO for gesture detection result.
 */
data class GestureResult(
    val gestureType: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * DTO for calibration parameters.
 */
data class CalibrationData(
    val gyroBias: Vector3,
    val accelOffset: Vector3,
    val accelScale: Vector3,
    val magOffset: Vector3,
    val magScale: Vector3
)

/**
 * DTO for Bluetooth proximity data.
 */
data class ProximityData(
    val rssi: Int,
    val distance: Float,
    val isNear: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)