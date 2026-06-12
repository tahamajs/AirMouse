package com.airmouse.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Gyroscope bias (offset) for each axis.
 */
@Entity(tableName = "gyro_bias")
data class GyroBias(
    @PrimaryKey val id: Int = 1,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

/**
 * Accelerometer calibration parameters (offset and scale for each axis).
 */
@Entity(tableName = "accel_calibration")
data class AccelCalibration(
    @PrimaryKey val id: Int = 1,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f
)

/**
 * Magnetometer hard-iron calibration.
 */
@Entity(tableName = "mag_calibration")
data class MagCalibration(
    @PrimaryKey val id: Int = 1,
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
