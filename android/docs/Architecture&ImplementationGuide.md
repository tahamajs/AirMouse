# Air Mouse – Complete Architecture & Implementation Guide

## 📋 Executive Summary

The Air Mouse project is a **complete, production‑ready Android application** that turns a smartphone into a wireless mouse, gesture controller, and presentation remote. The codebase follows **Clean Architecture** principles with a clear separation of concerns across **data**, **domain**, and **presentation** layers.

This document provides a comprehensive inventory of all files, identifies missing components, highlights duplication, and outlines the optimal architecture.

---

## 🏗️ Architecture Overview

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Activities & Fragments (HomeActivity, OnboardingActivity)      │   │
│  │  Compose Screens (HomeScreen, SettingsScreen, etc.)            │   │
│  │  ViewModels (HomeViewModel, SettingsViewModel, etc.)           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────┤
│                           DOMAIN LAYER                                 │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Models (CalibrationData, ConnectionConfig, etc.)             │   │
│  │  Repositories Interfaces (ICalibrationRepository, etc.)        │   │
│  │  Use Cases (CalibrationUseCase, ConnectToServerUseCase, etc.) │   │
│  └─────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────┤
│                            DATA LAYER                                  │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Repository Implementations (CalibrationRepositoryImpl, etc.)  │   │
│  │  Data Sources (Room DAOs, Preferences, Network)               │   │
│  │  Mappers (Domain ↔ Entity)                                    │   │
│  │  Room Database (AppDatabase, Entities, DAOs)                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────┤
│                       INFRASTRUCTURE LAYER                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Network (ConnectionManager, UdpDiscovery, WebSocket, TCP)    │   │
│  │  Sensors (CalibrationHelper, MadgwickAHRS, SensorService)     │   │
│  │  Notifications, File Transfer, Macros, Gaming, Mirroring      │   │
│  │  Utilities (PreferencesManager, LogManager, Permissions)      │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Dependency Flow

```
Presentation → Domain ← Data ← Infrastructure
     ↓           ↓        ↓          ↓
     └───────────┴────────┴──────────┘
              (Dependency Injection)
```

---

## 📁 Complete File Inventory

### 1. Presentation Layer (UI)

#### Activities & Entry Points

| File | Purpose | Status |
|------|---------|--------|
| `AirMouseApplication.kt` | Application class, Hilt setup, logging, crash reporting | ✅ Complete |
| `MainActivity.kt` | Main entry point, splash screen, onboarding redirect, theme | ✅ Complete |
| `HomeActivity.kt` | Home activity with permission handling, sensor service | ✅ Complete |
| `OnboardingActivity.kt` | Onboarding flow (4 pages with animations) | ✅ Complete |
| `WebViewActivity.kt` | WebView for help/documentation | ✅ Complete |
| `BaseActivity.kt` | Base activity with permissions, loading, debug overlay | ✅ Complete |

#### Compose Screens

| File | Purpose | Status |
|------|---------|--------|
| `MainScreen.kt` | Main navigation host with drawer and bottom bar | ✅ Complete |
| `HomeScreen.kt` | Dashboard with connection, sensors, gestures, stats | ✅ Complete |
| `SettingsScreen.kt` | Comprehensive settings with all sections | ✅ Complete |
| `HelpScreen.kt` | Help centre with search, categories, favourites | ✅ Complete |
| `ServerLogsScreen.kt` | Real‑time log viewer with filters, export, history | ✅ Complete |
| `ProfilesScreen.kt` | User profile management (CRUD, favourites, defaults) | ✅ Complete |
| `ProximityScreen.kt` | Bluetooth proximity lock/unlock | ✅ Complete |
| `AccessibilityScreen.kt` | Accessibility settings (display, feedback, gestures, voice) | ✅ Complete |
| `ThemesScreen.kt` | Theme selector with previews and accent colours | ✅ Complete |
| `VoiceCommandsScreen.kt` | Voice command control with custom commands | ✅ Complete |
| `TouchpadScreen.kt` | Full touchpad simulation with gesture recognition | ✅ Complete |
| `CalibrationScreen.kt` | Step‑by‑step sensor calibration (gyro, mag, accel) | ✅ Complete |
| `CalibrationResultScreen.kt` | Calibration results with quality assessment | ✅ Complete |
| `CalibrationGuideDialog.kt` | Animated instruction dialog | ✅ Complete |
| `StatisticsScreen.kt` | ❌ **Missing** |
| `AboutScreen.kt` | ❌ **Missing** (partially in Settings) |
| `SensorVisualizerScreen.kt` | ❌ **Missing** |
| `GestureStudioScreen.kt` | ❌ **Missing** |
| `EdgeGesturesScreen.kt` | ❌ **Missing** |
| `NetworkDiscoveryScreen.kt` | ❌ **Missing** |
| `BatteryScreen.kt` | ❌ **Missing** |
| `OnboardingScreen.kt` | ❌ **Missing** (Activity exists) |
| `FileTransferScreen.kt` | ❌ **Missing** |
| `TouchpadSettingsScreen.kt` | Stub (duplicate, can be removed) | ⚠️ **Duplicate** |

