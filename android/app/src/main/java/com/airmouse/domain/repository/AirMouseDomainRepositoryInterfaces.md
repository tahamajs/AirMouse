# 📘 Air Mouse Domain Repository Interfaces – Complete Documentation

## 📁 Package Overview

The `com.airmouse.domain.repository` package contains **all repository interfaces** that define the contract between the **domain layer** and the **data layer**. These interfaces abstract away the underlying data sources (Room, SharedPreferences, Network) and provide a clean, consistent API for the domain layer (use cases) to interact with data.

```
com.airmouse.domain.repository/
├── ICalibrationRepository.kt       # Sensor calibration operations
├── IConnectionRepository.kt        # Network connection management
├── IGestureRepository.kt           # Gesture detection and templates
├── IMouseRepository.kt             # Mouse cursor control
├── IProfileRepository.kt           # User profile management
├── IProximityRepository.kt         # Bluetooth proximity detection
├── ISensorRepository.kt            # Real-time sensor data
├── ISettingsRepository.kt          # App settings and preferences
├── IStatisticsRepository.kt        # Usage statistics
├── IUpdateRepository.kt            # App update management
└── IVoiceCommandRepository.kt      # Voice command processing
```

---

## 🎯 1. ICalibrationRepository

### Purpose
Manages **sensor calibration data** for gyroscope, accelerometer, and magnetometer sensors.

### Interface Definition

```kotlin
interface ICalibrationRepository {
    
    // ============================================================
    // Calibration Status & Progress
    // ============================================================
    
    /** Get the current calibration status (NOT_STARTED, IN_PROGRESS, COMPLETED, etc.) */
    suspend fun getCalibrationStatus(): CalibrationStatus
    
    /** Observe calibration status changes (reactive) */
    fun observeCalibrationStatus(): Flow<CalibrationStatus>
    
    /** Get calibration progress percentage (0-100) */
    suspend fun getCalibrationProgress(): Int
    
    /** Observe calibration progress changes (reactive) */
    fun observeCalibrationProgress(): Flow<Int>
    
    /** Observe calibration quality changes (reactive) */
    fun observeCalibrationQuality(): Flow<CalibrationQuality>
    
    // ============================================================
    // Gyroscope Calibration
    // ============================================================
    
    /** Calibrate gyroscope by collecting stationary samples */
    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean
    
    /** Get the current gyroscope bias values */
    suspend fun getGyroBias(): GyroBias
    
    /** Save gyroscope bias values */
    suspend fun saveGyroBias(bias: GyroBias)
    
    // ============================================================
    // Magnetometer Calibration
    // ============================================================
    
    /** Calibrate magnetometer using figure-8 motion */
    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean
    
    /** Get magnetometer offset data */
    suspend fun getMagOffset(): SensorCalibrationData
    
    /** Save magnetometer offset data */
    suspend fun saveMagOffset(data: SensorCalibrationData)
    
    // ============================================================
    // Accelerometer Calibration
    // ============================================================
    
    /** Calibrate accelerometer using 6-position method */
    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Boolean
    
    /** Get accelerometer offset data */
    suspend fun getAccelOffset(): SensorCalibrationData
    
    /** Save accelerometer offset data */
    suspend fun saveAccelOffset(data: SensorCalibrationData)
    
    // ============================================================
    // Full Calibration Data
    // ============================================================
    
    /** Get complete calibration data for all sensors */
    suspend fun getCalibrationData(): CalibrationData
    
    /** Save complete calibration data */
    suspend fun saveCalibrationData(data: CalibrationData)
    
    /** Get the overall calibration quality */
    suspend fun getCalibrationQuality(): CalibrationQuality
    
    // ============================================================
    // Reset Operations
    // ============================================================
    
    /** Reset all calibration data */
    suspend fun resetCalibration()
    
    /** Reset all calibration data (including status and progress) */
    suspend fun resetAllCalibration()
    
    /** Update calibration status (for progress tracking) */
    suspend fun updateCalibrationStatus(status: CalibrationStatus)
    
    /** Update calibration quality */
    suspend fun updateCalibrationQuality(quality: CalibrationQuality)
    
    /** Update calibration progress percentage */
    suspend fun updateCalibrationProgress(progress: Int)
}
```

### Usage Example

```kotlin
// In CalibrationViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationRepo: ICalibrationRepository
) : ViewModel() {
    
    fun startCalibration() {
        viewModelScope.launch {
            val success = calibrationRepo.calibrateGyroscope { progress ->
                _progress.value = progress
            }
            if (success) {
                val data = calibrationRepo.getCalibrationData()
                _calibrationData.value = data
            }
        }
    }
}
```

---

## 🌐 2. IConnectionRepository

### Purpose
Manages **network connections** to the Air Mouse server, including connection, disconnection, reconnection, and server discovery.

### Interface Definition

