package com.airmouse.data.datasource.local

import com.airmouse.domain.model.GestureTemplate
import com.airmouse.domain.model.GestureTrainingStats

/**
 * Data source for gesture operations.
 * Combines sync methods (for DataSyncManager) and full CRUD + training + stats.
 */
interface IGestureDataSource {
    // ============================================================
    // Sync methods (used by DataSyncManager)
    // ============================================================

    /** Returns all gesture templates. */
    suspend fun getAllTemplates(): List<GestureTemplate>

    /** Replaces the entire template list with the given list. */
    suspend fun saveAllTemplates(templates: List<GestureTemplate>)

    /** Saves a single template (inserts or updates). */
    suspend fun saveTemplate(template: GestureTemplate)

    /** Deletes a template by ID. */
    suspend fun deleteTemplate(id: String)

    // ============================================================
    // Additional methods (from the other interface)
    // ============================================================

    /** Retrieves a specific template by ID. */
    suspend fun getGestureTemplate(id: String): GestureTemplate?

    /** Updates an existing template. */
    suspend fun updateGestureTemplate(template: GestureTemplate)

    /** Toggles the 'favorite' flag on a template. */
    suspend fun toggleFavorite(id: String)

    /** Returns all templates marked as favorite. */
    suspend fun getFavoriteTemplates(): List<GestureTemplate>

    /** Saves training samples for a given gesture. */
    suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>)

    /** Retrieves training samples for a gesture. */
    suspend fun getTrainingSamples(gestureName: String): List<FloatArray>

    /** Clears training data for a gesture. */
    suspend fun clearTrainingData(gestureName: String)

    /** Updates the global gesture training statistics. */
    suspend fun updateGestureStats(stats: GestureTrainingStats)

    /** Retrieves the global gesture training statistics. */
    suspend fun getGestureStats(): GestureTrainingStats

    /** Increments the detection count for a specific gesture. */
    suspend fun incrementGestureCount(gestureName: String, confidence: Float)

    /** Returns the total number of gesture templates. */
    suspend fun getGestureCount(): Int

    /** Returns the total number of gesture detections recorded. */
    suspend fun getTotalDetections(): Int

    /** Returns the most frequently used gestures, limited by `limit`. */
    suspend fun getMostUsedGestures(limit: Int): List<GestureTemplate>

    /** Sets the global confidence threshold. */
    suspend fun setConfidenceThreshold(threshold: Float)

    /** Returns the global confidence threshold. */
    suspend fun getConfidenceThreshold(): Float

    /** Sets the gesture cooldown time in milliseconds. */
    suspend fun setCooldownMs(cooldown: Long)

    /** Returns the gesture cooldown time in milliseconds. */
    suspend fun getCooldownMs(): Long

    /** Resets all gesture statistics (counters, etc.). */
    suspend fun resetAllStats()
}