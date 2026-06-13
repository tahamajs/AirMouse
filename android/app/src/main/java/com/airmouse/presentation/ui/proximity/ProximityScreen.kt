package com.airmouse.presentation.ui.proximity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximityScreen(
    navigationActions: NavigationActions,
    viewModel: ProximityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proximity Lock") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Main Control Card
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
                            Column {
                                Text(
                                    text = "Proximity Service",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-lock PC when you walk away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isEnabled,
                                onCheckedChange = viewModel::toggleService
                            )
                        }

                        if (uiState.isEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Divider()

                            Spacer(modifier = Modifier.height(12.dp))

                            // Connected Device
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Connected Device:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    uiState.connectedDevice ?: "None",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Signal Strength
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Signal Strength:", style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                Color(uiState.signalStrength.color),
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(uiState.signalStrength.displayName)
                                }
                            }

                            // RSSI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RSSI:", style = MaterialTheme.typography.bodyMedium)
                                Text("${uiState.rssi} dBm", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }

                            // Current Distance
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Current Distance",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (uiState.currentDistance != null)
                                    String.format("%.2f m", uiState.currentDistance)
                                else
                                    "Calculating...",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isNear) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )

                            // Status
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color(uiState.statusColor).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = uiState.status,
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 12.sp,
                                    color = Color(uiState.statusColor)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isEnabled) {
                // Threshold Settings Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Threshold Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Near Threshold (Unlock)", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = uiState.nearThreshold,
                                onValueChange = viewModel::updateNearThreshold,
                                valueRange = 0.5f..5.0f,
                                steps = 45
                            )
                            Text("${String.format("%.1f", uiState.nearThreshold)} m", fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Far Threshold (Lock)", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = uiState.farThreshold,
                                onValueChange = viewModel::updateFarThreshold,
                                valueRange = 1.0f..10.0f,
                                steps = 90
                            )
                            Text("${String.format("%.1f", uiState.farThreshold)} m", fontSize = 12.sp)
                        }
                    }
                }

                // Calibration Card
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
                                    progress = uiState.calibrationProgress / 100f,
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
                        }
                    }
                }

                // Advanced Settings Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Advanced Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lock PC when far")
                                Switch(
                                    checked = uiState.lockActionEnabled,
                                    onCheckedChange = { viewModel.toggleLockAction(it) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Unlock PC when near")
                                Switch(
                                    checked = uiState.unlockActionEnabled,
                                    onCheckedChange = { viewModel.toggleUnlockAction(it) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vibration on lock/unlock")
                                Switch(
                                    checked = uiState.vibrationOnLock,
                                    onCheckedChange = { viewModel.toggleVibration(it) }
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
                                    onCheckedChange = { viewModel.toggleNotification(it) }
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

                // History Card
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

                                items(uiState.history.take(5)) { entry ->
                                    HistoryEntryItem(entry)
                                }
                            }
                        }
                    }
                }
            }

            // Info Card
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
                            text = "• The app uses Bluetooth RSSI to estimate distance from your PC\n" +
                                    "• When you walk away beyond the FAR threshold, your PC locks automatically\n" +
                                    "• When you return within the NEAR threshold, your PC unlocks\n" +
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
                text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(entry.timestamp)),
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
            text = String.format("%.2f m", entry.distance),
            fontWeight = FontWeight.Bold,
            color = if (entry.isNear) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
        Surface(
            shape = MaterialTheme.shapes.small,
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