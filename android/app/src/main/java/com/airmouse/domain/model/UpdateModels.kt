// app/src/main/java/com/airmouse/domain/model/UpdateModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Update result
 */
data class UpdateResult(
    val isAvailable: Boolean = false,
    val version: String? = null,
    val releaseNotes: String? = null,
    val fileSize: Long = 0,
    val downloadUrl: String? = null
)

/**
 * Version info
 */
@Parcelize
data class VersionInfo(
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val buildDate: Long = System.currentTimeMillis(),
    val minSupportedVersion: String = "1.0.0"
) : Parcelable

/**
 * Update info
 */
@Parcelize
data class UpdateInfo(
    val version: String = "",
    val date: Long = System.currentTimeMillis(),
    val isInstalled: Boolean = false,
    val isAvailable: Boolean = false
) : Parcelable

/**
 * Update progress
 */
data class UpdateProgress(
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val isComplete: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
)