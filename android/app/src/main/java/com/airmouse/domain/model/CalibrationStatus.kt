// app/src/main/java/com/airmouse/domain/model/CalibrationStatus.kt
package com.airmouse.domain.model

data class CalibrationStatus(
    val gyroCalibrated: Boolean,
    val accelCalibrated: Boolean,
    val magCalibrated: Boolean,
    val allCalibrated: Boolean,
    val progress: Int,
    val confidence: Float,
    val lastCalibrationTime: Long = 0  // ✅ Add this field
)