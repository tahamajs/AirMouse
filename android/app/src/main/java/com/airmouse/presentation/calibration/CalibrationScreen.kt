// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationScreen.kt
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.SensorCalibrationData
import com.airmouse.presentation.navigation.NavigationActions
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions? = null,
    onComplete: () -> Unit = {},
    quality: String = "GOOD",
    onContinue: () -> Unit = {},
    onRecalibrate: () -> Unit = {},
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
    val qualityConfig = when (quality.uppercase()) {
        "EXCELLENT" -> CalibrationQualityConfig(
            color = Color(0xFF10B981),
            emoji = "🌟",
            title = "Excellent",
            description = "Perfect calibration!",
            subtext = "Your device is performing at its best.",
            score = "95%",
            status = "✅ Ready"
        )
        "GOOD" -> CalibrationQualityConfig(
            color = Color(0xFF3B82F6),
            emoji = "👍",
            title = "Good",
            description = "Calibration successful.",
            subtext = "Device is calibrated with good accuracy.",
            score = "80%",
            status = "✅ Ready"
        )
        "FAIR" -> CalibrationQualityConfig(
            color = Color(0xFFF59E0B),
            emoji = "⚠️",
            title = "Fair",
            description = "Calibration complete.",
            subtext = "Consider recalibrating for best results.",
            score = "60%",
            status = "⚠️ Review"
        )
        else -> CalibrationQualityConfig(
            color = Color(0xFF64748B),
            emoji = "❓",
            title = "Unknown",
            description = "Calibration completed.",
            subtext = "Quality could not be determined.",
            score = "50%",
            status = "Unknown"
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
                text = qualityConfig.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = qualityConfig.color,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = qualityConfig.description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = qualityConfig.subtext,
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
                        QualityMetric(
                            label = "Quality",
                            value = quality.uppercase(),
                            color = qualityConfig.color
                        )
                        QualityMetric(
                            label = "Score",
                            value = qualityConfig.score,
                            color = qualityConfig.color
                        )
                        QualityMetric(
                            label = "Status",
                            value = qualityConfig.status,
                            color = qualityConfig.color
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
                        containerColor = qualityConfig.color
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
            value = formatBias(calibrationData.accelOffset),
            color = Color(0xFF3B82F6)
        )

        // Magnetometer
        DetailRow(
            label = "Magnetometer",
            value = formatBias(calibrationData.magOffset),
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

fun formatBias(data: SensorCalibrationData): String {
    return "(${data.offsetX.formatCalibrationValue()}, " +
            "${data.offsetY.formatCalibrationValue()}, " +
            "${data.offsetZ.formatCalibrationValue()})"
}

private fun Float.formatCalibrationValue(): String {
    return String.format(Locale.US, "%.2f", this)
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
                        this.alpha = alpha.coerceAtLeast(0.005f)
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
fun CalibrationScreenPreview() {
    CalibrationScreen(
        quality = "EXCELLENT",
        onContinue = {},
        onRecalibrate = {}
    )
}

private data class CalibrationQualityConfig(
    val color: Color,
    val emoji: String,
    val title: String,
    val description: String,
    val subtext: String,
    val score: String,
    val status: String
)
