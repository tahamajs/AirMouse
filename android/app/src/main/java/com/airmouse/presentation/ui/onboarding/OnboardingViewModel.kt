package com.airmouse.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
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

    data class OnboardingUiState(
        val currentPage: Int = 0,
        val showWelcomeAnimation: Boolean = true,
        val selectedTheme: String = "dark",
        val hapticEnabled: Boolean = true,
        val autoConnect: Boolean = true,
        val allowAnalytics: Boolean = true,
        val hasCompleted: Boolean = false
    )

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _animationProgress = MutableStateFlow(0f)
    val animationProgress: StateFlow<Float> = _animationProgress.asStateFlow()

    private val onboardingItems = OnboardingItem.getDefaultItems()

    init {
        // Check if user has already completed onboarding
        if (prefs.isOnboardingCompleted()) {
            _uiState.update { it.copy(hasCompleted = true) }
        }
        startAnimationProgress()
    }

    private fun startAnimationProgress() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(50)
                _animationProgress.value = (_animationProgress.value + 0.02f) % 1f
            }
        }
    }

    fun getOnboardingItems(): List<OnboardingItem> = onboardingItems

    fun nextPage() {
        if (_uiState.value.currentPage < onboardingItems.size - 1) {
            _uiState.update {
                it.copy(currentPage = it.currentPage + 1, showWelcomeAnimation = false)
            }
        }
    }

    fun previousPage() {
        if (_uiState.value.currentPage > 0) {
            _uiState.update {
                it.copy(currentPage = it.currentPage - 1)
            }
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    fun completeOnboarding() {
        val state = _uiState.value
        // Save user preferences
        prefs.setTheme(state.selectedTheme)
        prefs.setHapticEnabled(state.hapticEnabled)
        prefs.setAutoConnect(state.autoConnect)
        prefs.putBoolean("allow_analytics", state.allowAnalytics)

        // Mark onboarding as completed
        prefs.setOnboardingCompleted(true)
        prefs.setOnboardingVersion(1)

        _uiState.update { it.copy(hasCompleted = true) }
    }

    fun updateTheme(theme: String) {
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun updateHaptic(enabled: Boolean) {
        _uiState.update { it.copy(hapticEnabled = enabled) }
    }

    fun updateAutoConnect(enabled: Boolean) {
        _uiState.update { it.copy(autoConnect = enabled) }
    }

    fun updateAnalytics(enabled: Boolean) {
        _uiState.update { it.copy(allowAnalytics = enabled) }
    }

    fun goToPage(page: Int) {
        if (page in 0 until onboardingItems.size) {
            _uiState.update { it.copy(currentPage = page) }
        }
    }
}