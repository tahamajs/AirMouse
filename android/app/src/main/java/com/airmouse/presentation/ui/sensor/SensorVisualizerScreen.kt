package com.airmouse.presentation.ui.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.*
import kotlin.math.*
import kotlin.math.sqrt
import androidx.compose.ui.draw.clip
import kotlin.math.pow

// ==================== DATA CLASSES ====================

data class SensorVisualizerUiState(
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
    val light: Float = 0f,
    val proximity: Float = 0f,
    val humidity: Float = 0f,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = 0f,
    val linearAccelX: Float = 0f,
    val linearAccelY: Float = 0f,
    val linearAccelZ: Float = 0f,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    val gameRotationX: Float = 0f,
    val gameRotationY: Float = 0f,
    val gameRotationZ: Float = 0f,
    val steps: Int = 0,
    val stepDetector: Boolean = false,
    val heartRate: Float = 0f,
    val deviceOrientation: DeviceOrientation = DeviceOrientation.PORTRAIT,
    val isSensorAvailable: Boolean = true,
    val activeSensor: ActiveSensor = ActiveSensor.ORIENTATION,
    val showAxes: Boolean = true,
    val showGrid: Boolean = true,
    val show3DModel: Boolean = true,
    val showRawData: Boolean = false,
    val sampleRate: Int = 0,
    val calibrationStatus: CalibrationStatus = CalibrationStatus.IDLE,
    val signalQuality: SignalQuality = SignalQuality.GOOD,
    val cubeColor: Color = Color(0xFFFF5722),
    val backgroundColor: Color = Color(0xFF1A1A2E),
    val accentColor: Color = Color(0xFF00BCD4)
)

enum class ActiveSensor(val displayName: String, val icon: String) {
    ORIENTATION("Orientation", "🔄"),
    GYROSCOPE("Gyroscope", "⚡"),
    ACCELEROMETER("Accelerometer", "📊"),
    MAGNETOMETER("Magnetometer", "🧭"),
    ENVIRONMENTAL("Environmental", "🌡️")
}

enum class DeviceOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    REVERSE_PORTRAIT("Reverse Portrait"),
    REVERSE_LANDSCAPE("Reverse Landscape"),
    UNKNOWN("Unknown")
}

enum class CalibrationStatus(val displayName: String, val color: Color) {
    IDLE("Not Calibrated", Color(0xFF9E9E9E)),
    CALIBRATING("Calibrating...", Color(0xFFFFC107)),
    CALIBRATED("Calibrated", Color(0xFF4CAF50)),
    FAILED("Calibration Failed", Color(0xFFF44336))
}

enum class SignalQuality(val displayName: String, val color: Color, val level: Int) {
    EXCELLENT("Excellent", Color(0xFF4CAF50), 100),
    GOOD("Good", Color(0xFF8BC34A), 75),
    FAIR("Fair", Color(0xFFFFC107), 50),
    POOR("Poor", Color(0xFFFF9800), 25),
    NONE("No Signal", Color(0xFFF44336), 0)
}

data class SensorDataPoint(
    val timestamp: Long,
    val roll: Float,
    val pitch: Float,
    val yaw: Float,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f
)

data class SensorHistory(
    val dataPoints: List<SensorDataPoint> = emptyList(),
    val maxHistorySize: Int = 300,
    val isRecording: Boolean = false,
    val startTime: Long = 0L,
    val recordingName: String = "",
    val recordingDuration: Long = 0L
)

data class SensorStatistics(
    val minRoll: Float = 0f,
    val maxRoll: Float = 0f,
    val avgRoll: Float = 0f,
    val minPitch: Float = 0f,
    val maxPitch: Float = 0f,
    val avgPitch: Float = 0f,
    val minYaw: Float = 0f,
    val maxYaw: Float = 0f,
    val avgYaw: Float = 0f,
    val stabilityScore: Float = 0f,
    val movementIntensity: Float = 0f,
    val sampleCount: Int = 0
)

