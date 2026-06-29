# 📘 Air Mouse Main Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.main` package contains the **root navigation container** for the Air Mouse application. This screen provides the main scaffold with top bar, bottom navigation, drawer, and floating action button, orchestrating the entire navigation experience.

```
com.airmouse.presentation.ui.main/
├── MainScreen.kt              # Root navigation container
├── MainViewModel.kt           # Main ViewModel
├── MainUiState.kt             # Main state models
├── MainComponents.kt          # Reusable main UI components
└── MainNavHost.kt             # Navigation host (shared with presentation.navigation)
```

---

## 🎯 1. MainScreen

### Purpose
The **root navigation container** for the entire application. Provides the scaffold with:
- **Top App Bar** – Title, navigation icon, status chip
- **Navigation Drawer** – Side menu with all destinations
- **Bottom Navigation Bar** – 4 main tabs (Home, Statistics, Settings, Help)
- **Floating Action Button** – Contextual FAB that changes based on current route
- **Navigation Host** – The actual navigation graph

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Home.route,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val selectedIndex = getSelectedBottomNavIndex(currentRoute)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isConnected by viewModel.isConnected.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModernDrawerContent(
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                currentRoute = currentRoute,
                controlMode = uiState.controlMode,
                onModeSelected = { mode ->
                    viewModel.updateControlMode(mode)
                    scope.launch { drawerState.close() }
                },
                isConnected = isConnected,
                userName = uiState.userName,
                // ... other parameters
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { MainTopBar(...) },
            bottomBar = {
                if (shouldShowBottomBar(currentRoute)) {
                    ModernBottomBar(
                        selectedIndex = selectedIndex,
                        onItemSelected = { index ->
                            val route = bottomNavItems[index].route
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (currentRoute == Destinations.Home.route) {
                    when (uiState.controlMode) {
                        "touchpad" -> {
                            AnimatedFAB(
                                onClick = { navController.navigate(Destinations.Touchpad.route) },
                                icon = Icons.Default.TouchApp,
                                text = "Touchpad",
                                gradient = listOf(Color(0xFF00BCD4), Color(0xFF4CAF50))
                            )
                        }
                        "arm_movement" -> {
                            AnimatedFAB(
                                onClick = { viewModel.showArmCalibrationDialog() },
                                icon = Icons.Default.Accessibility,
                                text = "Arm Mode",
                                gradient = listOf(Color(0xFF9C27B0), Color(0xFFE91E63)),
                                isActive = true
                            )
                        }
                        else -> {
                            AnimatedFAB(
                                onClick = { navController.navigate(Destinations.Calibration.route) },
                                icon = Icons.Default.Build,
                                text = "Calibrate",
                                gradient = listOf(Color(0xFFFF5722), Color(0xFFFF9800))
                            )
                        }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            containerColor = Color(0xFF0F1115)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1D24), Color(0xFF0F1115))
                        )
                    )
            ) {
                MainNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize(),
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}
```

---

## 🎯 2. Bottom Navigation

### ModernBottomBar

```kotlin
@Composable
fun ModernBottomBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF1A1D24),
        tonalElevation = 0.dp,
        modifier = Modifier
            .shadow(8.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        bottomNavItems.forEachIndexed { index, destination ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        destination.title,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00BCD4),
                    selectedTextColor = Color(0xFF00BCD4),
                    unselectedIconColor = Color(0xFF96A0AE),
                    unselectedTextColor = Color(0xFF96A0AE),
                    indicatorColor = Color(0xFF00BCD4).copy(alpha = 0.2f)
                )
            )
        }
    }
}
```

### Bottom Navigation Items

```kotlin
val bottomNavItems = listOf(
    BottomNavItem(Destinations.Home.route, "Home", Icons.Default.Home),
    BottomNavItem(Destinations.Statistics.route, "Statistics", Icons.Default.Assessment),
    BottomNavItem(Destinations.Settings.route, "Settings", Icons.Default.Settings),
    BottomNavItem(Destinations.Help.route, "Help", Icons.AutoMirrored.Filled.Help)
)

fun getSelectedBottomNavIndex(currentRoute: String?): Int {
    val index = bottomNavItems.indexOfFirst { it.route == currentRoute }
    return if (index != -1) index else 0
}

