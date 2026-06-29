# 📘 Air Mouse Data Layer – Complete Documentation

## 📁 Package Overview

The `com.airmouse.data` package is the **data layer** of the Air Mouse application, following Clean Architecture principles. It acts as the bridge between the **domain layer** (business logic) and the **infrastructure layer** (database, network, sensors).

```
com.airmouse.data/
├── datasource/
│   ├── local/          # Local storage (Room, Preferences)
│   └── remote/         # Remote storage (Network, Bluetooth, USB)
├── repository/         # Repository implementations
├── mapper/             # Domain ↔ Entity mappers
└── helpers/            # Helper classes (SensorRepository, PreferencesDataStore)
```

---

## 🏗️ Architecture Overview

### Layer Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                            │
│                    (Compose Screens / ViewModels)                     │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                                 │
│                    (Models / Use Cases / Interfaces)                  │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            DATA LAYER                                  │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │                      com.airmouse.data                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │   │
│  │  │  repository/ │  │  datasource/ │  │      mapper/         │ │   │
│  │  │  (Impls)     │  │  local/      │  │  (Domain ↔ Entity)   │ │   │
│  │  │              │  │  remote/     │  │                      │ │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘ │   │
│  │  ┌──────────────┐  ┌──────────────┐                          │   │
│  │  │   helpers/   │  │  interfaces/ │                          │   │
│  │  │  (Flows)     │  │  (Contracts) │                          │   │
│  │  └──────────────┘  └──────────────┘                          │   │
│  └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER                           │
│            (Room Database / Preferences / Network / Sensors)          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 1. Repository Layer (`repository/`)

### Overview

The repository layer contains **11 repository implementations** that implement the domain repository interfaces. Each repository orchestrates data operations across multiple data sources.

### Repository List

| Repository | Domain Interface | Data Sources |
|------------|------------------|--------------|
| `CalibrationRepositoryImpl` | `ICalibrationRepository` | `ICalibrationDataSource`, `CalibrationHelper` |
| `ConnectionRepositoryImpl` | `IConnectionRepository` | `IConnectionDataSource` |
| `GestureRepositoryImpl` | `IGestureRepository` | `IGestureDataSource`, `EnhancedGestureDetector` |
| `MouseRepositoryImpl` | `IMouseRepository` | `ConnectionManager` |
| `ProfileRepositoryImpl` | `IProfileRepository` | `IProfileDataSource` |
| `ProximityRepositoryImpl` | `IProximityRepository` | `BluetoothAdapter`, `ConnectionManager` |
| `SensorRepositoryImpl` | `ISensorRepository` | `SensorManager`, `CalibrationHelper` |
| `SettingsRepositoryImpl` | `ISettingsRepository` | `IPreferencesDataSource` |
| `StatisticsRepositoryImpl` | `IStatisticsRepository` | `IStatisticsDataSource` |
| `UpdateRepositoryImpl` | `IUpdateRepository` | `IPreferencesDataSource` |
| `VoiceCommandRepositoryImpl` | `IVoiceCommandRepository` | `IPreferencesDataSource` |

### Repository Implementation Pattern

Each repository follows this pattern:

