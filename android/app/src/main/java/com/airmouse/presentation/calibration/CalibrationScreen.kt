// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationScreen.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.presentation.calibration.ConfettiEffect
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions? = null,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()
    val calibrationData by viewModel.calibrationData.collectAsStateWithLifecycle()

    var animationTriggered by remember { mutableStateOf(false) }
    var confettiActive by remember { mutableStateOf(false) }

    // Synchronize local step with ViewModel's step
    val currentStep = if (uiState.isComplete) 3 else (uiState.currentStep - 1).coerceAtLeast(0)
    val totalSteps = 4

    val scale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "success_scale"
    )

    val fadeIn by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "fade_in"
    )

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            animationTriggered = true
            delay(500)
            confettiActive = true
            delay(4000)
            confettiActive = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))))
    ) {
        if (confettiActive) ConfettiEffect()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (uiState.isComplete) "Success!" else "Calibration Guide", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigationActions?.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (uiState.isComplete) {
                CompletionScreen(
                    paddingValues = paddingValues,
                    calibrationData = calibrationData,
                    onContinue = { navigationActions?.navigateToHome(); onComplete() },
                    onRecalibrate = { viewModel.resetCalibration() }
                )
            } else {
                CalibrationGuideScreen(
                    paddingValues = paddingValues,
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    uiState = uiState,
                    viewModel = viewModel,
                    onStartCalibration = { viewModel.startCalibration() },
                    navigationActions = navigationActions,
                    calibrationData = calibrationData
                )
            }
        }
    }
}

@Composable
private fun CompletionScreen(
    paddingValues: PaddingValues,
    calibrationData: CalibrationData?,
    onContinue: () -> Unit,
    onRecalibrate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Calibration complete", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        CalibrationStatusCard(
            isCalibrating = false,
            progress = 100,
            statusMessage = "Calibration complete",
            samplesCollected = 0,
            totalSamplesNeeded = 0,
            calibrationData = calibrationData
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRecalibrate, modifier = Modifier.weight(1f)) { Text("Recalibrate") }
            Button(onClick = onContinue, modifier = Modifier.weight(1f)) { Text("Continue") }
        }
    }
}

@Composable
fun CalibrationGuideScreen(
    paddingValues: PaddingValues,
    currentStep: Int,
    totalSteps: Int,
    uiState: CalibrationUiState,
    viewModel: CalibrationViewModel,
    onStartCalibration: () -> Unit,
    navigationActions: NavigationActions?,
    calibrationData: CalibrationData?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HeroCalibrationCard(uiState = uiState)
        }

        item {
            CalibrationStepTracker(currentStep = currentStep, totalSteps = totalSteps)
        }

        item {
            AnimatedInstructionCard(step = currentStep, totalSteps = totalSteps, uiState = uiState)
        }

        item {
            CalibrationStatusCard(
                isCalibrating = uiState.isCalibrating,
                progress = uiState.progress,
                statusMessage = uiState.statusMessage,
                samplesCollected = uiState.samplesCollected,
                totalSamplesNeeded = uiState.totalSamplesNeeded,
                calibrationData = calibrationData
            )
        }

        item {
            CalibrationGuidanceCard()
        }

        item {
            LiveSensorDataCard(uiState = uiState)
        }

        item {
            if (!uiState.isCalibrating) {
                Button(
                    onClick = onStartCalibration,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Calibration", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.resetCalibration() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cancel")
                }
            }
        }
        
        item {
            SensorStatusDisplay(uiState = uiState, calibrationData = calibrationData)
        }
    }
}

@Composable
private fun HeroCalibrationCard(uiState: CalibrationUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A))))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Air Mouse calibration", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Follow the gyroscope, magnetometer, and six-position accelerometer steps exactly as the assignment describes.",
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalibrationTag("Gyro bias")
                CalibrationTag("Accel 6-pos")
                CalibrationTag("Mag min/max")
            }
            if (uiState.isCalibrating) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF22C55E),
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            }
        }
    }
}

