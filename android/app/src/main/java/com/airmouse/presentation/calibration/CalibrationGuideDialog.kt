// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationGuideDialog.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.CalibrationStatus
import kotlinx.coroutines.delay

@Composable
fun CalibrationGuideDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: Int = 0,
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()
    val calibrationProgress by viewModel.calibrationProgress.collectAsStateWithLifecycle()

    // Determine current display step (0: Gyro, 1: Mag, 2: Accel, 3: Success)
    val step = when {
        uiState.isComplete -> 3
        uiState.currentStep > 0 -> (uiState.currentStep - 1).coerceIn(0, 2)
        else -> initialStep.coerceIn(0, 3)
    }

    var currentImageIndex by remember { mutableStateOf(0) }
    
    // Auto-advance internal instruction frames for the animations
    LaunchedEffect(step) {
        currentImageIndex = 0
        while (true) {
            delay(2000)
            currentImageIndex = (currentImageIndex + 1) % 6
        }
    }

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
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Box {
                // Close button at top-right corner
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, "Close", tint = Color(0xFF94A3B8))
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Animated Illustration
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                            },
                            label = "IconTransition"
                        ) { targetStep ->
                            when (targetStep) {
                                0 -> GyroscopeInstructionAnimation(currentImageIndex)
                                1 -> MagnetometerInstructionAnimation(currentImageIndex)
                                2 -> AccelerometerInstructionAnimation(
                                    if (uiState.isCollecting) currentImageIndex else uiState.currentPosition
                                )
                                else -> CompletedInstructionAnimation()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. Title & Status
                    Text(
                        text = when (step) {
                            0 -> "🧭 Gyroscope"
                            1 -> "🧲 Magnetometer"
                            2 -> "📐 Accelerometer"
                            else -> "✅ Calibration Complete!"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Dynamic Instructions
                    Text(
                        text = if (uiState.statusMessage.isNotEmpty()) uiState.statusMessage else uiState.stepInstruction,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.heightIn(min = 48.dp)
                    )

                    // 4. Progress Bar
                    if ((isCalibrating || uiState.isCollecting) && !uiState.isComplete) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { if (step == 2) uiState.stepProgress else calibrationProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = Color(0xFF6366F1),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Text(
                            text = if (step == 2) "Capturing Position ${uiState.currentPosition + 1}/6" else "$calibrationProgress% complete",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isComplete) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val isStepFinished = uiState.statusMessage.contains("✓") || uiState.statusMessage.contains("Captured")
                            
                            Button(
                                onClick = {
                                    when {
                                        step == 2 && !uiState.isCollecting -> viewModel.startCurrentStep()
                                        isStepFinished -> {
                                            if (step == 2 && uiState.currentPosition >= 6) {
                                                viewModel.completeCalibration()
                                            } else {
                                                viewModel.nextStep()
                                            }
                                        }
                                        else -> viewModel.startCurrentStep()
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isCalibrating && !uiState.isCollecting || (step == 2 && !uiState.isCollecting) || isStepFinished,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isStepFinished) Color(0xFF10B981) else Color(0xFF6366F1)
                                )
                            ) {
                                if (isCalibrating || uiState.isCollecting) {
                                    CircularProgressIndicator(Modifier.size(24.dp), Color.White, 2.dp)
                                } else {
                                    val btnText = when {
                                        isStepFinished -> if (step == 2 && uiState.currentPosition >= 6) "Finish" else "Next Step →"
                                        step == 2 -> "Capture Position"
                                        uiState.currentStep == 0 -> "Start Calibration"
                                        else -> "Begin Step"
                                    }
                                    val icon = if (step == 2) Icons.Default.TouchApp else Icons.Default.PlayArrow
                                    Icon(icon, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(btnText, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.resetCalibration()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GyroscopeInstructionAnimation(frame: Int) {
    val rotation = when (frame % 3) {
        0 -> 0f
        1 -> 3f
        else -> -3f
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📱", fontSize = 64.sp, modifier = Modifier.rotate(rotation))
        Text("KEEP STILL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
    }
}

@Composable
fun MagnetometerInstructionAnimation(frame: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "mag")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)), label = ""
    )
    Text("∞", fontSize = 80.sp, color = Color(0xFF6366F1), modifier = Modifier.rotate(rotation))
}

@Composable
fun AccelerometerInstructionAnimation(position: Int) {
    val orientations = listOf("⬆️ Top", "⬇️ Bottom", "⬅️ Left", "➡️ Right", "🔄 Front", "↩️ Back")
    val current = orientations.getOrElse(position % 6) { "📱" }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(current.split(" ")[0], fontSize = 64.sp)
        Text(current.split(" ").getOrElse(1) { "" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun CompletedInstructionAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000), repeatMode = RepeatMode.Reverse), label = ""
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎉", fontSize = 72.sp, modifier = Modifier.scale(scale))
        Text("READY TO GO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalibrationGuideDialog() {
    MaterialTheme {
        CalibrationGuideDialog(onDismiss = {})
    }
}
