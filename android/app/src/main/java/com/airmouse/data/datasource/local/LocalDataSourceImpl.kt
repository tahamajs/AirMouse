// app/src/main/java/com/airmouse/data/datasource/local/LocalDataSourceImpl.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.*
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
                gestureName = gestureName,
                sampleData = sample.joinToString(","),
                timestamp = System.currentTimeMillis()
            )
        }
        trainingSampleDao.insertSamples(entities)
    }

    override suspend fun getTrainingSamples(gestureName: String): List<FloatArray> {
        return trainingSampleDao.getSamplesByGesture(gestureName).map { entity ->
            entity.sampleData.split(",").map { it.toFloat() }.toFloatArray()
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
            sessionId = UUID.randomUUID().toString(),
            totalClicks = stats.totalClicks,
            totalDoubleClicks = stats.totalDoubleClicks,
            totalRightClicks = stats.totalRightClicks,
            totalScrolls = stats.totalScrolls,
            totalMovements = stats.totalMovements,
            totalDistance = stats.totalDistance,
            averageSpeed = stats.averageSpeed,
            maxSpeed = stats.maxSpeed,
            startTime = System.currentTimeMillis() - stats.sessionDuration,
            isActive = true
        )
        statisticsDao.insertStatistics(entity)
    }

    override suspend fun getSessionStats(): StatisticsSummary {
        val entity = statisticsDao.getActiveSession()
        return if (entity != null) {
            StatisticsSummary(
                totalClicks = entity.totalClicks,
                totalDoubleClicks = entity.totalDoubleClicks,
                totalRightClicks = entity.totalRightClicks,
                totalScrolls = entity.totalScrolls,
                totalMovements = entity.totalMovements,
                totalDistance = entity.totalDistance,
                averageSpeed = entity.averageSpeed,
                maxSpeed = entity.maxSpeed,
                sessionDuration = if (entity.isActive) {
                    System.currentTimeMillis() - entity.startTime
                } else {
                    entity.endTime - entity.startTime
                }
            )
        } else {
            StatisticsSummary()
        }
    }

    override suspend fun resetSessionStats() {
        val activeSession = statisticsDao.getActiveSession()
        activeSession?.let {
            statisticsDao.endSession(it.sessionId, System.currentTimeMillis())
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
        val byType = allStats.associate { it.gestureName to it.detectionCount }
        val mostUsed = allStats.maxByOrNull { it.detectionCount }
        val customStats = allStats.filter { it.isCustom }

        return HistoricalStatistics(
            totalGestures = allStats.sumOf { it.detectionCount },
            gesturesByType = byType,
            mostUsedGesture = mostUsed?.gestureName ?: "",
            lastGestureTime = allStats.maxOfOrNull { it.lastDetected } ?: 0,
            customGestureUsage = customStats.associate { it.gestureName to it.detectionCount }
        )
    }

    override suspend fun saveHistoricalStats(stats: HistoricalStatistics) {
        stats.gesturesByType.forEach { (gesture, count) ->
            val existing = gestureStatsDao.getGestureStats(gesture)
            if (existing != null) {
                gestureStatsDao.incrementDetection(gesture, stats.lastGestureTime)
            } else {
                gestureStatsDao.insertGestureStats(
                    GestureStatsEntity(
                        gestureName = gesture,
                        detectionCount = count,
                        lastDetected = stats.lastGestureTime,
                        isCustom = stats.customGestureUsage.containsKey(gesture)
                    )
                )
            }
        }
    }

    override suspend fun getGestureStats(): List<GestureStatistics> {
        return gestureStatsDao.getAllGestureStats().map { entity ->
            GestureStatistics(
                gestureName = entity.gestureName,
                detectionCount = entity.detectionCount,
                confidencePercentage = entity.confidencePercentage,
                lastDetected = entity.lastDetected
            )
        }
    }

    override suspend fun saveGestureStats(stats: List<GestureStatistics>) {
        gestureStatsDao.deleteAllGestureStats()
        stats.forEach { stat ->
            gestureStatsDao.insertGestureStats(
                GestureStatsEntity(
                    gestureName = stat.gestureName,
                    detectionCount = stat.detectionCount,
                    confidencePercentage = stat.confidencePercentage,
                    lastDetected = stat.lastDetected,
                    isCustom = stat.detectionCount > 0
                )
            )
        }
    }

    override suspend fun incrementGestureCount(gesture: String) {
        val existing = gestureStatsDao.getGestureStats(gesture)
        if (existing != null) {
            gestureStatsDao.incrementDetection(gesture, System.currentTimeMillis())
        } else {
            gestureStatsDao.insertGestureStats(
                GestureStatsEntity(
                    gestureName = gesture,
                    detectionCount = 1,
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