package com.airmouse.domain.usecase

import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.repository.IStatisticsRepository
import javax.inject.Inject

class GetGestureStatisticsUseCase @Inject constructor(
    private val statisticsRepository: IStatisticsRepository
) {
    suspend operator fun invoke(): GestureTrainingStats =
        GestureTrainingStats(
            totalGestures = statisticsRepository.getGestureStats().sumOf { it.detectionCount },
            customGestureUsage = statisticsRepository.getGestureStats().associate { it.gestureName to it.detectionCount }
        )

    suspend fun getForGesture(gestureName: String): GestureTrainingStats =
        statisticsRepository.getGestureStats()
            .firstOrNull { it.gestureName == gestureName }
            ?.let {
                GestureTrainingStats(
                    totalGestures = it.detectionCount,
                    gesturesByType = mapOf(com.airmouse.domain.model.GestureType.CUSTOM to it.detectionCount),
                    mostUsedGesture = it.gestureName,
                    lastGestureTime = it.lastDetected,
                    customGestureUsage = mapOf(it.gestureName to it.detectionCount),
                    averageConfidence = it.confidencePercentage
                )
            } ?: GestureTrainingStats()
}
