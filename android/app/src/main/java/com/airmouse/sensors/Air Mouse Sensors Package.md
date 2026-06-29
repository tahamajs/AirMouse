# 📘 Air Mouse Sensors Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.sensors` package is the **core sensor processing system** of the Air Mouse application. It handles all sensor data collection, calibration, fusion, gesture detection, and motion analysis. This package is responsible for transforming raw sensor readings into meaningful cursor movements and gestures.

```
com.airmouse.sensors/
├── SensorService.kt                 # Main sensor service
├── CalibrationHelper.kt             # Sensor calibration (gyro, accel, mag)
├── EnhancedGestureDetector.kt       # Advanced gesture detection
├── MadgwickAHRS.kt                  # Sensor fusion algorithm
├── MotionAnalyzer.kt                # Motion feature analysis
├── MotionDetector.kt                # Basic motion detection
├── OrientationTracker.kt            # Orientation tracking
├── SensorDataLogger.kt              # CSV logging for debugging
├── SensorDataProcessor.kt           # Signal processing filters
├── SensorFusion.kt                  # Static fusion utilities
├── SensorManagerHelper.kt           # Sensor availability checks
└── GestureDetector.kt               # Legacy gesture detector
```

---

## 🏗️ Architecture Overview

### Sensor Processing Pipeline

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SENSOR PROCESSING PIPELINE                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    RAW SENSOR DATA                               │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │ Gyroscope   │  │  Accelerom- │  │   Magnetometer          │ │   │
│  │  │ (rad/s)     │  │  eter (m/s²)│  │   (μT)                  │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│                               ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    CALIBRATION                                   │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │  CalibrationHelper                                       │   │   │
│  │  │  • Gyro bias removal                                     │   │   │
│  │  │  • Accel offset & scale                                  │   │   │
│  │  │  • Mag hard-iron correction                              │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│                               ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    SENSOR FUSION                                 │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │  MadgwickAHRS                                            │   │   │
│  │  │  • Quaternion-based fusion                               │   │   │
│  │  │  • Gyro integration + accel/mag correction               │   │   │
│  │  │  • Output: Roll, Pitch, Yaw                             │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│                               ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    GESTURE DETECTION                             │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │  EnhancedGestureDetector                                 │   │   │
│  │  │  • Click detection (gyro spike)                         │   │   │
│  │  │  • Double-click detection                               │   │   │
│  │  │  • Right-click detection (roll tilt)                    │   │   │
│  │  │  • Scroll detection (accel movement)                    │   │   │
│  │  │  • Swipe detection                                      │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│                               ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    OUTPUT                                        │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │  Orientation → Cursor Movement                          │   │   │
│  │  │  Gestures → Click/Scroll Commands                       │   │   │
│  │  │  Motion Data → Analytics                                │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📱 1. SensorService

### Purpose
The **central sensor manager** that registers listeners, collects raw data, applies calibration, runs sensor fusion, detects gestures, and provides callbacks to the UI.

### Key Features

| Feature | Description |
|---------|-------------|
| **Sensor Registration** | Registers gyroscope, accelerometer, magnetometer, rotation vector |
| **Calibration** | Applies calibration corrections via CalibrationHelper |
| **Sensor Fusion** | Runs Madgwick AHRS for drift-free orientation |
| **Gesture Detection** | Detects gestures via EnhancedGestureDetector |
| **Stability Detection** | Monitors device stability for idle detection |
| **Power Management** | Adjustable sampling rate for battery saving |
| **Reactive Callbacks** | Provides callbacks for orientation, gestures, and sensor data |

### Key Properties

```kotlin
// Sensor references
private var gyroscope: Sensor? = null
private var accelerometer: Sensor? = null
private var magnetometer: Sensor? = null
private var rotationVector: Sensor? = null

// Fusion engine
private val madgwick = MadgwickAHRS(beta = 0.1f)

// Calibrated values
private var calibratedGyroX = 0f
private var calibratedGyroY = 0f
private var calibratedGyroZ = 0f
private var calibratedAccelX = 0f
private var calibratedAccelY = 0f
private var calibratedAccelZ = 0f
private var calibratedMagX = 0f
private var calibratedMagY = 0f
private var calibratedMagZ = 0f

// Orientation
private var yaw = 0f
private var pitch = 0f
private var roll = 0f

// Filtered orientation (smoothed)
private var filteredRoll = 0f
private var filteredYaw = 0f
private val smoothingAlpha = 0.6f
```

### Callbacks

