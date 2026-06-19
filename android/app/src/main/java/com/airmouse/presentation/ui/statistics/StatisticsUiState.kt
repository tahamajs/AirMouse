// app/src/main/java/com/airmouse/presentation/ui/statistics/StatisticsUiState.kt
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
    val timeRange: TimeRange = TimeRange.ALL_TIME,
    val selectedChart: ChartType = ChartType.GESTURES,
    val showExportDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val error: String? = null,
    val success: String? = null
) {
    // Helper functions
    fun getTotalGestures(): Int = clicks + doubleClicks + rightClicks + scrolls + gesturesDetected

    fun getSuccessRate(): Float {
        return if (connectionAttempts > 0) {
            successfulConnections.toFloat() / connectionAttempts.toFloat() * 100f
        } else 0f
    }

    fun getCalibrationRate(): Float {
        return if (calibrationCount > 0) {
            calibrationSuccessRate / calibrationCount
        } else 0f
    }

    fun getAverageSpeedFormatted(): String {
        return String.format("%.1f", averageSpeed)
    }

    fun getDistanceFormatted(): String {
        return String.format("%.1f", totalDistanceMoved)
    }

    fun getSessionTimeFormatted(): String {
        val hours = sessionTime / 3600
        val minutes = (sessionTime % 3600) / 60
        val seconds = sessionTime % 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
}

/**
 * Daily aggregated statistics for a single day.
 */
data class DailyStats(
    val date: Date,
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val distance: Float = 0f,
    val gestures: Int = 0,
    val movements: Int = 0
) {
    val totalClicks: Int get() = clicks + doubleClicks + rightClicks
    val totalActions: Int get() = totalClicks + scrolls + gestures + movements

    fun getDateFormatted(): String {
        return android.text.format.DateFormat.format("MMM dd, yyyy", date).toString()
    }
}

/**
 * Time range filter options.
 */
enum class TimeRange(val displayName: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("This Week", 7),
    MONTH("This Month", 30),
    YEAR("This Year", 365),
    ALL_TIME("All Time", 0);

    companion object {
        fun fromDays(days: Int): TimeRange {
            return values().find { it.days == days } ?: ALL_TIME
        }
    }
}

/**
 * Chart types for the statistics screen.
 */
enum class ChartType(val displayName: String) {
    GESTURES("Gesture Distribution"),
    MOVEMENT("Movement Over Time"),
    CONNECTION("Connection History"),
    PERFORMANCE("Performance Metrics");

    companion object {
        fun fromDisplayName(name: String): ChartType {
            return values().find { it.displayName == name } ?: GESTURES
        }
    }
}

/**
 * Statistics summary for quick view
 */
data class StatisticsSummary(
    val totalClicks: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalMovements: Int = 0,
    val totalDistance: Float = 0f,
    val averageSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val sessionDuration: Long = 0,
    val gesturesDetected: Int = 0,
    val customGesturesUsed: Int = 0
) {
    val totalActions: Int get() = totalClicks + totalDoubleClicks + totalRightClicks + totalScrolls + totalMovements + gesturesDetected
    val totalClicksAll: Int get() = totalClicks + totalDoubleClicks + totalRightClicks

    fun getAverageSpeedFormatted(): String {
        return String.format("%.1f", averageSpeed)
    }

    fun getTotalDistanceFormatted(): String {
        return String.format("%.1f", totalDistance)
    }

    fun getSessionDurationFormatted(): String {
        val hours = sessionDuration / 3600
        val minutes = (sessionDuration % 3600) / 60
        val seconds = sessionDuration % 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
}

/**
 * Historical statistics
 */
data class HistoricalStatistics(
    val totalGestures: Int = 0,
    val gesturesByType: Map<String, Int> = emptyMap(),
    val mostUsedGesture: String = "",
    val lastGestureTime: Long = 0,
    val customGestureUsage: Map<String, Int> = emptyMap()
) {
    val mostUsedCount: Int get() = gesturesByType[mostUsedGesture] ?: 0

    fun getGesturePercentage(gesture: String): Float {
        return if (totalGestures > 0) {
            (gesturesByType[gesture] ?: 0).toFloat() / totalGestures * 100f
        } else 0f
    }
}

/**
 * Gesture statistics
 */
data class GestureStatistics(
    val gestureName: String = "",
    val detectionCount: Int = 0,
    val confidencePercentage: Float = 0f,
    val lastDetected: Long = 0
) {
    val isConfident: Boolean get() = confidencePercentage >= 0.7f

    fun getConfidenceFormatted(): String {
        return String.format("%.1f%%", confidencePercentage * 100)
    }
}

/**
 * Export options
 */
enum class ExportFormat(val displayName: String, val extension: String) {
    JSON("JSON", "json"),
    CSV("CSV", "csv"),
    PDF("PDF", "pdf");

    companion object {
        fun fromExtension(extension: String): ExportFormat {
            return values().find { it.extension == extension } ?: JSON
        }
    }
}