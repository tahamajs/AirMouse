# 📘 Air Mouse Domain Models – Complete Documentation

## 📁 Package Overview

The `com.airmouse.domain.model` package contains **all domain models** used throughout the Air Mouse application. These models are **pure Kotlin data classes** with no Android dependencies (except `@Parcelize` for `Parcelable` support). They represent the **core business entities** and are shared across all layers (presentation, domain, data).

```
com.airmouse.domain.model/
├── CalibrationModels.kt          # Calibration data, status, quality
├── ConnectionModels.kt           # Connection config, status, quality
├── GestureModels.kt              # Gesture types, events, templates
├── MouseModels.kt                # Mouse events, profiles, statistics
├── ProximityModels.kt            # Proximity state, config, calibration
├── SensorModels.kt               # Sensor data, orientation, info
├── StatisticsModels.kt           # Statistics summary, daily, historical
├── UpdateModels.kt               # Update info, version, progress
├── VoiceCommandModels.kt         # Voice commands, config, history
├── ProfileModels.kt              # User profiles, settings
├── PreferencesModels.kt          # App preferences, user preferences
├── BluetoothModels.kt            # Bluetooth device info, BLE services
├── ErrorModels.kt                # Error types and models
├── ControlMode.kt                # Control mode enum
├── CalibrationData.kt            # Calibration data with JSON serialization
├── GestureTemplate.kt            # Gesture template with JSON serialization
├── StatisticsSummary.kt          # Statistics summary with JSON serialization
├── CalibrationResult.kt          # Calibration result
├── CustomGestureTemplate.kt      # Custom gesture template
├── GestureActionMap.kt           # Gesture to action mapping
├── GestureTrainingStats.kt       # Gesture training statistics
├── HistoricalStatistics.kt       # Historical aggregated statistics
├── Profile.kt                    # Simple profile for sync
├── TestResult.kt                 # Connection test result
├── UserProfile.kt                # Complete user profile
├── ProfileSettings.kt            # Profile settings
├── DiscoveredServer.kt           # Discovered server info
├── ConnectionQuality.kt          # Connection quality
├── CommunityHub.kt               # Community hub data
├── DailyStats.kt                 # Daily statistics
├── GestureStats.kt               # Gesture statistics
├── GyroBias.kt                   # Gyroscope bias
├── AccelCalibration.kt           # Accelerometer calibration
├── MagCalibration.kt             # Magnetometer calibration
├── CalibrationProgress.kt        # Calibration progress
├── CalibrationState.kt           # Calibration state
├── SensorCalibrationData.kt      # Sensor calibration data
├── TrainingSample.kt             # Training sample
├── TransferState.kt              # File transfer state
└── AppError.kt                   # Application error
```

---

## 🎯 1. Calibration Models

### `CalibrationStatus.kt`
Defines the current state of sensor calibration.

```kotlin
enum class CalibrationStatus {
    NOT_STARTED,      // No calibration has been performed
    IN_PROGRESS,      // Calibration is currently running
    GYRO_COMPLETE,    // Gyroscope calibration complete
    MAG_COMPLETE,     // Magnetometer calibration complete
    ACCEL_COMPLETE,   // Accelerometer calibration complete
    COMPLETED,        // All sensors calibrated
    FAILED,           // Calibration failed
    SKIPPED,          // User skipped calibration
    IDLE              // Waiting for user action
}
```

### `CalibrationQuality.kt`
Quality assessment of the calibration.

```kotlin
enum class CalibrationQuality {
    EXCELLENT,    // Perfect calibration (variance < 0.01)
    GOOD,         // Good calibration (variance < 0.05)
    FAIR,         // Acceptable calibration (variance < 0.1)
    POOR,         // Poor calibration (variance >= 0.1)
    UNKNOWN       // Quality not assessed
}
```

### `SensorCalibrationData.kt`
Offset and scale factors for a sensor.

