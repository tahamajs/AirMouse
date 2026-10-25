// app/src/main/java/com/airmouse/presentation/ui/home/HomeScreen.kt
@file:Suppress("DEPRECATION")

package com.airmouse.presentation.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.files.TransferState
import com.airmouse.presentation.theme.Success
import com.airmouse.presentation.theme.Warning
import com.airmouse.presentation.theme.Info
import com.airmouse.presentation.theme.Error as ThemeError
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.PresentationModeService
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.presentation.ui.components.ControlDashboard
import com.airmouse.presentation.ui.components.DonutChart
import com.airmouse.presentation.ui.components.GyroscopeVisualizer
import com.airmouse.presentation.ui.components.LineChart
import com.airmouse.presentation.ui.components.SensorVisualizer
import com.airmouse.presentation.ui.components.SweepGradientGlowRing
import com.airmouse.presentation.ui.components.GlassmorphicNeonProgressBar
import com.airmouse.presentation.ui.components.PulsingNeonOrbit
import com.airmouse.utils.QRScanner
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.airmouse.domain.model.CustomControlMode
import com.airmouse.network.ConnectionManager
import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import com.airmouse.presentation.ui.components.GlassmorphicCard
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        ControlDashboard()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigationActions: NavigationActions,
    onOpenDrawer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val approvalCountdownMs by homeViewModel.approvalCountdownMs.collectAsStateWithLifecycle()
    var serverIp by remember { mutableStateOf(homeUiState.serverIp.ifBlank { "192.168.1.100" }) }
    var serverPort by remember { mutableIntStateOf(homeUiState.serverPort.takeIf { it > 0 } ?: 8082) }
    var serverName by remember { mutableStateOf(homeUiState.userPreferences.serverName.ifBlank { "Air Mouse Pro" }) }
    var showQrPermissionDialog by remember { mutableStateOf(false) }
    var pendingUserName by rememberSaveable { mutableStateOf(homeUiState.userName) }
    var showRegistrationDialog by rememberSaveable {
        mutableStateOf(homeUiState.userName.isBlank() && !homeViewModel.hasRegisteredUser())
    }
    var showCalibrationRequiredDialog by remember { mutableStateOf(false) }

    val isRegistered = homeViewModel.hasRegisteredUser() || homeUiState.userName.isNotBlank()
    val selectedProtocol = homeUiState.selectedProtocol
    val connectionState = homeUiState.connectionState
    val isConnectionActive = connectionState is HomeViewModel.ConnectionState.Connected
    val isConnectionPending = connectionState is HomeViewModel.ConnectionState.Connecting ||
            connectionState is HomeViewModel.ConnectionState.WaitingForApproval ||
            homeUiState.isConnecting
    val isWaitingForApproval = connectionState is HomeViewModel.ConnectionState.WaitingForApproval
    val isAutoConnectEnabled = homeUiState.isAutoConnectEnabled
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val approvalCountdownText = remember(connectionState, approvalCountdownMs) {
        if (connectionState is HomeViewModel.ConnectionState.WaitingForApproval) {
            val seconds = (connectionState.remainingMs + 999) / 1000
            "Server approval timeout in ${seconds}s"
        } else if (isConnectionPending && approvalCountdownMs > 0L) {
            val seconds = (approvalCountdownMs + 999) / 1000
            "Server approval timeout in ${seconds}s"
        } else ""
    }
    val connectionStatusText = when (connectionState) {
        is HomeViewModel.ConnectionState.Connected -> "Connected"
        is HomeViewModel.ConnectionState.WaitingForApproval -> "Waiting for approval"
        is HomeViewModel.ConnectionState.Connecting -> "Connecting..."
        is HomeViewModel.ConnectionState.Error -> "Connection failed: ${connectionState.message}"
        is HomeViewModel.ConnectionState.Disconnected -> "Disconnected"
    }
    val statusAccent = when (connectionState) {
        is HomeViewModel.ConnectionState.Connected -> Success
        is HomeViewModel.ConnectionState.WaitingForApproval -> Warning
        is HomeViewModel.ConnectionState.Connecting -> Info
        is HomeViewModel.ConnectionState.Error -> MaterialTheme.colorScheme.error
        is HomeViewModel.ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LaunchedEffect(connectionState) {
        if (connectionState is HomeViewModel.ConnectionState.Error) {
            val msg = connectionState.message.ifBlank { "Please check server IP and port." }
            snackbarHostState.showSnackbar("Connection failed: $msg")
        }
    }

    LaunchedEffect(isConnectionPending, approvalCountdownMs) {
        if (isConnectionPending && approvalCountdownMs <= 0L) {
            homeViewModel.addLogMessage("Approval timed out; disconnected")
        }
    }

    // Screen brightness logic is now handled globally in HomeActivity

    LaunchedEffect(homeUiState.serverIp, homeUiState.serverPort) {
        serverIp = homeUiState.serverIp.ifBlank { serverIp }
        serverPort = homeUiState.serverPort.takeIf { it > 0 } ?: serverPort
    }

    LaunchedEffect(homeUiState.userPreferences.serverName, homeUiState.connectionConfig.ip) {
        serverName = homeUiState.userPreferences.serverName.ifBlank {
            homeViewModel.getServerName().ifBlank { "Air Mouse Pro" }
        }
    }

    LaunchedEffect(homeUiState.userName) {
        if (homeUiState.userName.isNotBlank()) {
            pendingUserName = homeUiState.userName
            showRegistrationDialog = false
        } else if (!homeViewModel.hasRegisteredUser()) {
            showRegistrationDialog = true
        }
    }

    LaunchedEffect(homeUiState.isAutoConnectEnabled, homeUiState.connectionStatus, homeUiState.serverIp, homeUiState.serverPort) {
        if (
            homeUiState.isAutoConnectEnabled &&
            homeUiState.connectionStatus == com.airmouse.domain.model.ConnectionStatus.DISCONNECTED &&
            homeUiState.serverIp.isNotBlank() &&
            homeUiState.serverPort > 0
        ) {
            delay(800)
            val currentUiState = homeViewModel.uiState.value
            if (
                currentUiState.isAutoConnectEnabled &&
                currentUiState.connectionStatus == com.airmouse.domain.model.ConnectionStatus.DISCONNECTED
            ) {
                homeViewModel.addLogMessage("Auto-connecting to ${homeUiState.serverIp}:${homeUiState.serverPort}")
                homeViewModel.connect()
            }
        }
    }

    fun toggleConnection() {
        if (isConnectionActive) {
            homeViewModel.disconnect()
        } else if (isConnectionPending) {
            homeViewModel.cancelConnection()
        } else {
            if (selectedProtocol != ConnectionProtocol.BLUETOOTH && homeUiState.pairingToken.isBlank()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Token is required for V2 Security")
                }
                return
            }
            if (!homeUiState.isCalibrated) {
                showCalibrationRequiredDialog = true
            } else {
                homeViewModel.connect()
            }
        }
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

    val scanQrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scannedData = result.contents
        if (!scannedData.isNullOrBlank()) {
            QRScanner.parseConnectionDataStatic(scannedData)?.let { parsed ->
                serverIp = parsed.ip
                serverPort = parsed.port
                serverName = parsed.name
                homeViewModel.updateIp(parsed.ip)
                homeViewModel.updatePort(parsed.port)
                homeViewModel.applyScannedConnection(parsed)
            }
        }
    }



    PremiumDashboardBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
            HomeTopBar(
                onDrawerClick = { onOpenDrawer?.invoke() },
                onSearchClick = { navigationActions.navigateTo(Destinations.Search.route) },
                onMenuItemClick = { route -> navigationActions.navigateTo(route) },
                isConnected = isConnectionActive,
                statusText = connectionStatusText
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Connection Status Banner
                if (!isConnectionActive) {
                    item {
                        StateOverviewBanner(
                            isConnected = isConnectionActive,
                            isCalibrated = homeUiState.isCalibrated,
                            needsRecalibration = homeUiState.calibrationState.needsRecalibration,
                            isConnecting = isConnectionPending,
                            isWaitingForApproval = isWaitingForApproval,
                            statusText = connectionStatusText,
                            approvalCountdownText = approvalCountdownText,
                            accent = statusAccent,
                            serverIp = serverIp,
                            serverPort = serverPort,
                            onOpenCalibration = { navigationActions.navigateToCalibration() },
                            onOpenNetwork = { navigationActions.navigateTo(Destinations.NetworkDiscovery.route) },
                            onDisconnect = { homeViewModel.disconnect() }
                        )
                    }
                }

                // 2. Primary Connection Toggle
                if (!isConnectionActive) {
                    item {
                        ConnectionToggleButton(
                            isAutoConnectEnabled = isAutoConnectEnabled,
                            onToggleAutoConnect = { homeViewModel.toggleAutoConnect() },
                            isConnected = isConnectionActive,
                            isConnecting = isConnectionPending,
                            statusText = connectionStatusText,
                            accentColor = statusAccent,
                            isMouseControlEnabled = homeUiState.isMouseControlEnabled,
                            onToggleMouseControl = { homeViewModel.setMouseControlEnabled(it) },
                            onToggle = { toggleConnection() }
                        )
                    }
                }

                // Control Mode Selector Card – only visible when connected
                if (isConnectionActive) {
                    item {
                        val activeMode = homeViewModel.getActiveMode()
                        if (homeUiState.isActive && activeMode.moveSource == "touchpad") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Touchpad Active", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Button(
                                        onClick = { homeViewModel.stopAirMouse() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text("Stop", color = Color.White)
                                    }
                                }
                                EmbeddedTouchpad(
                                    connectionManager = homeViewModel.connectionManager,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else if (homeUiState.isActive) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text("Motion Transmission Live", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = if (activeMode.moveSource == "linear_accel") {
                                            "Mode: ${activeMode.name}\nSlide phone in air to move cursor. Push/pull to scroll."
                                        } else {
                                            "Mode: ${activeMode.name}\nTilt phone up/down and left/right to move cursor. Push/pull to scroll."
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { homeViewModel.stopAirMouse() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Deactivate Mouse", color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    var showCreateDialog by remember { mutableStateOf(false) }
                                    var modeToEdit by remember { mutableStateOf<CustomControlMode?>(null) }
                                    val predefinedModes = CustomControlMode.getPredefinedModes()
                                    val customModes = homeViewModel.getCustomModes()
                                    val allModes = predefinedModes + customModes

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Gamepad,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text("Select Control Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        IconButton(onClick = { showCreateDialog = true }) {
                                            Icon(Icons.Default.Add, contentDescription = "Create Custom Mode", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        allModes.forEach { mode ->
                                            val isSelected = homeUiState.controlMode == mode.id
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { homeViewModel.setControlMode(mode.id) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = when (mode.moveSource) {
                                                                "gyro" -> Icons.Default.Sensors
                                                                "linear_accel" -> Icons.Default.DirectionsRun
                                                                "touchpad" -> Icons.Default.TouchApp
                                                                else -> Icons.Default.Build
                                                            },
                                                            contentDescription = null,
                                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Column {
                                                            Text(
                                                                text = mode.name,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp
                                                            )
                                                            Text(
                                                                text = when (mode.moveSource) {
                                                                    "gyro" -> "Gyroscope Relative Tilt"
                                                                    "linear_accel" -> "Translation Left/Right/Up/Down"
                                                                    "touchpad" -> "Screen Touchpad"
                                                                    else -> ""
                                                                },
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    if (mode.isCustom) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            IconButton(onClick = { modeToEdit = mode }) {
                                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                                            }
                                                            IconButton(onClick = { homeViewModel.deleteCustomMode(mode.id) }) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (showCreateDialog) {
                                        CustomModeDialog(
                                            modeToEdit = null,
                                            onDismiss = { showCreateDialog = false },
                                            onSave = {
                                                homeViewModel.saveCustomMode(it)
                                                showCreateDialog = false
                                            }
                                        )
                                    }

                                    if (modeToEdit != null) {
                                        CustomModeDialog(
                                            modeToEdit = modeToEdit,
                                            onDismiss = { modeToEdit = null },
                                            onSave = {
                                                homeViewModel.saveCustomMode(it)
                                                modeToEdit = null
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Customize button shortcuts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(
                                            onClick = { navigationActions.navigateTo(Destinations.EdgeGestures.route) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Configure ⚙️", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Connected Info Card (shown when connected) OR Connection Setup Card (shown when not connected)
                if (isConnectionActive) {
                    item {
                        ConnectedInfoCard(
                            serverIp = serverIp,
                            serverPort = serverPort,
                            protocol = selectedProtocol,
                            serverName = serverName,
                            isMouseControlEnabled = homeUiState.isMouseControlEnabled,
                            onToggleMouseControl = { homeViewModel.setMouseControlEnabled(it) },
                            onDisconnect = { homeViewModel.disconnect() }
                        )
                    }
                } else {
                    item {
                        ConnectionControlsCard(
                            ip = serverIp,
                            port = serverPort,
                            pairingToken = homeUiState.pairingToken,
                            protocol = selectedProtocol,
                            onIpChange = {
                                serverIp = it
                                homeViewModel.updateIp(it)
                            },
                            onPortChange = {
                                serverPort = it
                                homeViewModel.updatePort(it)
                            },
                            onTokenChange = {
                                homeViewModel.updatePairingToken(it)
                            },
                            onProtocolChange = { homeViewModel.updateConnectionProtocol(it) },
                            onConnect = { toggleConnection() },
                            onCancel = { homeViewModel.cancelConnection() },
                            onScanQr = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val options = ScanOptions().apply {
                                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        setPrompt("Scan the Air Mouse pairing QR code")
                                        setBeepEnabled(true)
                                        setOrientationLocked(false)
                                    }
                                    scanQrLauncher.launch(options)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            isConnecting = isConnectionPending,
                            isConnected = isConnectionActive,
                            isRegistered = isRegistered,
                            lastServer = homeUiState.connectionConfig.ip.ifBlank { "Not set" }
                        )
                    }
                }

                // 4. Feature & Module Navigation Hub
                item {
                    ModuleHubCard(
                        isRegistered = isRegistered,
                        isCalibrated = homeUiState.isCalibrated,
                        onModuleClick = { route ->
                            if (!isRegistered) {
                                showRegistrationDialog = true
                            } else if (!homeUiState.isCalibrated && route != Destinations.Calibration.route) {
                                showCalibrationRequiredDialog = true
                            } else {
                                navigationActions.navigateTo(route)
                            }
                        }
                    )
                }

                // 5. Minimal stats
                item {
                    StatsRow(
                        clicks = homeUiState.gestureStats.totalClicks,
                        scrolls = homeUiState.gestureStats.totalScrolls,
                        sessionDuration = homeViewModel.getSessionDuration().toReadableDuration()
                    )
                }

                // 6. Quick Actions
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    QuickActionsRow(
                        onCalibrate = { navigationActions.navigateTo(Destinations.Calibration.route) },
                        onGestureStudio = { navigationActions.navigateTo(Destinations.GestureStudio.route) },
                        onVoiceCommands = { navigationActions.navigateTo(Destinations.VoiceCommands.route) },
                        onNetworkDiscovery = { navigationActions.navigateTo(Destinations.NetworkDiscovery.route) }
                    )

                    // Bluetooth Mouse Feature Card
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        onClick = { navigationActions.navigateTo(Destinations.BluetoothMouse.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text("Bluetooth Mouse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Use as a native wireless mouse", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Footer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Air Mouse Pro v4.9.9",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
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
                    title = { Text("Approve your name") },
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
                            navigationActions.navigateToCalibration()
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
}

// ==========================================
// UNIFIED CONTROL BUTTON
// ==========================================

@Composable
fun UnifiedControlButton(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isConnected = uiState.connectionStatus == com.airmouse.domain.model.ConnectionStatus.CONNECTED
    val isActive = uiState.isActive
    val isConnecting = uiState.isConnecting

    val buttonText = when {
        isConnecting -> "Connecting..."
        isConnected && isActive -> "🟢 Stop Air Mouse"
        isConnected && !isActive -> "🔴 Start Air Mouse"
        else -> "📡 Connect to Server"
    }

    val buttonColor = when {
        isConnecting -> MaterialTheme.colorScheme.secondary
        isConnected && isActive -> MaterialTheme.colorScheme.error
        isConnected && !isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    Button(
        onClick = {
            when {
                isConnecting -> { /* Do nothing, already connecting */ }
                isConnected && isActive -> viewModel.stopAirMouse()
                isConnected && !isActive -> viewModel.startAirMouse()
                else -> viewModel.connect()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isConnecting,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isConnecting) {
            SweepGradientGlowRing(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                glowRadius = 3.dp,
                colors = listOf(
                    MaterialTheme.colorScheme.onPrimary,
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.onPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            buttonText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// CONNECTION TOGGLE BUTTON
// ==========================================

@Composable
fun ConnectionToggleButton(
    isConnected: Boolean,
    isConnecting: Boolean,
    isAutoConnectEnabled: Boolean,
    statusText: String,
    accentColor: Color,
    isMouseControlEnabled: Boolean,
    onToggleMouseControl: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onToggleAutoConnect: () -> Unit
) {
    val isAutoConnectSwitchEnabled = isConnected || isConnecting
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedStatusDot(
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    isAutoConnectEnabled = isAutoConnectEnabled
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isConnected -> "Connected"
                            isConnecting -> "Connecting..."
                            isAutoConnectEnabled -> "Auto-reconnect ON"
                            else -> "Disconnected"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isConnected) accentColor else if (isAutoConnectEnabled) Warning else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isAutoConnectEnabled) "Auto ON" else "Auto OFF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAutoConnectSwitchEnabled) {
                            if (isAutoConnectEnabled) Success else MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                    Switch(
                        checked = isAutoConnectEnabled,
                        onCheckedChange = { if (isAutoConnectSwitchEnabled) onToggleAutoConnect() },
                        enabled = isAutoConnectSwitchEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Success,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Main toggle button
            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isConnected -> ThemeError
                        isConnecting -> Warning
                        else -> Success
                    },
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                enabled = !isConnecting
            ) {
                if (isConnecting) {
                    SweepGradientGlowRing(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        glowRadius = 3.dp,
                        colors = listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.5f),
                            Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting...")
                } else if (isConnected) {
                    Icon(Icons.Default.Close, contentDescription = "Disconnect")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Connect")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect")
                }
            }

            if (isConnected) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isMouseControlEnabled) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = null,
                            tint = if (isMouseControlEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text(
                                text = "Active Mouse Transmission",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isMouseControlEnabled) "Sending phone sensors to PC" else "Transmission paused",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isMouseControlEnabled,
                        onCheckedChange = onToggleMouseControl,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Status hint
            if (!isConnected && isAutoConnectEnabled) {
                Text(
                    text = "Auto-reconnect is ON. Disconnect closes this session, then reconnects automatically.",
                    fontSize = 11.sp,
                    color = Warning,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!isConnected && !isAutoConnectEnabled) {
                Text(
                    text = "Tap Connect to start a session.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (isConnected) {
                Text(
                    text = "Connected to server. Mouse control is active.",
                    fontSize = 11.sp,
                    color = Success,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AnimatedStatusDot(
    isConnected: Boolean,
    isConnecting: Boolean,
    isAutoConnectEnabled: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusDot")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val color by animateColorAsState(
        targetValue = when {
            isConnected -> Success
            isConnecting -> Warning
            isAutoConnectEnabled -> Warning
            else -> ThemeError
        },
        animationSpec = tween(500),
        label = "statusColor"
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer {
                val scale = if (isConnected || isConnecting) pulse else 1f
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        // Glow effect
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = color.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        )
    }
}


// ==========================================
// TELEMETRY CARD
// ==========================================

@Composable
private fun SensorTelemetryCard(
    roll: Float,
    pitch: Float,
    yaw: Float,
    sensorState: HomeViewModel.SensorState,
    isConnected: Boolean,
    isCalibrated: Boolean,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Sensor visualizer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Live gyroscope, tilt, and orientation preview for motion control and calibration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            when {
                                !isConnected -> "Waiting for approval"
                                isConnected && isCalibrated -> "Connected"
                                else -> "Approved"
                            }
                        )
                    }
                )
            }

            SensorVisualizer(
                roll = roll,
                pitch = pitch,
                yaw = yaw,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                size = com.airmouse.presentation.ui.components.VisualizerSize.LARGE,
                isCalibrated = isCalibrated
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricChip(label = "Yaw", value = String.format(Locale.US, "%.2f", yaw))
                MiniMetricChip(label = "Pitch", value = String.format(Locale.US, "%.2f", pitch))
                MiniMetricChip(label = "Roll", value = String.format(Locale.US, "%.2f", roll))
                MiniMetricChip(label = "Speed", value = String.format(Locale.US, "%.2f", sensorState.currentSpeed))
            }

            GyroscopeVisualizer(
                x = sensorState.lastDx,
                y = sensorState.lastDy,
                z = sensorState.currentSpeed,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SensorVisualizerShortcutCard(
    isRegistered: Boolean,
    isCalibrated: Boolean,
    isConnected: Boolean,
    onOpenVisualizer: () -> Unit,
    onOpenCalibration: () -> Unit
) {
    val ready = isRegistered && isCalibrated
    val accent = when {
        isConnected -> Success
        ready -> Info
        else -> Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = accent
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Live sensor visualizer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Open the full gyroscope, accelerometer, and magnetometer charts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            when {
                                isConnected -> "Live"
                                ready -> "Approved"
                                else -> "Approval needed"
                            }
                        )
                    }
                )
            }

            Text(
                text = if (ready) {
                    "This screen shows the full sensor stack with charts, raw values, and orientation tracking."
                } else {
                    "Finish calibration before opening the live sensor stack."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = if (ready) onOpenVisualizer else onOpenCalibration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                )
            ) {
                Text(if (ready) "Open Sensor Visualizer" else "Go to Calibration")
            }
        }
    }
}

@Composable
private fun InlineSensorChartsCard(
    gyroSeries: List<Float>,
    accelSeries: List<Float>,
    magSeries: List<Float>,
    isActive: Boolean,
    accent: Color
) {
    val hintColor = if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(hintColor, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Inline sensor charts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tiny live previews for gyro, accel, and magnetometer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text(if (isActive) "Live" else "Idle") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniChartColumn(
                    title = "Gyro",
                    values = gyroSeries,
                    color = Info,
                    modifier = Modifier.weight(1f)
                )
                MiniChartColumn(
                    title = "Accel",
                    values = accelSeries,
                    color = Success,
                    modifier = Modifier.weight(1f)
                )
                MiniChartColumn(
                    title = "Mag",
                    values = magSeries,
                    color = Warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DeviceIdentityCard(
    userName: String,
    serverName: String,
    serverIp: String,
    serverPort: Int,
    statusText: String,
    isConnected: Boolean,
    isCalibrated: Boolean
) {
    val statusColor = when {
        isConnected && isCalibrated -> Success
        isConnected -> Info
        else -> Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Device identity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "This phone is saved by the server as a stable device profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text(statusText) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricChip("User", userName.ifBlank { "Unregistered" })
                MiniMetricChip("Model", Build.MODEL.ifBlank { "Unknown" })
                MiniMetricChip("Android", Build.VERSION.RELEASE.ifBlank { "Unknown" })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricChip("Manufacturer", Build.MANUFACTURER.ifBlank { "Unknown" })
                MiniMetricChip("Server", serverName.ifBlank { "Not set" })
                MiniMetricChip("Target", "$serverIp:$serverPort")
            }
        }
    }
}

@Composable
private fun MiniChartColumn(
    title: String,
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LineChart(
            data = values.ifEmpty { listOf(0f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            color = color,
            animated = false
        )
    }
}

@Composable
private fun CollaborationGateCard(
    isRegistered: Boolean,
    isCalibrated: Boolean,
    isConnected: Boolean,
    sensorsCalibrated: Int,
    totalSensors: Int,
    progress: Float,
    onOpenCalibration: () -> Unit,
    onConnect: () -> Unit
) {
    val ready = isRegistered && isCalibrated
    val gateLabel = when {
        !isRegistered -> "Approval needed"
        !isCalibrated -> "Calibration required"
        isConnected -> "Connected"
        else -> "Approved"
    }
    val gateColor = when {
        isConnected -> Success
        ready -> Info
        else -> Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DonutChart(
                    percentage = progress.coerceIn(0f, 1f),
                    size = 92,
                    color = gateColor,
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ready to collaborate",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Calibration and approval share the same gate across the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = { },
                        label = { Text(gateLabel) }
                    )
                }
            }

            Text(
                text = if (ready) {
                    "The phone is calibrated and ready to collaborate with the desktop."
                } else {
                    "Complete the sensor calibration before collaboration is unlocked."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { },
                    label = { Text("$sensorsCalibrated / $totalSensors sensors ready") }
                )
                AssistChip(
                    onClick = onOpenCalibration,
                    label = { Text("Open calibration") }
                )
            }

            Button(
                onClick = if (ready && !isConnected) onConnect else onOpenCalibration,
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = gateColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        isConnected -> "Connected"
                        ready -> "Connect now"
                        else -> "Finish calibration"
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
fun HomeTopBar(
    onDrawerClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuItemClick: (String) -> Unit,
    isConnected: Boolean,
    statusText: String
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val sharedPrefs = remember { context.getSharedPreferences("airmouse_prefs", android.content.Context.MODE_PRIVATE) }
    var isOrientationLocked by remember { mutableStateOf(sharedPrefs.getBoolean("orientation_locked", false)) }
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        "Air Mouse Pro",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(
                            label = if (isConnected) "Connected" else "Waiting",
                            color = if (isConnected) Success else Warning
                        )
                        Text(
                            statusText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onDrawerClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Open menu")
                }
            },
            actions = {
                IconButton(onClick = {
                    isOrientationLocked = !isOrientationLocked
                    sharedPrefs.edit().putBoolean("orientation_locked", isOrientationLocked).apply()
                    activity?.requestedOrientation = if (isOrientationLocked) {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }) {
                    Icon(
                        imageVector = if (isOrientationLocked) Icons.Default.ScreenLockPortrait else Icons.Default.ScreenRotation,
                        contentDescription = "Toggle Screen Orientation Lock",
                        tint = if (isOrientationLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search devices")
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Open app menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Home Actions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )

                        DropdownMenuItem(
                            text = { Text("Calibration") },
                            onClick = {
                                showMenu = false
                                onMenuItemClick(Destinations.Calibration.route)
                            },
                            leadingIcon = { Icon(Icons.Default.Tune, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sensor Visualizer") },
                            onClick = {
                                showMenu = false
                                onMenuItemClick(Destinations.SensorVisualizer.route)
                            },
                            leadingIcon = { Icon(Icons.Default.Sensors, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Statistics") },
                            onClick = {
                                showMenu = false
                                onMenuItemClick(Destinations.Statistics.route)
                            },
                            leadingIcon = { Icon(Icons.Default.Assessment, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Help") },
                            onClick = {
                                showMenu = false
                                onMenuItemClick(Destinations.Help.route)
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Help, null) }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun StateOverviewBanner(
    isConnected: Boolean,
    isCalibrated: Boolean,
    needsRecalibration: Boolean,
    isConnecting: Boolean,
    isWaitingForApproval: Boolean = false,
    statusText: String,
    approvalCountdownText: String,
    accent: Color,
    serverIp: String,
    serverPort: Int,
    onOpenCalibration: () -> Unit,
    onOpenNetwork: () -> Unit,
    onDisconnect: () -> Unit
) {
    val headline = when {
        isConnected && isCalibrated -> "Connected"
        isWaitingForApproval -> "Waiting for approval"
        isConnecting -> "Connecting..."
        isCalibrated -> "Calibration complete"
        needsRecalibration -> "Recalibration recommended"
        else -> "Ready to connect"
    }
    val detail = when {
        isConnected && isCalibrated -> "Approved and connected. Touchpad, gestures, and mouse control are active."
        isWaitingForApproval -> "Waiting for approval from the desktop."
        isConnecting -> "Attempting connection to the desktop server..."
        isCalibrated -> "Sensors calibrated and ready. Connect whenever you are ready."
        needsRecalibration -> "Sensors ready, but recalibration is recommended before the next session."
        else -> "Ready to connect. Verify your server details below."
    }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(
                    onClick = onOpenCalibration,
                    label = { Text(if (isCalibrated) "Recalibrate" else "Calibrate") },
                    elevation = AssistChipDefaults.assistChipElevation(
                        elevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                )
            }

            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (approvalCountdownText.isNotBlank()) {
                Text(
                    text = approvalCountdownText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Warning
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = isCalibrated,
                    onClick = onOpenCalibration,
                    label = { Text("Calibration") },
                    elevation = FilterChipDefaults.filterChipElevation(
                        elevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                )
                FilterChip(
                    selected = isConnected,
                    onClick = onOpenNetwork,
                    label = { Text("Network") },
                    elevation = FilterChipDefaults.filterChipElevation(
                        elevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                )
                FilterChip(
                    selected = isConnecting,
                    onClick = { },
                    label = { Text("Session") },
                    elevation = FilterChipDefaults.filterChipElevation(
                        elevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                )
            }

            Text(
                text = "Target: $serverIp:$serverPort",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Prominent Disconnect button when connected
            if (isConnected) {
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Disconnect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}


@Composable
private fun ScreenSleepHintCard(
    hint: String,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enabled) "Screen can sleep" else "Screen sleep locked",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = hint,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NotificationCenterCard(
    statusText: String,
    isConnected: Boolean,
    isConnecting: Boolean,
    isCalibrated: Boolean,
    needsRecalibration: Boolean,
    onOpenLogs: () -> Unit
) {
    val accent = when {
        isConnected && isCalibrated -> Success
        isConnecting -> Warning
        needsRecalibration -> Warning
        isCalibrated -> Info
        else -> ThemeError
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Connection status center", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(statusText, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                }
                AssistChip(
                    onClick = onOpenLogs,
                    label = { Text("Logs") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(
                    label = if (isConnecting) "Waiting for approval" else if (isConnected) "Approved" else "Approval needed",
                    color = accent
                )
                StatusChip(
                    label = when {
                        needsRecalibration -> "Recalibrate"
                        isCalibrated -> "Calibrated"
                        else -> "Needs calibration"
                    },
                    color = when {
                        needsRecalibration -> Warning
                        isCalibrated -> Success
                        else -> Warning
                    }
                )
            }
        }
    }
}

@Composable
private fun AccessGateCard(
    isRegistered: Boolean,
    isCalibrated: Boolean,
    isConnected: Boolean,
    onRegister: () -> Unit,
    onCalibrate: () -> Unit
) {
    val missingLabel = when {
        !isRegistered -> "Waiting for approval"
        !isCalibrated -> "Calibration required"
        !isConnected -> "Waiting for approval"
        else -> "Approved and connected"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isRegistered -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                !isCalibrated -> Warning.copy(alpha = 0.14f)
                !isConnected -> Info.copy(alpha = 0.14f)
                else -> Success.copy(alpha = 0.14f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Access gate", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(missingLabel, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
            Text(
                text = if (isRegistered && isCalibrated && isConnected) {
                    "The full control surface is approved and connected."
                } else if (!isConnected) {
                    "Waiting for approval from the desktop."
                } else {
                    "The app will guide the user through setup, calibration, and desktop approval before enabling the control features."
                },
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isRegistered) {
                    FilledTonalButton(onClick = onRegister) { Text("Approve user") }
                }
                if (!isCalibrated) {
                    FilledTonalButton(onClick = onCalibrate) { Text("Calibrate") }
                }
            }
        }
    }
}

@Composable
private fun ModuleHubCard(
    isRegistered: Boolean,
    isCalibrated: Boolean,
    onModuleClick: (String) -> Unit
) {
    val modules = listOf(
        Destinations.Touchpad to "Pointer control",
        Destinations.CalibrationResult to "Calibration",
        Destinations.GestureStudio to "Gesture Studio",
        Destinations.SensorVisualizer to "Sensor Monitor",
        Destinations.NetworkDiscovery to "Network",
        Destinations.ServerLogs to "Logs",
        Destinations.Proximity to "Proximity",
        Destinations.VoiceCommands to "Voice",
        Destinations.ScreenMirroring to "Screen Mirror",
        Destinations.FileTransfer to "File Transfer",
        Destinations.GamingMode to "Gaming",
        Destinations.EdgeGestures to "Edge Gestures",
        Destinations.Profiles to "Profiles",
        Destinations.Themes to "Themes",
        Destinations.Settings to "Settings",
        Destinations.Help to "Help"
    )

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Modules", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "All main screens are grouped here for quick access.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = { Text(if (isRegistered && isCalibrated) "Approved" else "Waiting for approval") }
                )
            }

            val chunked = modules.chunked(3)
            chunked.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { (destination, label) ->
                        ModuleTile(
                            label = label,
                            icon = destination.icon,
                            enabled = isRegistered && (isCalibrated || destination == Destinations.CalibrationResult),
                            modifier = Modifier.weight(1f),
                            onClick = { onModuleClick(destination.route) }
                        )
                    }
                    // Pad the last row if it's not full
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}


@Composable
private fun ModuleTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassmorphicCard(
        modifier = modifier
            .height(88.dp)
            .clickable(enabled = enabled, onClick = onClick),
        cornerRadius = 18.dp,
        backgroundAlpha = if (enabled) 0.25f else 0.10f
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ==========================================
// GREETING CARD
// ==========================================

@Composable
fun GreetingCard(text: String, userName: String, onEditProfile: () -> Unit) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
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
// CONNECTION CONTROLS CARD
// ==========================================

@Composable
fun ConnectionControlsCard(
    ip: String,
    port: Int,
    pairingToken: String = "",
    protocol: ConnectionProtocol,
    onIpChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onTokenChange: (String) -> Unit = {},
    onProtocolChange: (ConnectionProtocol) -> Unit,
    onConnect: () -> Unit,
    onCancel: () -> Unit = {},
    onScanQr: () -> Unit,
    isConnecting: Boolean,
    isConnected: Boolean = false,
    isRegistered: Boolean,
    lastServer: String
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "cardGradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            shadowElevation = 16.dp.toPx()
            shape = RoundedCornerShape(28.dp)
            clip = true
        },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface
                        ),
                        start = androidx.compose.ui.geometry.Offset(gradientOffset, 0f),
                        end = androidx.compose.ui.geometry.Offset(gradientOffset + 500f, 1000f)
                    )
                )
                .padding(top = 28.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. Protocol / Mode Selector at VERY TOP of the card
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Connection Mode & Protocol",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ConnectionProtocol.WEBSOCKET to "WebSocket",
                            ConnectionProtocol.UDP to "UDP ⚠️ Unencrypted",
                            ConnectionProtocol.TCP to "TCP",
                            ConnectionProtocol.BLUETOOTH to "Bluetooth",
                            ConnectionProtocol.USB_SERIAL to "USB"
                        ).forEach { (prot, label) ->
                            val isSelected = protocol == prot
                            Surface(
                                onClick = { if (!isConnected) onProtocolChange(prot) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.animateContentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = when (protocol) {
                            ConnectionProtocol.WEBSOCKET -> "Reliable full-duplex control with web-based servers."
                            ConnectionProtocol.UDP -> "Ultra-low latency for direct local control."
                            ConnectionProtocol.TCP -> "Direct socket connection for custom setups."
                            ConnectionProtocol.BLUETOOTH -> "Native wireless mouse emulation (direct PC pairing)."
                            ConnectionProtocol.USB_SERIAL -> "Zero-latency wired connection."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // 2. Card Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (protocol == ConnectionProtocol.BLUETOOTH) "Bluetooth Mouse" else "Connect Server",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (lastServer.isNotEmpty() && protocol != ConnectionProtocol.BLUETOOTH) {
                            Text(
                                text = "Last used: $lastServer", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (protocol == ConnectionProtocol.BLUETOOTH) {
                            Text(
                                text = "Direct HID Wireless Pairing",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (protocol == ConnectionProtocol.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Computer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!isRegistered) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Waiting for profile registration", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // 3. Network Configuration OR Bluetooth Discover/Broadcast
                if (protocol == ConnectionProtocol.BLUETOOTH) {
                    // Hide IP/Port/Token inputs and show Bluetooth discover & broadcast controls
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Bluetooth Discovery & Broadcast", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Make phone discoverable or connect to paired PC", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                                putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Discoverable", fontSize = 13.sp)
                                }
                                Button(
                                    onClick = onConnect,
                                    enabled = !isConnecting && isRegistered && !isConnected,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isConnecting) "Broadcasting..." else "Broadcast", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = onIpChange,
                        label = { Text("IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Wifi, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isConnecting && !isConnected,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (protocol == ConnectionProtocol.USB_SERIAL) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!isConnecting && !isConnected) onConnect()
                            }
                        )
                    )

                    AnimatedVisibility(
                        visible = protocol != ConnectionProtocol.USB_SERIAL,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = port.toString(),
                            onValueChange = { it.toIntOrNull()?.let(onPortChange) },
                            label = { Text("Port") },
                            placeholder = { Text("8082") },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.SettingsEthernet, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!isConnecting && !isConnected) onConnect()
                                }
                            ),
                            enabled = !isConnecting && !isConnected,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    // Pairing Token (HMAC-SHA256) input field
                    OutlinedTextField(
                        value = pairingToken,
                        onValueChange = onTokenChange,
                        label = { Text("Pairing Token (HMAC-SHA256)") },
                        placeholder = { Text("Enter security pairing token") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!isConnecting && !isConnected) onConnect()
                            }
                        ),
                        enabled = !isConnecting && !isConnected,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. Single Consolidated Material 3 Connection Button
                if (isConnecting) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        SweepGradientGlowRing(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            glowRadius = 4.dp,
                            colors = listOf(
                                MaterialTheme.colorScheme.onError,
                                MaterialTheme.colorScheme.onError.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.onError
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Connecting... (Tap to Cancel)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        enabled = isRegistered && !isConnected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) Success else MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        if (isConnected) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connected", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        } else {
                            Icon(if (protocol == ConnectionProtocol.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (protocol == ConnectionProtocol.BLUETOOTH) "Broadcast Mouse" else "Connect", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    }
                }

                if (protocol != ConnectionProtocol.BLUETOOTH) {
                    OutlinedButton(
                        onClick = onScanQr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        enabled = !isConnecting && isRegistered && !isConnected
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quick Scan QR", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}


// ==========================================
// CONNECTED INFO CARD (replaces connection form when connected)
// ==========================================

@Composable
fun ConnectedInfoCard(
    serverIp: String,
    serverPort: Int,
    protocol: ConnectionProtocol,
    serverName: String,
    isMouseControlEnabled: Boolean,
    onToggleMouseControl: (Boolean) -> Unit,
    onDisconnect: () -> Unit
) {
    val connectedGreen = Success

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header with green badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated green dot
                val infiniteTransition = rememberInfiniteTransition(label = "connectedPulse")
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .background(connectedGreen, CircleShape)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Connected to ${serverName.ifBlank { serverIp }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = connectedGreen
                    )
                    Text(
                        text = "Session active \u2022 Mouse control enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Connection details chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (protocol != ConnectionProtocol.BLUETOOTH) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = connectedGreen.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("IP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("$serverIp:$serverPort", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = connectedGreen.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Protocol", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(protocol.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = connectedGreen.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Device", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(android.os.Build.MODEL, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            HorizontalDivider(color = connectedGreen.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isMouseControlEnabled) Icons.Default.Sensors else Icons.Default.SensorsOff,
                        contentDescription = null,
                        tint = if (isMouseControlEnabled) connectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = if (isMouseControlEnabled) "Transmission Active" else "Transmission Paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isMouseControlEnabled) "Phone sensors control the PC mouse" else "Sensors are temporarily paused",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isMouseControlEnabled,
                    onCheckedChange = onToggleMouseControl,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = connectedGreen,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            HorizontalDivider(color = connectedGreen.copy(alpha = 0.25f))

            // Prominent Disconnect button
            Button(
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeError,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disconnect", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ==========================================
// QUICK ACTIONS ROW
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
        QuickActionButton(onClick = onCalibrate, icon = Icons.Default.Build, label = "Calibrate", color = MaterialTheme.colorScheme.primary)
        QuickActionButton(onClick = onGestureStudio, icon = Icons.Default.Gesture, label = "Gestures", color = MaterialTheme.colorScheme.secondary)
        QuickActionButton(onClick = onVoiceCommands, icon = Icons.Default.Mic, label = "Voice", color = Info)
        QuickActionButton(onClick = onNetworkDiscovery, icon = Icons.Default.Wifi, label = "Network", color = Success)
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

@Composable
fun MouseMotionPreviewCard(
    yaw: Float,
    pitch: Float,
    roll: Float,
    isActive: Boolean,
    isDemoMode: Boolean,
    isRegistered: Boolean
) {
    val previewPulse = rememberInfiniteTransition(label = "previewPulse")
    val demoX by previewPulse.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "demoX"
    )
    val demoY by previewPulse.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "demoY"
    )

    val boxX = if (isActive) ((yaw / 90f) + 0.5f).coerceIn(0.15f, 0.85f) else demoX
    val boxY = if (isActive) ((pitch / 90f) + 0.5f).coerceIn(0.15f, 0.85f) else demoY

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mouse movement preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (isActive) "The green square follows device orientation in real time."
                        else "Demo mode preview is animating until the live session is ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = {
                        Text(
                            when {
                                isActive -> "Live"
                                isDemoMode -> "Demo"
                                isRegistered -> "Approved"
                                else -> "Waiting for approval"
                            }
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.Center)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .align(Alignment.Center)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopStart)
                            .graphicsLayer {
                                translationX = boxX * 100.dp.toPx()
                                translationY = boxY * 100.dp.toPx()
                            }
                            .background(
                                color = if (isActive) Success else Info,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SensorStatusPill("Yaw ${String.format(Locale.US, "%.1f", yaw)}°")
                SensorStatusPill("Pitch ${String.format(Locale.US, "%.1f", pitch)}°")
                SensorStatusPill("Roll ${String.format(Locale.US, "%.1f", roll)}°")
                if (!isActive) SensorStatusPill("Demo motion")
            }
        }
    }
}

@Composable
private fun SensorStatusPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SmoothTrackingHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("Smooth tracking enabled", fontWeight = FontWeight.Bold)
                Text(
                    "Movement is smoothed, tiny hand jitter is filtered, and click/scroll gestures use stronger thresholds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        StatsCard(title = "Clicks", value = clicks.toString(), icon = Icons.Default.Mouse, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        StatsCard(title = "Scrolls", value = scrolls.toString(), icon = Icons.Default.SwapVert, color = Success, modifier = Modifier.weight(1f))
        StatsCard(title = "Session", value = sessionDuration, icon = Icons.Default.Timer, color = Warning, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatsCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    var previousValue by remember { mutableStateOf(value) }
    var triggerFlash by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value != previousValue && (title == "Clicks" || title == "Scrolls")) {
            triggerFlash = true
            kotlinx.coroutines.delay(200)
            triggerFlash = false
        }
        previousValue = value
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (triggerFlash) color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 150),
        label = "flashAnimation"
    )

    val scale by animateFloatAsState(
        targetValue = if (triggerFlash) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scaleAnimation"
    )

    Card(
        modifier = modifier.scale(scale),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.02f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedContent(targetState = value, label = "valueAnimation") { targetValue ->
                Text(text = targetValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
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
                    Icon(if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull, contentDescription = null, tint = Success)
                    Text("$batteryLevel%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Battery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    GlassmorphicNeonProgressBar(progress = cpuUsage / 100f, modifier = Modifier.width(40.dp), height = 4.dp, colors = listOf(MaterialTheme.colorScheme.primary, Info))
                    Text("$cpuUsage%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("CPU", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    GlassmorphicNeonProgressBar(progress = memoryUsage / 100f, modifier = Modifier.width(40.dp), height = 4.dp, colors = listOf(Success, MaterialTheme.colorScheme.primary))
                    Text("$memoryUsage%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("RAM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("$frameRate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Success)
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

private fun Long.toReadableDuration(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

// ==========================================
// LIVE MOTION CENTER CARD
// ==========================================

@Composable
fun LiveMotionCenterCard(
    sensorState: HomeViewModel.SensorState,
    uiState: HomeViewModel.HomeUiState,
    accent: Color
) {
    val motionLevel = (sensorState.currentSpeed * 12f).coerceIn(0f, 1f)
    val motionX = sensorState.lastDx.coerceIn(-24f, 24f)
    val motionY = sensorState.lastDy.coerceIn(-24f, 24f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Live motion center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Shows the live cursor-driving sensor values used by the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text(if (sensorState.isActive) "Tracking" else "Idle") }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = motionX.dp.toPx()
                            translationY = motionY.dp.toPx()
                            val s = 0.8f + motionLevel * 0.7f
                            scaleX = s
                            scaleY = s
                        }
                        .background(accent, RoundedCornerShape(8.dp))
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetricChip(label = "Speed", value = String.format(Locale.US, "%.2f", sensorState.currentSpeed))
                MiniMetricChip(label = "FPS", value = sensorState.fps.toString())
                MiniMetricChip(label = "Mode", value = uiState.controlMode)
                MiniMetricChip(label = "Battery", value = "${uiState.batteryLevel}%")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorAxisCard(label = "Dx", value = sensorState.lastDx, color = Info, modifier = Modifier.weight(1f))
                SensorAxisCard(label = "Dy", value = sensorState.lastDy, color = Success, modifier = Modifier.weight(1f))
                SensorAxisCard(label = "Samples", value = sensorState.sampleCount.toFloat(), color = Warning, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniMetricChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SensorAxisCard(
    label: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(String.format(Locale.US, "%.2f", value), fontFamily = FontFamily.Monospace, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DomainSummaryCard(
    uiState: HomeViewModel.HomeUiState,
    isConnected: Boolean
) {
    val profile = uiState.mouseProfile
    val prefs = uiState.appPreferences
    val stats = uiState.mouseStatistics
    val connection = uiState.connectionConfig

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Dataset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Domain model snapshot", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Data and domain objects feeding the UI.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                AssistChip(onClick = { }, label = { Text(if (isConnected) "Live" else "Saved") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MiniMetricChip("Sensitivity", String.format(Locale.US, "%.2f", profile.sensitivity))
                MiniMetricChip("Clicks", stats.totalClicks.toString())
                MiniMetricChip("Scrolls", stats.totalScrolls.toString())
                MiniMetricChip("Theme", prefs.theme)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MiniMetricChip("IP", connection.ip.ifBlank { "unset" })
                MiniMetricChip("Port", connection.port.toString())
                MiniMetricChip("Lang", prefs.language)
                MiniMetricChip("Auto connect", if (connection.autoReconnect) "On" else "Off")
            }
        }
    }
}

@Composable
private fun ServiceHubCard(
    presentationState: PresentationModeService.PresentationState,
    transferState: TransferState,
    transferFolderPath: String,
    onTogglePresentation: () -> Unit,
    onClearPresentationOverlay: () -> Unit,
    onClearTransfers: () -> Unit,
    isConnected: Boolean,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Service hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Live presentation tools and file transfer state are wired into the dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text(if (isConnected) "Connected" else "Waiting for approval") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricChip("Presentation", if (presentationState.isActive) "Live" else "Idle")
                MiniMetricChip("Slides", if (presentationState.totalSlides > 0) "${presentationState.currentSlide}/${presentationState.totalSlides}" else "0/0")
                MiniMetricChip("Annotations", presentationState.annotations.size.toString())
                MiniMetricChip("Transfers", transferState.queueSize.toString())
            }

            Text(
                text = if (presentationState.isActive) {
                    "Presentation mode is active for ${presentationState.elapsedTime.toReadableDuration()} with ${presentationState.annotations.size} annotations."
                } else {
                    "Presentation mode is ready to turn on from this screen."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Transfer folder: $transferFolderPath",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            if (transferState.currentTransfer != null) {
                Text(
                    text = "Active transfer: ${transferState.currentTransfer.fileName} (${transferState.currentTransfer.progress.toInt()}%)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTogglePresentation,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                ) {
                    Text(if (presentationState.isActive) "Stop Presentation" else "Start Presentation")
                }
                OutlinedButton(onClick = onClearPresentationOverlay) {
                    Text("Clear Overlay")
                }
                OutlinedButton(
                    onClick = onClearTransfers,
                    enabled = transferState.completedTransfers.isNotEmpty()
                ) {
                    Text("Clear Transfers")
                }
            }

            if (transferState.completedTransfers.isNotEmpty()) {
                Text(
                    text = "Completed transfers: ${transferState.completedTransfers.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomModeDialog(
    modeToEdit: CustomControlMode? = null,
    onDismiss: () -> Unit,
    onSave: (CustomControlMode) -> Unit
) {
    var name by remember { mutableStateOf(modeToEdit?.name ?: "") }
    var moveSource by remember { mutableStateOf(modeToEdit?.moveSource ?: "gyro") }
    var scrollSource by remember { mutableStateOf(modeToEdit?.scrollSource ?: "z_axis") }
    var clickSource by remember { mutableStateOf(modeToEdit?.clickSource ?: "roll_tilt") }
    var sensitivity by remember { mutableFloatStateOf(modeToEdit?.sensitivity ?: 1.0f) }
    var rollClickThreshold by remember { mutableFloatStateOf(modeToEdit?.rollClickThreshold ?: 45f) }
    var zScrollThreshold by remember { mutableFloatStateOf(modeToEdit?.zScrollThreshold ?: 4.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (modeToEdit == null) "Create Custom Mode" else "Edit Custom Mode") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Mode Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Move Source dropdown
                Text("Cursor Movement Source", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "gyro" to "Gyroscope Tilt (Point to move)",
                        "linear_accel" to "Linear Translation (Slide phone in air)",
                        "touchpad" to "Touchpad (Drag on screen)"
                    ).forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { moveSource = value }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = moveSource == value, onClick = { moveSource = value })
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }

                // Scroll Source
                Text("Scroll Trigger", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "z_axis" to "Push/Pull Phone (Along Z-Axis)",
                        "none" to "No Scroll Gesture"
                    ).forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scrollSource = value }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = scrollSource == value, onClick = { scrollSource = value })
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }

                // Click Source
                Text("Click Trigger", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "roll_tilt" to "Tilt/Roll Phone (Left for Left-Click, Right for Right-Click)",
                        "none" to "No Tilt Clicks"
                    ).forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { clickSource = value }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = clickSource == value, onClick = { clickSource = value })
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }

                // Sensitivity Slider
                Column {
                    Text("Sensitivity: ${"%.1f".format(sensitivity)}x", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.2f..3.0f,
                        steps = 28
                    )
                }

                if (clickSource == "roll_tilt") {
                    Column {
                        Text("Roll Click Threshold: ${rollClickThreshold.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Slider(
                            value = rollClickThreshold,
                            onValueChange = { rollClickThreshold = it },
                            valueRange = 15f..60f,
                            steps = 9
                        )
                    }
                }

                if (scrollSource == "z_axis") {
                    Column {
                        Text("Push/Pull Scroll Sensitivity: ${"%.1f".format(zScrollThreshold)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Slider(
                            value = zScrollThreshold,
                            onValueChange = { zScrollThreshold = it },
                            valueRange = 1.0f..8.0f,
                            steps = 14
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val id = modeToEdit?.id ?: "custom_${System.currentTimeMillis()}"
                        onSave(
                            CustomControlMode(
                                id = id,
                                name = name.trim(),
                                moveSource = moveSource,
                                scrollSource = scrollSource,
                                clickSource = clickSource,
                                sensitivity = sensitivity,
                                rollClickThreshold = rollClickThreshold,
                                zScrollThreshold = zScrollThreshold,
                                isCustom = true
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmbeddedTouchpad(
    connectionManager: ConnectionManager,
    modifier: Modifier = Modifier
) {
    var lastX by remember { mutableStateOf(0f) }
    var lastY by remember { mutableStateOf(0f) }
    val sensitivity = 1.3f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Touchpad Surface",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Slide to move cursor • Tap to click",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
