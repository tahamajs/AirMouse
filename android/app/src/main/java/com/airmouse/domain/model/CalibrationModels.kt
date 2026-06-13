package com.airmouse.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Gyroscope bias (offset) for each axis.
 */
@Entity(tableName = "gyro_bias")
@Parcelize
data class GyroBias(
    @PrimaryKey val id: Int = 1,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Accelerometer calibration parameters (offset and scale for each axis).
 */
@Entity(tableName = "accel_calibration")
@Parcelize
data class AccelCalibration(
    @PrimaryKey val id: Int = 1,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Magnetometer hard-iron calibration.
 */
@Entity(tableName = "mag_calibration")
@Parcelize
data class MagCalibration(
    @PrimaryKey val id: Int = 1,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Overall calibration state.
 */
@Parcelize
data class CalibrationState(
    val gyroCalibrated: Boolean = false,
    val accelCalibrated: Boolean = false,
    val magCalibrated: Boolean = false,
    val lastCalibrationDate: Long? = null,
    val calibrationQuality: Float = 0f,
    val needsRecalibration: Boolean = false,
    val recommendedActions: List<String> = emptyList()
) : Parcelable

/**
 * Calibration progress for UI.
 */
@Parcelize
data class CalibrationProgress(
    val step: CalibrationStep,
    val progress: Int,
    val message: String,
    val isComplete: Boolean = false,
    val error: String? = null
) : Parcelable

enum class CalibrationStep(val displayName: String) {
    GYROSCOPE("Gyroscope Calibration"),
    ACCELEROMETER("Accelerometer Calibration"),
    MAGNETOMETER("Magnetometer Calibration")
}
