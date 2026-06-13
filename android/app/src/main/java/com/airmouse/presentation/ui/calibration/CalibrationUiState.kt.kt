// CalibrationUiState.kt
package com.airmouse.presentation.ui.calibration

data class CalibrationUiState(
    val currentStep: Int = 0,
    val stepTitle: String = "Gyroscope Calibration",
    val stepDescription: String = "Place the phone on a flat, stationary surface",
    val stepInstruction: String = "Keep the phone perfectly still",
    val statusMessage: String = "Ready to calibrate",
    val progress: Int = 0,
    val isCollecting: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
    val calibrationData: CalibrationData = CalibrationData(),
    val currentPosition: Int = 0,
    val totalPositions: Int = 6,
    val samplesCollected: Int = 0,
    val totalSamplesNeeded: Int = 500
)

data class CalibrationData(
    val gyroOffsetX: Float = 0f,
    val gyroOffsetY: Float = 0f,
    val gyroOffsetZ: Float = 0f,
    val accelOffsetX: Float = 0f,
    val accelOffsetY: Float = 0f,
    val accelOffsetZ: Float = 0f,
    val accelScaleX: Float = 1f,
    val accelScaleY: Float = 1f,
    val accelScaleZ: Float = 1f,
    val magOffsetX: Float = 0f,
    val magOffsetY: Float = 0f,
    val magOffsetZ: Float = 0f,
    val magScaleX: Float = 1f,
    val magScaleY: Float = 1f,
    val magScaleZ: Float = 1f
)

val positions = listOf(
    "Flat, screen up (Z+)",
    "Flat, screen down (Z-)",
    "Vertical, port down (X+)",
    "Vertical, port up (X-)",
    "Landscape, left side down (Y+)",
    "Landscape, right side down (Y-)"
)