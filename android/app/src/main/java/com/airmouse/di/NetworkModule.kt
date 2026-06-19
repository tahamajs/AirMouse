// app/src/main/java/com/airmouse/di/NetworkModule.kt
package com.airmouse.di

import android.content.Context
import com.airmouse.network.ConnectionManager
import com.airmouse.network.UdpDiscovery
import com.airmouse.utils.PreferencesManager
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
object NetworkModule {

    /**
     * Provides OkHttpClient with logging and timeouts.
     */
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

    /**
     * Provides ConnectionManager for WebSocket/TCP communication.
     */
    @Provides
    @Singleton
    fun provideConnectionManager(
        @ApplicationContext context: Context,
        prefs: PreferencesManager
    ): ConnectionManager {
        // ConnectionManager expects Context and PreferencesManager
        return ConnectionManager(context, prefs)
    }

    /**
     * Provides UdpDiscovery for server discovery on the network.
     */
    @Provides
    @Singleton
    fun provideUdpDiscovery(): UdpDiscovery {
        // UdpDiscovery has no-arg constructor
        return UdpDiscovery()
    }
}