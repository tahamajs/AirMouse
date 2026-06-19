//// app/src/main/java/com/airmouse/data/datasource/local/GestureDataSourceImpl.kt
//package com.airmouse.data.datasource.local
//
//import com.airmouse.domain.model.CustomGestureTemplate
//import com.airmouse.domain.model.GestureTrainingStats
//import com.airmouse.domain.model.GestureType
//import com.airmouse.utils.PreferencesManager
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//import org.json.JSONArray
//import org.json.JSONObject
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class GestureDataSourceImpl @Inject constructor(
//    private val prefs: PreferencesManager,
//    private val gestureStatsDao: GestureStatsDao,
//    private val gestureDao: GestureDao
//) : IGestureDataSource {
//
//    // ==================== Gesture Templates ====================
//
//    override suspend fun saveGestureTemplate(template: CustomGestureTemplate) {
//        val entity = mapToEntity(template)
//        gestureDao.insertTemplate(entity)
//    }
//
//    override suspend fun getGestureTemplate(id: String): CustomGestureTemplate? {
//        val entity = gestureDao.getTemplateById(id)
//        return entity?.let { mapToDomain(it) }
//    }
//
//    override suspend fun getAllGestureTemplates(): List<CustomGestureTemplate> {
//        return gestureDao.getAllTemplates().map { mapToDomain(it) }
//    }
//
//    override suspend fun deleteGestureTemplate(id: String) {
//        gestureDao.deleteTemplate(id)
//        gestureStatsDao.deleteGestureStats(id)
//    }
//
//    override suspend fun updateGestureTemplate(template: CustomGestureTemplate) {
//        val entity = mapToEntity(template)
//        gestureDao.updateTemplate(entity)
//    }
//
//    override suspend fun toggleGestureTemplate(id: String, enabled: Boolean) {
//        gestureDao.setTemplateEnabled(id, enabled)
//    }
//
//    override suspend fun toggleFavorite(id: String) {
//        val template = gestureDao.getTemplateById(id)
//        template?.let {
//            gestureDao.setTemplateFavorite(id, !it.isFavorite)
//        }
//    }
//
//    override suspend fun getEnabledTemplates(): List<CustomGestureTemplate> {
//        return gestureDao.getEnabledTemplates().map { mapToDomain(it) }
//    }
//
//    override suspend fun getFavoriteTemplates(): List<CustomGestureTemplate> {
//        return gestureDao.getFavoriteTemplates().map { mapToDomain(it) }
//    }
//
//    override suspend fun searchTemplates(query: String): List<CustomGestureTemplate> {
//        return gestureDao.searchTemplates(query).map { mapToDomain(it) }
//    }
//
//    override suspend fun getTemplateCount(): Int {
//        return gestureDao.getTemplateCount()
//    }
//
//    override fun observeAllTemplates(): Flow<List<CustomGestureTemplate>> {
//        return gestureDao.observeAllTemplates().map { entities ->
//            entities.map { mapToDomain(it) }
//        }
//    }
//
//    override fun observeEnabledTemplates(): Flow<List<CustomGestureTemplate>> {
//        return gestureDao.observeEnabledTemplates().map { entities ->
//            entities.map { mapToDomain(it) }
//        }
//    }
//
//    override fun observeFavoriteTemplates(): Flow<List<CustomGestureTemplate>> {
//        return gestureDao.observeFavoriteTemplates().map { entities ->
//            entities.map { mapToDomain(it) }
//        }
//    }
//
//    // ==================== Gesture Statistics ====================
//
//    override suspend fun getGestureStats(): GestureTrainingStats {
//        val allStats = gestureStatsDao.getAllGestureStats()
//        val byType = allStats.associate {
//            try {
//                GestureType.valueOf(it.gestureName) to it.detectionCount
//            } catch (e: Exception) {
//                GestureType.CUSTOM to it.detectionCount
//            }
//        }
//        val mostUsed = allStats.maxByOrNull { it.detectionCount }
//        val customStats = allStats.filter { it.isCustom }
//
//        return GestureTrainingStats(
//            totalGestures = allStats.sumOf { it.detectionCount },
//            gesturesByType = byType,
//            mostUsedGesture = mostUsed?.gestureName ?: "",
//            lastGestureTime = allStats.maxOfOrNull { it.lastDetected } ?: 0,
//            customGestureUsage = customStats.associate { it.gestureName to it.detectionCount },
//            averageConfidence = allStats.filter { it.detectionCount > 0 }
//                .map { it.getAverageConfidence() }
//                .average()
//                .toFloat()
//        )
//    }
//
//    override suspend fun incrementGestureCount(gesture: String, confidence: Float) {
//        val existing = gestureStatsDao.getGestureStats(gesture)
//        if (existing != null) {
//            gestureStatsDao.updateWithConfidence(gesture, confidence, System.currentTimeMillis())
//        } else {
//            gestureStatsDao.insertGestureStats(
//                GestureStatsEntity(
//                    gestureName = gesture,
//                    detectionCount = 1,
//                    confidencePercentage = confidence,
//                    lastDetected = System.currentTimeMillis(),
//                    totalConfidenceSum = confidence
//                )
//            )
//        }
//        // Also update template if exists
//        val template = gestureDao.getTemplateByName(gesture)
//        template?.let {
//            gestureDao.incrementDetectionCount(it.id, System.currentTimeMillis())
//        }
//    }
//
//    override suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate> {
//        val stats = gestureStatsDao.getMostUsedGestures(limit)
//        return stats.mapNotNull { stat ->
//            gestureDao.getTemplateByName(stat.gestureName)?.let { mapToDomain(it) }
//        }
//    }
//
//    override suspend fun getGestureCount(): Int {
//        return gestureStatsDao.getGestureCount()
//    }
//
//    override suspend fun getTotalDetections(): Int {
//        return gestureStatsDao.getTotalDetections()
//    }
//
//    override suspend fun resetAllStats() {
//        gestureStatsDao.deleteAllGestureStats()
//    }
//
//    override fun observeGestureStats(): Flow<GestureTrainingStats> {
//        return gestureStatsDao.observeAllGestureStats().map { stats ->
//            GestureTrainingStats(
//                totalGestures = stats.sumOf { it.detectionCount },
//                gesturesByType = stats.associate {
//                    try {
//                        GestureType.valueOf(it.gestureName) to it.detectionCount
//                    } catch (e: Exception) {
//                        GestureType.CUSTOM to it.detectionCount
//                    }
//                },
//                mostUsedGesture = stats.maxByOrNull { it.detectionCount }?.gestureName ?: "",
//                lastGestureTime = stats.maxOfOrNull { it.lastDetected } ?: 0,
//                customGestureUsage = stats.filter { it.isCustom }.associate { it.gestureName to it.detectionCount },
//                averageConfidence = stats.filter { it.detectionCount > 0 }
//                    .map { it.getAverageConfidence() }
//                    .average()
//                    .toFloat()
//            )
//        }
//    }
//
//    // ==================== Training ====================
//
//    override suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>) {
//        val key = "training_$gestureName"
//        val jsonArray = JSONArray()
//        samples.forEach { sample ->
//            val arr = JSONArray()
//            sample.forEach { arr.put(it) }
//            jsonArray.put(arr)
//        }
//        prefs.putString(key, jsonArray.toString())
//
//        // Update training count in template
//        val template = gestureDao.getTemplateByName(gestureName)
//        template?.let {
//            gestureDao.incrementTrainingSamples(it.id)
//        }
//    }
//
//    override suspend fun getTrainingSamples(gestureName: String): List<FloatArray> {
//        val key = "training_$gestureName"
//        val json = prefs.getString(key, "[]")
//        return try {
//            val array = JSONArray(json)
//            val result = mutableListOf<FloatArray>()
//            for (i in 0 until array.length()) {
//                val sampleArray = array.getJSONArray(i)
//                val sample = FloatArray(sampleArray.length()) { j ->
//                    sampleArray.getDouble(j).toFloat()
//                }
//                result.add(sample)
//            }
//            result
//        } catch (e: Exception) {
//            emptyList()
//        }
//    }
//
//    override suspend fun clearTrainingData(gestureName: String) {
//        val key = "training_$gestureName"
//        prefs.remove(key)
//    }
//
//    override suspend fun getTrainingSampleCount(gestureName: String): Int {
//        return getTrainingSamples(gestureName).size
//    }
//
//    // ==================== Configuration ====================
//
//    override suspend fun setConfidenceThreshold(threshold: Float) {
//        prefs.putFloat("gesture_confidence_threshold", threshold.coerceIn(0.5f, 0.95f))
//    }
//
//    override suspend fun getConfidenceThreshold(): Float {
//        return prefs.getFloat("gesture_confidence_threshold", 0.7f)
//    }
//
//    override suspend fun setCooldownMs(cooldown: Long) {
//        prefs.putLong("gesture_cooldown_ms", cooldown.coerceIn(100L, 3000L))
//    }
//
//    override suspend fun getCooldownMs(): Long {
//        return prefs.getLong("gesture_cooldown_ms", 500L)
//    }
//
//    // ==================== Mapping Functions ====================
//
//    private fun mapToDomain(entity: GestureTemplateEntity): CustomGestureTemplate {
//        return CustomGestureTemplate(
//            id = entity.id,
//            name = entity.name,
//            type = try { GestureType.valueOf(entity.type) } catch (e: Exception) { GestureType.CUSTOM },
//            action = entity.action,
//            confidence = entity.confidenceThreshold,
//            isEnabled = entity.isEnabled,
//            createdAt = entity.createdAt,
//            updatedAt = entity.updatedAt,
//            usageCount = entity.detectionCount
//        )
//    }
//
//    private fun mapToEntity(domain: CustomGestureTemplate): GestureTemplateEntity {
//        return GestureTemplateEntity(
//            id = domain.id,
//            name = domain.name,
//            type = domain.type.name,
//            action = domain.action,
//            confidenceThreshold = domain.confidence,
//            isEnabled = domain.isEnabled,
//            isCustom = domain.type == GestureType.CUSTOM,
//            detectionCount = domain.usageCount,
//            createdAt = domain.createdAt,
//            updatedAt = domain.updatedAt
//        )
//    }
//}