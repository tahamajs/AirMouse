# 📘 Air Mouse Utils Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.utils` package is the **largest and most comprehensive utility package** in the Air Mouse application. It provides a wide range of helper classes, extensions, and utilities for preferences, permissions, logging, networking, file operations, animation, validation, and more.

```
com.airmouse.utils/
├── PreferencesManager.kt           # Core preferences management
├── PreferencesKeys.kt              # Preference key constants
├── PreferencesHelper.kt            # Helper for preferences
├── SharedPrefsUtils.kt             # SharedPreferences extensions
├── PreferencesDataStore.kt         # AndroidX DataStore wrapper
├── LogManager.kt                   # Centralized logging
├── PermissionHelper.kt             # Permission management
├── PermissionManager.kt            # Permission manager (Hilt)
├── PermissionUIHelper.kt           # Permission UI helpers
├── PermissionUtils.kt              # Permission utilities
├── VibrateUtils.kt                 # Haptic feedback utilities
├── AudioUtils.kt                   # Sound playback utilities
├── BluetoothUtils.kt               # Bluetooth operations
├── BatterySaver.kt                 # Battery optimization
├── BatteryOptimizer.kt             # Battery optimization (advanced)
├── ConnectedDeviceStore.kt         # Connected device history
├── QRScanner.kt                    # QR code scanning
├── AnimationUtils.kt               # Animation utilities
├── StringUtils.kt                  # String helpers
├── ValidationUtils.kt              # Validation helpers
├── MathUtils.kt                    # Math helpers
├── ConversionUtils.kt              # Unit conversion
├── DateUtils.kt                    # Date/time formatting
├── ColorUtils.kt                   # Color manipulation
├── FileHelper.kt                   # File operations
├── JsonHelper.kt                   # JSON serialization
├── NetworkUtils.kt                 # Network utilities
├── NetworkStateHelper.kt           # Network state monitoring
├── ResourceHelper.kt               # Resource access
├── ThemeManager.kt                 # Theme management
├── DialogHelper.kt                 # Dialog helpers
├── ViewHelpers.kt                  # View extensions
├── ResultExtensions.kt             # Result<T> extensions
├── SensorUtils.kt                  # Sensor utilities
├── ErrorHandler.kt                 # Centralized error handling
├── AppConstants.kt                 # Application constants
├── Constants.kt                    # Additional constants
├── ApplicationContext.kt           # Hilt qualifier
└── Utils.kt                        # General utilities
```

---

## 📦 1. Preferences & Storage

### PreferencesManager

#### Purpose
The **core preference manager** that handles all shared preferences operations. It provides type-safe getters and setters for all application settings.

#### Key Features

| Feature | Description |
|---------|-------------|
| **Type-Safe** | All getters/setters are type-specific |
| **Comprehensive** | Covers cursor, gesture, AI, haptic, display, touchpad, connection, privacy, presentation settings |
| **Calibration Storage** | Stores gyro, accel, mag calibration data |
| **Statistics** | Tracks clicks, scrolls, gestures, sessions |
| **Profiles** | Manages user profiles |
| **Export/Import** | Settings export/import functionality |
| **Reactive** | StateFlow for observable preferences |

#### Key Methods

```kotlin
// Settings
fun getSensitivity(): Float
fun setSensitivity(value: Float)
fun getClickThreshold(): Float
fun setClickThreshold(value: Float)
fun getTheme(): String
fun setTheme(theme: String)
fun isHapticEnabled(): Boolean
fun setHapticEnabled(enabled: Boolean)

// Calibration
fun saveCalibrationData(data: CalibrationData)
fun getCalibrationData(): CalibrationData
fun isCalibrated(): Boolean
fun setCalibrated(calibrated: Boolean)

// Statistics
fun getClickCount(): Int
fun incrementClickCount()
fun getSessionStats(): StatisticsSummary

// Profiles
fun saveProfile(name: String, sensitivity: Float, clickThreshold: Float, scrollThreshold: Float)
fun getAllProfileNames(): List<String>

// Export/Import
fun exportSettings(): String
fun importSettings(data: String): Boolean
```

### PreferencesKeys

#### Purpose
Centralizes **all preference key constants** to prevent typos and ensure consistency.

#### Key Categories

