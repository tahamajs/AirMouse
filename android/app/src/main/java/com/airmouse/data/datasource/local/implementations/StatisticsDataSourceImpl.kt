package com.airmouse.data.datasource.local

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.utils.PreferencesManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of IStatisticsDataSource using PreferencesManager.
 * Stores all statistics in SharedPreferences as JSON strings.
 */
@Singleton
class StatisticsDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IStatisticsDataSource {

    private val sessionKey = "session_stats"
    private val historicalKey = "historical_stats"
    private val gestureStatsKey = "gesture_stats"
    private val dailyPrefix = "daily_"

    // ============================================================
    // Session Stats (IStatisticsDataSource implementation)
    // ============================================================

    override suspend fun getStatistics(): StatisticsSummary? {
        return getSessionStats()
    }

    override suspend fun saveStatistics(stats: StatisticsSummary) {
        saveSessionStats(stats)
    }

    override suspend fun saveSessionStats(stats: StatisticsSummary) {
        val obj = JSONObject().apply {
            put("totalClicks", stats.totalClicks)
            put("totalDoubleClicks", stats.totalDoubleClicks)
            put("totalRightClicks", stats.totalRightClicks)
            put("totalScrolls", stats.totalScrolls)
            put("totalMovements", stats.totalMovements)
            put("totalDistance", stats.totalDistance)
            put("averageSpeed", stats.averageSpeed)
            put("maxSpeed", stats.maxSpeed)
            put("sessionDuration", stats.sessionDuration)
        }
        prefs.putString(sessionKey, obj.toString())
    }

    override suspend fun getSessionStats(): StatisticsSummary {
        val json = prefs.getString(sessionKey, "{}")
        return try {
            val obj = JSONObject(json)
            StatisticsSummary(
                totalClicks = obj.optInt("totalClicks", 0),
                totalDoubleClicks = obj.optInt("totalDoubleClicks", 0),
                totalRightClicks = obj.optInt("totalRightClicks", 0),
                totalScrolls = obj.optInt("totalScrolls", 0),
                totalMovements = obj.optInt("totalMovements", 0),
                totalDistance = obj.optDouble("totalDistance", 0.0).toFloat(),
                averageSpeed = obj.optDouble("averageSpeed", 0.0).toFloat(),
                maxSpeed = obj.optDouble("maxSpeed", 0.0).toFloat(),
                sessionDuration = obj.optLong("sessionDuration", 0)
            )
        } catch (e: Exception) {
            StatisticsSummary()
        }
    }

    // ============================================================
    // Daily Stats
    // ============================================================

    override suspend fun saveDailyStats(date: String, stats: DailyStats) {
        val key = dailyPrefix + date
        val obj = JSONObject().apply {
            put("date", date)
            put("clicks", stats.clicks)
            put("doubleClicks", stats.doubleClicks)
            put("rightClicks", stats.rightClicks)
            put("scrolls", stats.scrolls)
            put("movements", stats.movements)
            put("distance", stats.distance)
            put("gestures", stats.gestures)
            put("totalTime", stats.totalTime)
        }
        prefs.putString(key, obj.toString())
    }

    override suspend fun getDailyStats(date: String): DailyStats {
        val key = dailyPrefix + date
        val json = prefs.getString(key, "{}")
        return try {
            val obj = JSONObject(json)
            DailyStats(
                date = date,
                clicks = obj.optInt("clicks", 0),
                doubleClicks = obj.optInt("doubleClicks", 0),
                rightClicks = obj.optInt("rightClicks", 0),
                scrolls = obj.optInt("scrolls", 0),
                movements = obj.optInt("movements", 0),
                distance = obj.optDouble("distance", 0.0).toFloat(),
                gestures = obj.optInt("gestures", 0),
                totalTime = obj.optLong("totalTime", 0)
            )
        } catch (e: Exception) {
            DailyStats(date = date)
        }
    }

    override suspend fun getDailyStatsForRange(startDate: String, endDate: String): List<DailyStats> {
        val stats = mutableListOf<DailyStats>()

        try {
            val startParts = startDate.split("-").map { it.toInt() }
            val endParts = endDate.split("-").map { it.toInt() }

            var year = startParts[0]
            var month = startParts[1]
            var day = startParts[2]

            val endYear = endParts[0]
            val endMonth = endParts[1]
            val endDay = endParts[2]

            while (year < endYear || (year == endYear && (month < endMonth || (month == endMonth && day <= endDay)))) {
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
                val stat = getDailyStats(dateStr)

                // Only add if there is any activity
                if (stat.clicks > 0 || stat.scrolls > 0 || stat.movements > 0 || stat.gestures > 0) {
                    stats.add(stat)
                }

                // Increment day
                day++
                val daysInMonth = getDaysInMonth(year, month)

                if (day > daysInMonth) {
                    day = 1
                    month++
                    if (month > 12) {
                        month = 1
                        year++
                    }
                }
            }
        } catch (e: Exception) {
            // If date parsing fails, return empty list
        }

        return stats
    }

