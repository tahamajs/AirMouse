package com.airmouse.di

import com.airmouse.files.FileTransferService
import com.airmouse.files.FileTransferServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dagger Hilt module for File Transfer dependencies.
 * Provides the FileTransferService and its dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FileTransferModule {

    /**
     * Binds the FileTransferService interface to its implementation.
     */
    @Binds
    @Singleton
    abstract fun bindFileTransferService(
        impl: FileTransferServiceImpl
    ): FileTransferService

    companion object {

        /**
         * Provides the FileTransferServiceImpl instance.
         * Uses the application context for file operations.
         */
        @Provides
        @Singleton
        fun provideFileTransferServiceImpl(
            @ApplicationContext context: android.content.Context
        ): FileTransferServiceImpl {
            return FileTransferServiceImpl(context)
        }

        /**
         * Provides a dedicated OkHttpClient for file transfers.
         * Uses longer timeouts for large file transfers.
         */
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // Increased for large files
                .writeTimeout(120, TimeUnit.SECONDS) // Increased for large files
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}