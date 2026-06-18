package com.airmouse.domain.model

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize

/**
 * Proximity state based on Bluetooth RSSI.
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
    companion object {
        val UNKNOWN = ProximityState(false, 5.0f, -100, "", null, 0, 0f)
    }
}

/**
 * Proximity calibration status.
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
    
    val qualityScore: Int get() = (accuracy * 100).toInt()
    
    val qualityColor: Long get() = when {
        qualityScore >= 80 -> 0xFF4CAF50
        qualityScore >= 60 -> 0xFFFFC107
        else -> 0xFFF44336
    }
    
    val confidenceDescription: String get() = when {
        qualityScore >= 80 -> "High"
        qualityScore >= 60 -> "Medium"
        else -> "Low"
    }

    fun calculateDistance(rssi: Int): Float {
        return Math.pow(10.0, (referenceRssi - rssi) / (10.0 * pathLossExponent)).toFloat()
    }

    companion object {
        val DEFAULT = ProximityCalibration(false)
    }
}

/**
 * Detailed calibration status for UI.
 */
@Parcelize
data class ProximityCalibrationStatus(
    val isCalibrated: Boolean,
    val accuracy: Float,
    val nearThreshold: Float,
    val farThreshold: Float,
    val referenceRssi: Int,
    val pathLossExponent: Float,
    val qualityScore: Int,
    val qualityColor: Long,
    val confidenceDescription: String
) : Parcelable
