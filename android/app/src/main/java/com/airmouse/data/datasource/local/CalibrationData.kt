package com.airmouse.data.datasource.local

data class CalibrationData(
    val gyroBiasX: Float = 0f,
    val gyroBiasY: Float = 0f,
    val gyroBiasZ: Float = 0f,
    val accelOffsetX: Float = 0f,
    val accelOffsetY: Float = 0f,
    val accelOffsetZ: Float = 0f,
    val magOffsetX: Float = 0f,
    val magOffsetY: Float = 0f,
    val magOffsetZ: Float = 0f,
    val isCalibrated: Boolean = false,
    val quality: String = "UNKNOWN",
    val timestamp: Long = System.currentTimeMillis()
)
