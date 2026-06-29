# Air Mouse – Professional UI Architecture & Integration Guide

This document provides a **complete, in‑depth analysis** of the Air Mouse UI architecture. It explains how every UI component, from the smallest atomic composable to the largest screen, is designed, implemented, and integrated to create a **polished, professional, and highly performant** application.

---

## 1. Architectural Foundation: Unidirectional Data Flow (MVI)

### Why MVI?
The UI follows the **Model‑View‑Intent (MVI)** pattern, which ensures:
- **Predictability**: State is the single source of truth.
- **Testability**: Each layer (State, Event, Effect) is isolated.
- **Traceability**: Every user action results in a state change, making debugging trivial.

### The Three Pillars of Every Screen

| Component | Purpose | Example |
|-----------|---------|---------|
| **State (UiState)** | A data class holding all UI‑related data. | `SettingsUiState(sensitivity=0.5f, hapticEnabled=true, ...)` |
| **Event** | A sealed class representing user or system actions. | `SettingsEvent.ToggleHaptic` |
| **Effect** | A sealed class for side effects (e.g., navigation, toasts). | `SettingsEffect.ShowToast("Haptic toggled")` |

### The Flow
1. **User Interaction** → UI dispatches an `Event`.
2. **ViewModel** processes the `Event` (business logic, repository calls).
3. **ViewModel** updates the `State` (using `MutableStateFlow`).
4. **UI** recomposes with the new `State`.
5. **ViewModel** may emit a one‑time `Effect` (e.g., navigation, toast).
6. **UI** handles the `Effect`.

This pattern is used consistently across all screens, ensuring a unified and maintainable codebase.

---

## 2. Navigation System – Seamless and Structured

### `Destinations.kt` – The Single Source of Truth
Every screen is defined as a `sealed class Destinations` with:
- **`route`**: Unique string for navigation.
- **`title`**: Display name.
- **`icon`**: Compose `ImageVector` for bottom bar/drawer.

```kotlin
sealed class Destinations(val route: String, val title: String, val icon: ImageVector) {
    object Home : Destinations("home", "Home", Icons.Default.Home)
    object Settings : Destinations("settings", "Settings", Icons.Default.Settings)
    // ... 20+ destinations
}
```

### `NavigationActions.kt` – Abstraction for Decoupling
The UI never uses `NavController` directly. Instead, it uses the `NavigationActions` interface. This allows:
- Easy testing (mock the interface).
- Flexibility to change navigation implementation (e.g., deep linking).
- Clear separation of concerns.

```kotlin
interface NavigationActions {
    fun navigateTo(route: String)
    fun navigateBack()
    fun navigateToHome()
    fun navigateToSettings()
    // ...
}
```

### `MainNavHost.kt` – The NavGraph
This `NavHost` defines every possible route and the composable associated with it.

- **Transitions**: Uses `enterTransition` and `exitTransition` with `fadeIn` + `slideInHorizontally` for smooth, app‑specific transitions.
- **Argument Passing**: Supports passing arguments (e.g., `quality` for calibration result).
- **Nested Navigation**: The bottom bar and drawer routes coexist, and the `shouldShowBottomBar()` function conditionally shows the bottom bar based on the current route.

---

## 3. Theming – Dynamic, Comprehensive, and Consistent

### `ThemeColorScheme` – All Colour Roles
Instead of using Material 3’s `ColorScheme` directly, the app defines its own `ThemeColorScheme` data class containing **all** M3 colour roles (`background`, `surface`, `primary`, `onSurface`, `error`, etc.). This ensures we have full control and can extend with custom colours.

### `ThemeColorSchemes` – Theme Definitions
Each theme (light, dark, pure black, ocean, sunset, etc.) is defined as a function returning a `ThemeColorScheme`. These functions accept an `AccentColor` parameter, allowing the accent colour to be dynamically overlaid.

### `LocalThemeColors` – CompositionLocal for Scoped Access
```kotlin
val LocalThemeColors = staticCompositionLocalOf { ThemeColorSchemes.darkTheme(AccentColor.ORANGE) }
```
Any composable can access `LocalThemeColors.current` to get the current colour scheme, avoiding excessive prop‑drilling.

