
package com.airmouse.domain.usecase

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.domain.repository.IStatisticsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val statisticsRepository: IStatisticsRepository
) {

    suspend operator fun invoke(): StatisticsSummary {
        return statisticsRepository.getCurrentSession()
    }

    fun observeSessionStats(): Flow<StatisticsSummary> {
        return statisticsRepository.observeCurrentSession()
    }

    suspend fun getSessionStats(): StatisticsSummary {
        return statisticsRepository.getCurrentSession()
    }

    suspend fun getHistoricalStats(): HistoricalStatistics {
        return statisticsRepository.getHistoricalStats()
    }

    suspend fun getTodayStats(): DailyStats {
        return statisticsRepository.getTodayStats()
    }

    suspend fun getWeekStats(): List<DailyStats> {
        return statisticsRepository.getWeekStats()
    }

    suspend fun getMonthStats(): List<DailyStats> {
        return statisticsRepository.getMonthStats()
    }

    suspend fun getGestureStats(): List<GestureStatistics> {
        return statisticsRepository.getGestureStats()
    }

    suspend fun isTracking(): Boolean {
        return statisticsRepository.isTracking()
    }

    fun observeGestureStats(): Flow<List<GestureStatistics>> {
        return statisticsRepository.observeGestureStats()
    }

    suspend fun startTracking(): Result<Unit> {
        return try {
            statisticsRepository.startTracking()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopTracking(): Result<Unit> {
        return try {
            statisticsRepository.stopTracking()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetStats(): Result<Unit> {
        return try {
            statisticsRepository.resetStats()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportStats(format: String = "json"): Result<String> {
        return try {
            val result = statisticsRepository.exportStats(format)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