```kotlin
interface IConnectionRepository {
    
    // ============================================================
    // Connection Management
    // ============================================================
    
    /** Establish a connection to the server using the provided configuration */
    suspend fun connect(config: ConnectionConfig): Boolean
    
    /** Disconnect from the server */
    suspend fun disconnect()
    
    /** Attempt to reconnect to the server */
    suspend fun reconnect(): Boolean
    
    // ============================================================
    // Connection Status & Quality
    // ============================================================
    
    /** Get current connection status */
    suspend fun getConnectionStatus(): ConnectionStatus
    
    /** Observe connection status changes (reactive) */
    fun observeConnectionStatus(): Flow<ConnectionStatus>
    
    /** Get current connection quality metrics */
    suspend fun getConnectionQuality(): ConnectionQuality
    
    /** Observe connection quality changes (reactive) */
    fun observeConnectionQuality(): Flow<ConnectionQuality>
    
    // ============================================================
    // Connection Configuration
    // ============================================================
    
    /** Get the current connection configuration */
    suspend fun getConnectionConfig(): ConnectionConfig
    
    /** Save connection configuration */
    suspend fun saveConnectionConfig(config: ConnectionConfig)
    
    // ============================================================
    // Server Discovery
    // ============================================================
    
    /** Get list of discovered servers */
    suspend fun discoverServers(): List<DiscoveredServer>
    
    /** Start server discovery with callback */
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit)
    
    /** Stop server discovery */
    suspend fun stopDiscovery()
    
    // ============================================================
    // Sending Commands
    // ============================================================
    
    /** Send a raw text message */
    suspend fun sendMessage(message: String): Boolean
    
    /** Send a raw binary message */
    suspend fun sendMessage(message: ByteArray): Boolean
    
    /** Send mouse movement command */
    suspend fun sendMove(dx: Float, dy: Float): Boolean
    
    /** Send click command */
    suspend fun sendClick(button: String): Boolean
    
    /** Send double-click command */
    suspend fun sendDoubleClick(): Boolean
    
    /** Send right-click command */
    suspend fun sendRightClick(): Boolean
    
    /** Send scroll command */
    suspend fun sendScroll(delta: Int): Boolean
    
    /** Send gesture command */
    suspend fun sendGesture(gesture: String, confidence: Float): Boolean
    
    /** Send proximity update */
    suspend fun sendProximity(isNear: Boolean, distance: Float): Boolean
    
    /** Send control command (e.g., lock_screen, play_pause) */
    suspend fun sendControl(command: String): Boolean
    
    /** Send hello identification */
    suspend fun sendHello(name: String, version: String): Boolean
    
    /** Send ping (keep-alive) */
    suspend fun sendPing(): Boolean
    
    /** Send pong (response to ping) */
    suspend fun sendPong(): Boolean
    
    // ============================================================
    // Testing
    // ============================================================
    
    /** Test connection to a specific server */
    suspend fun testConnection(ip: String, port: Int): TestResult
    
    /** Get current ping latency */
    suspend fun ping(): Long
    
    // ============================================================
    // Callbacks
    // ============================================================
    
    /** Set listener for incoming text messages */
    fun setOnMessageListener(listener: (String) -> Unit)
    
    /** Set listener for incoming binary messages */
    fun setOnBinaryMessageListener(listener: (ByteArray) -> Unit)
    
    /** Set listener for disconnection events */
    fun setOnDisconnectedListener(listener: () -> Unit)
    
    /** Set listener for connection events */
    fun setOnConnectedListener(listener: () -> Unit)
}
```

### Usage Example

```kotlin
// In HomeViewModel
class HomeViewModel @Inject constructor(
    private val connectionRepo: IConnectionRepository
) : ViewModel() {
    
    fun connectToServer(ip: String, port: Int) {
        viewModelScope.launch {
            val config = ConnectionConfig(ip = ip, port = port)
            val success = connectionRepo.connect(config)
            if (success) {
                _connectionStatus.value = "Connected"
            }
        }
    }
    
    fun observeConnection() {
        viewModelScope.launch {
            connectionRepo.observeConnectionStatus().collect { status ->
                _status.value = status
            }
        }
    }
}
```

---

## ✋ 3. IGestureRepository

### Purpose
Manages **gesture detection, training, and templates**.

### Interface Definition