### `AirMouseTheme` – The Wrapper
This composable reads `themeId` and `accentColor` from preferences, generates the `ThemeColorScheme`, and provides it via `CompositionLocalProvider`. It also wraps the `MaterialTheme` to apply M3 typography and shapes.

### System Bar Integration
The `AirMouseTheme` also handles system bar colouring (status bar, navigation bar) using `WindowCompat.getInsetsController()`, ensuring seamless edge‑to‑edge design.

---

## 4. Reusable Component Library – The Design System

The app leverages a rich set of reusable composables that promote consistency and speed up development.

### Atomic Components (Atoms)
| Component | Purpose |
|-----------|---------|
| `GlassCard` | A card with glassmorphism effect (translucent background with blur). |
| `NeonButton` | A button with a pulsing glow gradient. |
| `AnimatedSwitch` / `AnimatedCheckbox` | Switches/checkboxes with spring animations. |
| `ShimmerEffect` | A loading placeholder with a moving gradient. |
| `ParticleBackground` | A canvas‑based particle system for background animations. |
| `HolographicText` | Text with a moving rainbow gradient. |
| `TypewriterText` | Text that types out character by character. |
| `AnimatedCounter` | A number that counts up/down with animation. |

### Molecular Components (Molecules)
| Component | Purpose |
|-----------|---------|
| `AnimatedConnectionStatus` | A circular status indicator with pulsing ring and signal bars. |
| `ConnectionStatusBadge` | A small chip with status text and reconnect/disconnect actions. |
| `SensorVisualizer` | A 3D phone visualisation with live roll/pitch/yaw rotation. |
| `GestureWaveform` | A real‑time chart showing gesture data points. |
| `DataChart` | Line and donut charts for analytics. |
| `FloatingActionMenu` | An expandable FAB with multiple actions. |

### Organic Components (Organisms)
| Component | Purpose |
|-----------|---------|
| `ControlDashboard` | A comprehensive card with connection status, battery, cursor position, and quick actions. |
| `GestureTrainingCenter` | A list of gestures with training progress bars and "Train" buttons. |
| `AnalyticsDashboard` | Displays gesture heatmap and activity chart. |
| `CommunityHub` | A social feed with posts, likes, and comments. |
| `MacroRecorder` | A card for recording/playing macros with action list and controls. |

All components are **themed** – they use `MaterialTheme.colorScheme` or `LocalThemeColors.current` to adapt to the current theme automatically.

---

## 5. Key Screens – Detailed Implementation

### A. `MainScreen` – The Root Container
- **`ModalNavigationDrawer`**: The side menu (drawer) with `ModernDrawerContent`.
- **`Scaffold`**: Provides the top bar, bottom bar, and FAB.
- **Dynamic FAB**: The FAB changes based on the current route and control mode (e.g., "Calibrate" on Home, "Touchpad" in touchpad mode).
- **`MainNavHost`**: Renders the current destination.

#### Drawer Content (`ModernDrawerContent`)
- **Header**: Shows app name, version, user name, connection status chips.
- **Quick Links**: Home, Calibration, Touchpad, Sensor Visualizer, Network Discovery.
- **Core**: Statistics, Settings, Help, About.
- **Control**: Gesture Studio, Edge Gestures, Voice Commands, Proximity Lock, Touchpad Settings.
- **Control Mode Selector**: Radio buttons for Motion, Touchpad, Arm Movement.
- **System**: Server Logs, Battery Monitor, Accessibility, Profiles, Themes.
- **Footer**: Drawer shortcuts info.

### B. `HomeScreen` – The Dashboard
Uses a `LazyColumn` with 20+ items, each a card or component.

- **Real‑time Sensor Data**: Subscribes to `HomeViewModel.sensorState` (emits at 30 FPS) to display:
    - `SensorVisualizer` (3D cube)
    - `MouseMotionPreviewCard` (green square moving based on tilt)
    - `InlineSensorChartsCard` (tiny gyro/accel/mag charts)
- **Connection Management**:
    - `ConnectionToggleButton`: Large CTA with auto‑reconnect toggle.
    - `StateOverviewBanner`: Shows approval countdown, server IP, calibration status.
