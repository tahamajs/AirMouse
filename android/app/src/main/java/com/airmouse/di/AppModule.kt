
package com.airmouse.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Vibrator
import com.airmouse.presentation.PresentationModeService
import com.airmouse.network.WebSocketManager
import com.airmouse.network.ConnectionManager
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.sensors.GestureDetector
import com.airmouse.utils.*
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
    fun providePreferencesHelper(@ApplicationContext context: Context): PreferencesHelper {
        PreferencesHelper.init(context)
        return PreferencesHelper
    }

    @Provides
    @Singleton
    fun providePermissionHelper(): PermissionHelper {
        return PermissionHelper
    }

    @Provides
    @Singleton
    fun provideVibrateUtils(@ApplicationContext context: Context): VibrateUtils {
        return VibrateUtils(context)
    }

    @Provides
    @Singleton
    fun provideVibrator(@ApplicationContext context: Context): Vibrator {
        return context.getSystemService(Vibrator::class.java)
    }

    @Provides
    @Singleton
    fun provideAudioUtils(@ApplicationContext context: Context): AudioUtils {
        return AudioUtils(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothUtils(@ApplicationContext context: Context): BluetoothUtils {
        return BluetoothUtils(context)
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
    fun provideGestureDetector(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): GestureDetector {
        return GestureDetector(context, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideEnhancedGestureDetector(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager,
        vibrator: Vibrator
    ): EnhancedGestureDetector {
        return EnhancedGestureDetector(context, preferencesManager, vibrator)
    }

    @Provides
    @Singleton
    fun providePresentationModeService(
        @ApplicationContext context: Context,
        connectionManager: ConnectionManager,
        preferencesManager: PreferencesManager
    ): PresentationModeService {
        return PresentationModeService(context, connectionManager, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideBatterySaver(): BatterySaver {
        return BatterySaver()
    }



    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        return bluetoothManager.adapter
    }



    @Provides
    @Singleton
    fun provideWebSocketManager(): WebSocketManager {
        return WebSocketManager
    }

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager {
        return context.getSystemService(UsbManager::class.java)
    }

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}
