// app/src/main/java/com/airmouse/di/NetworkModule.kt
package com.airmouse.di

import com.airmouse.ConnectionManager
import com.airmouse.network.TcpClient
import com.airmouse.network.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(okHttpClient: OkHttpClient): WebSocketManager {
        return WebSocketManager
    }

    @Provides
    @Singleton
    fun provideTcpClient(): TcpClient {
        return TcpClient()
    }

    @Provides
    @Singleton
    fun provideConnectionManager(
        webSocketManager: WebSocketManager,
        tcpClient: TcpClient
    ): ConnectionManager {
        return ConnectionManager(webSocketManager, tcpClient)
    }
}
