package com.airmouse.data.repository

import com.airmouse.BuildConfig
import com.airmouse.data.datasource.local.AppDatabase
import com.airmouse.domain.repository.IUpdateRepository
import com.airmouse.domain.repository.UpdateInfo
import com.airmouse.domain.repository.UpdateResult
import com.airmouse.domain.repository.VersionInfo
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
    private val prefs: PreferencesManager,
    private val database: AppDatabase
) : IUpdateRepository {

    private val _currentVersion = MutableStateFlow(VersionInfo(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildDate = prefs.getLong("build_date", System.currentTimeMillis())
    ))
    override val currentVersion: StateFlow<VersionInfo> = _currentVersion.asStateFlow()

    private val _updateHistory = MutableStateFlow<List<UpdateInfo>>(emptyList())
    override val updateHistory: StateFlow<List<UpdateInfo>> = _updateHistory.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    override val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    override val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    init {
        loadUpdateHistory()
    }

    private fun loadUpdateHistory() {
        val json = prefs.getString("update_history", "")
        if (json.isNotEmpty()) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<UpdateInfo>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(UpdateInfo(
                        version = obj.getString("version"),
                        date = obj.getLong("date"),
                        isInstalled = obj.optBoolean("installed", false)
                    ))
                }
                _updateHistory.value = list
            } catch (e: Exception) {
                // Use empty list
            }
        }
    }

    private fun saveUpdateHistory() {
        val array = JSONArray()
        _updateHistory.value.forEach { info ->
            val obj = JSONObject().apply {
                put("version", info.version)
                put("date", info.date)
                put("installed", info.isInstalled)
            }
            array.put(obj)
        }
        prefs.putString("update_history", array.toString())
    }

    override suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        _isChecking.value = true
        try {
            // Simulate network delay
            delay(1500)

            // In production, this would check a real API endpoint
            val currentVersion = _currentVersion.value.versionName
            val latestVersion = "3.0.1" // Simulated latest version

            val isAvailable = latestVersion > currentVersion

            UpdateResult(
                isAvailable = isAvailable,
                version = if (isAvailable) latestVersion else null,
                releaseNotes = if (isAvailable) "Bug fixes and performance improvements" else null,
                fileSize = if (isAvailable) 1024 * 1024 * 4 else 0 // 4MB
            )
        } catch (e: Exception) {
            UpdateResult(isAvailable = false)
        } finally {
            _isChecking.value = false
        }
    }

    override suspend fun downloadUpdate(version: String): Boolean = withContext(Dispatchers.IO) {
        _downloadProgress.value = 0
        try {
            // Simulate download progress
            for (i in 0..100 step 5) {
                _downloadProgress.value = i
                delay(100)
            }
            _downloadProgress.value = 100

            // In production, this would download the actual APK
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun installUpdate() {
        withContext(Dispatchers.IO) {
            // In production, this would install the downloaded APK
            val version = _currentVersion.value.versionName
            val newVersionInfo = UpdateInfo(
                version = version,
                date = System.currentTimeMillis(),
                isInstalled = true
            )
            _updateHistory.value = listOf(newVersionInfo) + _updateHistory.value
            saveUpdateHistory()
        }
    }

    override suspend fun getCurrentVersion(): VersionInfo = _currentVersion.value

    override suspend fun getUpdateHistory(): List<UpdateInfo> = _updateHistory.value

    override suspend fun checkForUpdatesManually(): UpdateResult {
        return checkForUpdates()
    }

    override suspend fun downloadAndInstallUpdate(version: String): Boolean {
        val downloaded = downloadUpdate(version)
        if (downloaded) {
            installUpdate()
            return true
        }
        return false
    }
}