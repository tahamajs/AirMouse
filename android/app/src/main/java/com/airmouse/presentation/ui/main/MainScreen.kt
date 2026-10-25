package com.airmouse.presentation.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.airmouse.presentation.ui.files.FileTransferScreen
import com.airmouse.presentation.ui.gesture.GestureStudioScreen
import com.airmouse.presentation.ui.gaming.GamingModeScreen
import com.airmouse.presentation.ui.mirroring.ScreenMirroringScreen
import com.airmouse.presentation.ui.help.HelpScreen
import com.airmouse.presentation.ui.home.HomeScreen
import com.airmouse.presentation.ui.logs.ServerLogsScreen
import com.airmouse.presentation.ui.network.NetworkDiscoveryScreen
import com.airmouse.presentation.ui.onboarding.OnboardingScreen
import com.airmouse.presentation.ui.search.SearchScreen
import com.airmouse.presentation.ui.splash.SplashScreen
import com.airmouse.presentation.ui.profiles.ProfilesScreen
import com.airmouse.presentation.ui.proximity.ProximityScreen
import com.airmouse.presentation.ui.notifications.NotificationsCenterScreen
import com.airmouse.presentation.ui.sensor.SensorVisualizerScreen
import com.airmouse.presentation.ui.sync.SyncStatusScreen
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.presentation.ui.components.GlassmorphicCard
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

