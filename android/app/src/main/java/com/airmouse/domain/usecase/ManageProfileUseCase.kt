package com.airmouse.domain.usecase

import com.airmouse.domain.model.Profile
import com.airmouse.domain.repository.IProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to manage user profiles.
 */
class ManageProfileUseCase @Inject constructor(
    private val profileRepository: IProfileRepository
) {
    fun getProfiles(): Flow<List<Profile>> = profileRepository.getProfiles()
    
    fun getActiveProfileId(): Flow<String?> = profileRepository.getActiveProfileId()

    suspend fun getProfile(id: String): Profile? =
        profileRepository.getProfile(id)

    suspend fun createProfile(name: String): Profile =
        profileRepository.createProfile(name)

    suspend fun updateProfile(profile: Profile) =
        profileRepository.updateProfile(profile)

    suspend fun deleteProfile(id: String) =
        profileRepository.deleteProfile(id)

    suspend fun setActiveProfile(id: String) =
        profileRepository.setActiveProfile(id)

    suspend fun getActiveProfile(): Profile? =
        profileRepository.getActiveProfile()

    suspend fun duplicateProfile(id: String): Profile? =
        profileRepository.duplicateProfile(id)

    suspend fun exportProfiles(): String =
        profileRepository.exportProfiles()

    suspend fun importProfiles(json: String): Boolean =
        profileRepository.importProfiles(json)
}
