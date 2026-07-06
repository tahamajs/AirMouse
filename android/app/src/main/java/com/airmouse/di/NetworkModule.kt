
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
}
