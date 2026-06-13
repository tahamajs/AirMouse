// app/src/main/java/com/airmouse/di/RepositoryModule.kt
package com.airmouse.di

import com.airmouse.data.repository.CalibrationRepositoryImpl
import com.airmouse.data.repository.ConnectionRepositoryImpl
import com.airmouse.data.repository.GestureRepositoryImpl
import com.airmouse.data.repository.SettingsRepositoryImpl
import com.airmouse.data.repository.MouseRepositoryImpl
import com.airmouse.data.repository.SensorRepositoryImpl
import com.airmouse.data.repository.ProximityRepositoryImpl
import com.airmouse.data.repository.VoiceCommandRepositoryImpl
import com.airmouse.data.repository.ProfileRepositoryImpl
import com.airmouse.data.repository.StatisticsRepositoryImpl
import com.airmouse.data.datasource.local.AppDatabase
import com.airmouse.network.WebSocketManager
import com.airmouse.network.TcpClient
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.domain.repository.IMouseRepository
import com.airmouse.domain.repository.ISensorRepository
import com.airmouse.domain.repository.IProximityRepository
import com.airmouse.domain.repository.IVoiceCommandRepository
import com.airmouse.domain.repository.IProfileRepository
import com.airmouse.domain.repository.IStatisticsRepository
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
    abstract fun bindGestureRepository(
        impl: GestureRepositoryImpl
    ): IGestureRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl
    ): IConnectionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): ISettingsRepository

    @Binds
    @Singleton
    abstract fun bindMouseRepository(
        impl: MouseRepositoryImpl
    ): IMouseRepository

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
        preferencesManager: PreferencesManager,
        webSocketManager: WebSocketManager
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
        preferencesManager: PreferencesManager
    ): ProximityRepositoryImpl {
        return ProximityRepositoryImpl(preferencesManager)
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
}

// app/src/main/java/com/airmouse/di/RepositoryModule.kt
package com.airmouse.di

import com.airmouse.data.repository.*
import com.airmouse.domain.repository.*

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

// app/src/main/java/com/airmouse/di/RepositoryModule.kt
package com.airmouse.di

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
}


// app/src/main/java/com/airmouse/di/RepositoryModule.kt
package com.airmouse.di

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
}