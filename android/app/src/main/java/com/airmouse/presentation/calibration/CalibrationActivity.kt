package com.airmouse.presentation.calibration

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.MadgwickAHRS
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CalibrationActivity : ComponentActivity() {

    @Inject lateinit var prefs: PreferencesManager
    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var vibrator: Vibrator

    private lateinit var calibrationHelper: CalibrationHelper
    private val madgwick = MadgwickAHRS(beta = 0.1f)

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

    // Sensor state
    private var currentRoll by mutableFloatStateOf(0f)
    private var currentPitch by mutableFloatStateOf(0f)
    private var currentYaw by mutableFloatStateOf(0f)
    private var isCollecting = false
    private val gyroSamples = mutableListOf<Triple<Float, Float, Float>>()
    private val magSamples = mutableListOf<Triple<Float, Float, Float>>()

    var calibrationMessage by mutableStateOf("Ready to start")
        private set

    enum class CalibrationStep {
        GYROSCOPE,
        MAGNETOMETER,
        ACCELEROMETER_FLAT,
        ACCELEROMETER_LEFT,
        ACCELEROMETER_RIGHT,
        ACCELEROMETER_TOP,
        ACCELEROMETER_BOTTOM,
        ACCELEROMETER_BACK,
        COMPLETE
    }

    private var currentStep by mutableStateOf(CalibrationStep.GYROSCOPE)
    private var accelStep by mutableIntStateOf(0)

    private val accelOrientations = listOf(
        "Flat Facing UP" to Triple(0f, 0f, 9.81f),
        "Flat Facing DOWN" to Triple(0f, 0f, -9.81f),
        "Left Side" to Triple(9.81f, 0f, 0f),
        "Right Side" to Triple(-9.81f, 0f, 0f),
        "Top Edge" to Triple(0f, 9.81f, 0f),
        "Bottom Edge" to Triple(0f, -9.81f, 0f)
    )
    private val accelMeasurements = mutableListOf<Triple<Float, Float, Float>>()

    private var sensorListener: SensorEventListener? = null

    companion object {
        private const val GYRO_SAMPLES = 500
        private const val MAG_DURATION_MS = 8000L
        private const val ACCEL_SAMPLES = 50

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
                    navigationActions = null, // Set if needed when launched inside a NavHost graph
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
                    onBack = { finish() }
                )
            }
        }
        startSensors()
    }

    private fun startSensors() {
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        if (isCollecting && currentStep == CalibrationStep.GYROSCOPE) {
                            gyroSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                        } else {
                            val gx = event.values[0] - gyroOffsetX
                            val gy = event.values[1] - gyroOffsetY
                            val gz = event.values[2] - gyroOffsetZ
                            madgwick.updateGyro(gx, gy, gz, 0.01f)
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val ax = (event.values[0] - accelOffsetX) / accelScaleX
                        val ay = (event.values[1] - accelOffsetY) / accelScaleY
                        val az = (event.values[2] - accelOffsetZ) / accelScaleZ
                        madgwick.updateAccel(ax, ay, az, 0.01f)
                        currentRoll = madgwick.getRollDegrees()
                        currentPitch = madgwick.getPitchDegrees()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (isCollecting && currentStep == CalibrationStep.MAGNETOMETER) {
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

        sensorListener?.let { listener ->
            gyroscope?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    private fun startGyroCalibration() {
        lifecycleScope.launch {
            isCollecting = true
            gyroSamples.clear()
            calibrationMessage = "Collecting gyroscope samples..."

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
                prefs.putBoolean("gyro_calibrated", true)
            }

            isCollecting = false
            currentStep = CalibrationStep.MAGNETOMETER
            vibrate(200)
        }
    }

    private fun startMagCalibration() {
        lifecycleScope.launch {
            isCollecting = true
            magSamples.clear()
            calibrationMessage = "Moving device in figure-8 pattern..."

            delay(MAG_DURATION_MS)

            var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = Float.MIN_VALUE

            magSamples.forEach { (x, y, z) ->
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z
            }

            magOffsetX = (minX + maxX) / 2f
            magOffsetY = (minY + maxY) / 2f
            magOffsetZ = (minZ + maxZ) / 2f

            prefs.putFloat("mag_offset_x", magOffsetX)
            prefs.putFloat("mag_offset_y", magOffsetY)
            prefs.putFloat("mag_offset_z", magOffsetZ)
            prefs.putBoolean("mag_calibrated", true)

            isCollecting = false
            currentStep = CalibrationStep.ACCELEROMETER_FLAT
            vibrate(200)
        }
    }

    private fun startAccelCalibration() {
        lifecycleScope.launch {
            accelMeasurements.clear()
            calibrationMessage = "Starting accelerometer calibration..."

            for (index in accelOrientations.indices) {
                currentStep = when (index) {
                    0 -> CalibrationStep.ACCELEROMETER_FLAT
                    1 -> CalibrationStep.ACCELEROMETER_BACK
                    2 -> CalibrationStep.ACCELEROMETER_LEFT
                    3 -> CalibrationStep.ACCELEROMETER_RIGHT
                    4 -> CalibrationStep.ACCELEROMETER_TOP
                    else -> CalibrationStep.ACCELEROMETER_BOTTOM
                }
                accelStep = index
                calibrationMessage = accelOrientations[index].first

                delay(3000)
                val samples = collectAccelSamples()
                if (samples != null) {
                    accelMeasurements.add(samples)
                }
                vibrate(100)
            }

            calculateAccelCalibration()
            prefs.putBoolean("calibration_complete", true)
            currentStep = CalibrationStep.COMPLETE
            vibrate(300)
            calibrationMessage = "Calibration complete!"
        }
    }

    private suspend fun collectAccelSamples(): Triple<Float, Float, Float>? {
        val samples = mutableListOf<Triple<Float, Float, Float>>()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return null

        val tempListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (samples.size < ACCEL_SAMPLES) {
                    samples.add(Triple(event.values[0], event.values[1], event.values[2]))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(tempListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        delay(1000)
        sensorManager.unregisterListener(tempListener)

        if (samples.isEmpty()) return null

        return Triple(
            samples.map { it.first }.average().toFloat(),
            samples.map { it.second }.average().toFloat(),
            samples.map { it.third }.average().toFloat()
        )
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
        prefs.putBoolean("accel_calibrated", true)
    }

    private fun vibrate(duration: Long) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        super.onDestroy()
        madgwick.reset()
        sensorListener?.let { sensorManager.unregisterListener(it) }
    }
}

@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions?,
    currentStep: CalibrationActivity.CalibrationStep,
    accelStep: Int,
    totalAccelSteps: Int,
    currentRoll: Float,
    currentPitch: Float,
    currentYaw: Float,
    onStartGyro: () -> Unit,
    onStartMag: () -> Unit,
    onStartAccel: () -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    var animationProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentStep) {
        animationProgress = 0f
        while (animationProgress < 1f) {
            animationProgress += 0.05f
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
        AnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (currentStep) {
                    CalibrationActivity.CalibrationStep.GYROSCOPE -> "🎯 Gyroscope Calibration"
                    CalibrationActivity.CalibrationStep.MAGNETOMETER -> "🧭 Magnetometer Calibration"
                    CalibrationActivity.CalibrationStep.COMPLETE -> "✅ Calibration Complete!"
                    else -> "⚡ Accelerometer Calibration"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (currentStep) {
                    CalibrationActivity.CalibrationStep.GYROSCOPE -> "Place your device on a flat, stationary surface"
                    CalibrationActivity.CalibrationStep.MAGNETOMETER -> "Move your device in a figure-8 pattern"
                    CalibrationActivity.CalibrationStep.COMPLETE -> "Your device is now calibrated!"
                    else -> getAccelInstruction(currentStep)
                },
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(250.dp)
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

            when (currentStep) {
                CalibrationActivity.CalibrationStep.GYROSCOPE -> {
                    AnimatedCircularProgress(progress = animationProgress)
                }
                CalibrationActivity.CalibrationStep.MAGNETOMETER -> {
                    AnimatedFigureEight()
                }
                CalibrationActivity.CalibrationStep.COMPLETE -> {
                    SuccessAnimation()
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { (accelStep + 1).toFloat() / totalAccelSteps },
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
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            when (currentStep) {
                CalibrationActivity.CalibrationStep.GYROSCOPE -> {
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
                CalibrationActivity.CalibrationStep.MAGNETOMETER -> {
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
                CalibrationActivity.CalibrationStep.COMPLETE -> {
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
                else -> {
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onBack,
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
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "Rotation"
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
fun AnimatedCircularProgress(progress: Float) {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF6366F1),
            strokeWidth = 6.dp
        )
        Text(
            "${(progress * 100).toInt()}%",
            color = Color(0xFF6366F1),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnimatedFigureEight() {
    val rotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "FigureEightRotation"
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
        label = "SuccessScale"
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
fun Phone3DVisualization(
    roll: Float,
    pitch: Float,
    yaw: Float,
    step: CalibrationActivity.CalibrationStep
) {
    val normalizedRoll = (roll / 45f).coerceIn(-1f, 1f)
    val normalizedPitch = (pitch / 45f).coerceIn(-1f, 1f)
    // Optional: map your yaw into visualization if needed
    val combinedRotationZ = yaw.coerceIn(-180f, 180f)

    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                rotationX = -normalizedPitch * 30f
                rotationY = normalizedRoll * 30f
                shadowElevation = 20f
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
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF020617))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                CalibrationActivity.CalibrationStep.GYROSCOPE -> {
                    Text("⚡", fontSize = 48.sp, color = Color(0xFF6366F1))
                }
                CalibrationActivity.CalibrationStep.MAGNETOMETER -> {
                    Text("∞", fontSize = 48.sp, color = Color(0xFF8B5CF6), modifier = Modifier.rotate(combinedRotationZ))
                }
                CalibrationActivity.CalibrationStep.COMPLETE -> {
                    Text("✓", fontSize = 48.sp, color = Color(0xFF10B981))
                }
                else -> {
                    val arrowRotation = when (step) {
                        CalibrationActivity.CalibrationStep.ACCELEROMETER_BACK -> 180f
                        CalibrationActivity.CalibrationStep.ACCELEROMETER_LEFT -> -90f
                        CalibrationActivity.CalibrationStep.ACCELEROMETER_RIGHT -> 90f
                        CalibrationActivity.CalibrationStep.ACCELEROMETER_BOTTOM -> 180f
                        else -> 0f
                    }
                    Text("⬆️", fontSize = 48.sp, modifier = Modifier.rotate(arrowRotation))
                }
            }
        }

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

private fun getAccelInstruction(step: CalibrationActivity.CalibrationStep): String {
    return when (step) {
        CalibrationActivity.CalibrationStep.ACCELEROMETER_FLAT -> "Place device flat facing UP"
        CalibrationActivity.CalibrationStep.ACCELEROMETER_BACK -> "Place device flat facing DOWN"
        CalibrationActivity.CalibrationStep.ACCELEROMETER_LEFT -> "Place device on LEFT side"
        CalibrationActivity.CalibrationStep.ACCELEROMETER_RIGHT -> "Place device on RIGHT side"
        CalibrationActivity.CalibrationStep.ACCELEROMETER_TOP -> "Place device standing on TOP edge"
        CalibrationActivity.CalibrationStep.ACCELEROMETER_BOTTOM -> "Place device standing on BOTTOM edge"
        else -> "Follow the instruction"
    }
}