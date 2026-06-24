package com.airmouse.data.datasource.local

import com.airmouse.domain.model.Profile
import com.airmouse.domain.model.ProfileSettings

/**
 * Data source for user profiles and settings.
 * Supports multiple profiles, default profile, favorites, and profile-specific settings.
 */
interface IProfileDataSource {

    // ============================================================
    // Basic CRUD (used by DataSyncManager)
    // ============================================================

    /** Retrieves the default or active profile. */
    suspend fun getProfile(): Profile?

    /** Saves a profile (inserts or updates). */
    suspend fun saveProfile(profile: Profile)

    // ============================================================
    // Extended CRUD (multiple profiles)
    // ============================================================

    /** Retrieves a specific profile by ID. */
    suspend fun getProfile(id: String): Profile?

    /** Returns all profiles. */
    suspend fun getAllProfiles(): List<Profile>

    /** Deletes a profile by ID. */
    suspend fun deleteProfile(id: String)

    /** Updates an existing profile. */
    suspend fun updateProfile(profile: Profile)

    // ============================================================
    // Default Profile
    // ============================================================

    /** Sets a profile as the default. */
    suspend fun setDefaultProfile(id: String)

    /** Returns the default profile. */
    suspend fun getDefaultProfile(): Profile?

    // ============================================================
    // Favorites
    // ============================================================

    /** Toggles the favorite status of a profile. */
    suspend fun toggleFavorite(id: String)

    /** Returns all favorite profiles. */
    suspend fun getFavoriteProfiles(): List<Profile>

    // ============================================================
    // Profile Settings
    // ============================================================

    /** Saves settings for a specific profile. */
    suspend fun saveProfileSettings(profileId: String, settings: ProfileSettings)

    /** Retrieves settings for a specific profile. */
    suspend fun getProfileSettings(profileId: String): ProfileSettings?

    // ============================================================
    // Search
    // ============================================================

    /** Searches profiles by name or other fields. */
    suspend fun searchProfiles(query: String): List<Profile>
}