```kotlin
@Parcelize
data class SensorCalibrationData(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f
) : Parcelable {
    fun isCalibrated(): Boolean {
        return offsetX != 0f || offsetY != 0f || offsetZ != 0f ||
                scaleX != 1f || scaleY != 1f || scaleZ != 1f
    }
    
    fun applyCalibration(rawX: Float, rawY: Float, rawZ: Float): Triple<Float, Float, Float> {
        return Triple(
            (rawX - offsetX) * scaleX,
            (rawY - offsetY) * scaleY,
            (rawZ - offsetZ) * scaleZ
        )
    }
}
```

### `CalibrationData.kt`
Complete calibration data for all sensors.

```kotlin
@Parcelize
data class CalibrationData(
    val gyroBias: SensorCalibrationData = SensorCalibrationData(),
    val accelOffset: SensorCalibrationData = SensorCalibrationData(),
    val magOffset: SensorCalibrationData = SensorCalibrationData(),
    val isCalibrated: Boolean = false,
    val quality: CalibrationQuality = CalibrationQuality.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun isFullyCalibrated(): Boolean {
        return isCalibrated &&
                gyroBias.isCalibrated() &&
                accelOffset.isCalibrated() &&
                magOffset.isCalibrated()
    }
    
    fun getCalibratedSensorCount(): Int {
        var count = 0
        if (gyroBias.isCalibrated()) count++
        if (accelOffset.isCalibrated()) count++
        if (magOffset.isCalibrated()) count++
        return count
    }
}
```

### `CalibrationProgress.kt`
Progress of a calibration step.

```kotlin
@Parcelize
data class CalibrationProgress(
    val step: CalibrationStep,
    val progress: Int,
    val message: String,
    val isComplete: Boolean = false,
    val error: String? = null
) : Parcelable

enum class CalibrationStep(val displayName: String) {
    GYROSCOPE("Gyroscope Calibration"),
    ACCELEROMETER("Accelerometer Calibration"),
    MAGNETOMETER("Magnetometer Calibration")
}
```

---

## 🌐 2. Connection Models

### `ConnectionStatus.kt`
Status of the network connection.

```kotlin
enum class ConnectionStatus {
    DISCONNECTED,    // Not connected
    CONNECTING,      // Attempting to connect
    CONNECTED,       // Successfully connected
    RECONNECTING,    // Reconnecting after disconnect
    ERROR,           // Connection error
    TIMEOUT          // Connection timeout
}
```

### `ConnectionProtocol.kt`
Supported connection protocols.

```kotlin
enum class ConnectionProtocol {
    TCP,           // Raw TCP socket
    WEBSOCKET,     // WebSocket (recommended)
    UDP            // UDP datagrams
}
```

### `ConnectionConfig.kt`
Configuration for a connection.

```kotlin
data class ConnectionConfig(
    val ip: String = "",
    val port: Int = 8080,
    val protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET,
    val useSSL: Boolean = false,
    val authToken: String? = null,
    val autoReconnect: Boolean = true,
    val timeoutMs: Long = 10000L
) {
    fun normalized(): ConnectionConfig {
        val effectivePort = when {
            protocol == ConnectionProtocol.WEBSOCKET -> DEFAULT_WEBSOCKET_PORT
            protocol == ConnectionProtocol.TCP -> DEFAULT_TCP_PORT
            protocol == ConnectionProtocol.UDP -> DEFAULT_UDP_PORT
            else -> port
        }
        return copy(port = effectivePort)
    }
    
    companion object {
        const val DEFAULT_TCP_PORT = 8080
        const val DEFAULT_WEBSOCKET_PORT = 8081
        const val DEFAULT_UDP_PORT = 8082
    }
}
```

### `ConnectionQuality.kt`
Quality of the network connection.

```kotlin
@Parcelize
data class ConnectionQuality(
    val rssi: Int = 0,              // Signal strength in dBm
    val ping: Int = 0,              // Round-trip time in ms
    val jitter: Int = 0,            // Latency variation in ms
    val packetLoss: Float = 0f,     // Packet loss percentage (0.0 - 1.0)
    val dataRate: Int = 0,          // Data rate in kbps
    val signalStrength: SignalStrength = SignalStrength.UNKNOWN
) : Parcelable {
    
    enum class SignalStrength {
        EXCELLENT, GOOD, FAIR, POOR, VERY_POOR, UNKNOWN
    }
    
    fun score(): Int = when (signalStrength) {
        SignalStrength.EXCELLENT -> 100
        SignalStrength.GOOD -> 75
        SignalStrength.FAIR -> 50
        SignalStrength.POOR -> 25
        SignalStrength.VERY_POOR -> 10
        SignalStrength.UNKNOWN -> 0
    }
    
    fun isHealthy(): Boolean = ping < 200 && packetLoss < 0.1f
}
```

