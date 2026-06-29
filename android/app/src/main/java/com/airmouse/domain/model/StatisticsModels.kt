package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Represents a summary of statistics for a session.
 * Used for both current session and historical data.
 */
@Parcelize
data class StatisticsSummary(
    val totalClicks: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalMovements: Int = 0,
    val totalDistance: Float = 0f,
    val averageSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val sessionDuration: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Converts this object to JSON format.
     */
    fun toJson(): JSONObject = JSONObject().apply {
        put("totalClicks", totalClicks)
        put("totalDoubleClicks", totalDoubleClicks)
        put("totalRightClicks", totalRightClicks)
        put("totalScrolls", totalScrolls)
        put("totalMovements", totalMovements)
        put("totalDistance", totalDistance)
        put("averageSpeed", averageSpeed)
        put("maxSpeed", maxSpeed)
        put("sessionDuration", sessionDuration)
        put("lastUpdated", lastUpdated)
    }

    /**
     * Returns a formatted string for display.
     */
    fun getFormattedSummary(): String {
        return buildString {
            appendLine("📊 Session Statistics")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Clicks: $totalClicks")
            appendLine("Double Clicks: $totalDoubleClicks")
            appendLine("Right Clicks: $totalRightClicks")
            appendLine("Scrolls: $totalScrolls")
            appendLine("Movements: $totalMovements")
            appendLine("Distance: ${"%.1f".format(totalDistance)} units")
            appendLine("Avg Speed: ${"%.1f".format(averageSpeed)} units/s")
            appendLine("Max Speed: ${"%.1f".format(maxSpeed)} units/s")
            appendLine("Duration: ${formatDuration(sessionDuration)}")
        }
    }

    /**
     * Formats a duration in milliseconds to a readable string.
     */
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format(java.util.Locale.US, "%02d:%02d", minutes, secs)
            else -> String.format(java.util.Locale.US, "%02ds", secs)
        }
    }

    companion object {
        /**
         * Creates a StatisticsSummary from JSON.
         */
        fun fromJson(json: JSONObject): StatisticsSummary? {
            return try {
                StatisticsSummary(
                    totalClicks = json.optInt("totalClicks", 0),
                    totalDoubleClicks = json.optInt("totalDoubleClicks", 0),
                    totalRightClicks = json.optInt("totalRightClicks", 0),
                    totalScrolls = json.optInt("totalScrolls", 0),
                    totalMovements = json.optInt("totalMovements", 0),
                    totalDistance = json.optDouble("totalDistance", 0.0).toFloat(),
                    averageSpeed = json.optDouble("averageSpeed", 0.0).toFloat(),
                    maxSpeed = json.optDouble("maxSpeed", 0.0).toFloat(),
                    sessionDuration = json.optLong("sessionDuration", 0L),
                    lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Creates an empty StatisticsSummary.
         */
        fun empty(): StatisticsSummary = StatisticsSummary()
    }
}

/**
 * Represents statistics for a single day.
 */
@Parcelize
data class DailyStats(
    val date: String = "",
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val movements: Int = 0,
    val distance: Float = 0f,
    val gestures: Int = 0,
    val totalTime: Long = 0
) : Parcelable

/**
 * Represents statistics for a specific gesture.
 */
@Parcelize
data class GestureStatistics(
    val gestureName: String,
    val detectionCount: Int,
    val confidencePercentage: Float,
    val lastDetected: Long
) : Parcelable

typealias GestureStats = GestureStatistics
