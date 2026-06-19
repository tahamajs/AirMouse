// app/src/main/java/com/airmouse/domain/usecase/DetectGestureUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.GestureEvent
import com.airmouse.domain.model.GestureType
import com.airmouse.domain.repository.IGestureRepository
import javax.inject.Inject

/**
 * Use case for detecting gestures
 */
class DetectGestureUseCase @Inject constructor(
    private val gestureRepository: IGestureRepository
) {

    /**
     * Detect gesture from sensor data
     */
    suspend operator fun invoke(sensorData: FloatArray): Result<GestureEvent> {
        return try {
            val result = gestureRepository.detectGesture(sensorData)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Detect gesture from motion
     */
    suspend fun detectFromMotion(dx: Float, dy: Float): GestureType {
        return gestureRepository.detectGestureFromMotion(dx, dy)
    }

    /**
     * Get current gesture
     */
    suspend fun getCurrentGesture(): GestureEvent? {
        // In real implementation, would track current gesture
        return null
    }

    /**
     * Check if gesture is recognized
     */
    suspend fun isGestureRecognized(): Boolean {
        return gestureRepository.getGestureStats().totalGestures > 0
    }
}