// app/src/main/java/com/airmouse/di/DatabaseModule.kt
package com.airmouse.di

import android.content.Context
import androidx.room.Room
import com.airmouse.data.datasource.local.AppDatabase
import com.airmouse.data.datasource.local.CalibrationDao
import com.airmouse.data.datasource.local.GestureDao
import com.airmouse.data.datasource.local.ProfileDao
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
            "airmouse_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideCalibrationDao(database: AppDatabase): CalibrationDao {
        return database.calibrationDao()
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
