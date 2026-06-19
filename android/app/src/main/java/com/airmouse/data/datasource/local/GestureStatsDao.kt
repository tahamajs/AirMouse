// app/src/main/java/com/airmouse/data/datasource/local/GestureStatsDao.kt
package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureStatsDao {

    // ==================== Insert/Update ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGestureStats(stats: GestureStatsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGestureStatsIfNotExists(stats: GestureStatsEntity)

    // ==================== Get All ====================

    @Query("SELECT * FROM gesture_stats")
    suspend fun getAllGestureStats(): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats")
    fun observeAllGestureStats(): Flow<List<GestureStatsEntity>>

    // ==================== Get By Name ====================

    @Query("SELECT * FROM gesture_stats WHERE gestureName = :gestureName")
    suspend fun getGestureStats(gestureName: String): GestureStatsEntity?

    @Query("SELECT * FROM gesture_stats WHERE gestureName = :gestureName")
    fun observeGestureStats(gestureName: String): Flow<GestureStatsEntity?>

    // ==================== Get By Category ====================

    @Query("SELECT * FROM gesture_stats WHERE category = :category")
    suspend fun getGestureStatsByCategory(category: String): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats WHERE category = :category")
    fun observeGestureStatsByCategory(category: String): Flow<List<GestureStatsEntity>>

    // ==================== Get Most Used ====================

    @Query("SELECT * FROM gesture_stats ORDER BY detection_count DESC LIMIT :limit")
    suspend fun getMostUsedGestures(limit: Int): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats ORDER BY detection_count DESC LIMIT :limit")
    fun observeMostUsedGestures(limit: Int): Flow<List<GestureStatsEntity>>

    // ==================== Get Favorites ====================

    @Query("SELECT * FROM gesture_stats WHERE is_favorite = 1")
    suspend fun getFavoriteGestures(): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats WHERE is_favorite = 1")
    fun observeFavoriteGestures(): Flow<List<GestureStatsEntity>>

    // ==================== Get Custom ====================

    @Query("SELECT * FROM gesture_stats WHERE is_custom = 1")
    suspend fun getCustomGestureStats(): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats WHERE is_custom = 1")
    fun observeCustomGestureStats(): Flow<List<GestureStatsEntity>>

    // ==================== Update Operations ====================

    @Query("UPDATE gesture_stats SET detection_count = detection_count + 1, last_detected = :timestamp WHERE gestureName = :gestureName")
    suspend fun incrementDetection(gestureName: String, timestamp: Long)

    @Query("UPDATE gesture_stats SET confidence_percentage = :confidence WHERE gestureName = :gestureName")
    suspend fun updateConfidence(gestureName: String, confidence: Float)

    @Query("UPDATE gesture_stats SET total_confidence_sum = total_confidence_sum + :confidence, detection_count = detection_count + 1, last_detected = :timestamp WHERE gestureName = :gestureName")
    suspend fun updateWithConfidence(gestureName: String, confidence: Float, timestamp: Long)

    @Query("UPDATE gesture_stats SET is_favorite = :isFavorite WHERE gestureName = :gestureName")
    suspend fun updateFavorite(gestureName: String, isFavorite: Boolean)

    @Query("UPDATE gesture_stats SET category = :category WHERE gestureName = :gestureName")
    suspend fun updateCategory(gestureName: String, category: String)

    @Query("UPDATE gesture_stats SET avg_execution_time_ms = :avgTime WHERE gestureName = :gestureName")
    suspend fun updateExecutionTime(gestureName: String, avgTime: Float)

    // ==================== Delete Operations ====================

    @Query("DELETE FROM gesture_stats WHERE gestureName = :gestureName")
    suspend fun deleteGestureStats(gestureName: String)

    @Query("DELETE FROM gesture_stats")
    suspend fun deleteAllGestureStats()

    @Query("DELETE FROM gesture_stats WHERE last_detected < :timestamp")
    suspend fun deleteOldStats(timestamp: Long)

    // ==================== Count Operations ====================

    @Query("SELECT COUNT(*) FROM gesture_stats")
    suspend fun getGestureCount(): Int

    @Query("SELECT SUM(detection_count) FROM gesture_stats")
    suspend fun getTotalDetections(): Int

    @Query("SELECT COUNT(*) FROM gesture_stats WHERE is_custom = 1")
    suspend fun getCustomGestureCount(): Int

    @Query("SELECT COUNT(*) FROM gesture_stats WHERE is_favorite = 1")
    suspend fun getFavoriteGestureCount(): Int

    @Query("SELECT COUNT(*) FROM gesture_stats WHERE category = :category")
    suspend fun getGestureCountByCategory(category: String): Int

    // ==================== Search Operations ====================

    @Query("SELECT * FROM gesture_stats WHERE gestureName LIKE '%' || :query || '%'")
    suspend fun searchGestures(query: String): List<GestureStatsEntity>

    @Query("SELECT * FROM gesture_stats WHERE gestureName LIKE '%' || :query || '%'")
    fun observeSearchGestures(query: String): Flow<List<GestureStatsEntity>>

    @Query("SELECT * FROM gesture_stats WHERE gestureName LIKE '%' || :query || '%' AND is_custom = 1")
    suspend fun searchCustomGestures(query: String): List<GestureStatsEntity>

    // ==================== Analytics Operations ====================

    @Query("SELECT * FROM gesture_stats ORDER BY detection_count DESC LIMIT 5")
    suspend fun getTop5Gestures(): List<GestureStatsEntity>

    @Query("SELECT SUM(detection_count) FROM gesture_stats WHERE last_detected > :timestamp")
    suspend fun getDetectionsSince(timestamp: Long): Int

    @Query("SELECT AVG(confidence_percentage) FROM gesture_stats WHERE detection_count > 0")
    suspend fun getAverageConfidenceAll(): Float

    @Query("SELECT * FROM gesture_stats WHERE confidence_percentage < :threshold")
    suspend fun getLowConfidenceGestures(threshold: Float): List<GestureStatsEntity>
}
