package com.airmouse.di

import com.airmouse.notifications.NotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationsModule {

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: android.content.Context
    ): NotificationManager {
        return NotificationManager(context)
    }
}