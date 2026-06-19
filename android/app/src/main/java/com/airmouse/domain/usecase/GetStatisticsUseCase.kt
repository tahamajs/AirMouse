// app/src/main/java/com/airmouse/domain/usecase/GetStatisticsUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.domain.repository.IStatisticsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting statistics
 */
class GetStatisticsUseCase @Inject constructor(
    private val statisticsRepository: IStatisticsRepository
) {

    /**
     * Get session statistics
     */
    suspend operator fun invoke(): StatisticsSummary {
        return statisticsRepository.getCurrentSession()
    }

    /**
     * Observe session statistics
     */
    fun observeSessionStats(): Flow<StatisticsSummary> {
        return statisticsRepository.observeCurrentSession()
    }

    /**
     * Get historical statistics
     */
    suspend fun getHistoricalStats(): HistoricalStatistics {
        return statisticsRepository.getHistoricalStats()
    }

    /**
     * Get today's statistics
     */
    suspend fun getTodayStats(): DailyStats {
        return statisticsRepository.getTodayStats()
    }

    /**
     * Get week statistics
     */
    suspend fun getWeekStats(): List<DailyStats> {
        return statisticsRepository.getWeekStats()
    }

    /**
     * Get month statistics
     */
    suspend fun getMonthStats(): List<DailyStats> {
        return statisticsRepository.getMonthStats()
    }

    /**
     * Get gesture statistics
     */
    suspend fun getGestureStats(): List<GestureStatistics> {
        return statisticsRepository.getGestureStats()
    }

    /**
     * Observe gesture statistics
     */
    fun observeGestureStats(): Flow<List<GestureStatistics>> {
        return statisticsRepository.observeGestureStats()
    }

    /**
     * Start tracking
     */
    suspend fun startTracking(): Result<Unit> {
        return try {
            statisticsRepository.startTracking()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop tracking
     */
    suspend fun stopTracking(): Result<Unit> {
        return try {
            statisticsRepository.stopTracking()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset statistics
     */
    suspend fun resetStats(): Result<Unit> {
        return try {
            statisticsRepository.resetStats()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export statistics
     */
    suspend fun exportStats(format: String = "json"): Result<String> {
        return try {
            val result = statisticsRepository.exportStats(format)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}