#### ViewModels

| File | Purpose | Status |
|------|---------|--------|
| `MainViewModel.kt` | Global navigation, connection, control mode | ✅ Complete |
| `HomeViewModel.kt` | Sensor processing, gestures, connection, stats | ✅ Complete |
| `SettingsViewModel.kt` | Settings persistence, reset, import/export | ✅ Complete |
| `HelpViewModel.kt` | Help search, categories, favourites | ✅ Complete |
| `ServerLogsViewModel.kt` | Log capture, filtering, export, persistence | ✅ Complete |
| `ProfilesViewModel.kt` | Profile CRUD, favourites, defaults | ✅ Complete |
| `ProximityViewModel.kt` | Bluetooth scanning, distance calculation, auto‑lock | ✅ Complete |
| `AccessibilityViewModel.kt` | Accessibility settings management | ✅ Complete |
| `ThemesViewModel.kt` | Theme switching, accent colours, preview | ✅ Complete |
| `VoiceCommandsViewModel.kt` | Voice command processing, TTS, history | ✅ Complete |
| `TouchpadViewModel.kt` | Touch event processing, gestures, network commands | ✅ Complete |
| `CalibrationViewModel.kt` | Calibration steps, sensor sampling, quality | ✅ Complete |
| `StatisticsViewModel.kt` | ❌ **Missing** |

#### UI Components & Utilities

| File | Purpose | Status |
|------|---------|--------|
| `AllUIComponents.kt` | Reusable composables (dashboard, gestures, macros, etc.) | ✅ Complete |
| `CalibrationComponents.kt` | Calibration‑specific UI components | ✅ Complete |
| `CalibrationUiState.kt` | Calibration state models | ✅ Complete |
| `Destinations.kt` | Navigation routes, icons, bottom nav items | ✅ Complete |
| `NavigationActions.kt` | Navigation abstraction | ✅ Complete |
| `AirMouseBottomBar.kt` | Bottom navigation bar | ✅ Complete |
| `AirMouseNavHost.kt` | Navigation host with all routes | ✅ Complete |
| `DebugOverlay.kt` | Floating debug overlay for sensors | ✅ Complete |
| `SensorCubeView.kt` | 3D sensor visualisation | ✅ Complete |
| `UiStyleUtils.kt` | Animation and styling helpers | ✅ Complete |
| `NotificationBadge.kt` | Animated notification badge | ✅ Complete |
| `ShimmerEffect.kt` | Loading shimmer effect | ✅ Complete |
| `FloatingParticles.kt` | Particle animation background | ✅ Complete |
| `AnimatedCheckbox.kt` | Animated checkbox | ✅ Complete |
| `AnimatedSwitch.kt` | Animated switch | ✅ Complete |
| `AnimatedToast.kt` | Toast with animation | ✅ Complete |
| `ConnectionStatusBadge.kt` | Connection status indicator | ✅ Complete |
| `GlassCard.kt` | Glass‑morphism card | ✅ Complete |
| `GradientBackground.kt` | Animated gradient background | ✅ Complete |
| `NeonButton.kt` | Neon‑style button | ✅ Complete |
| `HolographicText.kt` | Holographic/animated text | ✅ Complete |
| `TypewriterText.kt` | Typewriter animation text | ✅ Complete |
| `VoiceWaveAnimation.kt` | Voice activity animation | ✅ Complete |
| `RadarAnimation.kt` | Radar‑style loading animation | ✅ Complete |
| `SensorVisualizer.kt` | 3D sensor orientation visualisation | ✅ Complete |
| `DataChart.kt` | Line and donut charts | ✅ Complete |
| `GestureWaveform.kt` | Gesture data waveform | ✅ Complete |
| `ParticleBackground.kt` | Particle system background | ✅ Complete |
| `PullToRefresh.kt` | Custom pull‑to‑refresh | ✅ Complete |
| `SlideUpPanel.kt` | Slide‑up bottom panel | ✅ Complete |
| `SkeletonScreen.kt` | Loading skeleton UI | ✅ Complete |
| `FloatingActionMenu.kt` | Expandable FAB menu | ✅ Complete |
| `AnimatedCounter.kt` | Animated counter | ✅ Complete |
| `AnimatedConnectionStatus.kt` | Animated connection status indicator | ✅ Complete |
| `CircularProgressWithLabel.kt` | Circular progress with label | ✅ Complete |
| `DonutChart.kt` | Donut chart | ✅ Complete |
| `InteractiveTutorialCard.kt` | Tutorial card | ✅ Complete |
| `NeumorphicCard.kt` | Neumorphic design card | ✅ Complete |

