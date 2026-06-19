// app/src/main/java/com/airmouse/di/UseCaseModule.kt
package com.airmouse.di

import android.content.Context
import com.airmouse.domain.usecase.ConnectToServerUseCase
import com.airmouse.domain.usecase.SendMovementUseCase
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.ISettingsRepository
import com.airmouse.domain.repository.IMouseRepository
import com.airmouse.utils.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
}
// app/src/main/java/com/airmouse/di/UseCaseModule.kt
