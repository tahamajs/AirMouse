package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID

// --- Local Target Mock Data Architectures ---

data class CalibrationData(
    val gyroOffsetX: Float = 0f,
    val gyroOffsetY: Float = 0f,
    val gyroOffsetZ: Float = 0f,
    val accelOffsetX: Float = 0f,
    val accelOffsetY: Float = 0f,
    val accelOffsetZ: Float = 0f,
    val magOffsetX: Float = 0f,
    val magOffsetY: Float = 0f,
    val magOffsetZ: Float = 0f,
    val accelScaleX: Float = 1f,
    val accelScaleY: Float = 1f,
    val accelScaleZ: Float = 1f
)

data class CalibrationUiState(
    val stepTitle: String = "Gyroscope Calibration",
    val stepInstruction: String = "Place Device Flat",
    val stepDescription: String = "Keep your phone perfectly static on a level surface.",
    val currentStep: Int = 0,
    val currentPosition: Int = 0,
    val totalPositions: Int = 6,
    val progress: Int = 0,
    val samplesCollected: Int = 0,
    val totalSamplesNeeded: Int = 100,
    val isCollecting: Boolean = false,
    val isComplete: Boolean = false,
    val isSkipped: Boolean = false,
    val statusMessage: String = "Ready to start",
    val errorMessage: String? = null,
    val calibrationQuality: String = "Excellent",
    val gyroData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val calibrationData: CalibrationData = CalibrationData()
)

val positionsList = listOf(
    "Flat on Table (Face Up)",
    "Flat on Table (Face Down)",
    "Landscape Left Edge",
    "Landscape Right Edge",
    "Portrait Upright",
    "Portrait Upside Down"
)

enum class VisualizerSize { SMALL, MEDIUM, LARGE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(500)
        if (!uiState.isCollecting && !uiState.isComplete && !uiState.isSkipped) {
            viewModel.startCalibration()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.stepTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isComplete && !uiState.isCollecting) {
                        textToSpeechFallback(onClick = { viewModel.skipCalibration() }) {
                            Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { viewModel.openHelp() }) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            if (!uiState.isComplete && !uiState.isSkipped) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            enabled = uiState.currentStep > 0 && !uiState.isCollecting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Step", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                if (uiState.isCollecting) {
                                    if (uiState.currentStep == 1 && uiState.currentPosition < uiState.totalPositions - 1) {
                                        viewModel.nextAccelPosition()
                                    } else {
                                        viewModel.stopCollection()
                                    }
                                } else {
                                    viewModel.startCalibration()
                                }
                            },
                            enabled = !uiState.isComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isCollecting)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            when {
                                uiState.isCollecting && uiState.currentStep == 1 -> {
                                    if (uiState.currentPosition < uiState.totalPositions - 1) {
                                        Text("Next Position")
                                    } else {
                                        Text("Complete")
                                    }
                                }
                                uiState.isCollecting -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Collecting...")
                                }
                                else -> Text("Start Calibration")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.abortCalibration() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Stop Calibration", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedInstructionIcon(uiState)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(text = uiState.stepInstruction, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = uiState.stepDescription, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }

                item {
                    ProgressCard(
                        progress = uiState.progress,
                        samplesCollected = uiState.samplesCollected,
                        totalSamples = uiState.totalSamplesNeeded,
                        currentStep = uiState.currentStep,
                        currentPosition = uiState.currentPosition,
                        totalPositions = uiState.totalPositions
                    )
                }

                if (uiState.isCollecting) {
                    item {
                        SensorDataCard(gyroData = uiState.gyroData, accelData = uiState.accelData, magData = uiState.magData)
                    }
                }

                if (uiState.currentStep == 1 && !uiState.isComplete) {
                    item {
                        PositionGuideCard(
                            currentPosition = uiState.currentPosition,
                            positions = positionsList,
                            onPositionClick = { viewModel.jumpToPosition(it) }
                        )
                    }
                }

                item {
                    StatusCard(message = uiState.statusMessage, errorMessage = uiState.errorMessage, isComplete = uiState.isComplete)
                }

                if (uiState.isComplete) {
                    item {
                        CalibrationResultsCard(calibrationData = uiState.calibrationData, quality = uiState.calibrationQuality)
                    }

                    item {
                        SensorVisualizer(roll = uiState.gyroData.first, pitch = uiState.gyroData.second, yaw = uiState.gyroData.third, size = VisualizerSize.LARGE)
                    }
                    item {
                        GyroscopeVisualizer(x = uiState.gyroData.first, y = uiState.gyroData.second, z = uiState.gyroData.third)
                    }
                }

                if (uiState.isComplete || uiState.isSkipped) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onComplete,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Accept Results")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Using Air Mouse")
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetCalibration() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset Setup")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recalibrate")
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// --- Composite Display Component Overrides & Logic Layout blocks ---

