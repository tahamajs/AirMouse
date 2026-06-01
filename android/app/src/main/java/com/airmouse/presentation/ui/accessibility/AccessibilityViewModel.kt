package com.airmouse.presentation.ui.accessibility

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AccessibilityViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityUiState())
    val uiState: StateFlow<AccessibilityUiState> = _uiState.asStateFlow()

    fun setAnnounceMovement(enabled: Boolean) = _uiState.update { it.copy(announceMovement = enabled) }
    fun setAnnounceClicks(enabled: Boolean) = _uiState.update { it.copy(announceClicks = enabled) }
    fun setHighContrast(enabled: Boolean) = _uiState.update { it.copy(highContrast = enabled) }
    fun setLargeText(enabled: Boolean) = _uiState.update { it.copy(largeText = enabled) }
}

data class AccessibilityUiState(
    val announceMovement: Boolean = false,
    val announceClicks: Boolean = false,
    val highContrast: Boolean = false,
    val largeText: Boolean = false
)