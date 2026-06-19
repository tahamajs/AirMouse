// app/src/main/java/com/airmouse/data/mapper/DomainToEntityMapper.kt
package com.airmouse.data.mapper

import com.airmouse.data.datasource.local.*
import com.airmouse.domain.model.*
import java.util.UUID

// ==================== Domain to Entity Mapper ====================

object DomainToEntityMapper {

    // ==================== Calibration ====================

    fun mapToEntity(data: CalibrationData): CalibrationEntity {
        return CalibrationEntity(
            id = "default",
            gyroBiasX = data.gyroBias.offsetX,
            gyroBiasY = data.gyroBias.offsetY,
            gyroBiasZ = data.gyroBias.offsetZ,
            gyroVarianceX = 0f,
            gyroVarianceY = 0f,
            gyroVarianceZ = 0f,
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
            calibrationQuality = data.quality.name
        )
    }

    // ==================== Statistics ====================

    fun mapToEntity(stats: StatisticsSummary): StatisticsEntity {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        return StatisticsEntity(
            sessionId = sessionId,
            totalClicks = stats.totalClicks,
            totalDoubleClicks = stats.totalDoubleClicks,
            totalRightClicks = stats.totalRightClicks,
            totalScrolls = stats.totalScrolls,
            totalMovements = stats.totalMovements,
            totalDistance = stats.totalDistance,
            averageSpeed = stats.averageSpeed,
            maxSpeed = stats.maxSpeed,
            startTime = now - stats.sessionDuration,
            endTime = 0,
            isActive = true
        )
    }

