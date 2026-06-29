
package com.airmouse.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airmouse.data.datasource.local.entity.GestureStatsEntity

@Dao
interface GestureStatsDao {
// Add to GestureStatsDao.kt

    @Query("SELECT * FROM gesture_stats ORDER BY count DESC LIMIT :limit")
    suspend fun getTopGestures(limit: Int): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats WHERE last_detected > :timestamp")
    suspend fun getRecentGestures(timestamp: Long): List<GestureStatsEntity>

    @Query("SELECT SUM(count) FROM gesture_stats")
    suspend fun getTotalDetections(): Int

    @Query("SELECT AVG(avg_confidence) FROM gesture_stats")
    suspend fun getAverageConfidence(): Float

    @Query("SELECT COUNT(*) FROM gesture_stats")
    suspend fun getGestureCount(): Int

    @Query("SELECT * FROM gesture_stats WHERE is_custom = 1")
    suspend fun getCustomGestureStats(): List<GestureStatsEntity>

    @Query("DELETE FROM gesture_stats WHERE last_detected < :timestamp")
    suspend fun deleteOldStats(timestamp: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGestureStats(stats: GestureStatsEntity)

    @Query("SELECT * FROM gesture_stats")
    suspend fun getAllGestureStats(): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats")
    suspend fun getAllGestureStatsLegacy(): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats WHERE gesture_name = :name")
    suspend fun getGestureStats(name: String): GestureStatsEntity?

    @Query("UPDATE gesture_stats SET count = count + 1, avg_confidence = :confidence, last_detected = :timestamp WHERE gesture_name = :name")
    suspend fun incrementGestureCount(name: String, confidence: Float, timestamp: Long)

    @Query("UPDATE gesture_stats SET count = count + 1, avg_confidence = :confidence, last_detected = :timestamp WHERE gesture_name = :name")
    suspend fun incrementDetection(name: String, confidence: Float = 0f, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM gesture_stats WHERE gesture_name = :name")
    suspend fun deleteGestureStats(name: String)

    @Query("DELETE FROM gesture_stats")
    suspend fun deleteAll()

    @Query("DELETE FROM gesture_stats")
    suspend fun deleteAllGestureStats()
}
