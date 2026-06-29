# 📘 Air Mouse Data Repository Layer – Complete Documentation

## 📁 Package Overview

The `com.airmouse.data.repository` package contains **all repository implementations** that bridge the **domain layer** with the **data layer**. Each repository implements a corresponding domain interface and orchestrates data operations across multiple data sources (Room, SharedPreferences, Network).

```
com.airmouse.data.repository/
├── CalibrationRepositoryImpl.kt      # Sensor calibration data
├── ConnectionRepositoryImpl.kt       # Network connection management
├── GestureRepositoryImpl.kt          # Gesture detection & templates
├── MouseRepositoryImpl.kt            # Mouse movement & events
├── ProfileRepositoryImpl.kt          # User profiles & settings
├── ProximityRepositoryImpl.kt        # Bluetooth proximity detection
├── SensorRepositoryImpl.kt           # Real-time sensor data
├── SettingsRepositoryImpl.kt         # App & user preferences
├── StatisticsRepositoryImpl.kt       # Usage statistics
├── UpdateRepositoryImpl.kt           # App updates
└── VoiceCommandRepositoryImpl.kt     # Voice command processing
```

---

## 1. CalibrationRepositoryImpl

### Purpose
Manages **sensor calibration data** for gyroscope, accelerometer, and magnetometer. It coordinates calibration processes, stores calibration parameters, and provides quality assessment.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Gyroscope Calibration** | Collects 500 samples while stationary, calculates bias (average), and saves to preferences. |
| **Accelerometer Calibration** | Performs 6‑point calibration (6 orientations), calculates offset and scale for each axis. |
| **Magnetometer Calibration** | Collects samples during a figure‑8 motion, calculates hard‑iron offset and scale. |
| **Quality Assessment** | Evaluates calibration quality based on variance (Excellent/Good/Fair/Poor). |
| **Persistence** | Saves/loads calibration data via `ICalibrationDataSource`. |

### Dependencies
- `ICalibrationDataSource` – for persistence
- `CalibrationHelper` – for sensor sampling logic
- `PreferencesManager` – for storing calibration status

### Key Methods

```kotlin
// Start full calibration
suspend fun startFullCalibration(onProgress: (Int) -> Unit): Boolean

// Individual sensor calibration
suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean
suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean
suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Boolean

// Get calibration data
suspend fun getCalibrationData(): CalibrationData
suspend fun getCalibrationQuality(): CalibrationQuality

// Reset
suspend fun resetCalibration()
suspend fun resetAllCalibration()
```

### Flow Diagram

```
User Request → ViewModel → CalibrationUseCase → CalibrationRepositoryImpl
                                    ↓
                        CalibrationHelper (collects sensor samples)
                                    ↓
                        ICalibrationDataSource (save to Preferences)
                                    ↓
                    StateFlow updates → UI observes changes
```

---

## 2. ConnectionRepositoryImpl

### Purpose
Manages **network connections** to the Air Mouse server. It wraps `ConnectionManager` and `UdpDiscovery` to provide a unified interface for connection, disconnection, reconnection, and server discovery.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Connect/Disconnect** | Establishes and terminates connections to the server. |
| **Reconnection** | Automatic reconnection with exponential backoff. |
| **Connection Quality** | Monitors ping, RSSI, jitter, and packet loss. |
| **Server Discovery** | Discovers servers on the local network via UDP. |
| **Message Sending** | Sends move, click, scroll, gesture, control commands. |

### Dependencies
- `ConnectionManager` – core network client (WebSocket/TCP/UDP)
- `UdpDiscovery` – for server discovery
- `PreferencesManager` – for storing connection config

### Key Methods

