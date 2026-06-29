package com.airmouse.data.repository

import com.airmouse.data.datasource.local.IGestureDataSource
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.sensors.EnhancedGestureDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureRepositoryImpl @Inject constructor(
    private val gestureDetector: EnhancedGestureDetector,
    private val dataSource: IGestureDataSource
) : IGestureRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ============================================================
    // State Flows
    // ============================================================

    private val _customGestures = MutableStateFlow<List<CustomGestureTemplate>>(emptyList())
    override fun observeCustomGestures(): Flow<List<CustomGestureTemplate>> = _customGestures.asStateFlow()

    private val _gestureStats = MutableStateFlow(GestureTrainingStats())
    override fun observeGestureStats(): Flow<GestureTrainingStats> = _gestureStats.asStateFlow()

    private val _favoriteGestures = MutableStateFlow<List<CustomGestureTemplate>>(emptyList())
    override fun observeFavoriteGestures(): Flow<List<CustomGestureTemplate>> = _favoriteGestures.asStateFlow()

    private val _detectedGestures = MutableStateFlow<List<GestureEvent>>(emptyList())

    // ============================================================
    // Init
    // ============================================================

    init {
        loadData()
    }

    private fun loadData() {
        scope.launch {
            loadCustomGestures()
            loadGestureStats()
            loadFavoriteGestures()
        }
    }

    private suspend fun loadCustomGestures() {
        try {
            val templates = dataSource.getAllTemplates()
            _customGestures.value = templates
        } catch (_: Exception) {
            Timber.e("Failed to load custom gestures")
        }
    }

    private suspend fun loadGestureStats() {
        try {
            val stats = dataSource.getGestureStats()
            _gestureStats.value = stats
        } catch (_: Exception) {
            Timber.e("Failed to load gesture stats")
        }
    }

    private suspend fun loadFavoriteGestures() {
        try {
            val favorites = dataSource.getFavoriteTemplates()
            _favoriteGestures.value = favorites
        } catch (_: Exception) {
            Timber.e("Failed to load favorite gestures")
        }
    }

    // ============================================================
    // Gesture Detection
    // ============================================================

    override suspend fun detectGesture(sensorData: FloatArray): GestureEvent {
        return try {
            val gesture = gestureDetector.detect(
                sensorData[0], sensorData[1], sensorData[2],
                sensorData[3], sensorData[4], sensorData[5],
                sensorData[6]
            )

            val gestureType = mapToGestureType(gesture)
            val confidence = 0.9f

            val event = GestureEvent(
                type = gestureType,
                name = gestureType.name,
                confidence = confidence,
                timestamp = System.currentTimeMillis()
            )

            if (gestureType != GestureType.NONE && confidence >= getConfidenceThreshold()) {
                updateGestureStats(gestureType.name, confidence)
                dataSource.incrementGestureCount(gestureType.name, confidence)

                val current = _detectedGestures.value.toMutableList()
                current.add(event)
                if (current.size > 100) {
                    current.removeAt(0)
                }
                _detectedGestures.value = current
            }

            event
        } catch (_: Exception) {
            Timber.e("Gesture detection failed")
            GestureEvent(type = GestureType.NONE)
        }
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

    // ============================================================
    // Custom Gesture CRUD
    // ============================================================

    override suspend fun addCustomGesture(gesture: CustomGestureTemplate): String {
        val id = java.util.UUID.randomUUID().toString()
        val newGesture = gesture.copy(id = id)
        dataSource.saveGestureTemplate(newGesture)
        _customGestures.update { it + newGesture }
        return id
    }

    override suspend fun updateCustomGesture(gesture: CustomGestureTemplate) {
        dataSource.updateGestureTemplate(gesture)
        _customGestures.update { list ->
            list.map { if (it.id == gesture.id) gesture.copy(updatedAt = System.currentTimeMillis()) else it }
        }
    }

    override suspend fun deleteCustomGesture(id: String) {
        dataSource.deleteGestureTemplate(id)
        _customGestures.update { it.filter { it.id != id } }
    }

    override suspend fun getCustomGesture(id: String): CustomGestureTemplate? {
        return _customGestures.value.find { it.id == id }
    }

    override suspend fun getAllCustomGestures(): List<CustomGestureTemplate> {
        return _customGestures.value
    }

    // ============================================================
    // Gesture Training
    // ============================================================

    override suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Boolean {
        return try {
            dataSource.saveTrainingSamples(gestureName, samples)
            updateGestureStats(gestureName, 0.85f)
            true
        } catch (_: Exception) {
            Timber.e("Gesture training failed")
            false
        }
    }

    override suspend fun trainAllGestures(): Boolean {
        return try {
            val templates = _customGestures.value
            templates.forEach { template ->
                val samples = dataSource.getTrainingSamples(template.name)
                if (samples.isNotEmpty()) {
                    dataSource.saveTrainingSamples(template.name, samples)
                }
            }
            true
        } catch (_: Exception) {
            Timber.e("Training all gestures failed")
            false
        }
    }

    // ============================================================
    // Gesture Stats
    // ============================================================

    override suspend fun getGestureStats(): GestureTrainingStats {
        return try {
            dataSource.getGestureStats()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get gesture stats")
            GestureTrainingStats()
        }
    }

    // ============================================================
    // Gesture Templates
    // ============================================================

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

    // ============================================================
    // Settings
    // ============================================================

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

    // ============================================================
    // Favorites
    // ============================================================

    override suspend fun toggleFavorite(id: String) {
        dataSource.toggleFavorite(id)
        loadFavoriteGestures()
    }

    override suspend fun getFavoriteGestures(): List<CustomGestureTemplate> {
        return dataSource.getFavoriteTemplates()
    }

    // ============================================================
    // Statistics
    // ============================================================

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

    // ============================================================
    // Private Helpers
    // ============================================================

    private fun mapToGestureType(gesture: EnhancedGestureDetector.Gesture): GestureType {
        return when (gesture) {
            EnhancedGestureDetector.Gesture.CLICK -> GestureType.CLICK
            EnhancedGestureDetector.Gesture.DOUBLE_CLICK -> GestureType.DOUBLE_CLICK
            EnhancedGestureDetector.Gesture.RIGHT_CLICK -> GestureType.RIGHT_CLICK
            EnhancedGestureDetector.Gesture.SCROLL_UP -> GestureType.SWIPE_UP
            EnhancedGestureDetector.Gesture.SCROLL_DOWN -> GestureType.SWIPE_DOWN
            EnhancedGestureDetector.Gesture.SWIPE_LEFT -> GestureType.SWIPE_LEFT
            EnhancedGestureDetector.Gesture.SWIPE_RIGHT -> GestureType.SWIPE_RIGHT
            EnhancedGestureDetector.Gesture.SWIPE_UP -> GestureType.SWIPE_UP
            EnhancedGestureDetector.Gesture.SWIPE_DOWN -> GestureType.SWIPE_DOWN
            else -> GestureType.NONE
        }
    }

    private suspend fun updateGestureStats(gestureName: String, confidence: Float) {
        val current = _gestureStats.value
        val byType = current.gesturesByType.toMutableMap()
        val type = try {
            GestureType.valueOf(gestureName.uppercase())
        } catch (_: Exception) {
            GestureType.CUSTOM
        }
        byType[type] = (byType[type] ?: 0) + 1

        _gestureStats.value = GestureTrainingStats(
            totalGestures = current.totalGestures + 1,
            gesturesByType = byType,
            lastGestureTime = System.currentTimeMillis(),
            averageConfidence = (current.averageConfidence * current.totalGestures + confidence) / (current.totalGestures + 1)
        )
    }
}
