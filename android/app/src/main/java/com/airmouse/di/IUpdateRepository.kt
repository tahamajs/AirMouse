// app/src/main/java/com/airmouse/domain/repository/IUpdateRepository.kt
package com.airmouse.domain.repository

interface IUpdateRepository {
    suspend fun checkForUpdates(): UpdateResult
    suspend fun downloadUpdate(version: String): Boolean
    suspend fun installUpdate()
    suspend fun getCurrentVersion(): VersionInfo
    suspend fun getUpdateHistory(): List<UpdateInfo>
}

data class UpdateResult(
    val isAvailable: Boolean,
    val version: String?,
    val releaseNotes: String?,
    val fileSize: Long
)

data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildDate: Long
)

data class UpdateInfo(
    val version: String,
    val date: Long,
    val isInstalled: Boolean
)// app/src/main/java/com/airmouse/domain/repository/IUpdateRepository.kt
package com.airmouse.domain.repository

interface IUpdateRepository {
    suspend fun checkForUpdates(): UpdateResult
    suspend fun downloadUpdate(version: String): Boolean
    suspend fun installUpdate()
    suspend fun getCurrentVersion(): VersionInfo
    suspend fun getUpdateHistory(): List<UpdateInfo>
}

data class UpdateResult(
    val isAvailable: Boolean,
    val version: String?,
    val releaseNotes: String?,
    val fileSize: Long
)

data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildDate: Long
)

data class UpdateInfo(
    val version: String,
    val date: Long,
    val isInstalled: Boolean
)