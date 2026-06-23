
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.utils.PreferencesManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IGestureDataSource {

    private val gson = Gson()
    private val templatesKey = "gesture_templates"
    private val trainingKeyPrefix = "training_"
    private val statsKey = "gesture_stats"
    private val favoritesKey = "favorite_gestures"
    private val confidenceThresholdKey = "gesture_confidence_threshold"
    private val cooldownMsKey = "gesture_cooldown_ms"

    

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

    

    override suspend fun toggleFavorite(id: String) {
        val favorites = getFavorites().toMutableList()
        if (favorites.contains(id)) {
            favorites.remove(id)
        } else {
            favorites.add(id)
        }
        prefs.putString(favoritesKey, gson.toJson(favorites))
    }

    override suspend fun getFavoriteTemplates(): List<CustomGestureTemplate> {
        val favoriteIds = getFavorites()
        return getAllGestureTemplates().filter { it.id in favoriteIds }
    }

    private fun getFavorites(): List<String> {
        val json = prefs.getString(favoritesKey, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    

    override suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>) {
        val key = trainingKeyPrefix + gestureName
        val json = gson.toJson(samples.map { it.toList() })
        prefs.putString(key, json)
    }

    override suspend fun getTrainingSamples(gestureName: String): List<FloatArray> {
        val key = trainingKeyPrefix + gestureName
        val json = prefs.getString(key, "[]")
        val type = object : TypeToken<List<List<Float>>>() {}.type
        val list: List<List<Float>> = gson.fromJson(json, type)
        return list.map { it.toFloatArray() }
    }

    override suspend fun clearTrainingData(gestureName: String) {
        prefs.remove(trainingKeyPrefix + gestureName)
    }

    

    override suspend fun updateGestureStats(stats: GestureTrainingStats) {
        val json = gson.toJson(stats)
        prefs.putString(statsKey, json)
    }

    override suspend fun getGestureStats(): GestureTrainingStats {
        val json = prefs.getString(statsKey, "{}")
        val type = object : TypeToken<GestureTrainingStats>() {}.type
        return gson.fromJson(json, type) ?: GestureTrainingStats()
    }

    

    override suspend fun incrementGestureCount(gestureName: String, confidence: Float) {
        val stats = getGestureStats()
        val updatedStats = stats.copy(
            totalGestures = stats.totalGestures + 1,
            lastGestureTime = System.currentTimeMillis(),
            averageConfidence = (stats.averageConfidence * stats.totalGestures + confidence) / (stats.totalGestures + 1)
        )
        updateGestureStats(updatedStats)

        val key = "detection_count_$gestureName"
        val currentCount = prefs.getInt(key, 0)
        prefs.putInt(key, currentCount + 1)
    }

    override suspend fun getGestureCount(): Int {
        val stats = getGestureStats()
        return stats.gesturesByType.values.sum()
    }

    override suspend fun getTotalDetections(): Int {
        val stats = getGestureStats()
        return stats.totalGestures
    }

    override suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate> {
        val templates = getAllGestureTemplates()
        return templates.sortedByDescending { it.usageCount }.take(limit)
    }

    

    override suspend fun setConfidenceThreshold(threshold: Float) {
        prefs.putFloat(confidenceThresholdKey, threshold)
    }

    override suspend fun getConfidenceThreshold(): Float {
        return prefs.getFloat(confidenceThresholdKey, 0.7f)
    }

    override suspend fun setCooldownMs(cooldown: Long) {
        prefs.putLong(cooldownMsKey, cooldown)
    }

    override suspend fun getCooldownMs(): Long {
        return prefs.getLong(cooldownMsKey, 500L)
    }

    

    override suspend fun resetAllStats() {
        prefs.remove(statsKey)
        val allKeys = prefs.getAllKeys()
        allKeys.filter { it.startsWith("detection_count_") }.forEach { prefs.remove(it) }
    }

    

    private fun parseTemplatesFromJson(json: String): List<CustomGestureTemplate> {
        val type = object : TypeToken<List<CustomGestureTemplate>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveTemplatesToPrefs(templates: List<CustomGestureTemplate>) {
        val json = gson.toJson(templates)
        prefs.putString(templatesKey, json)
    }
}