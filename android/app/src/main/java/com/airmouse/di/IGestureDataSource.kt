// app/src/main/java/com/airmouse/data/datasource/local/IGestureDataSource.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats

interface IGestureDataSource {

    // ==================== Gesture Templates ====================

    suspend fun saveGestureTemplate(template: CustomGestureTemplate)
    suspend fun getGestureTemplate(id: String): CustomGestureTemplate?
    suspend fun getAllGestureTemplates(): List<CustomGestureTemplate>
    suspend fun deleteGestureTemplate(id: String)
    suspend fun updateGestureTemplate(template: CustomGestureTemplate)

    // ==================== Favorite Gestures ====================

    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteTemplates(): List<CustomGestureTemplate>

    // ==================== Training Data ====================

    suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>)
    suspend fun getTrainingSamples(gestureName: String): List<FloatArray>
    suspend fun clearTrainingData(gestureName: String)

    // ==================== Statistics ====================

    suspend fun updateGestureStats(stats: GestureTrainingStats)
    suspend fun getGestureStats(): GestureTrainingStats

    // ==================== Detection Tracking ====================

    suspend fun incrementGestureCount(gestureName: String, confidence: Float)
    suspend fun getGestureCount(): Int
    suspend fun getTotalDetections(): Int
    suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate>

    // ==================== Configuration ====================

    suspend fun setConfidenceThreshold(threshold: Float)
    suspend fun getConfidenceThreshold(): Float
    suspend fun setCooldownMs(cooldown: Long)
    suspend fun getCooldownMs(): Long

    // ==================== Reset ====================

    suspend fun resetAllStats()
}