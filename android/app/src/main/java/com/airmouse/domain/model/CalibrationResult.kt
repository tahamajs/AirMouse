package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CalibrationResult(
    val quality: CalibrationQuality = CalibrationQuality.UNKNOWN,
    val score: Float = 0f,
    val isSuccessful: Boolean = false,
    val message: String = "",
    val gyroBias: SensorCalibrationData = SensorCalibrationData(),
    val accelCalibration: SensorCalibrationData = SensorCalibrationData(),
    val magCalibration: SensorCalibrationData = SensorCalibrationData(),
    val completedAt: Long = System.currentTimeMillis()
) : Parcelable