    fun mapToEntity(stats: DailyStats): DailyStatsEntity {
        return DailyStatsEntity(
            date = stats.date,
            clicks = stats.clicks,
            doubleClicks = stats.doubleClicks,
            rightClicks = stats.rightClicks,
            scrolls = stats.scrolls,
            movements = stats.movements,
            distance = stats.distance,
            gestures = 0,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun mapToEntity(stat: GestureStatistics): GestureStatsEntity {
        return GestureStatsEntity(
            gestureName = stat.gestureName,
            detectionCount = stat.detectionCount,
            confidencePercentage = stat.confidencePercentage,
            lastDetected = stat.lastDetected,
            isCustom = false,
            category = "general",
            isFavorite = false,
            avgExecutionTimeMs = 0f,
            totalConfidenceSum = stat.confidencePercentage * stat.detectionCount
        )
    }

    fun mapToEntity(stats: GestureTrainingStats): GestureStatsEntity {
        val mostUsed = stats.gesturesByType.maxByOrNull { it.value }
        return GestureStatsEntity(
            gestureName = mostUsed?.key?.name ?: "none",
            detectionCount = stats.totalGestures,
            confidencePercentage = stats.averageConfidence,
            lastDetected = stats.lastGestureTime,
            isCustom = true,
            category = "training",
            isFavorite = false,
            avgExecutionTimeMs = 0f,
            totalConfidenceSum = stats.averageConfidence * stats.totalGestures
        )
    }

    // ==================== Profile ====================

    fun mapToEntity(profile: UserProfile): ProfileEntity {
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

    // ==================== Gesture Template ====================

    fun mapToEntity(template: CustomGestureTemplate): GestureTemplateEntity {
        return GestureTemplateEntity(
            id = template.id,
            name = template.name,
            type = template.type.name,
            action = template.action,
            confidenceThreshold = template.confidence,
            isEnabled = template.isEnabled,
            isCustom = template.type == GestureType.CUSTOM,
            detectionCount = template.usageCount,
            lastDetected = System.currentTimeMillis(),
            createdAt = template.createdAt,
            updatedAt = template.updatedAt,
            metadata = null,
            description = null,
            iconRes = 0,
            version = 1,
            isSystem = template.type != GestureType.CUSTOM,
            trainingSamplesCount = 0,
            isFavorite = false
        )
    }

    // ==================== Training Sample ====================

    fun mapToEntity(gestureName: String, sample: FloatArray, confidence: Float): TrainingSampleEntity {
        return TrainingSampleEntity(
            gestureName = gestureName,
            sampleData = sample.joinToString(","),
            timestamp = System.currentTimeMillis(),
            confidence = confidence
        )
    }

    // ==================== Batch Mappers ====================

    fun mapUserProfilesToEntityList(profiles: List<UserProfile>): List<ProfileEntity> {
        return profiles.map { mapToEntity(it) }
    }

    fun mapCustomGestureTemplatesToEntityList(templates: List<CustomGestureTemplate>): List<GestureTemplateEntity> {
        return templates.map { mapToEntity(it) }
    }

    fun mapGestureStatisticsToEntityList(stats: List<GestureStatistics>): List<GestureStatsEntity> {
        return stats.map { mapToEntity(it) }
    }
}

// ==================== Entity to Domain Mapper ====================

object EntityToDomainMapper {

    // ==================== Calibration ====================

    fun mapToDomain(entity: CalibrationEntity): CalibrationData {
        return CalibrationData(
            gyroBias = SensorCalibrationData(
                offsetX = entity.gyroBiasX,
                offsetY = entity.gyroBiasY,
                offsetZ = entity.gyroBiasZ,
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
            quality = try { CalibrationQuality.valueOf(entity.calibrationQuality) }
            catch (e: Exception) { CalibrationQuality.UNKNOWN },
            timestamp = entity.timestamp
        )
    }

    // ==================== Statistics ====================

    fun mapToDomain(entity: StatisticsEntity): StatisticsSummary {
        return StatisticsSummary(
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
    }

    fun mapToDomain(entity: DailyStatsEntity): DailyStats {
        return DailyStats(
            date = entity.date,
            clicks = entity.clicks,
            doubleClicks = entity.doubleClicks,
            rightClicks = entity.rightClicks,
            scrolls = entity.scrolls,
            movements = entity.movements,
            distance = entity.distance
        )
    }

    fun mapToDomain(entity: GestureStatsEntity): GestureStatistics {
        return GestureStatistics(
            gestureName = entity.gestureName,
            detectionCount = entity.detectionCount,
            confidencePercentage = entity.confidencePercentage,
            lastDetected = entity.lastDetected
        )
    }

    fun mapGestureStatsEntitiesToDomainList(entities: List<GestureStatsEntity>): List<GestureStatistics> {
        return entities.map { mapToDomain(it) }
    }

    // ==================== Profile ====================

    fun mapToDomain(entity: ProfileEntity): UserProfile {
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

    fun mapProfileEntitiesToDomainList(entities: List<ProfileEntity>): List<UserProfile> {
        return entities.map { mapToDomain(it) }
    }

    // ==================== Gesture Template ====================

    fun mapToDomain(entity: GestureTemplateEntity): CustomGestureTemplate {
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

    fun mapGestureTemplateEntitiesToDomainList(entities: List<GestureTemplateEntity>): List<CustomGestureTemplate> {
        return entities.map { mapToDomain(it) }
    }

    // ==================== Training Sample ====================

    fun mapToDomain(entity: TrainingSampleEntity): FloatArray {
        return entity.sampleData.split(",").map { it.toFloat() }.toFloatArray()
    }

    fun mapTrainingSampleEntitiesToDomainList(entities: List<TrainingSampleEntity>): List<FloatArray> {
        return entities.map { mapToDomain(it) }
    }

    // ==================== Gesture Data ====================

    fun mapGestureDataToDomain(
        entities: List<GestureTemplateEntity>,
        stats: List<GestureStatsEntity>
    ): Map<String, Any> {
        return mapOf(
            "templates" to mapGestureTemplateEntitiesToDomainList(entities),
            "statistics" to mapGestureStatsEntitiesToDomainList(stats)
        )
    }

    // ==================== Statistics Aggregation ====================

    fun mapToDomain(stats: AggregatedStats): Map<String, Any> {
        return mapOf(
            "totalClicks" to stats.totalClicks,
            "totalDoubleClicks" to stats.totalDoubleClicks,
            "totalRightClicks" to stats.totalRightClicks,
            "totalScrolls" to stats.totalScrolls,
            "totalMovements" to stats.totalMovements,
            "totalDistance" to stats.totalDistance,
            "totalGestures" to stats.totalGestures
        )
    }

    fun mapToDomain(avgStats: AverageStats): Map<String, Float> {
        return mapOf(
            "avgClicks" to avgStats.avgClicks,
            "avgMovements" to avgStats.avgMovements,
            "avgDistance" to avgStats.avgDistance
        )
    }

    // ==================== Complete Profile with Settings ====================

    fun mapToDomainWithSettings(profile: ProfileEntity, settings: ProfileSettings): UserProfile {
        return UserProfile(
            id = profile.id,
            name = profile.name,
            email = profile.email,
            avatarUri = profile.avatarUri,
            settings = settings,
            isDefault = profile.isDefault,
            isFavorite = profile.isFavorite,
            tags = profile.tags?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            iconRes = profile.iconRes,
            createdAt = profile.createdAt,
            updatedAt = profile.lastUsed
        )
    }

    // ==================== Full Calibration Data ====================

    fun mapToFullDomain(
        calibration: CalibrationEntity,
        status: CalibrationStatus,
        quality: CalibrationQuality
    ): CalibrationData {
        return mapToDomain(calibration).copy(
            isCalibrated = status == CalibrationStatus.COMPLETED,
            quality = quality
        )
    }
}