```kotlin
var onOrientationChanged: ((roll: Float, yaw: Float) -> Unit)? = null
var onGestureDetected: ((EnhancedGestureDetector.Gesture) -> Unit)? = null
var onStabilityChanged: ((isStable: Boolean) -> Unit)? = null
var onSensorData: ((gyroX: Float, gyroY: Float, gyroZ: Float, 
                     accelX: Float, accelY: Float, accelZ: Float) -> Unit)? = null
```

### Key Methods

| Method | Description |
|--------|-------------|
| `start()` | Register sensors and start processing |
| `stop()` | Unregister sensors and stop processing |
| `setSamplingRate(delay)` | Change sensor sampling rate |
| `recalibrate()` | Reset calibration |
| `destroy()` | Clean up resources |
| `getCurrentOrientation()` | Get current roll/yaw |
| `isStable()` | Check if device is stable |
| `isCalibrated()` | Check if sensors are calibrated |

### Sensor Processing Flow

```kotlin
override fun onSensorChanged(event: SensorEvent) {
    val currentTime = System.currentTimeMillis()
    val dt = if (lastTimestamp > 0) (currentTime - lastTimestamp) / 1000f else 0.01f
    lastTimestamp = currentTime

    when (event.sensor.type) {
        Sensor.TYPE_GYROSCOPE -> {
            // Apply calibration
            calibratedGyroX = calibrationHelper.correctGyro(values[0], 0)
            calibratedGyroY = calibrationHelper.correctGyro(values[1], 1)
            calibratedGyroZ = calibrationHelper.correctGyro(values[2], 2)
        }
        Sensor.TYPE_ACCELEROMETER -> {
            val corrected = calibrationHelper.correctAccelerometer(values[0], values[1], values[2])
            calibratedAccelX = corrected.first
            calibratedAccelY = corrected.second
            calibratedAccelZ = corrected.third
        }
        Sensor.TYPE_MAGNETIC_FIELD -> {
            val corrected = calibrationHelper.correctMagnetometer(values[0], values[1], values[2])
            calibratedMagX = corrected.first
            calibratedMagY = corrected.second
            calibratedMagZ = corrected.third
        }
    }

    // Run fusion
    madgwick.update(
        calibratedGyroX, calibratedGyroY, calibratedGyroZ,
        calibratedAccelX, calibratedAccelY, calibratedAccelZ,
        calibratedMagX, calibratedMagY, calibratedMagZ,
        dt
    )

    // Get orientation
    roll = madgwick.getRoll()
    yaw = madgwick.getYaw()

    // Apply smoothing
    filteredRoll = smoothingAlpha * roll + (1 - smoothingAlpha) * filteredRoll
    filteredYaw = smoothingAlpha * yaw + (1 - smoothingAlpha) * filteredYaw

    // Detect gestures
    detectGestures()

    // Callbacks
    onOrientationChanged?.invoke(filteredRoll, filteredYaw)
}
```

---

## 🔧 2. CalibrationHelper

### Purpose
Removes **systematic errors** from raw sensor readings through calibration procedures.

### Calibration Types

| Type | Method | Description |
|------|--------|-------------|
| **Gyroscope Bias** | Stationary average | Collects 500 samples while stationary, averages them for bias |
| **Accelerometer 6-Point** | Six orientations | Measures gravity in ±X, ±Y, ±Z to calculate offset and scale |
| **Magnetometer Hard-Iron** | Figure-8 motion | Records min/max for each axis to calculate offset and scale |

### Key Constants

```kotlin
companion object {
    private const val GYRO_SAMPLES_REQUIRED = 1000
    private const val GYRO_STABILITY_THRESHOLD = 0.05f
    private const val MAG_SAMPLES_REQUIRED = 500
    private const val MAG_CALIBRATION_DURATION_MS = 10000L
    private const val ACCEL_SAMPLES_PER_ORIENTATION = 100
    private const val GRAVITY_EARTH = 9.81f
    private const val ACCEL_TOLERANCE = 0.2f
}
```

### Calibration Data

```kotlin
// Gyroscope
private var gyroBiasX = 0f
private var gyroBiasY = 0f
private var gyroBiasZ = 0f
private var gyroVarianceX = 0f
private var gyroVarianceY = 0f
private var gyroVarianceZ = 0f

// Accelerometer
private var accelOffsetX = 0f
private var accelOffsetY = 0f
private var accelOffsetZ = 0f
private var accelScaleX = 1f
private var accelScaleY = 1f
private var accelScaleZ = 1f

// Magnetometer
private var magOffsetX = 0f
private var magOffsetY = 0f
private var magOffsetZ = 0f
private var magScaleX = 1f
private var magScaleY = 1f
private var magScaleZ = 1f
```

