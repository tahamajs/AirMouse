# 📘 Air Mouse Gaming Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.gaming` package provides **game detection and profile management** functionality. It automatically detects when the user launches a game and applies custom settings (sensitivity, gesture mappings, etc.) optimized for that specific game.

```
com.airmouse.gaming/
└── GameProfilesManager.kt          # Game detection and profile management
```

---

## 🎮 GameProfilesManager

### Purpose
Detects foreground games and automatically applies **game-specific profiles** with custom settings and gesture mappings.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Game Detection** | Monitors foreground apps to detect when a game is launched |
| **Profile Management** | CRUD operations for game profiles |
| **Auto-Switch** | Automatically switches settings when a game is detected |
| **Gesture Mappings** | Maps gestures to game-specific actions (Fire, Aim, Reload, etc.) |
| **Usage Tracking** | Tracks play time and last played for each game |
| **Settings Persistence** | Saves and loads profiles from storage |

---

## 📦 Data Models

### GameProfile

```kotlin
data class GameProfile(
    val id: String,                    // Unique identifier
    val gameName: String,              // Display name of the game
    val packageName: String,           // Android package name (e.g., "com.tencent.ig")
    val enabled: Boolean = true,       // Whether this profile is active
    val settings: GameSettings = GameSettings(),  // Game-specific settings
    val gestureMappings: List<GestureMapping> = emptyList(),  // Gesture → Action mappings
    var lastPlayed: Long = System.currentTimeMillis(),  // Last time played
    var playTime: Long = 0,            // Total play time in milliseconds
    val icon: String? = null           // Icon representation (emoji or URL)
)
```

### GameSettings

```kotlin
data class GameSettings(
    var sensitivity: Float = 1.0f,          // Cursor sensitivity multiplier
    var accelerationEnabled: Boolean = true, // Enable acceleration
    var smoothingEnabled: Boolean = false,   // Enable smoothing
    var invertY: Boolean = false,            // Invert Y-axis
    var deadzone: Float = 0.1f,              // Deadzone for movement
    var aimAssist: Boolean = true,           // Enable aim assist
    var rapidFireEnabled: Boolean = false,   // Enable rapid fire
    var rapidFireDelay: Int = 100,           // Rapid fire delay in ms
    var customReticle: Boolean = false,      // Custom reticle
    var vibrationIntensity: Float = 0.5f     // Vibration intensity
)
```

### GestureMapping

```kotlin
data class GestureMapping(
    val gesture: String,          // e.g., "click", "swipe_up", "double_click"
    val action: GameAction,       // e.g., FIRE, AIM, RELOAD
    val keyCode: Int? = null,     // Optional key code for keyboard emulation
    val macroId: String? = null,  // Macro ID for macro execution
    val confidence: Float = 0.7f, // Confidence threshold
    val enabled: Boolean = true   // Whether this mapping is active
)
```

### GameAction

```kotlin
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
```

---

## 🎯 Key Methods

### 1. Game Detection

```kotlin
/**
 * Detect the current foreground game using UsageStatsManager
 * @return The detected GameProfile, or null if no game is running
 */
fun detectCurrentGame(): GameProfile? {
    val foregroundPackage = getForegroundPackage() ?: return null
    return _profiles.value.find { it.packageName == foregroundPackage && it.enabled }
}

/**
 * Get the foreground package using UsageStatsManager
 * Requires PACKAGE_USAGE_STATS permission
 */
private fun getForegroundPackage(): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10000
        val stats = usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }
    return null
}
```

### 2. Profile Management (CRUD)

```kotlin
/**
 * Save a game profile to storage
 */
fun saveProfile(profile: GameProfile) {
    val json = profile.toJson()
    val file = File(context.filesDir, "$PROFILES_DIR/${profile.id}.json")
    file.writeText(json.toString())
    
    // Update in-memory list
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
 * Delete a game profile
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
```

### 3. Game Auto-Detection Loop

```kotlin
private fun startGameDetection() {
    detectionJob = scope.launch {
        while (isActive) {
            val detectedGame = detectCurrentGame()
            val currentPackage = detectedGame?.packageName
            val now = System.currentTimeMillis()

            if (currentPackage != null && currentPackage == lastForegroundPackage) {
                // Game is still in foreground - accumulate play time
                val elapsed = now - gameStartTime
                if (elapsed > 0) {
                    val updatedProfile = detectedGame.copy(
                        playTime = detectedGame.playTime + elapsed,
                        lastPlayed = now
                    )
                    saveProfile(updatedProfile)
                    
                    if (_currentGame.value?.id == detectedGame.id) {
                        _currentGame.value = updatedProfile
                    }
                }
                gameStartTime = now
            } else if (detectedGame != null) {
                // New game detected
                gameStartTime = now
                lastForegroundPackage = currentPackage
                _currentGame.value = detectedGame
                applyGameSettings(detectedGame)
            } else {
                // No game detected - reset
                if (lastForegroundPackage != null) {
                    lastForegroundPackage = null
                    _currentGame.value = null
                    resetToDefaultSettings()
                }
            }

            delay(2000) // Check every 2 seconds
        }
    }
}
```

