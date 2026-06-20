// app/src/main/java/com/airmouse/presentation/ui/home/HomeScreen.kt
@file:Suppress("DEPRECATION")

package com.airmouse.presentation.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigationActions: NavigationActions
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // UI State
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var mouseStatus by remember { mutableStateOf("Mouse Off") }
    var serverIp by remember { mutableStateOf("192.168.1.100") }
    var serverPort by remember { mutableIntStateOf(8080) }
    var serverName by remember { mutableStateOf("My Desktop PC") }
    var ping by remember { mutableIntStateOf(14) }
    var recentGestures by remember { mutableStateOf(listOf("Swipe Left - Next Slide", "Swipe Right - Prev Slide")) }
    var showGreeting by remember { mutableStateOf(false) }
    var showQrPermissionDialog by remember { mutableStateOf(false) }
    var pendingUserName by rememberSaveable { mutableStateOf(homeUiState.userName) }
    var showRegistrationDialog by rememberSaveable {
        mutableStateOf(homeUiState.userName.isBlank() && !homeViewModel.hasRegisteredUser())
    }
    var showCalibrationRequiredDialog by remember { mutableStateOf(false) }

    val userName = homeUiState.userName.ifBlank { pendingUserName }
    val greetingText = if (userName.isNotBlank()) "Welcome back, $userName!" else "Welcome to Air Mouse Pro!"

    LaunchedEffect(homeUiState.userName) {
        if (homeUiState.userName.isNotBlank()) {
            pendingUserName = homeUiState.userName
            showRegistrationDialog = false
        } else if (!homeViewModel.hasRegisteredUser()) {
            showRegistrationDialog = true
        }
    }

    fun connectMouse() {
        if (!homeUiState.isCalibrated) {
            showCalibrationRequiredDialog = true
            return
        }
        isConnected = true
        mouseStatus = "Mouse Active"
    }

    fun disconnectMouse() {
        isConnected = false
        mouseStatus = "Mouse Off"
    }

    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Trigger QR Scanning Action
        } else {
            showQrPermissionDialog = true
        }
    }

    // Animated greeting
    LaunchedEffect(Unit) {
        delay(500)
        showGreeting = true
    }

    Scaffold(
        topBar = { HomeTopBar(navigationActions = navigationActions) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isConnected,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { disconnectMouse() },
                    containerColor = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Disconnect")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Greeting Card
                item {
                    AnimatedVisibility(
                        visible = showGreeting,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        GreetingCard(
                            text = greetingText,
                            userName = userName,
                            onEditProfile = { navigationActions.navigateTo(Destinations.Profiles.route) }
                        )
                    }
                }

                // Connection Status
                item {
                    ConnectionStatusCard(
                        isConnected = isConnected,
                        serverName = serverName,
                        serverIp = serverIp,
                        ping = ping,
                        onDisconnect = { disconnectMouse() },
                        onReconnect = {
                            isConnecting = true
                            scope.launch {
                                delay(2000)
                                connectMouse()
                                isConnecting = false
                            }
                        }
                    )
                }

                // Connection Controls
                item {
                    ConnectionControlsCard(
                        ip = serverIp,
                        port = serverPort,
                        onIpChange = { serverIp = it },
                        onPortChange = { serverPort = it },
                        onConnect = { connectMouse() },
                        onScanQr = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                // Start scanning
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        isConnecting = isConnecting,
                        lastServer = "192.168.1.100"
                    )
                }

                // Quick Actions
                item {
                    QuickActionsRow(
                        onCalibrate = { navigationActions.navigateTo(Destinations.CalibrationResult.route) },
                        onGestureStudio = { navigationActions.navigateTo(Destinations.GestureStudio.route) },
                        onVoiceCommands = { navigationActions.navigateTo(Destinations.VoiceCommands.route) },
                        onNetworkDiscovery = { navigationActions.navigateTo(Destinations.NetworkDiscovery.route) }
                    )
                }

                // Sensor Preview
                item {
                    SensorPreviewCard(roll = 12f, yaw = -45f, pitch = 3f)
                }

                // Stats
                item {
                    StatsRow(clicks = 124, scrolls = 42, sessionDuration = "00:14:23")
                }

                // Performance
                item {
                    PerformanceCard(batteryLevel = 88, isCharging = false, cpuUsage = 14, memoryUsage = 45, frameRate = 60)
                }

                // Recent Gestures
                if (recentGestures.isNotEmpty()) {
                    item {
                        RecentGesturesCard(
                            gestures = recentGestures,
                            onClear = { recentGestures = emptyList() }
                        )
                    }
                }

                // Tips
                item {
                    TipsCard()
                }

                // Footer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Air Mouse Pro v3.0.0",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Status indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = when {
                            isConnected && homeUiState.isCalibrated -> Color(0xFF4CAF50)
                            homeUiState.isCalibrated -> Color(0xFF2196F3)
                            else -> Color(0xFFF44336)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when {
                        isConnected && homeUiState.isCalibrated -> mouseStatus
                        homeUiState.isCalibrated -> "Calibrated"
                        else -> "Calibrate First"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Permission dialog
            if (showQrPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showQrPermissionDialog = false },
                    title = { Text("Camera Permission Required") },
                    text = { Text("Camera permission is needed to scan QR codes for quick connection.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showQrPermissionDialog = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text("Grant")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showQrPermissionDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showRegistrationDialog) {
                AlertDialog(
                    onDismissRequest = { /* keep the prompt visible until a name is saved */ },
                    title = { Text("Register your name") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Enter the name you want to see on the welcome screen.")
                            OutlinedTextField(
                                value = pendingUserName,
                                onValueChange = { pendingUserName = it },
                                label = { Text("Your name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val trimmed = pendingUserName.trim()
                            if (trimmed.isNotBlank()) {
                                homeViewModel.saveUserName(trimmed)
                                showRegistrationDialog = false
                            }
                        }) {
                            Text("Save")
                        }
                    }
                )
            }

            if (showCalibrationRequiredDialog) {
                AlertDialog(
                    onDismissRequest = { showCalibrationRequiredDialog = false },
                    title = { Text("Calibration required") },
                    text = { Text("Please complete calibration before turning the mouse on.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showCalibrationRequiredDialog = false
                            navigationActions.navigateTo(Destinations.CalibrationResult.route)
                        }) {
                            Text("Calibrate")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCalibrationRequiredDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

// ==========================================
// TOP BAR
// ==========================================

@Composable
fun HomeTopBar(navigationActions: NavigationActions) {
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Air Mouse Pro", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Ready to control", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { overflowMenuExpanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = { overflowMenuExpanded = true }) {
                BadgedBox(badge = { Badge { Text("3") } }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
            }
            DropdownMenu(
                expanded = overflowMenuExpanded,
                onDismissRequest = { overflowMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Calibration") },
                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    onClick = {
                        overflowMenuExpanded = false
                        navigationActions.navigateTo(Destinations.CalibrationResult.route)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Profiles") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = {
                        overflowMenuExpanded = false
                        navigationActions.navigateTo(Destinations.Profiles.route)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        overflowMenuExpanded = false
                        navigationActions.navigateTo(Destinations.Settings.route)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Help") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                    onClick = {
                        overflowMenuExpanded = false
                        navigationActions.navigateTo(Destinations.Help.route)
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    )
}

// ==========================================
// GREETING CARD
// ==========================================

@Composable
fun GreetingCard(text: String, userName: String, onEditProfile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = if (userName.isNotEmpty()) "Tap to edit profile" else "Set up your profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEditProfile) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
            }
        }
    }
}

// ==========================================
// CONNECTION STATUS CARD
// ==========================================

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    serverName: String,
    serverIp: String,
    ping: Int,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(if (isConnected) pulse else 1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isConnected) "Connected to $serverName" else "Disconnected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (isConnected) {
                Text(
                    text = "$serverIp • Latency: ${ping}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onReconnect, modifier = Modifier.fillMaxWidth(0.6f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reconnect")
                }
            }
        }
    }
}

// ==========================================
// CONNECTION CONTROLS CARD
// ==========================================

@Composable
fun ConnectionControlsCard(
    ip: String,
    port: Int,
    onIpChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onConnect: () -> Unit,
    onScanQr: () -> Unit,
    isConnecting: Boolean,
    lastServer: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "🔌 Connect to Server", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (lastServer.isNotEmpty()) {
                Text(text = "Last used: $lastServer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = ip,
                onValueChange = onIpChange,
                label = { Text("IP Address") },
                placeholder = { Text("192.168.1.100") },
                leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isConnecting,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = port.toString(),
                onValueChange = { it.toIntOrNull()?.let(onPortChange) },
                label = { Text("Port") },
                placeholder = { Text("8080") },
                leadingIcon = { Icon(Icons.Default.SettingsEthernet, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isConnecting,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onConnect,
                    enabled = !isConnecting,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.ConnectWithoutContact, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect")
                    }
                }

                OutlinedButton(
                    onClick = onScanQr,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan QR")
                }
            }
        }
    }
}

// ==========================================
// QUICK ACTIONS
// ==========================================

@Composable
fun QuickActionsRow(
    onCalibrate: () -> Unit,
    onGestureStudio: () -> Unit,
    onVoiceCommands: () -> Unit,
    onNetworkDiscovery: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(onClick = onCalibrate, icon = Icons.Default.Build, label = "Calibrate", color = Color(0xFFFF5722))
        QuickActionButton(onClick = onGestureStudio, icon = Icons.Default.Gesture, label = "Gestures", color = Color(0xFF9C27B0))
        QuickActionButton(onClick = onVoiceCommands, icon = Icons.Default.Mic, label = "Voice", color = Color(0xFF2196F3))
        QuickActionButton(onClick = onNetworkDiscovery, icon = Icons.Default.Wifi, label = "Network", color = Color(0xFF4CAF50))
    }
}

@Composable
fun RowScope.QuickActionButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Card(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

// ==========================================
// SENSOR PREVIEW CARD
// ==========================================

@Composable
fun SensorPreviewCard(roll: Float, yaw: Float, pitch: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🎛️ IMU Gyro Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Roll: ${String.format(Locale.US, "%.1f", roll)}°", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Text("Yaw: ${String.format(Locale.US, "%.1f", yaw)}°", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Text("Pitch: ${String.format(Locale.US, "%.1f", pitch)}°", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ==========================================
// STATS ROW
// ==========================================

@Composable
fun StatsRow(clicks: Int, scrolls: Int, sessionDuration: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(title = "Clicks", value = clicks.toString(), icon = Icons.Default.Mouse, color = Color(0xFF00BCD4), modifier = Modifier.weight(1f))
        StatsCard(title = "Scrolls", value = scrolls.toString(), icon = Icons.Default.SwapVert, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
        StatsCard(title = "Session", value = sessionDuration, icon = Icons.Default.Timer, color = Color(0xFFFF9800), modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatsCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================================
// PERFORMANCE CARD
// ==========================================

@Composable
fun PerformanceCard(batteryLevel: Int, isCharging: Boolean, cpuUsage: Int, memoryUsage: Int, frameRate: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "📊 System Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Icon(if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull, contentDescription = null, tint = Color(0xFF4CAF50))
                    Text("$batteryLevel%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Battery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    LinearProgressIndicator(progress = { cpuUsage / 100f }, modifier = Modifier.width(40.dp).padding(vertical = 8.dp))
                    Text("$cpuUsage%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("CPU", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    LinearProgressIndicator(progress = { memoryUsage / 100f }, modifier = Modifier.width(40.dp).padding(vertical = 8.dp))
                    Text("$memoryUsage%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("RAM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("$frameRate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text("FPS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==========================================
// RECENT GESTURES CARD
// ==========================================

@Composable
fun RecentGesturesCard(gestures: List<String>, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎯 Recent Gestures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClear) { Text("Clear", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            gestures.forEach { gesture ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Text(text = gesture, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ==========================================
// TIPS CARD
// ==========================================

@Composable
fun TipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
            Column {
                Text(text = "Pro Tip", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    text = "Calibrate your sensors before starting a presentation for the smoothest tracking experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}