// app/src/main/java/com/airmouse/di/AppModule.kt
package com.airmouse.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.SensorManager
import android.os.Vibrator
import com.airmouse.utils.PreferencesManager
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.GestureDetector
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.utils.BatterySaver
import com.airmouse.utils.VibrateUtils
import com.airmouse.utils.AudioUtils
import com.airmouse.utils.BluetoothUtils
import com.airmouse.utils.QRScanner
import com.airmouse.utils.PermissionHelper
import com.airmouse.utils.PreferencesHelper
import com.airmouse.network.ConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ==================== Core Utilities ====================

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
        return context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

    // ==================== Sensor Components ====================

    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext context: Context): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
        return GestureDetector(preferencesManager.getSensitivity())
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
    fun provideBatterySaver(): BatterySaver {
        return BatterySaver()
    }

    // ==================== Bluetooth Components ====================

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    // ==================== Network Components ====================

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideConnectionManager(): ConnectionManager {
        return ConnectionManager
    }

    // ==================== Application Context ====================

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}