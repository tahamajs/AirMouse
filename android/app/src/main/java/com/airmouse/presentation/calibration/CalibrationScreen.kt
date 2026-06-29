package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.presentation.calibration.ConfettiEffect
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.DonutChart
import kotlinx.coroutines.delay
import java.util.Locale

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke

// ============================================================
// Main Calibration Screen
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions? = null,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calibrationData by viewModel.calibrationData.collectAsStateWithLifecycle()
    val calibrationProgress by viewModel.calibrationProgress.collectAsStateWithLifecycle()
    val phase = uiState.calibrationPhase

    var confettiActive by remember { mutableStateOf(false) }

    val currentStep = if (uiState.isComplete) 3 else (uiState.currentStep - 1).coerceAtLeast(0)
    val totalSteps = uiState.totalSteps
    val stepName = remember(uiState.currentStep) { calibrationStepName(uiState.currentStep) }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
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
                    title = {
                        Column {
                            Text(
                                if (uiState.isComplete) "Calibration complete" else "Assignment calibration",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Gyro, accelerometer, and magnetometer setup",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigationActions?.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (uiState.isComplete) {
                val resultQuality = calibrationData?.quality?.name ?: CalibrationQuality.UNKNOWN.name
                CompletionScreen(
                    paddingValues = paddingValues,
                    calibrationData = calibrationData,
                    alreadySaved = uiState.statusMessage.contains("saved", ignoreCase = true),
                    onContinue = {
                        if (navigationActions != null) {
                            navigationActions.navigateToCalibrationResult(resultQuality)
                        } else {
                            onComplete()
                        }
                    },
                    onRecalibrate = { viewModel.resetCalibration() }
                )
            } else {
                when (phase) {
                    CalibrationPhase.INTRO, CalibrationPhase.COUNTDOWN -> {
                        CalibrationStepIntroScreen(
                            paddingValues = paddingValues,
                            currentStep = currentStep,
                            totalSteps = totalSteps,
                            stepName = stepName,
                            phase = phase,
                            uiState = uiState,
                            onStartStep = {
                                if (uiState.currentStep == 0) {
                                    viewModel.startCalibration()
                                } else {
                                    viewModel.beginCurrentStep()
                                }
                            },
                            onCancel = { viewModel.resetCalibration() }
                        )
                    }
                    CalibrationPhase.SAMPLING -> {
                        CalibrationSamplingScreen(
                            paddingValues = paddingValues,
                            currentStep = currentStep,
                            totalSteps = totalSteps,
                            stepName = stepName,
                            uiState = uiState,
                            calibrationProgress = calibrationProgress,
                            onCancel = { viewModel.resetCalibration() }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Step Intro Screen
// ============================================================

@Composable
private fun CalibrationStepIntroScreen(
    paddingValues: PaddingValues,
    currentStep: Int,
    totalSteps: Int,
    stepName: String,
    phase: CalibrationPhase,
    uiState: CalibrationUiState,
    onStartStep: () -> Unit,
    onCancel: () -> Unit
) {
    val stepIndex = currentStep.coerceIn(0, 2)
    val accent = remember(stepIndex) {
        when (stepIndex) {
            0 -> Color(0xFF38BDF8)
            1 -> Color(0xFFF59E0B)
            else -> Color(0xFF22C55E)
        }
    }
    val title = when (stepIndex) {
        0 -> "Gyroscope warmup"
        1 -> "Magnetometer sweep"
        else -> "Accelerometer six‑position check"
    }
    val subtitle = when (stepIndex) {
        0 -> "We will first measure the idle gyro drift while the phone is steady."
        1 -> "Next, we capture magnetic field bias while the phone is held still."
        else -> "Finally, we record six orientations so the acceleration model is grounded."
    }
    val guidance = when (stepIndex) {
        0 -> "Put the phone on a flat surface. When you tap Start, a short animation plays before sampling begins."
        1 -> "Keep the phone still and clear of large metal objects while the preview runs."
        else -> "Follow the on‑screen pose for each orientation; sampling starts only after the animation finishes."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        accent.copy(alpha = 0.28f),
                        Color(0xFF111827)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Step ${currentStep + 1} of $totalSteps") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White
                        ),
                        elevation = AssistChipDefaults.assistChipElevation(elevation = 0.dp)
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text(if (phase == CalibrationPhase.COUNTDOWN) "Preparing" else "Ready") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = accent.copy(alpha = 0.20f),
                            labelColor = Color.White
                        ),
                        elevation = AssistChipDefaults.assistChipElevation(elevation = 0.dp)
                    )
                }

                Text(
                    title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 34.sp
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DonutChart(
                            percentage = (uiState.progress / 100f).coerceIn(0f, 1f),
                            size = 92,
                            color = accent,
                            backgroundColor = Color.White.copy(alpha = 0.08f)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.progress >= 100) "Ready to collaborate" else "Calibration progress",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = uiState.statusMessage.ifBlank { "Follow the guided steps to unlock collaboration." },
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Phone3DAnimation(
                    stepIndex = stepIndex,
                    currentPosition = uiState.currentPosition,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Text(
                    text = when (stepIndex) {
                        0 -> "GYROSCOPE WARMUP"
                        1 -> "MAGNETOMETER SWEEP"
                        else -> "ACCELEROMETER CHECK"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                CalibrationStepTracker(currentStep = currentStep, totalSteps = totalSteps)

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.07f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Before you start", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            guidance,
                            color = Color.White.copy(alpha = 0.82f),
                            lineHeight = 20.sp
                        )
                        if (phase == CalibrationPhase.COUNTDOWN) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = accent,
                                trackColor = Color.White.copy(alpha = 0.12f)
                            )
                            Text(
                                "Starting ${stepName.lowercase(Locale.US)} sampling soon...",
                                color = Color(0xFF93C5FD),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                uiState.errorMessage?.let { message ->
                    CalibrationErrorPanel(message = message)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.88f))
                }
                Button(
                    onClick = onStartStep,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = phase == CalibrationPhase.INTRO,
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// Sampling Screen
// ============================================================

@Composable
private fun CalibrationSamplingScreen(
    paddingValues: PaddingValues,
    currentStep: Int,
    totalSteps: Int,
    stepName: String,
    uiState: CalibrationUiState,
    calibrationProgress: Int,
    onCancel: () -> Unit
) {
    val stepIndex = currentStep.coerceIn(0, 2)
    val accent = when (stepIndex) {
        0 -> Color(0xFF38BDF8)
        1 -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }
    val instruction = when (stepIndex) {
        0 -> "Hold the phone flat and still while we collect gyro bias samples."
        1 -> "Keep the phone still and avoid magnets while magnetometer samples are captured."
        else -> "Rotate through the six orientations and hold each position steady until the capture ends."
    }
    val label = when (stepIndex) {
        0 -> "GYROSCOPE"
        1 -> "MAGNETOMETER"
        else -> "ACCELEROMETER"
    }
    val sampleText = if (uiState.totalSamplesNeeded > 0) {
        "${uiState.samplesCollected}/${uiState.totalSamplesNeeded}"
    } else if (stepIndex == 2) {
        "${uiState.currentPosition + 1}/${uiState.totalPositions}"
    } else {
        "Collecting"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020617),
                        accent.copy(alpha = 0.30f),
                        Color(0xFF111827)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Step ${currentStep + 1} of $totalSteps") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White
                        ),
                        elevation = AssistChipDefaults.assistChipElevation(elevation = 0.dp)
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text("Sampling") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = accent.copy(alpha = 0.20f),
                            labelColor = Color.White
                        ),
                        elevation = AssistChipDefaults.assistChipElevation(elevation = 0.dp)
                    )
                }

                Text(
                    "Collecting $stepName data",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    instruction,
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Phone3DAnimation(
                    stepIndex = stepIndex,
                    currentPosition = uiState.currentPosition,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Text(
                    text = "$label: $sampleText",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Live progress", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        LinearProgressIndicator(
                            progress = { calibrationProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = accent,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                        Text(
                            uiState.statusMessage.ifBlank { "Collecting data..." },
                            color = Color.White.copy(alpha = 0.84f)
                        )
                        if (uiState.stepInstruction.isNotBlank()) {
                            Text(
                                uiState.stepInstruction,
                                color = Color(0xFFBFDBFE),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                uiState.errorMessage?.let { message ->
                    CalibrationErrorPanel(message = message)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cancel", color = Color.White.copy(alpha = 0.90f))
                }
            }
        }
    }
}

// ============================================================
// Error Panel
// ============================================================

@Composable
private fun CalibrationErrorPanel(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEF4444).copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFCA5A5))
            Text(message, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

private fun calibrationStepName(stepNumber: Int): String {
    return when (stepNumber) {
        1 -> "Gyroscope"
        2 -> "Magnetometer"
        3 -> "Accelerometer"
        else -> "Calibration"
    }
}

// ============================================================
// Completion Screen
// ============================================================

@Composable
private fun CompletionScreen(
    paddingValues: PaddingValues,
    calibrationData: CalibrationData?,
    alreadySaved: Boolean,
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Calibration complete", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Your sensor offsets and scale factors are now saved, so the cursor tracking can run with better stability and less drift.",
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalibrationTag("Saved to device")
                    CalibrationTag("Ready for demo")
                    CalibrationTag("Low drift")
                    if (alreadySaved) CalibrationTag("Already calibrated")
                }
            }
        }
        CalibrationStatusCard(
            isCalibrating = false,
            progress = 100,
            statusMessage = "Calibration complete",
            samplesCollected = 0,
            totalSamplesNeeded = 0,
            calibrationData = calibrationData
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("What was calibrated", color = Color.White, fontWeight = FontWeight.Bold)
                Text("1. Gyroscope bias removed while the phone is stationary.", color = Color.White.copy(alpha = 0.82f))
                Text("2. Accelerometer adjusted across six orientations.", color = Color.White.copy(alpha = 0.82f))
                Text("3. Magnetometer min/max values stored for scale correction.", color = Color.White.copy(alpha = 0.82f))
                Text("4. Filtered data is now ready for motion, click, and scroll detection.", color = Color(0xFF93C5FD))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRecalibrate, modifier = Modifier.weight(1f)) { Text("Recalibrate") }
            Button(onClick = onContinue, modifier = Modifier.weight(1f)) { Text("View Results") }
        }
    }
}

