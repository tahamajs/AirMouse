@file:Suppress("unused")

package com.airmouse.presentation.ui.main
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airmouse.presentation.navigation.Destinations
import kotlinx.coroutines.launch
import java.util.Locale






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
                onDisconnect = { viewModel.disconnect() }
                )
            },
            gesturesEnabled = drawerState.isOpen
        ) {
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Air Mouse Pro", fontWeight = FontWeight.Bold)
                            Text("Telegram-style control center", fontSize = 11.sp, color = Color(0xFF96A0AE))
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
    onDisconnect: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF1A1D24),
        drawerContentColor = Color(0xFFE5E7EB),
        modifier = Modifier.width(320.dp)
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
                    userName = userName
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
                DrawerItem(Destinations.ServerLogs.route, "Server Logs", Icons.Default.List, "Debug", badge = if (isConnected) "LIVE" else "WAIT"),
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

data class DrawerItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val subtitle: String,
    val enabled: Boolean = true,
    val badge: String? = null
)

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

@Composable
private fun DrawerIdentityCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    isRegistered: Boolean,
    isCalibrated: Boolean,
    controlMode: String,
    userName: String
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
                    label = controlMode.replace('_', ' ').lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    color = Color(0xFF38BDF8)
                )
            }
            Text(
                userName.ifBlank { "Unknown user" },
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = Color(0xFF96A0AE),
                fontSize = 11.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(label = if (isRegistered) "Registered" else "Locked", color = if (isRegistered) Color(0xFF22C55E) else Color(0xFFF59E0B))
                StatusChip(label = if (isCalibrated) "Calibrated" else "Needs calibration", color = if (isCalibrated) Color(0xFF10B981) else Color(0xFFF97316))
            }
        }
    }
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
