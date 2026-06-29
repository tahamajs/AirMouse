# 📘 Air Mouse Domain Layer – Complete Documentation

## 📁 Package Overview

The `com.airmouse.domain` package is the **core business logic layer** of the Air Mouse application. Following **Clean Architecture** principles, this layer is **independent of Android frameworks** and contains only pure Kotlin code. It defines **what the application does** (use cases) and **what data it works with** (models), without knowing **how** the data is stored or retrieved.

```
com.airmouse.domain/
├── model/           # Domain entities & data classes (50+ files)
├── repository/      # Repository interfaces (11 files)
├── usecase/         # Business logic use cases (16 files)
└── feature/         # Feature orchestrators (10 files)
```

---

## 🏛️ Architecture Overview

### Clean Architecture – Domain Layer Position

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
│                           DOMAIN LAYER  ←── YOU ARE HERE              │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     WHAT THE APP DOES                            │   │
│  │  ┌───────────────┐  ┌───────────────┐  ┌─────────────────────┐ │   │
│  │  │    Use Cases  │  │   Repositories│  │      Models         │ │   │
│  │  │  (Business    │  │  (Contracts)  │  │   (Entities)        │ │   │
│  │  │   Logic)      │  │               │  │                     │ │   │
│  │  └───────────────┘  └───────────────┘  └─────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│                    INDEPENDENT OF FRAMEWORKS                            │
│                     (Pure Kotlin / No Android)                         │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            DATA LAYER                                  │
│               (Repository Implementations / Data Sources)              │
│                                                                         │
│                         DEPENDS ON                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

### Key Principles

| Principle | How It's Applied |
|-----------|------------------|
| **Independence** | No Android dependencies – pure Kotlin |
| **Testability** | All classes are easily unit-testable |
| **Separation** | Business logic is isolated from UI and data |
| **Single Responsibility** | Each class has one clear purpose |
| **Reusability** | Use cases can be composed and reused |
| **Dependency Inversion** | Depends on abstractions, not concretions |

---

## 📦 1. Domain Models (`model/`)

### Purpose
Define the **core business entities** of the application. These are pure data classes with no logic (or minimal utility methods).

### Category Breakdown

| Category | Models | Purpose |
|----------|--------|---------|
| **Calibration** | `CalibrationStatus`, `CalibrationQuality`, `CalibrationData`, `SensorCalibrationData`, `CalibrationProgress` | Sensor calibration data and state |
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

### Model Example

```kotlin
// CalibrationModels.kt
enum class CalibrationStatus {
    NOT_STARTED, IN_PROGRESS, GYRO_COMPLETE,
    MAG_COMPLETE, ACCEL_COMPLETE, COMPLETED,
    FAILED, SKIPPED, IDLE
}

enum class CalibrationQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}

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
}

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
}
```

### Model Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Immutability** | All models are `data class` with `copy()` for updates |
| **Serialization** | `@Parcelize` for Android, `toJson()` for network |
| **Type Safety** | Enums for status, quality, and types |
| **Default Values** | Sensible defaults for all fields |
| **Validation** | `isValid()` methods where needed |
| **Domain Richness** | Utility methods on models (e.g., `isCalibrated()`) |

---

## 📂 2. Repository Interfaces (`repository/`)

### Purpose
Define **contracts** for data operations. Repositories abstract the data sources (Room, Preferences, Network) and provide a clean API for the domain layer.

### Repository List

