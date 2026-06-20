// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationUiState.kt
package com.airmouse.presentation.ui.calibration

import com.airmouse.domain.model.CalibrationData

data class CalibrationUiState(
    val progress: Int = 0,
    val statusMessage: String = "Ready to calibrate",
    val isComplete: Boolean = false,
    val isCollecting: Boolean = false,
    val calibrationQuality: String = "UNKNOWN",
    val quality: String = "UNKNOWN",
    val calibrationData: CalibrationData? = null,
    val errorMessage: String? = null,
    val currentStep: Int = 1,
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val gyroData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val samplesCollected: Int = 0,
    val totalSamplesNeeded: Int = 100,
    val stepInstruction: String = "",
    val currentPosition: Int = 0,
    val isSkipped: Boolean = false
)