```kotlin
// Connection management
suspend fun connect(config: ConnectionConfig): Boolean
suspend fun disconnect()
suspend fun reconnect(): Boolean
suspend fun isConnected(): Boolean

// Observables
fun observeConnectionStatus(): Flow<ConnectionStatus>
fun observeConnectionQuality(): Flow<ConnectionQuality>

// Server discovery
suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit)
suspend fun stopDiscovery()

// Sending commands
suspend fun sendMove(dx: Float, dy: Float): Boolean
suspend fun sendClick(button: String): Boolean
suspend fun sendDoubleClick(): Boolean
suspend fun sendScroll(delta: Int): Boolean
suspend fun sendGesture(gesture: String, confidence: Float): Boolean
suspend fun sendControl(command: String): Boolean
```

### Flow Diagram

```
User Action → ViewModel → ConnectToServerUseCase → ConnectionRepositoryImpl
                                    ↓
                        ConnectionManager (WebSocket/TCP/UDP)
                                    ↓
                        Server (Go) → ACK/Response
                                    ↓
                    StateFlow updates → UI observes
```

---

## 3. GestureRepositoryImpl

### Purpose
Manages **gesture detection, training, and templates**. It processes raw sensor data to detect gestures, stores custom gesture templates, and tracks gesture statistics.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Gesture Detection** | Converts sensor data into gesture events using `EnhancedGestureDetector`. |
| **Gesture from Motion** | Detects gestures from motion deltas (dx, dy). |
| **Custom Gestures** | CRUD operations for user-defined gesture templates. |
| **Gesture Training** | Trains ML models with sample data. |
| **Favorites & Stats** | Tracks gesture usage, favorites, and detection stats. |

### Dependencies
- `EnhancedGestureDetector` – for gesture detection logic
- `IGestureDataSource` – for persistence (templates, stats)
- `PreferencesManager` – for settings

### Key Methods

```kotlin
// Detection
suspend fun detectGesture(sensorData: FloatArray): GestureEvent
suspend fun detectGestureFromMotion(dx: Float, dy: Float): GestureType

// Custom gestures
suspend fun addCustomGesture(gesture: CustomGestureTemplate): String
suspend fun updateCustomGesture(gesture: CustomGestureTemplate)
suspend fun deleteCustomGesture(id: String)
suspend fun getAllCustomGestures(): List<CustomGestureTemplate>

// Training
suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Boolean

// Stats & Favorites
suspend fun toggleFavorite(id: String)
suspend fun getGestureStats(): GestureTrainingStats
suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate>
```

### Flow Diagram

```
Sensor Data → EnhancedGestureDetector → GestureEvent
                                    ↓
                        GestureRepositoryImpl (process/record)
                                    ↓
                        IGestureDataSource (save to Room)
                                    ↓
                    StateFlow updates → UI observes
```

---

## 4. MouseRepositoryImpl

### Purpose
Controls **mouse cursor movement, clicks, scrolls, and gestures**. It applies movement profiles (sensitivity, smoothing, acceleration) and tracks usage statistics.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Movement** | Sends smooth cursor movement with deadband, inversion, and acceleration. |
| **Clicks** | Left, right, middle, double clicks. |
| **Scroll** | Scroll up/down with configurable delta. |
| **Gestures** | Sends gesture commands to the server. |
| **Statistics** | Tracks clicks, scrolls, movement distance, speed. |
| **Profile** | Manages movement profile (sensitivity, smoothing, etc.). |

### Dependencies
- `ConnectionManager` – for sending mouse commands
- `PreferencesManager` – for storing profile and stats

### Key Methods

```kotlin
// Movement
suspend fun move(dx: Float, dy: Float): Boolean
suspend fun moveSmooth(points: List<Pair<Float, Float>>, durationMs: Int): Boolean
suspend fun stopMovement()
suspend fun resumeMovement()

// Clicks & Scrolls
suspend fun click(button: MouseButton): Boolean
suspend fun doubleClick(): Boolean
suspend fun rightClick(): Boolean
suspend fun scroll(delta: Int): Boolean

// Profile & Stats
suspend fun getMovementProfile(): MovementProfile
suspend fun setMovementProfile(profile: MovementProfile)
suspend fun getStatistics(): MouseStatistics
suspend fun resetStatistics()
```

