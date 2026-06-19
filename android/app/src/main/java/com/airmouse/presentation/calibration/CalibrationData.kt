package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 1. ARCHITECTURE MODELS & STATE DATA
// ==========================================

data class CalibrationData(
    val gyroBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)
)

data class CalibrationUiState(
    val message: String = "Keep your device steady",
    val errorMessage: String? = null,
    val isComplete: Boolean = false,
    val isCollecting: Boolean = false,
    val isSkipped: Boolean = false,
    val progress: Int = 0,
    val samplesCollected: Int = 0,
    val totalSamples: Int = 100,
    val totalSamplesNeeded: Int = 100,
    val currentStep: Int = 1,
    val currentPosition: Int = 0,
    val totalPositions: Int = 4,
    val quality: String = "GOOD",
    val calibrationQuality: String = "GOOD",
    val stepTitle: String = "Step Title",
    val stepInstruction: String = "Follow layout instruction",
    val stepDescription: String = "Hold steady in this position",
    val statusMessage: String = "System ready",
    val calibrationData: CalibrationData = CalibrationData(),
    val gyroData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f
)

enum class VisualizerSize {
    SMALL, MEDIUM, LARGE
}

val positionsList = listOf(
    "Flat on Table",
    "Tilted Left",
    "Tilted Right",
    "Vertical Stand"
)

class CalibrationViewModel {
    val uiState: StateFlow<CalibrationUiState> = MutableStateFlow(CalibrationUiState())
    fun startCalibration() {}
    fun selectPosition(index: Int) {}
}

interface NavigationActions {
    fun navigateBack()
}

// ==========================================
// 2. MAIN CALIBRATION SCREEN
// ==========================================

@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions,
    viewModel: CalibrationViewModel = CalibrationViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sensor Calibration",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        AnimatedInstructionIcon(uiState = uiState)

        Spacer(modifier = Modifier.height(16.dp))

        ProgressCard(
            progress = uiState.progress,
            samplesCollected = uiState.samplesCollected,
            totalSamples = uiState.totalSamplesNeeded,
            currentStep = uiState.currentStep,
            currentPosition = uiState.currentPosition,
            totalPositions = uiState.totalPositions
        )

        Spacer(modifier = Modifier.height(16.dp))

        SensorVisualizer(
            roll = uiState.roll,
            pitch = uiState.pitch,
            yaw = uiState.yaw,
            size = VisualizerSize.MEDIUM
        )

        Spacer(modifier = Modifier.height(16.dp))

        SensorDataCard(
            gyroData = uiState.gyroData,
            accelData = uiState.accelData,
            magData = uiState.magData
        )

        Spacer(modifier = Modifier.height(16.dp))

        PositionGuideCard(
            currentPosition = uiState.currentPosition,
            positions = positionsList,
            onPositionClick = { viewModel.selectPosition(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusCard(
            message = uiState.statusMessage,
            errorMessage = uiState.errorMessage,
            isComplete = uiState.isComplete
        )

        if (uiState.isComplete) {
            Spacer(modifier = Modifier.height(16.dp))
            CalibrationResultsCard(
                calibrationData = uiState.calibrationData,
                quality = uiState.calibrationQuality
            )
        }
    }
}

// ==========================================
// 3. SUB-COMPONENTS & LAYOUT PARTS
// ==========================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        content()
    }
}

@Composable
fun AnimatedInstructionIcon(uiState: CalibrationUiState) {
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { uiState.progress / 100f },
            modifier = Modifier.fillMaxSize(),
        )
        Text(text = "${uiState.progress}%", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProgressCard(
    progress: Int,
    samplesCollected: Int,
    totalSamples: Int,
    currentStep: Int,
    currentPosition: Int,
    totalPositions: Int
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step $currentStep: Position ${currentPosition + 1}/$totalPositions", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedProgressBar(progress = progress / 100f)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Samples: $samplesCollected / $totalSamples",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

@Composable
fun SensorDataCard(
    gyroData: Triple<Float, Float, Float>,
    accelData: Triple<Float, Float, Float>,
    magData: Triple<Float, Float, Float>
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SensorRow("Gyroscope", gyroData.first, gyroData.second, gyroData.third, Color(0xFFEF4444))
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
            SensorRow("Accelerometer", accelData.first, accelData.second, accelData.third, Color(0xFF3B82F6))
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
            SensorRow("Magnetometer", magData.first, magData.second, magData.third, Color(0xFF10B981))
        }
    }
}

@Composable
fun SensorRow(title: String, x: Float, y: Float, z: Float, color: Color) {
    Column {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SensorValue("X", x, color)
            SensorValue("Y", y, color)
            SensorValue("Z", z, color)
        }
    }
}

@Composable
fun RowScope.SensorValue(label: String, value: Float, color: Color) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(String.format("%.2f", value), fontSize = 12.sp)
    }
}

@Composable
fun PositionGuideCard(
    currentPosition: Int,
    positions: List<String>,
    onPositionClick: (Int) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Calibration Positions", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            positions.forEachIndexed { index, title ->
                TextButton(
                    onClick = { onPositionClick(index) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (index == currentPosition) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = title, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun StatusCard(message: String, errorMessage: String?, isComplete: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = errorMessage ?: message,
                color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            if (isComplete) {
                Text("Ready to finalize", color = Color(0xFF10B981), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CalibrationResultsCard(calibrationData: CalibrationData, quality: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Calibration Step Matrix", fontWeight = FontWeight.Bold)
                Text("Offsets applied successfully", fontSize = 12.sp)
            }
            QualityBadge(quality = quality)
        }
    }
}

@Composable
fun QualityBadge(quality: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10B981).copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(quality, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun SensorVisualizer(roll: Float, pitch: Float, yaw: Float, size: VisualizerSize) {
    val dimensions = when (size) {
        VisualizerSize.SMALL -> 60.dp
        VisualizerSize.MEDIUM -> 100.dp
        VisualizerSize.LARGE -> 140.dp
    }
    Box(
        modifier = Modifier
            .size(dimensions)
            .background(Color.Gray.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        GyroscopeVisualizer(x = roll, y = pitch, z = yaw)
    }
}

@Composable
fun GyroscopeVisualizer(x: Float, y: Float, z: Float) {
    Text(
        text = "R:${x.toInt()} P:${y.toInt()}",
        fontSize = 11.sp,
        style = MaterialTheme.typography.labelSmall
    )
}

// ==========================================
// 4. CALIBRATION RESULT SCREEN
// ==========================================

@Composable
fun CalibrationResultScreen(
    quality: String,
    onContinue: () -> Unit,
    onRecalibrate: () -> Unit
) {
    var animationTriggered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_pop_animation"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF059669),
                        Color(0xFF10B981)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 64.sp, color = Color(0xFF059669))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Calibration Complete!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (quality.uppercase()) {
                    "EXCELLENT" -> "Your device is perfectly calibrated!"
                    "GOOD" -> "Your device is calibrated with good accuracy"
                    "FAIR" -> "Calibration complete, but consider recalibrating for best results"
                    else -> "Your device is ready to use"
                },
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Calibration Quality",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = quality,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    "Start Using Air Mouse",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF059669)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onRecalibrate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Recalibrate",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Simple types required for standard Flow emissions compilation wrappers
interface StateFlow<out T> { val value: T }
interface MutableStateFlow<T> : StateFlow<T> { override var value: T }
fun <T> MutableStateFlow(value: T): MutableStateFlow<T> = object : MutableStateFlow<T> {
    override var value: T = value
}
@Composable
fun <T> StateFlow<T>.collectAsState(): State<T> = remember { mutableStateOf(this.value) }