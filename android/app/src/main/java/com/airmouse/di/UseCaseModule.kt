package com.airmouse.di

import android.content.Context
import com.airmouse.domain.repository.*
import com.airmouse.domain.usecase.*
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
        connectionRepo: IConnectionRepository,
        calibrationRepo: ICalibrationRepository
    ): ConnectToServerUseCase {
        return ConnectToServerUseCase(connectionRepo, calibrationRepo)
    }

    @Provides
    @Singleton
    fun provideSendMovementUseCase(
        mouseRepository: IMouseRepository
    ): SendMovementUseCase {
        return SendMovementUseCase(mouseRepository)
    }

    @Provides
    @Singleton
    fun provideCalibrationUseCase(
        calibrationRepo: ICalibrationRepository
    ): CalibrationUseCase {
        return CalibrationUseCase(calibrationRepo)
    }

    // --- ADDED: Missing use cases ---

    @Provides
    @Singleton
    fun provideDiscoverServersUseCase(
        connectionRepo: IConnectionRepository
    ): DiscoverServersUseCase {
        return DiscoverServersUseCase(connectionRepo)
    }

    @Provides
    @Singleton
    fun provideGetConnectionStatusUseCase(
        connectionRepo: IConnectionRepository
    ): GetConnectionStatusUseCase {
        return GetConnectionStatusUseCase(connectionRepo)
    }

    @Provides
    @Singleton
    fun provideTestConnectionUseCase(
        connectionRepo: IConnectionRepository
    ): TestConnectionUseCase {
        return TestConnectionUseCase(connectionRepo)
    }

    @Provides
    @Singleton
    fun provideDetectGestureUseCase(
        gestureRepo: IGestureRepository
    ): DetectGestureUseCase {
        return DetectGestureUseCase(gestureRepo)
    }

    @Provides
    @Singleton
    fun provideManageGestureTemplatesUseCase(
        gestureRepo: IGestureRepository
    ): ManageGestureTemplatesUseCase {
        return ManageGestureTemplatesUseCase(gestureRepo)
    }

    @Provides
    @Singleton
    fun provideGetProximityStateUseCase(
        proximityRepo: IProximityRepository
    ): GetProximityStateUseCase {
        return GetProximityStateUseCase(proximityRepo)
    }

    @Provides
    @Singleton
    fun provideUpdateProximityConfigUseCase(
        proximityRepo: IProximityRepository
    ): UpdateProximityConfigUseCase {
        return UpdateProximityConfigUseCase(proximityRepo)
    }

    @Provides
    @Singleton
    fun provideGetStatisticsUseCase(
        statsRepo: IStatisticsRepository
    ): GetStatisticsUseCase {
        return GetStatisticsUseCase(statsRepo)
    }

    @Provides
    @Singleton
    fun provideRecordStatisticsUseCase(
        statsRepo: IStatisticsRepository
    ): RecordStatisticsUseCase {
        return RecordStatisticsUseCase(statsRepo)
    }

    @Provides
    @Singleton
    fun provideHandleVoiceCommandUseCase(
        voiceRepo: IVoiceCommandRepository
    ): HandleVoiceCommandUseCase {
        return HandleVoiceCommandUseCase(voiceRepo)
    }

    @Provides
    @Singleton
    fun provideManageProfileUseCase(
        profileRepo: IProfileRepository
    ): ManageProfileUseCase {
        return ManageProfileUseCase(profileRepo)
    }

    @Provides
    @Singleton
    fun provideCheckForUpdatesUseCase(
        updateRepo: IUpdateRepository
    ): CheckForUpdatesUseCase {
        return CheckForUpdatesUseCase(updateRepo)
    }
}