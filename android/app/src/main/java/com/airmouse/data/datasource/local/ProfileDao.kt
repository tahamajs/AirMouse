package com.airmouse.data.datasource.local

import androidx.room.*
import com.airmouse.domain.model.Profile

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Query("SELECT * FROM profiles WHERE name = :name")
    suspend fun getProfile(name: String): Profile?

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<Profile>

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("DELETE FROM profiles WHERE name = :name")
    suspend fun deleteProfile(name: String)
}