package com.airmouse.data.datasource.local

import com.airmouse.data.datasource.local.dao.ProfileDao
import com.airmouse.data.datasource.local.entity.ProfileEntity
import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileDataSourceImpl @Inject constructor(
    private val profileDao: ProfileDao
) : IProfileDataSource {

    // ============================================================
    // Basic CRUD
    // ============================================================

    override suspend fun saveProfile(profile: UserProfile) {
        val entity = ProfileEntity.fromDomainModel(profile)
        profileDao.insertProfile(entity)
    }

    override suspend fun getProfile(id: String): UserProfile? {
        return profileDao.getProfile(id)?.toDomainModel()
    }

    override suspend fun getAllProfiles(): List<UserProfile> {
        return profileDao.getAllProfiles().map { it.toDomainModel() }
    }

    override suspend fun deleteProfile(id: String) {
        profileDao.deleteProfile(id)
    }

    override suspend fun updateProfile(profile: UserProfile) {
        val entity = ProfileEntity.fromDomainModel(profile)
        profileDao.updateProfile(entity)
    }

    // ============================================================
    // Default Profile
    // ============================================================

    override suspend fun setDefaultProfile(id: String) {
        profileDao.clearDefaultFlag()
        profileDao.setDefaultProfile(id)
    }

    override suspend fun getDefaultProfile(): UserProfile? {
        return profileDao.getDefaultProfile()?.toDomainModel()
    }

    // ============================================================
    // Favorites
    // ============================================================

    override suspend fun toggleFavorite(id: String) {
        val profile = profileDao.getProfile(id) ?: return
        profileDao.setFavorite(id, !profile.isFavorite)
    }

    override suspend fun getFavoriteProfiles(): List<UserProfile> {
        return profileDao.getFavoriteProfiles().map { it.toDomainModel() }
    }

    // ============================================================
    // Profile Settings
    // ============================================================

    override suspend fun saveProfileSettings(profileId: String, settings: ProfileSettings) {
        val existing = profileDao.getProfile(profileId) ?: return
        val updated = existing.copy(
            sensitivity = settings.sensitivity,
            clickThreshold = settings.clickThreshold,
            doubleClickInterval = settings.doubleClickInterval,
            scrollThreshold = settings.scrollThreshold,
            rightClickTilt = settings.rightClickTilt,
            hapticEnabled = settings.hapticEnabled,
            theme = settings.theme,
            aiSmoothing = settings.aiSmoothing,
            predictiveMovement = settings.predictiveMovement,
            invertX = settings.invertX,
            invertY = settings.invertY,
            accelerationEnabled = settings.accelerationEnabled,
            smoothingEnabled = settings.smoothingEnabled,
            edgeGesturesEnabled = settings.edgeGesturesEnabled,
            voiceCommandsEnabled = settings.voiceCommandsEnabled,
            lastUsed = System.currentTimeMillis()
        )
        profileDao.updateProfile(updated)
    }

    override suspend fun getProfileSettings(profileId: String): ProfileSettings? {
        return profileDao.getProfile(profileId)?.toDomainModel()?.settings
    }

    // ============================================================
    // Search
    // ============================================================

    override suspend fun searchProfiles(query: String): List<UserProfile> {
        return profileDao.searchProfiles(query).map { it.toDomainModel() }
    }
}
