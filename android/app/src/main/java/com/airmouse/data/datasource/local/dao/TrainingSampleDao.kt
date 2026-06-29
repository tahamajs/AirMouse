
package com.airmouse.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airmouse.data.datasource.local.entity.TrainingSampleEntity

@Dao
interface TrainingSampleDao {
    // Add to TrainingSampleDao.kt

    @Query("DELETE FROM training_samples WHERE timestamp < :timestamp")
    suspend fun deleteOldSamples(timestamp: Long)

    @Query("SELECT * FROM training_samples WHERE confidence > :minConfidence")
    suspend fun getHighConfidenceSamples(minConfidence: Float): List<TrainingSampleEntity>

    @Query("SELECT * FROM training_samples WHERE is_valid = 1")
    suspend fun getValidSamples(): List<TrainingSampleEntity>

    @Query("UPDATE training_samples SET is_valid = 0 WHERE id = :id")
    suspend fun invalidateSample(id: Long)

    @Query("SELECT COUNT(*) FROM training_samples WHERE gesture_id = :gestureId AND is_valid = 1")
    suspend fun getValidSampleCount(gestureId: String): Int

    @Query("DELETE FROM training_samples WHERE gesture_id = :gestureId AND is_valid = 0")
    suspend fun deleteInvalidSamples(gestureId: String)

    @Query("SELECT * FROM training_samples WHERE session_id = :sessionId")
    suspend fun getSamplesBySession(sessionId: String): List<TrainingSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingSample(sample: TrainingSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<TrainingSampleEntity>)

    @Query("SELECT * FROM training_samples WHERE gesture_id = :gestureId")
    suspend fun getSamplesByGesture(gestureId: String): List<TrainingSampleEntity>

    @Query("SELECT * FROM training_samples WHERE label = :label")
    suspend fun getSamplesByLabel(label: String): List<TrainingSampleEntity>

    @Query("DELETE FROM training_samples WHERE gesture_id = :gestureId")
    suspend fun deleteSamplesByGesture(gestureId: String)

    @Query("DELETE FROM training_samples WHERE id = :id")
    suspend fun deleteSample(id: Long)

    @Query("SELECT COUNT(*) FROM training_samples")
    suspend fun getSampleCount(): Int

    @Query("SELECT COUNT(*) FROM training_samples WHERE label = :label")
    suspend fun getSampleCountByLabel(label: String): Int
}
