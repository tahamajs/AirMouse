package com.airmouse.domain.repository

import kotlinx.coroutines.flow.Flow

interface IStatisticsRepository {
    suspend fun incrementClickCount()
    suspend fun incrementDoubleClickCount()
    suspend fun incrementRightClickCount()
    suspend fun incrementScrollCount()
    suspend fun incrementGestureCount(gestureName: String)
    suspend fun recordMovement(distance: Float, duration: Long)
    suspend fun getStatistics(timeRange: TimeRange): Flow<AppStatistics>
    suspend fun resetStatistics()
    suspend fun exportStatistics(): String
}

data class AppStatistics(
    val clicks: Int,
    val doubleClicks: Int,
    val rightClicks: Int,
    val scrolls: Int,
    val gestures: Map<String, Int>,
    val totalDistance: Float,
    val averageSpeed: Float,
    val sessionTime: Long,
    val lastResetTime: Long
)

enum class TimeRange {
    TODAY, WEEK, MONTH, ALL_TIME
}