| Repository | Purpose | Key Methods |
|------------|---------|-------------|
| `ICalibrationRepository` | Sensor calibration | `calibrateGyroscope()`, `getCalibrationData()`, `resetCalibration()` |
| `IConnectionRepository` | Network connection | `connect()`, `disconnect()`, `sendMove()`, `sendClick()` |
| `IGestureRepository` | Gesture detection | `detectGesture()`, `addCustomGesture()`, `trainGesture()` |
| `IMouseRepository` | Cursor control | `move()`, `click()`, `scroll()`, `getMovementProfile()` |
| `IProfileRepository` | User profiles | `createProfile()`, `updateProfile()`, `deleteProfile()` |
| `IProximityRepository` | Proximity detection | `startMonitoring()`, `getProximityState()`, `calibrate()` |
| `ISensorRepository` | Sensor data | `observeSensorData()`, `startSensors()`, `calibrateSensors()` |
| `ISettingsRepository` | App settings | `getSensitivity()`, `setTheme()`, `resetAllSettings()` |
| `IStatisticsRepository` | Usage statistics | `recordClick()`, `getCurrentSession()`, `exportStats()` |
| `IUpdateRepository` | App updates | `checkForUpdates()`, `downloadUpdate()`, `installUpdate()` |
| `IVoiceCommandRepository` | Voice commands | `processVoiceInput()`, `addCommand()`, `startListening()` |

### Repository Interface Example

```kotlin
interface ICalibrationRepository {
    
    // Status & Progress
    suspend fun getCalibrationStatus(): CalibrationStatus
    fun observeCalibrationStatus(): Flow<CalibrationStatus>
    suspend fun getCalibrationProgress(): Int
    fun observeCalibrationProgress(): Flow<Int>
    fun observeCalibrationQuality(): Flow<CalibrationQuality>
    
    // Gyroscope
    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean
    suspend fun getGyroBias(): GyroBias
    suspend fun saveGyroBias(bias: GyroBias)
    
    // Magnetometer
    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean
    suspend fun getMagOffset(): SensorCalibrationData
    suspend fun saveMagOffset(data: SensorCalibrationData)
    
    // Accelerometer
    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Boolean
    suspend fun getAccelOffset(): SensorCalibrationData
    suspend fun saveAccelOffset(data: SensorCalibrationData)
    
    // Full Calibration Data
    suspend fun getCalibrationData(): CalibrationData
    suspend fun saveCalibrationData(data: CalibrationData)
    suspend fun getCalibrationQuality(): CalibrationQuality
    
    // Reset
    suspend fun resetCalibration()
    suspend fun resetAllCalibration()
    suspend fun updateCalibrationStatus(status: CalibrationStatus)
    suspend fun updateCalibrationQuality(quality: CalibrationQuality)
    suspend fun updateCalibrationProgress(progress: Int)
}
```

### Repository Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Interface Segregation** | Separate interfaces for each domain |
| **Reactive Support** | `Flow` and `StateFlow` for real-time updates |
| **Suspend Functions** | All operations are `suspend` for coroutines |
| **Clear Naming** | Method names clearly describe their purpose |
| **No Implementation** | Only contracts, no implementation details |

---

## ⚙️ 3. Use Cases (`usecase/`)

### Purpose
Encapsulate **business logic**. Each use case represents a single, atomic business rule that the application can perform.

### Use Case List

| Use Case | Purpose | Key Method |
|----------|---------|------------|
| `CalibrationUseCase` | Sensor calibration orchestration | `startFullCalibration()` |
| `ConnectToServerUseCase` | Server connection | `connect()` |
| `SendMovementUseCase` | Cursor movement | `sendMove()` |
| `DetectGestureUseCase` | Gesture detection | `detect()` |
| `DiscoverServersUseCase` | UDP server discovery | `startDiscovery()` |
| `GetConnectionStatusUseCase` | Connection status | `get()` |
| `GetStatisticsUseCase` | Statistics retrieval | `getSessionStats()` |
| `RecordStatisticsUseCase` | Statistics recording | `recordClick()` |
| `ManageProfileUseCase` | Profile management | `createProfile()` |
| `ManageGestureTemplatesUseCase` | Gesture templates | `addTemplate()` |
| `HandleVoiceCommandUseCase` | Voice commands | `process()` |
| `GetProximityStateUseCase` | Proximity state | `get()` |
| `UpdateProximityConfigUseCase` | Proximity configuration | `update()` |
| `TestConnectionUseCase` | Connection testing | `test()` |
| `CheckForUpdatesUseCase` | Update checking | `check()` |
| `GetGestureStatisticsUseCase` | Gesture statistics | `get()` |

