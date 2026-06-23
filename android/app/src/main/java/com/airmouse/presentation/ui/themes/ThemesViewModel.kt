
package com.airmouse.presentation.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemesViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemesUiState())
    val uiState: StateFlow<ThemesUiState> = _uiState.asStateFlow()

    val themeOptions = ThemeDefinitions.themes

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val savedTheme = prefs.getString("theme", "system")
        val savedAccent = prefs.getString("accent_color", "ORANGE")
        val accentColor = try {
            AccentColor.valueOf(savedAccent)
        } catch (e: Exception) {
            AccentColor.ORANGE
        }

        _uiState.update {
            it.copy(
                currentTheme = savedTheme,
                accentColor = accentColor,
                themeApplied = true
            )
        }
    }

    fun setTheme(themeId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                
                prefs.putString("theme", themeId)

                
                _uiState.update {
                    it.copy(
                        currentTheme = themeId,
                        isLoading = false,
                        success = "Theme applied: ${getThemeName(themeId)}",
                        themeApplied = true
                    )
                }

                
                delay(3000)
                _uiState.update { it.copy(success = null) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to apply theme: ${e.message}"
                    )
                }
            }
        }
    }

    fun setAccentColor(accentColor: AccentColor) {
        viewModelScope.launch {
            try {
                prefs.putString("accent_color", accentColor.name)
                _uiState.update {
                    it.copy(
                        accentColor = accentColor,
                        success = "Accent color: ${accentColor.displayName}"
                    )
                }
                delay(2000)
                _uiState.update { it.copy(success = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to set accent color: ${e.message}")
                }
            }
        }
    }

    fun previewTheme(themeId: String) {
        _uiState.update { it.copy(previewTheme = themeId) }
    }

    fun clearPreview() {
        _uiState.update { it.copy(previewTheme = null) }
    }

    fun toggleCustomization() {
        _uiState.update {
            it.copy(isCustomizing = !it.isCustomizing)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val defaultTheme = "system"
                val defaultAccent = AccentColor.ORANGE

                prefs.putString("theme", defaultTheme)
                prefs.putString("accent_color", defaultAccent.name)

                _uiState.update {
                    it.copy(
                        currentTheme = defaultTheme,
                        accentColor = defaultAccent,
                        isLoading = false,
                        success = "Reset to default theme",
                        themeApplied = true
                    )
                }

                delay(3000)
                _uiState.update { it.copy(success = null) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to reset: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun getThemeName(themeId: String): String {
        return ThemeDefinitions.getTheme(themeId)?.name ?: themeId
    }
}