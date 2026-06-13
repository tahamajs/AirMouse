// app/src/main/java/com/airmouse/gaming/GameProfilesManager.kt
package com.airmouse.gaming

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * Manages game-specific profiles with custom sensitivity and gesture mappings.
 * Automatically detects the foreground game and applies the corresponding profile.
 */
class GameProfilesManager(private val context: Context, private val prefs: PreferencesManager) {

    companion object {
        private const val TAG = "GameProfilesManager"
        private const val PROFILES_DIR = "game_profiles"
        private const val GAME_DB_FILE = "games_database.json"

        // Permission required for foreground detection on Android 5.0+
        private const val PACKAGE_USAGE_STATS_PERMISSION = "android.permission.PACKAGE_USAGE_STATS"
    }

    data class GameProfile(
        val id: String,
        val gameName: String,
        val packageName: String,
        val enabled: Boolean = true,
        val settings: GameSettings = GameSettings(),
        val gestureMappings: List<GestureMapping> = emptyList(),
        var lastPlayed: Long = System.currentTimeMillis(),
        var playTime: Long = 0,
        val icon: String? = null
    )

    data class GameSettings(
        var sensitivity: Float = 1.0f,
        var accelerationEnabled: Boolean = true,
        var smoothingEnabled: Boolean = false,
        var invertY: Boolean = false,
        var deadzone: Float = 0.1f,
        var aimAssist: Boolean = true,
        var rapidFireEnabled: Boolean = false,
        var rapidFireDelay: Int = 100,
        var customReticle: Boolean = false,
        var vibrationIntensity: Float = 0.5f
    )

    data class GestureMapping(
        val gesture: String,
        val action: GameAction,
        val keyCode: Int? = null,
        val macroId: String? = null,
        val confidence: Float = 0.7f,
        val enabled: Boolean = true
    )

    enum class GameAction(val displayName: String) {
        FIRE("Fire"),
        AIM("Aim Down Sights"),
        RELOAD("Reload"),
        JUMP("Jump"),
        CROUCH("Crouch"),
        SPRINT("Sprint"),
        INTERACT("Interact"),
        NEXT_WEAPON("Next Weapon"),
        PREV_WEAPON("Previous Weapon"),
        GRENADE("Grenade"),
        MELEE("Melee"),
        PAUSE("Pause"),
        MAP("Map"),
        SCOREBOARD("Scoreboard"),
        CUSTOM("Custom")
    }

    private val _currentGame = MutableStateFlow<GameProfile?>(null)
    val currentGame: StateFlow<GameProfile?> = _currentGame.asStateFlow()

    private val _profiles = MutableStateFlow<List<GameProfile>>(emptyList())
    val profiles: StateFlow<List<GameProfile>> = _profiles.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var detectionJob: Job? = null
    private var lastForegroundPackage: String? = null
    private var gameStartTime: Long = 0L

    init {
        loadProfiles()
        startGameDetection()
    }

    /**
     * Loads all profiles from disk into memory.
     */
    private fun loadProfiles() {
        val profilesDir = File(context.filesDir, PROFILES_DIR)
        if (!profilesDir.exists()) {
            profilesDir.mkdirs()
            createDefaultProfiles()
            return
        }

        val profileFiles = profilesDir.listFiles { file -> file.extension == "json" }
        val loadedProfiles = mutableListOf<GameProfile>()

        profileFiles?.forEach { file ->
            try {
                val json = JSONObject(file.readText())
                loadedProfiles.add(parseProfile(json))
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to load profile: ${file.name}", e)
            }
        }

        _profiles.value = loadedProfiles
    }

