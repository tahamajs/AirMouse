package com.airmouse.presentation.ui.statistics

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import java.util.Date

// Data Classes
data class StatisticsUiState(
    val sessionTime: Long = 0,
    val sessionStartTime: Long = System.currentTimeMillis(),
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val gesturesDetected: Int = 0,
    val totalDistanceMoved: Float = 0f,
    val averageSpeed: Float = 0f,
    val peakSpeed: Float = 0f,
    val totalMovements: Int = 0,
    val connectionAttempts: Int = 0,
    val successfulConnections: Int = 0,
    val failedConnections: Int = 0,
    val averagePing: Int = 0,
    val lastCalibrationTime: Long = 0,
    val calibrationCount: Int = 0,
    val calibrationSuccessRate: Float = 0f,
    val batteryUsage: Int = 0,
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val temperature: Float = 0f,
    val dailyStats: List<DailyStats> = emptyList(),
    val gestureBreakdown: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val timeRange: TimeRange = TimeRange.TODAY,
    val selectedChart: ChartType = ChartType.GESTURES,
    val showExportDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

data class DailyStats(
    val date: Date,
    val clicks: Int,
    val doubleClicks: Int,
    val rightClicks: Int,
    val scrolls: Int,
    val distance: Float
)

enum class TimeRange(val displayName: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("This Week", 7),
    MONTH("This Month", 30),
    YEAR("This Year", 365),
    ALL_TIME("All Time", 0)
}

enum class ChartType(val displayName: String) {
    GESTURES("Gestures"),
    MOVEMENT("Movement"),
    CONNECTION("Connection"),
    PERFORMANCE("Performance")
}

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
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                TimeRangeSelector(viewModel, uiState)
            }

            // Overview Stats
            item {
                OverviewStatsCard(uiState)
            }

            // Gesture Distribution Chart
            item {
                GestureDistributionChartCard(uiState.gestureBreakdown)
            }

            // Movement Stats
            item {
                MovementStatsCard(uiState)
            }

            // Connection Stats
            item {
                ConnectionStatsCard(uiState)
            }

            // Daily Activity Graph
            item {
                DailyActivityGraphCard(uiState.dailyStats)
            }

            // Performance Metrics
            item {
                PerformanceMetricsCard(uiState)
            }

            // Session Info
            item {
                SessionInfoCard(uiState)
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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

@Composable
fun TimeRangeSelector(viewModel: StatisticsViewModel, uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimeRange.values().forEach { range ->
                FilterChip(
                    selected = uiState.timeRange == range,
                    onClick = { viewModel.updateTimeRange(range) },
                    label = { Text(range.displayName) }
                )
            }
        }
    }
}

@Composable
fun OverviewStatsCard(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Session Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    value = "${String.format("%.1f", uiState.calibrationSuccessRate)}%",
                    label = "Calibration Rate",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun StatCircle(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.2f),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GestureDistributionChartCard(breakdown: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gesture Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                breakdown.forEach { (gesture, count) ->
                    GestureStatItem(
                        label = gesture,
                        value = count,
                        color = getGestureColor(gesture)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pie Chart
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val total = breakdown.values.sum().toFloat()
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
                breakdown.values.forEachIndexed { index, count ->
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

@Composable
fun GestureStatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MovementStatsCard(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
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
                StatBox("Total Distance", String.format("%.1f", uiState.totalDistanceMoved), Color(0xFF2196F3))
                StatBox("Avg Speed", String.format("%.1f", uiState.averageSpeed), Color(0xFF4CAF50))
                StatBox("Peak Speed", String.format("%.1f", uiState.peakSpeed), Color(0xFFFF9800))
                StatBox("Movements", uiState.totalMovements.toString(), Color(0xFF9C27B0))
            }
        }
    }
}

@Composable
fun ConnectionStatsCard(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
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

            LinearProgressIndicator(
                progress = uiState.successfulConnections.toFloat() / max(uiState.connectionAttempts, 1),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DailyActivityGraphCard(stats: List<DailyStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (stats.isEmpty()) {
                Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Line Chart
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val maxClicks = stats.maxOfOrNull { it.clicks } ?: 1
                    val stepX = size.width / (stats.size - 1)

                    val points = stats.mapIndexed { i, stat ->
                        Offset(
                            x = i * stepX,
                            y = size.height - (stat.clicks.toFloat() / maxClicks) * size.height
                        )
                    }

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = Color(0xFFFF5722),
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 3f
                        )
                    }

                    // Area under curve
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, size.height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path, Color(0xFFFF5722).copy(alpha = 0.2f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("Clicks", fontSize = 10.sp, color = Color(0xFFFF5722))
                    Text("Scrolls", fontSize = 10.sp, color = Color(0xFF2196F3))
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricsCard(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
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
                StatBox("CPU", String.format("%.1f%%", uiState.cpuUsage), Color(0xFF2196F3))
                StatBox("Memory", String.format("%.1f%%", uiState.memoryUsage), Color(0xFFFF9800))
                StatBox("Temp", String.format("%.1f°C", uiState.temperature), Color(0xFFF44336))
            }
        }
    }
}

@Composable
fun SessionInfoCard(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
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
                        java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(uiState.sessionStartTime)),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("Last Calibration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (uiState.lastCalibrationTime > 0)
                            java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(uiState.lastCalibrationTime))
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

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getGestureColor(gesture: String): Color {
    return when (gesture.lowercase()) {
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
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

// StatisticsScreen.kt - Use these components:
@Composable
fun StatisticsScreen() {
    Column {
        // Stats cards with neumorphic style
        NeumorphicCard {
            AnimatedCounter(targetValue = totalClicks, suffix = " clicks")
            DonutChart(percentage = 0.68f, size = 100)
        }
        
        // Battery level indicator
        BatteryLevelIndicator(level = batteryLevel, isCharging = isCharging)
        
        // Data charts
        LineChart(data = dailyClicks, color = Color(0xFF4CAF50))
        DonutChart(percentage = gestureAccuracy, size = 80)
        
        // Animated stats cards
        Row {
            GlassCard { AnimatedCounter(targetValue = activeSessions) }
            GlassCard { AnimatedCounter(targetValue = avgSpeed, suffix = " px/s") }
        }
    }
}