```kotlin
interface IGestureRepository {
    
    // ============================================================
    // Gesture Detection
    // ============================================================
    
    /** Detect gesture from raw sensor data */
    suspend fun detectGesture(sensorData: FloatArray): GestureEvent
    
    /** Detect gesture from motion delta values */
    suspend fun detectGestureFromMotion(dx: Float, dy: Float): GestureType
    
    // ============================================================
    // Custom Gesture Management (CRUD)
    // ============================================================
    
    /** Add a new custom gesture template */
    suspend fun addCustomGesture(gesture: CustomGestureTemplate): String
    
    /** Update an existing custom gesture template */
    suspend fun updateCustomGesture(gesture: CustomGestureTemplate)
    
    /** Delete a custom gesture template */
    suspend fun deleteCustomGesture(id: String)
    
    /** Get a specific custom gesture template by ID */
    suspend fun getCustomGesture(id: String): CustomGestureTemplate?
    
    /** Get all custom gesture templates */
    suspend fun getAllCustomGestures(): List<CustomGestureTemplate>
    
    /** Observe custom gestures (reactive) */
    fun observeCustomGestures(): Flow<List<CustomGestureTemplate>>
    
    // ============================================================
    // Gesture Training
    // ============================================================
    
    /** Train a gesture with sample data */
    suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Boolean
    
    /** Train all gestures */
    suspend fun trainAllGestures(): Boolean
    
    // ============================================================
    // Gesture Statistics
    // ============================================================
    
    /** Get gesture training statistics */
    suspend fun getGestureStats(): GestureTrainingStats
    
    /** Observe gesture statistics (reactive) */
    fun observeGestureStats(): Flow<GestureTrainingStats>
    
    // ============================================================
    // Gesture Templates
    // ============================================================
    
    /** Load all gesture templates */
    suspend fun loadGestureTemplates(): List<CustomGestureTemplate>
    
    /** Save a gesture template */
    suspend fun saveGestureTemplate(template: CustomGestureTemplate)
    
    /** Delete a gesture template */
    suspend fun deleteGestureTemplate(id: String)
    
    // ============================================================
    // Configuration
    // ============================================================
    
    /** Set confidence threshold for gesture detection */
    suspend fun setConfidenceThreshold(threshold: Float)
    
    /** Get current confidence threshold */
    suspend fun getConfidenceThreshold(): Float
    
    /** Set cooldown time between gesture detections */
    suspend fun setCooldownMs(cooldown: Long)
    
    /** Get current cooldown time */
    suspend fun getCooldownMs(): Long
    
    // ============================================================
    // Favorites
    // ============================================================
    
    /** Toggle gesture template as favorite */
    suspend fun toggleFavorite(id: String)
    
    /** Get all favorite gesture templates */
    suspend fun getFavoriteGestures(): List<CustomGestureTemplate>
    
    /** Observe favorite gestures (reactive) */
    fun observeFavoriteGestures(): Flow<List<CustomGestureTemplate>>
    
    // ============================================================
    // Utility
    // ============================================================
    
    /** Check if any gesture has been recognized */
    suspend fun isGestureRecognized(): Boolean
    
    /** Get most used gestures (limited by count) */
    suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate>
    
    /** Get total number of gesture templates */
    suspend fun getGestureCount(): Int
    
    /** Get total number of gesture detections */
    suspend fun getTotalDetections(): Int
    
    /** Reset all gesture statistics */
    suspend fun resetStats()
}
```

---

## 🖱️ 4. IMouseRepository

### Purpose
Controls **mouse cursor movement, clicks, scrolls, and gestures**.

### Interface Definition

```kotlin
interface IMouseRepository {
    
    // ============================================================
    // Movement
    // ============================================================
    
    /** Move cursor by delta values */
    suspend fun move(dx: Float, dy: Float): Boolean
    
    /** Smooth movement through multiple points */
    suspend fun moveSmooth(points: List<Pair<Float, Float>>, durationMs: Int): Boolean
    
    /** Pause all cursor movement */
    suspend fun stopMovement()
    
    /** Resume cursor movement */
    suspend fun resumeMovement()
    
    // ============================================================
    // Clicks & Scrolls
    // ============================================================
    
    /** Click a mouse button */
    suspend fun click(button: MouseButton): Boolean
    
    /** Double-click left button */
    suspend fun doubleClick(): Boolean
    
    /** Right-click */
    suspend fun rightClick(): Boolean
    
    /** Middle-click */
    suspend fun middleClick(): Boolean
    
    /** Scroll by delta */
    suspend fun scroll(delta: Int): Boolean
    
    /** Send a gesture command */
    suspend fun sendGesture(gesture: String, confidence: Float): Boolean
    
    // ============================================================
    // Cursor Position
    // ============================================================
    
    /** Get current cursor position */
    suspend fun getPosition(): Pair<Int, Int>
    
    /** Set cursor position */
    suspend fun setPosition(x: Int, y: Int): Boolean
    
    // ============================================================
    // Movement Profile
    // ============================================================
    
    /** Get current movement profile (sensitivity, smoothing, etc.) */
    suspend fun getMovementProfile(): MovementProfile
    
    /** Set movement profile */
    suspend fun setMovementProfile(profile: MovementProfile)
    
    /** Observe movement profile changes (reactive) */
    fun observeMovementProfile(): Flow<MovementProfile>
    
    // ============================================================
    // Statistics
    // ============================================================
    
    /** Get mouse usage statistics */
    suspend fun getStatistics(): MouseStatistics
    
    /** Observe mouse statistics (reactive) */
    fun observeStatistics(): Flow<MouseStatistics>
    
    /** Reset mouse statistics */
    suspend fun resetStatistics()
    
    // ============================================================
    // Events
    // ============================================================
    
    /** Observe mouse events (reactive) */
    fun observeMouseEvents(): Flow<MouseEvent>
    
    /** Clear all mouse events */
    suspend fun clearEvents()
}
```

---

## 👤 5. IProfileRepository

