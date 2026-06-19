// app/src/main/java/com/airmouse/domain/usecase/RecordStatisticsUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.repository.IStatisticsRepository
import javax.inject.Inject

/**
 * Use case for recording statistics
 */
class RecordStatisticsUseCase @Inject constructor(
    private val statisticsRepository: IStatisticsRepository
) {

    /**
     * Record a click
     */
    suspend operator fun invoke(type: String, data: Any? = null): Result<Unit> {
        return try {
            when (type) {
                "click" -> statisticsRepository.recordClick()
                "double_click" -> statisticsRepository.recordDoubleClick()
                "right_click" -> statisticsRepository.recordRightClick()
                "scroll" -> {
                    val delta = (data as? Int) ?: 0
                    statisticsRepository.recordScroll(delta)
                }
                "movement" -> {
                    val distance = (data as? Float) ?: 0f
                    val duration = (data as? Long) ?: 0L
                    statisticsRepository.recordMovement(distance, duration)
                }
                "gesture" -> {
                    val gesture = data as? String ?: ""
                    statisticsRepository.recordGesture(gesture, 0.9f)
                }
                else -> return Result.failure(Exception("Unknown statistic type"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record click
     */
    suspend fun recordClick(): Result<Unit> {
        return try {
            statisticsRepository.recordClick()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record double click
     */
    suspend fun recordDoubleClick(): Result<Unit> {
        return try {
            statisticsRepository.recordDoubleClick()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record right click
     */
    suspend fun recordRightClick(): Result<Unit> {
        return try {
            statisticsRepository.recordRightClick()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record scroll
     */
    suspend fun recordScroll(delta: Int): Result<Unit> {
        return try {
            statisticsRepository.recordScroll(delta)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record movement
     */
    suspend fun recordMovement(distance: Float, duration: Long): Result<Unit> {
        return try {
            statisticsRepository.recordMovement(distance, duration)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record gesture
     */
    suspend fun recordGesture(gesture: String, confidence: Float): Result<Unit> {
        return try {
            statisticsRepository.recordGesture(gesture, confidence)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}