- **Collaboration Gate**: `CollaborationGateCard` displays a donut chart with calibration progress and blocks features until complete.
- **Quick Actions**: `QuickActionsRow` (Calibrate, Gesture Studio, Voice Commands, Network Discovery).
- **Live Motion Center**: Shows live `Dx`, `Dy`, speed, FPS, and battery.
- **Domain Summary Card**: Displays snapshot of domain models (sensitivity, clicks, theme, etc.).
- **Service Hub**: Presentation mode and file transfer status.
- **Sensor Visualizer Shortcut**: Quick link to full‑screen sensor charts.
- **Access Gate**: Prompts registration/calibration if not done.
- **Greeting Card**: Animated welcome message with user name.
- **Connection Controls**: IP/Port input, protocol selection, QR scan.
- **Stats Row**: Clicks, scrolls, session duration.
- **Performance Card**: Battery, CPU, RAM, FPS.
- **Recent Gestures**: Lists recent gestures with clear button.
- **Tips Card**: Pro tips.
- **Footer**: App version.

### C. `SettingsScreen` – The Configuration Hub
This screen uses a `LazyColumn` with a `SettingsCard` for each section. Clicking a card navigates to a `SectionDetailScreen` within the same `Scaffold` (using a `selectedSection` state variable).

- **In‑line Editing**: Cursor and gesture settings use `SettingsSlider` and `SettingsSwitch` composables that directly dispatch events to the ViewModel.
- **Persistent State**: Every slider or switch change immediately writes to `PreferencesManager` via the ViewModel.
- **Import/Export**: Privacy section allows exporting all settings as a `.txt` file and importing them back, using `ActivityResultContracts.OpenDocument()`.
- **Reset to Defaults**: Clears all preferences and reloads defaults.

### D. `TouchpadScreen` – Gesture Surface
- **Touch Capture**: `Modifier.pointerInput(Unit) { awaitEachGesture { ... } }` tracks down, move, and up events.
- **Gesture Processing**: The ViewModel analyses pointer count (1,2,3,4) and movement to determine gestures (drag, scroll, swipe, pinch, tap, long‑press).
- **Visual Feedback**: `TouchpadSurface` draws touch points with Canvas if `showTouchPoints` is enabled.
- **Haptic Feedback**: On tap or gesture completion, `Vibrator` is triggered (if enabled).
- **Quick Presets**: Standard, Precision, Gaming, Presentation presets that apply a set of settings.
- **Expanded Settings**: Scroll, cursor, gesture, and feedback settings in expandable cards.

### E. `CalibrationScreen` – Wizard with Three Phases
- **Phases**: `INTRO` (display step info), `COUNTDOWN` (countdown animation), `SAMPLING` (collecting sensor data with live progress).
- **Live Sensor Data**: Shows real‑time gyro, accel, mag values.
- **Instruction**: Displays text instructions for each step (e.g., "Place device flat and keep still").
- **Step Tracker**: Visual progress bar showing 3 steps.
- **Quality Assessment**: After completion, shows `CalibrationQuality` (Excellent, Good, Fair, Poor) with emojis and descriptions.

### F. `VoiceCommandsScreen` – Voice Control
- **Start/Stop Listening**: Large microphone button with pulsing animation.
- **Wake Word**: Toggle and customisation.
- **Available Commands**: List of built‑in commands with icons and descriptions.
- **Custom Commands**: Add/delete custom phrases mapped to actions.
- **Command History**: Shows recent commands with timestamps and confidence.
- **Settings**: Sensitivity, continuous listening, voice feedback, sound effects.

### G. `ProfilesScreen` – User Profile Management
- **List/Grid/Compact Views**: Three view modes controlled by a toggle.
- **Sorting**: By name, date created, last used, favourite, usage count.
- **CRUD**: Create, edit, delete profiles.
- **Favourites**: Toggle favourite status.
- **Default Profile**: Set a profile as default.
- **Usage Statistics**: Shows usage count, last used, days since last used.
- **Import/Export**: Profile data can be exported/imported as JSON.

---

## 6. Integration with ViewModels and Use Cases

### ViewModel Responsibilities
- **State Management**: Holds `StateFlow` for `UiState` and `SharedFlow` for `Effect`.
- **Event Handling**: Processes events by calling use cases or repositories.
- **Persistence**: Saves/loads data via `PreferencesManager` or Room.
- **Network**: Sends commands via `ConnectionManager`.
- **Sensor**: Subscribes to sensor data (via `SensorService` or `SensorRepository`).

