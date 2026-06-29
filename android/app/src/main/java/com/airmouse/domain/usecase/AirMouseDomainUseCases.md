# 📘 Air Mouse Domain Use Cases – Complete Documentation

## 📁 Package Overview

The `com.airmouse.domain.usecase` package contains **all use cases** that encapsulate the **business logic** of the Air Mouse application. Each use case follows the **single responsibility principle** – it does exactly one thing and does it well. Use cases orchestrate repository calls and represent the application's **functional requirements**.

```
com.airmouse.domain.usecase/
├── CalibrationUseCase.kt              # Sensor calibration orchestration
├── ConnectToServerUseCase.kt          # Establish server connection
├── SendMovementUseCase.kt             # Send cursor movement
├── DetectGestureUseCase.kt            # Detect gestures from sensor data
├── DiscoverServersUseCase.kt          # Discover servers via UDP
├── GetConnectionStatusUseCase.kt      # Get connection status
├── GetStatisticsUseCase.kt            # Get usage statistics
├── RecordStatisticsUseCase.kt         # Record usage statistics
├── ManageProfileUseCase.kt            # User profile management
├── ManageGestureTemplatesUseCase.kt   # Gesture template management
├── HandleVoiceCommandUseCase.kt       # Voice command processing
├── GetProximityStateUseCase.kt        # Get proximity state
├── UpdateProximityConfigUseCase.kt    # Update proximity configuration
├── TestConnectionUseCase.kt           # Test server connection
├── CheckForUpdatesUseCase.kt          # Check for app updates
└── GetGestureStatisticsUseCase.kt    # Get gesture statistics
```

---

## 🎯 Use Case Architecture

### Use Case Pattern

Each use case follows a consistent pattern:

```kotlin
class XxxUseCase @Inject constructor(
    private val repository: IXxxRepository
) {
    
    suspend operator fun invoke(params: Params): Result {
        return try {
            val result = repository.doSomething(params)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Additional helper methods if needed
}
```

### Why Use Cases?

| Benefit | Description |
|---------|-------------|
| **Single Responsibility** | Each use case does one thing |
| **Testability** | Easy to unit test in isolation |
| **Reusability** | Can be used by multiple ViewModels |
| **Separation of Concerns** | Business logic is separate from UI |
| **Clean Architecture** | Domain layer is independent of frameworks |
| **Transaction Management** | Can coordinate multiple repositories |

---

## 📋 1. CalibrationUseCase

### Purpose
Orchestrates **sensor calibration** for gyroscope, magnetometer, and accelerometer sensors.

### Implementation

