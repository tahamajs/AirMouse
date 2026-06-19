// app/src/main/java/com/airmouse/data/datasource/local/IGestureDataSource.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.model.GestureType
import kotlinx.coroutines.flow.Flow

interface IGestureDataSource {

    // ==================== Gesture Templates ====================

    suspend fun saveGestureTemplate(template: CustomGestureTemplate)
    suspend fun getGestureTemplate(id: String): CustomGestureTemplate?
    suspend fun getAllGestureTemplates(): List<CustomGestureTemplate>
    suspend fun deleteGestureTemplate(id: String)
    suspend fun updateGestureTemplate(template: CustomGestureTemplate)
    suspend fun toggleGestureTemplate(id: String, enabled: Boolean)
    suspend fun toggleFavorite(id: String)
    suspend fun getEnabledTemplates(): List<CustomGestureTemplate>
    suspend fun getFavoriteTemplates(): List<CustomGestureTemplate>
    suspend fun searchTemplates(query: String): List<CustomGestureTemplate>
    suspend fun getTemplateCount(): Int

    fun observeAllTemplates(): Flow<List<CustomGestureTemplate>>
    fun observeEnabledTemplates(): Flow<List<CustomGestureTemplate>>
    fun observeFavoriteTemplates(): Flow<List<CustomGestureTemplate>>

    // ==================== Gesture Statistics ====================

    suspend fun getGestureStats(): GestureTrainingStats
    suspend fun incrementGestureCount(gesture: String, confidence: Float)
    suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate>
    suspend fun getGestureCount(): Int
    suspend fun getTotalDetections(): Int
    suspend fun resetAllStats()

    fun observeGestureStats(): Flow<GestureTrainingStats>

    // ==================== Training ====================

    suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>)
    suspend fun getTrainingSamples(gestureName: String): List<FloatArray>
    suspend fun clearTrainingData(gestureName: String)
    suspend fun getTrainingSampleCount(gestureName: String): Int

    // ==================== Configuration ====================

    suspend fun setConfidenceThreshold(threshold: Float)
    suspend fun getConfidenceThreshold(): Float
    suspend fun setCooldownMs(cooldown: Long)
    suspend fun getCooldownMs(): Long
}