### Use Cases – Encapsulated Business Logic
Each use case (e.g., `ConnectToServerUseCase`, `SendMovementUseCase`) contains a single business rule and calls the appropriate repository. This keeps ViewModels thin and focused on UI logic.

### Repository Pattern – Abstraction of Data Sources
Repositories (e.g., `CalibrationRepositoryImpl`) abstract the data source (Room, Preferences, Network). The ViewModel and Use Cases only depend on the repository interfaces, making the code testable and flexible.

### Example Flow: Calibration
1. User taps "Start Calibration" in `CalibrationScreen`.
2. `CalibrationViewModel` dispatches `CalibrationEvent.StartCalibration`.
3. ViewModel calls `CalibrationUseCase.startFullCalibration()`.
4. The use case calls `CalibrationRepository.calibrateGyroscope()`, `.calibrateMagnetometer()`, etc.
5. The repository uses `CalibrationHelper` to collect sensor data and save to `PreferencesManager`.
6. Progress is reported back via callbacks, updating the UI state.
7. When complete, the ViewModel emits a success state and navigates to the result screen.

---

## 7. Real‑time Data Handling – Sensors and Network

### Sensor Data Flow
- `SensorService` registers listeners for gyroscope, accelerometer, magnetometer.
- It applies calibration (via `CalibrationHelper`) and runs Madgwick fusion to produce `roll`, `pitch`, `yaw`.
- The `HomeViewModel` subscribes to `sensorState` (updated at 30 FPS) via a callback.
- The UI observes `sensorState` and recomposes the relevant components (e.g., `SensorVisualizer`, `MouseMotionPreviewCard`).

### Network Communication Flow
- `ConnectionManager` manages WebSocket/TCP/UDP connections.
- It exposes `connectionStatus`, `connectionQuality`, `serverName` as `StateFlow`.
- The `HomeViewModel` and `MainViewModel` collect these flows and update the UI (e.g., connection status chip, signal bars).
- `ConnectionManager` also handles message sending (move, click, scroll, control) and receives responses (ACK, pong).

### Throttling for Performance
- Sensor processing is throttled to 100Hz (10ms interval) and UI updates to ~30Hz (33ms interval) to balance responsiveness and battery life.
- Move messages are sent at ~60Hz (16ms interval) to maintain smooth cursor movement without overwhelming the network.

---

## 8. Animations and Transitions – Elevating UX

### Screen Transitions
- **Fade + Slide**: `enterTransition` and `exitTransition` use `fadeIn/Out` and `slideIn/OutHorizontally` with `tween(300)` for smooth, non‑jarring navigation.

### State‑based Animations
- **`animate*AsState`**: Used extensively for progress bars, counters, checkmarks, and pulsing effects.
- **`AnimatedVisibility`**: For expandable sections, loading spinners, and toast messages.
- **`AnimatedContent`**: For switching between view modes (list/grid) with a crossfade.

### Advanced Animations
- **Particle Systems**: `FloatingParticles` and `ParticleBackground` use `Canvas` with `LaunchedEffect` to animate dozens of particles.
- **Neon Glow**: `NeonButton` uses `infiniteRepeatable` animation to pulse the glow intensity.
- **Radar Animation**: `RadarAnimation` rotates a radar sweep and pulses the outer ring.
- **Voice Wave**: `VoiceWaveAnimation` animates bars to simulate voice activity.

---

## 9. Performance Optimisations

### Compose‑specific Optimisations
- **`derivedStateOf`**: For expensive computations derived from state.
- **`remember`**: To cache objects and avoid recomputation.
- **`key`** in LazyColumn items to avoid unnecessary recomposition.
- **`@Stable` / `@Immutable`** annotations for data classes to help Compose’s compiler.

### Sensor Throttling
- Sensor data is processed in a background coroutine, and UI updates are throttled to 30 FPS. This prevents the UI from being flooded with updates.

### Lazy Loading
- All scrollable content uses `LazyColumn` or `LazyRow`, which only compose visible items.
- `LazyColumn` items are keyed with `key = { it.id }` for efficient updates.

