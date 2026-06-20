// app/src/main/java/com/airmouse/presentation/calibration/CalibrationComponents.kt
package com.airmouse.presentation.calibration

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus

// ==========================================
// GLASS CARD
// ==========================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

// ==========================================
// PROGRESS INDICATOR
// ==========================================

@Composable
fun CalibrationProgressIndicator(
    progress: Int,
    totalSteps: Int = 4,
    currentStep: Int = 1,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            CalibrationStepIndicator(
                step = i,
                isActive = i == currentStep,
                isComplete = i < currentStep,
                modifier = Modifier.weight(1f)
            )
            if (i < totalSteps) {
                CalibrationStepConnector(
                    isComplete = i < currentStep,
                    modifier = Modifier.weight(0.5f)
                )
            }
        }
    }
}

@Composable
fun CalibrationStepIndicator(
    step: Int,
    isActive: Boolean,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isComplete -> Color(0xFF10B981)
                        isActive -> Color(0xFF6366F1)
                        else -> Color(0xFF1E293B)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isComplete) "✓" else step.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Step $step",
            fontSize = 10.sp,
            color = if (isActive) Color(0xFF6366F1) else Color(0xFF64748B)
        )
    }
}

@Composable
fun CalibrationStepConnector(
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .background(
                if (isComplete) Color(0xFF10B981) else Color(0xFF1E293B)
            )
    )
}

// ==========================================
// STATUS CHIP - Enhanced with Model Support
// ==========================================

@Composable
fun CalibrationStatusChip(
    status: CalibrationStatus,
    modifier: Modifier = Modifier
) {
    val (statusText, isError, isComplete, isSynced) = when (status) {
        CalibrationStatus.NOT_STARTED -> "Not Started" to false to false to false
        CalibrationStatus.IN_PROGRESS -> "In Progress" to false to false to false
        CalibrationStatus.GYRO_COMPLETE -> "Gyro Complete" to false to false to false
        CalibrationStatus.MAG_COMPLETE -> "Mag Complete" to false to false to false
        CalibrationStatus.ACCEL_COMPLETE -> "Accel Complete" to false to false to false
        CalibrationStatus.COMPLETED -> "✓ Complete" to false to true to false
        CalibrationStatus.FAILED -> "❌ Failed" to true to false to false
        CalibrationStatus.SKIPPED -> "⏭️ Skipped" to false to true to false
        CalibrationStatus.IDLE -> "Idle" to false to false to false
    }

    CalibrationStatusChipText(
        status = statusText,
        isError = isError,
        isComplete = isComplete,
        isSynced = isSynced,
        modifier = modifier
    )
}

@Composable
fun CalibrationStatusChipText(
    status: String,
    isError: Boolean = false,
    isComplete: Boolean = false,
    isSynced: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = when {
        isError -> Color(0xFFEF4444)
        isSynced -> Color(0xFF8B5CF6)
        isComplete -> Color(0xFF10B981)
        status.contains("Progress") || status.contains("In Progress") -> Color(0xFFF59E0B)
        else -> Color(0xFF6366F1)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
                    .graphicsLayer {
                        if (status.contains("Progress") || status.contains("In Progress")) {
                            val pulse by animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )
                            scaleX = 1f + pulse * 0.5f
                            scaleY = 1f + pulse * 0.5f
                        }
                    }
            )
            Text(
                text = status,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// QUALITY INDICATOR - Enhanced with Model
// ==========================================

@Composable
fun CalibrationQualityIndicator(
    quality: CalibrationQuality,
    modifier: Modifier = Modifier,
    showEmoji: Boolean = true
) {
    val (color, emoji, label) = when (quality) {
        CalibrationQuality.EXCELLENT -> Color(0xFF10B981) to "🌟" to "Excellent"
        CalibrationQuality.GOOD -> Color(0xFF3B82F6) to "👍" to "Good"
        CalibrationQuality.FAIR -> Color(0xFFF59E0B) to "⚠️" to "Fair"
        CalibrationQuality.POOR -> Color(0xFFEF4444) to "❌" to "Poor"
        CalibrationQuality.UNKNOWN -> Color(0xFF64748B) to "❓" to "Unknown"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showEmoji) {
            Text(emoji, fontSize = 16.sp)
        }
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// ==========================================
// ANIMATED CHECKMARK
// ==========================================

@Composable
fun AnimatedCheckmark(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 64
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkmark_animation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "checkmark_alpha"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "✓",
            fontSize = (size * 0.5).sp,
            color = Color(0xFF10B981),
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        )
    }
}

