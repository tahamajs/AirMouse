// app/src/main/java/com/airmouse/presentation/ui/accessibility/AccessibilityViewModel.kt
package com.airmouse.presentation.ui.accessibility

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
class AccessibilityViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityUiState())
    val uiState: StateFlow<AccessibilityUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                announceMovement = prefs.getBoolean("announce_movement", false),
                announceClicks = prefs.getBoolean("announce_clicks", false),
                highContrast = prefs.getBoolean("high_contrast", false),
                largeText = prefs.getBoolean("large_text", false),
                reduceMotion = prefs.getBoolean("reduce_motion", false),
                colorBlindMode = getColorBlindModeFromPrefs(),
                customFontSize = prefs.getFloat("custom_font_size", 16f),
                spokenFeedbackEnabled = prefs.getBoolean("spoken_feedback", false),
                hapticFeedbackEnabled = prefs.getBoolean("haptic_enabled", true),
                screenReaderCompatible = prefs.getBoolean("screen_reader_compatible", false)
            )
        }
    }

    private fun getColorBlindModeFromPrefs(): ColorBlindMode {
        val mode = prefs.getString("color_blind_mode", "NONE")
        return try {
            ColorBlindMode.valueOf(mode)
        } catch (e: Exception) {
            ColorBlindMode.NONE
        }
    }

    private fun saveColorBlindMode(mode: ColorBlindMode) {
        prefs.putString("color_blind_mode", mode.name)
    }

    fun updateAnnounceMovement(enabled: Boolean) {
        _uiState.update { it.copy(announceMovement = enabled) }
        prefs.putBoolean("announce_movement", enabled)
    }

    fun updateAnnounceClicks(enabled: Boolean) {
        _uiState.update { it.copy(announceClicks = enabled) }
        prefs.putBoolean("announce_clicks", enabled)
    }

    fun updateHighContrast(enabled: Boolean) {
        _uiState.update { it.copy(highContrast = enabled) }
        prefs.putBoolean("high_contrast", enabled)
        // Apply theme change if needed
        // (could broadcast an event to restart activity)
    }

    fun updateLargeText(enabled: Boolean) {
        _uiState.update { it.copy(largeText = enabled) }
        prefs.putBoolean("large_text", enabled)
    }

    fun updateReduceMotion(enabled: Boolean) {
        _uiState.update { it.copy(reduceMotion = enabled) }
        prefs.putBoolean("reduce_motion", enabled)
    }

    fun updateColorBlindMode(mode: ColorBlindMode) {
        _uiState.update { it.copy(colorBlindMode = mode) }
        saveColorBlindMode(mode)
    }

    fun updateCustomFontSize(size: Float) {
        _uiState.update { it.copy(customFontSize = size) }
        prefs.putFloat("custom_font_size", size)
    }

    fun updateSpokenFeedback(enabled: Boolean) {
        _uiState.update { it.copy(spokenFeedbackEnabled = enabled) }
        prefs.putBoolean("spoken_feedback", enabled)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        _uiState.update { it.copy(hapticFeedbackEnabled = enabled) }
        prefs.putBoolean("haptic_enabled", enabled)
    }

    fun resetToDefaults() {
        _uiState.update {
            it.copy(
                announceMovement = false,
                announceClicks = false,
                highContrast = false,
                largeText = false,
                reduceMotion = false,
                colorBlindMode = ColorBlindMode.NONE,
                customFontSize = 16f,
                spokenFeedbackEnabled = false,
                hapticFeedbackEnabled = true,
                screenReaderCompatible = false
            )
        }
        // Persist defaults
        prefs.putBoolean("announce_movement", false)
        prefs.putBoolean("announce_clicks", false)
        prefs.putBoolean("high_contrast", false)
        prefs.putBoolean("large_text", false)
        prefs.putBoolean("reduce_motion", false)
        prefs.putString("color_blind_mode", ColorBlindMode.NONE.name)
        prefs.putFloat("custom_font_size", 16f)
        prefs.putBoolean("spoken_feedback", false)
        prefs.putBoolean("haptic_enabled", true)
        prefs.putBoolean("screen_reader_compatible", false)
    }
}