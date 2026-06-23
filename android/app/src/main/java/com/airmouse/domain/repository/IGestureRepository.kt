
package com.airmouse.domain.repository

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureEvent
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.model.GestureType
import kotlinx.coroutines.flow.Flow

interface IGestureRepository {

    

    suspend fun detectGesture(sensorData: FloatArray): GestureEvent

    suspend fun detectGestureFromMotion(dx: Float, dy: Float): GestureType

    

    suspend fun addCustomGesture(gesture: CustomGestureTemplate): String

    suspend fun updateCustomGesture(gesture: CustomGestureTemplate)

    suspend fun deleteCustomGesture(id: String)

    suspend fun getCustomGesture(id: String): CustomGestureTemplate?

    suspend fun getAllCustomGestures(): List<CustomGestureTemplate>

    fun observeCustomGestures(): Flow<List<CustomGestureTemplate>>

    

    suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Boolean

    suspend fun trainAllGestures(): Boolean

    

    suspend fun getGestureStats(): GestureTrainingStats

    fun observeGestureStats(): Flow<GestureTrainingStats>

    

    suspend fun loadGestureTemplates(): List<CustomGestureTemplate>

    suspend fun saveGestureTemplate(template: CustomGestureTemplate)

    suspend fun deleteGestureTemplate(id: String)

    

    suspend fun setConfidenceThreshold(threshold: Float)

    suspend fun getConfidenceThreshold(): Float

    suspend fun setCooldownMs(cooldown: Long)

    suspend fun getCooldownMs(): Long

    suspend fun isGestureRecognized(): Boolean

    

    suspend fun toggleFavorite(id: String)

    suspend fun getFavoriteGestures(): List<CustomGestureTemplate>

    fun observeFavoriteGestures(): Flow<List<CustomGestureTemplate>>

    

    suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate>

    suspend fun getGestureCount(): Int

    suspend fun getTotalDetections(): Int

    suspend fun resetStats()
}