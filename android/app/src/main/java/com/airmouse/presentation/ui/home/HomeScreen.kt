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
import androidx.compose.ui.graphics.Color
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
import com.airmouse.utils.QRScanner
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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
    var serverPort by remember { mutableIntStateOf(homeUiState.serverPort.takeIf { it > 0 } ?: 8081) }
    var serverName by remember { mutableStateOf(homeUiState.userPreferences.serverName.ifBlank { "Air Mouse Pro" }) }
    var showQrPermissionDialog by remember { mutableStateOf(false) }
    var pendingUserName by rememberSaveable { mutableStateOf(homeUiState.userName) }
    var showRegistrationDialog by rememberSaveable {
        mutableStateOf(homeUiState.userName.isBlank() && !homeViewModel.hasRegisteredUser())
    }
    var showCalibrationRequiredDialog by remember { mutableStateOf(false) }

    val isRegistered = homeViewModel.hasRegisteredUser() || homeUiState.userName.isNotBlank()
    val selectedProtocol = homeUiState.selectedProtocol
    val isConnectionActive = homeUiState.connectionStatus == com.airmouse.domain.model.ConnectionStatus.CONNECTED
    val isConnectionPending = homeUiState.isConnecting
    val isAutoConnectEnabled = homeUiState.isAutoConnectEnabled

    val approvalCountdownText = remember(isConnectionPending, approvalCountdownMs) {
        if (!isConnectionPending || approvalCountdownMs <= 0L) "" else {
            val seconds = (approvalCountdownMs + 999) / 1000
            "Server approval timeout in ${seconds}s"
        }
    }
    val connectionStatusText = when {
        isConnectionActive && homeUiState.isCalibrated -> "Connected"
        isConnectionActive -> "Approved"
        isConnectionPending -> "Server approving"
        homeUiState.connectionStatus == com.airmouse.domain.model.ConnectionStatus.ERROR -> "Approval needed"
        homeUiState.isCalibrated -> "Approved"
        else -> "Approval needed"
    }
    val statusAccent = when {
        isConnectionActive && homeUiState.isCalibrated -> Color(0xFF10B981)
        isConnectionPending -> Color(0xFFF59E0B)
        homeUiState.connectionStatus == com.airmouse.domain.model.ConnectionStatus.ERROR -> Color(0xFFEF4444)
        homeUiState.isCalibrated -> Color(0xFF3B82F6)
        else -> Color(0xFF64748B)
    }
    val quietConnectedMode = isConnectionActive && homeUiState.isCalibrated

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
        } else {
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



    Scaffold(
        topBar = {
            HomeTopBar(
                onDrawerClick = { onOpenDrawer?.invoke() },
                onSearchClick = { navigationActions.navigateTo(Destinations.NetworkDiscovery.route) },
                onMenuItemClick = { route -> navigationActions.navigateTo(route) },
                isConnected = isConnectionActive,
                statusText = connectionStatusText
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isConnectionActive,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { homeViewModel.disconnect() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = MaterialTheme.colorScheme.onError)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (quietConnectedMode) Color(0xFF05070B) else MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Connection Status Banner
                item {
                    StateOverviewBanner(
                        isConnected = isConnectionActive,
                        isCalibrated = homeUiState.isCalibrated,
                        needsRecalibration = homeUiState.calibrationState.needsRecalibration,
                        isConnecting = isConnectionPending,
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

                // 2. Primary Connection Toggle
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

                // Control Mode Selector Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Select Control Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            val modes = listOf(
                                Triple("motion", "Motion Mouse", Icons.Default.Sensors),
                                Triple("touchpad", "Touchpad", Icons.Default.TouchApp),
                                Triple("arm_movement", "Arm Gestures", Icons.Default.AccessibilityNew)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                modes.forEach { (modeKey, modeName, icon) ->
                                    val isSelected = homeUiState.controlMode == modeKey
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { homeViewModel.setControlMode(modeKey) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = modeName,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = modeName,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                textAlign =  TextAlign.Center
                                            )
                                        }
                                    }
                                }
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

                // 3. Essential Device Details & Connection Setup Card
                item {
                    ConnectionControlsCard(
                        ip = serverIp,
                        port = serverPort,
                        protocol = selectedProtocol,
                        onIpChange = {
                            serverIp = it
                            homeViewModel.updateIp(it)
                        },
                        onPortChange = {
                            serverPort = it
                            homeViewModel.updatePort(it)
                        },
                        onProtocolChange = { homeViewModel.updateConnectionProtocol(it) },
                        onConnect = { toggleConnection() },
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
                        isRegistered = isRegistered,
                        lastServer = homeUiState.connectionConfig.ip.ifBlank { "Not set" }
                    )
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

            if (quietConnectedMode) {
                ConnectedQuietModeOverlay(
                    connectionStatusText = connectionStatusText,
                    serverName = serverName,
                    statusAccent = statusAccent
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
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                accentColor.copy(alpha = 0.12f)
            } else if (isAutoConnectEnabled) {
                Color(0xFFF59E0B).copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(24.dp),
        border = if (isConnected) {
            BorderStroke(2.dp, accentColor)
        } else if (isAutoConnectEnabled) {
            BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                        color = if (isConnected) accentColor else if (isAutoConnectEnabled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
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
                        color = if (isAutoConnectEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isAutoConnectEnabled,
                        onCheckedChange = { onToggleAutoConnect() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
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
                        isConnected -> Color(0xFFEF4444)
                        isConnecting -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    },
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                enabled = !isConnecting
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
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
                    color = Color(0xFFF59E0B),
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
                    color = Color(0xFF10B981),
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
            isConnected -> Color(0xFF10B981)
            isConnecting -> Color(0xFFF59E0B)
            isAutoConnectEnabled -> Color(0xFFF59E0B)
            else -> Color(0xFFEF4444)
        },
        animationSpec = tween(500),
        label = "statusColor"
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .scale(if (isConnected || isConnecting) pulse else 1f),
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
// QUIET CONNECTED MODE OVERLAY
// ==========================================

@Composable
private fun ConnectedQuietModeOverlay(
    connectionStatusText: String,
    serverName: String,
    statusAccent: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f))
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B1016).copy(alpha = 0.94f),
            border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.32f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusAccent, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Quiet connected mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$connectionStatusText to $serverName",
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 12.sp
                    )
                }
            }
        }
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
        isConnected -> Color(0xFF10B981)
        ready -> Color(0xFF38BDF8)
        else -> Color(0xFFF59E0B)
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
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                MiniChartColumn(
                    title = "Accel",
                    values = accelSeries,
                    color = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f)
                )
                MiniChartColumn(
                    title = "Mag",
                    values = magSeries,
                    color = Color(0xFFF59E0B),
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
        isConnected && isCalibrated -> Color(0xFF10B981)
        isConnected -> Color(0xFF3B82F6)
        else -> Color(0xFFF59E0B)
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
        isConnected -> Color(0xFF10B981)
        ready -> Color(0xFF3B82F6)
        else -> Color(0xFFF59E0B)
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
                            color = if (isConnected) Color(0xFF10B981) else Color(0xFFF59E0B)
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
        isConnecting -> "Waiting for approval"
        isCalibrated -> "Calibration complete"
        needsRecalibration -> "Approved"
        else -> "Waiting for approval"
    }
    val detail = when {
        isConnected && isCalibrated -> "Approved and connected. Touchpad, gestures, and mouse control are active."
        isConnecting -> "Waiting for approval from the desktop."
        isCalibrated -> "Approved for pairing. Connect whenever you are ready."
        needsRecalibration -> "Approved, but recalibration is recommended before the next session."
        else -> "Waiting for approval to start a session."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                    color = Color(0xFFF59E0B)
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
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
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
                Color(0xFF0F766E).copy(alpha = 0.16f)
            } else {
                Color(0xFF334155).copy(alpha = 0.16f)
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
                tint = if (enabled) Color(0xFF5EEAD4) else Color(0xFF94A3B8)
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
        isConnected && isCalibrated -> Color(0xFF10B981)
        isConnecting -> Color(0xFFF59E0B)
        needsRecalibration -> Color(0xFFF97316)
        isCalibrated -> Color(0xFF38BDF8)
        else -> Color(0xFFEF4444)
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
                        needsRecalibration -> Color(0xFFF97316)
                        isCalibrated -> Color(0xFF10B981)
                        else -> Color(0xFFF59E0B)
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
                !isRegistered -> Color(0xFF7C3AED).copy(alpha = 0.14f)
                !isCalibrated -> Color(0xFFF59E0B).copy(alpha = 0.14f)
                !isConnected -> Color(0xFF38BDF8).copy(alpha = 0.14f)
                else -> Color(0xFF10B981).copy(alpha = 0.14f)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    Card(
        modifier = modifier.height(88.dp),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
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
// CONNECTION CONTROLS CARD
// ==========================================

@Composable
fun ConnectionControlsCard(
    ip: String,
    port: Int,
    protocol: ConnectionProtocol,
    onIpChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onProtocolChange: (ConnectionProtocol) -> Unit,
    onConnect: () -> Unit,
    onScanQr: () -> Unit,
    isConnecting: Boolean,
    isRegistered: Boolean,
    lastServer: String
) {
    val focusManager = LocalFocusManager.current
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

            if (!isRegistered) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = { Text("Waiting for approval") }
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
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (!isConnecting) onConnect()
                    }
                ),
                enabled = !isConnecting,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Connection type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = protocol == ConnectionProtocol.UDP,
                    onClick = { onProtocolChange(ConnectionProtocol.UDP) },
                    label = { Text("UDP") }
                )
                FilterChip(
                    selected = protocol == ConnectionProtocol.WEBSOCKET,
                    onClick = { onProtocolChange(ConnectionProtocol.WEBSOCKET) },
                    label = { Text("WebSocket") }
                )
                FilterChip(
                    selected = protocol == ConnectionProtocol.TCP,
                    onClick = { onProtocolChange(ConnectionProtocol.TCP) },
                    label = { Text("TCP") }
                )
            }
            Text(
                text = when (protocol) {
                    ConnectionProtocol.UDP -> "Low-latency datagrams for direct control on the local network."
                    ConnectionProtocol.WEBSOCKET -> "Best for live control and approval-aware sessions."
                    ConnectionProtocol.TCP -> "Direct socket connection for simpler server setups."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onConnect,
                enabled = !isConnecting && isRegistered,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting...")
                } else {
                    Icon(Icons.Default.ConnectWithoutContact, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onScanQr,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isConnecting && isRegistered
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan QR Code", fontWeight = FontWeight.SemiBold)
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
                            .offset(x = (boxX * 100).dp, y = (boxY * 100).dp)
                            .background(
                                color = if (isActive) Color(0xFF22C55E) else Color(0xFF38BDF8),
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
        StatsCard(title = "Clicks", value = clicks.toString(), icon = Icons.Default.Mouse, color = Color(0xFF00BCD4), modifier = Modifier.weight(1f))
        StatsCard(title = "Scrolls", value = scrolls.toString(), icon = Icons.Default.SwapVert, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
        StatsCard(title = "Session", value = sessionDuration, icon = Icons.Default.Timer, color = Color(0xFFFF9800), modifier = Modifier.weight(1f))
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
        targetValue = if (triggerFlash) color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
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
                        .offset(x = motionX.dp, y = motionY.dp)
                        .scale(0.8f + motionLevel * 0.7f)
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
                SensorAxisCard(label = "Dx", value = sensorState.lastDx, color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                SensorAxisCard(label = "Dy", value = sensorState.lastDy, color = Color(0xFF22C55E), modifier = Modifier.weight(1f))
                SensorAxisCard(label = "Samples", value = sensorState.sampleCount.toFloat(), color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
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
