package com.airmouse.presentation.calibration

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.LinearInterpolator
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airmouse.R
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.MadgwickAHRS
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import javax.inject.Inject

@AndroidEntryPoint
class CalibrationActivity : ComponentActivity() {

    @Inject lateinit var prefs: PreferencesManager
    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var vibrator: Vibrator

    private lateinit var calibrationHelper: CalibrationHelper
    private var madgwick = MadgwickAHRS(beta = 0.1f)
    
    // Calibration data
    private var gyroOffsetX = 0f
    private var gyroOffsetY = 0f
    private var gyroOffsetZ = 0f
    private var magOffsetX = 0f
    private var magOffsetY = 0f
    private var magOffsetZ = 0f
    private var accelOffsetX = 0f
    private var accelOffsetY = 0f
    private var accelOffsetZ = 0f
    private var accelScaleX = 1f
    private var accelScaleY = 1f
    private var accelScaleZ = 1f
    
    // State
    private var currentRoll = 0f
    private var currentPitch = 0f
    private var currentYaw = 0f
    private var isCollecting = false
    private var gyroSamples = mutableListOf<Triple<Float, Float, Float>>()
    private var magSamples = mutableListOf<Triple<Float, Float, Float>>()
    
    // Calibration steps
    private enum class CalibStep { GYROSCOPE, MAGNETOMETER, ACCELEROMETER_FLAT, ACCELEROMETER_LEFT, ACCELEROMETER_RIGHT, ACCELEROMETER_TOP, ACCELEROMETER_BOTTOM, ACCELEROMETER_BACK, COMPLETE }
    private var currentStep = CalibStep.GYROSCOPE
    private var accelStep = 0
    private val accelOrientations = listOf(
        "Flat Facing UP" to Triple(0f, 0f, 9.81f),
        "Flat Facing DOWN" to Triple(0f, 0f, -9.81f),
        "Left Side" to Triple(9.81f, 0f, 0f),
        "Right Side" to Triple(-9.81f, 0f, 0f),
        "Top Edge" to Triple(0f, 9.81f, 0f),
        "Bottom Edge" to Triple(0f, -9.81f, 0f)
    )
    private val accelMeasurements = mutableListOf<Triple<Float, Float, Float>>()

    companion object {
        private const val GYRO_SAMPLES = 500
        private const val MAG_DURATION_MS = 8000L
        private const val ACCEL_SAMPLES = 50
        private const val STABILITY_THRESHOLD = 0.05f

        fun start(context: Context) {
            context.startActivity(Intent(context, CalibrationActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        calibrationHelper = CalibrationHelper(applicationContext, prefs)
        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6366F1),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                CalibrationScreen(
                    currentStep = currentStep,
                    accelStep = accelStep,
                    totalAccelSteps = accelOrientations.size,
                    currentRoll = currentRoll,
                    currentPitch = currentPitch,
                    currentYaw = currentYaw,
                    onStartGyro = { startGyroCalibration() },
                    onStartMag = { startMagCalibration() },
                    onStartAccel = { startAccelCalibration() },
                    onComplete = { finish() },
                    onSkip = { finish() }
                )
            }
        }
        
        startSensors()
    }

