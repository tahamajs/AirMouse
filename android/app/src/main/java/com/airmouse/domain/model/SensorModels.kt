package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Raw sensor data from device sensors.
 */
/**
 * Orientation data in both radians and degrees.
 */
@Parcelize
data class OrientationData(
    val roll: Float,
    val pitch: Float,
    val yaw: Float,
    val rollDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val yawDeg: Float = 0f
) : Parcelable {

    constructor(roll: Float, pitch: Float, yaw: Float) : this(
        roll = roll,
        pitch = pitch,
        yaw = yaw,
        rollDeg = Math.toDegrees(roll.toDouble()).toFloat(),
        pitchDeg = Math.toDegrees(pitch.toDouble()).toFloat(),
        yawDeg = Math.toDegrees(yaw.toDouble()).toFloat()
    )

    /**
     * Check if the device is flat (pitch and roll near zero).
     */
    fun isFlat(thresholdDeg: Float = 10f): Boolean {
        return abs(pitchDeg) < thresholdDeg && abs(rollDeg) < thresholdDeg
    }

    /**
     * Check if the device is upright (pitch near 90 degrees).
     */
    fun isUpright(thresholdDeg: Float = 15f): Boolean {
        return abs(pitchDeg - 90f) < thresholdDeg
    }

    /**
     * Get the orientation as a FloatArray.
     */
    fun toFloatArray(): FloatArray {
        return floatArrayOf(roll, pitch, yaw)
    }

    /**
     * Get the orientation in degrees as a FloatArray.
     */
    fun toDegreesArray(): FloatArray {
        return floatArrayOf(rollDeg, pitchDeg, yawDeg)
    }

    companion object {
        /**
         * Zero orientation.
         */
        fun zero(): OrientationData {
            return OrientationData(0f, 0f, 0f)
        }

        /**
         * Create orientation from degrees.
         */
        fun fromDegrees(rollDeg: Float, pitchDeg: Float, yawDeg: Float): OrientationData {
            return OrientationData(
                roll = Math.toRadians(rollDeg.toDouble()).toFloat(),
                pitch = Math.toRadians(pitchDeg.toDouble()).toFloat(),
                yaw = Math.toRadians(yawDeg.toDouble()).toFloat(),
                rollDeg = rollDeg,
                pitchDeg = pitchDeg,
                yawDeg = yawDeg
            )
        }
    }
}

/**
 * Calibration status for sensors.
 */
enum class SensorCalibrationStatus {
    NOT_CALIBRATED,
    CALIBRATING,
    CALIBRATED,
    NEEDS_RECALIBRATION
}

/**
 * Information about a sensor.
 */
data class SensorInfo(
    val name: String,
    val vendor: String,
    val version: Int,
    val maxRange: Float,
    val resolution: Float,
    val power: Float,
    val isAvailable: Boolean
) {
    /**
     * Get a formatted string for display.
     */
    fun getFormattedInfo(): String {
        return buildString {
            appendLine("Sensor: $name")
            appendLine("Vendor: $vendor")
            appendLine("Version: $version")
            appendLine("Max Range: $maxRange")
            appendLine("Resolution: $resolution")
            appendLine("Power: ${power}mA")
        }
    }
}

/**
 * Raw sensor event data.
 */
data class SensorEventData(
    val type: String,
    val values: FloatArray,
    val accuracy: Int,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorEventData

        if (type != other.type) return false
        if (!values.contentEquals(other.values)) return false
        if (accuracy != other.accuracy) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        result = 31 * result + timestamp.hashCode()
        return result
    }

    /**
     * Get the values as a List<Float>.
     */
    fun toList(): List<Float> {
        return values.toList()
    }

    /**
     * Get a specific value or default if index out of bounds.
     */
    fun getValue(index: Int, default: Float = 0f): Float {
        return values.getOrElse(index) { default }
    }

    companion object {
        /**
         * Create an empty SensorEventData.
         */
        fun empty(): SensorEventData {
            return SensorEventData(
                type = "none",
                values = floatArrayOf(),
                accuracy = 0,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
