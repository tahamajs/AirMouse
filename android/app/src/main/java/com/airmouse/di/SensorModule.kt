// app/src/main/java/com/airmouse/di/SensorModule.kt
package com.airmouse.di

import android.content.Context
import android.hardware.SensorManager
import com.airmouse.sensors.SensorService
import com.airmouse.utils.BatterySaver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext context: Context): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @Provides
    @Singleton
    fun provideSensorService(
        sensorManager: SensorManager,
        calibrationHelper: CalibrationHelper,
        gestureDetector: GestureDetector,
        preferencesManager: PreferencesManager,
        batterySaver: BatterySaver
    ): SensorService {
        return SensorService(
            sensorManager,
            calibrationHelper,
            gestureDetector,
            preferencesManager,
            batterySaver
        )
    }
}