    private fun createDefaultProfiles() {
        val defaultProfiles = listOf(
            GameProfile(
                id = UUID.randomUUID().toString(),
                gameName = "Call of Duty: Mobile",
                packageName = "com.activision.callofduty.shooter",
                settings = GameSettings(
                    sensitivity = 1.2f,
                    aimAssist = true,
                    rapidFireEnabled = true
                ),
                gestureMappings = listOf(
                    GestureMapping("click", GameAction.FIRE),
                    GestureMapping("double_click", GameAction.RELOAD),
                    GestureMapping("right_click", GameAction.AIM),
                    GestureMapping("swipe_up", GameAction.JUMP),
                    GestureMapping("swipe_down", GameAction.CROUCH),
                    GestureMapping("swipe_left", GameAction.PREV_WEAPON),
                    GestureMapping("swipe_right", GameAction.NEXT_WEAPON)
                )
            ),
            GameProfile(
                id = UUID.randomUUID().toString(),
                gameName = "PUBG Mobile",
                packageName = "com.tencent.ig",
                settings = GameSettings(
                    sensitivity = 0.9f,
                    aimAssist = true
                ),
                gestureMappings = listOf(
                    GestureMapping("click", GameAction.FIRE),
                    GestureMapping("double_click", GameAction.RELOAD),
                    GestureMapping("right_click", GameAction.AIM),
                    GestureMapping("swipe_up", GameAction.JUMP),
                    GestureMapping("swipe_down", GameAction.CROUCH)
                )
            ),
            GameProfile(
                id = UUID.randomUUID().toString(),
                gameName = "Genshin Impact",
                packageName = "com.miHoYo.GenshinImpact",
                settings = GameSettings(
                    sensitivity = 0.8f,
                    smoothingEnabled = true
                ),
                gestureMappings = listOf(
                    GestureMapping("click", GameAction.INTERACT),
                    GestureMapping("double_click", GameAction.JUMP),
                    GestureMapping("swipe_up", GameAction.CUSTOM, macroId = "switch_character")
                )
            )
        )

        defaultProfiles.forEach { saveProfile(it) }
        _profiles.value = defaultProfiles
    }

    private fun parseProfile(json: JSONObject): GameProfile {
        return GameProfile(
            id = json.getString("id"),
            gameName = json.getString("gameName"),
            packageName = json.getString("packageName"),
            enabled = json.optBoolean("enabled", true),
            settings = GameSettings().apply {
                val settingsJson = json.getJSONObject("settings")
                sensitivity = settingsJson.optDouble("sensitivity", 1.0).toFloat()
                accelerationEnabled = settingsJson.optBoolean("accelerationEnabled", true)
                smoothingEnabled = settingsJson.optBoolean("smoothingEnabled", false)
                invertY = settingsJson.optBoolean("invertY", false)
                deadzone = settingsJson.optDouble("deadzone", 0.1).toFloat()
                aimAssist = settingsJson.optBoolean("aimAssist", true)
                rapidFireEnabled = settingsJson.optBoolean("rapidFireEnabled", false)
                rapidFireDelay = settingsJson.optInt("rapidFireDelay", 100)
                customReticle = settingsJson.optBoolean("customReticle", false)
                vibrationIntensity = settingsJson.optDouble("vibrationIntensity", 0.5).toFloat()
            },
            gestureMappings = parseMappings(json.optJSONArray("gestureMappings")),
            lastPlayed = json.optLong("lastPlayed", System.currentTimeMillis()),
            playTime = json.optLong("playTime", 0),
            icon = json.optString("icon", null)
        )
    }

