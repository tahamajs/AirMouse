// app/src/main/java/com/airmouse/data/datasource/local/GestureStatsDao.kt
package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GestureStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGestureStats(stats: GestureStatsEntity)

    @Query("SELECT * FROM gesture_stats")
    suspend fun getAllGestureStats(): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats WHERE gesture_name = :name")
    suspend fun getGestureStats(name: String): GestureStatsEntity?

    @Query("UPDATE gesture_stats SET count = count + 1, avg_confidence = :confidence, last_detected = :timestamp WHERE gesture_name = :name")
    suspend fun incrementGestureCount(name: String, confidence: Float, timestamp: Long)

    @Query("DELETE FROM gesture_stats WHERE gesture_name = :name")
    suspend fun deleteGestureStats(name: String)

    @Query("DELETE FROM gesture_stats")
    suspend fun deleteAll()
}