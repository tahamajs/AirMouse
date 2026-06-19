# Air Mouse Android App - Complete Architecture & Folder Structure Documentation

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Package Structure](#package-structure)
4. [Module Breakdown](#module-breakdown)
5. [Data Flow](#data-flow)
6. [Layer Diagrams](#layer-diagrams)
7. [Key Components](#key-components)
8. [Dependency Injection](#dependency-injection)
9. [Testing Strategy](#testing-strategy)
10. [Build Variants](#build-variants)
11. [Third-Party Libraries](#third-party-libraries)
12. [Development Guidelines](#development-guidelines)

---

## Overview

The Air Mouse Android app follows **Clean Architecture** principles with a modern **MVVM (Model-View-ViewModel)** pattern using **Jetpack Compose** for UI. The architecture is designed to be:

- **Testable** - Each layer is independently testable
- **Maintainable** - Clear separation of concerns
- **Scalable** - Easy to add new features
- **Decoupled** - Low coupling between layers

---

## Architecture

### Layer Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                          │
│                    (UI / Compose Screens)                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Activities / Composables / ViewModels                     │   │
│  │  - HomeScreen, CalibrationScreen, SettingsScreen           │   │
│  │  - HomeViewModel, CalibrationViewModel                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                          DOMAIN LAYER                              │
│                    (Business Logic)                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Use Cases / Models / Repository Interfaces                │   │
│  │  - ConnectToServerUseCase, SendMovementUseCase             │   │
│  │  - IConnectionRepository, IMouseRepository                 │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                          DATA LAYER                                │
│                    (Data Sources & Repositories)                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Repositories / Data Sources / Mappers                     │   │
│  │  - ConnectionRepositoryImpl, CalibrationDataSourceImpl     │   │
│  │  - DomainToEntityMapper, EntityToDomainMapper              │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                      INFRASTRUCTURE LAYER                          │
│                    (External Dependencies)                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Network / Database / Sensors / Bluetooth / USB            │   │
│  │  - ConnectionManager, WebSocketManager                     │   │
│  │  - Room Database, PreferencesDataStore                     │   │
│  │  - SensorService, BluetoothHidService                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### Dependency Direction

```
Presentation → Domain ← Data → Infrastructure
     ↓           ↓        ↓          ↓
     └───────────┴────────┴──────────┘
              (Dependency Injection)
```

---

## Package Structure

### Complete Directory Tree

```
app/src/main/java/com/airmouse/
│
├── AirMouseApplication.kt              # Application entry point
│
├── auth/                                # Authentication
│   └── AuthManager.kt
│
├── data/                                # Data Layer
│   ├── datasource/                      # Data sources
│   │   ├── local/                       # Local data sources
│   │   │   ├── AppDatabase.kt
│   │   │   ├── Converters.kt
│   │   │   ├── CalibrationDao.kt
│   │   │   ├── GestureDao.kt
│   │   │   ├── ProfileDao.kt
│   │   │   ├── StatisticsDao.kt
│   │   │   ├── TrainingSampleDao.kt
│   │   │   ├── CalibrationEntity.kt
│   │   │   ├── GestureTemplateEntity.kt
│   │   │   ├── ProfileEntity.kt
│   │   │   ├── StatisticsEntity.kt
│   │   │   ├── TrainingSampleEntity.kt
│   │   │   ├── ILocalDataSource.kt
│   │   │   └── LocalDataSourceImpl.kt
│   │   │
│   │   └── remote/                     # Remote data sources
│   │       ├── IConnectionDataSource.kt
│   │       └── ConnectionDataSourceImpl.kt
│   │
│   ├── mapper/                         # Data mappers
│   │   ├── DomainToEntityMapper.kt
│   │   └── EntityToDomainMapper.kt
│   │
│   └── repository/                     # Repository implementations
│       ├── CalibrationRepositoryImpl.kt
│       ├── ConnectionRepositoryImpl.kt
│       ├── GestureRepositoryImpl.kt
│       ├── MouseRepositoryImpl.kt
│       ├── ProfileRepositoryImpl.kt
│       ├── ProximityRepositoryImpl.kt
│       ├── SensorRepositoryImpl.kt
│       ├── StatisticsRepositoryImpl.kt
│       └── VoiceCommandRepositoryImpl.kt
│
├── domain/                             # Domain Layer
│   ├── model/                          # Domain models
│   │   ├── ConnectionModels.kt
│   │   ├── MouseModels.kt
│   │   ├── GestureModels.kt
│   │   ├── CalibrationModels.kt
│   │   ├── SensorModels.kt
│   │   ├── ProximityModels.kt
│   │   ├── UpdateModels.kt
│   │   ├── UserProfileModels.kt
│   │   ├── StatisticsModels.kt
│   │   ├── VoiceCommandModels.kt
│   │   ├── ErrorModels.kt
│   │   └── UIStateModels.kt
│   │
│   ├── repository/                     # Repository interfaces
│   │   ├── ICalibrationRepository.kt
│   │   ├── IConnectionRepository.kt
│   │   ├── IGestureRepository.kt
│   │   ├── IMouseRepository.kt
│   │   ├── IProfileRepository.kt
│   │   ├── IProximityRepository.kt
│   │   ├── ISensorRepository.kt
│   │   ├── IStatisticsRepository.kt
│   │   ├── IUpdateRepository.kt
│   │   └── IVoiceCommandRepository.kt
│   │
│   └── usecase/                        # Use cases
│       ├── ConnectToServerUseCase.kt
│       ├── SendMovementUseCase.kt
│       ├── CalibrationUseCase.kt
│       ├── DetectGestureUseCase.kt
│       ├── ManageGestureTemplatesUseCase.kt
│       ├── GetProximityStateUseCase.kt
│       ├── UpdateProximityConfigUseCase.kt
│       ├── GetStatisticsUseCase.kt
│       ├── RecordStatisticsUseCase.kt
│       ├── ManageProfileUseCase.kt
│       ├── HandleVoiceCommandUseCase.kt
│       ├── CheckForUpdatesUseCase.kt
│       ├── DiscoverServersUseCase.kt
│       ├── TestConnectionUseCase.kt
│       └── GetConnectionStatusUseCase.kt
│
├── features/                           # Feature orchestration
│   ├── ConnectionFeature.kt
│   ├── MouseControlFeature.kt
│   ├── CalibrationFeature.kt
│   ├── GestureRecognitionFeature.kt
│   ├── ProximityFeature.kt
│   ├── StatisticsFeature.kt
│   ├── VoiceFeature.kt
│   ├── ProfileFeature.kt
│   ├── UpdateFeature.kt
│   └── SensorFeature.kt
│
├── network/                            # Network layer
│   ├── ConnectionManager.kt
│   ├── WebSocketManager.kt
│   ├── TcpClient.kt
│   ├── UdpDiscovery.kt
│   ├── NetworkQualityMonitor.kt
│   ├── NetworkStateHelper.kt
│   ├── MessageTypes.kt
│   ├── ConnectionHelper.kt
│   └── ConnectionQuality.kt
│
├── presentation/                       # Presentation Layer
│   ├── base/                           # Base classes
│   │   └── BaseViewModel.kt
│   │
│   ├── extensions/                     # Compose extensions
│   │   └── ComposableExtensions.kt
│   │
│   ├── navigation/                     # Navigation
│   │   ├── Navigation.kt
│   │   └── Destinations.kt
│   │
│   ├── theme/                          # Theme
│   │   ├── Theme.kt
│   │   ├── Type.kt
│   │   └── Color.kt
│   │
│   └── ui/                             # UI Screens
│       ├── home/
│       │   ├── HomeScreen.kt
│       │   ├── HomeViewModel.kt
│       │   └── components/
│       │
│       ├── calibration/
│       │   ├── CalibrationScreen.kt
│       │   ├── CalibrationViewModel.kt
│       │   └── components/
│       │
│       ├── settings/
│       │   ├── SettingsScreen.kt
│       │   ├── SettingsViewModel.kt
│       │   └── components/
│       │
│       ├── gesture/
│       │   ├── GestureStudioScreen.kt
│       │   ├── GestureStudioViewModel.kt
│       │   └── components/
│       │
│       ├── proximity/
│       │   ├── ProximityScreen.kt
│       │   ├── ProximityViewModel.kt
│       │   └── components/
│       │
│       ├── statistics/
│       │   ├── StatisticsScreen.kt
│       │   ├── StatisticsViewModel.kt
│       │   └── components/
│       │
│       ├── profiles/
│       │   ├── ProfilesScreen.kt
│       │   ├── ProfilesViewModel.kt
│       │   └── components/
│       │
│       ├── voice/
│       │   ├── VoiceCommandsScreen.kt
│       │   └── VoiceCommandsViewModel.kt
│       │
│       ├── edge/
│       │   ├── EdgeGesturesScreen.kt
│       │   └── EdgeGesturesViewModel.kt
│       │
│       ├── network/
│       │   ├── NetworkDiscoveryScreen.kt
│       │   └── NetworkDiscoveryViewModel.kt
│       │
│       ├── logs/
│       │   ├── ServerLogsScreen.kt
│       │   └── ServerLogsViewModel.kt
│       │
│       ├── onboarding/
│       │   ├── OnboardingScreen.kt
│       │   └── OnboardingViewModel.kt
│       │
│       ├── themes/
│       │   └── ThemesScreen.kt
│       │
│       ├── touchpad/
│       │   └── TouchpadScreen.kt
│       │
│       └── components/                 # Shared UI components
│           ├── AnimatedConnectionStatus.kt
│           ├── AnimatedCounter.kt
│           ├── AnimatedSwitch.kt
│           ├── AnimatedToast.kt
│           ├── CircularProgressWithLabel.kt
│           ├── ConnectionStatusBadge.kt
│           ├── DataChart.kt
│           ├── FloatingActionMenu.kt
│           ├── GestureWaveform.kt
│           ├── HolographicText.kt
│           ├── InteractiveTutorialCard.kt
│           ├── NeonButton.kt
│           ├── NotificationBadge.kt
│           ├── PullToRefresh.kt
│           ├── RadarAnimation.kt
│           ├── SensorVisualizer.kt
│           ├── SlideUpPanel.kt
│           └── VoiceWaveAnimation.kt
│
├── sensors/                            # Sensor processing
│   ├── CalibrationHelper.kt
│   ├── MadgwickAHRS.kt
│   └── EnhancedGestureDetector.kt
│
├── service/                            # Background services
│   ├── SensorService.kt
│   ├── BluetoothHidService.kt
│   ├── ProximityService.kt
│   ├── ProximityAwareService.kt
│   ├── VoiceCommandService.kt
│   ├── GestureInferenceService.kt
│   ├── GestureRecorderService.kt
│   ├── UsbHidService.kt
│   ├── UsbSerialService.kt
│   ├── OrientationMonitorService.kt
│   ├── EdgeGestureService.kt
│   ├── DebugOverlayService.kt
│   └── ForegroundServiceManager.kt
│
├── utils/                              # Utility classes
│   ├── PreferencesManager.kt
│   ├── PermissionManager.kt
│   ├── BatteryOptimizer.kt
│   ├── ErrorHandler.kt
│   ├── AnalyticsManager.kt
│   ├── AppConstants.kt
│   ├── ResultExtensions.kt
│   ├── DialogHelper.kt
│   ├── PermissionUIHelper.kt
│   ├── ViewHelpers.kt
│   ├── JsonHelper.kt
│   ├── FileHelper.kt
│   ├── ResourceHelper.kt
│   └── ValidationHelper.kt
│
├── notifications/                      # Notifications
│   └── NotificationManager.kt
│
├── receivers/                          # Broadcast receivers
│   ├── BootReceiver.kt
│   ├── UsbReceiver.kt
│   ├── BluetoothReceiver.kt
│   ├── NetworkReceiver.kt
│   └── NotificationReceiver.kt
│
├── di/                                 # Dependency Injection
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   ├── UseCaseModule.kt
│   ├── FeatureModule.kt
│   ├── ViewModelModule.kt
│   ├── ServiceModule.kt
│   ├── SensorModule.kt
│   ├── ViewModelKey.kt
│   └── AppContainer.kt
│
└── sync/                               # Sync manager
    └── DataSyncManager.kt
```

---

## Module Breakdown

### 1. **Auth Module**
- **Purpose:** Authentication and token management
- **Key Files:**
    - `AuthManager.kt` - Manages authentication state

### 2. **Data Module**
- **Purpose:** Data persistence and retrieval
- **Sub-packages:**
    - `datasource/local/` - Room database and local storage
    - `datasource/remote/` - Network data sources
    - `mapper/` - Data mappers between domain and entities
    - `repository/` - Repository implementations

### 3. **Domain Module**
- **Purpose:** Business logic and models
- **Sub-packages:**
    - `model/` - Domain models
    - `repository/` - Repository interfaces
    - `usecase/` - Business logic use cases

### 4. **Features Module**
- **Purpose:** Feature orchestration
- **Key Files:**
    - `ConnectionFeature.kt` - Network connection management
    - `MouseControlFeature.kt` - Mouse control
    - `CalibrationFeature.kt` - Sensor calibration
    - `GestureRecognitionFeature.kt` - Gesture detection
    - `ProximityFeature.kt` - Proximity security
    - `StatisticsFeature.kt` - Statistics tracking
    - `VoiceFeature.kt` - Voice commands
    - `ProfileFeature.kt` - User profiles
    - `UpdateFeature.kt` - App updates
    - `SensorFeature.kt` - Sensor management

### 5. **Network Module**
- **Purpose:** Network communication
- **Key Files:**
    - `ConnectionManager.kt` - Unified connection manager
    - `WebSocketManager.kt` - WebSocket communication
    - `TcpClient.kt` - TCP communication
    - `UdpDiscovery.kt` - UDP discovery
    - `NetworkQualityMonitor.kt` - Network quality monitoring
    - `MessageTypes.kt` - Message type constants

### 6. **Presentation Module**
- **Purpose:** UI and user interaction
- **Sub-packages:**
    - `base/` - Base classes (ViewModel)
    - `extensions/` - Compose extensions
    - `navigation/` - Navigation
    - `theme/` - Theming
    - `ui/` - UI Screens and components

### 7. **Sensors Module**
- **Purpose:** Sensor data processing
- **Key Files:**
    - `CalibrationHelper.kt` - Sensor calibration
    - `MadgwickAHRS.kt` - Sensor fusion
    - `EnhancedGestureDetector.kt` - Gesture detection

### 8. **Service Module**
- **Purpose:** Background services
- **Key Files:**
    - `SensorService.kt` - Sensor data service
    - `BluetoothHidService.kt` - Bluetooth HID
    - `ProximityService.kt` - Proximity monitoring
    - `VoiceCommandService.kt` - Voice recognition

### 9. **Utils Module**
- **Purpose:** Utility classes and helpers
- **Key Files:**
    - `PreferencesManager.kt` - Shared preferences
    - `PermissionManager.kt` - Permission handling
    - `BatteryOptimizer.kt` - Battery optimization
    - `ErrorHandler.kt` - Error handling
    - `AnalyticsManager.kt` - Analytics tracking

### 10. **DI Module**
- **Purpose:** Dependency injection
- **Key Files:**
    - All `*Module.kt` files for Hilt

---

## Data Flow

### UI → Data Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                               UI FLOW                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  User Action → ViewModel → UseCase → Repository → DataSource → Network │
│       ↓           ↓          ↓          ↓            ↓          ↓      │
│  Compose      StateFlow   Business   Data       Local/      Server     │
│  Screen      Update       Logic      Access     Remote                 │
│                                                                         │
│  ────────────────────────────────────────────────────────────────────── │
│                                                                         │
│  Network → DataSource → Repository → UseCase → ViewModel → UI          │
│     ↓          ↓           ↓          ↓          ↓          ↓          │
│  Server     Data        Data        Business   State     Compose       │
│             Access      Storage     Logic      Update    Screen        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### State Management Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User      │    │  ViewModel  │    │   State     │
│   Action    │───▶│  Event      │───▶│   Update    │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                                              ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Compose   │◀───│   State     │◀───│   State     │
│   Screen    │    │   Flow      │    │   Container │
└─────────────┘    └─────────────┘    └─────────────┘
```

---

## Layer Diagrams

### Clean Architecture Layers

```
┌────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                           │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Activities & Composables                                      │   │
│  │  - HomeScreen, CalibrationScreen, SettingsScreen              │   │
│  │  - GestureStudioScreen, ProximityScreen                       │   │
│  └────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  ViewModels                                                    │   │
│  │  - HomeViewModel, CalibrationViewModel, SettingsViewModel      │   │
│  │  - GestureStudioViewModel, ProximityViewModel                 │   │
│  └────────────────────────────────────────────────────────────────┘   │
├────────────────────────────────────────────────────────────────────────┤
│                           DOMAIN LAYER                                │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Use Cases                                                     │   │
│  │  - ConnectToServerUseCase, SendMovementUseCase                │   │
│  │  - CalibrationUseCase, DetectGestureUseCase                   │   │
│  └────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Domain Models                                                 │   │
│  │  - ConnectionModels, MouseModels, GestureModels               │   │
│  │  - CalibrationModels, SensorModels, ProximityModels           │   │
│  └────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Repository Interfaces                                          │   │
│  │  - IConnectionRepository, IMouseRepository                     │   │
│  │  - IGestureRepository, ICalibrationRepository                  │   │
│  └────────────────────────────────────────────────────────────────┘   │
├────────────────────────────────────────────────────────────────────────┤
│                           DATA LAYER                                  │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Repository Implementations                                    │   │
│  │  - ConnectionRepositoryImpl, MouseRepositoryImpl              │   │
│  │  - GestureRepositoryImpl, CalibrationRepositoryImpl            │   │
│  └────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Data Sources                                                  │   │
│  │  - Local: Room Database, Preferences                           │   │
│  │  - Remote: WebSocket, TCP, UDP                                 │   │
│  └────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Mappers                                                       │   │
│  │  - DomainToEntityMapper, EntityToDomainMapper                  │   │
│  └────────────────────────────────────────────────────────────────┘   │
├────────────────────────────────────────────────────────────────────────┤
│                       INFRASTRUCTURE LAYER                            │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Network                                                       │   │
│  │  - ConnectionManager, WebSocketManager, TcpClient             │   │
│  └────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Database                                                      │   │
│  │  - AppDatabase, Room DAOs, DataStore                           │   │
│  └────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Sensors                                                       │   │
│  │  - CalibrationHelper, MadgwickAHRS, EnhancedGestureDetector   │   │
│  └────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │  Services                                                      │   │
│  │  - SensorService, BluetoothHidService, ProximityService        │   │
│  └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### Dependency Injection Graph

```
┌────────────────────────────────────────────────────────────────────┐
│                         DI MODULES                                 │
├────────────────────────────────────────────────────────────────────┤
│  AppModule          │  Provides: Preferences, Sensors, Utils      │
│  NetworkModule      │  Provides: OkHttp, ConnectionManager        │
│  DatabaseModule     │  Provides: Room Database, DAOs              │
│  RepositoryModule   │  Provides: Repository Implementations       │
│  UseCaseModule      │  Provides: Use Cases                        │
│  FeatureModule      │  Provides: Features                         │
│  ViewModelModule    │  Provides: ViewModels                       │
│  ServiceModule      │  Provides: Services                         │
│  SensorModule       │  Provides: Sensor Components                │
└────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────────┐
│                    Hilt Components Tree                            │
├────────────────────────────────────────────────────────────────────┤
│  SingletonComponent                                                │
│  ├── AppModule                                                     │
│  ├── NetworkModule                                                 │
│  ├── DatabaseModule                                                │
│  ├── RepositoryModule                                              │
│  ├── UseCaseModule                                                 │
│  ├── FeatureModule                                                 │
│  ├── ServiceModule                                                 │
│  └── SensorModule                                                  │
│                                                                     │
│  ViewModelComponent                                                │
│  └── ViewModelModule                                               │
└────────────────────────────────────────────────────────────────────┘
```

---

## Key Components

### 1. **BaseViewModel**

```kotlin
abstract class BaseViewModel<State, Event>(initialState: State) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()
    
    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event.asSharedFlow()
    
    protected fun setState(reducer: State.() -> State) {
        _state.update { it.reducer() }
    }
    
    protected fun sendEvent(event: Event) {
        viewModelScope.launch { _event.emit(event) }
    }
    
    abstract fun onEvent(event: Event)
}
```

### 2. **ConnectionManager**

```kotlin
@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) {
    // Unified connection management for WebSocket and TCP
    // StateFlow for connection status and quality
    // Auto-reconnection with exponential backoff
}
```

### 3. **Feature Pattern**

```kotlin
@Singleton
class ConnectionFeature @Inject constructor(
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val discoverServersUseCase: DiscoverServersUseCase,
    private val getConnectionStatusUseCase: GetConnectionStatusUseCase,
    private val testConnectionUseCase: TestConnectionUseCase
) {
    // Orchestrates multiple use cases
    // Provides high-level feature API
    // Manages feature state
}
```

---

## Testing Strategy

### Test Pyramid

```
        ┌─────────┐
        │   UI    │  ← Espresso, Compose UI Testing
       ┌┴─────────┴┐
       │ Integration│  ← Integration Tests
      ┌┴────────────┴┐
      │   Unit Tests  │  ← JUnit, Mockito, MockK
     ┌┴───────────────┴┐
     │   Domain Logic  │  ← Use Case Testing
    ┌┴──────────────────┴┐
    │  Repository Tests  │  ← Data Layer Testing
   ┌┴─────────────────────┴┐
   │  Instrumentation Tests│  ← Android Testing
   └────────────────────────┘
```

### Test Locations

```
app/src/
├── test/                          # Unit tests
│   ├── java/com/airmouse/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── usecase/
│   │   ├── features/
│   │   └── utils/
│   └── resources/
│
└── androidTest/                   # Instrumentation tests
    ├── java/com/airmouse/
    │   ├── presentation/
    │   │   ├── ui/
    │   │   └── navigation/
    │   ├── data/
    │   └── service/
    └── resources/
```

---

## Build Variants

### Flavor Dimensions

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BUILD VARIANTS                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Environment:                                                       │
│  ├── dev     (Development)                                         │
│  ├── staging (Staging)                                             │
│  └── prod    (Production)                                          │
│                                                                     │
│  Mode:                                                              │
│  ├── debug   (Debug)                                               │
│  └── release (Release)                                             │
│                                                                     │
│  Flavors:                                                           │
│  ├── free    (Free version)                                        │
│  └── pro     (Pro version)                                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Third-Party Libraries

| Library | Purpose | Version |
|---------|---------|---------|
| **Jetpack Compose** | UI Framework | 2024.10.00 |
| **Material 3** | Material Design | 1.3.0 |
| **Hilt** | Dependency Injection | 2.52 |
| **Room** | Database | 2.6.1 |
| **DataStore** | Preferences | 1.1.1 |
| **OkHttp** | Networking | 4.12.0 |
| **Retrofit** | REST Client | 2.11.0 |
| **Gson** | JSON Parsing | 2.11.0 |
| **Coroutines** | Async Programming | 1.7.3 |
| **TensorFlow Lite** | ML Inference | 2.16.1 |
| **UsbSerial** | USB Communication | 3.8.0 |
| **Timber** | Logging | 5.0.1 |
| **Truth** | Assertions | 1.1.5 |
| **Mockk** | Mocking | 1.13.8 |

---

## Development Guidelines

### 1. **Package Naming Convention**

```
com.airmouse.[layer].[feature].[component]
```

### 2. **Class Naming Convention**

| Layer | Prefix/Suffix | Example |
|-------|---------------|---------|
| **Model** | No suffix | `ConnectionConfig` |
| **Repository Interface** | `I` prefix | `IConnectionRepository` |
| **Repository Implementation** | `Impl` suffix | `ConnectionRepositoryImpl` |
| **Use Case** | `UseCase` suffix | `ConnectToServerUseCase` |
| **ViewModel** | `ViewModel` suffix | `HomeViewModel` |
| **Screen** | `Screen` suffix | `HomeScreen` |
| **Service** | `Service` suffix | `SensorService` |
| **Feature** | `Feature` suffix | `ConnectionFeature` |

### 3. **File Organization**

```
Each feature should have:
├── domain/
│   ├── model/          # Feature-specific models
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Feature use cases
├── data/
│   ├── datasource/     # Data sources
│   └── repository/     # Repository implementations
└── presentation/
    ├── ui/             # UI Screens
    ├── viewmodel/      # ViewModels
    └── components/     # UI Components
```

### 4. **State Management**

- Use `StateFlow` for UI state
- Use `SharedFlow` for events
- Use `MutableStateFlow` for mutable state
- Use `update {}` for atomic updates

### 5. **Error Handling**

- Use `Result<T>` for operations
- Use `sealed class` for error types
- Use `ErrorHandler` for centralized error handling

### 6. **Logging**

- Use `Timber` for logging
- Use `Log.d()` for debug
- Use `Log.e()` for errors
- Use `Log.w()` for warnings

---

## Summary

The Air Mouse Android app follows a **Clean Architecture** with **MVVM** pattern, using **Jetpack Compose** for UI and **Hilt** for dependency injection. The architecture is designed to be:

- ✅ **Testable** - Each layer is independently testable
- ✅ **Maintainable** - Clear separation of concerns
- ✅ **Scalable** - Easy to add new features
- ✅ **Decoupled** - Low coupling between layers
- ✅ **Modern** - Uses latest Android technologies
- ✅ **Production-ready** - Error handling, logging, analytics

The app is structured to handle:

- 🔌 **Multiple Protocols**: WebSocket, TCP, UDP, Bluetooth, USB
- 🎯 **Gesture Recognition**: TensorFlow Lite model
- 📡 **Proximity Lock**: Bluetooth RSSI-based
- 🎙️ **Voice Commands**: Speech recognition
- 🖱️ **Mouse Control**: Cross-platform cursor control
- 📊 **Analytics**: Usage tracking and statistics
- 🔐 **Profiles**: Multiple user profiles
- 📦 **Updates**: In-app updates
