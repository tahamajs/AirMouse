package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A single training sample for gesture recognition.
 */
@Parcelize
data class TrainingSample(
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val label: String = "",
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
) : Parcelable {

    fun toFloatArray(): FloatArray {
        return floatArrayOf(gyroX, gyroY, gyroZ, accelX, accelY, accelZ, magX, magY, magZ)
    }

    companion object {
        fun fromFloatArray(data: FloatArray, label: String = ""): TrainingSample {
            return TrainingSample(
                gyroX = data.getOrNull(0) ?: 0f,
                gyroY = data.getOrNull(1) ?: 0f,
                gyroZ = data.getOrNull(2) ?: 0f,
                accelX = data.getOrNull(3) ?: 0f,
                accelY = data.getOrNull(4) ?: 0f,
                accelZ = data.getOrNull(5) ?: 0f,
                magX = data.getOrNull(6) ?: 0f,
                magY = data.getOrNull(7) ?: 0f,
                magZ = data.getOrNull(8) ?: 0f,
                label = label
            )
        }
    }
}