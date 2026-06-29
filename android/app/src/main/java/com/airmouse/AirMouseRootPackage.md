# 📘 Air Mouse Root Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse` root package contains the **core application class** and serves as the entry point for the entire Air Mouse application. This package defines the main application configuration, global constants, and core application-level functionality.

```
com.airmouse/
├── AirMouseApplication.kt          # Main application class
├── PreferencesManager.kt           # Preferences interface
├── SensorService.kt                # Sensor service interface
├── BuildConfig.kt                  # Build configuration (generated)
└── di/                             # Dependency injection modules
    ├── AppContainer.kt
    ├── AppModule.kt
    ├── NetworkModule.kt
    ├── DatabaseModule.kt
    ├── SensorModule.kt
    ├── ServiceModule.kt
    ├── RepositoryModule.kt
    ├── UseCaseModule.kt
    ├── FeatureModule.kt
    ├── ViewModelModule.kt
    ├── CalibrationModule.kt
    ├── CoroutineModule.kt
    └── GestureRepositoryModule.kt
```

---

## 🏗️ 1. AirMouseApplication

### Purpose
The **main application class** that initializes the entire application, sets up logging, crash reporting, dependency injection, notification channels, and theme management.

### Key Features

| Feature | Description |
|---------|-------------|
| **Hilt Integration** | `@HiltAndroidApp` for dependency injection |
| **Logging** | Timber integration with debug/release trees |
| **Crash Reporting** | Uncaught exception handling |
| **Theme Management** | Applies theme from preferences |
| **Notification Channels** | Creates all notification channels |
| **Activity Lifecycle** | Foreground/background detection |
| **Strict Mode** | Enabled in debug builds |
| **Performance Monitoring** | Uptime tracking, launch count |
| **Memory Management** | Trim memory handling |

### Application Constants

```kotlin
companion object {
    lateinit var instance: AirMouseApplication
        private set

    fun getAppContext(): Context = instance.applicationContext

    const val APP_VERSION = "3.0.0"
    const val APP_NAME = "Air Mouse Pro"
    const val BUILD_NUMBER = 30
    
    // Preference Keys
    const val PREF_APP_START_TIME = "app_start_time"
    const val PREF_LAST_EXIT_TIME = "last_exit_time"
    const val PREF_TOTAL_UPTIME = "total_uptime"
    const val PREF_APP_LAUNCH_COUNT = "app_launch_count"
    const val PREF_FIRST_LAUNCH = "first_launch"
}
```

### Lifecycle Management

```kotlin
override fun onCreate() {
    super.onCreate()
    instance = this
    appStartTime = System.currentTimeMillis()

    // Initialize core components
    LogManager.init(this)
    initLogging()
    initCrashReporting()
    initPreferences()
    applyTheme()

    // Enable strict mode in debug builds
    if (BuildConfig.DEBUG) {
        enableStrictMode()
    }

    // Register activity lifecycle callbacks
    registerActivityLifecycleCallbacks()

    // Set up uncaught exception handler
    setupUncaughtExceptionHandler()

    // Create notification channels
    createNotificationChannels()

    // Track app launch
    trackAppLaunch()
}
```

### Notification Channels

```kotlin
private fun createNotificationChannels() {
    val channels = listOf(
        NotificationChannel(
            "connection_channel",
            "Connection Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows connection status and network updates"
            setSound(null, null)
            enableVibration(false)
        },
        NotificationChannel(
            "gesture_channel",
            "Gesture Detection",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows gesture detection notifications"
            enableVibration(true)
        },
        NotificationChannel(
            "proximity_channel",
            "Proximity Lock",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows proximity lock/unlock notifications"
            enableVibration(true)
            enableLights(true)
            lightColor = 0xFFFF5722.toInt()
        },
        // ... other channels
    )
    manager.createNotificationChannels(channels)
}
```

### Foreground/Background Detection

