package com.airmouse.presentation.calibration

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
    private val madgwick = MadgwickAHRS(beta = 0.1f)

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

    private var currentRoll = 0f
    private var currentPitch = 0f
    private var currentYaw = 0f
    private var isCollecting = false
    private val gyroSamples = mutableListOf<Triple<Float, Float, Float>>()
    private val magSamples = mutableListOf<Triple<Float, Float, Float>>()

    enum class CalibStep {
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
    private var currentStep by mutableStateOf(CalibStep.GYROSCOPE)
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
                        val ax = (event.values[0] - accelOffsetX) / accelScaleX
                        val ay = (event.values[1] - accelOffsetY) / accelScaleY
                        val az = (event.values[2] - accelOffsetZ) / accelScaleZ
                        madgwick.updateAccel(ax, ay, az, 0.01f)
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

            isCollecting = false
            currentStep = CalibStep.ACCELEROMETER_FLAT
            vibrate(200)
        }
    }

    private fun startAccelCalibration() {
        lifecycleScope.launch {
            accelMeasurements.clear()
            for (index in accelOrientations.indices) {
                currentStep = when (index) {
                    0 -> CalibStep.ACCELEROMETER_FLAT
                    1 -> CalibStep.ACCELEROMETER_BACK
                    2 -> CalibStep.ACCELEROMETER_LEFT
                    3 -> CalibStep.ACCELEROMETER_RIGHT
                    4 -> CalibStep.ACCELEROMETER_TOP
                    else -> CalibStep.ACCELEROMETER_BOTTOM
                }
                accelStep = index
                delay(3000)
                val samples = collectAccelSamples()
                if (samples != null) {
                    accelMeasurements.add(samples)
                }
                vibrate(100)
            }
            calculateAccelCalibration()
            prefs.putBoolean("calibration_complete", true)
            currentStep = CalibStep.COMPLETE
            vibrate(300)
        }
    }

    private suspend fun collectAccelSamples(): Triple<Float, Float, Float>? {
        val samples = mutableListOf<Triple<Float, Float, Float>>()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                samples.add(Triple(event.values[0], event.values[1], event.values[2]))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        delay(1000)
        sensorManager.unregisterListener(listener)
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

        prefs.saveAccelerometerParams(
            floatArrayOf(accelOffsetX, accelOffsetY, accelOffsetZ),
            floatArrayOf(accelScaleX, accelScaleY, accelScaleZ)
        )
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (currentStep) {
                    CalibrationActivity.CalibStep.GYROSCOPE -> "🎯 Gyroscope Calibration"
                    CalibrationActivity.CalibStep.MAGNETOMETER -> "🧭 Magnetometer Calibration"
                    CalibrationActivity.CalibStep.COMPLETE -> "✅ Calibration Complete!"
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
                    CalibrationActivity.CalibStep.GYROSCOPE -> "Place your device on a flat, stationary surface"
                    CalibrationActivity.CalibStep.MAGNETOMETER -> "Move your device in a figure-8 pattern"
                    CalibrationActivity.CalibStep.COMPLETE -> "Your device is now calibrated!"
                    else -> getAccelInstruction(currentStep)
                },
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Phone3DVisualization(roll = currentRoll, pitch = currentPitch, step = currentStep)

            Spacer(modifier = Modifier.height(48.dp))

            when (currentStep) {
                CalibrationActivity.CalibStep.GYROSCOPE -> {
                    CircularProgressIndicator(modifier = Modifier.size(60.dp), color = Color(0xFF6366F1))
                }
                CalibrationActivity.CalibStep.MAGNETOMETER -> {
                    Text("∞", fontSize = 40.sp, color = Color(0xFF8B5CF6))
                }
                CalibrationActivity.CalibStep.COMPLETE -> {
                    Text("✓", fontSize = 48.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
                else -> {
                    LinearProgressIndicator(
                        progress = { (accelStep + 1).toFloat() / totalAccelSteps },
                        modifier = Modifier.fillMaxWidth(0.7f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = when (currentStep) {
                    CalibrationActivity.CalibStep.GYROSCOPE -> onStartGyro
                    CalibrationActivity.CalibStep.MAGNETOMETER -> onStartMag
                    CalibrationActivity.CalibStep.COMPLETE -> onComplete
                    else -> onStartAccel
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (currentStep == CalibrationActivity.CalibStep.COMPLETE) "Start" else "Next",
                    fontSize = 16.sp
                )
            }

            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip", color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun Phone3DVisualization(roll: Float, pitch: Float, step: CalibrationActivity.CalibStep) {
    val normalizedRoll = (roll / 45f).coerceIn(-1f, 1f)
    val normalizedPitch = (pitch / 45f).coerceIn(-1f, 1f)

    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                rotationX = -normalizedPitch * 30f
                rotationY = normalizedRoll * 30f
                shadowElevation = 20f
            }
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        val arrowRotation = when (step) {
            CalibrationActivity.CalibStep.ACCELEROMETER_BACK -> 180f
            CalibrationActivity.CalibStep.ACCELEROMETER_LEFT -> -90f
            CalibrationActivity.CalibStep.ACCELEROMETER_RIGHT -> 90f
            CalibrationActivity.CalibStep.ACCELEROMETER_BOTTOM -> 180f
            else -> 0f
        }
        Text("⬆️", fontSize = 48.sp, modifier = Modifier.rotate(arrowRotation))
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
        else -> "Follow instruction"
    }
}
