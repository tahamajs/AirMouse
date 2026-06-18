package com.airmouse.data.repository

import com.airmouse.domain.model.DailyStatistics
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.SessionStatistics
import com.airmouse.domain.repository.IStatisticsRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _currentSession = MutableStateFlow(SessionStatistics.newSession())
    override fun getCurrentSession(): Flow<SessionStatistics> = _currentSession.asStateFlow()

    private val _historicalStats = MutableStateFlow(HistoricalStatistics())
    override fun getHistoricalStats(): Flow<HistoricalStatistics> = _historicalStats.asStateFlow()

    private val _gestureStats = MutableStateFlow<List<GestureStatistics>>(emptyList())
    override fun getGestureStats(): Flow<List<GestureStatistics>> = _gestureStats.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    override fun isTracking(): Flow<Boolean> = _isTracking.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadHistoricalStats()
        loadGestureStats()
    }

    private fun loadHistoricalStats() {
        val json = prefs.getString("historical_stats", "")
        if (json.isNotEmpty()) {
            try {
                val obj = JSONObject(json)
                val dailyArray = obj.getJSONArray("daily")
                val dailyStats = mutableListOf<DailyStatistics>()
                for (i in 0 until dailyArray.length()) {
                    val d = dailyArray.getJSONObject(i)
                    dailyStats.add(DailyStatistics(
                        date = d.getString("date"),
                        totalClicks = d.getInt("clicks"),
                        totalGestures = d.getInt("gestures"),
                        totalDistance = d.getDouble("distance").toFloat(),
                        activeTime = d.getLong("activeTime"),
                        connectionCount = d.getInt("connections")
                    ))
                }
                _historicalStats.value = HistoricalStatistics(
                    dailyStats = dailyStats.sortedBy { it.date },
                    weeklyStats = dailyStats.takeLast(7),
                    monthlyStats = dailyStats.takeLast(30)
                )
            } catch (e: Exception) {
                // Use empty stats
            }
        }
    }

    private fun loadGestureStats() {
        val json = prefs.getString("gesture_stats", "")
        if (json.isNotEmpty()) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<GestureStatistics>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(GestureStatistics(
                        gestureName = obj.getString("name"),
                        detectionCount = obj.getInt("count"),
                        averageConfidence = obj.getDouble("confidence").toFloat(),
                        lastDetected = obj.getLong("lastDetected"),
                        isCustom = obj.optBoolean("isCustom", false)
                    ))
                }
                _gestureStats.value = list
            } catch (e: Exception) {
                // Use empty list
            }
        }
    }

    private fun saveHistoricalStats() {
        val obj = JSONObject()
        val dailyArray = JSONArray()
        _historicalStats.value.dailyStats.forEach { d ->
            val day = JSONObject().apply {
                put("date", d.date)
                put("clicks", d.totalClicks)
                put("gestures", d.totalGestures)
                put("distance", d.totalDistance)
                put("activeTime", d.activeTime)
                put("connections", d.connectionCount)
            }
            dailyArray.put(day)
        }
        obj.put("daily", dailyArray)
        prefs.putString("historical_stats", obj.toString())
    }

    private fun saveGestureStats() {
        val array = JSONArray()
        _gestureStats.value.forEach { g ->
            val obj = JSONObject().apply {
                put("name", g.gestureName)
                put("count", g.detectionCount)
                put("confidence", g.averageConfidence)
                put("lastDetected", g.lastDetected)
                put("isCustom", g.isCustom)
            }
            array.put(obj)
        }
        prefs.putString("gesture_stats", array.toString())
    }

    override suspend fun startTracking() {
        _isTracking.value = true
        _currentSession.value = SessionStatistics.newSession()
    }

    override suspend fun stopTracking() {
        _isTracking.value = false
        val session = _currentSession.value
        if (session.endTime == 0L) {
            _currentSession.value = session.copy(endTime = System.currentTimeMillis())
            updateDailyStats(session)
        }
    }

    override suspend fun recordClick() {
        if (!_isTracking.value) return
        _currentSession.update { it.copy(clicks = it.clicks + 1) }
    }

    override suspend fun recordDoubleClick() {
        if (!_isTracking.value) return
        _currentSession.update { it.copy(doubleClicks = it.doubleClicks + 1) }
    }

    override suspend fun recordRightClick() {
        if (!_isTracking.value) return
        _currentSession.update { it.copy(rightClicks = it.rightClicks + 1) }
    }

    override suspend fun recordScroll() {
        if (!_isTracking.value) return
        _currentSession.update { it.copy(scrolls = it.scrolls + 1) }
    }

    override suspend fun recordGesture(name: String, confidence: Float) {
        if (!_isTracking.value) return
        _currentSession.update { it.copy(gestures = it.gestures + name) }

        // Update gesture stats
        val existing = _gestureStats.value.find { it.gestureName == name }
        if (existing != null) {
            val updated = existing.copy(
                detectionCount = existing.detectionCount + 1,
                averageConfidence = (existing.averageConfidence * existing.detectionCount + confidence) / (existing.detectionCount + 1),
                lastDetected = System.currentTimeMillis()
            )
            _gestureStats.update { list ->
                list.map { if (it.gestureName == name) updated else it }
            }
        } else {
            _gestureStats.update { it + GestureStatistics(
                gestureName = name,
                detectionCount = 1,
                averageConfidence = confidence,
                lastDetected = System.currentTimeMillis()
            ) }
        }
        saveGestureStats()
    }

    override suspend fun recordMovement(distance: Float) {
        if (!_isTracking.value) return
        _currentSession.update { it.copy(totalDistance = it.totalDistance + distance) }
    }

    override suspend fun recordConnectionAttempt(success: Boolean) {
        if (!_isTracking.value) return
        _currentSession.update { session ->
            session.copy(
                connectionAttempts = session.connectionAttempts + 1,
                connectionSuccesses = if (success) session.connectionSuccesses + 1 else session.connectionSuccesses
            )
        }
    }

    override suspend fun getSessionStats(): SessionStatistics = _currentSession.value

    override suspend fun getHistoricalStats(): HistoricalStatistics = _historicalStats.value

    override suspend fun getTodayStats(): DailyStatistics {
        val today = dateFormat.format(Date())
        return _historicalStats.value.dailyStats.find { it.date == today } ?: DailyStatistics.today()
    }

    override suspend fun getWeekStats(): List<DailyStatistics> {
        return _historicalStats.value.weeklyStats
    }

    override suspend fun getMonthStats(): List<DailyStatistics> {
        return _historicalStats.value.monthlyStats
    }

    override suspend fun getGestureStats(): List<GestureStatistics> = _gestureStats.value

    override suspend fun resetStats() {
        _currentSession.value = SessionStatistics.newSession()
        _historicalStats.value = HistoricalStatistics()
        _gestureStats.value = emptyList()
        prefs.putString("historical_stats", "")
        prefs.putString("gesture_stats", "")
    }

    override suspend fun exportStats(): String {
        val stats = _historicalStats.value
        return buildString {
            appendLine("AIRMOUSE_STATS_EXPORT")
            appendLine("export_time=${System.currentTimeMillis()}")
            appendLine("total_clicks=${stats.totalClicks}")
            appendLine("total_gestures=${stats.totalGestures}")
            appendLine("total_distance=${stats.totalDistance}")
            appendLine("active_days=${stats.activeDays}")
            appendLine("average_daily_clicks=${stats.averageDailyClicks}")
            appendLine("---DAILY---")
            stats.dailyStats.forEach { day ->
                appendLine("${day.date}: clicks=${day.totalClicks}, gestures=${day.totalGestures}, distance=${day.totalDistance}")
            }
            appendLine("---GESTURES---")
            _gestureStats.value.forEach { g ->
                appendLine("${g.gestureName}: ${g.detectionCount} (${g.confidencePercentage}%)")
            }
        }
    }

    private suspend fun updateDailyStats(session: SessionStatistics) {
        val today = dateFormat.format(Date())
        val currentDaily = _historicalStats.value.dailyStats.find { it.date == today }

        val updatedDaily = if (currentDaily != null) {
            currentDaily.copy(
                totalClicks = currentDaily.totalClicks + session.totalClicks,
                totalGestures = currentDaily.totalGestures + session.gestureCount,
                totalDistance = currentDaily.totalDistance + session.totalDistance,
                activeTime = currentDaily.activeTime + session.duration,
                connectionCount = currentDaily.connectionCount + session.connectionAttempts
            )
        } else {
            DailyStatistics(
                date = today,
                totalClicks = session.totalClicks,
                totalGestures = session.gestureCount,
                totalDistance = session.totalDistance,
                activeTime = session.duration,
                connectionCount = session.connectionAttempts
            )
        }

        val dailyList = _historicalStats.value.dailyStats.toMutableList()
        val index = dailyList.indexOfFirst { it.date == today }
        if (index >= 0) {
            dailyList[index] = updatedDaily
        } else {
            dailyList.add(updatedDaily)
        }

        _historicalStats.value = HistoricalStatistics(
            dailyStats = dailyList.sortedBy { it.date },
            weeklyStats = dailyList.takeLast(7),
            monthlyStats = dailyList.takeLast(30)
        )
        saveHistoricalStats()
    }
}