### Key Methods

| Method | Description |
|--------|-------------|
| `calibrateGyroscope()` | Collects stationary samples, calculates bias |
| `calibrateMagnetometer()` | Collects samples during figure-8 motion |
| `calibrateAccelerometer()` | Six-position calibration |
| `correctGyro(x, y, z)` | Applies gyro bias correction |
| `correctAccelerometer(x, y, z)` | Applies accel offset/scale correction |
| `correctMagnetometer(x, y, z)` | Applies mag offset/scale correction |
| `getCalibrationStats()` | Returns calibration statistics |
| `resetCalibration()` | Resets all calibration data |
| `isCalibrated()` | Checks if calibration exists |

### Gyroscope Calibration

```kotlin
suspend fun calibrateGyroscope(): Boolean = suspendCoroutine { continuation ->
    gyroSamples.clear()
    val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (gyroSamples.size < GYRO_SAMPLES_REQUIRED) {
                gyroSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
            } else {
                sensorManager?.unregisterListener(this)
                calculateGyroBias()
                continuation.resume(true)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    
    sensorManager?.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
}

private fun calculateGyroBias() {
    val n = gyroSamples.size
    gyroBiasX = gyroSamples.map { it.first }.average().toFloat()
    gyroBiasY = gyroSamples.map { it.second }.average().toFloat()
    gyroBiasZ = gyroSamples.map { it.third }.average().toFloat()
    
    // Calculate variance for quality assessment
    gyroVarianceX = gyroSamples.map { (it.first - gyroBiasX) * (it.first - gyroBiasX) }.average().toFloat()
    // ... etc
}
```

### Quality Assessment

```kotlin
fun getCalibrationStats(): Map<String, Any> {
    val avgVariance = (gyroVarianceX + gyroVarianceY + gyroVarianceZ) / 3f
    val quality = when {
        avgVariance < 0.01f -> "EXCELLENT"
        avgVariance < 0.05f -> "GOOD"
        avgVariance < 0.1f -> "FAIR"
        else -> "POOR"
    }
    return mapOf(
        "gyro_bias_x" to gyroBiasX,
        "gyro_bias_y" to gyroBiasY,
        "gyro_bias_z" to gyroBiasZ,
        "calibration_quality" to quality
    )
}
```

---

## ✋ 3. EnhancedGestureDetector

### Purpose
Detects **user gestures** from sensor data with configurable thresholds and haptic feedback.

### Gestures Supported

| Gesture | Detection Method | Default Threshold |
|---------|------------------|-------------------|
| **Single Click** | Gyro Y angular speed > threshold | 5 rad/s |
| **Double Click** | Two clicks within interval | 400ms interval |
| **Right Click** | Roll tilt > threshold for duration | 45° for 500ms |
| **Scroll Up/Down** | Accel Y acceleration > threshold | 8 m/s² |
| **Swipe Left/Right** | Gyro Z angular speed > threshold | 15 rad/s |
| **Swipe Up/Down** | Gyro X angular speed > threshold | 15 rad/s |

### Key Properties

```kotlin
// Thresholds
private var clickSpeedThreshold: Float = 8f
private var doubleClickMaxInterval: Long = 400L
private var scrollSpeedThreshold: Float = 6f
private var scrollDebounceThreshold: Float = 2f
private var rightClickTiltThreshold: Float = 45f
private var rightClickDurationMs: Long = 500L
private var swipeThreshold: Float = 15f

// State
private var lastClickTime = 0L
private var potentialDoubleClick = false
private var rightClickStartTime = 0L
private var rightClickTriggered = false
private var scrollInProgress = false
```

### Click Detection Logic

