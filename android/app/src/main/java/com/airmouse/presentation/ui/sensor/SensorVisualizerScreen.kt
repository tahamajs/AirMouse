package com.airmouse.presentation.ui.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airmouse.presentation.ui.components.LineChart
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.GlassmorphicCard
import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.*
import kotlin.math.sqrt
import kotlin.math.pow

// ============================================================
// Data Models
// ============================================================

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

// ============================================================
// Main Screen
// ============================================================

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
    var gyroHistory by remember { mutableStateOf(listOf<Float>()) }
    var accelHistory by remember { mutableStateOf(listOf<Float>()) }
    var magHistory by remember { mutableStateOf(listOf<Float>()) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    // Helper to update statistics
    fun updateStatistics() {
        val points = history.dataPoints.takeLast(100)
        if (points.isNotEmpty()) {
            val rolls = points.map { it.roll }
            val pitches = points.map { it.pitch }
            val yaws = points.map { it.yaw }

            val stability = 1f - (rolls.std() + pitches.std() + yaws.std()) / 3f
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
            )
        }
    }

    // Sensor listener
    DisposableEffect(Unit) {
        var hasRotationVector = false
        val lastAccel = FloatArray(3)
        val lastMag = FloatArray(3)
        var hasAccel = false
        var hasMag = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        hasRotationVector = true
                        if (event.values.size >= 4) {
                            val rotMat = FloatArray(9)
                            SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(rotMat, orientation)
                            uiState = uiState.copy(
                                roll = orientation[2],
                                pitch = orientation[1],
                                yaw = orientation[0],
                                gameRotationX = event.values[0],
                                gameRotationY = event.values[1],
                                gameRotationZ = event.values[2]
                            )
                            if (history.isRecording) {
                                history = history.copy(
                                    dataPoints = (history.dataPoints + SensorDataPoint(
                                        timestamp = System.currentTimeMillis(),
                                        roll = orientation[2],
                                        pitch = orientation[1],
                                        yaw = orientation[0]
                                    )).takeLast(history.maxHistorySize)
                                )
                            }
                        }
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        hasRotationVector = true
                        if (event.values.size >= 3) {
                            val rotMat = FloatArray(9)
                            SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(rotMat, orientation)
                            uiState = uiState.copy(
                                roll = orientation[2],
                                pitch = orientation[1],
                                yaw = orientation[0],
                                rotationX = event.values[0],
                                rotationY = event.values[1],
                                rotationZ = event.values[2]
                            )
                            if (history.isRecording) {
                                history = history.copy(
                                    dataPoints = (history.dataPoints + SensorDataPoint(
                                        timestamp = System.currentTimeMillis(),
                                        roll = orientation[2],
                                        pitch = orientation[1],
                                        yaw = orientation[0]
                                    )).takeLast(history.maxHistorySize)
                                )
                            }
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        uiState = uiState.copy(
                            gyroX = event.values[0],
                            gyroY = event.values[1],
                            gyroZ = event.values[2]
                        )
                        gyroHistory = (gyroHistory + sqrt(
                            event.values[0] * event.values[0] +
                                    event.values[1] * event.values[1] +
                                    event.values[2] * event.values[2]
                        )).takeLast(40)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        uiState = uiState.copy(
                            accelX = event.values[0],
                            accelY = event.values[1],
                            accelZ = event.values[2]
                        )
                        accelHistory = (accelHistory + sqrt(
                            event.values[0] * event.values[0] +
                                    event.values[1] * event.values[1] +
                                    event.values[2] * event.values[2]
                        )).takeLast(40)

                        System.arraycopy(event.values, 0, lastAccel, 0, 3)
                        hasAccel = true

                        if (!hasRotationVector && hasAccel && hasMag) {
                            val r = FloatArray(9)
                            val i = FloatArray(9)
                            if (SensorManager.getRotationMatrix(r, i, lastAccel, lastMag)) {
                                val orientation = FloatArray(3)
                                SensorManager.getOrientation(r, orientation)
                                uiState = uiState.copy(
                                    roll = orientation[2],
                                    pitch = orientation[1],
                                    yaw = orientation[0]
                                )
                                if (history.isRecording) {
                                    history = history.copy(
                                        dataPoints = (history.dataPoints + SensorDataPoint(
                                            timestamp = System.currentTimeMillis(),
                                            roll = orientation[2],
                                            pitch = orientation[1],
                                            yaw = orientation[0]
                                        )).takeLast(history.maxHistorySize)
                                    )
                                }
                            }
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        uiState = uiState.copy(
                            magX = event.values[0],
                            magY = event.values[1],
                            magZ = event.values[2]
                        )
                        magHistory = (magHistory + sqrt(
                            event.values[0] * event.values[0] +
                                    event.values[1] * event.values[1] +
                                    event.values[2] * event.values[2]
                        )).takeLast(40)

                        System.arraycopy(event.values, 0, lastMag, 0, 3)
                        hasMag = true

                        if (!hasRotationVector && hasAccel && hasMag) {
                            val r = FloatArray(9)
                            val i = FloatArray(9)
                            if (SensorManager.getRotationMatrix(r, i, lastAccel, lastMag)) {
                                val orientation = FloatArray(3)
                                SensorManager.getOrientation(r, orientation)
                                uiState = uiState.copy(
                                    roll = orientation[2],
                                    pitch = orientation[1],
                                    yaw = orientation[0]
                                )
                                if (history.isRecording) {
                                    history = history.copy(
                                        dataPoints = (history.dataPoints + SensorDataPoint(
                                            timestamp = System.currentTimeMillis(),
                                            roll = orientation[2],
                                            pitch = orientation[1],
                                            yaw = orientation[0]
                                        )).takeLast(history.maxHistorySize)
                                    )
                                }
                            }
                        }
                    }
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

        // Register sensors
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

    // UI Scaffold
    PremiumDashboardBackground {
        Scaffold(
            containerColor = Color.Transparent,
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
                        history = history.copy(
                            isRecording = !history.isRecording,
                            startTime = System.currentTimeMillis()
                        )
                    }) {
                        Icon(
                            if (history.isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                            contentDescription = if (history.isRecording) "Stop Recording" else "Start Recording",
                            tint = if (history.isRecording) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(Icons.Default.Assessment, contentDescription = "Statistics")
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
            // Signal Quality Card
            item {
                SignalQualityCard(quality = uiState.signalQuality, calibration = uiState.calibrationStatus)
            }

            // 3D Cube
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
                                isInteractive = true,
                                colorPalette = SensorCubePalette.ELECTRIC
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

            // Sensor Type Selector
            item {
                SensorTypeSelector(
                    selectedSensor = uiState.activeSensor,
                    onSensorSelected = { uiState = uiState.copy(activeSensor = it) }
                )
            }

            // Sensor Charts
            item {
                SensorChartsCard(
                    gyroHistory = gyroHistory,
                    accelHistory = accelHistory,
                    magHistory = magHistory
                )
            }

            // Live Data Card
            item {
                SensorDataCard(
                    uiState = uiState,
                    activeSensor = uiState.activeSensor
                )
            }

            // Raw Data
            if (uiState.showRawData) {
                item {
                    RawDataCard(uiState = uiState)
                }
            }

            // Recording status
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

            // Statistics
            if (statistics.sampleCount > 0) {
                item {
                    StatisticsCard(statistics = statistics)
                }
            }
        }
    }
    }

    // Dialogs
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { _ ->
                showExportDialog = false
            },
            sampleCount = history.dataPoints.size
        )
    }

    if (showStatsDialog) {
        StatisticsDialog(
            statistics = statistics,
            onDismiss = { showStatsDialog = false },
            onReset = { statistics = SensorStatistics() }
        )
    }
}

