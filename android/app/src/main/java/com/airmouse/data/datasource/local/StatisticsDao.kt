// app/src/main/java/com/airmouse/data/datasource/local/StatisticsDao.kt
package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(stats: StatisticsEntity)

    @Query("SELECT * FROM statistics WHERE id = 'default'")
    suspend fun getStatistics(): StatisticsEntity?

    @Query("UPDATE statistics SET click_count = click_count + 1 WHERE id = 'default'")
    suspend fun incrementClicks()

    @Query("UPDATE statistics SET double_click_count = double_click_count + 1 WHERE id = 'default'")
    suspend fun incrementDoubleClicks()

    @Query("UPDATE statistics SET right_click_count = right_click_count + 1 WHERE id = 'default'")
    suspend fun incrementRightClicks()

    @Query("UPDATE statistics SET scroll_count = scroll_count + 1, total_scroll_delta = total_scroll_delta + :delta WHERE id = 'default'")
    suspend fun incrementScrolls(delta: Long)

    @Query("UPDATE statistics SET movement_count = movement_count + 1, total_movement = total_movement + :distance WHERE id = 'default'")
    suspend fun incrementMovement(distance: Float)

    @Query("UPDATE statistics SET gesture_count = gesture_count + 1 WHERE id = 'default'")
    suspend fun incrementGestures()

    @Query("UPDATE statistics SET session_count = session_count + 1, total_session_time = total_session_time + :duration WHERE id = 'default'")
    suspend fun incrementSession(duration: Long)

    @Query("UPDATE statistics SET last_updated = :timestamp WHERE id = 'default'")
    suspend fun updateLastUpdated(timestamp: Long)

    @Query("DELETE FROM statistics")
    suspend fun deleteAll()
}