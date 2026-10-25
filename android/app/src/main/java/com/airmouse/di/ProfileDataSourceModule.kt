package com.airmouse.di

import com.airmouse.data.datasource.local.IProfileDataSource
import com.airmouse.data.datasource.local.ProfileDataSourceImpl
import com.airmouse.data.datasource.local.dao.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileDataSourceModule {

    @Provides
    @Singleton
    fun provideProfileDataSource(
        profileDao: ProfileDao
    ): IProfileDataSource {
        return ProfileDataSourceImpl(profileDao)
    }
}