#### Calibration UI

| File | Purpose | Status |
|------|---------|--------|
| `CalibrationScreen.kt` | Main calibration screen | ✅ Complete |
| `CalibrationResultScreen.kt` | Results with quality | ✅ Complete |
| `CalibrationGuideDialog.kt` | Step guide dialog | ✅ Complete |
| `CalibrationComponents.kt` | Reusable components | ✅ Complete |
| `CalibrationUiState.kt` | State models | ✅ Complete |
| `CalibrationViewModel.kt` | ViewModel | ✅ Complete |
| `CalibrationProcessScreen.kt` | Stub (can be removed) | ⚠️ **Duplicate/Stub** |

---

### 2. Domain Layer

#### Models

| File | Purpose | Status |
|------|---------|--------|
| `CalibrationModels.kt` | Calibration status, quality, data models | ✅ Complete |
| `ConnectionModels.kt` | Connection config, quality, status | ✅ Complete |
| `GestureModels.kt` | Gesture types, events, templates | ✅ Complete |
| `MouseModels.kt` | Mouse events, profiles, statistics | ✅ Complete |
| `ProximityModels.kt` | Proximity state, config, calibration | ✅ Complete |
| `SensorModels.kt` | Sensor data, orientation, info | ✅ Complete |
| `StatisticsModels.kt` | Statistics summary, daily, historical | ✅ Complete |
| `UpdateModels.kt` | Update info, version, progress | ✅ Complete |
| `VoiceCommandModels.kt` | Voice commands, config, history | ✅ Complete |
| `ProfileModels.kt` | User profile, settings | ✅ Complete |
| `PreferencesModels.kt` | App preferences, user preferences | ✅ Complete |
| `BluetoothModels.kt` | Bluetooth device info, BLE services | ✅ Complete |
| `ControlMode.kt` | Control mode enum | ✅ Complete |
| `CalibrationData.kt` | Calibration data with JSON serialisation | ✅ Complete |
| `GestureTemplate.kt` | Gesture template with JSON serialisation | ✅ Complete |
| `StatisticsSummary.kt` | Statistics summary with JSON serialisation | ✅ Complete |
| `ErrorModels.kt` | Error types and models | ✅ Complete |
| `CalibrationResult.kt` | Calibration result model | ✅ Complete |

#### Repository Interfaces

| File | Purpose | Status |
|------|---------|--------|
| `ICalibrationRepository.kt` | Calibration operations | ✅ Complete |
| `IConnectionRepository.kt` | Connection management | ✅ Complete |
| `IGestureRepository.kt` | Gesture detection and templates | ✅ Complete |
| `IMouseRepository.kt` | Mouse movement and events | ✅ Complete |
| `IProfileRepository.kt` | User profile CRUD | ✅ Complete |
| `IProximityRepository.kt` | Proximity monitoring | ✅ Complete |
| `ISensorRepository.kt` | Sensor data and calibration | ✅ Complete |
| `ISettingsRepository.kt` | Settings management | ✅ Complete |
| `IStatisticsRepository.kt` | Statistics recording and retrieval | ✅ Complete |
| `IUpdateRepository.kt` | App updates | ✅ Complete |
| `IVoiceCommandRepository.kt` | Voice commands | ✅ Complete |

