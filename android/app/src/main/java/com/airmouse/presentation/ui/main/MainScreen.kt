package com.airmouse.presentation.ui.main
import com.airmouse.presentation.ui.files.FileTransferScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.airmouse.network.ConnectionManager
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.navigation.NavigationActionsImpl
import com.airmouse.presentation.ui.about.AboutScreen
import com.airmouse.presentation.ui.accessibility.AccessibilityScreen
import com.airmouse.presentation.ui.battery.BatteryScreen
import com.airmouse.presentation.ui.calibration.CalibrationResultScreen
import com.airmouse.presentation.ui.calibration.CalibrationScreen
import com.airmouse.presentation.ui.edge.EdgeGesturesScreen
import com.airmouse.presentation.ui.gesture.GestureStudioScreen
import com.airmouse.presentation.ui.help.HelpScreen
import com.airmouse.presentation.ui.home.HomeScreen
import com.airmouse.presentation.ui.logs.ServerLogsScreen
import com.airmouse.presentation.ui.network.NetworkDiscoveryScreen
import com.airmouse.presentation.ui.onboarding.OnboardingScreen
import com.airmouse.presentation.ui.profiles.ProfilesScreen
import com.airmouse.presentation.ui.proximity.ProximityScreen
import com.airmouse.presentation.ui.sensor.SensorVisualizerScreen
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.presentation.ui.statistics.StatisticsScreen
import com.airmouse.presentation.ui.themes.ThemesScreen
import com.airmouse.presentation.ui.touchpad.TouchpadScreen
import com.airmouse.presentation.ui.voice.VoiceCommandsScreen
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// ============================================================
// MainViewModel
// ============================================================

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    @Suppress("UNUSED")
    private val _connectionQuality = MutableStateFlow<ConnectionQuality?>(null)
    val connectionQuality: StateFlow<ConnectionQuality?> = _connectionQuality.asStateFlow()

    private val _serverInfo = MutableStateFlow(Pair("", ""))
    val serverInfo: StateFlow<Pair<String, String>> = _serverInfo.asStateFlow()

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

    @Suppress("unused")
    fun showArmCalibrationDialog() {
        _uiState.update { it.copy(error = "Arm calibration coming soon") }
    }

    @Suppress("unused")
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// ============================================================
// Bottom Navigation Data
// ============================================================

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

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

// ============================================================
// Drawer Data
// ============================================================

data class DrawerItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val subtitle: String,
    val enabled: Boolean = true,
    val badge: String? = null
)

// ============================================================
// Main Screen
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Home.route,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val selectedIndex = getSelectedBottomNavIndex(currentRoute)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isConnected by viewModel.isConnected.collectAsState()
    val serverInfo by viewModel.serverInfo.collectAsState()

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
                isConnecting = uiState.isLoading,
                isRegistered = uiState.isRegistered,
                isCalibrated = uiState.isCalibrated,
                userName = uiState.userName,
                serverName = serverInfo.first,
                serverVersion = serverInfo.second,
                onDisconnect = { viewModel.disconnect() }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Air Mouse Pro", fontWeight = FontWeight.Bold)
                            Text("Telegram‑style control center", fontSize = 11.sp, color = Color(0xFF96A0AE))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        AssistChip(
                            onClick = { scope.launch { drawerState.open() } },
                            label = { Text(if (isConnected) "Connected" else "Menu") }
                        )
                    }
                )
            },
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

// ============================================================
// Bottom Bar
// ============================================================

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

