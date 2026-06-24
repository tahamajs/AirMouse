package com.airmouse.presentation.ui.statistics

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
    val surfaceColor = MaterialTheme.colorScheme.surface

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Statistics", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "Live session, connection, and usage summary",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Range Selector
                item {
                    AnimatedContent(targetState = uiState.timeRange) { _ ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Time range",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TimeRange.entries.forEach { range ->
                                        FilterChip(
                                            selected = uiState.timeRange == range,
                                            onClick = { viewModel.updateTimeRange(range) },
                                            label = {
                                                Text(
                                                    range.displayName,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (uiState.timeRange == range) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading state
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Error message
                uiState.error?.let { error ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                                }
                            }
                        }
                    }
                }

                // Success message
                uiState.success?.let { message ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                                }
                            }
                        }
                    }
                }

                // Session Overview
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Session overview",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Live session metrics, connection quality, and calibration state.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SummaryInfoChip(
                                    label = "Tracking",
                                    value = if (uiState.isTracking) "Live" else "Idle",
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryInfoChip(
                                    label = "Calibration",
                                    value = if (uiState.calibrationComplete) "Ready" else "Pending",
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryInfoChip(
                                    label = "Protocol",
                                    value = if (uiState.useUdpDiscovery) "UDP" else "Manual",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricTile(
                                    value = formatDuration(uiState.sessionTime),
                                    label = "Session Time",
                                    accent = Color(0xFF2196F3),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    value = uiState.summaryStats.totalActions.toString(),
                                    label = "Actions",
                                    accent = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    value = uiState.gesturesDetected.toString(),
                                    label = "Gestures",
                                    accent = Color(0xFFFF9800),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricTile(
                                    value = String.format(Locale.getDefault(), "%.1f%%", uiState.getSuccessRate()),
                                    label = "Success",
                                    accent = Color(0xFF22C55E),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    value = if (uiState.averagePing > 0) "${uiState.averagePing} ms" else "No data",
                                    label = "Avg Ping",
                                    accent = Color(0xFF38BDF8),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(uiState.lastActivityTime)),
                                    label = "Last Activity",
                                    accent = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Domain Summary
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Shared summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                "This uses the domain StatisticsSummary model now.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                StatCircle(
                                    value = uiState.summaryStats.totalActions.toString(),
                                    label = "Actions",
                                    color = Color(0xFF6366F1)
                                )
                                StatCircle(
                                    value = uiState.summaryStats.totalClicksAll.toString(),
                                    label = "Clicks",
                                    color = Color(0xFF10B981)
                                )
                                StatCircle(
                                    value = uiState.summaryStats.getSessionDurationFormatted(),
                                    label = "Duration",
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }

                // Domain Summary (additional)
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Domain summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "The screen now reflects the shared statistics model used by the app.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                StatCircle(
                                    value = uiState.summaryStats.totalClicksAll.toString(),
                                    label = "Clicks",
                                    color = Color(0xFF22C55E)
                                )
                                StatCircle(
                                    value = uiState.summaryStats.totalScrolls.toString(),
                                    label = "Scrolls",
                                    color = Color(0xFF38BDF8)
                                )
                                StatCircle(
                                    value = uiState.summaryStats.getAverageSpeedFormatted(),
                                    label = "Avg Speed",
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }

                // Gesture Distribution
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gesture distribution",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.gestureBreakdown.isNotEmpty()) {
                                    Text(
                                        text = "${uiState.gestureBreakdown.size} types",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.gestureBreakdown.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No gesture data yet",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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

                                // Pie Chart
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
                                    val centerX = size.width / 2
                                    val centerY = size.height / 2
                                    val radius = minOf(size.width, size.height) * 0.4f

                                    uiState.gestureBreakdown.values.forEachIndexed { index, count ->
                                        val sweepAngle = (count / total) * 360f
                                        drawArc(
                                            color = colors[index % colors.size],
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = true,
                                            topLeft = Offset(centerX - radius, centerY - radius),
                                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                                        )
                                        startAngle += sweepAngle
                                    }

                                    // Center circle
                                    drawCircle(
                                        color = surfaceColor,
                                        radius = radius * 0.5f,
                                        center = Offset(centerX, centerY)
                                    )
                                }
                            }
                        }
                    }
                }

                // Movement Statistics
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Movement statistics",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatBox(
                                    "Distance",
                                    String.format(Locale.getDefault(), "%.1f", uiState.totalDistanceMoved),
                                    Color(0xFF2196F3)
                                )
                                StatBox(
                                    "Avg Speed",
                                    String.format(Locale.getDefault(), "%.1f", uiState.averageSpeed),
                                    Color(0xFF4CAF50)
                                )
                                StatBox(
                                    "Peak Speed",
                                    String.format(Locale.getDefault(), "%.1f", uiState.peakSpeed),
                                    Color(0xFFFF9800)
                                )
                                StatBox(
                                    "Movements",
                                    uiState.totalMovements.toString(),
                                    Color(0xFF9C27B0)
                                )
                            }
                        }
                    }
                }

                // Connection Statistics
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Connection statistics",
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (progressValue > 0.7f) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                            Text(
                                text = "Success Rate: ${String.format(Locale.getDefault(), "%.1f%%", progressValue * 100)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                // Daily Activity
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Daily activity (clicks)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.dailyStats.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No daily data",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                                            y = height - (stat.clicks.toFloat() / maxClicks) * height * 0.9f
                                        )
                                    }

                                    // Line
                                    for (i in 0 until points.size - 1) {
                                        drawLine(
                                            color = Color(0xFF00BCD4),
                                            start = points[i],
                                            end = points[i + 1],
                                            strokeWidth = 3f
                                        )
                                    }

                                    // Fill area
                                    val path = Path().apply {
                                        moveTo(0f, height)
                                        points.forEach { lineTo(it.x, it.y) }
                                        lineTo(width, height)
                                        close()
                                    }
                                    drawPath(path, Color(0xFF00BCD4).copy(alpha = 0.2f))

                                    // Points
                                    points.forEach { point ->
                                        drawCircle(
                                            color = Color(0xFF00BCD4),
                                            radius = 4f,
                                            center = point
                                        )
                                    }
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
                                text = "Performance metrics",
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

                // Session Information
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Session information",
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

                // Footer spacer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Reset Dialog
    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showResetDialog(false) },
            title = {
                Text(
                    "Reset Statistics",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to reset all statistics? This action cannot be undone."
                )
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resetStatistics() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showResetDialog(false) }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Export Dialog
    if (uiState.showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showExportDialog(false) },
            title = {
                Text(
                    "📤 Export Statistics",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Export your statistics to a file.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Format: Text file (.txt)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Location: Downloads folder",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            icon = {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.exportStatistics() }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showExportDialog(false) }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ============================================================
// COMPONENTS
// ============================================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        content()
    }
}

@Composable
fun StatCircle(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    MetricTile(
        value = value,
        label = label,
        accent = color
    )
}

@Composable
fun GestureStatItem(label: String, value: Int, color: Color) {
    MetricTile(
        value = value.toString(),
        label = label,
        accent = color
    )
}

@Composable
fun MetricTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun SummaryInfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ============================================================
// UTILITY FUNCTIONS
// ============================================================

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

// ============================================================
// EXTENSION FUNCTIONS
// ============================================================

private fun StatisticsUiState.getSuccessRate(): Float {
    return if (connectionAttempts > 0) {
        (successfulConnections.toFloat() / connectionAttempts) * 100
    } else 0f
}

private fun StatisticsSummary.getSessionDurationFormatted(): String {
    return formatDuration(sessionDuration / 1000L)
}

private fun StatisticsSummary.getAverageSpeedFormatted(): String {
    return String.format(Locale.getDefault(), "%.1f", averageSpeed)
}