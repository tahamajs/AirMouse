package com.airmouse.di

import com.airmouse.utils.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {

    @Provides
    @Singleton
    fun provideErrorHandler(
        @ApplicationContext context: android.content.Context
    ): ErrorHandler {
        return ErrorHandler(context)
    }

    @Provides
    @Singleton
    fun provideBatteryOptimizer(
        @ApplicationContext context: android.content.Context
    ): BatteryOptimizer {
        return BatteryOptimizer(context)
    }

    @Provides
    @Singleton
    fun provideResourceHelper(
        @ApplicationContext context: android.content.Context
    ): ResourceHelper {
        return ResourceHelper(context)
    }

    @Provides
    @Singleton
    fun provideThemeManager(
        @ApplicationContext context: android.content.Context
    ): ThemeManager {
        return ThemeManager(context)
    }

    @Provides
    @Singleton
    fun provideConnectedDeviceStore(): ConnectedDeviceStore {
        return ConnectedDeviceStore
    }

    @Provides
    @Singleton
    fun provideLogManager(@ApplicationContext context: android.content.Context): LogManager {
        LogManager.init(context)
        return LogManager
    }
}