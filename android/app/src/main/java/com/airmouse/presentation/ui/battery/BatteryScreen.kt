package com.airmouse.presentation.ui.battery

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import java.text.SimpleDateFormat
import java.util.*

// ==================== HELPER FUNCTIONS ====================

fun getBatteryColor(level: Int): Color {
    return when {
        level >= 80 -> Color(0xFF4CAF50)  // Green
        level >= 60 -> Color(0xFF8BC34A)  // Light Green
        level >= 40 -> Color(0xFFFFC107)  // Amber
        level >= 20 -> Color(0xFFFF9800)  // Orange
        else -> Color(0xFFF44336)         // Red
    }
}

fun getHealthColor(healthPercent: Int): Color {
    return when {
        healthPercent >= 80 -> Color(0xFF4CAF50)
        healthPercent >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

// ==================== COMPONENTS ====================

@Composable
fun BatteryLevelIndicator(
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val color = getBatteryColor(level)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val strokeWidth = width * 0.08f
            val radius = (width / 2) - strokeWidth / 2

            // Background circle
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = radius,
                center = Offset(width / 2, height / 2),
                style = Stroke(width = strokeWidth)
            )

            // Progress circle
            val sweepAngle = (level / 100f) * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = androidx.compose.ui.geometry.Size(width - strokeWidth, height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Charging indicator
            if (isCharging) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = radius * 0.9f,
                    center = Offset(width / 2, height / 2),
                    style = Stroke(width = 2f)
                )
                // Draw lightning bolt using lines
                val boltSize = 30f
                drawLine(
                    color = Color.White,
                    start = Offset(width / 2 - boltSize / 2, height / 2 - boltSize / 2),
                    end = Offset(width / 2 + boltSize / 2, height / 2 + boltSize / 2),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(width / 2 + boltSize / 2, height / 2 - boltSize / 2),
                    end = Offset(width / 2 - boltSize / 2, height / 2 + boltSize / 2),
                    strokeWidth = 4f
                )
            }
        }

        // Percentage text
        Text(
            text = if (isCharging) "⚡" else "${level}%",
            fontSize = if (isCharging) 32.sp else 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCharging) Color.White else color
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

// ==================== MAIN SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    navigationActions: NavigationActions,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "battery_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var selectedTab by remember { mutableStateOf(BatteryTab.DETAILS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Battery Monitor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshBatteryInfo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.openBatterySettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            getBatteryColor(uiState.level).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Battery Level Card
                item {
                    AnimatedBatteryCard(
                        level = uiState.level,
                        isCharging = uiState.isCharging,
                        temperature = uiState.temperature,
                        voltage = uiState.voltage,
                        pulse = pulse
                    )
                }

                // Quick Stats Row
                item {
                    QuickStatsRow(
                        temperature = uiState.temperature,
                        voltage = uiState.voltage,
                        current = uiState.current,
                        health = uiState.health
                    )
                }

                // Tabs
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        BatteryTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title, fontSize = 14.sp) },
                                icon = {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    BatteryTab.DETAILS -> {
                        item { DetailsContent(uiState) }
                    }
                    BatteryTab.HISTORY -> {
                        item { HistoryContent(uiState) }
                    }
                    BatteryTab.APPS -> {
                        item { AppsContent(viewModel) }
                    }
                    BatteryTab.SAVINGS -> {
                        item { SavingsContent(context) }
                    }
                }
            }
        }
    }
}

// ==================== SUB-COMPONENTS ====================

@Composable
fun AnimatedBatteryCard(
    level: Int,
    isCharging: Boolean,
    temperature: Float,
    voltage: Float,
    pulse: Float // Kept for API compatibility but used for animation
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Battery Icon
            BatteryLevelIndicator(
                level = level,
                isCharging = isCharging,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${level}%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = getBatteryColor(level),
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = CircleShape,
                color = getBatteryColor(level).copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (isCharging) "⚡ Charging" else "🔋 Discharging",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = getBatteryColor(level)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoChip("🌡️", String.format(Locale.US, "%.1f°C", temperature), "Temperature")
                InfoChip("⚡", String.format(Locale.US, "%.2fV", voltage), "Voltage")
                InfoChip("📊", if (isCharging) "Charging" else "Discharging", "Status")
            }
        }
    }
}

