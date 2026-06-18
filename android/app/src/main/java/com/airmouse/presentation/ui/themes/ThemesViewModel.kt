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

    val themeOptions = listOf(
        ThemeOption(
            id = "system",
            name = "System Default",
            description = "Follow your device theme",
            previewColors = listOf(0xFF212121, 0xFF303030, 0xFF424242)
        ),
        ThemeOption(
            id = "light",
            name = "Light",
            description = "Bright and clean interface",
            previewColors = listOf(0xFFFFFFFF, 0xFFF5F5F5, 0xFFE0E0E0)
        ),
        ThemeOption(
            id = "dark",
            name = "Dark",
            description = "Easy on the eyes, saves battery",
            previewColors = listOf(0xFF121212, 0xFF1E1E1E, 0xFF2D2D2D)
        ),
        ThemeOption(
            id = "pure_black",
            name = "Pure Black (AMOLED)",
            description = "Maximum battery savings on AMOLED screens",
            previewColors = listOf(0xFF000000, 0xFF0A0A0A, 0xFF141414)
        ),
        ThemeOption(
            id = "high_contrast",
            name = "High Contrast",
            description = "Enhanced visibility for accessibility",
            previewColors = listOf(0xFF000000, 0xFFFFFFFF, 0xFF000000)
        ),
        ThemeOption(
            id = "ocean",
            name = "Ocean Blue",
            description = "Calming blue tones",
            previewColors = listOf(0xFF0D47A1, 0xFF1976D2, 0xFF42A5F5),
            isPremium = true
        ),
        ThemeOption(
            id = "sunset",
            name = "Sunset Orange",
            description = "Warm orange gradients",
            previewColors = listOf(0xFFE65100, 0xFFFF5722, 0xFFFF7043),
            isPremium = true
        ),
        ThemeOption(
            id = "forest",
            name = "Forest Green",
            description = "Natural green tones",
            previewColors = listOf(0xFF1B5E20, 0xFF2E7D32, 0xFF4CAF50),
            isPremium = true
        ),
        ThemeOption(
            id = "purple_haze",
            name = "Purple Haze",
            description = "Deep purple vibes",
            previewColors = listOf(0xFF4A148C, 0xFF6A1B9A, 0xFF9C27B0),
            isPremium = true
        ),
        ThemeOption(
            id = "cherry",
            name = "Cherry Blossom",
            description = "Soft pink tones",
            previewColors = listOf(0xFF880E4F, 0xFFAD1457, 0xFFE91E63),
            isPremium = true
        ),
        ThemeOption(
            id = "neon",
            name = "Neon Cyber",
            description = "Vibrant neon colors",
            previewColors = listOf(0xFF004D40, 0xFF00BCD4, 0xFF76FF03),
            isPremium = true
        ),
        ThemeOption(
            id = "lavender",
            name = "Lavender Dream",
            description = "Soft lavender tones",
            previewColors = listOf(0xFF311B92, 0xFF4527A0, 0xFF7E57C2),
            isPremium = true
        ),
        ThemeOption(
            id = "mint",
            name = "Mint Fresh",
            description = "Cool mint green",
            previewColors = listOf(0xFF004D40, 0xFF00695C, 0xFF26A69A),
            isPremium = true
        ),
        ThemeOption(
            id = "peach",
            name = "Peach Paradise",
            description = "Warm peach colors",
            previewColors = listOf(0xFFBF360C, 0xFFD84315, 0xFFFF7043),
            isPremium = true
        ),
        ThemeOption(
            id = "sky",
            name = "Sky Blue",
            description = "Bright sky blue",
            previewColors = listOf(0xFF0D47A1, 0xFF1565C0, 0xFF42A5F5),
            isPremium = true
        )
    )

    init {
        loadTheme()
        loadAccentColor()
    }

    private fun loadTheme() {
        viewModelScope.launch {
            val currentTheme = prefs.getString("theme", "system")
            _uiState.update { it.copy(currentTheme = currentTheme) }
        }
    }

    private fun loadAccentColor() {
        viewModelScope.launch {
            val accentColorName = prefs.getString("accent_color", "ORANGE")
            val accentColor = try {
                AccentColor.valueOf(accentColorName)
            } catch (e: IllegalArgumentException) {
                AccentColor.ORANGE
            }
            _uiState.update { it.copy(accentColor = accentColor) }
        }
    }

    fun setTheme(themeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Apply theme
            prefs.putString("theme", themeId)
            _uiState.update { it.copy(currentTheme = themeId, success = "Theme applied successfully") }

            // Simulate delay for theme change
            delay(500)
            _uiState.update { it.copy(isLoading = false) }
            clearMessages()
        }
    }

    fun setAccentColor(accentColor: AccentColor) {
        viewModelScope.launch {
            prefs.putString("accent_color", accentColor.name)
            _uiState.update { it.copy(accentColor = accentColor, success = "Accent color changed to ${accentColor.displayName}") }
            clearMessages()
        }
    }

    fun previewTheme(themeId: String) {
        _uiState.update { it.copy(previewTheme = themeId) }
    }

    fun clearPreview() {
        _uiState.update { it.copy(previewTheme = null) }
    }

    fun toggleCustomization() {
        _uiState.update { it.copy(isCustomizing = !it.isCustomizing) }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            prefs.putString("theme", "system")
            prefs.putString("accent_color", "ORANGE")
            _uiState.update {
                it.copy(
                    currentTheme = "system",
                    accentColor = AccentColor.ORANGE,
                    success = "Reset to default theme"
                )
            }
            clearMessages()
        }
    }

    fun applyCustomTheme(primaryColor: String, secondaryColor: String) {
        viewModelScope.launch {
            prefs.putString("custom_primary_color", primaryColor)
            prefs.putString("custom_secondary_color", secondaryColor)
            _uiState.update { it.copy(success = "Custom theme applied") }
            clearMessages()
        }
    }

    private fun clearMessages() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(error = null, success = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}