// app/src/main/java/com/airmouse/di/ServiceModule.kt
package com.airmouse.di

import android.content.Context
import com.airmouse.proximity.ProximityAwareService
import com.airmouse.service.BluetoothHidService
import com.airmouse.service.GestureInferenceService
import com.airmouse.service.SensorService
import com.airmouse.service.VoiceCommandService
import com.airmouse.utils.BatterySaver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideSensorService(
        @ApplicationContext context: Context,
        calibrationHelper: CalibrationHelper,
        gestureDetector: GestureDetector,
        preferencesManager: PreferencesManager,
        batterySaver: BatterySaver
    ): SensorService {
        return SensorService(context, calibrationHelper, gestureDetector, preferencesManager, batterySaver)
    }

    @Provides
    @Singleton
    fun provideGestureInferenceService(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): GestureInferenceService {
        return GestureInferenceService(context, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideVoiceCommandService(
        @ApplicationContext context: Context,
        connectionManager: ConnectionManager
    ): VoiceCommandService {
        return VoiceCommandService(context, connectionManager)
    }

    @Provides
    @Singleton
    fun provideProximityAwareService(
        @ApplicationContext context: Context,
        connectionManager: ConnectionManager,
        preferencesManager: PreferencesManager
    ): ProximityAwareService {
        return ProximityAwareService(context, connectionManager, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideBluetoothHidService(
        @ApplicationContext context: Context
    ): BluetoothHidService {
        return BluetoothHidService(context)
    }
}