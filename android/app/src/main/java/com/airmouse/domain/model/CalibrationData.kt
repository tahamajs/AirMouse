// app/src/main/java/com/airmouse/domain/model/CalibrationData.kt
package com.airmouse.domain.model

/**
 * Gyroscope bias (offset) for each axis.
 */
data class GyroBias(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

/**
 * Accelerometer calibration parameters (offset and scale for each axis).
 */
data class AccelCalibration(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f
)

/**
 * Magnetometer hard‑iron calibration.
 */
data class MagCalibration(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f
)

/**
 * Overall calibration state.
 */
data class CalibrationState(
    val gyroCalibrated: Boolean = false,
    val accelCalibrated: Boolean = false,
    val magCalibrated: Boolean = false,
    val lastCalibrationDate: Long? = null
)