// app/src/main/java/com/airmouse/data/repository/GestureRepositoryImpl.kt
package com.airmouse.data.repository

import android.content.Context
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureRepositoryImpl @Inject constructor(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val gestureDetector: EnhancedGestureDetector
) : IGestureRepository {

    private val _customGestures = MutableStateFlow<List<CustomGestureTemplate>>(emptyList())
    override fun observeCustomGestures(): Flow<List<CustomGestureTemplate>> = _customGestures.asStateFlow()

    private val _gestureStats = MutableStateFlow(GestureTrainingStats())
    override fun observeGestureStats(): Flow<GestureTrainingStats> = _gestureStats.asStateFlow()

    init {
        loadCustomGestures()
    }

    private fun loadCustomGestures() {
        val json = prefs.getString("custom_gestures", "[]")
        val gestures = parseGesturesFromJson(json)
        _customGestures.value = gestures
    }

    private fun parseGesturesFromJson(json: String): List<CustomGestureTemplate> {
        val list = mutableListOf<CustomGestureTemplate>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val gesture = CustomGestureTemplate(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    type = try {
                        GestureType.valueOf(obj.getString("type"))
                    } catch (e: Exception) {
                        GestureType.CUSTOM
                    },
                    action = obj.getString("action"),
                    confidence = obj.optDouble("confidence", 0.7).toFloat(),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    usageCount = obj.optInt("usageCount", 0)
                )
                list.add(gesture)
            }
        } catch (e: Exception) {
            // Return empty list
        }
        return list
    }

    private fun saveCustomGestures() {
        val array = JSONArray()
        _customGestures.value.forEach { gesture ->
            val obj = JSONObject()
            obj.put("id", gesture.id)
            obj.put("name", gesture.name)
            obj.put("type", gesture.type.name)
            obj.put("action", gesture.action)
            obj.put("confidence", gesture.confidence)
            obj.put("isEnabled", gesture.isEnabled)
            obj.put("createdAt", gesture.createdAt)
            obj.put("updatedAt", gesture.updatedAt)
            obj.put("usageCount", gesture.usageCount)
            array.put(obj)
        }
        prefs.putString("custom_gestures", array.toString())
    }

    override suspend fun detectGesture(sensorData: FloatArray): GestureEvent {
        // Use EnhancedGestureDetector
        val gesture = gestureDetector.detect(
            sensorData[0], sensorData[1], sensorData[2],  // gyro
            sensorData[3], sensorData[4], sensorData[5],  // accel
            sensorData[6]  // roll
        )

        val gestureType = when (gesture) {
            EnhancedGestureDetector.Gesture.CLICK -> GestureType.CLICK
            EnhancedGestureDetector.Gesture.DOUBLE_CLICK -> GestureType.DOUBLE_CLICK
            EnhancedGestureDetector.Gesture.RIGHT_CLICK -> GestureType.RIGHT_CLICK
            EnhancedGestureDetector.Gesture.SCROLL_UP -> GestureType.SCROLL_UP
            EnhancedGestureDetector.Gesture.SCROLL_DOWN -> GestureType.SCROLL_DOWN
            EnhancedGestureDetector.Gesture.SWIPE_LEFT -> GestureType.SWIPE_LEFT
            EnhancedGestureDetector.Gesture.SWIPE_RIGHT -> GestureType.SWIPE_RIGHT
            EnhancedGestureDetector.Gesture.SWIPE_UP -> GestureType.SWIPE_UP
            EnhancedGestureDetector.Gesture.SWIPE_DOWN -> GestureType.SWIPE_DOWN
            else -> GestureType.NONE
        }

        return GestureEvent(
            type = gestureType,
            name = gestureType.name,
            confidence = 0.9f, // Would get from detector
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun detectGestureFromMotion(dx: Float, dy: Float): GestureType {
        // Simple motion-based gesture detection
        return when {
            abs(dx) > 50 && abs(dy) < 20 -> if (dx > 0) GestureType.SWIPE_RIGHT else GestureType.SWIPE_LEFT
            abs(dy) > 50 && abs(dx) < 20 -> if (dy > 0) GestureType.SWIPE_DOWN else GestureType.SWIPE_UP
            else -> GestureType.NONE
        }
    }

    override suspend fun addCustomGesture(gesture: CustomGestureTemplate): String {
        val id = java.util.UUID.randomUUID().toString()
        val newGesture = gesture.copy(id = id)
        val current = _customGestures.value.toMutableList()
        current.add(newGesture)
        _customGestures.value = current        saveCustomGestures()
        return id
    }

    override suspend fun updateCustomGesture(gesture: CustomGestureTemplate) {
        val current = _customGestures.value.toMutableList()
        val index = current.indexOfFirst { it.id == gesture.id }
        if (index >= 0) {
            current[index] = gesture.copy(updatedAt = System.currentTimeMillis())
            _customGestures.value = current
            saveCustomGestures()
        }
    }

    override suspend fun deleteCustomGesture(id: String) {
        val current = _customGestures.value.toMutableList()
        current.removeAll { it.id == id }
        _customGestures.value = current
        saveCustomGestures()
    }

    override suspend fun getCustomGesture(id: String): CustomGestureTemplate? {
        return _customGestures.value.find { it.id == id }
    }

    override suspend fun getAllCustomGestures(): List<CustomGestureTemplate> {
        return _customGestures.value
    }

    override suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Boolean {
        // In production, this would train the ML model
        // For now, just update stats
        updateGestureStats(gestureName, samples.size)
        return true
    }

    override suspend fun trainAllGestures(): Boolean {
        // Train all gestures using stored samples
        return true
    }

    override suspend fun getGestureStats(): GestureTrainingStats {
        return _gestureStats.value
    }

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

    override suspend fun setConfidenceThreshold(threshold: Float) {
        prefs.putFloat("gesture_confidence_threshold", threshold.coerceIn(0.5f, 0.95f))
    }

    override suspend fun getConfidenceThreshold(): Float {
        return prefs.getFloat("gesture_confidence_threshold", 0.7f)
    }

    override suspend fun setCooldownMs(cooldown: Long) {
        prefs.putLong("gesture_cooldown_ms", cooldown)
    }

    override suspend fun getCooldownMs(): Long {
        return prefs.getLong("gesture_cooldown_ms", 500L)
    }

    private fun updateGestureStats(gestureName: String, sampleCount: Int) {
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

        _gestureStats.value = GestureTrainingStats(
            totalGestures = current.totalGestures + sampleCount,
            gesturesByType = byType,
            mostUsedGesture = if (type != GestureType.NONE) gestureName else current.mostUsedGesture,
            lastGestureTime = System.currentTimeMillis(),
            customGestureUsage = customUsage,
            averageConfidence = 0.85f // Would calculate from actual data
        )
    }
}