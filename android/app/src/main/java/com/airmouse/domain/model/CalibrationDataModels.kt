package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class CalibrationStatus {
    NOT_STARTED,
    IN_PROGRESS,
    GYRO_COMPLETE,
    MAG_COMPLETE,
    ACCEL_COMPLETE,
    COMPLETED,
    FAILED,
    SKIPPED,
    IDLE
}

enum class CalibrationQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNKNOWN
}

@Parcelize
data class SensorCalibrationData(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f
) : Parcelable

@Parcelize
data class CalibrationData(
    val gyroBias: SensorCalibrationData = SensorCalibrationData(),
    val accelOffset: SensorCalibrationData = SensorCalibrationData(),
    val magOffset: SensorCalibrationData = SensorCalibrationData(),
    val isCalibrated: Boolean = false,
    val quality: CalibrationQuality = CalibrationQuality.UNKNOWN
) : Parcelable
