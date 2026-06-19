// app/src/main/java/com/airmouse/data/repository/StatisticsRepositoryImpl.kt
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
            // Parse gestures by type
            val byTypeJson = obj.optJSONObject("gesturesByType")
            if (byTypeJson != null) {
                val byType = mutableMapOf<String, Int>()
                byTypeJson.keys().forEach { key ->
                    byType[key] = byTypeJson.getInt(key)
                }
                _historicalStats.value = _historicalStats.value.copy(gesturesByType = byType)
            }
        } catch (e: Exception) {
            // Use defaults
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
            // Use defaults
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
        // Save session stats to historical
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
                clicks = obj.optInt("clicks",