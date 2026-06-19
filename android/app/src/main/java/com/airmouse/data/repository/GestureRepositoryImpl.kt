// app/src/main/java/com/airmouse/data/repository/GestureRepositoryImpl.kt
package com.airmouse.data.repository

import android.content.Context
import com.airmouse.data.datasource.local.IGestureDataSource
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureRepositoryImpl @Inject constructor(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val gestureDetector: EnhancedGestureDetector,
    private val dataSource: IGestureDataSource
) : IGestureRepository {

    // ==================== State Flows ====================

    private val _customGestures = MutableStateFlow<List<CustomGestureTemplate>>(emptyList())
    override fun observeCustomGestures(): Flow<List<CustomGestureTemplate>> = _customGestures.asStateFlow()

    private val _gestureStats = MutableStateFlow(GestureTrainingStats())
    override fun observeGestureStats(): Flow<GestureTrainingStats> = _gestureStats.asStateFlow()

    private val _favoriteGestures = MutableStateFlow<List<CustomGestureTemplate>>(emptyList())
    override fun observeFavoriteGestures(): Flow<List<CustomGestureTemplate>> = _favoriteGestures.asStateFlow()

    private val _detectedGestures = MutableStateFlow<List<GestureEvent>>(emptyList())

    // ==================== Initialization ====================

    init {
        loadData()
    }

    private fun loadData() {
        loadCustomGestures()
        loadGestureStats()
        loadFavoriteGestures()
    }

    private fun loadCustomGestures() {
        // Load from data source
        // _customGestures.value = dataSource.getAllGestureTemplates()
    }

    private fun loadGestureStats() {
        // Load from data source
        // _gestureStats.value = dataSource.getGestureStats()
    }

    private fun loadFavoriteGestures() {
        // Load from data source
        // _favoriteGestures.value = dataSource.getFavoriteTemplates()
    }

    // ==================== Gesture Detection ====================

    override suspend fun detectGesture(sensorData: FloatArray): GestureEvent {
        val gesture = gestureDetector.detect(
            sensorData[0], sensorData[1], sensorData[2],
            sensorData[3], sensorData[4], sensorData[5],
            sensorData[6]
        )

        val gestureType = mapToGestureType(gesture)
        val confidence = 0.9f // Would get from detector

        val event = GestureEvent(
            type = gestureType,
            name = gestureType.name,
            confidence = confidence,
            timestamp = System.currentTimeMillis()
        )

        if (gestureType != GestureType.NONE && confidence >= getConfidenceThreshold()) {
            // Update statistics
            updateGestureStats(gestureType.name, confidence)

            // Increment detection count
            dataSource.incrementGestureCount(gestureType.name, confidence)

            // Update detected gestures list
            val current = _detectedGestures.value.toMutableList()
            current.add(event)
            if (current.size > 100) {
                current.removeAt(0)
            }
            _detectedGestures.value = current
        }

        return event
    }

    override suspend fun detectGestureFromMotion(dx: Float, dy: Float): GestureType {
        return when {
            abs(dx) > 50 && abs(dy) < 20 -> if (dx > 0) GestureType.SWIPE_RIGHT else GestureType.SWIPE_LEFT
            abs(dy) > 50 && abs(dx) < 20 -> if (dy > 0) GestureType.SWIPE_DOWN else GestureType.SWIPE_UP
            abs(dx) > 50 && abs(dy) > 50 -> {
                when {
                    dx > 0 && dy > 0 -> GestureType.CIRCLE_CW
                    dx < 0 && dy < 0 -> GestureType.CIRCLE_CCW
                    else -> GestureType.NONE
                }
            }
            else -> GestureType.NONE
        }
    }

    // ==================== Custom Gesture CRUD ====================

    override suspend fun addCustomGesture(gesture: CustomGestureTemplate): String {
        val id = java.util.UUID.randomUUID().toString()
        val newGesture = gesture.copy(id = id)
        dataSource.saveGestureTemplate(newGesture)

        // Update local cache
        val current = _customGestures.value.toMutableList()
        current.add(newGesture)
        _customGestures.value = current

        return id
    }

    override suspend fun updateCustomGesture(gesture: CustomGestureTemplate) {
        dataSource.updateGestureTemplate(gesture)

        val current = _customGestures.value.toMutableList()
        val index = current.indexOfFirst { it.id == gesture.id }
        if (index >= 0) {
            current[index] = gesture.copy(updatedAt = System.currentTimeMillis())
            _customGestures.value = current
        }
    }

    override suspend fun deleteCustomGesture(id: String) {
        dataSource.deleteGestureTemplate(id)

        val current = _customGestures.value.toMutableList()
        current.removeAll { it.id == id }
        _customGestures.value = current
    }

    override suspend fun getCustomGesture(id: String): CustomGestureTemplate? {
        return _customGestures.value.find { it.id == id }
    }

    override suspend fun getAllCustomGestures(): List<CustomGestureTemplate> {
        return _customGestures.value
    }

    // ==================== Gesture Training ====================

    override suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Boolean {
        // Save training samples
        dataSource.saveTrainingSamples(gestureName, samples)

        // Update statistics
        updateGestureStats(gestureName, 0.85f)

        return true
    }

    override suspend fun trainAllGestures(): Boolean {
        // Train all gestures using stored samples
        return true
    }

    // ==================== Gesture Statistics ====================

    override suspend fun getGestureStats(): GestureTrainingStats {
        return dataSource.getGestureStats()
    }

    // ==================== Gesture Templates ====================

    override suspend fun loadGestureTemplates(): List<CustomGestureTemplate> {
        return _customGestures.value
    }

    override suspend fun saveGestureTemplate(template: CustomGestureTemplate) {
        if (template.id.isEmpty()) {
            addCustomGesture(template)
        } else {
            updateCustomGesture(template)
        }
    }

    override suspend fun deleteGestureTemplate(id: String) {
        deleteCustomGesture(id)
    }

    // ==================== Recognition Configuration ====================

    override suspend fun setConfidenceThreshold(threshold: Float) {
        dataSource.setConfidenceThreshold(threshold)
    }

    override suspend fun getConfidenceThreshold(): Float {
        return dataSource.getConfidenceThreshold()
    }

    override suspend fun setCooldownMs(cooldown: Long) {
        dataSource.setCooldownMs(cooldown)
    }

    override suspend fun getCooldownMs(): Long {
        return dataSource.getCooldownMs()
    }

    override suspend fun isGestureRecognized(): Boolean {
        return _gestureStats.value.totalGestures > 0
    }

    // ==================== Favorite Gestures ====================

    override suspend fun toggleFavorite(id: String) {
        dataSource.toggleFavorite(id)
        loadFavoriteGestures()
    }

    override suspend fun getFavoriteGestures(): List<CustomGestureTemplate> {
        return dataSource.getFavoriteTemplates()
    }

    // ==================== Gesture Analytics ====================

    override suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate> {
        return dataSource.getMostUsedGestures(limit)
    }

    override suspend fun getGestureCount(): Int {
        return dataSource.getGestureCount()
    }

    override suspend fun getTotalDetections(): Int {
        return dataSource.getTotalDetections()
    }

    override suspend fun resetStats() {
        dataSource.resetAllStats()
        _gestureStats.value = GestureTrainingStats()
    }

    // ==================== Private Helpers ====================

    private fun mapToGestureType(gesture: EnhancedGestureDetector.Gesture): GestureType {
        return when (gesture) {
            EnhancedGestureDetector.Gesture.CLICK -> GestureType.CLICK
            EnhancedGestureDetector.Gesture.DOUBLE_CLICK -> GestureType.DOUBLE_CLICK
            EnhancedGestureDetector.Gesture.RIGHT_CLICK -> GestureType.RIGHT_CLICK
            EnhancedGestureDetector.Gesture.SCROLL_UP -> GestureType.SCROLL_UP
            EnhancedGestureDetector.Gesture.SCROLL_DOWN -> GestureType.SCROLL_DOWN
            EnhancedGestureDetector.Gesture.SWIPE_LEFT -> GestureType.SWIPE_LEFT
            EnhancedGestureDetector.Gesture.SWIPE_RIGHT -> GestureType.SWIPE_RIGHT
            EnhancedGestureDetector.Gesture.SWIPE_UP -> GestureType.SWIPE_UP
            EnhancedGestureDetector.Gesture.SWIPE_DOWN -> GestureType.SWIPE_DOWN
            EnhancedGestureDetector.Gesture.CIRCLE_CW -> GestureType.CIRCLE_CW
            EnhancedGestureDetector.Gesture.CIRCLE_CCW -> GestureType.CIRCLE_CCW
            EnhancedGestureDetector.Gesture.THUMBS_UP -> GestureType.THUMBS_UP
            EnhancedGestureDetector.Gesture.THUMBS_DOWN -> GestureType.THUMBS_DOWN
            EnhancedGestureDetector.Gesture.ZOOM_IN -> GestureType.ZOOM_IN
            EnhancedGestureDetector.Gesture.ZOOM_OUT -> GestureType.ZOOM_OUT
            EnhancedGestureDetector.Gesture.SHAKE -> GestureType.SHAKE
            else -> GestureType.NONE
        }
    }

    private suspend fun updateGestureStats(gestureName: String, confidence: Float) {
        val current = _gestureStats.value
        val byType = current.gesturesByType.toMutableMap()
        val type = try {
            GestureType.valueOf(gestureName.uppercase())
        } catch (e: Exception) {
            GestureType.CUSTOM
        }
        byType[type] = (byType[type] ?: 0) + 1

        val customUsage = current.customGestureUsage.toMutableMap()
        if (type == GestureType.CUSTOM) {
            customUsage[gestureName] = (customUsage[gestureName] ?: 0) + 1
        }

        val mostUsed = byType.maxByOrNull { it.value }?.key?.name ?: gestureName

        _gestureStats.value = GestureTrainingStats(
            totalGestures = current.totalGestures + 1,
            gesturesByType = byType,
            mostUsedGesture = mostUsed,
            lastGestureTime = System.currentTimeMillis(),
            customGestureUsage = customUsage,
            averageConfidence = (current.averageConfidence * current.totalGestures + confidence) / (current.totalGestures + 1)
        )
    }
}