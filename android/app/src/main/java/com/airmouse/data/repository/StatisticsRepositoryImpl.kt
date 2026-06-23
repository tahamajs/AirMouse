
package com.airmouse.data.repository

import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IStatisticsRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IStatisticsRepository {

    private val _sessionStats = MutableStateFlow(StatisticsSummary())
    override fun observeCurrentSession(): Flow<StatisticsSummary> = _sessionStats.asStateFlow()

    private val _historicalStats = MutableStateFlow(HistoricalStatistics())
    override fun observeHistoricalStats(): Flow<HistoricalStatistics> = _historicalStats.asStateFlow()

    private val _gestureStats = MutableStateFlow<List<GestureStatistics>>(emptyList())
    override fun observeGestureStats(): Flow<List<GestureStatistics>> = _gestureStats.asStateFlow()

    private val _isTracking = MutableStateFlow(false)

    init {
        loadSessionStats()
        loadHistoricalStats()
        loadGestureStats()
    }

    private fun loadSessionStats() {
        _sessionStats.value = StatisticsSummary(
            totalClicks = prefs.getInt("session_clicks", 0),
            totalDoubleClicks = prefs.getInt("session_double_clicks", 0),
            totalRightClicks = prefs.getInt("session_right_clicks", 0),
            totalScrolls = prefs.getInt("session_scrolls", 0),
            totalMovements = prefs.getInt("session_movements", 0),
            totalDistance = prefs.getFloat("session_distance", 0f),
            averageSpeed = prefs.getFloat("session_avg_speed", 0f),
            maxSpeed = prefs.getFloat("session_max_speed", 0f),
            sessionDuration = System.currentTimeMillis() - prefs.getLong("session_start", System.currentTimeMillis())
        )
    }

    private fun loadHistoricalStats() {
        val json = prefs.getString("historical_stats", "{}")
        try {
            val obj = JSONObject(json)
            _historicalStats.value = HistoricalStatistics(
                totalGestures = obj.optInt("totalGestures", 0),
                mostUsedGesture = obj.optString("mostUsedGesture", ""),
                lastGestureTime = obj.optLong("lastGestureTime", 0)
            )
            val byTypeJson = obj.optJSONObject("gesturesByType")
            if (byTypeJson != null) {
                val byType = mutableMapOf<String, Int>()
                byTypeJson.keys().forEach { key ->
                    byType[key] = byTypeJson.getInt(key)
                }
                _historicalStats.value = _historicalStats.value.copy(gesturesByType = byType)
            }
        } catch (e: Exception) {
            
        }
    }

    private fun loadGestureStats() {
        val json = prefs.getString("gesture_stats", "[]")
        try {
            val array = JSONArray(json)
            val stats = mutableListOf<GestureStatistics>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                stats.add(GestureStatistics(
                    gestureName = obj.getString("gestureName"),
                    detectionCount = obj.getInt("detectionCount"),
                    confidencePercentage = obj.optDouble("confidencePercentage", 0.0).toFloat(),
                    lastDetected = obj.optLong("lastDetected", 0)
                ))
            }
            _gestureStats.value = stats
        } catch (e: Exception) {
            
        }
    }

    override suspend fun getCurrentSession(): StatisticsSummary = _sessionStats.value

    override suspend fun startTracking() {
        _isTracking.value = true
        prefs.putLong("session_start", System.currentTimeMillis())
        resetSession()
    }

    override suspend fun stopTracking() {
        _isTracking.value = false
        saveSessionToHistory()
    }

    override suspend fun isTracking(): Boolean = _isTracking.value

    override suspend fun getHistoricalStats(): HistoricalStatistics = _historicalStats.value

    override suspend fun getTodayStats(): DailyStats {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return getDailyStats(date)
    }

    override suspend fun getDailyStats(date: String): DailyStats {
        val key = "daily_stats_$date"
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

    override suspend fun getWeekStats(): List<DailyStats> {
        val stats = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()
        for (i in 6 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            stats.add(getDailyStats(date))
        }
        return stats
    }

    override suspend fun getMonthStats(): List<DailyStats> {
        val stats = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()
        for (i in 29 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            stats.add(getDailyStats(date))
        }
        return stats
    }

    override suspend fun getGestureStats(): List<GestureStatistics> = _gestureStats.value

    override suspend fun recordClick() {
        updateSessionStats { it.copy(totalClicks = it.totalClicks + 1) }
        updateDailyStats { it.copy(clicks = it.clicks + 1) }
        updateHistoricalStats("click")
        prefs.putInt("session_clicks", prefs.getInt("session_clicks", 0) + 1)
    }

    override suspend fun recordDoubleClick() {
        updateSessionStats { it.copy(totalDoubleClicks = it.totalDoubleClicks + 1) }
        updateDailyStats { it.copy(doubleClicks = it.doubleClicks + 1) }
        updateHistoricalStats("double_click")
        prefs.putInt("session_double_clicks", prefs.getInt("session_double_clicks", 0) + 1)
    }

    override suspend fun recordRightClick() {
        updateSessionStats { it.copy(totalRightClicks = it.totalRightClicks + 1) }
        updateDailyStats { it.copy(rightClicks = it.rightClicks + 1) }
        updateHistoricalStats("right_click")
        prefs.putInt("session_right_clicks", prefs.getInt("session_right_clicks", 0) + 1)
    }

    override suspend fun recordScroll(delta: Int) {
        updateSessionStats { it.copy(totalScrolls = it.totalScrolls + 1) }
        updateDailyStats { it.copy(scrolls = it.scrolls + 1) }
        updateHistoricalStats("scroll")
        prefs.putInt("session_scrolls", prefs.getInt("session_scrolls", 0) + 1)
    }

    override suspend fun recordMovement(distance: Float, duration: Long) {
        val speed = if (duration > 0) distance / duration * 1000 else 0f
        val currentStats = _sessionStats.value
        val newMovements = currentStats.totalMovements + 1
        val newDistance = currentStats.totalDistance + distance
        val newAvgSpeed = if (newMovements > 0) {
            (currentStats.averageSpeed * currentStats.totalMovements + speed) / newMovements
        } else 0f
        val newMaxSpeed = maxOf(currentStats.maxSpeed, speed)

        updateSessionStats {
            it.copy(
                totalMovements = newMovements,
                totalDistance = newDistance,
                averageSpeed = newAvgSpeed,
                maxSpeed = newMaxSpeed
            )
        }

        updateDailyStats { daily ->
            daily.copy(
                movements = daily.movements + 1,
                distance = daily.distance + distance
            )
        }

        prefs.putInt("session_movements", prefs.getInt("session_movements", 0) + 1)
        prefs.putFloat("session_distance", prefs.getFloat("session_distance", 0f) + distance)
        prefs.putFloat("session_avg_speed", newAvgSpeed)
        prefs.putFloat("session_max_speed", newMaxSpeed)
    }

    override suspend fun recordGesture(gesture: String, confidence: Float) {
        updateHistoricalStats(gesture)

        val currentStats = _gestureStats.value.toMutableList()
        val existing = currentStats.find { it.gestureName == gesture }
        if (existing != null) {
            val index = currentStats.indexOf(existing)
            val newCount = existing.detectionCount + 1
            val newConfidence = (existing.confidencePercentage * existing.detectionCount + confidence) / newCount
            currentStats[index] = existing.copy(
                detectionCount = newCount,
                confidencePercentage = newConfidence,
                lastDetected = System.currentTimeMillis()
            )
        } else {
            currentStats.add(GestureStatistics(
                gestureName = gesture,
                detectionCount = 1,
                confidencePercentage = confidence,
                lastDetected = System.currentTimeMillis()
            ))
        }
        _gestureStats.value = currentStats
        saveGestureStats()
    }

    override suspend fun recordConnectionAttempt(success: Boolean, latencyMs: Long) {
        val totalAttempts = prefs.getInt("connection_attempts", 0) + 1
        prefs.putInt("connection_attempts", totalAttempts)

        if (success) {
            val successful = prefs.getInt("connection_successful", 0) + 1
            prefs.putInt("connection_successful", successful)
            val totalLatency = prefs.getLong("connection_total_latency", 0) + latencyMs
            prefs.putLong("connection_total_latency", totalLatency)
        } else {
            val failed = prefs.getInt("connection_failed", 0) + 1
            prefs.putInt("connection_failed", failed)
        }
    }

    override suspend fun resetStats() {
        resetSession()
        prefs.remove("session_clicks")
        prefs.remove("session_double_clicks")
        prefs.remove("session_right_clicks")
        prefs.remove("session_scrolls")
        prefs.remove("session_movements")
        prefs.remove("session_distance")
        prefs.remove("session_avg_speed")
        prefs.remove("session_max_speed")
        prefs.remove("historical_stats")
        prefs.remove("gesture_stats")
        prefs.remove("connection_attempts")
        prefs.remove("connection_successful")
        prefs.remove("connection_failed")
        prefs.remove("connection_total_latency")
        _gestureStats.value = emptyList()
        _historicalStats.value = HistoricalStatistics()
        loadSessionStats()
    }

    override suspend fun resetSession() {
        _sessionStats.value = StatisticsSummary()
        prefs.remove("session_clicks")
        prefs.remove("session_double_clicks")
        prefs.remove("session_right_clicks")
        prefs.remove("session_scrolls")
        prefs.remove("session_movements")
        prefs.remove("session_distance")
        prefs.remove("session_avg_speed")
        prefs.remove("session_max_speed")
        prefs.putLong("session_start", System.currentTimeMillis())
    }

    override suspend fun exportStats(format: String): String {
        return when (format.lowercase()) {
            "json" -> exportJson()
            "csv" -> exportCsv()
            else -> exportJson()
        }
    }

    private suspend fun updateSessionStats(update: (StatisticsSummary) -> StatisticsSummary) {
        _sessionStats.value = update(_sessionStats.value)
    }

    private suspend fun updateDailyStats(update: (DailyStats) -> DailyStats) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val current = getDailyStats(date)
        val updated = update(current)
        val key = "daily_stats_$date"
        val obj = JSONObject()
        obj.put("clicks", updated.clicks)
        obj.put("doubleClicks", updated.doubleClicks)
        obj.put("rightClicks", updated.rightClicks)
        obj.put("scrolls", updated.scrolls)
        obj.put("movements", updated.movements)
        obj.put("distance", updated.distance)
        prefs.putString(key, obj.toString())
    }

    private suspend fun updateHistoricalStats(gesture: String) {
        val current = _historicalStats.value
        val byType = current.gesturesByType.toMutableMap()
        byType[gesture] = (byType[gesture] ?: 0) + 1

        val mostUsed = byType.maxByOrNull { it.value }?.key ?: gesture

        _historicalStats.value = current.copy(
            totalGestures = current.totalGestures + 1,
            gesturesByType = byType,
            mostUsedGesture = mostUsed,
            lastGestureTime = System.currentTimeMillis()
        )
        saveHistoricalStats()
    }

    private fun saveHistoricalStats() {
        val obj = JSONObject()
        obj.put("totalGestures", _historicalStats.value.totalGestures)
        obj.put("mostUsedGesture", _historicalStats.value.mostUsedGesture)
        obj.put("lastGestureTime", _historicalStats.value.lastGestureTime)

        val byType = JSONObject()
        _historicalStats.value.gesturesByType.forEach { (key, value) ->
            byType.put(key, value)
        }
        obj.put("gesturesByType", byType)

        prefs.putString("historical_stats", obj.toString())
    }

    private fun saveGestureStats() {
        val array = JSONArray()
        _gestureStats.value.forEach { stats ->
            val obj = JSONObject()
            obj.put("gestureName", stats.gestureName)
            obj.put("detectionCount", stats.detectionCount)
            obj.put("confidencePercentage", stats.confidencePercentage)
            obj.put("lastDetected", stats.lastDetected)
            array.put(obj)
        }
        prefs.putString("gesture_stats", array.toString())
    }

    private suspend fun saveSessionToHistory() {
        val session = _sessionStats.value
        val totalGestures = session.totalClicks + session.totalDoubleClicks + session.totalRightClicks + session.totalScrolls
        val current = _historicalStats.value
        _historicalStats.value = current.copy(
            totalGestures = current.totalGestures + totalGestures
        )
        saveHistoricalStats()
    }

    private fun exportJson(): String {
        val obj = JSONObject()
        obj.put("session", JSONObject().apply {
            put("totalClicks", _sessionStats.value.totalClicks)
            put("totalMovements", _sessionStats.value.totalMovements)
            put("totalDistance", _sessionStats.value.totalDistance)
            put("averageSpeed", _sessionStats.value.averageSpeed)
        })
        obj.put("historical", JSONObject().apply {
            put("totalGestures", _historicalStats.value.totalGestures)
            put("mostUsedGesture", _historicalStats.value.mostUsedGesture)
        })
        obj.put("gestures", JSONArray().apply {
            _gestureStats.value.forEach { stats ->
                put(JSONObject().apply {
                    put("gestureName", stats.gestureName)
                    put("detectionCount", stats.detectionCount)
                    put("confidencePercentage", stats.confidencePercentage)
                })
            }
        })
        return obj.toString()
    }

    private fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append("Metric,Value\n")
        sb.append("Total Clicks,${_sessionStats.value.totalClicks}\n")
        sb.append("Total Movements,${_sessionStats.value.totalMovements}\n")
        sb.append("Total Distance,${_sessionStats.value.totalDistance}\n")
        sb.append("Average Speed,${_sessionStats.value.averageSpeed}\n")
        sb.append("Total Gestures,${_historicalStats.value.totalGestures}\n")
        sb.append("Most Used Gesture,${_historicalStats.value.mostUsedGesture}\n")
        sb.append("\nGesture Statistics\n")
        sb.append("Gesture,Detections,Confidence\n")
        _gestureStats.value.forEach { stats ->
            sb.append("${stats.gestureName},${stats.detectionCount},${stats.confidencePercentage}\n")
        }
        return sb.toString()
    }
}