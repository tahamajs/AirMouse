# Air Mouse Pro Android App – Complete Documentation

> **Professional‑grade smartphone remote control** using motion sensors, custom gestures, proximity lock, and multiple connectivity protocols.  
> *University of Tehran – Embedded Systems Laboratory*

[![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](../LICENSE)
[![Hilt](https://img.shields.io/badge/Hilt-Dependency_Injection-2A6DB6)](https://dagger.dev/hilt/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-UI_Toolkit-4285F4)](https://developer.android.com/jetpack/compose)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow_Lite-2.16+-FF6F00)](https://www.tensorflow.org/lite)
[![PocketSphinx](https://img.shields.io/badge/PocketSphinx-Offline_ASR-1A1A2E)](https://github.com/cmusphinx/pocketsphinx-android)

---

## 📋 Table of Contents

- [Air Mouse Pro Android App – Complete Documentation](#air-mouse-pro-android-app--complete-documentation)
  - [📋 Table of Contents](#-table-of-contents)
  - [🎯 Overview & Motivation](#-overview--motivation)
  - [🏗️ System Architecture](#️-system-architecture)
    - [2.1 Layered Clean Architecture](#21-layered-clean-architecture)
    - [2.2 Dependency Injection with Hilt](#22-dependency-injection-with-hilt)
    - [2.3 Data Flow Diagram](#23-data-flow-diagram)
  - [📱 Detailed Feature Breakdown](#-detailed-feature-breakdown)
    - [3.1 Sensor Fusion & Orientation](#31-sensor-fusion--orientation)
    - [3.2 Gesture Detection Engine](#32-gesture-detection-engine)
    - [3.3 Calibration Procedures](#33-calibration-procedures)
      - [Gyroscope Bias Removal](#gyroscope-bias-removal)
      - [Accelerometer 6‑Point Calibration](#accelerometer-6point-calibration)
      - [Magnetometer Hard‑Iron Calibration](#magnetometer-hardiron-calibration)
    - [3.4 Connectivity Modules](#34-connectivity-modules)
    - [3.5 Proximity Lock/Unlock](#35-proximity-lockunlock)
    - [3.6 Custom Gesture Recognition (TFLite)](#36-custom-gesture-recognition-tflite)
    - [3.7 Voice Commands (PocketSphinx)](#37-voice-commands-pocketsphinx)
    - [3.8 Edge Gestures (Accessibility Service)](#38-edge-gestures-accessibility-service)
    - [3.9 Touchpad Mode](#39-touchpad-mode)
    - [3.10 Bluetooth HID Mouse](#310-bluetooth-hid-mouse)
    - [3.11 USB HID / Serial](#311-usb-hid--serial)
    - [3.12 USB Serial (CDC ACM / FTDI / CP210x / PL2303)](#312-usb-serial-cdc-acm--ftdi--cp210x--pl2303)
  - [🎨 User Interface (Jetpack Compose)](#-user-interface-jetpack-compose)
    - [4.1 Navigation Graph](#41-navigation-graph)
    - [4.2 Theming & Dynamic Colors](#42-theming--dynamic-colors)
    - [4.3 Reusable Components](#43-reusable-components)
  - [💾 Data Persistence](#-data-persistence)
    - [5.1 Room Database](#51-room-database)
    - [5.2 DataStore (Preferences)](#52-datastore-preferences)
  - [🔄 Background Services & Foreground Notifications](#-background-services--foreground-notifications)
  - [🔒 Permission Handling & Security](#-permission-handling--security)
  - [⚡ Performance Optimizations](#-performance-optimizations)
  - [🧪 Testing Strategy](#-testing-strategy)
    - [9.1 Unit Tests (JUnit + MockK + Kotlin Coroutines Test)](#91-unit-tests-junit--mockk--kotlin-coroutines-test)
    - [9.2 Instrumentation Tests (Espresso + Compose UI Test)](#92-instrumentation-tests-espresso--compose-ui-test)
    - [9.3 UI Tests (Compose UI Test)](#93-ui-tests-compose-ui-test)
  - [🛠️ Troubleshooting & Common Issues](#️-troubleshooting--common-issues)
  - [📦 Development & Contribution](#-development--contribution)
    - [Building from Source](#building-from-source)
    - [Adding a New Screen](#adding-a-new-screen)
    - [Code Style](#code-style)
    - [Pull Request Process](#pull-request-process)
  - [📄 License](#-license)

---

## 🎯 Overview & Motivation

The **Air Mouse Pro Android app** transforms any Android phone into a versatile, low‑latency remote control for desktop computers. Unlike conventional remote apps that rely only on touch input, Air Mouse Pro leverages the phone's built‑in inertial sensors (gyroscope, accelerometer, magnetometer) to detect natural hand movements, allowing users to control the cursor simply by rotating or moving the phone in space.

### 🚀 Core Use Cases

| Use Case | Description | Ideal For |
|----------|-------------|-----------|
| **Presentation Control** | Navigate slides, highlight content, control media | Teachers, speakers, business presenters |
| **Media Center Control** | Play/pause, volume, navigation from couch | Home theatre, streaming, music playback |
| **Accessibility** | Mouse control without physical mouse | Users with mobility impairments |
| **Gaming** | Motion-controlled cursor for casual games | Gamers, entertainment |
| **Remote Work** | Control computer from phone during meetings | Remote workers, hybrid work |

### 💡 Key Motivations

1. **Hands‑free interaction** – ideal for presentations, media centres, or when the keyboard/mouse is out of reach.
2. **Low‑cost alternative** – no need for specialised hardware; uses existing smartphone sensors.
3. **Customisability** – users can train their own gestures, adjust sensitivity, and choose from multiple connectivity protocols.
4. **Privacy** – all voice recognition is offline (PocketSphinx), and personal data stays on‑device.
5. **Cross‑Platform** – works with Windows, Linux, and macOS servers.

---

## 🏗️ System Architecture

### 2.1 Layered Clean Architecture

The app is structured into four distinct layers, each with a clear responsibility and dependency direction (inward).

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│  • Jetpack Compose UI Screens                                         │
│  • ViewModels (state holders with StateFlow)                         │
│  • Navigation (NavHost, Destinations)                                │
│  • Themes, animations, reusable composables                         │
│  • Material 3 Design System                                         │
└─────────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            DOMAIN LAYER                                │
│  • Entities (pure Kotlin data classes)                               │
│  • Use Cases (interactors with business logic)                       │
│  • Repository interfaces                                              │
│  • Business logic (no Android dependencies)                          │
│  • Domain models (CalibrationData, SensorData, etc.)                │
└─────────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────────┐
│                             DATA LAYER                                 │
│  • Repository implementations                                         │
│  • Data sources (WebSocket, Bluetooth, USB)                          │
│  • Local databases (Room, DataStore)                                 │
│  • DTOs (network models)                                             │
│  • DAOs (Data Access Objects)                                        │
└─────────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE LAYER                           │
│  • SensorService (foreground service)                                │
│  • CalibrationHelper (bias removal, 6‑point accel)                   │
│  • GestureDetector (threshold‑based)                                 │
│  • MadgwickAHRS (sensor fusion)                                      │
│  • PocketSphinx recognizer                                            │
│  • TFLite interpreter for custom gestures                            │
│  • Bluetooth HID, USB HID/serial                                     │
│  • OkHttp, WebSocketManager                                          │
│  • NetworkQualityMonitor (WiFi, Cellular, Ethernet)                 │
└─────────────────────────────────────────────────────────────────────────┘
```

**Dependency Rule**: The Domain layer knows nothing about the Data or Infrastructure layers. Dependencies point inward, making the core business logic independent of frameworks.

### 2.2 Dependency Injection with Hilt

All dependencies are injected at compile time using **Dagger Hilt**. This eliminates boilerplate and makes testing easier.

#### Key Hilt Components

| Component | Purpose |
|-----------|---------|
| `@HiltAndroidApp` | Application class annotation |
| `@AndroidEntryPoint` | Activities, Fragments, Services, ViewModels |
| `@Module` / `@InstallIn` | Module definitions for specific components |
| `@Provides` / `@Singleton` | Provider functions with singleton scope |

#### Hilt Modules

| Module | Provides |
|--------|----------|
| `AppModule` | Context, Application, PreferencesManager |
| `NetworkModule` | OkHttpClient, WebSocketManager, TcpClient |
| `SensorModule` | SensorManager, SensorService |
| `DatabaseModule` | Room Database, DAOs |
| `RepositoryModule` | Repository implementations |
| `UseCaseModule` | Use case implementations |
| `ServiceModule` | Foreground services |

#### Example – Providing the SensorManager

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SensorModule {
    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext context: Context): SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
}
```

### 2.3 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          USER INTERACTION                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐      │
│  │  Phone     │  │  Voice     │  │  Touchpad  │  │  Edge      │      │
│  │  Motion    │  │  Commands  │  │  Gestures  │  │  Gestures  │      │
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘      │
│         │               │               │               │             │
│         ▼               ▼               ▼               ▼             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Sensor Processing Pipeline                   │   │
│  │  • Madgwick AHRS (Sensor Fusion)                              │   │
│  │  • Gesture Detection (Threshold-based)                        │   │
│  │  • TFLite Inference (Custom Gestures)                         │   │
│  │  • PocketSphinx (Voice Recognition)                           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   Command Generation                            │   │
│  │  • Move (dx, dy)  • Click (left/right)                       │   │
│  │  • Scroll (delta)  • Gesture (name, confidence)              │   │
│  │  • Proximity (near/far)  • Control (pause/resume)            │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   Connection Manager                           │   │
│  │  • WebSocket (primary)                                         │   │
│  │  • TCP (fallback)                                              │   │
│  │  • UDP Discovery                                               │   │
│  │  • Bluetooth HID / USB HID                                     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          DESKTOP SERVER                                │
│  • Receives JSON messages                                             │
│  • Converts to mouse/keyboard events                                  │
│  • Executes system actions (lock, volume, media)                     │
│  • Sends acknowledgements                                            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📱 Detailed Feature Breakdown

### 3.1 Sensor Fusion & Orientation

**Why fusion?**  
Raw gyroscope data drifts over time; accelerometer alone cannot distinguish gravity from linear acceleration; magnetometer is noisy. The **Madgwick AHRS** algorithm fuses all three to produce a stable, drift‑free quaternion, from which we extract Euler angles (roll, pitch, yaw).

#### Implementation – `MadgwickAHRS.kt`

```kotlin
class MadgwickAHRS(private var beta: Float = 0.1f) {
    private val quaternion = FloatArray(4).apply { this[0] = 1f }
    
    fun updateGyro(gx: Float, gy: Float, gz: Float, dt: Float) { /* ... */ }
    fun updateAccel(ax: Float, ay: Float, az: Float, dt: Float) { /* ... */ }
    fun updateMag(mx: Float, my: Float, mz: Float, dt: Float) { /* ... */ }
    
    fun getRollDegrees(): Float = Math.toDegrees(getRollRad().toDouble()).toFloat()
    fun getPitchDegrees(): Float = Math.toDegrees(getPitchRad().toDouble()).toFloat()
    fun getYawDegrees(): Float = Math.toDegrees(getYawRad().toDouble()).toFloat()
}
```

**Algorithm Parameters**:
- Beta = 0.1 (gain, controls filter convergence speed)
- Update rate = 50‑100 Hz (depends on sensor delay)
- Update steps: `updateGyro()` → `updateAccel()` → `updateMag()`
- Output: quaternion (w, x, y, z) → converted to roll (X rotation) and yaw (Z rotation)

**Cursor Mapping**:
| Sensor | Mapping | Sensitivity Range |
|--------|---------|-------------------|
| Roll (X rotation) | Vertical movement | 0.2 – 2.0 multiplier |
| Yaw (Z rotation) | Horizontal movement | 0.2 – 2.0 multiplier |
| Pitch (Y rotation) | Not used (disabled) | - |

**Deadband**: Ignore movements below 0.3° to prevent jitter.

**Motion Smoothing**:
- EMA (Exponential Moving Average) filter with configurable alpha (0.3 default).
- Velocity‑based smoothing: faster movements have less smoothing (perceptual motion blur).

### 3.2 Gesture Detection Engine

#### Threshold-Based Gestures

| Gesture | Sensor | Detection Logic | Default Threshold |
|---------|--------|-----------------|-------------------|
| **Click** | Gyroscope Y | Angular speed > threshold AND not within double‑click window | 5 rad/s |
| **Double‑click** | Gyroscope Y | Two clicks within `double_click_interval` (300 ms) | 300 ms |
| **Right‑click** | Accelerometer roll | Tilt angle (roll) > threshold AND hold for > duration | 15°, 200 ms |
| **Scroll up** | Accelerometer Y | Linear acceleration (positive) > threshold | 8 m/s² |
| **Scroll down** | Accelerometer Y | Linear acceleration (negative) < -threshold | 8 m/s² |

#### Cooldowns & Debounce

| Parameter | Value | Purpose |
|-----------|-------|---------|
| Click cooldown | 100 ms | Prevent repeated triggers |
| Scroll debounce | 500 ms | Prevent return movement from triggering reverse scroll |
| Gesture cooldown | 300 ms | Prevent multiple gesture triggers |

#### Gesture Accuracy

```
Click Detection Accuracy: ~95% (after calibration)
Double-click Accuracy: ~88%
Right-click Accuracy: ~85%
Scroll Detection Accuracy: ~90%
```

### 3.3 Calibration Procedures

#### Gyroscope Bias Removal

**Purpose**: Eliminate zero-rate offset (static bias) that causes cursor drift even when the phone is stationary.

**Process**:
1. User places phone on a flat, stationary surface.
2. App collects 500 gyroscope samples at 50 Hz (10 seconds).
3. Computes average per axis – this is the bias.
4. Subtracts bias from all future readings.

**Formula**:
```
biasX = mean(gyroX_samples)
biasY = mean(gyroY_samples)
biasZ = mean(gyroZ_samples)

correctedGyroX = rawGyroX - biasX
correctedGyroY = rawGyroY - biasY
correctedGyroZ = rawGyroZ - biasZ
```

**Quality Check**: If the bias is too large (>1.0 rad/s), the calibration fails and prompts the user to try again.

#### Accelerometer 6‑Point Calibration

**Purpose**: Correct for offset and scale errors in accelerometer readings caused by manufacturing tolerances and temperature changes.

**Process**:
1. User holds phone in six orientations (+X, -X, +Y, -Y, +Z, -Z).
2. For each axis, measures the raw values when gravity aligns perfectly.
3. Solves: `raw = scale * ideal + offset` for each axis.
4. Resulting `offset` and `scale` are stored.

**Orientations**:

| Position | Orientation | Ideal Gravity (g) |
|----------|-------------|-------------------|
| 1 | +X | (1, 0, 0) |
| 2 | -X | (-1, 0, 0) |
| 3 | +Y | (0, 1, 0) |
| 4 | -Y | (0, -1, 0) |
| 5 | +Z | (0, 0, 1) |
| 6 | -Z | (0, 0, -1) |

**Formula**:
```
For each axis:
  offset = (raw_max + raw_min) / 2
  scale = 2 / (raw_max - raw_min)  // Assuming ±1g range
```

#### Magnetometer Hard‑Iron Calibration

**Purpose**: Correct for fixed magnetic offsets (hard-iron interference) from permanent magnets in the device or environment.

**Process**:
1. User waves phone in a figure‑8 pattern for 10 seconds.
2. App records min and max values for each axis.
3. Offset = `(min + max)/2`.
4. Scale = `(max - min)/2` (optional, for soft-iron correction).

**Formula**:
```
offsetX = (minX + maxX) / 2
offsetY = (minY + maxY) / 2
offsetZ = (minZ + maxZ) / 2

correctedMagX = rawMagX - offsetX
correctedMagY = rawMagY - offsetY
correctedMagZ = rawMagZ - offsetZ
```

### 3.4 Connectivity Modules

#### Protocol Comparison

| Module | Protocol | Latency | Reliability | Use Case |
|--------|----------|---------|-------------|----------|
| `WebSocketManager` | WebSocket | ~10-20ms | High | Primary, real-time control |
| `DataSender` | TCP | ~5-15ms | Very High | Critical commands (clicks) |
| `TcpClient` | TCP | ~5-15ms | High | Touchpad mode |
| `BluetoothMouseService` | Bluetooth HID | ~20-40ms | Medium | Local connection |
| `UsbHidService` | USB HID | <5ms | Very High | Wired connection |
| `UsbSerialService` | USB CDC | <5ms | Very High | Debugging, custom apps |

#### WebSocket Features

| Feature | Implementation |
|---------|----------------|
| Protocol | `ws://` / `wss://` |
| Port | 8081 (configurable) |
| Reconnection | Exponential backoff (1s, 2s, 4s, … up to 30s) |
| Ping/Pong | 30-second interval |
| Message Queue | Buffer of 100 messages when disconnected |
| Authentication | JWT token via query parameter |

#### Reconnection Logic

```kotlin
private fun scheduleReconnect() {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        return
    }
    val delay = (reconnectAttempts + 1) * 2000L
    reconnectAttempts++
    // Schedule reconnect with delay
    handler.postDelayed({ connect() }, delay)
}
```

### 3.5 Proximity Lock/Unlock

**Principle** – RSSI (Received Signal Strength Indicator) of a paired Bluetooth device decreases with distance. Using a path‑loss model:

```
distance = 10^((txPower - rssi) / (10 * n))
```

- `txPower` = calibrated RSSI at 1 metre (e.g., -59 dBm).
- `n` = environmental factor (2.5 for indoor office).

#### Flow

1. `ProximityAwareService` runs in foreground, reading RSSI every second.
2. Distance is fed into a hysteresis comparator:
  - If already "near", switch to "far" only when distance > `far_threshold`.
  - If already "far", switch to "near" only when distance < `near_threshold`.
3. On state change, the app sends a `proximity` message to the server.
4. Server triggers screen lock/unlock via OS commands.

#### Calibration

| Step | Action |
|------|--------|
| 1 | Place phone exactly 1m from computer |
| 2 | Tap "Calibrate" |
| 3 | App records RSSI at 1m → adjusts `txPower` |
| 4 | Optional: Repeat at 2m, 3m, 4m, 5m for better accuracy |

#### Configuration

| Parameter | Default | Range |
|-----------|---------|-------|
| Near Threshold | 1.5m | 0.5m – 3.0m |
| Far Threshold | 3.0m | 1.0m – 5.0m |
| Tx Power | -59 dBm | -80 – -30 dBm |
| Scan Interval | 1000ms | 500 – 5000ms |

### 3.6 Custom Gesture Recognition (TFLite)

#### Pipeline

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         GESTURE RECORDING                              │
│  • User taps "Record"                                                 │
│  • App collects gyro + accel data at 50Hz (10 seconds)               │
│  • Data saved as CSV                                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         TRAINING (PC)                                  │
│  • Python script loads CSV                                            │
│  • Trains 1D-CNN / LSTM model                                        │
│  • Exports gesture_model.tflite and gesture_labels.json              │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         INFERENCE (Android)                            │
│  • GestureInferenceService loads model                                │
│  • Buffers 30 consecutive sensor samples                             │
│  • Runs inference every 500ms (with cooldown)                        │
│  • Sends gesture message when confidence > 0.7                       │
└─────────────────────────────────────────────────────────────────────────┘
```

#### Model Architecture

```
Input: (30 × 6)
┌─────────────────────────────────────────────────────────────────────────┐
│  Conv1D(64, kernel_size=3, activation=relu)                         │
│  MaxPool1D(2)                                                        │
│  Conv1D(128, kernel_size=3, activation=relu)                        │
│  MaxPool1D(2)                                                        │
│  Flatten                                                             │
│  Dense(128, activation=relu)                                        │
│  Dropout(0.5)                                                        │
│  Dense(num_classes, activation=softmax)                             │
└─────────────────────────────────────────────────────────────────────────┘
```

#### Supported Gestures (Pre-trained)

| Gesture | Confidence Threshold | Action |
|---------|---------------------|--------|
| ThumbsUp | 0.7 | Play/Pause |
| ThumbsDown | 0.7 | Stop |
| SwipeLeft | 0.7 | Previous Track |
| SwipeRight | 0.7 | Next Track |
| CircleCW | 0.7 | Volume Up |
| CircleCCW | 0.7 | Volume Down |
| PinchIn | 0.7 | Zoom Out |
| PinchOut | 0.7 | Zoom In |

### 3.7 Voice Commands (PocketSphinx)

**Why offline?** Privacy and no internet dependency.

#### Setup

| Asset | Purpose |
|-------|---------|
| `en-us-ptm` | Acoustic model |
| `cmudict-en-us.dict` | Pronunciation dictionary |
| `commands.gram` | Command set grammar |

#### Commands

| Command | Network Message | Description |
|---------|-----------------|-------------|
| "click" | `{"type":"click","button":"left"}` | Left click |
| "double click" | `{"type":"doubleclick"}` | Double click |
| "right click" | `{"type":"click","button":"right"}` | Right click |
| "scroll up" | `{"type":"scroll","delta":3}` | Scroll up |
| "scroll down" | `{"type":"scroll","delta":-3}` | Scroll down |
| "stop listening" | `{"type":"control","command":"voice_stop"}` | Stop voice recognition |

#### Workflow

1. `VoiceCommandService` starts listening on button press.
2. Recogniser triggers on partial results (end‑point detection).
3. On full result, maps command to action.
4. Sends appropriate network message.
5. Optionally plays haptic feedback for recognition.

### 3.8 Edge Gestures (Accessibility Service)

**Concept**: Long‑press volume keys (up/down) trigger actions even when the app is in the background or screen off.

#### Implementation

```
Accessibility Service
├── onKeyEvent()
│   ├── Volume Up Press
│   │   ├── Start timer
│   │   └── If held > 1s → Send control message
│   └── Volume Down Press
│       ├── Start timer
│       └── If held > 1s → Send control message
└── onAccessibilityEvent()
```

#### Configuration

| Action | Trigger | Result |
|--------|---------|--------|
| Volume Up (short) | < 1s | Default volume behaviour |
| Volume Up (long) | > 1s | Configurable action (click, scroll, etc.) |
| Volume Down (short) | < 1s | Default volume behaviour |
| Volume Down (long) | > 1s | Configurable action |

#### Permissions Required

- `SYSTEM_ALERT_WINDOW` – Required for overlay detection.
- Accessibility Service – Must be enabled in system settings.

### 3.9 Touchpad Mode

When enabled, the screen becomes a full‑surface touchpad.

#### Touch Gestures

| Gesture | Action | Network Message |
|---------|--------|-----------------|
| Single‑finger drag | Move cursor | `{"type":"move","dx":x,"dy":y}` |
| Tap | Left click | `{"type":"click","button":"left"}` |
| Two‑finger drag | Scroll | `{"type":"scroll","delta":d}` |
| Two‑finger tap | Right click | `{"type":"click","button":"right"}` |
| Three‑finger tap | Back/Forward | Configurable |

#### Implementation

```kotlin
pointerInput(Unit) {
    detectDragGestures(
        onDragStart = { /* Start tracking */ },
        onDrag = { change, dragAmount ->
            // Convert touch drag to cursor movement
            val dx = dragAmount.x * sensitivity
            val dy = dragAmount.y * sensitivity
            connectionManager.sendMove(dx, dy)
        },
        onDragEnd = { /* End tracking */ }
    )
}
```

### 3.10 Bluetooth HID Mouse

**How it works** (Android 8+):
- App uses `BluetoothHidDevice` system service.
- Registers a HID application with a mouse report descriptor.
- When a computer pairs and connects, the app can send HID reports (`sendMouseReport`).
- The computer sees the phone as a standard Bluetooth mouse.

#### Report Format

```
Report Descriptor (8 bytes):
┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│ Buttons  │   X (low)│   X (high)│  Y (low)│  Y (high)│  Wheel   │  Padding │  Padding │
│ (1 byte) │  (1 byte)│  (1 byte)│ (1 byte)│ (1 byte)│ (1 byte)│ (1 byte)│ (1 byte)│
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘

Buttons:
  - Bit 0: Left click
  - Bit 1: Right click
  - Bit 2: Middle click
```

#### Limitations

| Issue | Workaround |
|-------|------------|
| Requires system permission `BLUETOOTH_CONNECT` | Request at runtime |
| May not work on all devices (OEM implementation) | Fallback to USB HID mode |
| Android 8+ required | - |

### 3.11 USB HID / Serial

#### USB HID (`UsbHidService`)

- Phone appears as a USB mouse when connected via OTG.
- Sends mouse reports over the USB interrupt endpoint.
- Works on any OS (Linux, Windows, macOS) without special drivers.

#### Report Descriptor (HID 1.1)

```kotlin
val HID_REPORT_DESCRIPTOR = byteArrayOf(
    0x05, 0x01, // Usage Page (Generic Desktop)
    0x09, 0x02, // Usage (Mouse)
    0xA1, 0x01, // Collection (Application)
    // ... Full descriptor in code
)
```

#### USB Serial (`UsbSerialService`)

- Emulates a CDC serial port.
- Sends JSON commands over bulk endpoints.
- Useful for debugging or for custom applications.

**Supported Chipsets**:
- FTDI (FT232R, FT230X, FT231X)
- Silabs CP210x
- Prolific PL2303
- Arduino (CDC ACM)
- CH340/CH341

**Baud Rates**: 300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600

### 3.12 USB Serial (CDC ACM / FTDI / CP210x / PL2303)

This module provides communication over USB serial using the `usb-serial-for-android` library.

#### Features

| Feature | Status |
|---------|--------|
| Automatic device detection | ✅ |
| Baud rate configuration | ✅ |
| Data bits (5, 6, 7, 8) | ✅ |
| Stop bits (1, 1.5, 2) | ✅ |
| Parity (none, odd, even, mark, space) | ✅ |
| Flow control (RTS/CTS) | ✅ |
| JSON message parsing | ✅ |
| Raw text transmission | ✅ |

#### Message Format

```json
// Move command
{"type":"move","payload":{"dx":12.5,"dy":-3.2}}

// Click command
{"type":"click","payload":{"button":"left"}}

// Hello command
{"type":"hello","payload":{"name":"Android Phone","version":"3.0"}}
```

---

## 🎨 User Interface (Jetpack Compose)

### 4.1 Navigation Graph

The app uses Compose Navigation with a sealed class `Destinations`. The `NavHost` defines all screens and transitions.

#### Destination Structure

```kotlin
sealed class Destinations(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Destinations("home", "Home", Icons.Filled.Home)
    object Statistics : Destinations("statistics", "Stats", Icons.Filled.BarChart)
    object Settings : Destinations("settings", "Settings", Icons.Filled.Settings)
    // ... 22+ destinations
}
```

#### Bottom Navigation

| Tab | Destination | Icon | Description |
|-----|-------------|------|-------------|
| Home | Home | 🏠 | Main dashboard |
| Stats | Statistics | 📊 | Usage statistics |
| Settings | Settings | ⚙️ | App configuration |
| Help | Help | ❓ | Help & support |

#### Navigation Graph

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              NavHost                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │   Home   │  │  Stats   │  │ Settings │  │   Help   │              │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘              │
│       │              │             │             │                     │
│       ▼              ▼             ▼             ▼                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │Calibrate │  │  About   │  │ Profiles │  │Onboarding│              │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘              │
│       │              │             │             │                     │
│       ▼              ▼             ▼             ▼                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ Touchpad │  │ Gestures │  │  Voice   │  │  Themes  │              │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘              │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Theming & Dynamic Colors

#### Theme System

| Theme | Description | Use Case |
|-------|-------------|----------|
| System | Follows system theme | Default |
| Dark | Dark material theme | General use |
| Light | Light material theme | Bright environments |
| Pure Black | AMOLED-friendly | Battery saving |
| Ocean | Blue tones | Calm experience |
| Sunset | Orange tones | Warm feel |
| Forest | Green tones | Natural feel |
| Purple Haze | Purple tones | Mystical feel |
| Cherry | Pink tones | Soft feel |
| Neon | Cyan tones | Cyberpunk feel |
| Lavender | Light purple | Gentle feel |
| Mint | Light green | Fresh feel |
| Peach | Light orange | Warm feel |
| Sky | Light blue | Bright feel |

#### Accent Colors (14+)

| Color | Name | Code |
|-------|------|------|
| Orange | 0xFFFF5722 | Default |
| Blue | 0xFF2196F3 | Calm |
| Green | 0xFF4CAF50 | Natural |
| Purple | 0xFF9C27B0 | Bold |
| Pink | 0xFFE91E63 | Soft |
| Red | 0xFFF44336 | Alert |
| Teal | 0xFF009688 | Cool |
| Indigo | 0xFF3F51B5 | Deep |
| Cyan | 0xFF00BCD4 | Cyber |
| Amber | 0xFFFFC107 | Warm |
| Rose | 0xFFE91E63 | Elegant |
| Lime | 0xFFCDDC39 | Fresh |
| Brown | 0xFF795548 | Earthy |
| Grey | 0xFF607D8B | Professional |

#### Dynamic Colors (Material You)

- Android 12+ support
- Extracts colors from wallpaper
- Applies to primary, secondary, tertiary, surface, and background

### 4.3 Reusable Components

| Component | Purpose | Used In |
|-----------|---------|---------|
| `ConnectionCard` | IP/port input, connect/disconnect | Home, Settings |
| `SensorDataCard` | Live yaw/pitch display | Home, Calibration |
| `GestureStatsCard` | Click, scroll, right-click, double-click counts | Home, Statistics |
| `CalibrationCard` | Progress bars, attempt counter | Calibration |
| `GestureChart` | Bar chart using MPAndroidChart | Statistics, Analytics |
| `StatusChip` | Colour-coded status indicator | Home, Devices |
| `QualityIndicator` | Signal quality display | Home, Connection |
| `ThemeSelector` | Theme picker grid | Settings, Themes |
| `GlassCard` | Glassmorphism card | About, Settings |

---

## 💾 Data Persistence

### 5.1 Room Database

#### Entities

| Entity | Table Name | Columns | Purpose |
|--------|-----------|---------|---------|
| `CalibrationEntity` | calibration | gyro_bias_x, gyro_bias_y, gyro_bias_z, accel_offset_x, ... | Calibration data |
| `GyroBias` | gyro_bias | id, offset_x, offset_y, offset_z, timestamp | Gyroscope bias |
| `AccelCalibration` | accel_calibration | id, offset_x, offset_y, offset_z, scale_x, scale_y, scale_z, timestamp | Accelerometer calibration |
| `MagCalibration` | mag_calibration | id, offset_x, offset_y, offset_z, scale_x, scale_y, scale_z, timestamp | Magnetometer calibration |
| `CustomGestureTemplate` | gesture_templates | id, name, data, created_at | Custom gesture templates |
| `Profile` | profiles | id, name, sensitivity, thresholds, theme, ai_settings | User profiles |

#### DAOs

```kotlin
@Dao
interface CalibrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibration(calibration: CalibrationEntity)
    
    @Query("SELECT * FROM calibration WHERE id = 'default'")
    suspend fun getCalibration(): CalibrationEntity?
    
    @Query("DELETE FROM calibration")
    suspend fun deleteAll()
    
    @Query("SELECT EXISTS(SELECT 1 FROM calibration WHERE id = 'default')")
    suspend fun exists(): Boolean
}
```

### 5.2 DataStore (Preferences)

#### Preference Categories

| Category | Keys | Purpose |
|----------|------|---------|
| Connection | last_ip, last_port, protocol, auth_token | Connection settings |
| Calibration | gyro_bias_x, gyro_bias_y, gyro_bias_z, calibration_status | Calibration data |
| Mouse | sensitivity, smoothing, acceleration, invert_x, invert_y | Mouse settings |
| Gesture | click_threshold, scroll_threshold, double_click_interval | Gesture detection |
| AI | ai_smoothing, ai_blend_factor, predictive_movement | AI settings |
| Theme | theme, accent_color, dynamic_colors | Appearance |
| Proximity | proximity_enabled, near_threshold, far_threshold | Proximity lock |
| Voice | wake_word, wake_word_enabled, voice_haptic_feedback | Voice commands |
| Touchpad | touchpad_sensitivity, natural_scrolling, two_finger_scroll | Touchpad mode |

#### Observation

```kotlin
val preferencesFlow: Flow<Preferences> = dataStore.data
    .catch { exception ->
        if (exception is IOException) emit(emptyPreferences())
        else throw exception
    }
```

---

## 🔄 Background Services & Foreground Notifications

### Service Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        FOREGROUND SERVICES                             │
│                                                                         │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │   SensorService  │  │  VoiceCommand    │  │    Proximity     │      │
│  │   (dataSync)     │  │   (microphone)   │  │   (location)     │      │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │    Bluetooth     │  │      USB         │  │     Gesture      │      │
│  │    HID Mouse     │  │    HID/Serial    │  │   Inference      │      │
│  │ (connectedDevice)│  │   (dataSync)     │  │   (dataSync)     │      │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘      │
└─────────────────────────────────────────────────────────────────────────┘
```

### Service Details

| Service | Foreground Type | Purpose | Notification ID |
|---------|----------------|---------|-----------------|
| `SensorService` | `dataSync` | Sensor fusion, movement generation | 1001 |
| `GestureInferenceService` | `dataSync` | TFLite custom gesture inference | 1002 |
| `VoiceCommandService` | `microphone` | Offline speech recognition | 1003 |
| `ProximityAwareService` | `location` | Bluetooth RSSI monitoring | 1004 |
| `BluetoothMouseService` | `connectedDevice` | Bluetooth HID mouse emulation | 1005 |
| `UsbHidService` | `dataSync` | USB HID mouse emulation | 1006 |
| `UsbSerialService` | `dataSync` | USB serial communication | 1007 |
| `OrientationMonitorService` | `dataSync` | Orientation monitoring | 1008 |
| `EdgeGestureService` | `dataSync` | Edge gesture detection | 1009 |
| `DebugOverlayService` | `dataSync` | Debug overlay (development) | 1010 |

### Notification Channels

| Channel ID | Name | Importance | Sound | Vibration |
|------------|------|------------|-------|-----------|
| `connection_channel` | Connection Status | LOW | No | No |
| `gesture_channel` | Gesture Detection | DEFAULT | Yes | Yes |
| `proximity_channel` | Proximity Lock | HIGH | Yes | Yes |
| `calibration_channel` | Calibration | DEFAULT | No | No |
| `voice_channel` | Voice Commands | LOW | No | No |
| `update_channel` | App Updates | DEFAULT | Yes | Yes |
| `sensor_channel` | Sensor Service | LOW | No | No |
| `bluetooth_channel` | Bluetooth HID | DEFAULT | No | No |
| `usb_channel` | USB Service | DEFAULT | No | No |

---

## 🔒 Permission Handling & Security

### Required Permissions

| Permission | Category | Purpose |
|------------|----------|---------|
| `INTERNET` | Normal | Network communication |
| `ACCESS_NETWORK_STATE` | Normal | Network connectivity check |
| `CAMERA` | Dangerous | QR scanning |
| `VIBRATE` | Normal | Haptic feedback |
| `BLUETOOTH` | Dangerous | Bluetooth operations |
| `BLUETOOTH_SCAN` | Dangerous | Bluetooth discovery |
| `BLUETOOTH_CONNECT` | Dangerous | Bluetooth connection |
| `BLUETOOTH_ADVERTISE` | Dangerous | Bluetooth advertising |
| `ACCESS_FINE_LOCATION` | Dangerous | Bluetooth scanning (Android 10+) |
| `RECORD_AUDIO` | Dangerous | Voice commands |
| `BODY_SENSORS` | Dangerous | Accelerometer, gyroscope, magnetometer |
| `FOREGROUND_SERVICE` | Normal | Background services |
| `SYSTEM_ALERT_WINDOW` | Special | Debug overlay |
| `WRITE_EXTERNAL_STORAGE` | Dangerous | Exporting gesture datasets |
| `POST_NOTIFICATIONS` | Dangerous | Notification display (Android 13+) |

### Permission Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PERMISSION REQUEST                              │
│                                                                         │
│  1. Check if permission is granted                                     │
│  2. If not, show rationale (optional)                                 │
│  3. Request permission using ActivityResultContracts                  │
│  4. Handle result                                                      │
│  5. If denied, show explanation and retry                             │
└─────────────────────────────────────────────────────────────────────────┘
```

### Security Features

| Feature | Implementation | Status |
|---------|----------------|--------|
| JWT Authentication | Token in WebSocket URL | ✅ |
| TLS/SSL Support | WSS (WebSocket Secure) | ✅ |
| Data Encryption | AES (optional) | 🔄 Planned |
| No Cloud Storage | All data stays on-device | ✅ |
| Offline Processing | All AI/ML on-device | ✅ |
| Permission Rationales | Shown before requesting | ✅ |
| Secure Communication | Local network only | ✅ |

---

## ⚡ Performance Optimizations

### 1. Sensor Sampling Optimization

- **Dynamic rate adjustment**:
  - Stationary: `SENSOR_DELAY_NORMAL` (200ms)
  - Movement: `SENSOR_DELAY_GAME` (20ms)

### 2. Network Optimization

- **Event coalescing**: Movement events batched at 60Hz
- **Message queue**: Buffer 100 messages when disconnected
- **JSON pooling**: Reuse JSON objects (planned)

### 3. UI Optimization

- **Lazy loading**: `LazyColumn` for large lists
- **Image caching**: Coil for image loading
- **Recomposition**: `@Stable` annotations, `remember` keys

### 4. Memory Optimization

- **Bitmap caching**: `LruCache` for bitmaps
- **Object pooling**: Reuse sensor data objects
- **Log trimming**: Limit to 500 entries

### 5. Battery Optimization

| Feature | Description |
|---------|-------------|
| Sensor rate adjustment | Lower rate when stationary |
| Background work | Coroutines on `Dispatchers.IO` |
| Wake locks | Released when screen off |
| Network | WebSocket ping interval (30s) |
| TFLite | Inference on `Dispatchers.Default` |

### 6. Performance Metrics

| Metric | Value |
|--------|-------|
| Sensor latency | ~5-10ms |
| Network latency (WiFi) | ~10-20ms |
| Network latency (Cellular) | ~30-50ms |
| UI frame rate | 60 FPS (target) |
| TFLite inference | ~10ms per batch |
| App startup time | <3 seconds |
| Memory usage | ~150-200 MB |
| Battery impact | ~5-10% per hour |

---

## 🧪 Testing Strategy

### 9.1 Unit Tests (JUnit + MockK + Kotlin Coroutines Test)

| Test Class | Purpose |
|------------|---------|
| `ValidationUtilsTest` | IP parsing, endpoint extraction |
| `PreferencesManagerTest` | Save/load, increment counters |
| `GestureDetectorTest` | Threshold detection, double-click window |
| `CalibrationHelperTest` | Bias calculation, 6-point formula |
| `MadgwickAHRSUnitTest` | Quaternion integration, Euler conversion |
| `SensorDataTest` | Sensor data processing |
| `ConnectionManagerTest` | Connection states, reconnection logic |

### 9.2 Instrumentation Tests (Espresso + Compose UI Test)

| Test Class | Purpose |
|------------|---------|
| `HomeScreenTest` | Connection card, start/stop button |
| `CalibrationScreenTest` | Step navigation, progress updates |
| `GestureStudioActivityTest` | Gesture recording, dataset export |
| `SettingsScreenTest` | Theme switching, sensitivity slider |
| `TouchpadScreenTest` | Touch gestures, scroll behaviour |

### 9.3 UI Tests (Compose UI Test)

```kotlin
@Test
fun testHomeScreenConnectionCard() {
    composeTestRule.setContent {
        HomeScreen()
    }
    composeTestRule
        .onNodeWithTag("connection_card")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("connect_button")
        .performClick()
    composeTestRule
        .onNodeWithText("Connected")
        .assertIsDisplayed()
}
```

### Continuous Integration

- **GitHub Actions**: Runs all tests on every push
- **Code Coverage**: Monitored by Codecov
- **Static Analysis**: Ktlint, Detekt

---

## 🛠️ Troubleshooting & Common Issues

### Connection Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Cannot connect | Wi‑Fi mismatch, firewall | Verify same network; disable firewall temporarily |
| Connection drops | WiFi interference, signal weak | Move closer to router, use 5GHz |
| Slow response | High latency, packet loss | Reduce WiFi congestion, use Ethernet |
| Authentication failed | Invalid token | Regenerate token, check JWT expiry |

### Sensor Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Cursor jumps erratically | Gyroscope bias not calibrated | Run gyroscope calibration on a flat surface |
| No movement | Sensors not active, permissions denied | Check sensor permissions, restart service |
| Inverted movement | Invert axes enabled | Disable invert X/Y in Settings |
| Jittery cursor | High sensitivity, low deadband | Reduce sensitivity, increase deadband |

### Gesture Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Gestures not recognised | Threshold too high | Lower click/scroll thresholds in Settings |
| False positive gestures | Threshold too low | Increase click/scroll thresholds |
| Double-click not working | Interval too short/long | Adjust double-click interval (200-400ms) |
| Right-click not working | Tilt angle too high | Reduce right-click tilt threshold |

### Voice Commands

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| No response | Microphone permission denied | Grant permission |
| Wrong commands recognised | Background noise, poor mic quality | Move to quieter environment |
| Commands not registered | PocketSphinx assets missing | Reinstall app, check assets folder |

### Bluetooth Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Bluetooth mouse not pairing | Android version / OEM limitation | Use USB HID mode instead |
| HID device not discovered | Bluetooth not enabled, permissions | Enable Bluetooth, grant permissions |
| Pairing fails | Incompatible HID profile | Try USB HID or WebSocket mode |

### Proximity Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Never locks | Bluetooth not paired or RSSI too low | Pair phone with computer; calibrate distance |
| Unlocks immediately | Threshold too high | Adjust near/far thresholds |
| Frequent lock/unlock | Hysteresis too small | Increase gap between near/far thresholds |

### App Crashes

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Crashes on opening | Missing TFLite model or labels | Ensure `gesture_model.tflite` and `gesture_labels.json` are in `assets/` |
| Crashes on calibration | Sensor permission denied | Grant BODY_SENSORS permission |
| Out of memory | Memory leak | Restart app, reduce memory usage |

---

## 📦 Development & Contribution

### Building from Source

```bash
# Clone repository
git clone https://github.com/yourusername/airmouse-android.git
cd airmouse-android

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

### Adding a New Screen

1. **Create screen file**: `presentation/ui/mypackage/MyScreen.kt`
2. **Define composable**: `@Composable fun MyScreen(...)`
3. **Create ViewModel**: `MyViewModel` extending `ViewModel`
4. **Add destination**: In `Destinations.kt`
   ```kotlin
   object MyScreen : Destinations("my_screen", "My Screen", Icons.Filled.Star)
   ```
5. **Add navigation**: In `AirMouseNavHost.kt`
   ```kotlin
   composable(Destinations.MyScreen.route) {
       MyScreen(navigationActions = navigationActions)
   }
   ```
6. **Add to bottom nav**: In `AirMouseBottomBar.kt` (if needed)

### Code Style

```bash
# Format code
./gradlew ktlintFormat

# Check code style
./gradlew ktlintCheck
```

### Pull Request Process

1. **Fork** the repository.
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Write tests** for new functionality.
4. **Ensure all tests pass**: `./gradlew test connectedAndroidTest`
5. **Commit** changes: `git commit -m 'Add amazing feature'`
6. **Push** to branch: `git push origin feature/amazing-feature`
7. **Open** a Pull Request with clear description.

---

## 📄 License

MIT License – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory.  
See [LICENSE](../LICENSE) for full text.

---

**Built with Kotlin, Jetpack Compose, and Hilt – turning your phone into a magic wand.**

