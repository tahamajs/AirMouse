// app/src/main/java/com/airmouse/di/UseCaseModule.kt
package com.airmouse.di

import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.ISettingsRepository
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.domain.usecase.ConnectToServerUseCase
import com.airmouse.domain.usecase.DetectGestureUseCase
import com.airmouse.domain.usecase.SendMovementUseCase
import com.airmouse.domain.usecase.StartServerUseCase
import com.airmouse.domain.usecase.StopServerUseCase
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
    fun provideCalibrationUseCase(
        calibrationRepository: ICalibrationRepository,
        settingsRepository: ISettingsRepository
    ): CalibrationUseCase {
        return CalibrationUseCase(calibrationRepository, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideDetectGestureUseCase(
        gestureRepository: IGestureRepository,
        settingsRepository: ISettingsRepository
    ): DetectGestureUseCase {
        return DetectGestureUseCase(gestureRepository, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideSendMovementUseCase(
        connectionRepository: IConnectionRepository,
        settingsRepository: ISettingsRepository
    ): SendMovementUseCase {
        return SendMovementUseCase(connectionRepository, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideConnectToServerUseCase(
        connectionRepository: IConnectionRepository
    ): ConnectToServerUseCase {
        return ConnectToServerUseCase(connectionRepository)
    }

    @Provides
    @Singleton
    fun provideStartServerUseCase(
        connectionRepository: IConnectionRepository
    ): StartServerUseCase {
        return StartServerUseCase(connectionRepository)
    }

    @Provides
    @Singleton
    fun provideStopServerUseCase(
        connectionRepository: IConnectionRepository
    ): StopServerUseCase {
        return StopServerUseCase(connectionRepository)
    }
}