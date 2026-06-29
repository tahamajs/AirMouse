# Air Mouse – Complete File Architecture & Project Structure

This document provides the **ultimate, definitive architectural blueprint** of the Air Mouse Android application. It maps every single file, package, and module, explaining their exact roles and how they interconnect to form a cohesive, production-grade system following **Clean Architecture** and **Unidirectional Data Flow (MVI)**.

---

## 1. High-Level Architecture (Clean Architecture)

The project is strictly divided into 4 concentric layers, ensuring that the UI is completely decoupled from the database and network logic.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER (UI)                         │
│  Activities, Compose Screens, ViewModels, Navigation, Theme       │
│  Dependencies: Domain Layer                                       │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ (Observes State, Sends Events)
┌───────────────────────────────▼─────────────────────────────────────┐
│                         DOMAIN LAYER (Core)                        │
│  Business Models, Repository Interfaces, Use Cases                 │
│  Dependencies: Pure Kotlin/Java (No Android SDK)                  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ (Implements Interfaces)
┌───────────────────────────────▼─────────────────────────────────────┐
│                           DATA LAYER                               │
│  Repository Implementations, Data Sources (Room/Preferences/Remote)│
│  Mappers, Entities                                                 │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ (Uses Infrastructure)
┌───────────────────────────────▼─────────────────────────────────────┐
│                       INFRASTRUCTURE LAYER                         │
│  Network, Sensors, Notifications, Utilities, Background Services   │
└─────────────────────────────────────────────────────────────────────┘
```

### Dependency Rule
> **Dependencies point inward.** The Presentation layer depends on Domain. The Data layer depends on Domain and Infrastructure. Infrastructure depends on external libraries (OkHttp, Android SDK).

---

## 2. Root Project Structure (`app/src/main/java/com/airmouse/`)

Here is the complete directory tree with every file categorized.

```
com.airmouse/
│
├── # =================== APPLICATION ROOT ===================
├── AirMouseApplication.kt               # Application class, Hilt setup, logging, crash reporting.
│
├── # =================== PRESENTATION LAYER ===================
├── presentation/
│   ├── ui/                              # All Screens (Activity & Compose)
│   │   ├── home/
│   │   │   ├── HomeActivity.kt          # Main entry point, permissions, sensor binding.
│   │   │   ├── HomeScreen.kt            # Dashboard with connection, sensors, gestures.
│   │   │   ├── HomeViewModel.kt         # Manages sensor, gesture, connection, stats.
│   │   │   └── HomeUiState.kt           # State models for Home screen.
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt        # Full settings hub (Cursor, Gesture, AI, etc.).
│   │   │   ├── SettingsViewModel.kt     # Persists settings, import/export, reset.
│   │   │   └── SettingsState.kt         # UiState, Events, Effects.
│   │   ├── calibration/
│   │   │   ├── CalibrationScreen.kt     # Step-by-step wizard (Gyro/Mag/Accel).
│   │   │   ├── CalibrationResultScreen.kt # Quality assessment & summary.
│   │   │   ├── CalibrationGuideDialog.kt # Animated instruction dialog.
│   │   │   ├── CalibrationViewModel.kt  # Sampling logic, sensor coordination.
│   │   │   ├── CalibrationUiState.kt    # State, phases, progress.
│   │   │   └── CalibrationComponents.kt # Reusable Calibration UI widgets.
│   │   ├── touchpad/
│   │   │   ├── TouchpadScreen.kt        # Full gesture capture surface.
│   │   │   ├── TouchpadViewModel.kt     # Gesture parsing, haptics, network sending.
│   │   │   ├── TouchpadState.kt         # State, events, effects, presets.
│   │   │   └── TouchpadSettingsScreen.kt # (DEPRECATED) – Delete.
│   │   ├── voice/
│   │   │   ├── VoiceCommandsScreen.kt   # Voice control with wake word and custom commands.
│   │   │   └── VoiceCommandsViewModel.kt # TTS, permission, command matching.
│   │   ├── themes/
│   │   │   ├── ThemesScreen.kt          # Theme picker with previews.
│   │   │   ├── ThemesViewModel.kt       # Theme/Accent persistence.
│   │   │   └── ThemesState.kt           # AccentColor, ThemeOption definitions.
│   │   ├── accessibility/
│   │   │   ├── AccessibilityScreen.kt   # Display, feedback, gesture, voice settings.
│   │   │   ├── AccessibilityViewModel.kt # Accessibility preferences.
│   │   │   └── AccessibilityUiState.kt  # High contrast, haptics, color blind modes.
│   │   ├── profiles/
│   │   │   ├── ProfilesScreen.kt        # User profile CRUD, favorites, defaults.
│   │   │   ├── ProfilesViewModel.kt     # Profile data handling.
│   │   │   └── ProfileSettings.kt       # Profile data class.
│   │   ├── proximity/
│   │   │   ├── ProximityScreen.kt       # Bluetooth proximity lock/unlock.
│   │   │   └── ProximityViewModel.kt    # RSSI calculation, auto-lock logic.
│   │   ├── logs/
│   │   │   ├── ServerLogsScreen.kt      # Real-time log viewer, filters, export.
│   │   │   ├── ServerLogsViewModel.kt   # Log capture, persistence, formatting.
│   │   │   └── ServerLogsState.kt       # LogEntry, LogLevel, SortOrder.
│   │   ├── help/
│   │   │   ├── HelpScreen.kt            # Searchable help center with categories.
│   │   │   ├── HelpViewModel.kt         # Search, favorites, expanded sections.
│   │   │   └── HelpModels.kt            # HelpCategory, HelpSection.
│   │   ├── onboarding/
│   │   │   ├── OnboardingActivity.kt    # 4-page walkthrough with animations.
│   │   │   ├── OnboardingItem.kt        # Data class for onboarding pages.
│   │   │   └── OnboardingPagerAdapter.kt # ViewPager adapter.
│   │   ├── about/
│   │   │   └── AboutScreen.kt           # ❌ MISSING (Partially in Settings).
│   │   ├── statistics/
│   │   │   └── StatisticsScreen.kt      # ❌ MISSING
│   │   ├── sensor/
│   │   │   └── SensorVisualizerScreen.kt # ❌ MISSING
│   │   ├── gesture/
│   │   │   └── GestureStudioScreen.kt   # ❌ MISSING
│   │   ├── edge/
│   │   │   └── EdgeGesturesScreen.kt    # ❌ MISSING
│   │   ├── network/
│   │   │   └── NetworkDiscoveryScreen.kt # ❌ MISSING
│   │   ├── battery/
│   │   │   └── BatteryScreen.kt         # ❌ MISSING
│   │   ├── files/
│   │   │   └── FileTransferScreen.kt    # ❌ MISSING
│   │   └── main/
│   │       └── MainScreen.kt            # Root navigation host with drawer & bottom bar.
│   │
│   ├── navigation/
│   │   ├── Destinations.kt              # Routes, titles, icons for all screens.
│   │   ├── NavigationActions.kt         # Abstraction for NavController.
│   │   ├── AirMouseNavHost.kt           # NavHost graph with composable mappings.
│   │   └── AirMouseBottomBar.kt         # Bottom navigation bar component.
│   │
│   ├── theme/
│   │   ├── Theme.kt                     # AirMouseTheme wrapper, Material 3 integration.
│   │   ├── ThemeColors.kt               # ThemeColorScheme, AccentColor, color families.
│   │   ├── Color.kt                     # Static color definitions (DeepOrange, Teal, etc.).
│   │   ├── Dimensions.kt                # Central spacing, padding, size, font constants.
│   │   └── Shapes.kt                    # RoundedCornerShape definitions (cards, buttons).
│   │
│   └── components/                      # Reusable UI Atoms & Molecules
│       ├── AllUIComponents.kt           # ControlDashboard, GestureTraining, Analytics, etc.
│       ├── AnimatedCheckbox.kt
│       ├── AnimatedSwitch.kt
│       ├── AnimatedToast.kt
│       ├── AnimatedCounter.kt
│       ├── AnimatedConnectionStatus.kt
│       ├── BatteryLevelIndicator.kt
│       ├── CircularProgressWithLabel.kt
│       ├── ConnectionStatusBadge.kt
│       ├── DataChart.kt                # Line & Donut charts.
│       ├── FloatingActionMenu.kt
│       ├── FloatingParticles.kt
│       ├── GestureWaveform.kt
│       ├── GlassCard.kt
│       ├── GradientBackground.kt
│       ├── GradientIconButton.kt
│       ├── HolographicText.kt
│       ├── InteractiveTutorialCard.kt
│       ├── NeonButton.kt
│       ├── NeumorphicCard.kt
│       ├── NotificationBadge.kt
│       ├── ParticleBackground.kt
│       ├── PullToRefresh.kt
│       ├── RadarAnimation.kt
│       ├── SensorVisualizer.kt          # 3D phone orientation cube.
│       ├── ShimmerEffect.kt
│       ├── SkeletonScreen.kt
│       ├── SlideUpPanel.kt
│       ├── TypewriterText.kt
│       └── VoiceWaveAnimation.kt
│
├── # =================== DOMAIN LAYER ===================
├── domain/
│   ├── model/                           # Pure Kotlin data classes.
│   │   ├── CalibrationModels.kt         # CalibrationStatus, Quality, Data.
│   │   ├── ConnectionModels.kt          # ConnectionConfig, Status, Quality.
│   │   ├── GestureModels.kt             # GestureType, Event, Template.
│   │   ├── MouseModels.kt               # MouseEvent, MovementProfile, Statistics.
│   │   ├── ProximityModels.kt           # ProximityState, Config.
│   │   ├── SensorModels.kt              # SensorData, OrientationData.
│   │   ├── StatisticsModels.kt          # StatisticsSummary, DailyStats.
│   │   ├── UpdateModels.kt              # UpdateResult, VersionInfo.
│   │   ├── VoiceCommandModels.kt        # VoiceCommand, Config, History.
│   │   ├── ProfileModels.kt             # UserProfile, ProfileSettings.
│   │   ├── PreferencesModels.kt         # AppPreferences, UserPreferences.
│   │   ├── BluetoothModels.kt           # BluetoothDeviceInfo, BLECharacteristic.
│   │   ├── ErrorModels.kt               # AppError, ErrorType.
│   │   └── ControlMode.kt               # ControlMode enum.
│   │
│   ├── repository/                       # Interfaces defining data operations.
│   │   ├── ICalibrationRepository.kt
│   │   ├── IConnectionRepository.kt
│   │   ├── IGestureRepository.kt
│   │   ├── IMouseRepository.kt
│   │   ├── IProfileRepository.kt
│   │   ├── IProximityRepository.kt
│   │   ├── ISensorRepository.kt
│   │   ├── ISettingsRepository.kt
│   │   ├── IStatisticsRepository.kt
│   │   ├── IUpdateRepository.kt
│   │   └── IVoiceCommandRepository.kt
│   │
│   ├── usecase/                         # Single responsibility business rules.
│   │   ├── CalibrationUseCase.kt
│   │   ├── ConnectToServerUseCase.kt
│   │   ├── SendMovementUseCase.kt
│   │   ├── DetectGestureUseCase.kt
│   │   ├── DiscoverServersUseCase.kt
│   │   ├── GetConnectionStatusUseCase.kt
│   │   ├── GetStatisticsUseCase.kt
│   │   ├── RecordStatisticsUseCase.kt
│   │   ├── ManageProfileUseCase.kt
│   │   ├── ManageGestureTemplatesUseCase.kt
│   │   ├── HandleVoiceCommandUseCase.kt
│   │   ├── GetProximityStateUseCase.kt
│   │   ├── UpdateProximityConfigUseCase.kt
│   │   ├── TestConnectionUseCase.kt
│   │   ├── CheckForUpdatesUseCase.kt
│   │   └── GetGestureStatisticsUseCase.kt
│   │
│   └── feature/                         # Feature orchestrators (bridging UI & UseCases).
│       ├── ConnectionFeature.kt
│       ├── MouseControlFeature.kt
│       ├── CalibrationFeature.kt
│       ├── GestureRecognitionFeature.kt
│       ├── ProximityFeature.kt
│       ├── StatisticsFeature.kt
│       ├── VoiceFeature.kt
│       ├── ProfileFeature.kt
│       ├── UpdateFeature.kt
│       └── SensorFeature.kt
│
├── # =================== DATA LAYER ===================
├── data/
│   ├── repository/                       # Implementations of Domain interfaces.
│   │   ├── CalibrationRepositoryImpl.kt
│   │   ├── ConnectionRepositoryImpl.kt
│   │   ├── GestureRepositoryImpl.kt
│   │   ├── MouseRepositoryImpl.kt
│   │   ├── ProfileRepositoryImpl.kt
│   │   ├── ProximityRepositoryImpl.kt
│   │   ├── SensorRepositoryImpl.kt
│   │   ├── SettingsRepositoryImpl.kt
│   │   ├── StatisticsRepositoryImpl.kt
│   │   ├── UpdateRepositoryImpl.kt
│   │   └── VoiceCommandRepositoryImpl.kt
│   │
│   ├── datasource/                      # Concrete data access.
│   │   ├── local/                       # Local storage (Room & Preferences).
│   │   │   ├── AppDatabase.kt           # Room database definition.
│   │   │   ├── Converters.kt            # Type converters for Room.
│   │   │   ├── entity/                  # Room Entities.
│   │   │   │   ├── CalibrationEntity.kt
│   │   │   │   ├── SettingsEntity.kt
│   │   │   │   ├── StatisticsEntity.kt
│   │   │   │   ├── GestureTemplateEntity.kt
│   │   │   │   ├── ProfileEntity.kt
│   │   │   │   ├── TrainingSampleEntity.kt
│   │   │   │   ├── DailyStatsEntity.kt
│   │   │   │   └── GestureStatsEntity.kt
│   │   │   ├── dao/                     # Room Data Access Objects.
│   │   │   │   ├── CalibrationDao.kt
│   │   │   │   ├── SettingsDao.kt
│   │   │   │   ├── StatisticsDao.kt
│   │   │   │   ├── GestureDao.kt
│   │   │   │   ├── ProfileDao.kt
│   │   │   │   ├── TrainingSampleDao.kt
│   │   │   │   ├── DailyStatsDao.kt
│   │   │   │   └── GestureStatsDao.kt
│   │   │   ├── CalibrationDataSourceImpl.kt
│   │   │   ├── GestureDataSourceImpl.kt
│   │   │   ├── ProfileDataSourceImpl.kt
│   │   │   ├── StatisticsDataSourceImpl.kt
│   │   │   ├── PreferencesDataSourceImpl.kt
│   │   │   ├── LocalDataSourceImpl.kt   # Unified wrapper over all DAOs.
│   │   │   └── interfaces/              # Interfaces for Data Sources.
│   │   │       ├── ICalibrationDataSource.kt
│   │   │       ├── IGestureDataSource.kt
│   │   │       ├── IProfileDataSource.kt
│   │   │       ├── IStatisticsDataSource.kt
│   │   │       ├── IPreferencesDataSource.kt
│   │   │       └── ILocalDataSource.kt
│   │   │
│   │   └── remote/                      # Remote data sources (Network/BT/USB).
│   │       ├── ConnectionDataSourceImpl.kt
│   │       ├── BluetoothDataSourceImpl.kt
│   │       ├── UsbDataSourceImpl.kt
│   │       ├── WebSocketDataSourceImpl.kt
│   │       └── interfaces/
│   │           ├── IConnectionDataSource.kt
│   │           ├── IBluetoothDataSource.kt
│   │           ├── IUsbDataSource.kt
│   │           └── IWebSocketDataSource.kt
│   │
│   ├── mapper/
│   │   ├── DomainToEntityMapper.kt      # Maps Domain → Room Entity.
│   │   └── EntityToDomainMapper.kt      # Maps Room Entity → Domain.
│   │
│   └── helpers/
│       ├── SensorRepository.kt          # Flow of fused sensor data (callbackFlow).
│       ├── PreferencesDataStore.kt      # AndroidX DataStore wrapper.
│       ├── CalibrationPrefsData.kt      # Helper data class.
│       └── GestureData.kt               # Helper data class.
│
├── # =================== INFRASTRUCTURE LAYER ===================
├── network/                              # Communication with PC Server.
│   ├── ConnectionManager.kt             # CORE – Unified WebSocket/TCP/UDP client.
│   ├── UdpDiscovery.kt                  # Server discovery via UDP broadcast.
│   ├── AutoReconnectManager.kt          # Exponential backoff reconnection logic.
│   ├── NetworkQualityMonitor.kt         # Wi-Fi/Cellular signal strength.
│   ├── MessageTypes.kt                  # Protocol constants (matching Go server).
│   ├── AirMouseProtocolMessages.kt      # JSON message builders.
│   ├── ConnectionHelper.kt              # Extension functions for sending commands.
│   ├── TcpClient.kt                     # DEPRECATED – Delete.
│   └── WebSocketManager.kt              # DEPRECATED – Delete.
│
├── sensors/                              # Hardware & Motion Processing.
│   ├── SensorService.kt                 # Registers sensors, runs Madgwick fusion.
│   ├── CalibrationHelper.kt             # Gyro bias, 6‑point accel, hard‑iron mag.
│   ├── EnhancedGestureDetector.kt       # Click, Double, Right, Scroll detection.
│   ├── MadgwickAHRS.kt                  # Quaternion-based sensor fusion.
│   ├── MotionAnalyzer.kt                # Jerk, acceleration, stability analysis.
│   ├── MotionDetector.kt                # Simple legacy detector.
│   ├── OrientationTracker.kt            # Tracks yaw/pitch/roll over time.
│   ├── SensorDataLogger.kt              # Logs raw sensor data to CSV.
│   ├── SensorDataProcessor.kt           # Filters (Low/High pass, Kalman).
│   ├── SensorFusion.kt                  # Static fusion utilities.
│   ├── SensorManagerHelper.kt           # Checks sensor availability.
│   └── GestureDetector.kt               # Legacy – Consider removing.
│
├── notifications/                       # System Alerts.
│   └── NotificationManager.kt           # Notification channels, badges, alerts.
│
├── files/                               # File Transfers.
│   └── FileTransferService.kt           # Handles sending/receiving files.
│
├── mirroring/                           # Screen Mirroring.
│   └── ScreenMirroringService.kt        # Captures screen and streams via WebSocket.
│
├── gaming/                              # Game Profiles.
│   └── GameProfilesManager.kt           # Detects foreground games, applies profiles.
│
├── macros/                              # Macro Recorder.
│   └── MacroRecorder.kt                 # Records/plays back mouse/keyboard macros.
│
├── sync/                                # Data Synchronization.
│   └── DataSyncManager.kt               # Syncs calibration/gestures/profiles to server.
│
├── utils/                               # Utility & Helper Classes.
│   ├── PreferencesManager.kt            # Interface for SharedPreferences/DataStore.
│   ├── LogManager.kt                    # ❌ MISSING
│   ├── PermissionHelper.kt              # ❌ MISSING
│   ├── VibrateUtils.kt                  # ❌ MISSING
│   ├── AudioUtils.kt                    # ❌ MISSING
│   ├── BluetoothUtils.kt                # ❌ MISSING
│   ├── BatterySaver.kt                  # ❌ MISSING
│   ├── ConnectedDeviceStore.kt          # ❌ MISSING
│   └── QRScanner.kt                     # ❌ MISSING
│
├── # =================== DEPENDENCY INJECTION ===================
├── di/
│   ├── AppContainer.kt                  # Manual DI container.
│   ├── AppModule.kt                     # Core dependencies (Context, Prefs, etc.).
│   ├── NetworkModule.kt                 # OkHttpClient, ConnectionManager.
│   ├── DatabaseModule.kt                # Room database, DAOs.
│   ├── SensorModule.kt                  # SensorManager, CalibrationHelper.
│   ├── ServiceModule.kt                 # SensorService, PresentationModeService.
│   ├── RepositoryModule.kt              # Binds Repository interfaces to impls.
│   ├── UseCaseModule.kt                 # Provides UseCase instances.
│   ├── FeatureModule.kt                 # Provides Feature instances.
│   ├── ViewModelModule.kt               # Binds ViewModels for Hilt.
│   ├── CalibrationModule.kt             # Calibration-specific bindings.
│   ├── CoroutineModule.kt               # Dispatchers (IO, Main, Default).
│   └── GestureRepositoryModule.kt       # Gesture repository bindings.
│
└── # =================== GO SERVER (Backend) ===================
└── go-server/ (External Package)
    ├── bspline.go                       # Cubic B‑spline smoothing.
    ├── humanizer.go                     # Human-like cursor movement.
    ├── tremor.go                        # Hand tremor simulation.
    ├── velocity.go                      # Log-normal velocity profiles.
    └── main.go / server.go              # ❌ MISSING (PC Server entry point).
