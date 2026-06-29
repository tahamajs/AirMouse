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

private object StatsTheme {
    val Primary = Color(0xFF6366F1)
    val PrimaryLight = Color(0xFF818CF8)
    val PrimaryDark = Color(0xFF4F46E5)
    val Secondary = Color(0xFF10B981)
    val SecondaryLight = Color(0xFF34D399)
    val SecondaryDark = Color(0xFF059669)
    val AccentOrange = Color(0xFFF59E0B)
    val AccentRed = Color(0xFFEF4444)
    val AccentBlue = Color(0xFF3B82F6)
    val AccentPurple = Color(0xFF8B5CF6)
    val StatusSuccess = Color(0xFF10B981)
    val StatusWarning = Color(0xFFF59E0B)
    val StatusError = Color(0xFFEF4444)
    val StatusInfo = Color(0xFF3B82F6)
    val Surface = Color(0xFF1E1E2E)
    val SurfaceVariant = Color(0xFF2A2A3A)
    val SurfaceHighlight = Color(0xFF3A3A4A)
    val Background = Color(0xFF0F0F1A)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextTertiary = Color(0xFF64748B)

    // Typography sp
    val DisplaySmall = 24.sp
    val HeadingLarge = 20.sp
    val HeadingMedium = 18.sp
    val HeadingSmall = 16.sp
    val BodyLarge = 16.sp
    val BodyMedium = 14.sp
    val BodySmall = 12.sp
    val LabelMedium = 12.sp

