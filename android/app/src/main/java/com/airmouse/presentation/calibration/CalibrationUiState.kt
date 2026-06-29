package com.airmouse.presentation.ui.calibration

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.SensorCalibrationData

/**
 * UI State for the Calibration screen.
 */
data class CalibrationUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 3,
    val calibrationPhase: CalibrationPhase = CalibrationPhase.INTRO,

    val isCalibrating: Boolean = false,
    val isCollecting: Boolean = false,
    val isComplete: Boolean = false,
    val isSkipped: Boolean = false,
    val isCalibrationApplied: Boolean = false,

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

    val isServerConnected: Boolean = false,

    val calibrationData: CalibrationData? = null
) {
    companion object {
        fun initial() = CalibrationUiState(
            statusMessage = "Loading calibration state...",
            stepInstruction = "If a saved calibration exists, it will appear here automatically."
        )
    }
}

enum class CalibrationPhase {
    INTRO,
    COUNTDOWN,
    SAMPLING
}

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

data class CalibrationResult(
    val gyroOffsets: Triple<Float, Float, Float>,
    val accelOffsets: Triple<Float, Float, Float>,
    val magOffsets: Triple<Float, Float, Float>,
    val accelScale: Triple<Float, Float, Float> = Triple(1f, 1f, 1f),
    val magScale: Triple<Float, Float, Float> = Triple(1f, 1f, 1f),
    val quality: CalibrationQuality,
    val isComplete: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toCalibrationData(): CalibrationData {
        return CalibrationData(
            gyroBias = SensorCalibrationData(
                offsetX = gyroOffsets.first,
                offsetY = gyroOffsets.second,
                offsetZ = gyroOffsets.third
            ),
            accelOffset = SensorCalibrationData(
                offsetX = accelOffsets.first,
                offsetY = accelOffsets.second,
                offsetZ = accelOffsets.third
            ),
            magOffset = SensorCalibrationData(
                offsetX = magOffsets.first,
                offsetY = magOffsets.second,
                offsetZ = magOffsets.third
            ),
            isCalibrated = isComplete,
            quality = quality,
            timestamp = timestamp
        )
    }

    fun getFormattedSummary(): String {
        return buildString {
            appendLine("📊 Calibration Summary")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Quality: ${quality.name}")
            appendLine("Status: ${if (isComplete) "✅ Complete" else "⏳ Incomplete"}")
            appendLine("Gyro Offsets: X=${"%.4f".format(gyroOffsets.first)}, Y=${"%.4f".format(gyroOffsets.second)}, Z=${"%.4f".format(gyroOffsets.third)}")
            appendLine("Accel Offsets: X=${"%.4f".format(accelOffsets.first)}, Y=${"%.4f".format(accelOffsets.second)}, Z=${"%.4f".format(accelOffsets.third)}")
            appendLine("Mag Offsets: X=${"%.4f".format(magOffsets.first)}, Y=${"%.4f".format(magOffsets.second)}, Z=${"%.4f".format(magOffsets.third)}")
        }
    }
}

data class CalibrationProgress(
    val step: CalibrationStep,
    val progress: Float,
    val message: String,
    val isComplete: Boolean = false,
    val error: String? = null
)

enum class CalibrationStep(val displayName: String, val icon: String) {
    GYROSCOPE("Gyroscope", "📱"),
    MAGNETOMETER("Magnetometer", "🔄"),
    ACCELEROMETER("Accelerometer", "📐"),
    COMPLETE("Complete", "✅")
}