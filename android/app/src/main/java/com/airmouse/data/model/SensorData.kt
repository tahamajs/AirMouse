// app/src/main/java/com/airmouse/data/model/SensorData.kt
package com.airmouse.data.model

import android.hardware.SensorManager
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

/**
 * Complete DTO for raw sensor readings from the phone.
 */
@Parcelize
data class SensorData(
    val timestamp: Long = System.currentTimeMillis(),
    val accelerometer: Vector3? = null,
    val gyroscope: Vector3? = null,
    val magnetometer: Vector3? = null,
    val rotationVector: FloatArray? = null,
    val gameRotationVector: FloatArray? = null,
    val gravity: Vector3? = null,
    val linearAcceleration: Vector3? = null,
    val pressure: Float? = null,
    val temperature: Float? = null,
    val proximity: Float? = null,
    val light: Float? = null,
    val accuracy: Int = 0,
    val sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME
) : Parcelable {

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
        if (gameRotationVector != null) {
            if (other.gameRotationVector == null) return false
            if (!gameRotationVector.contentEquals(other.gameRotationVector)) return false
        } else if (other.gameRotationVector != null) return false
        if (gravity != other.gravity) return false
        if (linearAcceleration != other.linearAcceleration) return false
        if (pressure != other.pressure) return false
        if (temperature != other.temperature) return false
        if (proximity != other.proximity) return false
        if (light != other.light) return false
        if (accuracy != other.accuracy) return false
        if (sensorDelay != other.sensorDelay) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + (accelerometer?.hashCode() ?: 0)
        result = 31 * result + (gyroscope?.hashCode() ?: 0)
        result = 31 * result + (magnetometer?.hashCode() ?: 0)
        result = 31 * result + (rotationVector?.contentHashCode() ?: 0)
        result = 31 * result + (gameRotationVector?.contentHashCode() ?: 0)
        result = 31 * result + (gravity?.hashCode() ?: 0)
        result = 31 * result + (linearAcceleration?.hashCode() ?: 0)
        result = 31 * result + (pressure?.hashCode() ?: 0)
        result = 31 * result + (temperature?.hashCode() ?: 0)
        result = 31 * result + (proximity?.hashCode() ?: 0)
        result = 31 * result + (light?.hashCode() ?: 0)
        result = 31 * result + accuracy
        result = 31 * result + sensorDelay
        return result
    }
}

/**
 * 3D vector data class with utility methods.
 */
@Parcelize
data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
) : Parcelable {

    fun toFloatArray(): FloatArray = floatArrayOf(x, y, z)

    fun magnitude(): Float = kotlin.math.sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3 {
        val mag = magnitude()
        return if (mag > 0) Vector3(x / mag, y / mag, z / mag) else Vector3.zero()
    }

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float): Vector3 = Vector3(x / scalar, y / scalar, z / scalar)

    fun toDegrees(): Vector3 = Vector3(
        x * 180f / kotlin.math.PI.toFloat(),
        y * 180f / kotlin.math.PI.toFloat(),
        z * 180f / kotlin.math.PI.toFloat()
    )

    fun toRadians(): Vector3 = Vector3(
        x * kotlin.math.PI.toFloat() / 180f,
        y * kotlin.math.PI.toFloat() / 180f,
        z * kotlin.math.PI.toFloat() / 180f
    )

    companion object {
        fun fromFloatArray(arr: FloatArray): Vector3 = Vector3(arr[0], arr[1], arr[2])
        fun zero(): Vector3 = Vector3(0f, 0f, 0f)
        fun one(): Vector3 = Vector3(1f, 1f, 1f)
    }
}

/**
 * Complete orientation data from sensor fusion.
 */
