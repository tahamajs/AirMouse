# 📘 Air Mouse Settings Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.settings` package contains the **Settings screen** for the Air Mouse application. This is the **most comprehensive settings interface** in the app, providing users with complete control over every aspect of the application's behavior, appearance, and performance.

```
com.airmouse.presentation.ui.settings/
├── SettingsScreen.kt              # Main settings UI
├── SettingsViewModel.kt           # Settings ViewModel
├── SettingsState.kt               # Settings state models
├── SettingsComponents.kt          # Reusable settings UI components
├── SettingsCategories.kt          # Category definitions
└── SettingsConstants.kt           # Settings constants
```

---

## 🎯 1. SettingsScreen

### Purpose
Provides a **comprehensive settings interface** with categorized sections for all configurable aspects of the Air Mouse application.

### Key Features

| Feature | Description |
|---------|-------------|
| **12 Categories** | Cursor, Gesture, AI, Haptic, Notifications, Display, Touchpad, Connection, Privacy, Presentation, About |
| **Live Preview** | Settings changes applied in real-time |
| **Search** | Search settings by keyword |
| **Reset** | Reset individual sections or all settings |
| **Export/Import** | Export and import settings as JSON |
| **Persistent** | All settings saved to PreferencesManager |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

    Scaffold(
        topBar = { /* TopAppBar with title, save, reset */ }
    ) { paddingValues ->
        if (selectedSection == null) {
            SettingsMainScreen(
                uiState = uiState,
                onSectionSelected = { selectedSection = it },
                navigationActions = navigationActions,
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            SectionDetailScreen(
                section = selectedSection!!,
                uiState = uiState,
                viewModel = viewModel,
                navigationActions = navigationActions,
                onBack = { selectedSection = null },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
```

---

## 🎯 2. SettingsState

### Purpose
Defines the **complete state model** for the settings screen, including all settings sections, enums, events, and effects.

### SettingsSection Enum

```kotlin
enum class SettingsSection(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    CURSOR("Cursor", "Movement and sensitivity", Icons.Default.Mouse),
    GESTURE("Gesture", "Click and scroll detection", Icons.Default.Gesture),
    AI("AI & Predictive", "Smart movement prediction", Icons.Default.Psychology),
    HAPTIC("Haptic & Sound", "Feedback preferences", Icons.Default.Vibration),
    NOTIFICATIONS("Notifications", "Alerts and badges", Icons.Default.Notifications),
    DISPLAY("Display", "Theme and appearance", Icons.Default.DisplaySettings),
    THEMES("Themes", "Theme presets and accents", Icons.Default.Palette),
    TOUCHPAD("Touchpad", "Touchpad mode and gesture controls", Icons.Default.TouchApp),
    CONNECTION("Connection", "Network settings", Icons.Default.Wifi),
    PRIVACY("Privacy & Data", "Your data preferences", Icons.Default.PrivacyTip),
    PRESENTATION("Presentation", "Slide control settings", Icons.Default.Slideshow),
    ABOUT("About", "App information", Icons.Default.Info)
}
```

### HapticStrength Enum

```kotlin
enum class HapticStrength(
    val displayName: String,
    val duration: Long
) {
    LIGHT("Light", 20),
    MEDIUM("Medium", 50),
    STRONG("Strong", 80)
}
```

### SettingsUiState

```kotlin
data class SettingsUiState(
    // Cursor Settings
    val sensitivity: Float = 0.5f,
    val accelerationEnabled: Boolean = false,
    val accelerationFactor: Float = 1.5f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val smoothingEnabled: Boolean = true,
    val smoothingFactor: Float = 0.5f,

    // Gesture Settings
    val clickThreshold: Float = 10f,
    val doubleClickInterval: Long = 300,
    val scrollThreshold: Float = 5f,
    val rightClickTilt: Float = 15f,
    val rightClickDuration: Long = 500,
    val gestureDebounce: Long = 100,

    // AI Settings
    val aiSmoothing: Boolean = false,
    val aiBlendFactor: Float = 0.7f,
    val predictive: Boolean = true,
    val predictionStrength: Float = 0.5f,
    val kalmanEnabled: Boolean = true,

    // Haptic Settings
    val hapticEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val soundEnabled: Boolean = false,
    val visualFeedback: Boolean = true,
    val notificationOnGesture: Boolean = false,
    val notificationsEnabled: Boolean = true,

    // Display Settings
    val theme: String = "system",
    val useDynamicColors: Boolean = true,
    val fontSize: Float = 16f,
    val showDebugInfo: Boolean = false,
    val keepScreenOn: Boolean = false,
    val showFps: Boolean = false,

    // Touchpad Settings
    val touchpadActive: Boolean = false,
    val touchpadSensitivity: Float = 1.0f,
    val touchpadCursorSpeed: Float = 1.0f,
    val touchpadPointerSpeed: Int = 50,
    val touchpadAccelerationEnabled: Boolean = true,
    val touchpadInvertVertical: Boolean = false,
    val touchpadInvertHorizontal: Boolean = false,
    val touchpadScrollSpeed: Float = 1.0f,
    val touchpadNaturalScrolling: Boolean = true,
    val touchpadTwoFingerScroll: Boolean = true,
    val touchpadEdgeScrolling: Boolean = false,
    val touchpadScrollInertia: Boolean = true,
    val touchpadTapToClick: Boolean = true,
    val touchpadDoubleTapDelay: Int = 300,
    val touchpadThreeFingerSwipe: Boolean = true,
    val touchpadPinchToZoom: Boolean = true,
    val touchpadRotateToRotate: Boolean = false,
    val touchpadHapticFeedback: Boolean = true,
    val touchpadShowTouchPoints: Boolean = false,

    // Connection Settings
    val autoConnect: Boolean = true,
    val reconnectAttempts: Int = 5,
    val connectionTimeout: Int = 5000,
    val useWebSocket: Boolean = true,
    val useUdpDiscovery: Boolean = true,

    // Privacy Settings
    val anonymousStats: Boolean = true,
    val crashReporting: Boolean = true,
    val clearDataOnExit: Boolean = false,

    // Presentation Settings
    val presentationModeEnabled: Boolean = false,
    val laserPointerSpeed: Float = 1.0f,
    val showPresentationTimer: Boolean = true,
    val autoHideLaser: Boolean = true,

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val isSaving: Boolean = false
)
```

### Settings Events

```kotlin
sealed class SettingsEvent {
    // Cursor Events
    data class UpdateSensitivity(val value: Float) : SettingsEvent()
    object ToggleAcceleration : SettingsEvent()
    data class UpdateAccelerationFactor(val value: Float) : SettingsEvent()
    object ToggleInvertX : SettingsEvent()
    object ToggleInvertY : SettingsEvent()
    object ToggleSmoothing : SettingsEvent()
    data class UpdateSmoothingFactor(val value: Float) : SettingsEvent()

    // Gesture Events
    data class UpdateClickThreshold(val value: Float) : SettingsEvent()
    data class UpdateDoubleClickInterval(val value: Long) : SettingsEvent()
    data class UpdateScrollThreshold(val value: Float) : SettingsEvent()
    data class UpdateRightClickTilt(val value: Float) : SettingsEvent()
    data class UpdateRightClickDuration(val value: Long) : SettingsEvent()

    // AI Events
    object ToggleAiSmoothing : SettingsEvent()
    data class UpdateAiBlendFactor(val value: Float) : SettingsEvent()
    object TogglePredictive : SettingsEvent()
    data class UpdatePredictionStrength(val value: Float) : SettingsEvent()
    object ToggleKalman : SettingsEvent()

    // Haptic Events
    object ToggleHaptic : SettingsEvent()
    data class UpdateHapticStrength(val strength: HapticStrength) : SettingsEvent()
    object ToggleSound : SettingsEvent()
    object ToggleVisualFeedback : SettingsEvent()
    object ToggleNotificationOnGesture : SettingsEvent()
    object ToggleNotifications : SettingsEvent()

    // Display Events
    data class UpdateTheme(val theme: String) : SettingsEvent()
    object ToggleDynamicColors : SettingsEvent()
    data class UpdateFontSize(val value: Float) : SettingsEvent()
    object ToggleDebugInfo : SettingsEvent()
    object ToggleKeepScreenOn : SettingsEvent()
    object ToggleShowFps : SettingsEvent()

    // Touchpad Events
    object ToggleTouchpadActive : SettingsEvent()
    data class UpdateTouchpadSensitivity(val value: Float) : SettingsEvent()
    data class UpdateTouchpadCursorSpeed(val value: Float) : SettingsEvent()
    data class UpdateTouchpadPointerSpeed(val value: Int) : SettingsEvent()
    object ToggleTouchpadAcceleration : SettingsEvent()
    object ToggleTouchpadInvertVertical : SettingsEvent()
    object ToggleTouchpadInvertHorizontal : SettingsEvent()
    data class UpdateTouchpadScrollSpeed(val value: Float) : SettingsEvent()
    object ToggleTouchpadNaturalScrolling : SettingsEvent()
    object ToggleTouchpadTwoFingerScroll : SettingsEvent()
    object ToggleTouchpadEdgeScrolling : SettingsEvent()
    object ToggleTouchpadScrollInertia : SettingsEvent()
    object ToggleTouchpadTapToClick : SettingsEvent()
    data class UpdateTouchpadDoubleTapDelay(val value: Int) : SettingsEvent()
    object ToggleTouchpadThreeFingerSwipe : SettingsEvent()
    object ToggleTouchpadPinchToZoom : SettingsEvent()
    object ToggleTouchpadRotateToRotate : SettingsEvent()
    object ToggleTouchpadHapticFeedback : SettingsEvent()
    object ToggleTouchpadShowTouchPoints : SettingsEvent()

    // Connection Events
    object ToggleAutoConnect : SettingsEvent()
    data class UpdateReconnectAttempts(val value: Int) : SettingsEvent()
    data class UpdateConnectionTimeout(val value: Int) : SettingsEvent()
    object ToggleUseWebSocket : SettingsEvent()
    object ToggleUdpDiscovery : SettingsEvent()

    // Privacy Events
    object ToggleAnonymousStats : SettingsEvent()
    object ToggleCrashReporting : SettingsEvent()
    object ToggleClearDataOnExit : SettingsEvent()

    // Presentation Events
    object TogglePresentationMode : SettingsEvent()
    data class UpdateLaserPointerSpeed(val value: Float) : SettingsEvent()
    object ToggleShowPresentationTimer : SettingsEvent()
    object ToggleAutoHideLaser : SettingsEvent()

    // Actions
    object SaveSettings : SettingsEvent()
    object ResetDefaults : SettingsEvent()
    object ExportSettings : SettingsEvent()
    object ImportSettings : SettingsEvent()
    data class ImportSettingsFromUri(val uri: String) : SettingsEvent()
    object ClearCache : SettingsEvent()
    object ClearAllData : SettingsEvent()
    object OpenSystemSettings : SettingsEvent()
    object OpenAccessibilitySettings : SettingsEvent()
    object OpenWebsite : SettingsEvent()
    object OpenPrivacyPolicy : SettingsEvent()
    object OpenLicense : SettingsEvent()
    object OpenGitHub : SettingsEvent()
    object OpenSupport : SettingsEvent()
}
```

### Settings Effects

```kotlin
sealed class SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect()
    data class NavigateTo(val route: String) : SettingsEffect()
    object NavigateBack : SettingsEffect()
    data class OpenUrl(val url: String) : SettingsEffect()
}
```

---

## 🧩 3. UI Components

### SettingsMainScreen

```kotlin
@Composable
fun SettingsMainScreen(
    uiState: SettingsUiState,
    onSectionSelected: (SettingsSection) -> Unit,
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsHeader(
                onSectionSelected = onSectionSelected,
                navigationActions = navigationActions
            )
        }
        item {
            SettingsOverviewCard(
                uiState = uiState,
                onSectionSelected = onSectionSelected,
                navigationActions = navigationActions
            )
        }
        item {
            SettingsQuickActionsCard(
                navigationActions = navigationActions,
                viewModel = viewModel
            )
        }
        items(SettingsSection.entries) { section ->
            SettingsCard(
                title = section.title,
                description = section.description,
                icon = section.icon,
                onClick = {
                    if (section == SettingsSection.THEMES) {
                        navigationActions.navigateToThemes()
                    } else {
                        onSectionSelected(section)
                    }
                }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
```

### SectionDetailScreen

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    section: SettingsSection,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    navigationActions: NavigationActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(section.icon, contentDescription = section.title)
                        Text(section.title, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionDescription(section) }
            when (section) {
                SettingsSection.CURSOR -> item { CursorSettings(uiState, viewModel) }
                SettingsSection.GESTURE -> item { GestureSettings(uiState, viewModel) }
                SettingsSection.AI -> item { AISettings(uiState, viewModel) }
                SettingsSection.HAPTIC -> item { HapticSettings(uiState, viewModel) }
                SettingsSection.NOTIFICATIONS -> item { NotificationSettings(uiState, viewModel) }
                SettingsSection.DISPLAY -> item { DisplaySettings(uiState, viewModel, navigationActions) }
                SettingsSection.TOUCHPAD -> item { TouchpadSettings(uiState, viewModel) }
                SettingsSection.CONNECTION -> item { ConnectionSettings(uiState, viewModel) }
                SettingsSection.PRIVACY -> item { PrivacySettings(uiState, viewModel) }
                SettingsSection.PRESENTATION -> item { PresentationSettings(uiState, viewModel) }
                SettingsSection.ABOUT -> item { AboutSection(viewModel) }
                SettingsSection.THEMES -> item { ThemesShortcutCard(navigationActions) }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
```

### Reusable Components

#### SettingsSwitch

```kotlin
@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.58f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}
```

#### SettingsSlider

```kotlin
@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    formatValue: (Float) -> String = { "%.2f".format(it) },
    description: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatValue(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
```

#### SettingsCard

```kotlin
@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Navigate")
        }
    }
}
```

---

## 📊 Settings Categories Documentation

### 1. Cursor Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| Sensitivity | Slider | 0.1-2.0 | 0.5 | Higher values make cursor faster |
| Smoothing | Switch | On/Off | On | Smooths cursor movement |
| Smoothing Factor | Slider | 0-1 | 0.5 | Amount of smoothing applied |
| Acceleration | Switch | On/Off | Off | Cursor speed increases with faster movement |
| Acceleration Factor | Slider | 1.0-3.0 | 1.5 | Strength of acceleration |
| Invert X | Switch | On/Off | Off | Swap left/right movement direction |
| Invert Y | Switch | On/Off | Off | Swap up/down movement direction |

### 2. Gesture Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| Click Threshold | Slider | 1-20 | 10 | Sensitivity for click detection |
| Double Click Interval | Slider | 100-800ms | 300 | Max time between clicks for double click |
| Scroll Threshold | Slider | 1-15 | 5 | Sensitivity for scroll detection |
| Right Click Tilt | Slider | 5-45° | 15 | Tilt angle to trigger right click |
| Right Click Duration | Slider | 100-1000ms | 500 | How long to hold tilt for right click |
| Gesture Debounce | Slider | 50-500ms | 100 | Time between gesture detections |

### 3. AI Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| AI Smoothing | Switch | On/Off | Off | Use AI to smooth cursor movement |
| AI Blend Factor | Slider | 0-1 | 0.7 | Balance between raw and AI-smoothed movement |
| Predictive Movement | Switch | On/Off | On | Predict future cursor position |
| Prediction Strength | Slider | 0-1 | 0.5 | How much prediction to apply |
| Kalman Filter | Switch | On/Off | On | Use Kalman filter for smoother tracking |

### 4. Haptic & Sound Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| Haptic Feedback | Switch | On/Off | On | Vibration on actions |
| Haptic Strength | Chip | Light/Medium/Strong | Medium | Intensity of vibration feedback |
| Sound Feedback | Switch | On/Off | Off | Play sounds on actions |
| Visual Feedback | Switch | On/Off | On | Show visual indicators on actions |
| Notification on Gesture | Switch | On/Off | Off | Show notification when gesture is detected |

### 5. Notification Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| App Notifications | Switch | On/Off | On | Show in-app notification badges and alerts |
| Gesture Notifications | Switch | On/Off | Off | Notify when a gesture is detected |
| Sound Alerts | Switch | On/Off | Off | Play a sound for notification events |
| Visual Feedback | Switch | On/Off | On | Show on-screen feedback for actions |

### 6. Display Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| Theme | Chip | System/Light/Dark/Pure Black/High Contrast | System | Choose app color scheme |
| Dynamic Colors | Switch | On/Off | On | Use Material You color scheme |
| Font Size | Slider | 12-24sp | 16 | Base font size for the app |
| Show Debug Info | Switch | On/Off | Off | Display debug information |
| Keep Screen On | Switch | On/Off | Off | Prevent screen from turning off |
| Show FPS | Switch | On/Off | Off | Display frames per second counter |

### 7. Touchpad Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| Touchpad Active | Switch | On/Off | Off | Enable touchpad mode on the phone |
| Sensitivity | Slider | 0.5-2.0 | 1.0 | Overall response of touchpad movement |
| Cursor Speed | Slider | 0.5-2.0 | 1.0 | How fast the pointer moves |
| Pointer Precision | Slider | 20-100% | 50 | Trade speed for finer control |
| Acceleration | Switch | On/Off | On | Increase cursor response for fast swipes |
| Invert Vertical | Switch | On/Off | Off | Flip up/down movement |
| Invert Horizontal | Switch | On/Off | Off | Flip left/right movement |
| Scroll Speed | Slider | 0.5-2.0 | 1.0 | Scroll distance per gesture |
| Natural Scrolling | Switch | On/Off | On | Content follows finger direction |
| Two-Finger Scroll | Switch | On/Off | On | Use two fingers to scroll |
| Edge Scrolling | Switch | On/Off | Off | Scroll when dragging near the edge |
| Scroll Inertia | Switch | On/Off | On | Smooth the end of scrolling gestures |
| Tap to Click | Switch | On/Off | On | Single tap sends left click |
| Double Tap Delay | Slider | 100-600ms | 300 | Maximum delay between taps |
| Three-Finger Swipe | Switch | On/Off | On | Use three-finger swipe shortcuts |
| Pinch to Zoom | Switch | On/Off | On | Allow pinch-to-zoom gestures |
| Rotate to Rotate | Switch | On/Off | Off | Rotate gestures map to rotate actions |
| Haptic Feedback | Switch | On/Off | On | Vibrate on touchpad actions |
| Show Touch Points | Switch | On/Off | Off | Display visible touch markers |

### 8. Connection Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| Auto Connect | Switch | On/Off | On | Auto-connect on app start |
| Reconnect Attempts | Slider | 1-20 | 5 | Number of reconnection attempts |
| Connection Timeout | Slider | 1000-15000ms | 5000 | Timeout for connection attempts |
| Use WebSocket | Switch | On/Off | On | Use WebSocket protocol |
| UDP Discovery | Switch | On/Off | On | Auto-discover servers on network |

### 9. Privacy Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| Anonymous Statistics | Switch | On/Off | On | Send anonymous usage data |
| Crash Reporting | Switch | On/Off | On | Automatically report crashes |
| Clear Data on Exit | Switch | On/Off | Off | Clear all app data when you exit |

### 10. Presentation Settings

| Setting | Type | Range | Default | Description |
|---------|------|-------|---------|-------------|
| Presentation Mode | Switch | On/Off | Off | Enable presentation controls |
| Laser Pointer Speed | Slider | 0.1-2.0 | 1.0 | Speed of laser pointer movement |
| Show Presentation Timer | Switch | On/Off | On | Display timer during presentations |
| Auto-Hide Laser | Switch | On/Off | On | Hide laser pointer when not in use |

---

## 🔧 ViewModel Methods

### Key Methods

```kotlin
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val mouseControlFeature: MouseControlFeature
) : ViewModel() {
    
    // Load settings from preferences
    private fun loadSettings()
    
    // Handle user events
    fun handleEvent(event: SettingsEvent)
    
    // Cursor methods
    private suspend fun updateSensitivity(value: Float)
    private suspend fun toggleAcceleration()
    private suspend fun toggleInvertX()
    private suspend fun toggleSmoothing()
    
    // Gesture methods
    private suspend fun updateClickThreshold(value: Float)
    private suspend fun updateDoubleClickInterval(value: Long)
    private suspend fun updateScrollThreshold(value: Float)
    
    // AI methods
    private suspend fun toggleAiSmoothing()
    private suspend fun updateAiBlendFactor(value: Float)
    private suspend fun togglePredictive()
    
    // Haptic methods
    private suspend fun toggleHaptic()
    private suspend fun updateHapticStrength(strength: HapticStrength)
    private suspend fun toggleSound()
    
    // Display methods
    private suspend fun updateTheme(theme: String)
    private suspend fun toggleDynamicColors()
    private suspend fun updateFontSize(value: Float)
    
    // Touchpad methods
    private suspend fun toggleTouchpadActive()
    private suspend fun updateTouchpadSensitivity(value: Float)
    private suspend fun toggleTouchpadAcceleration()
    
    // Connection methods
    private suspend fun toggleAutoConnect()
    private suspend fun updateReconnectAttempts(value: Int)
    private suspend fun toggleUseWebSocket()
    
    // Privacy methods
    private suspend fun toggleAnonymousStats()
    private suspend fun toggleCrashReporting()
    
    // Presentation methods
    private suspend fun togglePresentationMode()
    private suspend fun updateLaserPointerSpeed(value: Float)
    
    // Actions
    private suspend fun saveSettings()
    private suspend fun resetDefaults()
    private suspend fun exportSettings()
    private suspend fun importSettingsFromUri(uriString: String)
    private suspend fun clearCache()
    private suspend fun clearAllData()
}
```

---

## 📋 Settings Summary

| Category | Settings Count | Description |
|----------|----------------|-------------|
| Cursor | 7 | Movement, sensitivity, smoothing, acceleration |
| Gesture | 6 | Click, scroll, right-click detection |
| AI | 5 | AI smoothing, predictive movement, Kalman filter |
| Haptic | 4 | Haptic, sound, visual feedback |
| Notifications | 4 | App notifications, gesture alerts |
| Display | 6 | Theme, font, debug, screen settings |
| Touchpad | 19 | Sensitivity, gestures, scrolling, feedback |
| Connection | 5 | Auto-connect, reconnect, timeout, protocol |
| Privacy | 3 | Statistics, crash reporting, data clearing |
| Presentation | 4 | Mode, laser pointer, timer, auto-hide |
| **Total** | **63** | **Complete settings coverage** |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Centralized Management** | Single source of truth for all settings |
| **Real-time Updates** | Changes applied immediately |
| **Persistent Storage** | All settings saved to PreferencesManager |
| **Categorized** | Logical grouping of related settings |
| **Searchable** | Settings search functionality |
| **Export/Import** | Settings backup and restore |
| **Reset** | Reset individual sections or all settings |
| **Reactive UI** | StateFlow with automatic updates |

---

**The Settings Screen provides complete control over every aspect of the Air Mouse application, with intuitive controls, real-time feedback, and persistent storage of all user preferences.**