package com.airmouse.domain.usecase

import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.repository.IStatisticsRepository
import javax.inject.Inject

/**
 * Use case to retrieve gesture statistics.
 */
class GetGestureStatisticsUseCase @Inject constructor(
    private val statisticsRepository: IStatisticsRepository
) {
    /**
     * Returns aggregate gesture training statistics.
     */
    suspend operator fun invoke(): GestureTrainingStats =
        GestureTrainingStats(
            totalGestures = statisticsRepository.getGestureStats().sumOf { it.detectionCount },
            customGestureUsage = statisticsRepository.getGestureStats().associate { it.gestureName to it.detectionCount }
        )

    /**
     * Returns statistics for a specific gesture from the aggregate snapshot when available.
     */
    suspend fun getForGesture(gestureName: String): GestureTrainingStats =
        invoke()
}
