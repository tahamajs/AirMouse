package com.airmouse.data.repository

import com.airmouse.data.datasource.local.dao.ProfileDao
import com.airmouse.data.datasource.local.entity.ProfileEntity
import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile
import com.airmouse.domain.repository.IProfileRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val prefs: PreferencesManager
) : IProfileRepository {

    // ============================================================
    // CRUD Operations
    // ============================================================

    override suspend fun createProfile(profile: UserProfile): String {
        val id = profile.id.ifEmpty { UUID.randomUUID().toString() }
        val newProfile = profile.copy(id = id)
        val entity = mapToEntity(newProfile)
        profileDao.insertProfile(entity)
        return id
    }

    override suspend fun getProfile(id: String): UserProfile? {
        val entity = profileDao.getProfileById(id)
        return entity?.let { mapToDomain(it) }
    }

    override suspend fun updateProfile(profile: UserProfile) {
        val entity = mapToEntity(profile)
        profileDao.updateProfile(entity)
        profileDao.updateLastUsed(profile.id, System.currentTimeMillis())
    }

    override suspend fun deleteProfile(id: String) {
        val default = getDefaultProfile()
        if (default?.id == id) {
            return // Don't delete the default profile
        }
        profileDao.deleteProfile(id)
    }

    override suspend fun getAllProfiles(): List<UserProfile> {
        return profileDao.getAllProfiles().map { mapToDomain(it) }
    }

    override fun observeProfiles(): Flow<List<UserProfile>> {
        return profileDao.observeAllProfiles().map { entities ->
            entities.map { mapToDomain(it) }
        }
    }

    // ============================================================
    // Default Profile
    // ============================================================

    override suspend fun getDefaultProfile(): UserProfile? {
        val entity = profileDao.getDefaultProfile()
        return entity?.let { mapToDomain(it) }
    }

    override suspend fun setDefaultProfile(id: String) {
        profileDao.clearDefaultFlag()
        profileDao.setDefaultProfile(id)
        prefs.putString("default_profile_id", id)
    }

    // ============================================================
    // Favorites
    // ============================================================

    override suspend fun toggleFavorite(id: String) {
        val profile = profileDao.getProfileById(id)
        profile?.let {
            profileDao.setFavorite(id, !it.isFavorite)
        }
    }

    override suspend fun getFavoriteProfiles(): List<UserProfile> {
        return profileDao.getFavoriteProfiles().map { mapToDomain(it) }
    }

    override fun observeFavoriteProfiles(): Flow<List<UserProfile>> {
        return profileDao.observeAllProfiles().map { entities ->
            entities.filter { it.isFavorite }.map { mapToDomain(it) }
        }
    }

    // ============================================================
    // Settings
    // ============================================================

    override suspend fun getSettings(profileId: String): ProfileSettings? {
        val profile = getProfile(profileId)
        return profile?.settings
    }

    override suspend fun updateSettings(profileId: String, settings: ProfileSettings) {
        val profile = getProfile(profileId)
        profile?.let {
            val updated = profile.copy(settings = settings)
            updateProfile(updated)
        }
    }

    // ============================================================
    // Search
    // ============================================================

    override suspend fun searchProfiles(query: String): List<UserProfile> {
        return profileDao.searchProfiles(query).map { mapToDomain(it) }
    }

    // ============================================================
    // Export/Import
    // ============================================================

    override suspend fun exportProfile(id: String): String {
        val profile = getProfile(id) ?: return ""
        return mapToExportJson(profile).toString()
    }

    override suspend fun importProfile(json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val profile = mapFromExportJson(obj) ?: return false
            createProfile(profile)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ============================================================
    // Statistics
    // ============================================================

    override suspend fun getProfileCount(): Int {
        return profileDao.getProfileCount()
    }

    override suspend fun getProfileUsageStats(): Map<String, Int> {
        val profiles = getAllProfiles()
        return profiles.associate { it.name to it.usageCount }
    }

    // ============================================================
    // Mapping Functions
    // ============================================================

    private fun mapToEntity(domain: UserProfile): ProfileEntity {
        return ProfileEntity(
            id = domain.id,
            name = domain.name,
            email = domain.email,
            avatarUri = domain.avatarUri,
            sensitivity = domain.settings.sensitivity,
            clickThreshold = domain.settings.clickThreshold,
            doubleClickInterval = domain.settings.doubleClickInterval,
            scrollThreshold = domain.settings.scrollThreshold,
            rightClickTilt = domain.settings.rightClickTilt,
            hapticEnabled = domain.settings.hapticEnabled,
            theme = domain.settings.theme,
            aiSmoothing = domain.settings.aiSmoothing,
            predictiveMovement = domain.settings.predictiveMovement,
            invertX = domain.settings.invertX,
            invertY = domain.settings.invertY,
            accelerationEnabled = domain.settings.accelerationEnabled,
            smoothingEnabled = domain.settings.smoothingEnabled,
            edgeGesturesEnabled = domain.settings.edgeGesturesEnabled,
            voiceCommandsEnabled = domain.settings.voiceCommandsEnabled,
            isDefault = domain.isDefault,
            isFavorite = domain.isFavorite,
            tags = domain.tags.joinToString(","),
            iconRes = domain.iconRes,
            createdAt = domain.createdAt,
            lastUsed = domain.updatedAt
        )
    }

    private fun mapToDomain(entity: ProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            avatarUri = entity.avatarUri,
            settings = ProfileSettings(
                sensitivity = entity.sensitivity,
                clickThreshold = entity.clickThreshold,
                doubleClickInterval = entity.doubleClickInterval,
                scrollThreshold = entity.scrollThreshold,
                rightClickTilt = entity.rightClickTilt,
                hapticEnabled = entity.hapticEnabled,
                theme = entity.theme,
                aiSmoothing = entity.aiSmoothing,
                predictiveMovement = entity.predictiveMovement,
                invertX = entity.invertX,
                invertY = entity.invertY,
                accelerationEnabled = entity.accelerationEnabled,
                smoothingEnabled = entity.smoothingEnabled,
                edgeGesturesEnabled = entity.edgeGesturesEnabled,
                voiceCommandsEnabled = entity.voiceCommandsEnabled
            ),
            isDefault = entity.isDefault,
            isFavorite = entity.isFavorite,
            tags = entity.tags?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            iconRes = entity.iconRes,
            createdAt = entity.createdAt,
            updatedAt = entity.lastUsed,
            usageCount = 0
        )
    }

    private fun mapToExportJson(profile: UserProfile): JSONObject {
        return JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("email", profile.email)
            put("avatarUri", profile.avatarUri ?: "")

            val settingsObj = JSONObject().apply {
                put("sensitivity", profile.settings.sensitivity)
                put("clickThreshold", profile.settings.clickThreshold)
                put("doubleClickInterval", profile.settings.doubleClickInterval)
                put("scrollThreshold", profile.settings.scrollThreshold)
                put("rightClickTilt", profile.settings.rightClickTilt)
                put("hapticEnabled", profile.settings.hapticEnabled)
                put("theme", profile.settings.theme)
                put("aiSmoothing", profile.settings.aiSmoothing)
                put("predictiveMovement", profile.settings.predictiveMovement)
                put("invertX", profile.settings.invertX)
                put("invertY", profile.settings.invertY)
                put("accelerationEnabled", profile.settings.accelerationEnabled)
                put("smoothingEnabled", profile.settings.smoothingEnabled)
                put("edgeGesturesEnabled", profile.settings.edgeGesturesEnabled)
                put("voiceCommandsEnabled", profile.settings.voiceCommandsEnabled)
            }
            put("settings", settingsObj)

            put("isDefault", profile.isDefault)
            put("isFavorite", profile.isFavorite)
            put("createdAt", profile.createdAt)
            put("updatedAt", profile.updatedAt)

            val tagsArray = JSONArray()
            profile.tags.forEach { tagsArray.put(it) }
            put("tags", tagsArray)
            put("iconRes", profile.iconRes)
        }
    }

    private fun mapFromExportJson(obj: JSONObject): UserProfile? {
        return try {
            val settings = ProfileSettings(
                sensitivity = obj.getJSONObject("settings").optDouble("sensitivity", 1.0).toFloat(),
                clickThreshold = obj.getJSONObject("settings").optDouble("clickThreshold", 5.0).toFloat(),
                doubleClickInterval = obj.getJSONObject("settings").optLong("doubleClickInterval", 400),
                scrollThreshold = obj.getJSONObject("settings").optDouble("scrollThreshold", 8.0).toFloat(),
                rightClickTilt = obj.getJSONObject("settings").optDouble("rightClickTilt", 45.0).toFloat(),
                hapticEnabled = obj.getJSONObject("settings").optBoolean("hapticEnabled", true),
                theme = obj.getJSONObject("settings").optString("theme", "dark"),
                aiSmoothing = obj.getJSONObject("settings").optBoolean("aiSmoothing", false),
                predictiveMovement = obj.getJSONObject("settings").optBoolean("predictiveMovement", true),
                invertX = obj.getJSONObject("settings").optBoolean("invertX", false),
                invertY = obj.getJSONObject("settings").optBoolean("invertY", false),
                accelerationEnabled = obj.getJSONObject("settings").optBoolean("accelerationEnabled", true),
                smoothingEnabled = obj.getJSONObject("settings").optBoolean("smoothingEnabled", true),
                edgeGesturesEnabled = obj.getJSONObject("settings").optBoolean("edgeGesturesEnabled", false),
                voiceCommandsEnabled = obj.getJSONObject("settings").optBoolean("voiceCommandsEnabled", false)
            )

            UserProfile(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.getString("name"),
                email = obj.optString("email", ""),
                avatarUri = obj.optString("avatarUri", ""),
                settings = settings,
                tags = obj.optJSONArray("tags")?.let { tagsArray ->
                    (0 until tagsArray.length()).map { tagsArray.getString(it) }
                } ?: emptyList(),
                iconRes = obj.optInt("iconRes", 0),
                isDefault = false,
                isFavorite = false,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }
}