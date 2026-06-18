package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Session statistics for the app.
 */
@Parcelize
data class SessionStatistics(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long = 0,
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val gestures: List<String> = emptyList(),
    val totalDistance: Float = 0f,
    val averageSpeed: Float = 0f,
    val connectionAttempts: Int = 0,
    val connectionSuccesses: Int = 0,
    val batteryDrain: Int = 0,
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f
) : Parcelable {
    val totalClicks: Int get() = clicks + doubleClicks + rightClicks
    val gestureCount: Int get() = gestures.size
    val duration: Long get() = if (endTime > 0) endTime - startTime else System.currentTimeMillis() - startTime

    companion object {
        fun newSession() = SessionStatistics(
            sessionId = java.util.UUID.randomUUID().toString(),
            startTime = System.currentTimeMillis()
        )
    }
}

/**
 * Daily usage statistics.
 */
@Parcelize
data class DailyStatistics(
    val date: String, // YYYY-MM-DD
    val totalClicks: Int = 0,
    val totalGestures: Int = 0,
    val totalDistance: Float = 0f,
    val activeTime: Long = 0,
    val connectionCount: Int = 0
) : Parcelable {
    companion object {
        fun today() = DailyStatistics(
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        )
    }
}

/**
 * Historical statistics across multiple days.
 */
@Parcelize
data class HistoricalStatistics(
    val dailyStats: List<DailyStatistics> = emptyList(),
    val weeklyStats: List<DailyStatistics> = emptyList(),
    val monthlyStats: List<DailyStatistics> = emptyList()
) : Parcelable {
    val totalClicks: Int get() = dailyStats.sumOf { it.totalClicks }
    val totalGestures: Int get() = dailyStats.sumOf { it.totalGestures }
    val totalDistance: Float get() = dailyStats.sumOf { it.totalDistance.toDouble() }.toFloat()
    val activeDays: Int get() = dailyStats.size
    val averageDailyClicks: Float get() = if (activeDays > 0) totalClicks.toFloat() / activeDays else 0f
}