### `DiscoveredServer.kt`
A server discovered via UDP.

```kotlin
data class DiscoveredServer(
    val ip: String,
    val port: Int,
    val name: String,
    val version: String = "3.0",
    val rssi: Int = 0,
    val lastSeen: Long = System.currentTimeMillis()
)
```

---

## ✋ 3. Gesture Models

### `GestureType.kt`
Types of gestures.

```kotlin
enum class GestureType {
    NONE,
    CLICK,
    DOUBLE_CLICK,
    RIGHT_CLICK,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    CIRCLE_CW,
    CIRCLE_CCW,
    THUMBS_UP,
    THUMBS_DOWN,
    ZOOM_IN,
    ZOOM_OUT,
    SHAKE,
    PEACE,
    FIST,
    CUSTOM
}
```

### `GestureEvent.kt`
A detected gesture event.

```kotlin
data class GestureEvent(
    val type: GestureType,
    val name: String = "",
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val velocity: Float = 0f,
    val duration: Long = 0,
    val isCustom: Boolean = false
) {
    fun isHighConfidence(threshold: Float = 0.7f): Boolean = confidence >= threshold
    fun getAction(): String = GestureActionMap.getAction(type)
}
```

### `CustomGestureTemplate.kt`
User-defined gesture template.

```kotlin
@Parcelize
data class CustomGestureTemplate(
    val id: String = "",
    val name: String = "",
    val type: GestureType = GestureType.CUSTOM,
    val action: String = "",
    val confidence: Float = 0.7f,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
) : Parcelable {
    fun isValid(): Boolean = name.isNotEmpty() && action.isNotEmpty()
    fun toGestureEvent(): GestureEvent = GestureEvent(
        type = type,
        name = name,
        confidence = confidence,
        isCustom = true
    )
}
```

### `GestureTemplate.kt`
Simple gesture template for sync.

```kotlin
data class GestureTemplate(
    val id: String = "",
    val name: String = "",
    val type: GestureType = GestureType.CUSTOM,
    val data: ByteArray = byteArrayOf(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("data", data.toString())
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }
}
```

### `GestureTrainingStats.kt`
Statistics for gesture training.

```kotlin
@Parcelize
data class GestureTrainingStats(
    val totalGestures: Int = 0,
    val totalTrainingSessions: Int = 0,
    val averageConfidence: Float = 0.0f,
    val bestConfidence: Float = 0.0f,
    val lastGestureTime: Long = 0,
    val totalTrainingTime: Long = 0,
    val averageTrainingTime: Long = 0,
    val gesturesByType: Map<GestureType, Int> = emptyMap(),
    val detectionHistory: List<GestureDetectionEvent> = emptyList(),
    val successRate: Float = 0.0f,
    val falsePositiveRate: Float = 0.0f
) : Parcelable {
    
    fun recordSuccess(gestureName: String, confidence: Float): GestureTrainingStats {
        val updatedHistory = detectionHistory.takeLast(99).toMutableList()
        updatedHistory.add(
            GestureDetectionEvent(
                gestureName = gestureName,
                confidence = confidence,
                timestamp = System.currentTimeMillis(),
                isSuccessful = true
            )
        )
        val totalAttempts = updatedHistory.size
        val successes = updatedHistory.count { it.isSuccessful }
        val newSuccessRate = if (totalAttempts > 0) successes.toFloat() / totalAttempts else 0f
        
        return copy(
            totalGestures = totalGestures + 1,
            averageConfidence = if (totalGestures > 0) {
                (averageConfidence * totalGestures + confidence) / (totalGestures + 1)
            } else {
                confidence
            },
            bestConfidence = maxOf(bestConfidence, confidence),
            lastGestureTime = System.currentTimeMillis(),
            detectionHistory = updatedHistory,
            successRate = newSuccessRate
        )
    }
}
```

