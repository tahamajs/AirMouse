package com.airmouse.di

import android.bluetooth.BluetoothAdapter
import com.airmouse.data.datasource.local.AppDatabase
import com.airmouse.data.repository.*
import com.airmouse.domain.repository.*
import com.airmouse.network.TcpClient
import com.airmouse.network.WebSocketManager
import com.airmouse.utils.PreferencesManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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

    @Binds
    @Singleton
    abstract fun bindGestureRepository(
        impl: GestureRepositoryImpl
    ): IGestureRepository

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
    fun provideCalibrationRepositoryImpl(
        preferencesManager: PreferencesManager,
        appDatabase: AppDatabase
    ): CalibrationRepositoryImpl {
        return CalibrationRepositoryImpl(appDatabase.calibrationDao(), preferencesManager)
    }

    @Provides
    @Singleton
    fun provideGestureRepositoryImpl(
        preferencesManager: PreferencesManager,
        appDatabase: AppDatabase
    ): GestureRepositoryImpl {
        return GestureRepositoryImpl(appDatabase.gestureDao(), preferencesManager)
    }

    @Provides
    @Singleton
    fun provideConnectionRepositoryImpl(
        webSocketManager: WebSocketManager,
        tcpClient: TcpClient,
        preferencesManager: PreferencesManager
    ): ConnectionRepositoryImpl {
        return ConnectionRepositoryImpl(webSocketManager, tcpClient, preferencesManager)
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
        preferencesManager: PreferencesManager
    ): MouseRepositoryImpl {
        return MouseRepositoryImpl(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideSensorRepositoryImpl(
        preferencesManager: PreferencesManager
    ): SensorRepositoryImpl {
        return SensorRepositoryImpl(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideProximityRepositoryImpl(
        preferencesManager: PreferencesManager,
        bluetoothAdapter: BluetoothAdapter
    ): ProximityRepositoryImpl {
        return ProximityRepositoryImpl(preferencesManager, bluetoothAdapter)
    }

    @Provides
    @Singleton
    fun provideVoiceCommandRepositoryImpl(
        preferencesManager: PreferencesManager
    ): VoiceCommandRepositoryImpl {
        return VoiceCommandRepositoryImpl(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideProfileRepositoryImpl(
        preferencesManager: PreferencesManager,
        appDatabase: AppDatabase
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

    @Provides
    @Singleton
    fun provideUpdateRepositoryImpl(
        preferencesManager: PreferencesManager,
        appDatabase: AppDatabase
    ): UpdateRepositoryImpl {
        return UpdateRepositoryImpl(preferencesManager, appDatabase)
    }
}