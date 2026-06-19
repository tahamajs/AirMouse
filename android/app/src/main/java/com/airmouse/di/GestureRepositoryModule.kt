// app/src/main/java/com/airmouse/di/GestureRepositoryModule.kt
package com.airmouse.di

import android.content.Context
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

@Module
@InstallIn(SingletonComponent::class)
abstract class GestureRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGestureRepository(
        impl: GestureRepositoryImpl
    ): IGestureRepository
}

@Module
@InstallIn(SingletonComponent::class)
object GestureRepositoryProvidersModule {

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
