// app/src/main/java/com/airmouse/presentation/ui/home/HomeScreen.kt
package com.airmouse.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.ConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionStatus = uiState.connectionStatus

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Air Mouse Pro") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (uiState.isActive) viewModel.stopAirMouse() else viewModel.startAirMouse() },
                containerColor = if (uiState.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Text(if (uiState.isActive) "Stop" else "Start")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Card
            item {
                ConnectionCard(
                    ip = uiState.serverIp,
                    port = uiState.serverPort,
                    status = connectionStatus,
                    onIpChange = viewModel::updateIp,
                    onPortChange = viewModel::updatePort,
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() }
                )
            }

            // Calibration Status Card
            item {
                CalibrationCard(
                    progress = uiState.calibrationProgress,
                    calibratedCount = uiState.sensorsCalibrated,
                    totalSensors = uiState.totalSensors,
                    remainingAttempts = uiState.remainingAttempts
                )
            }

            // Sensor Data Card
            item {
                SensorDataCard(
                    yaw = uiState.orientationYaw,
                    pitch = uiState.orientationPitch
                )
            }

            // Gesture Stats Card
            item {
                GestureStatsCard(stats = uiState.gestureStats)
            }

            // Controls Card
            item {
                ControlsCard(
                    isTouchpadMode = uiState.isTouchpadMode,
                    onToggleTouchpad = viewModel::toggleTouchpadMode,
                    aiSmoothingEnabled = uiState.aiSmoothingEnabled,
                    predictiveEnabled = uiState.predictiveEnabled
                )
            }

            // Live Log Card
            if (uiState.logMessages.isNotEmpty()) {
                item {
                    LogCard(
                        messages = uiState.logMessages,
                        onClear = viewModel::clearLogs
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    ip: String,
    port: Int,
    status: ConnectionStatus,
    onIpChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Server Connection", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = ip,
                onValueChange = onIpChange,
                label = { Text("IP Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = port.toString(),
                onValueChange = { onPortChange(it.toIntOrNull() ?: 8080) },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.weight(1f),
                    enabled = status != ConnectionStatus.CONNECTED && status != ConnectionStatus.CONNECTING
                ) {
                    Text(if (status == ConnectionStatus.CONNECTING) "Connecting..." else "Connect")
                }
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    enabled = status == ConnectionStatus.CONNECTED,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: $status",
                color = when (status) {
                    ConnectionStatus.CONNECTED -> Color.Green
                    ConnectionStatus.CONNECTING -> Color.Yellow
                    else -> Color.Red
                },
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CalibrationCard(
    progress: Int,
    calibratedCount: Int,
    totalSensors: Int,
    remainingAttempts: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Calibration Status", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("$calibratedCount / $totalSensors sensors calibrated", fontSize = 12.sp)
            Text("Attempts remaining: $remainingAttempts", fontSize = 12.sp)
        }
    }
}

@Composable
fun SensorDataCard(yaw: Float, pitch: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .rotate(yaw),
                contentAlignment = Alignment.Center
            ) {
                Text("📱", fontSize = 30.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Live Orientation", fontWeight = FontWeight.Bold)
                Text("Yaw: ${"%.1f".format(yaw)}°", fontSize = 14.sp)
                Text("Pitch: ${"%.1f".format(pitch)}°", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun GestureStatsCard(stats: GestureStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gesture Statistics", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Clicks", stats.clicks)
                StatItem("Double", stats.doubleClicks)
                StatItem("Right", stats.rightClicks)
                StatItem("Scrolls", stats.scrolls)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun ControlsCard(
    isTouchpadMode: Boolean,
    onToggleTouchpad: () -> Unit,
    aiSmoothingEnabled: Boolean,
    predictiveEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Controls", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Touchpad Mode")
                Switch(checked = isTouchpadMode, onCheckedChange = { onToggleTouchpad() })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AI Smoothing")
                Switch(checked = aiSmoothingEnabled, onCheckedChange = {})
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Predictive Movement")
                Switch(checked = predictiveEnabled, onCheckedChange = {})
            }
        }
    }
}

@Composable
fun LogCard(messages: List<String>, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Log", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = onClear) { Text("Clear") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                messages.takeLast(10).forEach { msg ->
                    Text(msg, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}