// ============================================================
// Helper Components
// ============================================================

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

// ============================================================
// Animated 3D Phone Composable with Rich Visual Guides
// ============================================================

@Composable
fun Phone3DAnimation(
    stepIndex: Int, // 0: Gyro, 1: Mag, 2: Accel
    currentPosition: Int, // 0 to 5 for Accel positions
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "phone_3d")

    // -- Position label for each step/position --
    val positionLabel = when (stepIndex) {
        0 -> "Place phone flat on table"
        1 -> "Move phone in a figure‑8"
        2 -> when (currentPosition) {
            0 -> "Screen facing UP (flat on table)"
            1 -> "Screen facing DOWN (flip over)"
            2 -> "Left edge DOWN (landscape left)"
            3 -> "Right edge DOWN (landscape right)"
            4 -> "Top edge DOWN (upside-down portrait)"
            5 -> "Bottom edge DOWN (normal portrait, stand upright)"
            else -> "Hold phone as shown"
        }
        else -> "Hold phone as shown"
    }

    // -- Position emoji / icon text --
    val positionEmoji = when (stepIndex) {
        0 -> "📱 ➡ 🪑"
        1 -> "📱 ∞ 🔄"
        2 -> when (currentPosition) {
            0 -> "📱⬆"
            1 -> "📱⬇"
            2 -> "📱⬅"
            3 -> "📱➡"
            4 -> "📱🔃"
            5 -> "📱🔝"
            else -> "📱"
        }
        else -> "📱"
    }

    val targetRotationX by animateFloatAsState(
        targetValue = when (stepIndex) {
            0 -> 60f // Laying flat on table
            1 -> 45f // Tilted for sweep
            2 -> when (currentPosition) {
                0 -> 60f   // Flat Screen Up
                1 -> -60f  // Flat Screen Down
                2 -> 0f    // Left Side Down
                3 -> 0f    // Right Side Down
                4 -> 45f   // Top Edge Down
                5 -> 45f   // Bottom Edge Down
                else -> 0f
            }
            else -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rx"
    )

    // Magnetometer sweep animation
    val magSweepY by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mag_sweep_y"
    )
    val magSweepZ by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mag_sweep_z"
    )

    val targetRotationY by animateFloatAsState(
        targetValue = when (stepIndex) {
            2 -> when (currentPosition) {
                1 -> 180f // Face down
                else -> 0f
            }
            else -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ry"
    )

    val targetRotationZ by animateFloatAsState(
        targetValue = when (stepIndex) {
            2 -> when (currentPosition) {
                2 -> -90f  // Landscape Left
                3 -> 90f   // Landscape Right
                4 -> 180f  // Top Edge Down
                5 -> 0f    // Bottom Edge Down (portrait)
                else -> 0f
            }
            else -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rz"
    )

    // Gyro ripple effect (flat on table)
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "gyro_ripple"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "gyro_alpha"
    )

    // Pulsing guide arrow
    val arrowPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_pulse"
    )
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_alpha"
    )

    // Figure-8 path animation for magnetometer
    val figure8Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "figure8"
    )

    val accent = when (stepIndex) {
        0 -> Color(0xFF38BDF8)
        1 -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ---- Position Label Banner ----
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = accent.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = positionEmoji,
                    fontSize = 20.sp
                )
                Text(
                    text = positionLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ---- 3D Phone + Guides ----
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Table surface shadow (for flat positions)
            if (stepIndex == 0 || (stepIndex == 2 && currentPosition in listOf(0, 1))) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(80.dp)
                        .graphicsLayer {
                            rotationX = 75f
                            translationY = 55f
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )
            }


            // Gyro ripple rings
            if (stepIndex == 0) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = rippleScale
                            scaleY = rippleScale
                            rotationX = 75f
                            translationY = 20f
                        }
                        .border(2.dp, accent.copy(alpha = rippleAlpha), CircleShape)
                )
                // Second ripple ring offset
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = rippleScale * 0.8f
                            scaleY = rippleScale * 0.8f
                            rotationX = 75f
                            translationY = 20f
                        }
                        .border(1.5.dp, accent.copy(alpha = rippleAlpha * 0.6f), CircleShape)
                )
            }

            // Figure-8 trace for magnetometer
            if (stepIndex == 1) {
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer { rotationX = 30f }
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val rx = size.width * 0.35f
                    val ry = size.height * 0.2f
                    val steps = 100
                    // Draw the figure-8 path
                    for (i in 0 until steps) {
                        val t = i.toFloat() / steps * 2 * Math.PI.toFloat()
                        val x = cx + rx * kotlin.math.sin(t)
                        val y = cy + ry * kotlin.math.sin(2 * t)
                        val t2 = (i + 1).toFloat() / steps * 2 * Math.PI.toFloat()
                        val x2 = cx + rx * kotlin.math.sin(t2)
                        val y2 = cy + ry * kotlin.math.sin(2 * t2)
                        drawLine(
                            color = Color(0xFFF59E0B).copy(alpha = 0.3f),
                            start = Offset(x, y),
                            end = Offset(x2, y2),
                            strokeWidth = 2f
                        )
                    }
                    // Draw a moving dot on the path
                    val t = figure8Progress * 2 * Math.PI.toFloat()
                    val dotX = cx + rx * kotlin.math.sin(t)
                    val dotY = cy + ry * kotlin.math.sin(2 * t)
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 8f,
                        center = Offset(dotX, dotY)
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = 0.4f),
                        radius = 16f,
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // Animated guide arrows for accelerometer positions
            if (stepIndex == 2) {
                when (currentPosition) {
                    0 -> { // Screen Up: arrow pointing down onto table
                        Text(
                            "⬇",
                            fontSize = 28.sp,
                            color = Color.White.copy(alpha = arrowAlpha),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer { translationY = arrowPulse }
                        )
                        Text(
                            "🪑 Table",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                    1 -> { // Screen Down: arrow pointing down + flip indicator
                        Text(
                            "🔄",
                            fontSize = 28.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer {
                                    translationY = arrowPulse
                                    rotationZ = arrowPulse * 3
                                }
                        )
                        Text(
                            "Flip face‑down",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                    2 -> { // Left edge down: arrow pointing left
                        Text(
                            "⬅",
                            fontSize = 28.sp,
                            color = Color.White.copy(alpha = arrowAlpha),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .graphicsLayer { translationX = -arrowPulse }
                        )
                        Text(
                            "Left edge down",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                    3 -> { // Right edge down: arrow pointing right
                        Text(
                            "➡",
                            fontSize = 28.sp,
                            color = Color.White.copy(alpha = arrowAlpha),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .graphicsLayer { translationX = arrowPulse }
                        )
                        Text(
                            "Right edge down",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                    4 -> { // Top edge down: arrow pointing up (phone rotated 180)
                        Text(
                            "⬆",
                            fontSize = 28.sp,
                            color = Color.White.copy(alpha = arrowAlpha),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer { translationY = -arrowPulse }
                        )
                        Text(
                            "Top edge down",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                    5 -> { // Bottom edge down: arrow pointing down (normal portrait standing)
                        Text(
                            "⬇",
                            fontSize = 28.sp,
                            color = Color.White.copy(alpha = arrowAlpha),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .graphicsLayer { translationY = arrowPulse }
                        )
                        Text(
                            "Stand upright",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 4.dp)
                        )
                    }
                }
            }

            // Gyroscope: "STAY STILL" indicator
            if (stepIndex == 0) {
                Text(
                    "✋ STAY STILL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.copy(alpha = arrowAlpha),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }

            // ---- The 3D Phone Card ----
            Card(
                modifier = Modifier
                    .width(100.dp)
                    .height(180.dp)
                    .graphicsLayer {
                        rotationX = targetRotationX
                        rotationY = targetRotationY + if (stepIndex == 1) magSweepY else 0f
                        rotationZ = targetRotationZ + if (stepIndex == 1) magSweepZ else 0f
                        cameraDistance = 12 * density
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (stepIndex == 2 && currentPosition == 1) Color(0xFF1E293B) else Color(0xFF0F172A)
                ),
                border = BorderStroke(2.5.dp, Color.White.copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Notch
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                            .align(Alignment.TopCenter)
                    )

                    // Screen content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (stepIndex == 2 && currentPosition == 1) {
                                    Modifier.background(Color.Black)
                                } else {
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF4F46E5).copy(alpha = 0.15f),
                                                Color(0xFF06B6D4).copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                                }
                            )
                    ) {
                        if (stepIndex == 2 && currentPosition != 1) {
                            // Accelerometer position icons inside the screen
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when (currentPosition) {
                                        0 -> Icons.Default.VerticalAlignTop
                                        2 -> Icons.Default.ArrowBack
                                        3 -> Icons.Default.ArrowForward
                                        4 -> Icons.Default.ArrowUpward
                                        5 -> Icons.Default.ArrowDownward
                                        else -> Icons.Default.ScreenRotation
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (currentPosition) {
                                        0 -> "SCREEN\nUP"
                                        2 -> "LEFT\nDOWN"
                                        3 -> "RIGHT\nDOWN"
                                        4 -> "UPSIDE\nDOWN"
                                        5 -> "UPRIGHT"
                                        else -> "ALIGN"
                                    },
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 11.sp
                                )
                            }
                        } else if (stepIndex == 0) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else if (stepIndex == 1) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Face down camera indicator
                    if (stepIndex == 2 && currentPosition == 1) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "FACE\nDOWN",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ---- Accel Position Progress Dots ----
        if (stepIndex == 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                repeat(6) { i ->
                    val dotColor = when {
                        i < currentPosition -> Color(0xFF22C55E)  // completed: green
                        i == currentPosition -> accent            // current: accent
                        else -> Color.White.copy(alpha = 0.2f)    // upcoming: dim
                    }
                    val dotSize = if (i == currentPosition) 12.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor)
                            .then(
                                if (i == currentPosition) {
                                    Modifier.border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
            Text(
                text = "Position ${(currentPosition + 1).coerceAtMost(6)} of 6",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}