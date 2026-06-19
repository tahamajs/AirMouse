// app/src/main/java/com/airmouse/data/datasource/local/GestureDataSourceImpl.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.model.GestureType
import com.airmouse.utils.PreferencesManager
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IGestureDataSource {

    private val templatesKey = "gesture_templates"
    private val trainingKeyPrefix = "training_"
    private val statsKey = "gesture_stats"

    override suspend fun saveGestureTemplate(template: CustomGestureTemplate) {
        val templates = getAllGestureTemplates().toMutableList()
        val existingIndex = templates.indexOfFirst { it.id == template.id }
        if (existingIndex >= 0) {
            templates[existingIndex] = template
        } else {
            templates.add(template)
        }
        saveTemplatesToPrefs(templates)
    }

    override suspend fun getGestureTemplate(id: String): CustomGestureTemplate? {
        return getAllGestureTemplates().find { it.id == id }
    }

    override suspend fun getAllGestureTemplates(): List<CustomGestureTemplate> {
        val json = prefs.getString(templatesKey, "[]")
        return parseTemplatesFromJson(json)
    }

    override suspend fun deleteGestureTemplate(id: String) {
        val templates = getAllGestureTemplates().toMutableList()
        templates.removeAll { it.id == id }
        saveTemplatesToPrefs(templates)
    }

    override suspend fun updateGestureTemplate(template: CustomGestureTemplate) {
        saveGestureTemplate(template)
    }

    override suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>) {
        val key = trainingKeyPrefix + gestureName
        val jsonArray = JSONArray()
        samples.forEach { sample ->
            val arr = JSONArray()
            sample.forEach { arr.put(it) }
            jsonArray.put(arr)
        }
        prefs.putString(key, jsonArray.toString())
    }

    override suspend fun getTrainingSamples(gestureName: String): List<FloatArray> {
        val key = trainingKeyPrefix + gestureName
        val json = prefs.getString(key, "[]")
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<FloatArray>()
            for (i in 0 until array.length()) {
                val sampleArray = array.getJSONArray(i)
                val sample = FloatArray(sampleArray.length()) { j ->
                    sampleArray.getDouble(j).toFloat()
                }
                result.add(sample)
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun clearTrainingData(gestureName: String) {
        val key = trainingKeyPrefix + gestureName
        prefs.remove(key)
    }

    override suspend fun updateGestureStats(stats: GestureTrainingStats) {
        val obj = JSONObject()
        obj.put("totalGestures", stats.totalGestures)
        obj.put("mostUsedGesture", stats.mostUsedGesture)
        obj.put("lastGestureTime", stats.lastGestureTime)
        obj.put("averageConfidence", stats.averageConfidence)

        val byType = JSONObject()
        stats.gesturesByType.forEach { (key, value) ->
            byType.put(key.name, value)
        }
        obj.put("gesturesByType", byType)

        val customUsage = JSONObject()
        stats.customGestureUsage.forEach { (key, value) ->
            customUsage.put(key, value)
        }
        obj.put("customGestureUsage", customUsage)

        prefs.putString(statsKey, obj.toString())
    }

    override suspend fun getGestureStats(): GestureTrainingStats {
        val json = prefs.getString(statsKey, "{}")
        return try {
            val obj = JSONObject(json)
            val byType = mutableMapOf<GestureType, Int>()
            val byTypeJson = obj.optJSONObject("gesturesByType")
            byTypeJson?.keys()?.forEach { key ->
                try {
                    byType[GestureType.valueOf(key)] = byTypeJson.getInt(key)
                } catch (e: Exception) {
                    // Skip invalid keys
                }
            }

            val customUsage = mutableMapOf<String, Int>()
            val customJson = obj.optJSONObject("customGestureUsage")
            customJson?.keys()?.forEach { key ->
                customUsage[key] = customJson.getInt(key)
            }

            GestureTrainingStats(
                totalGestures = obj.optInt("totalGestures", 0),
                gesturesByType = byType,
                mostUsedGesture = obj.optString("mostUsedGesture", ""),
                lastGestureTime = obj.optLong("lastGestureTime", 0),
                customGestureUsage = customUsage,
                averageConfidence = obj.optDouble("averageConfidence", 0.0).toFloat()
            )
        } catch (e: Exception) {
            GestureTrainingStats()
        }
    }

    override suspend fun setConfidenceThreshold(threshold: Float) {
        prefs.putFloat("gesture_confidence_threshold", threshold)
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

    // ==================== Private Helpers ====================

    private fun parseTemplatesFromJson(json: String): List<CustomGestureTemplate> {
        val list = mutableListOf<CustomGestureTemplate>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val template = CustomGestureTemplate(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    type = try {
                        GestureType.valueOf(obj.getString("type"))
                    } catch (e: Exception) {
                        GestureType.CUSTOM
                    },
                    action = obj.getString("action"),
                    confidence = obj.optDouble("confidence", 0.7f).toFloat(),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    usageCount = obj.optInt("usageCount", 0)
                )
                list.add(template)
            }
        } catch (e: Exception) {
            // Return empty list on error
        }
        return list
    }

    private fun saveTemplatesToPrefs(templates: List<CustomGestureTemplate>) {
        val array = JSONArray()
        templates.forEach { template ->
            val obj = JSONObject()
            obj.put("id", template.id)
            obj.put("name", template.name)
            obj.put("type", template.type.name)
            obj.put("action", template.action)
            obj.put("confidence", template.confidence)
            obj.put("isEnabled", template.isEnabled)
            obj.put("createdAt", template.createdAt)
            obj.put("updatedAt", template.updatedAt)
            obj.put("usageCount", template.usageCount)
            array.put(obj)
        }
        prefs.putString(templatesKey, array.toString())
    }
}