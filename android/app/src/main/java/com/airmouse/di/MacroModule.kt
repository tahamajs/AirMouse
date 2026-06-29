package com.airmouse.di

import com.airmouse.macros.MacroRecorder
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MacroModule {

    @Provides
    @Singleton
    fun provideMacroRecorder(
        @ApplicationContext context: android.content.Context,
        prefs: PreferencesManager,
        connectionManager: ConnectionManager
    ): MacroRecorder {
        return MacroRecorder(context, prefs, connectionManager)
    }
}