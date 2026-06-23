
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile
import com.airmouse.domain.repository.IProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageProfileUseCase @Inject constructor(
    private val profileRepository: IProfileRepository
) {

    suspend operator fun invoke(profile: UserProfile): Result<String> {
        return try {
            val id = profileRepository.createProfile(profile)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(id: String): UserProfile? {
        return profileRepository.getProfile(id)
    }

    suspend fun getAllProfiles(): List<UserProfile> {
        return profileRepository.getAllProfiles()
    }

    fun observeProfiles(): Flow<List<UserProfile>> {
        return profileRepository.observeProfiles()
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            profileRepository.updateProfile(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            profileRepository.deleteProfile(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDefaultProfile(): UserProfile? {
        return profileRepository.getDefaultProfile()
    }

    suspend fun setDefaultProfile(id: String): Result<Unit> {
        return try {
            profileRepository.setDefaultProfile(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(id: String): Result<Unit> {
        return try {
            profileRepository.toggleFavorite(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavoriteProfiles(): List<UserProfile> {
        return profileRepository.getFavoriteProfiles()
    }

    suspend fun getSettings(profileId: String): ProfileSettings? {
        return profileRepository.getSettings(profileId)
    }

    suspend fun updateSettings(profileId: String, settings: ProfileSettings): Result<Unit> {
        return try {
            profileRepository.updateSettings(profileId, settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchProfiles(query: String): List<UserProfile> {
        return profileRepository.searchProfiles(query)
    }

    suspend fun exportProfile(id: String): Result<String> {
        return try {
            val json = profileRepository.exportProfile(id)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importProfile(json: String): Result<Boolean> {
        return try {
            val result = profileRepository.importProfile(json)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