// ============================================================
// MainViewModel
// ============================================================

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    val connectionManager: ConnectionManager
) : ViewModel() {

    val currentIp: StateFlow<String> = connectionManager.currentIp
    val connectionQuality: StateFlow<com.airmouse.network.ConnectionQuality> = connectionManager.connectionQuality
    val isMouseControlEnabled: StateFlow<Boolean> = connectionManager.isMouseControlEnabledFlow
    val currentProtocol: StateFlow<ConnectionManager.ConnectionProtocol> = connectionManager.currentProtocolFlow

    fun setMouseControlEnabled(enabled: Boolean) {
        connectionManager.isMouseControlEnabled = enabled
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _serverInfo = MutableStateFlow(Pair("", ""))
    val serverInfo: StateFlow<Pair<String, String>> = _serverInfo.asStateFlow()

    data class MainUiState(
        val controlMode: String = "motion",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isRegistered: Boolean = false,
        val isCalibrated: Boolean = false,
        val userName: String = "",
        val activeProfile: String = "General",
        val hapticEnabled: Boolean = true
    )

    init {
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                _isConnected.value = status == ConnectionManager.ConnectionStatus.CONNECTED
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

        val initialProfile = if (prefs.getLastUsedProfile().isBlank() || prefs.getLastUsedProfile() == "Default") "General" else prefs.getLastUsedProfile()
        val initialHaptic = prefs.isHapticEnabled()

        _uiState.update {
            it.copy(
                controlMode = prefs.getActiveMode().id,
                isRegistered = !prefs.isFirstLaunch() && prefs.getUserName().isNotBlank(),
                isCalibrated = prefs.getBoolean("calibration_complete", false) ||
                        prefs.getBoolean("is_calibrated", false),
                userName = prefs.getUserName(),
                activeProfile = initialProfile,
                hapticEnabled = initialHaptic
            )
        }
    }

    fun updateControlMode(mode: String) {
        prefs.setActiveModeId(mode)
        val legacyMode = when (mode) {
            "motion_translation" -> "accel"
            "touchpad_mode" -> "touchpad"
            else -> "motion"
        }
        prefs.setControlMode(legacyMode)
        _uiState.update { it.copy(controlMode = mode) }
    }

    fun updateActiveProfile(profile: String) {
        prefs.setLastUsedProfile(profile)
        when (profile) {
            "General" -> {
                prefs.setSensitivity(0.5f)
                prefs.setClickThreshold(8f)
                prefs.setScrollThreshold(8f)
            }
            "Presentation" -> {
                prefs.setSensitivity(0.8f)
                prefs.setClickThreshold(5f)
                prefs.setScrollThreshold(12f)
            }
            "Gaming" -> {
                prefs.setSensitivity(1.5f)
                prefs.setClickThreshold(3f)
                prefs.setScrollThreshold(4f)
            }
        }
        _uiState.update { it.copy(activeProfile = profile) }
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        prefs.setHapticEnabled(enabled)
        _uiState.update { it.copy(hapticEnabled = enabled) }
    }

    fun resetConnection() {
        prefs.remove("last_ip")
        prefs.remove("last_port")
        prefs.remove("server_mac")
        prefs.remove("auth_token")
        
        prefs.setAutoConnect(true)
        prefs.setWebSocketEnabled(true)
        prefs.setUdpDiscoveryEnabled(true)
        prefs.setLastProtocol("UDP")
        prefs.setLastPort(8082)
        
        disconnect()
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
    viewModel: MainViewModel = hiltViewModel(),
    pendingRoute: String? = null,
    onRouteHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val selectedIndex = getSelectedBottomNavIndex(currentRoute)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isConnected by viewModel.isConnected.collectAsState()
    val isMouseControlEnabled by viewModel.isMouseControlEnabled.collectAsStateWithLifecycle(true)
    val serverInfo by viewModel.serverInfo.collectAsState()
    val currentIp by viewModel.currentIp.collectAsStateWithLifecycle("")
    val currentProtocol by viewModel.currentProtocol.collectAsStateWithLifecycle()

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onRouteHandled()
        }
    }

    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = drawerState.isOpen || currentRoute != Destinations.Home.route || currentRoute == Destinations.Home.route) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (currentRoute != Destinations.Home.route) {
            navController.navigate(Destinations.Home.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                backPressedTime = currentTime
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                serverIp = currentIp,
                connectionQualityFlow = viewModel.connectionQuality,
                protocol = currentProtocol,
                onDisconnect = { viewModel.disconnect() }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            /* topBar removed: each child screen provides its own TopAppBar */
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
                                onClick = { /* Show arm calibration dialog */ },
                                icon = Icons.Default.Accessibility,
                                text = "Arm Mode",
                                gradient = listOf(Color(0xFF9C27B0), Color(0xFFE91E63)),
                                isActive = true
                            )
                        }
                        else -> {
                            // Calibration action is accessible directly via the Modules grid and Quick Actions
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

                if (isConnected && currentRoute == Destinations.Home.route) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Connection Status Banner Pill with RTT and Packet Loss
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
                            shadowElevation = 6.dp
                        ) {
                            ConnectionQualityPill(
                                connectionQualityFlow = viewModel.connectionQuality,
                                currentProtocol = currentProtocol
                            )
                        }

                        // Mouse Control Toggle Pill
                        Surface(
                            onClick = { viewModel.setMouseControlEnabled(!isMouseControlEnabled) },
                            shape = RoundedCornerShape(99.dp),
                            color = if (isMouseControlEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isMouseControlEnabled) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isMouseControlEnabled) Color(0xFF10B981) else Color.Gray, CircleShape)
                                )
                                Text(
                                    text = if (isMouseControlEnabled) "Disable Mouse" else "Enable Mouse",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMouseControlEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = if (isMouseControlEnabled) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                    contentDescription = null,
                                    tint = if (isMouseControlEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isConnected && isMouseControlEnabled && uiState.controlMode != "touchpad_mode" && uiState.controlMode != "touchpad" && currentRoute != Destinations.Touchpad.route) {
            BlackoutTouchpadOverlay(
                onDisableMouse = { viewModel.setMouseControlEnabled(false) },
                connectionManager = viewModel.connectionManager
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            modifier = Modifier
                .height(80.dp)
                .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            bottomNavItems.forEachIndexed { index, destination ->
                val isSelected = selectedIndex == index
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onItemSelected(index) },
                    icon = {
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.2f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "scale"
                        )
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(scale)
                        )
                    },
                    label = {
                        Text(
                            destination.title,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

// ============================================================
// Drawer Content
// ============================================================

private class RedesignThemeColors(
    val Primary: Color,
    val PrimaryLight: Color,
    val PrimaryDark: Color,
    val Secondary: Color,
    val SecondaryLight: Color,
    val SecondaryDark: Color,
    val AccentOrange: Color,
    val AccentRed: Color,
    val AccentBlue: Color,
    val AccentPurple: Color,
    val StatusSuccess: Color,
    val StatusWarning: Color,
    val StatusError: Color,
    val StatusInfo: Color,
    val Surface: Color,
    val SurfaceVariant: Color,
    val SurfaceHighlight: Color,
    val Background: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextTertiary: Color
)

@Composable
private fun rememberRedesignThemeColors(): RedesignThemeColors {
    val colorScheme = MaterialTheme.colorScheme
    return remember(colorScheme) {
        RedesignThemeColors(
            Primary = colorScheme.primary,
            PrimaryLight = colorScheme.primaryContainer,
            PrimaryDark = colorScheme.primary,
            Secondary = colorScheme.secondary,
            SecondaryLight = colorScheme.secondary.copy(alpha = 0.8f),
            SecondaryDark = colorScheme.secondary.copy(alpha = 0.6f),
            AccentOrange = colorScheme.secondary,
            AccentRed = colorScheme.error,
            AccentBlue = colorScheme.tertiary,
            AccentPurple = colorScheme.primary,
            StatusSuccess = Color(0xFF10B981),
            StatusWarning = Color(0xFFF59E0B),
            StatusError = colorScheme.error,
            StatusInfo = colorScheme.primary,
            Surface = colorScheme.surface,
            SurfaceVariant = colorScheme.surfaceVariant,
            SurfaceHighlight = colorScheme.surfaceVariant.copy(alpha = 0.8f),
            Background = colorScheme.background,
            TextPrimary = colorScheme.onBackground,
            TextSecondary = colorScheme.onSurfaceVariant,
            TextTertiary = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    serverIp: String,
    connectionQualityFlow: StateFlow<com.airmouse.network.ConnectionQuality>,
    protocol: ConnectionManager.ConnectionProtocol,
    onDisconnect: () -> Unit
) {
    val RedesignTheme = rememberRedesignThemeColors()
    var showDisconnectDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = Color.Transparent,
        drawerContentColor = RedesignTheme.TextPrimary,
        drawerTonalElevation = 0.dp,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .widthIn(max = 360.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1322).copy(alpha = 0.88f),
                            Color(0xFF07090E).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                )
        ) {
            // Glowing Ambient Blobs inside the drawer
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-50).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-80).dp, y = 100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD500F9).copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RedesignTheme.Primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                tint = RedesignTheme.Primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Air Mouse Pro",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedesignTheme.TextPrimary
                        )
                        Text(
                            text = "v1.0.1",
                            fontSize = 12.sp,
                            color = RedesignTheme.TextTertiary
                        )
                    }
                }

                // Divider
                HorizontalDivider(color = RedesignTheme.SurfaceHighlight)

                // User Profile Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = RedesignTheme.Primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (userName.isNotBlank()) userName.take(1).uppercase(Locale.ROOT) else "U",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column {
                        Text(
                            text = userName.ifBlank { "User" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RedesignTheme.TextPrimary
                        )
                        Text(
                            text = if (userName.isNotBlank()) "${userName.lowercase().replace(" ", "")}@airmouse.pro" else "user@airmouse.pro",
                            fontSize = 12.sp,
                            color = RedesignTheme.TextSecondary
                        )
                    }
                }

                val statusColor by animateColorAsState(
                    targetValue = if (isConnected) RedesignTheme.StatusSuccess else if (isConnecting) RedesignTheme.StatusWarning else RedesignTheme.StatusError,
                    animationSpec = tween(500),
                    label = "statusColor"
                )

                // Connection Status Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = statusColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(statusColor, CircleShape)
                                )
                            }
                            Text(
                                text = if (protocol == ConnectionManager.ConnectionProtocol.BLUETOOTH) {
                                    if (isConnected) "Connected via Bluetooth" else if (isConnecting) "Advertising..." else "Disconnected"
                                } else {
                                    if (isConnected) "Connected to server" else if (isConnecting) "Connecting..." else "Disconnected"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RedesignTheme.TextPrimary
                            )
                        }
                        if (protocol != ConnectionManager.ConnectionProtocol.BLUETOOTH && isConnected && serverName.isNotBlank()) {
                            Text(
                                text = "Server: $serverName",
                                fontSize = 12.sp,
                                color = RedesignTheme.TextSecondary
                            )
                        }
                        if (protocol != ConnectionManager.ConnectionProtocol.BLUETOOTH && serverIp.isNotBlank()) {
                            Text(
                                text = "IP Address: $serverIp",
                                fontSize = 12.sp,
                                color = RedesignTheme.TextSecondary
                            )
                        }
                        DrawerSignalQualityText(
                            connectionQualityFlow = connectionQualityFlow,
                            isConnected = isConnected,
                            protocol = protocol
                        )
                    }
                }

                // Calibration Status Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isCalibrated) RedesignTheme.StatusSuccess else RedesignTheme.StatusWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Calibration",
                                fontSize = 14.sp,
                                color = RedesignTheme.TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isCalibrated) RedesignTheme.StatusSuccess else RedesignTheme.StatusWarning).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isCalibrated) "✅ Complete" else "⏳ Pending",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCalibrated) RedesignTheme.StatusSuccess else RedesignTheme.StatusWarning
                            )
                        }
                    }
                }

                var showDeviceDetails by remember { mutableStateOf(false) }

                // Session & System Info Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = RedesignTheme.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "System & Details",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedesignTheme.TextPrimary
                                )
                            }
                            IconButton(
                                onClick = { showDeviceDetails = !showDeviceDetails },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (showDeviceDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle System details",
                                    tint = RedesignTheme.TextSecondary
                                )
                            }
                        }

                        if (showDeviceDetails) {
                            HorizontalDivider(color = RedesignTheme.SurfaceHighlight, modifier = Modifier.padding(vertical = 4.dp))
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val batteryManager = context.getSystemService(android.content.Context.BATTERY_SERVICE) as? android.os.BatteryManager
                            val batteryLevel = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                            
                            val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
                            val hasGyro = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE) != null
                            val hasAccel = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) != null
                            val hasMag = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD) != null

                            val sharedPrefs = remember(context) { context.getSharedPreferences("airmouse_prefs", android.content.Context.MODE_PRIVATE) }
                            val protocolVal = sharedPrefs.getString("last_protocol", "UDP") ?: "UDP"
                            val sensitivity = sharedPrefs.getFloat("sensitivity", 0.5f)

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                InfoRow(label = "Device", value = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                                InfoRow(label = "Android Version", value = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                                if (batteryLevel >= 0) {
                                    InfoRow(label = "Battery", value = "$batteryLevel%")
                                }
                                InfoRow(label = "Protocol", value = protocol.name)
                                InfoRow(label = "Sensitivity", value = "%.1f×".format(sensitivity))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SensorBadge(name = "GYR", active = hasGyro)
                                    SensorBadge(name = "ACC", active = hasAccel)
                                    SensorBadge(name = "MAG", active = hasMag)
                                }
                            }
                        }
                    }
                }

                var showQuickActions by remember { mutableStateOf(false) }

                // Quick Settings Actions Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.SettingsSuggest,
                                    contentDescription = null,
                                    tint = RedesignTheme.Secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Quick Actions",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedesignTheme.TextPrimary
                                )
                            }
                            IconButton(
                                onClick = { showQuickActions = !showQuickActions },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (showQuickActions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Quick Actions",
                                    tint = RedesignTheme.TextSecondary
                                )
                            }
                        }

                        if (showQuickActions) {
                            HorizontalDivider(color = RedesignTheme.SurfaceHighlight, modifier = Modifier.padding(vertical = 4.dp))
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val sharedPrefs = remember(context) { context.getSharedPreferences("airmouse_prefs", android.content.Context.MODE_PRIVATE) }
                            
                            var hapticEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("haptic_enabled", true)) }
                            var lastProfile by remember { mutableStateOf(sharedPrefs.getString("last_used_profile", "Default") ?: "Default") }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Profile switcher
                                Text("Active Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RedesignTheme.TextSecondary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Default", "Gaming", "Presentation").forEach { profile ->
                                        val isSelected = lastProfile.equals(profile, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) RedesignTheme.Primary.copy(alpha = 0.2f) else RedesignTheme.SurfaceHighlight.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSelected) RedesignTheme.Primary else Color.Transparent),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    lastProfile = profile
                                                    sharedPrefs.edit().putString("last_used_profile", profile).apply()
                                                }
                                        ) {
                                            Text(
                                                text = profile,
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) RedesignTheme.Primary else RedesignTheme.TextPrimary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Haptic Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Haptic Feedback", fontSize = 12.sp, color = RedesignTheme.TextSecondary)
                                    Switch(
                                        checked = hapticEnabled,
                                        onCheckedChange = { enabled ->
                                            hapticEnabled = enabled
                                            sharedPrefs.edit().putBoolean("haptic_enabled", enabled).apply()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = RedesignTheme.Secondary,
                                            checkedTrackColor = RedesignTheme.Secondary.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                // Reset Defaults button
                                OutlinedButton(
                                    onClick = {
                                        sharedPrefs.edit().apply {
                                            remove("server_ip")
                                            remove("server_port")
                                            remove("last_protocol")
                                            apply()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, RedesignTheme.AccentRed.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("Reset Connection Cache", color = RedesignTheme.AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Divider
                HorizontalDivider(color = RedesignTheme.SurfaceHighlight)

                // Navigation Section Header
                Text(
                    text = "Navigation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedesignTheme.TextTertiary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                val navItems = listOf(
                    Triple(Destinations.Home.route, "Dashboard", Destinations.Home.icon),
                    Triple(Destinations.Calibration.route, "Calibration", Destinations.Calibration.icon),
                    Triple(Destinations.Touchpad.route, "Touchpad", Destinations.Touchpad.icon),
                    Triple(Destinations.BluetoothMouse.route, "Bluetooth Mouse", Destinations.BluetoothMouse.icon),
                    Triple(Destinations.SensorVisualizer.route, "Sensors", Destinations.SensorVisualizer.icon),
                    Triple(Destinations.NetworkDiscovery.route, "Network", Destinations.NetworkDiscovery.icon),
                    Triple(Destinations.EdgeGestures.route, "Shortcut Settings", Destinations.EdgeGestures.icon),
                    Triple(Destinations.ScreenMirroring.route, "Screen Mirroring", Destinations.ScreenMirroring.icon),
                    Triple(Destinations.FileTransfer.route, "File Transfer", Destinations.FileTransfer.icon),
                    Triple(Destinations.GamingMode.route, "Gaming Mode", Destinations.GamingMode.icon),
                    Triple(Destinations.VoiceCommands.route, "Voice Commands", Destinations.VoiceCommands.icon),
                    Triple(Destinations.Settings.route, "Settings", Destinations.Settings.icon),
                    Triple(Destinations.Help.route, "Help", Destinations.Help.icon),
                    Triple(Destinations.About.route, "About", Destinations.About.icon)
                )

                navItems.forEach { (route, title, icon) ->
                    val isSelected = currentRoute == route
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = { onItemClick(route) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        badge = {
                            // Badge chips
                            if (route == Destinations.Touchpad.route && isCalibrated) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RedesignTheme.Primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, RedesignTheme.Primary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Active",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedesignTheme.Primary
                                    )
                                }
                            } else if (route == Destinations.NetworkDiscovery.route && isConnecting) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RedesignTheme.StatusWarning.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, RedesignTheme.StatusWarning.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "New",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedesignTheme.StatusWarning
                                    )
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = RedesignTheme.Primary.copy(alpha = 0.15f),
                            selectedIconColor = RedesignTheme.Primary,
                            selectedTextColor = RedesignTheme.Primary,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = RedesignTheme.TextSecondary,
                            unselectedTextColor = RedesignTheme.TextSecondary
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Disconnect Button
                if (isConnected) {
                    HorizontalDivider(color = RedesignTheme.SurfaceHighlight)
                    Button(
                        onClick = { showDisconnectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedesignTheme.StatusError),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Disconnect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text(text = "Disconnect?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to disconnect from the desktop server?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectDialog = false
                        onDisconnect()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedesignTheme.StatusError)
                ) {
                    Text(text = "Disconnect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(text = "Cancel")
                }
            },
            containerColor = RedesignTheme.Surface
        )
    }
}

/**
 * Isolated leaf composable for top bar connection quality and ping.
 * Prevents full MainScreen recomposition sweeps when ping updates.
 */
@Composable
private fun ConnectionQualityPill(
    connectionQualityFlow: StateFlow<com.airmouse.network.ConnectionQuality>,
    currentProtocol: ConnectionManager.ConnectionProtocol,
    modifier: Modifier = Modifier
) {
    val quality by connectionQualityFlow.collectAsStateWithLifecycle()
    Row(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(Color(0xFF10B981), CircleShape)
        )
        Text(
            text = "${currentProtocol.name} • ${quality.ping} ms",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        if (quality.packetLoss > 0f) {
            Text(
                text = "Loss: ${(quality.packetLoss * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
        }
    }
}

/**
 * Isolated leaf composable for navigation drawer signal quality and ping.
 * Recomposes independently without re-evaluating drawer items or main scaffold.
 */
@Composable
private fun DrawerSignalQualityText(
    connectionQualityFlow: StateFlow<com.airmouse.network.ConnectionQuality>,
    isConnected: Boolean,
    protocol: ConnectionManager.ConnectionProtocol
) {
    if (protocol != ConnectionManager.ConnectionProtocol.BLUETOOTH && isConnected) {
        val quality by connectionQualityFlow.collectAsStateWithLifecycle()
        val signalText = when (quality.signalStrength) {
            com.airmouse.network.ConnectionQuality.SignalStrength.EXCELLENT -> "📶 Signal: Excellent (${quality.ping} ms)"
            com.airmouse.network.ConnectionQuality.SignalStrength.GOOD -> "📶 Signal: Good (${quality.ping} ms)"
            com.airmouse.network.ConnectionQuality.SignalStrength.FAIR -> "📶 Signal: Fair (${quality.ping} ms)"
            com.airmouse.network.ConnectionQuality.SignalStrength.POOR -> "📶 Signal: Poor (${quality.ping} ms)"
            com.airmouse.network.ConnectionQuality.SignalStrength.VERY_POOR -> "📶 Signal: Very Poor (${quality.ping} ms)"
            else -> "📶 Signal: Unknown"
        }
        Text(
            text = signalText,
            fontSize = 12.sp,
            color = RedesignTheme.TextSecondary
        )
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
        // Splash
        composable(Destinations.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    val nextRoute = if (navController.context.getSharedPreferences("airmouse_prefs", android.content.Context.MODE_PRIVATE).getBoolean("onboarding_completed", false)) {
                        Destinations.Home.route
                    } else {
                        Destinations.Onboarding.route
                    }
                    navController.navigate(nextRoute) {
                        popUpTo(Destinations.Splash.route) { inclusive = true }
                    }
                }
            )
        }

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

        composable(Destinations.GamingMode.route) {
            GamingModeScreen(navigationActions = navigationActions)
        }

        composable("game_controller") {
            com.airmouse.presentation.ui.gaming.GameControllerScreen(navigationActions = navigationActions)
        }

        composable(Destinations.ScreenMirroring.route) {
            ScreenMirroringScreen(navigationActions = navigationActions)
        }

        composable(Destinations.SyncStatus.route) {
            SyncStatusScreen(navigationActions = navigationActions)
        }

        composable(Destinations.NotificationsCenter.route) {
            NotificationsCenterScreen(navigationActions = navigationActions)
        }

        // Network and logs
        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Search.route) {
            SearchScreen(navigationActions = navigationActions)
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

        // File Transfer
        composable(Destinations.FileTransfer.route) {
            FileTransferScreen(navigationActions = navigationActions)
        }

        // Onboarding
        composable(Destinations.Onboarding.route) {
            OnboardingScreen(navigationActions = navigationActions)
        }

        // Bluetooth Mouse
        composable(Destinations.BluetoothMouse.route) {
            com.airmouse.presentation.ui.bluetooth.BluetoothMouseScreen(navigationActions = navigationActions)
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

@Composable
private fun InfoRow(label: String, value: String) {
    val RedesignTheme = rememberRedesignThemeColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = RedesignTheme.TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RedesignTheme.TextPrimary)
    }
}

@Composable
private fun SensorBadge(name: String, active: Boolean) {
    val RedesignTheme = rememberRedesignThemeColors()
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = (if (active) RedesignTheme.StatusSuccess else RedesignTheme.StatusError).copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, if (active) RedesignTheme.StatusSuccess else RedesignTheme.StatusError)
    ) {
        Text(
            text = "$name: ${if (active) "OK" else "N/A"}",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) RedesignTheme.StatusSuccess else RedesignTheme.StatusError
        )
    }
}

@Composable
fun BlackoutTouchpadOverlay(
    onDisableMouse: () -> Unit,
    connectionManager: ConnectionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastX by remember { mutableStateOf(0f) }
    var lastY by remember { mutableStateOf(0f) }
    val sensitivity = 1.4f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    lastX = down.position.x
                    lastY = down.position.y
                    var hasMoved = false
                    val startTime = System.currentTimeMillis()

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        event.changes.forEach {
                            if ((it.position - down.position).getDistance() > 15f) {
                                hasMoved = true
                            }
                        }

                        if (pressed.isEmpty()) {
                            val duration = System.currentTimeMillis() - startTime
                            if (!hasMoved && duration < 300) {
                                try {
                                    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                                        manager?.defaultVibrator
                                    } else {
                                        @Suppress("DEPRECATION")
                                        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                    }
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(25, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator?.vibrate(25)
                                    }
                                } catch (e: Exception) {
                                    // ignore haptic error
                                }
                                connectionManager.sendClick("left")
                            }
                            break
                        } else {
                            val first = pressed.first()
                            val dx = (first.position.x - lastX) * sensitivity
                            val dy = (first.position.y - lastY) * sensitivity

                            if (hasMoved && (dx != 0f || dy != 0f)) {
                                connectionManager.sendMove(dx, dy)
                            }

                            lastX = first.position.x
                            lastY = first.position.y
                            pressed.forEach { it.consume() }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Saver Mode Active",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Slide to use trackpad\nTap for Left Click\nPress Volume Down or Tap below to exit",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDisableMouse,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Exit Saver Mode", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
