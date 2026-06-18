package com.airmouse.presentation.ui.accessibility

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
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

    private fun showSuccess(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(successMessage = message) }
            delay(3000)
            _uiState.update { it.copy(successMessage = null) }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = message) }
            delay(3000)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun applyTheme() {
        val intent = Intent("com.airmouse.THEME_CHANGED")
        intent.putExtra("theme", if (_uiState.value.darkMode) "dark" else "light")
        context.sendBroadcast(intent)
    }

    // --- Display ---
    fun setHighContrast(enabled: Boolean) {
        prefs.putBoolean("high_contrast", enabled)
        _uiState.update { it.copy(highContrast = enabled) }
        applyTheme()
        showSuccess("High contrast ${if (enabled) "enabled" else "disabled"}")
    }

    fun setLargeText(enabled: Boolean) {
        prefs.putBoolean("large_text", enabled)
        _uiState.update { it.copy(largeText = enabled) }
        showSuccess("Large text ${if (enabled) "enabled" else "disabled"}")
    }

    fun setReduceMotion(enabled: Boolean) {
        prefs.putBoolean("reduce_motion", enabled)
        _uiState.update { it.copy(reduceMotion = enabled) }
        showSuccess("Reduce motion ${if (enabled) "enabled" else "disabled"}")
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.putBoolean("dark_mode", enabled)
        _uiState.update { it.copy(darkMode = enabled) }
        applyTheme()
        showSuccess("Dark mode ${if (enabled) "enabled" else "disabled"}")
    }

    fun setCustomFontSize(size: Float) {
        prefs.putFloat("custom_font_size", size)
        _uiState.update { it.copy(customFontSize = size) }
        showSuccess("Font size set to ${size.toInt()}sp")
    }

    fun setColorBlindMode(mode: ColorBlindMode) {
        prefs.putString("color_blind_mode", mode.name)
        _uiState.update { it.copy(colorBlindMode = mode) }
        applyTheme()
        showSuccess("Color blind mode set to ${mode.displayName}")
    }

    // --- Feedback ---
    fun setHapticFeedback(enabled: Boolean) {
        prefs.putBoolean("haptic_enabled", enabled)
        _uiState.update { it.copy(hapticFeedback = enabled) }
        showSuccess("Haptic feedback ${if (enabled) "enabled" else "disabled"}")
    }

    fun setHapticIntensity(intensity: HapticIntensity) {
        prefs.putString("haptic_intensity", intensity.name)
        _uiState.update { it.copy(hapticIntensity = intensity) }
        showSuccess("Haptic intensity set to ${intensity.name.lowercase()}")
    }

    fun setSoundFeedback(enabled: Boolean) {
        prefs.putBoolean("sound_feedback", enabled)
        _uiState.update { it.copy(soundFeedback = enabled) }
        showSuccess("Sound feedback ${if (enabled) "enabled" else "disabled"}")
    }

    fun setVoiceFeedback(enabled: Boolean) {
        prefs.putBoolean("voice_feedback", enabled)
        _uiState.update { it.copy(voiceFeedback = enabled) }
        showSuccess("Voice feedback ${if (enabled) "enabled" else "disabled"}")
    }

    // --- Gesture ---
    fun setSimplifiedGestures(enabled: Boolean) {
        prefs.putBoolean("simplified_gestures", enabled)
        _uiState.update { it.copy(simplifiedGestures = enabled) }
        showSuccess("Simplified gestures ${if (enabled) "enabled" else "disabled"}")
    }

    fun setScreenReader(enabled: Boolean) {
        prefs.putBoolean("screen_reader", enabled)
        _uiState.update { it.copy(screenReader = enabled) }
        showSuccess("Screen reader support ${if (enabled) "enabled" else "disabled"}")
    }

    fun setAnnounceMovement(enabled: Boolean) {
        prefs.putBoolean("announce_movement", enabled)
        _uiState.update { it.copy(announceMovement = enabled) }
        showSuccess("Announce movement ${if (enabled) "enabled" else "disabled"}")
    }

    fun setAnnounceClicks(enabled: Boolean) {
        prefs.putBoolean("announce_clicks", enabled)
        _uiState.update { it.copy(announceClicks = enabled) }
        showSuccess("Announce clicks ${if (enabled) "enabled" else "disabled"}")
    }

    fun setGestureSensitivity(sensitivity: Float) {
        prefs.putFloat("gesture_sensitivity", sensitivity)
        _uiState.update { it.copy(gestureSensitivity = sensitivity) }
        showSuccess("Gesture sensitivity set to $sensitivity")
    }

    // --- Voice ---
    fun setVoiceWakeWord(enabled: Boolean) {
        prefs.putBoolean("voice_wake_word", enabled)
        _uiState.update { it.copy(voiceWakeWord = enabled) }
        showSuccess("Wake word ${if (enabled) "enabled" else "disabled"}")
    }

    fun setWakeWord(word: String) {
        prefs.putString("wake_word", word)
        _uiState.update { it.copy(wakeWord = word) }
        showSuccess("Wake word set to '$word'")
    }

    fun setVoiceConfirmation(enabled: Boolean) {
        prefs.putBoolean("voice_confirmation", enabled)
        _uiState.update { it.copy(voiceConfirmation = enabled) }
        showSuccess("Voice confirmation ${if (enabled) "enabled" else "disabled"}")
    }

    fun setVoiceContinuousListening(enabled: Boolean) {
        prefs.putBoolean("voice_continuous_listening", enabled)
        _uiState.update { it.copy(voiceContinuousListening = enabled) }
        showSuccess("Continuous listening ${if (enabled) "enabled" else "disabled"}")
    }

    // --- Advanced ---
    fun setSwitchAccess(enabled: Boolean) {
        prefs.putBoolean("switch_access", enabled)
        _uiState.update { it.copy(switchAccess = enabled) }
        showSuccess("Switch access ${if (enabled) "enabled" else "disabled"}")
    }

    fun setDwellClick(enabled: Boolean) {
        prefs.putBoolean("dwell_click", enabled)
        _uiState.update { it.copy(dwellClick = enabled) }
        showSuccess("Dwell click ${if (enabled) "enabled" else "disabled"}")
    }

    fun setDwellTime(time: Int) {
        prefs.putInt("dwell_time", time)
        _uiState.update { it.copy(dwellTime = time) }
        showSuccess("Dwell time set to ${time}ms")
    }

    fun setAudioCues(enabled: Boolean) {
        prefs.putBoolean("audio_cues", enabled)
        _uiState.update { it.copy(audioCues = enabled) }
        showSuccess("Audio cues ${if (enabled) "enabled" else "disabled"}")
    }

    fun setFlashOnClick(enabled: Boolean) {
        prefs.putBoolean("flash_on_click", enabled)
        _uiState.update { it.copy(flashOnClick = enabled) }
        showSuccess("Flash on click ${if (enabled) "enabled" else "disabled"}")
    }

    // --- Reset ---
    fun resetToDefaults() {
        setHighContrast(false)
        setLargeText(false)
        setReduceMotion(false)
        setDarkMode(false)
        setCustomFontSize(16f)
        setColorBlindMode(ColorBlindMode.NONE)

        setHapticFeedback(true)
        setHapticIntensity(HapticIntensity.MEDIUM)
        setSoundFeedback(false)
        setVoiceFeedback(false)

        setSimplifiedGestures(false)
        setScreenReader(false)
        setAnnounceMovement(false)
        setAnnounceClicks(false)
        setGestureSensitivity(1.0f)

        setVoiceWakeWord(true)
        setWakeWord("Hey Air Mouse")
        setVoiceConfirmation(true)
        setVoiceContinuousListening(false)

        setSwitchAccess(false)
        setDwellClick(false)
        setDwellTime(1000)
        setAudioCues(true)
        setFlashOnClick(false)

        applyTheme()
        showSuccess("All accessibility settings reset to defaults")
    }

    fun openAccessibilityHelp() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.airmouse.io/accessibility"))
        context.startActivity(intent)
    }
}