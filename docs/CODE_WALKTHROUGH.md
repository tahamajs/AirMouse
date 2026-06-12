# Code Walkthrough – Complete Line‑by‑Line Guide

This document provides an **exhaustive, file‑by‑file explanation** of the Air Mouse source code. It covers the Android app (Kotlin), the PC server (Python), and the configuration/build files. Each section describes the purpose of the file, its key data structures, important methods, and how it fits into the overall system. After reading this, you will understand every line of the project.

---

## 📖 Table of Contents

1. [Android App – Kotlin](#android-app--kotlin)
   - [MainActivity.kt](#1-mainactivitykt)
   - [MainViewModel.kt](#2-mainviewmodelkt)
   - [SensorService.kt](#3-sensorservicekt)
   - [MadgwickFusion.kt](#4-madgwickfusionkt)
   - [CalibrationHelper.kt / CalibrationUseCase.kt](#5-calibrationhelperkt--calibrationusecasekt)
   - [EnhancedGestureDetector.kt / GestureDetector.kt](#6-enhancedgesturedetectorkt--gesturedetectorkt)
   - [DataSender.kt](#7-datasenderkt)
   - [AutoReconnect.kt](#8-autoreconnectkt)
   - [PreferencesDataStore.kt / PreferencesManager.kt](#9-preferencesdatastorekt--preferencesmanagerkt)
   - [SensorRepository.kt & NetworkRepository.kt](#10-sensorrepositorykt--networkrepositorykt)
   - [DebugOverlayService.kt & DebugOverlay.kt](#11-debugoverlayservicekt--debugoverlaykt)
   - [CalibrationActivity.kt & SettingsScreen.kt](#12-calibrationactivitykt--settingsscreenkt)
2. [UI Layouts (XML)](#ui-layouts-xml)
   - [activity_main.xml](#activity_mainxml)
   - [activity_calibration.xml & fragment_settings.xml](#activity_calibrationxml--fragment_settingsxml)
   - [debug_overlay.xml](#debug_overlayxml)
   - [green_square.xml (drawable)](#green_squarexml-drawable)
3. [AndroidManifest.xml](#androidmanifestxml)
4. [Gradle Build Files](#gradle-build-files)
   - [Project‑level build.gradle](#projectlevel-buildgradle)
   - [Module‑level build.gradle](#modulelevel-buildgradle)
   - [settings.gradle & gradle-wrapper.properties](#settingsgradle--gradle-wrapperproperties)
5. [PC Server – Python](#pc-server--python)
   - [gui.py – Dark Mode GUI](#gui-py--dark-mode-gui)
   - [server.py – Console Server](#server-py--console-server)
   - [run.py / run.sh / run.bat](#runpy--runsh--runbat)
   - [perfetto_analyzer.py](#perfetto_analyzepy)
6. [Build Scripts (APK without Android Studio)](#build-scripts-apk-without-android-studio)
7. [Common Code Flow](#common-code-flow)

---

# Android App – Kotlin

## 1. `MainActivity.kt`

**Location:** `com/airmouse/MainActivity.kt`

**Purpose:** This is the entry point of the Android app. It inflates the main UI, handles user interactions (calibrate, start, settings, debug), requests permissions, and coordinates the lifecycle of the Air Mouse service.

### Key Sections

#### `onCreate(savedInstanceState: Bundle?)`
- Calls `setContentView(R.layout.activity_main)`.
- Binds UI elements using `findViewById` (or `ViewBinding` in the enhanced version).
- Initialises `PreferencesManager`, `BatterySaver`, `DebugOverlay`.
- Requests permissions for `INTERNET`, `VIBRATE`, and `SYSTEM_ALERT_WINDOW`.
- Sets up the sensitivity slider:
  - Maps saved sensitivity (0.2–2.0) to SeekBar progress (0–100).
  - Listener updates the TextView and saves when user stops touching.
- Instantiates `CalibrationHelper`, `EnhancedGestureDetector`, `SensorService`, and `DebugOverlay.setSensorService`.
- Sets click listeners for all buttons.

#### `setupSensitivitySlider()`
- Reads saved sensitivity from `PreferencesManager`.
- Calculates progress: `(sensitivity - 0.2) / 1.8 * 100`.
- Sets `SeekBar` listener:
  - `onProgressChanged`: updates the displayed value.
  - `onStopTrackingTouch`: saves the new sensitivity (debounced).

#### `startCalibration()`
- Checks network availability (WiFi).
- Launches a coroutine in `lifecycleScope`:
  - Calls `calibrationHelper.calibrateMagnetometer(30000)`
  - Calls `calibrationHelper.calibrateGyro()`
  - Calls `calibrationHelper.calibrateAccelerometer()`
  - Sets `isCalibrated = true` and updates status.

#### `startAirMouse()`
- Verifies calibration and IP address.
- Checks WiFi.
- Saves IP to `PreferencesManager`.
- Creates `DataSender` and `AutoReconnect`, starts them.
- Sets callbacks on `SensorService`:
  - `setOnOrientationChange`: computes delta (dx, dy) using sensitivity, sends via `DataSender.sendMove`, updates green square rotation.
  - `setOnGyroUpdate` and `setOnAccelUpdate`: stores latest values for debug overlay.
  - `setOnGestureDetected`: sends corresponding network messages (click, double‑click, right‑click, scroll) and flashes green square.
- Starts `SensorService` and `BatterySaver`.

#### `updateUIIndicator(roll, yaw)`
- Sets the rotation of the green square to `yaw` (in degrees, converted from radians). Yaw controls horizontal orientation.

#### `flashClick()`
- Changes the background color of the green square to red, then back to green after 100 ms.

#### `showSettingsDialog()`
- Creates a `SettingsDialog` with the current `PreferencesManager` and a lambda that calls `gestureDetector.reloadThresholds()` when dismissed.

#### `isNetworkAvailable()`
- Uses `ConnectivityManager` to check if WiFi or Ethernet is connected.

#### Lifecycle methods
- `onPause()`: stops `SensorService` and `BatterySaver`.
- `onResume()`: restarts them if `isActive` is true.
- `onDestroy()`: stops all services, sender, auto‑reconnect, and hides debug overlay.

### Important Design Choices
- Uses `lifecycleScope.launch` for calibration to avoid blocking the UI.
- Sensitivity save is debounced to avoid excessive `SharedPreferences` writes.
- The `lastOrientation` is stored locally to compute relative deltas, not absolute positions, so the cursor never jumps when you start moving.

---

## 2. `MainViewModel.kt`

**Location:** `com/airmouse/ui/MainViewModel.kt`

**Purpose:** This ViewModel separates UI logic from business logic. It observes the sensor repository and updates LiveData objects that `MainActivity` observes. It also handles network communication and gesture detection.

### Key Components

#### Properties
- `prefs: PreferencesDataStore` – persistent settings.
- `sensorRepo: SensorRepository` – provides a flow of sensor data.
- `networkRepo: NetworkRepository` – sends move/click/scroll messages.
- `gestureDetector: GestureDetector` – detects gestures from sensor values.

#### LiveData exposed to UI
- `statusText: LiveData<String>` – connection/calibration status.
- `orientationYaw: LiveData<Float>` – yaw angle for green square rotation.
- `clickFlashEvent: LiveData<Boolean>` – triggers a visual flash on click.
- `toastMessage: LiveData<String?>` – one‑time messages.
- `sensitivity: LiveData<Float>` – current cursor sensitivity.
- `lastIp: LiveData<String>` – last used IP.

#### `init` block
- Collects `prefs.sensitivityFlow` and updates `_sensitivity`.
- Collects `sensorRepo.sensorEvents` (a flow of `SensorData`). For each event:
  - Computes delta: `dx = (yaw - lastYaw) * sensitivity * 0.8f`, `dy = (roll - lastRoll) * sensitivity * 0.8f`.
  - Sends `move` via `networkRepo`.
  - Calls `gestureDetector.detect(gyroY, accelY, roll)`.
  - Based on the resulting gesture, sends corresponding network messages and updates LiveData (toast, click flash).

#### Public methods
- `setSensitivity(value: Float)`: saves to `prefs`.
- `calibrate()`: runs gyro, magnetometer, and accelerometer calibration in sequence, updating status LiveData.
- `start()`: retrieves last IP, connects to server, starts sensor streaming.
- `toggleDebugOverlay()`: shows a toast (actual toggling done by `MainActivity` for permission reasons).
- `onCleared()`: disconnects network repo and stops active flag.

### Threading
- All `prefs` and `sensorRepo` calls are suspend functions, launched in `viewModelScope`.
- Sensor events are collected on a background thread; LiveData updates are automatically posted to the main thread.

---

## 3. `SensorService.kt`

**Location:** `com/airmouse/sensors/SensorService.kt`

**Purpose:** This service is the core sensor manager. It registers listeners for accelerometer, gyroscope, and magnetometer, applies calibration corrections, runs the Madgwick fusion, and invokes orientation and gesture callbacks.

### Key Properties
- `sensorManager`, `accelerometer`, `gyroscope`, `magnetometer` – Android sensor objects.
- `madgwick: MadgwickFusion` – fusion algorithm instance.
- `orientationCallback`, `gestureCallback`, `gyroUpdateCallback`, `accelUpdateCallback` – lambdas for communicating with `MainActivity` / ViewModel.
- `timestamp` – used to compute `dt` (delta time) between sensor events.

### Important Methods

#### `start()`
- Gets the `SensorManager`.
- Finds default sensors.
- Registers listeners with `SENSOR_DELAY_GAME` (20 ms period, 50 Hz).
- If battery saver is active, it may register with `SENSOR_DELAY_NORMAL` instead.

#### `stop()`
- Unregisters all listeners.

#### `setSamplingRate(delay: Int)`
- Unregisters and re‑registers with a new delay (used by `BatterySaver`).

#### `onSensorChanged(event: SensorEvent)`
- This is called by Android on the thread specified during registration (a dedicated `HandlerThread`).
- Computes `dt` using `System.currentTimeMillis()`.
- For each sensor type:
  - **Accelerometer**: corrects raw values using `calibrationHelper`, calls `madgwick.updateAccel()`. Also passes `ay` to `gestureDetector.detectScroll()` and invokes callback.
  - **Gyroscope**: corrects raw values, calls `madgwick.updateGyro(gx, gy, gz, dt)`. Passes `gy` to `gestureDetector.detectClick()`.
  - **Magnetometer**: corrects raw values, calls `madgwick.updateMag()`.
- After processing, calls `madgwick.getRoll()` and `getYaw()`, then invokes `orientationCallback`.
- Also calls `gestureDetector.detectRightClick()` (using roll) and invokes gesture callback accordingly.

### Callback Setters
- `setOnOrientationChange(callback)`
- `setOnGestureDetected(callback)`
- `setOnGyroUpdate(callback)`
- `setOnAccelUpdate(callback)`

### Thread Safety
- The service is designed to be started/stopped from the main thread, but `onSensorChanged` runs on the background `HandlerThread`. All callbacks are invoked on that same thread; therefore, they should not perform heavy UI operations directly. The ViewModel uses `postValue` to safely update LiveData.

---

## 4. `MadgwickFusion.kt`

**Location:** `com/airmouse/domain/MadgwickFusion.kt`

**Purpose:** Implements the Madgwick AHRS algorithm. It maintains a quaternion representing orientation, updates it using gyroscope data, and corrects it using accelerometer and magnetometer data.

### Key Variables
- `q0, q1, q2, q3` – quaternion components (unit quaternion).
- `ax, ay, az` – last accelerometer reading.
- `mx, my, mz` – last magnetometer reading.
- `beta` – filter gain (default 0.1).

### Key Methods

#### `updateGyro(gx: Float, gy: Float, gz: Float, dt: Float)`
- Computes the quaternion derivative from gyroscope:
  ```
  qDot1 = 0.5f * (-q1*gx - q2*gy - q3*gz)
  qDot2 = 0.5f * ( q0*gx + q2*gz - q3*gy)
  qDot3 = 0.5f * ( q0*gy - q1*gz + q3*gx)
  qDot4 = 0.5f * ( q0*gz + q1*gy - q2*gx)
  ```
- If accelerometer data is available (ax, ay, az ≠ 0), it performs gradient descent correction:
  - Normalises accelerometer reading.
  - Computes error function `f` and Jacobian `J` (closed‑form expressions from Madgwick’s paper).
  - Computes step `s = (J^T * f) / (J^T * J)`.
  - Adjusts `qDot` by `-beta * s`.
- Integrates: `q += qDot * dt`.
- Normalises the quaternion to unit length.

#### `updateAccel(x, y, z)`, `updateMag(x, y, z)`
- Store the latest values for use in the next `updateGyro` call.

#### `getRoll()`, `getPitch()`, `getYaw()`
- Convert quaternion to Euler angles (in radians):
  ```
  roll  = atan2(2*(q0*q1 + q2*q3), 1 - 2*(q1*q1 + q2*q2))
  pitch = asin(2*(q0*q2 - q3*q1))
  yaw   = atan2(2*(q0*q3 + q1*q2), 1 - 2*(q2*q2 + q3*q3))
  ```

### Performance Notes
- Each `updateGyro` call does ~100 floating‑point operations. On a modern phone, it takes less than 0.5 ms.
- The fusion runs at the same rate as gyroscope updates (50 Hz).

---

## 5. `CalibrationHelper.kt` / `CalibrationUseCase.kt`

**Location:** `com/airmouse/sensors/CalibrationHelper.kt` and `com/airmouse/domain/CalibrationUseCase.kt`

**Purpose:** These classes handle sensor calibration. `CalibrationHelper` is the wrapper used by `SensorService` and `MainActivity`. `CalibrationUseCase` contains the low‑level suspend functions that collect samples.

### `CalibrationHelper.kt` (simplified wrapper)

#### Properties
- `gyroBias`, `accelOffset`, `accelScale`, `magOffset`, `magScale` – correction parameters.

#### Methods

**`suspend fun calibrateGyro()`**
- Creates a `CalibrationUseCase` and calls `calibrateGyro()`, storing the returned bias.

**`suspend fun calibrateMagnetometer(durationMs: Long)`**
- Calls `CalibrationUseCase.calibrateMagnetometer(durationMs)`, stores offset and scale.

**`suspend fun calibrateAccelerometer()`**
- Calls `CalibrationUseCase.calibrateAccelerometerSimple()` for simplified offset.

**Correction functions**
- `correctGyro(value, axis) = value - gyroBias[axis]`
- `correctAccelerometer(x, y, z)` – applies offset and scale.
- `correctMagnetometer(x, y, z)` – applies offset and scale.

### `CalibrationUseCase.kt` (low‑level collection)

#### `suspend fun calibrateGyro(): FloatArray`
- Registers a `SensorEventListener` for `TYPE_GYROSCOPE`.
- Collects 500 samples, unregisters, averages them, and returns the bias.
- Uses `suspendCancellableCoroutine` to bridge callback to coroutine.

#### `suspend fun calibrateMagnetometer(durationMs: Long): Pair<FloatArray, FloatArray>`
- Registers listener for `TYPE_MAGNETIC_FIELD`.
- Updates min/max for each axis.
- After `durationMs` (via `Handler.postDelayed`), unregisters, computes offset and scale, and returns.

#### `suspend fun calibrateAccelerometerSimple(): Pair<FloatArray, FloatArray>`
- Collects 200 stationary samples, averages them.
- Computes offset: `offset[0] = avgX`, `offset[1] = avgY`, `offset[2] = avgZ - 9.81f`. Scale is `[1,1,1]`.

#### `suspend fun calibrateAccelerometer(measuredOrientations: List<FloatArray>)`
- Full 6‑point calibration using pre‑collected data. Solves for offset and scale per axis.

---

## 6. `EnhancedGestureDetector.kt` / `GestureDetector.kt`

**Location:** `com/airmouse/sensors/EnhancedGestureDetector.kt` (or `com/airmouse/domain/GestureDetector.kt`)

**Purpose:** These classes detect gestures from gyroscope Y, accelerometer Y, and roll angle. `EnhancedGestureDetector` includes double‑click and right‑click; the simpler `GestureDetector` may only support single click and scroll.

### `EnhancedGestureDetector.kt` – Complete Version

#### Enum
```kotlin
enum class Gesture { NONE, CLICK, DOUBLE_CLICK, RIGHT_CLICK, SCROLL_UP, SCROLL_DOWN }
```

#### State variables
- `lastClickTime`, `potentialDoubleClick`, `rightClickStartTime`, `rightClickTriggered`, `scrollInProgress`.

#### `reloadThresholds()`
- Reads all thresholds from `PreferencesManager` (click speed, double‑click interval, right‑click tilt, right‑click duration, scroll speed, scroll debounce).

#### `detectClick(gyroY, dt): Boolean`
- Checks `|gyroY| > clickSpeedThreshold` and time since last click > doubleClickInterval.
- If `potentialDoubleClick` is true → returns `true` (caller will interpret as double‑click).
- Else sets `potentialDoubleClick = true`, schedules a timer to clear it, and returns `true` (caller interprets as single click after timer? This is tricky – in `SensorService` we call `detectClick` and separately handle double‑click detection via a callback. The actual implementation may differ.)

#### `detectRightClick(roll, dt): Boolean`
- Checks `|roll| > rightClickTiltAngle`.
- Starts timer; if held for `rightClickDuration`, returns `true`.

#### `detectScroll(accelY, dt): Int`
- Returns `1` (down), `-1` (up), or `0`.
- Uses `scrollThreshold` and `scrollDebounce`.

### Simpler `GestureDetector.kt` (Domain layer)
- Similar logic but may not include double‑click or right‑click. The domain version is used by `MainViewModel` in some implementations.

---

## 7. `DataSender.kt`

**Location:** `com/airmouse/network/DataSender.kt`

**Purpose:** A background thread that maintains a TCP connection to the PC server, sends JSON messages from a queue, and handles ACKs and retransmission for critical packets.

### Key Components

#### Properties
- `socket`, `out`, `input` – TCP connection.
- `running` – controls the main loop.
- `queue: LinkedBlockingQueue<String>` – thread‑safe queue of outgoing messages.
- `pendingAcks: ConcurrentHashMap<Long, String>` – messages waiting for ACK.
- `ACK_TIMEOUT_MS = 500L`

#### Main Thread (`run()`)
- While `running`, calls `connect()` and `processLoop()`.
- Catches exceptions and retries after `RECONNECT_DELAY_MS` (5 seconds).

#### `connect()`
- Opens `Socket(host, port)`, creates `DataOutputStream` and `BufferedReader`.

#### `processLoop()`
- Polls the queue (`take()`).
- For each message, calls `sendMessage()`.

#### `sendMessage(msg: String)`
- Writes `msg` followed by newline to the socket.
- If the message is critical (contains `"click"`, `"doubleclick"`, `"rightclick"`, or `"scroll"`):
  - Extracts the `id`.
  - Puts it into `pendingAcks`.
  - Starts a timer (simplified: sleeps for 500 ms, then retransmits if still pending).

#### ACK receiver thread
- Reads lines from the socket.
- If a line contains `"ack"`, parses the `id` and removes from `pendingAcks`.

#### Public send methods
- `sendMove(dx, dy)`, `sendClick()`, `sendDoubleClick()`, `sendRightClick()`, `sendScroll(delta)`
- Each builds a JSON object and offers it to the queue.

#### `stopSending()`
- Sets `running = false`, closes socket and streams.

### Threading Model
- The `DataSender` itself is a `Thread`. It creates two additional threads: one for sending (the main loop) and one for receiving ACKs. In practice, the main loop is the thread itself; the ACK receiver is a separate thread.

---

## 8. `AutoReconnect.kt`

**Location:** `com/airmouse/network/AutoReconnect.kt`

**Purpose:** Monitors the `DataSender` connection and attempts to reconnect if it becomes disconnected. Uses a `Handler` to periodically check `dataSender.isConnected` (a flag we added).

### Key Methods

#### `start()`
- Starts a periodic runnable that checks every `CHECK_INTERVAL_MS` (5 seconds).
- If `!dataSender.isConnected`, stops the old sender and starts a new one using the saved IP.

#### `stop()`
- Cancels the runnable.

---

## 9. `PreferencesDataStore.kt` / `PreferencesManager.kt`

**Location:** `com/airmouse/data/PreferencesDataStore.kt` (modern) and `com/airmouse/utils/PreferencesManager.kt` (legacy)

**Purpose:** Persist user settings (sensitivity, gesture thresholds, haptic enabled, last IP). The DataStore version uses AndroidX DataStore (coroutine‑based). The `PreferencesManager` uses `SharedPreferences` (blocking).

### `PreferencesDataStore.kt`

#### Keys
- `SENSITIVITY`, `CLICK_THRESHOLD`, `DOUBLE_CLICK_INTERVAL`, `SCROLL_THRESHOLD`, `SCROLL_DEBOUNCE`, `RIGHT_CLICK_TILT`, `RIGHT_CLICK_DURATION`, `HAPTIC_ENABLED`, `LAST_IP`.

#### Flows
- `lastIpFlow: Flow<String>` – observable IP.

#### Suspend getters
- `suspend fun getSensitivity(): Float` – reads once.

#### Suspend setters
- `suspend fun setSensitivity(value: Float) = dataStore.edit { it[SENSITIVITY] = value }`

#### Blocking wrappers
- `setSensitivityBlocking(value)` – uses `runBlocking` for compatibility with legacy code.

### `PreferencesManager.kt` (legacy)
- Same functionality using `SharedPreferences`. Used by older parts of the code.

---

## 10. `SensorRepository.kt` & `NetworkRepository.kt`

**Location:** `com/airmouse/data/`

**Purpose:** These repositories abstract sensor and network operations.

### `SensorRepository.kt`

#### `sensorEvents: Flow<SensorData>`
- Uses `callbackFlow` to create a flow that emits `SensorData(roll, yaw, gyroY, accelY)`.
- Internally registers a `SensorEventListener` that uses a `MadgwickFusion` instance and calibration helper.
- Each time gyroscope data arrives, it sends the latest roll, yaw, gyroY, and accelY to the flow.

#### Calibration methods
- `calibrateGyro()`, `calibrateMagnetometer(durationMs)`, `calibrateAccelerometer()` – delegate to inner `CalibrationHelper`.

#### `vibrate(duration)`
- Uses `Vibrator` service.

### `NetworkRepository.kt`

- Simplified wrapper around `DataSender`. Provides `connect(ip)`, `sendMove`, `sendClick`, etc.
- In the full implementation, `MainViewModel` uses this instead of creating `DataSender` directly.

---

## 11. `DebugOverlayService.kt` & `DebugOverlay.kt`

**Location:** `com/airmouse/ui/`

**Purpose:** Show a floating overlay with live sensor values.

### `DebugOverlayService.kt`

- Extends `Service`.
- Creates a `WindowManager.LayoutParams` with `TYPE_APPLICATION_OVERLAY`.
- Inflates `debug_overlay.xml` (a `TextView`) and adds it to the window manager.
- Singleton companion object: `instance` so that other parts can call `updateData()`.
- `updateData(roll, yaw, gyroY, accelY)` updates the `TextView` text.

### `DebugOverlay.kt`

- Object that starts/stops the service and provides `update()` forwarding.

---

## 12. `CalibrationActivity.kt` & `SettingsScreen.kt`

**Location:** `com/airmouse/ui/`

### `CalibrationActivity.kt`

- A dedicated `AppCompatActivity` that hosts the calibration wizard.
- Uses `ViewPager2` and `CalibrationPagerAdapter` to switch between the three guided sensor steps.
- Shows a header, timer, progress bar, and Back/Next/Stop controls.
- Prevents the user from advancing until the current step reports a valid calibration result.

### `SettingsScreen.kt`

- A modern Compose settings screen with sliders and switches for sensitivity, click threshold, double-click interval, scroll threshold, right-click tilt, haptic feedback, theme, AI smoothing, and predictive movement.
- Reads and writes through `SettingsViewModel`, which in turn talks to the settings repository.

---

# UI Layouts (XML)

## `activity_main.xml`
- Contains: IP input (TextInputLayout), buttons, status text, sensitivity seek bar, settings/debug buttons, and a `View` (green square) with a drawable background.

## `activity_calibration.xml`
- Contains: `ViewPager2`, step header, timer, progress bar, and the Back/Next/Stop control bar used by `CalibrationActivity`.

## `fragment_settings.xml`
- Contains: three groups of (`TextView`, `SeekBar`, `TextView`) for the three thresholds.

## `debug_overlay.xml`
- A single `TextView` with black background, white text, monospaced font.

## `green_square.xml` (drawable)
- A `<shape>` rectangle with solid green colour and 100dp size.

---

# AndroidManifest.xml

- Permissions: `INTERNET`, `VIBRATE`, `SYSTEM_ALERT_WINDOW`, `ACCESS_NETWORK_STATE`.
- Uses‑feature tags for accelerometer, gyroscope, magnetometer (all required).
- Application tag with theme `Theme.AirMouse` (MaterialComponents.DayNight.NoActionBar), `usesCleartextTraffic="true"`.
- Activity `MainActivity` with intent filter for LAUNCHER.
- Service `DebugOverlayService`.

---

# Gradle Build Files

## Project‑level `build.gradle`
- Defines `buildscript` with Google and MavenCentral repositories.
- Classpath: Android Gradle Plugin 7.4.2, Kotlin Gradle Plugin 1.8.0.

## Module‑level `app/build.gradle`
- `minSdk 29`, `targetSdk 34`.
- Build features: `viewBinding true`.
- Dependencies: `androidx.core-ktx`, `appcompat`, `material`, `constraintlayout`, `lifecycle-viewmodel-ktx`, `lifecycle-livedata-ktx`, `datastore-preferences`, `kotlinx-coroutines-android`, `org.json`.

## `settings.gradle`
- `pluginManagement` with Google, MavenCentral, GradlePluginPortal.
- `dependencyResolutionManagement` with `FAIL_ON_PROJECT_REPOS`.
- `include ':app'`.

## `gradle-wrapper.properties`
- `distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip`.

---

# PC Server – Python

## `gui.py` – Dark Mode GUI

- Uses `asyncio` for networking and `tkinter` for GUI.
- `AirMouseGUI` class creates window with start/stop buttons, log area, sensitivity slider.
- `AirMouseServer` class has `handle_client` that reads JSON lines, processes messages, sends ACK.
- Supports `move`, `click`, `doubleclick`, `rightclick`, `scroll`.
- Sensitivity slider updates `CONFIG["sensitivity"]` and the `mouse.sensitivity` attribute.

## `server.py` – Console Server

- Reads `config.json` for host, port, sensitivity, log level, log file.
- Logs to both console and file.
- Similar `AirMouseServer` class, but no GUI.

## `run.py` / `run.sh` / `run.bat`
- `run.py` checks for `pyautogui`, installs dependencies, then launches `gui.py`.
- Shell/batch scripts for one‑click start.

## `perfetto_analyzer.py`
- Uses `perfetto.trace_processor.TraceProcessor` and `pandas`.
- Queries the trace for sensor slices, sampling periods, filter CPU time, etc.
- Prints sample data and textual answers to the 11 questions.

---

# Build Scripts (APK without Android Studio)

## `build_apk.sh` / `build_apk.bat`
- Checks for Java 11 and Android SDK.
- Sets `ANDROID_HOME`.
- Accepts licenses via `yes | sdkmanager --licenses`.
- Installs `build-tools;29.0.3`, `platforms;android-29`, `platform-tools`.
- Runs `./gradlew clean assembleDebug` in the `android` folder.
- Outputs APK location.

---

# Common Code Flow

1. **User opens app** – `MainActivity.onCreate` sets up UI, initialises components, requests permissions.
2. **User calibrates** – `startCalibration()` runs `CalibrationHelper` suspend functions, updating UI.
3. **User enters IP and starts** – `startAirMouse()` creates `DataSender` and `AutoReconnect`, starts them, then starts `SensorService`.
4. **SensorService** registers listeners and waits for sensor events.
5. **On sensor event** – `onSensorChanged` corrects raw values, runs Madgwick fusion, computes deltas, sends `move` packets, detects gestures.
6. **DataSender** queues messages, sends over TCP, handles ACKs.
7. **PC server** receives messages, moves cursor, clicks, scrolls, sends ACKs.
8. **User closes app** – `onDestroy` stops all services, closes sockets.

---

This code walkthrough covers every significant file in the Air Mouse project. Use it to understand the implementation and to modify or extend the system as needed for your coursework.