```kotlin
fun detect(gyroY: Float, accelY: Float, roll: Float): Gesture {
    val now = System.currentTimeMillis()
    val angularSpeed = abs(gyroY)

    // Single/Double Click Detection
    if (angularSpeed > clickSpeedThreshold && now - lastClickTime > doubleClickMaxInterval) {
        lastClickTime = now
        
        if (potentialDoubleClick) {
            // Double click detected
            potentialDoubleClick = false
            vibrate(50)
            onGestureDetected?.invoke(Gesture.DOUBLE_CLICK)
            return Gesture.DOUBLE_CLICK
        } else {
            // Single click detected
            potentialDoubleClick = true
            handler.postDelayed({
                if (potentialDoubleClick) {
                    potentialDoubleClick = false
                    vibrate(30)
                    onGestureDetected?.invoke(Gesture.CLICK)
                }
            }, doubleClickMaxInterval)
            vibrate(30)
            onGestureDetected?.invoke(Gesture.CLICK)
            return Gesture.CLICK
        }
    }

    // Right Click Detection
    if (abs(roll) > rightClickTiltThreshold && !rightClickTriggered) {
        if (rightClickStartTime == 0L) {
            rightClickStartTime = now
        } else if (now - rightClickStartTime > rightClickDurationMs) {
            rightClickTriggered = true
            vibrate(80)
            onGestureDetected?.invoke(Gesture.RIGHT_CLICK)
            return Gesture.RIGHT_CLICK
        }
    } else {
        rightClickStartTime = 0L
        rightClickTriggered = false
    }

    // Scroll Detection
    val speed = abs(accelY)
    if (speed > scrollSpeedThreshold && !scrollInProgress) {
        scrollInProgress = true
        vibrate(20)
        val gesture = if (accelY > 0) Gesture.SCROLL_DOWN else Gesture.SCROLL_UP
        onGestureDetected?.invoke(gesture)
        return gesture
    } else if (speed < scrollDebounceThreshold) {
        scrollInProgress = false
    }

    return Gesture.NONE
}
```

---

## 🧮 4. MadgwickAHRS

### Purpose
Implements the **Madgwick sensor fusion algorithm** to combine gyroscope, accelerometer, and magnetometer data into a stable quaternion representation of device orientation.

### Algorithm Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    MADGWICK AHRS ALGORITHM                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. Gyroscope Integration (Prediction)                                 │
│     q_dot = 0.5 * q ⊗ ω                                               │
│     q = q + q_dot * dt                                                │
│                                                                         │
│  2. Accelerometer Correction (Gravity Vector)                          │
│     f(q, a) = q * g * q⁻¹ - a                                        │
│     J = ∂f/∂q                                                         │
│     Δq = J⁻¹ * f                                                      │
│     q = q - β * Δq * dt                                              │
│                                                                         │
│  3. Magnetometer Correction (Magnetic Vector)                          │
│     f(q, m) = q * h * q⁻¹ - m                                        │
│     J = ∂f/∂q                                                         │
│     Δq = J⁻¹ * f                                                      │
│     q = q - β * Δq * dt                                              │
│                                                                         │
│  4. Normalize Quaternion                                               │
│     q = q / ||q||                                                     │
│                                                                         │
│  5. Convert to Euler Angles                                            │
│     roll = atan2(2(q0*q1 + q2*q3), 1 - 2(q1² + q2²))                │
│     pitch = asin(2(q0*q2 - q3*q1))                                    │
│     yaw = atan2(2(q0*q3 + q1*q2), 1 - 2(q2² + q3²))                 │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Key Properties

```kotlin
// Quaternion (q0 = w, q1 = x, q2 = y, q3 = z)
private val quaternion = FloatArray(4).apply {
    this[0] = 1f  // w
    this[1] = 0f  // x
    this[2] = 0f  // y
    this[3] = 0f  // z
}

// Filter parameters
private var beta = 0.1f   // Accelerometer/magnetometer trust (0.01-0.2)
private var zeta = 0.0f   // Gyro drift correction (0-0.1)

// Euler angles
private var yaw = 0f
private var pitch = 0f
private var roll = 0f
```

### Key Methods

| Method | Description |
|--------|-------------|
| `updateGyro(gx, gy, gz, dt)` | Update from gyroscope only |
| `updateAccel(ax, ay, az, dt)` | Update from accelerometer only |
| `updateMag(mx, my, mz, dt)` | Update from magnetometer only |
| `update(gx, gy, gz, ax, ay, az, mx, my, mz, dt)` | Full update from all sensors |
| `updateImu(gx, gy, gz, ax, ay, az, dt)` | Update from gyro + accel (IMU) |
| `getRoll()`, `getPitch()`, `getYaw()` | Get Euler angles in degrees |
| `getQuaternion()` | Get the quaternion |
| `getRotationMatrix()` | Get the rotation matrix |
| `reset()` | Reset quaternion to identity |
| `setBeta(beta)` | Set filter gain |

---

## 📊 5. MotionAnalyzer

### Purpose
Provides **advanced motion analysis** including feature extraction, activity detection, and pattern recognition.

### Key Features