### Use Case Pattern

Each use case follows a consistent pattern:

```kotlin
class XxxUseCase @Inject constructor(
    private val repository: IXxxRepository
) {
    
    // Primary operation (operator invoke)
    suspend operator fun invoke(params: Params): Result<ReturnType> {
        return try {
            val result = repository.doSomething(params)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Additional helper methods if needed
    suspend fun helperMethod(): ReturnType {
        return repository.doOtherThing()
    }
}
```

### Use Case Example

```kotlin
class CalibrationUseCase @Inject constructor(
    private val calibrationRepository: ICalibrationRepository
) {

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

    suspend fun getCalibrationStatus(): CalibrationStatus {
        return calibrationRepository.getCalibrationStatus()
    }

    fun observeCalibrationStatus(): Flow<CalibrationStatus> {
        return calibrationRepository.observeCalibrationStatus()
    }

    suspend fun resetCalibration(): Result<Unit> {
        return try {
            calibrationRepository.resetCalibration()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Use Case Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Single Responsibility** | One use case = one business operation |
| **Result Wrapper** | Returns `Result<T>` for error handling |
| **Orchestration** | Coordinates multiple repository calls |
| **Reactive Support** | `Flow` for real-time data streams |
| **Testability** | Easy to unit test with mocked repositories |
| **Clear Naming** | Use case name describes the action |

---

## 🔗 4. Features (`feature/`)

### Purpose
**Orchestrate** multiple use cases into cohesive features. Features are higher-level components that combine related use cases for use by ViewModels.

### Feature List

| Feature | Use Cases | Purpose |
|---------|-----------|---------|
| `ConnectionFeature` | `ConnectToServerUseCase`, `DiscoverServersUseCase`, `GetConnectionStatusUseCase`, `TestConnectionUseCase` | Complete connection management |
| `MouseControlFeature` | `SendMovementUseCase`, `IMouseRepository` | Mouse control |
| `CalibrationFeature` | `CalibrationUseCase` | Sensor calibration |
| `GestureRecognitionFeature` | `DetectGestureUseCase`, `ManageGestureTemplatesUseCase` | Gesture detection and training |
| `ProximityFeature` | `GetProximityStateUseCase`, `UpdateProximityConfigUseCase` | Proximity detection |
| `StatisticsFeature` | `GetStatisticsUseCase`, `RecordStatisticsUseCase` | Usage statistics |
| `VoiceFeature` | `HandleVoiceCommandUseCase` | Voice commands |
| `ProfileFeature` | `ManageProfileUseCase` | User profiles |
| `UpdateFeature` | `CheckForUpdatesUseCase` | App updates |
| `SensorFeature` | `ISensorRepository` | Sensor data |

### Feature Example

```kotlin
class ConnectionFeature @Inject constructor(
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val discoverServersUseCase: DiscoverServersUseCase,
    private val getConnectionStatusUseCase: GetConnectionStatusUseCase,
    private val testConnectionUseCase: TestConnectionUseCase
) {

    // Connection
    suspend fun connect(config: ConnectionConfig): Result<Boolean> {
        return connectToServerUseCase(config)
    }

    suspend fun disconnect() {
        connectToServerUseCase.disconnect()
    }

    suspend fun reconnect(): Result<Boolean> {
        return connectToServerUseCase.reconnect()
    }

    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return getConnectionStatusUseCase.observeConnectionStatus()
    }

    // Discovery
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit): Result<Unit> {
        return discoverServersUseCase.startDiscovery(onServerFound)
    }

    suspend fun stopDiscovery(): Result<Unit> {
        return discoverServersUseCase.stopDiscovery()
    }

    suspend fun getDiscoveredServers(): List<DiscoveredServer> {
        return discoverServersUseCase.getDiscoveredServers()
    }

    // Testing
    suspend fun testConnection(ip: String, port: Int): Result<TestResult> {
        return testConnectionUseCase(ip, port)
    }

    suspend fun ping(): Result<Long> {
        return testConnectionUseCase.ping()
    }
}
```

---

## 🔄 Data Flow Through Domain Layer

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         USER ACTION                                    │
│                    (Click Button / Gesture / Voice)                    │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         VIEW MODEL                                     │
│                  (Compose ViewModel / UI Layer)                        │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          USE CASE                                      │
│                    (Business Logic Layer)                              │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     USE CASE LOGIC                               │   │
│  │  1. Validate input                                               │   │
│  │  2. Call repository methods                                      │   │
│  │  3. Process results                                              │   │
│  │  4. Return Result<T>                                             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       REPOSITORY INTERFACE                             │
│                      (Domain Contract)                                 │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    REPOSITORY IMPLEMENTATION                           │
│                      (Data Layer)                                      │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATA SOURCE                                    │
│              (Room / Preferences / Network)                            │
└─────────────────────────────────────────────────────────────────────────┘
```