### 4. Applying Game Settings

```kotlin
private fun applyGameSettings(profile: GameProfile) {
    // Apply settings to PreferencesManager
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

    // Apply gesture mappings
    updateGestureMappings(profile.gestureMappings)
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
}
```

### 5. Gesture Action Lookup

```kotlin
/**
 * Get the action for a gesture based on the current game profile
 * @param gesture The gesture name (e.g., "click", "swipe_up")
 * @return The GameAction, or null if not mapped
 */
fun getActionForGesture(gesture: String): GameAction? {
    val mappings = _currentGame.value?.gestureMappings ?: return null
    return mappings.find { it.gesture == gesture && it.enabled }?.action
}
```

---

## 🔄 Data Flow

### Game Detection & Profile Switching Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     GAME DETECTION FLOW                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. startGameDetection() launched in background                        │
│         │                                                               │
│         ▼                                                               │
│  2. Every 2 seconds, query foreground app                              │
│         │                                                               │
│         ▼                                                               │
│  3. Is foreground app a tracked game?                                  │
│         │                                                               │
│         ├─── YES ───> Apply game profile settings                     │
│         │              └──> Update play time                          │
│         │                                                               │
│         └─── NO ───> Reset to default settings                        │
│                                                                         │
│  4. When game is detected:                                             │
│     ├── Sensitivity changed                                            │
│     ├── Acceleration/Smoothing toggled                                 │
│     ├── Gesture mappings updated                                       │
│     └── Rapid fire settings applied                                    │
│                                                                         │
│  5. When game exits:                                                   │
│     └── All settings reset to defaults                                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Gesture → Action Mapping

```
┌─────────────────────────────────────────────────────────────────────────┐
│                  GESTURE TO ACTION MAPPING                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Gesture Input          →    GameAction                              │
│  ─────────────────────────────────────────────────────────────────     │
│  "click"                →    FIRE                                      │
│  "double_click"         →    RELOAD                                    │
│  "right_click"          →    AIM                                       │
│  "swipe_up"             →    JUMP                                      │
│  "swipe_down"           →    CROUCH                                    │
│  "swipe_left"           →    PREV_WEAPON                               │
│  "swipe_right"          →    NEXT_WEAPON                               │
│  "long_press"           →    INTERACT                                  │
│  "double_tap"           →    GRENADE                                   │
│                                                                         │
│  The mapping is stored per game profile and applied when the game     │
│  is running.                                                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Default Game Profiles

The manager comes with pre-configured profiles for popular games:

### Call of Duty: Mobile
```kotlin
GameProfile(
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
)
```

### PUBG Mobile
```kotlin
GameProfile(
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
)
```

### Genshin Impact
```kotlin
GameProfile(
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
```

---

## 📋 Public API Summary

| Method | Purpose | Return Type |
|--------|---------|-------------|
| `detectCurrentGame()` | Detect the foreground game | `GameProfile?` |
| `saveProfile(profile)` | Save a game profile | `Unit` |
| `deleteProfile(profileId)` | Delete a game profile | `Unit` |
| `getActionForGesture(gesture)` | Get action for gesture | `GameAction?` |
| `stopDetection()` | Stop game detection | `Unit` |
| `profiles` (StateFlow) | All game profiles | `StateFlow<List<GameProfile>>` |
| `currentGame` (StateFlow) | Currently detected game | `StateFlow<GameProfile?>` |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Auto-Detection** | Automatically detects when a game is launched |
| **Auto-Switch** | Automatically applies game-specific settings |
| **CRUD Operations** | Full create, read, update, delete for game profiles |
| **Usage Tracking** | Tracks play time and last played for each game |
| **Gesture Mapping** | Maps gestures to game-specific actions |
| **Settings Persistence** | Profiles saved as JSON files |
| **Reactive Updates** | StateFlow for UI updates |

---

**The GameProfilesManager provides a seamless gaming experience by automatically detecting games and applying optimized settings, making the Air Mouse a powerful gaming companion.**