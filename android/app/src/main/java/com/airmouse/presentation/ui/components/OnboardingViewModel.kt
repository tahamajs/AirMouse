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

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val isCompleted: Boolean = false,
    val userName: String = "",
    val selectedTheme: String = "system",
    val hapticEnabled: Boolean = true,
    val autoConnect: Boolean = true,
    val allowAnalytics: Boolean = true
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadSavedPreferences()
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
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }
    
    fun shouldShowOnboarding(): Boolean {
        return !prefs.getBoolean("onboarding_seen", false)
    }
}