### Purpose
Manages **user profiles and profile-specific settings**.

### Interface Definition

```kotlin
interface IProfileRepository {
    
    // ============================================================
    // CRUD Operations
    // ============================================================
    
    /** Create a new user profile */
    suspend fun createProfile(profile: UserProfile): String
    
    /** Get a profile by ID */
    suspend fun getProfile(id: String): UserProfile?
    
    /** Update an existing profile */
    suspend fun updateProfile(profile: UserProfile)
    
    /** Delete a profile by ID */
    suspend fun deleteProfile(id: String)
    
    /** Get all user profiles */
    suspend fun getAllProfiles(): List<UserProfile>
    
    /** Observe all profiles (reactive) */
    fun observeProfiles(): Flow<List<UserProfile>>
    
    // ============================================================
    // Default Profile
    // ============================================================
    
    /** Get the default profile */
    suspend fun getDefaultProfile(): UserProfile?
    
    /** Set a profile as default */
    suspend fun setDefaultProfile(id: String)
    
    // ============================================================
    // Favorites
    // ============================================================
    
    /** Toggle a profile as favorite */
    suspend fun toggleFavorite(id: String)
    
    /** Get all favorite profiles */
    suspend fun getFavoriteProfiles(): List<UserProfile>
    
    /** Observe favorite profiles (reactive) */
    fun observeFavoriteProfiles(): Flow<List<UserProfile>>
    
    // ============================================================
    // Profile Settings
    // ============================================================
    
    /** Get settings for a specific profile */
    suspend fun getSettings(profileId: String): ProfileSettings?
    
    /** Update settings for a profile */
    suspend fun updateSettings(profileId: String, settings: ProfileSettings)
    
    // ============================================================
    // Search & Export
    // ============================================================
    
    /** Search profiles by name or tags */
    suspend fun searchProfiles(query: String): List<UserProfile>
    
    /** Export profile data as JSON */
    suspend fun exportProfile(id: String): String
    
    /** Import profile data from JSON */
    suspend fun importProfile(json: String): Boolean
    
    // ============================================================
    // Statistics
    // ============================================================
    
    /** Get total number of profiles */
    suspend fun getProfileCount(): Int
    
    /** Get profile usage statistics */
    suspend fun getProfileUsageStats(): Map<String, Int>
}
```

---

## 📱 6. IProximityRepository

### Purpose
Manages **Bluetooth proximity detection** for auto-lock/unlock functionality.

### Interface Definition

```kotlin
interface IProximityRepository {
    
    // ============================================================
    // Proximity State
    // ============================================================
    
    /** Get current proximity state */
    suspend fun getProximityState(): ProximityState
    
    /** Observe proximity state (reactive) */
    fun observeProximityState(): Flow<ProximityState>
    
    // ============================================================
    // Monitoring
    // ============================================================
    
    /** Start proximity monitoring */
    suspend fun startMonitoring()
    
    /** Stop proximity monitoring */
    suspend fun stopMonitoring()
    
    /** Check if monitoring is active */
    suspend fun isMonitoring(): Boolean
    
    // ============================================================
    // Configuration
    // ============================================================
    
    /** Get proximity configuration */
    suspend fun getConfig(): ProximityConfig
    
    /** Update proximity configuration */
    suspend fun updateConfig(config: ProximityConfig)
    
    // ============================================================
    // Calibration
    // ============================================================
    
    /** Run proximity calibration */
    suspend fun calibrate(): Boolean
    
    /** Get calibration status */
    suspend fun getCalibrationStatus(): ProximityCalibrationStatus
    
    /** Reset calibration */
    suspend fun resetCalibration()
    
    /** Get calibration progress */
    suspend fun getCalibrationProgress(): Int
    
    /** Check if calibration is in progress */
    suspend fun isCalibrating(): Boolean
    
    // ============================================================
    // Device Management
    // ============================================================
    
    /** Set paired device address */
    suspend fun setDeviceAddress(address: String)
    
    /** Get paired device address */
    suspend fun getDeviceAddress(): String
    
    /** Get paired device name */
    suspend fun getDeviceName(): String
    
    /** Check if Bluetooth is enabled */
    suspend fun isBluetoothEnabled(): Boolean
    
    // ============================================================
    // Actions
    // ============================================================
    
    /** Lock the PC screen */
    suspend fun lockScreen()
    
    /** Unlock the PC screen */
    suspend fun unlockScreen()
    
    /** Disconnect and stop monitoring */
    suspend fun disconnect()
}
```

---

## 📡 7. ISensorRepository

### Purpose
Provides **real-time sensor data** from the device's sensors.

### Interface Definition

