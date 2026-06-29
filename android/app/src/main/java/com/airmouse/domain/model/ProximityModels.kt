package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.pow

/**
 * Current proximity state of a device.
 */
@Parcelize
data class ProximityState(
    val isNear: Boolean,
    val distance: Float,
    val signalStrength: Int,
    val deviceAddress: String,
    val deviceName: String? = null,
    val lastUpdate: Long = System.currentTimeMillis(),
    val confidence: Float = 0.8f
) : Parcelable {

    /**
     * Check if the state is valid (recent and confident).
     */
    fun isValid(maxAgeMs: Long = 5000L): Boolean {
        return lastUpdate > 0 &&
                (System.currentTimeMillis() - lastUpdate) < maxAgeMs &&
                confidence >= 0.5f
    }

    /**
     * Get a human-readable distance description.
     */
    fun getDistanceDescription(): String {
        return when {
            distance < 0.5f -> "Very Close"
            distance < 1.5f -> "Close"
            distance < 3.0f -> "Medium"
            distance < 5.0f -> "Far"
            else -> "Very Far"
        }
    }

    /**
     * Get a human-readable signal strength description.
     */
    fun getSignalDescription(): String {
        return when {
            signalStrength > -50 -> "Excellent 📶"
            signalStrength > -60 -> "Good 📶"
            signalStrength > -70 -> "Fair 📶"
            signalStrength > -80 -> "Poor 📶"
            else -> "Very Poor 📶"
        }
    }

    companion object {
        /**
         * Unknown proximity state (default).
         */
        val UNKNOWN = ProximityState(
            isNear = false,
            distance = 5.0f,
            signalStrength = -100,
            deviceAddress = "",
            deviceName = null,
            lastUpdate = 0,
            confidence = 0f
        )

        /**
         * Create a near state.
         */
        fun near(address: String, name: String? = null): ProximityState {
            return ProximityState(
                isNear = true,
                distance = 0.5f,
                signalStrength = -50,
                deviceAddress = address,
                deviceName = name,
                confidence = 0.9f
            )
        }

        /**
         * Create a far state.
         */
        fun far(address: String, name: String? = null): ProximityState {
            return ProximityState(
                isNear = false,
                distance = 5.0f,
                signalStrength = -80,
                deviceAddress = address,
                deviceName = name,
                confidence = 0.7f
            )
        }
    }
}

/**
 * Configuration for proximity detection.
 */
@Parcelize
data class ProximityConfig(
    val enabled: Boolean = false,
    val nearThreshold: Float = 1.5f,
    val farThreshold: Float = 3.0f,
    val scanInterval: Long = 1000L,
    val vibrationEnabled: Boolean = true,
    val autoLockEnabled: Boolean = true,
    val autoUnlockEnabled: Boolean = true,
    val deviceAddress: String = ""
) : Parcelable {

    /**
     * Check if the config is valid and ready for use.
     */
    fun isValid(): Boolean {
        return enabled &&
                nearThreshold > 0.3f &&
                farThreshold > nearThreshold &&
                scanInterval >= 200L &&
                deviceAddress.isNotBlank()
    }

    /**
     * Create a copy with a specific device address.
     */
    fun withDevice(address: String): ProximityConfig {
        return copy(deviceAddress = address)
    }

    /**
     * Create a copy enabling/disabling auto-lock.
     */
    fun withAutoLock(enabled: Boolean): ProximityConfig {
        return copy(autoLockEnabled = enabled)
    }

    /**
     * Create a copy with updated thresholds.
     */
    fun withThresholds(near: Float, far: Float): ProximityConfig {
        return copy(
            nearThreshold = near.coerceAtLeast(0.3f),
            farThreshold = far.coerceAtLeast(near)
        )
    }

    companion object {
        /**
         * Default proximity configuration.
         */
        fun default(): ProximityConfig {
            return ProximityConfig()
        }

        /**
         * Configuration for close-range proximity (1-2 meters).
         */
        fun closeRange(): ProximityConfig {
            return ProximityConfig(
                nearThreshold = 0.8f,
                farThreshold = 2.0f,
                scanInterval = 500L
            )
        }

        /**
         * Configuration for long-range proximity (3-5 meters).
         */
        fun longRange(): ProximityConfig {
            return ProximityConfig(
                nearThreshold = 2.0f,
                farThreshold = 4.5f,
                scanInterval = 1500L
            )
        }

        /**
         * Battery-friendly configuration.
         */
        fun batterySaver(): ProximityConfig {
            return ProximityConfig(
                nearThreshold = 1.5f,
                farThreshold = 3.0f,
                scanInterval = 3000L,
                vibrationEnabled = false
            )
        }
    }
}

