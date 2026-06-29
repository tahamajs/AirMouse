package com.airmouse.di

import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.data.datasource.local.IGestureDataSource
import com.airmouse.data.datasource.local.IProfileDataSource
import com.airmouse.data.datasource.local.IStatisticsDataSource
import com.airmouse.network.ConnectionManager
import com.airmouse.sync.DataSyncManager
import com.airmouse.utils.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideDataSyncManager(
        @ApplicationContext context: android.content.Context,
        prefs: PreferencesManager,
        calibrationDataSource: ICalibrationDataSource,
        gestureDataSource: IGestureDataSource,
        statisticsDataSource: IStatisticsDataSource,
        profileDataSource: IProfileDataSource,
        connectionManager: ConnectionManager
    ): DataSyncManager {
        return DataSyncManager(
            context,
            prefs,
            calibrationDataSource,
            gestureDataSource,
            statisticsDataSource,
            profileDataSource,
            connectionManager
        )
    }
}