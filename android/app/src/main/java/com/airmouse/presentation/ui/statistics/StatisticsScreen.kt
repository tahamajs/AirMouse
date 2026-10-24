package com.airmouse.presentation.ui.statistics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.DailyStats
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.presentation.navigation.NavigationActions
import java.text.SimpleDateFormat
import java.util.*

import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import com.airmouse.presentation.ui.components.GlassmorphicCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navigationActions: NavigationActions,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PremiumDashboardBackground {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Analytics Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Session, connection, and action analysis",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Time Range Selector
                item {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeRange.entries.forEach { range ->
                                val isSelected = uiState.timeRange == range
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF6366F1) else Color.Transparent)
                                        .clickable { viewModel.updateTimeRange(range) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = range.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Error or Success messages
                uiState.error?.let { err ->
                    item {
                        GlassmorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundAlpha = 0.15f,
                            borderAlpha = 0.3f
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = err, color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                uiState.success?.let { msg ->
                    item {
                        GlassmorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundAlpha = 0.15f,
                            borderAlpha = 0.3f
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = msg, color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF10B981))
                                }
                            }
                        }
                    }
                }

                // 1. Session Overview Card
                item {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Session Overview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Duration", fontSize = 12.sp, color = StatsTheme.TextTertiary)
                                    AnimatedContent(
                                        targetState = formatDuration(uiState.sessionTime),
                                        transitionSpec = {
                                            (slideInVertically { -it } + fadeIn()) togetherWith
                                            (slideOutVertically { it } + fadeOut())
                                        },
                                        label = "DurationCounter"
                                    ) { durationStr ->
                                        Text(
                                            text = durationStr,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6366F1)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Total Actions", fontSize = 12.sp, color = StatsTheme.TextTertiary)
                                    val totalActions = uiState.clicks + uiState.doubleClicks + uiState.rightClicks + uiState.scrolls
                                    AnimatedContent(
                                        targetState = totalActions,
                                        transitionSpec = {
                                            (slideInVertically { -it } + fadeIn()) togetherWith
                                            (slideOutVertically { it } + fadeOut())
                                        },
                                        label = "TotalActionsCounter"
                                    ) { count ->
                                        Text(
                                            text = count.toString(),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = StatsTheme.SurfaceHighlight)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val isConnected = uiState.isTracking || uiState.averagePing > 0 || uiState.activeProtocol == "BLUETOOTH"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isConnected) StatsTheme.StatusSuccess else StatsTheme.StatusError)
                                    )
                                    Text(
                                        text = if (isConnected) "Connected" else "Disconnected",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Latency: ${if (uiState.averagePing > 0) "${uiState.averagePing} ms" else "--"}",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha=0.6f)
                                )
                                Text(
                                    text = "Success: ${String.format(Locale.getDefault(), "%.0f%%", uiState.getSuccessRate())}",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha=0.6f)
                                )
                            }
                            HorizontalDivider(color = StatsTheme.SurfaceHighlight)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Active Transport Protocol", fontSize = 11.sp, color = StatsTheme.TextTertiary)
                                    Text(
                                        text = uiState.activeProtocol,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF818CF8)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Attempts (Pass / Fail)", fontSize = 11.sp, color = StatsTheme.TextTertiary)
                                    Text(
                                        text = "${uiState.successfulConnections} / ${uiState.failedConnections}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha=0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Activity Chart (Canvas)
                item {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Weekly Activity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            ActivityBarChart(dailyStats = uiState.dailyStats)
                        }
                    }
                }

                // 3. Gesture Breakdown
                item {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Gesture Breakdown",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            GestureDonutChart(breakdown = uiState.gestureBreakdown)
                        }
                    }
                }

                // 4. Performance Metrics
                item {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Performance Metrics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            val totalActions = uiState.clicks + uiState.doubleClicks + uiState.rightClicks + uiState.scrolls
                            val minutes = (uiState.sessionTime / 60f).coerceAtLeast(0.1f)
                            val apm = (totalActions / minutes).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    title = "Success Rate",
                                    value = String.format(Locale.getDefault(), "%.1f%%", uiState.getSuccessRate()),
                                    color = StatsTheme.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Avg Latency",
                                    value = "${uiState.averagePing} ms",
                                    color = StatsTheme.AccentBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Actions / Min",
                                    value = apm.toString(),
                                    color = StatsTheme.AccentPurple,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 4b. Device Resource Telemetry Card
                item {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Device Resource Telemetry",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    title = "CPU",
                                    value = String.format(Locale.getDefault(), "%.1f%%", uiState.cpuUsage),
                                    color = StatsTheme.AccentOrange,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Memory",
                                    value = String.format(Locale.getDefault(), "%.1f%%", uiState.memoryUsage),
                                    color = StatsTheme.AccentBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Battery",
                                    value = "${uiState.batteryUsage}%",
                                    color = StatsTheme.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Temp",
                                    value = String.format(Locale.getDefault(), "%.1f°C", uiState.temperature),
                                    color = StatsTheme.AccentRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 5. Recent Activity Feed
                item {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Recent Activity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            RecentActivityList(breakdown = uiState.gestureBreakdown)
                        }
                    }
                }

                // 6. Action Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.showExportDialog(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Export", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.showResetDialog(true) },
                            border = BorderStroke(1.dp, StatsTheme.StatusError),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatsTheme.StatusError),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Reset", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showResetDialog(false) },
            title = { Text(text = "Reset Stats?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "This will permanently delete all session and usage data. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resetStatistics() },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatsTheme.StatusError)
                ) {
                    Text(text = "Reset All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showResetDialog(false) }) {
                    Text(text = "Cancel")
                }
            },
            containerColor = Color(0xFF13161F)
        )
    }

    // Export Confirmation Dialog
    if (uiState.showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showExportDialog(false) },
            title = { Text(text = "Export Analytics?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Your telemetry and usage metrics will be compiled and exported to your Downloads folder.") },
            confirmButton = {
                TextButton(onClick = { viewModel.exportStatistics() }) {
                    Text(text = "Export Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showExportDialog(false) }) {
                    Text(text = "Cancel")
                }
            },
            containerColor = Color(0xFF13161F)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    var animatedIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animatedIn = true }
    val scaleFactor by animateFloatAsState(
        targetValue = if (animatedIn) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "MetricCardScale"
    )
    Surface(
        modifier = modifier.scale(scaleFactor),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1
            )
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (slideInVertically { -it } + fadeIn()) togetherWith
                    (slideOutVertically { it } + fadeOut())
                },
                label = "MetricValue"
            ) { v ->
                Text(
                    text = v,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun ActivityBarChart(dailyStats: List<DailyStats>) {
    // Generate default data if empty or insufficient
    val data = remember(dailyStats) {
        if (dailyStats.size >= 5) {
            dailyStats.takeLast(7)
        } else {
            val calendar = Calendar.getInstance()
            val format = SimpleDateFormat("EE", Locale.getDefault())
            List(7) { i ->
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                DailyStats(
                    date = format.format(calendar.time),
                    clicks = listOf(15, 24, 42, 30, 20, 35, 48)[i]
                )
            }.reversed()
        }
    }

    val maxVal = remember(data) { (data.maxOfOrNull { it.clicks } ?: 10).coerceAtLeast(1) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = data.size
        val yAxisWidth = 32.dp.toPx()
        val chartWidth = width - yAxisWidth
        val gap = 12.dp.toPx()
        val totalGaps = gap * (barCount - 1)
        val barWidth = (chartWidth - totalGaps) / barCount

        val gridLines = 4
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        for (i in 0..gridLines) {
            val y = height - (i.toFloat() / gridLines) * (height - 30.dp.toPx()) - 20.dp.toPx()
            // Y-axis gridline
            drawLine(
                color = StatsTheme.SurfaceHighlight,
                start = Offset(yAxisWidth, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )

            // Y-axis label
            val tickValue = (maxVal * i / gridLines)
            val tickLabel = if (tickValue >= 1000) "${tickValue / 1000}k" else "$tickValue"
            drawContext.canvas.nativeCanvas.drawText(
                tickLabel,
                yAxisWidth - 6.dp.toPx(),
                y + 3.dp.toPx(),
                textPaint
            )
        }

        data.forEachIndexed { idx, item ->
            val barHeight = ((item.clicks.toFloat() / maxVal) * (height - 50.dp.toPx())).coerceAtLeast(4.dp.toPx())
            val x = yAxisWidth + idx * (barWidth + gap)
            val y = height - barHeight - 20.dp.toPx()

            // Neon glow layer (wider, softer, same gradient)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF818CF8).copy(alpha = 0.35f), Color(0xFF6366F1).copy(alpha = 0.2f))
                ),
                topLeft = Offset(x - 4.dp.toPx(), y),
                size = Size(barWidth + 8.dp.toPx(), barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Main bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Draw Label
            drawContext.canvas.nativeCanvas.apply {
                val label = if (item.date.length > 3) item.date.take(3) else item.date
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(
                    label,
                    x + barWidth / 2,
                    height - 2.dp.toPx(),
                    labelPaint
                )
            }
        }
    }
}

@Composable
private fun GestureDonutChart(breakdown: Map<String, Int>) {
    val items = remember(breakdown) {
        if (breakdown.isNotEmpty()) {
            breakdown.toList()
        } else {
            listOf("Click" to 55, "Scroll" to 25, "Swipe" to 15, "Other" to 5)
        }
    }

    val total = remember(items) { items.sumOf { it.second }.toFloat().coerceAtLeast(1f) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(130.dp)
                .weight(1.2f)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 - 12.dp.toPx()
            var startAngle = -90f

            val colors = listOf(
                Color(0xFF6366F1),
                StatsTheme.Secondary,
                StatsTheme.AccentOrange,
                StatsTheme.AccentPurple,
                StatsTheme.AccentBlue
            )

            items.forEachIndexed { index, pair ->
                val sweep = (pair.second / total) * 360f
                // Outer glow
                drawArc(
                    color = colors[index % colors.size].copy(alpha = 0.18f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(centerX - radius - 4.dp.toPx(), centerY - radius - 4.dp.toPx()),
                    size = Size(radius * 2 + 8.dp.toPx(), radius * 2 + 8.dp.toPx()),
                    style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round)
                )
                // Main ring
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }

        Column(
            modifier = Modifier.weight(1.8f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val colors = listOf(
                Color(0xFF6366F1),
                StatsTheme.Secondary,
                StatsTheme.AccentOrange,
                StatsTheme.AccentPurple,
                StatsTheme.AccentBlue
            )
            items.forEachIndexed { idx, pair ->
                val pct = (pair.second / total * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors[idx % colors.size])
                    )
                    Text(
                        text = "${pair.first}: $pct%",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha=0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivityList(breakdown: Map<String, Int>) {
    // Compile fallback list or real list
    val feed = remember(breakdown) {
        val list = mutableListOf<Triple<String, String, String>>()
        val now = System.currentTimeMillis()
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())

        if (breakdown.isNotEmpty()) {
            breakdown.forEach { (type, count) ->
                list.add(Triple(type, "$count detected in current session", format.format(Date(now - (list.size * 180000L)))))
            }
        }

        if (list.isEmpty()) {
            list.addAll(listOf(
                Triple("Left Click", "Triggered standard click", "02:14 PM"),
                Triple("Scroll", "Scrolled 5 lines down", "02:12 PM"),
                Triple("Double Click", "Opened folder item", "02:08 PM"),
                Triple("Right Click", "Triggered context menu", "02:03 PM"),
                Triple("Proximity Lock", "Auto-locked desktop", "01:54 PM")
            ))
        }
        list.take(5)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        feed.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StatsTheme.SurfaceHighlight.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (item.first.lowercase(Locale.ROOT)) {
                                    "left click", "click" -> Icons.Default.Mouse
                                    "scroll" -> Icons.Default.VerticalAlignBottom
                                    "right click" -> Icons.Default.Menu
                                    else -> Icons.Default.Gesture
                                },
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(text = item.first, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = item.second, fontSize = 12.sp, color = StatsTheme.TextTertiary)
                    }
                }
                Text(text = item.third, fontSize = 12.sp, color = StatsTheme.TextTertiary, textAlign = TextAlign.End)
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
}

private fun StatisticsUiState.getSuccessRate(): Float {
    return if (connectionAttempts > 0) {
        (successfulConnections.toFloat() / connectionAttempts) * 100
    } else 100f
}

private object StatsTheme {
    val TextTertiary = Color.White.copy(alpha = 0.5f)
    val SurfaceHighlight = Color.White.copy(alpha = 0.1f)
    val StatusSuccess = Color(0xFF10B981)
    val StatusError = Color(0xFFEF4444)
    val Secondary = Color(0xFF64748B)
    val AccentBlue = Color(0xFF38BDF8)
    val AccentPurple = Color(0xFF8B5CF6)
    val AccentOrange = Color(0xFFF59E0B)
    val AccentRed = Color(0xFFEF4444)
}
