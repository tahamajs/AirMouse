// app/src/main/java/com/airmouse/domain/usecase/ManageProfileUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.Profile
import com.airmouse.domain.model.UserPreferences
import com.airmouse.domain.repository.IProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ManageProfileUseCase @Inject constructor(
    private val profileRepo: IProfileRepository,
    private val settingsRepo: ISettingsRepository
) {

    suspend fun saveCurrentAsProfile(name: String): Result<Profile> {
        return try {
            val prefs = settingsRepo.getPreferences().first()
            val profile = Profile(
                name = name,
                sensitivity = prefs.cursorSensitivity,
                clickThreshold = prefs.clickThreshold,
                doubleClickInterval = prefs.doubleClickInterval,
                scrollThreshold = prefs.scrollThreshold,
                rightClickTilt = prefs.rightClickTilt,
                hapticEnabled = prefs.hapticFeedbackEnabled,
                theme = prefs.theme,
                aiSmoothing = prefs.useAiSmoothing,
                predictiveMovement = prefs.usePredictiveMovement,
                invertX = prefs.invertX,
                invertY = prefs.invertY,
                accelerationEnabled = prefs.accelerationEnabled,
                smoothingEnabled = prefs.smoothingEnabled
            )
            profileRepo.saveProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadProfile(profileId: String): Result<Unit> {
        return try {
            val profile = profileRepo.getProfile(profileId)
            if (profile != null) {
                settingsRepo.setSensitivity(profile.sensitivity)
                settingsRepo.setClickThreshold(profile.clickThreshold)
                settingsRepo.setDoubleClickInterval(profile.doubleClickInterval)
                settingsRepo.setScrollThreshold(profile.scrollThreshold)
                settingsRepo.setRightClickTilt(profile.rightClickTilt)
                settingsRepo.setHapticEnabled(profile.hapticEnabled)
                settingsRepo.setTheme(profile.theme)
                settingsRepo.setAiSmoothing(profile.aiSmoothing)
                settingsRepo.setPredictiveMovement(profile.predictiveMovement)
                settingsRepo.setInvertX(profile.invertX)
                settingsRepo.setInvertY(profile.invertY)
                settingsRepo.setAccelerationEnabled(profile.accelerationEnabled)
                settingsRepo.setSmoothingEnabled(profile.smoothingEnabled)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}// app/src/main/java/com/airmouse/domain/usecase/ManageProfileUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.Profile
import com.airmouse.domain.model.UserPreferences
import com.airmouse.domain.repository.IProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ManageProfileUseCase @Inject constructor(
    private val profileRepo: IProfileRepository,
    private val settingsRepo: ISettingsRepository
) {

    suspend fun saveCurrentAsProfile(name: String): Result<Profile> {
        return try {
            val prefs = settingsRepo.getPreferences().first()
            val profile = Profile(
                name = name,
                sensitivity = prefs.cursorSensitivity,
                clickThreshold = prefs.clickThreshold,
                doubleClickInterval = prefs.doubleClickInterval,
                scrollThreshold = prefs.scrollThreshold,
                rightClickTilt = prefs.rightClickTilt,
                hapticEnabled = prefs.hapticFeedbackEnabled,
                theme = prefs.theme,
                aiSmoothing = prefs.useAiSmoothing,
                predictiveMovement = prefs.usePredictiveMovement,
                invertX = prefs.invertX,
                invertY = prefs.invertY,
                accelerationEnabled = prefs.accelerationEnabled,
                smoothingEnabled = prefs.smoothingEnabled
            )
            profileRepo.saveProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadProfile(profileId: String): Result<Unit> {
        return try {
            val profile = profileRepo.getProfile(profileId)
            if (profile != null) {
                settingsRepo.setSensitivity(profile.sensitivity)
                settingsRepo.setClickThreshold(profile.clickThreshold)
                settingsRepo.setDoubleClickInterval(profile.doubleClickInterval)
                settingsRepo.setScrollThreshold(profile.scrollThreshold)
                settingsRepo.setRightClickTilt(profile.rightClickTilt)
                settingsRepo.setHapticEnabled(profile.hapticEnabled)
                settingsRepo.setTheme(profile.theme)
                settingsRepo.setAiSmoothing(profile.aiSmoothing)
                settingsRepo.setPredictiveMovement(profile.predictiveMovement)
                settingsRepo.setInvertX(profile.invertX)
                settingsRepo.setInvertY(profile.invertY)
                settingsRepo.setAccelerationEnabled(profile.accelerationEnabled)
                settingsRepo.setSmoothingEnabled(profile.smoothingEnabled)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.airmouse.domain.usecase

import com.airmouse.domain.model.Profile
import com.airmouse.domain.model.UserPreferences
import com.airmouse.domain.repository.IProfileRepository
import com.airmouse.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ManageProfileUseCase @Inject constructor(
    private val profileRepo: IProfileRepository,
    private val settingsRepo: ISettingsRepository
) {
    suspend fun saveCurrentAsProfile(name: String): Profile {
        val prefs = settingsRepo.getPreferences().first()
        val profile = Profile(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            sensitivity = prefs.cursorSensitivity,
            clickThreshold = prefs.clickThreshold,
            doubleClickInterval = prefs.doubleClickInterval,
            scrollThreshold = prefs.scrollThreshold,
            rightClickTilt = prefs.rightClickTilt,
            hapticEnabled = prefs.hapticFeedbackEnabled,
            theme = prefs.theme,
            aiSmoothing = prefs.useAiSmoothing,
            predictiveMovement = prefs.usePredictiveMovement,
            invertX = prefs.invertX,
            invertY = prefs.invertY,
            accelerationEnabled = prefs.accelerationEnabled,
            smoothingEnabled = prefs.smoothingEnabled,
            createdAt = System.currentTimeMillis(),
            lastUsed = System.currentTimeMillis(),
            isDefault = false,
            isFavorite = false
        )
        profileRepo.saveProfile(profile)
        return profile
    }

    suspend fun loadProfile(profileId: String) {
        val profile = profileRepo.getProfile(profileId) ?: return
        settingsRepo.setSensitivity(profile.sensitivity)
        settingsRepo.setClickThreshold(profile.clickThreshold)
        settingsRepo.setDoubleClickInterval(profile.doubleClickInterval)
        settingsRepo.setScrollThreshold(profile.scrollThreshold)
        settingsRepo.setRightClickTilt(profile.rightClickTilt)
        settingsRepo.setHapticEnabled(profile.hapticEnabled)
        settingsRepo.setTheme(profile.theme)
        settingsRepo.setAiSmoothing(profile.aiSmoothing)
        settingsRepo.setPredictiveMovement(profile.predictiveMovement)
        settingsRepo.setInvertX(profile.invertX)
        settingsRepo.setInvertY(profile.invertY)
        settingsRepo.setAccelerationEnabled(profile.accelerationEnabled)
        settingsRepo.setSmoothingEnabled(profile.smoothingEnabled)
        profileRepo.updateLastUsed(profileId)
    }
}