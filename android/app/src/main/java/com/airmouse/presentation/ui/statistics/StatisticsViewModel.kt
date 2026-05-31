package com.airmouse.presentation.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        startSessionTimer()
    }

    private fun startSessionTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(sessionTime = it.sessionTime + 1) }
            }
        }
    }

    fun resetStats() {
        _uiState.update { it.copy(clicks = 0, doubleClicks = 0, rightClicks = 0, scrolls = 0) }
    }
}