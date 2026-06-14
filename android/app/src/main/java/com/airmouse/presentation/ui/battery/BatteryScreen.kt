package com.airmouse.presentation.ui.battery

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    navigationActions: NavigationActions,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        BatteryTab.values().forEach { tab ->
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
                    BatteryTab.DETAILS -> detailsContent(uiState)
                    BatteryTab.HISTORY -> historyContent(uiState)
                    BatteryTab.APPS -> appsContent(viewModel)
                    BatteryTab.SAVINGS -> savingsContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun AnimatedBatteryCard(level: Int, isCharging: Boolean, temperature: Float, voltage: Float, pulse: Float) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Battery Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulse),
                contentAlignment = Alignment.Center
            ) {
                BatteryLevelIndicator(
                    level = level,
                    isCharging = isCharging,
                    size = 100,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Battery Percentage
            Text(
                text = "${level}%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = getBatteryColor(level),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status Text
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
            
            // Additional Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoChip("🌡️", "${temperature}°C", "Temperature")
                InfoChip("⚡", "${voltage}V", "Voltage")
                InfoChip("📊", "${if (isCharging) "Charging" else "Discharging"}", "Status")
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
            value = String.format("%.1f°C", temperature),
            icon = Icons.Default.Thermostat,
            color = if (temperature > 45) Color(0xFFF44336) else Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Voltage",
            value = String.format("%.2fV", voltage),
            icon = Icons.Default.Bolt,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Current",
            value = if (current > 0) String.format("%.0fmA", current) else "N/A",
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
fun QuickStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
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
private fun LazyListScope.detailsContent(uiState: BatteryUiState) {
    item {
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
    }
    
    item {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "📊 Capacity Analysis",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val capacity = uiState.capacity
                val healthPercent = if (capacity > 0) (uiState.level * 100 / capacity).coerceIn(0, 100) else 100
                
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
                    progress = healthPercent / 100f,
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
}

@Composable
private fun LazyListScope.historyContent(uiState: BatteryUiState) {
    item {
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
                    // History Chart
                    BatteryHistoryChart(history = uiState.history)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // History List
                    Text("Recent Records", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    uiState.history.takeLast(10).reversed().forEach { entry ->
                        HistoryEntryRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryHistoryChart(history: List<BatteryHistoryEntry>) {
    if (history.isEmpty()) return
    
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val maxLevel = history.maxOfOrNull { it.level } ?: 100
    val minLevel = history.minOfOrNull { it.level } ?: 0
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (history.size - 1).coerceAtLeast(1)
        val levelRange = (maxLevel - minLevel).coerceAtLeast(1)
        
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
    
    // X-axis labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        history.takeLast(5).forEach { entry ->
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
                text = "${entry.temperature}°C",
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
private fun LazyListScope.appsContent(viewModel: BatteryViewModel) {
    item {
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
    }
    
    item {
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
}

@Composable
fun AppBatteryUsageItem(appName: String, usage: Float, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
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
            Text(String.format("%.1f%%", usage), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = usage / 100f,
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
private fun LazyListScope.savingsContent(viewModel: BatteryViewModel) {
    item {
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
    }
    
    item {
        Button(
            onClick = { viewModel.openPowerSavingSettings() },
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
fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
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

@Composable
fun getHealthColor(percent: Int): Color {
    return when {
        percent >= 80 -> Color(0xFF4CAF50)
        percent >= 60 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

enum class BatteryTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DETAILS("Details", Icons.Default.Info),
    HISTORY("History", Icons.Default.History),
    APPS("Apps", Icons.Default.Apps),
    SAVINGS("Savings", Icons.Default.EnergySavingsLeaf)
}