# 📘 Air Mouse Navigation Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.navigation` package provides a **complete navigation system** for the Air Mouse application. Built on Jetpack Compose Navigation, it offers a structured, type-safe, and maintainable navigation solution with bottom bar support, drawer integration, and comprehensive route management.

```
com.airmouse.presentation.navigation/
├── Destinations.kt              # Navigation routes and constants
├── NavigationActions.kt         # Navigation abstraction layer
├── AirMouseNavHost.kt           # NavHost graph definition
└── AirMouseBottomBar.kt         # Bottom navigation bar component
```

---

## 🧭 1. Destinations.kt

### Purpose
Centralizes **all navigation routes** in the application. Uses a sealed class to define each destination with its route, title, and icon. Provides helper functions for route validation, bottom bar visibility, and navigation arguments.

### Key Features

| Feature | Description |
|---------|-------------|
| **Type-Safe Routes** | All routes are defined as constants to prevent typos |
| **Icon Association** | Each destination has a Material Design icon |
| **Bottom Bar Management** | Defines which routes appear in the bottom navigation |
| **Route Helpers** | `fromRoute()`, `isBottomNavScreen()`, `shouldShowBackButton()` |
| **Argument Support** | `createRoute()` for building routes with arguments |

### Destination Objects

```kotlin
sealed class Destinations(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    // Bottom Navigation Screens
    object Home : Destinations(ROUTE_HOME, "Home", Icons.Default.Home)
    object Statistics : Destinations(ROUTE_STATISTICS, "Stats", Icons.Default.BarChart)
    object Settings : Destinations(ROUTE_SETTINGS, "Settings", Icons.Default.Settings)
    object Help : Destinations(ROUTE_HELP, "Help", Icons.AutoMirrored.Filled.Help)
    
    // Feature Screens
    object Calibration : Destinations(ROUTE_CALIBRATION, "Calibrate", Icons.Default.Tune)
    object Touchpad : Destinations(ROUTE_TOUCHPAD, "Touchpad", Icons.Default.TouchApp)
    object GestureStudio : Destinations(ROUTE_GESTURE_STUDIO, "Gestures", Icons.Default.Gesture)
    object VoiceCommands : Destinations(ROUTE_VOICE_COMMANDS, "Voice", Icons.Default.Mic)
    object Proximity : Destinations(ROUTE_PROXIMITY, "Proximity", Icons.Default.NearMe)
    object Profiles : Destinations(ROUTE_PROFILES, "Profiles", Icons.Default.Person)
    object Themes : Destinations(ROUTE_THEMES, "Themes", Icons.Default.Palette)
    object Accessibility : Destinations(ROUTE_ACCESSIBILITY, "Access", Icons.Default.Accessibility)
    object ServerLogs : Destinations(ROUTE_SERVER_LOGS, "Logs", Icons.Default.List)
    object NetworkDiscovery : Destinations(ROUTE_NETWORK_DISCOVERY, "Network", Icons.Default.Wifi)
    object Battery : Destinations(ROUTE_BATTERY, "Battery", Icons.Default.BatteryFull)
    object About : Destinations(ROUTE_ABOUT, "About", Icons.Default.Info)
    object Onboarding : Destinations(ROUTE_ONBOARDING, "Onboarding", Icons.Default.Apps)
    object CalibrationResult : Destinations(ROUTE_CALIBRATION_RESULT, "Calibration Result", Icons.Default.CheckCircle)
    object SensorVisualizer : Destinations(ROUTE_SENSOR_VISUALIZER, "Sensors", Icons.Default.Sensors)
    object EdgeGestures : Destinations(ROUTE_EDGE_GESTURES, "Edge", Icons.Default.Swipe)
    object TouchpadSettings : Destinations(ROUTE_TOUCHPAD_SETTINGS, "Touchpad Settings", Icons.Default.Settings)
    object FileTransfer : Destinations(ROUTE_FILE_TRANSFER, "File Transfer", Icons.Default.Folder)
}
```

### Route Constants

