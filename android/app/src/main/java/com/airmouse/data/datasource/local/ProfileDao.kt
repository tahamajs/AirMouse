package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles")
    fun observeAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE name LIKE '%' || :query || '%'")
    suspend fun searchProfiles(query: String): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE is_default = 1")
    suspend fun getDefaultProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE is_favorite = 1")
    suspend fun getFavoriteProfiles(): List<ProfileEntity>

    @Query("UPDATE profiles SET is_default = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE profiles SET is_default = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: String)

    @Query("UPDATE profiles SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE profiles SET last_used = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int
}