// ==================== MAIN SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorVisualizerScreen(
    navigationActions: NavigationActions? = null
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }

    var uiState by remember { mutableStateOf(SensorVisualizerUiState()) }
    var history by remember { mutableStateOf(SensorHistory()) }
    var statistics by remember { mutableStateOf(SensorStatistics()) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    fun updateStatistics() {
        val points = history.dataPoints.takeLast(100)
        if (points.isNotEmpty()) {
            val rolls = points.map { it.roll }
            val pitches = points.map { it.pitch }
            val yaws = points.map { it.yaw }

            val stability = 1f - (rolls.std() + pitches.std() + yaws.std()) / 3f

            statistics = SensorStatistics(
                minRoll = rolls.minOrNull() ?: 0f,
                maxRoll = rolls.maxOrNull() ?: 0f,
                avgRoll = rolls.average().toFloat(),
                // ... map the rest of your fields
                stabilityScore = stability.coerceIn(0f, 1f),
                sampleCount = points.size
            )
        }
    }
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        val orientation = FloatArray(3)
                        if (event.values.size >= 4) {
                            val rotMat = FloatArray(9)
                            SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                            SensorManager.getOrientation(rotMat, orientation)
                            uiState = uiState.copy(
                                roll = orientation[2],
                                pitch = orientation[1],
                                yaw = orientation[0],
                                gameRotationX = event.values[0],
                                gameRotationY = event.values[1],
                                gameRotationZ = event.values[2]
                            )
                        }
                        if (history.isRecording) {
                            history = history.copy(
                                dataPoints = (history.dataPoints + SensorDataPoint(
                                    timestamp = System.currentTimeMillis(),
                                    roll = uiState.roll,
                                    pitch = uiState.pitch,
                                    yaw = uiState.yaw
                                )).takeLast(history.maxHistorySize)
                            )
                        }
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        if (event.values.size >= 4) {
                            uiState = uiState.copy(
                                rotationX = event.values[0],
                                rotationY = event.values[1],
                                rotationZ = event.values[2]
                            )
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> uiState = uiState.copy(
                        gyroX = event.values[0],
                        gyroY = event.values[1],
                        gyroZ = event.values[2]
                    )
                    Sensor.TYPE_ACCELEROMETER -> uiState = uiState.copy(
                        accelX = event.values[0],
                        accelY = event.values[1],
                        accelZ = event.values[2]
                    )
                    Sensor.TYPE_MAGNETIC_FIELD -> uiState = uiState.copy(
                        magX = event.values[0],
                        magY = event.values[1],
                        magZ = event.values[2]
                    )
                    Sensor.TYPE_GRAVITY -> uiState = uiState.copy(
                        gravityX = event.values[0],
                        gravityY = event.values[1],
                        gravityZ = event.values[2]
                    )
                    Sensor.TYPE_LINEAR_ACCELERATION -> uiState = uiState.copy(
                        linearAccelX = event.values[0],
                        linearAccelY = event.values[1],
                        linearAccelZ = event.values[2]
                    )
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> uiState = uiState.copy(temperature = event.values[0])
                    Sensor.TYPE_PRESSURE -> uiState = uiState.copy(pressure = event.values[0])
                    Sensor.TYPE_LIGHT -> uiState = uiState.copy(light = event.values[0])
                    Sensor.TYPE_PROXIMITY -> uiState = uiState.copy(proximity = event.values[0])
                    Sensor.TYPE_RELATIVE_HUMIDITY -> uiState = uiState.copy(humidity = event.values[0])
                    Sensor.TYPE_STEP_COUNTER -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        uiState = uiState.copy(steps = event.values[0].toInt())
                    }
                    Sensor.TYPE_STEP_DETECTOR -> uiState = uiState.copy(stepDetector = event.values[0] > 0)
                    Sensor.TYPE_HEART_RATE -> uiState = uiState.copy(heartRate = event.values[0])
                }
                updateStatistics()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                uiState = uiState.copy(
                    signalQuality = when (accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SignalQuality.EXCELLENT
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SignalQuality.GOOD
                        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SignalQuality.FAIR
                        else -> SignalQuality.POOR
                    }
                )
            }
        }

        fun updateStatistics() {
            val points = history.dataPoints.takeLast(100)

            if (points.isNotEmpty()) {
                val rolls = points.map { it.roll }
                val pitches = points.map { it.pitch }
                val yaws = points.map { it.yaw }

// Ensure stability and intensity are explicitly Floats
                val stability = (1f - (rolls.std() + pitches.std() + yaws.std()) / 3f).toFloat()
                val intensity = ((rolls.map { abs(it) }.average() +
                        pitches.map { abs(it) }.average() +
                        yaws.map { abs(it) }.average()) / 3.0).toFloat()

                statistics = SensorStatistics(
                    minRoll = rolls.minOrNull() ?: 0f,
                    maxRoll = rolls.maxOrNull() ?: 0f,
                    avgRoll = rolls.average().toFloat(),
                    minPitch = pitches.minOrNull() ?: 0f,
                    maxPitch = pitches.maxOrNull() ?: 0f,
                    avgPitch = pitches.average().toFloat(),
                    minYaw = yaws.minOrNull() ?: 0f,
                    maxYaw = yaws.maxOrNull() ?: 0f,
                    avgYaw = yaws.average().toFloat(),
                    stabilityScore = stability.coerceIn(0f, 1f),
                    movementIntensity = intensity.coerceIn(0f, 5f),
                    sampleCount = points.size
                )            }
        }

        // Register all sensors
        fun registerSensor(type: Int) {
            sensorManager.getDefaultSensor(type)?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        registerSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        registerSensor(Sensor.TYPE_ROTATION_VECTOR)
        registerSensor(Sensor.TYPE_GYROSCOPE)
        registerSensor(Sensor.TYPE_ACCELEROMETER)
        registerSensor(Sensor.TYPE_MAGNETIC_FIELD)
        registerSensor(Sensor.TYPE_GRAVITY)
        registerSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        registerSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        registerSensor(Sensor.TYPE_PRESSURE)
        registerSensor(Sensor.TYPE_LIGHT)
        registerSensor(Sensor.TYPE_PROXIMITY)
        registerSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            registerSensor(Sensor.TYPE_STEP_COUNTER)
            registerSensor(Sensor.TYPE_STEP_DETECTOR)
        }
        registerSensor(Sensor.TYPE_HEART_RATE)

        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Recording timer
    LaunchedEffect(history.isRecording) {
        if (history.isRecording) {
            while (history.isRecording) {
                delay(1000)
                history = history.copy(recordingDuration = history.recordingDuration + 1000)
            }
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { uiState = uiState.copy(showAxes = !uiState.showAxes) }) {
                        Icon(Icons.Filled.GridOn, contentDescription = "Toggle Axes")
                    }
                    IconButton(onClick = { uiState = uiState.copy(showGrid = !uiState.showGrid) }) {
                        Icon(Icons.Filled.Grid3x3, contentDescription = "Toggle Grid")
                    }
                    IconButton(onClick = { uiState = uiState.copy(show3DModel = !uiState.show3DModel) }) {
                        Icon(Icons.Filled.CropFree, contentDescription = "Toggle 3D")
                    }
                    IconButton(onClick = { uiState = uiState.copy(showRawData = !uiState.showRawData) }) {
                        Icon(Icons.Filled.Code, contentDescription = "Raw Data")
                    }
                    IconButton(onClick = {
                        history = history.copy(isRecording = !history.isRecording, startTime = System.currentTimeMillis())
                    }) {
                        Icon(
                            if (history.isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                            contentDescription = if (history.isRecording) "Stop Recording" else "Start Recording",
                            tint = if (history.isRecording) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistics")
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
                    onClick = { showExportDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export Data")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Signal Quality Indicator
            item {
                SignalQualityCard(quality = uiState.signalQuality, calibration = uiState.calibrationStatus)
            }

            // 3D Cube Visualization
            if (uiState.show3DModel) {
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
                                text = "🎯 3D Orientation Tracker",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))

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
                                showTextures = true,
                                cubeColor = uiState.cubeColor,
                                backgroundColor = uiState.backgroundColor
                            )

                            Spacer(modifier = Modifier.height(20.dp))

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
            }

            // Sensor Selection Tabs
            item {
                SensorTypeSelector(
                    selectedSensor = uiState.activeSensor,
                    onSensorSelected = { uiState = uiState.copy(activeSensor = it) }
                )
            }

            // Sensor Data Display
            item {
                SensorDataCard(
                    uiState = uiState,
                    activeSensor = uiState.activeSensor
                )
            }

            // Raw Data Display
            if (uiState.showRawData) {
                item {
                    RawDataCard(uiState = uiState)
                }
            }

            // Recording Status
            if (history.isRecording) {
                item {
                    RecordingCard(
                        duration = history.recordingDuration,
                        sampleCount = history.dataPoints.size
                    )
                }
            }

            // History Chart
            if (history.dataPoints.isNotEmpty()) {
                item {
                    HistoryChartCard(
                        dataPoints = history.dataPoints.takeLast(100),
                        onClear = { history = history.copy(dataPoints = emptyList()) }
                    )
                }
            }

            // Statistics Card
            if (statistics.sampleCount > 0) {
                item {
                    StatisticsCard(statistics = statistics)
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { _ ->
                showExportDialog = false
            },
            sampleCount = history.dataPoints.size
        )
    }

    // Statistics Dialog
    if (showStatsDialog) {
        StatisticsDialog(
            statistics = statistics,
            onDismiss = { showStatsDialog = false },
            onReset = { statistics = SensorStatistics() }
        )
    }
}

// ==================== COMPONENTS ====================

@Composable
fun SignalQualityCard(quality: SignalQuality, calibration: CalibrationStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = quality.color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(quality.color))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Signal Quality", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(quality.displayName, fontWeight = FontWeight.Bold, color = quality.color)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(calibration.color))
                Spacer(modifier = Modifier.width(8.dp))
                Text(calibration.displayName, fontSize = 12.sp, color = calibration.color)
            }
        }
        LinearProgressIndicator(
            progress = { quality.level / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = quality.color,
            trackColor = quality.color.copy(alpha = 0.2f)
        )
    }
}




