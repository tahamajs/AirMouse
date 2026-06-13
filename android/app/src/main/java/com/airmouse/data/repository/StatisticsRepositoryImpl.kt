package com.airmouse.data.repository

import com.airmouse.domain.model.AppStatistics
import com.airmouse.domain.model.TimeRange
import com.airmouse.domain.repository.IStatisticsRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IStatisticsRepository {

    private val _stats = MutableStateFlow(AppStatistics(0,0,0,0,emptyMap(),0f,0f,0,0))
    override fun getStatistics(timeRange: TimeRange): Flow<AppStatistics> = _stats

    override suspend fun incrementClickCount() { prefs.putInt("stat_clicks", prefs.getInt("stat_clicks",0)+1) }
    override suspend fun incrementDoubleClickCount() { prefs.putInt("stat_double_clicks", prefs.getInt("stat_double_clicks",0)+1) }
    override suspend fun incrementRightClickCount() { prefs.putInt("stat_right_clicks", prefs.getInt("stat_right_clicks",0)+1) }
    override suspend fun incrementScrollCount() { prefs.putInt("stat_scrolls", prefs.getInt("stat_scrolls",0)+1) }
    override suspend fun incrementGestureCount(gestureName: String) { }
    override suspend fun recordMovement(distance: Float, duration: Long) { }
    override suspend fun resetStatistics() { }
    override suspend fun exportStatistics(): String = ""
}package com.airmouse.data.repository

import com.airmouse.domain.model.AppStatistics
import com.airmouse.domain.model.TimeRange
import com.airmouse.domain.repository.IStatisticsRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IStatisticsRepository {

    private val _stats = MutableStateFlow(AppStatistics(0,0,0,0,emptyMap(),0f,0f,0,0))
    override fun getStatistics(timeRange: TimeRange): Flow<AppStatistics> = _stats

    override suspend fun incrementClickCount() { prefs.putInt("stat_clicks", prefs.getInt("stat_clicks",0)+1) }
    override suspend fun incrementDoubleClickCount() { prefs.putInt("stat_double_clicks", prefs.getInt("stat_double_clicks",0)+1) }
    override suspend fun incrementRightClickCount() { prefs.putInt("stat_right_clicks", prefs.getInt("stat_right_clicks",0)+1) }
    override suspend fun incrementScrollCount() { prefs.putInt("stat_scrolls", prefs.getInt("stat_scrolls",0)+1) }
    override suspend fun incrementGestureCount(gestureName: String) { }
    override suspend fun recordMovement(distance: Float, duration: Long) { }
    override suspend fun resetStatistics() { }
    override suspend fun exportStatistics(): String = ""
}