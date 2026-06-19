// app/src/main/java/com/airmouse/di/RepositoryModule.kt
package com.airmouse.di

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.hardware.SensorManager
import com.airmouse.data.datasource.local.AppDatabase
import com.airmouse.data.repository.*
import com.airmouse.domain.repository.*
import com.airmouse.network.ConnectionManager
import com.airmouse.network.UdpDiscovery
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCalibrationRepository(
        impl: CalibrationRepositoryImpl
    ): ICalibrationRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl
    ): IConnectionRepository

    // ❌ REMOVED DUPLICATE: IGestureRepository is now bound ONLY in GestureRepositoryModule

    @Binds
    @Singleton
    abstract fun bindMouseRepository(
        impl: MouseRepositoryImpl
    ): IMouseRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): ISettingsRepository

    @Binds
    @Singleton
    abstract fun bindSensorRepository(
        impl: SensorRepositoryImpl
    ): ISensorRepository

    @Binds
    @Singleton
    abstract fun bindProximityRepository(
        impl: ProximityRepositoryImpl
    ): IProximityRepository

    @Binds
    @Singleton
    abstract fun bindVoiceCommandRepository(
        impl: VoiceCommandRepositoryImpl
    ): IVoiceCommandRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): IProfileRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(
        impl: StatisticsRepositoryImpl
    ): IStatisticsRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(
        impl: UpdateRepositoryImpl
    ): IUpdateRepository
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryProvidersModule {

    @Provides
    @Singleton
    fun provideConnectionRepositoryImpl(
        connectionManager: ConnectionManager,
        udpDiscovery: UdpDiscovery,
        preferencesManager: PreferencesManager
    ): ConnectionRepositoryImpl {
        return ConnectionRepositoryImpl(connectionManager, udpDiscovery, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideSettingsRepositoryImpl(
        preferencesManager: PreferencesManager
    ): SettingsRepositoryImpl {
        return SettingsRepositoryImpl(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideMouseRepositoryImpl(
        connectionManager: ConnectionManager,
        preferencesManager: PreferencesManager
    ): MouseRepositoryImpl {
        return MouseRepositoryImpl(connectionManager, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideSensorRepositoryImpl(
        sensorManager: SensorManager,
        preferencesManager: PreferencesManager,
        calibrationHelper: CalibrationHelper
    ): SensorRepositoryImpl {
        return SensorRepositoryImpl(sensorManager, preferencesManager, calibrationHelper)
    }

    @Provides
    @Singleton
    fun provideProximityRepositoryImpl(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager,
        connectionManager: ConnectionManager
    ): ProximityRepositoryImpl {
        return ProximityRepositoryImpl(context, preferencesManager, connectionManager)
    }

    @Provides
    @Singleton
    fun provideProfileRepositoryImpl(
        appDatabase: AppDatabase,
        preferencesManager: PreferencesManager
    ): ProfileRepositoryImpl {
        return ProfileRepositoryImpl(appDatabase.profileDao(), preferencesManager)
    }

    @Provides
    @Singleton
    fun provideStatisticsRepositoryImpl(
        preferencesManager: PreferencesManager
    ): StatisticsRepositoryImpl {
        return StatisticsRepositoryImpl(preferencesManager)
    }
}