private fun shouldShowBottomBar(route: String?): Boolean {
    return when (route) {
        Destinations.Home.route,
        Destinations.Statistics.route,
        Destinations.Settings.route,
        Destinations.Help.route -> true
        else -> false
    }
}
```

---

## 🎯 3. Navigation Drawer

### ModernDrawerContent

```kotlin
@Composable
fun ModernDrawerContent(
    onItemClick: (String) -> Unit,
    currentRoute: String?,
    controlMode: String,
    onModeSelected: (String) -> Unit,
    isConnected: Boolean,
    isConnecting: Boolean,
    isRegistered: Boolean,
    isCalibrated: Boolean,
    userName: String,
    serverName: String,
    serverVersion: String,
    onDisconnect: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF1A1D24),
        drawerContentColor = Color(0xFFE5E7EB),
        modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 420.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00BCD4).copy(alpha = 0.2f),
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(32.dp))
                    Text("Air Mouse Pro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(if (userName.isBlank()) "Waiting for approval" else "Hello, $userName", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Version 3.0.0", style = MaterialTheme.typography.bodySmall, color = Color(0xFF96A0AE))
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(label = if (isConnected) "Connected" else "Waiting", color = if (isConnected) Color(0xFF22C55E) else Color(0xFFF59E0B))
                    StatusChip(label = if (isConnecting) "Approval" else "Approved", color = if (isConnecting) Color(0xFFF59E0B) else Color(0xFF3B82F6))
                }
                Spacer(modifier = Modifier.height(8.dp))
                DrawerIdentityCard(...)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Quick access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF96A0AE))
                if (isConnected) {
                    TextButton(onClick = onDisconnect) {
                        Text("Disconnect", fontSize = 11.sp, color = Color(0xFFF43F5E))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2B3341))

        // Sections
        DrawerSection(
            title = "Quick links",
            subtitle = "Most used actions",
            items = listOf(
                DrawerItem(Destinations.Home.route, "Home", Icons.Default.Home, "Landing"),
                DrawerItem(Destinations.Calibration.route, "Calibration", Icons.Default.Tune, "Setup"),
                DrawerItem(Destinations.Touchpad.route, "Touchpad", Icons.Default.TouchApp, "Pointer", isRegistered && isCalibrated),
                DrawerItem(Destinations.SensorVisualizer.route, "Sensor Visualizer", Icons.Default.Sensors, "Live data", isRegistered && isCalibrated),
                DrawerItem(Destinations.NetworkDiscovery.route, "Network Discovery", Icons.Default.Wifi, "LAN", badge = if (isConnecting) "WAIT" else "SCAN"),
            ),
            currentRoute = currentRoute,
            onItemClick = onItemClick
        )

        DrawerSection(
            title = "Core",
            subtitle = "Primary screens",
            items = listOf(
                DrawerItem(Destinations.Statistics.route, "Statistics", Icons.Default.Assessment, "Usage"),
                DrawerItem(Destinations.Settings.route, "Settings", Icons.Default.Settings, "Preferences"),
                DrawerItem(Destinations.Help.route, "Help", Icons.AutoMirrored.Filled.Help, "Guide"),
                DrawerItem(Destinations.About.route, "About", Icons.Default.Info, "App info")
            ),
            currentRoute = currentRoute,
            onItemClick = onItemClick
        )

        HorizontalDivider(color = Color(0xFF2B3341))

        DrawerSection(
            title = "Control",
            subtitle = "Interaction modules",
            items = listOf(
                DrawerItem(Destinations.GestureStudio.route, "Gesture Studio", Icons.Default.Gesture, "Training", isRegistered && isCalibrated),
                DrawerItem(Destinations.EdgeGestures.route, "Edge Gestures", Icons.Default.Swipe, "Edges", isRegistered && isCalibrated),
                DrawerItem(Destinations.VoiceCommands.route, "Voice Commands", Icons.Default.Mic, "Speech", isRegistered && isCalibrated),
                DrawerItem(Destinations.Proximity.route, "Proximity Lock", Icons.Default.NearMe, "Security", isRegistered && isCalibrated),
                DrawerItem(Destinations.TouchpadSettings.route, "Touchpad Settings", Icons.Default.Settings, "Fine tuning", isRegistered && isCalibrated)
            ),
            currentRoute = currentRoute,
            onItemClick = onItemClick
        )

        HorizontalDivider(color = Color(0xFF2B3341))

        // Control Mode Selector
        Text(
            text = "Control Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF96A0AE),
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )
        listOf(
            "motion" to "🎯 Motion Mode (Rotation)",
            "touchpad" to "🖐️ Touchpad Mode",
            "arm_movement" to "💪 Arm Movement Mode"
        ).forEach { (mode, label) ->
            val isSelected = controlMode == mode
            NavigationDrawerItem(
                label = { Text(label, color = if (isSelected) Color(0xFF00BCD4) else Color(0xFFE5E7EB)) },
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                icon = {
                    Icon(
                        if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF00BCD4) else Color(0xFF96A0AE)
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF00BCD4).copy(alpha = 0.1f)
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider(color = Color(0xFF2B3341))

        DrawerSection(
            title = "System",
            subtitle = "Network and diagnostics",
            items = listOf(
                DrawerItem(Destinations.ServerLogs.route, "Server Logs", Icons.AutoMirrored.Filled.List, "Debug", badge = if (isConnected) "LIVE" else "WAIT"),
                DrawerItem(Destinations.Battery.route, "Battery Monitor", Icons.Default.BatteryFull, "Power"),
                DrawerItem(Destinations.Accessibility.route, "Accessibility", Icons.Default.Accessibility, "System"),
                DrawerItem(Destinations.Profiles.route, "Profiles", Icons.Default.Person, "Users"),
                DrawerItem(Destinations.Themes.route, "Themes", Icons.Default.Palette, "Style")
            ),
            currentRoute = currentRoute,
            onItemClick = onItemClick
        )

        // Footer
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF2B3341))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Drawer shortcuts", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    "Use the menu to jump into calibration, live sensors, touchpad mode, and server diagnostics without leaving the control center.",
                    color = Color(0xFF96A0AE),
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatusChip("Calibrate", Color(0xFFF59E0B))
                    StatusChip("Sensors", Color(0xFF38BDF8))
                    StatusChip("Logs", Color(0xFF22C55E))
                }
            }
        }
    }
}
```

### DrawerSection

```kotlin
@Composable
fun DrawerSection(
    title: String,
    subtitle: String,
    items: List<DrawerItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF96A0AE))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B), fontSize = 10.sp)
            }
            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF0F172A)) {
                Text(
                    "${items.count { it.enabled }} ready",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Color(0xFF93C5FD),
                    fontSize = 10.sp
                )
            }
        }
    }
    items.forEach { item ->
        val isSelected = currentRoute == item.route
        NavigationDrawerItem(
            label = {
                Column {
                    Text(
                        item.title,
                        color = when {
                            !item.enabled -> Color(0xFF64748B)
                            isSelected -> Color(0xFF00BCD4)
                            else -> Color(0xFFE5E7EB)
                        }
                    )
                    Text(item.subtitle, color = Color(0xFF96A0AE), fontSize = 10.sp)
                }
            },
            selected = isSelected,
            onClick = { if (item.enabled) onItemClick(item.route) },
            icon = {
                Icon(
                    item.icon,
                    contentDescription = item.title,
                    tint = when {
                        !item.enabled -> Color(0xFF475569)
                        isSelected -> Color(0xFF00BCD4)
                        else -> Color(0xFF96A0AE)
                    },
                    modifier = Modifier.size(22.dp)
                )
            },
            badge = {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = when {
                        !item.enabled -> Color(0xFF334155)
                        isSelected -> Color(0xFF00BCD4).copy(alpha = 0.18f)
                        else -> Color(0xFF2B3341)
                    }
                ) {
                    Text(
                        item.badge ?: if (item.enabled) item.subtitle else "Approval needed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        color = when {
                            !item.enabled -> Color(0xFF94A3B8)
                            isSelected -> Color(0xFF00BCD4)
                            else -> Color(0xFF96A0AE)
                        }
                    )
                }
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFF00BCD4).copy(alpha = 0.12f),
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
```

---

## 🎯 4. Animated FAB

```kotlin
@Composable
fun AnimatedFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    gradient: List<Color>,
    isActive: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScaling"
    )

    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (isActive) 8.dp else 4.dp
        ),
        modifier = Modifier
            .graphicsLayer {
                if (isActive) {
                    scaleX = pulse
                    scaleY = pulse
                }
            }
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(gradient),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = text,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
```

---

## 🎯 5. MainViewModel

### Purpose
Manages **main screen state**, including control mode, connection status, and user registration.

### Key State Properties

| Property | Type | Description |
|----------|------|-------------|
| `uiState` | `StateFlow<MainUiState>` | Complete UI state |
| `isConnected` | `StateFlow<Boolean>` | Connection status |
| `connectionQuality` | `StateFlow<ConnectionQuality?>` | Connection quality |
| `serverInfo` | `StateFlow<Pair<String, String>>` | Server name and version |

### Key Methods

| Method | Purpose |
|--------|---------|
| `updateControlMode(mode: String)` | Update control mode |
| `disconnect()` | Disconnect from server |
| `reconnect()` | Reconnect to server |
| `showArmCalibrationDialog()` | Show arm calibration dialog |
| `clearError()` | Clear error state |

### Implementation

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionQuality = MutableStateFlow<ConnectionQuality?>(null)
    val connectionQuality: StateFlow<ConnectionQuality?> = _connectionQuality.asStateFlow()

    private val _serverInfo = MutableStateFlow(Pair("", ""))
    val serverInfo: StateFlow<Pair<String, String>> = _serverInfo.asStateFlow()

    init {
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                _isConnected.value = status == ConnectionManager.ConnectionStatus.CONNECTED
            }
        }

        viewModelScope.launch {
            connectionManager.connectionQuality.collect { quality ->
                _connectionQuality.value = ConnectionQuality(
                    ping = quality.ping,
                    signalStrength = quality.level(),
                    isStable = quality.isHealthy()
                )
            }
        }

        viewModelScope.launch {
            connectionManager.serverName.collect { name ->
                _serverInfo.value = Pair(name, _serverInfo.value.second)
            }
        }

        viewModelScope.launch {
            connectionManager.serverVersion.collect { version ->
                _serverInfo.value = Pair(_serverInfo.value.first, version)
            }
        }

        _uiState.update {
            it.copy(
                controlMode = prefs.getString("control_mode", "motion"),
                isRegistered = !prefs.isFirstLaunch() && prefs.getUserName().isNotBlank(),
                isCalibrated = prefs.getBoolean("calibration_complete", false) ||
                        prefs.getBoolean("is_calibrated", false),
                userName = prefs.getUserName()
            )
        }
    }

    fun updateControlMode(mode: String) {
        prefs.putString("control_mode", mode)
        _uiState.update { it.copy(controlMode = mode) }
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionManager.disconnect()
        }
    }

    fun reconnect() {
        viewModelScope.launch {
            connectionManager.reconnect()
        }
    }

    fun showArmCalibrationDialog() {
        _uiState.update { it.copy(error = "Arm calibration coming soon") }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    data class ConnectionQuality(
        val ping: Int,
        val signalStrength: Int,
        val isStable: Boolean
    )

    data class MainUiState(
        val controlMode: String = "motion",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isRegistered: Boolean = false,
        val isCalibrated: Boolean = false,
        val userName: String = ""
    )
}
```

