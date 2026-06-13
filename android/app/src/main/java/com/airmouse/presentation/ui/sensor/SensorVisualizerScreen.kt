// app/src/main/java/com/airmouse/presentation/ui/sensor/SensorVisualizerScreen.kt
package com.airmouse.presentation.ui.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.BlurEffect
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorVisualizerScreen(
    navigationActions: NavigationActions? = null
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf(SensorVisualizerUiState()) }
    var history by remember { mutableStateOf(SensorHistory()) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }

    // Sensor listener
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        val rotMat = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotMat, orientation)
                        uiState = uiState.copy(
                            roll = orientation[2], pitch = orientation[1], yaw = orientation[0]
                        )
                        if (isRecording) {
                            val dataPoint = SensorDataPoint(
                                timestamp = System.currentTimeMillis(),
                                roll = orientation[2], pitch = orientation[1], yaw = orientation[0]
                            )
                            history = history.copy(
                                dataPoints = (history.dataPoints + dataPoint).takeLast(history.maxHistorySize)
                            )
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> uiState = uiState.copy(
                        gyroX = event.values[0], gyroY = event.values[1], gyroZ = event.values[2]
                    )
                    Sensor.TYPE_ACCELEROMETER -> uiState = uiState.copy(
                        accelX = event.values[0], accelY = event.values[1], accelZ = event.values[2]
                    )
                    Sensor.TYPE_MAGNETIC_FIELD -> uiState = uiState.copy(
                        magX = event.values[0], magY = event.values[1], magZ = event.values[2]
                    )
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> uiState = uiState.copy(temperature = event.values[0])
                    Sensor.TYPE_PRESSURE -> uiState = uiState.copy(pressure = event.values[0])
                    Sensor.TYPE_LIGHT -> uiState = uiState.copy(light = event.values[0])
                    Sensor.TYPE_PROXIMITY -> uiState = uiState.copy(proximity = event.values[0])
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        } else {
            recordingDuration = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sensor Visualizer",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    if (navigationActions != null) {
                        IconButton(onClick = { navigationActions.navigateBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { uiState = uiState.copy(showAxes = !uiState.showAxes) }) {
                        Icon(
                            if (uiState.showAxes) Icons.Filled.GridOn else Icons.Outlined.GridOn,
                            contentDescription = "Toggle Axes"
                        )
                    }
                    IconButton(onClick = { uiState = uiState.copy(showGrid = !uiState.showGrid) }) {
                        Icon(
                            if (uiState.showGrid) Icons.Filled.Grid3x3 else Icons.Outlined.Grid3x3,
                            contentDescription = "Toggle Grid"
                        )
                    }
                    IconButton(onClick = { isRecording = !isRecording }) {
                        Icon(
                            if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (history.dataPoints.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { history = history.copy(dataPoints = emptyList()) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear History")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Stunning 3D Cube Card
            item {
                GlowingCard(
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🪄 3D Orientation Tracker",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Animated cube size
                        val cubeSize by animateDpAsState(
                            targetValue = 260.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        SensorCubeView(
                            roll = uiState.roll,
                            pitch = uiState.pitch,
                            yaw = uiState.yaw,
                            modifier = Modifier
                                .size(cubeSize)
                                .shadow(20.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp)),
                            showAxes = uiState.showAxes,
                            showGrid = uiState.showGrid,
                            cubeColor = Color(0xFFFF5722),
                            backgroundColor = Color(0xFF1A1A2E)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Animated orientation chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AnimatedSensorChip("Roll", uiState.roll, Color(0xFF4CAF50))
                            AnimatedSensorChip("Pitch", uiState.pitch, Color(0xFF2196F3))
                            AnimatedSensorChip("Yaw", uiState.yaw, Color(0xFFFF9800))
                        }
                    }
                }
            }

            // Sensor Selection – Beautiful animated tabs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📡 Live Sensor Stream",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Animated selection chips
                        SensorTypeChip(
                            selected = uiState.activeSensor == ActiveSensor.ORIENTATION,
                            icon = Icons.Default.RotateRight,
                            label = "Orientation",
                            onClick = { uiState = uiState.copy(activeSensor = ActiveSensor.ORIENTATION) }
                        )
                        SensorTypeChip(
                            selected = uiState.activeSensor == ActiveSensor.GYROSCOPE,
                            icon = Icons.Default.Speed,
                            label = "Gyroscope",
                            onClick = { uiState = uiState.copy(activeSensor = ActiveSensor.GYROSCOPE) }
                        )
                        SensorTypeChip(
                            selected = uiState.activeSensor == ActiveSensor.ACCELEROMETER,
                            icon = Icons.Default.AccountBalance,
                            label = "Accelerometer",
                            onClick = { uiState = uiState.copy(activeSensor = ActiveSensor.ACCELEROMETER) }
                        )
                        SensorTypeChip(
                            selected = uiState.activeSensor == ActiveSensor.MAGNETOMETER,
                            icon = Icons.Default.Explore,
                            label = "Magnetometer",
                            onClick = { uiState = uiState.copy(activeSensor = ActiveSensor.MAGNETOMETER) }
                        )
                    }
                }
            }

            // Sensor Data Display with animated bars
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "📊 Real‑time Values",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        when (uiState.activeSensor) {
                            ActiveSensor.ORIENTATION -> {
                                AnimatedSensorBar("Roll", uiState.roll, -PI.toFloat(), PI.toFloat(), "rad")
                                AnimatedSensorBar("Pitch", uiState.pitch, -PI.toFloat() / 2, PI.toFloat() / 2, "rad")
                                AnimatedSensorBar("Yaw", uiState.yaw, -PI.toFloat(), PI.toFloat(), "rad")
                            }
                            ActiveSensor.GYROSCOPE -> {
                                AnimatedSensorBar("X", uiState.gyroX, -10f, 10f, "rad/s")
                                AnimatedSensorBar("Y", uiState.gyroY, -10f, 10f, "rad/s")
                                AnimatedSensorBar("Z", uiState.gyroZ, -10f, 10f, "rad/s")
                            }
                            ActiveSensor.ACCELEROMETER -> {
                                AnimatedSensorBar("X", uiState.accelX, -20f, 20f, "m/s²")
                                AnimatedSensorBar("Y", uiState.accelY, -20f, 20f, "m/s²")
                                AnimatedSensorBar("Z", uiState.accelZ, -20f, 20f, "m/s²")
                                val magnitude = sqrt(
                                    uiState.accelX * uiState.accelX +
                                            uiState.accelY * uiState.accelY +
                                            uiState.accelZ * uiState.accelZ
                                )
                                AnimatedSensorBar("Magnitude", magnitude, 0f, 30f, "m/s²")
                            }
                            ActiveSensor.MAGNETOMETER -> {
                                AnimatedSensorBar("X", uiState.magX, -100f, 100f, "μT")
                                AnimatedSensorBar("Y", uiState.magY, -100f, 100f, "μT")
                                AnimatedSensorBar("Z", uiState.magZ, -100f, 100f, "μT")
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Environmental sensors (if available)
            if (uiState.temperature != 0f || uiState.pressure != 0f || uiState.light != 0f || uiState.proximity != 0f) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "🌿 Environmental",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.temperature != 0f) {
                                SensorInfoRow(Icons.Default.Thermostat, "Temperature", "${uiState.temperature}°C")
                            }
                            if (uiState.pressure != 0f) {
                                SensorInfoRow(Icons.Default.BarChart, "Pressure", "${uiState.pressure} hPa")
                            }
                            if (uiState.light != 0f) {
                                SensorInfoRow(Icons.Default.Lightbulb, "Light", "${uiState.light} lx")
                            }
                            if (uiState.proximity != 0f) {
                                SensorInfoRow(Icons.Default.Radar, "Proximity", "${uiState.proximity} cm")
                            }
                        }
                    }
                }
            }

            // Recording status
            if (isRecording) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val pulse by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.5f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(500, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .scale(pulse)
                                        .background(Color.Red, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recording: ${formatDuration(recordingDuration)}", fontWeight = FontWeight.Bold)
                            }
                            Text("${history.dataPoints.size} samples", fontSize = 12.sp)
                        }
                    }
                }
            }

            // History chart – beautiful line graph
            if (history.dataPoints.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "📈 Orientation History (Roll)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val points = history.dataPoints.takeLast(100)
                            SmoothLineChart(points)
                        }
                    }
                }
            }
        }
    }
}

