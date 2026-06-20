// app/src/main/java/com/airmouse/di/CalibrationModule.kt
package com.airmouse.di

import com.airmouse.data.datasource.local.CalibrationDataSourceImpl
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.data.repository.CalibrationRepositoryImpl
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalibrationModule {

    @Provides
    @Singleton
    fun provideCalibrationHelper(
        @ApplicationContext context: Context,
        prefs: PreferencesManager
    ): CalibrationHelper {
        return CalibrationHelper(context, prefs)
    }

    @Provides
    @Singleton
    fun provideCalibrationRepository(
        calibrationHelper: CalibrationHelper,
        dataSource: ICalibrationDataSource,
        prefs: PreferencesManager
    ): ICalibrationRepository {
        return CalibrationRepositoryImpl(calibrationHelper, dataSource, prefs)
    }

    @Provides
    @Singleton
    fun provideCalibrationDataSource(
        prefs: PreferencesManager
    ): ICalibrationDataSource {
        return CalibrationDataSourceImpl(prefs)
    }

    @Provides
    @Singleton
    fun provideCalibrationUseCase(
        repository: ICalibrationRepository
    ): CalibrationUseCase {
        return CalibrationUseCase(repository)
    }
}
