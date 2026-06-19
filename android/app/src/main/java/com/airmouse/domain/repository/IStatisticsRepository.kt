// app/src/main/java/com/airmouse/domain/repository/IStatisticsRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for statistics operations
 */
interface IStatisticsRepository {

    // ==================== Recording ====================

    /**
     * Record a single click
     */
    suspend fun recordClick()

    /**
     * Record a double click
     */
    suspend fun recordDoubleClick()

    /**
     * Record a right click
     */
    suspend fun recordRightClick()

    /**
     * Record a scroll action
     * @param delta Scroll amount (positive = up, negative = down)
     */
    suspend fun recordScroll(delta: Int)

    /**
     * Record a mouse movement
     * @param distance Distance moved in pixels
     * @param duration Duration of the movement in milliseconds
     */
    suspend fun recordMovement(distance: Float, duration: Long)

    /**
     * Record a gesture detection
     * @param gesture Name of the gesture
     * @param confidence Confidence level (0.0 to 1.0)
     */
    suspend fun recordGesture(gesture: String, confidence: Float)

    /**
     * Record a connection attempt
     * @param success Whether the connection was successful
     * @param latencyMs Connection latency in milliseconds
     */
    suspend fun recordConnectionAttempt(success: Boolean, latencyMs: Long)

    // ==================== Session ====================

    /**
     * Get current session statistics
     */
    suspend fun getCurrentSession(): StatisticsSummary

    /**
     * Observe current session statistics as a Flow
     */
    fun observeCurrentSession(): Flow<StatisticsSummary>

    /**
     * Start tracking statistics for a new session
     */
    suspend fun startTracking()

    /**
     * Stop tracking statistics for the current session
     */
    suspend fun stopTracking()

    /**
     * Check if tracking is currently active
     */
    suspend fun isTracking(): Boolean

    // ==================== Historical ====================

    /**
     * Get statistics for a specific date
     * @param date Date in "yyyy-MM-dd" format
     */
    suspend fun getDailyStats(date: String): DailyStats

    /**
     * Get statistics for today
     */
    suspend fun getTodayStats(): DailyStats

    /**
     * Get statistics for the last 7 days
     */
    suspend fun getWeekStats(): List<DailyStats>

    /**
     * Get statistics for the last 30 days
     */
    suspend fun getMonthStats(): List<DailyStats>

    /**
     * Get aggregated historical statistics
     */
    suspend fun getHistoricalStats(): HistoricalStatistics

    /**
     * Observe historical statistics as a Flow
     */
    fun observeHistoricalStats(): Flow<HistoricalStatistics>

    // ==================== Gesture Statistics ====================

    /**
     * Get statistics for all gestures
     */
    suspend fun getGestureStats(): List<GestureStatistics>

    /**
     * Observe gesture statistics as a Flow
     */
    fun observeGestureStats(): Flow<List<GestureStatistics>>

    // ==================== Reset ====================

    /**
     * Reset all statistics (session + historical + gesture)
     */
    suspend fun resetStats()

    /**
     * Reset only the current session statistics
     */
    suspend fun resetSession()

    // ==================== Export ====================

    /**
     * Export statistics in the specified format
     * @param format Format type ("json" or "csv")
     * @return Exported statistics as a string
     */
    suspend fun exportStats(format: String): String
}