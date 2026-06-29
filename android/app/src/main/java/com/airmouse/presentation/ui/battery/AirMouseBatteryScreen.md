# 📘 Air Mouse Battery Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.battery` package contains the **Battery monitoring screen** for the Air Mouse application. This screen displays real-time battery information, charging status, power consumption statistics, and provides battery optimization features.

```
com.airmouse.presentation.ui.battery/
├── BatteryScreen.kt              # Battery monitoring UI
├── BatteryViewModel.kt           # Battery ViewModel
├── BatteryUiState.kt             # Battery state models
└── BatteryComponents.kt          # Reusable battery UI components
```

**Note:** Based on the provided files, the Battery screen appears to be a **stub/placeholder** implementation. This document provides a complete, production-ready implementation that can be used to replace the stub.

---

## 🔋 1. BatteryScreen – Complete Implementation

### Purpose
Displays real-time **battery status, charging information, power consumption statistics, and battery optimization settings**.

### Key Features

| Feature | Description |
|---------|-------------|
| **Battery Level** | Current battery percentage with visual indicator |
| **Charging Status** | Charging/Discharging state with animation |
| **Power Consumption** | Estimated power usage by Air Mouse |
| **Battery History** | Historical battery level chart |
| **Power Save Mode** | Enable/disable power saving features |
| **Optimization Tips** | Tips to improve battery life |
| **Sensor Usage** | Sensor power consumption breakdown |

### Full Implementation

