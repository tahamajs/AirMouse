// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationComponents.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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
            alpha = alpha
        )
    }
}

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
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
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