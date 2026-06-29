package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Result of checking for an update.
 */
data class UpdateResult(
    val isAvailable: Boolean = false,
    val version: String? = null,
    val releaseNotes: String? = null,
    val fileSize: Long = 0,
    val downloadUrl: String? = null
) {
    /**
     * Check if the update is valid (has version and download URL).
     */
    fun isValid(): Boolean {
        return isAvailable && !version.isNullOrEmpty() && !downloadUrl.isNullOrEmpty()
    }

    /**
     * Get a formatted version string.
     */
    fun getVersionString(): String {
        return version ?: "Unknown"
    }

    companion object {
        /**
         * Create a result indicating no update is available.
         */
        fun none(): UpdateResult {
            return UpdateResult(isAvailable = false)
        }

        /**
         * Create a result with update information.
         */
        fun available(
            version: String,
            releaseNotes: String,
            fileSize: Long,
            downloadUrl: String
        ): UpdateResult {
            return UpdateResult(
                isAvailable = true,
                version = version,
                releaseNotes = releaseNotes,
                fileSize = fileSize,
                downloadUrl = downloadUrl
            )
        }
    }
}

/**
 * Information about the current app version.
 */
@Parcelize
data class VersionInfo(
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val buildDate: Long = System.currentTimeMillis(),
    val minSupportedVersion: String = "1.0.0"
) : Parcelable {

    /**
     * Check if this version is compatible with a minimum version.
     */
    fun isCompatibleWith(minVersion: String): Boolean {
        return compareVersions(versionName, minVersion) >= 0
    }

    /**
     * Get a formatted build date string.
     */
    fun getFormattedBuildDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(buildDate)
    }

    companion object {
        /**
         * Create a VersionInfo from a version string and code.
         */
        fun from(versionName: String, versionCode: Int): VersionInfo {
            return VersionInfo(
                versionName = versionName,
                versionCode = versionCode
            )
        }

        /**
         * Compare two version strings (e.g., "1.2.3").
         * Returns > 0 if v1 > v2, < 0 if v1 < v2, 0 if equal.
         */
        fun compareVersions(v1: String, v2: String): Int {
            val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLength = maxOf(parts1.size, parts2.size)
            for (i in 0 until maxLength) {
                val p1 = if (i < parts1.size) parts1[i] else 0
                val p2 = if (i < parts2.size) parts2[i] else 0
                if (p1 != p2) return p1 - p2
            }
            return 0
        }
    }
}

/**
 * Information about an available update.
 */
@Parcelize
data class UpdateInfo(
    val version: String = "",
    val date: Long = System.currentTimeMillis(),
    val isInstalled: Boolean = false,
    val isAvailable: Boolean = false
) : Parcelable {

    /**
     * Get a formatted date string.
     */
    fun getFormattedDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(date)
    }

    companion object {
        /**
         * Create an UpdateInfo indicating no update is available.
         */
        fun none(): UpdateInfo {
            return UpdateInfo(isAvailable = false)
        }

        /**
         * Create an UpdateInfo from UpdateResult.
         */
        fun fromResult(result: UpdateResult): UpdateInfo {
            return UpdateInfo(
                version = result.version ?: "",
                date = System.currentTimeMillis(),
                isAvailable = result.isAvailable,
                isInstalled = false
            )
        }
    }
}

/**
 * Progress of an ongoing update download.
 */
data class UpdateProgress(
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val isComplete: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Get the progress as a percentage (0-100).
     */
    fun getPercentage(): Int {
        return if (totalBytes > 0) {
            ((bytesDownloaded.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100)
        } else {
            (progress * 100).toInt().coerceIn(0, 100)
        }
    }

    /**
     * Create a copy with an error state.
     */
    fun withError(message: String): UpdateProgress {
        return copy(
            isError = true,
            isComplete = false,
            errorMessage = message
        )
    }

    /**
     * Create a copy marking completion.
     */
    fun withComplete(): UpdateProgress {
        return copy(
            isComplete = true,
            progress = 1f,
            bytesDownloaded = totalBytes
        )
    }

    companion object {
        /**
         * Create a new progress instance.
         */
        fun of(progress: Float): UpdateProgress {
            return UpdateProgress(progress = progress.coerceIn(0f, 1f))
        }

        /**
         * Create a progress instance from bytes.
         */
        fun ofBytes(downloaded: Long, total: Long): UpdateProgress {
            val progress = if (total > 0) downloaded.toFloat() / total else 0f
            return UpdateProgress(
                progress = progress.coerceIn(0f, 1f),
                bytesDownloaded = downloaded,
                totalBytes = total
            )
        }
    }
}