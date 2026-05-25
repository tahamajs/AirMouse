# Air Mouse – Complete Sensor & Motion Processing Guide

This document provides a **complete technical explanation** of all sensor-related Kotlin files in the Air Mouse Android application. It covers calibration, sensor fusion (Madgwick AHRS), gesture detection (click, double‑click, right‑click, scroll), data logging, preferences, and battery saving.

---

## 📁 Package Structure

```
com/airmouse/
├── sensors/
│   ├── SensorService.kt           # Main service – sensor registration, fusion, callbacks
│   ├── CalibrationHelper.kt       # Gyro bias, magnetometer hard‑iron, accelerometer 6‑point calibration
│   ├── EnhancedGestureDetector.kt # Advanced gestures: single/double click, right‑click, scroll
│   ├── MadgwickAHRS.kt            # Madgwick sensor fusion algorithm (quaternion based)
│   ├── MotionDetector.kt          # Simple gesture detection (legacy, kept for reference)
│   └── SensorDataLogger.kt        # CSV logging of raw sensor data for debugging
├── utils/
│   ├── PreferencesManager.kt      # Persistent storage of sensitivity, thresholds, haptic, IP
│   └── BatterySaver.kt            # Reduces sampling rate when phone is stationary
└── ui/ ... (other UI files)
```

---

## 1. `SensorService.kt` – Core Sensor Manager

### Purpose
`SensorService` is the central hub that:
- Registers listeners for accelerometer, gyroscope, and magnetometer.
- Applies calibration corrections (from `CalibrationHelper`).
- Runs the Madgwick fusion (from `MadgwickAHRS`) to compute absolute orientation (roll, yaw).
- Detects gestures via `EnhancedGestureDetector`.
- Provides callbacks to `MainActivity` for orientation changes, gestures, and debug values.

### Key Properties
- `madgwick`: Instance of `MadgwickAHRS` with `beta = 0.1f` (trust in accelerometer/magnetometer).
- `orientationCallback`: `(roll, yaw) -> Unit` – sends fused angles to UI for cursor movement.
- `gestureCallback`: `(Gesture) -> Unit` – notifies UI of click/double‑click/right‑click/scroll.
- `gyroUpdateCallback` / `accelUpdateCallback`: Used only by `DebugOverlay` for real‑time monitoring.
- `isRunning`: Prevents processing after `stop()`.

### Important Methods
| Method | Description |
|--------|-------------|
| `start()` | Initialises `SensorManager`, gets default sensors, registers listeners with `SENSOR_DELAY_GAME` (50 Hz) or lower if battery saver is active. |
| `stop()` | Unregisters all listeners. |
| `setSamplingRate(delay)` | Dynamically changes sensor sampling rate (used by `BatterySaver`). |
| `onSensorChanged(event)` | Called by Android for each sensor sample. Corrects raw values, updates Madgwick, detects gestures, and invokes callbacks. |
| Callback setters | `setOnOrientationChange`, `setOnGestureDetected`, `setOnGyroUpdate`, `setOnAccelUpdate`. |

### Gesture Integration
- **Scroll**: Detected from accelerometer Y‑axis linear acceleration via `EnhancedGestureDetector.detectScroll()`.
- **Click / double‑click**: Detected from gyroscope Y‑axis angular velocity via `EnhancedGestureDetector.detectClick()`.
- **Right‑click**: Detected from roll angle (from Madgwick) via `EnhancedGestureDetector.detectRightClick()`.

### Time Calculation
- `dt = (currentTime - lastTimestamp) / 1000f` – passed to Madgwick for gyro integration.
- First call uses `0.01f` as default delta.

---

## 2. `CalibrationHelper.kt` – Sensor Calibration

### Purpose
Calibration removes systematic errors from raw sensor readings.

### Calibration Types

#### Gyroscope Bias Removal
- **Method**: Collect 500 samples while phone is stationary. Average them → bias.
- **Correction**: `correctGyro(value, axis) = raw - bias[axis]`.

#### Accelerometer 6‑Point Calibration
- **Why**: Factory offset and scale errors cause incorrect tilt.
- **Method**: Place phone in six orientations: ±X, ±Y, ±Z aligned with gravity (9.81 m/s²). For each orientation, average 100 samples. Solve for offset and scale per axis using two equations (positive and negative gravity).
- **Correction**: `correctAccelerometer(x,y,z) = (raw - offset) / scale`.

#### Magnetometer Hard‑Iron Calibration
- **Why**: Nearby magnets (speakers, metal) add constant offset to each axis.
- **Method**: Rotate phone in all orientations (figure‑8) for 30 seconds, record min and max per axis.  
  `offset = (max + min) / 2`, `scale = (max - min) / 2`.
- **Correction**: `correctMagnetometer(x,y,z) = (raw - offset) / scale`.

