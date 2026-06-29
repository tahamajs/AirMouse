package com.airmouse.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airmouse.data.datasource.local.entity.StatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {

    // ──────────────── Insert / Update ────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(stats: StatisticsEntity)

    /**
     * End a session: set is_active_legacy = 0 and store the end_time.
     */
    @Query("UPDATE statistics SET is_active_legacy = 0, end_time = :time WHERE id = :sessionId")
    suspend fun endSession(sessionId: String, time: Long)

    /**
     * Deactivate a session without setting an end_time.
     */
    @Query("UPDATE statistics SET is_active_legacy = 0 WHERE id = :id")
    suspend fun deactivateSession(id: String)

    // ──────────────── Single session queries ────────────────

    @Query("SELECT * FROM statistics WHERE is_active_legacy = 1 LIMIT 1")
    suspend fun getActiveSession(): StatisticsEntity?

    @Query("SELECT * FROM statistics WHERE id = :id")
    suspend fun getStatistics(id: String): StatisticsEntity?

    @Query("SELECT * FROM statistics WHERE is_active_legacy = 0 ORDER BY end_time DESC LIMIT 1")
    suspend fun getLastSession(): StatisticsEntity?

    // ──────────────── List queries ────────────────

    @Query("SELECT * FROM statistics WHERE is_active_legacy = 0 ORDER BY end_time DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<StatisticsEntity>

    // ──────────────── Flows (observers) ────────────────

    @Query("SELECT * FROM statistics ORDER BY start_time DESC")
    fun observeAllStatistics(): Flow<List<StatisticsEntity>>

    @Query("SELECT * FROM statistics WHERE is_active_legacy = 1 LIMIT 1")
    fun observeActiveSession(): Flow<StatisticsEntity?>

    // ──────────────── Aggregated stats ────────────────

    @Query("SELECT AVG(total_session_time) FROM statistics WHERE is_active_legacy = 0")
    suspend fun getAverageSessionTime(): Long?

    @Query("SELECT SUM(click_count) FROM statistics WHERE is_active_legacy = 0")
    suspend fun getTotalClicksAllTime(): Long?

    @Query("SELECT SUM(scroll_count) FROM statistics WHERE is_active_legacy = 0")
    suspend fun getTotalScrollsAllTime(): Long?

    // ──────────────── Delete / cleanup ────────────────

    @Query("DELETE FROM statistics")
    suspend fun deleteAll()

    @Query("DELETE FROM statistics WHERE is_active_legacy = 0 AND end_time < :timestamp")
    suspend fun deleteOldSessions(timestamp: Long)
}