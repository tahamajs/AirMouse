# 📘 Air Mouse Statistics Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.statistics` package contains the **Statistics screen** for the Air Mouse application. This screen provides comprehensive usage analytics, session statistics, gesture breakdowns, and performance metrics to help users understand their usage patterns.

```
com.airmouse.presentation.ui.statistics/
├── StatisticsScreen.kt              # Main statistics UI
├── StatisticsViewModel.kt           # Statistics ViewModel
├── StatisticsUiState.kt             # Statistics state models
├── StatisticsComponents.kt          # Reusable statistics UI components
└── StatisticsConstants.kt           # Statistics constants
```

---

## 🎯 1. StatisticsScreen

### Purpose
Provides a **comprehensive analytics dashboard** displaying usage statistics, session data, gesture breakdowns, and performance metrics.

### Key Features

| Feature | Description |
|---------|-------------|
| **Session Stats** | Clicks, scrolls, movements, distance, speed |
| **Historical Stats** | Total gestures, most used gestures, custom gestures |
| **Daily Stats** | Daily breakdown of usage |
| **Gesture Breakdown** | Gesture type distribution |
| **Time Range Selection** | Today, Week, Month, Year, All Time |
| **Export** | Export statistics as JSON or CSV |
| **Reset** | Reset all statistics |
| **Charts** | Visual representation of data |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navigationActions: NavigationActions,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Statistics", fontWeight = FontWeight.Bold)
                        Text("Usage analytics and insights", fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleEvent(StatisticsEvent.RefreshData) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.handleEvent(StatisticsEvent.ShowExportDialog) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { viewModel.handleEvent(StatisticsEvent.ShowResetDialog) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Reset")
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
            // Time Range Selector
            item {
                TimeRangeSelector(
                    selectedRange = uiState.timeRange,
                    onRangeSelected = { viewModel.handleEvent(StatisticsEvent.SelectTimeRange(it)) }
                )
            }

            // Session Overview
            item {
                SessionOverviewCard(uiState)
            }

            // Performance Stats
            item {
                PerformanceStatsCard(uiState)
            }

            // Gesture Breakdown
            item {
                GestureBreakdownCard(uiState)
            }

            // Daily Stats Chart
            item {
                DailyStatsChart(uiState)
            }

            // Connection Stats
            item {
                ConnectionStatsCard(uiState)
            }

            // Calibration Stats
            item {
                CalibrationStatsCard(uiState)
            }

            // System Stats
            item {
                SystemStatsCard(uiState)
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Export Dialog
    if (uiState.showExportDialog) {
        ExportDialog(
            onDismiss = { viewModel.handleEvent(StatisticsEvent.DismissExportDialog) },
            onExport = { format ->
                viewModel.handleEvent(StatisticsEvent.ExportData)
            }
        )
    }

    // Reset Dialog
    if (uiState.showResetDialog) {
        ResetDialog(
            onDismiss = { viewModel.handleEvent(StatisticsEvent.DismissResetDialog) },
            onConfirm = { viewModel.handleEvent(StatisticsEvent.ConfirmReset) }
        )
    }
}
```

---

## 🎯 2. StatisticsUiState

### Purpose
Defines the **complete state model** for the statistics screen.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `sessionTime` | Long | Current session duration in seconds |
| `sessionStartTime` | Long | Session start timestamp |
| `lastActivityTime` | Long | Last activity timestamp |
| `clicks` | Int | Total clicks in session |
| `doubleClicks` | Int | Total double clicks |
| `rightClicks` | Int | Total right clicks |
| `scrolls` | Int | Total scrolls |
| `gesturesDetected` | Int | Total gestures detected |
| `customGesturesUsed` | Int | Total custom gestures used |
| `mostUsedGesture` | String | Most used gesture name |
| `mostUsedGestureCount` | Int | Count of most used gesture |
| `gestureTypeCount` | Int | Number of unique gesture types |
| `customGestureCount` | Int | Number of custom gestures |
| `gestureBreakdown` | Map<String, Int> | Breakdown by gesture type |
| `totalDistanceMoved` | Float | Total cursor distance in units |
| `averageSpeed` | Float | Average speed in units/s |
| `peakSpeed` | Float | Peak speed in units/s |
| `totalMovements` | Int | Total movement events |
| `connectionAttempts` | Int | Total connection attempts |
| `successfulConnections` | Int | Successful connections |
| `failedConnections` | Int | Failed connections |
| `averagePing` | Int | Average ping in ms |
| `calibrationComplete` | Boolean | Whether calibration is complete |
| `lastCalibrationTime` | Long | Last calibration timestamp |
| `calibrationCount` | Int | Number of calibrations |
| `calibrationSuccessRate` | Float | Calibration success rate |
| `isTracking` | Boolean | Whether tracking is active |
| `touchpadActive` | Boolean | Whether touchpad is active |
| `presentationModeEnabled` | Boolean | Whether presentation mode is enabled |
| `autoConnect` | Boolean | Auto-connect setting |
| `useWebSocket` | Boolean | WebSocket setting |
| `useUdpDiscovery` | Boolean | UDP discovery setting |
| `theme` | String | Current theme |
| `language` | String | Current language |
| `batteryUsage` | Int | Battery usage percentage |
| `cpuUsage` | Float | CPU usage percentage |
| `memoryUsage` | Float | Memory usage percentage |
| `temperature` | Float | Device temperature in °C |
| `timeRange` | TimeRange | Selected time range |
| `dailyStats` | List<DailyStats> | Daily statistics |
| `summaryStats` | StatisticsSummary | Summary statistics |
| `showExportDialog` | Boolean | Whether export dialog is shown |
| `showResetDialog` | Boolean | Whether reset dialog is shown |
| `isLoading` | Boolean | Whether loading data |
| `error` | String? | Error message if any |
| `success` | String? | Success message if any |

### Enums

```kotlin
enum class TimeRange(
    val displayName: String,
    val days: Int
) {
    TODAY("Today", 1),
    WEEK("Week", 7),
    MONTH("Month", 30),
    YEAR("Year", 365),
    ALL_TIME("All Time", -1)
}

enum class ChartType(
    val displayName: String
) {
    BAR("Bar Chart"),
    LINE("Line Chart"),
    PIE("Pie Chart")
}
```

---

## 🧩 3. UI Components

### TimeRangeSelector

```kotlin
@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
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
                text = "📅 Time Range",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onRangeSelected(range) },
                        label = { Text(range.displayName, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}
```

### SessionOverviewCard

```kotlin
@Composable
fun SessionOverviewCard(uiState: StatisticsUiState) {
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
                text = "📊 Session Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(
                    label = "Session Time",
                    value = formatDuration(uiState.sessionTime),
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Clicks",
                    value = uiState.clicks.toString(),
                    icon = Icons.Default.Mouse,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Scrolls",
                    value = uiState.scrolls.toString(),
                    icon = Icons.Default.SwapVert,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Gestures",
                    value = uiState.gesturesDetected.toString(),
                    icon = Icons.Default.Gesture,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(
                    label = "Movements",
                    value = uiState.totalMovements.toString(),
                    icon = Icons.Default.DirectionsRun,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Distance",
                    value = "${uiState.totalDistanceMoved.toInt()} units",
                    icon = Icons.Default.Map,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Avg Speed",
                    value = String.format("%.1f", uiState.averageSpeed),
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Peak Speed",
                    value = String.format("%.1f", uiState.peakSpeed),
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
```

### PerformanceStatsCard

```kotlin
@Composable
fun PerformanceStatsCard(uiState: StatisticsUiState) {
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
                text = "⚡ Performance",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerformanceItem(
                    label = "Battery",
                    value = "${uiState.batteryUsage}%",
                    progress = uiState.batteryUsage / 100f,
                    color = getBatteryColor(uiState.batteryUsage),
                    modifier = Modifier.weight(1f)
                )
                PerformanceItem(
                    label = "CPU",
                    value = "${uiState.cpuUsage.toInt()}%",
                    progress = uiState.cpuUsage / 100f,
                    color = getCpuColor(uiState.cpuUsage),
                    modifier = Modifier.weight(1f)
                )
                PerformanceItem(
                    label = "Memory",
                    value = "${uiState.memoryUsage.toInt()}%",
                    progress = uiState.memoryUsage / 100f,
                    color = getMemoryColor(uiState.memoryUsage),
                    modifier = Modifier.weight(1f)
                )
                PerformanceItem(
                    label = "Temperature",
                    value = "${uiState.temperature.toInt()}°C",
                    progress = (uiState.temperature / 50f).coerceIn(0f, 1f),
                    color = getTemperatureColor(uiState.temperature),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
```

### GestureBreakdownCard

```kotlin
@Composable
fun GestureBreakdownCard(uiState: StatisticsUiState) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Gesture Breakdown",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.gestureTypeCount} types",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Top gestures
            if (uiState.gestureBreakdown.isNotEmpty()) {
                uiState.gestureBreakdown.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .forEach { (gesture, count) ->
                        GestureBreakdownItem(
                            gesture = gesture,
                            count = count,
                            total = uiState.gesturesDetected
                        )
                    }

                if (uiState.gestureBreakdown.size > 5) {
                    Text(
                        text = "+ ${uiState.gestureBreakdown.size - 5} more gestures",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No gesture data available",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Most used gesture
            if (uiState.mostUsedGesture.isNotEmpty()) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most Used:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.mostUsedGesture,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${uiState.mostUsedGestureCount}x",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

### DailyStatsChart

```kotlin
@Composable
fun DailyStatsChart(uiState: StatisticsUiState) {
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
                text = "📈 Daily Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (uiState.dailyStats.isNotEmpty()) {
                // Chart
                LineChart(
                    data = uiState.dailyStats.map { it.gestures.toFloat() },
                    labels = uiState.dailyStats.map { it.date },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = "Gestures"
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.secondary,
                        label = "Clicks"
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = "Scrolls"
                    )
                }
            } else {
                Text(
                    text = "No daily data available for the selected range",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}
```

### ConnectionStatsCard

```kotlin
@Composable
fun ConnectionStatsCard(uiState: StatisticsUiState) {
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
                text = "🌐 Connection Stats",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(
                    label = "Attempts",
                    value = uiState.connectionAttempts.toString(),
                    icon = Icons.Default.ConnectWithoutContact,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Successful",
                    value = uiState.successfulConnections.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Failed",
                    value = uiState.failedConnections.toString(),
                    icon = Icons.Default.Error,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Avg Ping",
                    value = "${uiState.averagePing}ms",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }

            // Success rate
            if (uiState.connectionAttempts > 0) {
                val successRate = uiState.successfulConnections.toFloat() / uiState.connectionAttempts * 100
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Success Rate:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${successRate.toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            successRate >= 80 -> Color(0xFF4CAF50)
                            successRate >= 50 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
                LinearProgressIndicator(
                    progress = successRate / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        successRate >= 80 -> Color(0xFF4CAF50)
                        successRate >= 50 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            }
        }
    }
}
```

### CalibrationStatsCard

```kotlin
@Composable
fun CalibrationStatsCard(uiState: StatisticsUiState) {
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
                text = "🔧 Calibration Stats",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(
                    label = "Status",
                    value = if (uiState.calibrationComplete) "✅ Complete" else "❌ Not Calibrated",
                    icon = if (uiState.calibrationComplete) Icons.Default.CheckCircle else Icons.Default.Tune,
                    color = if (uiState.calibrationComplete) Color(0xFF4CAF50) else Color(0xFFFFC107),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Attempts",
                    value = uiState.calibrationCount.toString(),
                    icon = Icons.Default.Repeat,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Success Rate",
                    value = "${uiState.calibrationSuccessRate.toInt()}%",
                    icon = Icons.Default.TrendingUp,
                    color = when {
                        uiState.calibrationSuccessRate >= 80 -> Color(0xFF4CAF50)
                        uiState.calibrationSuccessRate >= 50 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.lastCalibrationTime > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Last Calibration:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(uiState.lastCalibrationTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
```

### SystemStatsCard

```kotlin
@Composable
fun SystemStatsCard(uiState: StatisticsUiState) {
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
                text = "⚙️ System Stats",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SystemStatItem(
                    label = "Touchpad",
                    value = if (uiState.touchpadActive) "Active" else "Inactive",
                    icon = Icons.Default.TouchApp,
                    color = if (uiState.touchpadActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                SystemStatItem(
                    label = "Presentation Mode",
                    value = if (uiState.presentationModeEnabled) "On" else "Off",
                    icon = Icons.Default.Slideshow,
                    color = if (uiState.presentationModeEnabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                SystemStatItem(
                    label = "Auto Connect",
                    value = if (uiState.autoConnect) "On" else "Off",
                    icon = Icons.Default.Wifi,
                    color = if (uiState.autoConnect) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SystemStatItem(
                    label = "WebSocket",
                    value = if (uiState.useWebSocket) "Enabled" else "Disabled",
                    icon = Icons.Default.Web,
                    color = if (uiState.useWebSocket) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                SystemStatItem(
                    label = "UDP Discovery",
                    value = if (uiState.useUdpDiscovery) "Enabled" else "Disabled",
                    icon = Icons.Default.Search,
                    color = if (uiState.useUdpDiscovery) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                SystemStatItem(
                    label = "Tracking",
                    value = if (uiState.isTracking) "Active" else "Inactive",
                    icon = Icons.Default.TrackChanges,
                    color = if (uiState.isTracking) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Theme: ${uiState.theme}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Language: ${uiState.language}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### Helper Components

```kotlin
@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PerformanceItem(
    label: String,
    value: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SystemStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

## 📊 Statistics Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    STATISTICS SCREEN FLOW                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. Screen opens → ViewModel loads data                                │
│         │                                                               │
│         ▼                                                               │
│  2. Data loaded from StatisticsRepository                              │
│         │                                                               │
│         ▼                                                               │
│  3. UI displays:                                                       │
│     ├── Session Stats (session time, clicks, scrolls)                 │
│     ├── Performance Stats (battery, CPU, memory)                      │
│     ├── Gesture Breakdown (gesture types, most used)                  │
│     ├── Daily Stats Chart                                             │
│     ├── Connection Stats (attempts, success rate, ping)               │
│     └── Calibration Stats (status, attempts, success rate)            │
│                                                                         │
│  4. User selects time range                                            │
│         │                                                               │
│         ▼                                                               │
│  5. ViewModel reloads data for selected range                          │
│         │                                                               │
│         ▼                                                               │
│  6. UI updates with new data                                           │
│                                                                         │
│  7. User exports statistics                                            │
│         │                                                               │
│         ▼                                                               │
│  8. Statistics exported as JSON/CSV                                    │
│                                                                         │
│  9. User resets statistics                                             │
│         │                                                               │
│         ▼                                                               │
│  10. All statistics cleared                                            │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Utility Functions

```kotlin
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
        minutes > 0 -> String.format("%02d:%02d", minutes, secs)
        else -> String.format("%02ds", secs)
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return format.format(date)
}

private fun getBatteryColor(level: Int): Color = when {
    level >= 80 -> Color(0xFF4CAF50)
    level >= 50 -> Color(0xFF8BC34A)
    level >= 30 -> Color(0xFFFFC107)
    level >= 15 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}

private fun getCpuColor(usage: Float): Color = when {
    usage < 30 -> Color(0xFF4CAF50)
    usage < 60 -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
}

private fun getMemoryColor(usage: Float): Color = when {
    usage < 50 -> Color(0xFF4CAF50)
    usage < 80 -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
}

private fun getTemperatureColor(temp: Float): Color = when {
    temp < 30 -> Color(0xFF4CAF50)
    temp < 40 -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
}
```

---

## 📋 Summary Table

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **SessionOverviewCard** | Session statistics | Time, clicks, scrolls, gestures, movements |
| **PerformanceStatsCard** | System performance | Battery, CPU, memory, temperature |
| **GestureBreakdownCard** | Gesture analysis | Types, counts, most used |
| **DailyStatsChart** | Daily activity | Line chart with gestures/clicks/scrolls |
| **ConnectionStatsCard** | Network stats | Attempts, success rate, ping |
| **CalibrationStatsCard** | Calibration stats | Status, attempts, success rate |
| **SystemStatsCard** | System status | Touchpad, presentation, auto-connect |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Data Visualization** | Charts, progress bars, color coding |
| **Real-time Updates** | Live data from StatisticsRepository |
| **Time Range Selection** | Filter data by Today/Week/Month/Year |
| **Export** | Export as JSON or CSV |
| **Reset** | Reset all statistics |
| **Comprehensive** | 30+ metrics displayed |
| **Color Coding** | Visual indicators for status |

---

**The Statistics Screen provides comprehensive usage analytics, helping users understand their Air Mouse usage patterns, performance, and system health.**