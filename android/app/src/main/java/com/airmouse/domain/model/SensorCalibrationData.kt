package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Sensor calibration data containing offset and scale factors for a sensor.
 *
 * Offsets represent the bias (zero-point error) that should be subtracted from raw readings.
 * Scales represent the gain correction to normalize the sensor output.
 */
@Parcelize
data class SensorCalibrationData(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f
) : Parcelable {

    /** Returns true if any offset or scale is non-default */
    fun isCalibrated(): Boolean {
        return offsetX != 0f || offsetY != 0f || offsetZ != 0f ||
                scaleX != 1f || scaleY != 1f || scaleZ != 1f
    }

    /**
     * Apply calibration data to a raw sensor value.
     * Formula: calibrated = (raw - offset) * scale
     */
    fun applyCalibration(rawX: Float, rawY: Float, rawZ: Float): Triple<Float, Float, Float> {
        return Triple(
            (rawX - offsetX) * scaleX,
            (rawY - offsetY) * scaleY,
            (rawZ - offsetZ) * scaleZ
        )
    }

    /**
     * Apply calibration to a single axis value.
     */
    fun applyToAxis(rawValue: Float, axis: String): Float {
        return when (axis.lowercase()) {
            "x" -> (rawValue - offsetX) * scaleX
            "y" -> (rawValue - offsetY) * scaleY
            "z" -> (rawValue - offsetZ) * scaleZ
            else -> rawValue
        }
    }

    /**
     * Reverse calibration (convert calibrated back to raw).
     */
    fun reverseCalibration(calX: Float, calY: Float, calZ: Float): Triple<Float, Float, Float> {
        return Triple(
            calX / scaleX + offsetX,
            calY / scaleY + offsetY,
            calZ / scaleZ + offsetZ
        )
    }

    /** Convert to a Triple for convenience */
    fun toTriple(): Triple<Float, Float, Float> {
        return Triple(offsetX, offsetY, offsetZ)
    }

    /**
     * Get the offsets as a Triple.
     */
    fun getOffsets(): Triple<Float, Float, Float> {
        return Triple(offsetX, offsetY, offsetZ)
    }

    /**
     * Get the scales as a Triple.
     */
    fun getScales(): Triple<Float, Float, Float> {
        return Triple(scaleX, scaleY, scaleZ)
    }

    /**
     * Get a formatted summary string.
     */
    fun getFormattedSummary(): String {
        return buildString {
            appendLine("Sensor Calibration")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Offsets: X=${"%.4f".format(offsetX)}, Y=${"%.4f".format(offsetY)}, Z=${"%.4f".format(offsetZ)}")
            appendLine("Scales:  X=${"%.4f".format(scaleX)}, Y=${"%.4f".format(scaleY)}, Z=${"%.4f".format(scaleZ)}")
            appendLine("Status: ${if (isCalibrated()) "✅ Calibrated" else "❌ Default"}")
        }
    }

    companion object {
        /** Identity calibration (no offset, scale = 1) */
        fun identity(): SensorCalibrationData = SensorCalibrationData()

        /**
         * Create calibration from offsets only (scales set to 1).
         */
        fun fromOffsets(offsetX: Float, offsetY: Float, offsetZ: Float): SensorCalibrationData {
            return SensorCalibrationData(
                offsetX = offsetX,
                offsetY = offsetY,
                offsetZ = offsetZ,
                scaleX = 1f,
                scaleY = 1f,
                scaleZ = 1f
            )
        }

        /**
         * Create calibration from scales only (offsets set to 0).
         */
        fun fromScales(scaleX: Float, scaleY: Float, scaleZ: Float): SensorCalibrationData {
            return SensorCalibrationData(
                offsetX = 0f,
                offsetY = 0f,
                offsetZ = 0f,
                scaleX = scaleX,
                scaleY = scaleY,
                scaleZ = scaleZ
            )
        }

        /**
         * Create calibration from a Triple of offsets.
         */
        fun fromOffsetsTriple(offsets: Triple<Float, Float, Float>): SensorCalibrationData {
            return SensorCalibrationData(
                offsetX = offsets.first,
                offsetY = offsets.second,
                offsetZ = offsets.third
            )
        }

        /**
         * Create calibration from a Triple of scales.
         */
        fun fromScalesTriple(scales: Triple<Float, Float, Float>): SensorCalibrationData {
            return SensorCalibrationData(
                scaleX = scales.first,
                scaleY = scales.second,
                scaleZ = scales.third
            )
        }
    }
}