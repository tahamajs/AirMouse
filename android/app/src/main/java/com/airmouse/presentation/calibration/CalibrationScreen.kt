// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationScreen.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.R
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay

// ==========================================
// NAVIGATION ACTIONS
// ==========================================
 

@Composable
fun ConfettiEffect() {
    val colors = listOf(
        Color(0xFF6366F1),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF3B82F6),
        Color(0xFFEC4899)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        for (i in 0..20) {
            val index = (i + progress * 20).toInt() % colors.size
            val x = (i * 37 + progress * 150) % 360
            val y = (i * 53 + progress * 200) % 400
            val size = 6 + (i % 4) * 2

            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = (y - 100).dp)
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(colors[index].copy(alpha = 0.6f))
            )
        }
    }
}

// ==========================================
// COMPOSABLE COMPONENTS
// ==========================================

@Composable
fun QualityMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun SensorStatusItem(
    label: String,
    data: Triple<Float, Float, Float>,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            "${"%.1f".format(data.first)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            "${"%.1f".format(data.second)}",
            fontSize = 10.sp,
            color = color.copy(alpha = 0.7f)
        )
        Text(
            "${"%.1f".format(data.third)}",
            fontSize = 10.sp,
            color = color.copy(alpha = 0.5f)
        )
    }
}

// ==========================================
// CALIBRATION SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions? = null,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onComplete: () -> Unit = {},
    onSkip: () -> Unit = {},
    onContinue: () -> Unit = onComplete,
    onRecalibrate: () -> Unit = onSkip,
    onShare: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()
    val calibrationStatus by viewModel.calibrationStatus.collectAsStateWithLifecycle()
    val calibrationData by viewModel.calibrationData.collectAsStateWithLifecycle()

    // Animation states for completion screen
    var animationTriggered by remember { mutableStateOf(false) }
    var confettiActive by remember { mutableStateOf(false) }

    // Current step for guide
    var currentGuideStep by remember { mutableStateOf(0) }
    val totalGuideSteps = 4

    val scale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_pop_animation"
    )

    val fadeIn by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "fade_in_animation"
    )

    // Auto-advance guide steps only when not calibrating
    LaunchedEffect(currentGuideStep, uiState.isCalibrating) {
        if (!uiState.isCalibrating && currentGuideStep < totalGuideSteps - 1) {
            delay(3000)
            currentGuideStep = (currentGuideStep + 1) % totalGuideSteps
        }
    }

    val qualityText = uiState.calibrationQuality.ifEmpty { "GOOD" }
    val qualityConfig = CalibrationQualityConfig.fromQualityString(qualityText)

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            animationTriggered = true
            delay(500)
            confettiActive = true
            delay(3000)
            confettiActive = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
    ) {
        if (confettiActive) {
            ConfettiEffect()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (uiState.isComplete) "Calibration Complete" else "Calibration Guide",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navigationActions?.navigateBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (!uiState.isComplete && !uiState.isCalibrating) {
                            TextButton(onClick = { viewModel.skipCalibration() }) {
                                Text("Skip", color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            if (uiState.isComplete) {
                // Completion Screen
                CompletionScreen(
                    paddingValues = paddingValues,
                    qualityConfig = qualityConfig,
                    scale = scale,
                    fadeIn = fadeIn,
                    animationTriggered = animationTriggered,
                    onContinue = {
                        navigationActions?.navigateToHome()
                        onContinue()
                        onComplete()
                    },
                    onRecalibrate = {
                        onRecalibrate()
                        onSkip()
                    },
                    onShare = onShare
                )
            } else {
                // Calibration Guide Screen
                CalibrationGuideScreen(
                    paddingValues = paddingValues,
                    currentStep = currentGuideStep,
                    totalSteps = totalGuideSteps,
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

// ==========================================
// CALIBRATION GUIDE SCREEN
// ==========================================

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
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentStep) 14.dp else 10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index == currentStep -> Color(0xFF6366F1)
                                    index < currentStep -> Color(0xFF10B981)
                                    else -> Color(0xFF1E293B)
                                }
                            )
                            .animateContentSize()
                    )
                    if (index < totalSteps - 1) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    if (index < currentStep) Color(0xFF10B981) else Color(0xFF1E293B)
                                )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Animated instruction card
            AnimatedInstructionCard(
                step = currentStep,
                totalSteps = totalSteps,
                uiState = uiState
            )
        }

        item {
            // Sensor status display
            SensorStatusDisplay(
                uiState = uiState,
                calibrationData = calibrationData
            )
        }

        item {
            // Calibration status
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
            // Action buttons
            if (!uiState.isCalibrating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStartCalibration,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Calibration", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { navigationActions?.navigateBack() },
                        modifier = Modifier
                            .weight(0.5f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }
                }
            } else {
                // Show cancel button when calibrating
                OutlinedButton(
                    onClick = { viewModel.resetCalibration() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFEF4444).copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Calibration", fontSize = 14.sp)
                }
            }
        }
    }
}

// ==========================================
// ANIMATED INSTRUCTION CARD
// ==========================================

