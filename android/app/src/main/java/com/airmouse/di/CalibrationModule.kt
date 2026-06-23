
package com.airmouse.di

import com.airmouse.data.datasource.local.CalibrationDataSourceImpl
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.utils.PreferencesManager
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
}