// ============================================================
// UI Components (fully defined)
// ============================================================

@Composable
fun SignalQualityCard(quality: SignalQuality, calibration: CalibrationStatus) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
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
    val neonCyan = Color(0xFF00E5FF)
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📡 Sensor Stream",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActiveSensor.entries.forEach { sensor ->
                    val isSelected = selectedSensor == sensor
                    val tabAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.45f,
                        animationSpec = tween(250)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) neonCyan.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable {
                                    onSensorSelected(sensor)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sensor.icon + "\n" + sensor.displayName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) neonCyan else Color.White.copy(alpha = tabAlpha),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                        // Neon underline indicator
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(200)) + expandHorizontally(),
                            exit = fadeOut(tween(200)) + shrinkHorizontally()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, neonCyan, Color.Transparent)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorDataCard(uiState: SensorVisualizerUiState, activeSensor: ActiveSensor) {
    val infiniteLive = rememberInfiniteTransition(label = "live_pulse")
    val livePulseAlpha by infiniteLive.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "live_alpha"
    )
    val livePulseScale by infiniteLive.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "live_scale"
    )
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Live Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                // Pulsing LIVE badge
                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = livePulseScale; scaleY = livePulseScale }
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF1744).copy(alpha = livePulseAlpha * 0.9f),
                                    Color(0xFFFF6D00).copy(alpha = livePulseAlpha * 0.7f))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = livePulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (activeSensor) {
                ActiveSensor.ORIENTATION -> {
                    AnimatedSensorBar("Roll", uiState.roll, (-PI).toFloat(), PI.toFloat(), "rad")
                    AnimatedSensorBar("Pitch", uiState.pitch, (-PI / 2).toFloat(), (PI / 2).toFloat(), "rad")
                    AnimatedSensorBar("Yaw", uiState.yaw, (-PI).toFloat(), PI.toFloat(), "rad")
                }
                ActiveSensor.GYROSCOPE -> {
                    // 3D Compass visualization
                    PremiumCompass3D(
                        gyroX = uiState.gyroX,
                        gyroY = uiState.gyroY,
                        gyroZ = uiState.gyroZ,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(vertical = 8.dp)
                    )
                    AnimatedSensorBar("X", uiState.gyroX, -10f, 10f, "rad/s")
                    AnimatedSensorBar("Y", uiState.gyroY, -10f, 10f, "rad/s")
                    AnimatedSensorBar("Z", uiState.gyroZ, -10f, 10f, "rad/s")
                }
                ActiveSensor.ACCELEROMETER -> {
                    AnimatedSensorBar("X", uiState.accelX, -20f, 20f, "m/s²")
                    AnimatedSensorBar("Y", uiState.accelY, -20f, 20f, "m/s²")
                    AnimatedSensorBar("Z", uiState.accelZ, -20f, 20f, "m/s²")
                    val magnitude = sqrt(uiState.accelX * uiState.accelX +
                            uiState.accelY * uiState.accelY +
                            uiState.accelZ * uiState.accelZ)
                    AnimatedSensorBar("Magnitude", magnitude, 0f, 30f, "m/s²")
                }
                ActiveSensor.MAGNETOMETER -> {
                    AnimatedSensorBar("X", uiState.magX, -100f, 100f, "μT")
                    AnimatedSensorBar("Y", uiState.magY, -100f, 100f, "μT")
                    AnimatedSensorBar("Z", uiState.magZ, -100f, 100f, "μT")
                }
                ActiveSensor.ENVIRONMENTAL -> {
                    if (uiState.temperature != 0f) {
                        SensorInfoRow(Icons.Default.Thermostat, "Temperature",
                            String.format(Locale.US, "%.1f°C", uiState.temperature))
                    }
                    if (uiState.pressure != 0f) {
                        SensorInfoRow(Icons.Default.Assessment, "Pressure",
                            String.format(Locale.US, "%.1f hPa", uiState.pressure))
                    }
                    if (uiState.light != 0f) {
                        SensorInfoRow(Icons.Default.Lightbulb, "Light",
                            String.format(Locale.US, "%.0f lx", uiState.light))
                    }
                    if (uiState.humidity != 0f) {
                        SensorInfoRow(Icons.Default.WaterDrop, "Humidity",
                            String.format(Locale.US, "%.0f%%", uiState.humidity))
                    }
                    if (uiState.proximity != 0f) {
                        SensorInfoRow(Icons.Default.Radar, "Proximity",
                            String.format(Locale.US, "%.0f cm", uiState.proximity))
                    }
                    if (uiState.steps > 0) {
                        SensorInfoRow(Icons.AutoMirrored.Filled.DirectionsWalk, "Steps",
                            uiState.steps.toString())
                    }
                    if (uiState.heartRate > 0) {
                        SensorInfoRow(Icons.Default.Favorite, "Heart Rate",
                            String.format(Locale.US, "%.0f BPM", uiState.heartRate))
                    }
                }
            }
        }
    }
}

