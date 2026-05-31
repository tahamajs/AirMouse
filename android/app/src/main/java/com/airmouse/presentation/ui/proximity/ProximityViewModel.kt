package com.airmouse.presentation.ui.proximity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProximityViewModel @Inject constructor(
    private val settingsRepo: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProximityUiState())
    val uiState: StateFlow<ProximityUiState> = _uiState.asStateFlow()

    fun toggleService(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled, status = if (enabled) "Service running" else "Service stopped") }
        // Start/stop foreground service
    }

    fun updateNearThreshold(value: Float) {
        _uiState.update { it.copy(nearThreshold = value) }
        // Save to preferences
    }

    fun updateFarThreshold(value: Float) {
        _uiState.update { it.copy(farThreshold = value) }
    }

    fun calibrate() {
        _uiState.update { it.copy(isCalibrating = true) }
        // Simulate calibration
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(isCalibrating = false, currentDistance = 1.0f) }
        }
    }
}