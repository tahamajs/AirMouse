# 📘 Air Mouse Presentation Layer – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation` package is the **UI layer** of the Air Mouse application, built entirely with Jetpack Compose. It follows **Model-View-Intent (MVI)** architecture with unidirectional data flow, providing a reactive and maintainable user interface.

```
com.airmouse.presentation/
├── MainActivity.kt                    # Main entry point
├── MainScreen.kt                      # Root navigation host
├── ui/
│   ├── home/                          # Home screen (dashboard)
│   ├── settings/                      # Settings screen
│   ├── calibration/                   # Calibration screens
│   ├── touchpad/                      # Touchpad screen
│   ├── voice/                         # Voice commands screen
│   ├── themes/                        # Themes screen
│   ├── accessibility/                 # Accessibility screen
│   ├── profiles/                      # Profiles screen
│   ├── proximity/                     # Proximity screen
│   ├── logs/                          # Server logs screen
│   ├── help/                          # Help screen
│   ├── about/                         # About screen (stub)
│   ├── statistics/                    # Statistics screen (stub)
│   ├── gesture/                       # Gesture studio (stub)
│   ├── edge/                          # Edge gestures (stub)
│   ├── network/                       # Network discovery (stub)
│   ├── sensor/                        # Sensor visualizer (stub)
│   ├── battery/                       # Battery screen (stub)
│   ├── files/                         # File transfer (stub)
│   ├── onboarding/                    # Onboarding screens
│   └── main/                          # Main screen components
├── navigation/
│   ├── Destinations.kt                # Navigation routes
│   ├── NavigationActions.kt           # Navigation abstraction
│   ├── AirMouseNavHost.kt             # NavHost definition
│   └── AirMouseBottomBar.kt           # Bottom navigation bar
├── theme/
│   ├── Theme.kt                       # Compose theme
│   ├── ThemeColors.kt                 # Color schemes
│   ├── Color.kt                       # Color definitions
│   ├── Dimensions.kt                  # Spacing & sizing
│   └── Shapes.kt                      # Shape definitions
├── components/                        # Reusable UI components
│   ├── AllUIComponents.kt             # Various UI components
│   ├── AnimatedCheckbox.kt
│   ├── AnimatedSwitch.kt
│   ├── AnimatedToast.kt
│   ├── AnimatedCounter.kt
│   ├── AnimatedConnectionStatus.kt
│   ├── BatteryLevelIndicator.kt
│   ├── CircularProgressWithLabel.kt
│   ├── ConnectionStatusBadge.kt
│   ├── DataChart.kt
│   ├── FloatingActionMenu.kt
│   ├── FloatingParticles.kt
│   ├── GestureWaveform.kt
│   ├── GlassCard.kt
│   ├── GradientBackground.kt
│   ├── GradientIconButton.kt
│   ├── HolographicText.kt
│   ├── InteractiveTutorialCard.kt
│   ├── NeonButton.kt
│   ├── NeumorphicCard.kt
│   ├── NotificationBadge.kt
│   ├── ParticleBackground.kt
│   ├── PullToRefresh.kt
│   ├── RadarAnimation.kt
│   ├── SensorVisualizer.kt
│   ├── ShimmerEffect.kt
│   ├── SkeletonScreen.kt
│   ├── SlideUpPanel.kt
│   ├── TypewriterText.kt
│   └── VoiceWaveAnimation.kt
└── calibration/                       # Calibration UI components
    ├── CalibrationComponents.kt
    └── CalibrationGuideDialog.kt
```

---

## 🏛️ Architecture Overview

