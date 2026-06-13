package com.airmouse.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.SensorManager
import com.airmouse.utils.PreferencesManager
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
    fun provideCalibrationHelper(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): CalibrationHelper {
        return CalibrationHelper(context, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideGestureDetector(preferencesManager: PreferencesManager): GestureDetector {
        return GestureDetector(preferencesManager.getSensitivity())
    }

    @Provides
    @Singleton
    fun provideBatterySaver(): BatterySaver {
        return BatterySaver()
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }
}
