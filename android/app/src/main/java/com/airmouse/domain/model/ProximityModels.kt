package com.airmouse.domain.model

import android.os.Parcelable
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
) : Parcelable

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
) : Parcelable