---

## 🖱️ 4. Mouse Models

### `MouseButton.kt`
Mouse button types.

```kotlin
enum class MouseButton {
    LEFT,
    RIGHT,
    MIDDLE,
    BACK,
    FORWARD
}
```

### `MouseEvent.kt`
Sealed class for mouse events.

```kotlin
sealed class MouseEvent {
    data class Move(val dx: Float, val dy: Float) : MouseEvent()
    data class Click(val button: MouseButton, val repeat: Int = 1) : MouseEvent()
    data class Scroll(val delta: Int, val direction: ScrollDirection) : MouseEvent()
    object DoubleClick : MouseEvent()
    object RightClick : MouseEvent()
    object MiddleClick : MouseEvent()
}

enum class ScrollDirection {
    UP,
    DOWN,
    NONE
}
```

### `MovementProfile.kt`
Cursor movement profile.

```kotlin
@Parcelize
data class MovementProfile(
    val sensitivity: Float = 1.0f,
    val smoothingEnabled: Boolean = true,
    val accelerationEnabled: Boolean = true,
    val accelerationFactor: Float = 1.5f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val swapAxes: Boolean = false,
    val deadband: Float = 0.5f,
    val maxSpeed: Float = 100f,
    val minSpeed: Float = 0.5f,
    val predictiveBlend: Float = 0.6f,
    val smoothingAlpha: Float = 0.3f
) : Parcelable {
    
    fun isValid(): Boolean {
        return sensitivity in 0.1f..3.0f &&
                accelerationFactor in 1.0f..3.0f &&
                deadband in 0f..2.0f &&
                maxSpeed > 0 &&
                minSpeed in 0f..maxSpeed &&
                predictiveBlend in 0f..1f &&
                smoothingAlpha in 0f..1f
    }
}
```

### `MouseStatistics.kt`
Mouse usage statistics.

```kotlin
@Parcelize
data class MouseStatistics(
    val totalClicks: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalMovement: Float = 0f,
    val movementCount: Int = 0,
    val averageSpeed: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun recordMovement(dx: Float, dy: Float): MouseStatistics {
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val newTotalMovement = totalMovement + distance
        val newMovementCount = movementCount + 1
        val newAverageSpeed = if (newMovementCount > 0) {
            newTotalMovement / newMovementCount
        } else 0f
        
        return copy(
            totalMovement = newTotalMovement,
            movementCount = newMovementCount,
            averageSpeed = newAverageSpeed,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
```

---

## 📱 5. Proximity Models

### `ProximityState.kt`
Current proximity state.

```kotlin
@Parcelize
data class ProximityState(
    val isNear: Boolean,
    val distance: Float,
    val signalStrength: Int,
    val deviceAddress: String,
    val deviceName: String? = null,
    val lastUpdate: Long = System.currentTimeMillis(),
    val confidence: Float = 0.8f
) : Parcelable {
    fun isValid(maxAgeMs: Long = 5000L): Boolean {
        return lastUpdate > 0 &&
                (System.currentTimeMillis() - lastUpdate) < maxAgeMs &&
                confidence >= 0.5f
    }
}
```

### `ProximityConfig.kt`
Proximity detection configuration.

```kotlin
@Parcelize
data class ProximityConfig(
    val enabled: Boolean = false,
    val nearThreshold: Float = 1.5f,
    val farThreshold: Float = 3.0f,
    val scanInterval: Long = 1000L,
    val vibrationEnabled: Boolean = true,
    val autoLockEnabled: Boolean = true,
    val autoUnlockEnabled: Boolean = true,
    val deviceAddress: String = ""
) : Parcelable {
    fun isValid(): Boolean {
        return enabled &&
                nearThreshold > 0.3f &&
                farThreshold > nearThreshold &&
                scanInterval >= 200L &&
                deviceAddress.isNotBlank()
    }
}
```

### `ProximityCalibration.kt`
Calibration data for proximity detection.