```kotlin
interface ISensorRepository {
    
    // ============================================================
    // Sensor Data
    // ============================================================
    
    /** Observe real-time sensor data (reactive) */
    fun observeSensorData(): Flow<SensorData>
    
    /** Observe orientation data (reactive) */
    fun observeOrientation(): Flow<OrientationData>
    
    /** Get current sensor data snapshot */
    suspend fun getCurrentSensorData(): SensorData
    
    // ============================================================
    // Calibration Status
    // ============================================================
    
    /** Get sensor calibration status */
    suspend fun getCalibrationStatus(): SensorCalibrationStatus
    
    /** Observe calibration status (reactive) */
    fun observeCalibrationStatus(): Flow<SensorCalibrationStatus>
    
    // ============================================================
    // Sensor Control
    // ============================================================
    
    /** Start sensor collection */
    suspend fun startSensors()
    
    /** Stop sensor collection */
    suspend fun stopSensors()
    
    /** Check if sensors are active */
    suspend fun isSensorActive(): Boolean
    
    // ============================================================
    // Sensor Info
    // ============================================================
    
    /** Get list of available sensors */
    suspend fun getSensorInfo(): List<SensorInfo>
    
    // ============================================================
    // Calibration
    // ============================================================
    
    /** Calibrate all sensors */
    suspend fun calibrateSensors(): Boolean
    
    /** Reset all sensor calibration */
    suspend fun resetCalibration()
    
    /** Check if sensors are calibrated */
    suspend fun isCalibrated(): Boolean
    
    // ============================================================
    // Power Management
    // ============================================================
    
    /** Set power save mode */
    suspend fun setPowerSaveMode(enabled: Boolean)
    
    /** Get recommended sensor delay for current power mode */
    suspend fun getRecommendedDelay(): Int
}
```

---

## ⚙️ 8. ISettingsRepository

### Purpose
Manages **all app settings and user preferences** – sensitivity, thresholds, theme, haptic feedback, etc.

### Interface Definition

This is the **largest repository** with over 100 methods. Here are the key categories:

```kotlin
interface ISettingsRepository {
    
    // ============================================================
    // Cursor Settings
    // ============================================================
    
    suspend fun getSensitivity(): Float
    suspend fun setSensitivity(value: Float)
    fun observeSensitivity(): Flow<Float>
    
    suspend fun isSmoothingEnabled(): Boolean
    suspend fun setSmoothingEnabled(enabled: Boolean)
    fun observeSmoothingEnabled(): Flow<Boolean>
    
    suspend fun isAccelerationEnabled(): Boolean
    suspend fun setAccelerationEnabled(enabled: Boolean)
    suspend fun getAccelerationFactor(): Float
    suspend fun setAccelerationFactor(factor: Float)
    
    suspend fun isInvertX(): Boolean
    suspend fun setInvertX(enabled: Boolean)
    suspend fun isInvertY(): Boolean
    suspend fun setInvertY(enabled: Boolean)
    
    suspend fun isSwapAxes(): Boolean
    suspend fun setSwapAxes(enabled: Boolean)
    
    suspend fun getDeadband(): Float
    suspend fun setDeadband(value: Float)
    suspend fun getMaxSpeed(): Float
    suspend fun setMaxSpeed(value: Float)
    suspend fun getMinSpeed(): Float
    suspend fun setMinSpeed(value: Float)
    
    // ============================================================
    // Gesture Settings
    // ============================================================
    
    suspend fun getClickThreshold(): Float
    suspend fun setClickThreshold(threshold: Float)
    suspend fun getDoubleClickInterval(): Long
    suspend fun setDoubleClickInterval(interval: Long)
    suspend fun getScrollThreshold(): Float
    suspend fun setScrollThreshold(threshold: Float)
    suspend fun getRightClickTilt(): Float
    suspend fun setRightClickTilt(tilt: Float)
    suspend fun getRightClickDuration(): Long
    suspend fun setRightClickDuration(duration: Long)
    suspend fun getGestureDebounce(): Long
    suspend fun setGestureDebounce(debounce: Long)
    
    // ============================================================
    // AI Settings
    // ============================================================
    
    suspend fun isAiSmoothingEnabled(): Boolean
    suspend fun setAiSmoothingEnabled(enabled: Boolean)
    suspend fun getAiBlendFactor(): Float
    suspend fun setAiBlendFactor(factor: Float)
    suspend fun isPredictiveEnabled(): Boolean
    suspend fun setPredictiveEnabled(enabled: Boolean)
    suspend fun getPredictionStrength(): Float
    suspend fun setPredictionStrength(strength: Float)
    suspend fun isKalmanEnabled(): Boolean
    suspend fun setKalmanEnabled(enabled: Boolean)
    
    // ============================================================
    // UI & Theme Settings
    // ============================================================
    
    suspend fun getTheme(): String
    suspend fun setTheme(theme: String)
    suspend fun getAvailableThemes(): List<String>
    suspend fun isDynamicColorsEnabled(): Boolean
    suspend fun setDynamicColorsEnabled(enabled: Boolean)
    suspend fun getFontSize(): Float
    suspend fun setFontSize(value: Float)
    suspend fun isDebugInfoEnabled(): Boolean
    suspend fun setDebugInfoEnabled(enabled: Boolean)
    suspend fun isKeepScreenOn(): Boolean
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun isShowFpsEnabled(): Boolean
    suspend fun setShowFpsEnabled(enabled: Boolean)
    
    // ============================================================
    // Haptic & Sound Settings
    // ============================================================
    
    suspend fun isHapticEnabled(): Boolean
    suspend fun setHapticEnabled(enabled: Boolean)
    suspend fun getHapticStrength(): String
    suspend fun setHapticStrength(strength: String)
    suspend fun isSoundEnabled(): Boolean
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun isVisualFeedbackEnabled(): Boolean
    suspend fun setVisualFeedbackEnabled(enabled: Boolean)
    suspend fun isNotificationOnGestureEnabled(): Boolean
    suspend fun setNotificationOnGestureEnabled(enabled: Boolean)
    
    // ============================================================
    // Connection Settings
    // ============================================================
    
    suspend fun isAutoConnect(): Boolean
    suspend fun setAutoConnect(enabled: Boolean)
    suspend fun getReconnectAttempts(): Int
    suspend fun setReconnectAttempts(attempts: Int)
    suspend fun getConnectionTimeout(): Int
    suspend fun setConnectionTimeout(timeout: Int)
    suspend fun isWebSocketEnabled(): Boolean
    suspend fun setWebSocketEnabled(enabled: Boolean)
    suspend fun isUdpDiscoveryEnabled(): Boolean
    suspend fun setUdpDiscoveryEnabled(enabled: Boolean)
    
    // ============================================================
    // Privacy & Analytics
    // ============================================================
    
    suspend fun isAnonymousStatsEnabled(): Boolean
    suspend fun setAnonymousStatsEnabled(enabled: Boolean)
    suspend fun isCrashReportingEnabled(): Boolean
    suspend fun setCrashReportingEnabled(enabled: Boolean)
    suspend fun isClearDataOnExitEnabled(): Boolean
    suspend fun setClearDataOnExitEnabled(enabled: Boolean)
    
    // ============================================================
    // Presentation Settings
    // ============================================================
    
    suspend fun isPresentationModeEnabled(): Boolean
    suspend fun setPresentationModeEnabled(enabled: Boolean)
    suspend fun getLaserPointerSpeed(): Float
    suspend fun setLaserPointerSpeed(speed: Float)
    suspend fun isShowPresentationTimerEnabled(): Boolean
    suspend fun setShowPresentationTimerEnabled(enabled: Boolean)
    suspend fun isAutoHideLaserEnabled(): Boolean
    suspend fun setAutoHideLaserEnabled(enabled: Boolean)
    
    // ============================================================
    // Profile Settings
    // ============================================================
    
    suspend fun getProfileSettings(): ProfileSettings
    suspend fun saveProfileSettings(settings: ProfileSettings)
    fun observeProfileSettings(): Flow<ProfileSettings>
    
    // ============================================================
    // Full Reset
    // ============================================================
    
    suspend fun resetAllSettings()
}
```