| Category | Keys | Count |
|----------|------|-------|
| **Connection** | `KEY_LAST_IP`, `KEY_LAST_PORT`, `KEY_AUTO_CONNECT`, `KEY_RECONNECT_ATTEMPTS`, etc. | 15 |
| **Gyroscope** | `KEY_GYRO_BIAS_X`, `KEY_GYRO_BIAS_Y`, `KEY_GYRO_BIAS_Z`, `KEY_GYRO_VARIANCE_X`, etc. | 10 |
| **Accelerometer** | `KEY_ACCEL_OFFSET_X`, `KEY_ACCEL_OFFSET_Y`, `KEY_ACCEL_OFFSET_Z`, etc. | 15 |
| **Magnetometer** | `KEY_MAG_OFFSET_X`, `KEY_MAG_OFFSET_Y`, `KEY_MAG_OFFSET_Z`, etc. | 10 |
| **Calibration** | `KEY_CALIBRATION_STATUS`, `KEY_CALIBRATION_QUALITY`, `KEY_CALIBRATION_PROGRESS`, etc. | 12 |
| **Mouse** | `KEY_SENSITIVITY`, `KEY_SMOOTHING_ENABLED`, `KEY_ACCELERATION_ENABLED`, etc. | 10 |
| **AI** | `KEY_AI_SMOOTHING`, `KEY_PREDICTIVE_MOVEMENT`, `KEY_KALMAN_ENABLED`, etc. | 5 |
| **Touchpad** | `KEY_TOUCHPAD_ACTIVE`, `KEY_TOUCHPAD_SENSITIVITY`, etc. | 19 |
| **Voice** | `KEY_VOICE_ENABLED`, `KEY_VOICE_WAKE_WORD`, `KEY_VOICE_SENSITIVITY`, etc. | 10 |
| **Proximity** | `KEY_PROXIMITY_ENABLED`, `KEY_PROXIMITY_NEAR_THRESHOLD`, etc. | 8 |
| **Privacy** | `KEY_ANONYMOUS_STATS`, `KEY_CRASH_REPORTING`, `KEY_CLEAR_DATA_ON_EXIT` | 3 |
| **Accessibility** | `KEY_ANNOUNCE_MOVEMENT`, `KEY_HIGH_CONTRAST`, `KEY_COLOR_BLIND_MODE`, etc. | 6 |
| **Statistics** | `KEY_STAT_CLICKS`, `KEY_STAT_SCROLLS`, `KEY_SESSION_CLICKS`, etc. | 15 |
| **Total** | | **~138** |

### ConnectedDeviceStore

#### Purpose
Stores and manages **connection history** for discovered servers.

#### Key Methods

```kotlin
fun rememberConnection(
    prefs: PreferencesManager,
    serverName: String,
    ip: String,
    port: Int,
    protocol: String,
    version: String = "3.0.0"
)

private fun loadSavedServers(prefs: PreferencesManager): List<JSONObject>
private fun loadConnectionHistory(prefs: PreferencesManager): List<JSONObject>
```

#### Usage

```kotlin
ConnectedDeviceStore.rememberConnection(
    prefs = prefs,
    serverName = "Air Mouse Pro",
    ip = "192.168.1.100",
    port = 8081,
    protocol = "WEBSOCKET",
    version = "3.0.0"
)
```

---

## 📝 2. Logging & Error Handling

### LogManager

#### Purpose
Centralized **logging system** with in-memory storage, file logging, and LiveData for UI observation.

#### Key Features

| Feature | Description |
|---------|-------------|
| **In-Memory Storage** | Stores last 500 log entries |
| **File Logging** | Logs to file with date-based rotation |
| **LiveData** | Observable log entries for UI |
| **Log Levels** | DEBUG, INFO, WARN, ERROR |
| **Tag Support** | Per-log tag for filtering |

#### Key Methods

```kotlin
fun init(context: Context)
fun add(message: String, level: String, tag: String)
fun debug(message: String, tag: String)
fun info(message: String, tag: String)
fun warn(message: String, tag: String)
fun error(message: String, tag: String)
fun clear()
fun getEntries(): List<LogEntry>
fun setEnabled(enabled: Boolean)
fun getLogFile(): File?
```

#### LogEntry Model

```kotlin
data class LogEntry(
    val timestampMs: Long,
    val timestamp: String,
    val message: String,
    val level: String,
    val tag: String = "AirMouse"
)
```

### ErrorHandler

#### Purpose
Centralized **error handling** with user-friendly messages and listeners.

#### Key Methods

```kotlin
fun handleError(error: AppError)
fun handleException(throwable: Throwable, type: ErrorType)
fun addErrorListener(listener: (AppError) -> Unit)
fun removeErrorListener(listener: (AppError) -> Unit)
```

