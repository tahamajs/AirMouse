package com.airmouse.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airmouse.data.datasource.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: SettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: List<SettingsEntity>)

    @Query("SELECT * FROM settings WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingsEntity?

    @Query("SELECT * FROM settings ORDER BY key ASC")
    suspend fun getAllSettings(): List<SettingsEntity>

    @Query("SELECT * FROM settings ORDER BY key ASC")
    fun observeAllSettings(): Flow<List<SettingsEntity>>

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSetting(key: String)

    @Query("DELETE FROM settings")
    suspend fun clearSettings()
}
