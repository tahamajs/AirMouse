package com.airmouse.presentation.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.AppPreferences
import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.domain.usecase.GetStatisticsUseCase
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsUseCase: GetStatisticsUseCase,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        observeLiveStats()
        refreshData()
    }

    fun handleEvent(event: StatisticsScreenEvent) {
        when (event) {
            is StatisticsScreenEvent.LoadData -> refreshData()
            is StatisticsScreenEvent.RefreshData -> refreshData()
            is StatisticsScreenEvent.SelectTimeRange -> {
                _uiState.update { it.copy(timeRange = event.timeRange, isLoading = true) }
                refreshData()
            }
            is StatisticsScreenEvent.SelectChart -> {
                _uiState.update { it.copy(chartType = event.chartType) }
            }
            is StatisticsScreenEvent.ShowExportDialog -> {
                _uiState.update { it.copy(showExportDialog = true) }
            }
            is StatisticsScreenEvent.DismissExportDialog -> {
                _uiState.update { it.copy(showExportDialog = false) }
            }
            is StatisticsScreenEvent.ExportData -> {
                exportStatistics()
            }
            is StatisticsScreenEvent.ShowResetDialog -> {
                _uiState.update { it.copy(showResetDialog = true) }
            }
            is StatisticsScreenEvent.DismissResetDialog -> {
                _uiState.update { it.copy(showResetDialog = false) }
            }
            is StatisticsScreenEvent.ConfirmReset -> {
                resetStatistics()
            }
            is StatisticsScreenEvent.DismissError -> {
                _uiState.update { it.copy(error = null) }
            }
            is StatisticsScreenEvent.DismissSuccess -> {
                _uiState.update { it.copy(success = null) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                val selectedRange = _uiState.value.timeRange
                val sessionStats = statisticsUseCase.getSessionStats()
                val historicalStats = statisticsUseCase.getHistoricalStats()
                val gestureStats = statisticsUseCase.getGestureStats()
                val dailyStats = loadDailyStats(selectedRange)
                val isTracking = statisticsUseCase.isTracking()

                val connectionAttempts = prefs.getInt("connection_attempts", 0)
                val successfulConnections = prefs.getInt("connection_successful", 0)
                val failedConnections = prefs.getInt("connection_failed", 0)
                val totalLatency = prefs.getLong("connection_total_latency", 0L)
                val averagePing = if (successfulConnections > 0) {
                    (totalLatency / successfulConnections).toInt()
                } else {
                    0
                }
                val calibrationComplete = prefs.getBoolean("calibration_complete", false) ||
                        prefs.getBoolean("is_calibrated", false)
                val lastCalibrationTime = prefs.getLong("last_calibration_time", 0L)
                val calibrationCount = prefs.getInt("calibration_count", if (calibrationComplete) 1 else 0)
                val currentTime = System.currentTimeMillis()
                val touchpadActive = prefs.getBoolean("touchpad_active", false)
                val presentationModeEnabled = prefs.getBoolean("presentation_mode_enabled", false)
                val autoConnect = prefs.getBoolean("auto_connect", true)
                val useWebSocket = prefs.getBoolean("use_websocket", true)
                val useUdpDiscovery = prefs.getBoolean("use_udp_discovery", true)
                val theme = prefs.getString("theme", "system") ?: "system"
                val language = prefs.getLanguage()
                val mostUsed = historicalStats.mostUsedGesture
                val mostUsedCount = historicalStats.gesturesByType[mostUsed] ?: 0
                val gestureTypeCount = historicalStats.gesturesByType.size
                val customGestureCount = historicalStats.customGestureUsage.values.sum()

                _uiState.update { current ->
                    current.copy(
                        sessionTime = (sessionStats.sessionDuration / 1000L).coerceAtLeast(0),
                        sessionStartTime = currentTime - sessionStats.sessionDuration.coerceAtLeast(0L),
                        lastActivityTime = historicalStats.lastGestureTime.takeIf { it > 0 } ?: currentTime,
                        clicks = sessionStats.totalClicks,
                        doubleClicks = sessionStats.totalDoubleClicks,
                        rightClicks = sessionStats.totalRightClicks,
                        scrolls = sessionStats.totalScrolls,
                        gesturesDetected = historicalStats.totalGestures,
                        customGesturesUsed = gestureStats.sumOf { it.detectionCount },
                        totalDistanceMoved = sessionStats.totalDistance,
                        averageSpeed = sessionStats.averageSpeed,
                        peakSpeed = sessionStats.maxSpeed,
                        totalMovements = sessionStats.totalMovements,
                        connectionAttempts = connectionAttempts,
                        successfulConnections = successfulConnections,
                        failedConnections = failedConnections,
                        averagePing = averagePing,
                        lastCalibrationTime = lastCalibrationTime,
                        calibrationCount = calibrationCount,
                        calibrationSuccessRate = if (calibrationComplete) 100f else 0f,
                        isTracking = isTracking,
                        calibrationComplete = calibrationComplete,
                        touchpadActive = touchpadActive,
                        presentationModeEnabled = presentationModeEnabled,
                        autoConnect = autoConnect,
                        useWebSocket = useWebSocket,
                        useUdpDiscovery = useUdpDiscovery,
                        theme = theme,
                        language = language,
                        batteryUsage = prefs.getInt("battery_usage", 0),
                        cpuUsage = prefs.getFloat("cpu_usage", 0f),
                        memoryUsage = prefs.getFloat("memory_usage", 0f),
                        temperature = prefs.getFloat("temperature", 0f),
                        mostUsedGesture = mostUsed,
                        mostUsedGestureCount = mostUsedCount,
                        gestureTypeCount = gestureTypeCount,
                        customGestureCount = customGestureCount,
                        dailyStats = dailyStats,
                        gestureBreakdown = historicalStats.gesturesByType,
                        summaryStats = sessionStats,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load statistics"
                    )
                }
            }
        }
    }

    fun updateTimeRange(timeRange: TimeRange) {
        _uiState.update { it.copy(timeRange = timeRange, isLoading = true) }
        refreshData()
    }

    fun showExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun showResetDialog(show: Boolean) {
        _uiState.update { it.copy(showResetDialog = show) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, success = null) }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            try {
                statisticsUseCase.resetStats()
                _uiState.value = createInitialState()
                _uiState.update { it.copy(showResetDialog = false, success = "Statistics reset successfully") }
                refreshData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showResetDialog = false,
                        error = e.message ?: "Reset failed"
                    )
                }
            }
        }
    }

    fun exportStatistics() {
        viewModelScope.launch {
            try {
                val format = when (_uiState.value.timeRange) {
                    TimeRange.TODAY -> "json"
                    TimeRange.WEEK -> "csv"
                    TimeRange.MONTH -> "json"
                    TimeRange.YEAR -> "csv"
                    TimeRange.ALL_TIME -> "json"
                }
                val result = statisticsUseCase.exportStats(format)
                _uiState.update {
                    it.copy(
                        showExportDialog = false,
                        success = "Statistics exported successfully ($format)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showExportDialog = false,
                        error = e.message ?: "Export failed"
                    )
                }
            }
        }
    }

    private fun observeLiveStats() {
        viewModelScope.launch {
            statisticsUseCase.observeSessionStats().collect { sessionStats ->
                _uiState.update {
                    it.copy(
                        sessionTime = (sessionStats.sessionDuration / 1000L).coerceAtLeast(0),
                        clicks = sessionStats.totalClicks,
                        doubleClicks = sessionStats.totalDoubleClicks,
                        rightClicks = sessionStats.totalRightClicks,
                        scrolls = sessionStats.totalScrolls,
                        totalDistanceMoved = sessionStats.totalDistance,
                        averageSpeed = sessionStats.averageSpeed,
                        peakSpeed = sessionStats.maxSpeed,
                        totalMovements = sessionStats.totalMovements,
                        summaryStats = sessionStats,
                        lastActivityTime = System.currentTimeMillis()
                    )
                }
            }
        }

        viewModelScope.launch {
            statisticsUseCase.observeGestureStats().collect { gestureStats ->
                val totalCustom = gestureStats.sumOf { it.detectionCount }
                val breakdown = gestureStats.associate { it.gestureName to it.detectionCount }
                val mostUsed = gestureStats.maxByOrNull { it.detectionCount }
                _uiState.update { current ->
                    current.copy(
                        customGesturesUsed = totalCustom,
                        gestureBreakdown = breakdown,
                        mostUsedGesture = mostUsed?.gestureName.orEmpty(),
                        mostUsedGestureCount = mostUsed?.detectionCount ?: 0,
                        gestureTypeCount = gestureStats.size,
                        customGestureCount = totalCustom
                    )
                }
            }
        }
    }

    private fun createInitialState(): StatisticsUiState {
        val now = System.currentTimeMillis()
        val summary = StatisticsSummary()
        return StatisticsUiState(
            sessionStartTime = now,
            lastActivityTime = now,
            dailyStats = listOf(
                DailyStats( // This expects date: String, but we're passing Date? – need to adjust
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now)),
                    clicks = 0,
                    doubleClicks = 0,
                    rightClicks = 0,
                    scrolls = 0,
                    movements = 0,
                    distance = 0f
                )
            ),
            gestureBreakdown = emptyMap(),
            gestureTypeCount = 0,
            summaryStats = summary,
            mouseStatistics = MouseStatistics(),
            appPreferences = AppPreferences()
        )
    }

    private suspend fun loadDailyStats(timeRange: TimeRange): List<DailyStats> {
        return when (timeRange) {
            TimeRange.TODAY -> listOf(statisticsUseCase.getTodayStats())
            TimeRange.WEEK -> statisticsUseCase.getWeekStats()
            TimeRange.MONTH -> statisticsUseCase.getMonthStats()
            TimeRange.YEAR, TimeRange.ALL_TIME -> statisticsUseCase.getMonthStats()
        }
    }

    // No need for mapDailyStats anymore because we're using the domain model directly.
}