---

## 🔐 3. Permission Management

### PermissionHelper

#### Purpose
Comprehensive **permission management** with request handling and rationale display.

#### Key Methods

```kotlin
fun hasRequiredPermissions(context: Context): Boolean
fun hasBluetoothPermissions(context: Context): Boolean
fun hasLocationPermissions(context: Context): Boolean
fun requestPermissions(activity: Activity, requestCode: Int)
fun requestBluetoothPermissions(activity: Activity, requestCode: Int)
fun hasOverlayPermission(context: Context): Boolean
fun requestOverlayPermission(activity: Activity, requestCode: Int)
fun showPermissionDeniedDialog(context: Context, title: String, message: String, onConfirm: (() -> Unit)?)
fun shouldShowRationale(activity: Activity): Boolean
```

### PermissionManager

#### Purpose
Hilt-injected **permission manager** for modern DI-based permission handling.

```kotlin
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(permission: String): Boolean
    fun hasAllPermissions(): Boolean
    fun requestPermissions(activity: Activity, requestCode: Int)
    fun requestBluetoothPermissions(activity: Activity, requestCode: Int)
}
```

---

## 📡 4. Bluetooth & Network

### BluetoothUtils

#### Purpose
Handles **Bluetooth operations** including BLE scanning, device discovery, and connection management.

#### Key Features

| Feature | Description |
|---------|-------------|
| **BLE Scanning** | Scans for Bluetooth LE devices |
| **Bonded Devices** | Lists bonded devices |
| **Permission Checks** | Handles Android 12+ Bluetooth permissions |
| **RSSI Monitoring** | Tracks signal strength |
| **Scan Callback** | Device found and scan complete callbacks |

#### Key Methods

```kotlin
fun isBluetoothEnabled(): Boolean
fun enableBluetooth(): Boolean
fun disableBluetooth(): Boolean
fun getBondedDevices(): List<BluetoothDevice>
fun startScanning(onDeviceFound: (BluetoothDevice, Int) -> Unit, onComplete: () -> Unit)
fun stopScanning()
fun getBluetoothStatus(): BluetoothStatus
```

### NetworkUtils

#### Purpose
Provides **network connectivity utilities**.

#### Key Methods

```kotlin
fun isWifiConnected(context: Context): Boolean
fun getLocalIpAddress(): String?
fun isInternetAvailable(context: Context): Boolean
fun getNetworkType(context: Context): String
```

### NetworkStateHelper

#### Purpose
Hilt-injected **network state monitoring**.

```kotlin
@Singleton
class NetworkStateHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isNetworkAvailable(): Boolean
    fun getNetworkType(): String
    fun isWifiAvailable(): Boolean
}
```

---

## 🔊 5. Audio & Vibration

### VibrateUtils

#### Purpose
Provides **haptic feedback** with predefined vibration patterns.

#### Key Methods

```kotlin
fun vibrateShort()    // 30ms
fun vibrateMedium()   // 50ms
fun vibrateLong()     // 100ms
fun vibrateClick()    // 20ms
fun vibrateDoubleClick() // Pattern: 20ms, 50ms, 20ms
fun vibrateError()    // Pattern: 100ms, 50ms, 100ms
fun vibrateSuccess()  // Pattern: 20ms, 30ms, 50ms
fun cancel()
fun hasVibrator(): Boolean
```

### AudioUtils

#### Purpose
Plays **sound effects** for user interactions.

#### Key Methods

```kotlin
fun playClick()
fun playDoubleClick()
fun playRightClick()
fun playScroll()
fun playConnect()
fun playDisconnect()
fun playError()
fun setEnabled(enabled: Boolean)
fun setVolume(volume: Float)
fun release()
```

---

## 🔋 6. Battery Management

### BatterySaver

#### Purpose
Reduces **sensor sampling rate** when the device is idle to save battery.

#### Key Features

| Feature | Description |
|---------|-------------|
| **Idle Detection** | Monitors device movement |
| **Rate Reduction** | Reduces sampling rate after 10 seconds of inactivity |
| **Auto-Restore** | Restores normal rate on movement |
| **Callback** | Power state change callback |

#### Key Methods

```kotlin
fun start(service: SensorService)
fun stop()
fun isLowPowerMode(): Boolean
fun onMovement()
fun updateMovement(roll: Float, pitch: Float, yaw: Float)
fun setEnabled(enabled: Boolean)
fun setOnPowerStateChange(callback: (Boolean) -> Unit)
```