// ============================================================
// Drawer Content
// ============================================================

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Computer,
                        contentDescription = null,
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "Air Mouse Pro",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (userName.isBlank()) "Waiting for approval" else "Hello, $userName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE5E7EB)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Version 3.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF96A0AE)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusChip(
                        label = if (isConnected) "Connected" else "Waiting",
                        color = if (isConnected) Color(0xFF22C55E) else Color(0xFFF59E0B)
                    )
                    StatusChip(
                        label = if (isConnecting) "Approval" else "Approved",
                        color = if (isConnecting) Color(0xFFF59E0B) else Color(0xFF3B82F6)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                DrawerIdentityCard(
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    isRegistered = isRegistered,
                    isCalibrated = isCalibrated,
                    controlMode = controlMode,
                    userName = userName,
                    serverName = serverName,
                    serverVersion = serverVersion
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Quick access",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF96A0AE),
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                )
                if (isConnected) {
                    TextButton(
                        onClick = onDisconnect,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Disconnect", fontSize = 11.sp, color = Color(0xFFF43F5E))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2B3341))

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

        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF2B3341))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

// ============================================================
// Drawer Helper Composables
// ============================================================

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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF96A0AE)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
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
                    Text(
                        item.subtitle,
                        color = Color(0xFF96A0AE),
                        fontSize = 10.sp
                    )
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

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DrawerIdentityCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    isRegistered: Boolean,
    isCalibrated: Boolean,
    controlMode: String,
    userName: String,
    serverName: String,
    serverVersion: String
) {
    val title = when {
        isConnected -> "Approved and connected"
        isConnecting -> "Waiting for approval"
        else -> "Approval needed"
    }
    val subtitle = when {
        !isRegistered -> "Register first, then pair with the desktop."
        !isCalibrated -> "Calibration is required before touchpad or motion control."
        isConnected -> "This phone is stored as a known device on the server."
        else -> "The desktop is waiting to accept this connection."
    }
    val badgeColor = when {
        isConnected && isCalibrated -> Color(0xFF22C55E)
        isConnecting -> Color(0xFFF59E0B)
        isRegistered -> Color(0xFF3B82F6)
        else -> Color(0xFF64748B)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF2B3341))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(label = title, color = badgeColor)
                StatusChip(
                    label = controlMode.replace('_', ' ').lowercase(Locale.ROOT)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    color = Color(0xFF38BDF8)
                )
            }
            Text(
                userName.ifBlank { "Unknown user" },
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                serverName.ifBlank { "No server discovered yet" },
                color = Color(0xFFE5E7EB),
                fontSize = 11.sp
            )
            if (serverVersion.isNotBlank()) {
                Text(
                    "Server v$serverVersion",
                    color = Color(0xFF96A0AE),
                    fontSize = 10.sp
                )
            }
            Text(
                subtitle,
                color = Color(0xFF96A0AE),
                fontSize = 11.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(label = if (isRegistered) "Registered" else "Locked", color = if (isRegistered) Color(0xFF22C55E) else Color(0xFFF59E0B))
                StatusChip(label = if (isCalibrated) "Calibrated" else "Needs calibration", color = if (isCalibrated) Color(0xFF10B981) else Color(0xFFF97316))
                StatusChip(label = "Identity", color = Color(0xFF8B5CF6))
            }
        }
    }
}

// ============================================================
// Animated FAB
// ============================================================

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

// ============================================================
// Navigation Host
// ============================================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavHost(

    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Home.route,
    onOpenDrawer: (() -> Unit)? = null
) {
    val navigationActions = NavigationActionsImpl(navController)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { it }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
                    slideOutHorizontally(animationSpec = tween(300)) { -it }
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { -it }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                    slideOutHorizontally(animationSpec = tween(300)) { it }
        }
    ) {
        // Bottom navigation screens
        composable(Destinations.Home.route) {
            HomeScreen(
                navigationActions = navigationActions,
                onOpenDrawer = onOpenDrawer
            )
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
            CalibrationScreen(
                navigationActions = navigationActions,
                onComplete = { }
            )
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
        ) {
            CalibrationResultScreen(
                navigationActions = navigationActions,
                onContinue = { navigationActions.navigateToHome() },
                onRecalibrate = { navigationActions.navigateTo(Destinations.Calibration.route) }
            )
        }

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
        composable(Destinations.FileTransfer.route) {
            FileTransferScreen(navigationActions = navigationActions)
        }

        // Onboarding
        composable(Destinations.Onboarding.route) {
            OnboardingScreen(navigationActions = navigationActions)
        }
    }
}

// ============================================================
// Helpers
// ============================================================

private fun shouldShowBottomBar(route: String?): Boolean {
    return when (route) {
        Destinations.Home.route,
        Destinations.Statistics.route,
        Destinations.Settings.route,
        Destinations.Help.route -> true
        else -> false
    }
}
