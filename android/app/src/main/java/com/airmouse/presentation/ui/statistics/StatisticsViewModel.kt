package com.airmouse.presentation.ui.statistics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var sessionTimerJob: kotlinx.coroutines.Job? = null

    init {
        loadStatistics()
        startSessionTimer()
        startPerformanceMonitoring()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    // Gesture Stats
                    clicks = prefs.getInt("stat_clicks", 0),
                    doubleClicks = prefs.getInt("stat_double_clicks", 0),
                    rightClicks = prefs.getInt("stat_right_clicks", 0),
                    scrolls = prefs.getInt("stat_scrolls", 0),
                    gesturesDetected = prefs.getInt("stat_gestures", 0),
                    customGesturesUsed = prefs.getInt("stat_custom_gestures", 0),

                    // Movement Stats
                    totalDistanceMoved = prefs.getFloat("stat_total_distance", 0f),
                    totalMovements = prefs.getInt("stat_total_movements", 0),
                    peakSpeed = prefs.getFloat("stat_peak_speed", 0f),

                    // Connection Stats
                    connectionAttempts = prefs.getInt("stat_connection_attempts", 0),
                    successfulConnections = prefs.getInt("stat_successful_connections", 0),
                    failedConnections = prefs.getInt("stat_failed_connections", 0),

                    // Calibration Stats
                    lastCalibrationTime = prefs.getLong("stat_last_calibration", 0),
                    calibrationCount = prefs.getInt("stat_calibration_count", 0),

                    // Device Stats
                    batteryUsage = prefs.getInt("stat_battery_usage", 0),

                    // Session Start
                    sessionStartTime = prefs.getLong("stat_session_start", System.currentTimeMillis())
                )
            }
            loadDailyStats()
            loadGestureBreakdown()
            calculateSuccessRates()
        }
    }

    private fun loadDailyStats() {
        val stats = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()

        for (i in 0..29) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = calendar.time

            val dateKey = dateFormat.format(date)
            stats.add(
                DailyStats(
                    date = date,
                    clicks = prefs.getInt("stat_${dateKey}_clicks", 0),
                    doubleClicks = prefs.getInt("stat_${dateKey}_double_clicks", 0),
                    rightClicks = prefs.getInt("stat_${dateKey}_right_clicks", 0),
                    scrolls = prefs.getInt("stat_${dateKey}_scrolls", 0),
                    distance = prefs.getFloat("stat_${dateKey}_distance", 0f)
                )
            )
        }

        _uiState.update { it.copy(dailyStats = stats.reversed()) }
    }

    private fun loadGestureBreakdown() {
        val breakdown = mutableMapOf<String, Int>()
        val gestures = listOf("Click", "Double Click", "Right Click", "Scroll")
        breakdown["Click"] = _uiState.value.clicks
        breakdown["Double Click"] = _uiState.value.doubleClicks
        breakdown["Right Click"] = _uiState.value.rightClicks
        breakdown["Scroll"] = _uiState.value.scrolls

        _uiState.update { it.copy(gestureBreakdown = breakdown) }
    }

    private fun calculateSuccessRates() {
        val total = _uiState.value.connectionAttempts
        val successful = _uiState.value.successfulConnections
        val successRate = if (total > 0) (successful.toFloat() / total) * 100 else 0f

        val calibrationTotal = _uiState.value.calibrationCount
        val calibrationSuccess = prefs.getInt("stat_calibration_success", 0)
        val calibrationRate = if (calibrationTotal > 0) (calibrationSuccess.toFloat() / calibrationTotal) * 100 else 0f

        _uiState.update {
            it.copy(
                averagePing = prefs.getInt("stat_avg_ping", 0),
                calibrationSuccessRate = calibrationRate
            )
        }
    }

    private fun startSessionTimer() {
        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update {
                    it.copy(sessionTime = (System.currentTimeMillis() - it.sessionStartTime) / 1000)
                }
            }
        }
    }

    private fun startPerformanceMonitoring() {
        viewModelScope.launch {
            while (true) {
                // Simulate performance metrics
                val cpuUsage = (0..100).random().toFloat()
                val memoryUsage = (20..80).random().toFloat()
                val temperature = (25..45).random().toFloat()

                _uiState.update {
                    it.copy(
                        cpuUsage = cpuUsage,
                        memoryUsage = memoryUsage,
                        temperature = temperature
                    )
                }
                delay(5000)
            }
        }
    }

    fun recordGesture(gesture: String) {
        viewModelScope.launch {
            val today = dateFormat.format(Date())

            when (gesture) {
                "click" -> {
                    prefs.putInt("stat_clicks", _uiState.value.clicks + 1)
                    prefs.putInt("stat_${today}_clicks", prefs.getInt("stat_${today}_clicks", 0) + 1)
                    _uiState.update { it.copy(clicks = it.clicks + 1) }
                }
                "doubleclick" -> {
                    prefs.putInt("stat_double_clicks", _uiState.value.doubleClicks + 1)
                    prefs.putInt("stat_${today}_double_clicks", prefs.getInt("stat_${today}_double_clicks", 0) + 1)
                    _uiState.update { it.copy(doubleClicks = it.doubleClicks + 1) }
                }
                "rightclick" -> {
                    prefs.putInt("stat_right_clicks", _uiState.value.rightClicks + 1)
                    prefs.putInt("stat_${today}_right_clicks", prefs.getInt("stat_${today}_right_clicks", 0) + 1)
                    _uiState.update { it.copy(rightClicks = it.rightClicks + 1) }
                }
                "scroll" -> {
                    prefs.putInt("stat_scrolls", _uiState.value.scrolls + 1)
                    prefs.putInt("stat_${today}_scrolls", prefs.getInt("stat_${today}_scrolls", 0) + 1)
                    _uiState.update { it.copy(scrolls = it.scrolls + 1) }
                }
            }
            loadGestureBreakdown()
        }
    }

    fun recordMovement(distance: Float, speed: Float) {
        viewModelScope.launch {
            val today = dateFormat.format(Date())
            val currentTotalDistance = _uiState.value.totalDistanceMoved + distance
            val currentTotalMovements = _uiState.value.totalMovements + 1
            val averageSpeed = if (currentTotalMovements > 0) currentTotalDistance / currentTotalMovements else 0f
            val peakSpeed = maxOf(_uiState.value.peakSpeed, speed)

            prefs.putFloat("stat_total_distance", currentTotalDistance)
            prefs.putInt("stat_total_movements", currentTotalMovements)
            prefs.putFloat("stat_avg_speed", averageSpeed)
            prefs.putFloat("stat_peak_speed", peakSpeed)
            prefs.putFloat("stat_${today}_distance", prefs.getFloat("stat_${today}_distance", 0f) + distance)

            _uiState.update {
                it.copy(
                    totalDistanceMoved = currentTotalDistance,
                    totalMovements = currentTotalMovements,
                    averageSpeed = averageSpeed,
                    peakSpeed = peakSpeed
                )
            }
        }
    }

    fun recordConnectionAttempt(success: Boolean, ping: Int) {
        viewModelScope.launch {
            val attempts = _uiState.value.connectionAttempts + 1
            val successful = if (success) _uiState.value.successfulConnections + 1 else _uiState.value.successfulConnections
            val failed = if (!success) _uiState.value.failedConnections + 1 else _uiState.value.failedConnections

            prefs.putInt("stat_connection_attempts", attempts)
            prefs.putInt("stat_successful_connections", successful)
            prefs.putInt("stat_failed_connections", failed)

            // Update average ping
            val currentAvg = _uiState.value.averagePing
            val newAvg = if (currentAvg == 0) ping else (currentAvg + ping) / 2
            prefs.putInt("stat_avg_ping", newAvg)

            _uiState.update {
                it.copy(
                    connectionAttempts = attempts,
                    successfulConnections = successful,
                    failedConnections = failed,
                    averagePing = newAvg
                )
            }
        }
    }

    fun recordCalibration(success: Boolean) {
        viewModelScope.launch {
            val count = _uiState.value.calibrationCount + 1
            prefs.putInt("stat_calibration_count", count)
            prefs.putLong("stat_last_calibration", System.currentTimeMillis())

            if (success) {
                val successes = prefs.getInt("stat_calibration_success", 0) + 1
                prefs.putInt("stat_calibration_success", successes)
            }

            _uiState.update {
                it.copy(
                    calibrationCount = count,
                    lastCalibrationTime = System.currentTimeMillis()
                )
            }
            calculateSuccessRates()
        }
    }

    fun updateTimeRange(range: TimeRange) {
        _uiState.update { it.copy(timeRange = range) }
        filterStatsByTimeRange()
    }

    private fun filterStatsByTimeRange() {
        val range = _uiState.value.timeRange
        val cutoffDays = range.days

        if (cutoffDays > 0) {
            val cutoffDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -cutoffDays)
            }.time

            val filteredStats = _uiState.value.dailyStats.filter { it.date.after(cutoffDate) }
            _uiState.update { it.copy(dailyStats = filteredStats) }
        }
    }

    fun updateChartType(type: ChartType) {
        _uiState.update { it.copy(selectedChart = type) }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Reset all counters
            prefs.putInt("stat_clicks", 0)
            prefs.putInt("stat_double_clicks", 0)
            prefs.putInt("stat_right_clicks", 0)
            prefs.putInt("stat_scrolls", 0)
            prefs.putInt("stat_gestures", 0)
            prefs.putInt("stat_custom_gestures", 0)
            prefs.putFloat("stat_total_distance", 0f)
            prefs.putInt("stat_total_movements", 0)
            prefs.putInt("stat_connection_attempts", 0)
            prefs.putInt("stat_successful_connections", 0)
            prefs.putInt("stat_failed_connections", 0)
            prefs.putInt("stat_calibration_count", 0)
            prefs.putInt("stat_calibration_success", 0)

            loadStatistics()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    showResetDialog = false,
                    success = "Statistics reset successfully"
                )
            }
            clearMessages()
        }
    }

    fun exportStatistics() {
        viewModelScope.launch {
            try {
                val exportData = buildString {
                    appendLine("Air Mouse Pro - Statistics Export")
                    appendLine("Exported: ${Date()}")
                    appendLine("=".repeat(50))
                    appendLine()
                    appendLine("Gesture Statistics:")
                    appendLine("  Total Clicks: ${_uiState.value.clicks}")
                    appendLine("  Double Clicks: ${_uiState.value.doubleClicks}")
                    appendLine("  Right Clicks: ${_uiState.value.rightClicks}")
                    appendLine("  Scrolls: ${_uiState.value.scrolls}")
                    appendLine("  Gestures Detected: ${_uiState.value.gesturesDetected}")
                    appendLine()
                    appendLine("Movement Statistics:")
                    appendLine("  Total Distance: ${String.format("%.2f", _uiState.value.totalDistanceMoved)} units")
                    appendLine("  Average Speed: ${String.format("%.2f", _uiState.value.averageSpeed)} units/s")
                    appendLine("  Peak Speed: ${String.format("%.2f", _uiState.value.peakSpeed)} units/s")
                    appendLine("  Total Movements: ${_uiState.value.totalMovements}")
                    appendLine()
                    appendLine("Connection Statistics:")
                    appendLine("  Connection Attempts: ${_uiState.value.connectionAttempts}")
                    appendLine("  Successful: ${_uiState.value.successfulConnections}")
                    appendLine("  Failed: ${_uiState.value.failedConnections}")
                    appendLine("  Average Ping: ${_uiState.value.averagePing}ms")
                    appendLine()
                    appendLine("Session Information:")
                    appendLine("  Session Time: ${formatDuration(_uiState.value.sessionTime)}")
                    appendLine("  Last Calibration: ${if (_uiState.value.lastCalibrationTime > 0) Date(_uiState.value.lastCalibrationTime) else "Never"}")
                    appendLine("  Calibration Count: ${_uiState.value.calibrationCount}")
                    appendLine("  Success Rate: ${String.format("%.1f", _uiState.value.calibrationSuccessRate)}%")
                }

                // Save to file
                val fileName = "airmouse_stats_${System.currentTimeMillis()}.txt"
                val file = java.io.File(context.getExternalFilesDir(null), fileName)
                file.writeText(exportData)

                _uiState.update {
                    it.copy(
                        showExportDialog = false,
                        success = "Statistics exported to $fileName"
                    )
                }
                clearMessages()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    fun showResetDialog(show: Boolean) {
        _uiState.update { it.copy(showResetDialog = show) }
    }

    fun showExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun clearMessages() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(error = null, success = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        sessionTimerJob?.cancel()
        prefs.putLong("stat_session_start", System.currentTimeMillis())
    }
}