```kotlin
@Singleton
class XxxRepositoryImpl @Inject constructor(
    private val dataSource: IXxxDataSource,
    // ... other dependencies
) : IXxxRepository {

    // State flows for reactive UI updates
    private val _state = MutableStateFlow(initialState)
    override fun observeState(): Flow<State> = _state.asStateFlow()

    // Business logic methods
    override suspend fun doSomething(params: Params): Result {
        return try {
            val result = dataSource.getData(params)
            _state.update { it.copy(data = result) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 💾 2. Data Source Layer (`datasource/`)

### 2.1 Local Data Sources (`datasource/local/`)

#### Purpose
Handles **persistent storage** on the device using Room Database and SharedPreferences.

#### Data Source Interfaces

| Interface | Purpose |
|-----------|---------|
| `ICalibrationDataSource` | Calibration data (gyro, accel, mag offsets/scales) |
| `IGestureDataSource` | Gesture templates, training samples, statistics |
| `IProfileDataSource` | User profiles, settings, favorites |
| `IStatisticsDataSource` | Session, daily, historical statistics |
| `IPreferencesDataSource` | Generic preference operations |
| `ILocalDataSource` | Unified interface over all local data |

#### Room Database (`database/`)

##### Entities (8 tables)

| Entity | Purpose | Key Fields |
|--------|---------|------------|
| `CalibrationEntity` | Sensor calibration data | gyroBias, accelOffset, magOffset |
| `ProfileEntity` | User profiles | name, sensitivity, theme, settings |
| `GestureTemplateEntity` | Gesture templates | name, type, action, confidence |
| `TrainingSampleEntity` | ML training data | gyro, accel, mag samples |
| `StatisticsEntity` | Session statistics | clicks, scrolls, movement |
| `DailyStatsEntity` | Daily aggregated stats | date, clicks, gestures |
| `GestureStatsEntity` | Per-gesture stats | gesture_name, count, confidence |
| `SettingsEntity` | App settings | key, value |

##### Data Access Objects (DAOs)

| DAO | Purpose |
|-----|---------|
| `CalibrationDao` | CRUD for calibration data |
| `ProfileDao` | CRUD for profiles, default/favorite operations |
| `GestureDao` | CRUD for gestures, favorites, increment detection |
| `TrainingSampleDao` | CRUD for training samples |
| `StatisticsDao` | CRUD for statistics |
| `DailyStatsDao` | CRUD for daily stats |
| `GestureStatsDao` | CRUD for gesture stats |
| `SettingsDao` | CRUD for settings |

##### Mappers (`mapper/`)

| Mapper | Purpose |
|--------|---------|
| `DomainToEntityMapper` | Domain models → Room entities |
| `EntityToDomainMapper` | Room entities → Domain models |

#### Helper Models (`models/`)

| Model | Purpose |
|-------|---------|
| `CalibrationPrefsData` | Calibration data stored in preferences |
| `GestureData` | Gesture data for serialization |
| `SensorData` | Raw sensor data |
| `Quadruple` | Generic quadruple holder |
| `GestureTypeCount` | Gesture type count |

#### Type Converters (`Converters.kt`)

Converts complex types for Room:
- `List<String>` ↔ JSON string
- `FloatArray` ↔ JSON string
- `List<FloatArray>` ↔ JSON string
- `GestureType` ↔ String
- `CalibrationPrefsData` ↔ JSON string
- `SensorData` ↔ JSON string
- `GestureData` ↔ JSON string

#### Data Source Implementations

| Implementation | Storage | Format |
|----------------|---------|--------|
| `CalibrationDataSourceImpl` | SharedPreferences | Individual keys |
| `GestureDataSourceImpl` | SharedPreferences | JSON arrays |
| `ProfileDataSourceImpl` | SharedPreferences | JSON arrays |
| `StatisticsDataSourceImpl` | SharedPreferences | JSON objects |
| `PreferencesDataSourceImpl` | SharedPreferences | Type-safe keys |
| `LocalDataSourceImpl` | Room + Preferences | Entities |

### 2.2 Remote Data Sources (`datasource/remote/`)

#### Purpose
Handles **external communication** with the PC server, Bluetooth devices, USB, and WebSocket endpoints.

#### Data Source Interfaces

| Interface | Purpose |
|-----------|---------|
| `IConnectionDataSource` | Network communication (WebSocket/TCP/UDP) |
| `IBluetoothDataSource` | Bluetooth LE communication |
| `IUsbDataSource` | USB communication |
| `IWebSocketDataSource` | WebSocket communication (legacy) |

#### Implementation Details

| Implementation | Dependency | Protocol |
|----------------|------------|----------|
| `ConnectionDataSourceImpl` | `ConnectionManager`, `UdpDiscovery` | WebSocket/TCP/UDP |
| `BluetoothDataSourceImpl` | `BluetoothManager`, `BluetoothLeScanner` | BLE |
| `UsbDataSourceImpl` | `UsbManager` | USB HID |
| `WebSocketDataSourceImpl` | `WebSocketManager` | WebSocket (legacy) |

---

## 🔄 3. Data Flow Examples

### Example 1: Calibration Data Flow

```
User taps "Calibrate"
        ↓
CalibrationViewModel.startCalibration()
        ↓
CalibrationUseCase.startFullCalibration()
        ↓
CalibrationRepositoryImpl.calibrateGyroscope()
        ↓
CalibrationDataSourceImpl.saveGyroBias(x, y, z)
        ↓
PreferencesManager.putFloat("gyro_bias_x", x)
        ↓
SharedPreferences (persisted)
```

### Example 2: Gesture Detection Flow

```
User performs gesture
        ↓
EnhancedGestureDetector detects gesture
        ↓
GestureRepositoryImpl.detectGesture(sensorData)
        ↓
GestureDataSourceImpl.incrementGestureCount(gesture, confidence)
        ↓
PreferencesManager.update statistics
        ↓
UI observes StateFlow update
```

### Example 3: Profile Management Flow

```
User creates profile
        ↓
ProfilesViewModel.createProfile(name)
        ↓
ManageProfileUseCase(profile)
        ↓
ProfileRepositoryImpl.createProfile(profile)
        ↓
ProfileDataSourceImpl.saveProfile(profile)
        ↓
Room Database (ProfileEntity)
        ↓
ProfilesScreen recomposes with new profile
```

### Example 4: Connection Flow

```
User enters IP and taps Connect
        ↓
HomeViewModel.connect()
        ↓
ConnectToServerUseCase(config)
        ↓
ConnectionRepositoryImpl.connect(config)
        ↓
ConnectionDataSourceImpl.connect(ip, port)
        ↓
ConnectionManager.connect(ip, port)
        ↓
WebSocket/TCP/UDP connection established
        ↓