@Composable
private fun CalibrationTag(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun CalibrationStepTracker(currentStep: Int, totalSteps: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Step progress", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(totalSteps) { i ->
                    val active = i == currentStep
                    val done = i < currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                when {
                                    done -> Color(0xFF10B981)
                                    active -> Color(0xFF6366F1)
                                    else -> Color(0xFF1E293B)
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    )
                }
            }
            Text(
                "Step ${currentStep + 1} of $totalSteps",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AnimatedInstructionCard(step: Int, totalSteps: Int, uiState: CalibrationUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Step ${step + 1} of $totalSteps", color = Color.White.copy(alpha = 0.7f))
            Text(
                text = when (step) {
                    0 -> "Gyroscope"
                    1 -> "Magnetometer"
                    2 -> "Accelerometer"
                    else -> "Calibration"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(uiState.stepInstruction.ifBlank { uiState.statusMessage }, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun CalibrationStatusCard(
    isCalibrating: Boolean,
    progress: Int,
    statusMessage: String,
    samplesCollected: Int,
    totalSamplesNeeded: Int,
    calibrationData: CalibrationData?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(statusMessage.ifBlank { "Ready" }, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Progress: $progress%", color = Color.White.copy(alpha = 0.8f))
            if (isCalibrating || totalSamplesNeeded > 0) {
                Text("Samples: $samplesCollected / $totalSamplesNeeded", color = Color.White.copy(alpha = 0.8f))
            }
            if (calibrationData != null) {
                Text("Saved calibration available", color = Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun SensorStatusDisplay(uiState: CalibrationUiState, calibrationData: CalibrationData?) {
    val message = when {
        uiState.isComplete -> "Calibration saved"
        uiState.isCalibrating -> "Collecting sensor data"
        calibrationData != null -> "Existing calibration loaded"
        else -> "Awaiting start"
    }
    Text(message, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CalibrationGuidanceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("How to calibrate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "The assignment requires calibrated sensor data, not raw readings. The important parts are:\n" +
                    "1. Read the gyroscope while the phone is still, collect several samples, and subtract the average bias so the stationary value becomes close to zero.\n" +
                    "2. Calibrate the accelerometer in six orientations and compare each axis against gravity (about 9.81 m/s²) to estimate offsets and scale.\n" +
                    "3. Calibrate the magnetometer by rotating the phone through all directions and storing min/max values for each axis.\n" +
                    "4. Compute magnetometer offset as (min + max) / 2 and scale as (max - min) / 2, then correct readings with (Raw - Offset) / Scale.\n" +
                    "5. Use a sensor-fusion filter such as Madgwick AHRS, or another suitable filter, to combine the calibrated sensors and reduce drift.\n" +
                    "6. Raw sensor values should not be used directly for control; smoothing, deadzones, and drift reduction are required so the cursor does not jump or drift.\n" +
                    "7. The live readings below help verify that calibration is stable before saving.",
                color = Color.White.copy(alpha = 0.82f),
                lineHeight = 20.sp
            )
            Text(
                "The app combines calibrated sensor data to infer motion direction, click, scroll, and cursor movement without relying on raw unfiltered readings.",
                color = Color(0xFF93C5FD),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun LiveSensorDataCard(uiState: CalibrationUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Live sensor values", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            SensorValueRow("Gyro", uiState.gyroData)
            SensorValueRow("Accel", uiState.accelData)
            SensorValueRow("Mag", uiState.magData)
            SensorValueRow("Orientation", Triple(uiState.roll, uiState.pitch, uiState.yaw))
        }
    }
}

@Composable
private fun SensorValueRow(label: String, values: Triple<Float, Float, Float>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
        Text(
            text = "${formatSensorValue(values.first)}, ${formatSensorValue(values.second)}, ${formatSensorValue(values.third)}",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

private fun formatSensorValue(value: Float): String = String.format(Locale.US, "%.3f", value)
