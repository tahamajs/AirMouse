package com.airmouse.data.datasource.local

import androidx.room.*
import com.airmouse.domain.model.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    // Insert/Update operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    // Query operations
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfile(id: String): Profile?

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeProfile(id: String): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE name = :name")
    suspend fun getProfileByName(name: String): Profile?

    @Query("SELECT * FROM profiles ORDER BY lastUsed DESC")
    suspend fun getAllProfiles(): List<Profile>

    @Query("SELECT * FROM profiles ORDER BY lastUsed DESC")
    fun observeAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): Profile?

    @Query("SELECT * FROM profiles WHERE isFavorite = 1 ORDER BY name")
    suspend fun getFavoriteProfiles(): List<Profile>

    // Delete operations
    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("DELETE FROM profiles WHERE isDefault = 0")
    suspend fun deleteAllCustomProfiles()

    // Update operations
    @Query("UPDATE profiles SET lastUsed = :lastUsed WHERE id = :id")
    suspend fun updateLastUsed(id: String, lastUsed: Long)

    @Query("UPDATE profiles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE profiles SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: String)

    // Check existence
    @Query("SELECT EXISTS(SELECT 1 FROM profiles WHERE name = :name)")
    suspend fun profileNameExists(name: String): Boolean
}// app/src/main/java/com/airmouse/data/datasource/local/ProfileDao.kt
