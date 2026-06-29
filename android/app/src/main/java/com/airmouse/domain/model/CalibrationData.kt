package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the full calibration data for all sensors.
 */
@Parcelize
data class CalibrationData(
    val gyroBias: SensorCalibrationData = SensorCalibrationData(),
    val accelOffset: SensorCalibrationData = SensorCalibrationData(),
    val magOffset: SensorCalibrationData = SensorCalibrationData(),
    val isCalibrated: Boolean = false,
    val quality: CalibrationQuality = CalibrationQuality.UNKNOWN,
    val timestamp: Long = 0L
) : Parcelable
