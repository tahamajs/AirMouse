package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Historical statistics aggregated across all sessions.
 * Contains total gestures, breakdown by type, most used gestures, and custom gesture usage.
 */
@Parcelize
data class HistoricalStatistics(
    val totalGestures: Int = 0,
    val gesturesByType: Map<String, Int> = emptyMap(),
    val mostUsedGesture: String = "",
    val lastGestureTime: Long = 0,
    val customGestureUsage: Map<String, Int> = emptyMap(),
    val totalSessions: Int = 0,
    val averageGesturesPerSession: Float = 0f,
    val totalClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val longestSessionMs: Long = 0,
    val firstSessionDate: Long = 0,
    val lastSessionDate: Long = 0
) : Parcelable {

    /**
     * Gets the total number of gesture detections across all types.
     */
    fun getTotalDetections(): Int = totalGestures

    /**
     * Gets the detection count for a specific gesture type.
     */
    fun getCountForGesture(gesture: String): Int = gesturesByType[gesture] ?: 0

    /**
     * Gets the most used gesture name, or "None" if no gestures.
     */
    fun getMostUsedGestureName(): String = mostUsedGesture.ifEmpty { "None" }

    /**
     * Gets the number of unique gesture types detected.
     */
    fun getUniqueGestureTypes(): Int = gesturesByType.size

    /**
     * Gets the number of custom gestures used.
     */
    fun getCustomGestureCount(): Int = customGestureUsage.size

    /**
     * Gets the total number of custom gesture detections.
     */
    fun getTotalCustomDetections(): Int = customGestureUsage.values.sum()

    /**
     * Gets the most used custom gesture.
     */
    fun getMostUsedCustomGesture(): String {
        return customGestureUsage.maxByOrNull { it.value }?.key ?: ""
    }

    /**
     * Checks if any gestures have been recorded.
     */
    fun hasData(): Boolean = totalGestures > 0

    /**
     * Gets the average gestures per session.
     */
    fun averageGesturesPerSessionValue(): Float = averageGesturesPerSession

    /**
     * Gets the total click count (all click types combined).
     */
    fun totalClicksCombined(): Int = totalClicks + totalDoubleClicks + totalRightClicks

    /**
     * Gets the average daily gestures (if first session date is available).
     */
    fun getAverageDailyGestures(): Float {
        if (firstSessionDate == 0L || lastSessionDate == 0L || totalGestures == 0) return 0f
        val days = ((lastSessionDate - firstSessionDate) / (24 * 60 * 60 * 1000f)).coerceAtLeast(1f)
        return totalGestures / days
    }

    /**
     * Checks if this is a new session (no data).
     */
    fun isNewSession(): Boolean = totalGestures == 0 && totalClicks == 0 && totalScrolls == 0

    companion object {
        /**
         * Creates an empty historical statistics object.
         */
        fun empty(): HistoricalStatistics = HistoricalStatistics()

        /**
         * Creates a historical statistics object from a list of gesture statistics.
         */
        fun fromGestureStats(gestureStats: List<GestureStatistics>): HistoricalStatistics {
            val byType = gestureStats.associate { it.gestureName to it.detectionCount }
            val total = gestureStats.sumOf { it.detectionCount }
            val mostUsed = gestureStats.maxByOrNull { it.detectionCount }?.gestureName ?: ""
            val lastTime = gestureStats.maxOfOrNull { it.lastDetected } ?: 0L

            return HistoricalStatistics(
                totalGestures = total,
                gesturesByType = byType,
                mostUsedGesture = mostUsed,
                lastGestureTime = lastTime,
                customGestureUsage = gestureStats
                    .filter { it.gestureName.startsWith("custom_") }
                    .associate { it.gestureName to it.detectionCount }
            )
        }

        /**
         * Creates a historical statistics object from a list of statistics summaries.
         */
        fun fromStatisticsSummaries(summaries: List<StatisticsSummary>): HistoricalStatistics {
            if (summaries.isEmpty()) return empty()

            val totalGestures = summaries.sumOf { it.totalClicks + it.totalDoubleClicks + it.totalRightClicks + it.totalScrolls }
            val totalClicks = summaries.sumOf { it.totalClicks }
            val totalDoubleClicks = summaries.sumOf { it.totalDoubleClicks }
            val totalRightClicks = summaries.sumOf { it.totalRightClicks }
            val totalScrolls = summaries.sumOf { it.totalScrolls }

            val totalSessions = summaries.size
            val avgGesturesPerSession = if (totalSessions > 0) totalGestures.toFloat() / totalSessions else 0f
            val longestSession = summaries.maxOfOrNull { it.sessionDuration } ?: 0L
            val firstSessionDate = summaries.minOfOrNull { it.lastUpdated } ?: 0L
            val lastSessionDate = summaries.maxOfOrNull { it.lastUpdated } ?: 0L

            return HistoricalStatistics(
                totalGestures = totalGestures,
                totalSessions = totalSessions,
                averageGesturesPerSession = avgGesturesPerSession,
                totalClicks = totalClicks,
                totalScrolls = totalScrolls,
                totalDoubleClicks = totalDoubleClicks,
                totalRightClicks = totalRightClicks,
                longestSessionMs = longestSession,
                firstSessionDate = firstSessionDate,
                lastSessionDate = lastSessionDate,
                mostUsedGesture = "click", // This would be calculated from detailed data
                gesturesByType = emptyMap(),
                customGestureUsage = emptyMap()
            )
        }
    }
}

