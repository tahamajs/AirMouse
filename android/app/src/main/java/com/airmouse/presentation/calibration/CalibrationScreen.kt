package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

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

    var confettiActive by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            confettiActive = true
            delay(5000)
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
                                when {
                                    uiState.isComplete -> "Calibration Complete"
                                    uiState.currentStep == 0 -> "Sensor Calibration"
                                    uiState.currentStep == 1 -> "Step 1 · Gyroscope"
                                    uiState.currentStep == 2 -> "Step 2 · Magnetometer"
                                    uiState.currentStep == 3 -> "Step 3 · Accelerometer"
                                    else -> "Calibration"
                                },
                                fontWeight = FontWeight.Bold, fontSize = 18.sp
                            )
                            if (!uiState.isComplete && uiState.currentStep > 0) {
                                Text(
                                    "Step ${uiState.currentStep} of 3",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
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
            when {
                // Step 4: Completion
                uiState.isComplete -> {
                    CompletionScreen(
                        paddingValues = paddingValues,
                        calibrationData = calibrationData,
                        alreadySaved = uiState.statusMessage.contains("saved", ignoreCase = true),
                        onContinue = {
                            val resultQuality = calibrationData?.quality?.name ?: CalibrationQuality.UNKNOWN.name
                            if (navigationActions != null) {
                                navigationActions.navigateToCalibrationResult(resultQuality)
                            } else {
                                onComplete()
                            }
                        },
                        onRecalibrate = { viewModel.resetCalibration() }
                    )
                }
                // Step 0: Welcome
                uiState.currentStep == 0 -> {
                    WelcomeScreen(
                        paddingValues = paddingValues,
                        uiState = uiState,
                        onStart = { viewModel.startCalibration() },
                        onRecalibrate = { viewModel.resetCalibration() }
                    )
                }
                // Steps 1-3: Calibration phases
                else -> {
                    CalibrationStepScreen(
                        paddingValues = paddingValues,
                        uiState = uiState,
                        onStartRecording = { viewModel.beginCurrentStep() },
                        onCancel = { viewModel.resetCalibration() }
                    )
                }
            }
        }
    }
}

// ============================================================
// Step 0: Welcome / Intro Screen
// ============================================================

@Composable
private fun WelcomeScreen(
    paddingValues: PaddingValues,
    uiState: CalibrationUiState,
    onStart: () -> Unit,
    onRecalibrate: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val hasExistingCalibration = uiState.calibrationData != null && uiState.calibrationData?.isCalibrated == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B).copy(alpha = 0.8f),
                        Color(0xFF0F172A)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Animated icon
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF6366F1).copy(alpha = glowAlpha),
                            Color(0xFF6366F1).copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Sensors,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Text(
            "Sensor Calibration",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Text(
            "Calibrate your phone's gyroscope, magnetometer, and accelerometer for precise cursor control.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Steps preview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("What we'll calibrate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                StepPreviewRow(
                    icon = "📱",
                    title = "Gyroscope",
                    description = "Measure idle drift (phone flat on table)",
                    accent = Color(0xFF38BDF8),
                    samples = "250 samples"
                )
                StepPreviewRow(
                    icon = "🧲",
                    title = "Magnetometer",
                    description = "Capture magnetic field (figure-8 motion)",
                    accent = Color(0xFFF59E0B),
                    samples = "500 samples"
                )
                StepPreviewRow(
                    icon = "📐",
                    title = "Accelerometer",
                    description = "Record 6 orientations (100 samples each)",
                    accent = Color(0xFF22C55E),
                    samples = "6 positions"
                )
            }
        }

        // Existing calibration info
        if (hasExistingCalibration) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calibration exists", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Quality: ${uiState.calibrationData?.quality?.name ?: "Unknown"}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                if (hasExistingCalibration) "Recalibrate" else "Start Calibration",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        if (hasExistingCalibration) {
            uiState.errorMessage?.let { message ->
                CalibrationErrorPanel(message = message)
            }
        }
    }
}

