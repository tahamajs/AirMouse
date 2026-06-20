// app/src/main/java/com/airmouse/data/datasource/local/LocalDataSourceImpl.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.*
import com.airmouse.data.datasource.local.dao.CalibrationDao
import com.airmouse.data.datasource.local.dao.DailyStatsDao
import com.airmouse.data.datasource.local.dao.GestureStatsDao
import com.airmouse.data.datasource.local.dao.TrainingSampleDao
import com.airmouse.data.datasource.local.dao.GestureDao
import com.airmouse.data.datasource.local.dao.ProfileDao
import com.airmouse.data.datasource.local.dao.StatisticsDao
import com.airmouse.data.datasource.local.entity.CalibrationEntity
import com.airmouse.data.datasource.local.entity.DailyStatsEntity
import com.airmouse.data.datasource.local.entity.GestureStatsEntity
import com.airmouse.data.datasource.local.entity.GestureTemplateEntity
import com.airmouse.data.datasource.local.entity.ProfileEntity
import com.airmouse.data.datasource.local.entity.StatisticsEntity
import com.airmouse.data.datasource.local.entity.TrainingSampleEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSourceImpl @Inject constructor(
    private val calibrationDao: CalibrationDao,
    private val gestureDao: GestureDao,
    private val trainingSampleDao: TrainingSampleDao,
    private val profileDao: ProfileDao,
    private val statisticsDao: StatisticsDao,
    private val dailyStatsDao: DailyStatsDao,
    private val gestureStatsDao: GestureStatsDao
) : ILocalDataSource {

    // ==================== Calibration ====================

    override suspend fun saveCalibrationData(data: CalibrationData) {
        val entity = CalibrationEntity(
            gyroBiasX = data.gyroBias.offsetX,
            gyroBiasY = data.gyroBias.offsetY,
            gyroBiasZ = data.gyroBias.offsetZ,
            accelOffsetX = data.accelOffset.offsetX,
            accelOffsetY = data.accelOffset.offsetY,
            accelOffsetZ = data.accelOffset.offsetZ,
            accelScaleX = data.accelOffset.scaleX,
            accelScaleY = data.accelOffset.scaleY,
            accelScaleZ = data.accelOffset.scaleZ,
            magOffsetX = data.magOffset.offsetX,
            magOffsetY = data.magOffset.offsetY,
            magOffsetZ = data.magOffset.offsetZ,
            magScaleX = data.magOffset.scaleX,
            magScaleY = data.magOffset.scaleY,
            magScaleZ = data.magOffset.scaleZ,
            isCalibrated = data.isCalibrated,
            calibrationQuality = data.quality.name,
            timestamp = System.currentTimeMillis()
        )
        calibrationDao.insertCalibration(entity)
    }

    override suspend fun getCalibrationData(): CalibrationData {
        val entity = calibrationDao.getCalibration()
        return if (entity != null) {
            CalibrationData(
                gyroBias = SensorCalibrationData(
                    offsetX = entity.gyroBiasX,
                    offsetY = entity.gyroBiasY,
                    offsetZ = entity.gyroBiasZ
                ),
                accelOffset = SensorCalibrationData(
                    offsetX = entity.accelOffsetX,
                    offsetY = entity.accelOffsetY,
                    offsetZ = entity.accelOffsetZ,
                    scaleX = entity.accelScaleX,
                    scaleY = entity.accelScaleY,
                    scaleZ = entity.accelScaleZ
                ),
                magOffset = SensorCalibrationData(
                    offsetX = entity.magOffsetX,
                    offsetY = entity.magOffsetY,
                    offsetZ = entity.magOffsetZ,
                    scaleX = entity.magScaleX,
                    scaleY = entity.magScaleY,
                    scaleZ = entity.magScaleZ
                ),
                isCalibrated = entity.isCalibrated,
                quality = try {
                    CalibrationQuality.valueOf(entity.calibrationQuality)
                } catch (e: Exception) {
                    CalibrationQuality.UNKNOWN
                },
                timestamp = entity.timestamp
            )
        } else {
            CalibrationData()
        }
    }

    override suspend fun resetCalibrationData() {
        calibrationDao.deleteAll()
    }

    // ==================== Gestures ====================

    override suspend fun saveGestureTemplate(template: CustomGestureTemplate) {
        val entity = GestureTemplateEntity(
            id = template.id,
            name = template.name,
            type = template.type.name,
            action = template.action,
            confidenceThreshold = template.confidence,
            isEnabled = template.isEnabled,
            isCustom = template.type == GestureType.CUSTOM,
            detectionCount = template.usageCount,
            createdAt = template.createdAt,
            updatedAt = template.updatedAt
        )
        gestureDao.insertTemplate(entity)
    }

    override suspend fun getGestureTemplate(id: String): CustomGestureTemplate? {
        val entity = gestureDao.getTemplateById(id)
        return entity?.let { mapToCustomGesture(it) }
    }

    override suspend fun getAllGestureTemplates(): List<CustomGestureTemplate> {
        return gestureDao.getAllTemplates().map { mapToCustomGesture(it) }
    }

    override suspend fun deleteGestureTemplate(id: String) {
        gestureDao.deleteTemplate(id)
    }

    override suspend fun updateGestureTemplate(template: CustomGestureTemplate) {
        saveGestureTemplate(template)
    }

    override suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>) {
        trainingSampleDao.deleteSamplesByGesture(gestureName)
        val entities = samples.map { sample ->
            TrainingSampleEntity(
                gestureId = gestureName,
                gyroX = sample.getOrNull(0) ?: 0f,
                gyroY = sample.getOrNull(1) ?: 0f,
                gyroZ = sample.getOrNull(2) ?: 0f,
                accelX = sample.getOrNull(3) ?: 0f,
                accelY = sample.getOrNull(4) ?: 0f,
                accelZ = sample.getOrNull(5) ?: 0f,
                magX = sample.getOrNull(6) ?: 0f,
                magY = sample.getOrNull(7) ?: 0f,
                magZ = sample.getOrNull(8) ?: 0f,
                label = gestureName,
                confidence = 1f,
                timestamp = System.currentTimeMillis()
            )
        }
        trainingSampleDao.insertSamples(entities)
    }

    override suspend fun getTrainingSamples(gestureName: String): List<FloatArray> {
        return trainingSampleDao.getSamplesByGesture(gestureName).map { entity ->
            floatArrayOf(
                entity.gyroX,
                entity.gyroY,
                entity.gyroZ,
                entity.accelX,
                entity.accelY,
                entity.accelZ,
                entity.magX,
                entity.magY,
                entity.magZ
            )
        }
    }

    override suspend fun clearTrainingData(gestureName: String) {
        trainingSampleDao.deleteSamplesByGesture(gestureName)
    }

    // ==================== Profiles ====================

    override suspend fun saveProfile(profile: UserProfile) {
        val entity = mapToProfileEntity(profile)
        profileDao.insertProfile(entity)
    }

    override suspend fun getProfile(id: String): UserProfile? {
        val entity = profileDao.getProfileById(id)
        return entity?.let { mapToUserProfile(it) }
    }

    override suspend fun getAllProfiles(): List<UserProfile> {
        return profileDao.getAllProfiles().map { mapToUserProfile(it) }
    }

    override suspend fun deleteProfile(id: String) {
        profileDao.deleteProfile(id)
    }

    override suspend fun updateProfile(profile: UserProfile) {
        saveProfile(profile)
    }

    override suspend fun getDefaultProfile(): UserProfile? {
        val entity = profileDao.getDefaultProfile()
        return entity?.let { mapToUserProfile(it) }
    }

    override suspend fun setDefaultProfile(id: String) {
        profileDao.clearDefaultFlag()
        profileDao.setDefaultProfile(id)
    }

    override suspend fun getFavoriteProfiles(): List<UserProfile> {
        return profileDao.getFavoriteProfiles().map { mapToUserProfile(it) }
    }

    override suspend fun toggleFavorite(id: String) {
        val profile = profileDao.getProfileById(id)
        if (profile != null) {
            profileDao.setFavorite(id, !profile.isFavorite)
        }
    }

    // ==================== Statistics ====================

    override suspend fun saveSessionStats(stats: StatisticsSummary) {
        val entity = StatisticsEntity(
            id = UUID.randomUUID().toString(),
            totalMovement = stats.totalDistance,
            movementCount = stats.totalMovements.toLong(),
            clickCount = stats.totalClicks.toLong(),
            doubleClickCount = stats.totalDoubleClicks.toLong(),
            rightClickCount = stats.totalRightClicks.toLong(),
            scrollCount = stats.totalScrolls.toLong(),
            totalScrollDelta = 0L,
            gestureCount = 0L,
            sessionCount = 1L,
            totalSessionTime = stats.sessionDuration,
            lastReset = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )
        statisticsDao.insertStatistics(entity)
    }

    override suspend fun getSessionStats(): StatisticsSummary {
        val entity = statisticsDao.getActiveSession()
        return if (entity != null) {
            StatisticsSummary(
                totalClicks = entity.clickCount.toInt(),
                totalDoubleClicks = entity.doubleClickCount.toInt(),
                totalRightClicks = entity.rightClickCount.toInt(),
                totalScrolls = entity.scrollCount.toInt(),
                totalMovements = entity.movementCount.toInt(),
                totalDistance = entity.totalMovement,
                averageSpeed = 0f,
                maxSpeed = 0f,
                sessionDuration = entity.totalSessionTime
            )
        } else {
            StatisticsSummary()
        }
    }

    override suspend fun resetSessionStats() {
        val activeSession = statisticsDao.getActiveSession()
        activeSession?.let {
            statisticsDao.endSession(it.id, System.currentTimeMillis())
        }
    }

    override suspend fun saveDailyStats(date: String, stats: DailyStats) {
        dailyStatsDao.insertDailyStats(
            DailyStatsEntity(
                date = date,
                clicks = stats.clicks,
                doubleClicks = stats.doubleClicks,
                rightClicks = stats.rightClicks,
                scrolls = stats.scrolls,
                movements = stats.movements,
                distance = stats.distance
            )
        )
    }

    override suspend fun getDailyStats(date: String): DailyStats {
        val entity = dailyStatsDao.getDailyStats(date)
        return entity?.let {
            DailyStats(
                date = it.date,
                clicks = it.clicks,
                doubleClicks = it.doubleClicks,
                rightClicks = it.rightClicks,
                scrolls = it.scrolls,
                movements = it.movements,
                distance = it.distance
            )
        } ?: DailyStats(date = date)
    }

    override suspend fun getDailyStatsRange(startDate: String, endDate: String): List<DailyStats> {
        return dailyStatsDao.getDailyStatsRange(startDate, endDate).map { entity ->
            DailyStats(
                date = entity.date,
                clicks = entity.clicks,
                doubleClicks = entity.doubleClicks,
                rightClicks = entity.rightClicks,
                scrolls = entity.scrolls,
                movements = entity.movements,
                distance = entity.distance
            )
        }
    }

    override suspend fun getHistoricalStats(): HistoricalStatistics {
        val allStats = gestureStatsDao.getAllGestureStats()
        val byType = allStats.associate { it.gesture_name to it.count }
        val mostUsed = allStats.maxByOrNull { it.count }
        val customStats = allStats.filter { it.isCustom }

        return HistoricalStatistics(
            totalGestures = allStats.sumOf { it.count },
            gesturesByType = byType,
            mostUsedGesture = mostUsed?.gesture_name ?: "",
            lastGestureTime = allStats.maxOfOrNull { it.lastDetected } ?: 0,
            customGestureUsage = customStats.associate { it.gesture_name to it.count }
        )
    }

    override suspend fun saveHistoricalStats(stats: HistoricalStatistics) {
        stats.gesturesByType.forEach { (gesture, count) ->
            val existing = gestureStatsDao.getGestureStats(gesture)
            if (existing != null) {
                gestureStatsDao.incrementDetection(gesture, timestamp = stats.lastGestureTime)
            } else {
                gestureStatsDao.insertGestureStats(
                    GestureStatsEntity(
                        gesture_name = gesture,
                        count = count,
                        avgConfidence = 0f,
                        lastDetected = stats.lastGestureTime,
                        detectionRate = if (count > 0) count.toFloat() else 0f,
                        isCustom = stats.customGestureUsage.containsKey(gesture)
                    )
                )
            }
        }
    }

    override suspend fun getGestureStats(): List<GestureStatistics> {
        return gestureStatsDao.getAllGestureStats().map { entity ->
            GestureStatistics(
                gestureName = entity.gesture_name,
                detectionCount = entity.count,
                confidencePercentage = entity.avgConfidence,
                lastDetected = entity.lastDetected
            )
        }
    }

    override suspend fun saveGestureStats(stats: List<GestureStatistics>) {
        gestureStatsDao.deleteAllGestureStats()
        stats.forEach { stat ->
            gestureStatsDao.insertGestureStats(
                GestureStatsEntity(
                    gesture_name = stat.gestureName,
                    count = stat.detectionCount,
                    avgConfidence = stat.confidencePercentage,
                    lastDetected = stat.lastDetected,
                    detectionRate = stat.confidencePercentage,
                    isCustom = stat.detectionCount > 0
                )
            )
        }
    }

    override suspend fun incrementGestureCount(gesture: String) {
        val existing = gestureStatsDao.getGestureStats(gesture)
        if (existing != null) {
            gestureStatsDao.incrementDetection(gesture, timestamp = System.currentTimeMillis())
        } else {
            gestureStatsDao.insertGestureStats(
                GestureStatsEntity(
                    gesture_name = gesture,
                    count = 1,
                    lastDetected = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun resetAllStats() {
        statisticsDao.deleteOldSessions(System.currentTimeMillis())
        gestureStatsDao.deleteAllGestureStats()
        // Keep daily stats for historical tracking
    }

    // ==================== Mapping Functions ====================

    private fun mapToCustomGesture(entity: GestureTemplateEntity): CustomGestureTemplate {
        return CustomGestureTemplate(
            id = entity.id,
            name = entity.name,
            type = try { GestureType.valueOf(entity.type) } catch (e: Exception) { GestureType.CUSTOM },
            action = entity.action,
            confidence = entity.confidenceThreshold,
            isEnabled = entity.isEnabled,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            usageCount = entity.detectionCount
        )
    }

    private fun mapToProfileEntity(profile: UserProfile): ProfileEntity {
        return ProfileEntity(
            id = profile.id,
            name = profile.name,
            email = profile.email,
            avatarUri = profile.avatarUri,
            sensitivity = profile.settings.sensitivity,
            clickThreshold = profile.settings.clickThreshold,
            doubleClickInterval = profile.settings.doubleClickInterval,
            scrollThreshold = profile.settings.scrollThreshold,
            rightClickTilt = profile.settings.rightClickTilt,
            hapticEnabled = profile.settings.hapticEnabled,
            theme = profile.settings.theme,
            aiSmoothing = profile.settings.aiSmoothing,
            predictiveMovement = profile.settings.predictiveMovement,
            invertX = profile.settings.invertX,
            invertY = profile.settings.invertY,
            accelerationEnabled = profile.settings.accelerationEnabled,
            smoothingEnabled = profile.settings.smoothingEnabled,
            edgeGesturesEnabled = profile.settings.edgeGesturesEnabled,
            voiceCommandsEnabled = profile.settings.voiceCommandsEnabled,
            isDefault = profile.isDefault,
            isFavorite = profile.isFavorite,
            tags = profile.tags.joinToString(","),
            iconRes = profile.iconRes,
            createdAt = profile.createdAt,
            lastUsed = profile.updatedAt
        )
    }

    private fun mapToUserProfile(entity: ProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            avatarUri = entity.avatarUri,
            settings = ProfileSettings(
                sensitivity = entity.sensitivity,
                clickThreshold = entity.clickThreshold,
                doubleClickInterval = entity.doubleClickInterval,
                scrollThreshold = entity.scrollThreshold,
                rightClickTilt = entity.rightClickTilt,
                hapticEnabled = entity.hapticEnabled,
                theme = entity.theme,
                aiSmoothing = entity.aiSmoothing,
                predictiveMovement = entity.predictiveMovement,
                invertX = entity.invertX,
                invertY = entity.invertY,
                accelerationEnabled = entity.accelerationEnabled,
                smoothingEnabled = entity.smoothingEnabled,
                edgeGesturesEnabled = entity.edgeGesturesEnabled,
                voiceCommandsEnabled = entity.voiceCommandsEnabled
            ),
            isDefault = entity.isDefault,
            isFavorite = entity.isFavorite,
            tags = entity.tags?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            iconRes = entity.iconRes,
            createdAt = entity.createdAt,
            updatedAt = entity.lastUsed
        )
    }
}