```kotlin
companion object {
    // Bottom Navigation
    const val ROUTE_HOME = "home"
    const val ROUTE_STATISTICS = "statistics"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_HELP = "help"
    
    // Feature Routes
    const val ROUTE_CALIBRATION = "calibration"
    const val ROUTE_CALIBRATION_RESULT = "calibration_result"
    const val ROUTE_TOUCHPAD = "touchpad"
    const val ROUTE_GESTURE_STUDIO = "gesture_studio"
    const val ROUTE_VOICE_COMMANDS = "voice_commands"
    const val ROUTE_PROXIMITY = "proximity"
    const val ROUTE_PROFILES = "profiles"
    const val ROUTE_THEMES = "themes"
    const val ROUTE_ACCESSIBILITY = "accessibility"
    const val ROUTE_SERVER_LOGS = "server_logs"
    const val ROUTE_NETWORK_DISCOVERY = "network_discovery"
    const val ROUTE_BATTERY = "battery"
    const val ROUTE_ABOUT = "about"
    const val ROUTE_ONBOARDING = "onboarding"
    const val ROUTE_SENSOR_VISUALIZER = "sensor_visualizer"
    const val ROUTE_EDGE_GESTURES = "edge_gestures"
    const val ROUTE_TOUCHPAD_SETTINGS = "touchpad_settings"
    const val ROUTE_FILE_TRANSFER = "file_transfer"
}
```

### Helper Functions

```kotlin
companion object {
    // Bottom Navigation
    private val bottomNavRoutes: Set<String> = setOf(
        ROUTE_HOME, ROUTE_STATISTICS, ROUTE_SETTINGS, ROUTE_HELP
    )
    
    val bottomNavDestinations: List<Destinations>
        get() = listOf(Home, Statistics, Settings, Help)
    
    fun isBottomNavScreen(route: String?): Boolean =
        route != null && bottomNavRoutes.contains(route)
    
    fun fromRoute(route: String): Destinations? = when (route) {
        ROUTE_HOME -> Home
        ROUTE_STATISTICS -> Statistics
        // ... etc
        else -> null
    }
    
    fun shouldShowBackButton(route: String?): Boolean =
        route != null && route in listOf(
            ROUTE_ABOUT,
            ROUTE_CALIBRATION,
            ROUTE_CALIBRATION_RESULT,
            ROUTE_SENSOR_VISUALIZER,
            // ... etc
        )
    
    fun createRoute(baseRoute: String, vararg args: Pair<String, String>): String =
        if (args.isEmpty()) baseRoute
        else "$baseRoute?${args.joinToString("&") { "${it.first}=${it.second}" }}"
}
```

---

## 🚀 2. NavigationActions.kt

### Purpose
Provides an **abstraction layer** for navigation, decoupling the UI from the `NavController`. This makes the UI more testable and easier to maintain.

### Key Features

| Feature | Description |
|---------|-------------|
| **Abstraction** | UI code doesn't depend directly on `NavController` |
| **Type-Safe** | All navigation actions are explicit methods |
| **Testability** | Easy to mock for unit tests |
| **Consistency** | Centralized navigation logic |
| **Argument Support** | Methods for routes with arguments (e.g., `navigateToCalibrationResult`) |

### Interface Definition

```kotlin
interface NavigationActions {
    // Basic navigation
    fun navigateTo(route: String)
    fun navigateBack()
    
    // Bottom navigation
    fun navigateToHome()
    fun navigateToSettings()
    fun navigateToStatistics()
    fun navigateToHelp()
    
    // Feature navigation
    fun navigateToCalibration()
    fun navigateToCalibrationResult(quality: String)
    fun navigateToTouchpad()
    fun navigateToGestureStudio()
    fun navigateToVoiceCommands()
    fun navigateToProximity()
    fun navigateToProfiles()
    fun navigateToThemes()
    fun navigateToAccessibility()
    fun navigateToSensorVisualizer()
    fun navigateToServerLogs()
    fun navigateToNetworkDiscovery()
    fun navigateToBattery()
    fun navigateToAbout()
    fun navigateToOnboarding()
    fun navigateToEdgeGestures()
    fun navigateToTouchpadSettings()
}
```

### Implementation