@Composable
fun SensorChartsCard(
    gyroHistory: List<Float>,
    accelHistory: List<Float>,
    magHistory: List<Float>
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "📈 Separate Sensor Charts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Each graph tracks a single sensor family — see drift and movement at a glance.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )

            NeonSensorChartBlock(
                title = "Gyroscope",
                subtitle = "Angular speed magnitude",
                data = gyroHistory,
                neonColor = Color(0xFF00E5FF)
            )
            NeonSensorChartBlock(
                title = "Accelerometer",
                subtitle = "Acceleration magnitude",
                data = accelHistory,
                neonColor = Color(0xFF69FF47)
            )
            NeonSensorChartBlock(
                title = "Magnetometer",
                subtitle = "Magnetic field magnitude",
                data = magHistory,
                neonColor = Color(0xFFFFD600)
            )
        }
    }
}

// Premium neon chart block replacing the old SensorChartBlock
@Composable
private fun NeonSensorChartBlock(
    title: String,
    subtitle: String,
    data: List<Float>,
    neonColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(neonColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${data.size} pts",
                    fontSize = 11.sp,
                    color = neonColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (data.isNotEmpty()) {
            NeonLineChart(
                data = data,
                neonColor = neonColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D1117)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Waiting for sensor samples…",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Premium neon Canvas chart with dark grid
@Composable
private fun NeonLineChart(
    data: List<Float>,
    neonColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "neon_chart_anim"
    )
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D1117))
    ) {
        val w = size.width
        val h = size.height
        val maxVal = data.maxOrNull() ?: 1f
        val minVal = data.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(0.01f)
        val stepX = if (data.size > 1) w / (data.size - 1) else w

        // Dark grid lines
        val gridCols = 6
        val gridRows = 4
        for (col in 0..gridCols) {
            val x = w * col / gridCols
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(x, 0f), end = Offset(x, h),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }
        for (row in 0..gridRows) {
            val y = h * row / gridRows
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(0f, y), end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        val visibleCount = (data.size * animProgress).toInt().coerceAtLeast(2)
        val visibleData = data.takeLast(visibleCount)
        val visStepX = if (visibleData.size > 1) w / (visibleData.size - 1) else w

        val points = visibleData.mapIndexed { i, v ->
            val x = i * visStepX
            val y = h - ((v - minVal) / range) * (h * 0.85f) - h * 0.075f
            Offset(x, y)
        }

        // Gradient fill under line
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    listOf(neonColor.copy(alpha = 0.35f), neonColor.copy(alpha = 0f))
                )
            )
        }

        // Glow layer (wider stroke, low alpha)
        for (i in 0 until points.size - 1) {
            drawLine(
                color = neonColor.copy(alpha = 0.25f),
                start = points[i], end = points[i + 1],
                strokeWidth = 8f, cap = StrokeCap.Round
            )
        }
        // Main neon line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = neonColor,
                start = points[i], end = points[i + 1],
                strokeWidth = 2.5f, cap = StrokeCap.Round
            )
        }
        // Latest value dot
        if (points.isNotEmpty()) {
            drawCircle(color = Color.White, radius = 5f, center = points.last())
            drawCircle(color = neonColor, radius = 3.5f, center = points.last())
        }
    }
}