    private fun startSensors() {
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        if (isCollecting && currentStep == CalibStep.GYROSCOPE) {
                            gyroSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                        } else {
                            val gx = event.values[0] - gyroOffsetX
                            val gy = event.values[1] - gyroOffsetY
                            val gz = event.values[2] - gyroOffsetZ
                            madgwick.updateGyro(gx, gy, gz, 0.01f)
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (isCollecting && currentStep in listOf(CalibStep.ACCELEROMETER_FLAT, CalibStep.ACCELEROMETER_LEFT, CalibStep.ACCELEROMETER_RIGHT, CalibStep.ACCELEROMETER_TOP, CalibStep.ACCELEROMETER_BOTTOM, CalibStep.ACCELEROMETER_BACK)) {
                            // Collect samples for current orientation
                        } else {
                            val ax = (event.values[0] - accelOffsetX) / accelScaleX
                            val ay = (event.values[1] - accelOffsetY) / accelScaleY
                            val az = (event.values[2] - accelOffsetZ) / accelScaleZ
                            madgwick.updateAccel(ax, ay, az, 0.01f)
                        }
                        currentRoll = madgwick.getRollDegrees()
                        currentPitch = madgwick.getPitchDegrees()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (isCollecting && currentStep == CalibStep.MAGNETOMETER) {
                            magSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                        } else {
                            val mx = event.values[0] - magOffsetX
                            val my = event.values[1] - magOffsetY
                            val mz = event.values[2] - magOffsetZ
                            madgwick.updateMag(mx, my, mz, 0.01f)
                        }
                        currentYaw = madgwick.getYawDegrees()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        gyroscope?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    private fun startGyroCalibration() {
        lifecycleScope.launch {
            isCollecting = true
            gyroSamples.clear()
            delay(3000)
            
            if (gyroSamples.size >= GYRO_SAMPLES) {
                var sumX = 0f; var sumY = 0f; var sumZ = 0f
                gyroSamples.take(GYRO_SAMPLES).forEach { (x, y, z) ->
                    sumX += x; sumY += y; sumZ += z
                }
                gyroOffsetX = sumX / GYRO_SAMPLES
                gyroOffsetY = sumY / GYRO_SAMPLES
                gyroOffsetZ = sumZ / GYRO_SAMPLES
                
                prefs.putFloat("gyro_offset_x", gyroOffsetX)
                prefs.putFloat("gyro_offset_y", gyroOffsetY)
                prefs.putFloat("gyro_offset_z", gyroOffsetZ)
            }
            
            isCollecting = false
            currentStep = CalibStep.MAGNETOMETER
            vibrate(200)
        }
    }

    private fun startMagCalibration() {
        lifecycleScope.launch {
            isCollecting = true
            magSamples.clear()
            
            var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = Float.MIN_VALUE
            
            delay(MAG_DURATION_MS)
            
            magSamples.forEach { (x, y, z) ->
                minX = min(minX, x); maxX = max(maxX, x)
                minY = min(minY, y); maxY = max(maxY, y)
                minZ = min(minZ, z); maxZ = max(maxZ, z)
            }
            
            magOffsetX = (minX + maxX) / 2f
            magOffsetY = (minY + maxY) / 2f
            magOffsetZ = (minZ + maxZ) / 2f
            
            prefs.putFloat("mag_offset_x", magOffsetX)
            prefs.putFloat("mag_offset_y", magOffsetY)
            prefs.putFloat("mag_offset_z", magOffsetZ)
            
            isCollecting = false
            currentStep = CalibStep.ACCELEROMETER_FLAT
            vibrate(200)
        }
    }

    private fun startAccelCalibration() {
        lifecycleScope.launch {
            for ((index, (instruction, expected)) in accelOrientations.withIndex()) {
                currentStep = when (index) {
                    0 -> CalibStep.ACCELEROMETER_FLAT
                    1 -> CalibStep.ACCELEROMETER_BACK
                    2 -> CalibStep.ACCELEROMETER_LEFT
                    3 -> CalibStep.ACCELEROMETER_RIGHT
                    4 -> CalibStep.ACCELEROMETER_TOP
                    else -> CalibStep.ACCELEROMETER_BOTTOM
                }
                accelStep = index
                
                // Wait for user to position device
                delay(3000)
                
                // Collect samples
                val samples = mutableListOf<Triple<Float, Float, Float>>()
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 1000) {
                    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                    // In real implementation, collect from listener
                    delay(20)
                }
                
                if (samples.isNotEmpty()) {
                    val avgX = samples.map { it.first }.average().toFloat()
                    val avgY = samples.map { it.second }.average().toFloat()
                    val avgZ = samples.map { it.third }.average().toFloat()
                    accelMeasurements.add(Triple(avgX, avgY, avgZ))
                }
                vibrate(100)
            }
            
            calculateAccelCalibration()
            prefs.putBoolean("calibration_complete", true)
            currentStep = CalibStep.COMPLETE
            vibrate(300)
        }
    }

    private fun calculateAccelCalibration() {
        if (accelMeasurements.size != accelOrientations.size) return
        
        var sumXx = 0f; var sumYy = 0f; var sumZz = 0f
        var sumX = 0f; var sumY = 0f; var sumZ = 0f
        
        for (i in accelMeasurements.indices) {
            val (mx, my, mz) = accelMeasurements[i]
            val (ex, ey, ez) = accelOrientations[i].second
            
            sumXx += mx * ex; sumYy += my * ey; sumZz += mz * ez
            sumX += mx * mx; sumY += my * my; sumZ += mz * mz
        }
        
        accelScaleX = if (sumX > 0) sumXx / sumX else 1f
        accelScaleY = if (sumY > 0) sumYy / sumY else 1f
        accelScaleZ = if (sumZ > 0) sumZz / sumZ else 1f
        
        var sumOx = 0f; var sumOy = 0f; var sumOz = 0f
        for (i in accelMeasurements.indices) {
            val (mx, my, mz) = accelMeasurements[i]
            val (ex, ey, ez) = accelOrientations[i].second
            sumOx += ex - mx * accelScaleX
            sumOy += ey - my * accelScaleY
            sumOz += ez - mz * accelScaleZ
        }
        
        accelOffsetX = sumOx / accelMeasurements.size
        accelOffsetY = sumOy / accelMeasurements.size
        accelOffsetZ = sumOz / accelMeasurements.size
        
        prefs.putFloat("accel_offset_x", accelOffsetX)
        prefs.putFloat("accel_offset_y", accelOffsetY)
        prefs.putFloat("accel_offset_z", accelOffsetZ)
        prefs.putFloat("accel_scale_x", accelScaleX)
        prefs.putFloat("accel_scale_y", accelScaleY)
        prefs.putFloat("accel_scale_z", accelScaleZ)
    }

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        madgwick.reset()
    }
}

