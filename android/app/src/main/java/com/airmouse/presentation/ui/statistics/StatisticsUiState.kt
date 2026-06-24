package com.airmouse.presentation.ui.statistics

import com.airmouse.domain.model.AppPreferences
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.StatisticsSummary
import java.util.Date

/**
 * UI state for the Statistics screen.
 */
data class StatisticsUiState(
    // Session stats
    val sessionTime: Long = 0L,
    val sessionStartTime: Long = System.currentTimeMillis(),
    val lastActivityTime: Long = System.currentTimeMillis(),

    // Click stats
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,

    // Gesture stats
    val gesturesDetected: Int = 0,
    val customGesturesUsed: Int = 0,
    val mostUsedGesture: String = "",
    val mostUsedGestureCount: Int = 0,
    val gestureTypeCount: Int = 0,
    val customGestureCount: Int = 0,
    val gestureBreakdown: Map<String, Int> = emptyMap(),

    // Movement stats
    val totalDistanceMoved: Float = 0f,
    val averageSpeed: Float = 0f,
    val peakSpeed: Float = 0f,
    val totalMovements: Int = 0,

    // Connection stats
    val connectionAttempts: Int = 0,
    val successfulConnections: Int = 0,
    val failedConnections: Int = 0,
    val averagePing: Int = 0,

    // Calibration stats
    val calibrationComplete: Boolean = false,
    val lastCalibrationTime: Long = 0L,
    val calibrationCount: Int = 0,
    val calibrationSuccessRate: Float = 0f,

    // System stats
    val isTracking: Boolean = false,
    val touchpadActive: Boolean = false,
    val presentationModeEnabled: Boolean = false,
    val autoConnect: Boolean = true,
    val useWebSocket: Boolean = true,
    val useUdpDiscovery: Boolean = true,
    val theme: String = "system",
    val language: String = "en",
    val batteryUsage: Int = 0,
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val temperature: Float = 0f,

    // Time range and chart
    val timeRange: TimeRange = TimeRange.WEEK,
    val chartType: ChartType = ChartType.CLICK_HISTORY,

    // Daily stats
    val dailyStats: List<DailyStats> = emptyList(),

    // Domain models
    val summaryStats: StatisticsSummary = StatisticsSummary(),
    val mouseStatistics: MouseStatistics = MouseStatistics(),
    val appPreferences: AppPreferences = AppPreferences(),

    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val showExportDialog: Boolean = false,
    val showResetDialog: Boolean = false
)

/**
 * Time range for statistics.
 */
enum class TimeRange {
    TODAY,
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

/**
 * Chart types for statistics display.
 */
enum class ChartType {
    CLICK_HISTORY,
    GESTURE_BREAKDOWN,
    MOVEMENT_TRENDS,
    CONNECTION_QUALITY,
    PERFORMANCE_METRICS
}

/**
 * Export formats for statistics.
 */
enum class ExportFormat {
    JSON,
    CSV,
    PDF
}

/**
 * Daily statistics data point.
 */
data class DailyStats(
    val date: Date,
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val gestures: Int = 0,
    val movements: Int = 0,
    val distance: Float = 0f
)

/**
 * Statistics summary for export.
 */
data class StatisticsExportData(
    val version: String = "1.0",
    val exportDate: Long = System.currentTimeMillis(),
    val summary: StatisticsSummary,
    val dailyStats: List<DailyStats>,
    val gestureBreakdown: Map<String, Int>,
    val systemInfo: Map<String, Any>
)