// ==================== BEAUTIFUL COMPONENTS ====================

@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(24.dp, RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(gradientColors),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        content()
    }
}

@Composable
fun AnimatedSensorChip(label: String, value: Float, color: Color) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.2f),
            shadowElevation = 2.dp
        ) {
            Text(
                text = String.format("%.1f°", Math.toDegrees(animatedValue.toDouble())),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 16.sp
            )
        }
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun SensorTypeChip(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(targetValue = if (selected) 1.05f else 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .scale(scale)
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        if (selected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun AnimatedSensorBar(label: String, value: Float, min: Float, max: Float, unit: String) {
    val progress = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(300))
    val displayValue by animateFloatAsState(targetValue = value, animationSpec = tween(300))

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(
                text = String.format("%.2f %s", displayValue, unit),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = getColorForValue(value, min, max),
            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun SensorInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmoothLineChart(dataPoints: List<SensorDataPoint>) {
    if (dataPoints.isEmpty()) return

    val animatedAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(500))
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1)

        val maxRoll = dataPoints.maxOfOrNull { it.roll } ?: 1f
        val minRoll = dataPoints.minOfOrNull { it.roll } ?: -1f
        val range = (maxRoll - minRoll).coerceAtLeast(0.1f)

        val points = dataPoints.mapIndexed { i, point ->
            val x = i * stepX
            val y = height - ((point.roll - minRoll) / range) * height
            Offset(x, y)
        }

        // Area under curve (gradient fill)
        val path = Path().apply {
            moveTo(0f, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(width, height)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFF5722).copy(alpha = 0.3f * animatedAlpha),
                    Color(0xFFFF5722).copy(alpha = 0.05f * animatedAlpha)
                )
            )
        )

        // Line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFFFF5722).copy(alpha = animatedAlpha),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        // Points
        points.forEach { point ->
            drawCircle(
                color = Color(0xFFFF5722),
                radius = 4f,
                center = point
            )
        }

        // Grid lines
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }
    }
}

private fun getColorForValue(value: Float, min: Float, max: Float): Color {
    val t = ((value - min) / (max - min)).coerceIn(0f, 1f)
    return when {
        t < 0.33f -> Color(0xFF4CAF50)
        t < 0.66f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}