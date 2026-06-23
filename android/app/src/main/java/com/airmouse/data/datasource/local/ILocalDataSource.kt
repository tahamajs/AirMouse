
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ILocalDataSource {

    
    suspend fun saveCalibrationData(data: CalibrationData)
    suspend fun getCalibrationData(): CalibrationData
    suspend fun resetCalibrationData()

    
    suspend fun saveGestureTemplate(template: CustomGestureTemplate)
    suspend fun getGestureTemplate(id: String): CustomGestureTemplate?
    suspend fun getAllGestureTemplates(): List<CustomGestureTemplate>
    suspend fun deleteGestureTemplate(id: String)
    suspend fun updateGestureTemplate(template: CustomGestureTemplate)

    
    suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>)
    suspend fun getTrainingSamples(gestureName: String): List<FloatArray>
    suspend fun clearTrainingData(gestureName: String)

    
    suspend fun saveProfile(profile: UserProfile)
    suspend fun getProfile(id: String): UserProfile?
    suspend fun getAllProfiles(): List<UserProfile>
    suspend fun deleteProfile(id: String)
    suspend fun updateProfile(profile: UserProfile)
    suspend fun getDefaultProfile(): UserProfile?
    suspend fun setDefaultProfile(id: String)
    suspend fun getFavoriteProfiles(): List<UserProfile>
    suspend fun toggleFavorite(id: String)

    
    suspend fun saveSessionStats(stats: StatisticsSummary)
    suspend fun getSessionStats(): StatisticsSummary
    suspend fun resetSessionStats()

    suspend fun saveDailyStats(date: String, stats: DailyStats)
    suspend fun getDailyStats(date: String): DailyStats
    suspend fun getDailyStatsRange(startDate: String, endDate: String): List<DailyStats>

    suspend fun getHistoricalStats(): HistoricalStatistics
    suspend fun saveHistoricalStats(stats: HistoricalStatistics)

    suspend fun getGestureStats(): List<GestureStatistics>
    suspend fun saveGestureStats(stats: List<GestureStatistics>)
    suspend fun incrementGestureCount(gesture: String)

    suspend fun resetAllStats()
}