### BatteryOptimizer

#### Purpose
Provides **battery level monitoring** and optimization recommendations.

```kotlin
@Singleton
class BatteryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class OptimizationLevel {
        PERFORMANCE, BALANCED, POWER_SAVE, ULTRA_POWER_SAVE
    }
    
    fun updateBatteryStatus()
    fun getRecommendedSensorDelay(): Int
}
```

---

## 🔢 7. Math & Conversion

### MathUtils

#### Purpose
Provides **mathematical utility functions**.

#### Key Methods

```kotlin
fun radToDeg(rad: Float): Float
fun degToRad(deg: Float): Float
fun clamp(value: Float, min: Float, max: Float): Float
fun lerp(start: Float, end: Float, t: Float): Float
fun mapRange(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float
fun magnitude(x: Float, y: Float, z: Float): Float
fun normalize(value: Float, min: Float, max: Float): Float
fun smoothStep(edge0: Float, edge1: Float, x: Float): Float
fun exponentialDecay(value: Float, target: Float, decay: Float): Float
```

### ConversionUtils

#### Purpose
Provides **unit conversion utilities**.

#### Key Methods

```kotlin
// Pixels
fun pxToDp(px: Int, density: Float): Int
fun dpToPx(dp: Int, density: Float): Int

// Time
fun msToSeconds(ms: Long): Float
fun formatTime(ms: Long, includeHours: Boolean = true): String

// Bytes
fun bytesToMB(bytes: Long): Float
fun formatBytes(bytes: Long): String

// Angles
fun radToDeg(rad: Double): Double
fun normalizeDeg(angle: Double): Double

// Acceleration
fun mps2ToG(ms2: Float): Float
fun vectorMagnitude3D(x: Float, y: Float, z: Float): Float

// Rotation
fun rotationVectorToEuler(rotationVector: FloatArray): Triple<Double, Double, Double>
fun eulerToRotationMatrix(roll: Float, pitch: Float, yaw: Float): FloatArray
```

---

## 📅 8. Date & Time

### DateUtils

#### Purpose
Provides **date and time formatting utilities**.

#### Key Methods

```kotlin
fun formatDateTime(timestamp: Long): String
fun formatTime(timestamp: Long): String
fun formatShortDate(timestamp: Long): String
fun formatDuration(ms: Long): String
fun getStartOfDay(timestamp: Long): Long
fun getEndOfDay(timestamp: Long): Long
fun isToday(timestamp: Long): Boolean
fun isYesterday(timestamp: Long): Boolean
```

---

## 📁 9. File & JSON

### FileHelper

#### Purpose
Provides **file operations** for reading, writing, and managing files.

#### Key Methods

```kotlin
fun createExportFile(context: Context, fileName: String, extension: String = "json"): File?
fun createExportFileInCache(context: Context, fileName: String, extension: String = "json"): File?
fun writeToFile(file: File, data: String): Boolean
fun readFromFile(file: File): String?
fun copyInputStreamToFile(inputStream: InputStream, file: File): Boolean
fun getFileSize(file: File): Long
fun getFileSizeFormatted(file: File): String
fun deleteFile(file: File): Boolean
fun deleteDirectory(directory: File): Boolean
```

### JsonHelper

#### Purpose
Provides **JSON serialization/deserialization** using Gson.

#### Key Methods

```kotlin
inline fun <reified T> fromJson(json: String): T?
inline fun <reified T> fromJsonList(json: String): List<T>?
fun <T> toJson(obj: T): String?
fun <T> toJsonPretty(obj: T): String?
inline fun <reified T> fromMap(map: Map<String, Any>): T?
fun <T> toMap(obj: T): Map<String, Any>?
```

---

## 📱 10. QR & Bluetooth

### QRScanner

#### Purpose
Handles **QR code scanning** for quick server connection.

#### Key Features

| Feature | Description |
|---------|-------------|
| **Camera Permission** | Handles camera permission request |
| **Scan Result** | Parses QR code data |
| **Connection Data** | Extracts IP, port, protocol, name from QR |
| **Callbacks** | Success, failure, complete callbacks |

#### Key Methods

```kotlin
fun startScan()
fun hasCameraPermission(): Boolean
```

#### QR Data Format

```
airmouse://connect?ip=192.168.1.100&port=8081&name=Air%20Mouse%20Pro&protocol=WEBSOCKET
```

