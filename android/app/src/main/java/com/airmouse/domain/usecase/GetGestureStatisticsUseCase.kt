package com.airmouse.domain.usecase

import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.repository.IGestureRepository
import javax.inject.Inject

/**
 * Use case to retrieve gesture statistics.
 */
class GetGestureStatisticsUseCase @Inject constructor(
    private val gestureRepository: IGestureRepository
) {
    /**
     * Returns aggregate gesture training statistics.
     */
    suspend operator fun invoke(): GestureTrainingStats =
        gestureRepository.getGestureStats()

    /**
     * Returns statistics for a specific gesture from the aggregate snapshot when available.
     */
    suspend fun getForGesture(gestureName: String): GestureTrainingStats =
        gestureRepository.getGestureStats()
}