```kotlin
private fun registerActivityLifecycleCallbacks() {
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            activeActivities++
            if (!isAppInForeground && activeActivities == 1) {
                isAppInForeground = true
                onAppForeground()
            }
        }
        override fun onActivityStopped(activity: Activity) {
            activeActivities--
            if (isAppInForeground && activeActivities == 0) {
                isAppInForeground = false
                onAppBackground()
            }
        }
        // ... other callbacks
    })
}
```

### Public Methods

```kotlin
fun isAppInForegroundState(): Boolean = isAppInForeground
fun getAppExecutionUptime(): Long = System.currentTimeMillis() - appStartTime
fun getTotalUptime(): Long = prefsManager.getLong(PREF_TOTAL_UPTIME, 0L)
fun getAppLaunchCount(): Int = prefsManager.getInt(PREF_APP_LAUNCH_COUNT, 0)
fun isFirstLaunch(): Boolean = prefsManager.getBoolean(PREF_FIRST_LAUNCH, true)
fun getBuildInfo(): String = "$APP_NAME v$APP_VERSION (Build $BUILD_NUMBER)"
fun getDeviceInfo(): String = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
```

---

## 📱 2. PreferencesManager (Interface)

### Purpose
Defines the **contract** for preference management, providing a unified API for all preference operations.

### Interface Definition

```kotlin
interface PreferencesManager {
    // Basic CRUD
    fun putString(key: String, value: String)
    fun getString(key: String, defaultValue: String = ""): String
    fun putInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putLong(key: String, value: Long)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putFloat(key: String, value: Float)
    fun getFloat(key: String, defaultValue: Float = 0f): Float
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    // Calibration
    fun getGyroBias(): FloatArray
    fun saveGyroBias(values: FloatArray)

    // Gesture
    fun getClickThreshold(): Float
    fun getDoubleClickInterval(): Long
    fun getScrollThreshold(): Float
    fun getScrollDebounce(): Float
    fun getRightClickTilt(): Float
    fun getRightClickDuration(): Long

    // Feedback
    fun isHapticEnabled(): Boolean
}
```

---

## 📡 3. SensorService (Interface)

### Purpose
Defines the **contract** for the sensor service, providing a unified API for sensor management.

### Interface Definition

```kotlin
interface SensorService {
    fun start()
    fun stop()
    fun setSamplingRate(delay: Int)
}
```

---

## 🔧 4. BuildConfig

### Purpose
Provides **build configuration constants** (generated by Gradle during build).

```kotlin
object BuildConfig {
    const val APPLICATION_ID = "com.airmouse"
    const val BUILD_TYPE = "debug" // or "release"
    const val DEBUG = true
    const val VERSION_CODE = 30
    const val VERSION_NAME = "3.0.0"
}
```

---

## 🗂️ 5. DI Package

### AppContainer

#### Purpose
Manual DI container for **non-Hilt components**.

```kotlin
class AppContainer(private val context: Context) {
    private val preferencesManager by lazy { PreferencesManager(context) }
    val connectionManager by lazy { ConnectionManager(context, preferencesManager) }
    
    fun cleanup() {
        connectionManager.cleanup()
    }
}
```

### AppModule

