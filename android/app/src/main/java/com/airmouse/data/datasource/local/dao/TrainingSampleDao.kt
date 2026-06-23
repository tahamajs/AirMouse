
package com.airmouse.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airmouse.data.datasource.local.entity.TrainingSampleEntity

@Dao
interface TrainingSampleDao {

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
