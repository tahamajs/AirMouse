// app/src/main/java/com/airmouse/di/CalibrationModule.kt
package com.airmouse.di

import com.airmouse.data.datasource.local.CalibrationDataSourceImpl
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.data.repository.CalibrationRepositoryImpl
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.usecase.CalibrationUseCase
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
    fun provideCalibrationDataSource(
        prefs: PreferencesManager
    ): ICalibrationDataSource {
        return CalibrationDataSourceImpl(prefs)
    }

    @Provides
    @Singleton
    fun provideCalibrationRepository(
        dataSource: ICalibrationDataSource,
        prefs: PreferencesManager
    ): ICalibrationRepository {
        return CalibrationRepositoryImpl(dataSource, prefs)
    }

    @Provides
    @Singleton
    fun provideCalibrationUseCase(
        repository: ICalibrationRepository
    ): CalibrationUseCase {
        return CalibrationUseCase(repository)
    }
}