---

## 📊 9. IStatisticsRepository

### Purpose
Tracks **usage statistics** – clicks, scrolls, gestures, sessions, etc.

### Interface Definition

```kotlin
interface IStatisticsRepository {
    
    // ============================================================
    // Recording
    // ============================================================
    
    /** Record a click event */
    suspend fun recordClick()
    
    /** Record a double-click event */
    suspend fun recordDoubleClick()
    
    /** Record a right-click event */
    suspend fun recordRightClick()
    
    /** Record a scroll event */
    suspend fun recordScroll(delta: Int)
    
    /** Record a movement event */
    suspend fun recordMovement(distance: Float, duration: Long)
    
    /** Record a gesture detection */
    suspend fun recordGesture(gesture: String, confidence: Float)
    
    /** Record a connection attempt */
    suspend fun recordConnectionAttempt(success: Boolean, latencyMs: Long)
    
    // ============================================================
    // Session Stats
    // ============================================================
    
    /** Get current session statistics */
    suspend fun getCurrentSession(): StatisticsSummary
    
    /** Observe current session statistics (reactive) */
    fun observeCurrentSession(): Flow<StatisticsSummary>
    
    /** Start tracking session */
    suspend fun startTracking()
    
    /** Stop tracking session */
    suspend fun stopTracking()
    
    /** Check if tracking is active */
    suspend fun isTracking(): Boolean
    
    // ============================================================
    // Daily & Historical Stats
    // ============================================================
    
    /** Get daily statistics for a specific date */
    suspend fun getDailyStats(date: String): DailyStats
    
    /** Get today's statistics */
    suspend fun getTodayStats(): DailyStats
    
    /** Get last 7 days statistics */
    suspend fun getWeekStats(): List<DailyStats>
    
    /** Get last 30 days statistics */
    suspend fun getMonthStats(): List<DailyStats>
    
    /** Get historical statistics */
    suspend fun getHistoricalStats(): HistoricalStatistics
    
    /** Observe historical statistics (reactive) */
    fun observeHistoricalStats(): Flow<HistoricalStatistics>
    
    // ============================================================
    // Gesture Stats
    // ============================================================
    
    /** Get gesture statistics */
    suspend fun getGestureStats(): List<GestureStatistics>
    
    /** Observe gesture statistics (reactive) */
    fun observeGestureStats(): Flow<List<GestureStatistics>>
    
    // ============================================================
    // Reset & Export
    // ============================================================
    
    /** Reset all statistics */
    suspend fun resetStats()
    
    /** Reset only current session */
    suspend fun resetSession()
    
    /** Export statistics in specified format (JSON, CSV) */
    suspend fun exportStats(format: String): String
}
```