#### Purpose
Core Dagger Hilt module providing **application-wide dependencies**.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
    
    @Provides
    @Singleton
    fun provideVibrator(@ApplicationContext context: Context): Vibrator {
        return context.getSystemService(Vibrator::class.java)
    }
    
    // ... other providers
}
```

### Module Summary

| Module | Purpose | Key Dependencies |
|--------|---------|------------------|
| **AppModule** | Core dependencies | PreferencesManager, Vibrator, BluetoothAdapter, UsbManager |
| **NetworkModule** | Network dependencies | OkHttpClient, ConnectionManager, UdpDiscovery |
| **DatabaseModule** | Room database | AppDatabase, DAOs |
| **SensorModule** | Sensor dependencies | SensorManager, CalibrationHelper |
| **ServiceModule** | Service dependencies | SensorService, PresentationModeService |
| **RepositoryModule** | Repository bindings | All repository implementations |
| **UseCaseModule** | Use case providers | All use cases |
| **FeatureModule** | Feature orchestrators | ConnectionFeature, MouseControlFeature, etc. |
| **ViewModelModule** | ViewModel bindings | All ViewModels |
| **CalibrationModule** | Calibration dependencies | ICalibrationDataSource |
| **CoroutineModule** | Coroutine dispatchers | IoDispatcher, MainDispatcher, DefaultDispatcher |
| **GestureRepositoryModule** | Gesture repository | IGestureRepository, GestureRepositoryImpl |

---

## 📊 Application Initialization Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    APPLICATION INITIALIZATION FLOW                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. Application created                                                │
│         │                                                               │
│         ▼                                                               │
│  2. AirMouseApplication.onCreate()                                     │
│         │                                                               │
│         ├── LogManager.init()                                         │
│         │                                                               │
│         ├── initLogging() → Timber.plant()                            │
│         │                                                               │
│         ├── initCrashReporting()                                       │
│         │                                                               │
│         ├── initPreferences()                                          │
│         │                                                               │
│         ├── applyTheme()                                               │
│         │                                                               │
│         ├── enableStrictMode() (debug only)                           │
│         │                                                               │
│         ├── registerActivityLifecycleCallbacks()                      │
│         │                                                               │
│         ├── setupUncaughtExceptionHandler()                           │
│         │                                                               │
│         ├── createNotificationChannels()                              │
│         │                                                               │
│         └── trackAppLaunch()                                           │
│                                                                         │
│  3. Hilt generates Dagger components                                   │
│         │                                                               │
│         ▼                                                               │
│  4. Activities injected with dependencies                              │
│         │                                                               │
│         ▼                                                               │
│  5. App ready for user interaction                                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 WorkManager Integration

```kotlin
override val workManagerConfiguration: WorkConfiguration
    get() = WorkConfiguration.Builder()
        .setWorkerFactory(workerFactory)
        .setMinimumLoggingLevel(android.util.Log.INFO)
        .build()
```

---

## ✅ Root Package Summary

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **AirMouseApplication** | Main application class | Hilt, logging, crash reporting, theme, notifications |
| **PreferencesManager** | Preferences interface | CRUD operations, calibration, gestures |
| **SensorService** | Sensor service interface | Start, stop, set sampling rate |
| **BuildConfig** | Build configuration | Version, build type, debug flags |
| **AppContainer** | Manual DI container | ConnectionManager, cleanup |
| **AppModule** | Core DI module | Context, PreferencesManager, Vibrator |
| **NetworkModule** | Network DI module | OkHttpClient, ConnectionManager |
| **DatabaseModule** | Database DI module | AppDatabase, DAOs |
| **SensorModule** | Sensor DI module | SensorManager, CalibrationHelper |
| **ServiceModule** | Service DI module | SensorService, PresentationModeService |
| **RepositoryModule** | Repository bindings | All repository implementations |
| **UseCaseModule** | Use case providers | All use cases |
| **FeatureModule** | Feature orchestrators | ConnectionFeature, MouseControlFeature |
| **ViewModelModule** | ViewModel bindings | All ViewModels |
| **CalibrationModule** | Calibration dependencies | ICalibrationDataSource |
| **CoroutineModule** | Coroutine dispatchers | IoDispatcher, MainDispatcher |
| **GestureRepositoryModule** | Gesture repository | IGestureRepository |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Dependency Injection** | Hilt for all dependencies |
| **Separation of Concerns** | Each module handles one domain |
| **Lifecycle Awareness** | Foreground/background detection |
| **Error Handling** | Uncaught exception handler |
| **Logging** | Timber with debug/release trees |
| **Theme Management** | Dynamic theme application |
| **Notification Channels** | Proper channel creation |
| **Performance Monitoring** | Uptime, launch count tracking |
| **Strict Mode** | Debug-only strict mode |

---

**The Root Package serves as the entry point and foundation for the entire Air Mouse application, providing core application configuration, dependency injection, and global services.**