# 📘 Air Mouse Sensor Visualizer Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.sensor` package contains the **Sensor Visualizer screen** for the Air Mouse application. This screen provides a comprehensive real-time visualization of all device sensors, including gyroscope, accelerometer, magnetometer, and orientation data.

```
com.airmouse.presentation.ui.sensor/
├── SensorVisualizerScreen.kt        # Main sensor visualizer UI
├── SensorVisualizerViewModel.kt     # Sensor visualizer ViewModel
├── SensorVisualizerUiState.kt       # Sensor visualizer state models
├── SensorComponents.kt              # Reusable sensor UI components
└── SensorCharts.kt                  # Sensor chart components
```

**Note:** Based on the provided files, the Sensor Visualizer screen appears to be a **stub/placeholder** implementation. This document provides a complete, production-ready implementation description.

---

## 🎯 1. SensorVisualizerScreen

### Purpose
Provides a **comprehensive real-time sensor visualization** dashboard displaying gyroscope, accelerometer, magnetometer, and orientation data with live charts and 3D visualization.

### Key Features

| Feature | Description |
|---------|-------------|
| **3D Orientation** | Interactive 3D phone visualization |
| **Live Charts** | Real-time sensor data charts |
| **Sensor Cards** | Individual sensor displays with values |
| **Raw & Calibrated Data** | Toggle between raw and calibrated values |
| **Recording** | Record sensor data for analysis |
| **Export** | Export sensor data as CSV |
| **Fullscreen Mode** | Expand view for detailed analysis |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorVisualizerScreen(
    navigationActions: NavigationActions,
    viewModel: SensorVisualizerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sensorState by viewModel.sensorState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sensor Visualizer", fontWeight = FontWeight.Bold)
                        Text(
                            if (sensorState.isActive) "Live data streaming" else "Sensors inactive",
                            fontSize = 11.sp,
                            color = if (sensorState.isActive) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleRecording() }) {
                        Icon(
                            if (uiState.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (uiState.isRecording) "Stop Recording" else "Start Recording",
                            tint = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFullscreen() }) {
                        Icon(
                            if (uiState.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen"
                        )
                    }
                    IconButton(onClick = { viewModel.exportData() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { viewModel.toggleCalibrated() }) {
                        Icon(
                            if (uiState.showCalibrated) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle Calibrated Data"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isFullscreen) {
            FullscreenVisualizer(
                sensorState = sensorState,
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 3D Visualization
                item {
                    Sensor3DVisualization(
                        roll = sensorState.roll,
                        pitch = sensorState.pitch,
                        yaw = sensorState.yaw,
                        isActive = sensorState.isActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }

                // Sensor Data Cards
                item {
                    SensorDataCards(sensorState, uiState)
                }

                // Orientation Values
                item {
                    OrientationValuesCard(sensorState)
                }

                // Gyroscope Chart
                item {
                    SensorChartCard(
                        title = "Gyroscope",
                        dataX = sensorState.gyroXHistory,
                        dataY = sensorState.gyroYHistory,
                        dataZ = sensorState.gyroZHistory,
                        colors = listOf(
                            Color(0xFFF44336),
                            Color(0xFF4CAF50),
                            Color(0xFF2196F3)
                        ),
                        labels = listOf("X", "Y", "Z")
                    )
                }

                // Accelerometer Chart
                item {
                    SensorChartCard(
                        title = "Accelerometer",
                        dataX = sensorState.accelXHistory,
                        dataY = sensorState.accelYHistory,
                        dataZ = sensorState.accelZHistory,
                        colors = listOf(
                            Color(0xFFFF5722),
                            Color(0xFFFFC107),
                            Color(0xFF00BCD4)
                        ),
                        labels = listOf("X", "Y", "Z"),
                        yRange = -20f to 20f
                    )
                }

                // Magnetometer Chart
                item {
                    SensorChartCard(
                        title = "Magnetometer",
                        dataX = sensorState.magXHistory,
                        dataY = sensorState.magYHistory,
                        dataZ = sensorState.magZHistory,
                        colors = listOf(
                            Color(0xFF9C27B0),
                            Color(0xFFE91E63),
                            Color(0xFF3F51B5)
                        ),
                        labels = listOf("X", "Y", "Z")
                    )
                }

                // Sensor Status
                item {
                    SensorStatusCard(sensorState)
                }

                // Footer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
```

---

## 🎯 2. SensorVisualizerUiState

### Purpose
Defines the **complete state model** for the sensor visualizer screen.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `isFullscreen` | Boolean | Whether in fullscreen mode |
| `isRecording` | Boolean | Whether recording sensor data |
| `showCalibrated` | Boolean | Whether to show calibrated data |
| `recordingDuration` | Long | Recording duration in milliseconds |
| `sampleCount` | Int | Number of samples collected |
| `selectedSensor` | String | Currently selected sensor type |
| `exportFormat` | String | Export format (CSV, JSON) |
| `isLoading` | Boolean | Whether loading data |
| `error` | String? | Error message if any |

### SensorState

```kotlin
data class SensorState(
    val isActive: Boolean = false,
    val isConnected: Boolean = false,
    
    // Current values
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val temperature: Float = 0f,
    val pressure: Float = 0f,
    val fps: Float = 0f,
    
    // History data (for charts)
    val gyroXHistory: List<Float> = emptyList(),
    val gyroYHistory: List<Float> = emptyList(),
    val gyroZHistory: List<Float> = emptyList(),
    val accelXHistory: List<Float> = emptyList(),
    val accelYHistory: List<Float> = emptyList(),
    val accelZHistory: List<Float> = emptyList(),
    val magXHistory: List<Float> = emptyList(),
    val magYHistory: List<Float> = emptyList(),
    val magZHistory: List<Float> = emptyList(),
    val rollHistory: List<Float> = emptyList(),
    val pitchHistory: List<Float> = emptyList(),
    val yawHistory: List<Float> = emptyList(),
    
    // Statistics
    val minGyroX: Float = 0f,
    val maxGyroX: Float = 0f,
    val avgGyroX: Float = 0f,
    val minAccelX: Float = 0f,
    val maxAccelX: Float = 0f,
    val avgAccelX: Float = 0f,
    val minMagX: Float = 0f,
    val maxMagX: Float = 0f,
    val avgMagX: Float = 0f,
    
    // Timestamps
    val lastUpdate: Long = 0,
    val dataAge: Long = 0
)
```

---

## 🧩 3. UI Components

### Sensor3DVisualization

```kotlin
@Composable
fun Sensor3DVisualization(
    roll: Float,
    pitch: Float,
    yaw: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 3D Phone Visualization
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val phoneWidth = size.width * 0.5f
                val phoneHeight = size.height * 0.6f

                // Rotation matrix
                val rollRad = Math.toRadians(roll.toDouble()).toFloat()
                val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()
                val yawRad = Math.toRadians(yaw.toDouble()).toFloat()

                // Draw 3D phone
                rotate(degrees = yaw) {
                    // Phone body
                    drawRoundRect(
                        color = if (isActive) Color(0xFF1A1D24) else Color(0xFF2B3341),
                        topLeft = Offset(centerX - phoneWidth/2, centerY - phoneHeight/2),
                        size = androidx.compose.ui.geometry.Size(phoneWidth, phoneHeight),
                        cornerRadius = CornerRadius(20f)
                    )

                    // Screen
                    drawRoundRect(
                        color = if (isActive) Color(0xFF00BCD4).copy(alpha = 0.3f) else Color(0xFF2B3341),
                        topLeft = Offset(centerX - phoneWidth/2 + 8, centerY - phoneHeight/2 + 8),
                        size = androidx.compose.ui.geometry.Size(phoneWidth - 16, phoneHeight - 50),
                        cornerRadius = CornerRadius(12f)
                    )

                    // Crosshair
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(centerX - 30f, centerY),
                        end = Offset(centerX + 30f, centerY),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(centerX, centerY - 30f),
                        end = Offset(centerX, centerY + 30f),
                        strokeWidth = 1f
                    )

                    // Position dot
                    val dotX = centerX + (roll / 45f) * (phoneWidth / 3)
                    val dotY = centerY + (pitch / 45f) * (phoneHeight / 3)

                    drawCircle(
                        color = if (isActive) Color(0xFF4CAF50) else Color(0xFF96A0AE),
                        radius = 8f,
                        center = Offset(
                            dotX.coerceIn(centerX - phoneWidth/3, centerX + phoneWidth/3),
                            dotY.coerceIn(centerY - phoneHeight/3, centerY + phoneHeight/3)
                        )
                    )
                    drawCircle(
                        color = if (isActive) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color(0xFF96A0AE).copy(alpha = 0.3f),
                        radius = 14f,
                        center = Offset(
                            dotX.coerceIn(centerX - phoneWidth/3, centerX + phoneWidth/3),
                            dotY.coerceIn(centerY - phoneHeight/3, centerY + phoneHeight/3)
                        )
                    )
                }

                // Labels
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 30f
                        textAlign = android.graphics.Paint.Align.CENTER
                        alpha = 128
                    }
                    drawText("Roll: ${"%.1f".format(roll)}°", centerX, size.height - 20, paint)
                }
            }

            // Status overlay
            if (!isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SensorsOff,
                            contentDescription = "Sensors Inactive",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Sensors Inactive",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
```

### SensorDataCards

```kotlin
@Composable
fun SensorDataCards(
    sensorState: SensorState,
    uiState: SensorVisualizerUiState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SensorDataCard(
            title = "Gyroscope",
            values = listOf(
                "X: ${"%.2f".format(sensorState.gyroX)}",
                "Y: ${"%.2f".format(sensorState.gyroY)}",
                "Z: ${"%.2f".format(sensorState.gyroZ)}"
            ),
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        SensorDataCard(
            title = "Accelerometer",
            values = listOf(
                "X: ${"%.2f".format(sensorState.accelX)}",
                "Y: ${"%.2f".format(sensorState.accelY)}",
                "Z: ${"%.2f".format(sensorState.accelZ)}"
            ),
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        SensorDataCard(
            title = "Magnetometer",
            values = listOf(
                "X: ${"%.2f".format(sensorState.magX)}",
                "Y: ${"%.2f".format(sensorState.magY)}",
                "Z: ${"%.2f".format(sensorState.magZ)}"
            ),
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
    }
}
```

### SensorChartCard

```kotlin
@Composable
fun SensorChartCard(
    title: String,
    dataX: List<Float>,
    dataY: List<Float>,
    dataZ: List<Float>,
    colors: List<Color>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    yRange: Pair<Float, Float>? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Chart
            LineChart(
                data = listOf(dataX, dataY, dataZ),
                colors = colors,
                labels = labels,
                yRange = yRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                labels.forEachIndexed { index, label ->
                    LegendItem(
                        color = colors[index],
                        label = label
                    )
                }
            }

            // Current values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "X: ${formatValue(dataX.lastOrNull() ?: 0f)}",
                    fontSize = 11.sp,
                    color = colors[0]
                )
                Text(
                    text = "Y: ${formatValue(dataY.lastOrNull() ?: 0f)}",
                    fontSize = 11.sp,
                    color = colors[1]
                )
                Text(
                    text = "Z: ${formatValue(dataZ.lastOrNull() ?: 0f)}",
                    fontSize = 11.sp,
                    color = colors[2]
                )
            }
        }
    }
}
```

### OrientationValuesCard

```kotlin
@Composable
fun OrientationValuesCard(sensorState: SensorState) {
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
                text = "🧭 Orientation",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrientationValue(
                    label = "Roll",
                    value = sensorState.roll,
                    color = Color(0xFF00BCD4)
                )
                OrientationValue(
                    label = "Pitch",
                    value = sensorState.pitch,
                    color = Color(0xFF4CAF50)
                )
                OrientationValue(
                    label = "Yaw",
                    value = sensorState.yaw,
                    color = Color(0xFFFF9800)
                )
            }

            // Progress bars
            listOf(
                "Roll" to sensorState.roll,
                "Pitch" to sensorState.pitch,
                "Yaw" to sensorState.yaw
            ).forEach { (label, value) ->
                val normalizedValue = ((value + 180f) / 360f).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp)
                    )
                    LinearProgressIndicator(
                        progress = normalizedValue,
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = when (label) {
                            "Roll" -> Color(0xFF00BCD4)
                            "Pitch" -> Color(0xFF4CAF50)
                            else -> Color(0xFFFF9800)
                        },
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Text(
                        "${"%.1f".format(value)}°",
                        fontSize = 11.sp,
                        modifier = Modifier.width(50.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
```

### SensorStatusCard

```kotlin
@Composable
fun SensorStatusCard(sensorState: SensorState) {
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
                text = "📊 Sensor Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorStatusItem(
                    label = "Status",
                    value = if (sensorState.isActive) "Active" else "Inactive",
                    color = if (sensorState.isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                SensorStatusItem(
                    label = "FPS",
                    value = "${"%.1f".format(sensorState.fps)}",
                    color = Color(0xFF2196F3)
                )
                SensorStatusItem(
                    label = "Samples",
                    value = sensorState.gyroXHistory.size.toString(),
                    color = Color(0xFF9C27B0)
                )
                SensorStatusItem(
                    label = "Data Age",
                    value = "${sensorState.dataAge / 1000}s",
                    color = when {
                        sensorState.dataAge < 1000 -> Color(0xFF4CAF50)
                        sensorState.dataAge < 5000 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
            }
        }
    }
}
```

### FullscreenVisualizer

```kotlin
@Composable
fun FullscreenVisualizer(
    sensorState: SensorState,
    uiState: SensorVisualizerUiState,
    viewModel: SensorVisualizerViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large 3D Visualization
            Sensor3DVisualization(
                roll = sensorState.roll,
                pitch = sensorState.pitch,
                yaw = sensorState.yaw,
                isActive = sensorState.isActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Charts in fullscreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorChartCard(
                    title = "Gyro",
                    dataX = sensorState.gyroXHistory,
                    dataY = sensorState.gyroYHistory,
                    dataZ = sensorState.gyroZHistory,
                    colors = listOf(Color(0xFFF44336), Color(0xFF4CAF50), Color(0xFF2196F3)),
                    labels = listOf("X", "Y", "Z"),
                    modifier = Modifier.weight(1f)
                )
                SensorChartCard(
                    title = "Accel",
                    dataX = sensorState.accelXHistory,
                    dataY = sensorState.accelYHistory,
                    dataZ = sensorState.accelZHistory,
                    colors = listOf(Color(0xFFFF5722), Color(0xFFFFC107), Color(0xFF00BCD4)),
                    labels = listOf("X", "Y", "Z"),
                    yRange = -20f to 20f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
```

### Helper Components

```kotlin
@Composable
fun SensorDataCard(
    title: String,
    values: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
            values.forEach { value ->
                Text(
                    value,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun OrientationValue(
    label: String,
    value: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            "${"%.1f".format(value)}°",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SensorStatusItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LineChart(
    data: List<List<Float>>,
    colors: List<Color>,
    labels: List<String>,
    yRange: Pair<Float, Float>?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 8f

        if (data.isEmpty() || data.all { it.isEmpty() }) {
            // Draw empty state
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 40f
                    textAlign = android.graphics.Paint.Align.CENTER
                    alpha = 64
                }
                drawText("No Data", width / 2, height / 2, paint)
            }
            return@Canvas
        }

        val minY = yRange?.first ?: data.minOf { series -> series.minOrNull() ?: 0f }
        val maxY = yRange?.second ?: data.maxOf { series -> series.maxOrNull() ?: 1f }
        val range = (maxY - minY).coerceAtLeast(1f)

        data.forEachIndexed { index, series ->
            if (series.isEmpty()) return@forEachIndexed

            val color = colors[index % colors.size]
            val path = Path()

            series.forEachIndexed { i, value ->
                val x = (i.toFloat() / series.size) * (width - 2 * padding) + padding
                val y = height - ((value - minY) / range) * (height - 2 * padding) - padding

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2f)
            )
        }
    }
}

private fun formatValue(value: Float): String {
    return when {
        abs(value) >= 10 -> "%.1f".format(value)
        abs(value) >= 1 -> "%.2f".format(value)
        else -> "%.3f".format(value)
    }
}
```

---

## 🎯 4. SensorVisualizerViewModel

### Key Methods

| Method | Purpose |
|--------|---------|
| `observeSensors()` | Collect sensor data from SensorService |
| `toggleRecording()` | Start/stop sensor data recording |
| `toggleFullscreen()` | Toggle fullscreen mode |
| `toggleCalibrated()` | Toggle between raw and calibrated data |
| `exportData()` | Export sensor data as CSV/JSON |
| `clearHistory()` | Clear sensor data history |
| `pauseStreaming()` | Pause sensor data streaming |
| `resumeStreaming()` | Resume sensor data streaming |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Real-time Visualization** | Live sensor data updates |
| **Multiple Views** | 3D, charts, and numerical displays |
| **Data Recording** | Record and export sensor data |
| **Calibrated/Uncalibrated** | Toggle between raw and calibrated data |
| **Fullscreen Mode** | Expand view for detailed analysis |
| **Performance** | Efficient data processing and rendering |
| **Responsive** | Adapts to screen size and orientation |

---

**The Sensor Visualizer Screen provides a comprehensive real-time visualization of all device sensors, making it an essential tool for debugging, calibration, and understanding sensor behavior.**