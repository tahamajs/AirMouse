// app/src/main/java/com/airmouse/presentation/ui/battery/BatteryScreen.kt
package com.airmouse.presentation.ui.battery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Refresh
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    navigationActions: NavigationActions,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Monitor") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshBatteryInfo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
            // Battery Level Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = getBatteryColor(uiState.level).copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.BatteryFull,
                            contentDescription = "Battery",
                            modifier = Modifier.size(48.dp),
                            tint = getBatteryColor(uiState.level)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.level}%",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = getBatteryColor(uiState.level)
                        )
                        Text(
                            text = viewModel.getPowerSourceDescription(),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Remaining: ${viewModel.formatRemainingTime(uiState.estimatedRemaining)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Battery Details Card
            item {
                DetailsCard(title = "Battery Details") {
                    DetailRow("Status", uiState.status)
                    DetailRow("Health", uiState.health)
                    DetailRow("Technology", uiState.technology)
                    DetailRow("Voltage", "${uiState.voltage}V")
                    DetailRow("Temperature", "${uiState.temperature}°C")
                    if (uiState.current != 0f) {
                        DetailRow("Current", "${uiState.current}mA")
                    }
                    DetailRow("Charge Counter", "${uiState.chargeCounter}μAh")
                    DetailRow("Energy Counter", "${uiState.energyCounter}nWh")
                }
            }

            // Power Source Card
            item {
                DetailsCard(title = "Power Source") {
                    DetailRow("Plugged", uiState.plugged)
                    DetailRow("Battery Present", if (uiState.isPresent) "Yes" else "No")
                    DetailRow("Battery Saver", if (uiState.batterySaverEnabled) "Enabled" else "Disabled")
                }
            }

            // Temperature History (simplified – you can add a real chart)
            if (uiState.history.isNotEmpty()) {
                item {
                    DetailsCard(title = "Recent History (last minute)") {
                        val lastEntries = uiState.history.takeLast(6)
                        lastEntries.forEach { entry ->
                            DetailRow(
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp)),
                                "${entry.level}% / ${entry.temperature}°C"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun getBatteryColor(level: Int): Color {
    return when {
        level >= 70 -> Color(0xFF4CAF50)
        level >= 30 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}