/**
 * Extension function to combine two HistoricalStatistics objects.
 */
fun HistoricalStatistics.combineWith(other: HistoricalStatistics): HistoricalStatistics {
    if (!this.hasData() && !other.hasData()) return this

    val combinedGestures = this.totalGestures + other.totalGestures
    val combinedClicks = this.totalClicks + other.totalClicks
    val combinedDoubleClicks = this.totalDoubleClicks + other.totalDoubleClicks
    val combinedRightClicks = this.totalRightClicks + other.totalRightClicks
    val combinedScrolls = this.totalScrolls + other.totalScrolls

    // Merge gesture type maps
    val mergedByType = this.gesturesByType.toMutableMap()
    other.gesturesByType.forEach { (key, value) ->
        mergedByType[key] = (mergedByType[key] ?: 0) + value
    }

    // Find most used gesture
    val mostUsed = mergedByType.maxByOrNull { it.value }?.key ?: ""

    // Merge custom gesture usage
    val mergedCustom = this.customGestureUsage.toMutableMap()
    other.customGestureUsage.forEach { (key, value) ->
        mergedCustom[key] = (mergedCustom[key] ?: 0) + value
    }

    return this.copy(
        totalGestures = combinedGestures,
        gesturesByType = mergedByType,
        mostUsedGesture = mostUsed,
        lastGestureTime = maxOf(this.lastGestureTime, other.lastGestureTime),
        customGestureUsage = mergedCustom,
        totalSessions = this.totalSessions + other.totalSessions,
        averageGesturesPerSession = if (this.totalSessions + other.totalSessions > 0) {
            combinedGestures.toFloat() / (this.totalSessions + other.totalSessions)
        } else 0f,
        totalClicks = combinedClicks,
        totalScrolls = combinedScrolls,
        totalDoubleClicks = combinedDoubleClicks,
        totalRightClicks = combinedRightClicks,
        longestSessionMs = maxOf(this.longestSessionMs, other.longestSessionMs),
        firstSessionDate = if (this.firstSessionDate == 0L) other.firstSessionDate else minOf(this.firstSessionDate, other.firstSessionDate),
        lastSessionDate = maxOf(this.lastSessionDate, other.lastSessionDate)
    )
}

/**
 * Extension function to format historical statistics as a summary string.
 */
fun HistoricalStatistics.toSummaryString(): String {
    return buildString {
        appendLine("📊 Historical Statistics Summary")
        appendLine("=================================")
        appendLine("Total Gestures: $totalGestures")
        appendLine("Unique Gesture Types: ${gesturesByType.size}")
        appendLine("Most Used Gesture: ${getMostUsedGestureName()}")
        appendLine("Total Clicks: ${totalClicksCombined()}")
        appendLine("Total Scrolls: $totalScrolls")
        appendLine("Total Sessions: $totalSessions")
        appendLine("Avg Gestures/Session: ${"%.1f".format(averageGesturesPerSession)}")
        appendLine("Custom Gestures Used: ${getCustomGestureCount()}")
        appendLine("Total Custom Detections: ${getTotalCustomDetections()}")
        appendLine("Longest Session: ${formatDuration(longestSessionMs)}")
        if (firstSessionDate > 0 && lastSessionDate > 0) {
            appendLine("First Session: ${DateUtils.formatDateTime(firstSessionDate)}")
            appendLine("Last Session: ${DateUtils.formatDateTime(lastSessionDate)}")
            appendLine("Avg Daily Gestures: ${"%.1f".format(getAverageDailyGestures())}")
        }
    }
}

/**
 * Extension function to format duration in milliseconds to a readable string.
 */
private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0s"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

/**
 * Placeholder DateUtils for formatting (should be imported from your utils package).
 * Replace with your actual DateUtils implementation.
 */
private object DateUtils {
    fun formatDateTime(timestamp: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}
