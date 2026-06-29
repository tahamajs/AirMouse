package com.airmouse.domain.repository

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface IProfileRepository {

    // ============================================================
    // CRUD Operations
    // ============================================================

    suspend fun createProfile(profile: UserProfile): String
    suspend fun getProfile(id: String): UserProfile?
    suspend fun updateProfile(profile: UserProfile)
    suspend fun deleteProfile(id: String)
    suspend fun getAllProfiles(): List<UserProfile>
    fun observeProfiles(): Flow<List<UserProfile>>

    // ============================================================
    // Default Profile
    // ============================================================

    suspend fun getDefaultProfile(): UserProfile?
    suspend fun setDefaultProfile(id: String)

    // ============================================================
    // Favorites
    // ============================================================

    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteProfiles(): List<UserProfile>
    fun observeFavoriteProfiles(): Flow<List<UserProfile>>

    // ============================================================
    // Settings
    // ============================================================

    suspend fun getSettings(profileId: String): ProfileSettings?
    suspend fun updateSettings(profileId: String, settings: ProfileSettings)

    // ============================================================
    // Search
    // ============================================================

    suspend fun searchProfiles(query: String): List<UserProfile>

    // ============================================================
    // Export/Import
    // ============================================================

    suspend fun exportProfile(id: String): String
    suspend fun importProfile(json: String): Boolean

    // ============================================================
    // Statistics
    // ============================================================

    suspend fun getProfileCount(): Int
    suspend fun getProfileUsageStats(): Map<String, Int>
}