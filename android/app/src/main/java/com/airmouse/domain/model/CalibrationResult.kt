package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

/**
 * Result of a calibration session.
 * Contains quality assessment, scores, and sensor calibration data.
 */
@Parcelize
data class CalibrationResult(
    val quality: CalibrationQuality = CalibrationQuality.UNKNOWN,
    val score: Float = 0f,
    val isSuccessful: Boolean = false,
    val message: String = "",
    val gyroBias: SensorCalibrationData = SensorCalibrationData(),
    val accelCalibration: SensorCalibrationData = SensorCalibrationData(),
    val magCalibration: SensorCalibrationData = SensorCalibrationData(),
    val completedAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Get a formatted summary string for display.
     */
    fun getFormattedSummary(): String {
        return buildString {
            appendLine("📊 Calibration Summary")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Quality: ${quality.name}")
            appendLine("Status: ${if (isSuccessful) "✅ Complete" else "⏳ Incomplete"}")
            appendLine("Score: ${"%.1f".format(score)}%")
            appendLine("Completed: ${getFormattedDate()}")
            appendLine()
            appendLine("Gyro Bias: X=${"%.4f".format(gyroBias.offsetX)}, Y=${"%.4f".format(gyroBias.offsetY)}, Z=${"%.4f".format(gyroBias.offsetZ)}")
            appendLine("Accel Offset: X=${"%.4f".format(accelCalibration.offsetX)}, Y=${"%.4f".format(accelCalibration.offsetY)}, Z=${"%.4f".format(accelCalibration.offsetZ)}")
            appendLine("Mag Offset: X=${"%.4f".format(magCalibration.offsetX)}, Y=${"%.4f".format(magCalibration.offsetY)}, Z=${"%.4f".format(magCalibration.offsetZ)}")
            if (message.isNotEmpty()) {
                appendLine()
                appendLine("Message: $message")
            }
        }
    }

    /**
     * Get a short summary for quick display.
     */
    fun getShortSummary(): String {
        return "${quality.name} - ${"%.0f".format(score)}%"
    }

    /**
     * Get the formatted completion date.
     */
    fun getFormattedDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(completedAt))
    }

    /**
     * Check if the calibration was successful.
     */
    fun wasSuccessful(): Boolean = isSuccessful

    /**
     * Get the quality level as a numeric grade (0-100).
     */
    fun getQualityGrade(): Int {
        return when (quality) {
            CalibrationQuality.EXCELLENT -> 95
            CalibrationQuality.GOOD -> 80
            CalibrationQuality.FAIR -> 65
            CalibrationQuality.POOR -> 40
            CalibrationQuality.UNKNOWN -> 0
        }
    }

    /**
     * Get a colour code for the quality.
     */
    fun getQualityColor(): Long {
        return when (quality) {
            CalibrationQuality.EXCELLENT -> 0xFF10B981  // Green
            CalibrationQuality.GOOD -> 0xFF3B82F6      // Blue
            CalibrationQuality.FAIR -> 0xFFF59E0B      // Yellow
            CalibrationQuality.POOR -> 0xFFEF4444      // Red
            CalibrationQuality.UNKNOWN -> 0xFF64748B   // Grey
        }
    }

    /**
     * Get an emoji for the quality.
     */
    fun getQualityEmoji(): String {
        return when (quality) {
            CalibrationQuality.EXCELLENT -> "🌟"
            CalibrationQuality.GOOD -> "👍"
            CalibrationQuality.FAIR -> "⚠️"
            CalibrationQuality.POOR -> "❌"
            CalibrationQuality.UNKNOWN -> "❓"
        }
    }

    /**
     * Check if the calibration quality is acceptable for use.
     */
    fun isAcceptable(): Boolean {
        return quality == CalibrationQuality.EXCELLENT ||
                quality == CalibrationQuality.GOOD
    }

    /**
     * Get a recommendation based on quality.
     */
    fun getRecommendation(): String {
        return when (quality) {
            CalibrationQuality.EXCELLENT -> "Perfect calibration! Device is ready for use."
            CalibrationQuality.GOOD -> "Calibration is good. Device is ready for use."
            CalibrationQuality.FAIR -> "Calibration is fair. Consider recalibrating for better accuracy."
            CalibrationQuality.POOR -> "Calibration quality is poor. Please recalibrate."
            CalibrationQuality.UNKNOWN -> "Calibration quality could not be determined. Please recalibrate."
        }
    }

    companion object {
        /**
         * Create a successful calibration result.
         */
        fun success(
            quality: CalibrationQuality,
            score: Float,
            gyroBias: SensorCalibrationData,
            accelCalibration: SensorCalibrationData,
            magCalibration: SensorCalibrationData,
            message: String = "Calibration completed successfully"
        ): CalibrationResult {
            return CalibrationResult(
                quality = quality,
                score = score.coerceIn(0f, 100f),
                isSuccessful = true,
                message = message,
                gyroBias = gyroBias,
                accelCalibration = accelCalibration,
                magCalibration = magCalibration,
                completedAt = System.currentTimeMillis()
            )
        }

        /**
         * Create a failed calibration result.
         */
        fun failure(
            message: String,
            quality: CalibrationQuality = CalibrationQuality.UNKNOWN
        ): CalibrationResult {
            return CalibrationResult(
                quality = quality,
                score = 0f,
                isSuccessful = false,
                message = message,
                completedAt = System.currentTimeMillis()
            )
        }

        /**
         * Create an empty calibration result.
         */
        fun empty(): CalibrationResult {
            return CalibrationResult()
        }
    }
}
