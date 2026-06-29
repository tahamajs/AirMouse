# 📘 Air Mouse Accessibility Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.accessibility` package contains the **Accessibility screen** for the Air Mouse application. This screen provides comprehensive accessibility settings for users with different needs, including display adjustments, feedback controls, gesture modifications, voice settings, and advanced accessibility features.

```
com.airmouse.presentation.ui.accessibility/
├── AccessibilityScreen.kt          # Main accessibility UI
├── AccessibilityUiState.kt         # Accessibility state and enums
└── AccessibilityViewModel.kt       # Accessibility ViewModel
```

---

## 🎯 1. AccessibilityScreen

### Purpose
Provides a comprehensive **accessibility settings interface** with multiple tabs for different accessibility categories. Users can customize display, feedback, gestures, voice, and advanced settings to suit their needs.

### Key Features

| Feature | Description |
|---------|-------------|
| **6 Categories** | Display, Feedback, Gesture, Voice, Advanced, Mouse Control |
| **Tab Navigation** | Swipeable tabs for each category |
| **Live Preview** | Display settings show real-time preview |
| **Color Blind Modes** | Protanopia, Deuteranopia, Tritanopia support |
| **Haptic Intensity** | Light, Medium, Strong vibration feedback |
| **Voice Wake Word** | Custom wake word for voice commands |
| **Dwell Click** | Auto-click after cursor stops moving |
| **Reset to Defaults** | Reset all accessibility settings |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(
    navigationActions: NavigationActions,
    viewModel: AccessibilityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf(AccessibilityCategory.DISPLAY) }

    Scaffold(
        topBar = { /* TopAppBar with title, reset, help */ }
    ) { paddingValues ->
        Column {
            // Tab Row (6 categories)
            ScrollableTabRow(...)
            
            // Content area
            LazyColumn {
                when (selectedCategory) {
                    AccessibilityCategory.DISPLAY -> DisplaySettings(uiState, viewModel)
                    AccessibilityCategory.FEEDBACK -> FeedbackSettings(uiState, viewModel)
                    AccessibilityCategory.GESTURE -> GestureSettings(uiState, viewModel)
                    AccessibilityCategory.VOICE -> VoiceSettings(uiState, viewModel)
                    AccessibilityCategory.ADVANCED -> AdvancedSettings(uiState, viewModel)
                    AccessibilityCategory.MOUSE -> MouseSettings(uiState, viewModel)
                }
            }
        }
    }
}
```

---

## 🎨 2. AccessibilityUiState

### Purpose
Defines the **state model** for the accessibility screen, including all settings, enums, and extension functions.

### State Definition

```kotlin
data class AccessibilityUiState(
    // Display
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reduceMotion: Boolean = false,
    val darkMode: Boolean = false,
    val customFontSize: Float = 16f,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,

    // Feedback
    val hapticFeedback: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val soundFeedback: Boolean = false,
    val voiceFeedback: Boolean = false,

    // Gesture
    val simplifiedGestures: Boolean = false,
    val screenReader: Boolean = false,
    val announceMovement: Boolean = false,
    val announceClicks: Boolean = false,
    val gestureSensitivity: Float = 1.0f,

    // Voice
    val voiceWakeWord: Boolean = true,
    val wakeWord: String = "Hey Air Mouse",
    val voiceConfirmation: Boolean = true,
    val voiceContinuousListening: Boolean = false,

    // Advanced
    val switchAccess: Boolean = false,
    val dwellClick: Boolean = false,
    val dwellTime: Int = 1000,
    val audioCues: Boolean = true,
    val flashOnClick: Boolean = false,

    // Mouse Control
    val isMouseEnabled: Boolean = true,
    val mousePointerLarge: Boolean = false,
    val mouseTrails: Boolean = false,
    val snapToDefault: Boolean = false,

    // Control Mode
    val controlMode: String = "motion",

    // UI State
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
```

### Enums

```kotlin
enum class AccessibilityCategory(
    val title: String,
    val icon: ImageVector
) {
    DISPLAY("Display", Icons.Default.DisplaySettings),
    FEEDBACK("Feedback", Icons.Default.VolumeUp),
    GESTURE("Gesture", Icons.Default.Gesture),
    VOICE("Voice", Icons.Default.Mic),
    ADVANCED("Advanced", Icons.Default.Settings),
    MOUSE("Mouse Control", Icons.Default.Computer)
}

enum class ColorBlindMode(val displayName: String) {
    NONE("None"),
    PROTANOPIA("Protanopia (Red-Blind)"),
    DEUTERANOPIA("Deuteranopia (Green-Blind)"),
    TRITANOPIA("Tritanopia (Blue-Blind)")
}

enum class HapticIntensity {
    LIGHT,
    MEDIUM,
    STRONG
}

enum class ControlMode(val displayName: String) {
    GYRO("Motion (Gyro)"),
    ACCEL("Acceleration"),
    HYBRID("Hybrid"),
    MOUSE("Mouse Control")
}
```

---

## 🔧 3. AccessibilityViewModel

### Purpose
Manages all accessibility settings, providing persistence via `PreferencesManager` with real-time state updates.

### Key Methods

#### Display Settings
```kotlin
fun setHighContrast(enabled: Boolean)
fun setLargeText(enabled: Boolean)
fun setReduceMotion(enabled: Boolean)
fun setDarkMode(enabled: Boolean)
fun setCustomFontSize(size: Float)
fun setColorBlindMode(mode: ColorBlindMode)
```

#### Feedback Settings
```kotlin
fun setHapticFeedback(enabled: Boolean)
fun setHapticIntensity(intensity: HapticIntensity)
fun setSoundFeedback(enabled: Boolean)
fun setVoiceFeedback(enabled: Boolean)
```

#### Gesture Settings
```kotlin
fun setSimplifiedGestures(enabled: Boolean)
fun setScreenReader(enabled: Boolean)
fun setAnnounceMovement(enabled: Boolean)
fun setAnnounceClicks(enabled: Boolean)
fun setGestureSensitivity(sensitivity: Float)
```

#### Voice Settings
```kotlin
fun setVoiceWakeWord(enabled: Boolean)
fun setWakeWord(word: String)
fun setVoiceConfirmation(enabled: Boolean)
fun setVoiceContinuousListening(enabled: Boolean)
```

#### Advanced Settings
```kotlin
fun setSwitchAccess(enabled: Boolean)
fun setDwellClick(enabled: Boolean)
fun setDwellTime(time: Int)
fun setAudioCues(enabled: Boolean)
fun setFlashOnClick(enabled: Boolean)
```

#### Mouse Control Settings
```kotlin
fun setMouseEnabled(enabled: Boolean)
fun setMousePointerLarge(enabled: Boolean)
fun setMouseTrails(enabled: Boolean)
fun setSnapToDefault(enabled: Boolean)
fun setControlMode(mode: String)
```

#### Reset
```kotlin
fun resetToDefaults()
fun openAccessibilityHelp()
```

### Implementation Pattern

```kotlin
fun setHighContrast(enabled: Boolean) {
    prefs.putBoolean("high_contrast", enabled)
    _uiState.update { it.copy(highContrast = enabled) }
    applyTheme()
    showSuccess("High contrast ${if (enabled) "enabled" else "disabled"}")
}
```

---

## 🧩 4. UI Components

### GlassCard
A **glassmorphism card** used throughout the accessibility screen.

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}
```

### AnimatedSwitch
A **switch with animation** and description.

```kotlin
@Composable
fun AnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
```

---

## 📱 5. Category Screens

### Display Settings

| Setting | Description | Controls |
|---------|-------------|----------|
| High Contrast Mode | Increase contrast for better visibility | Switch |
| Large Text | Increase text size throughout the app | Switch |
| Reduce Motion | Minimize animations and transitions | Switch |
| Dark Mode | Use dark theme for reduced eye strain | Switch |
| Custom Font Size | Adjust text size (12-24sp) | Slider |
| Color Blind Mode | Protanopia, Deuteranopia, Tritanopia | Chip selector |

### Feedback Settings

| Setting | Description | Controls |
|---------|-------------|----------|
| Haptic Feedback | Vibration on gestures and actions | Switch |
| Haptic Intensity | Light, Medium, Strong | Chip selector |
| Sound Feedback | Audio cues for actions | Switch |
| Voice Feedback | Spoken announcements for actions | Switch |

### Gesture Settings

| Setting | Description | Controls |
|---------|-------------|----------|
| Simplified Gestures | Easier gesture detection with lower sensitivity | Switch |
| Screen Reader Support | Enhanced TalkBack compatibility | Switch |
| Announce Movement | TalkBack reads cursor movement aloud | Switch |
| Announce Clicks | TalkBack announces click actions | Switch |
| Gesture Sensitivity | Adjust sensitivity (0.5-2.0) | Slider |

### Voice Settings

| Setting | Description | Controls |
|---------|-------------|----------|
| Wake Word Detection | "Hey Air Mouse" activates voice commands | Switch |
| Custom Wake Word | Enter custom wake word | TextField |
| Voice Confirmation | Verbal confirmation of commands | Switch |
| Continuous Listening | Keep listening after wake word | Switch |

### Advanced Settings

| Setting | Description | Controls |
|---------|-------------|----------|
| Switch Access | Use external switches for control | Switch |
| Dwell Click | Auto-click after cursor stops moving | Switch + Slider |
| Audio Cues | Sound effects for all interactions | Switch |
| Flash on Click | Visual flash when clicking | Switch |

### Mouse Control Settings

| Setting | Description | Controls |
|---------|-------------|----------|
| Enable Mouse Control | Allow cursor control from the phone | Switch |
| Large Pointer | Use a larger cursor for better visibility | Switch |
| Mouse Trails | Show pointer trails for easier tracking | Switch |
| Snap to Default | Auto-snap cursor on connection | Switch |
| Control Mode | Motion, Acceleration, Hybrid, Mouse Control | Chip selector |

---

## 📊 Accessibility Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ACCESSIBILITY SETTINGS FLOW                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User opens Accessibility Screen                                    │
│         │                                                               │
│         ▼                                                               │
│  2. ViewModel loads settings from PreferencesManager                  │
│         │                                                               │
│         ▼                                                               │
│  3. UI displays current settings                                       │
│         │                                                               │
│         ▼                                                               │
│  4. User modifies a setting                                            │
│         │                                                               │
│         ▼                                                               │
│  5. ViewModel updates StateFlow                                        │
│         │                                                               │
│         ▼                                                               │
│  6. Setting saved to PreferencesManager                               │
│         │                                                               │
│         ▼                                                               │
│  7. UI updates to reflect new state                                   │
│         │                                                               │
│         ▼                                                               │
│  8. If theme-related: applyTheme() is called                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🎨 Color Blind Mode Preview

```kotlin
@Composable
fun ColorBlindModeSelector(
    selectedMode: ColorBlindMode,
    onModeSelected: (ColorBlindMode) -> Unit
) {
    Column {
        Text("Color Blind Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("Adjust colors for color vision deficiency", fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorBlindMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.displayName, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}
```

---

## 📋 Summary of Accessibility Features

| Category | Settings | Accessibility Benefit |
|----------|----------|----------------------|
| **Display** | High Contrast, Large Text, Reduce Motion, Dark Mode, Custom Font Size, Color Blind Mode | Visual impairments, motion sensitivity, eye strain |
| **Feedback** | Haptic, Sound, Voice Feedback | Hearing impairments, sensory preferences |
| **Gesture** | Simplified Gestures, Screen Reader, Announce Movement/Clicks | Motor impairments, visual impairments |
| **Voice** | Wake Word, Voice Confirmation, Continuous Listening | Motor impairments, hands-free control |
| **Advanced** | Switch Access, Dwell Click, Audio Cues, Flash on Click | Severe motor impairments, accessibility needs |
| **Mouse Control** | Mouse Enable, Large Pointer, Trails, Snap to Default | Visual impairments, motor control |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Inclusive Design** | Settings for various disabilities and preferences |
| **Real-time Preview** | Changes applied immediately |
| **Persistence** | Settings saved to PreferencesManager |
| **Reset** | One-click reset to defaults |
| **Help** | Accessibility help link |
| **Clear Labeling** | Descriptive labels and explanations |
| **Granular Control** | Fine-grained adjustments (sliders, intensity levels) |

---

**The Accessibility Screen provides comprehensive accessibility settings, ensuring the Air Mouse application is usable by people with diverse abilities and preferences.**