@Composable
fun AnimatedInstructionCard(
    step: Int,
    totalSteps: Int,
    uiState: CalibrationUiState
) {
    val instructions = listOf(
        Triple("📱", "Place Device Flat", "Keep your device on a flat, stationary surface for 5 seconds."),
        Triple("🔄", "Move in Figure-8", "Rotate your device in all directions for 10 seconds."),
        Triple("📐", "Hold Each Position", "Rotate to each position and hold steady for 3 seconds."),
        Triple("✅", "Calibration Complete!", "Your device is now calibrated and ready to use!")
    )

    val (emoji, title, description) = instructions.getOrElse(step % instructions.size) {
        instructions.first()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "instruction_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "instruction_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "instruction_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .rotate(rotation)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step indicator
            Text(
                "Step ${step + 1} of $totalSteps",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

// ==========================================
// SENSOR STATUS DISPLAY
// ==========================================

@Composable
fun SensorStatusDisplay(
    uiState: CalibrationUiState,
    calibrationData: CalibrationData?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "📊 Sensor Status",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorStatusItem("Gyroscope", uiState.gyroData, Color(0xFFEF4444))
                SensorStatusItem("Accelerometer", uiState.accelData, Color(0xFF3B82F6))
                SensorStatusItem("Magnetometer", uiState.magData, Color(0xFF10B981))
            }

            // Show calibration data if available
            if (calibrationData != null && calibrationData.isCalibrated) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quality: ${calibrationData.quality.name}",
                        fontSize = 12.sp,
                        color = when (calibrationData.quality) {
                            CalibrationQuality.EXCELLENT -> Color(0xFF10B981)
                            CalibrationQuality.GOOD -> Color(0xFF3B82F6)
                            CalibrationQuality.FAIR -> Color(0xFFF59E0B)
                            CalibrationQuality.POOR -> Color(0xFFEF4444)
                            else -> Color.White.copy(alpha = 0.5f)
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (calibrationData.isCalibrated) "✅ Calibrated" else "⏳ Not Calibrated",
                        fontSize = 12.sp,
                        color = if (calibrationData.isCalibrated) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}

// ==========================================
// CALIBRATION STATUS CARD
// ==========================================

@Composable
fun CalibrationStatusCard(
    isCalibrating: Boolean,
    progress: Int,
    statusMessage: String,
    samplesCollected: Int,
    totalSamplesNeeded: Int,
    calibrationData: CalibrationData?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    statusMessage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                if (isCalibrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF6366F1)
                    )
                } else if (calibrationData != null && calibrationData.isCalibrated) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Calibrated",
                        tint = Color(0xFF10B981)
                    )
                }
            }

            if (isCalibrating) {
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF6366F1),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Text(
                    "${progress}% complete • $samplesCollected / $totalSamplesNeeded samples",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ==========================================
// COMPLETION SCREEN
// ==========================================

@Composable
fun CompletionScreen(
    paddingValues: PaddingValues,
    qualityConfig: CalibrationQualityConfig,
    scale: Float,
    fadeIn: Float,
    animationTriggered: Boolean,
    onContinue: () -> Unit,
    onRecalibrate: () -> Unit,
    onShare: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp)
            .graphicsLayer {
                alpha = fadeIn
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success animation circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            qualityConfig.color.copy(alpha = 0.3f),
                            qualityConfig.color.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = qualityConfig.emoji,
                    fontSize = 48.sp,
                    modifier = Modifier.scale(if (animationTriggered) 1f else 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Calibration Complete!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = qualityConfig.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = qualityConfig.color,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = qualityConfig.description,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quality metrics card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                qualityConfig.color.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QualityMetric("Quality", qualityConfig.title, qualityConfig.color)
                    QualityMetric("Score", qualityConfig.score, qualityConfig.color)
                    QualityMetric("Status", qualityConfig.status, qualityConfig.color)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Using Air Mouse", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRecalibrate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recalibrate", fontSize = 14.sp)
                }

                if (onShare != null) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Air Mouse v3.0.0",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.3f)
        )
    }
}

// ==========================================
// PREVIEW
// ==========================================

@Preview(showBackground = true)
@Composable
fun CalibrationScreenPreview() {
    MaterialTheme {
        CalibrationScreen(
            onContinue = {},
            onRecalibrate = {}
        )
    }
}

@Preview(showBackground = true, name = "Completion Screen")
@Composable
fun CompletionScreenPreview() {
    MaterialTheme {
        val qualityConfig = CalibrationQualityConfig(
            color = Color(0xFF10B981),
            emoji = "🌟",
            title = "Excellent",
            description = "Perfect calibration! Your device is performing at its best.",
            subtext = "All sensors are optimally calibrated.",
            score = "95%",
            status = "✅ Ready"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            CompletionScreen(
                paddingValues = PaddingValues(16.dp),
                qualityConfig = qualityConfig,
                scale = 1f,
                fadeIn = 1f,
                animationTriggered = true,
                onContinue = {},
                onRecalibrate = {},
                onShare = null
            )
        }
    }
}