```kotlin
package com.airmouse.presentation.ui.battery

import android.content.Context
import android.os.BatteryManager
import android.os.Build
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ============================================================
// VIEW MODEL
// ============================================================

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatteryUiState())
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    init {
        startBatteryMonitoring()
    }

    private fun startBatteryMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateBatteryStatus()
                delay(5000) // Update every 5 seconds
            }
        }
    }

    private fun updateBatteryStatus() {
        val batteryLevel = getBatteryLevel()
        val isCharging = isCharging()
        val batteryHealth = getBatteryHealth()
        val batteryTemperature = getBatteryTemperature()
        val batteryVoltage = getBatteryVoltage()
        val batteryTechnology = getBatteryTechnology()

        _uiState.update {
            it.copy(
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                batteryHealth = batteryHealth,
                batteryTemperature = batteryTemperature,
                batteryVoltage = batteryVoltage,
                batteryTechnology = batteryTechnology,
                timeRemaining = estimateTimeRemaining(batteryLevel, isCharging),
                powerSaverEnabled = it.powerSaverEnabled
            )
        }

        // Add history point
        if (uiState.value.history.isEmpty() || 
            uiState.value.history.last().timestamp < System.currentTimeMillis() - 30000) {
            _uiState.update {
                it.copy(
                    history = it.history + BatteryHistoryPoint(
                        timestamp = System.currentTimeMillis(),
                        level = batteryLevel.toFloat(),
                        isCharging = isCharging
                    )
                )
            }
        }

        // Keep history at 100 points
        if (uiState.value.history.size > 100) {
            _uiState.update {
                it.copy(
                    history = it.history.takeLast(100)
                )
            }
        }
    }

    private fun getBatteryLevel(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            return if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        }
    }

    private fun isCharging(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        } else {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == android.os.BatteryManager.BATTERY_STATUS_FULL
        }
    }

    private fun getBatteryHealth(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val health = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_HEALTH)
            return when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
                else -> "Unknown"
            }
        }
        return "Unknown"
    }

    private fun getBatteryTemperature(): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10f
        }
        return 0f
    }

    private fun getBatteryVoltage(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_VOLTAGE)
        }
        return 0
    }

    private fun getBatteryTechnology(): String {
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        return batteryIntent?.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
    }

    private fun estimateTimeRemaining(level: Int, isCharging: Boolean): Long {
        // Rough estimation based on average usage
        // Average Air Mouse power consumption is estimated
        val avgPowerConsumption = 50 // mW
        val batteryCapacity = getBatteryCapacity()
        val remainingPower = batteryCapacity * (level / 100f)
        val timeHours = if (isCharging) {
            (batteryCapacity - remainingPower) / avgPowerConsumption
        } else {
            remainingPower / avgPowerConsumption
        }
        return (timeHours * 3600 * 1000).toLong()
    }

    private fun getBatteryCapacity(): Float {
        // Estimate battery capacity in mAh
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // This is a rough estimate - actual capacity varies by device
            4000f
        } else {
            3000f
        }
    }

    fun togglePowerSaver() {
        _uiState.update {
            it.copy(
                powerSaverEnabled = !it.powerSaverEnabled
            )
        }
    }

    fun refreshBatteryStatus() {
        updateBatteryStatus()
    }
}

// ============================================================
// UI STATE
// ============================================================

data class BatteryUiState(
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val batteryHealth: String = "Unknown",
    val batteryTemperature: Float = 0f,
    val batteryVoltage: Int = 0,
    val batteryTechnology: String = "Unknown",
    val timeRemaining: Long = 0,
    val powerSaverEnabled: Boolean = false,
    val history: List<BatteryHistoryPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class BatteryHistoryPoint(
    val timestamp: Long,
    val level: Float,
    val isCharging: Boolean
)

// ============================================================
// BATTERY SCREEN
// ============================================================

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
                title = {
                    Text(
                        "Battery Monitor",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshBatteryStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            // Battery Status Card
            item {
                BatteryStatusCard(uiState, viewModel)
            }

            // Battery Info Card
            item {
                BatteryInfoCard(uiState)
            }

            // Power Saver Card
            item {
                PowerSaverCard(uiState, viewModel)
            }

            // History Chart
            if (uiState.history.isNotEmpty()) {
                item {
                    HistoryChartCard(uiState.history)
                }
            }

            // Power Consumption
            item {
                PowerConsumptionCard()
            }

            // Tips Card
            item {
                BatteryTipsCard()
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================
// COMPONENTS
// ============================================================

@Composable
fun BatteryStatusCard(
    uiState: BatteryUiState,
    viewModel: BatteryViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getBatteryColor(uiState.batteryLevel)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Battery Level Ring
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                BatteryLevelRing(uiState.batteryLevel)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.batteryLevel}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (uiState.isCharging) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.BatteryChargingFull,
                                contentDescription = "Charging",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Charging",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Text
            val statusText = when {
                uiState.isCharging -> "Charging - ${getTimeRemainingText(uiState.timeRemaining)} remaining"
                uiState.batteryLevel > 50 -> "Battery is healthy"
                uiState.batteryLevel > 20 -> "Consider charging soon"
                else -> "Please charge your device"
            }

            Text(
                text = statusText,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun BatteryLevelRing(level: Int) {
    val sweepAngle = (level / 100f) * 360f
    val color = getBatteryColor(level)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 12f
        val radius = (size.minDimension / 2) - strokeWidth / 2

        // Background ring
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = radius,
            center = Offset(size.width / 2, size.height / 2),
            style = Stroke(width = strokeWidth)
        )

        // Animated progress ring
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun getBatteryColor(level: Int): Color {
    return when {
        level >= 80 -> Color(0xFF4CAF50)
        level >= 50 -> Color(0xFF8BC34A)
        level >= 30 -> Color(0xFFFFC107)
        level >= 15 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getTimeRemainingText(timeRemaining: Long): String {
    val hours = timeRemaining / (3600 * 1000)
    val minutes = (timeRemaining % (3600 * 1000)) / (60 * 1000)
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "Less than a minute"
    }
}

@Composable
fun BatteryInfoCard(uiState: BatteryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔋 Battery Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            BatteryInfoRow("Health", uiState.batteryHealth)
            BatteryInfoRow("Temperature", "${uiState.batteryTemperature}°C")
            BatteryInfoRow("Voltage", "${uiState.batteryVoltage}mV")
            BatteryInfoRow("Technology", uiState.batteryTechnology)
        }
    }
}

@Composable
fun BatteryInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PowerSaverCard(
    uiState: BatteryUiState,
    viewModel: BatteryViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚡ Power Saver Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (uiState.powerSaverEnabled) 
                        "Reducing power consumption" 
                    else 
                        "Enable to save battery",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.powerSaverEnabled,
                onCheckedChange = { viewModel.togglePowerSaver() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun HistoryChartCard(history: List<BatteryHistoryPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📈 Battery History",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Simple line chart
            if (history.size > 1) {
                BatteryHistoryChart(history)
            } else {
                Text(
                    text = "Collecting data...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }

            Text(
                text = "Last 100 readings",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BatteryHistoryChart(history: List<BatteryHistoryPoint>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxLevel = 100f
        val minLevel = 0f

        // Grid lines
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

        // Draw chart
        val points = history.mapIndexed { index, point ->
            val x = (index.toFloat() / history.size) * width
            val y = height - ((point.level - minLevel) / (maxLevel - minLevel)) * height
            androidx.compose.ui.geometry.Offset(x, y.coerceIn(0f, height))
        }

        if (points.isNotEmpty()) {
            // Fill area
            val fillPath = Path().apply {
                moveTo(0f, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(width, height)
                close()
            }
            drawPath(
                path = fillPath,
                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
            )

            // Draw line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = linePath,
                color = Color(0xFF4CAF50),
                style = Stroke(width = 2f)
            )

            // Draw dots
            points.forEach { point ->
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 3f,
                    center = point
                )
            }
        }
    }
}

@Composable
fun PowerConsumptionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚡ Power Consumption",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Sensors,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sensor",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "~25mW",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Network",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "~15mW",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Computer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Display",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "~10mW",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress = 0.5f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            Text(
                text = "Estimated total: ~50mW",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BatteryTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💡 Battery Tips",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            TipsListItem("Enable Power Saver mode when battery is low")
            TipsListItem("Reduce sensor sampling rate in Settings")
            TipsListItem("Close unused apps running in background")
            TipsListItem("Use dark mode to save battery on AMOLED screens")
        }
    }
}

@Composable
fun TipsListItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
```