@Composable
fun InfoChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuickStatsRow(temperature: Float, voltage: Float, current: Float, health: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            title = "Temperature",
            value = String.format(Locale.US, "%.1f°C", temperature),
            icon = Icons.Default.Thermostat,
            color = if (temperature > 45) Color(0xFFF44336) else Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Voltage",
            value = String.format(Locale.US, "%.2fV", voltage),
            icon = Icons.Default.Bolt,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Current",
            value = if (current != 0f) String.format(Locale.US, "%.0fmA", current) else "N/A",
            icon = Icons.Default.FlashOn,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Health",
            value = health,
            icon = Icons.Default.Favorite,
            color = when (health) {
                "Good" -> Color(0xFF4CAF50)
                "Overheat" -> Color(0xFFF44336)
                else -> Color(0xFFFF9800)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DetailsContent(uiState: BatteryUiState) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🔋 Battery Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow("Status", uiState.status, Icons.Default.Info)
            DetailRow("Health", uiState.health, Icons.Default.Favorite)
            DetailRow("Technology", uiState.technology, Icons.Default.Science)
            DetailRow("Power Source", uiState.plugged, Icons.Default.Power)
            DetailRow("Charge Counter", "${uiState.chargeCounter} μAh", Icons.Default.BatteryFull)
            DetailRow("Energy Counter", "${uiState.energyCounter} nWh", Icons.Default.EnergySavingsLeaf)
            DetailRow("Battery Present", if (uiState.isPresent) "Yes" else "No", Icons.Default.CheckCircle)
            DetailRow("Battery Saver", if (uiState.batterySaverEnabled) "Enabled" else "Disabled", Icons.Default.BatterySaver)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📊 Capacity Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            val healthPercent = uiState.capacity

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Battery Health", fontSize = 14.sp)
                Text("$healthPercent%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = getHealthColor(healthPercent))
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { healthPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = getHealthColor(healthPercent),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    healthPercent >= 80 -> "✅ Your battery is in excellent condition"
                    healthPercent >= 60 -> "⚠️ Battery showing normal wear"
                    else -> "🔴 Consider battery replacement soon"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HistoryContent(uiState: BatteryUiState) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📈 Battery History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No history data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                BatteryHistoryChart(history = uiState.history)

                Spacer(modifier = Modifier.height(16.dp))

                Text("Recent Records", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                uiState.history.takeLast(10).reversed().forEach { entry ->
                    HistoryEntryRow(entry = entry)
                }
            }
        }
    }
}

@Composable
fun BatteryHistoryChart(history: List<BatteryHistoryEntry>) {
    if (history.isEmpty()) return

    val maxLevel = history.maxOfOrNull { it.level } ?: 100
    val minLevel = history.minOfOrNull { it.level } ?: 0
    val levelRange = (maxLevel - minLevel).coerceAtLeast(1).toFloat()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (history.size - 1).coerceAtLeast(1)

        // Draw grid lines
        val gridColor = Color.White.copy(alpha = 0.05f)
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw level line
        val path = Path()
        history.forEachIndexed { index, entry ->
            val x = index * stepX
            val y = height - ((entry.level - minLevel) / levelRange) * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = getBatteryColor(history.last().level),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Fill area under curve
        val fillPath = Path().apply {
            moveTo(0f, height)
            history.forEachIndexed { index, entry ->
                val x = index * stepX
                val y = height - ((entry.level - minLevel) / levelRange) * height
                lineTo(x, y)
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = fillPath,
            color = getBatteryColor(history.last().level).copy(alpha = 0.1f)
        )

        // Draw data points
        history.forEachIndexed { index, entry ->
            val x = index * stepX
            val y = height - ((entry.level - minLevel) / levelRange) * height
            drawCircle(
                color = getBatteryColor(entry.level),
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun HistoryEntryRow(entry: BatteryHistoryEntry) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(getBatteryColor(entry.level))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dateFormat.format(Date(entry.timestamp)),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "${entry.level}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = getBatteryColor(entry.level)
            )
            Text(
                text = String.format(Locale.US, "%.1f°C", entry.temperature),
                fontSize = 12.sp,
                color = if (entry.temperature > 45) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.isCharging) {
                Text("⚡", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AppsContent(viewModel: BatteryViewModel) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📱 Battery Usage by App",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            val topApps = viewModel.getTopBatteryApps()

            if (topApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Collecting usage data...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                topApps.forEach { app ->
                    AppBatteryUsageItem(
                        appName = app.name,
                        usage = app.usagePercent,
                        icon = app.icon
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { viewModel.openBatteryOptimizationSettings() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.BatterySaver, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open Battery Optimization Settings")
    }
}

@Composable
fun AppBatteryUsageItem(appName: String, usage: Float, icon: ImageVector?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(icon, contentDescription = appName, modifier = Modifier.size(24.dp))
                } else {
                    Text(appName.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(appName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Battery usage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(String.format(Locale.US, "%.1f%%", usage), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { usage / 100f },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = when {
                    usage > 20 -> Color(0xFFF44336)
                    usage > 10 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
            )
        }
    }
}

@Composable
fun SavingsContent(context: android.content.Context) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "💰 Battery Savings Tips",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            SavingsTip(
                icon = "🔆",
                title = "Reduce Screen Brightness",
                description = "Lower brightness can save up to 20% battery"
            )
            SavingsTip(
                icon = "📱",
                title = "Close Unused Apps",
                description = "Background apps consume significant power"
            )
            SavingsTip(
                icon = "🌐",
                title = "Disable Unused Connections",
                description = "Turn off Bluetooth/WiFi when not in use"
            )
            SavingsTip(
                icon = "🔇",
                title = "Reduce Haptic Feedback",
                description = "Haptic feedback uses battery with each interaction"
            )
            SavingsTip(
                icon = "🎨",
                title = "Use Dark Mode",
                description = "Dark mode saves battery on OLED screens"
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.EnergySavingsLeaf, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Enable Power Saving Mode")
    }
}

@Composable
fun SavingsTip(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, fontSize = 24.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

enum class BatteryTab(
    val title: String,
    val icon: ImageVector
) {
    DETAILS("Details", Icons.Default.Info),
    HISTORY("History", Icons.Default.History),
    APPS("Apps", Icons.Default.Apps),
    SAVINGS("Savings", Icons.Default.EnergySavingsLeaf)
}