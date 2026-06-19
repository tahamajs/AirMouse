// app/src/main/java/com/airmouse/data/datasource/local/IProfileDataSource.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile

interface IProfileDataSource {

    // CRUD operations
    suspend fun saveProfile(profile: UserProfile)
    suspend fun getProfile(id: String): UserProfile?
    suspend fun getAllProfiles(): List<UserProfile>
    suspend fun deleteProfile(id: String)
    suspend fun updateProfile(profile: UserProfile)

    // Default profile
    suspend fun setDefaultProfile(id: String)
    suspend fun getDefaultProfile(): UserProfile?

    // Favorites
    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteProfiles(): List<UserProfile>

    // Settings
    suspend fun saveProfileSettings(profileId: String, settings: ProfileSettings)
    suspend fun getProfileSettings(profileId: String): ProfileSettings?

    // Search
    suspend fun searchProfiles(query: String): List<UserProfile>
}