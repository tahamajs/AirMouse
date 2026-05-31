package com.airmouse.presentation.ui.themes

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
class ThemesViewModel @Inject constructor(
    private val settingsRepo: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemesUiState())
    val uiState: StateFlow<ThemesUiState> = _uiState.asStateFlow()

    init {
        loadTheme()
    }

    private fun loadTheme() {
        viewModelScope.launch {
            settingsRepo.getPreferences().collect { prefs ->
                _uiState.update { it.copy(currentTheme = prefs.theme) }
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepo.setTheme(theme)
            _uiState.update { it.copy(currentTheme = theme) }
        }
    }
}