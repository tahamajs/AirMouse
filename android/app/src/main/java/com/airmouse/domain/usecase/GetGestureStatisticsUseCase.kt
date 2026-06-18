package com.airmouse.domain.usecase

import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.repository.IGestureRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve gesture statistics.
 */
class GetGestureStatisticsUseCase @Inject constructor(
    private val gestureRepository: IGestureRepository
) {
    /**
     * Returns a flow of gesture statistics for all gestures.
     */
    operator fun invoke(): Flow<List<GestureStatistics>> =
        gestureRepository.getGestureStatistics()

    /**
     * Returns statistics for a specific gesture.
     */
    suspend fun getForGesture(gestureName: String): GestureStatistics? =
        gestureRepository.getGestureStatistics(gestureName)
}