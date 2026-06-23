
package com.airmouse.domain.repository

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface IProfileRepository {

    

    suspend fun createProfile(profile: UserProfile): String
    suspend fun getProfile(id: String): UserProfile?
    suspend fun updateProfile(profile: UserProfile)
    suspend fun deleteProfile(id: String)
    suspend fun getAllProfiles(): List<UserProfile>
    fun observeProfiles(): Flow<List<UserProfile>>

    

    suspend fun getDefaultProfile(): UserProfile?
    suspend fun setDefaultProfile(id: String)

    

    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteProfiles(): List<UserProfile>
    fun observeFavoriteProfiles(): Flow<List<UserProfile>>

    

    suspend fun getSettings(profileId: String): ProfileSettings?
    suspend fun updateSettings(profileId: String, settings: ProfileSettings)

    

    suspend fun searchProfiles(query: String): List<UserProfile>

    

    suspend fun exportProfile(id: String): String
    suspend fun importProfile(json: String): Boolean

    

    suspend fun getProfileCount(): Int
    suspend fun getProfileUsageStats(): Map<String, Int>
}