---

## 🔄 10. IUpdateRepository

### Purpose
Manages **app updates** – checking, downloading, and installing.

### Interface Definition

```kotlin
interface IUpdateRepository {
    
    // ============================================================
    // Check for Updates
    // ============================================================
    
    /** Check for available updates */
    suspend fun checkForUpdates(): UpdateResult
    
    /** Check for updates manually (forces a fresh check) */
    suspend fun checkForUpdatesManually(): UpdateResult
    
    /** Observe update status (reactive) */
    fun observeUpdateStatus(): Flow<UpdateResult>
    
    // ============================================================
    // Download
    // ============================================================
    
    /** Download an update */
    suspend fun downloadUpdate(version: String): Boolean
    
    /** Download with progress callback */
    suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Boolean
    
    /** Observe download progress (reactive) */
    fun observeDownloadProgress(): Flow<UpdateProgress>
    
    // ============================================================
    // Install
    // ============================================================
    
    /** Install a specific version */
    suspend fun installUpdate(version: String): Boolean
    
    /** Install the latest available update */
    suspend fun installUpdate()
    
    // ============================================================
    // Version Info
    // ============================================================
    
    /** Get current app version */
    suspend fun getCurrentVersion(): VersionInfo
    
    /** Get latest available version */
    suspend fun getLatestVersion(): VersionInfo?
    
    /** Get update history */
    suspend fun getUpdateHistory(): List<UpdateInfo>
    
    // ============================================================
    // Status
    // ============================================================
    
    /** Check if update check is in progress */
    suspend fun isChecking(): Boolean
    
    /** Check if download is in progress */
    suspend fun isDownloading(): Boolean
    
    /** Check if installation is in progress */
    suspend fun isInstalling(): Boolean
    
    // ============================================================
    // Cancel
    // ============================================================
    
    /** Cancel ongoing download */
    suspend fun cancelDownload()
    
    /** Cancel ongoing installation */
    suspend fun cancelInstall()
    
    /** Download and install in one operation */
    suspend fun downloadAndInstallUpdate(version: String): Boolean
}
```

---

## 🎙️ 11. IVoiceCommandRepository

### Purpose
Manages **voice command processing**, custom commands, and configuration.

### Interface Definition

```kotlin
interface IVoiceCommandRepository {
    
    // ============================================================
    // Command Management
    // ============================================================
    
    /** Get all voice commands */
    suspend fun getCommands(): List<VoiceCommand>
    
    /** Get a specific command by ID */
    suspend fun getCommand(id: String): VoiceCommand?
    
    /** Add a new voice command */
    suspend fun addCommand(command: VoiceCommand)
    
    /** Update an existing command */
    suspend fun updateCommand(command: VoiceCommand)
    
    /** Delete a command */
    suspend fun deleteCommand(id: String)
    
    /** Enable/disable a command */
    suspend fun toggleCommand(id: String, enabled: Boolean)
    
    /** Observe commands (reactive) */
    fun observeCommands(): Flow<List<VoiceCommand>>
    
    // ============================================================
    // Listening Control
    // ============================================================
    
    /** Start listening for voice commands */
    suspend fun startListening()
    
    /** Stop listening for voice commands */
    suspend fun stopListening()
    
    /** Check if currently listening */
    suspend fun isListening(): Boolean
    
    /** Enable/disable listening */
    suspend fun setListening(enabled: Boolean)
    
    // ============================================================
    // Voice Processing
    // ============================================================
    
    /** Process spoken text and return matching command */
    suspend fun processVoiceInput(text: String): VoiceCommand?
    
    /** Get the last executed command */
    suspend fun getLastCommand(): VoiceCommand?
    
    /** Observe the last command (reactive) */
    fun observeLastCommand(): Flow<VoiceCommand?>
    
    // ============================================================
    // History
    // ============================================================
    
    /** Get command execution history */
    suspend fun getCommandHistory(): List<VoiceCommandHistory>
    
    /** Add an entry to history */
    suspend fun addToHistory(history: VoiceCommandHistory)
    
    /** Clear command history */
    suspend fun clearHistory()
    
    // ============================================================
    // Configuration
    // ============================================================
    
    /** Get voice command configuration */
    suspend fun getConfig(): VoiceCommandConfig
    
    /** Update voice command configuration */
    suspend fun updateConfig(config: VoiceCommandConfig)
    
    /** Get list of supported command phrases */
    suspend fun getSupportedCommands(): List<String>
}
```

---

