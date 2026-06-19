// app/src/main/java/com/airmouse/domain/repository/IUpdateRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.UpdateInfo
import com.airmouse.domain.model.UpdateProgress
import com.airmouse.domain.model.UpdateResult
import com.airmouse.domain.model.VersionInfo
import kotlinx.coroutines.flow.Flow

interface IUpdateRepository {
    // Check updates
    suspend fun checkForUpdates(): UpdateResult
    suspend fun checkForUpdatesManually(): UpdateResult
    fun observeUpdateStatus(): Flow<UpdateResult>

    // Download
    suspend fun downloadUpdate(version: String): Boolean
    suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Boolean
    fun observeDownloadProgress(): Flow<UpdateProgress>

    // Install
    suspend fun installUpdate(version: String): Boolean
    suspend fun installUpdate()

    // Version info
    suspend fun getCurrentVersion(): VersionInfo
    suspend fun getLatestVersion(): VersionInfo?
    suspend fun getUpdateHistory(): List<UpdateInfo>

    // Status
    suspend fun isChecking(): Boolean
    suspend fun isDownloading(): Boolean
    suspend fun isInstalling(): Boolean

    // Cancel
    suspend fun cancelDownload()
    suspend fun cancelInstall()
}