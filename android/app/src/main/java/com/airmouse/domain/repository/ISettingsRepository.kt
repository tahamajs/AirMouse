// app/src/main/java/com/airmouse/domain/repository/ISettingsRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {
    fun getPreferences(): Flow<UserPreferences>
    suspend fun updatePreferences(preferences: UserPreferences)
    suspend fun setSensitivity(value: Float)
    suspend fun setClickThreshold(value: Float)
    suspend fun setDoubleClickInterval(value: Long)
    suspend fun setScrollThreshold(value: Float)
    suspend fun setRightClickTilt(value: Float)
    suspend fun setHapticEnabled(enabled: Boolean)
    suspend fun setTheme(theme: String)
}