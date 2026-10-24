package com.airmouse.presentation.ui.proximity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximityScreen(
    navigationActions: NavigationActions,
    viewModel: ProximityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proximity Lock") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main status card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Proximity Service",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto‑lock PC when you walk away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isEnabled,
                                onCheckedChange = viewModel::toggleService,
                                enabled = uiState.deviceMac.isNotBlank()
                            )
                        }

                        if (uiState.isEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Device:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    uiState.connectedDevice ?: "None",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Signal:", style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(uiState.signalStrength.color))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(uiState.signalStrength.displayName)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RSSI:", style = MaterialTheme.typography.bodyMedium)
                                Text("${uiState.rssi} dBm", fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Distance", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (uiState.currentDistance != null)
                                    String.format(Locale.US, "%.2f m", uiState.currentDistance)
                                else
                                    "Calculating...",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isNear) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(uiState.statusColor).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = uiState.status,
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 12.sp,
                                    color = Color(uiState.statusColor)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.deviceMac.isBlank())
                                    "Select a paired Bluetooth device below to enable proximity auto-lock."
                                else
                                    "Selected: ${uiState.connectedDevice ?: uiState.deviceMac}. Turn on toggle above to start proximity monitoring.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Paired Devices Card (always visible so user can select device)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paired Bluetooth Devices",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.refreshPairedDevices() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh paired devices")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val devices = if (uiState.pairedDevices.isNotEmpty()) {
                            uiState.pairedDevices
                        } else {
                            uiState.bondableDevices.map { dev ->
                                PairedDeviceInfo(
                                    name = try { dev.name ?: dev.address } catch (_: SecurityException) { dev.address },
                                    address = dev.address
                                )
                            }
                        }

                        if (devices.isEmpty()) {
                            Text(
                                text = "No paired devices found. Pair your PC via Bluetooth settings first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(devices) { device ->
                                    val isSelected = device.address == uiState.deviceMac
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                else Color.Transparent
                                            )
                                            .clickable { viewModel.selectDevice(device.address, device.name) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = device.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = device.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isEnabled) {
                // Threshold settings
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Thresholds",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Near (Unlock)", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = uiState.nearThreshold,
                                onValueChange = viewModel::updateNearThreshold,
                                valueRange = 0.5f..5.0f,
                                steps = 45
                            )
                            Text("${String.format(Locale.US, "%.1f", uiState.nearThreshold)} m", fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Far (Lock)", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = uiState.farThreshold,
                                onValueChange = viewModel::updateFarThreshold,
                                valueRange = 1.0f..10.0f,
                                steps = 90
                            )
                            Text("${String.format(Locale.US, "%.1f", uiState.farThreshold)} m", fontSize = 12.sp)
                        }
                    }
                }

                // Calibration
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Calibration",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.isCalibrating) {
                                LinearProgressIndicator(
                                    progress = { uiState.calibrationProgress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    uiState.calibrationStatus,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = { viewModel.calibrate() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isCalibrating
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Calibrate")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (uiState.isCalibrating) "Calibrating..." else "Calibrate Distance")
                            }
                            Text(
                                text = "Place device at 0.5m, 1m, 2m, 3m, and 5m distances when prompted.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Advanced settings
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Actions & Feedback",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lock when far")
                                Switch(
                                    checked = uiState.lockActionEnabled,
                                    onCheckedChange = viewModel::toggleLockAction
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Unlock when near")
                                Switch(
                                    checked = uiState.unlockActionEnabled,
                                    onCheckedChange = viewModel::toggleUnlockAction
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vibrate on lock/unlock")
                                Switch(
                                    checked = uiState.vibrationOnLock,
                                    onCheckedChange = viewModel::toggleVibration
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show notifications")
                                Switch(
                                    checked = uiState.notificationOnLock,
                                    onCheckedChange = viewModel::toggleNotification
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetToDefaults() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset to Defaults")
                            }
                        }
                    }
                }

                // History
                if (uiState.history.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Recent History",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                uiState.history.take(5).forEach { entry ->
                                    HistoryEntryItem(entry)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Information card (always visible)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How It Works",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Uses Bluetooth RSSI to estimate distance from your PC\n" +
                                    "• Walks away beyond FAR threshold → PC locks automatically\n" +
                                    "• Returns within NEAR threshold → PC unlocks\n" +
                                    "• Calibrate for best accuracy in your environment\n" +
                                    "• Make sure Bluetooth is enabled on both devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryEntryItem(entry: ProximityHistoryEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(entry.timestamp)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "RSSI: ${entry.rssi} dBm",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = String.format(Locale.US, "%.2f m", entry.distance),
            fontWeight = FontWeight.Bold,
            color = if (entry.isNear) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (entry.isNear) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFF44336).copy(alpha = 0.2f)
        ) {
            Text(
                text = if (entry.isNear) "Near" else "Far",
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = if (entry.isNear) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
