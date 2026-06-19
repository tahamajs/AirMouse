// app/src/main/java/com/airmouse/data/datasource/local/TrainingSampleDao.kt
package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: TrainingSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<TrainingSampleEntity>)

    @Query("SELECT * FROM training_samples WHERE gesture_name = :gestureName ORDER BY timestamp DESC")
    suspend fun getSamplesByGesture(gestureName: String): List<TrainingSampleEntity>

    @Query("SELECT * FROM training_samples WHERE gesture_name = :gestureName ORDER BY timestamp DESC")
    fun observeSamplesByGesture(gestureName: String): Flow<List<TrainingSampleEntity>>

    @Query("SELECT * FROM training_samples ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSamples(limit: Int): List<TrainingSampleEntity>

    @Query("DELETE FROM training_samples WHERE gesture_name = :gestureName")
    suspend fun deleteSamplesByGesture(gestureName: String)

    @Query("DELETE FROM training_samples WHERE timestamp < :timestamp")
    suspend fun deleteSamplesOlderThan(timestamp: Long)

    @Query("SELECT COUNT(*) FROM training_samples WHERE gesture_name = :gestureName")
    suspend fun getSampleCount(gestureName: String): Int

    @Query("SELECT COUNT(*) FROM training_samples")
    suspend fun getTotalSampleCount(): Int

    @Query("DELETE FROM training_samples")
    suspend fun deleteAllSamples()
}