### Memory Management
- Images and bitmaps are handled with `remember` and `DisposableEffect` to release resources when the composable leaves the composition.
- The `DebugOverlay` service releases sensor listeners when not visible.

---

## 10. Professional Touches – The "Wow" Factor

### Glassmorphism (GlassCard)
- `GlassCard` uses a semi‑transparent background with a subtle gradient and a blur effect (achieved via `Modifier.background(Brush.verticalGradient(...))` with alpha).
- This gives a modern, premium look, often used in Apple’s design language.

### Neon and Glow Effects (NeonButton)
- A `NeonButton` uses a `Brush.horizontalGradient` and an infinite animation to pulsate the glow intensity, creating a futuristic, gaming‑style aesthetic.

### Holographic Text
- `HolographicText` uses a `Brush.linearGradient` with colours that shift over time using `Color.hsv()`, creating a rainbow, holographic effect.

### Typewriter Text
- `TypewriterText` uses a `LaunchedEffect` to add characters one by one, creating a nostalgic, interactive feel.

### Radar Animation
- `RadarAnimation` simulates a radar sweep with a pulsing outer ring, used to indicate active scanning or connection.

### Voice Wave Animation
- `VoiceWaveAnimation` animates bars to simulate voice input, giving immediate audio feedback.

### Particle Backgrounds
- `ParticleBackground` and `FloatingParticles` add subtle, dynamic backgrounds that make the UI feel alive and fluid.

---

## 11. Accessibility – Inclusive Design

### Semantic Content
- All `Icon` and `Image` composables use `contentDescription` for screen readers.
- `Modifier.semantics` is used to provide additional context.

### High Contrast and Large Text
- The accessibility screen allows users to enable high contrast mode and increase font size, both of which are respected by all components.

### Focus and Keyboard Navigation
- `Modifier.focusable()` and `Modifier.onKeyEvent` ensure that keyboard navigation is possible for external keyboards.

### Colour Blind Modes
- The accessibility screen includes presets for protanopia, deuteranopia, and tritanopia, adjusting colours accordingly.

### Switch Access / Dwell Click
- Advanced accessibility features like dwell click (auto‑click after cursor stops) are implemented, making the app usable for users with motor impairments.

---

## 12. Testing and Maintainability

### Unit Testing
- ViewModels are tested with `MockK` to verify event handling and state updates.
- Use cases are tested in isolation with mocked repositories.

### UI Testing
- `ComposeTestRule` is used to write UI tests that verify the rendering and behaviour of screens.

### Code Maintainability
- **Separation of Concerns**: UI, domain, and data layers are strictly separated.
- **Dependency Injection**: Hilt manages all dependencies, making it easy to swap implementations.
- **Centralised Constants**: All strings, dimensions, colours, and routes are defined in central files (`Dimensions.kt`, `Destinations.kt`, etc.).
- **Consistent Patterns**: All screens follow the same MVI pattern, reducing cognitive load for developers.

---

## ✅ Summary – What Makes the Air Mouse UI "Professional"

| Aspect | How It's Achieved |
|--------|-------------------|
| **Architecture** | Clean MVI with unidirectional data flow, separation of concerns, and reactive state. |
| **Theming** | Fully dynamic, with 20+ themes, accent colours, and system‑bar integration. |
| **Navigation** | Structured, testable, and intuitive with bottom bar, drawer, and transitions. |
| **Component Library** | Rich, reusable, and consistent atoms/molecules/organisms. |
| **Real‑time Integration** | Seamless connection of sensors, network, and UI with throttling for performance. |
| **Animations** | Fluid, purposeful, and engaging – from screen transitions to neon glows. |
| **Accessibility** | Inclusive design with high contrast, large text, and screen reader support. |
| **Performance** | Optimised composition, lazy loading, and background processing. |
| **Professional Touches** | Glassmorphism, neon effects, holographic text, particle systems – all carefully applied for a premium feel. |
| **Maintainability** | Modular, well‑documented, and dependency‑injected codebase. |

The Air Mouse UI is **not just functional** – it is a **showcase of modern Android development**, blending cutting‑edge design with robust engineering to deliver a seamless, immersive, and accessible user experience.