### MVI Pattern (Model-View-Intent)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MVI ARCHITECTURE                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                         VIEW                                    │   │
│  │  ┌─────────────────────────────────────────────────────────┐   │   │
│  │  │  Compose Screen                                          │   │   │
│  │  │  • Renders UI based on State                            │   │   │
│  │  │  • Dispatches Events to ViewModel                       │   │   │
│  │  │  • Handles Effects (navigation, toasts)                 │   │   │
│  │  └─────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                       VIEW MODEL                                │   │
│  │  ┌─────────────────────────────────────────────────────────┐   │   │
│  │  │  StateFlow<UiState>   │  SharedFlow<Effect>             │   │   │
│  │  ├─────────────────────────────────────────────────────────┤   │   │
│  │  │  fun handleEvent(event: Event) {                        │   │   │
│  │  │    when (event) { ... }                                 │   │   │
│  │  │  }                                                      │   │   │
│  │  └─────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                         MODEL                                   │   │
│  │  ┌─────────────────────────────────────────────────────────┐   │   │
│  │  │  data class UiState(...)                                │   │   │
│  │  │  sealed class Event { ... }                            │   │   │
│  │  │  sealed class Effect { ... }                           │   │   │
│  │  └─────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📱 1. MainActivity

### Purpose
The **entry point** of the application. Handles splash screen, onboarding, theme application, and system bar configuration.

### Key Features

| Feature | Description |
|---------|-------------|
| **Splash Screen** | Uses Android 12+ splash screen API |
| **Onboarding Check** | Redirects to onboarding if not completed |
| **Theme Application** | Applies theme from preferences |
| **System Bars** | Edge-to-edge with transparent system bars |
| **Dynamic Colors** | Material You support (Android 12+) |
| **Hilt Integration** | @AndroidEntryPoint |

### Lifecycle Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      MAIN ACTIVITY FLOW                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  onCreate()                                                            │
│     │                                                                   │
│     ├── installSplashScreen()                                          │
│     │                                                                   │
│     ├── Check onboarding                                               │
│     │     ├── Not completed → startOnboarding() → finish()            │
│     │     └── Completed → continue                                    │
│     │                                                                   │
│     ├── setupWindow()                                                  │
│     │                                                                   │
│     └── setContent { MainScreen() }                                   │
│                                                                         │
│  onResume()                                                            │
│     └── updateSystemBarsColor()                                       │
│                                                                         │
│  onConfigurationChanged()                                              │
│     └── updateSystemBarsColor()                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🧭 2. Navigation

### Destinations.kt

Defines all navigation routes as a sealed class.

```kotlin
sealed class Destinations(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Destinations("home", "Home", Icons.Default.Home)
    object Settings : Destinations("settings", "Settings", Icons.Default.Settings)
    object Statistics : Destinations("statistics", "Stats", Icons.Default.Assessment)
    object Help : Destinations("help", "Help", Icons.AutoMirrored.Filled.Help)
    // ... 20+ destinations
}
```

### NavigationActions.kt

Abstraction for navigation to decouple UI from NavController.

```kotlin
interface NavigationActions {
    fun navigateTo(route: String)
    fun navigateBack()
    fun navigateToHome()
    fun navigateToSettings()
    fun navigateToCalibration()
    fun navigateToCalibrationResult(quality: String)
    // ... etc
}
```

### AirMouseNavHost.kt

The main navigation graph with all composable routes.

```kotlin
@Composable
fun AirMouseNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Home.route,
    modifier: Modifier = Modifier
) {
    NavHost(...) {
        composable(Destinations.Home.route) {
            HomeScreen(navigationActions = NavigationActionsImpl(navController))
        }
        // ... all other routes
    }
}
```

### AirMouseBottomBar.kt

Bottom navigation bar for the main tabs.

```kotlin
@Composable
fun AirMouseBottomBar(
    currentRoute: String?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = Destinations.bottomNavDestinations
    // ... renders bottom bar
}
```

---

## 🎨 3. Theming

### Theme.kt

The main theme composable that wraps the entire app.

```kotlin
@Composable
fun AirMouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    themeColors: ThemeColorScheme? = null,
    content: @Composable () -> Unit
) {
    // ... theme logic
}
```

### ThemeColors.kt

Defines color schemes for different themes.

