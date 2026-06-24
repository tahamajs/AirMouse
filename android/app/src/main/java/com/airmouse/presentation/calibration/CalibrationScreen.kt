
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
import com.airmouse.ui.components.DonutChart
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
    val phase = uiState.calibrationPhase

    var animationTriggered by remember { mutableStateOf(false) }
    var confettiActive by remember { mutableStateOf(false) }


    val currentStep = if (uiState.isComplete) 3 else (uiState.currentStep - 1).coerceAtLeast(0)
    val totalSteps = uiState.totalSteps
    val stepName = remember(uiState.currentStep) { calibrationStepName(uiState.currentStep) }

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
                    title = {
                        Column {
                            Text(if (uiState.isComplete) "Calibration complete" else "Assignment calibration", fontWeight = FontWeight.Bold)
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
                                }
                                viewModel.beginCurrentStep()
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
                            onCancel = { viewModel.resetCalibration() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationSamplingScreen(
    paddingValues: PaddingValues,
    currentStep: Int,
    totalSteps: Int,
    stepName: String,
    uiState: CalibrationUiState,
    onCancel: () -> Unit
) {
    val stepIndex = currentStep.coerceIn(0, 2)
    val accent = when (stepIndex) {
        0 -> Color(0xFF38BDF8)
        1 -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }
    val pulseTransition = rememberInfiniteTransition(label = "sampling_pulse")
    val outerScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sampling_outer_scale"
    )
    val ringRotation by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "sampling_ring_rotation"
    )
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
                        )
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text("Sampling") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = accent.copy(alpha = 0.20f),
                            labelColor = Color.White
                        )
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
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            scaleX = outerScale
                            scaleY = outerScale
                            rotationZ = ringRotation
                        }
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accent.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = sampleText,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Live progress", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        LinearProgressIndicator(
                            progress = { uiState.progress / 100f },
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
        else -> "Accelerometer six-position check"
    }
    val subtitle = when (stepIndex) {
        0 -> "We will first measure the idle gyro drift while the phone is steady."
        1 -> "Next, we capture magnetic field bias while the phone is held still."
        else -> "Finally, we record six orientations so the acceleration model is grounded."
    }
    val guidance = when (stepIndex) {
        0 -> "Put the phone on a flat surface. When you tap Start, a short animation plays before sampling begins."
        1 -> "Keep the phone still and clear of large metal objects while the preview runs."
        else -> "Follow the on-screen pose for each orientation; sampling starts only after the animation finishes."
    }
    val introPulse = rememberInfiniteTransition(label = "intro_pulse")
    val pulsingScale by introPulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "calibration_intro_scale"
    )
    val ringRotation by rememberInfiniteTransition(label = "intro_ring").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "intro_ring_rotation"
    )

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
                        )
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text(if (phase == CalibrationPhase.COUNTDOWN) "Preparing" else "Ready") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = accent.copy(alpha = 0.20f),
                            labelColor = Color.White
                        )
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
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .graphicsLayer {
                            scaleX = if (phase == CalibrationPhase.COUNTDOWN) pulsingScale else 1f
                            scaleY = if (phase == CalibrationPhase.COUNTDOWN) pulsingScale else 1f
                            rotationZ = ringRotation
                        }
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accent.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (stepIndex) {
                                0 -> "GYRO"
                                1 -> "MAG"
                                else -> "ACCEL"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                        Text(
                            text = when (stepIndex) {
                                0 -> "Keep still"
                                1 -> "Avoid metal"
                                else -> "Rotate slowly"
                            },
                            color = Color.White.copy(alpha = 0.80f),
                            fontSize = 12.sp
                        )
                    }
                }

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

@Composable
fun CalibrationGuideScreen(
    paddingValues: PaddingValues,
    currentStep: Int,
    totalSteps: Int,
    stepName: String,
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
            HeroCalibrationCard(uiState = uiState, currentStep = currentStep, stepName = stepName)
        }

        item {
            CalibrationStepTracker(currentStep = currentStep, totalSteps = totalSteps)
        }

        item {
            FitnessStepCard(step = currentStep, totalSteps = totalSteps, stepName = stepName, uiState = uiState)
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
            CalibrationGuidanceCard(isCalibrating = uiState.isCalibrating)
        }

        item {
            CalibrationSensorCards(activeStep = currentStep)
        }

        item {
            CalibrationFormulaSection()
        }

        item {
            LiveSensorDataCard(uiState = uiState)
        }

        item {
            PrimaryCalibrationActions(
                isCalibrating = uiState.isCalibrating,
                stepName = stepName,
                onStartCalibration = onStartCalibration,
                onCancel = { viewModel.resetCalibration() }
            )
        }

        item {
            SensorStatusDisplay(uiState = uiState, calibrationData = calibrationData)
        }
    }
}

