// app/src/main/java/com/airmouse/features/ProfileFeature.kt
package com.airmouse.features

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile
import com.airmouse.domain.usecase.ManageProfileUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileFeature @Inject constructor(
    private val manageProfileUseCase: ManageProfileUseCase
) {

    data class ProfileFeatureState(
        val profiles: List<UserProfile> = emptyList(),
        val currentProfile: UserProfile? = null,
        val defaultProfile: UserProfile? = null,
        val favoriteProfiles: List<UserProfile> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(ProfileFeatureState())
    val state: StateFlow<ProfileFeatureState> = _state.asStateFlow()

    init {
        refreshProfiles()
    }

    suspend fun createProfile(profile: UserProfile): Result<String> {
        _state.value = _state.value.copy(isLoading = true)

        val result = manageProfileUseCase(profile)

        if (result.isSuccess) {
            refreshProfiles()
        }

        _state.value = _state.value.copy(isLoading = false)
        return result
    }

    suspend fun getProfile(id: String): UserProfile? {
        return manageProfileUseCase.getProfile(id)
    }

    suspend fun getAllProfiles(): List<UserProfile> {
        return manageProfileUseCase.getAllProfiles()
    }

    fun observeProfiles(): Flow<List<UserProfile>> {
        return manageProfileUseCase.observeProfiles()
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        _state.value = _state.value.copy(isLoading = true)

        val result = manageProfileUseCase.updateProfile(profile)

        if (result.isSuccess) {
            refreshProfiles()
            // Update current profile if it's the one being updated
            if (profile.id == _state.value.currentProfile?.id) {
                _state.value = _state.value.copy(currentProfile = profile)
            }
        }

        _state.value = _state.value.copy(isLoading = false)
        return result
    }

    suspend fun deleteProfile(id: String): Result<Unit> {
        _state.value = _state.value.copy(isLoading = true)

        val result = manageProfileUseCase.deleteProfile(id)

        if (result.isSuccess) {
            refreshProfiles()
            // Clear current profile if deleted
            if (id == _state.value.currentProfile?.id) {
                _state.value = _state.value.copy(currentProfile = null)
            }
        }

        _state.value = _state.value.copy(isLoading = false)
        return result
    }

    suspend fun getDefaultProfile(): UserProfile? {
        return manageProfileUseCase.getDefaultProfile()
    }

    suspend fun setDefaultProfile(id: String): Result<Unit> {
        val result = manageProfileUseCase.setDefaultProfile(id)
        if (result.isSuccess) {
            refreshProfiles()
        }
        return result
    }

    suspend fun toggleFavorite(id: String): Result<Unit> {
        val result = manageProfileUseCase.toggleFavorite(id)
        if (result.isSuccess) {
            refreshProfiles()
        }
        return result
    }

    suspend fun getSettings(profileId: String): ProfileSettings? {
        return manageProfileUseCase.getSettings(profileId)
    }

    suspend fun updateSettings(profileId: String, settings: ProfileSettings): Result<Unit> {
        val result = manageProfileUseCase.updateSettings(profileId, settings)
        if (result.isSuccess) {
            refreshProfiles()
            // Update current profile settings
            if (profileId == _state.value.currentProfile?.id) {
                val current = _state.value.currentProfile
                _state.value = _state.value.copy(
                    currentProfile = current?.copy(settings = settings)
                )
            }
        }
        return result
    }

    suspend fun searchProfiles(query: String): List<UserProfile> {
        return manageProfileUseCase.searchProfiles(query)
    }

    suspend fun exportProfile(id: String): Result<String> {
        return manageProfileUseCase.exportProfile(id)
    }

    suspend fun importProfile(json: String): Result<Boolean> {
        val result = manageProfileUseCase.importProfile(json)
        if (result.isSuccess) {
            refreshProfiles()
        }
        return result
    }

    suspend fun setCurrentProfile(profile: UserProfile) {
        _state.value = _state.value.copy(currentProfile = profile)
    }

    suspend fun getFavoriteProfiles(): List<UserProfile> {
        return manageProfileUseCase.getFavoriteProfiles()
    }

    suspend fun refreshProfiles() {
        _state.value = _state.value.copy(isLoading = true)

        try {
            val profiles = manageProfileUseCase.getAllProfiles()
            val default = manageProfileUseCase.getDefaultProfile()
            val favorites = manageProfileUseCase.getFavoriteProfiles()

            _state.value = _state.value.copy(
                profiles = profiles,
                defaultProfile = default,
                favoriteProfiles = favorites,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    fun getProfileFeatureState(): ProfileFeatureState = _state.value
}