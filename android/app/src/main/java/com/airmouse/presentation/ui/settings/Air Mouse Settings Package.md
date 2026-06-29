


# 📘 Air Mouse Settings Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.settings` package is the **most comprehensive settings interface** in the Air Mouse application. It provides users with complete control over every aspect of the application's behavior, appearance, and performance.

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

## 🎯 1. SettingsScreen – Main UI

### Purpose
The **main settings interface** that displays all settings categories and handles navigation to individual section detail screens.

### Key Features

| Feature | Description |
|---------|-------------|
| **12 Categories** | Cursor, Gesture, AI, Haptic, Notifications, Display, Touchpad, Connection, Privacy, Presentation, Themes, About |
| **Live Preview** | Settings changes applied in real-time |
| **Quick Actions** | Save and Reset buttons |
| **Section Navigation** | Drill-down into each category |
| **Status Overview** | Quick summary of current settings |

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

    // Handle effects (toasts, navigation)
    LaunchedEffect(effect) {
        when (effect) {
            is SettingsEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            is SettingsEffect.NavigateTo -> navigationActions.navigateTo(effect.route)
            is SettingsEffect.NavigateBack -> onBack()
            null -> { /* Ignore */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Access tuning, theme, and connection controls", fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.handleEvent(SettingsEvent.SaveSettings) }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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

## 🎯 2. SettingsState – State Models

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

### SettingsUiState – Complete State

| Category | Properties | Count |
|----------|------------|-------|
| **Cursor** | sensitivity, accelerationEnabled, accelerationFactor, invertX, invertY, smoothingEnabled, smoothingFactor | 7 |
| **Gesture** | clickThreshold, doubleClickInterval, scrollThreshold, rightClickTilt, rightClickDuration, gestureDebounce | 6 |
| **AI** | aiSmoothing, aiBlendFactor, predictive, predictionStrength, kalmanEnabled | 5 |
| **Haptic** | hapticEnabled, hapticStrength, soundEnabled, visualFeedback, notificationOnGesture, notificationsEnabled | 6 |
| **Display** | theme, useDynamicColors, fontSize, showDebugInfo, keepScreenOn, showFps | 6 |
| **Touchpad** | touchpadActive, touchpadSensitivity, touchpadCursorSpeed, touchpadPointerSpeed, touchpadAccelerationEnabled, touchpadInvertVertical, touchpadInvertHorizontal, touchpadScrollSpeed, touchpadNaturalScrolling, touchpadTwoFingerScroll, touchpadEdgeScrolling, touchpadScrollInertia, touchpadTapToClick, touchpadDoubleTapDelay, touchpadThreeFingerSwipe, touchpadPinchToZoom, touchpadRotateToRotate, touchpadHapticFeedback, touchpadShowTouchPoints | 19 |
| **Connection** | autoConnect, reconnectAttempts, connectionTimeout, useWebSocket, useUdpDiscovery | 5 |
| **Privacy** | anonymousStats, crashReporting, clearDataOnExit | 3 |
| **Presentation** | presentationModeEnabled, laserPointerSpeed, showPresentationTimer, autoHideLaser | 4 |
| **UI** | isLoading, error, success, isSaving | 4 |
| **Total** | | **65** |

### Settings Events – Complete List

#### Cursor Events (7)
```kotlin
data class UpdateSensitivity(val value: Float)
object ToggleAcceleration
data class UpdateAccelerationFactor(val value: Float)
object ToggleInvertX
object ToggleInvertY
object ToggleSmoothing
data class UpdateSmoothingFactor(val value: Float)
```

#### Gesture Events (5)
```kotlin
data class UpdateClickThreshold(val value: Float)
data class UpdateDoubleClickInterval(val value: Long)
data class UpdateScrollThreshold(val value: Float)
data class UpdateRightClickTilt(val value: Float)
data class UpdateRightClickDuration(val value: Long)
```

#### AI Events (5)
```kotlin
object ToggleAiSmoothing
data class UpdateAiBlendFactor(val value: Float)
object TogglePredictive
data class UpdatePredictionStrength(val value: Float)
object ToggleKalman
```

#### Haptic Events (6)
```kotlin
object ToggleHaptic
data class UpdateHapticStrength(val strength: HapticStrength)
object ToggleSound
object ToggleVisualFeedback
object ToggleNotificationOnGesture
object ToggleNotifications
```

#### Display Events (6)
```kotlin
data class UpdateTheme(val theme: String)
object ToggleDynamicColors
data class UpdateFontSize(val value: Float)
object ToggleDebugInfo
object ToggleKeepScreenOn
object ToggleShowFps
```

#### Touchpad Events (19)
```kotlin
object ToggleTouchpadActive
data class UpdateTouchpadSensitivity(val value: Float)
data class UpdateTouchpadCursorSpeed(val value: Float)
data class UpdateTouchpadPointerSpeed(val value: Int)
object ToggleTouchpadAcceleration
object ToggleTouchpadInvertVertical
object ToggleTouchpadInvertHorizontal
data class UpdateTouchpadScrollSpeed(val value: Float)
object ToggleTouchpadNaturalScrolling
object ToggleTouchpadTwoFingerScroll
object ToggleTouchpadEdgeScrolling
object ToggleTouchpadScrollInertia
object ToggleTouchpadTapToClick
data class UpdateTouchpadDoubleTapDelay(val value: Int)
object ToggleTouchpadThreeFingerSwipe
object ToggleTouchpadPinchToZoom
object ToggleTouchpadRotateToRotate
object ToggleTouchpadHapticFeedback
object ToggleTouchpadShowTouchPoints
```

#### Connection Events (5)
```kotlin
object ToggleAutoConnect
data class UpdateReconnectAttempts(val value: Int)
data class UpdateConnectionTimeout(val value: Int)
object ToggleUseWebSocket
object ToggleUdpDiscovery
```

#### Privacy Events (3)
```kotlin
object ToggleAnonymousStats
object ToggleCrashReporting
object ToggleClearDataOnExit
```

#### Presentation Events (4)
```kotlin
object TogglePresentationMode
data class UpdateLaserPointerSpeed(val value: Float)
object ToggleShowPresentationTimer
object ToggleAutoHideLaser
```

#### Action Events (14)
```kotlin
object SaveSettings
object ResetDefaults
object ExportSettings
object ImportSettings
data class ImportSettingsFromUri(val uri: String)
object ClearCache
object ClearAllData
object OpenSystemSettings
object OpenAccessibilitySettings
object OpenWebsite
object OpenPrivacyPolicy
object OpenLicense
object OpenGitHub
object OpenSupport
```

**Total Events: 74**

---

## 🧩 3. UI Components

### SettingsMainScreen

The main settings list with category cards.

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

Drill-down screen for each settings category.

```kotlin
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

## 📊 4. Section Detail Screens

### Cursor Settings

```kotlin
@Composable
fun CursorSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSlider(
            title = "Sensitivity",
            value = uiState.sensitivity,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateSensitivity(it)) },
            valueRange = 0.1f..2.0f, steps = 19,
            formatValue = { "%.1fx".format(it) },
            description = "Higher values make cursor faster"
        )
        SettingsSlider(
            title = "Smoothing Factor",
            value = uiState.smoothingFactor,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateSmoothingFactor(it)) },
            valueRange = 0f..1f, steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "Smooths cursor movement"
        )
        SettingsSwitch(
            title = "Acceleration",
            description = "Cursor speed increases with faster movement",
            checked = uiState.accelerationEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAcceleration) }
        )
        SettingsSwitch(
            title = "Invert X Axis",
            description = "Swap left/right movement direction",
            checked = uiState.invertX,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleInvertX) }
        )
        SettingsSwitch(
            title = "Invert Y Axis",
            description = "Swap up/down movement direction",
            checked = uiState.invertY,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleInvertY) }
        )
        SettingsSwitch(
            title = "Smoothing",
            description = "Smooth cursor movement for better control",
            checked = uiState.smoothingEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleSmoothing) }
        )
    }
}
```

### Gesture Settings

```kotlin
@Composable
fun GestureSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSlider(
            title = "Click Threshold",
            value = uiState.clickThreshold,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateClickThreshold(it)) },
            valueRange = 1f..20f, steps = 19,
            formatValue = { "%.1f".format(it) },
            description = "Sensitivity for click detection"
        )
        SettingsSlider(
            title = "Double Click Interval",
            value = uiState.doubleClickInterval.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateDoubleClickInterval(it.toLong())) },
            valueRange = 100f..800f, steps = 14,
            formatValue = { "${it.toInt()}ms" },
            description = "Max time between clicks for double click"
        )
        SettingsSlider(
            title = "Scroll Threshold",
            value = uiState.scrollThreshold,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateScrollThreshold(it)) },
            valueRange = 1f..15f, steps = 14,
            formatValue = { "%.1f".format(it) },
            description = "Sensitivity for scroll detection"
        )
        SettingsSlider(
            title = "Right Click Tilt",
            value = uiState.rightClickTilt,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateRightClickTilt(it)) },
            valueRange = 5f..45f, steps = 8,
            formatValue = { "${it.toInt()}°" },
            description = "Tilt angle to trigger right click"
        )
        SettingsSlider(
            title = "Right Click Duration",
            value = uiState.rightClickDuration.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateRightClickDuration(it.toLong())) },
            valueRange = 100f..1000f, steps = 9,
            formatValue = { "${it.toInt()}ms" },
            description = "How long to hold tilt for right click"
        )
    }
}
```

### AI Settings

```kotlin
@Composable
fun AISettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "AI Smoothing",
            description = "Use AI to smooth cursor movement",
            checked = uiState.aiSmoothing,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAiSmoothing) }
        )
        SettingsSlider(
            title = "AI Blend Factor",
            value = uiState.aiBlendFactor,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateAiBlendFactor(it)) },
            valueRange = 0f..1f, steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "Balance between raw and AI-smoothed movement"
        )
        SettingsSwitch(
            title = "Predictive Movement",
            description = "Predict future cursor position",
            checked = uiState.predictive,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.TogglePredictive) }
        )
        SettingsSlider(
            title = "Prediction Strength",
            value = uiState.predictionStrength,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdatePredictionStrength(it)) },
            valueRange = 0f..1f, steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "How much prediction to apply"
        )
        SettingsSwitch(
            title = "Kalman Filter",
            description = "Use Kalman filter for smoother tracking",
            checked = uiState.kalmanEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleKalman) }
        )
    }
}
```

### Haptic Settings

```kotlin
@Composable
fun HapticSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Haptic Feedback",
            description = "Vibration on actions",
            checked = uiState.hapticEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleHaptic) }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Haptic Strength", style = MaterialTheme.typography.bodyLarge)
                Text("Intensity of vibration feedback", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HapticStrength.entries.forEach { strength ->
                        FilterChip(
                            selected = uiState.hapticStrength == strength,
                            onClick = { viewModel.handleEvent(SettingsEvent.UpdateHapticStrength(strength)) },
                            label = { Text(strength.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        SettingsSwitch(
            title = "Sound Feedback",
            description = "Play sounds on actions",
            checked = uiState.soundEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleSound) }
        )
        SettingsSwitch(
            title = "Visual Feedback",
            description = "Show visual indicators on actions",
            checked = uiState.visualFeedback,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleVisualFeedback) }
        )
        SettingsSwitch(
            title = "Notification on Gesture",
            description = "Show notification when gesture is detected",
            checked = uiState.notificationOnGesture,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleNotificationOnGesture) }
        )
    }
}
```

### Display Settings

```kotlin
@Composable
fun DisplaySettings(uiState: SettingsUiState, viewModel: SettingsViewModel, navigationActions: NavigationActions?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Text("Choose app color scheme", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Quick themes", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system", "light", "dark", "pure_black", "high_contrast").forEach { theme ->
                        FilterChip(
                            selected = uiState.theme == theme,
                            onClick = { viewModel.handleEvent(SettingsEvent.UpdateTheme(theme)) },
                            label = { Text(when (theme) {
                                "system" -> "System"; "light" -> "Light"; "dark" -> "Dark"
                                "pure_black" -> "Pure Black"; "high_contrast" -> "High Contrast"
                                else -> theme }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        ThemesShortcutCard(navigationActions)
        SettingsSwitch(
            title = "Dynamic Colors",
            description = "Use Material You color scheme",
            checked = uiState.useDynamicColors,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleDynamicColors) }
        )
        SettingsSlider(
            title = "Font Size",
            value = uiState.fontSize,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateFontSize(it)) },
            valueRange = 12f..24f, steps = 6,
            formatValue = { "${it.toInt()}sp" },
            description = "Base font size for the app"
        )
        SettingsSwitch(
            title = "Show Debug Info",
            description = "Display debug information",
            checked = uiState.showDebugInfo,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleDebugInfo) }
        )
        SettingsSwitch(
            title = "Keep Screen On",
            description = "Prevent screen from turning off",
            checked = uiState.keepScreenOn,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleKeepScreenOn) }
        )
        SettingsSwitch(
            title = "Show FPS",
            description = "Display frames per second counter",
            checked = uiState.showFps,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleShowFps) }
        )
    }
}
```

### Touchpad Settings

```kotlin
@Composable
fun TouchpadSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Touchpad Active",
            description = "Enable touchpad mode on the phone",
            checked = uiState.touchpadActive,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadActive) }
        )
        SettingsSlider(
            title = "Touchpad Sensitivity",
            value = uiState.touchpadSensitivity,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateTouchpadSensitivity(it)) },
            valueRange = 0.5f..2.0f, steps = 15,
            formatValue = { "%.1fx".format(it) },
            description = "Overall response of touchpad movement"
        )
        SettingsSlider(
            title = "Cursor Speed",
            value = uiState.touchpadCursorSpeed,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateTouchpadCursorSpeed(it)) },
            valueRange = 0.5f..2.0f, steps = 15,
            formatValue = { "%.1fx".format(it) },
            description = "How fast the pointer moves"
        )
        // ... 15+ more touchpad settings
    }
}
```

### Connection Settings

```kotlin
@Composable
fun ConnectionSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Auto Connect",
            description = "Auto-connect on app start",
            checked = uiState.autoConnect,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAutoConnect) }
        )
        SettingsSwitch(
            title = "Use WebSocket",
            description = "Use WebSocket protocol for live control messages",
            checked = uiState.useWebSocket,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleUseWebSocket) }
        )
        SettingsSwitch(
            title = "UDP Discovery",
            description = "Auto-discover servers on network",
            checked = uiState.useUdpDiscovery,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleUdpDiscovery) }
        )
        SettingsSlider(
            title = "Reconnect Attempts",
            value = uiState.reconnectAttempts.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateReconnectAttempts(it.toInt())) },
            valueRange = 1f..20f, steps = 19,
            formatValue = { "${it.toInt()}" },
            description = "Number of reconnection attempts"
        )
        SettingsSlider(
            title = "Connection Timeout",
            value = uiState.connectionTimeout.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateConnectionTimeout(it.toInt())) },
            valueRange = 1000f..15000f, steps = 14,
            formatValue = { "${it.toInt()}ms" },
            description = "Timeout for connection attempts"
        )
    }
}
```

### Privacy Settings

```kotlin
@Composable
fun PrivacySettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.handleEvent(SettingsEvent.ImportSettingsFromUri(uri.toString()))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Anonymous Statistics",
            description = "Help improve Air Mouse by sending anonymous usage data",
            checked = uiState.anonymousStats,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAnonymousStats) }
        )
        SettingsSwitch(
            title = "Crash Reporting",
            description = "Automatically report crashes to help fix issues",
            checked = uiState.crashReporting,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleCrashReporting) }
        )
        SettingsSwitch(
            title = "Clear Data on Exit",
            description = "Clear all app data when you exit",
            checked = uiState.clearDataOnExit,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleClearDataOnExit) }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.handleEvent(SettingsEvent.ClearCache) }, modifier = Modifier.weight(1f)) {
                        Text("Clear Cache")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                        Text("Import Data")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.handleEvent(SettingsEvent.ExportSettings) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Settings")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.handleEvent(SettingsEvent.OpenSystemSettings) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("System")
                    }
                    OutlinedButton(
                        onClick = { viewModel.handleEvent(SettingsEvent.OpenAccessibilitySettings) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accessibility")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.handleEvent(SettingsEvent.ClearAllData) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All Data", color = Color.White)
                }
            }
        }
    }
}
```

---

## 🔧 ViewModel Architecture

### State Management

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val mouseControlFeature: MouseControlFeature,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<SettingsEffect?>(null)
    val effect: StateFlow<SettingsEffect?> = _effect.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                // Cursor
                sensitivity = prefs.getFloat("sensitivity", 0.5f),
                accelerationEnabled = prefs.getBoolean("acceleration_enabled", false),
                // ... all other settings loaded from preferences
            )
        }
    }

    fun handleEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                // Each event updates the state and saves to preferences
                is SettingsEvent.UpdateSensitivity -> updateSensitivity(event.value)
                SettingsEvent.ToggleHaptic -> toggleHaptic()
                // ... all other events
            }
        }
    }

    private suspend fun updateSensitivity(value: Float) {
        prefs.putFloat("sensitivity", value)
        _uiState.update { it.copy(sensitivity = value) }
    }

    private suspend fun toggleHaptic() {
        val current = _uiState.value.hapticEnabled
        prefs.putBoolean("haptic_enabled", !current)
        _uiState.update { it.copy(hapticEnabled = !current) }
        showToast("Haptic ${if (!current) "enabled" else "disabled"}")
    }

    // ... all other methods

    private suspend fun showToast(message: String) {
        _effect.value = SettingsEffect.ShowToast(message)
        delay(3000)
        _effect.value = null
    }
}
```

