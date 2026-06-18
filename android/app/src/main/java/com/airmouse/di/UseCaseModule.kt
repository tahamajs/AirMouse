// app/src/main/java/com/airmouse/di/UseCaseModule.kt
package com.airmouse.di

import com.airmouse.domain.usecase.ConnectToServerUseCase
import com.airmouse.domain.usecase.SendMovementUseCase
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.ICalibrationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideConnectToServerUseCase(
        connectionRepo: IConnectionRepository
    ): ConnectToServerUseCase {
        return ConnectToServerUseCase(connectionRepo)
    }

    @Provides
    @Singleton
    fun provideSendMovementUseCase(
        connectionRepo: IConnectionRepository,
        settingsRepo: ISettingsRepository
    ): SendMovementUseCase {
        return SendMovementUseCase(connectionRepo, settingsRepo)
    }

    @Provides
    @Singleton
    fun provideCalibrationUseCase(
        calibrationRepo: ICalibrationRepository,
        settingsRepo: ISettingsRepository
    ): CalibrationUseCase {
        return CalibrationUseCase(calibrationRepo, settingsRepo)
    }
}