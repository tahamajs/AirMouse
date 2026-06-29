# 📘 Air Mouse UI Screens Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui` package contains **all screen implementations** for the Air Mouse application. Each screen follows the MVI pattern with its own ViewModel, state management, and composable UI.

```
com.airmouse.presentation.ui/
├── home/                          # Home screen (dashboard)
├── settings/                      # Settings screen
├── calibration/                   # Calibration screens
├── touchpad/                      # Touchpad screen
├── voice/                         # Voice commands screen
├── themes/                        # Themes screen
├── accessibility/                 # Accessibility screen
├── profiles/                      # Profiles screen
├── proximity/                     # Proximity screen
├── logs/                          # Server logs screen
├── help/                          # Help screen
├── about/                         # About screen (stub)
├── statistics/                    # Statistics screen (stub)
├── gesture/                       # Gesture studio (stub)
├── edge/                          # Edge gestures (stub)
├── network/                       # Network discovery (stub)
├── sensor/                        # Sensor visualizer (stub)
├── battery/                       # Battery screen (stub)
├── files/                         # File transfer (stub)
├── onboarding/                    # Onboarding screens
├── main/                          # Main screen components
├── components/                    # Reusable UI components
└── theme/                         # Theme definitions
```

---

## 🏠 1. Home Screen (`home/`)

### Purpose
The **main dashboard** of the Air Mouse application. Provides an overview of connection status, sensor data, gestures, and quick actions.

### Key Components

| Component | Purpose |
|-----------|---------|
| **ConnectionToggleButton** | Primary connect/disconnect button with auto-reconnect |
| **StateOverviewBanner** | Shows connection status, calibration state, server info |
| **SensorVisualizer** | 3D phone orientation visualization |
| **MouseMotionPreviewCard** | Live cursor movement preview |
| **QuickActionsRow** | Quick access to calibration, gestures, voice, network |
| **LiveMotionCenterCard** | Real-time sensor data display |
| **CollaborationGateCard** | Calibration progress and readiness |
| **StatsRow** | Clicks, scrolls, session duration |
| **PerformanceCard** | Battery, CPU, RAM, FPS monitoring |
| **RecentGesturesCard** | Recently detected gestures |
| **TipsCard** | Pro tips for users |

### ViewModel State

```kotlin
data class HomeUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val serverIp: String = "",
    val serverPort: Int = ConnectionConfig.DEFAULT_WEBSOCKET_PORT,
    val isCalibrated: Boolean = false,
    val isActive: Boolean = false,
    val isConnecting: Boolean = false,
    val orientationRoll: Float = 0f,
    val orientationPitch: Float = 0f,
    val orientationYaw: Float = 0f,
    val batteryLevel: Int = 100,
    val gestureStats: GestureStats = GestureStats(),
    val logMessages: List<String> = emptyList(),
    val userName: String = "",
    val theme: String = "dark"
)
```

### Usage Example

```kotlin
@Composable
fun HomeScreen(
    navigationActions: NavigationActions,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sensorState by viewModel.sensorState.collectAsStateWithLifecycle()
    
    // ... render UI
}
```

---

## ⚙️ 2. Settings Screen (`settings/`)

### Purpose
Comprehensive settings management with multiple sections.

### Sections

| Section | Settings |
|---------|----------|
| **Cursor** | Sensitivity, smoothing, acceleration, inversion |
| **Gesture** | Click threshold, double-click interval, scroll threshold |
| **AI** | AI smoothing, predictive movement, Kalman filter |
| **Haptic & Sound** | Haptic feedback, sound effects, visual feedback |
| **Display** | Theme, accent color, font size, debug info |
| **Touchpad** | Sensitivity, cursor speed, gestures, scroll |
| **Connection** | Auto-connect, reconnect attempts, timeout |
| **Privacy** | Anonymous stats, crash reporting, clear data |
| **Presentation** | Presentation mode, laser pointer, timer |
| **About** | App info, version, open source licenses |

### ViewModel State

```kotlin
data class SettingsUiState(
    val sensitivity: Float = 0.5f,
    val hapticEnabled: Boolean = true,
    val theme: String = "system",
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)
```

---

## 📊 3. Statistics Screen (`statistics/`)

### Purpose
Displays **usage statistics** and analytics.

### Features

| Feature | Description |
|---------|-------------|
| **Session Stats** | Current session clicks, scrolls, movements, distance |
| **Historical Stats** | Total gestures, most used gesture, custom gesture usage |
| **Daily Stats** | Daily breakdown of usage |
| **Gesture Breakdown** | Gesture type distribution |
| **Export** | Export statistics as JSON or CSV |
| **Reset** | Reset all statistics |

### ViewModel State

