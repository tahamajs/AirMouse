
package com.airmouse.domain.usecase

import com.airmouse.domain.model.UpdateProgress
import com.airmouse.domain.model.UpdateResult
import com.airmouse.domain.repository.IUpdateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckForUpdatesUseCase @Inject constructor(
    private val updateRepository: IUpdateRepository
) {

    suspend operator fun invoke(): Result<UpdateResult> {
        return try {
            val result = updateRepository.checkForUpdates()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkManually(): Result<UpdateResult> {
        return try {
            val result = updateRepository.checkForUpdatesManually()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUpdateStatus(): Flow<UpdateResult> {
        return updateRepository.observeUpdateStatus()
    }

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

    fun observeDownloadProgress(): Flow<UpdateProgress> {
        return updateRepository.observeDownloadProgress()
    }

    suspend fun installUpdate(): Result<Unit> {
        return try {
            updateRepository.installUpdate()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentVersion(): String {
        return updateRepository.getCurrentVersion().versionName
    }

    suspend fun getLatestVersion(): String? {
        return updateRepository.getLatestVersion()?.versionName
    }

    suspend fun isUpdateAvailable(): Boolean {
        val result = updateRepository.checkForUpdates()
        return result.isAvailable
    }

    suspend fun cancelDownload(): Result<Unit> {
        return try {
            updateRepository.cancelDownload()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}