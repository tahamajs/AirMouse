package com.airmouse.presentation.ui.accessibility

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessibilityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityUiState())
    val uiState: StateFlow<AccessibilityUiState> = _uiState.asStateFlow()

    data class AccessibilityUiState(
        // Display Settings
        val highContrast: Boolean = false,
        val largeText: Boolean = false,
        val reduceMotion: Boolean = false,
        val darkMode: Boolean = false,
        val customFontSize: Float = 16f,
        val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
        
        // Feedback Settings
        val hapticFeedback: Boolean = true,
        val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
        val soundFeedback: Boolean = false,
        val voiceFeedback: Boolean = false,
        
        // Gesture Settings
        val simplifiedGestures: Boolean = false,
        val screenReader: Boolean = false,
        val announceMovement: Boolean = false,
        val announceClicks: Boolean = false,
        val gestureSensitivity: Float = 1.0f,
        
        // Voice Settings
        val voiceWakeWord: Boolean = true,
        val wakeWord: String = "Hey Air Mouse",
        val voiceConfirmation: Boolean = true,
        val voiceContinuousListening: Boolean = false,
        
        // Advanced Settings
        val switchAccess: Boolean = false,
        val dwellClick: Boolean = false,
        val dwellTime: Int = 1000,
        val audioCues: Boolean = true,
        val flashOnClick: Boolean = false
    )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                highContrast = prefs.getBoolean("high_contrast", false),
                largeText = prefs.getBoolean("large_text", false),
                reduceMotion = prefs.getBoolean("reduce_motion", false),
                darkMode = prefs.getBoolean("dark_mode", false),
                customFontSize = prefs.getFloat("custom_font_size", 16f),
                colorBlindMode = getColorBlindModeFromPrefs(),
                hapticFeedback = prefs.getBoolean("haptic_enabled", true),
                hapticIntensity = getHapticIntensityFromPrefs(),
                soundFeedback = prefs.getBoolean("sound_feedback", false),
                voiceFeedback = prefs.getBoolean("voice_feedback", false),
                simplifiedGestures = prefs.getBoolean("simplified_gestures", false),
                screenReader = prefs.getBoolean("screen_reader", false),
                announceMovement = prefs.getBoolean("announce_movement", false),
                announceClicks = prefs.getBoolean("announce_clicks", false),
                gestureSensitivity = prefs.getFloat("gesture_sensitivity", 1.0f),
                voiceWakeWord = prefs.getBoolean("voice_wake_word", true),
                wakeWord = prefs.getString("wake_word", "Hey Air Mouse"),
                voiceConfirmation = prefs.getBoolean("voice_confirmation", true),
                voiceContinuousListening = prefs.getBoolean("voice_continuous_listening", false),
                switchAccess = prefs.getBoolean("switch_access", false),
                dwellClick = prefs.getBoolean("dwell_click", false),
                dwellTime = prefs.getInt("dwell_time", 1000),
                audioCues = prefs.getBoolean("audio_cues", true),
                flashOnClick = prefs.getBoolean("flash_on_click", false)
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

    private fun getHapticIntensityFromPrefs(): HapticIntensity {
        val intensity = prefs.getString("haptic_intensity", "MEDIUM")
        return try {
            HapticIntensity.valueOf(intensity)
        } catch (e: Exception) {
            HapticIntensity.MEDIUM
        }
    }

    private fun saveColorBlindMode(mode: ColorBlindMode) {
        prefs.putString("color_blind_mode", mode.name)
        applyTheme()
    }

    private fun saveHapticIntensity(intensity: HapticIntensity) {
        prefs.putString("haptic_intensity", intensity.name)
    }

    private fun applyTheme() {
        // Theme application would be handled by MainActivity
        // Could broadcast an intent or use LiveData to trigger recreation
    }

    // Display Settings
    fun setHighContrast(enabled: Boolean) {
        prefs.putBoolean("high_contrast", enabled)
        _uiState.update { it.copy(highContrast = enabled) }
        applyTheme()
    }

    fun setLargeText(enabled: Boolean) {
        prefs.putBoolean("large_text", enabled)
        _uiState.update { it.copy(largeText = enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        prefs.putBoolean("reduce_motion", enabled)
        _uiState.update { it.copy(reduceMotion = enabled) }
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.putBoolean("dark_mode", enabled)
        _uiState.update { it.copy(darkMode = enabled) }
        applyTheme()
    }

    fun setCustomFontSize(size: Float) {
        prefs.putFloat("custom_font_size", size)
        _uiState.update { it.copy(customFontSize = size) }
    }

    fun setColorBlindMode(mode: ColorBlindMode) {
        _uiState.update { it.copy(colorBlindMode = mode) }
        saveColorBlindMode(mode)
    }

    // Feedback Settings
    fun setHapticFeedback(enabled: Boolean) {
        prefs.putBoolean("haptic_enabled", enabled)
        _uiState.update { it.copy(hapticFeedback = enabled) }
    }

    fun setHapticIntensity(intensity: HapticIntensity) {
        _uiState.update { it.copy(hapticIntensity = intensity) }
        saveHapticIntensity(intensity)
    }

    fun setSoundFeedback(enabled: Boolean) {
        prefs.putBoolean("sound_feedback", enabled)
        _uiState.update { it.copy(soundFeedback = enabled) }
    }

    fun setVoiceFeedback(enabled: Boolean) {
        prefs.putBoolean("voice_feedback", enabled)
        _uiState.update { it.copy(voiceFeedback = enabled) }
    }

    // Gesture Settings
    fun setSimplifiedGestures(enabled: Boolean) {
        prefs.putBoolean("simplified_gestures", enabled)
        _uiState.update { it.copy(simplifiedGestures = enabled) }
    }

    fun setScreenReader(enabled: Boolean) {
        prefs.putBoolean("screen_reader", enabled)
        _uiState.update { it.copy(screenReader = enabled) }
    }

    fun setAnnounceMovement(enabled: Boolean) {
        prefs.putBoolean("announce_movement", enabled)
        _uiState.update { it.copy(announceMovement = enabled) }
    }

    fun setAnnounceClicks(enabled: Boolean) {
        prefs.putBoolean("announce_clicks", enabled)
        _uiState.update { it.copy(announceClicks = enabled) }
    }

    fun setGestureSensitivity(sensitivity: Float) {
        prefs.putFloat("gesture_sensitivity", sensitivity)
        _uiState.update { it.copy(gestureSensitivity = sensitivity) }
    }

    // Voice Settings
    fun setVoiceWakeWord(enabled: Boolean) {
        prefs.putBoolean("voice_wake_word", enabled)
        _uiState.update { it.copy(voiceWakeWord = enabled) }
    }

    fun setWakeWord(word: String) {
        prefs.putString("wake_word", word)
        _uiState.update { it.copy(wakeWord = word) }
    }

    fun setVoiceConfirmation(enabled: Boolean) {
        prefs.putBoolean("voice_confirmation", enabled)
        _uiState.update { it.copy(voiceConfirmation = enabled) }
    }

    fun setVoiceContinuousListening(enabled: Boolean) {
        prefs.putBoolean("voice_continuous_listening", enabled)
        _uiState.update { it.copy(voiceContinuousListening = enabled) }
    }

    // Advanced Settings
    fun setSwitchAccess(enabled: Boolean) {
        prefs.putBoolean("switch_access", enabled)
        _uiState.update { it.copy(switchAccess = enabled) }
    }

    fun setDwellClick(enabled: Boolean) {
        prefs.putBoolean("dwell_click", enabled)
        _uiState.update { it.copy(dwellClick = enabled) }
    }

    fun setDwellTime(time: Int) {
        prefs.putInt("dwell_time", time)
        _uiState.update { it.copy(dwellTime = time) }
    }

    fun setAudioCues(enabled: Boolean) {
        prefs.putBoolean("audio_cues", enabled)
        _uiState.update { it.copy(audioCues = enabled) }
    }

    fun setFlashOnClick(enabled: Boolean) {
        prefs.putBoolean("flash_on_click", enabled)
        _uiState.update { it.copy(flashOnClick = enabled) }
    }

    // Reset all settings to defaults
    fun resetToDefaults() {
        _uiState.update {
            it.copy(
                highContrast = false,
                largeText = false,
                reduceMotion = false,
                darkMode = false,
                customFontSize = 16f,
                colorBlindMode = ColorBlindMode.NONE,
                hapticFeedback = true,
                hapticIntensity = HapticIntensity.MEDIUM,
                soundFeedback = false,
                voiceFeedback = false,
                simplifiedGestures = false,
                screenReader = false,
                announceMovement = false,
                announceClicks = false,
                gestureSensitivity = 1.0f,
                voiceWakeWord = true,
                wakeWord = "Hey Air Mouse",
                voiceConfirmation = true,
                voiceContinuousListening = false,
                switchAccess = false,
                dwellClick = false,
                dwellTime = 1000,
                audioCues = true,
                flashOnClick = false
            )
        }
        prefs.putBoolean("high_contrast", false)
        prefs.putBoolean("large_text", false)
        prefs.putBoolean("reduce_motion", false)
        prefs.putBoolean("dark_mode", false)
        prefs.putFloat("custom_font_size", 16f)
        prefs.putString("color_blind_mode", ColorBlindMode.NONE.name)
        prefs.putBoolean("haptic_enabled", true)
        prefs.putString("haptic_intensity", HapticIntensity.MEDIUM.name)
        prefs.putBoolean("sound_feedback", false)
        prefs.putBoolean("voice_feedback", false)
        prefs.putBoolean("simplified_gestures", false)
        prefs.putBoolean("screen_reader", false)
        prefs.putBoolean("announce_movement", false)
        prefs.putBoolean("announce_clicks", false)
        prefs.putFloat("gesture_sensitivity", 1.0f)
        prefs.putBoolean("voice_wake_word", true)
        prefs.putString("wake_word", "Hey Air Mouse")
        prefs.putBoolean("voice_confirmation", true)
        prefs.putBoolean("voice_continuous_listening", false)
        prefs.putBoolean("switch_access", false)
        prefs.putBoolean("dwell_click", false)
        prefs.putInt("dwell_time", 1000)
        prefs.putBoolean("audio_cues", true)
        prefs.putBoolean("flash_on_click", false)
        applyTheme()
    }

    fun openAccessibilityHelp() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.airmouse.io/accessibility"))
        context.startActivity(intent)
    }
}