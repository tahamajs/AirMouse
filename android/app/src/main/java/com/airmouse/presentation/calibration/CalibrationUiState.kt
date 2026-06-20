// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationModels.kt
package com.airmouse.presentation.ui.calibration

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.SensorCalibrationData

// ==========================================
// UI STATE
// ==========================================

/**
 * UI state for the calibration screen.
 * Contains all UI-specific state that drives the Compose UI.
 */
data class CalibrationUiState(
    // Step tracking
    val currentStep: Int = 0,
    val totalSteps: Int = 3,

    // Calibration state
    val isCalibrating: Boolean = false,
    val isCollecting: Boolean = false,
    val isComplete: Boolean = false,
    val isSkipped: Boolean = false,
    val isCalibrationApplied: Boolean = false,

    // Progress tracking
    val progress: Int = 0,
    val stepProgress: Float = 0f,
    val samplesCollected: Int = 0,
    val totalSamplesNeeded: Int = 0,

    // Status messages
    val statusMessage: String = "",
    val stepInstruction: String = "",
    val detailedInstruction: String = "",
    val errorMessage: String? = null,

    // Quality & Results
    val calibrationQuality: String = "",
    val quality: String = "",
    val showConfetti: Boolean = false,

    // Accelerometer positions
    val currentPosition: Int = 0,
    val totalPositions: Int = 6,
    val completedPositions: List<Int> = emptyList(),

    // Sensor data
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val gyroData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),

    // Connection
    val isServerConnected: Boolean = false,

    // Calibration data from repository
    val calibrationData: CalibrationData? = null
) {
    companion object {
        /**
         * Creates an initial state for the calibration screen.
         */
        fun initial() = CalibrationUiState(
            statusMessage = "Ready to calibrate",
            stepInstruction = "Tap 'Start Calibration' to begin"
        )
    }
}

// ==========================================
// SENSOR DATA
// ==========================================

/**
 * Raw sensor data from device sensors.
 * Used for real-time display during calibration.
 */
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

/**
 * Result of a calibration session.
 * Used for reporting and storing calibration data.
 */
data class CalibrationResult(
    val gyroOffsets: Triple<Float, Float, Float>,
    val accelOffsets: Triple<Float, Float, Float>,
    val magOffsets: Triple<Float, Float, Float>,
    val quality: CalibrationQuality,
    val isComplete: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts to CalibrationData domain model.
     */
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

    /**
     * Returns a formatted string representation of the result.
     */
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

// ==========================================
// CALIBRATION QUALITY CONFIG
// ==========================================

/**
 * UI configuration for calibration quality display.
 */
data class CalibrationQualityConfig(
    val color: androidx.compose.ui.graphics.Color,
    val emoji: String,
    val title: String,
    val description: String,
    val subtext: String,
    val score: String,
    val status: String
) {
    companion object {
        /**
         * Gets quality config for a given quality.
         */
        fun fromQuality(quality: CalibrationQuality): CalibrationQualityConfig {
            return when (quality) {
                CalibrationQuality.EXCELLENT -> CalibrationQualityConfig(
                    color = androidx.compose.ui.graphics.Color(0xFF10B981),
                    emoji = "🌟",
                    title = "Excellent",
                    description = "Perfect calibration! Your device is performing at its best.",
                    subtext = "All sensors are optimally calibrated.",
                    score = "95%",
                    status = "✅ Ready"
                )
                CalibrationQuality.GOOD -> CalibrationQualityConfig(
                    color = androidx.compose.ui.graphics.Color(0xFF3B82F6),
                    emoji = "👍",
                    title = "Good",
                    description = "Calibration successful with good accuracy.",
                    subtext = "Device is ready for use.",
                    score = "80%",
                    status = "✅ Ready"
                )
                CalibrationQuality.FAIR -> CalibrationQualityConfig(
                    color = androidx.compose.ui.graphics.Color(0xFFF59E0B),
                    emoji = "⚠️",
                    title = "Fair",
                    description = "Calibration complete with fair accuracy.",
                    subtext = "Consider recalibrating for best results.",
                    score = "60%",
                    status = "⚠️ Review"
                )
                CalibrationQuality.POOR -> CalibrationQualityConfig(
                    color = androidx.compose.ui.graphics.Color(0xFFEF4444),
                    emoji = "❌",
                    title = "Poor",
                    description = "Calibration quality is low.",
                    subtext = "Please recalibrate for better results.",
                    score = "30%",
                    status = "❌ Recalibrate"
                )
                CalibrationQuality.UNKNOWN -> CalibrationQualityConfig(
                    color = androidx.compose.ui.graphics.Color(0xFF64748B),
                    emoji = "❓",
                    title = "Unknown",
                    description = "Calibration completed but quality could not be determined.",
                    subtext = "Please recalibrate for best results.",
                    score = "50%",
                    status = "❓ Unknown"
                )
            }
        }

        /**
         * Gets quality config from string quality name.
         */
        fun fromQualityString(quality: String): CalibrationQualityConfig {
            return when (quality.uppercase()) {
                "EXCELLENT" -> fromQuality(CalibrationQuality.EXCELLENT)
                "GOOD" -> fromQuality(CalibrationQuality.GOOD)
                "FAIR" -> fromQuality(CalibrationQuality.FAIR)
                "POOR" -> fromQuality(CalibrationQuality.POOR)
                else -> fromQuality(CalibrationQuality.UNKNOWN)
            }
        }
    }
}

// ==========================================
// CALIBRATION PROGRESS
// ==========================================

/**
 * Represents the progress of a calibration step.
 */
data class CalibrationProgress(
    val step: CalibrationStep,
    val progress: Float,
    val message: String,
    val isComplete: Boolean = false,
    val error: String? = null
)

/**
 * Calibration steps enum.
 */
enum class CalibrationStep(val displayName: String, val icon: String) {
    GYROSCOPE("Gyroscope", "📱"),
    MAGNETOMETER("Magnetometer", "🔄"),
    ACCELEROMETER("Accelerometer", "📐"),
    COMPLETE("Complete", "✅")
}

// ==========================================
// CALIBRATION EVENT
// ==========================================

/**
 * Events that can be sent to the ViewModel.
 */
sealed class CalibrationEvent {
    // Lifecycle events
    object StartCalibration : CalibrationEvent()
    object ResetCalibration : CalibrationEvent()
    object SkipCalibration : CalibrationEvent()
    object RetryCalibration : CalibrationEvent()
    object ApplyCalibration : CalibrationEvent()
    object SyncToServer : CalibrationEvent()
    object NextStep : CalibrationEvent()

    // User interaction events
    data class SelectPosition(val position: Int) : CalibrationEvent()
    data class UpdateSensorData(val data: SensorData) : CalibrationEvent()
    data class UpdateOrientation(val roll: Float, val pitch: Float, val yaw: Float) : CalibrationEvent()

    // Navigation events
    object NavigateBack : CalibrationEvent()
    object NavigateToHome : CalibrationEvent()
}

// ==========================================
// CALIBRATION EFFECT
// ==========================================

/**
 * Side effects for the calibration screen.
 */
sealed class CalibrationEffect {
    data class ShowToast(val message: String) : CalibrationEffect()
    data class ShowError(val message: String) : CalibrationEffect()
    data class NavigateTo(val route: String) : CalibrationEffect()
    object NavigateBack : CalibrationEffect()
    object NavigateToHome : CalibrationEffect()
    data class PlaySound(val soundId: Int) : CalibrationEffect()
    data class Vibrate(val duration: Long) : CalibrationEffect()
}
