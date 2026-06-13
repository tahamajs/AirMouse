// app/src/main/java/com/airmouse/presentation/ui/home/HomeScreen.kt
package com.airmouse.presentation.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.utils.QRScanner
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigationActions: NavigationActions,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // QR scanner launcher
    val qrScanner = remember { QRScanner() }
    var showQrPermissionDialog by remember { mutableStateOf(false) }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            qrScanner.startScan()
        } else {
            showQrPermissionDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Air Mouse Pro", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { navigationActions.navigateTo("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with connection status
                item {
                    ConnectionStatusCard(
                        isConnected = uiState.isConnected,
                        connectionQuality = uiState.connectionQuality,
                        serverIp = uiState.serverIp,
                        onDisconnect = { viewModel.disconnect() }
                    )
                }

                // Connection controls
                item {
                    ConnectionControlsCard(
                        ip = uiState.serverIp,
                        port = uiState.serverPort,
                        onIpChange = { viewModel.updateIp(it) },
                        onPortChange = { viewModel.updatePort(it) },
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
                        isConnecting = uiState.isConnecting
                    )
                }

                // Calibration status & action
                item {
                    CalibrationStatusCard(
                        isCalibrated = uiState.isCalibrated,
                        onCalibrate = { navigationActions.navigateTo("calibration") }
                    )
                }

                // Sensor preview (orientation ball)
                item {
                    SensorPreviewCard(
                        roll = uiState.roll,
                        yaw = uiState.yaw,
                        pitch = uiState.pitch
                    )
                }

                // Gesture stats summary
                item {
                    GestureStatsCard(
                        clicks = uiState.clickCount,
                        scrolls = uiState.scrollCount,
                        rightClicks = uiState.rightClickCount,
                        doubleClicks = uiState.doubleClickCount
                    )
                }

                // Live log (optional)
                item {
                    LiveLogCard(
                        logs = uiState.logs,
                        onClear = { viewModel.clearLogs() }
                    )
                }
            }

            // QR scan result handler
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
                    // Optionally show a snackbar
                }
            }

            // Permission denied dialog
            if (showQrPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showQrPermissionDialog = false },
                    title = { Text("Camera Permission") },
                    text = { Text("Camera permission is required to scan QR codes.") },
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

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    connectionQuality: Int,
    serverIp: String,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated connection indicator
            val scale by animateFloatAsState(
                targetValue = if (isConnected) 1.2f else 1f,
                animationSpec = tween(500)
            )
            Icon(
                if (isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                contentDescription = "Connection",
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale),
                tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isConnected) "Connected to $serverIp" else "Disconnected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isConnected) {
                Text(
                    text = "Signal quality: $connectionQuality%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect")
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
    isConnecting: Boolean
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
                text = "Connect to PC",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ip,
                onValueChange = onIpChange,
                label = { Text("IP Address") },
                placeholder = { Text("192.168.1.100") },
                leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isConnecting
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = port.toString(),
                onValueChange = { it.toIntOrNull()?.let(onPortChange) },
                label = { Text("Port") },
                placeholder = { Text("8080") },
                leadingIcon = { Icon(Icons.Default.SettingsEthernet, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardType = KeyboardType.Number,
                enabled = !isConnecting
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onConnect,
                    enabled = !isConnecting,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                }

                OutlinedButton(
                    onClick = onScanQr,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan QR")
                }
            }
        }
    }
}

@Composable
fun CalibrationStatusCard(isCalibrated: Boolean, onCalibrate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCalibrated)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isCalibrated) "✓ Sensors Calibrated" else "⚠️ Calibration Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isCalibrated)
                        "Your sensors are ready for accurate tracking"
                    else
                        "For best accuracy, please calibrate your sensors",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onCalibrate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCalibrated)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isCalibrated) "Recalibrate" else "Calibrate Now")
            }
        }
    }
}

@Composable
fun SensorPreviewCard(roll: Float, yaw: Float, pitch: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Live Sensor Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 3D orientation ball (simple circle with dot)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .offset(
                            x = (yaw / 90f * 40).dp,
                            y = (roll / 90f * 40).dp
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SensorValue("Roll", roll)
                SensorValue("Pitch", pitch)
                SensorValue("Yaw", yaw)
            }
        }
    }
}

@Composable
fun SensorValue(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%.1f°".format(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun GestureStatsCard(
    clicks: Int,
    scrolls: Int,
    rightClicks: Int,
    doubleClicks: Int
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
                text = "Gesture Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Clicks", clicks, Icons.Default.Mouse)
                StatItem("Scrolls", scrolls, Icons.Default.SwapVert)
                StatItem("Right", rightClicks, Icons.Default.Menu)
                StatItem("Double", doubleClicks, Icons.Default.Repeat)
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LiveLogCard(logs: List<String>, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "No events yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(logs.takeLast(20)) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// Inside HomeScreen composable, after Scaffold content
Box(modifier = Modifier.fillMaxSize()) {
    // Your existing LazyColumn or main content
    LazyColumn(...) { ... }

    ConnectionStatusBadge(
        connectionManager = viewModel.connectionManager,
        modifier = Modifier.align(Alignment.TopEnd)
    )
}