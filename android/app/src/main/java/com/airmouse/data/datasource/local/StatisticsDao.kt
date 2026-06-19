// app/src/main/java/com/airmouse/data/datasource/local/StatisticsDao.kt
package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(stats: StatisticsEntity)

    @Update
    suspend fun updateStatistics(stats: StatisticsEntity)

    @Query("SELECT * FROM statistics WHERE session_id = :sessionId")
    suspend fun getStatisticsBySession(sessionId: String): StatisticsEntity?

    @Query("SELECT * FROM statistics WHERE is_active = 1 ORDER BY start_time DESC LIMIT 1")
    suspend fun getActiveSession(): StatisticsEntity?

    @Query("SELECT * FROM statistics WHERE is_active = 1")
    fun observeActiveSession(): Flow<StatisticsEntity?>

    @Query("SELECT * FROM statistics ORDER BY start_time DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<StatisticsEntity>

    @Query("UPDATE statistics SET is_active = 0, end_time = :endTime WHERE session_id = :sessionId")
    suspend fun endSession(sessionId: String, endTime: Long)

    @Query("DELETE FROM statistics WHERE end_time < :timestamp AND is_active = 0")
    suspend fun deleteOldSessions(timestamp: Long)

    @Query("SELECT SUM(total_clicks) FROM statistics")
    suspend fun getTotalClicks(): Int

    @Query("SELECT SUM(total_movements) FROM statistics")
    suspend fun getTotalMovements(): Int

    @Query("SELECT SUM(total_distance) FROM statistics")
    suspend fun getTotalDistance(): Float

    @Query("SELECT AVG(average_speed) FROM statistics WHERE average_speed > 0")
    suspend fun getAverageSpeed(): Float

    @Query("SELECT COUNT(*) FROM statistics")
    suspend fun getSessionCount(): Int
}