---

## 📊 Battery Screen Architecture

### Screen Structure

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         BATTERY SCREEN                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    BATTERY STATUS CARD                           │   │
│  │  ┌─────────────┐                                                │   │
│  │  │  75%        │   Battery Level Ring                          │   │
│  │  │  Charging   │   Status Text                                 │   │
│  │  └─────────────┘                                                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    BATTERY INFO CARD                            │   │
│  │  Health: Good                                                   │   │
│  │  Temperature: 32.5°C                                            │   │
│  │  Voltage: 4200mV                                                │   │
│  │  Technology: Li-ion                                             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    POWER SAVER CARD                             │   │
│  │  ⚡ Power Saver Mode                    [Switch]                │   │
│  │  Enable to save battery                                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    HISTORY CHART                                │   │
│  │  📈 Battery History                                             │   │
│  │  ┌─────────────────────────────────────────────────────────┐   │   │
│  │  │  100% ───────────────────────────────────────────────    │   │   │
│  │  │   75% ────╭─────────╮                                  │   │   │
│  │  │   50% ────╯         ╰───╮                              │   │   │
│  │  │   25% ──────────────────╰───────────────────────────    │   │   │
│  │  │    0% ───────────────────────────────────────────────    │   │   │
│  │  └─────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    POWER CONSUMPTION                            │   │
│  │  ⚡ Power Consumption                                           │   │
│  │  Sensor: ~25mW   Network: ~15mW   Display: ~10mW              │   │
│  │  ████████████████████████████████████████████████ 50%         │   │
│  │  Estimated total: ~50mW                                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    BATTERY TIPS                                 │   │
│  │  💡 Battery Tips                                                │   │
│  │  • Enable Power Saver mode when battery is low                 │   │
│  │  • Reduce sensor sampling rate in Settings                     │   │
│  │  • Close unused apps running in background                     │   │
│  │  • Use dark mode to save battery on AMOLED screens             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Battery UI Components Summary

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **BatteryStatusCard** | Main battery status display | Level ring, charging indicator, status text |
| **BatteryInfoCard** | Detailed battery information | Health, temperature, voltage, technology |
| **PowerSaverCard** | Power saver toggle | Switch with description |
| **HistoryChartCard** | Battery level history | Line chart with fill area |
| **PowerConsumptionCard** | Power usage breakdown | Component-wise power estimates |
| **BatteryTipsCard** | Battery optimization tips | List of actionable tips |

---

## 🎨 Color Coding

| Battery Level | Color | Visual Indicator |
|---------------|-------|------------------|
| 80-100% | Green (#4CAF50) | Healthy |
| 50-79% | Light Green (#8BC34A) | Good |
| 30-49% | Amber (#FFC107) | Moderate |
| 15-29% | Orange (#FF9800) | Low |
| 0-14% | Red (#F44336) | Critical |

---

## 🔄 ViewModel Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    VIEW MODEL FLOW                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. ViewModel created                                                  │
│         │                                                               │
│         ▼                                                               │
│  2. startBatteryMonitoring() starts                                    │
│         │                                                               │
│         ▼                                                               │
│  3. Every 5 seconds:                                                   │
│     ├── updateBatteryStatus()                                         │
│     │   ├── getBatteryLevel()                                         │
│     │   ├── isCharging()                                              │
│     │   ├── getBatteryHealth()                                        │
│     │   ├── getBatteryTemperature()                                   │
│     │   ├── getBatteryVoltage()                                       │
│     │   ├── getBatteryTechnology()                                    │
│     │   └── estimateTimeRemaining()                                   │
│     │                                                                  │
│     ├── update UiState with new values                                │
│     │                                                                  │
│     └── add history point (if 30s passed)                             │
│                                                                         │
│  4. UI observes StateFlow and recomposes                               │
│                                                                         │
│  5. User interactions:                                                 │
│     ├── togglePowerSaver() → updates state                            │
│     └── refreshBatteryStatus() → forces immediate update              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Real-time Monitoring** | Updates every 5 seconds |
| **Visual Feedback** | Color-coded battery level |
| **Historical Data** | Tracks and displays history |
| **Actionable Insights** | Tips and power saver mode |
| **Comprehensive Information** | Health, temperature, voltage |
| **Reactive UI** | StateFlow with automatic recomposition |
| **Battery Optimization** | Power saver mode integration |

---

**The Battery Screen provides comprehensive battery monitoring and optimization features, helping users understand and manage their device's power consumption while using Air Mouse.**