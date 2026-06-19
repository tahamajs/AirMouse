package com.airmouse.presentation.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    sessionTime = ((System.currentTimeMillis() - current.sessionStartTime) / 1000).coerceAtLeast(0),
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    fun updateTimeRange(timeRange: TimeRange) {
        _uiState.update { it.copy(timeRange = timeRange) }
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
        _uiState.value = createInitialState()
    }

    fun exportStatistics() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showExportDialog = false,
                    success = "Statistics export is not wired yet"
                )
            }
        }
    }

    private fun createInitialState(): StatisticsUiState {
        val now = System.currentTimeMillis()
        return StatisticsUiState(
            sessionStartTime = now,
            lastActivityTime = now,
            dailyStats = listOf(
                DailyStats(
                    date = Date(now),
                    clicks = 0,
                    doubleClicks = 0,
                    rightClicks = 0,
                    scrolls = 0,
                    gestures = 0,
                    movements = 0
                )
            ),
            gestureBreakdown = emptyMap()
        )
    }
}
