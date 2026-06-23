
package com.airmouse.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airmouse.data.datasource.local.entity.DailyStatsEntity

@Dao
interface DailyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 30")
    suspend fun getLast30DaysStats(): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getDailyStats(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getDailyStatsRange(startDate: String, endDate: String): List<DailyStatsEntity>

    @Query("UPDATE daily_stats SET clicks = clicks + 1 WHERE date = :date")
    suspend fun incrementClicks(date: String)

    @Query("UPDATE daily_stats SET scrolls = scrolls + 1 WHERE date = :date")
    suspend fun incrementScrolls(date: String)

    @Query("UPDATE daily_stats SET gestures = gestures + 1 WHERE date = :date")
    suspend fun incrementGestures(date: String)

    @Query("UPDATE daily_stats SET movements = movements + 1 WHERE date = :date")
    suspend fun incrementMovements(date: String)

    @Query("UPDATE daily_stats SET active_time = active_time + :duration WHERE date = :date")
    suspend fun incrementActiveTime(date: String, duration: Long)
}
