// app/src/main/java/com/airmouse/di/ServiceModule.kt
package com.airmouse.di

import android.content.Context
import com.airmouse.sensors.SensorService
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.utils.BatterySaver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideSensorService(
        @ApplicationContext context: Context,
        calibrationHelper: CalibrationHelper,
        gestureDetector: EnhancedGestureDetector,
        preferencesManager: PreferencesManager,
        batterySaver: BatterySaver
    ): SensorService {
        return SensorService(context, calibrationHelper, gestureDetector, preferencesManager, batterySaver)
    }
}// app/src/main/java/com/airmouse/di/ServiceModule.kt
package com.airmouse.di

import android.content.Context
import com.airmouse.sensors.SensorService
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.utils.BatterySaver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideSensorService(
        @ApplicationContext context: Context,
        calibrationHelper: CalibrationHelper,
        gestureDetector: EnhancedGestureDetector,
        preferencesManager: PreferencesManager,
        batterySaver: BatterySaver
    ): SensorService {
        return SensorService(context, calibrationHelper, gestureDetector, preferencesManager, batterySaver)
    }
}