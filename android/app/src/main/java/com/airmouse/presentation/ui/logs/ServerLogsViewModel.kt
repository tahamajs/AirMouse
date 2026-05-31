package com.airmouse.presentation.ui.logs

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ServerLogsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ServerLogsUiState())
    val uiState: StateFlow<ServerLogsUiState> = _uiState.asStateFlow()

    fun setFilter(filter: String) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setLevel(level: String) {
        _uiState.update { it.copy(level = level) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun exportLogs() {
        // Export to file
    }
}