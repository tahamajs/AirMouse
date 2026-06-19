// app/src/main/java/com/airmouse/features/StatisticsFeature.kt
package com.airmouse.features

import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.model.HistoricalStatistics
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.domain.usecase.GetStatisticsUseCase
import com.airmouse.domain.usecase.RecordStatisticsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsFeature @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val recordStatisticsUseCase: RecordStatisticsUseCase
) {

    data class StatisticsFeatureState(
        val sessionStats: StatisticsSummary = StatisticsSummary(),
        val historicalStats: HistoricalStatistics = HistoricalStatistics(),
        val todayStats: DailyStats = DailyStats(),
        val gestureStats: List<GestureStatistics> = emptyList(),
        val isTracking: Boolean = false,
        val sessionDuration: Long = 0,
        val totalSessions: Int = 0
    )

    private val _state = MutableStateFlow(StatisticsFeatureState())
    val state: StateFlow<StatisticsFeatureState> = _state.asStateFlow()

    init {
        observeStats()
    }

    private fun observeStats() {
        // Observe statistics
    }

    suspend fun getSessionStats(): StatisticsSummary {
        return getStatisticsUseCase()
    }

    fun observeSessionStats(): Flow<StatisticsSummary> {
        return getStatisticsUseCase.observeSessionStats()
    }

    suspend fun getHistoricalStats(): HistoricalStatistics {
        return getStatisticsUseCase.getHistoricalStats()
    }

    suspend fun getTodayStats(): DailyStats {
        return getStatisticsUseCase.getTodayStats()
    }

    suspend fun getWeekStats(): List<DailyStats> {
        return getStatisticsUseCase.getWeekStats()
    }

    suspend fun getMonthStats(): List<DailyStats> {
        return getStatisticsUseCase.getMonthStats()
    }

    suspend fun getGestureStats(): List<GestureStatistics> {
        return getStatisticsUseCase.getGestureStats()
    }

    fun observeGestureStats(): Flow<List<GestureStatistics>> {
        return getStatisticsUseCase.observeGestureStats()
    }

    suspend fun startTracking(): Result<Unit> {
        val result = getStatisticsUseCase.startTracking()
        if (result.isSuccess) {
            _state.value = _state.value.copy(isTracking = true)
        }
        return result
    }

    suspend fun stopTracking(): Result<Unit> {
        val result = getStatisticsUseCase.stopTracking()
        if (result.isSuccess) {
            _state.value = _state.value.copy(isTracking = false)
        }
        return result
    }

    suspend fun resetStats(): Result<Unit> {
        val result = getStatisticsUseCase.resetStats()
        if (result.isSuccess) {
            _state.value = StatisticsFeatureState()
        }
        return result
    }

    suspend fun exportStats(format: String = "json"): Result<String> {
        return getStatisticsUseCase.exportStats(format)
    }

    suspend fun recordClick(): Result<Unit> {
        val result = recordStatisticsUseCase.recordClick()
        if (result.isSuccess) {
            refreshStats()
        }
        return result
    }

    suspend fun recordDoubleClick(): Result<Unit> {
        val result = recordStatisticsUseCase.recordDoubleClick()
        if (result.isSuccess) {
            refreshStats()
        }
        return result
    }

    suspend fun recordRightClick(): Result<Unit> {
        val result = recordStatisticsUseCase.recordRightClick()
        if (result.isSuccess) {
            refreshStats()
        }
        return result
    }

    suspend fun recordScroll(delta: Int): Result<Unit> {
        val result = recordStatisticsUseCase.recordScroll(delta)
        if (result.isSuccess) {
            refreshStats()
        }
        return result
    }

    suspend fun recordMovement(distance: Float, duration: Long): Result<Unit> {
        val result = recordStatisticsUseCase.recordMovement(distance, duration)
        if (result.isSuccess) {
            refreshStats()
        }
        return result
    }

    suspend fun recordGesture(gesture: String, confidence: Float): Result<Unit> {
        val result = recordStatisticsUseCase.recordGesture(gesture, confidence)
        if (result.isSuccess) {
            refreshStats()
        }
        return result
    }

    suspend fun isTracking(): Boolean {
        return getStatisticsUseCase.isTracking()
    }

    private suspend fun refreshStats() {
        val sessionStats = getStatisticsUseCase()
        val historicalStats = getStatisticsUseCase.getHistoricalStats()
        val todayStats = getStatisticsUseCase.getTodayStats()
        val gestureStats = getStatisticsUseCase.getGestureStats()

        _state.value = _state.value.copy(
            sessionStats = sessionStats,
            historicalStats = historicalStats,
            todayStats = todayStats,
            gestureStats = gestureStats,
            sessionDuration = sessionStats.sessionDuration
        )
    }

    suspend fun getSessionDuration(): Long {
        return getStatisticsUseCase.getSessionStats().sessionDuration
    }

    suspend fun getTotalClicks(): Int {
        return getStatisticsUseCase.getSessionStats().totalClicks
    }

    suspend fun getTotalMovements(): Int {
        return getStatisticsUseCase.getSessionStats().totalMovements
    }

    suspend fun getAverageSpeed(): Float {
        return getStatisticsUseCase.getSessionStats().averageSpeed
    }

    fun getStatisticsFeatureState(): StatisticsFeatureState = _state.value
}