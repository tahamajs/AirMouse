package com.airmouse.presentation.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.*
import com.airmouse.utils.QRScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    navigationActions: NavigationActions,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    // Animation states
    var showGreeting by remember { mutableStateOf(false) }
    var greetingText by remember { mutableStateOf("") }
    
    // QR scanner
    val qrScanner = remember { QRScanner() }
    var showQrPermissionDialog by remember { mutableStateOf(false) }
    
    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            qrScanner.startScan()
        } else {
            showQrPermissionDialog = true
        }
    }
    
    // Animated greeting on first launch
    LaunchedEffect(Unit) {
        delay(500)
        showGreeting = true
        val userName = viewModel.getUserName()
        greetingText = if (userName.isNotEmpty()) "Welcome back, $userName!" else "Welcome to Air Mouse Pro!"
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                userName = uiState.userName,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    navigationActions.navigateTo(route)
                },
                onDisconnect = { viewModel.disconnect() },
                isConnected = uiState.isConnected
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            topBar = { HomeTopBar(onMenuClick = { scope.launch { drawerState.open() } }) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = uiState.isConnected,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.disconnect() },
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
                // Background animation
                ParticleBackground(particleCount = 30)
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated Greeting
                    item {
                        AnimatedVisibility(
                            visible = showGreeting,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            GreetingCard(
                                text = greetingText,
                                userName = uiState.userName,
                                onEditProfile = { navigationActions.navigateTo("profiles") }
                            )
                        }
                    }
                    
                    // Connection Status Card
                    item {
                        ConnectionStatusCard(
                            isConnected = uiState.isConnected,
                            connectionQuality = uiState.connectionQuality,
                            serverName = uiState.serverName,
                            serverIp = uiState.serverIp,
                            ping = uiState.ping,
                            onDisconnect = { viewModel.disconnect() },
                            onReconnect = { viewModel.reconnect() }
                        )
                    }
                    
                    // Connection Controls
                    item {
                        ConnectionControlsCard(
                            ip = uiState.serverIp,
                            port = uiState.serverPort,
                            onIpChange = viewModel::updateIp,
                            onPortChange = viewModel::updatePort,
                            onConnect = { viewModel.connect() },
                            onScanQr = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    qrScanner.startScan()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            isConnecting = uiState.isConnecting,
                            lastServer = uiState.lastServerIp
                        )
                    }
                    
                    // Quick Actions Row
                    item {
                        QuickActionsRow(
                            onCalibrate = { navigationActions.navigateTo("calibration") },
                            onGestureStudio = { navigationActions.navigateTo("gesture_studio") },
                            onVoiceCommands = { navigationActions.navigateTo("voice_commands") },
                            onNetworkDiscovery = { navigationActions.navigateTo("network_discovery") }
                        )
                    }
                    
                    // Sensor Preview with 3D Visualization
                    item {
                        SensorPreviewCard(
                            roll = uiState.roll,
                            yaw = uiState.yaw,
                            pitch = uiState.pitch,
                            isCalibrated = uiState.isCalibrated
                        )
                    }
                    
                    // Stats Cards Row
                    item {
                        StatsRow(
                            clicks = uiState.clickCount,
                            scrolls = uiState.scrollCount,
                            rightClicks = uiState.rightClickCount,
                            doubleClicks = uiState.doubleClickCount,
                            sessionDuration = uiState.sessionDuration
                        )
                    }
                    
                    // Battery & Performance
                    item {
                        PerformanceCard(
                            batteryLevel = uiState.batteryLevel,
                            isCharging = uiState.isCharging,
                            cpuUsage = uiState.cpuUsage,
                            memoryUsage = uiState.memoryUsage,
                            frameRate = uiState.frameRate
                        )
                    }
                    
                    // Recent Gestures
                    if (uiState.recentGestures.isNotEmpty()) {
                        item {
                            RecentGesturesCard(
                                gestures = uiState.recentGestures,
                                onClear = { viewModel.clearRecentGestures() }
                            )
                        }
                    }
                    
                    // Tips Section
                    item {
                        TipsCard()
                    }
                }
                
                // Connection Status Badge (floating)
                ConnectionStatusBadge(
                    connectionManager = viewModel.connectionManager,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
                
                // QR Scan Result Handler
                LaunchedEffect(Unit) {
                    qrScanner.onScanResult = { result ->
                        val connectionData = qrScanner.parseConnectionData(result)
                        connectionData?.let {
                            viewModel.updateIp(it.ip)
                            viewModel.updatePort(it.port)
                            viewModel.connect()
                        }
                    }
                    qrScanner.onScanFailed = {
                        // Show error toast
                    }
                }
                
                // Permission Dialog
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
            }
        }
    }
}

