package com.airmouse.di

import android.content.Context
import androidx.room.Room
import androidx.room.TypeConverters
import com.airmouse.data.datasource.local.AppDatabase
import com.airmouse.data.datasource.local.dao.CalibrationDao
import com.airmouse.data.datasource.local.dao.GestureDao
import com.airmouse.data.datasource.local.dao.ProfileDao
import com.airmouse.data.datasource.local.dao.SettingsDao
import com.airmouse.data.datasource.local.dao.StatisticsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "airmouse_database"
        )
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .build()
    }

    @Provides
    @Singleton
    fun provideCalibrationDao(database: AppDatabase): CalibrationDao {
        return database.calibrationDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideStatisticsDao(database: AppDatabase): StatisticsDao {
        return database.statisticsDao()
    }

    @Provides
    @Singleton
    fun provideGestureDao(database: AppDatabase): GestureDao {
        return database.gestureDao()
    }

    @Provides
    @Singleton
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }
}