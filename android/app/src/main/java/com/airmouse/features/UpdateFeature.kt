// app/src/main/java/com/airmouse/features/UpdateFeature.kt
package com.airmouse.features

import com.airmouse.domain.model.UpdateProgress
import com.airmouse.domain.model.UpdateResult
import com.airmouse.domain.usecase.CheckForUpdatesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateFeature @Inject constructor(
    private val checkForUpdatesUseCase: CheckForUpdatesUseCase
) {

    data class UpdateFeatureState(
        val isChecking: Boolean = false,
        val isDownloading: Boolean = false,
        val isInstalling: Boolean = false,
        val updateAvailable: Boolean = false,
        val latestVersion: String? = null,
        val currentVersion: String = "",
        val downloadProgress: Float = 0f,
        val error: String? = null,
        val releaseNotes: String? = null,
        val fileSize: Long = 0
    )

    private val _state = MutableStateFlow(UpdateFeatureState())
    val state: StateFlow<UpdateFeatureState> = _state.asStateFlow()

    init {
        loadCurrentVersion()
        observeUpdateStatusInternal()
        observeDownloadProgressInternal()
    }

    private fun loadCurrentVersion() {
        // Load current version from build config
    }

    private fun observeUpdateStatusInternal() {
        // Observe update status
    }

    private fun observeDownloadProgressInternal() {
        // Observe download progress
    }

    suspend fun checkForUpdates(): Result<UpdateResult> {
        _state.value = _state.value.copy(isChecking = true)

        val result = checkForUpdatesUseCase()

        if (result.isSuccess) {
            val updateResult = result.getOrNull()
            _state.value = _state.value.copy(
                isChecking = false,
                updateAvailable = updateResult?.isAvailable ?: false,
                latestVersion = updateResult?.version,
                releaseNotes = updateResult?.releaseNotes,
                fileSize = updateResult?.fileSize ?: 0
            )
        } else {
            _state.value = _state.value.copy(
                isChecking = false,
                error = result.exceptionOrNull()?.message
            )
        }

        return result
    }

    suspend fun checkManually(): Result<UpdateResult> {
        _state.value = _state.value.copy(isChecking = true)

        val result = checkForUpdatesUseCase.checkManually()

        if (result.isSuccess) {
            val updateResult = result.getOrNull()
            _state.value = _state.value.copy(
                isChecking = false,
                updateAvailable = updateResult?.isAvailable ?: false,
                latestVersion = updateResult?.version,
                releaseNotes = updateResult?.releaseNotes,
                fileSize = updateResult?.fileSize ?: 0
            )
        } else {
            _state.value = _state.value.copy(
                isChecking = false,
                error = result.exceptionOrNull()?.message
            )
        }

        return result
    }

    fun observeUpdateStatus(): Flow<UpdateResult> {
        return checkForUpdatesUseCase.observeUpdateStatus()
    }

    suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Result<Boolean> {
        _state.value = _state.value.copy(isDownloading = true)

        val result = checkForUpdatesUseCase.downloadUpdate(version) { progress ->
            onProgress(progress)
            _state.value = _state.value.copy(downloadProgress = progress)
        }

        if (result.isSuccess) {
            _state.value = _state.value.copy(isDownloading = false)
        } else {
            _state.value = _state.value.copy(
                isDownloading = false,
                error = result.exceptionOrNull()?.message
            )
        }

        return result
    }

    fun observeDownloadProgress(): Flow<UpdateProgress> {
        return checkForUpdatesUseCase.observeDownloadProgress()
    }

    suspend fun installUpdate(): Result<Unit> {
        _state.value = _state.value.copy(isInstalling = true)

        val result = checkForUpdatesUseCase.installUpdate()

        if (result.isSuccess) {
            _state.value = _state.value.copy(isInstalling = false)
        } else {
            _state.value = _state.value.copy(
                isInstalling = false,
                error = result.exceptionOrNull()?.message
            )
        }

        return result
    }

    suspend fun getCurrentVersion(): String {
        return checkForUpdatesUseCase.getCurrentVersion()
    }

    suspend fun getLatestVersion(): String? {
        return _state.value.latestVersion
    }

    suspend fun isUpdateAvailable(): Boolean {
        return checkForUpdatesUseCase.isUpdateAvailable()
    }

    suspend fun cancelDownload(): Result<Unit> {
        return checkForUpdatesUseCase.cancelDownload()
    }

    suspend fun resetUpdateState() {
        _state.value = UpdateFeatureState()
    }

    fun getUpdateFeatureState(): UpdateFeatureState = _state.value
}
