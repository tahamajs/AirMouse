// app/src/main/java/com/airmouse/data/datasource/local/StatisticsDataSourceImpl.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.utils.PreferencesManager
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IStatisticsDataSource {

    private val sessionKey = "session_stats"
    private val historicalKey = "historical_stats"
    private val gestureStatsKey = "gesture_stats"
    private val dailyPrefix = "daily_"

    override suspend fun saveSessionStats(stats: StatisticsSummary) {
        val obj = JSONObject()
        obj.put("totalClicks", stats.totalClicks)
        obj.put("totalDoubleClicks", stats.totalDoubleClicks)
        obj.put("totalRightClicks", stats.totalRightClicks)
        obj.put("totalScrolls", stats.totalScrolls)
        obj.put("totalMovements", stats.totalMovements)
        obj.put("totalDistance", stats.totalDistance)
        obj.put("averageSpeed", stats.averageSpeed)
        obj.put("maxSpeed", stats.maxSpeed)
        obj.put("sessionDuration", stats.sessionDuration)
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

    override suspend fun saveDailyStats(date: String, stats: DailyStats) {
        val key = dailyPrefix + date
        val obj = JSONObject()
        obj.put("date", date)
        obj.put("clicks", stats.clicks)
        obj.put("doubleClicks", stats.doubleClicks)
        obj.put("rightClicks", stats.rightClicks)
        obj.put("scrolls", stats.scrolls)
        obj.put("movements", stats.movements)
        obj.put("distance", stats.distance)
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
                distance = obj.optDouble("distance", 0.0).toFloat()
            )
        } catch (e: Exception) {
            DailyStats(date = date)
        }
    }

    override suspend fun getDailyStatsForRange(startDate: String, endDate: String): List<DailyStats> {
        // In production, would query database
        // For now, return empty list
        return emptyList()
    }

    override suspend fun saveHistoricalStats(stats: HistoricalStatistics) {
        val obj = JSONObject()
        obj.put("totalGestures", stats.totalGestures)
        obj.put("mostUsedGesture", stats.mostUsedGesture)
        obj.put("lastGestureTime", stats.lastGestureTime)

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
                customGestureUsage = customUsage
            )
        } catch (e: Exception) {
            HistoricalStatistics()
        }
    }

    override suspend fun saveGestureStats(stats: List<GestureStatistics>) {
        val array = JSONArray()
        stats.forEach { stat ->
            val obj = JSONObject()
            obj.put("gestureName", stat.gestureName)
            obj.put("detectionCount", stat.detectionCount)
            obj.put("confidencePercentage", stat.confidencePercentage)
            obj.put("lastDetected", stat.lastDetected)
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
                list.add(GestureStatistics(
                    gestureName = obj.getString("gestureName"),
                    detectionCount = obj.getInt("detectionCount"),
                    confidencePercentage = obj.optDouble("confidencePercentage", 0.0).toFloat(),
                    lastDetected = obj.optLong("lastDetected", 0)
                ))
            }
        } catch (e: Exception) {
            // Return empty list
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
            currentStats.add(GestureStatistics(
                gestureName = gesture,
                detectionCount = 1,
                lastDetected = System.currentTimeMillis()
            ))
        }
        saveGestureStats(currentStats)
    }

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
}