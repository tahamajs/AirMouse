package com.airmouse.di

import com.airmouse.gaming.GameProfilesManager
import com.airmouse.utils.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GamingModule {

    @Provides
    @Singleton
    fun provideGameProfilesManager(
        @ApplicationContext context: android.content.Context,
        prefs: PreferencesManager
    ): GameProfilesManager {
        return GameProfilesManager(context, prefs)
    }
}