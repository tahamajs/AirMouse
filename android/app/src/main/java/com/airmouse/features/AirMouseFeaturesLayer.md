# 📘 Air Mouse Features Layer – Complete Documentation

## 📁 Package Overview

The `com.airmouse.features` package contains **feature orchestrators** that combine multiple use cases into cohesive, high-level features. These features serve as the **bridge between the domain layer and the presentation layer**, providing a clean, unified API for ViewModels to consume.

```
com.airmouse.features/
├── ConnectionFeature.kt              # Connection management feature
├── MouseControlFeature.kt            # Mouse control feature
├── CalibrationFeature.kt             # Sensor calibration feature
├── GestureRecognitionFeature.kt      # Gesture detection & training feature
├── ProximityFeature.kt               # Proximity detection feature
├── StatisticsFeature.kt              # Usage statistics feature
├── VoiceFeature.kt                   # Voice command feature
├── ProfileFeature.kt                 # User profile feature
├── UpdateFeature.kt                  # App update feature
└── SensorFeature.kt                  # Sensor data feature
```

---

## 🏗️ Architecture Overview

### Feature Layer Position

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                             │
│                    (Compose Screens / ViewModels)                     │
│                                                                         │
│                         DEPENDS ON                                     │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         FEATURE LAYER  ←── YOU ARE HERE               │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     FEATURE ORCHESTRATORS                        │   │
│  │                                                                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │ Connection  │  │    Mouse    │  │      Calibration        │ │   │
│  │  │  Feature    │  │  Feature    │  │       Feature           │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  │                                                                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │  Gesture    │  │  Proximity  │  │      Statistics         │ │   │
│  │  │  Feature    │  │  Feature    │  │       Feature           │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  │                                                                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │   Voice     │  │   Profile   │  │        Update           │ │   │
│  │  │  Feature    │  │  Feature    │  │       Feature           │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  │                                                                  │   │
│  │  ┌─────────────┐                                                │   │
│  │  │   Sensor    │                                                │   │
│  │  │  Feature    │                                                │   │
│  │  └─────────────┘                                                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│                    DEPENDS ON USE CASES                                │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                                 │
│                        (Use Cases / Models)                            │
└─────────────────────────────────────────────────────────────────────────┘
```

### Key Differences: Features vs Use Cases

| Aspect | Use Cases | Features |
|--------|-----------|----------|
| **Granularity** | Single operation | Multiple operations |
| **Responsibility** | Atomic business logic | Orchestration & coordination |
| **Dependencies** | Repositories | Multiple use cases |
| **Purpose** | Do one thing | Combine related things |
| **API Exposure** | Individual methods | Cohesive API for ViewModels |

---

## 📦 1. ConnectionFeature

### Purpose
Orchestrates **connection management**, including connecting, disconnecting, server discovery, and connection testing.

### Implementation

```kotlin
class ConnectionFeature @Inject constructor(
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val discoverServersUseCase: DiscoverServersUseCase,
    private val getConnectionStatusUseCase: GetConnectionStatusUseCase,
    private val testConnectionUseCase: TestConnectionUseCase
) {

    // ============================================================
    // Connection Management
    // ============================================================

    /**
     * Connect to the server with full configuration
     */
    suspend fun connect(config: ConnectionConfig): Result<Boolean> {
        return connectToServerUseCase(config)
    }

    /**
     * Connect with simplified parameters
     */
    suspend fun connect(
        ip: String,
        port: Int = ConnectionConfig.DEFAULT_WEBSOCKET_PORT,
        protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET
    ): Result<Boolean> {
        val config = ConnectionConfig(ip = ip, port = port, protocol = protocol).normalized()
        return connect(config)
    }

    /**
     * Connect to the last used server
     */
    suspend fun connectToLastServer(): Result<Boolean> {
        return connectToServerUseCase.connectToLastServer()
    }

    /**
     * Disconnect from the server
     */
    suspend fun disconnect() {
        connectToServerUseCase.disconnect()
    }

    /**
     * Reconnect to the server
     */
    suspend fun reconnect(): Result<Boolean> {
        return connectToServerUseCase.reconnect()
    }

    /**
     * Check if connected
     */
    suspend fun isConnected(): Boolean {
        return connectToServerUseCase.isConnected()
    }

    // ============================================================
    // Connection Status
    // ============================================================

    /**
     * Get current connection status
     */
    suspend fun getConnectionStatus(): ConnectionStatus {
        return getConnectionStatusUseCase()
    }

    /**
     * Observe connection status (reactive)
     */
    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return getConnectionStatusUseCase.observeConnectionStatus()
    }

    /**
     * Get connection quality
     */
    suspend fun getConnectionQuality(): ConnectionQuality {
        return getConnectionStatusUseCase.getConnectionQuality()
    }

    /**
     * Observe connection quality (reactive)
     */
    fun observeConnectionQuality(): Flow<ConnectionQuality> {
        return getConnectionStatusUseCase.observeConnectionQuality()
    }

    /**
     * Get connection configuration
     */
    suspend fun getConnectionConfig(): ConnectionConfig {
        return getConnectionStatusUseCase.getConnectionConfig()
    }

    // ============================================================
    // Server Discovery
    // ============================================================

    /**
     * Start server discovery
     */
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit): Result<Unit> {
        return discoverServersUseCase.startDiscovery(onServerFound)
    }

    /**
     * Stop server discovery
     */
    suspend fun stopDiscovery(): Result<Unit> {
        return discoverServersUseCase.stopDiscovery()
    }

    /**
     * Get discovered servers
     */
    suspend fun getDiscoveredServers(): List<DiscoveredServer> {
        return discoverServersUseCase.getDiscoveredServers()
    }

    /**
     * Check if discovery is active
     */
    suspend fun isDiscovering(): Boolean {
        return discoverServersUseCase.isDiscovering()
    }

    // ============================================================
    // Connection Testing
    // ============================================================

    /**
     * Test connection to a server
     */
    suspend fun testConnection(ip: String, port: Int = 8080): Result<TestResult> {
        return testConnectionUseCase(ip, port)
    }

    /**
     * Test current connection
     */
    suspend fun testCurrentConnection(): Result<TestResult> {
        return testConnectionUseCase.testCurrentConnection()
    }

    /**
     * Get ping latency
     */
    suspend fun ping(): Result<Long> {
        return testConnectionUseCase.ping()
    }
}
```

### Usage Example

```kotlin
// In HomeViewModel
class HomeViewModel @Inject constructor(
    private val connectionFeature: ConnectionFeature
) : ViewModel() {
    
    fun connect(ip: String, port: Int) {
        viewModelScope.launch {
            val result = connectionFeature.connect(ip, port)
            result.onSuccess { connected ->
                if (connected) {
                    _status.value = "Connected"
                }
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }
    
    fun observeConnection() {
        viewModelScope.launch {
            connectionFeature.observeConnectionStatus().collect { status ->
                _connectionStatus.value = status
            }
        }
    }
}
```

---

## 🖱️ 2. MouseControlFeature

### Purpose
Orchestrates **mouse control**, including movement, clicks, scrolls, and gesture commands.

### Implementation

```kotlin
class MouseControlFeature @Inject constructor(
    private val sendMovementUseCase: SendMovementUseCase,
    private val mouseRepository: IMouseRepository
) {

    // ============================================================
    // Movement
    // ============================================================

    /**
     * Send cursor movement
     */
    suspend fun move(dx: Float, dy: Float): Result<Boolean> {
        return sendMovementUseCase(dx, dy)
    }

    /**
     * Send smooth movement through multiple points
     */
    suspend fun moveSmooth(
        points: List<Pair<Float, Float>>,
        durationMs: Int = 100
    ): Result<Boolean> {
        return sendMovementUseCase.sendSmoothMovement(points, durationMs)
    }

    /**
     * Pause cursor movement
     */
    suspend fun pauseMovement(): Result<Boolean> {
        return sendMovementUseCase.pauseMovement()
    }

    /**
     * Resume cursor movement
     */
    suspend fun resumeMovement(): Result<Boolean> {
        return sendMovementUseCase.resumeMovement()
    }

    // ============================================================
    // Clicks & Scrolls
    // ============================================================

    /**
     * Send click
     */
    suspend fun click(button: MouseButton = MouseButton.LEFT): Result<Boolean> {
        return sendMovementUseCase.sendClick(button)
    }

    /**
     * Send double click
     */
    suspend fun doubleClick(): Result<Boolean> {
        return sendMovementUseCase.sendDoubleClick()
    }

    /**
     * Send right click
     */
    suspend fun rightClick(): Result<Boolean> {
        return sendMovementUseCase.sendRightClick()
    }

    /**
     * Send scroll
     */
    suspend fun scroll(delta: Int): Result<Boolean> {
        return sendMovementUseCase.sendScroll(delta)
    }

    /**
     * Send gesture command
     */
    suspend fun sendGesture(gesture: String, confidence: Float): Result<Boolean> {
        return sendMovementUseCase.sendGesture(gesture, confidence)
    }

    // ============================================================
    // Movement Profile
    // ============================================================

    /**
     * Get movement profile
     */
    suspend fun getMovementProfile(): MovementProfile {
        return mouseRepository.getMovementProfile()
    }

    /**
     * Set movement profile
     */
    suspend fun setMovementProfile(profile: MovementProfile) {
        mouseRepository.setMovementProfile(profile)
    }

    /**
     * Observe movement profile (reactive)
     */
    fun observeMovementProfile(): Flow<MovementProfile> {
        return mouseRepository.observeMovementProfile()
    }

    // ============================================================
    // Statistics
    // ============================================================

    /**
     * Get mouse statistics
     */
    suspend fun getStatistics(): MouseStatistics {
        return mouseRepository.getStatistics()
    }

    /**
     * Observe mouse statistics (reactive)
     */
    fun observeStatistics(): Flow<MouseStatistics> {
        return mouseRepository.observeStatistics()
    }

    /**
     * Reset mouse statistics
     */
    suspend fun resetStatistics() {
        mouseRepository.resetStatistics()
    }
}
```

---

## 🎯 3. CalibrationFeature

### Purpose
Orchestrates **sensor calibration** for gyroscope, magnetometer, and accelerometer.

### Implementation

```kotlin
class CalibrationFeature @Inject constructor(
    private val calibrationUseCase: CalibrationUseCase
) {

    // ============================================================
    // Full Calibration
    // ============================================================

    /**
     * Start full calibration for all sensors
     */
    suspend fun startFullCalibration(onProgress: (Int) -> Unit): Result<Boolean> {
        return calibrationUseCase.startFullCalibration(onProgress)
    }

    // ============================================================
    // Individual Calibration
    // ============================================================

    /**
     * Calibrate only gyroscope
     */
    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Result<Boolean> {
        return calibrationUseCase.calibrateGyroscope(onProgress)
    }

    /**
     * Calibrate only magnetometer
     */
    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Result<Boolean> {
        return calibrationUseCase.calibrateMagnetometer(onProgress)
    }

    /**
     * Calibrate only accelerometer
     */
    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Result<Boolean> {
        return calibrationUseCase.calibrateAccelerometer(onInstruction)
    }

    // ============================================================
    // Calibration Status
    // ============================================================

    /**
     * Get calibration status
     */
    suspend fun getCalibrationStatus(): CalibrationStatus {
        return calibrationUseCase.getCalibrationStatus()
    }

    /**
     * Observe calibration status (reactive)
     */
    fun observeCalibrationStatus(): Flow<CalibrationStatus> {
        return calibrationUseCase.observeCalibrationStatus()
    }

    /**
     * Get calibration quality
     */
    suspend fun getCalibrationQuality(): CalibrationQuality {
        return calibrationUseCase.getCalibrationQuality()
    }

    /**
     * Observe calibration quality (reactive)
     */
    fun observeCalibrationQuality(): Flow<CalibrationQuality> {
        return calibrationUseCase.observeCalibrationQuality()
    }

    /**
     * Get calibration progress
     */
    suspend fun getCalibrationProgress(): Int {
        return calibrationUseCase.getCalibrationProgress()
    }

    /**
     * Observe calibration progress (reactive)
     */
    fun observeCalibrationProgress(): Flow<Int> {
        return calibrationUseCase.observeCalibrationProgress()
    }

    // ============================================================
    // Calibration Data
    // ============================================================

    /**
     * Get calibration data
     */
    suspend fun getCalibrationData(): CalibrationData {
        return calibrationUseCase.getCalibrationData()
    }

    /**
     * Save calibration data
     */
    suspend fun saveCalibrationData(data: CalibrationData) {
        calibrationUseCase.saveCalibrationData(data)
    }

    /**
     * Apply calibration data
     */
    suspend fun applyCalibration(data: CalibrationData) {
        calibrationUseCase.applyCalibration(data)
    }

    /**
     * Check if calibrated
     */
    suspend fun isCalibrated(): Boolean {
        return calibrationUseCase.isCalibrated()
    }

    // ============================================================
    // Reset
    // ============================================================

    /**
     * Reset calibration
     */
    suspend fun resetCalibration(): Result<Unit> {
        return calibrationUseCase.resetCalibration()
    }

    /**
     * Reset all calibration data
     */
    suspend fun resetAllCalibration(): Result<Unit> {
        return calibrationUseCase.resetAllCalibration()
    }
}
```

---

## ✋ 4. GestureRecognitionFeature

### Purpose
Orchestrates **gesture detection, training, and template management**.

### Implementation

```kotlin
class GestureRecognitionFeature @Inject constructor(
    private val detectGestureUseCase: DetectGestureUseCase,
    private val manageGestureTemplatesUseCase: ManageGestureTemplatesUseCase
) {

    // ============================================================
    // Gesture Detection
    // ============================================================

    /**
     * Detect gesture from sensor data
     */
    suspend fun detectGesture(sensorData: FloatArray): Result<GestureEvent> {
        return detectGestureUseCase(sensorData)
    }

    /**
     * Detect gesture from motion deltas
     */
    suspend fun detectFromMotion(dx: Float, dy: Float): GestureType {
        return detectGestureUseCase.detectFromMotion(dx, dy)
    }

    /**
     * Get current gesture
     */
    suspend fun getCurrentGesture(): GestureEvent? {
        return detectGestureUseCase.getCurrentGesture()
    }

    /**
     * Check if gesture recognized
     */
    suspend fun isGestureRecognized(): Boolean {
        return detectGestureUseCase.isGestureRecognized()
    }

    // ============================================================
    // Template Management
    // ============================================================

    /**
     * Add custom gesture template
     */
    suspend fun addTemplate(template: CustomGestureTemplate): Result<String> {
        return manageGestureTemplatesUseCase(template)
    }

    /**
     * Update template
     */
    suspend fun updateTemplate(template: CustomGestureTemplate): Result<Unit> {
        return manageGestureTemplatesUseCase.updateTemplate(template)
    }

    /**
     * Delete template
     */
    suspend fun deleteTemplate(id: String): Result<Unit> {
        return manageGestureTemplatesUseCase.deleteTemplate(id)
    }

    /**
     * Get all templates
     */
    suspend fun getAllTemplates(): List<CustomGestureTemplate> {
        return manageGestureTemplatesUseCase.getAllTemplates()
    }

    /**
     * Get template by ID
     */
    suspend fun getTemplate(id: String): CustomGestureTemplate? {
        return manageGestureTemplatesUseCase.getTemplate(id)
    }

    /**
     * Observe templates (reactive)
     */
    fun observeTemplates(): Flow<List<CustomGestureTemplate>> {
        return manageGestureTemplatesUseCase.observeTemplates()
    }

    // ============================================================
    // Training
    // ============================================================

    /**
     * Train a gesture
     */
    suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Result<Boolean> {
        return manageGestureTemplatesUseCase.trainTemplate(gestureName, samples)
    }

    // ============================================================
    // Configuration
    // ============================================================

    /**
     * Get confidence threshold
     */
    suspend fun getConfidenceThreshold(): Float {
        return manageGestureTemplatesUseCase.getConfidenceThreshold()
    }

    /**
     * Set confidence threshold
     */
    suspend fun setConfidenceThreshold(threshold: Float) {
        manageGestureTemplatesUseCase.setConfidenceThreshold(threshold)
    }

    /**
     * Get cooldown
     */
    suspend fun getCooldownMs(): Long {
        return manageGestureTemplatesUseCase.getCooldownMs()
    }

    /**
     * Set cooldown
     */
    suspend fun setCooldownMs(cooldown: Long) {
        manageGestureTemplatesUseCase.setCooldownMs(cooldown)
    }

    /**
     * Get gesture stats
     */
    suspend fun getGestureStats(): GestureTrainingStats {
        return manageGestureTemplatesUseCase.getGestureStats()
    }
}
```

---

## 📱 5. ProximityFeature

### Purpose
Orchestrates **proximity detection** for auto-lock/unlock functionality.

### Implementation

```kotlin
class ProximityFeature @Inject constructor(
    private val getProximityStateUseCase: GetProximityStateUseCase,
    private val updateProximityConfigUseCase: UpdateProximityConfigUseCase
) {

    // ============================================================
    // Proximity State
    // ============================================================

    /**
     * Get proximity state
     */
    suspend fun getProximityState(): ProximityState {
        return getProximityStateUseCase()
    }

    /**
     * Observe proximity state (reactive)
     */
    fun observeProximityState(): Flow<ProximityState> {
        return getProximityStateUseCase.observeProximityState()
    }

    /**
     * Check if device is near
     */
    suspend fun isDeviceNear(): Boolean {
        return getProximityStateUseCase.isDeviceNear()
    }

    /**
     * Get current distance
     */
    suspend fun getCurrentDistance(): Float {
        return getProximityStateUseCase.getCurrentDistance()
    }

    // ============================================================
    // Monitoring
    // ============================================================

    /**
     * Start monitoring
     */
    suspend fun startMonitoring(): Result<Unit> {
        return getProximityStateUseCase.startMonitoring()
    }

    /**
     * Stop monitoring
     */
    suspend fun stopMonitoring(): Result<Unit> {
        return getProximityStateUseCase.stopMonitoring()
    }

    // ============================================================
    // Configuration
    // ============================================================

    /**
     * Get proximity configuration
     */
    suspend fun getConfig(): ProximityConfig {
        return updateProximityConfigUseCase.getConfig()
    }

    /**
     * Update proximity configuration
     */
    suspend fun updateConfig(config: ProximityConfig): Result<Unit> {
        return updateProximityConfigUseCase(config)
    }

    /**
     * Set device address
     */
    suspend fun setDeviceAddress(address: String): Result<Unit> {
        return updateProximityConfigUseCase.setDeviceAddress(address)
    }

    /**
     * Update thresholds
     */
    suspend fun updateThresholds(near: Float, far: Float): Result<Unit> {
        return updateProximityConfigUseCase.updateThresholds(near, far)
    }

    /**
     * Toggle proximity
     */
    suspend fun toggleProximity(enabled: Boolean): Result<Unit> {
        return updateProximityConfigUseCase.toggleProximity(enabled)
    }

    // ============================================================
    // Calibration
    // ============================================================

    /**
     * Run calibration
     */
    suspend fun calibrate(): Result<Boolean> {
        return updateProximityConfigUseCase.calibrate()
    }
}
```

---

## 📊 6. StatisticsFeature

### Purpose
Orchestrates **usage statistics** tracking and retrieval.

### Implementation

```kotlin
class StatisticsFeature @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val recordStatisticsUseCase: RecordStatisticsUseCase
) {

    // ============================================================
    // Recording
    // ============================================================

    /**
     * Record a click
     */
    suspend fun recordClick(): Result<Unit> {
        return recordStatisticsUseCase.recordClick()
    }

    /**
     * Record a double click
     */
    suspend fun recordDoubleClick(): Result<Unit> {
        return recordStatisticsUseCase.recordDoubleClick()
    }

    /**
     * Record a right click
     */
    suspend fun recordRightClick(): Result<Unit> {
        return recordStatisticsUseCase.recordRightClick()
    }

    /**
     * Record a scroll
     */
    suspend fun recordScroll(delta: Int): Result<Unit> {
        return recordStatisticsUseCase.recordScroll(delta)
    }

    /**
     * Record a movement
     */
    suspend fun recordMovement(distance: Float, duration: Long): Result<Unit> {
        return recordStatisticsUseCase.recordMovement(distance, duration)
    }

    /**
     * Record a gesture
     */
    suspend fun recordGesture(gesture: String, confidence: Float): Result<Unit> {
        return recordStatisticsUseCase.recordGesture(gesture, confidence)
    }

    // ============================================================
    // Retrieval
    // ============================================================

    /**
     * Get session statistics
     */
    suspend fun getSessionStats(): StatisticsSummary {
        return getStatisticsUseCase.getSessionStats()
    }

    /**
     * Observe session statistics (reactive)
     */
    fun observeSessionStats(): Flow<StatisticsSummary> {
        return getStatisticsUseCase.observeSessionStats()
    }

    /**
     * Get historical statistics
     */
    suspend fun getHistoricalStats(): HistoricalStatistics {
        return getStatisticsUseCase.getHistoricalStats()
    }

    /**
     * Get today's statistics
     */
    suspend fun getTodayStats(): DailyStats {
        return getStatisticsUseCase.getTodayStats()
    }

    /**
     * Get week statistics
     */
    suspend fun getWeekStats(): List<DailyStats> {
        return getStatisticsUseCase.getWeekStats()
    }

    /**
     * Get month statistics
     */
    suspend fun getMonthStats(): List<DailyStats> {
        return getStatisticsUseCase.getMonthStats()
    }

    /**
     * Get gesture statistics
     */
    suspend fun getGestureStats(): List<GestureStatistics> {
        return getStatisticsUseCase.getGestureStats()
    }

    /**
     * Observe gesture statistics (reactive)
     */
    fun observeGestureStats(): Flow<List<GestureStatistics>> {
        return getStatisticsUseCase.observeGestureStats()
    }

    // ============================================================
    // Session Control
    // ============================================================

    /**
     * Start tracking
     */
    suspend fun startTracking(): Result<Unit> {
        return getStatisticsUseCase.startTracking()
    }

    /**
     * Stop tracking
     */
    suspend fun stopTracking(): Result<Unit> {
        return getStatisticsUseCase.stopTracking()
    }

    /**
     * Check if tracking
     */
    suspend fun isTracking(): Boolean {
        return getStatisticsUseCase.isTracking()
    }

    // ============================================================
    // Reset & Export
    // ============================================================

    /**
     * Reset all statistics
     */
    suspend fun resetStats(): Result<Unit> {
        return getStatisticsUseCase.resetStats()
    }

    /**
     * Export statistics
     */
    suspend fun exportStats(format: String = "json"): Result<String> {
        return getStatisticsUseCase.exportStats(format)
    }
}
```

---

## 🎙️ 7. VoiceFeature

### Purpose
Orchestrates **voice command** processing and management.

### Implementation

```kotlin
class VoiceFeature @Inject constructor(
    private val handleVoiceCommandUseCase: HandleVoiceCommandUseCase
) {

    // ============================================================
    // Command Processing
    // ============================================================

    /**
     * Process voice input
     */
    suspend fun processVoiceInput(text: String): Result<VoiceCommand?> {
        return handleVoiceCommandUseCase(text)
    }

    // ============================================================
    // Listening Control
    // ============================================================

    /**
     * Start listening
     */
    suspend fun startListening(): Result<Unit> {
        return handleVoiceCommandUseCase.startListening()
    }

    /**
     * Stop listening
     */
    suspend fun stopListening(): Result<Unit> {
        return handleVoiceCommandUseCase.stopListening()
    }

    /**
     * Check if listening
     */
    suspend fun isListening(): Boolean {
        return handleVoiceCommandUseCase.isListening()
    }

    // ============================================================
    // Command Management
    // ============================================================

    /**
     * Get all commands
     */
    suspend fun getCommands(): List<VoiceCommand> {
        return handleVoiceCommandUseCase.getCommands()
    }

    /**
     * Get command by ID
     */
    suspend fun getCommand(id: String): VoiceCommand? {
        return handleVoiceCommandUseCase.getCommand(id)
    }

    /**
     * Add custom command
     */
    suspend fun addCommand(command: VoiceCommand): Result<Unit> {
        return handleVoiceCommandUseCase.addCommand(command)
    }

    /**
     * Update command
     */
    suspend fun updateCommand(command: VoiceCommand): Result<Unit> {
        return handleVoiceCommandUseCase.updateCommand(command)
    }

    /**
     * Delete command
     */
    suspend fun deleteCommand(id: String): Result<Unit> {
        return handleVoiceCommandUseCase.deleteCommand(id)
    }

    /**
     * Toggle command
     */
    suspend fun toggleCommand(id: String, enabled: Boolean): Result<Unit> {
        return handleVoiceCommandUseCase.toggleCommand(id, enabled)
    }

    // ============================================================
    // History
    // ============================================================

    /**
     * Get command history
     */
    suspend fun getCommandHistory(): List<VoiceCommandHistory> {
        return handleVoiceCommandUseCase.getCommandHistory()
    }

    /**
     * Clear command history
     */
    suspend fun clearHistory(): Result<Unit> {
        return handleVoiceCommandUseCase.clearHistory()
    }

    // ============================================================
    // Configuration
    // ============================================================

    /**
     * Get voice configuration
     */
    suspend fun getConfig(): VoiceCommandConfig {
        return handleVoiceCommandUseCase.getConfig()
    }

    /**
     * Update voice configuration
     */
    suspend fun updateConfig(config: VoiceCommandConfig): Result<Unit> {
        return handleVoiceCommandUseCase.updateConfig(config)
    }

    /**
     * Get supported commands
     */
    suspend fun getSupportedCommands(): List<String> {
        return handleVoiceCommandUseCase.getSupportedCommands()
    }

    /**
     * Observe commands (reactive)
     */
    fun observeCommands(): Flow<List<VoiceCommand>> {
        return handleVoiceCommandUseCase.observeCommands()
    }

    /**
     * Observe last command (reactive)
     */
    fun observeLastCommand(): Flow<VoiceCommand?> {
        return handleVoiceCommandUseCase.observeLastCommand()
    }
}
```

---

## 👤 8. ProfileFeature

### Purpose
Orchestrates **user profile** management.

### Implementation

```kotlin
class ProfileFeature @Inject constructor(
    private val manageProfileUseCase: ManageProfileUseCase
) {

    // ============================================================
    // CRUD Operations
    // ============================================================

    /**
     * Create profile
     */
    suspend fun createProfile(profile: UserProfile): Result<String> {
        return manageProfileUseCase(profile)
    }

    /**
     * Get profile
     */
    suspend fun getProfile(id: String): UserProfile? {
        return manageProfileUseCase.getProfile(id)
    }

    /**
     * Get all profiles
     */
    suspend fun getAllProfiles(): List<UserProfile> {
        return manageProfileUseCase.getAllProfiles()
    }

    /**
     * Observe profiles (reactive)
     */
    fun observeProfiles(): Flow<List<UserProfile>> {
        return manageProfileUseCase.observeProfiles()
    }

    /**
     * Update profile
     */
    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return manageProfileUseCase.updateProfile(profile)
    }

    /**
     * Delete profile
     */
    suspend fun deleteProfile(id: String): Result<Unit> {
        return manageProfileUseCase.deleteProfile(id)
    }

    // ============================================================
    // Default & Favorites
    // ============================================================

    /**
     * Get default profile
     */
    suspend fun getDefaultProfile(): UserProfile? {
        return manageProfileUseCase.getDefaultProfile()
    }

    /**
     * Set default profile
     */
    suspend fun setDefaultProfile(id: String): Result<Unit> {
        return manageProfileUseCase.setDefaultProfile(id)
    }

    /**
     * Toggle favorite
     */
    suspend fun toggleFavorite(id: String): Result<Unit> {
        return manageProfileUseCase.toggleFavorite(id)
    }

    /**
     * Get favorite profiles
     */
    suspend fun getFavoriteProfiles(): List<UserProfile> {
        return manageProfileUseCase.getFavoriteProfiles()
    }

    // ============================================================
    // Settings
    // ============================================================

    /**
     * Get profile settings
     */
    suspend fun getSettings(profileId: String): ProfileSettings? {
        return manageProfileUseCase.getSettings(profileId)
    }

    /**
     * Update profile settings
     */
    suspend fun updateSettings(profileId: String, settings: ProfileSettings): Result<Unit> {
        return manageProfileUseCase.updateSettings(profileId, settings)
    }

    // ============================================================
    // Search & Export
    // ============================================================

    /**
     * Search profiles
     */
    suspend fun searchProfiles(query: String): List<UserProfile> {
        return manageProfileUseCase.searchProfiles(query)
    }

    /**
     * Export profile
     */
    suspend fun exportProfile(id: String): Result<String> {
        return manageProfileUseCase.exportProfile(id)
    }

    /**
     * Import profile
     */
    suspend fun importProfile(json: String): Result<Boolean> {
        return manageProfileUseCase.importProfile(json)
    }
}
```

---

## 🔄 9. UpdateFeature

### Purpose
Orchestrates **app update** checking, downloading, and installation.

### Implementation

```kotlin
class UpdateFeature @Inject constructor(
    private val checkForUpdatesUseCase: CheckForUpdatesUseCase
) {

    // ============================================================
    // Check for Updates
    // ============================================================

    /**
     * Check for updates
     */
    suspend fun checkForUpdates(): Result<UpdateResult> {
        return checkForUpdatesUseCase()
    }

    /**
     * Check manually
     */
    suspend fun checkManually(): Result<UpdateResult> {
        return checkForUpdatesUseCase.checkManually()
    }

    /**
     * Observe update status (reactive)
     */
    fun observeUpdateStatus(): Flow<UpdateResult> {
        return checkForUpdatesUseCase.observeUpdateStatus()
    }

    // ============================================================
    // Download
    // ============================================================

    /**
     * Download update
     */
    suspend fun downloadUpdate(version: String, onProgress: (Float) -> Unit): Result<Boolean> {
        return checkForUpdatesUseCase.downloadUpdate(version, onProgress)
    }

    /**
     * Observe download progress (reactive)
     */
    fun observeDownloadProgress(): Flow<UpdateProgress> {
        return checkForUpdatesUseCase.observeDownloadProgress()
    }

    // ============================================================
    // Install
    // ============================================================

    /**
     * Install update
     */
    suspend fun installUpdate(): Result<Unit> {
        return checkForUpdatesUseCase.installUpdate()
    }

    // ============================================================
    // Version Info
    // ============================================================

    /**
     * Get current version
     */
    suspend fun getCurrentVersion(): String {
        return checkForUpdatesUseCase.getCurrentVersion()
    }

    /**
     * Get latest version
     */
    suspend fun getLatestVersion(): String? {
        return checkForUpdatesUseCase.getLatestVersion()
    }

    /**
     * Check if update is available
     */
    suspend fun isUpdateAvailable(): Boolean {
        return checkForUpdatesUseCase.isUpdateAvailable()
    }

    // ============================================================
    // Cancel
    // ============================================================

    /**
     * Cancel download
     */
    suspend fun cancelDownload(): Result<Unit> {
        return checkForUpdatesUseCase.cancelDownload()
    }
}
```

---

## 📡 10. SensorFeature

### Purpose
Provides **sensor data** for real-time motion tracking.

### Implementation

```kotlin
class SensorFeature @Inject constructor(
    private val sensorRepository: ISensorRepository
) {

    // ============================================================
    // Sensor Data
    // ============================================================

    /**
     * Observe sensor data (reactive)
     */
    fun observeSensorData(): Flow<SensorData> {
        return sensorRepository.observeSensorData()
    }

    /**
     * Observe orientation (reactive)
     */
    fun observeOrientation(): Flow<OrientationData> {
        return sensorRepository.observeOrientation()
    }

    /**
     * Get current sensor data
     */
    suspend fun getCurrentSensorData(): SensorData {
        return sensorRepository.getCurrentSensorData()
    }

    // ============================================================
    // Sensor Control
    // ============================================================

    /**
     * Start sensors
     */
    suspend fun startSensors() {
        sensorRepository.startSensors()
    }

    /**
     * Stop sensors
     */
    suspend fun stopSensors() {
        sensorRepository.stopSensors()
    }

    /**
     * Check if sensors are active
     */
    suspend fun isSensorActive(): Boolean {
        return sensorRepository.isSensorActive()
    }

    // ============================================================
    // Calibration Status
    // ============================================================

    /**
     * Get calibration status
     */
    suspend fun getCalibrationStatus(): SensorCalibrationStatus {
        return sensorRepository.getCalibrationStatus()
    }

    /**
     * Observe calibration status (reactive)
     */
    fun observeCalibrationStatus(): Flow<SensorCalibrationStatus> {
        return sensorRepository.observeCalibrationStatus()
    }

    /**
     * Calibrate sensors
     */
    suspend fun calibrateSensors(): Boolean {
        return sensorRepository.calibrateSensors()
    }

    /**
     * Check if calibrated
     */
    suspend fun isCalibrated(): Boolean {
        return sensorRepository.isCalibrated()
    }

    // ============================================================
    // Power Management
    // ============================================================

    /**
     * Set power save mode
     */
    suspend fun setPowerSaveMode(enabled: Boolean) {
        sensorRepository.setPowerSaveMode(enabled)
    }

    /**
     * Get recommended sensor delay
     */
    suspend fun getRecommendedDelay(): Int {
        return sensorRepository.getRecommendedDelay()
    }
}
```

---

## 📋 Feature Summary Table

| Feature | Use Cases | Key Methods |
|---------|-----------|-------------|
| **ConnectionFeature** | `ConnectToServerUseCase`, `DiscoverServersUseCase`, `GetConnectionStatusUseCase`, `TestConnectionUseCase` | `connect()`, `disconnect()`, `observeConnectionStatus()`, `startDiscovery()` |
| **MouseControlFeature** | `SendMovementUseCase`, `IMouseRepository` | `move()`, `click()`, `scroll()`, `getMovementProfile()` |
| **CalibrationFeature** | `CalibrationUseCase` | `startFullCalibration()`, `getCalibrationStatus()`, `resetCalibration()` |
| **GestureRecognitionFeature** | `DetectGestureUseCase`, `ManageGestureTemplatesUseCase` | `detectGesture()`, `addTemplate()`, `trainGesture()` |
| **ProximityFeature** | `GetProximityStateUseCase`, `UpdateProximityConfigUseCase` | `getProximityState()`, `startMonitoring()`, `calibrate()` |
| **StatisticsFeature** | `GetStatisticsUseCase`, `RecordStatisticsUseCase` | `recordClick()`, `getSessionStats()`, `exportStats()` |
| **VoiceFeature** | `HandleVoiceCommandUseCase` | `processVoiceInput()`, `startListening()`, `addCommand()` |
| **ProfileFeature** | `ManageProfileUseCase` | `createProfile()`, `getDefaultProfile()`, `exportProfile()` |
| **UpdateFeature** | `CheckForUpdatesUseCase` | `checkForUpdates()`, `downloadUpdate()`, `installUpdate()` |
| **SensorFeature** | `ISensorRepository` | `observeSensorData()`, `startSensors()`, `calibrateSensors()` |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Single Responsibility** | Each feature handles one domain (Connection, Mouse, etc.) |
| **Orchestration** | Features coordinate multiple use cases |
| **Clean API** | ViewModels get a clean, cohesive API |
| **Dependency Injection** | All dependencies are injected via constructor |
| **Reactive Support** | `Flow` for real-time data streams |
| **Error Handling** | `Result<T>` for explicit error handling |
| **Testability** | Easy to mock use cases in unit tests |

---

**The Features Layer provides a clean, cohesive API for ViewModels, orchestrating multiple use cases into high-level business operations.**