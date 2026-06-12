// app/src/main/java/com/airmouse/data/repository/SettingsRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.data.local.PreferencesManager
import com.airmouse.domain.model.UserPreferences
import com.airmouse.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ISettingsRepository {

    override fun getPreferences(): Flow<UserPreferences> = flow {
        emit(UserPreferences(
            cursorSensitivity = prefs.getSensitivity(),
            clickThreshold = prefs.getClickThreshold(),
            doubleClickInterval = prefs.getDoubleClickInterval(),
            scrollThreshold = prefs.getScrollThreshold(),
            rightClickTilt = prefs.getRightClickTilt(),
            hapticFeedbackEnabled = prefs.isHapticEnabled(),
            theme = prefs.getTheme(),
            useAiSmoothing = prefs.isAISmoothingEnabled(),
            usePredictiveMovement = prefs.isPredictiveEnabled()
        ))
    }

    override suspend fun updatePreferences(preferences: UserPreferences) {
        prefs.setSensitivity(preferences.cursorSensitivity)
        prefs.setClickThreshold(preferences.clickThreshold)
        prefs.setDoubleClickInterval(preferences.doubleClickInterval)
        prefs.setScrollThreshold(preferences.scrollThreshold)
        prefs.setRightClickTilt(preferences.rightClickTilt)
        prefs.setHapticEnabled(preferences.hapticFeedbackEnabled)
        prefs.setTheme(preferences.theme)
        prefs.setAISmoothingEnabled(preferences.useAiSmoothing)
        prefs.setPredictiveEnabled(preferences.usePredictiveMovement)
    }

    override suspend fun setSensitivity(value: Float) = prefs.setSensitivity(value)
    override suspend fun setClickThreshold(value: Float) = prefs.setClickThreshold(value)
    override suspend fun setDoubleClickInterval(value: Long) = prefs.setDoubleClickInterval(value)
    override suspend fun setScrollThreshold(value: Float) = prefs.setScrollThreshold(value)
    override suspend fun setRightClickTilt(value: Float) = prefs.setRightClickTilt(value)
    override suspend fun setHapticEnabled(enabled: Boolean) = prefs.setHapticEnabled(enabled)
    override suspend fun setTheme(theme: String) = prefs.setTheme(theme)
}