@Parcelize
data class OrientationData(
    val roll: Float,   // degrees - rotation around X axis
    val pitch: Float,  // degrees - rotation around Y axis
    val yaw: Float,    // degrees - rotation around Z axis
    val quaternion: FloatArray = floatArrayOf(1f, 0f, 0f, 0f),
    val rotationMatrix: FloatArray = FloatArray(9),
    val confidence: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OrientationData
        if (roll != other.roll) return false
        if (pitch != other.pitch) return false
        if (yaw != other.yaw) return false
        if (!quaternion.contentEquals(other.quaternion)) return false
        if (!rotationMatrix.contentEquals(other.rotationMatrix)) return false
        if (confidence != other.confidence) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = roll.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + quaternion.contentHashCode()
        result = 31 * result + rotationMatrix.contentHashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Processed movement data sent to server.
 */
@Parcelize
data class MovementData(
    val deltaX: Float,
    val deltaY: Float,
    val rawDx: Float = 0f,
    val rawDy: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Gesture detection result.
 */
@Parcelize
data class GestureResult(
    val gestureType: String,
    val confidence: Float,
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Complete calibration data for all sensors.
 */
@Parcelize
data class CalibrationData(
    val gyroBias: Vector3 = Vector3.zero(),
    val accelOffset: Vector3 = Vector3.zero(),
    val accelScale: Vector3 = Vector3(1f, 1f, 1f),
    val magOffset: Vector3 = Vector3.zero(),
    val magScale: Vector3 = Vector3(1f, 1f, 1f),
    val isCalibrated: Boolean = false,
    val lastCalibrationTime: Long = 0,
    val calibrationQuality: Float = 0f
) : Parcelable

/**
 * Bluetooth proximity data.
 */
@Parcelize
data class ProximityData(
    val rssi: Int,
    val distance: Float,
    val isNear: Boolean,
    val deviceAddress: String,
    val deviceName: String? = null,
    val signalStrength: SignalStrength = SignalStrength.NONE,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

enum class SignalStrength(val dbm: Int, val displayName: String) {
    EXCELLENT(-50, "Excellent"),
    GOOD(-60, "Good"),
    FAIR(-70, "Fair"),
    POOR(-80, "Poor"),
    NONE(-100, "None")
}

/**
 * Network message data.
 */
@Parcelize
sealed class NetworkMessage : Parcelable {
    @Parcelize
    data class Move(val dx: Float, val dy: Float) : NetworkMessage()

    @Parcelize
    data class Click(val button: String = "left", val id: Int? = null) : NetworkMessage()

    @Parcelize
    object DoubleClick : NetworkMessage()

    @Parcelize
    object RightClick : NetworkMessage()

    @Parcelize
    data class Scroll(val delta: Int, val id: Int? = null) : NetworkMessage()

    @Parcelize
    data class Gesture(val name: String, val confidence: Float) : NetworkMessage()

    @Parcelize
    data class Proximity(val isNear: Boolean, val distance: Float) : NetworkMessage()

    @Parcelize
    data class Control(val command: String) : NetworkMessage()

    @Parcelize
    data class KeyPress(val keyCode: Int, val keyChar: Char? = null) : NetworkMessage()

    @Parcelize
    data class Hello(val name: String, val version: String) : NetworkMessage()

    @Parcelize
    data class Ack(val id: Int, val success: Boolean) : NetworkMessage()

    @Parcelize
    data class Error(val message: String, val code: Int = 0) : NetworkMessage()

    companion object {
        fun toJson(message: NetworkMessage): String {
            return when (message) {
                is Move -> """{"type":"move","payload":{"dx":${message.dx},"dy":${message.dy}}}"""
                is Click -> """{"type":"click","payload":{"button":"${message.button}"}${message.id?.let { ",\"id\":$it" } ?: ""}}"""
                is DoubleClick -> """{"type":"doubleclick"}"""
                is RightClick -> """{"type":"rightclick"}"""
                is Scroll -> """{"type":"scroll","payload":{"delta":${message.delta}}${message.id?.let { ",\"id\":$it" } ?: ""}}"""
                is Gesture -> """{"type":"gesture","payload":{"gesture":"${message.name}","confidence":${message.confidence}}}"""
                is Proximity -> """{"type":"proximity","payload":{"is_near":${message.isNear},"distance":${message.distance}}}"""
                is Control -> """{"type":"control","payload":{"command":"${message.command}"}}"""
                is KeyPress -> """{"type":"keypress","payload":{"keyCode":${message.keyCode}${message.keyChar?.let { ",\"keyChar\":\"$it\"" } ?: ""}}}"""
                is Hello -> """{"type":"hello","payload":{"name":"${message.name}","version":"${message.version}"}}"""
                is Ack -> """{"type":"ack","id":${message.id},"success":${message.success}}"""
                is Error -> """{"type":"error","payload":{"message":"${message.message}","code":${message.code}}}"""
            }
        }
    }
}

/**
 * Sensor fusion data for advanced processing.
 */
@Parcelize
data class SensorFusionData(
    val orientation: OrientationData,
    val linearAcceleration: Vector3,
    val gravity: Vector3,
    val rotationRate: Vector3,
    val estimatedGyroDrift: Vector3 = Vector3.zero(),
    val filterGain: Float = 0.1f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Device motion state analysis.
 */
@Parcelize
data class MotionStateData(
    val isMoving: Boolean,
    val isStable: Boolean,
    val activityType: String,
    val confidence: Float,
    val lastMovementTime: Long,
    val stabilityDuration: Long,
    val movementIntensity: Float,
    val dominantAxis: String
) : Parcelable

/**
 * Battery status data.
 */
@Parcelize
data class BatteryData(
    val level: Int,
    val temperature: Float,
    val voltage: Float,
    val current: Float,
    val health: String,
    val status: String,
    val isCharging: Boolean,
    val plugged: String,
    val technology: String,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Connection statistics data.
 */
@Parcelize
data class ConnectionStatsData(
    val totalConnections: Int,
    val successfulConnections: Int,
    val failedConnections: Int,
    val averageLatency: Int,
    val lastConnectionTime: Long,
    val totalDataSent: Long,
    val totalDataReceived: Long,
    val currentUptime: Long
) : Parcelable

/**
 * Performance metrics data.
 */
@Parcelize
data class PerformanceData(
    val cpuUsage: Float,
    val memoryUsage: Float,
    val batteryUsage: Int,
    val fps: Float,
    val sensorUpdateRate: Float,
    val networkLatency: Int,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Log entry data.
 */
@Parcelize
data class LogEntryData(
    val id: String,
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String,
    val details: String? = null
) : Parcelable