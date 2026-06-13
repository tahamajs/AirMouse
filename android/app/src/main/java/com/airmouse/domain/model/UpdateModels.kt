// app/src/main/java/com/airmouse/domain/model/UpdateModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Version information for the app.
 */
@Parcelize
data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildDate: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val releaseNotes: String = ""
) : Parcelable

/**
 * Update check result.
 */
@Parcelize
data class UpdateResult(
    val isAvailable: Boolean,
    val version: String? = null,
    val versionCode: Int = 0,
    val releaseNotes: String? = null,
    val fileSize: Long = 0,
    val downloadUrl: String? = null,
    val isMandatory: Boolean = false
) : Parcelable

/**
 * Update download progress.
 */
@Parcelize
data class UpdateProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progress: Int,
    val isComplete: Boolean = false,
    val error: String? = null
) : Parcelable// app/src/main/java/com/airmouse/domain/model/UpdateModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Version information for the app.
 */
@Parcelize
data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildDate: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val releaseNotes: String = ""
) : Parcelable

/**
 * Update check result.
 */
@Parcelize
data class UpdateResult(
    val isAvailable: Boolean,
    val version: String? = null,
    val versionCode: Int = 0,
    val releaseNotes: String? = null,
    val fileSize: Long = 0,
    val downloadUrl: String? = null,
    val isMandatory: Boolean = false
) : Parcelable

/**
 * Update download progress.
 */
@Parcelize
data class UpdateProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progress: Int,
    val isComplete: Boolean = false,
    val error: String? = null
) : Parcelable