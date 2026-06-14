package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.R
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Auto-start calibration when screen loads
    LaunchedEffect(Unit) {
        delay(500)
        if (!uiState.isCollecting && !uiState.isComplete && !uiState.isSkipped) {
            viewModel.startCalibration()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.stepTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isComplete && !uiState.isCollecting) {
                        TextButton(onClick = { viewModel.skipCalibration() }) {
                            Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { viewModel.openHelp() }) {
                        Icon(Icons.Default.Help, contentDescription = "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            if (!uiState.isComplete && !uiState.isSkipped) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            enabled = uiState.currentStep > 0 && !uiState.isCollecting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                if (uiState.isCollecting) {
                                    if (uiState.currentStep == 1 && uiState.currentPosition < uiState.totalPositions - 1) {
                                        viewModel.nextAccelPosition()
                                    } else {
                                        viewModel.stopCollection()
                                    }
                                } else {
                                    viewModel.startCalibration()
                                }
                            },
                            enabled = !uiState.isComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isCollecting)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            when {
                                uiState.isCollecting && uiState.currentStep == 1 -> {
                                    if (uiState.currentPosition < uiState.totalPositions - 1) {
                                        Text("Next Position")
                                    } else {
                                        Text("Complete")
                                    }
                                }
                                uiState.isCollecting -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Collecting...")
                                }
                                else -> Text("Start Calibration")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.abortCalibration() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            ParticleBackground(particleCount = 20)
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Instruction Card
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Animated Instruction Icon
                            AnimatedInstructionIcon(uiState)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = uiState.stepInstruction,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = uiState.stepDescription,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Progress Section
                item {
                    ProgressCard(
                        progress = uiState.progress,
                        samplesCollected = uiState.samplesCollected,
                        totalSamples = uiState.totalSamplesNeeded,
                        currentStep = uiState.currentStep,
                        currentPosition = uiState.currentPosition,
                        totalPositions = uiState.totalPositions
                    )
                }

                // Sensor Data Visualization
                if (uiState.isCollecting) {
                    item {
                        SensorDataCard(
                            gyroData = uiState.gyroData,
                            accelData = uiState.accelData,
                            magData = uiState.magData
                        )
                    }
                }

                // Position Guide (for accelerometer calibration)
                if (uiState.currentStep == 1 && !uiState.isComplete) {
                    item {
                        PositionGuideCard(
                            currentPosition = uiState.currentPosition,
                            positions = positions,
                            onPositionClick = { viewModel.jumpToPosition(it) }
                        )
                    }
                }

                // Status Message
                item {
                    StatusCard(
                        message = uiState.statusMessage,
                        errorMessage = uiState.errorMessage,
                        isComplete = uiState.isComplete
                    )
                }

                // Calibration Results
                if (uiState.isComplete) {
                    item {
                        CalibrationResultsCard(
                            calibrationData = uiState.calibrationData,
                            quality = uiState.calibrationQuality
                        )
                    }
                }

                // Action Buttons
                if (uiState.isComplete || uiState.isSkipped) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onComplete,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Using Air Mouse")
                            }
                            
                            OutlinedButton(
                                onClick = { viewModel.resetCalibration() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recalibrate")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun AnimatedInstructionIcon(uiState: CalibrationUiState) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val color = if (uiState.isCollecting || uiState.isComplete) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(pulse),
        contentAlignment = Alignment.Center
    ) {
        when (uiState.currentStep) {
            0 -> {
                // Gyroscope animation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.width / 2 - 10f
                    
                    drawCircle(
                        color = color.copy(alpha = 0.2f),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                    
                    drawArc(
                        color = color,
                        startAngle = rotation,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                    
                    drawCircle(
                        color = color,
                        radius = 8f,
                        center = Offset(centerX + radius * 0.7f, centerY)
                    )
                }
                Text("🔄", fontSize = 40.sp, modifier = Modifier.rotate(rotation))
            }
            1 -> {
                // Accelerometer animation
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(y = bounce.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = color
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                            .offset(y = bounce.dp / 2)
                    )
                }
            }
            2 -> {
                // Magnetometer animation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.width / 2 - 10f
                    
                    // Compass rose
                    for (i in 0..3) {
                        val angle = i * 90f + rotation
                        val rad = Math.toRadians(angle.toDouble()).toFloat()
                        val endX = centerX + radius * 0.8f * kotlin.math.cos(rad)
                        val endY = centerY + radius * 0.8f * kotlin.math.sin(rad)
                        
                        drawLine(
                            color = color,
                            start = Offset(centerX, centerY),
                            end = Offset(endX, endY),
                            strokeWidth = if (i == 0) 4f else 2f,
                            cap = StrokeCap.Round
                        )
                    }
                    
                    drawCircle(
                        color = color,
                        radius = 8f,
                        center = Offset(centerX, centerY)
                    )
                }
                Text("🧭", fontSize = 48.sp, modifier = Modifier.offset(y = bounce.dp / 2))
            }
        }
    }
}

