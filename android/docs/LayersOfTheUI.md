The UI of the Air Mouse Android application is a **modern, reactive, and highly modular system** built entirely with Jetpack Compose. It follows a **Unidirectional Data Flow (UDF)** architecture, meaning the UI is a pure function of state, and user interactions trigger events that update that state.

Here is a **complete, end‑to‑end explanation** of how the UI works, from the moment the app launches to the deepest gesture interaction.

---

## 1. The Big Picture: Layers of the UI

The UI system is divided into four interconnected layers:

1.  **Entry & Navigation** – How the app starts and moves between screens.
2.  **Theming & Styling** – How colours, shapes, and typography are dynamically applied.
3.  **Screen Architecture (MVI)** – The internal structure of each screen (State, Events, ViewModel).
4.  **Component Library** – The reusable building blocks that ensure consistency.

---

## 2. Entry Point & Root Container

### `MainActivity` – The Launchpad
- **Splash Screen**: Uses `installSplashScreen()` and keeps it visible while the app loads resources and checks permissions.
- **Onboarding Check**: If `prefs.isOnboardingCompleted()` is false, it immediately launches `OnboardingActivity` and finishes. Otherwise, it proceeds.
- **Permission Handling**: Checks for Camera, Bluetooth, Microphone, and Notifications permissions. Shows a rationale screen if they are missing, and retries.
- **Theme Application**: Reads `theme` and `accent_color` from preferences and applies them via `AirMouseTheme`.
- **Sensor Initialisation**: Starts the `SensorService` and sets the `onOrientationChanged` callback to send movement data via the `ConnectionManager`.
- **Set Content**: Finally, it calls `setContent { MainScreen() }`, rendering the root UI.

### `MainScreen.kt` – The Root UI Container
This is the heart of the UI. It sets up the `NavHostController`, `DrawerState`, and the top-level `Scaffold`.

```kotlin
@Composable
fun MainScreen(...) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { ModernDrawerContent(...) }
    ) {
        Scaffold(
            topBar = { /* Top App Bar with menu, title, status chip */ },
            bottomBar = { if (shouldShowBottomBar(currentRoute)) ModernBottomBar(...) },
            floatingActionButton = { /* Contextual FAB (Calibrate, Touchpad, etc.) */ }
        ) { paddingValues ->
            MainNavHost(
                navController = navController,
                modifier = Modifier.padding(paddingValues),
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
        }
    }
}
```

**Key components inside `MainScreen`**:

- **`ModalNavigationDrawer`**: The side menu (drawer) that appears when the hamburger icon is clicked.
- **`TopAppBar`**: Displays the app title and connection status (e.g., "Online" or "Waiting for approval").
- **`ModernBottomBar`**: Shows 4 tabs: **Home**, **Statistics**, **Settings**, **Help**. It only appears when the current route is one of these four (controlled by `shouldShowBottomBar`).
- **`FloatingActionButton`**: Dynamically changes based on the current route and control mode (e.g., shows "Calibrate" on Home, "Touchpad" when in touchpad mode).
- **`MainNavHost`**: The actual navigation graph where all screens are defined.

---

## 3. Navigation System

### `Destinations.kt` – The Route Registry
Every screen is defined as a `sealed class Destinations` with a unique `route`, a `title`, and an `icon`. This centralises all routing logic.

```kotlin
sealed class Destinations(val route: String, val title: String, val icon: ImageVector) {
    object Home : Destinations("home", "Home", Icons.Default.Home)
    object Settings : Destinations("settings", "Settings", Icons.Default.Settings)
    // ... over 20 destinations
}
```

### `NavigationActions.kt` – Abstraction for Navigation
Instead of calling `navController.navigate()` directly, the UI uses the `NavigationActions` interface. This makes the UI testable and decouples it from the specific `NavController` implementation.

```kotlin
interface NavigationActions {
    fun navigateTo(route: String)
    fun navigateBack()
    fun navigateToHome()
    fun navigateToSettings()
    // ... etc.
}
```

### `MainNavHost.kt` – The NavGraph
This function defines the `NavHost` and maps each `composable` route to its respective screen composable.

- **Transitions**: Uses `enterTransition` and `exitTransition` with `fadeIn` and `slideInHorizontally` for smooth screen changes.
- **Arguments**: Supports passing arguments (e.g., `quality` for the calibration result screen).
- **Dependency Injection**: Each screen uses `hiltViewModel()` to get its ViewModel.