```kotlin
class CalibrationUseCase @Inject constructor(
    private val calibrationRepository: ICalibrationRepository
) {

    /**
     * Start full calibration for all sensors
     * @param onProgress Progress callback (0-100)
     */
    suspend fun startFullCalibration(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            // 1. Gyroscope calibration (0-33%)
            val gyroSuccess = calibrationRepository.calibrateGyroscope { progress ->
                onProgress(progress / 3)
            }
            if (!gyroSuccess) {
                return Result.failure(Exception("Gyroscope calibration failed"))
            }

            // 2. Magnetometer calibration (33-66%)
            val magSuccess = calibrationRepository.calibrateMagnetometer { progress ->
                onProgress(33 + (progress / 3))
            }
            if (!magSuccess) {
                return Result.failure(Exception("Magnetometer calibration failed"))
            }

            // 3. Accelerometer calibration (66-100%)
            val accelSuccess = calibrationRepository.calibrateAccelerometer { instruction ->
                // Instruction callback for UI guidance
            }
            if (!accelSuccess) {
                return Result.failure(Exception("Accelerometer calibration failed"))
            }

            onProgress(100)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calibrate only gyroscope
     */
    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateGyroscope(onProgress)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calibrate only magnetometer
     */
    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateMagnetometer(onProgress)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calibrate only accelerometer
     */
    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateAccelerometer(onInstruction)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current calibration status
     */
    suspend fun getCalibrationStatus(): CalibrationStatus {
        return calibrationRepository.getCalibrationStatus()
    }

    /**
     * Observe calibration status (reactive)
     */
    fun observeCalibrationStatus(): Flow<CalibrationStatus> {
        return calibrationRepository.observeCalibrationStatus()
    }

    /**
     * Get calibration quality
     */
    suspend fun getCalibrationQuality(): CalibrationQuality {
        return calibrationRepository.getCalibrationQuality()
    }

    /**
     * Observe calibration quality (reactive)
     */
    fun observeCalibrationQuality(): Flow<CalibrationQuality> {
        return calibrationRepository.observeCalibrationQuality()
    }

    /**
     * Get full calibration data
     */
    suspend fun getCalibrationData(): CalibrationData {
        return calibrationRepository.getCalibrationData()
    }

    /**
     * Save calibration data
     */
    suspend fun saveCalibrationData(data: CalibrationData) {
        calibrationRepository.saveCalibrationData(data)
    }

    /**
     * Apply calibration data (sets it as active)
     */
    suspend fun applyCalibration(data: CalibrationData) {
        calibrationRepository.saveCalibrationData(data)
        calibrationRepository.updateCalibrationStatus(CalibrationStatus.COMPLETED)
        calibrationRepository.updateCalibrationQuality(data.quality)
        calibrationRepository.updateCalibrationProgress(100)
    }

    /**
     * Reset all calibration data
     */
    suspend fun resetCalibration(): Result<Unit> {
        return try {
            calibrationRepository.resetCalibration()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if device is calibrated
     */
    suspend fun isCalibrated(): Boolean {
        return calibrationRepository.getCalibrationStatus() == CalibrationStatus.COMPLETED
    }

    /**
     * Reset all calibration data (including status)
     */
    suspend fun resetAllCalibration(): Result<Unit> {
        return try {
            calibrationRepository.resetAllCalibration()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if calibration data exists
     */
    suspend fun hasCalibrationData(): Boolean {
        val data = calibrationRepository.getCalibrationData()
        return data.isCalibrated
    }
}
```

### Usage Example

```kotlin
// In CalibrationViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationUseCase: CalibrationUseCase
) : ViewModel() {
    
    fun startCalibration() {
        viewModelScope.launch {
            val result = calibrationUseCase.startFullCalibration { progress ->
                _progress.value = progress
            }
            result.onSuccess {
                _status.value = "Calibration complete!"
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }
}
```

---

## 🌐 2. ConnectToServerUseCase

### Purpose
Manages **server connection** with various protocols and configurations.

### Implementation

```kotlin
class ConnectToServerUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    /**
     * Connect to server with full configuration
     */
    suspend operator fun invoke(config: ConnectionConfig): Result<Boolean> {
        return try {
            val result = connectionRepository.connect(config)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connect to server with simplified parameters
     */
    suspend fun connect(
        ip: String,
        port: Int = ConnectionConfig.DEFAULT_WEBSOCKET_PORT,
        protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET
    ): Result<Boolean> {
        val config = ConnectionConfig(
            ip = ip,
            port = port,
            protocol = protocol
        ).normalized()
        return invoke(config)
    }

    /**
     * Connect to last used server
     */
    suspend fun connectToLastServer(): Result<Boolean> {
        return try {
            val config = connectionRepository.getConnectionConfig()
            val result = connectionRepository.connect(config)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe connection status (reactive)
     */
    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return connectionRepository.observeConnectionStatus()
    }

    /**
     * Get current connection status
     */
    suspend fun getConnectionStatus(): ConnectionStatus {
        return connectionRepository.getConnectionStatus()
    }

    /**
     * Disconnect from server
     */
    suspend fun disconnect() {
        connectionRepository.disconnect()
    }

    /**
     * Reconnect to server
     */
    suspend fun reconnect(): Result<Boolean> {
        return try {
            val result = connectionRepository.reconnect()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if connected
     */
    suspend fun isConnected(): Boolean {
        return connectionRepository.getConnectionStatus() == ConnectionStatus.CONNECTED
    }
}
```