```kotlin
data class ThemeColorScheme(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    // ... all M3 color roles
)

object ThemeColorSchemes {
    fun lightTheme(accent: AccentColor): ThemeColorScheme
    fun darkTheme(accent: AccentColor): ThemeColorScheme
    fun pureBlackTheme(accent: AccentColor): ThemeColorScheme
    // ... etc
}
```

### Color.kt

Static color definitions.

```kotlin
// Material colors
val DeepOrange500 = Color(0xFFFF5722)
val Amber500 = Color(0xFFFFC107)
val Teal500 = Color(0xFF009688)

// Semantic colors
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
```

### Dimensions.kt

Centralized spacing, sizing, and typography constants.

```kotlin
object Dimensions {
    // Spacing
    val space4 = 4.dp
    val space8 = 8.dp
    val space16 = 16.dp
    
    // Corner radius
    val radius8 = 8.dp
    val radius16 = 16.dp
    
    // Font sizes
    val textSmall = 12.sp
    val textBody = 16.sp
    val textTitle = 24.sp
    
    // Icon sizes
    val iconMedium = 24.dp
    val iconLarge = 32.dp
}
```

### Shapes.kt

Reusable shape definitions.

```kotlin
object AppShapesProvider {
    val roundedDefault = RoundedCornerShape(8.dp)
    val roundedMedium = RoundedCornerShape(12.dp)
    val roundedLarge = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(100.dp)
}
```

---

## 🧩 4. UI Components

### GlassCard.kt
Glassmorphism card with translucent background.

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) { content() }
}
```

### NeonButton.kt
Neon-style button with pulsing glow effect.

```kotlin
@Composable
fun NeonButton(
    onClick: () -> Unit,
    text: String,
    gradient: List<Color> = listOf(Color(0xFF00BCD4), Color(0xFF4CAF50)),
    glowing: Boolean = true
) {
    // ... neon button implementation
}
```

### SensorVisualizer.kt
3D phone visualisation with live orientation.

```kotlin
@Composable
fun SensorVisualizer(
    roll: Float,
    pitch: Float,
    yaw: Float,
    size: VisualizerSize = VisualizerSize.MEDIUM
) {
    // ... 3D visualisation
}
```

### AnimatedConnectionStatus.kt
Animated connection status indicator.

```kotlin
@Composable
fun AnimatedConnectionStatus(
    isConnected: Boolean,
    signalStrength: Int = 100,
    ping: Int = 0
) {
    // ... animated status
}
```

---

## 📱 5. Screens

### HomeScreen.kt
The main dashboard with connection, sensors, and quick actions.

**Key Components:**
- Connection Toggle Button
- Sensor Visualizer
- Mouse Motion Preview
- Quick Actions Row
- Stats Cards
- Performance Monitor
- Recent Gestures
- Tips Card

### SettingsScreen.kt
Comprehensive settings with multiple sections.

**Sections:**
- Cursor Settings (sensitivity, smoothing, acceleration)
- Gesture Settings (thresholds, intervals)
- AI Settings (smoothing, predictive)
- Haptic & Sound
- Display & Theme
- Touchpad Settings
- Connection Settings
- Privacy & Data
- Presentation Settings
- About

### CalibrationScreen.kt
Step-by-step calibration wizard.

**Phases:**
1. INTRO – Show step information
2. COUNTDOWN – Countdown animation
3. SAMPLING – Collect sensor data

### TouchpadScreen.kt
Full touchpad simulation with gestures.

**Features:**
- Touch surface with pointer input
- Gesture detection (drag, scroll, swipe, pinch, tap)
- Visual feedback (touch points)
- Haptic feedback
- Quick presets

### VoiceCommandsScreen.kt
Voice command control with custom commands.

**Features:**
- Start/Stop listening
- Wake word detection
- Built-in commands list
- Custom commands
- Command history
- Settings (sensitivity, feedback)

### AccessibilityScreen.kt
Accessibility settings.

**Sections:**
- Display (high contrast, large text, reduce motion)
- Feedback (haptic, sound, voice)
- Gesture (simplified, screen reader)
- Voice (wake word, continuous listening)
- Advanced (switch access, dwell click)

### ThemesScreen.kt
Theme selector with previews.

**Features:**
- Theme list with preview
- Accent color picker
- Premium themes
- Live preview
- Reset to defaults

### ProfilesScreen.kt
User profile management.

**Features:**
- List/Grid/Compact views
- Create, edit, delete profiles
- Favorites
- Default profile
- Usage statistics

### ProximityScreen.kt
Bluetooth proximity detection.

**Features:**
- Device scanning
- Distance estimation
- Auto-lock/unlock
- Calibration
- History

### ServerLogsScreen.kt
Real-time log viewer.

**Features:**
- Live log streaming
- Search/filter
- Pagination
- Export (TXT, CSV, JSON, HTML)
- Statistics
- Auto-refresh

### HelpScreen.kt
Searchable help center.

**Features:**
- Categories
- Search
- Favorites
- Expandable sections
- Contact support
- Feedback

---

## 🏗️ 6. Screen Architecture Pattern

### MVI Implementation

#### State (UiState)
```kotlin
data class SettingsUiState(
    val sensitivity: Float = 0.5f,
    val hapticEnabled: Boolean = true,
    val theme: String = "system",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)