### Flow Diagram

```
User Gesture → ViewModel → SendMovementUseCase → MouseRepositoryImpl
                                    ↓
                        MovementProfile (sensitivity, smoothing)
                                    ↓
                        ConnectionManager (sendMove)
                                    ↓
                        Server → Cursor moves on PC
```

---

## 5. ProfileRepositoryImpl

### Purpose
Manages **user profiles** with custom settings. Supports multiple profiles, default profile selection, favorites, and profile-specific settings.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **CRUD** | Create, read, update, delete user profiles. |
| **Default Profile** | Set/get the default profile. |
| **Favorites** | Toggle and retrieve favorite profiles. |
| **Settings** | Get/update profile-specific settings. |
| **Search** | Search profiles by name or tags. |
| **Import/Export** | Export profile as JSON, import from JSON. |

### Dependencies
- `ProfileDao` (Room) – for persistence
- `PreferencesManager` – for default profile ID

### Key Methods

```kotlin
// CRUD
suspend fun createProfile(profile: UserProfile): String
suspend fun getProfile(id: String): UserProfile?
suspend fun updateProfile(profile: UserProfile)
suspend fun deleteProfile(id: String)
suspend fun getAllProfiles(): List<UserProfile>

// Default & Favorites
suspend fun getDefaultProfile(): UserProfile?
suspend fun setDefaultProfile(id: String)
suspend fun toggleFavorite(id: String)
suspend fun getFavoriteProfiles(): List<UserProfile>

// Settings
suspend fun getSettings(profileId: String): ProfileSettings?
suspend fun updateSettings(profileId: String, settings: ProfileSettings)

// Import/Export
suspend fun exportProfile(id: String): String
suspend fun importProfile(json: String): Boolean
```

### Flow Diagram

```
User Action → ManageProfileUseCase → ProfileRepositoryImpl
                                    ↓
                        ProfileDao (Room) → SQLite
                                    ↓
                    Flow<List<UserProfile>> → UI observes
```

---

## 6. ProximityRepositoryImpl

### Purpose
Detects **proximity to a paired Bluetooth device** and automatically locks/unlocks the PC when the user walks away or returns.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **RSSI Monitoring** | Continuously scans RSSI of paired device. |
| **Distance Estimation** | Converts RSSI to distance using path‑loss model. |
| **Auto‑Lock/Unlock** | Locks PC when far, unlocks when near. |
| **Calibration** | Calibrates RSSI/distance mapping. |
| **Configuration** | Adjustable thresholds, scan interval, haptics. |

### Dependencies
- `BluetoothAdapter` – for RSSI scanning
- `ConnectionManager` – for sending lock/unlock commands

### Key Methods

```kotlin
// Monitoring
suspend fun startMonitoring()
suspend fun stopMonitoring()
suspend fun isMonitoring(): Boolean

// State
suspend fun getProximityState(): ProximityState
fun observeProximityState(): Flow<ProximityState>

// Config
suspend fun getConfig(): ProximityConfig
suspend fun updateConfig(config: ProximityConfig)

// Calibration
suspend fun calibrate(): Boolean
suspend fun getCalibrationStatus(): ProximityCalibrationStatus

// Actions
suspend fun lockScreen()
suspend fun unlockScreen()
```

### Flow Diagram

```
Bluetooth RSSI → ProximityRepositoryImpl
                    ↓
            Distance calculation (RSSI → meters)
                    ↓
            Check thresholds (near/far)
                    ↓
            sendLockScreen() / sendUnlockScreen()
                    ↓
            ConnectionManager → Server → PC Lock/Unlock
```

---

## 7. SensorRepositoryImpl