#### ConnectionData Model

```kotlin
data class ConnectionData(
    val ip: String,
    val port: Int,
    val name: String,
    val wsUrl: String? = null,
    val token: String? = null,
    val protocol: String = "WEBSOCKET",
    val useSSL: Boolean = false
)
```

---

## 🎨 11. Animation & UI

### AnimationUtils

#### Purpose
Provides **animation utilities** for both View and Compose.

#### View Animations

```kotlin
fun fadeIn(view: View, duration: Long = 300)
fun fadeOut(view: View, duration: Long = 300, onComplete: (() -> Unit)? = null)
fun slideUp(view: View, duration: Long = 300)
fun slideDown(view: View, duration: Long = 300, startY: Float)
fun pulse(view: View, duration: Long = 200, scale: Float = 1.05f)
fun shake(view: View, duration: Long = 500, intensity: Float = 10f)
```

#### Compose Animations

```kotlin
@Composable
fun infinitePulseAnimation(): InfiniteRepeatableSpec<Float>

@Composable
fun fadeInAnimation(targetState: Boolean): Float

@Composable
fun slideInAnimation(targetState: Boolean): Dp
```

### ColorUtils

#### Purpose
Provides **color manipulation utilities**.

#### Key Methods

```kotlin
fun alphaBlend(foreground: Int, background: Int, alpha: Float): Int
fun isLight(color: Int): Boolean
fun darken(color: Int, factor: Float): Int
fun lighten(color: Int, factor: Float): Int
fun intToHex(color: Int): String
fun hexToInt(hex: String): Int
```

---

## 📊 12. Validation & Strings

### StringUtils

#### Purpose
Provides **string manipulation and validation utilities**.

#### Key Methods

```kotlin
fun isValidIp(ip: String): Boolean
fun isValidPort(port: Int): Boolean
fun truncate(text: String, maxLength: Int): String
fun capitalizeFirst(str: String): String
fun toCamelCase(str: String): String
fun toSnakeCase(str: String): String
```

### ValidationUtils

#### Purpose
Provides **validation utilities** for IP addresses, endpoints, and more.

```kotlin
fun isValidIp(ip: String): Boolean
fun extractIpAddress(value: String): String?
fun parseEndpoint(value: String, defaultPort: Int = 8080): Endpoint?
```

---

## ✅ Utils Package Summary

| Category | Files | Purpose |
|----------|-------|---------|
| **Preferences** | `PreferencesManager`, `PreferencesKeys`, `PreferencesHelper`, `SharedPrefsUtils`, `PreferencesDataStore` | Persistent storage |
| **Logging** | `LogManager`, `ErrorHandler` | Logging and error handling |
| **Permissions** | `PermissionHelper`, `PermissionManager`, `PermissionUIHelper`, `PermissionUtils` | Permission management |
| **Bluetooth** | `BluetoothUtils` | Bluetooth operations |
| **Network** | `NetworkUtils`, `NetworkStateHelper` | Network connectivity |
| **Audio/Vibration** | `VibrateUtils`, `AudioUtils` | Feedback and sound |
| **Battery** | `BatterySaver`, `BatteryOptimizer` | Power management |
| **Math** | `MathUtils`, `ConversionUtils` | Mathematical operations |
| **Date** | `DateUtils` | Date/time formatting |
| **File** | `FileHelper`, `JsonHelper` | File and JSON operations |
| **QR** | `QRScanner` | QR code scanning |
| **Animation** | `AnimationUtils` | Animation utilities |
| **Color** | `ColorUtils` | Color manipulation |
| **Validation** | `StringUtils`, `ValidationUtils` | Validation and string operations |
| **UI** | `DialogHelper`, `ViewHelpers` | UI helpers |
| **Theme** | `ThemeManager` | Theme management |
| **Sensor** | `SensorUtils` | Sensor utilities |
| **Constants** | `AppConstants`, `Constants` | Application constants |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Centralization** | All utilities in one package |
| **Reusability** | Utilities used across the app |
| **Type Safety** | Type-specific methods |
| **Null Safety** | Safe handling of null values |
| **Performance** | Optimized for frequent use |
| **Consistency** | Consistent naming and patterns |
| **Testability** | Most utilities are pure functions |
| **Hilt Integration** | DI-ready utilities |

---

**The Utils Package provides a comprehensive set of helper classes and utilities that serve as the foundation for the entire Air Mouse application.**