```kotlin
class NavigationActionsImpl(
    private val navController: NavController
) : NavigationActions {

    override fun navigateTo(route: String) {
        navController.navigate(route)
    }

    override fun navigateBack() {
        navController.popBackStack()
    }

    override fun navigateToHome() {
        navController.navigate(Destinations.Home.route) {
            popUpTo(Destinations.Home.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    override fun navigateToCalibrationResult(quality: String) {
        navController.navigate("${Destinations.CalibrationResult.route}?quality=$quality") {
            popUpTo(Destinations.Calibration.route) { inclusive = true }
        }
    }

    // ... all other methods implemented
}
```

---

## 🗺️ 3. AirMouseNavHost.kt

### Purpose
Defines the **navigation graph** for the entire application. Maps each route to its corresponding composable screen.

### Key Features

| Feature | Description |
|---------|-------------|
| **All Routes** | Every screen is defined in one place |
| **Composable Mapping** | Routes are mapped to Compose functions |
| **Argument Support** | Routes with arguments (e.g., calibration result) |
| **Navigation Actions** | `NavigationActionsImpl` is passed to each screen |
| **Start Destination** | Configurable starting route |

### NavHost Definition

```kotlin
@Composable
fun AirMouseNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Home.route,
    modifier: Modifier = Modifier
) {
    val navigationActions = NavigationActionsImpl(navController)
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Bottom navigation screens
        composable(Destinations.Home.route) {
            HomeScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Statistics.route) {
            StatisticsScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Settings.route) {
            SettingsScreen(
                navigationActions = navigationActions,
                onBack = { navigationActions.navigateBack() }
            )
        }
        composable(Destinations.Help.route) {
            HelpScreen(navigationActions = navigationActions)
        }
        
        // Info screens
        composable(Destinations.About.route) {
            AboutScreen(navigationActions = navigationActions)
        }
        
        // Calibration
        composable(Destinations.Calibration.route) {
            CalibrationScreen(navigationActions = navigationActions)
        }
        composable(
            route = "${Destinations.ROUTE_CALIBRATION_RESULT}?quality={quality}",
            arguments = listOf(
                navArgument("quality") {
                    type = NavType.StringType
                    defaultValue = "UNKNOWN"
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val quality = backStackEntry.arguments?.getString("quality") ?: "UNKNOWN"
            CalibrationResultScreen(
                navigationActions = navigationActions,
                quality = quality
            )
        }
        
        // Sensor visualizer
        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen(navigationActions = navigationActions)
        }
        
        // Gestures and touch
        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen(navigationActions = navigationActions)
        }
        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Touchpad.route) {
            TouchpadScreen(navigationActions = navigationActions)
        }
        
        // Network and logs
        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(navigationActions = navigationActions)
        }
        composable(Destinations.ServerLogs.route) {
            ServerLogsScreen(navigationActions = navigationActions)
        }
        
        // Proximity and voice
        composable(Destinations.Proximity.route) {
            ProximityScreen(navigationActions = navigationActions)
        }
        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen(navigationActions = navigationActions)
        }
        
        // Profiles, themes
        composable(Destinations.Profiles.route) {
            ProfilesScreen(
                navigationActions = navigationActions,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }
        composable(Destinations.Themes.route) {
            ThemesScreen(navigationActions = navigationActions)
        }
        
        // Battery and accessibility
        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions = navigationActions)
        }
        
        // File transfer
        composable(Destinations.FileTransfer.route) {
            FileTransferScreen(navigationActions = navigationActions)
        }
        
        // Onboarding
        composable(Destinations.Onboarding.route) {
            OnboardingScreen(navigationActions = navigationActions)
        }
    }
}
```

---

## 📱 4. AirMouseBottomBar.kt

### Purpose
Renders the **bottom navigation bar** for the main tabs of the application.

### Key Features

| Feature | Description |
|---------|-------------|
| **4 Tabs** | Home, Statistics, Settings, Help |
| **Icon + Label** | Material Design icons with labels |
| **Active Highlight** | Selected tab is highlighted |
| **Themed** | Adapts to current theme colors |
| **Rounded Corners** | Top corners are rounded (24dp) |

