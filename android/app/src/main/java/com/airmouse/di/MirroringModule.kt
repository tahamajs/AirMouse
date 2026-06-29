package com.airmouse.di

import com.airmouse.mirroring.ScreenMirroringService
import com.airmouse.network.ConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MirroringModule {

    @Provides
    @Singleton
    fun provideScreenMirroringService(
        @ApplicationContext context: android.content.Context,
        connectionManager: ConnectionManager
    ): ScreenMirroringService {
        return ScreenMirroringService()
    }
}