**How navigation works:**
1. A user clicks a button in `HomeScreen`.
2. The button calls `navigationActions.navigateTo(Destinations.Settings.route)`.
3. `NavigationActionsImpl` calls `navController.navigate(route)`.
4. The `NavHost` matches the route and renders the `SettingsScreen` composable.
5. The back button is handled by `navigationActions.navigateBack()`.

---

## 4. Theming & Styling – Dynamic and Centralised

### `Theme.kt` & `ThemeColors.kt` – The Colour System
The UI supports **light, dark, pure black, high contrast**, and several **premium themes** (Ocean, Sunset, Forest, etc.), all with customisable **accent colours**.

- **`ThemeColorScheme`**: A data class containing all Material 3 colour roles (`background`, `surface`, `primary`, `onSurface`, etc.).
- **`ThemeColorSchemes`**: An object that provides predefined schemes (e.g., `darkTheme()`, `oceanTheme()`).
- **`AccentColor`**: An enum with 14 colours (Orange, Blue, Green, Purple, Pink, etc.), each with its own hex codes.

### `LocalThemeColors` – CompositionLocal
To avoid passing colours down through every composable, the theme is provided via a `CompositionLocal`.

```kotlin
val LocalThemeColors = staticCompositionLocalOf { ThemeColorSchemes.darkTheme(AccentColor.ORANGE) }

@Composable
fun ProvideThemeColors(colors: ThemeColorScheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThemeColors provides colors) {
        content()
    }
}
```

Any composable can access the current theme colours with `val colors = LocalThemeColors.current`.

### `AirMouseTheme` – The Wrapper
This composable reads the current `themeId` and `accentColor` from preferences, generates the `ThemeColorScheme`, and wraps the entire app content.

```kotlin
@Composable
fun AirMouseTheme(themeId: String, accentColor: AccentColor, content: @Composable () -> Unit) {
    val colors = getThemeColorScheme(themeId, accentColor)
    ProvideThemeColors(colors) {
        MaterialTheme(
            colorScheme = // Maps ThemeColorScheme to M3 ColorScheme,
            typography = AirMouseTypography,
            shapes = AirMouseShapes,
            content = content
        )
    }
}
```

### `Dimensions.kt` & `Shapes.kt`
- **`Dimensions`**: Centralises all spacing (4dp, 8dp, 16dp...), padding, corner radii, icon sizes, button heights, and font sizes. Ensures consistent spacing across the entire app.
- **`AppShapesProvider`**: Defines reusable `RoundedCornerShape`s (e.g., `card`, `button`, `bottomSheet`).

---

## 5. Screen Architecture – The MVI Pattern

Every screen follows a consistent **Model-View-Intent (MVI)** pattern, which is a flavour of Unidirectional Data Flow.

### Components of the MVI Pattern

1.  **State (`UiState`)** – A **data class** that represents the entire state of the screen (e.g., `HomeUiState`, `SettingsUiState`). It contains all the data needed to render the UI (loading state, lists, booleans, error messages, etc.).

2.  **Event (`Event`)** – A **sealed class** that represents user actions or system events (e.g., `SettingsEvent.ToggleHaptic`, `TouchpadEvent.TouchEvent`).

3.  **Effect (`Effect`)** – A **sealed class** for one‑time events that should not survive recomposition (e.g., showing a Toast, navigating to another screen, opening a URL).

4.  **ViewModel** – The bridge between the UI and the domain/data layers. It holds the `State` and `Effect` flows, handles incoming `Event`s, and executes business logic.

### The Flow (Unidirectional Data Flow)

```mermaid
graph LR
    A[User Interaction] --> B[UI sends Event]
    B --> C[ViewModel processes Event]
    C --> D[ViewModel updates State]
    D --> E[UI recomposes with new State]
    C --> F[ViewModel emits Effect]
    F --> G[UI handles Effect (Toast, Nav)]
```

### Example: `SettingsScreen` in Action

#### 1. State Definition
```kotlin
data class SettingsUiState(
    val sensitivity: Float = 0.5f,
    val hapticEnabled: Boolean = true,
    val theme: String = "system",
    // ... 50+ other settings
)
```

#### 2. Event Definition
```kotlin
sealed class SettingsEvent {
    data class UpdateSensitivity(val value: Float) : SettingsEvent()
    object ToggleHaptic : SettingsEvent()
    data class UpdateTheme(val theme: String) : SettingsEvent()
    // ...
}
```

