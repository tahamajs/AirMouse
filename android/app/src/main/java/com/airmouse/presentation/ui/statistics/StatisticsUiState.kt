
package com.airmouse.presentation.ui.statistics

import java.util.Date
import java.util.Locale
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.AppPreferences

data class StatisticsUiState(
    
    val sessionTime: Long = 0,                    
    val sessionStartTime: Long = System.currentTimeMillis(),
    val lastActivityTime: Long = System.currentTimeMillis(),

    
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val gesturesDetected: Int = 0,
    val customGesturesUsed: Int = 0,

    
    val totalDistanceMoved: Float = 0f,
    val averageSpeed: Float = 0f,
    val peakSpeed: Float = 0f,
    val totalMovements: Int = 0,

    
    val connectionAttempts: Int = 0,
    val successfulConnections: Int = 0,
    val failedConnections: Int = 0,
    val averagePing: Int = 0,

    
    val lastCalibrationTime: Long = 0,
    val calibrationCount: Int = 0,
    val calibrationSuccessRate: Float = 0f,

    val isTracking: Boolean = false,
    val calibrationComplete: Boolean = false,
    val touchpadActive: Boolean = false,
    val presentationModeEnabled: Boolean = false,
    val autoConnect: Boolean = false,
    val useWebSocket: Boolean = true,
    val useUdpDiscovery: Boolean = true,
    val theme: String = "system",
    val language: String = "en",

    
    val batteryUsage: Int = 0,
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val temperature: Float = 0f,

    val mostUsedGesture: String = "",
    val mostUsedGestureCount: Int = 0,
    val gestureTypeCount: Int = 0,
    val customGestureCount: Int = 0,

    
    val dailyStats: List<DailyStats> = emptyList(),
    val gestureBreakdown: Map<String, Int> = emptyMap(),

    
    val isLoading: Boolean = false,
    val timeRange: TimeRange = TimeRange.ALL_TIME,
    val selectedChart: ChartType = ChartType.GESTURES,
    val showExportDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val summaryStats: StatisticsSummary = StatisticsSummary(),
    val mouseStatistics: MouseStatistics = MouseStatistics(),
    val appPreferences: AppPreferences = AppPreferences()
) {
    val summary: StatisticsSummary = StatisticsSummary(
        totalClicks = clicks,
        totalDoubleClicks = doubleClicks,
        totalRightClicks = rightClicks,
        totalScrolls = scrolls,
        totalMovements = totalMovements,
        totalDistance = totalDistanceMoved,
        averageSpeed = averageSpeed,
        maxSpeed = peakSpeed,
        sessionDuration = sessionTime
    )
    
    fun getTotalGestures(): Int = clicks + doubleClicks + rightClicks + scrolls + gesturesDetected + customGesturesUsed

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
        return String.format(Locale.US, "%.1f", averageSpeed)
    }

    fun getDistanceFormatted(): String {
        return String.format(Locale.US, "%.1f", totalDistanceMoved)
    }

    fun getSessionTimeFormatted(): String {
        val hours = sessionTime / 3600
        val minutes = (sessionTime % 3600) / 60
        val seconds = sessionTime % 60
        return when {
            hours > 0 -> String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}

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

val StatisticsSummary.totalActions: Int
    get() = totalClicks + totalDoubleClicks + totalRightClicks + totalScrolls + totalMovements

val StatisticsSummary.totalClicksAll: Int
    get() = totalClicks + totalDoubleClicks + totalRightClicks

fun StatisticsSummary.getAverageSpeedFormatted(): String {
    return String.format(Locale.US, "%.1f", averageSpeed)
}

fun StatisticsSummary.getSessionDurationFormatted(): String {
    val hours = sessionDuration / 3600
    val minutes = (sessionDuration % 3600) / 60
    val seconds = sessionDuration % 60
    return when {
        hours > 0 -> String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

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

data class GestureStatistics(
    val gestureName: String = "",
    val detectionCount: Int = 0,
    val confidencePercentage: Float = 0f,
    val lastDetected: Long = 0
) {
    val isConfident: Boolean get() = confidencePercentage >= 0.7f

    fun getConfidenceFormatted(): String {
        return String.format(Locale.US, "%.1f%%", confidencePercentage * 100)
    }
}

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