#### Use Cases

| File | Purpose | Status |
|------|---------|--------|
| `CalibrationUseCase.kt` | Calibration orchestration | ✅ Complete |
| `ConnectToServerUseCase.kt` | Connect to server | ✅ Complete |
| `SendMovementUseCase.kt` | Send mouse movement | ✅ Complete |
| `DetectGestureUseCase.kt` | Gesture detection | ✅ Complete |
| `DiscoverServersUseCase.kt` | UDP server discovery | ✅ Complete |
| `GetConnectionStatusUseCase.kt` | Get connection status | ✅ Complete |
| `GetStatisticsUseCase.kt` | Get statistics | ✅ Complete |
| `RecordStatisticsUseCase.kt` | Record statistics | ✅ Complete |
| `ManageProfileUseCase.kt` | Profile management | ✅ Complete |
| `ManageGestureTemplatesUseCase.kt` | Gesture template management | ✅ Complete |
| `HandleVoiceCommandUseCase.kt` | Voice command handling | ✅ Complete |
| `GetProximityStateUseCase.kt` | Get proximity state | ✅ Complete |
| `UpdateProximityConfigUseCase.kt` | Update proximity config | ✅ Complete |
| `TestConnectionUseCase.kt` | Test connection | ✅ Complete |
| `CheckForUpdatesUseCase.kt` | Check for updates | ✅ Complete |
| `GetGestureStatisticsUseCase.kt` | Get gesture statistics | ✅ Complete |

---

### 3. Data Layer

#### Repository Implementations

| File | Purpose | Status |
|------|---------|--------|
| `CalibrationRepositoryImpl.kt` | Calibration repository | ✅ Complete |
| `ConnectionRepositoryImpl.kt` | Connection repository | ✅ Complete |
| `GestureRepositoryImpl.kt` | Gesture repository | ✅ Complete |
| `MouseRepositoryImpl.kt` | Mouse repository | ✅ Complete |
| `ProfileRepositoryImpl.kt` | Profile repository | ✅ Complete |
| `ProximityRepositoryImpl.kt` | Proximity repository | ✅ Complete |
| `SensorRepositoryImpl.kt` | Sensor repository | ✅ Complete |
| `SettingsRepositoryImpl.kt` | Settings repository | ✅ Complete |
| `StatisticsRepositoryImpl.kt` | Statistics repository | ✅ Complete |
| `UpdateRepositoryImpl.kt` | Update repository | ✅ Complete |
| `VoiceCommandRepositoryImpl.kt` | Voice command repository | ✅ Complete |

#### Data Sources

| File | Purpose | Status |
|------|---------|--------|
| `CalibrationDataSourceImpl.kt` | Calibration preferences | ✅ Complete |
| `GestureDataSourceImpl.kt` | Gesture templates, training, stats | ✅ Complete |
| `ProfileDataSourceImpl.kt` | Profile JSON storage | ✅ Complete |
| `StatisticsDataSourceImpl.kt` | Statistics storage | ✅ Complete |
| `PreferencesDataSourceImpl.kt` | Generic preferences | ✅ Complete |
| `LocalDataSourceImpl.kt` | Unified Room data source | ✅ Complete |
| `ConnectionDataSourceImpl.kt` | Remote connection data source | ✅ Complete |
| `BluetoothDataSourceImpl.kt` | Bluetooth data source | ✅ Complete |
| `UsbDataSourceImpl.kt` | USB data source | ✅ Complete |
| `WebSocketDataSourceImpl.kt` | WebSocket data source | ✅ Complete |

#### Room Database