/**
 * Calibration data for proximity detection (RSSI-based distance estimation).
 */
@Parcelize
data class ProximityCalibration(
    val isCalibrated: Boolean,
    val referenceRssi: Int = -59,
    val pathLossExponent: Float = 2.5f,
    val nearThreshold: Float = 1.5f,
    val farThreshold: Float = 3.0f,
    val accuracy: Float = 0.7f,
    val calibrationTime: Long = 0
) : Parcelable {

    /**
     * Quality score as a percentage (0-100).
     */
    val qualityScore: Int get() = (accuracy * 100).toInt()

    /**
     * Color representation of the quality.
     */
    val qualityColor: Long get() = when {
        qualityScore >= 80 -> 0xFF4CAF50  // Green
        qualityScore >= 60 -> 0xFFFFC107  // Yellow
        else -> 0xFFF44336                // Red
    }

    /**
     * Human-readable confidence description.
     */
    val confidenceDescription: String get() = when {
        qualityScore >= 80 -> "High"
        qualityScore >= 60 -> "Medium"
        else -> "Low"
    }

    /**
     * Calculate distance from RSSI using the path loss model.
     * d = 10^((refRssi - rssi) / (10 * n))
     */
    fun calculateDistance(rssi: Int): Float {
        if (rssi >= 0) return 0.3f
        return (10.0.pow((referenceRssi - rssi) / (10.0 * pathLossExponent))).toFloat()
    }

    /**
     * Calculate RSSI from a given distance (inverse of calculateDistance).
     */
    fun calculateRssi(distance: Float): Int {
        if (distance <= 0) return referenceRssi
        return referenceRssi - (10.0 * pathLossExponent * Math.log10(distance.toDouble())).toInt()
    }

    /**
     * Get a formatted summary string.
     */
    fun getFormattedSummary(): String {
        return buildString {
            appendLine("Proximity Calibration")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Status: ${if (isCalibrated) "✅ Calibrated" else "❌ Not Calibrated"}")
            appendLine("Reference RSSI: $referenceRssi dBm")
            appendLine("Path Loss Exponent: ${"%.1f".format(pathLossExponent)}")
            appendLine("Near Threshold: ${"%.1f".format(nearThreshold)}m")
            appendLine("Far Threshold: ${"%.1f".format(farThreshold)}m")
            appendLine("Accuracy: ${"%.0f".format(accuracy * 100)}%")
            appendLine("Quality: $confidenceDescription")
            appendLine("Calibration: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(calibrationTime)}")
        }
    }

    companion object {
        /**
         * Default uncalibrated state.
         */
        val DEFAULT = ProximityCalibration(
            isCalibrated = false,
            referenceRssi = -59,
            pathLossExponent = 2.5f,
            nearThreshold = 1.5f,
            farThreshold = 3.0f,
            accuracy = 0.7f,
            calibrationTime = 0
        )

        /**
         * Create a new calibration.
         */
        fun create(
            referenceRssi: Int,
            pathLossExponent: Float,
            nearThreshold: Float,
            farThreshold: Float,
            accuracy: Float
        ): ProximityCalibration {
            return ProximityCalibration(
                isCalibrated = true,
                referenceRssi = referenceRssi,
                pathLossExponent = pathLossExponent,
                nearThreshold = nearThreshold,
                farThreshold = farThreshold,
                accuracy = accuracy.coerceIn(0.5f, 0.99f),
                calibrationTime = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Calibration status for proximity sensors.
 */
enum class ProximityCalibrationStatus {
    NOT_CALIBRATED,
    CALIBRATED
}