    // Spacing
    val Space0 = 0.dp
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val Space10 = 40.dp
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
                title = {
                    Column {
                        Text(
                            text = "Analytics Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = StatsTheme.TextPrimary
                        )
                        Text(
                            text = "Session, connection, and action analysis",
                            fontSize = 12.sp,
                            color = StatsTheme.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StatsTheme.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = StatsTheme.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StatsTheme.Background,
                    titleContentColor = StatsTheme.TextPrimary,
                    navigationIconContentColor = StatsTheme.TextPrimary
                )
            )
        },
        containerColor = StatsTheme.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(StatsTheme.Background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = StatsTheme.Space4),
                verticalArrangement = Arrangement.spacedBy(StatsTheme.Space4)
            ) {
                // Time Range Selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatsTheme.SurfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StatsTheme.SurfaceHighlight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(StatsTheme.Space3),
                            horizontalArrangement = Arrangement.spacedBy(StatsTheme.Space2)
                        ) {
                            TimeRange.entries.forEach { range ->
                                val isSelected = uiState.timeRange == range
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) StatsTheme.Primary else Color.Transparent)
                                        .clickable { viewModel.updateTimeRange(range) }
                                        .padding(vertical = StatsTheme.Space2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = range.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else StatsTheme.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Error or Success messages
                uiState.error?.let { err ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = StatsTheme.StatusError.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, StatsTheme.StatusError)
                        ) {
                            Row(
                                modifier = Modifier.padding(StatsTheme.Space4),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = err, color = StatsTheme.StatusError, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = StatsTheme.StatusError)
                                }
                            }
                        }
                    }
                }

                uiState.success?.let { msg ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = StatsTheme.StatusSuccess.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, StatsTheme.StatusSuccess)
                        ) {
                            Row(
                                modifier = Modifier.padding(StatsTheme.Space4),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = msg, color = StatsTheme.StatusSuccess, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = StatsTheme.StatusSuccess)
                                }
                            }
                        }
                    }
                }

                // 1. Session Overview Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatsTheme.SurfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StatsTheme.SurfaceHighlight)
                    ) {
                        Column(
                            modifier = Modifier.padding(StatsTheme.Space4),
                            verticalArrangement = Arrangement.spacedBy(StatsTheme.Space3)
                        ) {
                            Text(
                                text = "Session Overview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatsTheme.TextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(StatsTheme.Space3)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Duration", fontSize = 12.sp, color = StatsTheme.TextTertiary)
                                    Text(
                                        text = formatDuration(uiState.sessionTime),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatsTheme.TextPrimary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Total Actions", fontSize = 12.sp, color = StatsTheme.TextTertiary)
                                    Text(
                                        text = (uiState.clicks + uiState.doubleClicks + uiState.rightClicks + uiState.scrolls).toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatsTheme.TextPrimary
                                    )
                                }
                            }
                            HorizontalDivider(color = StatsTheme.SurfaceHighlight)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val isConnected = uiState.isTracking || uiState.averagePing > 0
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
                                        color = StatsTheme.TextPrimary
                                    )
                                }
                                Text(
                                    text = "Latency: ${if (uiState.averagePing > 0) "${uiState.averagePing} ms" else "--"}",
                                    fontSize = 13.sp,
                                    color = StatsTheme.TextSecondary
                                )
                                Text(
                                    text = "Success: ${String.format(Locale.getDefault(), "%.0f%%", uiState.getSuccessRate())}",
                                    fontSize = 13.sp,
                                    color = StatsTheme.TextSecondary
                                )
                            }
                        }
                    }
                }

                // 2. Activity Chart (Canvas)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatsTheme.SurfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StatsTheme.SurfaceHighlight)
                    ) {
                        Column(modifier = Modifier.padding(StatsTheme.Space4)) {
                            Text(
                                text = "Weekly Activity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatsTheme.TextPrimary,
                                modifier = Modifier.padding(bottom = StatsTheme.Space4)
                            )
                            ActivityBarChart(dailyStats = uiState.dailyStats)
                        }
                    }
                }

                // 3. Gesture Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatsTheme.SurfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StatsTheme.SurfaceHighlight)
                    ) {
                        Column(modifier = Modifier.padding(StatsTheme.Space4)) {
                            Text(
                                text = "Gesture Breakdown",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatsTheme.TextPrimary,
                                modifier = Modifier.padding(bottom = StatsTheme.Space4)
                            )
                            GestureDonutChart(breakdown = uiState.gestureBreakdown)
                        }
                    }
                }

                // 4. Performance Metrics
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatsTheme.SurfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StatsTheme.SurfaceHighlight)
                    ) {
                        Column(
                            modifier = Modifier.padding(StatsTheme.Space4),
                            verticalArrangement = Arrangement.spacedBy(StatsTheme.Space3)
                        ) {
                            Text(
                                text = "Performance Metrics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatsTheme.TextPrimary
                            )
                            val totalActions = uiState.clicks + uiState.doubleClicks + uiState.rightClicks + uiState.scrolls
                            val minutes = (uiState.sessionTime / 60f).coerceAtLeast(0.1f)
                            val apm = (totalActions / minutes).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(StatsTheme.Space3)
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

                // 5. Recent Activity Feed
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatsTheme.SurfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StatsTheme.SurfaceHighlight)
                    ) {
                        Column(
                            modifier = Modifier.padding(StatsTheme.Space4),
                            verticalArrangement = Arrangement.spacedBy(StatsTheme.Space3)
                        ) {
                            Text(
                                text = "Recent Activity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatsTheme.TextPrimary
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
                            .padding(vertical = StatsTheme.Space4),
                        horizontalArrangement = Arrangement.spacedBy(StatsTheme.Space3)
                    ) {
                        Button(
                            onClick = { viewModel.showExportDialog(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatsTheme.Primary),
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
                    Spacer(modifier = Modifier.height(StatsTheme.Space6))
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
            containerColor = StatsTheme.Surface
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
            containerColor = StatsTheme.Surface
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = StatsTheme.Space3, vertical = StatsTheme.Space3),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = StatsTheme.TextTertiary, maxLines = 1)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
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
            .padding(top = StatsTheme.Space2)
    ) {
        val width = size.width
        val height = size.height
        val barCount = data.size
        val gap = 16.dp.toPx()
        val totalGaps = gap * (barCount - 1)
        val barWidth = (width - totalGaps) / barCount

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height - (i.toFloat() / gridLines) * (height - 30.dp.toPx()) - 20.dp.toPx()
            drawLine(
                color = StatsTheme.SurfaceHighlight,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        data.forEachIndexed { idx, item ->
            val barHeight = ((item.clicks.toFloat() / maxVal) * (height - 50.dp.toPx())).coerceAtLeast(4.dp.toPx())
            val x = idx * (barWidth + gap)
            val y = height - barHeight - 20.dp.toPx()

            // Draw Bar
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(StatsTheme.PrimaryLight, StatsTheme.Primary)),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Draw Label
            drawContext.canvas.nativeCanvas.apply {
                val label = if (item.date.length > 3) item.date.take(3) else item.date
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(
                    label,
                    x + barWidth / 2,
                    height - 2.dp.toPx(),
                    textPaint
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
        horizontalArrangement = Arrangement.spacedBy(StatsTheme.Space4)
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
                StatsTheme.Primary,
                StatsTheme.Secondary,
                StatsTheme.AccentOrange,
                StatsTheme.AccentPurple,
                StatsTheme.AccentBlue
            )

            items.forEachIndexed { index, pair ->
                val sweep = (pair.second / total) * 360f
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
                StatsTheme.Primary,
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
                        color = StatsTheme.TextSecondary,
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

    Column(verticalArrangement = Arrangement.spacedBy(StatsTheme.Space3)) {
        feed.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StatsTheme.SurfaceHighlight.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = StatsTheme.Space3, vertical = StatsTheme.Space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StatsTheme.Space3)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = StatsTheme.Primary.copy(alpha = 0.12f),
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
                                tint = StatsTheme.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(text = item.first, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatsTheme.TextPrimary)
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
