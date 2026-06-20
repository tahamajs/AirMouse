// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationScreen.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

// ==========================================
// DATA CLASSES
// ==========================================

data class CalibrationData(
    val gyroBias: SensorCalibrationData = SensorCalibrationData(),
    val accelOffset: SensorCalibrationData = SensorCalibrationData(),
    val magOffset: SensorCalibrationData = SensorCalibrationData()
)

data class SensorCalibrationData(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f
)

data class CalibrationQualityConfig(
    val color: Color,
    val emoji: String,
    val title: String,
    val description: String,
    val subtext: String,
    val score: String,
    val status: String
)

// ==========================================
// NAVIGATION ACTIONS (Local definition)
// ==========================================

interface NavigationActions {
    fun navigateBack()
    fun navigateToHome()
}

// ==========================================
// MAIN SCREEN
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
    // Use collectAsStateWithLifecycle for proper lifecycle handling
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()

    // Animation states
    var animationTriggered by remember { mutableStateOf(false) }
    var confettiActive by remember { mutableStateOf(false) }

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

    // Quality configuration - use safe access with fallback
    val quality = uiState.calibrationQuality.ifEmpty { "GOOD" }
    val qualityConfig = when (quality.uppercase()) {
        "EXCELLENT" -> CalibrationQualityConfig(
            color = Color(0xFF10B981),
            emoji = "🌟",
            title = "Excellent",
            description = "Perfect calibration! Your device is performing at its best.",
            subtext = "All sensors are optimally calibrated.",
            score = "95%",
            status = "✅ Ready"
        )
        "GOOD" -> CalibrationQualityConfig(
            color = Color(0xFF3B82F6),
            emoji = "👍",
            title = "Good",
            description = "Calibration successful with good accuracy.",
            subtext = "Device is ready for use.",
            score = "80%",
            status = "✅ Ready"
        )
        "FAIR" -> CalibrationQualityConfig(
            color = Color(0xFFF59E0B),
            emoji = "⚠️",
            title = "Fair",
            description = "Calibration complete with fair accuracy.",
            subtext = "Consider recalibrating for best results.",
            score = "60%",
            status = "⚠️ Review"
        )
        else -> CalibrationQualityConfig(
            color = Color(0xFF64748B),
            emoji = "❓",
            title = "Unknown",
            description = "Calibration completed but quality could not be determined.",
            subtext = "Please recalibrate for best results.",
            score = "50%",
            status = "❓ Unknown"
        )
    }

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
                    title = { Text("Calibration Complete", fontWeight = FontWeight.Bold) },
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
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
                            QualityMetric("Quality", quality.uppercase(), qualityConfig.color)
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
                        onClick = {
                            navigationActions?.navigateToHome()
                            onContinue()
                            onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Using Air Mouse", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onRecalibrate()
                                onSkip()
                            },
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

// ==========================================
// CONFETTI EFFECT
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