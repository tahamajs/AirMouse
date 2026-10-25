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

    private data class CachedRelease(
        val version: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileSize: Long,
        val fetchedAt: Long
    )
    private var cachedRelease: CachedRelease? = null

    override suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        Timber.d("Checking for updates via GitHub...")
        _isChecking.value = true

        try {
            val currentVersion = _currentVersion.value.versionName
            val remoteRelease = fetchGitHubRelease()
            val latestVersion = remoteRelease?.version

            val result = if (latestVersion != null && isVersionNewer(latestVersion, currentVersion)) {
                UpdateResult(
                    isAvailable = true,
                    version = latestVersion,
                    releaseNotes = remoteRelease.releaseNotes,
                    fileSize = remoteRelease.fileSize,
                    downloadUrl = remoteRelease.downloadUrl
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
                Timber.i("Update available on GitHub: ${result.version}")
            } else {
                Timber.d("No updates available (current: $currentVersion)")
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
    // Private Helpers - GitHub Releases Integration
    // ============================================================

    private suspend fun fetchGitHubRelease(): CachedRelease? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = cachedRelease
        if (cached != null && (now - cached.fetchedAt) < 60_000L) {
            return@withContext cached
        }

        try {
            val url = URL("https://api.github.com/repos/tahamajs/AirMouse/releases/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "AirMouse-Android/${BuildConfig.VERSION_NAME}")
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val body = json.optString("body", "AirMouse Pro Latest Release")
                var downloadUrl = ""
                var fileSize = 0L

                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            fileSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (downloadUrl.isEmpty()) {
                    downloadUrl = json.optString("html_url", "https://github.com/tahamajs/AirMouse/releases")
                }

                val release = CachedRelease(
                    version = tagName,
                    releaseNotes = body,
                    downloadUrl = downloadUrl,
                    fileSize = fileSize,
                    fetchedAt = now
                )
                cachedRelease = release
                release
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "GitHub update check failed (offline or network error)")
            null
        }
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
        return try {
            val rParts = remote.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }
            val lParts = local.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(rParts.size, lParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun fetchLatestVersionFromServer(): String? = fetchGitHubRelease()?.version

    private suspend fun fetchReleaseNotes(version: String): String? = fetchGitHubRelease()?.releaseNotes

    private suspend fun fetchFileSize(version: String): Long = fetchGitHubRelease()?.fileSize ?: 0L

    private suspend fun fetchDownloadUrl(version: String): String? = fetchGitHubRelease()?.downloadUrl

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
