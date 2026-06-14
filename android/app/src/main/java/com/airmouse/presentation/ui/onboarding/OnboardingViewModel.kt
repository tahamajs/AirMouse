package com.airmouse.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    private val _animationProgress = MutableStateFlow(0f)
    val animationProgress: StateFlow<Float> = _animationProgress.asStateFlow()

    init {
        loadSavedPreferences()
        startWelcomeAnimation()
    }

    private fun loadSavedPreferences() {
        _uiState.update {
            it.copy(
                userName = prefs.getString("user_name", ""),
                selectedTheme = prefs.getString("theme", "system"),
                hapticEnabled = prefs.getBoolean("haptic_enabled", true),
                autoConnect = prefs.getBoolean("auto_connect", true),
                allowAnalytics = prefs.getBoolean("allow_analytics", true)
            )
        }
    }

    private fun startWelcomeAnimation() {
        viewModelScope.launch {
            while (_animationProgress.value < 1f && _uiState.value.showWelcomeAnimation) {
                _animationProgress.value += 0.02f
                delay(16)
            }
        }
    }

    fun nextPage() {
        if (_uiState.value.canGoNext) {
            _uiState.update { it.copy(currentPage = it.currentPage + 1, showWelcomeAnimation = false) }
            resetAnimation()
        }
    }

    fun previousPage() {
        if (_uiState.value.canGoPrevious) {
            _uiState.update { it.copy(currentPage = it.currentPage - 1, showWelcomeAnimation = false) }
            resetAnimation()
        }
    }

    fun goToPage(page: Int) {
        if (page in 0 until _uiState.value.totalPages) {
            _uiState.update { it.copy(currentPage = page, showWelcomeAnimation = false) }
            resetAnimation()
        }
    }

    private fun resetAnimation() {
        _animationProgress.value = 0f
        if (_uiState.value.currentPage == 0) {
            startWelcomeAnimation()
        }
    }

    fun updateUserName(name: String) {
        _uiState.update { it.copy(userName = name) }
        prefs.putString("user_name", name)
    }

    fun updateTheme(theme: String) {
        _uiState.update { it.copy(selectedTheme = theme) }
        prefs.putString("theme", theme)
    }

    fun updateHapticEnabled(enabled: Boolean) {
        _uiState.update { it.copy(hapticEnabled = enabled) }
        prefs.putBoolean("haptic_enabled", enabled)
    }

    fun updateAutoConnect(enabled: Boolean) {
        _uiState.update { it.copy(autoConnect = enabled) }
        prefs.putBoolean("auto_connect", enabled)
    }

    fun updateAllowAnalytics(enabled: Boolean) {
        _uiState.update { it.copy(allowAnalytics = enabled) }
        prefs.putBoolean("allow_analytics", enabled)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.putBoolean("onboarding_completed", true)
            prefs.putBoolean("onboarding_seen", true)
            prefs.putLong("onboarding_completed_timestamp", System.currentTimeMillis())
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean("onboarding_completed", false)
    
    fun shouldShowOnboarding(): Boolean = !prefs.getBoolean("onboarding_seen", false)
    
    fun getOnboardingItems(): List<OnboardingItem> = OnboardingItem.getDefaultItems()
    
    fun resetOnboarding() {
        viewModelScope.launch {
            prefs.putBoolean("onboarding_completed", false)
            prefs.putBoolean("onboarding_seen", false)
            _uiState.update { 
                it.copy(
                    currentPage = 0,
                    isCompleted = false,
                    showWelcomeAnimation = true
                )
            }
            startWelcomeAnimation()
        }
    }
}