@Composable
private fun StepPreviewRow(
    icon: String,
    title: String,
    description: String,
    accent: Color,
    samples: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accent.copy(alpha = 0.12f)
        ) {
            Text(
                samples,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============================================================
// Steps 1-3: Unified Calibration Step Screen
// ============================================================

@Composable
private fun CalibrationStepScreen(
    paddingValues: PaddingValues,
    uiState: CalibrationUiState,
    onStartRecording: () -> Unit,
    onCancel: () -> Unit
) {
    val stepIndex = (uiState.currentStep - 1).coerceIn(0, 2) // 0=gyro, 1=mag, 2=accel
    val phase = uiState.calibrationPhase

    val accent = remember(stepIndex) {
        when (stepIndex) {
            0 -> Color(0xFF38BDF8)   // Blue for gyro
            1 -> Color(0xFFF59E0B)   // Amber for mag
            else -> Color(0xFF22C55E) // Green for accel
        }
    }

    val accentGradient = remember(stepIndex) {
        when (stepIndex) {
            0 -> listOf(Color(0xFF0F172A), Color(0xFF38BDF8).copy(alpha = 0.15f), Color(0xFF111827))
            1 -> listOf(Color(0xFF0F172A), Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFF111827))
            else -> listOf(Color(0xFF0F172A), Color(0xFF22C55E).copy(alpha = 0.15f), Color(0xFF111827))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Brush.verticalGradient(accentGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: Step indicator + title + instruction
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Step chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepProgressChips(currentStep = uiState.currentStep)
                    PhaseChip(phase = phase, accent = accent)
                }

                // Title
                Text(
                    text = when (stepIndex) {
                        0 -> "Gyroscope Calibration"
                        1 -> "Magnetometer Calibration"
                        else -> {
                            val pos = uiState.currentPosition.coerceIn(0, 5)
                            "Accelerometer · Position ${pos + 1}/6"
                        }
                    },
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 30.sp
                )

                // Instruction text
                Text(
                    text = getStepInstruction(stepIndex, uiState.currentPosition, phase),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Middle section: Animation + progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Phone 3D animation
                Phone3DAnimation(
                    stepIndex = stepIndex,
                    currentPosition = uiState.currentPosition,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Phase-specific content
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "phase_content"
                ) { currentPhase ->
                    when (currentPhase) {
                        CalibrationPhase.INTRO -> {
                            // Show instruction card
                            IntroInstructionCard(
                                stepIndex = stepIndex,
                                currentPosition = uiState.currentPosition,
                                accent = accent
                            )
                        }
                        CalibrationPhase.COUNTDOWN -> {
                            // Show countdown animation
                            CountdownDisplay(accent = accent)
                        }
                        CalibrationPhase.SAMPLING -> {
                            // Show sampling progress
                            SamplingProgressCard(
                                stepIndex = stepIndex,
                                uiState = uiState,
                                accent = accent
                            )
                        }
                    }
                }

                // Accel position tracker dots
                if (stepIndex == 2) {
                    AccelPositionTracker(
                        currentPosition = uiState.currentPosition,
                        completedPositions = uiState.completedPositions,
                        accent = accent
                    )
                }

                // Error message
                uiState.errorMessage?.let { message ->
                    CalibrationErrorPanel(message = message)
                }
            }

            // Bottom section: Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.88f))
                }
                Button(
                    onClick = onStartRecording,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = phase == CalibrationPhase.INTRO,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    if (phase == CalibrationPhase.SAMPLING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Sampling...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else if (phase == CalibrationPhase.COUNTDOWN) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Preparing...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (stepIndex == 2) "Record Position" else "Start Recording",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Phase-specific UI components
// ============================================================

@Composable
private fun StepProgressChips(currentStep: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..3) {
            val isDone = i < currentStep
            val isCurrent = i == currentStep
            val chipColor = when {
                isDone -> Color(0xFF10B981)
                isCurrent -> when (i) {
                    1 -> Color(0xFF38BDF8)
                    2 -> Color(0xFFF59E0B)
                    else -> Color(0xFF22C55E)
                }
                else -> Color.White.copy(alpha = 0.12f)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = chipColor.copy(alpha = if (isCurrent) 0.25f else if (isDone) 0.2f else 1f),
                border = BorderStroke(1.dp, chipColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = when {
                        isDone -> "✓"
                        else -> "$i"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (isDone) Color(0xFF10B981) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PhaseChip(phase: CalibrationPhase, accent: Color) {
    val label = when (phase) {
        CalibrationPhase.INTRO -> "Ready"
        CalibrationPhase.COUNTDOWN -> "Preparing"
        CalibrationPhase.SAMPLING -> "Sampling"
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (phase == CalibrationPhase.SAMPLING) {
                val infiniteTransition = rememberInfiniteTransition(label = "sampling_pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .graphicsLayer { alpha = pulseAlpha }
                )
            }
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IntroInstructionCard(
    stepIndex: Int,
    currentPosition: Int,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                when (stepIndex) {
                    0 -> "📱  Before you start"
                    1 -> "🧲  Before you start"
                    else -> "📐  Position ${currentPosition + 1} of 6"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                when (stepIndex) {
                    0 -> "Place your phone flat on a stable surface (table or desk). Keep it completely still during recording. The gyroscope will measure its idle drift."
                    1 -> "Hold the phone in your hand and slowly move it in a figure-8 pattern. Stay away from metal objects and magnets. This captures the magnetic field range."
                    else -> getAccelPositionDescription(currentPosition)
                },
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun CountdownDisplay(accent: Color) {
    var countdownValue by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        countdownValue = 3
        delay(467)
        countdownValue = 2
        delay(467)
        countdownValue = 1
        delay(467)
        countdownValue = 0
    }

    val scale by animateFloatAsState(
        targetValue = if (countdownValue > 0) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "countdown_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        if (countdownValue > 0) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f))
                    .border(3.dp, accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$countdownValue",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black
                )
            }
        } else {
            Text(
                "GO!",
                color = accent,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun SamplingProgressCard(
    stepIndex: Int,
    uiState: CalibrationUiState,
    accent: Color
) {
    val progress = if (uiState.totalSamplesNeeded > 0) {
        uiState.samplesCollected.toFloat() / uiState.totalSamplesNeeded
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(200),
        label = "progress_anim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Circular progress
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    strokeWidth = 6.dp
                )
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Sample count
            Text(
                "${uiState.samplesCollected} / ${uiState.totalSamplesNeeded} samples",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            // Linear progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.08f)
            )

            // Status label
            Text(
                when (stepIndex) {
                    0 -> "Keep the phone still on the flat surface..."
                    1 -> "Keep moving in a figure-8 pattern..."
                    else -> "Hold this position steady..."
                },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AccelPositionTracker(
    currentPosition: Int,
    completedPositions: List<Int>,
    accent: Color
) {
    val positionLabels = listOf("UP", "DOWN", "LEFT", "RIGHT", "TOP↓", "BOT↓")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            repeat(6) { i ->
                val isDone = i in completedPositions
                val isCurrent = i == currentPosition
                val dotColor = when {
                    isDone -> Color(0xFF10B981)
                    isCurrent -> accent
                    else -> Color.White.copy(alpha = 0.15f)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 14.dp else 10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .then(
                                if (isCurrent) {
                                    Modifier.border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                    )
                    Text(
                        positionLabels[i],
                        color = when {
                            isDone -> Color(0xFF10B981)
                            isCurrent -> Color.White
                            else -> Color.White.copy(alpha = 0.3f)
                        },
                        fontSize = 8.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ============================================================
// Completion Screen (Step 4)
// ============================================================

@Composable
private fun CompletionScreen(
    paddingValues: PaddingValues,
    calibrationData: CalibrationData?,
    alreadySaved: Boolean,
    onContinue: () -> Unit,
    onRecalibrate: () -> Unit
) {
    val checkScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkmark"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Celebration icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(checkScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF10B981).copy(alpha = 0.3f), Color(0xFF10B981).copy(alpha = 0.05f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Text(
            "Calibration Complete! 🎉",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        // Quality badge
        val quality = calibrationData?.quality ?: CalibrationQuality.UNKNOWN
        val qualityColor = when (quality) {
            CalibrationQuality.EXCELLENT -> Color(0xFF10B981)
            CalibrationQuality.GOOD -> Color(0xFF3B82F6)
            CalibrationQuality.FAIR -> Color(0xFFF59E0B)
            CalibrationQuality.POOR -> Color(0xFFEF4444)
            CalibrationQuality.UNKNOWN -> Color(0xFF64748B)
        }
        val qualityEmoji = when (quality) {
            CalibrationQuality.EXCELLENT -> "🌟"
            CalibrationQuality.GOOD -> "👍"
            CalibrationQuality.FAIR -> "⚠️"
            CalibrationQuality.POOR -> "❌"
            CalibrationQuality.UNKNOWN -> "❓"
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = qualityColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, qualityColor.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(qualityEmoji, fontSize = 24.sp)
                Text(
                    "Quality: ${quality.name}",
                    color = qualityColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // Sensor offsets card
        if (calibrationData != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📊 Sensor Offsets", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    SensorOffsetRow(
                        label = "Gyroscope Bias",
                        data = calibrationData.gyroBias,
                        color = Color(0xFF38BDF8)
                    )
                    SensorOffsetRow(
                        label = "Accelerometer",
                        data = calibrationData.accelOffset,
                        color = Color(0xFF22C55E)
                    )
                    SensorOffsetRow(
                        label = "Magnetometer",
                        data = calibrationData.magOffset,
                        color = Color(0xFFF59E0B)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        Text(
                            if (calibrationData.isCalibrated) "✅ Saved to device" else "⏳ Not saved",
                            color = if (calibrationData.isCalibrated) Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Description card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What was calibrated", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• Gyroscope bias removed for stationary drift correction", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text("• Accelerometer adjusted across six orientations", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text("• Magnetometer scale corrected via figure-8 sweep", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text("• Data is ready for precise cursor control", color = Color(0xFF93C5FD), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Continue to App", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        OutlinedButton(
            onClick = onRecalibrate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.width(8.dp))
            Text("Recalibrate", color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SensorOffsetRow(
    label: String,
    data: com.airmouse.domain.model.SensorCalibrationData,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        Text(
            "X:${"%.3f".format(data.offsetX)} Y:${"%.3f".format(data.offsetY)} Z:${"%.3f".format(data.offsetZ)}",
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
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

// ============================================================
// Helper functions
// ============================================================

private fun getStepInstruction(stepIndex: Int, currentPosition: Int, phase: CalibrationPhase): String {
    if (phase == CalibrationPhase.SAMPLING) {
        return when (stepIndex) {
            0 -> "Keep the phone completely still on the flat surface. Collecting gyroscope data..."
            1 -> "Continue the figure-8 motion slowly. Collecting magnetometer data..."
            else -> "Hold this position perfectly still. Collecting accelerometer data..."
        }
    }
    if (phase == CalibrationPhase.COUNTDOWN) {
        return "Get ready! Sampling starts in a moment..."
    }
    return when (stepIndex) {
        0 -> "Place your phone flat on a stable surface. Tap \"Start Recording\" when ready."
        1 -> "Move your phone slowly in a figure-8 pattern. Tap \"Start Recording\" when ready."
        else -> when (currentPosition) {
            0 -> "Screen facing UP — Place flat on a table. Tap \"Record Position\" when ready."
            1 -> "Screen facing DOWN — Flip the phone over on the table. Tap \"Record Position\" when ready."
            2 -> "Left edge DOWN — Hold in landscape with left side down. Tap \"Record Position\" when ready."
            3 -> "Right edge DOWN — Hold in landscape with right side down. Tap \"Record Position\" when ready."
            4 -> "Top edge DOWN — Hold upside-down in portrait. Tap \"Record Position\" when ready."
            5 -> "Bottom edge DOWN — Stand the phone upright normally. Tap \"Record Position\" when ready."
            else -> "All positions recorded!"
        }
    }
}

private fun getAccelPositionDescription(position: Int): String {
    return when (position) {
        0 -> "Place the phone with screen facing UP on a flat, stable surface like a table. Keep it perfectly still during recording."
        1 -> "Flip the phone over so the screen faces DOWN on the table. Keep it perfectly still during recording."
        2 -> "Hold the phone in landscape orientation with the LEFT edge pointing DOWN. Hold it stable and still."
        3 -> "Hold the phone in landscape orientation with the RIGHT edge pointing DOWN. Hold it stable and still."
        4 -> "Hold the phone in portrait with the TOP edge pointing DOWN (upside-down). Hold it stable."
        5 -> "Stand the phone upright with the BOTTOM edge DOWN (normal portrait). Hold it stable and still."
        else -> "Move to the next position as shown."
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