    /**
     * Helper to get the number of days in a month.
     */
    private fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }
    }

    /**
     * Helper to check if a year is a leap year.
     */
    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }

    // ============================================================
    // Historical Stats
    // ============================================================

    override suspend fun saveHistoricalStats(stats: HistoricalStatistics) {
        val obj = JSONObject().apply {
            put("totalGestures", stats.totalGestures)
            put("mostUsedGesture", stats.mostUsedGesture)
            put("lastGestureTime", stats.lastGestureTime)
            put("totalSessions", stats.totalSessions)
            put("averageGesturesPerSession", stats.averageGesturesPerSession)
            put("totalClicks", stats.totalClicks)
            put("totalScrolls", stats.totalScrolls)
            put("totalDoubleClicks", stats.totalDoubleClicks)
            put("totalRightClicks", stats.totalRightClicks)
            put("longestSessionMs", stats.longestSessionMs)
            put("firstSessionDate", stats.firstSessionDate)
            put("lastSessionDate", stats.lastSessionDate)
        }

        val byType = JSONObject()
        stats.gesturesByType.forEach { (key, value) ->
            byType.put(key, value)
        }
        obj.put("gesturesByType", byType)

        val customUsage = JSONObject()
        stats.customGestureUsage.forEach { (key, value) ->
            customUsage.put(key, value)
        }
        obj.put("customGestureUsage", customUsage)

        prefs.putString(historicalKey, obj.toString())
    }

    override suspend fun getHistoricalStats(): HistoricalStatistics {
        val json = prefs.getString(historicalKey, "{}")
        return try {
            val obj = JSONObject(json)

            val byType = mutableMapOf<String, Int>()
            val byTypeJson = obj.optJSONObject("gesturesByType")
            byTypeJson?.keys()?.forEach { key ->
                byType[key] = byTypeJson.getInt(key)
            }

            val customUsage = mutableMapOf<String, Int>()
            val customJson = obj.optJSONObject("customGestureUsage")
            customJson?.keys()?.forEach { key ->
                customUsage[key] = customJson.getInt(key)
            }

            HistoricalStatistics(
                totalGestures = obj.optInt("totalGestures", 0),
                gesturesByType = byType,
                mostUsedGesture = obj.optString("mostUsedGesture", ""),
                lastGestureTime = obj.optLong("lastGestureTime", 0),
                customGestureUsage = customUsage,
                totalSessions = obj.optInt("totalSessions", 0),
                averageGesturesPerSession = obj.optDouble("averageGesturesPerSession", 0.0).toFloat(),
                totalClicks = obj.optInt("totalClicks", 0),
                totalScrolls = obj.optInt("totalScrolls", 0),
                totalDoubleClicks = obj.optInt("totalDoubleClicks", 0),
                totalRightClicks = obj.optInt("totalRightClicks", 0),
                longestSessionMs = obj.optLong("longestSessionMs", 0),
                firstSessionDate = obj.optLong("firstSessionDate", 0),
                lastSessionDate = obj.optLong("lastSessionDate", 0)
            )
        } catch (e: Exception) {
            HistoricalStatistics()
        }
    }

    // ============================================================
    // Gesture Stats
    // ============================================================

    override suspend fun saveGestureStats(stats: List<GestureStatistics>) {
        val array = JSONArray()
        stats.forEach { stat ->
            val obj = JSONObject().apply {
                put("gestureName", stat.gestureName)
                put("detectionCount", stat.detectionCount)
                put("confidencePercentage", stat.confidencePercentage)
                put("lastDetected", stat.lastDetected)
            }
            array.put(obj)
        }
        prefs.putString(gestureStatsKey, array.toString())
    }

    override suspend fun getGestureStats(): List<GestureStatistics> {
        val json = prefs.getString(gestureStatsKey, "[]")
        val list = mutableListOf<GestureStatistics>()

        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GestureStatistics(
                        gestureName = obj.getString("gestureName"),
                        detectionCount = obj.getInt("detectionCount"),
                        confidencePercentage = obj.optDouble("confidencePercentage", 0.0).toFloat(),
                        lastDetected = obj.optLong("lastDetected", 0)
                    )
                )
            }
        } catch (e: Exception) {
            // Return empty list on error
        }

        return list
    }

    override suspend fun incrementGestureCount(gesture: String) {
        val currentStats = getGestureStats().toMutableList()
        val existing = currentStats.find { it.gestureName == gesture }

        if (existing != null) {
            val index = currentStats.indexOf(existing)
            currentStats[index] = existing.copy(
                detectionCount = existing.detectionCount + 1,
                lastDetected = System.currentTimeMillis()
            )
        } else {
            currentStats.add(
                GestureStatistics(
                    gestureName = gesture,
                    detectionCount = 1,
                    confidencePercentage = 1f,
                    lastDetected = System.currentTimeMillis()
                )
            )
        }

        saveGestureStats(currentStats)
    }

    // ============================================================
    // Reset Operations
    // ============================================================

    override suspend fun resetSessionStats() {
        prefs.remove(sessionKey)
    }

    override suspend fun resetAllStats() {
        prefs.remove(sessionKey)
        prefs.remove(historicalKey)
        prefs.remove(gestureStatsKey)

        // Remove all daily stats
        val allKeys = prefs.getAllKeys()
        allKeys.filter { it.startsWith(dailyPrefix) }.forEach { key ->
            prefs.remove(key)
        }
    }

    // ============================================================
    // Utility: Get All Keys
    // ============================================================

    /**
     * Get all preference keys (useful for debugging).
     */
    fun getAllKeys(): Set<String> {
        return prefs.getAllKeys()
    }

    /**
     * Get a summary of all stored statistics.
     */
    suspend fun getStatisticsSummary(): Map<String, Any> {
        return mapOf(
            "session" to getSessionStats(),
            "historical" to getHistoricalStats(),
            "gestureCount" to getGestureStats().size
        )
    }

    // ============================================================
    // Companion Object
    // ============================================================

    companion object {
        /**
         * Get the current date as a string (yyyy-MM-dd).
         */
        fun getTodayDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        /**
         * Get a date string from a timestamp.
         */
        fun getDateString(timestamp: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
