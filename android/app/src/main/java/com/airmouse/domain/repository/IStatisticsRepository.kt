// app/src/main/java/com/airmouse/domain/repository/IStatisticsRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary
import kotlinx.coroutines.flow.Flow

interface IStatisticsRepository {
    // Recording
    suspend fun recordClick()
    suspend fun recordDoubleClick()
    suspend fun recordRightClick()
    suspend fun recordScroll(delta: Int)
    suspend fun recordMovement(distance: Float, duration: Long)
    suspend fun recordGesture(gesture: String, confidence: Float)
    suspend fun recordConnectionAttempt(success: Boolean, latencyMs: Long)

    // Session
    suspend fun getCurrentSession(): StatisticsSummary
    fun observeCurrentSession(): Flow<StatisticsSummary>
    suspend fun startTracking()
    suspend fun stopTracking()
    suspend fun isTracking(): Boolean

    // Historical
    suspend fun getDailyStats(date: String): DailyStats
    suspend fun getHistoricalStats(): HistoricalStatistics
    fun observeHistoricalStats(): Flow<HistoricalStatistics>
    suspend fun getTodayStats(): DailyStats
    suspend fun getWeekStats(): List<DailyStats>
    suspend fun getMonthStats(): List<DailyStats>

    // Gesture stats
    suspend fun getGestureStats(): List<GestureStatistics>
    fun observeGestureStats(): Flow<List<GestureStatistics>>

    // Reset
    suspend fun resetStats()
    suspend fun resetSession()

    // Export
    suspend fun exportStats(format: String): String
}