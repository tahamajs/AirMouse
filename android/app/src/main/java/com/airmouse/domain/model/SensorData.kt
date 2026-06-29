package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.sqrt

/**
 * Raw sensor data from device sensors including gyroscope, accelerometer,
 * magnetometer, and orientation data.
 */
@Parcelize
data class SensorData(
    // Gyroscope data (rad/s)
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,

    // Accelerometer data (m/s²)
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,

    // Magnetometer data (μT)
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,

    // Orientation data (radians)
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Calculate the magnitude of the accelerometer vector.
     */
    fun getAccelMagnitude(): Float {
        return sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
    }

    /**
     * Calculate the magnitude of the gyroscope vector.
     */
    fun getGyroMagnitude(): Float {
        return sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
    }

    /**
     * Calculate the magnitude of the magnetometer vector.
     */
    fun getMagMagnitude(): Float {
        return sqrt(magX * magX + magY * magY + magZ * magZ)
    }

    /**
     * Check if this is a significant motion event.
     */
    fun isSignificantMotion(threshold: Float = 0.5f): Boolean {
        return getAccelMagnitude() > (9.8f + threshold) ||
                getGyroMagnitude() > 0.5f ||
                getMagMagnitude() > 10f
    }

    /**
     * Check if the device is stationary.
     */
    fun isStationary(gyroThreshold: Float = 0.1f): Boolean {
        return getGyroMagnitude() < gyroThreshold
    }

    /**
     * Get the sensor values as a FloatArray for ML inference.
     */
    fun toFloatArray(): FloatArray {
        return floatArrayOf(
            gyroX, gyroY, gyroZ,
            accelX, accelY, accelZ,
            magX, magY, magZ,
            roll, pitch, yaw
        )
    }

    /**
     * Get orientation in degrees.
     */
    fun getOrientationDegrees(): Triple<Float, Float, Float> {
        return Triple(
            Math.toDegrees(roll.toDouble()).toFloat(),
            Math.toDegrees(pitch.toDouble()).toFloat(),
            Math.toDegrees(yaw.toDouble()).toFloat()
        )
    }

    /**
     * Get a summary string for debugging.
     */
    fun getSummary(): String {
        return buildString {
            appendLine("📊 Sensor Data")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Gyroscope: X=${"%.3f".format(gyroX)}, Y=${"%.3f".format(gyroY)}, Z=${"%.3f".format(gyroZ)}")
            appendLine("Accelerometer: X=${"%.3f".format(accelX)}, Y=${"%.3f".format(accelY)}, Z=${"%.3f".format(accelZ)}")
            appendLine("Magnetometer: X=${"%.3f".format(magX)}, Y=${"%.3f".format(magY)}, Z=${"%.3f".format(magZ)}")
            appendLine("Orientation: Roll=${"%.2f".format(Math.toDegrees(roll.toDouble()))}°, " +
                    "Pitch=${"%.2f".format(Math.toDegrees(pitch.toDouble()))}°, " +
                    "Yaw=${"%.2f".format(Math.toDegrees(yaw.toDouble()))}°")
            appendLine("Timestamp: ${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(timestamp)}")
        }
    }

    companion object {
        /**
         * Create a zeroed SensorData.
         */
        fun zero(): SensorData {
            return SensorData()
        }

        /**
         * Create SensorData from arrays.
         */
        fun fromArrays(
            gyro: FloatArray,
            accel: FloatArray,
            mag: FloatArray,
            orientation: FloatArray = floatArrayOf(0f, 0f, 0f)
        ): SensorData {
            return SensorData(
                gyroX = gyro.getOrNull(0) ?: 0f,
                gyroY = gyro.getOrNull(1) ?: 0f,
                gyroZ = gyro.getOrNull(2) ?: 0f,
                accelX = accel.getOrNull(0) ?: 0f,
                accelY = accel.getOrNull(1) ?: 0f,
                accelZ = accel.getOrNull(2) ?: 0f,
                magX = mag.getOrNull(0) ?: 0f,
                magY = mag.getOrNull(1) ?: 0f,
                magZ = mag.getOrNull(2) ?: 0f,
                roll = orientation.getOrNull(0) ?: 0f,
                pitch = orientation.getOrNull(1) ?: 0f,
                yaw = orientation.getOrNull(2) ?: 0f
            )
        }

        /**
         * Create a SensorData with gravity-only accelerometer values (no motion).
         */
        fun gravityOnly(): SensorData {
            return SensorData(
                accelZ = 9.81f
            )
        }
    }
}