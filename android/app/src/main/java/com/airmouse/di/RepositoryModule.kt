// app/src/main/java/com/airmouse/di/RepositoryModule.kt
package com.airmouse.di

import com.airmouse.data.repository.CalibrationRepositoryImpl
import com.airmouse.data.repository.ConnectionRepositoryImpl
import com.airmouse.data.repository.GestureRepositoryImpl
import com.airmouse.data.repository.SettingsRepositoryImpl
import com.airmouse.data.local.PreferencesManager
import com.airmouse.data.local.AppDatabase
import com.airmouse.data.remote.WebSocketManager
import com.airmouse.data.remote.TcpClient
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.domain.repository.ISettingsRepository
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
}

// For dependencies that are not interfaces, we provide them here
@Module
@InstallIn(SingletonComponent::class)
object RepositoryProvidersModule {

    @Provides
    @Singleton
    fun provideCalibrationRepositoryImpl(
        preferencesManager: PreferencesManager,
        appDatabase: AppDatabase
    ): CalibrationRepositoryImpl {
        return CalibrationRepositoryImpl(preferencesManager, appDatabase)
    }

    @Provides
    @Singleton
    fun provideGestureRepositoryImpl(
        preferencesManager: PreferencesManager,
        appDatabase: AppDatabase
    ): GestureRepositoryImpl {
        return GestureRepositoryImpl(preferencesManager, appDatabase)
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
}