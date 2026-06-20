package com.airmouse.presentation.calibration

import androidx.compose.ui.graphics.Color
import com.airmouse.domain.model.CalibrationData

/**
 * Represents the complete UI state for the calibration process.
 * Used by CalibrationViewModel to drive the UI.
 */
data class CalibrationUiState(
    // Progress and status
    val progress: Int = 0,
    val statusMessage: String = "Ready to calibrate",
    val isComplete: Boolean = false,
    val isCollecting: Boolean = false,
    val isSkipped: Boolean = false,

    // Quality assessment
    val calibrationQuality: String = "UNKNOWN",
    val quality: String = "UNKNOWN",

    // Calibration data
    val calibrationData: CalibrationData? = null,
    val errorMessage: String? = null,

    // Step tracking
    val currentStep: Int = 1,
    val totalSteps: Int = 3,

    // Sensor orientation (degrees)
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,

    // Raw sensor data
    val gyroData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),

    // Sample collection
    val samplesCollected: Int = 0,
    val totalSamplesNeeded: Int = 100,
    val stepProgress: Float = 0f,

    // Accelerometer position tracking
    val currentPosition: Int = 0,
    val totalPositions: Int = 6,
    val completedPositions: List<Int> = emptyList(),

    // Instruction
    val stepInstruction: String = "",
    val detailedInstruction: String = "",

    // Flags
    val isCalibrating: Boolean = false,
    val isProcessing: Boolean = false,
    val showConfetti: Boolean = false,
    val showDetails: Boolean = false
) {
    // Derived properties
    val isStepComplete: Boolean get() = stepProgress >= 1f
    val isFullyComplete: Boolean get() = isComplete && progress >= 100
    val hasError: Boolean get() = errorMessage != null
    val canProceed: Boolean get() = isStepComplete && !hasError && !isCalibrating

    val stepLabel: String get() = when (currentStep) {
        1 -> "Gyroscope"
        2 -> "Magnetometer"
        3 -> "Accelerometer"
        else -> "Complete"
    }

    val progressPercentage: Int get() = progress.coerceIn(0, 100)

    val qualityColor: Color
        get() = when (calibrationQuality.uppercase()) {
            "EXCELLENT" -> Color(0xFF10B981)
            "GOOD" -> Color(0xFF3B82F6)
            "FAIR" -> Color(0xFFF59E0B)
            "POOR" -> Color(0xFFEF4444)
            else -> Color(0xFF64748B)
        }

    val qualityEmoji: String get() = when (calibrationQuality.uppercase()) {
        "EXCELLENT" -> "🌟"
        "GOOD" -> "👍"
        "FAIR" -> "⚠️"
        "POOR" -> "❌"
        else -> "❓"
    }

    companion object {
        fun initial() = CalibrationUiState()

        fun calibrating(step: Int = 1) = CalibrationUiState(
            isCalibrating = true,
            isCollecting = true,
            currentStep = step,
            statusMessage = "Calibrating ${
                when(step) {
                    1 -> "gyroscope"
                    2 -> "magnetometer"
                    3 -> "accelerometer"
                    else -> "sensors"
                }
            }..."
        )

        fun complete(quality: String = "GOOD") = CalibrationUiState(
            isComplete = true,
            isCalibrating = false,
            isCollecting = false,
            calibrationQuality = quality,
            quality = quality,
            progress = 100,
            statusMessage = "Calibration complete!",
            showConfetti = true
        )

        fun error(message: String) = CalibrationUiState(
            errorMessage = message,
            isCalibrating = false,
            isCollecting = false,
            statusMessage = "Error: $message"
        )
    }
}

/**
 * Extended UI state with additional stats for the results screen.
 */
data class CalibrationResultUiState(
    val calibrationData: CalibrationData? = null,
    val stats: Map<String, Any>? = null,
    val showDetails: Boolean = false,
    val isComplete: Boolean = false,
    val quality: String = "UNKNOWN",
    val message: String = ""
)

/**
 * Calibration step definitions for the guide.
 */
sealed class CalibrationStepGuide(
    val index: Int,
    val title: String,
    val instruction: String,
    val description: String,
    val icon: String
) {
    object GYROSCOPE : CalibrationStepGuide(
        index = 1,
        title = "Gyroscope",
        instruction = "Place device on a flat, stationary surface",
        description = "Keep the device perfectly still for 5 seconds",
        icon = "🧭"
    )

    object MAGNETOMETER : CalibrationStepGuide(
        index = 2,
        title = "Magnetometer",
        instruction = "Move device in a smooth figure-8 pattern",
        description = "Ensure you cover all axes of movement",
        icon = "🧲"
    )

    object ACCELEROMETER : CalibrationStepGuide(
        index = 3,
        title = "Accelerometer",
        instruction = "Rotate device to each orientation",
        description = "Hold each position steady for 3 seconds",
        icon = "📐"
    )

    object COMPLETE : CalibrationStepGuide(
        index = 4,
        title = "Complete",
        instruction = "Calibration successful!",
        description = "Your device is now fully calibrated",
        icon = "✅"
    )

    companion object {
        fun fromIndex(index: Int): CalibrationStepGuide = when (index) {
            1 -> GYROSCOPE
            2 -> MAGNETOMETER
            3 -> ACCELEROMETER
            else -> COMPLETE
        }
    }
}
