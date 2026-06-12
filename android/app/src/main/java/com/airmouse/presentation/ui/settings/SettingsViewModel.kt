package com.airmouse.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepo.getPreferences().collect { prefs ->
                _uiState.update {
                    it.copy(
                        sensitivity = prefs.cursorSensitivity,
                        clickThreshold = prefs.clickThreshold,
                        doubleClickInterval = prefs.doubleClickInterval,
                        scrollThreshold = prefs.scrollThreshold,
                        rightClickTilt = prefs.rightClickTilt,
                        hapticEnabled = prefs.hapticFeedbackEnabled,
                        theme = prefs.theme,
                        aiSmoothing = prefs.useAiSmoothing,
                        predictive = prefs.usePredictiveMovement
                    )
                }
            }
        }
    }

    fun updateSensitivity(value: Float) {
        viewModelScope.launch { settingsRepo.setSensitivity(value) }
        _uiState.update { it.copy(sensitivity = value) }
    }

    fun updateClickThreshold(value: Float) {
        viewModelScope.launch { settingsRepo.setClickThreshold(value) }
        _uiState.update { it.copy(clickThreshold = value) }
    }

    fun updateDoubleClickInterval(value: Long) {
        viewModelScope.launch { settingsRepo.setDoubleClickInterval(value) }
        _uiState.update { it.copy(doubleClickInterval = value) }
    }

    fun updateScrollThreshold(value: Float) {
        viewModelScope.launch { settingsRepo.setScrollThreshold(value) }
        _uiState.update { it.copy(scrollThreshold = value) }
    }

    fun updateRightClickTilt(value: Float) {
        viewModelScope.launch { settingsRepo.setRightClickTilt(value) }
        _uiState.update { it.copy(rightClickTilt = value) }
    }

    fun updateHaptic(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setHapticEnabled(enabled) }
        _uiState.update { it.copy(hapticEnabled = enabled) }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch { settingsRepo.setTheme(theme) }
        _uiState.update { it.copy(theme = theme) }
    }

    fun updateAiSmoothing(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updatePreferences(settingsRepo.getPreferences().first().copy(useAiSmoothing = enabled)) }
        _uiState.update { it.copy(aiSmoothing = enabled) }
    }

    fun updatePredictive(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updatePreferences(settingsRepo.getPreferences().first().copy(usePredictiveMovement = enabled)) }
        _uiState.update { it.copy(predictive = enabled) }
    }
}
