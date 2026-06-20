// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationGuideDialog.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airmouse.R
import kotlinx.coroutines.delay

/**
 * A comprehensive calibration guide dialog with animated instructions.
 *
 * @param step The calibration step (0 = gyroscope, 1 = magnetometer, 2 = accelerometer, 3+ = complete)
 * @param onDismiss Callback when the user dismisses the dialog
 * @param modifier Optional modifier
 */
@Composable
fun CalibrationGuideDialog(
    step: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State for the animated instruction frames
    var currentImageIndex by remember { mutableIntStateOf(0) }
    val totalImages = when (step) {
        0 -> 3
        1 -> 4
        2 -> 6
        else -> 1
    }

    // Animate through instruction frames every 2 seconds
    LaunchedEffect(step) {
        while (true) {
            delay(2000)
            currentImageIndex = (currentImageIndex + 1) % totalImages
        }
    }

    // Dialog content
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box {
                // Close button at top-right corner
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated instruction icon
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (step) {
                            0 -> GyroInstructionAnimation(currentImageIndex)
                            1 -> MagnetometerInstructionAnimation(currentImageIndex)
                            2 -> AccelerometerInstructionAnimation(currentImageIndex)
                            else -> CompletedInstructionAnimation()
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        text = when (step) {
                            0 -> "🧭 Gyroscope Calibration"
                            1 -> "🧲 Magnetometer Calibration"
                            2 -> "📐 Accelerometer Calibration"
                            else -> "✅ Calibration Complete!"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Main instruction
                    Text(
                        text = when (step) {
                            0 -> "Place your device on a flat, stationary surface"
                            1 -> "Move your device in a smooth figure‑8 pattern"
                            2 -> "Rotate your device to each shown orientation"
                            else -> "Your device is now fully calibrated!"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Detailed instruction
                    Text(
                        text = when (step) {
                            0 -> "Keep the device perfectly still for 5 seconds"
                            1 -> "Ensure you cover all axes of movement"
                            2 -> "Hold each position steady for 3 seconds"
                            else -> "You can now enjoy precise cursor control"
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Step progress indicator (dots)
                    if (step < 3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            repeat(totalImages) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (index == currentImageIndex % totalImages)
                                                Color(0xFF6366F1)
                                            else
                                                Color(0xFF334155),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        )
                    ) {
                        Text(
                            if (step >= 3) "Get Started" else "Got it",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// ANIMATION COMPONENTS
// ==========================================

@Composable
fun GyroInstructionAnimation(frame: Int) {
    val rotation = when (frame % 3) {
        0 -> 0f
        1 -> 5f
        else -> -5f
    }

    // Add a subtle pulse to the phone icon
    val pulse by remember { Animatable(1f) }
    LaunchedEffect(frame) {
        pulse.animateTo(
            targetValue = if (frame % 2 == 0) 1.0f else 1.1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .size(140.dp)
            .rotate(rotation)
            .scale(pulse.value)
            .background(Color(0xFF020617), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📱", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Flat Surface", fontSize = 13.sp, color = Color(0xFF94A3B8))
            Text("Keep Still", fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun MagnetometerInstructionAnimation(frame: Int) {
    val angle = (frame * 45f) % 360f
    val infiniteTransition = rememberInfiniteTransition(label = "mag_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "mag_rotation"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .background(Color(0xFF020617), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("∞", fontSize = 56.sp, modifier = Modifier.rotate(rotation))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Figure‑8 Pattern", fontSize = 13.sp, color = Color(0xFF94A3B8))
            Text("Rotate in all directions", fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun AccelerometerInstructionAnimation(frame: Int) {
    val orientations = listOf(
        "⬆️" to "Top",
        "⬇️" to "Bottom",
        "⬅️" to "Left",
        "➡️" to "Right",
        "🔄" to "Front",
        "↩️" to "Back"
    )
    val index = frame % orientations.size
    val (emoji, label) = orientations[index]

    // Pulse effect
    val pulse by remember { Animatable(1f) }
    LaunchedEffect(frame) {
        pulse.animateTo(
            targetValue = if (frame % 2 == 0) 1.0f else 1.15f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(pulse.value)
            .background(Color(0xFF020617), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 56.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 13.sp, color = Color(0xFF94A3B8))
            Text("Hold this position", fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun CompletedInstructionAnimation() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "complete_scale"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(scale)
            .background(Color(0xFF020617), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Ready to Go!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            Text("Calibration successful", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "Gyroscope Step")
@Composable
fun PreviewGyroGuideDialog() {
    MaterialTheme {
        CalibrationGuideDialog(step = 0, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Magnetometer Step")
@Composable
fun PreviewMagnetometerGuideDialog() {
    MaterialTheme {
        CalibrationGuideDialog(step = 1, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Accelerometer Step")
@Composable
fun PreviewAccelerometerGuideDialog() {
    MaterialTheme {
        CalibrationGuideDialog(step = 2, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Completed Step")
@Composable
fun PreviewCompletedGuideDialog() {
    MaterialTheme {
        CalibrationGuideDialog(step = 3, onDismiss = {})
    }
}