package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats

interface IGestureDataSource {
    suspend fun saveGestureTemplate(template: CustomGestureTemplate)
    suspend fun getGestureTemplate(id: String): CustomGestureTemplate?
    suspend fun getAllGestureTemplates(): List<CustomGestureTemplate>
    suspend fun deleteGestureTemplate(id: String)
    suspend fun updateGestureTemplate(template: CustomGestureTemplate)
    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteTemplates(): List<CustomGestureTemplate>
    suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>)
    suspend fun getTrainingSamples(gestureName: String): List<FloatArray>
    suspend fun clearTrainingData(gestureName: String)
    suspend fun updateGestureStats(stats: GestureTrainingStats)
    suspend fun getGestureStats(): GestureTrainingStats
    suspend fun incrementGestureCount(gestureName: String, confidence: Float)
    suspend fun getGestureCount(): Int
    suspend fun getTotalDetections(): Int
    suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate>
    suspend fun setConfidenceThreshold(threshold: Float)
    suspend fun getConfidenceThreshold(): Float
    suspend fun setCooldownMs(cooldown: Long)
    suspend fun getCooldownMs(): Long
    suspend fun resetAllStats()
}
