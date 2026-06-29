package com.airmouse.data.repository

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.domain.repository.IStatisticsRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IStatisticsRepository {

    companion object {
        private const val SESSION_STATS_KEY = "stat_session_data"
        private const val HISTORICAL_STATS_KEY = "stat_historical_data"
        private const val GESTURE_STATS_KEY = "stat_gesture_data"
        private const val DAILY_STATS_PREFIX = "stat_daily_"
        private const val MAX_HISTORY_SIZE = 1000
    }

    // ============================================================
    // State Flows
    // ============================================================

    private val _sessionStats = MutableStateFlow<StatisticsSummary>(loadSessionStats())
    override fun observeCurrentSession(): Flow<StatisticsSummary> = _sessionStats.asStateFlow()

    private val _historicalStats = MutableStateFlow<HistoricalStatistics>(loadHistoricalStats())
    override fun observeHistoricalStats(): Flow<HistoricalStatistics> = _historicalStats.asStateFlow()

    private val _gestureStats = MutableStateFlow<List<GestureStatistics>>(loadGestureStats())
    override fun observeGestureStats(): Flow<List<GestureStatistics>> = _gestureStats.asStateFlow()

    private var isTracking = false

    // ============================================================
    // Record Operations
    // ============================================================

    override suspend fun recordClick() {
        updateSessionStats { stats ->
            stats.copy(
                totalClicks = stats.totalClicks + 1,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updateDailyStats { it.copy(clicks = it.clicks + 1) }
        Timber.d("Recorded click")
    }

    override suspend fun recordDoubleClick() {
        updateSessionStats { stats ->
            stats.copy(
                totalDoubleClicks = stats.totalDoubleClicks + 1,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updateDailyStats { it.copy(doubleClicks = it.doubleClicks + 1) }
        Timber.d("Recorded double click")
    }

    override suspend fun recordRightClick() {
        updateSessionStats { stats ->
            stats.copy(
                totalRightClicks = stats.totalRightClicks + 1,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updateDailyStats { it.copy(rightClicks = it.rightClicks + 1) }
        Timber.d("Recorded right click")
    }

    override suspend fun recordScroll(delta: Int) {
        updateSessionStats { stats ->
            stats.copy(
                totalScrolls = stats.totalScrolls + 1,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updateDailyStats { it.copy(scrolls = it.scrolls + 1) }
        Timber.d("Recorded scroll: $delta")
    }

    override suspend fun recordMovement(distance: Float, duration: Long) {
        val speed = if (duration > 0) distance / duration * 1000 else 0f
        updateSessionStats { stats ->
            stats.copy(
                totalMovements = stats.totalMovements + 1,
                totalDistance = stats.totalDistance + distance,
                averageSpeed = if (stats.totalMovements > 0) {
                    (stats.averageSpeed * stats.totalMovements + speed) / (stats.totalMovements + 1)
                } else speed,
                maxSpeed = maxOf(stats.maxSpeed, speed),
                lastUpdated = System.currentTimeMillis()
            )
        }
        updateDailyStats { it.copy(movements = it.movements + 1, distance = it.distance + distance) }
        Timber.d("Recorded movement: distance=$distance, speed=$speed")
    }

    override suspend fun recordGesture(gesture: String, confidence: Float) {
        updateSessionStats { stats ->
            stats.copy(
                lastUpdated = System.currentTimeMillis()
            )
        }

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
            currentStats.add(
                GestureStatistics(
                    gestureName = gesture,
                    detectionCount = 1,
                    confidencePercentage = confidence,
                    lastDetected = System.currentTimeMillis()
                )
            )
        }
        _gestureStats.value = currentStats
        saveGestureStats(currentStats)

        updateDailyStats { it.copy(gestures = it.gestures + 1) }
        Timber.d("Recorded gesture: $gesture (confidence: $confidence)")
    }

    override suspend fun recordConnectionAttempt(success: Boolean, latencyMs: Long) {
        // Optional: implement connection attempt tracking
        Timber.d("Recorded connection attempt: success=$success, latency=$latencyMs")
    }

    // ============================================================
    // Session Management
    // ============================================================

    override suspend fun getCurrentSession(): StatisticsSummary = _sessionStats.value

    override suspend fun startTracking() {
        isTracking = true
        _sessionStats.value = StatisticsSummary(lastUpdated = System.currentTimeMillis())
        saveSessionStats(_sessionStats.value)
        Timber.d("Statistics tracking started")
    }

    override suspend fun stopTracking() {
        isTracking = false
        saveSessionStats(_sessionStats.value)
        Timber.d("Statistics tracking stopped")
    }

    override suspend fun isTracking(): Boolean = isTracking

    // ============================================================
    // Daily Stats
    // ============================================================

    override suspend fun getDailyStats(date: String): DailyStats {
        return loadDailyStats(date) ?: DailyStats(date = date)
    }

    override suspend fun getTodayStats(): DailyStats {
        val today = getTodayDate()
        return getDailyStats(today)
    }

    override suspend fun getWeekStats(): List<DailyStats> {
        val stats = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            stats.add(getDailyStats(date))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return stats
    }

    override suspend fun getMonthStats(): List<DailyStats> {
        val stats = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()
        for (i in 29 downTo 0) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            stats.add(getDailyStats(date))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return stats
    }

    // ============================================================
    // Historical Stats
    // ============================================================

    override suspend fun getHistoricalStats(): HistoricalStatistics {
        return _historicalStats.value
    }

    // ============================================================
    // Gesture Stats
    // ============================================================

    override suspend fun getGestureStats(): List<GestureStatistics> {
        return _gestureStats.value
    }

    // ============================================================
    // Reset Operations
    // ============================================================

    override suspend fun resetStats() {
        _sessionStats.value = StatisticsSummary()
        _historicalStats.value = HistoricalStatistics()
        _gestureStats.value = emptyList()
        saveSessionStats(_sessionStats.value)
        saveHistoricalStats(_historicalStats.value)
        saveGestureStats(emptyList())
        Timber.d("All statistics reset")
    }

    override suspend fun resetSession() {
        _sessionStats.value = StatisticsSummary()
        saveSessionStats(_sessionStats.value)
        Timber.d("Session statistics reset")
    }

    // ============================================================
    // Export
    // ============================================================

    override suspend fun exportStats(format: String): String {
        return when (format.lowercase()) {
            "json" -> exportJson()
            "csv" -> exportCsv()
            else -> exportJson()
        }
    }

    // ============================================================
    // Private Helpers
    // ============================================================

    private suspend fun updateSessionStats(update: (StatisticsSummary) -> StatisticsSummary) {
        val current = _sessionStats.value
        val updated = update(current)
        _sessionStats.value = updated
        saveSessionStats(updated)
    }

    private suspend fun updateDailyStats(update: (DailyStats) -> DailyStats) {
        val today = getTodayDate()
        val current = getDailyStats(today)
        val updated = update(current)
        saveDailyStats(today, updated)
    }

    private fun saveSessionStats(stats: StatisticsSummary) {
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
            put("lastUpdated", stats.lastUpdated)
        }
        prefs.putString(SESSION_STATS_KEY, obj.toString())
    }

    private fun loadSessionStats(): StatisticsSummary {
        val json = prefs.getString(SESSION_STATS_KEY, "{}")
        return runCatching {
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
                sessionDuration = obj.optLong("sessionDuration", 0),
                lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
            )
        }.getOrDefault(StatisticsSummary())
    }

    private fun saveDailyStats(date: String, stats: DailyStats) {
        val key = DAILY_STATS_PREFIX + date
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
            put("lastUpdated", System.currentTimeMillis())
        }
        prefs.putString(key, obj.toString())
    }

    private fun loadDailyStats(date: String): DailyStats? {
        val key = DAILY_STATS_PREFIX + date
        val json = prefs.getString(key, "{}")
        return runCatching {
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
        }.getOrNull()
    }

    private fun saveHistoricalStats(stats: HistoricalStatistics) {
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

        prefs.putString(HISTORICAL_STATS_KEY, obj.toString())
    }

    private fun loadHistoricalStats(): HistoricalStatistics {
        val json = prefs.getString(HISTORICAL_STATS_KEY, "{}")
        return runCatching {
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
        }.getOrDefault(HistoricalStatistics())
    }

    private fun saveGestureStats(stats: List<GestureStatistics>) {
        val array = JSONArray()
        stats.forEach { stat ->
            array.put(JSONObject().apply {
                put("gestureName", stat.gestureName)
                put("detectionCount", stat.detectionCount)
                put("confidencePercentage", stat.confidencePercentage)
                put("lastDetected", stat.lastDetected)
            })
        }
        prefs.putString(GESTURE_STATS_KEY, array.toString())
    }

    private fun loadGestureStats(): List<GestureStatistics> {
        val json = prefs.getString(GESTURE_STATS_KEY, "[]")
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        GestureStatistics(
                            gestureName = obj.getString("gestureName"),
                            detectionCount = obj.getInt("detectionCount"),
                            confidencePercentage = obj.optDouble("confidencePercentage", 0.0).toFloat(),
                            lastDetected = obj.optLong("lastDetected", 0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun exportJson(): String {
        val obj = JSONObject().apply {
            put("session", JSONObject().apply {
                put("totalClicks", _sessionStats.value.totalClicks)
                put("totalDoubleClicks", _sessionStats.value.totalDoubleClicks)
                put("totalRightClicks", _sessionStats.value.totalRightClicks)
                put("totalScrolls", _sessionStats.value.totalScrolls)
                put("totalMovements", _sessionStats.value.totalMovements)
                put("totalDistance", _sessionStats.value.totalDistance)
                put("averageSpeed", _sessionStats.value.averageSpeed)
                put("maxSpeed", _sessionStats.value.maxSpeed)
            })
            put("historical", JSONObject().apply {
                put("totalGestures", _historicalStats.value.totalGestures)
                put("mostUsedGesture", _historicalStats.value.mostUsedGesture)
                put("totalSessions", _historicalStats.value.totalSessions)
            })
            put("gestures", JSONArray().apply {
                _gestureStats.value.forEach { stat ->
                    put(JSONObject().apply {
                        put("gestureName", stat.gestureName)
                        put("detectionCount", stat.detectionCount)
                        put("confidencePercentage", stat.confidencePercentage)
                    })
                }
            })
        }
        return obj.toString()
    }

    private fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append("Metric,Value\n")
        sb.append("Total Clicks,${_sessionStats.value.totalClicks}\n")
        sb.append("Total Double Clicks,${_sessionStats.value.totalDoubleClicks}\n")
        sb.append("Total Right Clicks,${_sessionStats.value.totalRightClicks}\n")
        sb.append("Total Scrolls,${_sessionStats.value.totalScrolls}\n")
        sb.append("Total Movements,${_sessionStats.value.totalMovements}\n")
        sb.append("Total Distance,${_sessionStats.value.totalDistance}\n")
        sb.append("Average Speed,${_sessionStats.value.averageSpeed}\n")
        sb.append("Max Speed,${_sessionStats.value.maxSpeed}\n")
        sb.append("\nGesture Statistics\n")
        sb.append("Gesture,Detections,Confidence\n")
        _gestureStats.value.forEach { stat ->
            sb.append("${stat.gestureName},${stat.detectionCount},${stat.confidencePercentage}\n")
        }
        return sb.toString()
    }
}