package com.airmouse.presentation.ui.profiles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
    
    val filteredProfiles: StateFlow<List<UserProfile>> = combine(
        _uiState.map { it.profiles },
        _uiState.map { it.searchQuery },
        _uiState.map { it.sortBy },
        _uiState.map { it.selectedTags }
    ) { profiles, query, sortBy, tags ->
        var result = profiles
        
        // Apply search filter
        if (query.isNotEmpty()) {
            result = result.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        // Apply tag filter
        if (tags.isNotEmpty()) {
            result = result.filter { it.tags.any { tag -> tags.contains(tag) } }
        }
        
        // Apply sorting
        when (sortBy) {
            ProfileSort.NAME -> result.sortedBy { it.name.lowercase() }
            ProfileSort.DATE_CREATED -> result.sortedByDescending { it.createdAt }
            ProfileSort.LAST_USED -> result.sortedByDescending { it.lastUsedAt }
            ProfileSort.FAVORITE -> result.sortedByDescending { it.isFavorite }
            ProfileSort.USAGE_COUNT -> result.sortedByDescending { it.usageCount }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profiles = mutableListOf<UserProfile>()

            // Load default profile from current settings
            val defaultProfile = UserProfile(
                id = "default",
                name = "Default",
                isDefault = true,
                usageCount = prefs.getInt("default_profile_usage", 0),
                settings = ProfileSettings(
                    sensitivity = prefs.getFloat("sensitivity", 1.0f),
                    smoothingEnabled = prefs.getBoolean("smoothing_enabled", true),
                    smoothingFactor = prefs.getFloat("smoothing_factor", 0.5f),
                    accelerationEnabled = prefs.getBoolean("acceleration_enabled", true),
                    accelerationFactor = prefs.getFloat("acceleration_factor", 1.5f),
                    invertX = prefs.getBoolean("invert_x", false),
                    invertY = prefs.getBoolean("invert_y", false),
                    clickThreshold = prefs.getFloat("click_threshold", 8f),
                    doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                    scrollThreshold = prefs.getFloat("scroll_threshold", 6f),
                    rightClickTilt = prefs.getFloat("right_click_tilt", 45f),
                    rightClickDuration = prefs.getLong("right_click_duration", 500L),
                    gestureDebounce = prefs.getLong("gesture_debounce", 100L),
                    aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                    aiBlendFactor = prefs.getFloat("ai_blend_factor", 0.7f),
                    predictiveMovement = prefs.getBoolean("predictive_movement", true),
                    predictionStrength = prefs.getFloat("prediction_strength", 0.5f),
                    kalmanEnabled = prefs.getBoolean("kalman_enabled", true),
                    hapticFeedback = prefs.getBoolean("haptic_enabled", true),
                    soundEnabled = prefs.getBoolean("sound_enabled", false),
                    visualFeedback = prefs.getBoolean("visual_feedback", true),
                    theme = prefs.getString("theme", "system"),
                    fontSize = prefs.getFloat("font_size", 16f),
                    showDebugInfo = prefs.getBoolean("show_debug_info", false),
                    keepScreenOn = prefs.getBoolean("keep_screen_on", false),
                    autoConnect = prefs.getBoolean("auto_connect", true),
                    reconnectAttempts = prefs.getInt("reconnect_attempts", 5),
                    connectionTimeout = prefs.getInt("connection_timeout", 5000),
                    useWebSocket = prefs.getBoolean("use_websocket", true),
                    jitterCompensation = prefs.getBoolean("jitter_compensation", true),
                    deadband = prefs.getFloat("deadband", 0.5f),
                    maxSpeed = prefs.getFloat("max_speed", 100f),
                    minSpeed = prefs.getFloat("min_speed", 0.5f)
                )
            )
            profiles.add(defaultProfile)

            // Load custom profiles from storage
            val savedProfiles = prefs.getString("custom_profiles", "")
            if (savedProfiles.isNotEmpty()) {
                try {
                    val jsonArray = JSONArray(savedProfiles)
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        val settings = ProfileSettings(
                            sensitivity = json.getDouble("sensitivity").toFloat(),
                            smoothingEnabled = json.optBoolean("smoothingEnabled", true),
                            smoothingFactor = json.optDouble("smoothingFactor", 0.5).toFloat(),
                            accelerationEnabled = json.optBoolean("accelerationEnabled", true),
                            accelerationFactor = json.optDouble("accelerationFactor", 1.5).toFloat(),
                            invertX = json.optBoolean("invertX", false),
                            invertY = json.optBoolean("invertY", false),
                            clickThreshold = json.getDouble("clickThreshold").toFloat(),
                            doubleClickInterval = json.getLong("doubleClickInterval"),
                            scrollThreshold = json.getDouble("scrollThreshold").toFloat(),
                            rightClickTilt = json.getDouble("rightClickTilt").toFloat(),
                            rightClickDuration = json.optLong("rightClickDuration", 500L),
                            gestureDebounce = json.optLong("gestureDebounce", 100L),
                            aiSmoothing = json.optBoolean("aiSmoothing", false),
                            aiBlendFactor = json.optDouble("aiBlendFactor", 0.7).toFloat(),
                            predictiveMovement = json.optBoolean("predictiveMovement", true),
                            predictionStrength = json.optDouble("predictionStrength", 0.5).toFloat(),
                            kalmanEnabled = json.optBoolean("kalmanEnabled", true),
                            hapticFeedback = json.optBoolean("hapticFeedback", true),
                            soundEnabled = json.optBoolean("soundEnabled", false),
                            visualFeedback = json.optBoolean("visualFeedback", true),
                            theme = json.optString("theme", "system"),
                            fontSize = json.optDouble("fontSize", 16.0).toFloat(),
                            showDebugInfo = json.optBoolean("showDebugInfo", false),
                            keepScreenOn = json.optBoolean("keepScreenOn", false),
                            autoConnect = json.optBoolean("autoConnect", true),
                            reconnectAttempts = json.optInt("reconnectAttempts", 5),
                            connectionTimeout = json.optInt("connectionTimeout", 5000),
                            useWebSocket = json.optBoolean("useWebSocket", true),
                            jitterCompensation = json.optBoolean("jitterCompensation", true),
                            deadband = json.optDouble("deadband", 0.5).toFloat(),
                            maxSpeed = json.optDouble("maxSpeed", 100.0).toFloat(),
                            minSpeed = json.optDouble("minSpeed", 0.5).toFloat()
                        )
                        
                        val profile = UserProfile(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            createdAt = json.getLong("createdAt"),
                            updatedAt = json.getLong("updatedAt"),
                            lastUsedAt = json.optLong("lastUsedAt", System.currentTimeMillis()),
                            isFavorite = json.optBoolean("isFavorite", false),
                            usageCount = json.optInt("usageCount", 0),
                            tags = json.optJSONArray("tags")?.let { arr ->
                                (0 until arr.length()).map { arr.getString(it) }
                            } ?: emptyList(),
                            iconRes = json.optInt("iconRes", 0).takeIf { it > 0 },
                            color = json.optString("color", "#6366F1"),
                            settings = settings
                        )
                        profiles.add(profile)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _uiState.update {
                it.copy(
                    profiles = profiles,
                    selectedProfile = profiles.firstOrNull(),
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
                put("updatedAt", profile.updatedAt)
                put("lastUsedAt", profile.lastUsedAt)
                put("isFavorite", profile.isFavorite)
                put("usageCount", profile.usageCount)
                put("tags", JSONArray(profile.tags))
                put("color", profile.color)
                profile.iconRes?.let { put("iconRes", it) }
                put("sensitivity", profile.settings.sensitivity)
                put("smoothingEnabled", profile.settings.smoothingEnabled)
                put("smoothingFactor", profile.settings.smoothingFactor)
                put("accelerationEnabled", profile.settings.accelerationEnabled)
                put("accelerationFactor", profile.settings.accelerationFactor)
                put("invertX", profile.settings.invertX)
                put("invertY", profile.settings.invertY)
                put("clickThreshold", profile.settings.clickThreshold)
                put("doubleClickInterval", profile.settings.doubleClickInterval)
                put("scrollThreshold", profile.settings.scrollThreshold)
                put("rightClickTilt", profile.settings.rightClickTilt)
                put("rightClickDuration", profile.settings.rightClickDuration)
                put("gestureDebounce", profile.settings.gestureDebounce)
                put("aiSmoothing", profile.settings.aiSmoothing)
                put("aiBlendFactor", profile.settings.aiBlendFactor)
                put("predictiveMovement", profile.settings.predictiveMovement)
                put("predictionStrength", profile.settings.predictionStrength)
                put("kalmanEnabled", profile.settings.kalmanEnabled)
                put("hapticFeedback", profile.settings.hapticFeedback)
                put("soundEnabled", profile.settings.soundEnabled)
                put("visualFeedback", profile.settings.visualFeedback)
                put("theme", profile.settings.theme)
                put("fontSize", profile.settings.fontSize)
                put("showDebugInfo", profile.settings.showDebugInfo)
                put("keepScreenOn", profile.settings.keepScreenOn)
                put("autoConnect", profile.settings.autoConnect)
                put("reconnectAttempts", profile.settings.reconnectAttempts)
                put("connectionTimeout", profile.settings.connectionTimeout)
                put("useWebSocket", profile.settings.useWebSocket)
                put("jitterCompensation", profile.settings.jitterCompensation)
                put("deadband", profile.settings.deadband)
                put("maxSpeed", profile.settings.maxSpeed)
                put("minSpeed", profile.settings.minSpeed)
            }
            jsonArray.put(json)
        }
        prefs.putString("custom_profiles", jsonArray.toString())
    }

    fun updateNewProfileName(name: String) {
        _uiState.update { it.copy(newProfileName = name) }
    }

    fun selectProfile(profile: UserProfile) {
        val incrementedProfile = profile.incrementUsage()
        
        val updatedProfiles = _uiState.value.profiles.map {
            if (it.id == profile.id) incrementedProfile else it
        }
        
        _uiState.update {
            it.copy(
                profiles = updatedProfiles,
                selectedProfile = incrementedProfile
            )
        }
        
        applyProfileSettings(incrementedProfile)
        saveProfiles(updatedProfiles)
    }

    private fun applyProfileSettings(profile: UserProfile) {
        viewModelScope.launch {
            // Apply all settings to preferences
            prefs.putFloat("sensitivity", profile.settings.sensitivity)
            prefs.putBoolean("smoothing_enabled", profile.settings.smoothingEnabled)
            prefs.putFloat("smoothing_factor", profile.settings.smoothingFactor)
            prefs.putBoolean("acceleration_enabled", profile.settings.accelerationEnabled)
            prefs.putFloat("acceleration_factor", profile.settings.accelerationFactor)
            prefs.putBoolean("invert_x", profile.settings.invertX)
            prefs.putBoolean("invert_y", profile.settings.invertY)
            prefs.putFloat("click_threshold", profile.settings.clickThreshold)
            prefs.putLong("double_click_interval", profile.settings.doubleClickInterval)
            prefs.putFloat("scroll_threshold", profile.settings.scrollThreshold)
            prefs.putFloat("right_click_tilt", profile.settings.rightClickTilt)
            prefs.putLong("right_click_duration", profile.settings.rightClickDuration)
            prefs.putLong("gesture_debounce", profile.settings.gestureDebounce)
            prefs.putBoolean("ai_smoothing", profile.settings.aiSmoothing)
            prefs.putFloat("ai_blend_factor", profile.settings.aiBlendFactor)
            prefs.putBoolean("predictive_movement", profile.settings.predictiveMovement)
            prefs.putFloat("prediction_strength", profile.settings.predictionStrength)
            prefs.putBoolean("kalman_enabled", profile.settings.kalmanEnabled)
            prefs.putBoolean("haptic_enabled", profile.settings.hapticFeedback)
            prefs.putBoolean("sound_enabled", profile.settings.soundEnabled)
            prefs.putBoolean("visual_feedback", profile.settings.visualFeedback)
            prefs.putString("theme", profile.settings.theme)
            prefs.putFloat("font_size", profile.settings.fontSize)
            prefs.putBoolean("show_debug_info", profile.settings.showDebugInfo)
            prefs.putBoolean("keep_screen_on", profile.settings.keepScreenOn)
            prefs.putBoolean("auto_connect", profile.settings.autoConnect)
            prefs.putInt("reconnect_attempts", profile.settings.reconnectAttempts)
            prefs.putInt("connection_timeout", profile.settings.connectionTimeout)
            prefs.putBoolean("use_websocket", profile.settings.useWebSocket)
            prefs.putBoolean("jitter_compensation", profile.settings.jitterCompensation)
            prefs.putFloat("deadband", profile.settings.deadband)
            prefs.putFloat("max_speed", profile.settings.maxSpeed)
            prefs.putFloat("min_speed", profile.settings.minSpeed)

            // Update default profile usage count
            if (profile.id == "default") {
                prefs.putInt("default_profile_usage", profile.usageCount)
            }

            showSuccess("Profile '${profile.name}' loaded")
        }
    }

    fun createProfile(name: String, settings: ProfileSettings? = null) {
        val newProfile = UserProfile(
            name = name,
            settings = settings ?: ProfileSettings(
                sensitivity = prefs.getFloat("sensitivity", 1.0f),
                smoothingEnabled = prefs.getBoolean("smoothing_enabled", true),
                smoothingFactor = prefs.getFloat("smoothing_factor", 0.5f),
                accelerationEnabled = prefs.getBoolean("acceleration_enabled", true),
                accelerationFactor = prefs.getFloat("acceleration_factor", 1.5f),
                invertX = prefs.getBoolean("invert_x", false),
                invertY = prefs.getBoolean("invert_y", false),
                clickThreshold = prefs.getFloat("click_threshold", 8f),
                doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                scrollThreshold = prefs.getFloat("scroll_threshold", 6f),
                rightClickTilt = prefs.getFloat("right_click_tilt", 45f),
                rightClickDuration = prefs.getLong("right_click_duration", 500L),
                gestureDebounce = prefs.getLong("gesture_debounce", 100L),
                aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                aiBlendFactor = prefs.getFloat("ai_blend_factor", 0.7f),
                predictiveMovement = prefs.getBoolean("predictive_movement", true),
                predictionStrength = prefs.getFloat("prediction_strength", 0.5f),
                kalmanEnabled = prefs.getBoolean("kalman_enabled", true),
                hapticFeedback = prefs.getBoolean("haptic_enabled", true),
                soundEnabled = prefs.getBoolean("sound_enabled", false),
                visualFeedback = prefs.getBoolean("visual_feedback", true),
                theme = prefs.getString("theme", "system"),
                fontSize = prefs.getFloat("font_size", 16f),
                showDebugInfo = prefs.getBoolean("show_debug_info", false),
                keepScreenOn = prefs.getBoolean("keep_screen_on", false),
                autoConnect = prefs.getBoolean("auto_connect", true),
                reconnectAttempts = prefs.getInt("reconnect_attempts", 5),
                connectionTimeout = prefs.getInt("connection_timeout", 5000),
                useWebSocket = prefs.getBoolean("use_websocket", true),
                jitterCompensation = prefs.getBoolean("jitter_compensation", true),
                deadband = prefs.getFloat("deadband", 0.5f),
                maxSpeed = prefs.getFloat("max_speed", 100f),
                minSpeed = prefs.getFloat("min_speed", 0.5f)
            )
        )

        val updatedProfiles = _uiState.value.profiles + newProfile
        
        _uiState.update {
            it.copy(
                profiles = updatedProfiles,
                selectedProfile = newProfile,
                newProfileName = "",
                successMessage = "Profile '${name}' created"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun saveProfile() {
        val name = _uiState.value.newProfileName.trim()
        if (name.isEmpty()) {
            showError("Please enter a profile name")
            return
        }

        if (_uiState.value.profiles.any { it.name.equals(name, ignoreCase = true) && !it.isDefault }) {
            showError("Profile name already exists")
            return
        }

        createProfile(name)
    }

    fun updateProfile(profile: UserProfile) {
        val updatedProfiles = _uiState.value.profiles.map {
            if (it.id == profile.id) profile else it
        }

        _uiState.update {
            it.copy(
                profiles = updatedProfiles,
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
            showError("Cannot delete default profile")
            return
        }

        val updatedProfiles = _uiState.value.profiles.filter { it.id != profile.id }
        
        _uiState.update {
            it.copy(
                profiles = updatedProfiles,
                selectedProfile = updatedProfiles.firstOrNull(),
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
        var newName = "${profile.name} (Copy)"
        var counter = 1
        while (_uiState.value.profiles.any { it.name == newName }) {
            counter++
            newName = "${profile.name} (Copy $counter)"
        }
        
        val newProfile = profile.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isFavorite = false,
            usageCount = 0
        )

        val updatedProfiles = _uiState.value.profiles + newProfile
        
        _uiState.update {
            it.copy(
                profiles = updatedProfiles,
                selectedProfile = newProfile,
                successMessage = "Profile duplicated as '$newName'"
            )
        }

        saveProfiles(updatedProfiles)
        clearMessages()
    }

    fun addTagToProfile(profile: UserProfile, tag: String) {
        if (!profile.tags.contains(tag)) {
            val updatedProfile = profile.copy(tags = profile.tags + tag)
            updateProfile(updatedProfile)
        }
    }

    fun removeTagFromProfile(profile: UserProfile, tag: String) {
        val updatedProfile = profile.copy(tags = profile.tags - tag)
        updateProfile(updatedProfile)
    }

    fun updateProfileColor(profile: UserProfile, color: String) {
        val updatedProfile = profile.copy(color = color)
        updateProfile(updatedProfile)
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
                        put("updatedAt", profile.updatedAt)
                        put("lastUsedAt", profile.lastUsedAt)
                        put("isDefault", profile.isDefault)
                        put("isFavorite", profile.isFavorite)
                        put("usageCount", profile.usageCount)
                        put("tags", JSONArray(profile.tags))
                        put("color", profile.color)
                        put("settings", JSONObject().apply {
                            put("sensitivity", profile.settings.sensitivity)
                            put("smoothingEnabled", profile.settings.smoothingEnabled)
                            put("smoothingFactor", profile.settings.smoothingFactor)
                            put("accelerationEnabled", profile.settings.accelerationEnabled)
                            put("accelerationFactor", profile.settings.accelerationFactor)
                            put("invertX", profile.settings.invertX)
                            put("invertY", profile.settings.invertY)
                            put("clickThreshold", profile.settings.clickThreshold)
                            put("doubleClickInterval", profile.settings.doubleClickInterval)
                            put("scrollThreshold", profile.settings.scrollThreshold)
                            put("rightClickTilt", profile.settings.rightClickTilt)
                            put("rightClickDuration", profile.settings.rightClickDuration)
                            put("gestureDebounce", profile.settings.gestureDebounce)
                            put("aiSmoothing", profile.settings.aiSmoothing)
                            put("aiBlendFactor", profile.settings.aiBlendFactor)
                            put("predictiveMovement", profile.settings.predictiveMovement)
                            put("predictionStrength", profile.settings.predictionStrength)
                            put("kalmanEnabled", profile.settings.kalmanEnabled)
                            put("hapticFeedback", profile.settings.hapticFeedback)
                            put("soundEnabled", profile.settings.soundEnabled)
                            put("visualFeedback", profile.settings.visualFeedback)
                            put("theme", profile.settings.theme)
                            put("fontSize", profile.settings.fontSize)
                            put("showDebugInfo", profile.settings.showDebugInfo)
                            put("keepScreenOn", profile.settings.keepScreenOn)
                            put("autoConnect", profile.settings.autoConnect)
                            put("reconnectAttempts", profile.settings.reconnectAttempts)
                            put("connectionTimeout", profile.settings.connectionTimeout)
                            put("useWebSocket", profile.settings.useWebSocket)
                            put("jitterCompensation", profile.settings.jitterCompensation)
                            put("deadband", profile.settings.deadband)
                            put("maxSpeed", profile.settings.maxSpeed)
                            put("minSpeed", profile.settings.minSpeed)
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
                showError("Export failed: ${e.message}")
            }
        }
    }

    fun importProfiles(jsonContent: String) {
        viewModelScope.launch {
            try {
                val jsonArray = JSONArray(jsonContent)
                val newProfiles = mutableListOf<UserProfile>()
                
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    if (json.optBoolean("isDefault", false)) continue // Skip default profile
                    
                    val settings = ProfileSettings(
                        sensitivity = json.getJSONObject("settings").getDouble("sensitivity").toFloat(),
                        smoothingEnabled = json.getJSONObject("settings").optBoolean("smoothingEnabled", true),
                        smoothingFactor = json.getJSONObject("settings").optDouble("smoothingFactor", 0.5).toFloat(),
                        accelerationEnabled = json.getJSONObject("settings").optBoolean("accelerationEnabled", true),
                        accelerationFactor = json.getJSONObject("settings").optDouble("accelerationFactor", 1.5).toFloat(),
                        invertX = json.getJSONObject("settings").optBoolean("invertX", false),
                        invertY = json.getJSONObject("settings").optBoolean("invertY", false),
                        clickThreshold = json.getJSONObject("settings").getDouble("clickThreshold").toFloat(),
                        doubleClickInterval = json.getJSONObject("settings").getLong("doubleClickInterval"),
                        scrollThreshold = json.getJSONObject("settings").getDouble("scrollThreshold").toFloat(),
                        rightClickTilt = json.getJSONObject("settings").getDouble("rightClickTilt").toFloat(),
                        rightClickDuration = json.getJSONObject("settings").optLong("rightClickDuration", 500L),
                        gestureDebounce = json.getJSONObject("settings").optLong("gestureDebounce", 100L),
                        aiSmoothing = json.getJSONObject("settings").optBoolean("aiSmoothing", false),
                        aiBlendFactor = json.getJSONObject("settings").optDouble("aiBlendFactor", 0.7).toFloat(),
                        predictiveMovement = json.getJSONObject("settings").optBoolean("predictiveMovement", true),
                        predictionStrength = json.getJSONObject("settings").optDouble("predictionStrength", 0.5).toFloat(),
                        kalmanEnabled = json.getJSONObject("settings").optBoolean("kalmanEnabled", true),
                        hapticFeedback = json.getJSONObject("settings").optBoolean("hapticFeedback", true),
                        soundEnabled = json.getJSONObject("settings").optBoolean("soundEnabled", false),
                        visualFeedback = json.getJSONObject("settings").optBoolean("visualFeedback", true),
                        theme = json.getJSONObject("settings").optString("theme", "system"),
                        fontSize = json.getJSONObject("settings").optDouble("fontSize", 16.0).toFloat(),
                        showDebugInfo = json.getJSONObject("settings").optBoolean("showDebugInfo", false),
                        keepScreenOn = json.getJSONObject("settings").optBoolean("keepScreenOn", false),
                        autoConnect = json.getJSONObject("settings").optBoolean("autoConnect", true),
                        reconnectAttempts = json.getJSONObject("settings").optInt("reconnectAttempts", 5),
                        connectionTimeout = json.getJSONObject("settings").optInt("connectionTimeout", 5000),
                        useWebSocket = json.getJSONObject("settings").optBoolean("useWebSocket", true),
                        jitterCompensation = json.getJSONObject("settings").optBoolean("jitterCompensation", true),
                        deadband = json.getJSONObject("settings").optDouble("deadband", 0.5).toFloat(),
                        maxSpeed = json.getJSONObject("settings").optDouble("maxSpeed", 100.0).toFloat(),
                        minSpeed = json.getJSONObject("settings").optDouble("minSpeed", 0.5).toFloat()
                    )
                    
                    val profile = UserProfile(
                        id = UUID.randomUUID().toString(),
                        name = json.getString("name"),
                        createdAt = System.currentTimeMillis(),
                        isFavorite = json.optBoolean("isFavorite", false),
                        tags = json.optJSONArray("tags")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList(),
                        color = json.optString("color", "#6366F1"),
                        settings = settings
                    )
                    newProfiles.add(profile)
                }
                
                val allProfiles = _uiState.value.profiles + newProfiles
                
                _uiState.update {
                    it.copy(
                        profiles = allProfiles,
                        showImportDialog = false,
                        successMessage = "Imported ${newProfiles.size} profiles"
                    )
                }
                
                saveProfiles(allProfiles)
                clearMessages()
            } catch (e: Exception) {
                showError("Import failed: ${e.message}")
            }
        }
    }

    fun setSortBy(sortBy: ProfileSort) {
        _uiState.update { it.copy(sortBy = sortBy) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleTagFilter(tag: String) {
        val currentTags = _uiState.value.selectedTags
        val newTags = if (currentTags.contains(tag)) {
            currentTags - tag
        } else {
            currentTags + tag
        }
        _uiState.update { it.copy(selectedTags = newTags) }
    }

    fun clearTagFilters() {
        _uiState.update { it.copy(selectedTags = emptyList()) }
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

    fun showDetailsDialog(show: Boolean) {
        _uiState.update { it.copy(showDetailsDialog = show) }
    }

    fun updateSelectedProfileName(name: String) {
        _uiState.value.selectedProfile?.let { profile ->
            val updatedProfile = profile.copy(name = name, updatedAt = System.currentTimeMillis())
            updateProfile(updatedProfile)
        }
    }

    fun resetToDefault() {
        val defaultProfile = _uiState.value.profiles.find { it.isDefault }
        defaultProfile?.let {
            selectProfile(it)
            showSuccess("Reset to default settings")
        }
    }

    private fun showSuccess(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(successMessage = message) }
            delay(3000)
            _uiState.update { it.copy(successMessage = null) }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = message) }
            delay(3000)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun clearMessages() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(errorMessage = null, successMessage = null, exportPath = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
}