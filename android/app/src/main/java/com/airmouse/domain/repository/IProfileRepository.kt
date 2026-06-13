// app/src/main/java/com/airmouse/domain/repository/IProfileRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface IProfileRepository {
    suspend fun saveProfile(profile: UserProfile)
    suspend fun getProfile(id: String): UserProfile?
    suspend fun getAllProfiles(): List<UserProfile>
    suspend fun deleteProfile(id: String)
    suspend fun setActiveProfile(id: String)
    fun getActiveProfile(): Flow<UserProfile?>
    suspend fun duplicateProfile(id: String): UserProfile
    suspend fun exportProfile(id: String): String
    suspend fun importProfile(data: String): Boolean
}

data class UserProfile(
    val id: String,
    val name: String,
    val settings: ProfileSettings,
    val createdAt: Long,
    val lastUsed: Long,
    val isDefault: Boolean = false
)

data class ProfileSettings(
    val sensitivity: Float,
    val clickThreshold: Float,
    val scrollThreshold: Float,
    val rightClickTilt: Float,
    val hapticEnabled: Boolean,
    val theme: String
)// app/src/main/java/com/airmouse/domain/repository/IProfileRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface IProfileRepository {
    suspend fun saveProfile(profile: UserProfile)
    suspend fun getProfile(id: String): UserProfile?
    suspend fun getAllProfiles(): List<UserProfile>
    suspend fun deleteProfile(id: String)
    suspend fun setActiveProfile(id: String)
    fun getActiveProfile(): Flow<UserProfile?>
    suspend fun duplicateProfile(id: String): UserProfile
    suspend fun exportProfile(id: String): String
    suspend fun importProfile(data: String): Boolean
}

data class UserProfile(
    val id: String,
    val name: String,
    val settings: ProfileSettings,
    val createdAt: Long,
    val lastUsed: Long,
    val isDefault: Boolean = false
)

data class ProfileSettings(
    val sensitivity: Float,
    val clickThreshold: Float,
    val scrollThreshold: Float,
    val rightClickTilt: Float,
    val hapticEnabled: Boolean,
    val theme: String
)