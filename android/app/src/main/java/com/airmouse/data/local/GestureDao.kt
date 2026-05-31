// app/src/main/java/com/airmouse/data/local/GestureDao.kt
package com.airmouse.data.local

import androidx.room.*
import com.airmouse.domain.model.CustomGestureTemplate

@Dao
interface GestureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomGesture(template: CustomGestureTemplate)

    @Query("SELECT * FROM custom_gestures WHERE id = :id")
    suspend fun getCustomGesture(id: String): CustomGestureTemplate?

    @Query("SELECT * FROM custom_gestures")
    suspend fun getAllCustomGestures(): List<CustomGestureTemplate>

    @Delete
    suspend fun deleteCustomGesture(template: CustomGestureTemplate)

    @Query("DELETE FROM custom_gestures WHERE id = :id")
    suspend fun deleteCustomGesture(id: String)

    @Query("UPDATE custom_gestures SET threshold = :threshold WHERE id = :id")
    suspend fun updateGestureThreshold(id: String, threshold: Float)
}