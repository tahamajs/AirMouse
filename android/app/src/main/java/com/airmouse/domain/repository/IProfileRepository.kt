package com.airmouse.domain.repository

import com.airmouse.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface IProfileRepository {
    fun getProfiles(): Flow<List<Profile>>
    fun getActiveProfileId(): Flow<String?>
    fun isLoading(): Flow<Boolean>
    suspend fun getProfile(id: String): Profile?
    suspend fun createProfile(name: String): Profile
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(id: String)
    suspend fun setActiveProfile(id: String)
    suspend fun duplicateProfile(id: String): Profile?
    suspend fun getActiveProfile(): Profile?
    suspend fun resetToDefault()
    suspend fun exportProfiles(): String
    suspend fun importProfiles(json: String): Boolean
    suspend fun getProfileCount(): Int
}
