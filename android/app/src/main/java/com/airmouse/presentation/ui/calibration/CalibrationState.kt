package com.airmouse.presentation.ui.calibration

data class CalibrationUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 3,
    val stepTitle: String = "Gyroscope",
    val stepDescription: String = "Place phone on a flat surface",
    val statusMessage: String = "Ready",
    val progress: Int = 0,
    val timerSeconds: Int = 0,
    val isCollecting: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

sealed class CalibrationStep {
    object Gyroscope : CalibrationStep()
    object Accelerometer : CalibrationStep()
    object Magnetometer : CalibrationStep()
}