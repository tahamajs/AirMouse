// app/src/main/java/com/airmouse/domain/repository/IProfileRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface IProfileRepository {
    // CRUD
    suspend fun createProfile(profile: UserProfile): String
    suspend fun getProfile(id: String): UserProfile?
    suspend fun updateProfile(profile: UserProfile)
    suspend fun deleteProfile(id: String)
    suspend fun getAllProfiles(): List<UserProfile>
    fun observeProfiles(): Flow<List<UserProfile>>

    // Default
    suspend fun getDefaultProfile(): UserProfile?
    suspend fun setDefaultProfile(id: String)

    // Favorites
    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteProfiles(): List<UserProfile>

    // Settings
    suspend fun getSettings(profileId: String): ProfileSettings?
    suspend fun updateSettings(profileId: String, settings: ProfileSettings)

    // Search
    suspend fun searchProfiles(query: String): List<UserProfile>

    // Export/Import
    suspend fun exportProfile(id: String): String
    suspend fun importProfile(json: String): Boolean
}