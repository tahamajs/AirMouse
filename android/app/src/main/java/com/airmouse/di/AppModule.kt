// app/src/main/java/com/airmouse/di/AppModule.kt
package com.airmouse.di

import android.content.Context
import com.airmouse.data.local.PreferencesManager
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.GestureDetector
import com.airmouse.utils.BatterySaver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideCalibrationHelper(preferencesManager: PreferencesManager): CalibrationHelper {
        return CalibrationHelper(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideGestureDetector(preferencesManager: PreferencesManager): GestureDetector {
        return GestureDetector(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideBatterySaver(): BatterySaver {
        return BatterySaver()
    }
}