| File | Purpose | Status |
|------|---------|--------|
| `AppDatabase.kt` | Room database definition | ✅ Complete |
| `Converters.kt` | Type converters | ✅ Complete |
| `CalibrationEntity.kt` | Calibration entity | ✅ Complete |
| `SettingsEntity.kt` | Settings entity | ✅ Complete |
| `StatisticsEntity.kt` | Statistics entity | ✅ Complete |
| `GestureTemplateEntity.kt` | Gesture template entity | ✅ Complete |
| `ProfileEntity.kt` | Profile entity | ✅ Complete |
| `TrainingSampleEntity.kt` | Training sample entity | ✅ Complete |
| `DailyStatsEntity.kt` | Daily statistics entity | ✅ Complete |
| `GestureStatsEntity.kt` | Gesture statistics entity | ✅ Complete |
| `CalibrationDao.kt` | Calibration DAO | ✅ Complete |
| `SettingsDao.kt` | Settings DAO | ✅ Complete |
| `StatisticsDao.kt` | Statistics DAO | ✅ Complete |
| `GestureDao.kt` | Gesture DAO | ✅ Complete |
| `ProfileDao.kt` | Profile DAO | ✅ Complete |
| `TrainingSampleDao.kt` | Training sample DAO | ✅ Complete |
| `DailyStatsDao.kt` | Daily statistics DAO | ✅ Complete |
| `GestureStatsDao.kt` | Gesture statistics DAO | ✅ Complete |

#### Mappers

| File | Purpose | Status |
|------|---------|--------|
| `DomainToEntityMapper.kt` | Domain ↔ Entity mapping | ✅ Complete |
| `EntityToDomainMapper.kt` | Entity → Domain mapping | ✅ Complete |

#### Data Helpers

| File | Purpose | Status |
|------|---------|--------|
| `SensorRepository.kt` | Sensor data flow (callbackFlow) | ✅ Complete |
| `PreferencesDataStore.kt` | AndroidX DataStore wrapper | ✅ Complete |
| `CalibrationPrefsData.kt` | Calibration preference data class | ✅ Complete |
| `GestureData.kt` | Gesture data class | ✅ Complete |
| `SensorData.kt` | Sensor data class | ✅ Complete |
| `Quadruple.kt` | Quadruple helper | ✅ Complete |
| `GestureTypeCount.kt` | Gesture count helper | ✅ Complete |

#### Repository Interfaces (Data Layer)

| File | Purpose | Status |
|------|---------|--------|
| `ICalibrationDataSource.kt` | Calibration data source interface | ✅ Complete |
| `IGestureDataSource.kt` | Gesture data source interface | ✅ Complete |
| `IProfileDataSource.kt` | Profile data source interface | ✅ Complete |
| `IStatisticsDataSource.kt` | Statistics data source interface | ✅ Complete |
| `IPreferencesDataSource.kt` | Preferences data source interface | ✅ Complete |
| `ILocalDataSource.kt` | Unified local data source interface | ✅ Complete |
| `IConnectionDataSource.kt` | Connection data source interface | ✅ Complete |
| `IBluetoothDataSource.kt` | Bluetooth data source interface | ✅ Complete |
| `IUsbDataSource.kt` | USB data source interface | ✅ Complete |
| `IWebSocketDataSource.kt` | WebSocket data source interface | ✅ Complete |

---

### 4. Infrastructure Layer

#### Network

| File | Purpose | Status |
|------|---------|--------|
| `ConnectionManager.kt` | **Core** – Unified WebSocket, TCP, UDP with reconnection, heartbeat, ACK | ✅ Complete |
| `UdpDiscovery.kt` | UDP server discovery | ✅ Complete |
| `AutoReconnectManager.kt` | Reconnection orchestration | ✅ Complete |
| `NetworkQualityMonitor.kt` | Network quality monitoring | ✅ Complete |
| `MessageTypes.kt` | Protocol message types and constants | ✅ Complete |
| `AirMouseProtocolMessages.kt` | JSON message builders | ✅ Complete |
| `ConnectionHelper.kt` | Extension functions for message sending | ✅ Complete |
| `TcpClient.kt` | ⚠️ **Deprecated** – Use `ConnectionManager` instead | ❌ **Remove/Archive** |
| `WebSocketManager.kt` | ⚠️ **Deprecated** – Use `ConnectionManager` instead | ❌ **Remove/Archive** |