---

## 📊 Settings Summary

| Category | Settings Count | Components Used | Description |
|----------|----------------|-----------------|-------------|
| **Cursor** | 7 | Slider, Switch | Movement, sensitivity, smoothing, acceleration |
| **Gesture** | 6 | Slider | Click, scroll, right-click detection |
| **AI** | 5 | Slider, Switch | AI smoothing, predictive movement, Kalman filter |
| **Haptic** | 6 | Switch, Chip, Slider | Haptic, sound, visual feedback |
| **Notifications** | 4 | Switch | App notifications, gesture alerts |
| **Display** | 6 | Chip, Switch, Slider | Theme, font, debug, screen settings |
| **Touchpad** | 19 | Switch, Slider | Sensitivity, gestures, scrolling, feedback |
| **Connection** | 5 | Switch, Slider | Auto-connect, reconnect, timeout, protocol |
| **Privacy** | 3 | Switch, Button | Statistics, crash reporting, data clearing |
| **Presentation** | 4 | Switch, Slider | Mode, laser pointer, timer, auto-hide |
| **About** | 0 | Display | App information |
| **Total** | **65** | | **Complete settings coverage** |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Centralized Management** | Single source of truth for all settings |
| **Real-time Updates** | Changes applied immediately |
| **Persistent Storage** | All settings saved to PreferencesManager |
| **Categorized** | Logical grouping of related settings |
| **Export/Import** | Settings backup and restore |
| **Reset** | Reset individual sections or all settings |
| **Reactive UI** | StateFlow with automatic updates |
| **Testability** | ViewModel separated from UI |

---

**The Settings Screen provides complete control over every aspect of the Air Mouse application, with intuitive controls, real-time feedback, and persistent storage of all user preferences.**