// ==========================================
// LOADING SPINNER
// ==========================================

@Composable
fun AnimatedLoadingSpinner(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_rotation"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .rotate(if (isActive) rotation else 0f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size((size * 0.7).dp),
            strokeWidth = 3.dp,
            color = Color(0xFF6366F1)
        )
    }
}

// ==========================================
// INSTRUCTION CARD
// ==========================================

@Composable
fun CalibrationInstructionCard(
    title: String,
    instruction: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: String = "📱"
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                icon,
                fontSize = 32.sp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF6366F1)
                )
                Text(
                    instruction,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

// ==========================================
// PULSE ANIMATION
// ==========================================

@Composable
fun PulseAnimation(
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
    size: Int = 16,
    color: Color = Color(0xFF6366F1)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .scale(if (isActive) scale else 1f)
            .clip(CircleShape)
            .background(
                if (isActive) color else Color(0xFF64748B)
            )
            .graphicsLayer {
                this.alpha = if (isActive) alpha else 1f
            }
    )
}

// ==========================================
// SENSOR VISUALIZER
// ==========================================

@Composable
fun CalibrationSensorVisualizer(
    roll: Float = 0f,
    pitch: Float = 0f,
    yaw: Float = 0f,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
) {
    val rotationXVal = pitch * 0.5f
    val rotationYVal = roll * 0.5f

    Box(
        modifier = modifier
            .size(120.dp)
            .graphicsLayer {
                rotationX = rotationXVal
                rotationY = rotationYVal
                shadowElevation = 8f
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF020617))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.minDimension / 3

                drawCircle(
                    color = Color(0xFF334155),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1f)
                )
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(centerX - radius, centerY),
                    end = Offset(centerX + radius, centerY),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(centerX, centerY - radius),
                    end = Offset(centerX, centerY + radius),
                    strokeWidth = 1f
                )
            }

            val dotX = (roll / 45f).coerceIn(-1f, 1f) * 20f
            val dotY = (pitch / 45f).coerceIn(-1f, 1f) * 20f
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1))
                    .offset(x = dotX.dp, y = dotY.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )

            if (showLabels) {
                Text(
                    "Air Mouse",
                    fontSize = 8.sp,
                    color = Color(0xFF6366F1).copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ==========================================
// STATS ROW
// ==========================================

@Composable
fun CalibrationStatsRow(
    gyroX: Float,
    gyroY: Float,
    gyroZ: Float,
    accelX: Float,
    accelY: Float,
    accelZ: Float,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CalibrationStatItem(
            label = if (showLabels) "Gyro" else "",
            x = gyroX,
            y = gyroY,
            z = gyroZ,
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
        CalibrationStatItem(
            label = if (showLabels) "Accel" else "",
            x = accelX,
            y = accelY,
            z = accelZ,
            color = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CalibrationStatItem(
    label: String,
    x: Float,
    y: Float,
    z: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (label.isNotEmpty()) {
                Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
            }
            Text(
                "%.2f".format(x),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                "%.2f".format(y),
                fontSize = 10.sp,
                color = color.copy(alpha = 0.7f)
            )
            Text(
                "%.2f".format(z),
                fontSize = 10.sp,
                color = color.copy(alpha = 0.5f)
            )
        }
    }
}

// ==========================================
// ACTION BUTTON
// ==========================================

@Composable
fun CalibrationActionButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFF6366F1) else Color(0xFF1E293B),
            disabledContainerColor = Color(0xFF1E293B)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==========================================
// STEPS GUIDE
// ==========================================

@Composable
fun CalibrationStepsGuide(
    steps: List<Pair<String, String>>,
    currentStep: Int,
    modifier: Modifier = Modifier,
    showIcons: Boolean = true
) {
    val icons = listOf("📱", "🔄", "📐", "✅")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEachIndexed { index, (title, description) ->
            val isActive = index == currentStep
            val isDone = index < currentStep

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isActive) Color(0xFF6366F1).copy(alpha = 0.1f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(if (isActive) 12.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> Color(0xFF10B981)
                                isActive -> Color(0xFF6366F1)
                                else -> Color(0xFF1E293B)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Text("✓", color = Color.White, fontSize = 12.sp)
                    } else if (showIcons && index < icons.size) {
                        Text(icons[index], fontSize = 14.sp)
                    } else {
                        Text("${index + 1}", color = Color.White, fontSize = 12.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        color = if (isActive) Color(0xFF6366F1) else Color.White
                    )
                    if (isActive) {
                        Text(
                            description,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CALIBRATION SUMMARY CARD - Enhanced with Repository Data
// ==========================================

@Composable
fun CalibrationSummaryCard(
    title: String = "Calibration Summary",
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    quality: CalibrationQuality = CalibrationQuality.UNKNOWN,
    status: CalibrationStatus = CalibrationStatus.NOT_STARTED
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (quality != CalibrationQuality.UNKNOWN) {
                    CalibrationQualityIndicator(quality = quality, modifier = Modifier)
                }
            }

            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            if (status != CalibrationStatus.NOT_STARTED) {
                Spacer(modifier = Modifier.height(4.dp))
                CalibrationStatusChip(status = status, modifier = Modifier.align(Alignment.End))
            }
        }
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
// CALIBRATION STATUS CARD
// ==========================================

@Composable
fun CalibrationStatusCard(
    calibrationData: com.airmouse.domain.model.CalibrationData?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "📊 Calibration Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (calibrationData == null) {
                Text(
                    "No calibration data available",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                return@Card
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Gyroscope Bias",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    "X: %.2f  Y: %.2f  Z: %.2f".format(
                        calibrationData.gyroBias.offsetX,
                        calibrationData.gyroBias.offsetY,
                        calibrationData.gyroBias.offsetZ
                    ),
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Accelerometer Offset",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    "X: %.2f  Y: %.2f  Z: %.2f".format(
                        calibrationData.accelOffset.offsetX,
                        calibrationData.accelOffset.offsetY,
                        calibrationData.accelOffset.offsetZ
                    ),
                    fontSize = 12.sp,
                    color = Color(0xFF3B82F6),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Magnetometer Offset",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    "X: %.2f  Y: %.2f  Z: %.2f".format(
                        calibrationData.magOffset.offsetX,
                        calibrationData.magOffset.offsetY,
                        calibrationData.magOffset.offsetZ
                    ),
                    fontSize = 12.sp,
                    color = Color(0xFF8B5CF6),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalibrationQualityIndicator(quality = calibrationData.quality)
                Text(
                    if (calibrationData.isCalibrated) "✅ Calibrated" else "⏳ Not Calibrated",
                    fontSize = 12.sp,
                    color = if (calibrationData.isCalibrated) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationStatusChip() {
    MaterialTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalibrationStatusChip(status = CalibrationStatus.IN_PROGRESS)
            CalibrationStatusChip(status = CalibrationStatus.COMPLETED)
            CalibrationStatusChip(status = CalibrationStatus.FAILED)
            CalibrationStatusChip(status = CalibrationStatus.SKIPPED)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationQualityIndicator() {
    MaterialTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalibrationQualityIndicator(quality = CalibrationQuality.EXCELLENT)
            CalibrationQualityIndicator(quality = CalibrationQuality.GOOD)
            CalibrationQualityIndicator(quality = CalibrationQuality.FAIR)
            CalibrationQualityIndicator(quality = CalibrationQuality.POOR)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationSummaryCard() {
    MaterialTheme {
        CalibrationSummaryCard(
            items = listOf(
                "Status" to "✅ Complete",
                "Quality" to "🌟 Excellent",
                "Date" to "2024-01-15 14:30:00",
                "Version" to "v2"
            ),
            quality = CalibrationQuality.EXCELLENT,
            status = CalibrationStatus.COMPLETED,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationStatusCard() {
    MaterialTheme {
        val data = com.airmouse.domain.model.CalibrationData(
            gyroBias = com.airmouse.domain.model.SensorCalibrationData(
                offsetX = 0.02f,
                offsetY = -0.01f,
                offsetZ = 0.03f
            ),
            accelOffset = com.airmouse.domain.model.SensorCalibrationData(
                offsetX = 0.1f,
                offsetY = -0.05f,
                offsetZ = 9.81f
            ),
            magOffset = com.airmouse.domain.model.SensorCalibrationData(
                offsetX = -0.3f,
                offsetY = 0.2f,
                offsetZ = 0.1f
            ),
            isCalibrated = true,
            quality = CalibrationQuality.GOOD,
            timestamp = System.currentTimeMillis()
        )
        CalibrationStatusCard(
            calibrationData = data,
            modifier = Modifier.padding(16.dp)
        )
    }
}