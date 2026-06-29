
package com.airmouse.di

import com.airmouse.network.ConnectionManager
import com.airmouse.network.UdpDiscovery
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideUdpDiscovery(): UdpDiscovery = UdpDiscovery()

    @Provides
    @Singleton
    fun provideConnectionManager(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        prefs: com.airmouse.utils.PreferencesManager
    ): ConnectionManager = ConnectionManager(context, prefs)
}
