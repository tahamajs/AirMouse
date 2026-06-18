package com.airmouse.data.repository

import com.airmouse.data.datasource.local.ProfileDao
import com.airmouse.domain.model.Profile
import com.airmouse.domain.repository.IProfileRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    override fun getProfiles(): Flow<List<Profile>> = _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow<String?>(null)
    override fun getActiveProfileId(): Flow<String?> = _activeProfileId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override fun isLoading(): Flow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProfiles()
        loadActiveProfile()
    }

    private fun loadProfiles() {
        val saved = prefs.getString("profiles", "")
        if (saved.isNotEmpty()) {
            try {
                val profiles = parseProfilesFromJson(saved)
                _profiles.value = profiles
                return
            } catch (e: Exception) {
                // Fall through to defaults
            }
        }
        // Default profiles
        val defaultProfiles = listOf(
            Profile(
                id = "default",
                name = "Default",
                isDefault = true,
                sensitivity = 1.0f,
                smoothingEnabled = true,
                accelerationEnabled = true
            ),
            Profile(
                id = UUID.randomUUID().toString(),
                name = "Gaming",
                sensitivity = 1.5f,
                smoothingEnabled = false,
                accelerationEnabled = true
            ),
            Profile(
                id = UUID.randomUUID().toString(),
                name = "Precision",
                sensitivity = 0.5f,
                smoothingEnabled = true,
                accelerationEnabled = false
            ),
            Profile(
                id = UUID.randomUUID().toString(),
                name = "Presentation",
                sensitivity = 0.8f,
                smoothingEnabled = true,
                accelerationEnabled = true
            )
        )
        _profiles.value = defaultProfiles
        saveProfiles()
    }

    private fun parseProfilesFromJson(json: String): List<Profile> {
        val array = JSONArray(json)
        val list = mutableListOf<Profile>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(Profile(
                id = obj.getString("id"),
                name = obj.getString("name"),
                isDefault = obj.optBoolean("isDefault", false),
                sensitivity = obj.optDouble("sensitivity", 1.0).toFloat(),
                smoothingEnabled = obj.optBoolean("smoothingEnabled", true),
                accelerationEnabled = obj.optBoolean("accelerationEnabled", true),
                invertX = obj.optBoolean("invertX", false),
                invertY = obj.optBoolean("invertY", false),
                hapticEnabled = obj.optBoolean("hapticEnabled", true),
                theme = obj.optString("theme", "system")
            ))
        }
        return list
    }

    private fun profilesToJson(profiles: List<Profile>): String {
        val array = JSONArray()
        profiles.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("isDefault", p.isDefault)
                put("sensitivity", p.sensitivity)
                put("smoothingEnabled", p.smoothingEnabled)
                put("accelerationEnabled", p.accelerationEnabled)
                put("invertX", p.invertX)
                put("invertY", p.invertY)
                put("hapticEnabled", p.hapticEnabled)
                put("theme", p.theme)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun loadActiveProfile() {
        val id = prefs.getString("active_profile_id", "default")
        _activeProfileId.value = id
    }

    private fun saveProfiles() {
        prefs.putString("profiles", profilesToJson(_profiles.value))
    }

    override suspend fun getProfile(id: String): Profile? {
        return _profiles.value.find { it.id == id }
    }

    override suspend fun createProfile(name: String): Profile {
        val profile = Profile(
            id = UUID.randomUUID().toString(),
            name = name,
            isDefault = false,
            sensitivity = 1.0f,
            smoothingEnabled = true,
            accelerationEnabled = true
        )
        _profiles.update { it + profile }
        saveProfiles()
        return profile
    }

    override suspend fun updateProfile(profile: Profile) {
        _profiles.update { list ->
            list.map { if (it.id == profile.id) profile else it }
        }
        saveProfiles()
    }

    override suspend fun deleteProfile(id: String) {
        val profile = getProfile(id)
        if (profile?.isDefault == true) return
        _profiles.update { it.filter { it.id != id } }
        saveProfiles()
        if (_activeProfileId.value == id) {
            setActiveProfile("default")
        }
    }

    override suspend fun setActiveProfile(id: String) {
        _activeProfileId.value = id
        prefs.putString("active_profile_id", id)
        val profile = getProfile(id)
        profile?.let { applyProfile(it) }
    }

    private suspend fun applyProfile(profile: Profile) {
        prefs.putFloat("sensitivity", profile.sensitivity)
        prefs.putBoolean("smoothing_enabled", profile.smoothingEnabled)
        prefs.putBoolean("acceleration_enabled", profile.accelerationEnabled)
        prefs.putBoolean("invert_x", profile.invertX)
        prefs.putBoolean("invert_y", profile.invertY)
        prefs.putBoolean("haptic_enabled", profile.hapticEnabled)
        prefs.putString("theme", profile.theme)
    }

    override suspend fun duplicateProfile(id: String): Profile? {
        val original = getProfile(id) ?: return null
        val newProfile = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            isDefault = false
        )
        _profiles.update { it + newProfile }
        saveProfiles()
        return newProfile
    }

    override suspend fun getActiveProfile(): Profile? {
        val id = _activeProfileId.value ?: "default"
        return getProfile(id)
    }

    override suspend fun resetToDefault() {
        val defaultProfile = _profiles.value.find { it.isDefault }
        defaultProfile?.let { setActiveProfile(it.id) }
    }

    override suspend fun exportProfiles(): String {
        return profilesToJson(_profiles.value)
    }

    override suspend fun importProfiles(json: String): Boolean {
        return try {
            val profiles = parseProfilesFromJson(json)
            _profiles.value = profiles
            saveProfiles()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getProfileCount(): Int = _profiles.value.size
}