@Composable
fun ProgressCard(
    progress: Int,
    samplesCollected: Int,
    totalSamples: Int,
    currentStep: Int,
    currentPosition: Int,
    totalPositions: Int
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Calibration Progress",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Animated progress bar
            AnimatedProgressBar(progress = progress / 100f)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$progress%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (samplesCollected > 0) {
                    Text(
                        "$samplesCollected / $totalSamples samples",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (currentStep == 1 && currentPosition < totalPositions) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (currentPosition + 1).toFloat() / totalPositions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF4CAF50)
                )
                Text(
                    "Position ${currentPosition + 1} of $totalPositions",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AnimatedProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00BCD4),
                            Color(0xFF4CAF50)
                        )
                    )
                )
        )
    }
}

@Composable
fun SensorDataCard(
    gyroData: Triple<Float, Float, Float>,
    accelData: Triple<Float, Float, Float>,
    magData: Triple<Float, Float, Float>
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Live Sensor Data",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SensorRow("Gyroscope", gyroData.first, gyroData.second, gyroData.third, Color(0xFFFF9800))
            SensorRow("Accelerometer", accelData.first, accelData.second, accelData.third, Color(0xFF00BCD4))
            SensorRow("Magnetometer", magData.first, magData.second, magData.third, Color(0xFF4CAF50))
        }
    }
}

@Composable
fun SensorRow(title: String, x: Float, y: Float, z: Float, color: Color) {
    Column {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensorValue("X", x, color)
            SensorValue("Y", y, color)
            SensorValue("Z", z, color)
        }
    }
}

@Composable
fun SensorValue(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(
            String.format("%.2f", value),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = color
        )
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PositionGuideCard(
    currentPosition: Int,
    positions: List<String>,
    onPositionClick: (Int) -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Position Guide",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            positions.forEachIndexed { index, position ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onPositionClick(index) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < currentPosition) Color(0xFF4CAF50)
                                else if (index == currentPosition) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < currentPosition) {
                            Text("✓", fontSize = 12.sp, color = Color.White)
                        } else {
                            Text("${index + 1}", fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        position,
                        fontSize = 13.sp,
                        color = if (index <= currentPosition) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(message: String, errorMessage: String?, isComplete: Boolean) {
    val backgroundColor = when {
        errorMessage != null -> MaterialTheme.colorScheme.errorContainer
        isComplete -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    
    val textColor = when {
        errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
        isComplete -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                when {
                    errorMessage != null -> Icons.Default.Error
                    isComplete -> Icons.Default.CheckCircle
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = textColor
            )
            Text(
                text = errorMessage ?: message,
                color = textColor,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CalibrationResultsCard(calibrationData: CalibrationData, quality: String) {
    var expanded by remember { mutableStateOf(false) }
    
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calibration Results",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                QualityBadge(quality = quality)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CalibrationMetric("Gyro X", calibrationData.gyroOffsetX, "rad/s")
                CalibrationMetric("Gyro Y", calibrationData.gyroOffsetY, "rad/s")
                CalibrationMetric("Gyro Z", calibrationData.gyroOffsetZ, "rad/s")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CalibrationMetric("Accel X", calibrationData.accelOffsetX, "m/s²")
                CalibrationMetric("Accel Y", calibrationData.accelOffsetY, "m/s²")
                CalibrationMetric("Accel Z", calibrationData.accelOffsetZ, "m/s²")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CalibrationMetric("Mag X", calibrationData.magOffsetX, "μT")
                        CalibrationMetric("Mag Y", calibrationData.magOffsetY, "μT")
                        CalibrationMetric("Mag Z", calibrationData.magOffsetZ, "μT")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CalibrationMetric("Accel Scale X", calibrationData.accelScaleX, "")
                        CalibrationMetric("Accel Scale Y", calibrationData.accelScaleY, "")
                        CalibrationMetric("Accel Scale Z", calibrationData.accelScaleZ, "")
                    }
                }
            }
            
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (expanded) "Show Less" else "Show More")
            }
        }
    }
}

@Composable
fun CalibrationMetric(label: String, value: Float, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(
            String.format("%.3f", value),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Text("$label $unit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QualityBadge(quality: String) {
    val (color, text) = when (quality.lowercase()) {
        "excellent" -> Color(0xFF4CAF50) to "Excellent"
        "good" -> Color(0xFF00BCD4) to "Good"
        "fair" -> Color(0xFFFFC107) to "Fair"
        else -> Color(0xFFF44336) to "Poor"
    }
    
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}


// CalibrationScreen.kt - Use these components:
@Composable
fun CalibrationScreen() {
    Column {
        // Sensor visualizer with 3D phone
        SensorVisualizer(
            roll = currentRoll,
            pitch = currentPitch,
            yaw = currentYaw,
            size = VisualizerSize.LARGE
        )
        
        // Gyroscope visualizer
        GyroscopeVisualizer(x = gyroX, y = gyroY, z = gyroZ)
        
        // Progress indicator
        CircularProgressWithLabel(progress = calibrationProgress)
        
        // Animated counter for samples
        AnimatedCounter(targetValue = sampleCount, suffix = " samples")
        
        // Step indicators
        Row {
            NotificationBadge(count = if (step1Complete) 1 else 0)
            NotificationBadge(count = if (step2Complete) 1 else 0)
            NotificationBadge(count = if (step3Complete) 1 else 0)
        }
        
        // Voice wave for voice-guided calibration
        VoiceWaveAnimation(isActive = isListening)
        
        // Animated toast for instructions
        AnimatedToast(
            message = "Place device on flat surface",
            isVisible = showInstruction,
            onDismiss = { /* dismiss */ },
            type = ToastType.INFO
        )
    }
}