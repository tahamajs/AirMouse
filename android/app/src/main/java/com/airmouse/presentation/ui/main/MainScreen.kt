package com.airmouse.presentation.ui.main
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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

// =======================================================================
// Navigation Elements Mock / Setup
// NOTE: If bottomNavItems and getSelectedBottomNavIndex are defined in
// your com.airmouse.presentation.navigation package, delete these copies.
// =======================================================================
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
// =======================================================================

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
                onDisconnect = { viewModel.disconnect() }
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                                onClick = { navController.navigate(Destinations.CalibrationResult.route) },
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
                    modifier = Modifier.fillMaxSize()
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
    onDisconnect: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF1A1D24),
        drawerContentColor = Color(0xFFE5E7EB),
        modifier = Modifier.width(320.dp)
    ) {
        // Header with gradient
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
                    "Version 3.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF96A0AE)
                )
                // Connection status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    if (isConnected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDisconnect) {
                            Text("Disconnect", fontSize = 11.sp, color = Color(0xFFF44336))
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2B3341))

        // Main menu items
        DrawerSection(
            title = "Main",
            items = listOf(
                DrawerItem(Destinations.Home.route, "Home", Icons.Default.Home),
                DrawerItem(Destinations.Statistics.route, "Statistics", Icons.Default.Assessment),
                DrawerItem(Destinations.Settings.route, "Settings", Icons.Default.Settings),
                DrawerItem(Destinations.Help.route, "Help", Icons.AutoMirrored.Filled.Help),
                DrawerItem(Destinations.About.route, "About", Icons.Default.Info)
            ),
            currentRoute = currentRoute,
            onItemClick = onItemClick
        )

        HorizontalDivider(color = Color(0xFF2B3341))

        // Control Mode Selection
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

        // Advanced tools group
        Text(
            text = "Advanced Tools",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF96A0AE),
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )

        val advancedTools = listOf(
            Destinations.GestureStudio.route to "✋ Gesture Studio",
            Destinations.NetworkDiscovery.route to "🌐 Network Discovery",
            Destinations.Proximity.route to "📡 Proximity Lock",
            Destinations.VoiceCommands.route to "🎤 Voice Commands",
            Destinations.EdgeGestures.route to "👆 Edge Gestures",
            Destinations.Touchpad.route to "🖱️ Touchpad Settings",
            Destinations.SensorVisualizer.route to "📊 Sensor Visualizer",
            Destinations.ServerLogs.route to "📋 Server Logs",
            Destinations.Battery.route to "🔋 Battery Monitor",
            Destinations.Accessibility.route to "♿ Accessibility",
            Destinations.Profiles.route to "👤 Profiles",
            Destinations.Themes.route to "🎨 Themes"
        )

        advancedTools.forEach { (route, title) ->
            NavigationDrawerItem(
                label = { Text(title, color = Color(0xFFE5E7EB), fontSize = 13.sp) },
                selected = currentRoute == route,
                onClick = { onItemClick(route) },
                icon = {
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        modifier = Modifier.size(6.dp),
                        tint = Color(0xFF4CAF50)
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF00BCD4).copy(alpha = 0.1f)
                ),
                modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

data class DrawerItem(val route: String, val title: String, val icon: ImageVector)

@Composable
fun DrawerSection(
    title: String,
    items: List<DrawerItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF96A0AE),
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
    )
    items.forEach { item ->
        val isSelected = currentRoute == item.route
        NavigationDrawerItem(
            label = { Text(item.title, color = if (isSelected) Color(0xFF00BCD4) else Color(0xFFE5E7EB)) },
            selected = isSelected,
            onClick = { onItemClick(item.route) },
            icon = {
                Icon(
                    item.icon,
                    contentDescription = item.title,
                    tint = if (isSelected) Color(0xFF00BCD4) else Color(0xFF96A0AE)
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFF00BCD4).copy(alpha = 0.1f)
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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

private fun shouldShowBottomBar(route: String?): Boolean {
    return when (route) {
        Destinations.Home.route,
        Destinations.Statistics.route,
        Destinations.Settings.route,
        Destinations.Help.route -> true
        else -> false
    }
}
