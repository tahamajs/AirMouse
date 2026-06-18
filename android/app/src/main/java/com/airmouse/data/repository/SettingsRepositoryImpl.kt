package com.airmouse.data.repository

import com.airmouse.domain.model.UserPreferences
import com.airmouse.domain.repository.ISettingsRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ISettingsRepository {

    private val _preferences = MutableStateFlow(loadPreferences())
    private val _settingsVersion = MutableStateFlow(1)

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            cursorSensitivity = prefs.getFloat("sensitivity", 0.5f),
            clickThreshold = prefs.getFloat("click_threshold", 8f),
            doubleClickInterval = prefs.getLong("double_click_interval", 300L),
            scrollThreshold = prefs.getFloat("scroll_threshold", 6f),
            rightClickTilt = prefs.getFloat("right_click_tilt", 15f),
            hapticFeedbackEnabled = prefs.getBoolean("haptic_enabled", true),
            theme = prefs.getString("theme", "system"),
            useAiSmoothing = prefs.getBoolean("ai_smoothing", false),
            usePredictiveMovement = prefs.getBoolean("predictive_movement", true),
            invertX = prefs.getBoolean("invert_x", false),
            invertY = prefs.getBoolean("invert_y", false),
            accelerationEnabled = prefs.getBoolean("acceleration_enabled", true),
            smoothingEnabled = prefs.getBoolean("smoothing_enabled", true),
            soundEffectsEnabled = prefs.getBoolean("sound_effects", true),
            autoConnect = prefs.getBoolean("auto_connect", true),
            keepScreenOn = prefs.getBoolean("keep_screen_on", false),
            lastUpdated = prefs.getLong("settings_last_updated", System.currentTimeMillis())
        )
    }

    private fun savePreferences(preferences: UserPreferences) {
        prefs.putFloat("sensitivity", preferences.cursorSensitivity)
        prefs.putFloat("click_threshold", preferences.clickThreshold)
        prefs.putLong("double_click_interval", preferences.doubleClickInterval)
        prefs.putFloat("scroll_threshold", preferences.scrollThreshold)
        prefs.putFloat("right_click_tilt", preferences.rightClickTilt)
        prefs.putBoolean("haptic_enabled", preferences.hapticFeedbackEnabled)
        prefs.putString("theme", preferences.theme)
        prefs.putBoolean("ai_smoothing", preferences.useAiSmoothing)
        prefs.putBoolean("predictive_movement", preferences.usePredictiveMovement)
        prefs.putBoolean("invert_x", preferences.invertX)
        prefs.putBoolean("invert_y", preferences.invertY)
        prefs.putBoolean("acceleration_enabled", preferences.accelerationEnabled)
        prefs.putBoolean("smoothing_enabled", preferences.smoothingEnabled)
        prefs.putBoolean("sound_effects", preferences.soundEffectsEnabled)
        prefs.putBoolean("auto_connect", preferences.autoConnect)
        prefs.putBoolean("keep_screen_on", preferences.keepScreenOn)
        prefs.putLong("settings_last_updated", System.currentTimeMillis())
        _settingsVersion.update { it + 1 }
    }

    override fun getPreferences(): Flow<UserPreferences> = _preferences.asStateFlow()

    override suspend fun updatePreferences(preferences: UserPreferences) {
        savePreferences(preferences)
        _preferences.update { preferences }
    }

    override suspend fun setSensitivity(value: Float) {
        _preferences.update { it.copy(cursorSensitivity = value, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setClickThreshold(value: Float) {
        _preferences.update { it.copy(clickThreshold = value, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setDoubleClickInterval(value: Long) {
        _preferences.update { it.copy(doubleClickInterval = value, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setScrollThreshold(value: Float) {
        _preferences.update { it.copy(scrollThreshold = value, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setRightClickTilt(value: Float) {
        _preferences.update { it.copy(rightClickTilt = value, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setHapticEnabled(enabled: Boolean) {
        _preferences.update { it.copy(hapticFeedbackEnabled = enabled, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setTheme(theme: String) {
        _preferences.update { it.copy(theme = theme, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setAiSmoothing(enabled: Boolean) {
        _preferences.update { it.copy(useAiSmoothing = enabled, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setPredictiveMovement(enabled: Boolean) {
        _preferences.update { it.copy(usePredictiveMovement = enabled, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setInvertX(enabled: Boolean) {
        _preferences.update { it.copy(invertX = enabled, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setInvertY(enabled: Boolean) {
        _preferences.update { it.copy(invertY = enabled, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setAccelerationEnabled(enabled: Boolean) {
        _preferences.update { it.copy(accelerationEnabled = enabled, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun setSmoothingEnabled(enabled: Boolean) {
        _preferences.update { it.copy(smoothingEnabled = enabled, lastUpdated = System.currentTimeMillis()) }
        savePreferences(_preferences.value)
    }

    override suspend fun resetToDefaults() {
        val defaultPrefs = UserPreferences()
        _preferences.update { defaultPrefs }
        savePreferences(defaultPrefs)
    }

    override suspend fun exportSettings(): String {
        val prefs = _preferences.value
        return buildString {
            appendLine("AIRMOUSE_SETTINGS_EXPORT")
            appendLine("version=1")
            appendLine("export_time=${System.currentTimeMillis()}")
            appendLine("cursor_sensitivity=${prefs.cursorSensitivity}")
            appendLine("click_threshold=${prefs.clickThreshold}")
            appendLine("double_click_interval=${prefs.doubleClickInterval}")
            appendLine("scroll_threshold=${prefs.scrollThreshold}")
            appendLine("right_click_tilt=${prefs.rightClickTilt}")
            appendLine("haptic_feedback=${prefs.hapticFeedbackEnabled}")
            appendLine("theme=${prefs.theme}")
            appendLine("ai_smoothing=${prefs.useAiSmoothing}")
            appendLine("predictive_movement=${prefs.usePredictiveMovement}")
            appendLine("invert_x=${prefs.invertX}")
            appendLine("invert_y=${prefs.invertY}")
            appendLine("acceleration=${prefs.accelerationEnabled}")
            appendLine("smoothing=${prefs.smoothingEnabled}")
            appendLine("sound_effects=${prefs.soundEffectsEnabled}")
            appendLine("auto_connect=${prefs.autoConnect}")
            appendLine("keep_screen_on=${prefs.keepScreenOn}")
        }
    }

    override suspend fun importSettings(data: String): Boolean {
        return try {
            val lines = data.lines()
            if (lines.firstOrNull() != "AIRMOUSE_SETTINGS_EXPORT") return false

            var newPrefs = _preferences.value
            for (line in lines) {
                when {
                    line.startsWith("cursor_sensitivity=") -> newPrefs = newPrefs.copy(cursorSensitivity = line.substringAfter("=").toFloat())
                    line.startsWith("click_threshold=") -> newPrefs = newPrefs.copy(clickThreshold = line.substringAfter("=").toFloat())
                    line.startsWith("double_click_interval=") -> newPrefs = newPrefs.copy(doubleClickInterval = line.substringAfter("=").toLong())
                    line.startsWith("scroll_threshold=") -> newPrefs = newPrefs.copy(scrollThreshold = line.substringAfter("=").toFloat())
                    line.startsWith("right_click_tilt=") -> newPrefs = newPrefs.copy(rightClickTilt = line.substringAfter("=").toFloat())
                    line.startsWith("haptic_feedback=") -> newPrefs = newPrefs.copy(hapticFeedbackEnabled = line.substringAfter("=").toBoolean())
                    line.startsWith("theme=") -> newPrefs = newPrefs.copy(theme = line.substringAfter("="))
                    line.startsWith("ai_smoothing=") -> newPrefs = newPrefs.copy(useAiSmoothing = line.substringAfter("=").toBoolean())
                    line.startsWith("predictive_movement=") -> newPrefs = newPrefs.copy(usePredictiveMovement = line.substringAfter("=").toBoolean())
                    line.startsWith("invert_x=") -> newPrefs = newPrefs.copy(invertX = line.substringAfter("=").toBoolean())
                    line.startsWith("invert_y=") -> newPrefs = newPrefs.copy(invertY = line.substringAfter("=").toBoolean())
                    line.startsWith("acceleration=") -> newPrefs = newPrefs.copy(accelerationEnabled = line.substringAfter("=").toBoolean())
                    line.startsWith("smoothing=") -> newPrefs = newPrefs.copy(smoothingEnabled = line.substringAfter("=").toBoolean())
                    line.startsWith("sound_effects=") -> newPrefs = newPrefs.copy(soundEffectsEnabled = line.substringAfter("=").toBoolean())
                    line.startsWith("auto_connect=") -> newPrefs = newPrefs.copy(autoConnect = line.substringAfter("=").toBoolean())
                    line.startsWith("keep_screen_on=") -> newPrefs = newPrefs.copy(keepScreenOn = line.substringAfter("=").toBoolean())
                }
            }
            _preferences.update { newPrefs }
            savePreferences(newPrefs)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getSettingsVersion(): Flow<Int> = _settingsVersion.asStateFlow()
}