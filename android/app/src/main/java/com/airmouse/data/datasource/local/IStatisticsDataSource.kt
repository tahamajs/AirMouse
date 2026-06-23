
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary

interface IStatisticsDataSource {

    
    suspend fun saveSessionStats(stats: StatisticsSummary)
    suspend fun getSessionStats(): StatisticsSummary

    
    suspend fun saveDailyStats(date: String, stats: DailyStats)
    suspend fun getDailyStats(date: String): DailyStats
    suspend fun getDailyStatsForRange(startDate: String, endDate: String): List<DailyStats>

    
    suspend fun saveHistoricalStats(stats: HistoricalStatistics)
    suspend fun getHistoricalStats(): HistoricalStatistics

    
    suspend fun saveGestureStats(stats: List<GestureStatistics>)
    suspend fun getGestureStats(): List<GestureStatistics>
    suspend fun incrementGestureCount(gesture: String)

    
    suspend fun resetSessionStats()
    suspend fun resetAllStats()
}