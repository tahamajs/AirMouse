
package com.airmouse.domain.usecase

import com.airmouse.domain.model.GestureEvent
import com.airmouse.domain.model.GestureType
import com.airmouse.domain.repository.IGestureRepository
import javax.inject.Inject

class DetectGestureUseCase @Inject constructor(
    private val gestureRepository: IGestureRepository
) {
    private var lastGesture: GestureEvent? = null

    suspend operator fun invoke(sensorData: FloatArray): Result<GestureEvent> {
        return try {
            val result = gestureRepository.detectGesture(sensorData)
            lastGesture = result
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun detectFromMotion(dx: Float, dy: Float): GestureType {
        return gestureRepository.detectGestureFromMotion(dx, dy)
    }

    suspend fun getCurrentGesture(): GestureEvent? {
        return lastGesture
    }

    suspend fun isGestureRecognized(): Boolean {
        return gestureRepository.getGestureStats().totalGestures > 0
    }
}
