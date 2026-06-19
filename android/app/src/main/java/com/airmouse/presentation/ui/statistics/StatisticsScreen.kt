package com.airmouse.presentation.ui.statistics

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
fun StatisticsScreen(
    navigationActions: NavigationActions,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showExportDialog(true) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { viewModel.showResetDialog(true) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Reset")
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
            // Time Range Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TimeRange.entries.forEach { range ->
                            FilterChip(
                                selected = uiState.timeRange == range,
                                onClick = { viewModel.updateTimeRange(range) },
                                label = { Text(range.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Overview Stats
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Session Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCircle(
                                value = formatDuration(uiState.sessionTime),
                                label = "Session Time",
                                color = Color(0xFF2196F3)
                            )
                            StatCircle(
                                value = uiState.gesturesDetected.toString(),
                                label = "Total Gestures",
                                color = Color(0xFF4CAF50)
                            )
                            StatCircle(
                                value = "${String.format(Locale.getDefault(), "%.1f", uiState.calibrationSuccessRate)}%",
                                label = "Calibration Rate",
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            // Gesture Distribution
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Gesture Distribution",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.gestureBreakdown.isEmpty()) {
                            Text("No gesture data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                uiState.gestureBreakdown.forEach { (gesture, count) ->
                                    GestureStatItem(
                                        label = gesture,
                                        value = count,
                                        color = getGestureColor(gesture)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                val total = uiState.gestureBreakdown.values.sum().toFloat()
                                if (total == 0f) return@Canvas
                                val colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF2196F3),
                                    Color(0xFFFF9800),
                                    Color(0xFF9C27B0),
                                    Color(0xFFE91E63),
                                    Color(0xFF00BCD4)
                                )
                                var startAngle = -90f
                                uiState.gestureBreakdown.values.forEachIndexed { index, count ->
                                    val sweepAngle = (count / total) * 360f
                                    drawArc(
                                        color = colors[index % colors.size],
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = true,
                                        topLeft = Offset(size.width / 4, 0f),
                                        size = androidx.compose.ui.geometry.Size(size.width / 2, size.height)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                        }
                    }
                }
            }

            // Movement Stats
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Movement Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox("Distance", String.format(Locale.getDefault(), "%.1f", uiState.totalDistanceMoved), Color(0xFF2196F3))
                            StatBox("Avg Speed", String.format(Locale.getDefault(), "%.1f", uiState.averageSpeed), Color(0xFF4CAF50))
                            StatBox("Peak Speed", String.format(Locale.getDefault(), "%.1f", uiState.peakSpeed), Color(0xFFFF9800))
                            StatBox("Movements", uiState.totalMovements.toString(), Color(0xFF9C27B0))
                        }
                    }
                }
            }

            // Connection Stats
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Connection Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox("Attempts", uiState.connectionAttempts.toString(), Color(0xFF2196F3))
                            StatBox("Successful", uiState.successfulConnections.toString(), Color(0xFF4CAF50))
                            StatBox("Failed", uiState.failedConnections.toString(), Color(0xFFF44336))
                            StatBox("Avg Ping", "${uiState.averagePing}ms", Color(0xFFFF9800))
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val progressValue = if (uiState.connectionAttempts > 0)
                            uiState.successfulConnections.toFloat() / uiState.connectionAttempts
                        else 0f

                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Daily Activity
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Activity (Clicks)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.dailyStats.isEmpty()) {
                            Text("No daily data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val maxClicks = uiState.dailyStats.maxOfOrNull { it.clicks } ?: 1

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                val width = size.width
                                val height = size.height
                                val stepX = width / (uiState.dailyStats.size - 1).coerceAtLeast(1)

                                val points = uiState.dailyStats.mapIndexed { i, stat ->
                                    Offset(
                                        x = i * stepX,
                                        y = height - (stat.clicks.toFloat() / maxClicks) * height
                                    )
                                }

                                for (i in 0 until points.size - 1) {
                                    drawLine(
                                        color = Color(0xFF00BCD4),
                                        start = points[i],
                                        end = points[i + 1],
                                        strokeWidth = 3f
                                    )
                                }

                                val path = Path().apply {
                                    moveTo(0f, height)
                                    points.forEach { lineTo(it.x, it.y) }
                                    lineTo(width, height)
                                    close()
                                }
                                drawPath(path, Color(0xFF00BCD4).copy(alpha = 0.2f))

                                drawLine(
                                    color = Color.White.copy(alpha = 0.3f),
                                    start = Offset(0f, height),
                                    end = Offset(width, height),
                                    strokeWidth = 1f
                                )
                            }
                        }
                    }
                }
            }

            // Performance Metrics
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Performance Metrics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox("Battery", "${uiState.batteryUsage}%", Color(0xFF4CAF50))
                            StatBox("CPU", String.format(Locale.getDefault(), "%.1f%%", uiState.cpuUsage), Color(0xFF2196F3))
                            StatBox("Memory", String.format(Locale.getDefault(), "%.1f%%", uiState.memoryUsage), Color(0xFFFF9800))
                            StatBox("Temp", String.format(Locale.getDefault(), "%.1f°C", uiState.temperature), Color(0xFFF44336))
                        }
                    }
                }
            }

            // Session Info
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Session Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Session Start", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                        .format(Date(uiState.sessionStartTime)),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Last Calibration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (uiState.lastCalibrationTime > 0)
                                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                            .format(Date(uiState.lastCalibrationTime))
                                    else "Never",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Calibrations", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${uiState.calibrationCount} times", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showResetDialog(false) },
            title = { Text("Reset Statistics") },
            text = { Text("Are you sure you want to reset all statistics? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resetStatistics() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showResetDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showExportDialog(false) },
            title = { Text("Export Statistics") },
            text = { Text("Export statistics to a text file?") },
            confirmButton = {
                TextButton(onClick = { viewModel.exportStatistics() }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showExportDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==================== UI COMPONENTS ====================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        content()
    }
}

@Composable
fun StatCircle(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GestureStatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getGestureColor(gesture: String): Color {
    return when (gesture.lowercase(Locale.getDefault())) {
        "click" -> Color(0xFF4CAF50)
        "double click" -> Color(0xFF2196F3)
        "right click" -> Color(0xFFFF9800)
        "scroll" -> Color(0xFF9C27B0)
        else -> Color(0xFFE91E63)
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
    else String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
}

// --- Target ViewModel interface definition for standalone compilation ---