@Composable
fun RawDataCard(uiState: SensorVisualizerUiState) {
    var fRoll by remember { mutableFloatStateOf(0f) }
    var fPitch by remember { mutableFloatStateOf(0f) }
    var fYaw by remember { mutableFloatStateOf(0f) }
    var fGyroX by remember { mutableFloatStateOf(0f) }
    var fGyroY by remember { mutableFloatStateOf(0f) }
    var fGyroZ by remember { mutableFloatStateOf(0f) }
    var fAccelX by remember { mutableFloatStateOf(0f) }
    var fAccelY by remember { mutableFloatStateOf(0f) }
    var fAccelZ by remember { mutableFloatStateOf(0f) }

    val alpha = 0.15f // EMA factor

    LaunchedEffect(uiState.roll, uiState.pitch, uiState.yaw, uiState.gyroX, uiState.gyroY, uiState.accelX) {
        fRoll = fRoll * (1 - alpha) + uiState.roll * alpha
        fPitch = fPitch * (1 - alpha) + uiState.pitch * alpha
        fYaw = fYaw * (1 - alpha) + uiState.yaw * alpha
        fGyroX = fGyroX * (1 - alpha) + uiState.gyroX * alpha
        fGyroY = fGyroY * (1 - alpha) + uiState.gyroY * alpha
        fGyroZ = fGyroZ * (1 - alpha) + uiState.gyroZ * alpha
        fAccelX = fAccelX * (1 - alpha) + uiState.accelX * alpha
        fAccelY = fAccelY * (1 - alpha) + uiState.accelY * alpha
        fAccelZ = fAccelZ * (1 - alpha) + uiState.accelZ * alpha
    }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📟 Raw & Filtered Sensor Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // RAW COLUMN
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "RAW\n" + buildString {
                            appendLine("Roll: ${String.format(Locale.US, "%+6.3f", uiState.roll)}")
                            appendLine("Pitch: ${String.format(Locale.US, "%+6.3f", uiState.pitch)}")
                            appendLine("Yaw: ${String.format(Locale.US, "%+6.3f", uiState.yaw)}")
                            appendLine("GyroX: ${String.format(Locale.US, "%+6.2f", uiState.gyroX)}")
                            appendLine("GyroY: ${String.format(Locale.US, "%+6.2f", uiState.gyroY)}")
                            appendLine("AccX: ${String.format(Locale.US, "%+6.2f", uiState.accelX)}")
                            appendLine("AccY: ${String.format(Locale.US, "%+6.2f", uiState.accelY)}")
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00FF00).copy(alpha = 0.8f)
                    )
                }

                // FILTERED COLUMN
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "FILTERED (EMA)\n" + buildString {
                            appendLine("Roll: ${String.format(Locale.US, "%+6.3f", fRoll)}")
                            appendLine("Pitch: ${String.format(Locale.US, "%+6.3f", fPitch)}")
                            appendLine("Yaw: ${String.format(Locale.US, "%+6.3f", fYaw)}")
                            appendLine("GyroX: ${String.format(Locale.US, "%+6.2f", fGyroX)}")
                            appendLine("GyroY: ${String.format(Locale.US, "%+6.2f", fGyroY)}")
                            appendLine("AccX: ${String.format(Locale.US, "%+6.2f", fAccelX)}")
                            appendLine("AccY: ${String.format(Locale.US, "%+6.2f", fAccelY)}")
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00BCD4).copy(alpha = 0.8f)
                    )
                }
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

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
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
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .clip(CircleShape)
                        .background(Color.Red)
                )
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
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
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
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
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
                StatisticsRow("Min Roll", String.format(Locale.US, "%.2f rad (%.1f°)",
                    statistics.minRoll, Math.toDegrees(statistics.minRoll.toDouble())))
                StatisticsRow("Max Roll", String.format(Locale.US, "%.2f rad (%.1f°)",
                    statistics.maxRoll, Math.toDegrees(statistics.maxRoll.toDouble())))
                StatisticsRow("Avg Roll", String.format(Locale.US, "%.2f rad (%.1f°)",
                    statistics.avgRoll, Math.toDegrees(statistics.avgRoll.toDouble())))
                HorizontalDivider()
                StatisticsRow("Min Pitch", String.format(Locale.US, "%.2f rad (%.1f°)",
                    statistics.minPitch, Math.toDegrees(statistics.minPitch.toDouble())))
                StatisticsRow("Max Pitch", String.format(Locale.US, "%.2f rad (%.1f°)",
                    statistics.maxPitch, Math.toDegrees(statistics.maxPitch.toDouble())))
                StatisticsRow("Avg Pitch", String.format(Locale.US, "%.2f rad (%.1f°)",
                    statistics.avgPitch, Math.toDegrees(statistics.avgPitch.toDouble())))
                HorizontalDivider()
                StatisticsRow("Stability Score", String.format(Locale.US, "%.1f%%",
                    statistics.stabilityScore * 100))
                StatisticsRow("Movement Intensity", String.format(Locale.US, "%.2f",
                    statistics.movementIntensity))
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
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
    Box(
        modifier = modifier
            .shadow(20.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(brush = Brush.verticalGradient(gradientColors))
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
    val neonRed   = Color(0xFFFF1744)
    val neonGreen = Color(0xFF69FF47)
    val neonBlue  = Color(0xFF00E5FF)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D1117))
    ) {
        val width  = size.width
        val height = size.height
        val stepX  = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

        // Dark grid
        for (col in 0..6) {
            val x = width * col / 6
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }
        for (row in 0..4) {
            val y = height * row / 4
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        // Helper to draw one neon axis series
        fun drawNeonSeries(values: List<Float>, color: Color) {
            val minV = values.minOrNull() ?: -1f
            val maxV = values.maxOrNull() ?: 1f
            val range = (maxV - minV).coerceAtLeast(0.01f)
            val pts = values.mapIndexed { i, v ->
                val x = i * stepX
                val y = height - ((v - minV) / range) * (height * 0.85f) - height * 0.075f
                Offset(x, y)
            }
            // Fill
            if (pts.size >= 2) {
                val fp = Path().apply {
                    moveTo(pts.first().x, height)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, height)
                    close()
                }
                drawPath(fp, brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.3f * animatedAlpha), color.copy(alpha = 0f))
                ))
            }
            // Glow
            for (i in 0 until pts.size - 1) {
                drawLine(color.copy(alpha = 0.2f * animatedAlpha), pts[i], pts[i+1], 7f, StrokeCap.Round)
            }
            // Neon line
            for (i in 0 until pts.size - 1) {
                drawLine(color.copy(alpha = animatedAlpha), pts[i], pts[i+1], 2.5f, StrokeCap.Round)
            }
            // Latest dot
            if (pts.isNotEmpty()) {
                drawCircle(Color.White, 5f, pts.last())
                drawCircle(color, 3f, pts.last())
            }
        }

        drawNeonSeries(dataPoints.map { it.roll  }, neonRed)
        drawNeonSeries(dataPoints.map { it.pitch }, neonGreen)
        drawNeonSeries(dataPoints.map { it.yaw   }, neonBlue)
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = Color.White)
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

