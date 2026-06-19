// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationResultScreen.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.delay

@Composable
fun CalibrationResultScreen(
    quality: String,
    onContinue: () -> Unit,
    onRecalibrate: () -> Unit,
    onShare: (() -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null,
    calibrationData: CalibrationData? = null,
    stats: Map<String, Any>? = null
) {
    // Animation states
    var animationTriggered by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var confettiActive by remember { mutableStateOf(false) }

    // Animations
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

    val translateY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "translate_y_animation"
    )

    // Quality colors and icons
    val (qualityColor, qualityEmoji, qualityTitle, qualityDescription, qualitySubtext) = when (quality.uppercase()) {
        "EXCELLENT" -> Pair(
            Color(0xFF10B981),
            "🌟",
            "Excellent",
            "Perfect calibration!",
            "Your device is performing at its best."
        )
        "GOOD" -> Pair(
            Color(0xFF3B82F6),
            "👍",
            "Good",
            "Calibration successful.",
            "Device is calibrated with good accuracy."
        )
        "FAIR" -> Pair(
            Color(0xFFF59E0B),
            "⚠️",
            "Fair",
            "Calibration complete.",
            "Consider recalibrating for best results."
        )
        else -> Pair(
            Color(0xFF64748B),
            "❓",
            "Unknown",
            "Calibration completed.",
            "Quality could not be determined."
        )
    }

    LaunchedEffect(Unit) {
        animationTriggered = true
        delay(500)
        confettiActive = true
        delay(3000)
        confettiActive = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B)
                    )
                )
            )
    ) {
        // Animated background particles
        AnimatedBackgroundParticles()

        // Confetti effect
        if (confettiActive) {
            ConfettiEffect()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    alpha = fadeIn
                    translationY = translateY
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
                                qualityColor.copy(alpha = 0.3f),
                                qualityColor.copy(alpha = 0.1f)
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
                        text = qualityEmoji,
                        fontSize = 48.sp,
                        modifier = Modifier.scale(
                            if (animationTriggered) 1f else 0.5f
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Calibration Complete!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = qualityTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = qualityColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = qualityDescription,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = qualitySubtext,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quality metrics card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    qualityColor.copy(alpha = 0.3f)
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
                        QualityMetric(
                            label = "Quality",
                            value = quality.uppercase(),
                            color = qualityColor
                        )
                        QualityMetric(
                            label = "Score",
                            value = when (quality.uppercase()) {
                                "EXCELLENT" -> "95%"
                                "GOOD" -> "80%"
                                "FAIR" -> "60%"
                                else -> "50%"
                            },
                            color = qualityColor
                        )
                        QualityMetric(
                            label = "Status",
                            value = if (quality.uppercase() in listOf("EXCELLENT", "GOOD")) "✅ Ready" else "⚠️ Review",
                            color = qualityColor
                        )
                    }

                    if (calibrationData != null && showDetails) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        CalibrationDetails(
                            calibrationData = calibrationData,
                            stats = stats
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show details toggle
            if (calibrationData != null) {
                TextButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (showDetails) "Hide Details" else "Show Details",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
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
                        containerColor = qualityColor
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Start Using Air Mouse",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Recalibrate button
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
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recalibrate", fontSize = 14.sp)
                    }

                    // Share button (if provided)
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
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Version info
            Text(
                text = "Air Mouse v3.0.0",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
fun QualityMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CalibrationDetails(
    calibrationData: CalibrationData,
    stats: Map<String, Any>?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Calibration Details",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.8f)
        )

        // Gyroscope
        DetailRow(
            label = "Gyroscope",
            value = formatBias(calibrationData.gyroBias),
            color = Color(0xFFEF4444)
        )

        // Accelerometer
        DetailRow(
            label = "Accelerometer",
            value = formatBias(calibrationData.accelBias),
            color = Color(0xFF3B82F6)
        )

        // Magnetometer
        DetailRow(
            label = "Magnetometer",
            value = formatBias(calibrationData.magBias),
            color = Color(0xFF10B981)
        )

        // Additional stats if available
        stats?.let {
            if (it.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Additional Stats",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                stats.forEach { (key, value) ->
                    Text(
                        text = "$key: $value",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun formatBias(bias: Triple<Float, Float, Float>): String {
    return "(${bias.first.formatCalibrationValue()}, " +
            "${bias.second.formatCalibrationValue()}, " +
            "${bias.third.formatCalibrationValue()})"
}

@Composable
fun AnimatedBackgroundParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing)
        ),
        label = "particle_rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        for (i in 0..5) {
            val delay = i * 2000L
            val size = 50 + i * 20
            val alpha = 0.03f - i * 0.003f

            Box(
                modifier = Modifier
                    .size(size.dp)
                    .graphicsLayer {
                        rotationZ = rotation * (1f + i * 0.1f)
                        alpha = alpha.coerceAtLeast(0.005f)
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = alpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
                    .align(Alignment.Center)
            )
        }
    }
}

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
                    .offset(
                        x = x.dp,
                        y = (y - 100).dp
                    )
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(colors[index].copy(alpha = 0.6f))
            )
        }
    }
}

// Preview for development
@Preview(showBackground = true)
@Composable
fun CalibrationResultScreenPreview() {
    CalibrationResultScreen(
        quality = "EXCELLENT",
        onContinue = {},
        onRecalibrate = {}
    )
}