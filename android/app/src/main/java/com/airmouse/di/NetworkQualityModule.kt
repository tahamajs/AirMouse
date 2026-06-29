package com.airmouse.di

import com.airmouse.network.AutoReconnectManager
import com.airmouse.network.ConnectionManager
import com.airmouse.network.NetworkQualityMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkQualityModule {

    @Provides
    @Singleton
    fun provideNetworkQualityMonitor(
        @ApplicationContext context: android.content.Context
    ): NetworkQualityMonitor {
        return NetworkQualityMonitor(context)
    }

    @Provides
    @Singleton
    fun provideAutoReconnectManager(
        connectionManager: ConnectionManager
    ): AutoReconnectManager {
        return AutoReconnectManager(connectionManager)
    }
}