// ============================================================
// 3D Compass for Gyroscope
// ============================================================

@Composable
fun PremiumCompass3D(
    gyroX: Float,
    gyroY: Float,
    gyroZ: Float,
    modifier: Modifier = Modifier
) {
    val infiniteT = rememberInfiniteTransition(label = "compass")
    val rotateDial by infiniteT.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "dial_rotate"
    )
    val animGX by animateFloatAsState(gyroX, tween(200), label = "gx")
    val animGY by animateFloatAsState(gyroY, tween(200), label = "gy")
    val animGZ by animateFloatAsState(gyroZ, tween(200), label = "gz")

    val magnitude = sqrt(animGX * animGX + animGY * animGY + animGZ * animGZ)
    val neonCyan   = Color(0xFF00E5FF)
    val neonPink   = Color(0xFFFF1744)
    val neonYellow = Color(0xFFFFD600)
    val neonGreen  = Color(0xFF69FF47)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(cx, cy) * 0.82f

            // Outer glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(neonCyan.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(cx, cy), radius = radius * 1.15f
                ),
                radius = radius * 1.15f, center = Offset(cx, cy)
            )
            // Compass circle
            drawCircle(color = Color(0xFF1A2030), radius = radius, center = Offset(cx, cy))
            drawCircle(color = neonCyan.copy(alpha = 0.35f), radius = radius, center = Offset(cx, cy), style = Stroke(2f))

            // Tick marks
            for (i in 0 until 36) {
                val angle = Math.toRadians((i * 10 + rotateDial * 0.05f).toDouble())
                val isCardinal = i % 9 == 0
                val innerR = if (isCardinal) radius * 0.78f else radius * 0.88f
                val outerR = radius * 0.97f
                val cos = cos(angle).toFloat()
                val sin = sin(angle).toFloat()
                drawLine(
                    color = if (isCardinal) neonCyan else neonCyan.copy(alpha = 0.3f),
                    start = Offset(cx + cos * innerR, cy + sin * innerR),
                    end   = Offset(cx + cos * outerR, cy + sin * outerR),
                    strokeWidth = if (isCardinal) 2.5f else 1f
                )
            }

            // X axis (red)
            val xAngle = Math.toRadians(animGX * 30.0)
            drawLine(
                color = neonPink, strokeWidth = 3f, cap = StrokeCap.Round,
                start = Offset(cx, cy),
                end   = Offset(cx + cos(xAngle).toFloat() * radius * 0.6f, cy + sin(xAngle).toFloat() * radius * 0.6f)
            )
            // Y axis (green)
            val yAngle = Math.toRadians((animGY * 30.0 + 90.0))
            drawLine(
                color = neonGreen, strokeWidth = 3f, cap = StrokeCap.Round,
                start = Offset(cx, cy),
                end   = Offset(cx + cos(yAngle).toFloat() * radius * 0.6f, cy + sin(yAngle).toFloat() * radius * 0.6f)
            )
            // Z needle (yellow)
            val zAngle = Math.toRadians(animGZ * 30.0 - 90.0)
            val needleLen = radius * 0.72f
            val needleTip = Offset(cx + cos(zAngle).toFloat() * needleLen, cy + sin(zAngle).toFloat() * needleLen)
            val needleTail = Offset(cx - cos(zAngle).toFloat() * needleLen * 0.35f, cy - sin(zAngle).toFloat() * needleLen * 0.35f)
            // Glow
            drawLine(neonYellow.copy(alpha = 0.3f), needleTail, needleTip, 10f, StrokeCap.Round)
            drawLine(neonYellow, needleTail, needleTip, 3f, StrokeCap.Round)

            // Center hub
            drawCircle(brush = Brush.radialGradient(
                listOf(Color.White, neonCyan), center = Offset(cx, cy), radius = 8f
            ), radius = 8f, center = Offset(cx, cy))
        }

        // Magnitude text overlay
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                String.format(java.util.Locale.US, "%.2f rad/s", magnitude),
                fontSize = 11.sp, color = neonCyan,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("X", fontSize = 10.sp, color = neonPink, fontWeight = FontWeight.Bold)
                Text("Y", fontSize = 10.sp, color = neonGreen, fontWeight = FontWeight.Bold)
                Text("Z", fontSize = 10.sp, color = neonYellow, fontWeight = FontWeight.Bold)
            }
        }
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

// ============================================================
// Utility Functions
// ============================================================

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
    val variance = this.map { (it - mean).toDouble().pow(2.0) }.average().toFloat()
    return sqrt(variance)
}
