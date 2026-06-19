// app/src/main/java/com/airmouse/di/GestureRepositoryModule.kt
package com.airmouse.di

import android.content.Context
import com.airmouse.data.datasource.local.GestureDataSourceImpl
import com.airmouse.data.datasource.local.IGestureDataSource
import com.airmouse.data.repository.GestureRepositoryImpl
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.utils.PreferencesManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module that provides gesture-related repository bindings.
 * This is the single source of truth for IGestureRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GestureRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGestureRepository(
        impl: GestureRepositoryImpl
    ): IGestureRepository
}

/**
 * Providers for gesture repository dependencies.
 * Includes data source provider to satisfy all dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object GestureRepositoryProvidersModule {

    /**
     * Provides IGestureDataSource implementation.
     * This resolves the MissingBinding error.
     */
    @Provides
    @Singleton
    fun provideGestureDataSource(
        prefs: PreferencesManager
    ): IGestureDataSource {
        return GestureDataSourceImpl(prefs)
    }

    @Provides
    @Singleton
    fun provideGestureRepositoryImpl(
        context: Context,
        prefs: PreferencesManager,
        gestureDetector: EnhancedGestureDetector,
        dataSource: IGestureDataSource
    ): GestureRepositoryImpl {
        return GestureRepositoryImpl(context, prefs, gestureDetector, dataSource)
    }
}