@Composable
private fun HeroCalibrationCard(uiState: CalibrationUiState, currentStep: Int, stepName: String) {
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
                        "Step ${currentStep + 1}: $stepName. Do one stage at a time, like a guided fitness routine.",
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
            } else {
                Text(
                    "Tap start to begin the current stage. Each step unlocks the next one automatically.",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
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
private fun FitnessStepCard(step: Int, totalSteps: Int, stepName: String, uiState: CalibrationUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF6366F1).copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${step + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current workout", color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp)
                    Text(stepName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Text("Step ${step + 1}/$totalSteps", color = Color(0xFF93C5FD), fontWeight = FontWeight.SemiBold)
            }
            Text(
                uiState.stepInstruction.ifBlank { uiState.statusMessage },
                color = Color.White.copy(alpha = 0.82f),
                lineHeight = 20.sp
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
private fun CalibrationGuidanceCard(isCalibrating: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("How to calibrate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (isCalibrating) {
                WarningBanner(
                    text = "Don't move the phone while the app is sampling gyro bias. Keep it flat and still until the step finishes."
                )
            }
            Text(
                "Follow the assignment sequence and keep the phone steady whenever the step asks for a still sample.\n\n" +
                    "• Gyroscope: collect stationary samples, compute the bias, and subtract it so the idle reading trends to zero.\n" +
                    "• Accelerometer: repeat the six-position routine and fit each axis against gravity (9.81 m/s²).\n" +
                    "• Magnetometer: rotate the phone through many orientations, save the minimum and maximum of each axis, and derive offset/scale from them.\n" +
                    "• Fusion: combine the calibrated sensors with a filter such as Madgwick AHRS or another suitable fusion approach to reduce drift.\n" +
                    "• Output: use the filtered signal for cursor motion, click, and scroll so the pointer stays smooth and stable.",
                color = Color.White.copy(alpha = 0.82f),
                lineHeight = 20.sp
            )
            Text(
                "The live values below are a demo aid. They help verify the calibration results before you save or continue.",
                color = Color(0xFF93C5FD),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun WarningBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24))
            Text(text, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun CalibrationSensorCards(activeStep: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        SensorStepCard(
            stepNumber = 1,
            title = "Gyroscope bias",
            subtitle = "Keep the phone still and sample the angular velocity noise.",
            accent = Color(0xFF38BDF8),
            formula = "bias = average(raw samples)\ncorrected = raw - bias",
            isActive = activeStep == 0,
            details = listOf(
                "Goal: make the stationary output close to zero.",
                "Use the averaged bias to reduce drift before motion tracking."
            )
        )
        SensorStepCard(
            stepNumber = 2,
            title = "Accelerometer six-position calibration",
            subtitle = "Place the phone in each required orientation and compare against gravity.",
            accent = Color(0xFF22C55E),
            formula = "offset = (min + max) / 2\nscale = (max - min) / 2\ncorrected = (raw - offset) / scale",
            isActive = activeStep == 2,
            details = listOf(
                "Goal: estimate offset and scale for all axes.",
                "Gravity reference is approximately 9.81 m/s²."
            )
        )
        SensorStepCard(
            stepNumber = 3,
            title = "Magnetometer min/max calibration",
            subtitle = "Rotate the phone through all directions to sample the field envelope.",
            accent = Color(0xFFF59E0B),
            formula = "offset = (min + max) / 2\nscale = (max - min) / 2\ncorrected = (raw - offset) / scale",
            isActive = activeStep == 1,
            details = listOf(
                "Goal: compensate hard-iron bias and axis scaling.",
                "Rotate through as much of the sphere as possible."
            )
        )
    }
}

@Composable
private fun SensorStepCard(
    stepNumber: Int,
    title: String,
    subtitle: String,
    accent: Color,
    formula: String,
    isActive: Boolean,
    details: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.06f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accent.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$stepNumber", color = accent, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accent.copy(alpha = 0.18f)
                    ) {
                        Text(
                            "Active",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF020617).copy(alpha = 0.55f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
            ) {
                Text(
                    text = formula,
                    modifier = Modifier.padding(14.dp),
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            details.forEach { detail ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = accent, fontWeight = FontWeight.Bold)
                    Text(detail, color = Color.White.copy(alpha = 0.84f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PrimaryCalibrationActions(
    isCalibrating: Boolean,
    stepName: String,
    onStartCalibration: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (!isCalibrating) {
            Button(
                onClick = onStartCalibration,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Start $stepName", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(8.dp))
                Text("Cancel current step")
            }
        }
        Text(
            "The app will guide the rest automatically once this step is finished.",
            color = Color.White.copy(alpha = 0.66f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CalibrationFormulaSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Visible calibration formulas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            FormulaBlock(
                title = "Gyro",
                formula = "bias = average(raw)\ncorrected = raw - bias",
                accent = Color(0xFF38BDF8)
            )
            FormulaBlock(
                title = "Accel",
                formula = "offset = (min + max) / 2\nscale = (max - min) / 2\ncorrected = (raw - offset) / scale",
                accent = Color(0xFF22C55E)
            )
            FormulaBlock(
                title = "Magnetometer",
                formula = "offset = (min + max) / 2\nscale = (max - min) / 2\ncorrected = (raw - offset) / scale",
                accent = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun FormulaBlock(
    title: String,
    formula: String,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.26f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
        ) {
            Text(
                text = formula,
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
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
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { uiState.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF60A5FA),
                trackColor = Color.White.copy(alpha = 0.12f)
            )
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
