package com.airmouse.presentation.ui.edge

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class EdgeGesturesViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(EdgeGesturesUiState())
    val uiState: StateFlow<EdgeGesturesUiState> = _uiState.asStateFlow()

    fun toggleService(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
        // Start/stop EdgeGestureService
    }

    fun updateVolumeUpAction(action: String) {
        _uiState.update { it.copy(volumeUpAction = action) }
        // Save to prefs
    }

    fun updateVolumeDownAction(action: String) {
        _uiState.update { it.copy(volumeDownAction = action) }
    }
}