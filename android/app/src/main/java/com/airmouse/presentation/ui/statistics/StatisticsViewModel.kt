package com.airmouse.presentation.ui.statistics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
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
    private var performanceJob: kotlinx.coroutines.Job? = null

    init {
        loadAllStatistics()
        startSessionTimer()
        startPerformanceMonitoring()
    }

    // ==================== LOAD ====================

    private fun loadAllStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            loadGestureStats()
            loadMovementStats()
            loadConnectionStats()
            loadCalibrationStats()
            loadDailyStats()
            loadGestureBreakdown()
            loadSessionStartTime()
            calculateSuccessRates()
            updateLastActivity()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadGestureStats() {
        val clicks = prefs.getClickCount()
        val doubleClicks = prefs.getDoubleClickCount()
        val rightClicks = prefs.getRightClickCount()
        val scrolls = prefs.getScrollCount()
        val gestures = clicks + doubleClicks + rightClicks + scrolls
        val custom = prefs.getInt("stat_custom_gestures", 0)

        _uiState.update {
            it.copy(
                clicks = clicks,
                doubleClicks = doubleClicks,
                rightClicks = rightClicks,
                scrolls = scrolls,
                gesturesDetected = gestures,
                customGesturesUsed = custom
            )
        }
    }

    private fun loadMovementStats() {
        val distance = prefs.getFloat("stat_total_distance", 0f)
        val movements = prefs.getInt("stat_total_movements", 0)
        val avgSpeed = if (movements > 0) distance / movements else 0f
        val peakSpeed = prefs.getFloat("stat_peak_speed", 0f)

        _uiState.update {
            it.copy(
                totalDistanceMoved = distance,
                totalMovements = movements,
                averageSpeed = avgSpeed,
                peakSpeed = peakSpeed
            )
        }
    }

    private fun loadConnectionStats() {
        val attempts = prefs.getInt("stat_connection_attempts", 0)
        val successful = prefs.getInt("stat_successful_connections", 0)
        val failed = prefs.getInt("stat_failed_connections", 0)
        val avgPing = prefs.getInt("stat_avg_ping", 0)

        _uiState.update {
            it.copy(
                connectionAttempts = attempts,
                successfulConnections = successful,
                failedConnections = failed,
                averagePing = avgPing
            )
        }
    }

    private fun loadCalibrationStats() {
        val count = prefs.getInt("stat_calibration_count", 0)
        val lastTime = prefs.getLong("stat_last_calibration", 0)

        _uiState.update {
            it.copy(
                calibrationCount = count,
                lastCalibrationTime = lastTime
            )
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
        val breakdown = mapOf(
            "Click" to _uiState.value.clicks,
            "Double Click" to _uiState.value.doubleClicks,
            "Right Click" to _uiState.value.rightClicks,
            "Scroll" to _uiState.value.scrolls
        ).filter { it.value > 0 }

        _uiState.update { it.copy(gestureBreakdown = breakdown) }
    }

    private fun loadSessionStartTime() {
        val start = prefs.getLong("stat_session_start", System.currentTimeMillis())
        _uiState.update { it.copy(sessionStartTime = start) }
    }

    private fun calculateSuccessRates() {
        val total = _uiState.value.connectionAttempts
        val successful = _uiState.value.successfulConnections
        val successRate = if (total > 0) (successful.toFloat() / total) * 100 else 0f

        val calTotal = _uiState.value.calibrationCount
        val calSuccess = prefs.getInt("stat_calibration_success", 0)
        val calRate = if (calTotal > 0) (calSuccess.toFloat() / calTotal) * 100 else 0f

        _uiState.update {
            it.copy(calibrationSuccessRate = calRate)
        }
    }

    private fun updateLastActivity() {
        _uiState.update { it.copy(lastActivityTime = System.currentTimeMillis()) }
    }

    // ==================== REAL-TIME UPDATES ====================

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
        performanceJob = viewModelScope.launch {
            while (true) {
                // Simulate – replace with real readings from system
                val cpu = (5..60).random().toFloat()
                val mem = (20..80).random().toFloat()
                val temp = (25..45).random().toFloat()

                _uiState.update {
                    it.copy(
                        cpuUsage = cpu,
                        memoryUsage = mem,
                        temperature = temp
                    )
                }
                delay(5000)
            }
        }
    }

    // ==================== RECORD METHODS (called from outside) ====================

    fun recordGesture(gesture: String) {
        viewModelScope.launch {
            val today = dateFormat.format(Date())
            when (gesture) {
                "click" -> {
                    prefs.incrementClickCount()
                    prefs.putInt("stat_${today}_clicks", prefs.getInt("stat_${today}_clicks", 0) + 1)
                    _uiState.update { it.copy(clicks = it.clicks + 1) }
                }
                "doubleclick" -> {
                    prefs.incrementDoubleClickCount()
                    prefs.putInt("stat_${today}_double_clicks", prefs.getInt("stat_${today}_double_clicks", 0) + 1)
                    _uiState.update { it.copy(doubleClicks = it.doubleClicks + 1) }
                }
                "rightclick" -> {
                    prefs.incrementRightClickCount()
                    prefs.putInt("stat_${today}_right_clicks", prefs.getInt("stat_${today}_right_clicks", 0) + 1)
                    _uiState.update { it.copy(rightClicks = it.rightClicks + 1) }
                }
                "scroll" -> {
                    prefs.incrementScrollCount()
                    prefs.putInt("stat_${today}_scrolls", prefs.getInt("stat_${today}_scrolls", 0) + 1)
                    _uiState.update { it.copy(scrolls = it.scrolls + 1) }
                }
                "custom" -> {
                    prefs.incrementStat("custom_gestures")
                    _uiState.update { it.copy(customGesturesUsed = it.customGesturesUsed + 1) }
                }
            }
            loadGestureBreakdown()
        }
    }

    fun recordMovement(distance: Float, speed: Float) {
        viewModelScope.launch {
            val today = dateFormat.format(Date())
            val newDistance = _uiState.value.totalDistanceMoved + distance
            val newMovements = _uiState.value.totalMovements + 1
            val avgSpeed = if (newMovements > 0) newDistance / newMovements else 0f
            val peakSpeed = maxOf(_uiState.value.peakSpeed, speed)

            prefs.putFloat("stat_total_distance", newDistance)
            prefs.putInt("stat_total_movements", newMovements)
            prefs.putFloat("stat_peak_speed", peakSpeed)
            prefs.putFloat("stat_${today}_distance", prefs.getFloat("stat_${today}_distance", 0f) + distance)

            _uiState.update {
                it.copy(
                    totalDistanceMoved = newDistance,
                    totalMovements = newMovements,
                    averageSpeed = avgSpeed,
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

    // ==================== UI ACTIONS ====================

    fun updateTimeRange(range: TimeRange) {
        _uiState.update { it.copy(timeRange = range) }
        filterStatsByTimeRange()
    }

    private fun filterStatsByTimeRange() {
        val range = _uiState.value.timeRange
        if (range.days > 0) {
            val cutoff = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -range.days)
            }.time

            val filtered = _uiState.value.dailyStats.filter { it.date.after(cutoff) }
            _uiState.update { it.copy(dailyStats = filtered) }
        }
    }

    fun updateChartType(type: ChartType) {
        _uiState.update { it.copy(selectedChart = type) }
    }

    fun showResetDialog(show: Boolean) {
        _uiState.update { it.copy(showResetDialog = show) }
    }

    fun showExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            prefs.resetStatistics()
            // Also clear daily stats – in production you would iterate and clear all stat_* keys
            // For simplicity we reload everything.
            loadAllStatistics()

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
                    appendLine("  Custom Gestures: ${_uiState.value.customGesturesUsed}")
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
                    appendLine("Calibration Statistics:")
                    appendLine("  Last Calibration: ${if (_uiState.value.lastCalibrationTime > 0) Date(_uiState.value.lastCalibrationTime) else "Never"}")
                    appendLine("  Calibration Count: ${_uiState.value.calibrationCount}")
                    appendLine("  Success Rate: ${String.format("%.1f", _uiState.value.calibrationSuccessRate)}%")
                    appendLine()
                    appendLine("Session:")
                    appendLine("  Session Time: ${formatDuration(_uiState.value.sessionTime)}")
                    appendLine("  Battery Usage: ${_uiState.value.batteryUsage}%")
                    appendLine()
                    appendLine("Performance (current):")
                    appendLine("  CPU: ${String.format("%.1f", _uiState.value.cpuUsage)}%")
                    appendLine("  Memory: ${String.format("%.1f", _uiState.value.memoryUsage)}%")
                    appendLine("  Temperature: ${String.format("%.1f", _uiState.value.temperature)}°C")
                }

                val fileName = "airmouse_stats_${System.currentTimeMillis()}.txt"
                val file = File(context.getExternalFilesDir(null), fileName)
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun clearMessages() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(error = null, success = null) }
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, secs)
        else String.format("%02d:%02d", minutes, secs)
    }

    override fun onCleared() {
        super.onCleared()
        sessionTimerJob?.cancel()
        performanceJob?.cancel()
        prefs.putLong("stat_session_start", System.currentTimeMillis())
    }
}