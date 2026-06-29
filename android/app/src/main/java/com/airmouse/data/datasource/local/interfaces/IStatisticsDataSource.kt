package com.airmouse.data.datasource.local

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary

/**
 * Data source for all statistical operations.
 * Combines session, daily, historical, and gesture statistics.
 */
interface IStatisticsDataSource {

    // ============================================================
    // Session Stats (used by DataSyncManager)
    // ============================================================

    /** Returns the current session statistics. */
    suspend fun getStatistics(): StatisticsSummary?

    /** Saves the current session statistics. */
    suspend fun saveStatistics(stats: StatisticsSummary)

    // ============================================================
    // Extended Session Stats
    // ============================================================

    /** Saves the session stats (same as saveStatistics, but with more descriptive name). */
    suspend fun saveSessionStats(stats: StatisticsSummary)

    /** Retrieves the current session stats. */
    suspend fun getSessionStats(): StatisticsSummary

    // ============================================================
    // Daily Stats
    // ============================================================

    /** Saves stats for a specific date. */
    suspend fun saveDailyStats(date: String, stats: DailyStats)

    /** Retrieves stats for a specific date. */
    suspend fun getDailyStats(date: String): DailyStats

    /** Retrieves stats for a date range. */
    suspend fun getDailyStatsForRange(startDate: String, endDate: String): List<DailyStats>

    // ============================================================
    // Historical Stats (aggregated)
    // ============================================================

    /** Saves historical statistics. */
    suspend fun saveHistoricalStats(stats: HistoricalStatistics)

    /** Retrieves historical statistics. */
    suspend fun getHistoricalStats(): HistoricalStatistics

    // ============================================================
    // Gesture Stats
    // ============================================================

    /** Saves a list of gesture statistics. */
    suspend fun saveGestureStats(stats: List<GestureStatistics>)

    /** Returns a list of all gesture statistics. */
    suspend fun getGestureStats(): List<GestureStatistics>

    /** Increments the count for a specific gesture. */
    suspend fun incrementGestureCount(gesture: String)

    // ============================================================
    // Reset Operations
    // ============================================================

    /** Resets the current session statistics. */
    suspend fun resetSessionStats()

    /** Resets all statistics (session, daily, historical, gesture). */
    suspend fun resetAllStats()
}
