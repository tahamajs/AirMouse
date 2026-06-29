package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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