```

---

## 3. Dependency Injection (Hilt Modules) – The Glue

Here is how Dagger/Hilt wires everything together:

| Module | Provides |
|--------|----------|
| **AppModule** | `Context`, `PreferencesManager`, `Vibrator`, `BluetoothAdapter`, `UsbManager`. |
| **NetworkModule** | `OkHttpClient`, `ConnectionManager`, `UdpDiscovery`. |
| **DatabaseModule** | `AppDatabase`, all DAOs (`CalibrationDao`, `ProfileDao`, etc.). |
| **SensorModule** | `SensorManager`, `CalibrationHelper`, `EnhancedGestureDetector`. |
| **ServiceModule** | `SensorService`, `PresentationModeService`. |
| **RepositoryModule** | Binds `ICalibrationRepository` → `CalibrationRepositoryImpl`, etc. |
| **UseCaseModule** | Provides `CalibrationUseCase`, `ConnectToServerUseCase`, etc. |
| **FeatureModule** | Provides `ConnectionFeature`, `MouseControlFeature`, etc. |
| **ViewModelModule** | Binds `HomeViewModel`, `SettingsViewModel`, etc., with `@ViewModelKey`. |
| **CoroutineModule** | Provides `@IoDispatcher`, `@MainDispatcher`, `@DefaultDispatcher`. |
| **CalibrationModule** | `ICalibrationDataSource` → `CalibrationDataSourceImpl`. |

### The Injection Flow
1. **Activity/Fragment** uses `@AndroidEntryPoint`.
2. **ViewModel** uses `@HiltViewModel` and `@Inject constructor`.
3. **ViewModel** injects `UseCase` or `Feature` dependencies.
4. **UseCase** injects `Repository` interfaces.
5. **RepositoryImpl** injects `DataSource` interfaces and `PreferencesManager`.
6. **DataSource** injects `Dao` (Room) or `ConnectionManager` (Network).

---

## 4. Data Flow – Complete Round Trip

Let's trace a **"Move Cursor"** action to see how all layers interact:

1. **User Gesture**: The user tilts the phone.
2. **SensorService** (Infrastructure) receives gyroscope data.
3. **SensorService** applies calibration (`CalibrationHelper`) and fusion (`MadgwickAHRS`).
4. **SensorService** invokes `onOrientationChanged` callback.
5. **HomeViewModel** (Presentation) receives the callback, calculates `dx` and `dy`.
6. **HomeViewModel** calls `connectionManager.sendMove(dx, dy)`.
7. **ConnectionManager** (Infrastructure) formats JSON and sends via WebSocket/TCP/UDP.
8. **Server** (Go) receives the message, applies smoothing (`bspline.go`), and moves the OS cursor.
9. **Server** sends back ACK or status updates.
10. **ConnectionManager** receives the response and updates `connectionQuality` StateFlow.
11. **HomeViewModel** collects the `connectionQuality` flow and updates the UI State.
12. **HomeScreen** recomposes, updating the signal strength indicator.

---

## 5. Duplicate Files to Delete

| File | Reason | Action |
|------|--------|--------|
| `TcpClient.kt` | Superseded by `ConnectionManager`. | ❌ **Delete** |
| `WebSocketManager.kt` | Superseded by `ConnectionManager`. | ❌ **Delete** |
| `TouchpadSettingsScreen.kt` | Settings are in `SettingsScreen` & `TouchpadScreen`. | ❌ **Delete** |
| `CalibrationProcessScreen.kt` | Duplicate of `CalibrationScreen`. | ❌ **Delete** |
| `GestureDetector.kt` (in `sensors/`) | Superseded by `EnhancedGestureDetector`. | ⚠️ Consider deleting/archiving. |

---

## 6. Missing Files to Implement

### A. Presentation (UI)
| File | Priority |
|------|----------|
| `StatisticsScreen.kt` | High |
| `AboutScreen.kt` | Medium |
| `SensorVisualizerScreen.kt` | High |
| `GestureStudioScreen.kt` | High |
| `EdgeGesturesScreen.kt` | Medium |
| `NetworkDiscoveryScreen.kt` | High |
| `BatteryScreen.kt` | Low |
| `FileTransferScreen.kt` | Medium |
| `OnboardingScreen.kt` (Compose) | Low (Activity exists) |

### B. Infrastructure (Utilities)
| File | Priority |
|------|----------|
| `LogManager.kt` | High |
| `PermissionHelper.kt` | High |
| `VibrateUtils.kt` | High |
| `AudioUtils.kt` | Medium |
| `BluetoothUtils.kt` | Medium |
| `BatterySaver.kt` | Medium |
| `ConnectedDeviceStore.kt` | Medium |
| `QRScanner.kt` | Medium |

### C. Backend (Go Server)
| File | Priority |
|------|----------|
| `main.go` | **Critical** |
| `server.go` | **Critical** |
| `handler.go` | **Critical** |

---

## 7. Gradle Module Structure (Recommended)

For large teams, consider modularising further:

```
app/ (Main application module)
  └── src/main/java/com/airmouse/
      ├── presentation/ (UI)
      ├── domain/ (Pure logic)
      ├── data/ (Implementation)
      └── di/ (Dagger)

core/ (Infrastructure shared across features)
  ├── network/
  ├── sensors/
  ├── utils/
  └── notifications/

feature/home/
feature/settings/
feature/calibration/
feature/touchpad/
feature/voice/
...
```

---

## ✅ Summary

The Air Mouse Android application is built on a **rock-solid Clean Architecture** with:
- **Strict layer separation** (Presentation ↔ Domain ↔ Data ↔ Infrastructure).
- **Unidirectional Data Flow (MVI)** for predictable UI state.
- **Dependency Injection (Hilt)** for loose coupling.
- **A rich, reusable component library** for consistent UI.
- **Comprehensive sensor and network handling** for real‑time control.

The architecture is **scalable, testable, and maintainable**. With the missing files and PC server implemented, it will be a fully production‑ready system.

---

*This architecture document is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.*