### Implementation

```kotlin
@Composable
fun AirMouseBottomBar(
    currentRoute: String?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = Destinations.bottomNavDestinations
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }
    val colorScheme = MaterialTheme.colorScheme

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        containerColor = colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, destination ->
            val selected = selectedIndex == index

            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorScheme.primary,
                    selectedTextColor = colorScheme.primary,
                    unselectedIconColor = colorScheme.onSurfaceVariant,
                    unselectedTextColor = colorScheme.onSurfaceVariant,
                    indicatorColor = colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}
```

---

## 📊 Navigation Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         NAVIGATION FLOW                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  User Action → NavigationActions → NavController → Screen Change      │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    BOTTOM NAVIGATION                             │   │
│  │                                                                  │   │
│  │  Home ←──→ Statistics ←──→ Settings ←──→ Help                  │   │
│  │    │            │              │            │                    │   │
│  │    └────────────┴──────────────┴────────────┘                    │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    FEATURE NAVIGATION                            │   │
│  │                                                                  │   │
│  │  Calibration → CalibrationResult                               │   │
│  │  Touchpad → TouchpadSettings                                   │   │
│  │  Network Discovery → Server Logs                               │   │
│  │  Profiles → Profile Settings                                   │   │
│  │  Themes → Theme Settings                                       │   │
│  │  Accessibility → Access Settings                               │   │
│  │  Voice Commands → Voice Settings                               │   │
│  │  Proximity → Proximity Settings                                │   │
│  │  Gesture Studio → Edge Gestures                                │   │
│  │  Sensor Visualizer → Battery Monitor                           │   │
│  │  File Transfer → (external)                                   │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Usage Examples

### In a Screen Composable

```kotlin
@Composable
fun SomeScreen(navigationActions: NavigationActions) {
    Button(
        onClick = { navigationActions.navigateToSettings() }
    ) {
        Text("Go to Settings")
    }
    
    Button(
        onClick = { navigationActions.navigateBack() }
    ) {
        Text("Back")
    }
}
```

### In a ViewModel

```kotlin
class SomeViewModel @Inject constructor(
    // NavigationActions is not injected into ViewModels
    // Use Effects pattern instead
) : ViewModel() {
    private val _effect = MutableSharedFlow<NavigationEffect>()
    val effect: SharedFlow<NavigationEffect> = _effect.asSharedFlow()
    
    fun navigateToSettings() {
        viewModelScope.launch {
            _effect.emit(NavigationEffect.NavigateTo(Destinations.Settings.route))
        }
    }
}
```

### In MainScreen

```kotlin
@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val navigationActions = NavigationActionsImpl(navController)
    
    Scaffold(
        bottomBar = {
            if (Destinations.isBottomNavScreen(currentRoute)) {
                AirMouseBottomBar(
                    currentRoute = currentRoute,
                    onItemSelected = { destination ->
                        navigationActions.navigateTo(destination.route)
                    }
                )
            }
        }
    ) { paddingValues ->
        AirMouseNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

---

## 📋 Navigation Summary

| Component | Purpose | Key Methods |
|-----------|---------|-------------|
| **Destinations** | Route definitions | `isBottomNavScreen()`, `fromRoute()`, `shouldShowBackButton()` |
| **NavigationActions** | Navigation abstraction | `navigateTo()`, `navigateBack()`, `navigateToCalibration()` |
| **AirMouseNavHost** | NavGraph | All `composable()` routes |
| **AirMouseBottomBar** | Bottom navigation | `onItemSelected` callback |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Type-Safety** | All routes are defined as constants |
| **Abstraction** | UI doesn't depend on `NavController` directly |
| **Testability** | NavigationActions can be mocked |
| **Single Source of Truth** | All routes are in `Destinations.kt` |
| **Consistency** | All screens use the same navigation pattern |
| **Argument Support** | Routes with arguments (e.g., calibration result) |

---

**The Navigation Package provides a complete, type-safe, and maintainable navigation system for the Air Mouse application, handling all routing, bottom bar management, and screen transitions.**