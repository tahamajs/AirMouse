package com.airmouse.data.datasource.local

import com.airmouse.data.datasource.local.dao.*
import com.airmouse.data.datasource.local.entity.*
import com.airmouse.data.mapper.DomainToEntityMapper
import com.airmouse.data.mapper.EntityToDomainMapper
import com.airmouse.domain.model.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified local data source implementation using Room DAOs.
 * Provides access to all local data: calibration, gestures, profiles, statistics.
 */
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

    // ============================================================
    // Calibration
    // ============================================================

    override suspend fun saveCalibrationData(data: CalibrationData) {
        val entity = DomainToEntityMapper.mapToEntity(data)
        calibrationDao.insertCalibration(entity)
    }

    override suspend fun getCalibrationData(): CalibrationData {
        val entity = calibrationDao.getCalibration()
        return entity?.let { EntityToDomainMapper.mapToDomain(it) } ?: CalibrationData()
    }

    override suspend fun resetCalibrationData() {
        calibrationDao.deleteAll()
    }

    // ============================================================
    // Gesture Templates
    // ============================================================

    override suspend fun saveGestureTemplate(template: CustomGestureTemplate) {
        val entity = mapToGestureTemplateEntity(template)
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
        val entity = mapToGestureTemplateEntity(template)
        gestureDao.updateTemplate(entity)
    }

    // ============================================================
    // Training Samples
    // ============================================================

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

    // ============================================================
    // Profile
    // ============================================================

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
        val entity = mapToProfileEntity(profile)
        profileDao.updateProfile(entity)
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

    // ============================================================
    // Session Statistics
    // ============================================================

    override suspend fun saveSessionStats(stats: StatisticsSummary) {
        val entity = StatisticsEntity(
            id = UUID.randomUUID().toString(),
            sessionId = UUID.randomUUID().toString(),
            totalMovement = stats.totalDistance,
            movementCount = stats.totalMovements.toLong(),
            clickCount = stats.totalClicks.toLong(),
            doubleClickCount = stats.totalDoubleClicks.toLong(),
            rightClickCount = stats.totalRightClicks.toLong(),
            scrollCount = stats.totalScrolls.toLong(),
            totalSessionTime = stats.sessionDuration,
            totalDistance = stats.totalDistance,
            totalClicks = stats.totalClicks,
            totalDoubleClicks = stats.totalDoubleClicks,
            totalRightClicks = stats.totalRightClicks,
            totalScrolls = stats.totalScrolls,
            averageSpeed = stats.averageSpeed,
            maxSpeed = stats.maxSpeed,
            startTime = System.currentTimeMillis() - stats.sessionDuration,
            lastReset = System.currentTimeMillis()
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
                averageSpeed = entity.averageSpeed,
                maxSpeed = entity.maxSpeed,
                sessionDuration = if (entity.isActive) {
                    System.currentTimeMillis() - entity.startTime
                } else {
                    entity.totalSessionTime
                }
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

    // ============================================================
    // Daily Statistics
    // ============================================================

    override suspend fun saveDailyStats(date: String, stats: DailyStats) {
        dailyStatsDao.insertDailyStats(
            DailyStatsEntity(
                date = date,
                clicks = stats.clicks,
                doubleClicks = stats.doubleClicks,
                rightClicks = stats.rightClicks,
                scrolls = stats.scrolls,
                movements = stats.movements,
                distance = stats.distance,
                gestures = stats.gestures,
                sessionTime = stats.totalTime,
                activeTime = stats.totalTime,
                lastUpdated = System.currentTimeMillis()
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
                distance = it.distance,
                gestures = it.gestures,
                totalTime = it.sessionTime
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
                distance = entity.distance,
                gestures = entity.gestures,
                totalTime = entity.sessionTime
            )
        }
    }

    // ============================================================
    // Historical Statistics
    // ============================================================

    override suspend fun getHistoricalStats(): HistoricalStatistics {
        val allStats = gestureStatsDao.getAllGestureStats()
        val byType = allStats.associate { it.gesture_name to it.count }
        val mostUsed = allStats.maxByOrNull { it.count }
        val customStats = allStats.filter { it.isCustom }
        val totalGestures = allStats.sumOf { it.count }
        val lastTime = allStats.maxOfOrNull { it.lastDetected } ?: 0

        return HistoricalStatistics(
            totalGestures = totalGestures,
            gesturesByType = byType,
            mostUsedGesture = mostUsed?.gesture_name ?: "",
            lastGestureTime = lastTime,
            customGestureUsage = customStats.associate { it.gesture_name to it.count },
            totalSessions = 1,
            averageGesturesPerSession = totalGestures.toFloat().coerceAtLeast(0f),
            totalClicks = 0,
            totalScrolls = 0,
            totalDoubleClicks = 0,
            totalRightClicks = 0,
            longestSessionMs = 0,
            firstSessionDate = lastTime,
            lastSessionDate = lastTime
        )
    }

    override suspend fun saveHistoricalStats(stats: HistoricalStatistics) {
        stats.gesturesByType.forEach { (gesture, count) ->
            val existing = gestureStatsDao.getGestureStats(gesture)
            if (existing != null) {
            gestureStatsDao.incrementDetection(
                name = gesture,
                confidence = 0.5f,
                timestamp = stats.lastGestureTime
            )
            } else {
                gestureStatsDao.insertGestureStats(
                    GestureStatsEntity(
                        gesture_name = gesture,
                        count = count,
                        avgConfidence = 0.5f,
                        lastDetected = stats.lastGestureTime,
                        detectionRate = if (count > 0) count.toFloat() else 0f,
                        isCustom = stats.customGestureUsage.containsKey(gesture)
                    )
                )
            }
        }
    }

    // ============================================================
    // Gesture Statistics
    // ============================================================

    override suspend fun getGestureStats(): List<GestureStatistics> {
        return gestureStatsDao.getAllGestureStats().map { entity ->
            GestureStatistics(
                gestureName = entity.gesture_name,
                detectionCount = entity.count,
                confidencePercentage = entity.avgConfidence.coerceAtLeast(0f),
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
                    isCustom = false
                )
            )
        }
    }

    override suspend fun incrementGestureCount(gesture: String) {
        val existing = gestureStatsDao.getGestureStats(gesture)
        if (existing != null) {
            gestureStatsDao.incrementDetection(
                name = gesture,
                confidence = 0.5f,
                timestamp = System.currentTimeMillis()
            )
        } else {
            gestureStatsDao.insertGestureStats(
                GestureStatsEntity(
                    gesture_name = gesture,
                    count = 1,
                    avgConfidence = 0.5f,
                    lastDetected = System.currentTimeMillis(),
                    detectionRate = 1f,
                    isCustom = false
                )
            )
        }
    }

    override suspend fun resetAllStats() {
        statisticsDao.deleteOldSessions(System.currentTimeMillis())
        gestureStatsDao.deleteAllGestureStats()
        dailyStatsDao.deleteAll() // If you add this method to DailyStatsDao
        // Note: You may need to add deleteAll() to DailyStatsDao
    }

    // ============================================================
    // Mapper Functions
    // ============================================================

    private fun mapToGestureTemplateEntity(template: CustomGestureTemplate): GestureTemplateEntity {
        return GestureTemplateEntity(
            id = template.id,
            name = template.name,
            type = template.type.name,
            action = template.action,
            confidence = template.confidence,
            confidenceThreshold = template.confidence,
            isEnabled = template.isEnabled,
            isCustom = template.type == GestureType.CUSTOM,
            detectionCount = template.usageCount,
            createdAt = template.createdAt,
            updatedAt = template.updatedAt,
            iconRes = 0,
            isSystem = template.isSystemGesture,
            isFavorite = template.isFavorite,
            trainingSamplesCount = 0,
            lastDetected = template.lastUsed,
            version = template.version,
            metadata = null,
            description = template.description
        )
    }

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
            usageCount = entity.detectionCount,
            lastUsed = entity.lastDetected,
            isFavorite = entity.isFavorite,
            isSystemGesture = entity.isSystem,
            version = entity.version,
            description = entity.description ?: ""
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
            updatedAt = entity.lastUsed,
            usageCount = 0
        )
    }
}