#### 3. Effect Definition
```kotlin
sealed class SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect()
    data class NavigateTo(val route: String) : SettingsEffect()
}
```

#### 4. ViewModel Logic
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(private val prefs: PreferencesManager) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsEffect>()
    val effect: SharedFlow<SettingsEffect> = _effect.asSharedFlow()

    fun handleEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.UpdateSensitivity -> updateSensitivity(event.value)
                SettingsEvent.ToggleHaptic -> toggleHaptic()
                // ...
            }
        }
    }

    private suspend fun toggleHaptic() {
        val current = _uiState.value.hapticEnabled
        prefs.putBoolean("haptic_enabled", !current)
        _uiState.update { it.copy(hapticEnabled = !current) }
        _effect.emit(SettingsEffect.ShowToast("Haptic ${if (!current) "enabled" else "disabled"}"))
    }
}
```

#### 5. UI Observation and Event Dispatch
```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)

    // Handle Effects
    LaunchedEffect(effect) {
        when (effect) {
            is SettingsEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            is SettingsEffect.NavigateTo -> navigationActions.navigateTo(effect.route)
            null -> { /* ignore */ }
        }
    }

    // Render UI based on State
    SettingsSwitch(
        title = "Haptic Feedback",
        checked = uiState.hapticEnabled,
        onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleHaptic) }
    )
}
```

---

## 6. Deep Dive: Major Screens and Their Unique Mechanisms

### A. `HomeScreen` – The Command Centre
The `HomeScreen` is the most complex UI component. It uses a `LazyColumn` to vertically stack numerous information cards.

- **Real‑time Sensor Data**: It subscribes to `HomeViewModel.sensorState` (which updates at ~30 FPS) to render:
    - A 3D `SensorVisualizer` (rotating cube) showing the phone's orientation.
    - A 2D `MouseMotionPreviewCard` showing a green square that follows the phone's tilt.
    - Live gyroscope charts (`InlineSensorChartsCard`).
- **Connection Management**: The top card (`ConnectionToggleButton`) handles the primary CTA (Connect/Disconnect). The `StateOverviewBanner` shows detailed status (Approval countdown, server IP, calibration status).
- **Collaboration Gate**: The `CollaborationGateCard` displays a donut chart showing calibration progress. It blocks access to certain features until calibration is complete.
- **Quick Actions**: A row of four small cards (`QuickActionsRow`) provides shortcuts to Calibration, Gesture Studio, Voice Commands, and Network Discovery.

### B. `TouchpadScreen` – Full Gesture Surface
This screen turns the phone into a virtual touchpad. It uses the `pointerInput` modifier to capture raw touch events.

- **Event Capture**: `Modifier.pointerInput(Unit) { awaitEachGesture { ... } }` tracks down, move, and up events.
- **Gesture Processing**: The ViewModel analyses the pointer count (1, 2, 3, or 4 fingers) and movement to determine the gesture (Drag, Scroll, Swipe, Pinch, Tap, Long‑Press).
- **Visual Feedback**: If `showTouchPoints` is enabled, the `TouchpadSurface` draws circles at the finger positions with a Canvas.
- **Haptic Feedback**: On tap or gesture completion, the ViewModel triggers `Vibrator` (if enabled).

### C. `SettingsScreen` – The Configuration Hub
This screen uses a `LazyColumn` with a `SettingsCard` for each section. Clicking a card navigates to a `SectionDetailScreen` within the same `Scaffold` (using a `selectedSection` state variable).

- **In‑line Editing**: Cursor and gesture settings use `SettingsSlider` and `SettingsSwitch` composables that directly dispatch events to the ViewModel.
- **Persistent State**: Every slider or switch change immediately writes to `PreferencesManager` via the ViewModel, ensuring settings persist across app restarts.
- **Import/Export**: The Privacy section allows exporting all settings as a `.txt` file and importing them back, using the `ActivityResultContracts.OpenDocument()` launcher.

### D. `CalibrationScreen` – Step‑by‑Step Wizard
This is a state‑driven wizard with three phases: `INTRO`, `COUNTDOWN`, and `SAMPLING`.

- **Phase Management**: The UI changes drastically based on the `CalibrationPhase`. In `SAMPLING`, it shows a pulsing animation and a linear progress bar.
- **Sensor Sampling**: The `CalibrationViewModel` requests sensor data via the `CalibrationHelper` and updates the UI with live gyro/accel values.
- **Quality Assessment**: After completion, it navigates to `CalibrationResultScreen` which displays the `CalibrationQuality` (Excellent, Good, Fair, Poor) with emojis and descriptions.

---

## 7. Reusable UI Component Library

To maintain consistency and speed up development, the app has a rich library of composable components (mostly in `AllUIComponents.kt` and `/components`).

### Atomic Components
- **`GlassCard`**: A card with a blurred, translucent background (glassmorphism).
- **`NeonButton`**: A button with a pulsing glow effect.
- **`AnimatedSwitch` / `AnimatedCheckbox`**: Components with spring animations.
- **`ShimmerEffect`**: A loading placeholder with a moving gradient.
- **`ParticleBackground`**: A background with floating particles.

### Compound Components
- **`ControlDashboard`**: A reusable dashboard card showing connection status, battery, cursor position, and quick actions.
- **`GestureTrainingCenter`**: A card displaying a list of gestures with training progress bars and "Train" buttons.
- **`AnalyticsDashboard`**: Displays a gesture heatmap and activity chart using custom Canvas drawing.
- **`CommunityHub`**: A social feed with posts, likes, and comments.

---

## 8. State Persistence and Lifecycle

- **PreferencesManager**: All settings (sensitivity, theme, haptic, etc.) are stored in `SharedPreferences` (via DataStore or the custom `PreferencesManager` interface). This ensures that when the user re‑opens the app, their preferences are restored.
- **Room Database**: Large datasets (calibration data, gesture templates, user profiles, statistics) are stored in Room. The `LocalDataSourceImpl` handles all CRUD operations.
- **ViewModel Lifecycle**: ViewModels survive configuration changes (screen rotation). They are scoped to the `NavBackStackEntry` of their respective screen, so state is preserved when navigating back.
- **State Restoration**: The `StateFlow` in ViewModels automatically retains the last emitted state. The `collectAsStateWithLifecycle()` API suspends collection when the screen is in the background to save resources.

---

## 9. Interaction Flow: From Touch to Server

To illustrate how the UI connects to the outside world, here is the complete flow of a **touchpad drag**:

1. **User Touch**: User touches the `TouchpadSurface` and drags their finger.
2. **Touch Event**: The `Modifier.pointerInput` block captures the motion event and calls `viewModel.handleEvent(TouchpadEvent.TouchEvent(x, y, pointerCount, pointers, pressure))`.
3. **ViewModel Processing**: `TouchpadViewModel` calculates `dx` and `dy` based on the movement, applies sensitivity, acceleration, and inversion settings.
4. **Command Dispatch**: The ViewModel calls `connectionManager.sendMove(dx, dy)`.
5. **Network Layer**: `ConnectionManager` formats the message as JSON `{"type":"move","dx":12.5,"dy":-3.2}` and sends it via WebSocket/TCP/UDP.
6. **UI Feedback**: Simultaneously, the ViewModel updates the `_uiState` (e.g., updating `currentX`, `currentY`, and `touchPoints`), which triggers a recomposition of the touch points on the Canvas.

---

## 10. Performance Optimisation

- **Throttling**: `HomeViewModel` limits sensor processing to 100Hz (10ms interval) and UI updates to ~30Hz (33ms interval). Move messages are sent at ~60Hz (16ms interval).
- **Lazy Loading**: All scrollable lists use `LazyColumn` or `LazyRow` to only compose visible items.
- **Remember and DeriveState**: Heavy calculations are wrapped in `remember` or `derivedStateOf` to prevent recomputation on every recomposition.
- **Animations**: Uses `AnimatedVisibility` and `animate*AsState` for smooth transitions without blocking the UI thread. Heavy particle systems run on a separate `Canvas` with minimal recomposition.

---

## ✅ Summary

The Air Mouse UI is a **state‑of‑the‑art Jetpack Compose application** that excels in:

- **Reactive UDF**: Unidirectional data flow makes the app predictable and easy to debug.
- **Modularity**: Every screen and component is isolated and reusable.
- **Dynamic Theming**: Full M3 support with custom colours and accent schemes.
- **Real‑time Performance**: Optimised sensor and touch handling for smooth, lag‑free control.
- **Rich Interaction**: Supports complex gestures (click, scroll, pinch, swipe, long‑press) across multiple modes (mouse, touchpad, voice, proximity).

The UI seamlessly ties together the sensor pipeline, network communication, and data persistence, providing a cohesive and polished user experience.