### Key Functions
| Function | Description |
|----------|-------------|
| `suspend fun calibrateGyro()` | Blocks until 500 samples, computes bias. |
| `suspend fun calibrateMagnetometer(durationMs)` | Records min/max for given duration, computes offset/scale. |
| `suspend fun calibrateAccelerometer()` | Implements full 6‑point calibration (user must place phone in 6 orientations – in a real app you would show instructions). |
| Correction functions | `correctGyro`, `correctAccelerometer`, `correctMagnetometer`. |

---

## 3. `EnhancedGestureDetector.kt` – Advanced Gestures

### Purpose
Detects user gestures from sensor values with configurable thresholds (stored in `PreferencesManager`).

### Gestures Supported
| Gesture | Detection Method | Parameters |
|---------|------------------|------------|
| **Single click** | Gyro Y angular speed > `clickSpeedThreshold`, no second click within `doubleClickMaxInterval`. | `clickSpeedThreshold` (default 5 rad/s), `doubleClickMaxInterval` (400 ms) |
| **Double click** | Two quick flicks within `doubleClickMaxInterval`. | Same thresholds, plus cooldown. |
| **Right click** | Roll (tilt) absolute value > `rightClickTiltThreshold` for longer than `rightClickDurationMs`. | `rightClickTiltThreshold` (45°), `rightClickDurationMs` (500 ms) |
| **Scroll up/down** | Accelerometer Y linear acceleration > `scrollSpeedThreshold`, debounced until speed falls below `scrollDebounceThreshold`. | `scrollSpeedThreshold` (8 m/s²), `scrollDebounceThreshold` (2 m/s²) |

### Double‑Click Logic
- When a flick is detected, a timer starts (`doubleClickMaxInterval`).
- If another flick occurs before timer expires → double click.
- If timer expires → single click.
- Haptic feedback with different durations: 50 ms for double click, 30 ms for single click, 80 ms for right click.

### Reloading Thresholds
- `reloadThresholds()` reads current preferences. Called after settings dialog closes.

### Key Methods
- `detectClick(gyroY, dt): Boolean` – returns true if a click or double‑click should be signalled (caller must differentiate using a separate callback – in `SensorService` we use the returned boolean to trigger the appropriate `Gesture` enum).
- `detectRightClick(roll, dt): Boolean`
- `detectScroll(accelY, dt): Int` – returns 1 (down), -1 (up), or 0.

---

## 4. `MadgwickAHRS.kt` – Sensor Fusion Algorithm

### Purpose
Combines gyroscope (fast, drift‑prone), accelerometer (absolute gravity reference), and magnetometer (absolute yaw reference) into a stable quaternion representing device orientation.

### Algorithm Overview
1. **Predict**: Integrate gyroscope angular velocities to update quaternion.
   ```
   q_dot = 0.5 * q ⊗ ω
   q = q + q_dot * dt
   ```
2. **Correct**: Use gradient descent to minimise error between measured gravity/magnetic field and predicted orientation.
   - Compute error vector `f(q, a, m)`.
   - Compute Jacobian `J`.
   - Compute step `s = (J^T * f) / (J^T * J)`.
   - Adjust quaternion by `beta * s * dt`.
3. **Normalise**: Ensure quaternion is unit length.

### Parameters
- `beta`: Filter gain (default 0.1). Higher values trust accelerometer/magnetometer more (less drift, more noise).

### Exposed Angles
- `getRoll()`: Rotation around X‑axis (vertical cursor movement).
- `getPitch()`: Rotation around Y‑axis (not used directly).
- `getYaw()`: Rotation around Z‑axis (horizontal cursor movement).

### Key Methods
- `updateGyro(gx, gy, gz, dt)`: Call every time a new gyro sample arrives.
- `updateAccel(ax, ay, az)`: Stores latest accelerometer values for correction.
- `updateMag(mx, my, mz)`: Stores latest magnetometer values for correction.

---

## 5. `MotionDetector.kt` (Legacy)

### Purpose
Simpler gesture detection (only single click and scroll) – kept for reference or as a fallback. Not used by the enhanced `MainActivity`.

### Differences from `EnhancedGestureDetector`
- No double‑click, no right‑click.
- Fixed thresholds (not configurable).
- No haptic feedback.

---

## 6. `SensorDataLogger.kt` – Debugging Tool

### Purpose
Logs raw sensor data (accelerometer, gyroscope, magnetometer) to a CSV file on external storage for offline analysis.

### Usage
- `startLogging()`: Registers listeners at `SENSOR_DELAY_FASTEST` (200 Hz), creates CSV file with headers.
- `stopLogging()`: Unregisters, closes file.
- File location: `getExternalFilesDir(null)/sensor_log_<timestamp>.csv`.

### CSV Format
```
timestamp,type,x,y,z
1700000000000,1,0.12,-0.05,9.81
1700000000020,4,0.001,0.002,-0.001
...
```
Sensor type codes: 1 = accelerometer, 4 = gyroscope, 2 = magnetometer.

---

## 7. `PreferencesManager.kt` – Persistent Settings

### Purpose
Stores user‑configurable parameters using Android `SharedPreferences`. All settings survive app restarts.

