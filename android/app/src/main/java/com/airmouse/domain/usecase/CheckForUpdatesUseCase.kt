// app/src/main/java/com/airmouse/domain/usecase/CheckForUpdatesUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.UpdateProgress
import com.airmouse.domain.model.UpdateResult
import com.airmouse.domain.repository.IUpdateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for checking and installing updates
 */
class CheckForUpdatesUseCase @Inject constructor(
    private val updateRepository: IUpdateRepository
) {

    /**
     * Check for updates
     */
    suspend operator fun invoke(): Result<UpdateResult> {
        return try {
            val result = updateRepository.checkForUpdates()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check for updates manually
     */
    suspend fun checkManually(): Result<UpdateResult> {
        return try {
            val result = updateRepository.checkForUpdatesManually()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe update status
     */
    fun observeUpdateStatus(): Flow<UpdateResult> {
        return updateRepository.observeUpdateStatus()
    }

    /**
     * Download update
     */
    suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Result<Boolean> {
        return try {
            val result = updateRepository.downloadUpdate(version) { progress ->
                onProgress(progress)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe download progress
     */
    fun observeDownloadProgress(): Flow<UpdateProgress> {
        return updateRepository.observeDownloadProgress()
    }

    /**
     * Install update
     */
    suspend fun installUpdate(): Result<Unit> {
        return try {
            updateRepository.installUpdate()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current version
     */
    suspend fun getCurrentVersion(): String {
        return updateRepository.getCurrentVersion().versionName
    }

    /**
     * Get latest version
     */
    suspend fun getLatestVersion(): String? {
        return updateRepository.getLatestVersion()?.versionName
    }

    /**
     * Check if update is available
     */
    suspend fun isUpdateAvailable(): Boolean {
        val result = updateRepository.checkForUpdates()
        return result.isAvailable
    }

    /**
     * Cancel download
     */
    suspend fun cancelDownload(): Result<Unit> {
        return try {
            updateRepository.cancelDownload()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}