## 📊 Repository Relationships Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      DOMAIN REPOSITORIES                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     ICalibrationRepository                       │   │
│  │  ├── getCalibrationStatus()                                     │   │
│  │  ├── calibrateGyroscope()                                       │   │
│  │  ├── calibrateMagnetometer()                                    │   │
│  │  ├── calibrateAccelerometer()                                   │   │
│  │  └── getCalibrationData()                                       │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     IConnectionRepository                        │   │
│  │  ├── connect() / disconnect()                                   │   │
│  │  ├── observeConnectionStatus()                                  │   │
│  │  ├── sendMove() / sendClick() / sendScroll()                    │   │
│  │  └── startDiscovery() / stopDiscovery()                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      IGestureRepository                          │   │
│  │  ├── detectGesture()                                            │   │
│  │  ├── addCustomGesture()                                         │   │
│  │  ├── trainGesture()                                             │   │
│  │  └── getGestureStats()                                          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                       IMouseRepository                           │   │
│  │  ├── move() / moveSmooth()                                      │   │
│  │  ├── click() / doubleClick() / rightClick()                     │   │
│  │  ├── scroll()                                                   │   │
│  │  └── getMovementProfile() / setMovementProfile()                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      IProfileRepository                          │   │
│  │  ├── createProfile() / updateProfile() / deleteProfile()        │   │
│  │  ├── getDefaultProfile() / setDefaultProfile()                  │   │
│  │  ├── toggleFavorite()                                           │   │
│  │  └── exportProfile() / importProfile()                          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     IProximityRepository                         │   │
│  │  ├── startMonitoring() / stopMonitoring()                       │   │
│  │  ├── getProximityState()                                        │   │
│  │  ├── calibrate()                                                │   │
│  │  └── lockScreen() / unlockScreen()                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      ISensorRepository                           │   │
│  │  ├── observeSensorData()                                        │   │
│  │  ├── observeOrientation()                                       │   │
│  │  ├── startSensors() / stopSensors()                             │   │
│  │  └── calibrateSensors()                                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     ISettingsRepository                          │   │
│  │  ├── getSensitivity() / setSensitivity()                        │   │
│  │  ├── getTheme() / setTheme()                                    │   │
│  │  ├── isHapticEnabled() / setHapticEnabled()                     │   │
│  │  └── resetAllSettings()                                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    IStatisticsRepository                         │   │
│  │  ├── recordClick() / recordScroll()                             │   │
│  │  ├── getCurrentSession()                                        │   │
│  │  ├── getDailyStats() / getWeekStats()                           │   │
│  │  └── exportStats()                                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      IUpdateRepository                           │   │
│  │  ├── checkForUpdates()                                          │   │
│  │  ├── downloadUpdate()                                           │   │
│  │  ├── installUpdate()                                            │   │
│  │  └── getCurrentVersion()                                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    IVoiceCommandRepository                       │   │
│  │  ├── getCommands() / addCommand()                               │   │
│  │  ├── processVoiceInput()                                        │   │
│  │  ├── startListening() / stopListening()                         │   │
│  │  └── getConfig() / updateConfig()                               │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Repository Summary Table

| Repository | Domain Interface | Primary Data | Reactive Features |
|------------|------------------|--------------|-------------------|
| **Calibration** | `ICalibrationRepository` | Sensor calibration data | `observeCalibrationStatus()`, `observeCalibrationQuality()` |
| **Connection** | `IConnectionRepository` | Network connection state | `observeConnectionStatus()`, `observeConnectionQuality()` |
| **Gesture** | `IGestureRepository` | Gesture templates, stats | `observeCustomGestures()`, `observeGestureStats()` |
| **Mouse** | `IMouseRepository` | Cursor control, statistics | `observeMovementProfile()`, `observeStatistics()` |
| **Profile** | `IProfileRepository` | User profiles | `observeProfiles()`, `observeFavoriteProfiles()` |
| **Proximity** | `IProximityRepository` | Proximity state | `observeProximityState()` |
| **Sensor** | `ISensorRepository` | Real-time sensor data | `observeSensorData()`, `observeOrientation()` |
| **Settings** | `ISettingsRepository` | App preferences | 40+ observe methods for each setting |
| **Statistics** | `IStatisticsRepository` | Usage statistics | `observeCurrentSession()`, `observeHistoricalStats()` |
| **Update** | `IUpdateRepository` | App updates | `observeUpdateStatus()`, `observeDownloadProgress()` |
| **Voice** | `IVoiceCommandRepository` | Voice commands | `observeCommands()`, `observeLastCommand()` |

---

## 🎯 Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Interface Segregation** | Each repository handles one domain (Calibration, Connection, etc.) |
| **Dependency Inversion** | Domain layer depends on interfaces, not implementations |
| **Reactive Programming** | `Flow` and `StateFlow` for real-time updates |
| **Suspend Functions** | All operations are `suspend` for coroutine support |
| **Single Responsibility** | Each method does one thing |
| **Testability** | Interfaces can be easily mocked in unit tests |
| **Clear Naming** | Method names clearly describe their purpose |

---

**These repository interfaces form the contract between the domain and data layers, providing a clean, consistent, and testable API for all data operations in the Air Mouse application.**