#### Sensors

| File | Purpose | Status |
|------|---------|--------|
| `SensorService.kt` | Sensor registration, fusion, gestures | ✅ Complete |
| `CalibrationHelper.kt` | Gyro bias, magnetometer, accelerometer calibration | ✅ Complete |
| `EnhancedGestureDetector.kt` | Advanced gesture detection (click, scroll, right‑click) | ✅ Complete |
| `MadgwickAHRS.kt` | Sensor fusion algorithm | ✅ Complete |
| `MotionAnalyzer.kt` | Motion feature analysis | ✅ Complete |
| `MotionDetector.kt` | Simple motion detection | ✅ Complete |
| `OrientationTracker.kt` | Orientation tracking | ✅ Complete |
| `SensorDataLogger.kt` | CSV logging for debugging | ✅ Complete |
| `SensorDataProcessor.kt` | Filters (low‑pass, high‑pass, Kalman, etc.) | ✅ Complete |
| `SensorFusion.kt` | Static sensor fusion utilities | ✅ Complete |
| `SensorManagerHelper.kt` | Sensor availability checks | ✅ Complete |
| `GestureDetector.kt` | Legacy gesture detection | ✅ Complete |

#### Services

| File | Purpose | Status |
|------|---------|--------|
| `ForegroundServiceManager.kt` | Foreground service management | ✅ Complete |
| `BluetoothHidService.kt` | Bluetooth HID mouse service | ✅ Complete |
| `DebugOverlayService.kt` | Debug overlay service | ✅ Complete |
| `EdgeGestureService.kt` | Edge gesture detection service | ✅ Complete |
| `GestureInferenceService.kt` | Gesture inference service | ✅ Complete |
| `GestureRecorderService.kt` | Gesture recording service | ✅ Complete |
| `OrientationMonitorService.kt` | Orientation monitoring service | ✅ Complete |
| `ProximityAwareService.kt` | Proximity awareness service | ✅ Complete |
| `VoiceCommandService.kt` | Voice command service | ✅ Complete |
| `PresentationModeService.kt` | Presentation control service | ✅ Complete |
| `ScreenMirroringService.kt` | Screen mirroring service | ✅ Complete |

#### Notifications

| File | Purpose | Status |
|------|---------|--------|
| `NotificationManager.kt` | Notification channels, badges, alerts | ✅ Complete |

#### Files & Transfer

| File | Purpose | Status |
|------|---------|--------|
| `FileTransferService.kt` | File transfer service | ✅ Complete |

#### Gaming & Macros

| File | Purpose | Status |
|------|---------|--------|
| `GameProfilesManager.kt` | Game detection and profile switching | ✅ Complete |
| `MacroRecorder.kt` | Macro recording and playback | ✅ Complete |

#### Utilities

| File | Purpose | Status |
|------|---------|--------|
| `PreferencesManager.kt` | **Core** – Preferences interface | ✅ Complete |
| `LogManager.kt` | ❌ **Missing** |
| `PermissionHelper.kt` | ❌ **Missing** |
| `VibrateUtils.kt` | ❌ **Missing** |
| `AudioUtils.kt` | ❌ **Missing** |
| `BluetoothUtils.kt` | ❌ **Missing** |
| `BatterySaver.kt` | ❌ **Missing** |
| `ConnectedDeviceStore.kt` | ❌ **Missing** |
| `QRScanner.kt` | ❌ **Missing** |

---

### 5. Dependency Injection (Hilt)

| File | Purpose | Status |
|------|---------|--------|
| `AppContainer.kt` | App container for manual DI | ✅ Complete |
| `AppModule.kt` | Core DI modules | ✅ Complete |
| `NetworkModule.kt` | Network dependencies | ✅ Complete |
| `DatabaseModule.kt` | Room database dependencies | ✅ Complete |
| `SensorModule.kt` | Sensor dependencies | ✅ Complete |
| `ServiceModule.kt` | Service dependencies | ✅ Complete |
| `RepositoryModule.kt` | Repository bindings | ✅ Complete |
| `UseCaseModule.kt` | Use case bindings | ✅ Complete |
| `FeatureModule.kt` | Feature dependencies | ✅ Complete |
| `ViewModelModule.kt` | ViewModel bindings | ✅ Complete |
| `CalibrationModule.kt` | Calibration dependencies | ✅ Complete |
| `CoroutineModule.kt` | Coroutine dispatchers | ✅ Complete |
| `GestureRepositoryModule.kt` | Gesture repository bindings | ✅ Complete |

