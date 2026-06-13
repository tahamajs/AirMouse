package com.airmouse.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(totalPages = getOnboardingItems().size) }
    }

    fun getOnboardingItems(): List<OnboardingItem> {
        return listOf(
            OnboardingItem(
                imageRes = com.airmouse.R.drawable.ic_air_mouse,
                title = "Welcome to Air Mouse Pro",
                description = "Turn your phone into a wireless mouse using motion sensors and AI"
            ),
            OnboardingItem(
                imageRes = com.airmouse.R.drawable.ic_gesture,
                title = "Intuitive Gestures",
                description = "Rotate, tilt, and move naturally to control your PC cursor"
            ),
            OnboardingItem(
                imageRes = com.airmouse.R.drawable.ic_wifi,
                title = "Quick Connection",
                description = "Scan QR code or enter IP address to connect instantly"
            ),
            OnboardingItem(
                imageRes = com.airmouse.R.drawable.ic_about,
                title = "Advanced Features",
                description = "Voice commands, custom gestures, statistics, and more"
            )
        )
    }

    fun nextPage() {
        if (_uiState.value.currentPage < _uiState.value.totalPages - 1) {
            _uiState.update { it.copy(currentPage = it.currentPage + 1) }
        }
    }

    fun previousPage() {
        if (_uiState.value.currentPage > 0) {
            _uiState.update { it.copy(currentPage = it.currentPage - 1) }
        }
    }

    fun goToPage(page: Int) {
        if (page in 0 until _uiState.value.totalPages) {
            _uiState.update { it.copy(currentPage = page) }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.putBoolean("onboarding_completed", true)
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }
}package com.airmouse.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(totalPages = getOnboardingItems().size) }
    }

    fun getOnboardingItems(): List<OnboardingItem> {
        return listOf(
            OnboardingItem(
                imageRes = com.airmouse.R.drawable.ic_air_mouse,
                title = "Welcome to Air Mouse Pro",
                description = "Turn your phone into a wireless mouse using motion sensors and AI"
            ),
            OnboardingItem(
                imageRes = com.airmouse.R.drawable.ic_gesture,
                title = "Intuitive Gestures",
                description = "Rotate, tilt, and move naturally to control your PC cursor"
            ),
            OnboardingItem(
                imageRes = com.airmouse.R.drawable.ic_wifi,
                title = "Quick Connection",
                description = "Scan QR code or enter IP address to connect instantly"
            ),
            OnboardingItem(
                imageRes = com.airmouse.R.drawable.ic_about,
                title = "Advanced Features",
                description = "Voice commands, custom gestures, statistics, and more"
            )
        )
    }

    fun nextPage() {
        if (_uiState.value.currentPage < _uiState.value.totalPages - 1) {
            _uiState.update { it.copy(currentPage = it.currentPage + 1) }
        }
    }

    fun previousPage() {
        if (_uiState.value.currentPage > 0) {
            _uiState.update { it.copy(currentPage = it.currentPage - 1) }
        }
    }

    fun goToPage(page: Int) {
        if (page in 0 until _uiState.value.totalPages) {
            _uiState.update { it.copy(currentPage = page) }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.putBoolean("onboarding_completed", true)
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }
}