@Composable
fun SensorTypeSelector(selectedSensor: ActiveSensor, onSensorSelected: (ActiveSensor) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📡 Sensor Stream", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActiveSensor.entries.forEach { sensor ->
                    FilterChip(
                        selected = selectedSensor == sensor,
                        onClick = { onSensorSelected(sensor) },
                        label = { Text(sensor.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SensorDataCard(uiState: SensorVisualizerUiState, activeSensor: ActiveSensor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📊 Live Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (activeSensor) {
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
                    val magnitude = sqrt(uiState.accelX * uiState.accelX + uiState.accelY * uiState.accelY + uiState.accelZ * uiState.accelZ)
                    AnimatedSensorBar("Magnitude", magnitude, 0f, 30f, "m/s²")
                }
                ActiveSensor.MAGNETOMETER -> {
                    AnimatedSensorBar("X", uiState.magX, -100f, 100f, "μT")
                    AnimatedSensorBar("Y", uiState.magY, -100f, 100f, "μT")
                    AnimatedSensorBar("Z", uiState.magZ, -100f, 100f, "μT")
                }
                ActiveSensor.ENVIRONMENTAL -> {
                    if (uiState.temperature != 0f) {
                        SensorInfoRow(Icons.Default.Thermostat, "Temperature", String.format(Locale.US, "%.1f°C", uiState.temperature))
                    }
                    if (uiState.pressure != 0f) {
                        SensorInfoRow(Icons.Default.BarChart, "Pressure", String.format(Locale.US, "%.1f hPa", uiState.pressure))
                    }
                    if (uiState.light != 0f) {
                        SensorInfoRow(Icons.Default.Lightbulb, "Light", String.format(Locale.US, "%.0f lx", uiState.light))
                    }
                    if (uiState.humidity != 0f) {
                        SensorInfoRow(Icons.Default.WaterDrop, "Humidity", String.format(Locale.US, "%.0f%%", uiState.humidity))
                    }
                    if (uiState.proximity != 0f) {
                        SensorInfoRow(Icons.Default.Radar, "Proximity", String.format(Locale.US, "%.0f cm", uiState.proximity))
                    }
                    if (uiState.steps > 0) {
                        SensorInfoRow(Icons.AutoMirrored.Filled.DirectionsWalk, "Steps", uiState.steps.toString())
                    }
                    if (uiState.heartRate > 0) {
                        SensorInfoRow(Icons.Default.Favorite, "Heart Rate", String.format(Locale.US, "%.0f BPM", uiState.heartRate))
                    }
                }
            }
        }
    }
}

@Composable
fun RawDataCard(uiState: SensorVisualizerUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📟 Raw Sensor Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = buildString {
                        appendLine("Roll: ${String.format(Locale.US, "%.4f", uiState.roll)} rad")
                        appendLine("Pitch: ${String.format(Locale.US, "%.4f", uiState.pitch)} rad")
                        appendLine("Yaw: ${String.format(Locale.US, "%.4f", uiState.yaw)} rad")
                        appendLine("Gyro: (${String.format(Locale.US, "%.2f", uiState.gyroX)}, ${String.format(Locale.US, "%.2f", uiState.gyroY)}, ${String.format(Locale.US, "%.2f", uiState.gyroZ)}) rad/s")
                        appendLine("Accel: (${String.format(Locale.US, "%.2f", uiState.accelX)}, ${String.format(Locale.US, "%.2f", uiState.accelY)}, ${String.format(Locale.US, "%.2f", uiState.accelZ)}) m/s²")
                        appendLine("Mag: (${String.format(Locale.US, "%.1f", uiState.magX)}, ${String.format(Locale.US, "%.1f", uiState.magY)}, ${String.format(Locale.US, "%.1f", uiState.magZ)}) μT")
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00FF00).copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun RecordingCard(duration: Long, sampleCount: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer { // Use graphicsLayer for smoother performance
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .clip(CircleShape)
                        .background(Color.Red)                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Recording...", fontWeight = FontWeight.Bold)
                    Text(formatDuration(duration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("${sampleCount} samples", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxWidth(),
            color = Color.Red,
            trackColor = Color.Red.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun HistoryChartCard(dataPoints: List<SensorDataPoint>, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Orientation History (Roll)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClear) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SmoothLineChart(dataPoints)
        }
    }
}

@Composable
fun StatisticsCard(statistics: SensorStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📊 Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Min Roll", String.format(Locale.US, "%.1f°", Math.toDegrees(statistics.minRoll.toDouble())))
                StatItem("Max Roll", String.format(Locale.US, "%.1f°", Math.toDegrees(statistics.maxRoll.toDouble())))
                StatItem("Avg Roll", String.format(Locale.US, "%.1f°", Math.toDegrees(statistics.avgRoll.toDouble())))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Stability", String.format(Locale.US, "%.0f%%", statistics.stabilityScore * 100))
                StatItem("Movement", String.format(Locale.US, "%.1f", statistics.movementIntensity))
                StatItem("Samples", statistics.sampleCount.toString())
            }
        }
    }
}

@Composable
fun StatisticsDialog(statistics: SensorStatistics, onDismiss: () -> Unit, onReset: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sensor Statistics") },
        text = {
            Column {
                StatisticsRow("Min Roll", String.format(Locale.US, "%.2f rad (%.1f°)", statistics.minRoll, Math.toDegrees(statistics.minRoll.toDouble())))
                StatisticsRow("Max Roll", String.format(Locale.US, "%.2f rad (%.1f°)", statistics.maxRoll, Math.toDegrees(statistics.maxRoll.toDouble())))
                StatisticsRow("Avg Roll", String.format(Locale.US, "%.2f rad (%.1f°)", statistics.avgRoll, Math.toDegrees(statistics.avgRoll.toDouble())))
                HorizontalDivider()
                StatisticsRow("Min Pitch", String.format(Locale.US, "%.2f rad (%.1f°)", statistics.minPitch, Math.toDegrees(statistics.minPitch.toDouble())))
                StatisticsRow("Max Pitch", String.format(Locale.US, "%.2f rad (%.1f°)", statistics.maxPitch, Math.toDegrees(statistics.maxPitch.toDouble())))
                StatisticsRow("Avg Pitch", String.format(Locale.US, "%.2f rad (%.1f°)", statistics.avgPitch, Math.toDegrees(statistics.avgPitch.toDouble())))
                HorizontalDivider()
                StatisticsRow("Stability Score", String.format(Locale.US, "%.1f%%", statistics.stabilityScore * 100))
                StatisticsRow("Movement Intensity", String.format(Locale.US, "%.2f", statistics.movementIntensity))
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Reset Statistics")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ExportDialog(onDismiss: () -> Unit, onExport: (String) -> Unit, sampleCount: Int) {
    var selectedFormat by remember { mutableStateOf("CSV") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column {
                Text("Export ${sampleCount} sensor data points to file")
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == "CSV",
                        onClick = { selectedFormat = "CSV" },
                        label = { Text("CSV") }
                    )
                    FilterChip(
                        selected = selectedFormat == "JSON",
                        onClick = { selectedFormat = "JSON" },
                        label = { Text("JSON") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onExport(selectedFormat) }) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.shadow(20.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(brush = Brush.verticalGradient(gradientColors), shape = RoundedCornerShape(24.dp))
        ) {
            content()
        }
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
            color = color.copy(alpha = 0.15f),
            shadowElevation = 2.dp
        ) {
            Text(
                text = String.format(Locale.US, "%.1f°", Math.toDegrees(animatedValue.toDouble())),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
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
            Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(
                text = String.format(Locale.US, "%.2f %s", displayValue, unit),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
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
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun SmoothLineChart(dataPoints: List<SensorDataPoint>) {
    if (dataPoints.isEmpty()) return

    val animatedAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(500))
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
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

        // Area fill
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
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }

        // Points
        points.forEach { point ->
            drawCircle(color = Color(0xFFFF5722), radius = 3f, center = point)
        }

        // Grid
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun StatisticsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

// ==================== HELPER FUNCTIONS ====================

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return if (hours > 0) String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun getColorForValue(value: Float, min: Float, max: Float): Color {
    val t = ((value - min) / (max - min)).coerceIn(0f, 1f)
    return when {
        t < 0.33f -> Color(0xFF4CAF50)
        t < 0.66f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

private fun List<Float>.std(): Float {
    if (isEmpty()) return 0f
    val mean = this.average().toFloat()
    val variance = this.map { (it - mean).pow(2f) }.average().toFloat()
    return sqrt(variance)
}