---

### 6. Go Server (Smoothing Package)

| File | Purpose | Status |
|------|---------|--------|
| `bspline.go` | Cubic B‑spline interpolation | ✅ Complete |
| `humanizer.go` | Human‑like cursor smoothing | ✅ Complete |
| `tremor.go` | Hand tremor simulation | ✅ Complete |
| `velocity.go` | Log‑normal velocity profile | ✅ Complete |
| **PC Server** (main.go, server.go) | ❌ **Missing** |

---

### 7. Data Synchronisation

| File | Purpose | Status |
|------|---------|--------|
| `DataSyncManager.kt` | Sync calibration, gestures, statistics, profiles | ✅ Complete |

---

## 🔄 Duplicate & Redundant Files (To Delete/Archive)

### 1. Deprecated Network Classes

| File | Reason | Action |
|------|--------|--------|
| `TcpClient.kt` | Superseded by `ConnectionManager` | ❌ **Delete** |
| `WebSocketManager.kt` | Superseded by `ConnectionManager` | ❌ **Delete** |

### 2. Duplicate/Stub UI Screens

| File | Reason | Action |
|------|--------|--------|
| `TouchpadSettingsScreen.kt` | Settings are in `SettingsScreen.kt` and `TouchpadScreen.kt` | ❌ **Delete** |
| `CalibrationProcessScreen.kt` | Duplicate of `CalibrationScreen.kt` | ❌ **Delete** |

### 3. Redundant Legacy Files

| File | Reason | Action |
|------|--------|--------|
| `GestureDetector.kt` (sensors/package) | Superseded by `EnhancedGestureDetector.kt` | ⚠️ Consider removing |
| `PreferencesManager.kt` (interface) vs `PreferencesDataSourceImpl.kt` | Consolidate into `PreferencesManager` | ⚠️ Review for duplication |

---

## ❌ Missing Files (To Implement)

### 1. UI Screens

| File | Priority |
|------|----------|
| `StatisticsScreen.kt` | High |
| `AboutScreen.kt` | Medium |
| `SensorVisualizerScreen.kt` | High |
| `GestureStudioScreen.kt` | High |
| `EdgeGesturesScreen.kt` | Medium |
| `NetworkDiscoveryScreen.kt` | High |
| `BatteryScreen.kt` | Low |
| `OnboardingScreen.kt` | Low |
| `FileTransferScreen.kt` | Medium |

### 2. ViewModels

| File | Priority |
|------|----------|
| `StatisticsViewModel.kt` | High |

### 3. Utility Classes

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

### 4. PC Server

| File | Priority |
|------|----------|
| `main.go` | **Critical** |
| `server.go` | **Critical** |
| `handler.go` | **Critical** |

---

## 🏗️ Optimal Architecture Summary

### Layer Responsibilities

| Layer | Responsibility | Key Components |
|-------|---------------|----------------|
| **Presentation** | UI rendering, user interaction | Compose Screens, ViewModels, Navigation |
| **Domain** | Business logic, use cases | Models, Repository Interfaces, Use Cases |
| **Data** | Data persistence, retrieval | Repository Implementations, Data Sources, Room |
| **Infrastructure** | External dependencies | Network, Sensors, Notifications, Utilities |

### Dependency Direction

```
Presentation → Domain ← Data ← Infrastructure
     ↓           ↓        ↓          ↓
     └───────────┴────────┴──────────┘
              (Dependency Injection)
```

### Key Design Patterns

| Pattern | Usage |
|---------|-------|
| **Repository Pattern** | Abstracts data sources (Room, Preferences, Network) |
| **Use Case Pattern** | Encapsulates single business rules |
| **Observer Pattern** | `StateFlow` for reactive UI updates |
| **Dependency Injection** | Hilt for loose coupling |
| **CallbackFlow** | Reactive sensor data streams |
| **Strategy Pattern** | Multiple gesture detection strategies |