```

#### Event
```kotlin
sealed class SettingsEvent {
    data class UpdateSensitivity(val value: Float) : SettingsEvent()
    object ToggleHaptic : SettingsEvent()
    data class UpdateTheme(val theme: String) : SettingsEvent()
    object SaveSettings : SettingsEvent()
    object ResetDefaults : SettingsEvent()
}
```

#### Effect
```kotlin
sealed class SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect()
    data class NavigateTo(val route: String) : SettingsEffect()
    object NavigateBack : SettingsEffect()
}
```

#### ViewModel
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: ISettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _effect = MutableSharedFlow<SettingsEffect>()
    val effect: SharedFlow<SettingsEffect> = _effect.asSharedFlow()
    
    fun handleEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.UpdateSensitivity -> updateSensitivity(event.value)
                SettingsEvent.ToggleHaptic -> toggleHaptic()
                // ... etc
            }
        }
    }
}
```

#### Screen
```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)
    
    // Handle effects
    LaunchedEffect(effect) {
        when (effect) {
            is SettingsEffect.ShowToast -> Toast.makeText(...).show()
            is SettingsEffect.NavigateTo -> navigationActions.navigateTo(effect.route)
            null -> { /* ignore */ }
        }
    }
    
    // Render UI
    SettingsSwitch(
        checked = uiState.hapticEnabled,
        onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleHaptic) }
    )
}
```

---

## 🎯 Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Unidirectional Data Flow** | State → View → Event → ViewModel → State |
| **Reactive UI** | StateFlow for automatic recomposition |
| **Separation of Concerns** | UI, ViewModel, and State are separate |
| **Testability** | ViewModels are easily unit-testable |
| **Reusability** | Components are modular and reusable |
| **Accessibility** | Semantic content, high contrast, large text |
| **Performance** | Lazy loading, throttling, remember |
| **Consistency** | Centralized theming, dimensions, shapes |

---

## 📋 Component Categories

| Category | Examples |
|----------|----------|
| **Atoms** | Button, Text, Icon, Switch |
| **Molecules** | GlassCard, NeonButton, DataChart |
| **Organisms** | ControlDashboard, GestureTrainingCenter |
| **Templates** | SettingsScreen, HomeScreen |
| **Pages** | MainActivity, Full screens |

---

## ✅ Summary

The Presentation Layer provides a **modern, reactive, and accessible UI** built with Jetpack Compose. It follows MVI architecture with unidirectional data flow, ensuring predictable and maintainable UI updates. The extensive component library and theming system ensure consistency across all screens, while the navigation system provides a seamless user experience.

---

**The Air Mouse Presentation Layer is a complete, production-ready UI system built with Jetpack Compose, following best practices for modern Android development.**