```kotlin
@Parcelize
data class ProximityCalibration(
    val isCalibrated: Boolean,
    val referenceRssi: Int = -59,
    val pathLossExponent: Float = 2.5f,
    val nearThreshold: Float = 1.5f,
    val farThreshold: Float = 3.0f,
    val accuracy: Float = 0.7f,
    val calibrationTime: Long = 0
) : Parcelable {
    
    fun calculateDistance(rssi: Int): Float {
        if (rssi >= 0) return 0.3f
        return (10.0.pow((referenceRssi - rssi) / (10.0 * pathLossExponent))).toFloat()
    }
}
```

---

## 📊 6. Statistics Models

### `StatisticsSummary.kt`
Summary of statistics for a session.

```kotlin
@Parcelize
data class StatisticsSummary(
    val totalClicks: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalMovements: Int = 0,
    val totalDistance: Float = 0f,
    val averageSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val sessionDuration: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable {
    fun toJson(): JSONObject = JSONObject().apply {
        put("totalClicks", totalClicks)
        put("totalDoubleClicks", totalDoubleClicks)
        put("totalRightClicks", totalRightClicks)
        put("totalScrolls", totalScrolls)
        put("totalMovements", totalMovements)
        put("totalDistance", totalDistance)
        put("averageSpeed", averageSpeed)
        put("maxSpeed", maxSpeed)
        put("sessionDuration", sessionDuration)
    }
}
```

### `DailyStats.kt`
Statistics for a single day.

```kotlin
@Parcelize
data class DailyStats(
    val date: String, // Format: "yyyy-MM-dd"
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val movements: Int = 0,
    val distance: Float = 0f,
    val gestures: Int = 0,
    val totalTime: Long = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable {
    fun add(other: DailyStats): DailyStats {
        return DailyStats(
            date = date,
            clicks = clicks + other.clicks,
            doubleClicks = doubleClicks + other.doubleClicks,
            rightClicks = rightClicks + other.rightClicks,
            scrolls = scrolls + other.scrolls,
            movements = movements + other.movements,
            distance = distance + other.distance,
            gestures = gestures + other.gestures,
            totalTime = totalTime + other.totalTime,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
```

### `HistoricalStatistics.kt`
Aggregated historical statistics.

```kotlin
@Parcelize
data class HistoricalStatistics(
    val totalGestures: Int = 0,
    val gesturesByType: Map<String, Int> = emptyMap(),
    val mostUsedGesture: String = "",
    val lastGestureTime: Long = 0,
    val customGestureUsage: Map<String, Int> = emptyMap(),
    val totalSessions: Int = 0,
    val averageGesturesPerSession: Float = 0f,
    val totalClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val longestSessionMs: Long = 0,
    val firstSessionDate: Long = 0,
    val lastSessionDate: Long = 0
) : Parcelable {
    
    fun getTotalDetections(): Int = totalGestures
    fun getTotalClicks(): Int = totalClicks + totalDoubleClicks + totalRightClicks
    fun hasData(): Boolean = totalGestures > 0
}
```

---

## 👤 7. Profile Models

### `UserProfile.kt`
Complete user profile.

```kotlin
@Parcelize
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUri: String? = null,
    val settings: ProfileSettings = ProfileSettings(),
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val iconRes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
) : Parcelable {
    
    fun isDefaultProfile(): Boolean = isDefault
    fun updateSettings(newSettings: ProfileSettings): UserProfile {
        return copy(settings = newSettings, updatedAt = System.currentTimeMillis())
    }
    fun toggleFavorite(): UserProfile {
        return copy(isFavorite = !isFavorite, updatedAt = System.currentTimeMillis())
    }
    fun recordUsage(): UserProfile {
        return copy(usageCount = usageCount + 1, updatedAt = System.currentTimeMillis())
    }
}
```

### `ProfileSettings.kt`
User profile settings.

