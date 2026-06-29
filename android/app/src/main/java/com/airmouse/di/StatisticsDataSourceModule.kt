package com.airmouse.di

import com.airmouse.data.datasource.local.IStatisticsDataSource
import com.airmouse.data.datasource.local.StatisticsDataSourceImpl
import com.airmouse.utils.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StatisticsDataSourceModule {

    @Provides
    @Singleton
    fun provideStatisticsDataSource(
        prefs: PreferencesManager
    ): IStatisticsDataSource {
        return StatisticsDataSourceImpl(prefs)
    }
}