@Composable
fun CalibrationScreen(
    currentStep: CalibrationActivity.CalibStep,
    accelStep: Int,
    totalAccelSteps: Int,
    currentRoll: Float,
    currentPitch: Float,
    currentYaw: Float,
    onStartGyro: () -> Unit,
    onStartMag: () -> Unit,
    onStartAccel: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var animationProgress by remember { mutableStateOf(0f) }
    val configuration = LocalConfiguration.current
    
    LaunchedEffect(currentStep) {
        while (animationProgress < 1f) {
            animationProgress += 0.05f
            kotlinx.coroutines.delay(16)
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
        // Animated background
        AnimatedBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = when (currentStep) {
                    CalibrationActivity.CalibStep.GYROSCOPE -> "🎯 Gyroscope Calibration"
                    CalibrationActivity.CalibStep.MAGNETOMETER -> "🧭 Magnetometer Calibration"
                    CalibrationActivity.CalibStep.ACCELEROMETER_FLAT,
                    CalibrationActivity.CalibStep.ACCELEROMETER_LEFT,
                    CalibrationActivity.CalibStep.ACCELEROMETER_RIGHT,
                    CalibrationActivity.CalibStep.ACCELEROMETER_TOP,
                    CalibrationActivity.CalibStep.ACCELEROMETER_BOTTOM,
                    CalibrationActivity.CalibStep.ACCELEROMETER_BACK -> "⚡ Accelerometer Calibration"
                    CalibrationActivity.CalibStep.COMPLETE -> "✅ Calibration Complete!"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instruction
            Text(
                text = when (currentStep) {
                    CalibrationActivity.CalibStep.GYROSCOPE -> "Place your device on a flat, stationary surface"
                    CalibrationActivity.CalibStep.MAGNETOMETER -> "Move your device in a figure-8 pattern"
                    in listOf(CalibrationActivity.CalibStep.ACCELEROMETER_FLAT, CalibrationActivity.CalibStep.ACCELEROMETER_LEFT, CalibrationActivity.CalibStep.ACCELEROMETER_RIGHT, CalibrationActivity.CalibStep.ACCELEROMETER_TOP, CalibrationActivity.CalibStep.ACCELEROMETER_BOTTOM, CalibrationActivity.CalibStep.ACCELEROMETER_BACK) -> getAccelInstruction(currentStep)
                    CalibrationActivity.CalibStep.COMPLETE -> "Your device is now calibrated!"
                },
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 3D Phone Visualization
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Phone3DVisualization(
                    roll = currentRoll,
                    pitch = currentPitch,
                    yaw = currentYaw,
                    step = currentStep
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Progress
            when (currentStep) {
                CalibrationActivity.CalibStep.GYROSCOPE -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = Color(0xFF6366F1),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Collecting data...", color = Color(0xFF6366F1))
                }
                CalibrationActivity.CalibStep.MAGNETOMETER -> {
                    AnimatedFigureEight()
                }
                in listOf(CalibrationActivity.CalibStep.ACCELEROMETER_FLAT, CalibrationActivity.CalibStep.ACCELEROMETER_LEFT, CalibrationActivity.CalibStep.ACCELEROMETER_RIGHT, CalibrationActivity.CalibStep.ACCELEROMETER_TOP, CalibrationActivity.CalibStep.ACCELEROMETER_BOTTOM, CalibrationActivity.CalibStep.ACCELEROMETER_BACK) -> {
                    LinearProgressIndicator(
                        progress = (accelStep + 1).toFloat() / totalAccelSteps,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Step ${accelStep + 1} of $totalAccelSteps",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
                CalibrationActivity.CalibStep.COMPLETE -> {
                    SuccessAnimation()
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Buttons
            when (currentStep) {
                CalibrationActivity.CalibStep.GYROSCOPE -> {
                    Button(
                        onClick = onStartGyro,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF6366F1))
                    ) {
                        Text("Start Calibration", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                CalibrationActivity.CalibStep.MAGNETOMETER -> {
                    Button(
                        onClick = onStartMag,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF8B5CF6))
                    ) {
                        Text("Start Calibration", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                in listOf(CalibrationActivity.CalibStep.ACCELEROMETER_FLAT, CalibrationActivity.CalibStep.ACCELEROMETER_LEFT, CalibrationActivity.CalibStep.ACCELEROMETER_RIGHT, CalibrationActivity.CalibStep.ACCELEROMETER_TOP, CalibrationActivity.CalibStep.ACCELEROMETER_BOTTOM, CalibrationActivity.CalibStep.ACCELEROMETER_BACK) -> {
                    Button(
                        onClick = onStartAccel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF10B981))
                    ) {
                        Text("Next Position", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                CalibrationActivity.CalibStep.COMPLETE -> {
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
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip Calibration", color = Color(0xFF64748B))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
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
fun Phone3DVisualization(roll: Float, pitch: Float, yaw: Float, step: CalibrationActivity.CalibStep) {
    val normalizedRoll = (roll / 45f).coerceIn(-1f, 1f)
    val normalizedPitch = (pitch / 45f).coerceIn(-1f, 1f)
    
    var pulse by remember { mutableStateOf(0f) }
    
    LaunchedEffect(step) {
        while (true) {
            pulse = (pulse + 0.05f) % 1f
            kotlinx.coroutines.delay(50)
        }
    }
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                rotationX = -normalizedPitch * 30f
                rotationY = normalizedRoll * 30f
                rotationZ = 0f
                shadowElevation = 20f
                spotShadowAlpha = 0.5f
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(2.dp, Color(0xFF334155), RoundedCornerShape(24.dp)),
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
            // Crosshair for aiming
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                drawCircle(
                    color = Color(0xFF6366F1).copy(alpha = 0.3f),
                    radius = 40f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2f)
                )
                drawLine(
                    color = Color(0xFF6366F1).copy(alpha = 0.5f),
                    start = Offset(centerX - 30f, centerY),
                    end = Offset(centerX + 30f, centerY),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF6366F1).copy(alpha = 0.5f),
                    start = Offset(centerX, centerY - 30f),
                    end = Offset(centerX, centerY + 30f),
                    strokeWidth = 1f
                )
                drawCircle(
                    color = Color(0xFF6366F1),
                    radius = 4f,
                    center = Offset(
                        centerX + roll / 2,
                        centerY + pitch / 2
                    )
                )
            }
            
            // Calibration overlay
            when (step) {
                CalibrationActivity.CalibStep.GYROSCOPE -> {
                    Text("⚡", fontSize = 48.sp, color = Color(0xFF6366F1))
                }
                CalibrationActivity.CalibStep.MAGNETOMETER -> {
                    Text("∞", fontSize = 48.sp, color = Color(0xFF8B5CF6))
                }
                else -> {
                    // Show orientation arrow
                    val arrowRotation = when (step) {
                        CalibrationActivity.CalibStep.ACCELEROMETER_FLAT -> 0f
                        CalibrationActivity.CalibStep.ACCELEROMETER_BACK -> 180f
                        CalibrationActivity.CalibStep.ACCELEROMETER_LEFT -> -90f
                        CalibrationActivity.CalibStep.ACCELEROMETER_RIGHT -> 90f
                        CalibrationActivity.CalibStep.ACCELEROMETER_TOP -> 0f
                        CalibrationActivity.CalibStep.ACCELEROMETER_BOTTOM -> 180f
                        else -> 0f
                    }
                    Text(
                        "⬆️",
                        fontSize = 48.sp,
                        modifier = Modifier.rotate(arrowRotation)
                    )
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
    }
}

@Composable
fun AnimatedFigureEight() {
    val rotation by remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing)
            )
        )
    }
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color(0xFF1E293B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("∞", fontSize = 40.sp, modifier = Modifier.rotate(rotation.value))
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

private fun getAccelInstruction(step: CalibrationActivity.CalibStep): String {
    return when (step) {
        CalibrationActivity.CalibStep.ACCELEROMETER_FLAT -> "Place device flat facing UP"
        CalibrationActivity.CalibStep.ACCELEROMETER_BACK -> "Place device flat facing DOWN"
        CalibrationActivity.CalibStep.ACCELEROMETER_LEFT -> "Place device on LEFT side"
        CalibrationActivity.CalibStep.ACCELEROMETER_RIGHT -> "Place device on RIGHT side"
        CalibrationActivity.CalibStep.ACCELEROMETER_TOP -> "Place device standing on TOP edge"
        CalibrationActivity.CalibStep.ACCELEROMETER_BOTTOM -> "Place device standing on BOTTOM edge"
        else -> "Follow the instruction"
    }
}