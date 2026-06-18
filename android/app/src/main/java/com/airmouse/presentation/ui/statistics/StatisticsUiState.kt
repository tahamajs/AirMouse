package com.airmouse.presentation.ui.statistics

import java.util.Date

/**
 * Complete UI state for the statistics screen.
 */
data class StatisticsUiState(
    // Session
    val sessionTime: Long = 0,                    // seconds
    val sessionStartTime: Long = System.currentTimeMillis(),
    val lastActivityTime: Long = System.currentTimeMillis(),

    // Gesture Stats
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val gesturesDetected: Int = 0,
    val customGesturesUsed: Int = 0,

    // Movement Stats
    val totalDistanceMoved: Float = 0f,
    val averageSpeed: Float = 0f,
    val peakSpeed: Float = 0f,
    val totalMovements: Int = 0,

    // Connection Stats
    val connectionAttempts: Int = 0,
    val successfulConnections: Int = 0,
    val failedConnections: Int = 0,
    val averagePing: Int = 0,

    // Calibration Stats
    val lastCalibrationTime: Long = 0,
    val calibrationCount: Int = 0,
    val calibrationSuccessRate: Float = 0f,

    // Device / Performance
    val batteryUsage: Int = 0,
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val temperature: Float = 0f,

    // History & Breakdown
    val dailyStats: List<DailyStats> = emptyList(),
    val gestureBreakdown: Map<String, Int> = emptyMap(),

    // UI State
    val isLoading: Boolean = false,
    val timeRange: TimeRange = TimeRange.LAST_30_DAYS,
    val selectedChart: ChartType = ChartType.GESTURES,
    val showExportDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

/**
 * Daily aggregated statistics for a single day.
 */
data class DailyStats(
    val date: Date,
    val clicks: Int,
    val doubleClicks: Int,
    val rightClicks: Int,
    val scrolls: Int,
    val distance: Float
)

/**
 * Time range filter options.
 */
enum class TimeRange(val displayName: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("This Week", 7),
    MONTH("This Month", 30),
    YEAR("This Year", 365),
    ALL_TIME("All Time", 0)
}

/**
 * Chart types for the statistics screen.
 */
enum class ChartType(val displayName: String) {
    GESTURES("Gesture Distribution"),
    MOVEMENT("Movement Over Time"),
    CONNECTION("Connection History"),
    PERFORMANCE("Performance Metrics")
}