### Purpose
Provides **real‑time sensor data** from the device's gyroscope, accelerometer, and magnetometer.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Sensor Registration** | Registers listeners for all sensors. |
| **Data Streaming** | Emits `SensorData` via Flow. |
| **Orientation** | Provides orientation data (roll, pitch, yaw). |
| **Power Save** | Adjusts sensor delay based on battery mode. |
| **Calibration Status** | Tracks whether sensors are calibrated. |

### Dependencies
- `SensorManager` – Android sensor framework
- `CalibrationHelper` – for sensor calibration

### Key Methods

```kotlin
// Observables
fun observeSensorData(): Flow<SensorData>
fun observeOrientation(): Flow<OrientationData>

// Control
suspend fun startSensors()
suspend fun stopSensors()
suspend fun isSensorActive(): Boolean

// Calibration
suspend fun calibrateSensors(): Boolean
suspend fun isCalibrated(): Boolean

// Power save
suspend fun setPowerSaveMode(enabled: Boolean)
suspend fun getRecommendedDelay(): Int
```

### Flow Diagram

```
SensorManager → SensorEventListener → SensorData
                                    ↓
                        SensorRepositoryImpl (emits Flow)
                                    ↓
                    ViewModel collects → UI updates
```

---

## 8. SettingsRepositoryImpl

### Purpose
Manages all **user preferences and settings** – sensitivity, gesture thresholds, theme, haptic feedback, connection settings, AI settings, etc.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Cursor Settings** | Sensitivity, smoothing, acceleration, inversion. |
| **Gesture Settings** | Click/scroll thresholds, double‑click interval, right‑click tilt. |
| **AI Settings** | AI smoothing, predictive movement, Kalman filter. |
| **UI Settings** | Theme, accent colour, font size, debug info. |
| **Connection Settings** | Auto‑connect, reconnect attempts, timeout. |
| **Privacy Settings** | Anonymous stats, crash reporting. |
| **Presentation Settings** | Presentation mode, laser pointer speed, timer. |

### Dependencies
- `PreferencesManager` – for all preferences

### Key Methods

This repository has **over 100 methods** – here are the key categories:

```kotlin
// Cursor
suspend fun getSensitivity(): Float
suspend fun setSensitivity(value: Float)

// Gesture
suspend fun getClickThreshold(): Float
suspend fun setClickThreshold(threshold: Float)

// AI
suspend fun isAiSmoothingEnabled(): Boolean
suspend fun setAiSmoothingEnabled(enabled: Boolean)

// Theme
suspend fun getTheme(): String
suspend fun setTheme(theme: String)

// Connection
suspend fun isAutoConnect(): Boolean
suspend fun setAutoConnect(enabled: Boolean)

// Reset
suspend fun resetAllSettings()
```

---

## 9. StatisticsRepositoryImpl

### Purpose
Tracks all **usage statistics** – clicks, scrolls, gestures, movements, and session data.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Session Tracking** | Tracks current session duration, clicks, scrolls, distance. |
| **Daily Stats** | Aggregates daily statistics. |
| **Historical Stats** | Maintains historical aggregated data. |
| **Gesture Stats** | Tracks gesture detection counts and confidence. |
| **Export** | Exports stats in JSON/CSV format. |

### Dependencies
- `PreferencesManager` – for statistics storage

### Key Methods

```kotlin
// Recording
suspend fun recordClick()
suspend fun recordDoubleClick()
suspend fun recordRightClick()
suspend fun recordScroll(delta: Int)
suspend fun recordMovement(distance: Float, duration: Long)
suspend fun recordGesture(gesture: String, confidence: Float)

// Retrieval
suspend fun getCurrentSession(): StatisticsSummary
suspend fun getHistoricalStats(): HistoricalStatistics
suspend fun getTodayStats(): DailyStats
suspend fun getWeekStats(): List<DailyStats>

// Observables
fun observeCurrentSession(): Flow<StatisticsSummary>
fun observeHistoricalStats(): Flow<HistoricalStatistics>

// Reset & Export
suspend fun resetStats()
suspend fun exportStats(format: String): String
```

---

## 10. UpdateRepositoryImpl

