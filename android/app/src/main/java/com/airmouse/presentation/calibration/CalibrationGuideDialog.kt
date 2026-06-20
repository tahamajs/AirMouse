// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationComponents.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
// STATUS CHIP
// ==========================================

@Composable
fun CalibrationStatusChip(
    status: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = when {
        isError -> Color(0xFFEF4444)
        status.contains("Complete") || status.contains("✓") -> Color(0xFF10B981)
        status.contains("Calibrating") -> Color(0xFFF59E0B)
        else -> Color(0xFF6366F1)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// QUALITY INDICATOR
// ==========================================

@Composable
fun CalibrationQualityIndicator(
    quality: String,
    modifier: Modifier = Modifier
) {
    val (color, emoji) = when (quality.uppercase()) {
        "EXCELLENT" -> Color(0xFF10B981) to "🌟"
        "GOOD" -> Color(0xFF3B82F6) to "👍"
        "FAIR" -> Color(0xFFF59E0B) to "⚠️"
        "POOR" -> Color(0xFFEF4444) to "❌"
        else -> Color(0xFF64748B) to "❓"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(
            quality,
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
    modifier: Modifier = Modifier
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
            .size(64.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "✓",
            fontSize = 32.sp,
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
    modifier: Modifier = Modifier
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
            .size(48.dp)
            .rotate(if (isActive) rotation else 0f)
            .clip(CircleShape)
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

// ==========================================
// PULSE ANIMATION
// ==========================================

@Composable
fun PulseAnimation(
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .size(16.dp)
            .scale(if (isActive) scale else 1f)
            .clip(CircleShape)
            .background(
                if (isActive) Color(0xFF6366F1) else Color(0xFF64748B)
            )
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
    modifier: Modifier = Modifier
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
            val dotX = (roll / 45f).coerceIn(-1f, 1f) * 20f
            val dotY = (pitch / 45f).coerceIn(-1f, 1f) * 20f
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1))
                    .offset(x = dotX.dp, y = dotY.dp)
            )
            Text("Air Mouse", fontSize = 10.sp, color = Color(0xFF6366F1))
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CalibrationStatItem(
            label = "Gyro",
            x = gyroX,
            y = gyroY,
            z = gyroZ,
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
        CalibrationStatItem(
            label = "Accel",
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
        modifier = modifier
            .fillMaxWidth(),
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
            Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
            Text(
                "%.1f".format(x),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                "%.1f".format(y),
                fontSize = 10.sp,
                color = color.copy(alpha = 0.7f)
            )
            Text(
                "%.1f".format(z),
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6366F1),
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
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier.fillMaxWidth()
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
// PREVIEWS
// ==========================================

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationProgressIndicator() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CalibrationProgressIndicator(progress = 50, currentStep = 2)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationStatusChip() {
    MaterialTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalibrationStatusChip("Calibrating")
            CalibrationStatusChip("Complete")
            CalibrationStatusChip("Error", isError = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationQualityIndicator() {
    MaterialTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalibrationQualityIndicator("EXCELLENT")
            CalibrationQualityIndicator("GOOD")
            CalibrationQualityIndicator("FAIR")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAnimatedCheckmark() {
    MaterialTheme {
        AnimatedCheckmark(isVisible = true, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAnimatedLoadingSpinner() {
    MaterialTheme {
        AnimatedLoadingSpinner(isActive = true, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationInstructionCard() {
    MaterialTheme {
        CalibrationInstructionCard(
            title = "Place Device Flat",
            instruction = "Place your device on a flat surface",
            description = "Ensure the device is stationary during calibration.",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPulseAnimation() {
    MaterialTheme {
        PulseAnimation(isActive = true, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationSensorVisualizer() {
    MaterialTheme {
        CalibrationSensorVisualizer(roll = 10f, pitch = 5f, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationStatsRow() {
    MaterialTheme {
        CalibrationStatsRow(
            gyroX = 0.1f, gyroY = -0.2f, gyroZ = 0.05f,
            accelX = 9.8f, accelY = -0.1f, accelZ = 0.2f,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationActionButton() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalibrationActionButton(text = "Start Calibration", onClick = {})
            CalibrationActionButton(text = "Calibrating...", onClick = {}, isLoading = true)
            CalibrationActionButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationStepsGuide() {
    MaterialTheme {
        val steps = listOf(
            "Place Device Flat" to "Keep device stationary",
            "Move in Figure-8" to "Rotate device in all directions",
            "Hold Still" to "Wait for calibration to complete"
        )
        CalibrationStepsGuide(
            steps = steps,
            currentStep = 1,
            modifier = Modifier.padding(16.dp)
        )
    }
}