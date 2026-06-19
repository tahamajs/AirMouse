// app/src/main/java/com/airmouse/domain/usecase/SendMovementUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.MouseButton
import com.airmouse.domain.repository.IMouseRepository
import javax.inject.Inject

/**
 * Use case for sending mouse movements and clicks
 */
class SendMovementUseCase @Inject constructor(
    private val mouseRepository: IMouseRepository
) {

    /**
     * Send cursor movement
     */
    suspend operator fun invoke(dx: Float, dy: Float): Result<Boolean> {
        return try {
            val result = mouseRepository.move(dx, dy)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send smooth cursor movement
     */
    suspend fun sendSmoothMovement(points: List<Pair<Float, Float>>, durationMs: Int = 100): Result<Boolean> {
        return try {
            val result = mouseRepository.moveSmooth(points, durationMs)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send click
     */
    suspend fun sendClick(button: MouseButton = MouseButton.LEFT): Result<Boolean> {
        return try {
            val result = mouseRepository.click(button)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send double click
     */
    suspend fun sendDoubleClick(): Result<Boolean> {
        return try {
            val result = mouseRepository.doubleClick()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send right click
     */
    suspend fun sendRightClick(): Result<Boolean> {
        return try {
            val result = mouseRepository.rightClick()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send scroll
     */
    suspend fun sendScroll(delta: Int): Result<Boolean> {
        return try {
            val result = mouseRepository.scroll(delta)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send gesture
     */
    suspend fun sendGesture(gesture: String, confidence: Float): Result<Boolean> {
        return try {
            val result = mouseRepository.sendGesture(gesture, confidence)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pause movement
     */
    suspend fun pauseMovement(): Result<Boolean> {
        return try {
            mouseRepository.stopMovement()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume movement
     */
    suspend fun resumeMovement(): Result<Boolean> {
        return try {
            mouseRepository.resumeMovement()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}