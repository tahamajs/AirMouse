package com.airmouse.data.datasource.local

import com.airmouse.data.datasource.local.dao.GestureDao
import com.airmouse.data.datasource.local.dao.TrainingSampleDao
import com.airmouse.data.datasource.local.entity.GestureTemplateEntity
import com.airmouse.data.datasource.local.entity.TrainingSampleEntity
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureDataSourceImpl @Inject constructor(
    private val gestureDao: GestureDao,
    private val trainingSampleDao: TrainingSampleDao
) : IGestureDataSource {

    private val mutex = Mutex()
    private var confidenceThreshold: Float = 0.7f
    private var cooldownMs: Long = 500L
    private var customStats: GestureTrainingStats? = null

    // ============================================================
    // Sync methods
    // ============================================================

    override suspend fun getAllTemplates(): List<CustomGestureTemplate> {
        return gestureDao.getAllTemplates().map { it.toDomainModel() }
    }

    override suspend fun saveAllTemplates(templates: List<CustomGestureTemplate>) {
        templates.forEach { template ->
            gestureDao.insertTemplate(GestureTemplateEntity.fromDomainModel(template))
        }
    }

    override suspend fun saveGestureTemplate(template: CustomGestureTemplate) {
        val entity = GestureTemplateEntity.fromDomainModel(template)
        gestureDao.insertTemplate(entity)
    }

    override suspend fun deleteGestureTemplate(id: String) {
        gestureDao.deleteTemplate(id)
        trainingSampleDao.deleteSamplesByGesture(id)
    }

    // ============================================================
    // Additional methods
    // ============================================================

    override suspend fun getGestureTemplate(id: String): CustomGestureTemplate? {
        return gestureDao.getTemplateById(id)?.toDomainModel()
    }

    override suspend fun updateGestureTemplate(template: CustomGestureTemplate) {
        val entity = GestureTemplateEntity.fromDomainModel(template)
        gestureDao.updateTemplate(entity)
    }

    override suspend fun toggleFavorite(id: String) {
        val template = gestureDao.getTemplateById(id) ?: return
        gestureDao.setFavorite(id, !template.isFavorite)
    }

    override suspend fun getFavoriteTemplates(): List<CustomGestureTemplate> {
        return gestureDao.getFavoriteTemplates().map { it.toDomainModel() }
    }

    override suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>) {
        val entities = samples.map { sample ->
            TrainingSampleEntity(
                gestureId = gestureName,
                label = gestureName,
                gyroX = sample.getOrElse(0) { 0f },
                gyroY = sample.getOrElse(1) { 0f },
                gyroZ = sample.getOrElse(2) { 0f },
                accelX = sample.getOrElse(3) { 0f },
                accelY = sample.getOrElse(4) { 0f },
                accelZ = sample.getOrElse(5) { 0f },
                magX = sample.getOrElse(6) { 0f },
                magY = sample.getOrElse(7) { 0f },
                magZ = sample.getOrElse(8) { 0f },
                confidence = 1.0f,
                timestamp = System.currentTimeMillis()
            )
        }
        trainingSampleDao.insertSamples(entities)
        val template = gestureDao.getTemplateByName(gestureName)
        if (template != null) {
            gestureDao.incrementTrainingSamples(template.id)
        }
    }

    override suspend fun getTrainingSamples(gestureName: String): List<FloatArray> {
        val samples = trainingSampleDao.getSamplesByGesture(gestureName).ifEmpty {
            trainingSampleDao.getSamplesByLabel(gestureName)
        }
        return samples.map { sample ->
            floatArrayOf(
                sample.gyroX, sample.gyroY, sample.gyroZ,
                sample.accelX, sample.accelY, sample.accelZ,
                sample.magX, sample.magY, sample.magZ
            )
        }
    }

    override suspend fun clearTrainingData(gestureName: String) {
        trainingSampleDao.deleteSamplesByGesture(gestureName)
    }

    override suspend fun updateGestureStats(stats: GestureTrainingStats) {
        mutex.withLock {
            customStats = stats
        }
    }

    override suspend fun getGestureStats(): GestureTrainingStats {
        mutex.withLock {
            val stats = customStats
            if (stats != null) return stats

            val totalGestures = gestureDao.getTemplateCount()
            val mostUsed = gestureDao.getAllTemplatesByUsage().firstOrNull()?.name ?: "NONE"
            return GestureTrainingStats(
                totalGestures = totalGestures,
                mostUsedGesture = mostUsed,
                averageConfidence = gestureDao.getAverageConfidenceThreshold()
            )
        }
    }

    override suspend fun incrementGestureCount(gestureName: String, confidence: Float) {
        val template = gestureDao.getTemplateByName(gestureName)
        if (template != null) {
            gestureDao.incrementDetectionCount(template.id, System.currentTimeMillis())
        }
    }

    override suspend fun getGestureCount(): Int {
        return gestureDao.getTemplateCount()
    }

    override suspend fun getTotalDetections(): Int {
        return gestureDao.getTotalDetections()
    }

    override suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate> {
        return gestureDao.getAllTemplatesByUsage().take(limit).map { it.toDomainModel() }
    }

    override suspend fun setConfidenceThreshold(threshold: Float) {
        mutex.withLock {
            confidenceThreshold = threshold
        }
    }

    override suspend fun getConfidenceThreshold(): Float {
        return mutex.withLock { confidenceThreshold }
    }

    override suspend fun setCooldownMs(cooldown: Long) {
        mutex.withLock {
            cooldownMs = cooldown
        }
    }

    override suspend fun getCooldownMs(): Long {
        return mutex.withLock { cooldownMs }
    }

    override suspend fun resetAllStats() {
        mutex.withLock {
            customStats = null
        }
    }
}