@Composable
fun textToSpeechFallback(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TextButton(onClick = onClick, modifier = modifier) { content() }
}

@Composable
fun AnimatedInstructionIcon(uiState: CalibrationUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "inf_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "bounce"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    val color = if (uiState.isCollecting || uiState.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier.size(120.dp).scale(pulse),
        contentAlignment = Alignment.Center
    ) {
        when (uiState.currentStep) {
            0 -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.width / 2 - 10f
                    drawCircle(color = color.copy(alpha = 0.2f), radius = radius, center = Offset(centerX, centerY))
                    drawArc(
                        color = color, startAngle = rotation, sweepAngle = 90f, useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius), size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
                Text("🔄", fontSize = 40.sp, modifier = Modifier.rotate(rotation))
            }
            1 -> {
                Box(modifier = Modifier.size(100.dp).offset(y = bounce.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = "Device Orientation indicator", modifier = Modifier.fillMaxSize(), tint = color)
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(color).offset(y = (bounce / 2).dp))
                }
            }
            else -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.width / 2 - 10f
                    for (i in 0..3) {
                        val angle = i * 90f + rotation
                        val rad = Math.toRadians(angle.toDouble()).toFloat()
                        val endX = centerX + radius * 0.8f * kotlin.math.cos(rad)
                        val endY = centerY + radius * 0.8f * kotlin.math.sin(rad)
                        drawLine(color = color, start = Offset(centerX, centerY), end = Offset(endX, endY), strokeWidth = if (i == 0) 4f else 2f, cap = StrokeCap.Round)
                    }
                }
                Text("🧭", fontSize = 48.sp, modifier = Modifier.offset(y = (bounce / 2).dp))
            }
        }
    }
}

@Composable
fun ProgressCard(progress: Int, samplesCollected: Int, totalSamples: Int, currentStep: Int, currentPosition: Int, totalPositions: Int) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(text = "Calibration Progress", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedProgressBar(progress = progress / 100f)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$progress%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (samplesCollected > 0) {
                    Text("$samplesCollected / $totalSamples samples", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (currentStep == 1 && currentPosition < totalPositions) {
                Spacer(modifier = Modifier.height(8.dp))
                val subProgress = (currentPosition + 1).toFloat() / totalPositions
                LinearProgressIndicator(progress = { subProgress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = Color(0xFF4CAF50))
                Text("Position ${currentPosition + 1} of $totalPositions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AnimatedProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500, easing = FastOutSlowInEasing), label = "progress")
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(colors = listOf(Color(0xFF00BCD4), Color(0xFF4CAF50)))))
    }
}

@Composable
fun SensorDataCard(gyroData: Triple<Float, Float, Float>, accelData: Triple<Float, Float, Float>, magData: Triple<Float, Float, Float>) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Live Sensor Data", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            SensorRow("Gyroscope", gyroData.first, gyroData.second, gyroData.third, Color(0xFFFF9800))
            SensorRow("Accelerometer", accelData.first, accelData.second, accelData.third, Color(0xFF00BCD4))
            SensorRow("Magnetometer", magData.first, magData.second, magData.third, Color(0xFF4CAF50))
        }
    }
}

@Composable
fun SensorRow(title: String, x: Float, y: Float, z: Float, color: Color) {
    Column {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SensorValue("X", x, color)
            SensorValue("Y", y, color)
            SensorValue("Z", z, color)
        }
    }
}

