package com.airmouse.domain.usecase

import com.airmouse.domain.model.UserProfile
import com.airmouse.domain.repository.IProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to manage user profiles.
 */
class ManageProfileUseCase @Inject constructor(
    private val profileRepository: IProfileRepository
) {
    /**
     * Saves a new or existing profile.
     */
    suspend fun saveProfile(profile: UserProfile) {
        profileRepository.saveProfile(profile)
    }

    /**
     * Retrieves a profile by its ID.
     */
    suspend fun getProfile(id: String): UserProfile? =
        profileRepository.getProfile(id)

    /**
     * Returns all stored profiles.
     */
    suspend fun getAllProfiles(): List<UserProfile> =
        profileRepository.getAllProfiles()

    /**
     * Deletes a profile by ID.
     */
    suspend fun deleteProfile(id: String) {
        profileRepository.deleteProfile(id)
    }

    /**
     * Sets the active profile.
     */
    suspend fun setActiveProfile(id: String) {
        profileRepository.setActiveProfile(id)
    }

    /**
     * Returns the currently active profile as a Flow.
     */
    fun getActiveProfile(): Flow<UserProfile?> =
        profileRepository.getActiveProfile()

    /**
     * Duplicates an existing profile.
     */
    suspend fun duplicateProfile(id: String): UserProfile =
        profileRepository.duplicateProfile(id)

    /**
     * Exports a profile to a JSON string.
     */
    suspend fun exportProfile(id: String): String =
        profileRepository.exportProfile(id)

    /**
     * Imports a profile from a JSON string.
     */
    suspend fun importProfile(data: String): Boolean =
        profileRepository.importProfile(data)
}