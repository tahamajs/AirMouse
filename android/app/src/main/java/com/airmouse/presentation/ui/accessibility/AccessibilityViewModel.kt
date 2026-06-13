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

    fun toggleAnnounceMovement(enabled: Boolean) = _uiState.update { it.copy(announceMovement = enabled) }
    fun toggleAnnounceClicks(enabled: Boolean) = _uiState.update { it.copy(announceClicks = enabled) }
    fun toggleHighContrast(enabled: Boolean) = _uiState.update { it.copy(highContrast = enabled) }
    fun toggleLargeText(enabled: Boolean) = _uiState.update { it.copy(largeText = enabled) }
}