---

## 🖱️ 3. SendMovementUseCase

### Purpose
Sends **cursor movement, clicks, scrolls, and gestures** to the server.

### Implementation

```kotlin
class SendMovementUseCase @Inject constructor(
    private val mouseRepository: IMouseRepository
) {

    /**
     * Send cursor movement delta
     */
    suspend operator fun invoke(dx: Float, dy: Float): Result<Boolean> {
        return try {
            val result = mouseRepository.move(dx, dy)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send smooth movement through multiple points
     */
    suspend fun sendSmoothMovement(
        points: List<Pair<Float, Float>>,
        durationMs: Int = 100
    ): Result<Boolean> {
        return try {
            val result = mouseRepository.moveSmooth(points, durationMs)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send mouse click
     */
    suspend fun sendClick(button: MouseButton = MouseButton.LEFT): Result<Boolean> {
        return try {
            val result = mouseRepository.click(button)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send double click
     */
    suspend fun sendDoubleClick(): Result<Boolean> {
        return try {
            val result = mouseRepository.doubleClick()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send right click
     */
    suspend fun sendRightClick(): Result<Boolean> {
        return try {
            val result = mouseRepository.rightClick()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send scroll
     */
    suspend fun sendScroll(delta: Int): Result<Boolean> {
        return try {
            val result = mouseRepository.scroll(delta)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send gesture command
     */
    suspend fun sendGesture(gesture: String, confidence: Float): Result<Boolean> {
        return try {
            val result = mouseRepository.sendGesture(gesture, confidence)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pause movement
     */
    suspend fun pauseMovement(): Result<Boolean> {
        return try {
            mouseRepository.stopMovement()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume movement
     */
    suspend fun resumeMovement(): Result<Boolean> {
        return try {
            mouseRepository.resumeMovement()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## ✋ 4. DetectGestureUseCase

### Purpose
Detects **gestures from sensor data** and motion deltas.

### Implementation

```kotlin
class DetectGestureUseCase @Inject constructor(
    private val gestureRepository: IGestureRepository
) {
    private var lastGesture: GestureEvent? = null

    /**
     * Detect gesture from raw sensor data
     */
    suspend operator fun invoke(sensorData: FloatArray): Result<GestureEvent> {
        return try {
            val result = gestureRepository.detectGesture(sensorData)
            lastGesture = result
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Detect gesture from motion delta values
     */
    suspend fun detectFromMotion(dx: Float, dy: Float): GestureType {
        return gestureRepository.detectGestureFromMotion(dx, dy)
    }

    /**
     * Get the last detected gesture
     */
    suspend fun getCurrentGesture(): GestureEvent? {
        return lastGesture
    }

    /**
     * Check if any gesture has been recognized
     */
    suspend fun isGestureRecognized(): Boolean {
        return gestureRepository.getGestureStats().totalGestures > 0
    }
}
```

---

## 🔍 5. DiscoverServersUseCase

### Purpose
Discovers **servers on the local network** via UDP broadcast.

### Implementation

```kotlin
class DiscoverServersUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {
    private var discovering = false

    /**
     * Get list of discovered servers
     */
    suspend operator fun invoke(): List<DiscoveredServer> {
        return connectionRepository.discoverServers()
    }

    /**
     * Start server discovery
     */
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit): Result<Unit> {
        return try {
            discovering = true
            connectionRepository.startDiscovery { server ->
                onServerFound(server)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            discovering = false
            Result.failure(e)
        }
    }

    /**
     * Stop server discovery
     */
    suspend fun stopDiscovery(): Result<Unit> {
        return try {
            connectionRepository.stopDiscovery()
            discovering = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get discovered servers list
     */
    suspend fun getDiscoveredServers(): List<DiscoveredServer> {
        return connectionRepository.discoverServers()
    }

    /**
     * Check if discovery is active
     */
    suspend fun isDiscovering(): Boolean {
        return discovering
    }
}
```

---

## 📊 6. GetConnectionStatusUseCase

### Purpose
Retrieves **connection status and quality**.

### Implementation

```kotlin
class GetConnectionStatusUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    /**
     * Get current connection status
     */
    suspend operator fun invoke(): ConnectionStatus {
        return connectionRepository.getConnectionStatus()
    }

    /**
     * Observe connection status (reactive)
     */
    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return connectionRepository.observeConnectionStatus()
    }

    /**
     * Check if connected
     */
    suspend fun isConnected(): Boolean {
        return connectionRepository.getConnectionStatus() == ConnectionStatus.CONNECTED
    }

    /**
     * Get connection quality
     */
    suspend fun getConnectionQuality(): ConnectionQuality {
        return connectionRepository.getConnectionQuality()
    }

    /**
     * Observe connection quality (reactive)
     */
    fun observeConnectionQuality(): Flow<ConnectionQuality> {
        return connectionRepository.observeConnectionQuality()
    }

    /**
     * Get connection configuration
     */
    suspend fun getConnectionConfig(): ConnectionConfig {
        return connectionRepository.getConnectionConfig()
    }
}
```

---

## 📈 7. GetStatisticsUseCase

### Purpose
Retrieves **usage statistics** for display and analysis.

### Implementation

```kotlin
class GetStatisticsUseCase @Inject constructor(
    private val statisticsRepository: IStatisticsRepository
) {

    /**
     * Get current session statistics
     */
    suspend operator fun invoke(): StatisticsSummary {
        return statisticsRepository.getCurrentSession()
    }

    /**
     * Observe session statistics (reactive)
     */
    fun observeSessionStats(): Flow<StatisticsSummary> {
        return statisticsRepository.observeCurrentSession()
    }

    /**
     * Get session statistics
     */
    suspend fun getSessionStats(): StatisticsSummary {
        return statisticsRepository.getCurrentSession()
    }

    /**
     * Get historical statistics
     */
    suspend fun getHistoricalStats(): HistoricalStatistics {
        return statisticsRepository.getHistoricalStats()
    }

    /**
     * Get today's statistics
     */
    suspend fun getTodayStats(): DailyStats {
        return statisticsRepository.getTodayStats()
    }

    /**
     * Get last 7 days statistics
     */
    suspend fun getWeekStats(): List<DailyStats> {
        return statisticsRepository.getWeekStats()
    }

    /**
     * Get last 30 days statistics
     */
    suspend fun getMonthStats(): List<DailyStats> {
        return statisticsRepository.getMonthStats()
    }

    /**
     * Get gesture statistics
     */
    suspend fun getGestureStats(): List<GestureStatistics> {
        return statisticsRepository.getGestureStats()
    }

    /**
     * Check if tracking is active
     */
    suspend fun isTracking(): Boolean {
        return statisticsRepository.isTracking()
    }

    /**
     * Observe gesture statistics (reactive)
     */
    fun observeGestureStats(): Flow<List<GestureStatistics>> {
        return statisticsRepository.observeGestureStats()
    }

    /**
     * Start tracking session
     */
    suspend fun startTracking(): Result<Unit> {
        return try {
            statisticsRepository.startTracking()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop tracking session
     */
    suspend fun stopTracking(): Result<Unit> {
        return try {
            statisticsRepository.stopTracking()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset all statistics
     */
    suspend fun resetStats(): Result<Unit> {
        return try {
            statisticsRepository.resetStats()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export statistics in specified format
     */
    suspend fun exportStats(format: String = "json"): Result<String> {
        return try {
            val result = statisticsRepository.exportStats(format)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 📝 8. RecordStatisticsUseCase

### Purpose
Records **usage statistics** for tracking user behavior.

### Implementation

```kotlin
class RecordStatisticsUseCase @Inject constructor(
    private val statisticsRepository: IStatisticsRepository
) {

    /**
     * Record a statistics event
     */
    suspend operator fun invoke(type: String, data: Any? = null): Result<Unit> {
        return try {
            when (type) {
                "click" -> statisticsRepository.recordClick()
                "double_click" -> statisticsRepository.recordDoubleClick()
                "right_click" -> statisticsRepository.recordRightClick()
                "scroll" -> {
                    val delta = (data as? Int) ?: 0
                    statisticsRepository.recordScroll(delta)
                }
                "movement" -> {
                    val distance = (data as? Float) ?: 0f
                    val duration = (data as? Long) ?: 0L
                    statisticsRepository.recordMovement(distance, duration)
                }
                "gesture" -> {
                    val gesture = data as? String ?: ""
                    statisticsRepository.recordGesture(gesture, 0.9f)
                }
                else -> return Result.failure(Exception("Unknown statistic type"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a click
     */
    suspend fun recordClick(): Result<Unit> {
        return try {
            statisticsRepository.recordClick()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a double click
     */
    suspend fun recordDoubleClick(): Result<Unit> {
        return try {
            statisticsRepository.recordDoubleClick()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a right click
     */
    suspend fun recordRightClick(): Result<Unit> {
        return try {
            statisticsRepository.recordRightClick()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a scroll
     */
    suspend fun recordScroll(delta: Int): Result<Unit> {
        return try {
            statisticsRepository.recordScroll(delta)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a movement
     */
    suspend fun recordMovement(distance: Float, duration: Long): Result<Unit> {
        return try {
            statisticsRepository.recordMovement(distance, duration)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a gesture detection
     */
    suspend fun recordGesture(gesture: String, confidence: Float): Result<Unit> {
        return try {
            statisticsRepository.recordGesture(gesture, confidence)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 👤 9. ManageProfileUseCase

### Purpose
Manages **user profiles** – create, update, delete, export, import.

### Implementation

```kotlin
class ManageProfileUseCase @Inject constructor(
    private val profileRepository: IProfileRepository
) {

    /**
     * Create a new profile
     */
    suspend operator fun invoke(profile: UserProfile): Result<String> {
        return try {
            val id = profileRepository.createProfile(profile)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a profile by ID
     */
    suspend fun getProfile(id: String): UserProfile? {
        return profileRepository.getProfile(id)
    }

    /**
     * Get all profiles
     */
    suspend fun getAllProfiles(): List<UserProfile> {
        return profileRepository.getAllProfiles()
    }

    /**
     * Observe all profiles (reactive)
     */
    fun observeProfiles(): Flow<List<UserProfile>> {
        return profileRepository.observeProfiles()
    }

    /**
     * Update a profile
     */
    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            profileRepository.updateProfile(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a profile
     */
    suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            profileRepository.deleteProfile(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get default profile
     */
    suspend fun getDefaultProfile(): UserProfile? {
        return profileRepository.getDefaultProfile()
    }

    /**
     * Set default profile
     */
    suspend fun setDefaultProfile(id: String): Result<Unit> {
        return try {
            profileRepository.setDefaultProfile(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(id: String): Result<Unit> {
        return try {
            profileRepository.toggleFavorite(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get favorite profiles
     */
    suspend fun getFavoriteProfiles(): List<UserProfile> {
        return profileRepository.getFavoriteProfiles()
    }

    /**
     * Get profile settings
     */
    suspend fun getSettings(profileId: String): ProfileSettings? {
        return profileRepository.getSettings(profileId)
    }

    /**
     * Update profile settings
     */
    suspend fun updateSettings(profileId: String, settings: ProfileSettings): Result<Unit> {
        return try {
            profileRepository.updateSettings(profileId, settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search profiles
     */
    suspend fun searchProfiles(query: String): List<UserProfile> {
        return profileRepository.searchProfiles(query)
    }

    /**
     * Export profile as JSON
     */
    suspend fun exportProfile(id: String): Result<String> {
        return try {
            val json = profileRepository.exportProfile(id)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import profile from JSON
     */
    suspend fun importProfile(json: String): Result<Boolean> {
        return try {
            val result = profileRepository.importProfile(json)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 📱 10. GetProximityStateUseCase

### Purpose
Gets the current **proximity state** for auto-lock/unlock functionality.

### Implementation

```kotlin
class GetProximityStateUseCase @Inject constructor(
    private val proximityRepository: IProximityRepository
) {

    /**
     * Get current proximity state
     */
    suspend operator fun invoke(): ProximityState {
        return proximityRepository.getProximityState()
    }

    /**
     * Observe proximity state (reactive)
     */
    fun observeProximityState(): Flow<ProximityState> {
        return proximityRepository.observeProximityState()
    }

    /**
     * Check if device is near
     */
    suspend fun isDeviceNear(): Boolean {
        return proximityRepository.getProximityState().isNear
    }

    /**
     * Get current distance
     */
    suspend fun getCurrentDistance(): Float {
        return proximityRepository.getProximityState().distance
    }

    /**
     * Check if proximity is enabled
     */
    suspend fun isProximityEnabled(): Boolean {
        return proximityRepository.getConfig().enabled
    }

    /**
     * Start monitoring
     */
    suspend fun startMonitoring(): Result<Unit> {
        return try {
            proximityRepository.startMonitoring()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop monitoring
     */
    suspend fun stopMonitoring(): Result<Unit> {
        return try {
            proximityRepository.stopMonitoring()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## ⚙️ 11. UpdateProximityConfigUseCase

### Purpose
Updates the **proximity detection configuration**.

### Implementation

```kotlin
class UpdateProximityConfigUseCase @Inject constructor(
    private val proximityRepository: IProximityRepository
) {

    /**
     * Update proximity configuration
     */
    suspend operator fun invoke(config: ProximityConfig): Result<Unit> {
        return try {
            proximityRepository.updateConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set device address
     */
    suspend fun setDeviceAddress(address: String): Result<Unit> {
        return try {
            proximityRepository.setDeviceAddress(address)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update thresholds
     */
    suspend fun updateThresholds(near: Float, far: Float): Result<Unit> {
        return try {
            val config = proximityRepository.getConfig()
            proximityRepository.updateConfig(config.copy(
                nearThreshold = near,
                farThreshold = far
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enable/disable proximity
     */
    suspend fun toggleProximity(enabled: Boolean): Result<Unit> {
        return try {
            val config = proximityRepository.getConfig()
            proximityRepository.updateConfig(config.copy(enabled = enabled))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Run calibration
     */
    suspend fun calibrate(): Result<Boolean> {
        return try {
            val result = proximityRepository.calibrate()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 🧪 12. TestConnectionUseCase

### Purpose
Tests server **connection and latency**.

### Implementation

```kotlin
class TestConnectionUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    /**
     * Test connection to a server
     */
    suspend operator fun invoke(ip: String, port: Int = 8080): Result<TestResult> {
        return try {
            val result = connectionRepository.testConnection(ip, port)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Test current connection
     */
    suspend fun testCurrentConnection(): Result<TestResult> {
        return try {
            val config = connectionRepository.getConnectionConfig()
            val result = connectionRepository.testConnection(config.ip, config.port)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get ping latency
     */
    suspend fun ping(): Result<Long> {
        return try {
            val latency = connectionRepository.ping()
            Result.success(latency)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 🔄 13. CheckForUpdatesUseCase

### Purpose
Checks for **app updates**.

### Implementation

```kotlin
class CheckForUpdatesUseCase @Inject constructor(
    private val updateRepository: IUpdateRepository
) {

    /**
     * Check for updates
     */
    suspend operator fun invoke(): Result<UpdateResult> {
        return try {
            val result = updateRepository.checkForUpdates()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check for updates manually
     */
    suspend fun checkManually(): Result<UpdateResult> {
        return try {
            val result = updateRepository.checkForUpdatesManually()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe update status (reactive)
     */
    fun observeUpdateStatus(): Flow<UpdateResult> {
        return updateRepository.observeUpdateStatus()
    }

    /**
     * Download an update
     */
    suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Result<Boolean> {
        return try {
            val result = updateRepository.downloadUpdate(version) { progress ->
                onProgress(progress)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe download progress (reactive)
     */
    fun observeDownloadProgress(): Flow<UpdateProgress> {
        return updateRepository.observeDownloadProgress()
    }

    /**
     * Install the update
     */
    suspend fun installUpdate(): Result<Unit> {
        return try {
            updateRepository.installUpdate()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current app version
     */
    suspend fun getCurrentVersion(): String {
        return updateRepository.getCurrentVersion().versionName
    }

    /**
     * Get latest available version
     */
    suspend fun getLatestVersion(): String? {
        return updateRepository.getLatestVersion()?.versionName
    }

    /**
     * Check if update is available
     */
    suspend fun isUpdateAvailable(): Boolean {
        val result = updateRepository.checkForUpdates()
        return result.isAvailable
    }

    /**
     * Cancel ongoing download
     */
    suspend fun cancelDownload(): Result<Unit> {
        return try {
            updateRepository.cancelDownload()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 🎙️ 14. HandleVoiceCommandUseCase

### Purpose
Processes **voice commands** and manages voice command configuration.

### Implementation

```kotlin
class HandleVoiceCommandUseCase @Inject constructor(
    private val voiceCommandRepository: IVoiceCommandRepository
) {

    /**
     * Process voice input and execute command
     */
    suspend operator fun invoke(text: String): Result<VoiceCommand?> {
        return try {
            val command = voiceCommandRepository.processVoiceInput(text)
            Result.success(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start listening for voice commands
     */
    suspend fun startListening(): Result<Unit> {
        return try {
            voiceCommandRepository.startListening()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop listening for voice commands
     */
    suspend fun stopListening(): Result<Unit> {
        return try {
            voiceCommandRepository.stopListening()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if listening
     */
    suspend fun isListening(): Boolean {
        return voiceCommandRepository.isListening()
    }

    /**
     * Get all voice commands
     */
    suspend fun getCommands(): List<VoiceCommand> {
        return voiceCommandRepository.getCommands()
    }

    /**
     * Get a specific command
     */
    suspend fun getCommand(id: String): VoiceCommand? {
        return voiceCommandRepository.getCommand(id)
    }

    /**
     * Add a custom command
     */
    suspend fun addCommand(command: VoiceCommand): Result<Unit> {
        return try {
            voiceCommandRepository.addCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update a command
     */
    suspend fun updateCommand(command: VoiceCommand): Result<Unit> {
        return try {
            voiceCommandRepository.updateCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a command
     */
    suspend fun deleteCommand(id: String): Result<Unit> {
        return try {
            voiceCommandRepository.deleteCommand(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle command enabled state
     */
    suspend fun toggleCommand(id: String, enabled: Boolean): Result<Unit> {
        return try {
            voiceCommandRepository.toggleCommand(id, enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get command history
     */
    suspend fun getCommandHistory(): List<VoiceCommandHistory> {
        return voiceCommandRepository.getCommandHistory()
    }

    /**
     * Clear command history
     */
    suspend fun clearHistory(): Result<Unit> {
        return try {
            voiceCommandRepository.clearHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get voice configuration
     */
    suspend fun getConfig(): VoiceCommandConfig {
        return voiceCommandRepository.getConfig()
    }

    /**
     * Update voice configuration
     */
    suspend fun updateConfig(config: VoiceCommandConfig): Result<Unit> {
        return try {
            voiceCommandRepository.updateConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get supported commands
     */
    suspend fun getSupportedCommands(): List<String> {
        return voiceCommandRepository.getSupportedCommands()
    }

    /**
     * Observe commands (reactive)
     */
    fun observeCommands(): Flow<List<VoiceCommand>> {
        return voiceCommandRepository.observeCommands()
    }

    /**
     * Observe last command (reactive)
     */
    fun observeLastCommand(): Flow<VoiceCommand?> {
        return voiceCommandRepository.observeLastCommand()
    }
}
```

---

## 📊 15. GetGestureStatisticsUseCase

### Purpose
Gets **gesture statistics** for training and analysis.

### Implementation

```kotlin
class GetGestureStatisticsUseCase @Inject constructor(
    private val statisticsRepository: IStatisticsRepository
) {

    /**
     * Get gesture statistics
     */
    suspend operator fun invoke(): GestureTrainingStats {
        val gestureStats = statisticsRepository.getGestureStats()
        return GestureTrainingStats(
            totalGestures = gestureStats.sumOf { it.detectionCount },
            customGestureUsage = gestureStats.associate { it.gestureName to it.detectionCount }
        )
    }

    /**
     * Get statistics for a specific gesture
     */
    suspend fun getForGesture(gestureName: String): GestureTrainingStats {
        return statisticsRepository.getGestureStats()
            .firstOrNull { it.gestureName == gestureName }
            ?.let {
                GestureTrainingStats(
                    totalGestures = it.detectionCount,
                    gesturesByType = mapOf(GestureType.CUSTOM to it.detectionCount),
                    mostUsedGesture = it.gestureName,
                    lastGestureTime = it.lastDetected,
                    customGestureUsage = mapOf(it.gestureName to it.detectionCount),
                    averageConfidence = it.confidencePercentage
                )
            } ?: GestureTrainingStats()
    }
}
```

---

## 📋 Use Case Summary Table

| Use Case | Repository | Purpose |
|----------|------------|---------|
| `CalibrationUseCase` | `ICalibrationRepository` | Sensor calibration orchestration |
| `ConnectToServerUseCase` | `IConnectionRepository` | Server connection management |
| `SendMovementUseCase` | `IMouseRepository` | Cursor movement and clicks |
| `DetectGestureUseCase` | `IGestureRepository` | Gesture detection |
| `DiscoverServersUseCase` | `IConnectionRepository` | UDP server discovery |
| `GetConnectionStatusUseCase` | `IConnectionRepository` | Connection status retrieval |
| `GetStatisticsUseCase` | `IStatisticsRepository` | Usage statistics retrieval |
| `RecordStatisticsUseCase` | `IStatisticsRepository` | Usage statistics recording |
| `ManageProfileUseCase` | `IProfileRepository` | User profile management |
| `ManageGestureTemplatesUseCase` | `IGestureRepository` | Gesture template management |
| `HandleVoiceCommandUseCase` | `IVoiceCommandRepository` | Voice command processing |
| `GetProximityStateUseCase` | `IProximityRepository` | Proximity state retrieval |
| `UpdateProximityConfigUseCase` | `IProximityRepository` | Proximity configuration |
| `TestConnectionUseCase` | `IConnectionRepository` | Connection testing |
| `CheckForUpdatesUseCase` | `IUpdateRepository` | App update checking |
| `GetGestureStatisticsUseCase` | `IStatisticsRepository` | Gesture statistics retrieval |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Single Responsibility** | Each use case does exactly one thing |
| **Testability** | Use cases are pure Kotlin classes, easy to mock |
| **Reusability** | Use cases can be composed and reused |
| **Separation of Concerns** | UI doesn't know about repositories |
| **Clean Architecture** | Domain layer is independent of frameworks |
| **Error Handling** | All use cases return `Result<T>` |
| **Reactive Support** | `Flow` for reactive data streams |

---

**These use cases form the core business logic of the Air Mouse application, providing a clean, testable, and reusable layer between the UI and data layers.**