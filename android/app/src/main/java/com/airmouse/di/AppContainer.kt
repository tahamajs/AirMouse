package com.airmouse.utils.di

import android.content.Context
import com.airmouse.data.repository.*
import com.airmouse.domain.repository.*
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager

/**
 * Manual dependency injection container for legacy code
 * Used alongside Hilt for backward compatibility
 */
class AppContainer(private val context: Context) {
    
    // Preferences
    private val preferencesManager by lazy { PreferencesManager(context) }
    
    // Network
    private val connectionManager by lazy { ConnectionManager(context, preferencesManager) }
    
    // Repositories
    val calibrationRepository: ICalibrationRepository by lazy {
        CalibrationRepositoryImpl(preferencesManager)
    }
    
    val connectionRepository: IConnectionRepository by lazy {
        ConnectionRepositoryImpl(connectionManager, preferencesManager)
    }
    
    val gestureRepository: IGestureRepository by lazy {
        GestureRepositoryImpl(preferencesManager)
    }
    
    val mouseRepository: IMouseRepository by lazy {
        MouseRepositoryImpl(preferencesManager)
    }
    
    val settingsRepository: ISettingsRepository by lazy {
        SettingsRepositoryImpl(preferencesManager)
    }
    
    val sensorRepository: ISensorRepository by lazy {
        SensorRepositoryImpl(preferencesManager)
    }
    
    val proximityRepository: IProximityRepository by lazy {
        ProximityRepositoryImpl(preferencesManager)
    }
    
    val voiceCommandRepository: IVoiceCommandRepository by lazy {
        VoiceCommandRepositoryImpl(preferencesManager)
    }
    
    val profileRepository: IProfileRepository by lazy {
        ProfileRepositoryImpl(preferencesManager)
    }
    
    val statisticsRepository: IStatisticsRepository by lazy {
        StatisticsRepositoryImpl(preferencesManager)
    }
    
    fun cleanup() {
        connectionManager.cleanup()
    }
}