### Stored Keys
| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `sensitivity` | Float | 0.5 | Cursor speed multiplier (0.2–2.0) |
| `click_threshold` | Float | 5.0 | Angular speed (rad/s) to trigger click |
| `double_click_interval` | Long | 400 | Max time between two flicks (ms) |
| `scroll_threshold` | Float | 8.0 | Linear acceleration (m/s²) to trigger scroll |
| `scroll_debounce` | Float | 2.0 | Acceleration below which scroll resets |
| `rightclick_tilt` | Float | 45.0 | Degrees of tilt to start right‑click |
| `rightclick_duration` | Long | 500 | How long tilt must be held (ms) |
| `haptic_enabled` | Boolean | true | Enable vibration feedback |
| `last_ip` | String | "" | Last used laptop IP address |

### Methods
- Getters: `getSensitivity()`, `getClickThreshold()`, etc.
- Setters: `setSensitivity(value)`, `setClickThreshold(value)`, etc.
- All operations are synchronous (using `apply()` for performance).

---

## 8. `BatterySaver.kt` – Adaptive Sampling Rate

### Purpose
Reduces sensor sampling rate from 50 Hz (game mode) to 20 Hz (normal) when the phone has been stationary for 10 seconds, saving battery.

### How It Works
- Monitors changes in roll and yaw (orientation).
- If the absolute change is small for `idleThresholdMs` (10 sec), calls `setSamplingRate(SENSOR_DELAY_NORMAL)` on `SensorService`.
- When movement resumes, restores `SENSOR_DELAY_GAME`.

### Methods
- `start(service)`: Begins monitoring.
- `stop()`: Restores normal rate and stops.
- `updateMovement(roll, yaw)`: Called from `SensorService` on every orientation update.
- `isLowPowerMode()`: Query current state.

---

## 🧪 Integration with `MainActivity`

The `MainActivity` (provided in a separate document) uses these classes as follows:

1. **Initialisation** in `onCreate`:
   ```kotlin
   calibrationHelper = CalibrationHelper(this)
   gestureDetector = EnhancedGestureDetector(this, preferences, vibrator)
   sensorService = SensorService(this, calibrationHelper, gestureDetector, preferences, batterySaver)
   debugOverlay.setSensorService(sensorService)
   ```

2. **Setting callbacks** before `start()`:
   ```kotlin
   sensorService.setOnOrientationChange { roll, yaw -> ... }
   sensorService.setOnGestureDetected { gesture -> ... }
   sensorService.setOnGyroUpdate { gyroY -> currentGyroY = gyroY }
   sensorService.setOnAccelUpdate { accelY -> currentAccelY = accelY }
   ```

3. **Calibration** (called from button):
   ```kotlin
   calibrationHelper.calibrateMagnetometer(30000)
   calibrationHelper.calibrateGyro()
   calibrationHelper.calibrateAccelerometer()
   ```

4. **Starting/Stopping**:
   - `sensorService.start()` / `stop()`.
   - `batterySaver.start(sensorService)` / `stop()`.

5. **Lifecycle**:
   - `onPause()` pauses sensors (battery saving).
   - `onResume()` restarts if active.

---

## 🔧 Troubleshooting & Common Issues

| Issue | Likely Cause | Solution |
|-------|--------------|----------|
| Cursor drifts when stationary | Gyro bias not removed | Re‑calibrate gyro on a flat, still surface. |
| Yaw drifts over time | Magnetometer not calibrated | Perform figure‑8 pattern for 30 seconds, away from magnets. |
| Tilt (roll) is inaccurate | Accelerometer not calibrated | Run 6‑point accelerometer calibration. |
| Double‑click not detected | `doubleClickMaxInterval` too short or `clickSpeedThreshold` too high | Increase interval in settings or lower threshold. |
| Right‑click triggers too easily | `rightClickTiltThreshold` too low | Increase to 60° or more. |
| Scroll triggers randomly | `scrollSpeedThreshold` too low, hand tremor | Increase threshold to 10–12 m/s². |
| High battery drain | Sampling rate always at 50 Hz | Enable battery saver (default on). |

---

## 📚 Further Reading

- [Madgwick AHRS original paper](https://x-io.co.uk/res/doc/madgwick_internal_report.pdf)
- Android [SensorManager documentation](https://developer.android.com/reference/android/hardware/SensorManager)
- [Hard‑iron and soft‑iron magnetometer calibration](https://en.wikipedia.org/wiki/Magnetometer#Calibration)

---

## ✅ Summary

This sensor package provides a **complete, production‑ready** motion processing pipeline for the Air Mouse:

- **Calibration** removes systematic errors from all three sensors.
- **Madgwick fusion** produces stable, drift‑free orientation.
- **Gesture detection** supports single click, double click, right click, and scroll with fully configurable thresholds.
- **Battery saver** reduces power consumption when idle.
- **Data logging** aids debugging and Perfetto analysis.

All code is modular, well‑commented, and follows Android best practices (coroutines, callbacks, lifecycle awareness).

---

*This README is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.*