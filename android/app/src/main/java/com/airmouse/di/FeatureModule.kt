// app/src/main/java/com/airmouse/di/FeatureModule.kt
package com.airmouse.di

import com.airmouse.features.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {

    @Provides
    @Singleton
    fun provideConnectionFeature(
        connectToServerUseCase: ConnectToServerUseCase,
        discoverServersUseCase: DiscoverServersUseCase,
        getConnectionStatusUseCase: GetConnectionStatusUseCase,
        testConnectionUseCase: TestConnectionUseCase
    ): ConnectionFeature {
        return ConnectionFeature(
            connectToServerUseCase,
            discoverServersUseCase,
            getConnectionStatusUseCase,
            testConnectionUseCase
        )
    }

    @Provides
    @Singleton
    fun provideMouseControlFeature(
        sendMovementUseCase: SendMovementUseCase,
        mouseRepo: IMouseRepository
    ): MouseControlFeature {
        return MouseControlFeature(sendMovementUseCase, mouseRepo)
    }

    @Provides
    @Singleton
    fun provideCalibrationFeature(
        calibrationUseCase: CalibrationUseCase
    ): CalibrationFeature {
        return CalibrationFeature(calibrationUseCase)
    }

    @Provides
    @Singleton
    fun provideGestureRecognitionFeature(
        detectGestureUseCase: DetectGestureUseCase,
        manageGestureTemplatesUseCase: ManageGestureTemplatesUseCase
    ): GestureRecognitionFeature {
        return GestureRecognitionFeature(
            detectGestureUseCase,
            manageGestureTemplatesUseCase
        )
    }

    @Provides
    @Singleton
    fun provideProximityFeature(
        getProximityStateUseCase: GetProximityStateUseCase,
        updateProximityConfigUseCase: UpdateProximityConfigUseCase
    ): ProximityFeature {
        return ProximityFeature(
            getProximityStateUseCase,
            updateProximityConfigUseCase
        )
    }

    @Provides
    @Singleton
    fun provideStatisticsFeature(
        getStatisticsUseCase: GetStatisticsUseCase,
        recordStatisticsUseCase: RecordStatisticsUseCase
    ): StatisticsFeature {
        return StatisticsFeature(
            getStatisticsUseCase,
            recordStatisticsUseCase
        )
    }

    @Provides
    @Singleton
    fun provideVoiceFeature(
        handleVoiceCommandUseCase: HandleVoiceCommandUseCase
    ): VoiceFeature {
        return VoiceFeature(handleVoiceCommandUseCase)
    }

    @Provides
    @Singleton
    fun provideProfileFeature(
        manageProfileUseCase: ManageProfileUseCase
    ): ProfileFeature {
        return ProfileFeature(manageProfileUseCase)
    }

    @Provides
    @Singleton
    fun provideUpdateFeature(
        checkForUpdatesUseCase: CheckForUpdatesUseCase
    ): UpdateFeature {
        return UpdateFeature(checkForUpdatesUseCase)
    }

    @Provides
    @Singleton
    fun provideSensorFeature(
        sensorRepo: ISensorRepository
    ): SensorFeature {
        return SensorFeature(sensorRepo)
    }
}