StateFlow updates connection status
        ↓
HomeScreen shows "Connected" status
```

---

## 🔗 4. Dependency Injection (Hilt)

### Module Structure

```kotlin
// RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCalibrationRepository(
        impl: CalibrationRepositoryImpl
    ): ICalibrationRepository
    
    // ... all other repository bindings
}

// DataSourceModule.kt (local)
@Module
@InstallIn(SingletonComponent::class)
object LocalDataSourceModule {
    @Provides
    @Singleton
    fun provideCalibrationDataSource(
        prefs: PreferencesManager
    ): ICalibrationDataSource = CalibrationDataSourceImpl(prefs)
    
    // ... all other data source providers
}

// DataSourceModule.kt (remote)
@Module
@InstallIn(SingletonComponent::class)
object RemoteDataSourceModule {
    @Provides
    @Singleton
    fun provideConnectionDataSource(
        connectionManager: ConnectionManager,
        udpDiscovery: UdpDiscovery
    ): IConnectionDataSource = ConnectionDataSourceImpl(connectionManager, udpDiscovery)
}
```

### Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│                      DI CONTAINER (Hilt)                           │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
        ▼                        ▼                        ▼
┌───────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│ Repositories  │    │   Data Sources      │    │   Infrastructure    │
│ (Singleton)   │    │   (Singleton)       │    │   (Singleton)       │
│               │    │                     │    │                     │
│ • Calibration │───▶│ • CalibrationDS     │───▶│ • PreferencesMgr    │
│ • Connection  │───▶│ • ConnectionDS      │───▶│ • ConnectionMgr     │
│ • Gesture     │───▶│ • GestureDS         │───▶│ • SensorManager     │
│ • Mouse       │───▶│ • ProfileDS         │───▶│ • BluetoothAdapter  │
│ • Profile     │───▶│ • StatisticsDS      │───▶│ • Room Database     │
│ • Proximity   │───▶│ • PreferencesDS     │    │ • UdpDiscovery      │
│ • Sensor      │───▶│ • LocalDS           │    └─────────────────────┘
│ • Settings    │───▶│ • RemoteDS          │
│ • Statistics  │    └─────────────────────┘
│ • Update      │
│ • Voice       │
└───────────────┘
```

---

## 📊 5. Data Source Comparison

| Data Source | Interface | Implementation | Storage | Performance | Use Case |
|-------------|-----------|----------------|---------|-------------|----------|
| **Calibration** | `ICalibrationDataSource` | `CalibrationDataSourceImpl` | Preferences | Fast | Sensor calibration |
| **Gesture** | `IGestureDataSource` | `GestureDataSourceImpl` | Preferences (JSON) | Medium | Gesture templates |
| **Profile** | `IProfileDataSource` | `ProfileDataSourceImpl` | Preferences (JSON) | Medium | User profiles |
| **Statistics** | `IStatisticsDataSource` | `StatisticsDataSourceImpl` | Preferences (JSON) | Medium | Usage stats |
| **Preferences** | `IPreferencesDataSource` | `PreferencesDataSourceImpl` | Preferences | Fast | App settings |
| **Local** | `ILocalDataSource` | `LocalDataSourceImpl` | Room + Preferences | Medium | Unified storage |
| **Connection** | `IConnectionDataSource` | `ConnectionDataSourceImpl` | Network | N/A | Server communication |
| **Bluetooth** | `IBluetoothDataSource` | `BluetoothDataSourceImpl` | Network (BLE) | N/A | Proximity detection |
| **USB** | `IUsbDataSource` | `UsbDataSourceImpl` | Network (USB) | N/A | USB HID mode |
| **WebSocket** | `IWebSocketDataSource` | `WebSocketDataSourceImpl` | Network | N/A | Legacy WebSocket |

---

## ✅ Summary

### Data Layer Architecture Principles

| Principle | Implementation |
|-----------|----------------|
| **Separation of Concerns** | Each data source handles one domain |
| **Dependency Inversion** | Repositories depend on abstractions, not concretions |
| **Single Responsibility** | Each class has one reason to change |
| **Interface Segregation** | Separate interfaces for each data type |
| **Reactive Programming** | `StateFlow` and `Flow` for reactive data streams |
| **Testability** | All dependencies are interfaces, easily mocked |
| **Persistence** | Room for structured data, Preferences for key-value |

### Data Flow Direction

```
UI → ViewModel → UseCase → Repository → DataSource → Storage
                                ↓
                        StateFlow (reactive updates)
                                ↓
                              UI
```

### Key Benefits

1. **Maintainability**: Clear separation of concerns
2. **Testability**: Every component can be unit tested
3. **Flexibility**: Data sources can be swapped without affecting domain
4. **Performance**: Room with coroutines for async operations
5. **Consistency**: Single source of truth for each data type

---

**This data layer provides a robust, scalable, and maintainable foundation for the Air Mouse application, handling all data persistence and retrieval needs with clean separation of concerns.**