@Composable
fun SensorValue(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(String.format(Locale.getDefault(), "%.2f", value), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = color)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PositionGuideCard(currentPosition: Int, positions: List<String>, onPositionClick: (Int) -> Unit) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Position Guide", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            positions.forEachIndexed { index, position ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onPositionClick(index) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(if (index < currentPosition) Color(0xFF4CAF50) else if (index == currentPosition) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < currentPosition) Text("✓", fontSize = 12.sp, color = Color.White) else Text("${index + 1}", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(position, fontSize = 13.sp, color = if (index <= currentPosition) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StatusCard(message: String, errorMessage: String?, isComplete: Boolean) {
    val backgroundColor = when {
        errorMessage != null -> MaterialTheme.colorScheme.errorContainer
        isComplete -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when {
        errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
        isComplete -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = backgroundColor), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(when { errorMessage != null -> Icons.Default.Error; isComplete -> Icons.Default.CheckCircle; else -> Icons.Default.Info }, contentDescription = "Status Status indicator decoration", tint = textColor)
            Text(text = errorMessage ?: message, color = textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CalibrationResultsCard(calibrationData: CalibrationData, quality: String) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Calibration Results", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                QualityBadge(quality = quality)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CalibrationMetric("Gyro X", calibrationData.gyroOffsetX, "rad/s")
                CalibrationMetric("Gyro Y", calibrationData.gyroOffsetY, "rad/s")
                CalibrationMetric("Gyro Z", calibrationData.gyroOffsetZ, "rad/s")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CalibrationMetric("Accel X", calibrationData.accelOffsetX, "m/s²")
                CalibrationMetric("Accel Y", calibrationData.accelOffsetY, "m/s²")
                CalibrationMetric("Accel Z", calibrationData.accelOffsetZ, "m/s²")
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(visible = expanded) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CalibrationMetric("Mag X", calibrationData.magOffsetX, "μT")
                        CalibrationMetric("Mag Y", calibrationData.magOffsetY, "μT")
                        CalibrationMetric("Mag Z", calibrationData.magOffsetZ, "μT")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CalibrationMetric("Accel Scale X", calibrationData.accelScaleX, "")
                        CalibrationMetric("Accel Scale Y", calibrationData.accelScaleY, "")
                        CalibrationMetric("Accel Scale Z", calibrationData.accelScaleZ, "")
                    }
                }
            }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) { Text(if (expanded) "Show Less" else "Show More") }
        }
    }
}

@Composable
fun CalibrationMetric(label: String, value: Float, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(String.format(Locale.getDefault(), "%.3f", value), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Text("$label $unit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QualityBadge(quality: String) {
    val (color, text) = when (quality.lowercase(Locale.getDefault())) {
        "excellent" -> Color(0xFF4CAF50) to "Excellent"
        "good" -> Color(0xFF00BCD4) to "Good"
        "fair" -> Color(0xFFFFC107) to "Fair"
        else -> Color(0xFFF44336) to "Poor"
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.2f)) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
fun SensorVisualizer(roll: Float, pitch: Float, yaw: Float, size: VisualizerSize) {
    Box(modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
        Text(String.format(Locale.getDefault(), "r:%.1f p:%.1f", roll, pitch), fontSize = 11.sp)
    }
}

@Composable
fun GyroscopeVisualizer(x: Float, y: Float, z: Float) {
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Text("Gyro Active Visualization Matrix Stream Data", fontSize = 12.sp)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) { content() }
}

// --- Local Layout Fallback Component Mocking Layer ---
@Composable
fun CircleProgressWithLabel(progress: Int) {}
@Composable
fun GyroscopeVisualizer() {}

// --- Target ViewModel Interface representation layer ---
class CalibrationViewModel : androidx.lifecycle.ViewModel() {
    val uiState = kotlinx.coroutines.flow.MutableStateFlow(CalibrationUiState())
    fun startCalibration() {}
    fun skipCalibration() {}
    fun previousStep() {}
    fun stopCollection() {}
    fun nextAccelPosition() {}
    fun abortCalibration() {}
    fun resetCalibration() {}
    fun openHelp() {}
    fun jumpToPosition(index: Int) {}
}