    private fun parseMappings(array: JSONArray?): List<GestureMapping> {
        if (array == null) return emptyList()
        val mappings = mutableListOf<GestureMapping>()
        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)
            mappings.add(
                GestureMapping(
                    gesture = json.getString("gesture"),
                    action = GameAction.valueOf(json.getString("action")),
                    keyCode = if (json.has("keyCode")) json.getInt("keyCode") else null,
                    macroId = json.optString("macroId", null),
                    confidence = json.optDouble("confidence", 0.7).toFloat(),
                    enabled = json.optBoolean("enabled", true)
                )
            )
        }
        return mappings
    }

    /**
     * Saves a single profile to disk and updates the in‑memory list.
     */
    fun saveProfile(profile: GameProfile) {
        val json = JSONObject().apply {
            put("id", profile.id)
            put("gameName", profile.gameName)
            put("packageName", profile.packageName)
            put("enabled", profile.enabled)
            put("settings", JSONObject().apply {
                put("sensitivity", profile.settings.sensitivity)
                put("accelerationEnabled", profile.settings.accelerationEnabled)
                put("smoothingEnabled", profile.settings.smoothingEnabled)
                put("invertY", profile.settings.invertY)
                put("deadzone", profile.settings.deadzone)
                put("aimAssist", profile.settings.aimAssist)
                put("rapidFireEnabled", profile.settings.rapidFireEnabled)
                put("rapidFireDelay", profile.settings.rapidFireDelay)
                put("customReticle", profile.settings.customReticle)
                put("vibrationIntensity", profile.settings.vibrationIntensity)
            })
            put("gestureMappings", JSONArray().apply {
                profile.gestureMappings.forEach { mapping ->
                    put(JSONObject().apply {
                        put("gesture", mapping.gesture)
                        put("action", mapping.action.name)
                        mapping.keyCode?.let { put("keyCode", it) }
                        mapping.macroId?.let { put("macroId", it) }
                        put("confidence", mapping.confidence)
                        put("enabled", mapping.enabled)
                    })
                }
            })
            put("lastPlayed", profile.lastPlayed)
            put("playTime", profile.playTime)
            profile.icon?.let { put("icon", it) }
        }

        val file = File(context.filesDir, "$PROFILES_DIR/${profile.id}.json")
        file.writeText(json.toString())

        // Update memory
        val updatedProfiles = _profiles.value.toMutableList()
        val index = updatedProfiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            updatedProfiles[index] = profile
        } else {
            updatedProfiles.add(profile)
        }
        _profiles.value = updatedProfiles
    }

    /**
     * Deletes a profile by its ID.
     */
    fun deleteProfile(profileId: String) {
        val file = File(context.filesDir, "$PROFILES_DIR/$profileId.json")
        file.delete()
        _profiles.value = _profiles.value.filter { it.id != profileId }
        if (_currentGame.value?.id == profileId) {
            _currentGame.value = null
            resetToDefaultSettings()
        }
    }

    /**
     * Returns the currently active game profile based on the foreground package.
     */
    fun detectCurrentGame(): GameProfile? {
        val foregroundPackage = getForegroundPackage() ?: return null
        return _profiles.value.find { it.packageName == foregroundPackage && it.enabled }
    }

    /**
     * Gets the package name of the currently visible app.
     * Uses UsageStatsManager (requires permission) or falls back to a placeholder.
     */
    private fun getForegroundPackage(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usageStatsManager == null) {
                    android.util.Log.w(TAG, "UsageStatsManager not available")
                    return null
                }
                val endTime = System.currentTimeMillis()
                val beginTime = endTime - 10000 // last 10 seconds
                val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
                if (stats != null) {
                    var recentTask: android.app.usage.UsageStats? = null
                    for (stat in stats) {
                        if (recentTask == null || stat.lastTimeUsed > recentTask.lastTimeUsed) {
                            recentTask = stat
                        }
                    }
                    return recentTask?.packageName
                }
            } catch (e: SecurityException) {
                android.util.Log.e(TAG, "Permission denied for UsageStatsManager. Grant android.permission.PACKAGE_USAGE_STATS")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to get foreground package", e)
            }
        }

        // Fallback – no reliable method without permission
        return null
    }

    /**
     * Starts periodic game detection in a coroutine.
     * Updates play time when a game is running.
     */
    private fun startGameDetection() {
        detectionJob?.cancel()
        detectionJob = scope.launch {
            while (true) {
                val detectedGame = detectCurrentGame()
                val currentPackage = detectedGame?.packageName
                val now = System.currentTimeMillis()

                if (currentPackage != null && currentPackage == lastForegroundPackage) {
                    // Same game still in foreground → accumulate play time
                    val elapsed = now - gameStartTime
                    if (elapsed > 0 && detectedGame != null) {
                        val updatedProfile = detectedGame.copy(
                            playTime = detectedGame.playTime + elapsed,
                            lastPlayed = now
                        )
                        saveProfile(updatedProfile)
                        // Update the state flow if this is the current game
                        if (_currentGame.value?.id == detectedGame.id) {
                            _currentGame.value = updatedProfile
                        }
                    }
                    gameStartTime = now
                } else if (currentPackage != null) {
                    // New game detected
                    gameStartTime = now
                    lastForegroundPackage = currentPackage
                    _currentGame.value = detectedGame
                    if (detectedGame != null) {
                        applyGameSettings(detectedGame)
                    } else {
                        resetToDefaultSettings()
                    }
                } else {
                    // No game detected
                    if (lastForegroundPackage != null) {
                        lastForegroundPackage = null
                        _currentGame.value = null
                        resetToDefaultSettings()
                    }
                }

                delay(2000)
            }
        }
    }

    /**
     * Applies the settings of a game profile to the global preferences.
     */
    private fun applyGameSettings(profile: GameProfile) {
        prefs.putFloat("sensitivity", profile.settings.sensitivity)
        prefs.putBoolean("acceleration_enabled", profile.settings.accelerationEnabled)
        prefs.putBoolean("smoothing_enabled", profile.settings.smoothingEnabled)
        prefs.putBoolean("invert_y", profile.settings.invertY)
        prefs.putFloat("deadzone", profile.settings.deadzone)
        prefs.putBoolean("aim_assist", profile.settings.aimAssist)
        prefs.putBoolean("rapid_fire_enabled", profile.settings.rapidFireEnabled)
        prefs.putInt("rapid_fire_delay", profile.settings.rapidFireDelay)
        prefs.putBoolean("custom_reticle", profile.settings.customReticle)
        prefs.putFloat("vibration_intensity", profile.settings.vibrationIntensity)

        updateGestureMappings(profile.gestureMappings)
        android.util.Log.i(TAG, "Applied game profile: ${profile.gameName}")
    }

    private fun updateGestureMappings(mappings: List<GestureMapping>) {
        val gestureMap = JSONObject()
        mappings.forEach { mapping ->
            if (mapping.enabled) {
                gestureMap.put(mapping.gesture, mapping.action.name)
            }
        }
        prefs.putString("game_gesture_mappings", gestureMap.toString())
    }

    private fun resetToDefaultSettings() {
        prefs.putFloat("sensitivity", 1.0f)
        prefs.putBoolean("acceleration_enabled", true)
        prefs.putBoolean("smoothing_enabled", true)
        prefs.putBoolean("invert_y", false)
        prefs.putFloat("deadzone", 0.1f)
        prefs.putBoolean("aim_assist", true)
        prefs.putBoolean("rapid_fire_enabled", false)
        prefs.putInt("rapid_fire_delay", 100)
        prefs.putBoolean("custom_reticle", false)
        prefs.putFloat("vibration_intensity", 0.5f)
        prefs.putString("game_gesture_mappings", "")
        android.util.Log.i(TAG, "Reset to default settings")
    }

    /**
     * Returns the game action associated with a gesture for the currently active game.
     */
    fun getActionForGesture(gesture: String): GameAction? {
        return _currentGame.value?.gestureMappings?.find { it.gesture == gesture && it.enabled }?.action
    }

    /**
     * Exports all profiles to a single JSON file.
     */
    fun exportProfiles(): File {
        val exportFile = File(context.getExternalFilesDir(null), "game_profiles_export.json")
        val jsonArray = JSONArray()
        _profiles.value.forEach { profile ->
            jsonArray.put(JSONObject().apply {
                put("id", profile.id)
                put("gameName", profile.gameName)
                put("packageName", profile.packageName)
                put("settings", JSONObject().apply {
                    put("sensitivity", profile.settings.sensitivity)
                    put("aimAssist", profile.settings.aimAssist)
                })
            })
        }
        exportFile.writeText(jsonArray.toString())
        return exportFile
    }

    /**
     * Imports profiles from a JSON file.
     */
    fun importProfiles(file: File) {
        try {
            val jsonArray = JSONArray(file.readText())
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val profile = GameProfile(
                    id = json.getString("id"),
                    gameName = json.getString("gameName"),
                    packageName = json.getString("packageName"),
                    settings = GameSettings().apply {
                        sensitivity = json.getJSONObject("settings").optDouble("sensitivity", 1.0).toFloat()
                        aimAssist = json.getJSONObject("settings").optBoolean("aimAssist", true)
                    }
                )
                saveProfile(profile)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to import profiles", e)
        }
    }

    /**
     * Stops the detection coroutine – call when the service is destroyed.
     */
    fun stopDetection() {
        detectionJob?.cancel()
        scope.cancel()
    }
}