@Composable
fun HomeTopBar(onMenuClick: () -> Unit) {
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
                    Text(
                        "Air Mouse Pro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Ready to control",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = { /* Open notifications */ }) {
                BadgedBox(badge = { Badge { Text("3") } }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }
            IconButton(onClick = { /* Open settings */ }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    )
}

@Composable
fun GreetingCard(text: String, userName: String, onEditProfile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
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
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    connectionQuality: Int,
    serverName: String,
    serverIp: String,
    ping: Int,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated connection icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(if (isConnected) pulse else 1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedConnectionStatus(
                    isConnected = isConnected,
                    signalStrength = connectionQuality,
                    ping = ping,
                    modifier = Modifier.size(80.dp),
                    showDetails = false
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
                    text = serverIp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConnectionQualityIndicator(
                        quality = ConnectionQuality(
                            ping = ping,
                            jitter = 5,
                            packetLoss = 0.02f,
                            signalStrength = when {
                                connectionQuality >= 75 -> ConnectionQuality.SignalStrength.EXCELLENT
                                connectionQuality >= 50 -> ConnectionQuality.SignalStrength.GOOD
                                connectionQuality >= 25 -> ConnectionQuality.SignalStrength.FAIR
                                else -> ConnectionQuality.SignalStrength.POOR
                            }
                        ),
                        modifier = Modifier
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReconnect,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reconnect")
                }
            }
        }
    }
}

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🔌 Connect to Server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            if (lastServer.isNotEmpty()) {
                Text(
                    text = "Last used: $lastServer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                keyboardType = KeyboardType.Number,
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
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting...")
                    } else {
                        Icon(Icons.Default.ConnectWithoutContact, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect")
                    }
                }
                
                OutlinedButton(
                    onClick = onScanQr,
                    modifier = Modifier.weight(1f).height(56.dp),
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
        QuickActionButton(
            onClick = onCalibrate,
            icon = Icons.Default.Build,
            label = "Calibrate",
            color = Color(0xFFFF5722)
        )
        QuickActionButton(
            onClick = onGestureStudio,
            icon = Icons.Default.Gesture,
            label = "Gestures",
            color = Color(0xFF9C27B0)
        )
        QuickActionButton(
            onClick = onVoiceCommands,
            icon = Icons.Default.Mic,
            label = "Voice",
            color = Color(0xFF2196F3)
        )
        QuickActionButton(
            onClick = onNetworkDiscovery,
            icon = Icons.Default.Wifi,
            label = "Network",
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
fun QuickActionButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Card(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@Composable
fun StatsRow(
    clicks: Int,
    scrolls: Int,
    rightClicks: Int,
    doubleClicks: Int,
    sessionDuration: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(
            title = "Clicks",
            value = clicks,
            icon = Icons.Default.Mouse,
            color = Color(0xFF00BCD4),
            modifier = Modifier.weight(1f)
        )
        StatsCard(
            title = "Scrolls",
            value = scrolls,
            icon = Icons.Default.SwapVert,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        StatsCard(
            title = "Session",
            value = sessionDuration,
            icon = Icons.Default.Timer,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f),
            isText = true
        )
    }
}

@Composable
fun StatsCard(
    title: String,
    value: Any,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isText: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            if (isText) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                AnimatedCounter(
                    targetValue = value as Int,
                    fontSize = 18.sp,
                    color = color
                )
            }
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PerformanceCard(
    batteryLevel: Int,
    isCharging: Boolean,
    cpuUsage: Int,
    memoryUsage: Int,
    frameRate: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 System Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Battery
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    BatteryLevelIndicator(level = batteryLevel, isCharging = isCharging, size = 40)
                    Text("$batteryLevel%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Battery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // CPU
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    CircularProgressWithLabel(progress = cpuUsage / 100f, size = 40, strokeWidth = 4)
                    Text("$cpuUsage%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("CPU", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // Memory
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    CircularProgressWithLabel(progress = memoryUsage / 100f, size = 40, strokeWidth = 4)
                    Text("$memoryUsage%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("RAM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // FPS
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("$frameRate", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text("FPS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun RecentGesturesCard(gestures: List<String>, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Recent Gestures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClear) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 150.dp)
            ) {
                items(gestures.takeLast(5)) { gesture ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00BCD4))
                        )
                        Text(
                            text = gesture,
                            fontSize = 13.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("💡", fontSize = 20.sp)
                }
            }
            
            Column {
                Text(
                    text = "Pro Tip",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Calibrate your sensors for the best accuracy and smooth cursor control.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HomeDrawerContent(
    userName: String,
    onItemClick: (String) -> Unit,
    onDisconnect: () -> Unit,
    isConnected: Boolean
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // User profile header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (userName.isNotEmpty()) userName else "Guest User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isConnected) "🟢 Online" else "⚫ Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        HorizontalDivider()
        
        // Navigation items
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = false,
            onClick = { onItemClick("home") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            label = { Text("Statistics") },
            selected = false,
            onClick = { onItemClick("statistics") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = { onItemClick("settings") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Help, contentDescription = null) },
            label = { Text("Help & Support") },
            selected = false,
            onClick = { onItemClick("help") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profiles") },
            selected = false,
            onClick = { onItemClick("profiles") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Palette, contentDescription = null) },
            label = { Text("Themes") },
            selected = false,
            onClick = { onItemClick("themes") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            label = { Text("About") },
            selected = false,
            onClick = { onItemClick("about") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (isConnected) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Close, contentDescription = null) },
                label = { Text("Disconnect", color = MaterialTheme.colorScheme.error) },
                selected = false,
                onClick = onDisconnect,
                colors = NavigationDrawerItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.error,
                    selectedIconColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        Text(
            text = "Version 3.0.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}


// HomeScreen.kt - Use these components:
@Composable
fun HomeScreen() {
    Column {
        // Header with animated connection status
        AnimatedConnectionStatus(
            isConnected = isConnected,
            signalStrength = 85,
            ping = 45
        )
        
        // Stats cards with glass effect
        GlassCard {
            Row {
                AnimatedCounter(targetValue = totalClicks, prefix = "Clicks: ")
                DonutChart(percentage = 0.75f, size = 80)
            }
        }
        
        // Speed chart
        LineChart(
            data = speedHistory,
            color = Color(0xFF00BCD4),
            animated = true
        )
        
        // Connection quality indicator
        ConnectionQualityIndicator(quality = connectionQuality)
        
        // Animated switch for settings
        AnimatedSwitch(
            checked = isActive,
            onCheckedChange = { /* toggle */ },
            label = "Sensor Active"
        )
    }
}