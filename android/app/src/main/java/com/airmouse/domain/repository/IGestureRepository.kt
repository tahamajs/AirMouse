// app/src/main/java/com/airmouse/domain/repository/IGestureRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureEvent
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.model.GestureType
import kotlinx.coroutines.flow.Flow

interface IGestureRepository {

    // ==================== Gesture Detection ====================

    /**
     * Detect gesture from sensor data
     */
    suspend fun detectGesture(sensorData: FloatArray): GestureEvent

    /**
     * Detect gesture from motion (simple swipe detection)
     */
    suspend fun detectGestureFromMotion(dx: Float, dy: Float): GestureType

    // ==================== Custom Gesture CRUD ====================

    /**
     * Add a new custom gesture
     * @return ID of the created gesture
     */
    suspend fun addCustomGesture(gesture: CustomGestureTemplate): String

    /**
     * Update an existing custom gesture
     */
    suspend fun updateCustomGesture(gesture: CustomGestureTemplate)

    /**
     * Delete a custom gesture by ID
     */
    suspend fun deleteCustomGesture(id: String)

    /**
     * Get a custom gesture by ID
     */
    suspend fun getCustomGesture(id: String): CustomGestureTemplate?

    /**
     * Get all custom gestures
     */
    suspend fun getAllCustomGestures(): List<CustomGestureTemplate>

    /**
     * Observe custom gestures (Flow for real-time updates)
     */
    fun observeCustomGestures(): Flow<List<CustomGestureTemplate>>

    // ==================== Gesture Training ====================

    /**
     * Train a gesture with samples
     */
    suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Boolean

    /**
     * Train all gestures
     */
    suspend fun trainAllGestures(): Boolean

    // ==================== Gesture Statistics ====================

    /**
     * Get gesture training statistics
     */
    suspend fun getGestureStats(): GestureTrainingStats

    /**
     * Observe gesture statistics (Flow for real-time updates)
     */
    fun observeGestureStats(): Flow<GestureTrainingStats>

    // ==================== Gesture Templates ====================

    /**
     * Load all gesture templates
     */
    suspend fun loadGestureTemplates(): List<CustomGestureTemplate>

    /**
     * Save a gesture template
     */
    suspend fun saveGestureTemplate(template: CustomGestureTemplate)

    /**
     * Delete a gesture template by ID
     */
    suspend fun deleteGestureTemplate(id: String)

    // ==================== Recognition Configuration ====================

    /**
     * Set confidence threshold for gesture recognition
     */
    suspend fun setConfidenceThreshold(threshold: Float)

    /**
     * Get confidence threshold
     */
    suspend fun getConfidenceThreshold(): Float

    /**
     * Set cooldown between gestures (ms)
     */
    suspend fun setCooldownMs(cooldown: Long)

    /**
     * Get cooldown between gestures (ms)
     */
    suspend fun getCooldownMs(): Long

    /**
     * Check if a gesture is recognized
     */
    suspend fun isGestureRecognized(): Boolean

    // ==================== Favorite Gestures ====================

    /**
     * Toggle favorite status of a gesture
     */
    suspend fun toggleFavorite(id: String)

    /**
     * Get favorite gestures
     */
    suspend fun getFavoriteGestures(): List<CustomGestureTemplate>

    /**
     * Observe favorite gestures
     */
    fun observeFavoriteGestures(): Flow<List<CustomGestureTemplate>>

    // ==================== Gesture Analytics ====================

    /**
     * Get most used gestures
     */
    suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate>

    /**
     * Get total gesture count
     */
    suspend fun getGestureCount(): Int

    /**
     * Get total detections
     */
    suspend fun getTotalDetections(): Int

    /**
     * Reset all gesture statistics
     */
    suspend fun resetStats()
}