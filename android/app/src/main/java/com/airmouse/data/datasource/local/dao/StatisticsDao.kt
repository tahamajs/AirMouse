package com.airmouse.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airmouse.data.datasource.local.entity.StatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(stats: StatisticsEntity)

    @Query("SELECT * FROM statistics WHERE id = :id")
    suspend fun getStatistics(id: String): StatisticsEntity?

    @Query("SELECT * FROM statistics")
    fun observeStatistics(): Flow<StatisticsEntity?>

    @Query("DELETE FROM statistics")
    suspend fun deleteAll()

    @Query("SELECT * FROM statistics WHERE id = 'default' LIMIT 1")
    suspend fun getActiveSession(): StatisticsEntity?

    @Query("UPDATE statistics SET last_updated = :timestamp WHERE id = :sessionId")
    suspend fun endSession(sessionId: String, timestamp: Long)

    @Query("DELETE FROM statistics WHERE last_updated < :timestamp")
    suspend fun deleteOldSessions(timestamp: Long)
}
