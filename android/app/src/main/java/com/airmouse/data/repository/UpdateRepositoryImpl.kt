package com.airmouse.data.repository

import com.airmouse.BuildConfig
import com.airmouse.domain.model.UpdateInfo
import com.airmouse.domain.model.UpdateProgress
import com.airmouse.domain.model.UpdateResult
import com.airmouse.domain.model.VersionInfo
import com.airmouse.domain.repository.IUpdateRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IUpdateRepository {

    private val _status = MutableStateFlow(UpdateResult())
    private val _downloadProgress = MutableStateFlow(UpdateProgress())
    private val _currentVersion = MutableStateFlow(
        VersionInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            buildDate = prefs.getLong("build_date", System.currentTimeMillis())
        )
    )
    private val _updateHistory = MutableStateFlow<List<UpdateInfo>>(emptyList())
    private val _isChecking = MutableStateFlow(false)
    private val _isDownloading = MutableStateFlow(false)
    private val _isInstalling = MutableStateFlow(false)

    override fun observeUpdateStatus(): StateFlow<UpdateResult> = _status.asStateFlow()
    override fun observeDownloadProgress(): StateFlow<UpdateProgress> = _downloadProgress.asStateFlow()

    init {
        loadUpdateHistory()
    }

    override suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        _isChecking.value = true
        val current = _currentVersion.value.versionName
        val latest = "3.0.1"
        val result = UpdateResult(
            isAvailable = latest != current,
            version = if (latest != current) latest else null,
            releaseNotes = if (latest != current) "Bug fixes and performance improvements" else null,
            fileSize = if (latest != current) 4L * 1024L * 1024L else 0L
        )
        _status.value = result
        _isChecking.value = false
        result
    }

    override suspend fun checkForUpdatesManually(): UpdateResult = checkForUpdates()

    override suspend fun downloadUpdate(version: String): Boolean {
        return downloadUpdate(version) { }
    }

    override suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            _isDownloading.value = true
            try {
                for (step in 0..100 step 10) {
                    val progress = step / 100f
                    _downloadProgress.value = UpdateProgress(progress = progress, bytesDownloaded = step.toLong())
                    onProgress(progress)
                    delay(50)
                }
                _downloadProgress.value = UpdateProgress(progress = 1f, bytesDownloaded = 1L, totalBytes = 1L, isComplete = true)
                true
            } finally {
                _isDownloading.value = false
            }
        }

    override suspend fun installUpdate(version: String): Boolean = withContext(Dispatchers.IO) {
        _isInstalling.value = true
        try {
            val info = UpdateInfo(version = version, date = System.currentTimeMillis(), isInstalled = true)
            _updateHistory.value = listOf(info) + _updateHistory.value
            saveUpdateHistory()
            true
        } finally {
            _isInstalling.value = false
        }
    }

    override suspend fun installUpdate() {
        val version = _status.value.version ?: _currentVersion.value.versionName
        installUpdate(version)
    }

    override suspend fun getCurrentVersion(): VersionInfo = _currentVersion.value

    override suspend fun getLatestVersion(): VersionInfo? {
        val result = checkForUpdates()
        return result.version?.let {
            VersionInfo(versionName = it, versionCode = _currentVersion.value.versionCode + 1)
        }
    }

    override suspend fun getUpdateHistory(): List<UpdateInfo> = _updateHistory.value

    override suspend fun isChecking(): Boolean = _isChecking.value
    override suspend fun isDownloading(): Boolean = _isDownloading.value
    override suspend fun isInstalling(): Boolean = _isInstalling.value

    override suspend fun cancelDownload() {
        _isDownloading.value = false
        _downloadProgress.value = UpdateProgress(isError = true, errorMessage = "Cancelled")
    }

    override suspend fun cancelInstall() {
        _isInstalling.value = false
    }

    override suspend fun downloadAndInstallUpdate(version: String): Boolean {
        return downloadUpdate(version) && installUpdate(version)
    }

    private fun loadUpdateHistory() {
        val json = prefs.getString("update_history", "[]")
        runCatching {
            val array = JSONArray(json)
            val items = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        UpdateInfo(
                            version = obj.optString("version", ""),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            isInstalled = obj.optBoolean("installed", false),
                            isAvailable = obj.optBoolean("available", false)
                        )
                    )
                }
            }
            _updateHistory.value = items
        }
    }

    private fun saveUpdateHistory() {
        val array = JSONArray()
        _updateHistory.value.forEach { info ->
            array.put(JSONObject().apply {
                put("version", info.version)
                put("date", info.date)
                put("installed", info.isInstalled)
                put("available", info.isAvailable)
            })
        }
        prefs.putString("update_history", array.toString())
    }
}
