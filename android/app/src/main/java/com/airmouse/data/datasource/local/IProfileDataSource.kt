
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile

interface IProfileDataSource {

    
    suspend fun saveProfile(profile: UserProfile)
    suspend fun getProfile(id: String): UserProfile?
    suspend fun getAllProfiles(): List<UserProfile>
    suspend fun deleteProfile(id: String)
    suspend fun updateProfile(profile: UserProfile)

    
    suspend fun setDefaultProfile(id: String)
    suspend fun getDefaultProfile(): UserProfile?

    
    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteProfiles(): List<UserProfile>

    
    suspend fun saveProfileSettings(profileId: String, settings: ProfileSettings)
    suspend fun getProfileSettings(profileId: String): ProfileSettings?

    
    suspend fun searchProfiles(query: String): List<UserProfile>
}