```kotlin
data class StatisticsUiState(
    val sessionTime: Long = 0,
    val clicks: Int = 0,
    val scrolls: Int = 0,
    val gesturesDetected: Int = 0,
    val totalDistanceMoved: Float = 0f,
    val averageSpeed: Float = 0f,
    val peakSpeed: Float = 0f,
    val timeRange: TimeRange = TimeRange.TODAY,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## 🎯 4. Calibration Screen (`calibration/`)

### Purpose
Step-by-step **sensor calibration** wizard.

### Phases

| Phase | Description |
|-------|-------------|
| **INTRO** | Show step information and instructions |
| **COUNTDOWN** | Countdown before sampling starts |
| **SAMPLING** | Collect sensor data with progress |

### Components

- `CalibrationScreen.kt` – Main calibration screen
- `CalibrationResultScreen.kt` – Results with quality assessment
- `CalibrationGuideDialog.kt` – Step-by-step guide dialog
- `CalibrationViewModel.kt` – Calibration logic

### ViewModel State

```kotlin
data class CalibrationUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 3,
    val calibrationPhase: CalibrationPhase = CalibrationPhase.INTRO,
    val isCalibrating: Boolean = false,
    val isComplete: Boolean = false,
    val progress: Int = 0,
    val statusMessage: String = "",
    val calibrationQuality: String = "",
    val gyroData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)
)
```

---

## 🖐️ 5. Touchpad Screen (`touchpad/`)

### Purpose
Full **touchpad simulation** with gesture recognition.

### Features

| Feature | Description |
|---------|-------------|
| **Touch Surface** | Large touch area for cursor control |
| **Gesture Detection** | Drag, scroll, swipe, pinch, tap, long-press |
| **Visual Feedback** | Touch points display |
| **Haptic Feedback** | Vibration on touch actions |
| **Quick Presets** | Standard, Precision, Gaming, Presentation |
| **Settings** | Sensitivity, scroll speed, gestures, feedback |

### ViewModel State

```kotlin
data class TouchpadUiState(
    val isActive: Boolean = false,
    val sensitivity: Float = 1.0f,
    val cursorSpeed: Float = 1.0f,
    val scrollSpeed: Float = 1.0f,
    val touchPoints: List<TouchPoint> = emptyList(),
    val lastGesture: String = "",
    val gestureHistory: List<String> = emptyList()
)
```

---

## 🎙️ 6. Voice Commands Screen (`voice/`)

### Purpose
Voice command control with custom commands.

### Features

| Feature | Description |
|---------|-------------|
| **Start/Stop Listening** | Toggle voice recognition |
| **Wake Word** | "Hey Air Mouse" wake word detection |
| **Built-in Commands** | Click, scroll, volume, media control |
| **Custom Commands** | Add, edit, delete custom voice commands |
| **Command History** | Recent command executions |
| **Settings** | Sensitivity, continuous listening, voice feedback |

### ViewModel State

```kotlin
data class VoiceCommandsUiState(
    val isListening: Boolean = false,
    val microphonePermissionGranted: Boolean = false,
    val wakeWordEnabled: Boolean = true,
    val wakeWord: String = "Hey Air Mouse",
    val sensitivity: Float = 0.5f,
    val commandHistory: List<VoiceCommandHistory> = emptyList(),
    val availableCommands: List<VoiceCommand> = emptyList()
)
```

---

## 🎨 7. Themes Screen (`themes/`)

### Purpose
Theme selector with previews and accent colors.

### Features

| Feature | Description |
|---------|-------------|
| **Theme List** | All available themes with preview |
| **Accent Colors** | 14+ accent color options |
| **Premium Themes** | Paid themes with special designs |
| **Live Preview** | Hover preview of themes |
| **Reset** | Reset to default theme |
| **Customization** | Theme customization options |

### ViewModel State

```kotlin
data class ThemesUiState(
    val currentTheme: String = "system",
    val accentColor: AccentColor = AccentColor.ORANGE,
    val isCustomizing: Boolean = false,
    val previewTheme: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## ♿ 8. Accessibility Screen (`accessibility/`)

### Purpose
Accessibility settings for all users.

### Sections

| Section | Settings |
|---------|----------|
| **Display** | High contrast, large text, reduce motion, dark mode |
| **Feedback** | Haptic, sound, voice feedback |
| **Gesture** | Simplified gestures, screen reader support |
| **Voice** | Wake word, voice confirmation, continuous listening |
| **Advanced** | Switch access, dwell click, audio cues |

### ViewModel State

```kotlin
data class AccessibilityUiState(
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reduceMotion: Boolean = false,
    val hapticFeedback: Boolean = true,
    val voiceFeedback: Boolean = false,
    val simplifiedGestures: Boolean = false,
    val voiceWakeWord: Boolean = true
)
```

---

## 👤 9. Profiles Screen (`profiles/`)

### Purpose
User profile management.

### Features

| Feature | Description |
|---------|-------------|
| **CRUD** | Create, read, update, delete profiles |
| **View Modes** | List, grid, compact views |
| **Sorting** | By name, date, usage, favorites |
| **Favorites** | Mark profiles as favorites |
| **Default** | Set default profile |
| **Import/Export** | Profile data import/export |

### ViewModel State

```kotlin
data class ProfileUiState(
    val profiles: List<UserProfile> = emptyList(),
    val filteredProfiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null,
    val sortBy: ProfileSort = ProfileSort.NAME,
    val viewMode: ViewMode = ViewMode.LIST,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)
```

---

## 📱 10. Proximity Screen (`proximity/`)

### Purpose
Bluetooth proximity detection for auto-lock/unlock.

### Features

| Feature | Description |
|---------|-------------|
| **Device Scanning** | Scan for Bluetooth devices |
| **Distance Estimation** | RSSI-based distance calculation |
| **Auto-Lock/Unlock** | Lock/unlock PC based on distance |
| **Calibration** | Calibrate distance estimation |
| **History** | Recent proximity events |
| **Thresholds** | Near/far distance thresholds |

### ViewModel State

```kotlin
data class ProximityUiState(
    val isEnabled: Boolean = false,
    val isNear: Boolean = false,
    val currentDistance: Float? = null,
    val status: String = "Service stopped",
    val connectedDevice: String? = null,
    val rssi: Int = -100,
    val signalStrength: SignalStrength = SignalStrength.NONE,
    val history: List<ProximityHistoryEntry> = emptyList()
)
```

---

## 📋 11. Server Logs Screen (`logs/`)

### Purpose
Real-time log viewer for debugging.

### Features

| Feature | Description |
|---------|-------------|
| **Live Logging** | Real-time log streaming |
| **Search/Filter** | Filter by text, level, tag |
| **Pagination** | Page through logs |
| **Export** | Export as TXT, CSV, JSON, HTML |
| **Statistics** | Log statistics |
| **Auto-Refresh** | Auto-refresh interval |

### ViewModel State

```kotlin
data class ServerLogsUiState(
    val filteredLogs: List<LogEntry> = emptyList(),
    val filter: String = "",
    val level: String = "All",
    val isAutoScroll: Boolean = true,
    val currentPage: Int = 0,
    val pageSize: Int = 50,
    val totalPages: Int = 0,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
)
```

---

## ❓ 12. Help Screen (`help/`)

### Purpose
Searchable help center with categories.

### Features

| Feature | Description |
|---------|-------------|
| **Categories** | Getting started, connection, gestures, calibration |
| **Search** | Search help articles |
| **Favorites** | Mark articles as favorites |
| **Expandable** | Expandable sections |
| **Contact** | Contact support |
| **Feedback** | Submit feedback |

### ViewModel State

```kotlin
data class HelpUiState(
    val searchQuery: String = "",
    val selectedCategory: HelpCategory = HelpCategory.ALL,
    val expandedSections: Set<String> = emptySet(),
    val favoriteSections: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false
)
```

---

## 📊 Screen Architecture Summary

| Screen | Status | ViewModel | State |
|--------|--------|-----------|-------|
| **Home** | ✅ Complete | `HomeViewModel` | `HomeUiState` |
| **Settings** | ✅ Complete | `SettingsViewModel` | `SettingsUiState` |
| **Statistics** | ⚠️ Stub | `StatisticsViewModel` | `StatisticsUiState` |
| **Calibration** | ✅ Complete | `CalibrationViewModel` | `CalibrationUiState` |
| **Touchpad** | ✅ Complete | `TouchpadViewModel` | `TouchpadUiState` |
| **Voice** | ✅ Complete | `VoiceCommandsViewModel` | `VoiceCommandsUiState` |
| **Themes** | ✅ Complete | `ThemesViewModel` | `ThemesUiState` |
| **Accessibility** | ✅ Complete | `AccessibilityViewModel` | `AccessibilityUiState` |
| **Profiles** | ✅ Complete | `ProfilesViewModel` | `ProfileUiState` |
| **Proximity** | ✅ Complete | `ProximityViewModel` | `ProximityUiState` |
| **Server Logs** | ✅ Complete | `ServerLogsViewModel` | `ServerLogsUiState` |
| **Help** | ✅ Complete | `HelpViewModel` | `HelpUiState` |

---

## 🔄 Common Screen Pattern

### 1. State Definition
```kotlin
data class XxxUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: List<Xxx> = emptyList()
)
```

### 2. Event Definition
```kotlin
sealed class XxxEvent {
    object LoadData : XxxEvent()
    data class SelectItem(val id: String) : XxxEvent()
    object RefreshData : XxxEvent()
}
```

### 3. ViewModel
```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val useCase: XxxUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(XxxUiState())
    val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()
    
    fun handleEvent(event: XxxEvent) {
        viewModelScope.launch {
            when (event) {
                XxxEvent.LoadData -> loadData()
                is XxxEvent.SelectItem -> selectItem(event.id)
                // ...
            }
        }
    }
}
```

### 4. Screen Composable
```kotlin
@Composable
fun XxxScreen(
    navigationActions: NavigationActions,
    viewModel: XxxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Render UI based on state
    // Dispatch events on user actions
}
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Unidirectional Data Flow** | State → View → Event → ViewModel → State |
| **Reactive UI** | StateFlow for automatic recomposition |
| **Separation of Concerns** | UI, ViewModel, and State are separate |
| **Testability** | ViewModels are easily unit-testable |
| **Consistency** | All screens follow the same pattern |
| **Navigation** | Screens use NavigationActions for navigation |

---

**The UI Screens Package provides a complete set of screens for the Air Mouse application, each following the MVI pattern with consistent architecture and design.**