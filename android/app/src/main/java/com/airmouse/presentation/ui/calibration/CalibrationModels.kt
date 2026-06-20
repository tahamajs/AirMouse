// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationModels.kt
package com.airmouse.presentation.ui.calibration

import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus

// ==========================================
// UI STATE
// ==========================================

data class CalibrationUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 3,
    val isCalibrating: Boolean = false,
    val isCollecting: Boolean = false,
    val isComplete: Boolean = false,
    val isSkipped: Boolean = false,
    val progress: Int = 0,
    val stepProgress: Float = 0f,
    val samplesCollected: Int = 0,
    val totalSamplesNeeded: Int = 0,
    val statusMessage: String = "",
    val stepInstruction: String = "",
    val detailedInstruction: String = "",
    val errorMessage: String? = null,
    val calibrationQuality: String = "",
    val quality: String = "",
    val showConfetti: Boolean = false,
    val currentPosition: Int = 0,
    val totalPositions: Int = 6,
    val completedPositions: List<Int> = emptyList(),
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val gyroData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val isServerConnected: Boolean = false
) {
    companion object {
        fun initial() = CalibrationUiState(
            statusMessage = "Ready to calibrate",
            stepInstruction = "Tap 'Start Calibration' to begin"
        )
    }
}

// ==========================================
// SENSOR DATA
// ==========================================

data class SensorData(
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f
)

// ==========================================
// CALIBRATION RESULT
// ==========================================

data class CalibrationResult(
    val gyroOffsets: Triple<Float, Float, Float>,
    val accelOffsets: Triple<Float, Float, Float>,
    val magOffsets: Triple<Float, Float, Float>,
    val quality: CalibrationQuality,
    val isComplete: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)