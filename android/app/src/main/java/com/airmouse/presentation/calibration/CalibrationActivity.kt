package com.airmouse.presentation.calibration

import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airmouse.sensors.MadgwickAHRS
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CalibrationActivity : ComponentActivity() {

    @Inject lateinit var prefs: PreferencesManager
    @Inject lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6366F1),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                CalibrationScreen(
                    viewModel = viewModel(),
                    onComplete = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val gyroProgress by viewModel.gyroProgress.collectAsState()
    val magProgress by viewModel.magProgress.collectAsState()
    val accelStep by viewModel.accelStep.collectAsState()
    val configuration = LocalConfiguration.current
    
    var animationValue by remember { mutableStateOf(0f) }
    
    LaunchedEffect(uiState.currentStep) {
        while (animationValue < 1f) {
            animationValue += 0.05f
                       delay(16)
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
        // Animated background circles
        AnimatedBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with step indicator
            StepIndicator(
                currentStep = uiState.currentStep,
                gyroComplete = uiState.gyroComplete,
                magComplete = uiState.magComplete,
                accelComplete = uiState.accelComplete
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main instruction text
            Text(
                text = when (uiState.currentStep) {
                    CalibrationViewModel.CalibrationStep.GYROSCOPE -> "🎯 Gyroscope Calibration"
                    CalibrationViewModel.CalibrationStep.MAGNETOMETER -> "🧭 Magnetometer Calibration"
                    CalibrationViewModel.CalibrationStep.ACCELEROMETER -> "⚡ Accelerometer Calibration"
                    CalibrationViewModel.CalibrationStep.COMPLETE -> "✅ Calibration Complete!"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = when {
                    uiState.isCalibrating -> uiState.instruction
                    uiState.currentStep == CalibrationViewModel.CalibrationStep.GYROSCOPE -> "Place your device on a flat, stationary surface"
                    uiState.currentStep == CalibrationViewModel.CalibrationStep.MAGNETOMETER -> "Move your device in a smooth figure-8 pattern"
                    uiState.currentStep == CalibrationViewModel.CalibrationStep.ACCELEROMETER -> getAccelInstruction(accelStep)
                    uiState.currentStep == CalibrationViewModel.CalibrationStep.COMPLETE -> "Your device is now perfectly calibrated!"
                    else -> "Follow the instructions below"
                },
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 3D Phone Visualization with live orientation
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Phone3DVisualization(
                    step = uiState.currentStep,
                    subStep = accelStep,
                    isCalibrating = uiState.isCalibrating
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Progress indicator based on current step
            when (uiState.currentStep) {
                CalibrationViewModel.CalibrationStep.GYROSCOPE -> {
                    if (uiState.isCalibrating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            color = Color(0xFF6366F1),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Collecting data... $gyroProgress%",
                            color = Color(0xFF6366F1),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        AnimatedCalibrationButton(
                            text = "Start Gyroscope Calibration",
                            color = Color(0xFF6366F1),
                            onClick = { viewModel.startGyroCalibration() }
                        )
                    }
                }
                
                CalibrationViewModel.CalibrationStep.MAGNETOMETER -> {
                    if (uiState.isCalibrating) {
                        AnimatedFigureEight()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Calibrating... $magProgress%",
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        AnimatedCalibrationButton(
                            text = "Start Magnetometer Calibration",
                            color = Color(0xFF8B5CF6),
                            onClick = { viewModel.startMagCalibration() }
                        )
                    }
                }
                
                CalibrationViewModel.CalibrationStep.ACCELEROMETER -> {
                    if (accelStep < 6) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                progress = accelStep / 6f,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Step $accelStep of 6",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        val success = viewModel.calibrateAccelerometerStep(accelStep)
                                        if (success && accelStep >= 5) {
                                            viewModel.completeAccelerometerCalibration()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(Color(0xFF10B981))
                            ) {
                                Text(
                                    if (accelStep < 5) "Next Position" else "Complete Calibration",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                
                CalibrationViewModel.CalibrationStep.COMPLETE -> {
                    SuccessAnimation()
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Quality badge
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF1E293B)),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "Quality: ${uiState.quality.lowercase().replaceFirstChar { it.uppercase() }}",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            color = getQualityColor(uiState.quality),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF10B981))
                    ) {
                        Text("Start Using Air Mouse", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = { viewModel.resetAndRecalibrate() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Recalibrate", color = Color(0xFF64748B))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Skip button (only show before completion)
            if (uiState.currentStep != CalibrationViewModel.CalibrationStep.COMPLETE && !uiState.isCalibrating) {
                TextButton(
                    onClick = { viewModel.skipCalibration() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip Calibration", color = Color(0xFF64748B))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StepIndicator(
    currentStep: CalibrationViewModel.CalibrationStep,
    gyroComplete: Boolean,
    magComplete: Boolean,
    accelComplete: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StepCircle(
            step = 1,
            label = "Gyro",
            isActive = currentStep == CalibrationViewModel.CalibrationStep.GYROSCOPE,
            isComplete = gyroComplete
        )
        
        StepConnector(isComplete = gyroComplete && magComplete)
        
        StepCircle(
            step = 2,
            label = "Magnetometer",
            isActive = currentStep == CalibrationViewModel.CalibrationStep.MAGNETOMETER,
            isComplete = magComplete
        )
        
        StepConnector(isComplete = magComplete && accelComplete)
        
        StepCircle(
            step = 3,
            label = "Accelerometer",
            isActive = currentStep == CalibrationViewModel.CalibrationStep.ACCELEROMETER,
            isComplete = accelComplete
        )
        
        StepConnector(isComplete = accelComplete)
        
        StepCircle(
            step = 4,
            label = "Complete",
            isActive = currentStep == CalibrationViewModel.CalibrationStep.COMPLETE,
            isComplete = currentStep == CalibrationViewModel.CalibrationStep.COMPLETE
        )
    }
}

@Composable
fun StepCircle(step: Int, label: String, isActive: Boolean, isComplete: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isComplete -> Color(0xFF10B981)
                        isActive -> Color(0xFF6366F1)
                        else -> Color(0xFF334155)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Text("$step", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (isActive || isComplete) Color.White else Color(0xFF64748B)
        )
    }
}

@Composable
fun StepConnector(isComplete: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(2.dp)
            .background(if (isComplete) Color(0xFF10B981) else Color(0xFF334155))
            .align(Alignment.CenterVertically)
    )
}

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        )
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        for (i in 0..2) {
            Box(
                modifier = Modifier
                    .size((300 + i * 100).dp)
                    .graphicsLayer { rotationZ = rotation * (1f - i * 0.3f) }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.1f - i * 0.03f),
                                Color.Transparent
                            ),
                            radius = 300f
                        ),
                        shape = CircleShape
                    )
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun Phone3DVisualization(step: CalibrationViewModel.CalibrationStep, subStep: Int, isCalibrating: Boolean) {
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    var pulse by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isCalibrating) {
        while (isCalibrating) {
            pulse = (pulse + 0.05f) % 1f
            delay(50)
        }
    }
    
    // Animate rotation based on calibration step
    LaunchedEffect(step, subStep) {
        when (step) {
            CalibrationViewModel.CalibrationStep.ACCELEROMETER -> {
                rotationX = when (subStep) {
                    0 -> 0f      // Flat up
                    1 -> 180f    // Flat down
                    2 -> 90f     // Left side
                    3 -> -90f    // Right side
                    4 -> 0f      // Top edge
                    5 -> 180f    // Bottom edge
                    else -> 0f
                }
                rotationY = when (subStep) {
                    2 -> 90f
                    3 -> -90f
                    else -> 0f
                }
            }
            CalibrationViewModel.CalibrationStep.MAGNETOMETER -> {
                rotationX = (rotationX + 5f) % 360f
                rotationY = (rotationY + 3f) % 360f
            }
            else -> {
                rotationX = 0f
                rotationY = 0f
            }
        }
    }
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                rotationX = rotationX
                rotationY = rotationY
                shadowElevation = 20f
                spotShadowAlpha = 0.5f
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(2.dp, if (isCalibrating) Color(0xFF6366F1) else Color(0xFF334155), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Screen
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF020617))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Content based on calibration step
            when (step) {
                CalibrationViewModel.CalibrationStep.GYROSCOPE -> {
                    Text("⚡", fontSize = 48.sp, color = Color(0xFF6366F1))
                    if (isCalibrating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = Color(0xFF6366F1),
                            strokeWidth = 2.dp
                        )
                    }
                }
                CalibrationViewModel.CalibrationStep.MAGNETOMETER -> {
                    Text("∞", fontSize = 48.sp, color = Color(0xFF8B5CF6))
                }
                CalibrationViewModel.CalibrationStep.ACCELEROMETER -> {
                    val arrow = when (subStep) {
                        0 -> "⬆️"
                        1 -> "⬇️"
                        2 -> "⬅️"
                        3 -> "➡️"
                        4 -> "⬆️"
                        5 -> "⬇️"
                        else -> "⬆️"
                    }
                    Text(arrow, fontSize = 48.sp)
                }
                CalibrationViewModel.CalibrationStep.COMPLETE -> {
                    Text("✓", fontSize = 48.sp, color = Color(0xFF10B981))
                }
            }
        }
        
        // Home button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(40.dp, 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF334155))
        )
        
        // Pulsing ring effect
        if (isCalibrating) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF6366F1).copy(alpha = 0.3f),
                    radius = 100f + 50f * pulse,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

@Composable
fun AnimatedFigureEight() {
    val rotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = ""
    )
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color(0xFF1E293B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("∞", fontSize = 40.sp, modifier = Modifier.rotate(rotation))
    }
}

@Composable
fun SuccessAnimation() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xFF10B981)),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnimatedCalibrationButton(text: String, color: Color, onClick: () -> Unit) {
    var buttonScale by remember { mutableStateOf(1f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            buttonScale = 1.05f
            delay(500)
            buttonScale = 1f
            delay(500)
        }
    }
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(buttonScale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(color)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun getAccelInstruction(step: Int): String {
    return when (step) {
        0 -> "Place device flat facing UP"
        1 -> "Place device flat facing DOWN"
        2 -> "Place device on LEFT side"
        3 -> "Place device on RIGHT side"
        4 -> "Place device standing on TOP edge"
        5 -> "Place device standing on BOTTOM edge"
        else -> "Follow the instruction"
    }
}

private fun getQualityColor(quality: String): Color {
    return when (quality.lowercase()) {
        "excellent" -> Color(0xFF10B981)
        "good" -> Color(0xFF6366F1)
        "fair" -> Color(0xFFF59E0B)
        "poor" -> Color(0xFFEF4444)
        else -> Color(0xFF94A3B8)
    }
}