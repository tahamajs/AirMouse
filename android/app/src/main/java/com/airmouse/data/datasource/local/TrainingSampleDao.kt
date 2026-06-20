// app/src/main/java/com/airmouse/data/datasource/local/TrainingSampleDao.kt
package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrainingSampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingSample(sample: TrainingSampleEntity)

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