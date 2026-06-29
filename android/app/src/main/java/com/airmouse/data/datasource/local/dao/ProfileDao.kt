package com.airmouse.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.airmouse.data.datasource.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    // Add to ProfileDao.kt

    @Query("SELECT * FROM profiles WHERE is_default = 1")
    suspend fun getDefaultProfileEntity(): ProfileEntity?

    @Query("UPDATE profiles SET is_active = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("UPDATE profiles SET is_active = 0 WHERE id = :id")
    suspend fun setInactive(id: String)

    @Query("SELECT * FROM profiles WHERE is_active = 1")
    suspend fun getActiveProfileEntity(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE is_favorite = 1")
    suspend fun getFavoriteProfilesEntities(): List<ProfileEntity>

    @Query("SELECT COUNT(*) FROM profiles WHERE is_favorite = 1")
    suspend fun getFavoriteProfileCount(): Int

    @Query("SELECT * FROM profiles ORDER BY last_used DESC LIMIT :limit")
    suspend fun getRecentlyUsedProfiles(limit: Int): List<ProfileEntity>

    @Query("DELETE FROM profiles WHERE last_used < :timestamp")
    suspend fun deleteOldProfiles(timestamp: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfile(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE is_active = 1")
    suspend fun getActiveProfile(): ProfileEntity?

    @Query("UPDATE profiles SET is_active = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE profiles SET is_active = 1 WHERE id = :id")
    suspend fun activateProfile(id: String)

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles")
    fun observeAllProfiles(): Flow<List<ProfileEntity>>

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("SELECT * FROM profiles WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultProfile(): ProfileEntity?

    @Query("UPDATE profiles SET is_default = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE profiles SET is_default = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: String)

    @Query("SELECT * FROM profiles WHERE is_favorite = 1")
    suspend fun getFavoriteProfiles(): List<ProfileEntity>

    @Query("UPDATE profiles SET is_favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("SELECT * FROM profiles WHERE name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%'")
    suspend fun searchProfiles(query: String): List<ProfileEntity>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int

    @Query("UPDATE profiles SET last_used = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)
}