```kotlin
@Parcelize
data class ProfileSettings(
    val sensitivity: Float = 1.0f,
    val clickThreshold: Float = 5.0f,
    val doubleClickInterval: Long = 400L,
    val scrollThreshold: Float = 8.0f,
    val rightClickTilt: Float = 45f,
    val hapticEnabled: Boolean = true,
    val theme: String = "dark",
    val aiSmoothing: Boolean = false,
    val predictiveMovement: Boolean = true,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val accelerationEnabled: Boolean = true,
    val smoothingEnabled: Boolean = true,
    val edgeGesturesEnabled: Boolean = false,
    val voiceCommandsEnabled: Boolean = false
) : Parcelable {
    fun isValid(): Boolean {
        return sensitivity in 0.1f..3.0f &&
                clickThreshold in 1.0f..20.0f &&
                doubleClickInterval in 100L..1000L &&
                scrollThreshold in 1.0f..20.0f &&
                rightClickTilt in 10f..80f
    }
}
```

---

## 🎙️ 8. Voice Command Models

### `VoiceCommand.kt`
A voice command mapping a phrase to an action.

```kotlin
@Parcelize
data class VoiceCommand(
    val id: String = "",
    val text: String = "",
    val action: String = "",
    val confidence: Float = 0f,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {
    fun getActionDisplayName(): String {
        return when (action) {
            "play_pause" -> "Play/Pause"
            "next_track" -> "Next Track"
            "volume_up" -> "Volume Up"
            // ... etc
            else -> action.replace("_", " ").split(" ").joinToString(" ") {
                it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }
}
```

### `VoiceCommandConfig.kt`
Voice command configuration.

```kotlin
data class VoiceCommandConfig(
    val wakeWord: String = "hey air mouse",
    val wakeWordConfidence: Float = 0.7f,
    val isEnabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val silenceTimeoutMs: Long = 2000L,
    val wakeWordTimeoutMs: Long = 10000L
)
```

---

## 🔄 9. Update Models

### `UpdateResult.kt`
Result of checking for updates.

```kotlin
data class UpdateResult(
    val isAvailable: Boolean = false,
    val version: String? = null,
    val releaseNotes: String? = null,
    val fileSize: Long = 0,
    val downloadUrl: String? = null
) {
    fun isValid(): Boolean {
        return isAvailable && !version.isNullOrEmpty() && !downloadUrl.isNullOrEmpty()
    }
}
```

### `VersionInfo.kt`
App version information.

```kotlin
@Parcelize
data class VersionInfo(
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val buildDate: Long = System.currentTimeMillis(),
    val minSupportedVersion: String = "1.0.0"
) : Parcelable
```

---

## ❌ 10. Error Models

### `ErrorType.kt`
Types of errors.

```kotlin
enum class ErrorType {
    NETWORK,
    CONNECTION,
    AUTHENTICATION,
    PERMISSION,
    SENSOR,
    BLUETOOTH,
    USB,
    GESTURE,
    CALIBRATION,
    UNKNOWN
}
```

### `AppError.kt`
Application error model.

```kotlin
data class AppError(
    val type: ErrorType,
    val message: String,
    val code: Int = 0,
    val cause: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isRecoverable(): Boolean = when (type) {
        ErrorType.NETWORK -> true
        ErrorType.CONNECTION -> true
        ErrorType.AUTHENTICATION -> true
        ErrorType.PERMISSION -> false
        else -> false
    }
    
    fun getUserMessage(): String {
        return when (type) {
            ErrorType.NETWORK -> "Network connection issue. Please check your internet."
            ErrorType.CONNECTION -> "Failed to connect to server. Please try again."
            // ... etc
            else -> "An error occurred. Please try again."
        }
    }
}
```

---

## 📋 11. Additional Models

### `ControlMode.kt`
Control mode for cursor control.

```kotlin
enum class ControlMode(val displayName: String) {
    GYRO("Gyroscope (Tilt)"),
    ACCEL("Accelerometer (Move)"),
    HYBRID("Hybrid")
}
```

### `TransferState.kt`
File transfer state.

```kotlin
enum class TransferState {
    IDLE,
    UPLOADING,
    DOWNLOADING,
    COMPLETE,
    ERROR
}
```

### `CommunityHub.kt`
Community hub data.