### Purpose
Checks for app updates, downloads them, and manages the installation process.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Check for Updates** | Compares current version with latest version. |
| **Download** | Downloads the update APK with progress tracking. |
| **Install** | Triggers APK installation. |
| **History** | Tracks update history. |
| **Progress** | Provides download progress via Flow. |

### Dependencies
- `PreferencesManager` – for storing version info and update history

### Key Methods

```kotlin
// Check
suspend fun checkForUpdates(): UpdateResult
suspend fun checkForUpdatesManually(): UpdateResult

// Download
suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Boolean
fun observeDownloadProgress(): Flow<UpdateProgress>

// Install
suspend fun installUpdate(version: String): Boolean

// History
suspend fun getUpdateHistory(): List<UpdateInfo>
suspend fun getCurrentVersion(): VersionInfo
```

---

## 11. VoiceCommandRepositoryImpl

### Purpose
Processes voice commands, manages custom voice commands, and provides speech-to-text integration.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Command Matching** | Matches spoken text to predefined commands. |
| **Custom Commands** | CRUD for user-defined voice commands. |
| **History** | Tracks command history with success/failure. |
| **Configuration** | Wake word, confidence threshold, haptic feedback. |

### Dependencies
- `PreferencesManager` – for storing commands and config

### Key Methods

```kotlin
// Commands
suspend fun getCommands(): List<VoiceCommand>
suspend fun addCommand(command: VoiceCommand)
suspend fun deleteCommand(id: String)
suspend fun toggleCommand(id: String, enabled: Boolean)

// Processing
suspend fun processVoiceInput(text: String): VoiceCommand?

// History
suspend fun getCommandHistory(): List<VoiceCommandHistory>
suspend fun clearHistory()

// Config
suspend fun getConfig(): VoiceCommandConfig
suspend fun updateConfig(config: VoiceCommandConfig)

// Listening
suspend fun startListening()
suspend fun stopListening()
```

---

## 🔗 Repository Dependencies & Injection

### All Repositories Are Injected via Hilt

```kotlin
// In RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCalibrationRepository(
        impl: CalibrationRepositoryImpl
    ): ICalibrationRepository
    
    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl
    ): IConnectionRepository
    
    // ... etc
}
```

### Dependency Graph

```
ViewModel → UseCase → Repository (interface)
                        ↓
                    RepositoryImpl (implementation)
                        ↓
                    DataSource (Room/Preferences/Network)
```

---

## ✅ Summary

| Repository | Domain Interface | Data Sources |
|------------|------------------|--------------|
| `CalibrationRepositoryImpl` | `ICalibrationRepository` | `ICalibrationDataSource`, `CalibrationHelper` |
| `ConnectionRepositoryImpl` | `IConnectionRepository` | `ConnectionManager`, `UdpDiscovery` |
| `GestureRepositoryImpl` | `IGestureRepository` | `IGestureDataSource`, `EnhancedGestureDetector` |
| `MouseRepositoryImpl` | `IMouseRepository` | `ConnectionManager` |
| `ProfileRepositoryImpl` | `IProfileRepository` | `ProfileDao` (Room), `PreferencesManager` |
| `ProximityRepositoryImpl` | `IProximityRepository` | `BluetoothAdapter`, `ConnectionManager` |
| `SensorRepositoryImpl` | `ISensorRepository` | `SensorManager`, `CalibrationHelper` |
| `SettingsRepositoryImpl` | `ISettingsRepository` | `PreferencesManager` |
| `StatisticsRepositoryImpl` | `IStatisticsRepository` | `PreferencesManager` |
| `UpdateRepositoryImpl` | `IUpdateRepository` | `PreferencesManager` |
| `VoiceCommandRepositoryImpl` | `IVoiceCommandRepository` | `PreferencesManager` |

---

**These repositories form the backbone of the Air Mouse data layer, providing a clean, testable, and well‑organized abstraction for all data operations.**