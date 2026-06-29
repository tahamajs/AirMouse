package com.airmouse.data.repository

import com.airmouse.BuildConfig
import com.airmouse.domain.model.UpdateInfo
import com.airmouse.domain.model.UpdateProgress
import com.airmouse.domain.model.UpdateResult
import com.airmouse.domain.model.VersionInfo
import com.airmouse.domain.repository.IUpdateRepository
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : IUpdateRepository {

    // ============================================================
    // State Flows
    // ============================================================

    private val _status = MutableStateFlow(UpdateResult())
    override fun observeUpdateStatus(): StateFlow<UpdateResult> = _status.asStateFlow()

    private val _downloadProgress = MutableStateFlow(UpdateProgress())
    override fun observeDownloadProgress(): StateFlow<UpdateProgress> = _downloadProgress.asStateFlow()

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

    // ============================================================
    // Init
    // ============================================================

    init {
        loadUpdateHistory()
        Timber.d("UpdateRepository initialized with version ${_currentVersion.value.versionName}")
    }

    // ============================================================
    // Check for Updates
    // ============================================================

    override suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        Timber.d("Checking for updates...")
        _isChecking.value = true

        try {
            val currentVersion = _currentVersion.value.versionName
            val latestVersion = fetchLatestVersionFromServer()

            val result = if (latestVersion != null && latestVersion > currentVersion) {
                UpdateResult(
                    isAvailable = true,
                    version = latestVersion,
                    releaseNotes = fetchReleaseNotes(latestVersion),
                    fileSize = fetchFileSize(latestVersion),
                    downloadUrl = fetchDownloadUrl(latestVersion)
                )
            } else {
                UpdateResult(
                    isAvailable = false,
                    version = currentVersion,
                    releaseNotes = null,
                    fileSize = 0,
                    downloadUrl = null
                )
            }

            _status.value = result
            _isChecking.value = false

            if (result.isAvailable) {
                Timber.i("Update available: ${result.version}")
            } else {
                Timber.d("No updates available")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for updates")
            _isChecking.value = false
            UpdateResult(
                isAvailable = false,
                version = _currentVersion.value.versionName,
                releaseNotes = null,
                fileSize = 0,
                downloadUrl = null
            )
        }
    }

    override suspend fun checkForUpdatesManually(): UpdateResult = withContext(Dispatchers.IO) {
        Timber.d("Manual update check triggered")
        val result = checkForUpdates()
        if (result.isAvailable) {
            Timber.i("Manual check found update: ${result.version}")
        } else {
            Timber.d("Manual check: No updates available")
        }
        result
    }

    // ============================================================
    // Download Update
    // ============================================================

    override suspend fun downloadUpdate(version: String): Boolean {
        return downloadUpdate(version) { }
    }

    override suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            if (_isDownloading.value) {
                Timber.w("Download already in progress")
                return@withContext false
            }

            Timber.d("Starting download for version: $version")
            _isDownloading.value = true
            _downloadProgress.value = UpdateProgress(progress = 0f, isComplete = false)

            try {
                val downloadUrl = fetchDownloadUrl(version)
                if (downloadUrl == null) {
                    Timber.e("No download URL found for version $version")
                    _isDownloading.value = false
                    return@withContext false
                }

                val connection = URL(downloadUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Timber.e("Download failed with response code: $responseCode")
                    _isDownloading.value = false
                    _downloadProgress.value = UpdateProgress(
                        isComplete = false,
                        isError = true,
                        errorMessage = "Download failed: HTTP $responseCode"
                    )
                    return@withContext false
                }

                val fileSize = connection.contentLengthLong
                val inputStream = connection.inputStream
                val outputFile = createTempFile("update_$version", ".apk")
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (fileSize > 0) {
                        val progress = (totalBytesRead.toFloat() / fileSize.toFloat()) * 100f
                        _downloadProgress.value = UpdateProgress(
                            progress = progress / 100f,
                            bytesDownloaded = totalBytesRead,
                            totalBytes = fileSize,
                            isComplete = false
                        )
                        onProgress(progress / 100f)
                    }
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                _downloadProgress.value = UpdateProgress(
                    progress = 1f,
                    bytesDownloaded = totalBytesRead,
                    totalBytes = fileSize,
                    isComplete = true
                )

                Timber.i("Download completed: ${outputFile.absolutePath}")
                _isDownloading.value = false
                true
            } catch (e: Exception) {
                Timber.e(e, "Download failed for version $version")
                _isDownloading.value = false
                _downloadProgress.value = UpdateProgress(
                    isComplete = false,
                    isError = true,
                    errorMessage = e.message ?: "Download failed"
                )
                false
            }
        }

    // ============================================================
    // Install Update
    // ============================================================

    override suspend fun installUpdate(version: String): Boolean = withContext(Dispatchers.IO) {
        if (_isInstalling.value) {
            Timber.w("Install already in progress")
            return@withContext false
        }

        Timber.d("Installing update for version: $version")
        _isInstalling.value = true

        try {
            // Find the downloaded APK file
            val downloadDir = createTempDir()
            val apkFile = downloadDir.listFiles()?.find { it.name.startsWith("update_$version") && it.extension == "apk" }

            if (apkFile == null || !apkFile.exists()) {
                Timber.e("APK file not found for version $version")
                _isInstalling.value = false
                return@withContext false
            }

            // In a real implementation, you would use PackageInstaller or Intent to install
            // For now, we'll just simulate successful installation
            val info = UpdateInfo(
                version = version,
                date = System.currentTimeMillis(),
                isInstalled = true,
                isAvailable = false
            )
            _updateHistory.value = listOf(info) + _updateHistory.value
            saveUpdateHistory()

            // Update current version
            _currentVersion.value = _currentVersion.value.copy(
                versionName = version,
                versionCode = _currentVersion.value.versionCode + 1
            )

            _isInstalling.value = false
            Timber.i("Update installed successfully: $version")
            true
        } catch (e: Exception) {
            Timber.e(e, "Install failed for version $version")
            _isInstalling.value = false
            false
        }
    }

    override suspend fun installUpdate() {
        val version = _status.value.version
        if (version != null) {
            installUpdate(version)
        } else {
            Timber.w("No update version available to install")
        }
    }

    // ============================================================
    // Version Info
    // ============================================================

    override suspend fun getCurrentVersion(): VersionInfo = _currentVersion.value

    override suspend fun getLatestVersion(): VersionInfo? {
        val result = checkForUpdates()
        return result.version?.let {
            VersionInfo(
                versionName = it,
                versionCode = _currentVersion.value.versionCode + 1,
                buildDate = System.currentTimeMillis()
            )
        }
    }

    override suspend fun getUpdateHistory(): List<UpdateInfo> = _updateHistory.value

    // ============================================================
    // Status Checks
    // ============================================================

    override suspend fun isChecking(): Boolean = _isChecking.value
    override suspend fun isDownloading(): Boolean = _isDownloading.value
    override suspend fun isInstalling(): Boolean = _isInstalling.value

    // ============================================================
    // Cancel Operations
    // ============================================================

    override suspend fun cancelDownload() {
        _isDownloading.value = false
        _downloadProgress.value = UpdateProgress(
            isComplete = false,
            isError = true,
            errorMessage = "Cancelled"
        )
        Timber.d("Download cancelled")
    }

    override suspend fun cancelInstall() {
        _isInstalling.value = false
        Timber.d("Install cancelled")
    }

    // ============================================================
    // Combined Operation
    // ============================================================

    override suspend fun downloadAndInstallUpdate(version: String): Boolean {
        Timber.d("Downloading and installing update: $version")
        val downloaded = downloadUpdate(version)
        if (!downloaded) {
            Timber.e("Download failed for version $version")
            return false
        }
        return installUpdate(version)
    }

    // ============================================================
    // Private Helpers - Server Communication
    // ============================================================

    private suspend fun fetchLatestVersionFromServer(): String? {
        return withContext(Dispatchers.IO) {
            try {
                // In production, fetch from a server
                // For now, return hardcoded value for testing
                "3.0.1"
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch latest version")
                null
            }
        }
    }

    private suspend fun fetchReleaseNotes(version: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // In production, fetch from a server
                "Bug fixes and performance improvements for version $version"
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch release notes")
                null
            }
        }
    }

    private suspend fun fetchFileSize(version: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                // In production, fetch from a server
                4L * 1024L * 1024L // 4MB
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch file size")
                0L
            }
        }
    }

    private suspend fun fetchDownloadUrl(version: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // In production, fetch from a server
                "https://airmouse.io/downloads/airmouse-$version.apk"
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch download URL")
                null
            }
        }
    }

    private fun createTempFile(prefix: String, suffix: String): File {
        val tempDir = File(android.os.Environment.getExternalStorageDirectory(), ".airmouse_updates")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return File(tempDir, "$prefix$suffix")
    }

    private fun createTempDir(): File {
        val tempDir = File(android.os.Environment.getExternalStorageDirectory(), ".airmouse_updates")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }

    // ============================================================
    // Private Helpers - Persistence
    // ============================================================

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
        }.onFailure { e ->
            Timber.e(e, "Failed to load update history")
            _updateHistory.value = emptyList()
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