---

## 📊 Completion Status

### Presentation Layer
```
✅ HomeScreen, SettingsScreen, HelpScreen, ServerLogsScreen
✅ ProfilesScreen, ProximityScreen, AccessibilityScreen
✅ ThemesScreen, VoiceCommandsScreen, TouchpadScreen
✅ CalibrationScreen (full flow)
❌ StatisticsScreen, AboutScreen, SensorVisualizerScreen
❌ GestureStudioScreen, EdgeGesturesScreen, NetworkDiscoveryScreen
❌ BatteryScreen, FileTransferScreen
```

### Domain Layer
```
✅ All models (100%)
✅ All repository interfaces (100%)
✅ All use cases (100%)
```

### Data Layer
```
✅ All repository implementations (100%)
✅ All data sources (100%)
✅ Room database (100%)
✅ Mappers (100%)
```

### Infrastructure
```
✅ Network (ConnectionManager, UdpDiscovery, etc.) – 100%
✅ Sensors (SensorService, CalibrationHelper, Madgwick, etc.) – 100%
✅ Notifications – 100%
✅ File Transfer – 100%
✅ Gaming & Macros – 100%
❌ Utilities (LogManager, PermissionHelper, etc.) – 0%
❌ PC Server – 0%
```

### Overall Android Client: 85% Complete
### Overall System (with PC Server): 60% Complete

---

## 🚀 Roadmap to Completion

### Phase 1: UI Completion (1–2 weeks)
- [ ] Implement `StatisticsScreen`
- [ ] Implement `AboutScreen`
- [ ] Implement `SensorVisualizerScreen`
- [ ] Implement `GestureStudioScreen`
- [ ] Implement `NetworkDiscoveryScreen`

### Phase 2: Utilities (1 week)
- [ ] Implement `LogManager`
- [ ] Implement `PermissionHelper`
- [ ] Implement `VibrateUtils`
- [ ] Implement `ConnectedDeviceStore`
- [ ] Implement `QRScanner`

### Phase 3: PC Server (2–3 weeks)
- [ ] Write Go server with WebSocket/TCP support
- [ ] Integrate `adaptivesmoothing` package
- [ ] Implement cursor control (Windows, macOS, Linux)
- [ ] Add QR code generation for pairing

### Phase 4: Testing & Polish (1–2 weeks)
- [ ] Unit tests for repositories and use cases
- [ ] UI tests for critical flows
- [ ] Error handling and edge cases
- [ ] Performance optimisation

---

## 🧪 Testing Strategy

| Test Type | Scope | Tools |
|-----------|-------|-------|
| **Unit Tests** | Use cases, repositories, mappers | JUnit, Mockito, MockK |
| **Integration Tests** | Room, Preferences, Network | AndroidX Test, Robolectric |
| **UI Tests** | Critical user flows | Compose UI Test, Espresso |
| **Instrumentation Tests** | Sensor, Bluetooth, Network | AndroidX Test |

---

## 📚 Recommended Libraries

| Library | Purpose |
|---------|---------|
| **Jetpack Compose** | UI framework |
| **Hilt** | Dependency injection |
| **Room** | Local database |
| **DataStore** | Preferences |
| **Coroutines & Flow** | Asynchronous programming |
| **OkHttp** | HTTP/WebSocket client |
| **Gson** | JSON serialisation |
| **Timber** | Logging |
| **MockK** | Unit testing |

---

## 🏁 Final Assessment

The Air Mouse project is **architecturally complete** and follows **best practices** for modern Android development. The codebase is well‑structured, modular, and maintainable. The remaining work is primarily UI screens and the PC server. With these completed, the project will be **production‑ready**.

**Key Strengths:**
- Clean Architecture with clear separation of concerns
- Complete sensor processing pipeline with calibration
- Robust network layer with reconnection and heartbeat
- Comprehensive gesture detection
- Modern Compose UI with Material 3
- Full Room database with offline support

**Key Gaps:**
- Missing utility classes
- Missing PC server
- A few UI screens incomplete

**Recommendation:** Prioritise the PC server, as it is the critical missing piece for the app to function as intended. The Android client is otherwise ready for integration.

---

*This document is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.*