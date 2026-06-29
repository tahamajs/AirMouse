package com.airmouse.domain.model

data class CalibrationState(
    val gyroCalibrated: Boolean = false,
    val accelCalibrated: Boolean = false,
    val magCalibrated: Boolean = false,
    val lastCalibrationDate: Long = 0L,
    val calibrationQuality: Float = 0f,
    val needsRecalibration: Boolean = false,
    val recommendedActions: List<String> = emptyList()
)