---

## 📊 Main Screen Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       MAIN SCREEN FLOW                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. MAIN SCREEN LOADS                                                  │
│     ├── ModalNavigationDrawer (drawer)                                │
│     ├── Scaffold                                                      │
│     │   ├── TopAppBar (title, menu, status)                          │
│     │   ├── BottomBar (4 tabs)                                       │
│     │   └── FAB (contextual)                                         │
│     └── MainNavHost (navigation graph)                               │
│                                                                         │
│  2. USER INTERACTS                                                    │
│     ├── Click hamburger → Open drawer                                │
│     ├── Click bottom nav → Navigate to tab                           │
│     ├── Click FAB → Contextual action                                │
│     └── Click drawer item → Navigate to screen                      │
│                                                                         │
│  3. DRAWER INTERACTIONS                                               │
│     ├── Quick links → Navigation                                     │
│     ├── Control mode → Update mode                                   │
│     ├── System → Navigation                                          │
│     └── Disconnect → Disconnect from server                         │
│                                                                         │
│  4. STATE MANAGEMENT                                                  │
│     ├── Observe connection status                                    │
│     ├── Observe connection quality                                   │
│     ├── Observe server info                                          │
│     └── Update UI accordingly                                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Drawer Items

| Section | Items | Badges |
|---------|-------|--------|
| **Quick links** | Home, Calibration, Touchpad, Sensor Visualizer, Network Discovery | WAIT/SCAN |
| **Core** | Statistics, Settings, Help, About | - |
| **Control** | Gesture Studio, Edge Gestures, Voice Commands, Proximity Lock, Touchpad Settings | - |
| **System** | Server Logs, Battery Monitor, Accessibility, Profiles, Themes | LIVE/WAIT |

---

## 🎨 Theming

The Main Screen uses a **dark theme** with a gradient background:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF1A1D24), Color(0xFF0F1115))
            )
        )
)
```

**Colors:**
- Background: `#0F1115` (dark) / `#1A1D24` (surface)
- Primary: `#00BCD4` (cyan)
- Surface: `#1A1D24`
- On Surface: `#E5E7EB`
- Surface Variant: `#2B3341`

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Single Source of Truth** | StateFlow for all state |
| **Reactive UI** | Automatic recomposition |
| **Modular Components** | Separate composables for drawer, bottom bar, FAB |
| **Navigation Consistency** | Bottom bar + drawer + nav host |
| **Contextual FAB** | FAB changes based on current route |
| **Themed** | Consistent dark theme |
| **Accessibility** | Semantic content, labels |
| **Performance** | Lazy loading, efficient recomposition |

---

**The Main Screen provides the root navigation container for the Air Mouse application, orchestrating the entire navigation experience with a modern, consistent, and accessible UI.**