| Feature | Description |
|---------|-------------|
| **Motion Features** | Acceleration, jerk, rotation speed, stability |
| **Activity Detection** | IDLE, WALKING, RUNNING, GESTURE, SHAKING, ROTATING |
| **Stationary Detection** | Detects when device is stationary |
| **Sudden Movement** | Detects sudden movements |
| **Shaking Detection** | Detects shaking patterns |
| **Rotation Detection** | Detects rotation gestures |

### Motion Features

```kotlin
data class MotionFeatures(
    val acceleration: Float,
    val jerk: Float,
    val rotationSpeed: Float,
    val direction: Float,
    val stability: Float,
    val isMoving: Boolean,
    val dominantAxis: String
)
```

### Activity Detection

```kotlin
fun detectActivity(features: MotionFeatures, timestamp: Long): Activity {
    val newActivity = when {
        features.acceleration < idleThreshold -> ActivityType.IDLE
        features.acceleration in idleThreshold..walkingThreshold -> ActivityType.WALKING
        features.acceleration in walkingThreshold..runningThreshold -> ActivityType.RUNNING
        features.jerk > gestureThreshold -> ActivityType.GESTURE
        features.rotationSpeed > 10f -> ActivityType.ROTATING
        else -> currentActivity
    }
    return Activity(newActivity, confidence, duration)
}
```

---

## 🔄 6. SensorDataProcessor

### Purpose
Provides **signal processing filters** for cleaning and analyzing sensor data.

### Filters Available

| Filter | Description | Use Case |
|--------|-------------|----------|
| **LowPassFilter** | Removes high-frequency noise | Smoothing accelerometer data |
| **HighPassFilter** | Removes low-frequency drift | Removing gravity from accelerometer |
| **MovingAverageFilter** | Windowed average | Smoothing gyroscope data |
| **ComplementaryFilter** | Blends gyro and accel | Orientation estimation |
| **KalmanFilter** | Optimal state estimation | Tracking position/velocity |
| **ButterworthFilter** | Frequency-based filter | Noise removal with phase preservation |

### Usage Example

```kotlin
// Low-pass filter for accelerometer smoothing
val lowPass = LowPassFilter(alpha = 0.2f)
val filtered = lowPass.filter(rawX, rawY, rawZ)

// Moving average for gyroscope
val movingAvg = MovingAverageFilter(windowSize = 10)
val smoothedGyro = movingAvg.filter(gyroX, gyroY, gyroZ)

// Kalman filter for position estimation
val kalman = KalmanFilter(processNoise = 0.01f, measurementNoise = 0.1f)
val estimated = kalman.filter(measurement)
```

---

## 📋 7. SensorManagerHelper

### Purpose
Provides **utility functions** for checking sensor availability and getting sensor information.

### Key Methods

| Method | Description |
|--------|-------------|
| `hasGyroscope()` | Check if gyroscope is available |
| `hasAccelerometer()` | Check if accelerometer is available |
| `hasMagnetometer()` | Check if magnetometer is available |
| `hasRotationVector()` | Check if rotation vector is available |
| `getAllSensors()` | Get list of all available sensors |
| `getRecommendedSensors()` | Get recommended sensor list |
| `getBestOrientationSensor()` | Get the best orientation sensor |
| `getSensorQualityScore()` | Get device sensor quality score |

---

## 📊 Sensor Package Summary

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **SensorService** | Central sensor manager | Registration, fusion, callbacks |
| **CalibrationHelper** | Sensor calibration | Gyro bias, accel 6-point, mag hard-iron |
| **EnhancedGestureDetector** | Gesture detection | Click, double-click, right-click, scroll |
| **MadgwickAHRS** | Sensor fusion | Quaternion-based orientation |
| **MotionAnalyzer** | Motion analysis | Feature extraction, activity detection |
| **SensorDataProcessor** | Signal processing | Filters (low-pass, Kalman, etc.) |
| **SensorManagerHelper** | Sensor utilities | Availability checks, recommendations |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Real-time Processing** | All sensors processed on dedicated thread |
| **Calibration** | Systematic errors removed before fusion |
| **Sensor Fusion** | Madgwick AHRS for drift-free orientation |
| **Gesture Detection** | Configurable thresholds with haptic feedback |
| **Power Efficiency** | Adjustable sampling rate |
| **Modularity** | Separate components for each concern |
| **Testability** | Each component can be tested independently |

---

**The Sensors Package provides a complete, production-ready sensor processing pipeline for the Air Mouse application, handling everything from raw data collection to gesture detection and motion analysis.**