### Example Flow: Connect to Server

```
1. User taps "Connect" button
        ↓
2. HomeViewModel.connect()
        ↓
3. ConnectToServerUseCase(config)
        ↓
4. ConnectionRepository.connect(config)
        ↓
5. ConnectionDataSourceImpl.connect(ip, port)
        ↓
6. ConnectionManager.connect(ip, port)
        ↓
7. WebSocket/TCP/UDP connection
        ↓
8. ConnectionManager emits status update
        ↓
9. ConnectionRepository observes status change
        ↓
10. ConnectToServerUseCase returns Result<Boolean>
        ↓
11. HomeViewModel updates UI state
        ↓
12. HomeScreen shows "Connected" status
```

---

## 📊 Domain Layer Statistics

### File Count Summary

| Package | Files | Lines of Code (approx) |
|---------|-------|------------------------|
| `model/` | 50+ | ~3,000 |
| `repository/` | 11 | ~800 |
| `usecase/` | 16 | ~2,000 |
| `feature/` | 10 | ~600 |
| **Total** | **87+** | **~6,400** |

### Dependency Matrix

| Layer | Depends On | Depended By |
|-------|------------|-------------|
| **Models** | None | All other packages |
| **Repository Interfaces** | Models | Use Cases, Data Layer |
| **Use Cases** | Repository Interfaces, Models | Features, ViewModels |
| **Features** | Use Cases, Repository Interfaces | ViewModels |

---

## 🎯 Key Design Patterns

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Repository Pattern** | `repository/` | Abstracts data sources |
| **Use Case Pattern** | `usecase/` | Encapsulates business logic |
| **Dependency Injection** | Constructor injection | Manages dependencies |
| **Observer Pattern** | `Flow` / `StateFlow` | Reactive data streams |
| **Result Wrapper** | `Result<T>` | Error handling |
| **Factory Pattern** | Use case constructors | Creates use cases with dependencies |
| **Strategy Pattern** | Different use cases | Encapsulates different algorithms |

---

## ✅ Domain Layer Summary

| Aspect | Description |
|--------|-------------|
| **Purpose** | Core business logic and data definitions |
| **Independence** | No Android dependencies – pure Kotlin |
| **Components** | Models, Repository Interfaces, Use Cases, Features |
| **Communication** | Use Cases → Repositories → Data Layer |
| **Reactive** | Flow and StateFlow for real-time updates |
| **Testability** | All components easily unit-testable |
| **Reusability** | Use cases can be composed and reused |
| **Separation** | Business logic is isolated from UI and data |

---

**The Domain Layer forms the heart of the Air Mouse application, encapsulating all business logic and data definitions in a clean, testable, and framework-independent way.**