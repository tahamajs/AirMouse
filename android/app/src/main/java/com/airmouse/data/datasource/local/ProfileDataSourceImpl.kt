// app/src/main/java/com/airmouse/data/datasource/local/ProfileDataSourceImpl.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile
import com.airmouse.utils.PreferencesManager
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IProfileDataSource {

    private val profilesKey = "user_profiles"
    private val defaultProfileKey = "default_profile"
    private val favoriteProfilesKey = "favorite_profiles"

    override suspend fun saveProfile(profile: UserProfile) {
        val profiles = getAllProfiles().toMutableList()
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        if (existingIndex >= 0) {
            profiles[existingIndex] = profile
        } else {
            profiles.add(profile)
        }
        saveProfilesToPrefs(profiles)
    }

    override suspend fun getProfile(id: String): UserProfile? {
        return getAllProfiles().find { it.id == id }
    }

    override suspend fun getAllProfiles(): List<UserProfile> {
        val json = prefs.getString(profilesKey, "[]")
        return parseProfilesFromJson(json)
    }

    override suspend fun deleteProfile(id: String) {
        val profiles = getAllProfiles().toMutableList()
        profiles.removeAll { it.id == id }
        saveProfilesToPrefs(profiles)

        // If deleted profile was default, clear default
        if (getDefaultProfile()?.id == id) {
            prefs.remove(defaultProfileKey)
        }
    }

    override suspend fun updateProfile(profile: UserProfile) {
        saveProfile(profile)
    }

    override suspend fun setDefaultProfile(id: String) {
        prefs.putString(defaultProfileKey, id)
    }

    override suspend fun getDefaultProfile(): UserProfile? {
        val id = prefs.getString(defaultProfileKey, "")
        return if (id.isNotEmpty()) getProfile(id) else null
    }

    override suspend fun toggleFavorite(id: String) {
        val favorites = prefs.getString(favoriteProfilesKey, "").split(",").filter { it.isNotEmpty() }.toMutableList()
        if (favorites.contains(id)) {
            favorites.remove(id)
        } else {
            favorites.add(id)
        }
        prefs.putString(favoriteProfilesKey, favorites.joinToString(","))
    }

    override suspend fun getFavoriteProfiles(): List<UserProfile> {
        val favoriteIds = prefs.getString(favoriteProfilesKey, "").split(",").filter { it.isNotEmpty() }
        return getAllProfiles().filter { it.id in favoriteIds }
    }

    override suspend fun saveProfileSettings(profileId: String, settings: ProfileSettings) {
        val profile = getProfile(profileId)
        if (profile != null) {
            val updatedProfile = profile.copy(settings = settings)
            saveProfile(updatedProfile)
        }
    }

    override suspend fun getProfileSettings(profileId: String): ProfileSettings? {
        return getProfile(profileId)?.settings
    }

    override suspend fun searchProfiles(query: String): List<UserProfile> {
        val lowerQuery = query.lowercase()
        return getAllProfiles().filter { profile ->
            profile.name.lowercase().contains(lowerQuery) ||
                    profile.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }

    private fun parseProfilesFromJson(json: String): List<UserProfile> {
        val list = mutableListOf<UserProfile>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val settings = parseSettings(obj.getJSONObject("settings"))
                val profile = UserProfile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    email = obj.optString("email", ""),
                    avatarUri = obj.optString("avatarUri", ""),
                    settings = settings,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    isDefault = obj.optBoolean("isDefault", false),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    tags = obj.optJSONArray("tags")?.let { tagsArray ->
                        (0 until tagsArray.length()).map { tagsArray.getString(it) }
                    } ?: emptyList(),
                    iconRes = obj.optInt("iconRes", 0)
                )
                list.add(profile)
            }
        } catch (e: Exception) {
            // Return empty list
        }
        return list
    }

    private fun parseSettings(obj: JSONObject): ProfileSettings {
        return ProfileSettings(
            sensitivity = obj.optDouble("sensitivity", 1.0).toFloat(),
            clickThreshold = obj.optDouble("clickThreshold", 5.0).toFloat(),
            doubleClickInterval = obj.optLong("doubleClickInterval", 400),
            scrollThreshold = obj.optDouble("scrollThreshold", 8.0).toFloat(),
            rightClickTilt = obj.optDouble("rightClickTilt", 45.0).toFloat(),
            hapticEnabled = obj.optBoolean("hapticEnabled", true),
            theme = obj.optString("theme", "dark"),
            aiSmoothing = obj.optBoolean("aiSmoothing", false),
            predictiveMovement = obj.optBoolean("predictiveMovement", true),
            invertX = obj.optBoolean("invertX", false),
            invertY = obj.optBoolean("invertY", false),
            accelerationEnabled = obj.optBoolean("accelerationEnabled", true),
            smoothingEnabled = obj.optBoolean("smoothingEnabled", true),
            edgeGesturesEnabled = obj.optBoolean("edgeGesturesEnabled", false),
            voiceCommandsEnabled = obj.optBoolean("voiceCommandsEnabled", false)
        )
    }

    private fun saveProfilesToPrefs(profiles: List<UserProfile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            val obj = JSONObject()
            obj.put("id", profile.id)
            obj.put("name", profile.name)
            obj.put("email", profile.email)
            obj.put("avatarUri", profile.avatarUri ?: "")

            val settingsObj = JSONObject()
            settingsObj.put("sensitivity", profile.settings.sensitivity)
            settingsObj.put("clickThreshold", profile.settings.clickThreshold)
            settingsObj.put("doubleClickInterval", profile.settings.doubleClickInterval)
            settingsObj.put("scrollThreshold", profile.settings.scrollThreshold)
            settingsObj.put("rightClickTilt", profile.settings.rightClickTilt)
            settingsObj.put("hapticEnabled", profile.settings.hapticEnabled)
            settingsObj.put("theme", profile.settings.theme)
            settingsObj.put("aiSmoothing", profile.settings.aiSmoothing)
            settingsObj.put("predictiveMovement", profile.settings.predictiveMovement)
            settingsObj.put("invertX", profile.settings.invertX)
            settingsObj.put("invertY", profile.settings.invertY)
            settingsObj.put("accelerationEnabled", profile.settings.accelerationEnabled)
            settingsObj.put("smoothingEnabled", profile.settings.smoothingEnabled)
            settingsObj.put("edgeGesturesEnabled", profile.settings.edgeGesturesEnabled)
            settingsObj.put("voiceCommandsEnabled", profile.settings.voiceCommandsEnabled)
            obj.put("settings", settingsObj)

            obj.put("createdAt", profile.createdAt)
            obj.put("updatedAt", profile.updatedAt)
            obj.put("isDefault", profile.isDefault)
            obj.put("isFavorite", profile.isFavorite)

            val tagsArray = JSONArray()
            profile.tags.forEach { tagsArray.put(it) }
            obj.put("tags", tagsArray)
            obj.put("iconRes", profile.iconRes)

            array.put(obj)
        }
        prefs.putString(profilesKey, array.toString())
    }
}