```kotlin
data class CommunityHub(
    val isEnabled: Boolean = false,
    val userId: String = "",
    val deviceName: String = "",
    val sharedProfiles: List<String> = emptyList(),
    val communityGestures: List<String> = emptyList(),
    val lastSync: Long = System.currentTimeMillis()
)
```

### `BluetoothModels.kt`
Bluetooth device information.

```kotlin
@Parcelize
data class BluetoothDeviceInfo(
    val address: String = "",
    val name: String = "",
    val rssi: Int = 0,
    val txPower: Int = 0,
    val isConnected: Boolean = false,
    val isPaired: Boolean = false,
    val deviceType: String = "",
    val bondState: Int = 0
) : Parcelable
```

---

## 📊 Model Relationships Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         DOMAIN MODELS                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │  Calibration    │    │   Connection    │    │    Gesture      │     │
│  │  ├─Status       │    │  ├─Status       │    │  ├─Type         │     │
│  │  ├─Quality      │    │  ├─Protocol     │    │  ├─Event        │     │
│  │  ├─Data         │    │  ├─Config       │    │  ├─Template     │     │
│  │  └─Progress     │    │  ├─Quality      │    │  └─Stats        │     │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘     │
│                                                                         │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │     Mouse       │    │   Proximity     │    │   Statistics    │     │
│  │  ├─Button       │    │  ├─State        │    │  ├─Summary      │     │
│  │  ├─Event        │    │  ├─Config       │    │  ├─Daily        │     │
│  │  ├─Profile      │    │  └─Calibration  │    │  └─Historical   │     │
│  │  └─Statistics   │    └─────────────────┘    └─────────────────┘     │
│  └─────────────────┘                                                   │
│                                                                         │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │    Profile      │    │   Voice         │    │    Update       │     │
│  │  ├─User         │    │  ├─Command      │    │  ├─Result       │     │
│  │  ├─Settings     │    │  ├─Config       │    │  ├─Version      │     │
│  │  └─Sort         │    │  └─History      │    │  └─Progress     │     │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘     │
│                                                                         │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │    Bluetooth    │    │    Error        │    │   Control       │     │
│  │  ├─DeviceInfo   │    │  ├─Type         │    │  └─Mode         │     │
│  │  ├─BLEService   │    │  └─AppError     │    │                  │     │
│  │  └─Charact.     │    └─────────────────┘    └─────────────────┘     │
│  └─────────────────┘                                                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Summary of All Models

| Category | Models | Purpose |
|----------|--------|---------|
| **Calibration** | `CalibrationStatus`, `CalibrationQuality`, `SensorCalibrationData`, `CalibrationData`, `CalibrationProgress` | Sensor calibration data and state |
| **Connection** | `ConnectionStatus`, `ConnectionProtocol`, `ConnectionConfig`, `ConnectionQuality`, `DiscoveredServer` | Network connection management |
| **Gesture** | `GestureType`, `GestureEvent`, `CustomGestureTemplate`, `GestureTemplate`, `GestureTrainingStats` | Gesture detection and training |
| **Mouse** | `MouseButton`, `MouseEvent`, `MovementProfile`, `MouseStatistics` | Cursor control and statistics |
| **Proximity** | `ProximityState`, `ProximityConfig`, `ProximityCalibration` | Bluetooth proximity detection |
| **Statistics** | `StatisticsSummary`, `DailyStats`, `HistoricalStatistics`, `GestureStats` | Usage statistics |
| **Profile** | `UserProfile`, `ProfileSettings`, `ProfileSort`, `ViewMode` | User profiles and settings |
| **Voice** | `VoiceCommand`, `VoiceCommandConfig`, `VoiceCommandHistory` | Voice command processing |
| **Update** | `UpdateResult`, `VersionInfo`, `UpdateInfo`, `UpdateProgress` | App updates |
| **Bluetooth** | `BluetoothDeviceInfo`, `BLEService`, `BLECharacteristic` | Bluetooth communication |
| **Error** | `ErrorType`, `AppError` | Error handling |
| **Other** | `ControlMode`, `TransferState`, `CommunityHub` | Miscellaneous models |

---

**These domain models form the core business entities of the Air Mouse application, providing a consistent, type-safe, and maintainable foundation for all layers of the app.**