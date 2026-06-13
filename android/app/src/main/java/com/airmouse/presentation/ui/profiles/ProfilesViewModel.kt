package com.airmouse.presentation.ui.profiles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profiles = mutableListOf<UserProfile>()

            // Load default profile
            val defaultProfile = UserProfile(
                id = "default",
                name = "Default",
                isDefault = true,
                settings = ProfileSettings(
                    sensitivity = prefs.getFloat("sensitivity", 0.5f),
                    clickThreshold = prefs.getFloat("click_threshold", 8f),
                    doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                    scrollThreshold = prefs.getFloat("scroll_threshold", 6f),
                    rightClickTilt = prefs.getFloat("right_click_tilt", 45f),
                    hapticFeedback = prefs.getBoolean("haptic_enabled", true),
                    aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                    predictiveMovement = prefs.getBoolean("predictive_movement", false)
                )
            )
            profiles.add(defaultProfile)

            // Load custom profiles
            val savedProfiles = prefs.getString("custom_profiles", "")
            if (savedProfiles.isNotEmpty()) {
                val jsonArray = JSONArray(savedProfiles)
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    val profile = UserProfile(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        createdAt = json.getLong("createdAt"),
                        updatedAt = json.getLong("updatedAt"),
                        isFavorite = json.optBoolean("isFavorite", false),
                        settings = ProfileSettings(
                            sensitivity = json.getDouble("sensitivity").toFloat(),
                            clickThreshold = json.getDouble("clickThreshold").toFloat(),
                            doubleClickInterval = json.getLong("doubleClickInterval"),
                            scrollThreshold = json.getDouble("scrollThreshold").toFloat(),
                            rightClickTilt = json.getDouble("rightClickTilt").toFloat(),
                            hapticFeedback = json.getBoolean("hapticFeedback"),
                            aiSmoothing = json.optBoolean("aiSmoothing", false),
                            predictiveMovement = json.optBoolean("predictiveMovement", false)
                        )
                    )
                    profiles.add(profile)
                }
            }

            val sortedProfiles = sortProfiles(profiles, _uiState.value.sortBy)

            _uiState.update {
                it.copy(
                    profiles = sortedProfiles,
                    selectedProfile = sortedProfiles.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }

    private fun saveProfiles(profiles: List<UserProfile>) {
        val jsonArray = JSONArray()
        profiles.filter { !it.isDefault }.forEach { profile ->
            val json = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("createdAt", profile.createdAt)
                put("updatedAt", System.currentTimeMillis())
                put("isFavorite", profile.isFavorite)
                put("sensitivity", profile.settings.sensitivity)
                put("clickThreshold", profile.settings.clickThreshold)
                put("doubleClickInterval", profile.settings.doubleClickInterval)
                put("scrollThreshold", profile.settings.scrollThreshold)
                put("rightClickTilt", profile.settings.rightClickTilt)
                put("hapticFeedback", profile.settings.hapticFeedback)
                put("aiSmoothing", profile.settings.aiSmoothing)
                put("predictiveMovement", profile.settings.predictiveMovement)
            }
            jsonArray.put(json)
        }
        prefs.putString("custom_profiles", jsonArray.toString())
    }

    fun updateNewProfileName(name: String) {
        _uiState.update { it.copy(newProfileName = name) }
    }

    fun selectProfile(profile: UserProfile) {
        _uiState.update { it.copy(selectedProfile = profile) }
        applyProfileSettings(profile)
    }

    private fun applyProfileSettings(profile: UserProfile) {
        viewModelScope.launch {
            prefs.putFloat("sensitivity", profile.settings.sensitivity)
            prefs.putFloat("click_threshold", profile.settings.clickThreshold)
            prefs.putLong("double_click_interval", profile.settings.doubleClickInterval)
            prefs.putFloat("scroll_threshold", profile.settings.scrollThreshold)
            prefs.putFloat("right_click_tilt", profile.settings.rightClickTilt)
            prefs.putBoolean("haptic_enabled", profile.settings.hapticFeedback)
            prefs.putBoolean("ai_smoothing", profile.settings.aiSmoothing)
            prefs.putBoolean("predictive_movement", profile.settings.predictiveMovement)

            _uiState.update {
                it.copy(successMessage = "Profile '${profile.name}' loaded successfully")
            }
            clearMessages()
        }
    }

    fun createProfile(name: String) {
        val newProfile = UserProfile(
            name = name,
            settings = ProfileSettings(
                sensitivity = prefs.getFloat("sensitivity", 0.5f),
                clickThreshold = prefs.getFloat("click_threshold", 8f),
                doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                scrollThreshold = prefs.getFloat("scroll_threshold", 6f),
                rightClickTilt = prefs.getFloat("right_click_tilt", 45f),
                hapticFeedback = prefs.getBoolean("haptic_enabled", true),
                aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                predictiveMovement = prefs.getBoolean("predictive_movement", false)
            )
        )

        val updatedProfiles = _uiState.value.profiles + newProfile
        val sortedProfiles = sortProfiles(updatedProfiles, _uiState.value.sortBy)

        _uiState.update {
            it.copy(
                profiles = sortedProfiles,
                selectedProfile = newProfile,
                newProfileName = "",
                successMessage = "Profile '$name' created successfully"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun saveProfile() {
        val name = _uiState.value.newProfileName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter a profile name") }
            return
        }

        if (_uiState.value.profiles.any { it.name.equals(name, ignoreCase = true) }) {
            _uiState.update { it.copy(errorMessage = "Profile name already exists") }
            return
        }

        createProfile(name)
    }

    fun updateProfile(profile: UserProfile) {
        val updatedProfiles = _uiState.value.profiles.map {
            if (it.id == profile.id) profile else it
        }
        val sortedProfiles = sortProfiles(updatedProfiles, _uiState.value.sortBy)

        _uiState.update {
            it.copy(
                profiles = sortedProfiles,
                selectedProfile = profile,
                showEditDialog = false,
                successMessage = "Profile '${profile.name}' updated"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun deleteProfile() {
        val profile = _uiState.value.selectedProfile
        if (profile == null || profile.isDefault) {
            _uiState.update { it.copy(errorMessage = "Cannot delete default profile") }
            return
        }

        val updatedProfiles = _uiState.value.profiles.filter { it.id != profile.id }
        val sortedProfiles = sortProfiles(updatedProfiles, _uiState.value.sortBy)

        _uiState.update {
            it.copy(
                profiles = sortedProfiles,
                selectedProfile = sortedProfiles.firstOrNull(),
                showDeleteDialog = false,
                successMessage = "Profile '${profile.name}' deleted"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun toggleFavorite(profile: UserProfile) {
        val updatedProfile = profile.copy(isFavorite = !profile.isFavorite)
        updateProfile(updatedProfile)
    }

    fun duplicateProfile(profile: UserProfile) {
        val newName = "${profile.name} (Copy)"
        val newProfile = profile.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            createdAt = System.currentTimeMillis(),
            isFavorite = false
        )

        val updatedProfiles = _uiState.value.profiles + newProfile
        val sortedProfiles = sortProfiles(updatedProfiles, _uiState.value.sortBy)

        _uiState.update {
            it.copy(
                profiles = sortedProfiles,
                selectedProfile = newProfile,
                successMessage = "Profile duplicated as '$newName'"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun exportProfiles() {
        viewModelScope.launch {
            try {
                val fileName = "airmouse_profiles_${System.currentTimeMillis()}.json"
                val file = File(context.getExternalFilesDir(null), fileName)

                val jsonArray = JSONArray()
                _uiState.value.profiles.forEach { profile ->
                    val json = JSONObject().apply {
                        put("id", profile.id)
                        put("name", profile.name)
                        put("createdAt", profile.createdAt)
                        put("isDefault", profile.isDefault)
                        put("isFavorite", profile.isFavorite)
                        put("settings", JSONObject().apply {
                            put("sensitivity", profile.settings.sensitivity)
                            put("clickThreshold", profile.settings.clickThreshold)
                            put("doubleClickInterval", profile.settings.doubleClickInterval)
                            put("scrollThreshold", profile.settings.scrollThreshold)
                            put("rightClickTilt", profile.settings.rightClickTilt)
                            put("hapticFeedback", profile.settings.hapticFeedback)
                            put("aiSmoothing", profile.settings.aiSmoothing)
                            put("predictiveMovement", profile.settings.predictiveMovement)
                        })
                    }
                    jsonArray.put(json)
                }

                file.writeText(jsonArray.toString(2))

                _uiState.update {
                    it.copy(
                        showExportDialog = false,
                        exportPath = file.absolutePath,
                        successMessage = "Profiles exported to $fileName"
                    )
                }
                clearMessages()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun setSortBy(sortBy: ProfileSort) {
        _uiState.update { it.copy(sortBy = sortBy) }
        val sortedProfiles = sortProfiles(_uiState.value.profiles, sortBy)
        _uiState.update { it.copy(profiles = sortedProfiles) }
    }

    private fun sortProfiles(profiles: List<UserProfile>, sortBy: ProfileSort): List<UserProfile> {
        return when (sortBy) {
            ProfileSort.NAME -> profiles.sortedBy { it.name.lowercase() }
            ProfileSort.DATE -> profiles.sortedByDescending { it.createdAt }
            ProfileSort.LAST_USED -> profiles.sortedByDescending { it.updatedAt }
            ProfileSort.FAVORITE -> profiles.sortedByDescending { it.isFavorite }
        }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getFilteredProfiles(): List<UserProfile> {
        val query = _uiState.value.searchQuery.lowercase()
        if (query.isEmpty()) return _uiState.value.profiles

        return _uiState.value.profiles.filter {
            it.name.lowercase().contains(query)
        }
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }

    fun showExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun showImportDialog(show: Boolean) {
        _uiState.update { it.copy(showImportDialog = show) }
    }

    fun showEditDialog(show: Boolean) {
        _uiState.update { it.copy(showEditDialog = show) }
    }

    fun updateSelectedProfileName(name: String) {
        _uiState.value.selectedProfile?.let { profile ->
            val updatedProfile = profile.copy(name = name, updatedAt = System.currentTimeMillis())
            updateProfile(updatedProfile)
        }
    }

    fun updateProfileName(name: String) {
        updateSelectedProfileName(name)
    }

    fun resetToDefault() {
        val defaultProfile = _uiState.value.profiles.find { it.isDefault }
        defaultProfile?.let {
            selectProfile(it)
            _uiState.update {
                it.copy(successMessage = "Reset to default settings")
            }
            clearMessages()
        }
    }

    private fun clearMessages() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update {
                it.copy(errorMessage = null, successMessage = null, exportPath = null)
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
}package com.airmouse.presentation.ui.profiles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profiles = mutableListOf<UserProfile>()

            // Load default profile
            val defaultProfile = UserProfile(
                id = "default",
                name = "Default",
                isDefault = true,
                settings = ProfileSettings(
                    sensitivity = prefs.getFloat("sensitivity", 0.5f),
                    clickThreshold = prefs.getFloat("click_threshold", 8f),
                    doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                    scrollThreshold = prefs.getFloat("scroll_threshold", 6f),
                    rightClickTilt = prefs.getFloat("right_click_tilt", 45f),
                    hapticFeedback = prefs.getBoolean("haptic_enabled", true),
                    aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                    predictiveMovement = prefs.getBoolean("predictive_movement", false)
                )
            )
            profiles.add(defaultProfile)

            // Load custom profiles
            val savedProfiles = prefs.getString("custom_profiles", "")
            if (savedProfiles.isNotEmpty()) {
                val jsonArray = JSONArray(savedProfiles)
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    val profile = UserProfile(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        createdAt = json.getLong("createdAt"),
                        updatedAt = json.getLong("updatedAt"),
                        isFavorite = json.optBoolean("isFavorite", false),
                        settings = ProfileSettings(
                            sensitivity = json.getDouble("sensitivity").toFloat(),
                            clickThreshold = json.getDouble("clickThreshold").toFloat(),
                            doubleClickInterval = json.getLong("doubleClickInterval"),
                            scrollThreshold = json.getDouble("scrollThreshold").toFloat(),
                            rightClickTilt = json.getDouble("rightClickTilt").toFloat(),
                            hapticFeedback = json.getBoolean("hapticFeedback"),
                            aiSmoothing = json.optBoolean("aiSmoothing", false),
                            predictiveMovement = json.optBoolean("predictiveMovement", false)
                        )
                    )
                    profiles.add(profile)
                }
            }

            val sortedProfiles = sortProfiles(profiles, _uiState.value.sortBy)

            _uiState.update {
                it.copy(
                    profiles = sortedProfiles,
                    selectedProfile = sortedProfiles.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }

    private fun saveProfiles(profiles: List<UserProfile>) {
        val jsonArray = JSONArray()
        profiles.filter { !it.isDefault }.forEach { profile ->
            val json = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("createdAt", profile.createdAt)
                put("updatedAt", System.currentTimeMillis())
                put("isFavorite", profile.isFavorite)
                put("sensitivity", profile.settings.sensitivity)
                put("clickThreshold", profile.settings.clickThreshold)
                put("doubleClickInterval", profile.settings.doubleClickInterval)
                put("scrollThreshold", profile.settings.scrollThreshold)
                put("rightClickTilt", profile.settings.rightClickTilt)
                put("hapticFeedback", profile.settings.hapticFeedback)
                put("aiSmoothing", profile.settings.aiSmoothing)
                put("predictiveMovement", profile.settings.predictiveMovement)
            }
            jsonArray.put(json)
        }
        prefs.putString("custom_profiles", jsonArray.toString())
    }

    fun updateNewProfileName(name: String) {
        _uiState.update { it.copy(newProfileName = name) }
    }

    fun selectProfile(profile: UserProfile) {
        _uiState.update { it.copy(selectedProfile = profile) }
        applyProfileSettings(profile)
    }

    private fun applyProfileSettings(profile: UserProfile) {
        viewModelScope.launch {
            prefs.putFloat("sensitivity", profile.settings.sensitivity)
            prefs.putFloat("click_threshold", profile.settings.clickThreshold)
            prefs.putLong("double_click_interval", profile.settings.doubleClickInterval)
            prefs.putFloat("scroll_threshold", profile.settings.scrollThreshold)
            prefs.putFloat("right_click_tilt", profile.settings.rightClickTilt)
            prefs.putBoolean("haptic_enabled", profile.settings.hapticFeedback)
            prefs.putBoolean("ai_smoothing", profile.settings.aiSmoothing)
            prefs.putBoolean("predictive_movement", profile.settings.predictiveMovement)

            _uiState.update {
                it.copy(successMessage = "Profile '${profile.name}' loaded successfully")
            }
            clearMessages()
        }
    }

    fun createProfile(name: String) {
        val newProfile = UserProfile(
            name = name,
            settings = ProfileSettings(
                sensitivity = prefs.getFloat("sensitivity", 0.5f),
                clickThreshold = prefs.getFloat("click_threshold", 8f),
                doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                scrollThreshold = prefs.getFloat("scroll_threshold", 6f),
                rightClickTilt = prefs.getFloat("right_click_tilt", 45f),
                hapticFeedback = prefs.getBoolean("haptic_enabled", true),
                aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                predictiveMovement = prefs.getBoolean("predictive_movement", false)
            )
        )

        val updatedProfiles = _uiState.value.profiles + newProfile
        val sortedProfiles = sortProfiles(updatedProfiles, _uiState.value.sortBy)

        _uiState.update {
            it.copy(
                profiles = sortedProfiles,
                selectedProfile = newProfile,
                newProfileName = "",
                successMessage = "Profile '$name' created successfully"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun saveProfile() {
        val name = _uiState.value.newProfileName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter a profile name") }
            return
        }

        if (_uiState.value.profiles.any { it.name.equals(name, ignoreCase = true) }) {
            _uiState.update { it.copy(errorMessage = "Profile name already exists") }
            return
        }

        createProfile(name)
    }

    fun updateProfile(profile: UserProfile) {
        val updatedProfiles = _uiState.value.profiles.map {
            if (it.id == profile.id) profile else it
        }
        val sortedProfiles = sortProfiles(updatedProfiles, _uiState.value.sortBy)

        _uiState.update {
            it.copy(
                profiles = sortedProfiles,
                selectedProfile = profile,
                showEditDialog = false,
                successMessage = "Profile '${profile.name}' updated"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun deleteProfile() {
        val profile = _uiState.value.selectedProfile
        if (profile == null || profile.isDefault) {
            _uiState.update { it.copy(errorMessage = "Cannot delete default profile") }
            return
        }

        val updatedProfiles = _uiState.value.profiles.filter { it.id != profile.id }
        val sortedProfiles = sortProfiles(updatedProfiles, _uiState.value.sortBy)

        _uiState.update {
            it.copy(
                profiles = sortedProfiles,
                selectedProfile = sortedProfiles.firstOrNull(),
                showDeleteDialog = false,
                successMessage = "Profile '${profile.name}' deleted"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun toggleFavorite(profile: UserProfile) {
        val updatedProfile = profile.copy(isFavorite = !profile.isFavorite)
        updateProfile(updatedProfile)
    }

    fun duplicateProfile(profile: UserProfile) {
        val newName = "${profile.name} (Copy)"
        val newProfile = profile.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            createdAt = System.currentTimeMillis(),
            isFavorite = false
        )

        val updatedProfiles = _uiState.value.profiles + newProfile
        val sortedProfiles = sortProfiles(updatedProfiles, _uiState.value.sortBy)

        _uiState.update {
            it.copy(
                profiles = sortedProfiles,
                selectedProfile = newProfile,
                successMessage = "Profile duplicated as '$newName'"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun exportProfiles() {
        viewModelScope.launch {
            try {
                val fileName = "airmouse_profiles_${System.currentTimeMillis()}.json"
                val file = File(context.getExternalFilesDir(null), fileName)

                val jsonArray = JSONArray()
                _uiState.value.profiles.forEach { profile ->
                    val json = JSONObject().apply {
                        put("id", profile.id)
                        put("name", profile.name)
                        put("createdAt", profile.createdAt)
                        put("isDefault", profile.isDefault)
                        put("isFavorite", profile.isFavorite)
                        put("settings", JSONObject().apply {
                            put("sensitivity", profile.settings.sensitivity)
                            put("clickThreshold", profile.settings.clickThreshold)
                            put("doubleClickInterval", profile.settings.doubleClickInterval)
                            put("scrollThreshold", profile.settings.scrollThreshold)
                            put("rightClickTilt", profile.settings.rightClickTilt)
                            put("hapticFeedback", profile.settings.hapticFeedback)
                            put("aiSmoothing", profile.settings.aiSmoothing)
                            put("predictiveMovement", profile.settings.predictiveMovement)
                        })
                    }
                    jsonArray.put(json)
                }

                file.writeText(jsonArray.toString(2))

                _uiState.update {
                    it.copy(
                        showExportDialog = false,
                        exportPath = file.absolutePath,
                        successMessage = "Profiles exported to $fileName"
                    )
                }
                clearMessages()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun setSortBy(sortBy: ProfileSort) {
        _uiState.update { it.copy(sortBy = sortBy) }
        val sortedProfiles = sortProfiles(_uiState.value.profiles, sortBy)
        _uiState.update { it.copy(profiles = sortedProfiles) }
    }

    private fun sortProfiles(profiles: List<UserProfile>, sortBy: ProfileSort): List<UserProfile> {
        return when (sortBy) {
            ProfileSort.NAME -> profiles.sortedBy { it.name.lowercase() }
            ProfileSort.DATE -> profiles.sortedByDescending { it.createdAt }
            ProfileSort.LAST_USED -> profiles.sortedByDescending { it.updatedAt }
            ProfileSort.FAVORITE -> profiles.sortedByDescending { it.isFavorite }
        }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getFilteredProfiles(): List<UserProfile> {
        val query = _uiState.value.searchQuery.lowercase()
        if (query.isEmpty()) return _uiState.value.profiles

        return _uiState.value.profiles.filter {
            it.name.lowercase().contains(query)
        }
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }

    fun showExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun showImportDialog(show: Boolean) {
        _uiState.update { it.copy(showImportDialog = show) }
    }

    fun showEditDialog(show: Boolean) {
        _uiState.update { it.copy(showEditDialog = show) }
    }

    fun updateSelectedProfileName(name: String) {
        _uiState.value.selectedProfile?.let { profile ->
            val updatedProfile = profile.copy(name = name, updatedAt = System.currentTimeMillis())
            updateProfile(updatedProfile)
        }
    }

    fun updateProfileName(name: String) {
        updateSelectedProfileName(name)
    }

    fun resetToDefault() {
        val defaultProfile = _uiState.value.profiles.find { it.isDefault }
        defaultProfile?.let {
            selectProfile(it)
            _uiState.update {
                it.copy(successMessage = "Reset to default settings")
            }
            clearMessages()
        }
    }

    private fun clearMessages() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update {
                it.copy(errorMessage = null, successMessage = null, exportPath = null)
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
}