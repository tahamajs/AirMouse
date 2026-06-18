// AppContainer.kt
package com.airmouse.utils.di

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.SensorManager
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

    // Core Infrastructure Services
    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    // Mock DAO placeholder for Profile setup — Replace with your Room DB reference if applicable
    private val profileDao by lazy {
        java.lang.Object()
    }

    // Network Management
    private val connectionManager by lazy {
        ConnectionManager(context, preferencesManager)
    }

    // ==================== Repositories ====================

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
        MouseRepositoryImpl(context, preferencesManager)
    }

    val settingsRepository: ISettingsRepository by lazy {
        SettingsRepositoryImpl(preferencesManager)
    }

    val sensorRepository: ISensorRepository by lazy {
        SensorRepositoryImpl(sensorManager, preferencesManager)
    }

    val proximityRepository: IProximityRepository by lazy {
        ProximityRepositoryImpl(context, preferencesManager)
    }

    val voiceCommandRepository: IVoiceCommandRepository by lazy {
        VoiceCommandRepositoryImpl(context, preferencesManager)
    }

    val profileRepository: IProfileRepository by lazy {
        // If your implementation uses a genuine Room DAO interface, replace the cast target below
        @Suppress("UNCHECKED_CAST")
        ProfileRepositoryImpl(profileDao as com.airmouse.data.local.dao.ProfileDao, preferencesManager)
    }

    val statisticsRepository: IStatisticsRepository by lazy {
        StatisticsRepositoryImpl(preferencesManager)
    }

    fun cleanup() {
        connectionManager.cleanup()
    }
}