# Air Mouse Pro Android App – Complete, Extended Documentation

> **Professional‑grade smartphone remote control** using motion sensors, custom gestures, proximity lock, and multiple connectivity protocols.  
> *University of Tehran – Embedded Systems Laboratory*

[![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](../LICENSE)
[![Hilt](https://img.shields.io/badge/Hilt-Dependency_Injection-2A6DB6)](https://dagger.dev/hilt/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-UI_Toolkit-4285F4)](https://developer.android.com/jetpack/compose)

---

## Table of Contents

- [Air Mouse Pro Android App – Complete, Extended Documentation](#air-mouse-pro-android-app--complete-extended-documentation)
  - [Table of Contents](#table-of-contents)
  - [Overview \& Motivation](#overview--motivation)
  - [System Architecture](#system-architecture)
    - [2.1 Layered Clean Architecture](#21-layered-clean-architecture)
    - [2.2 Dependency Injection with Hilt](#22-dependency-injection-with-hilt)
    - [2.3 Data Flow Diagram (Text‑based)](#23-data-flow-diagram-textbased)
  - [Detailed Feature Breakdown](#detailed-feature-breakdown)
    - [3.1 Sensor Fusion \& Orientation](#31-sensor-fusion--orientation)
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
  - [User Interface (Jetpack Compose)](#user-interface-jetpack-compose)
    - [4.1 Navigation Graph](#41-navigation-graph)
    - [4.2 Theming \& Dynamic Colors](#42-theming--dynamic-colors)
    - [4.3 Reusable Components](#43-reusable-components)
  - [Data Persistence](#data-persistence)
    - [5.1 Room Database](#51-room-database)
    - [5.2 DataStore (Preferences)](#52-datastore-preferences)
  - [Background Services \& Foreground Notifications](#background-services--foreground-notifications)
  - [Permission Handling \& Security](#permission-handling--security)
  - [Performance Optimizations](#performance-optimizations)
  - [Testing Strategy](#testing-strategy)
    - [9.1 Unit Tests (JUnit + MockK + Kotlin Coroutines Test)](#91-unit-tests-junit--mockk--kotlin-coroutines-test)
    - [9.2 Instrumentation Tests (Espresso + Compose UI Test)](#92-instrumentation-tests-espresso--compose-ui-test)
    - [9.3 UI Tests (Compose UI Test)](#93-ui-tests-compose-ui-test)
  - [Troubleshooting \& Common Issues](#troubleshooting--common-issues)
  - [Development \& Contribution](#development--contribution)
    - [Building from Source](#building-from-source)
    - [Adding a New Screen](#adding-a-new-screen)
    - [Code Style](#code-style)
    - [Pull Request Process](#pull-request-process)
  - [License](#license)

---

## Overview & Motivation

The **Air Mouse Pro Android app** transforms any Android phone into a versatile, low‑latency remote control for desktop computers. Unlike conventional remote apps that rely only on touch input, Air Mouse Pro leverages the phone’s built‑in inertial sensors (gyroscope, accelerometer, magnetometer) to detect natural hand movements, allowing users to control the cursor simply by rotating or moving the phone in space.

**Motivations:**
- **Hands‑free interaction** – ideal for presentations, media centres, or when the keyboard/mouse is out of reach.
- **Low‑cost alternative** – no need for specialised hardware; uses existing smartphone sensors.
- **Customisability** – users can train their own gestures, adjust sensitivity, and choose from multiple connectivity protocols.
- **Privacy** – all voice recognition is offline (PocketSphinx), and personal data stays on‑device.

---

## System Architecture

### 2.1 Layered Clean Architecture

The app is structured into four distinct layers, each with a clear responsibility and dependency direction (inward).

```
┌────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                         │
│  • Jetpack Compose UI Screens                                 │
│  • ViewModels (state holders)                                 │
│  • Navigation (NavHost, Destinations)                         │
│  • Themes, animations, reusable composables                   │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                         Domain Layer                           │
│  • Entities (pure Kotlin data classes)                        │
│  • Use Cases (interactors)                                    │
│  • Repository interfaces                                      │
│  • Business logic (no Android dependencies)                   │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                          Data Layer                            │
│  • Repository implementations                                 │
│  • Data sources (WebSocket, Bluetooth, USB)                   │
│  • Local databases (Room, DataStore)                          │
│  • DTOs (network models)                                      │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                     Infrastructure Layer                       │
│  • SensorService (foreground service)                         │
│  • CalibrationHelper (bias removal, 6‑point accel)            │
│  • GestureDetector (threshold‑based)                          │
│  • MadgwickAHRS (sensor fusion)                               │
│  • PocketSphinx recognizer                                     │
│  • TFLite interpreter for custom gestures                     │
│  • Bluetooth HID, USB HID/serial                              │
│  • OkHttp, WebSocketManager                                   │
└────────────────────────────────────────────────────────────────┘
```

**Dependency Rule**: The Domain layer knows nothing about the Data or Infrastructure layers. Dependencies point inward, making the core business logic independent of frameworks.

### 2.2 Dependency Injection with Hilt

All dependencies are injected at compile time using **Dagger Hilt**. This eliminates boilerplate and makes testing easier.

- **`@HiltAndroidApp`** – in `AirMouseApplication`.
- **`@AndroidEntryPoint`** – in Activities, Fragments, Services, ViewModels.
- **Modules**: `AppModule`, `NetworkModule`, `SensorModule`, `DatabaseModule`, `RepositoryModule`, `UseCaseModule`, `ServiceModule`, `CoroutineModule`.

Example – providing the SensorManager:

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

### 2.3 Data Flow Diagram (Text‑based)

```
[User] → [Phone Sensors] → [SensorService] → [MadgwickFilter] → [GestureDetector]
                                                      │
                                                      ▼
                                            [Orientation (roll, pitch, yaw)]
                                                      │
                                                      ▼
                                            [WebSocketManager] → [TCP/WebSocket]
                                                      │
                                                      ▼
                                            [Desktop Server] → [Mouse/Keyboard]
```

Parallel flows:
- **Custom Gestures** → [TFLite model] → [GestureInferenceService] → [WebSocket]
- **Voice Commands** → [PocketSphinx] → [VoiceCommandService] → [WebSocket]
- **Proximity** → [Bluetooth RSSI] → [ProximityAwareService] → [WebSocket]
- **Edge Gestures** → [Accessibility Service] → [WebSocket]

---

## Detailed Feature Breakdown

### 3.1 Sensor Fusion & Orientation

**Why fusion?**  
Raw gyroscope data drifts over time; accelerometer alone cannot distinguish gravity from linear acceleration; magnetometer is noisy. The **Madgwick AHRS** algorithm fuses all three to produce a stable, drift‑free quaternion, from which we extract Euler angles (roll, pitch, yaw).

**Implementation** – `MadgwickAHRS.kt`:
- Beta = 0.1 (gain).
- Update rate = 50‑100 Hz (depends on sensor delay).
- Update steps: `updateGyro()` → `updateAccel()` → `updateMag()`.
- Output: quaternion (w, x, y, z) → converted to roll (X rotation) and yaw (Z rotation).

**Cursor mapping**:
- **Roll** (rotation around X) → vertical movement (pitch actually, but for the user it feels natural).
- **Yaw** (rotation around Z) → horizontal movement.
- Sensitivity multiplier (0.2–2.0) scales the deltas.

**Deadband**: Ignore movements below 0.3° to prevent jitter.

### 3.2 Gesture Detection Engine

| Gesture | Sensor | Detection Logic | Default Threshold |
|---------|--------|-----------------|------------------|
| **Click** | Gyroscope Y | Angular speed > threshold AND not within double‑click window | 5 rad/s |
| **Double‑click** | Gyroscope Y | Two clicks within `double_click_interval` (300 ms) | 300 ms |
| **Right‑click** | Accelerometer roll | Tilt angle (roll) > threshold AND hold for > duration | 15°, 200 ms |
| **Scroll up** | Accelerometer Y | Linear acceleration (positive) > threshold | 8 m/s² |
| **Scroll down** | Accelerometer Y | Linear acceleration (negative) < -threshold | 8 m/s² |

**Cooldowns**:
- After a click, the detector ignores further clicks for 100 ms to avoid repeated triggers.
- After a scroll, a debounce window (0.5 s) prevents the return movement from being interpreted as another scroll.

### 3.3 Calibration Procedures

#### Gyroscope Bias Removal
1. User places phone on a stationary surface.
2. App collects 500 gyroscope samples.
3. Computes average per axis – this is the bias.
4. Subtracts bias from all future readings.

#### Accelerometer 6‑Point Calibration
1. User holds phone in six orientations ( +X, -X, +Y, -Y, +Z, -Z ).
2. For each axis, measures the raw values when gravity aligns perfectly.
3. Solves: `raw = scale * ideal + offset` for each axis.
4. Resulting `offset` and `scale` are stored.

#### Magnetometer Hard‑Iron Calibration
1. User waves phone in a figure‑8 pattern.
2. App records min and max values for each axis.
3. Offset = `(min + max)/2`, scale = `(max - min)/2`.
4. Corrected mag = `(raw - offset) / scale`.

### 3.4 Connectivity Modules

| Module | Protocol | Features |
|--------|----------|----------|
| `WebSocketManager` | WebSocket | Automatic reconnection, exponential backoff, ping/pong, message queue |
| `DataSender` | TCP | ACK‑based reliable delivery for critical commands (clicks, double‑clicks) |
| `TcpClient` | TCP | Simple JSON‑line protocol for touchpad mode |
| `BluetoothMouseService` | Bluetooth HID | Emulates a standard Bluetooth mouse (requires system permission) |
| `UsbHidService` | USB HID | Acts as a USB mouse via OTG cable |
| `UsbSerialService` | USB CDC | Virtual serial port sending JSON commands |

**Reconnection logic**:  
- WebSocket attempts to reconnect with delays: 1s, 2s, 4s, … up to 30s.
- After reconnection, the app automatically sends a `hello` message to re‑identify.

### 3.5 Proximity Lock/Unlock

**Principle** – RSSI (Received Signal Strength Indicator) of a paired Bluetooth device decreases with distance. Using a path‑loss model:

```
distance = 10^((txPower - rssi) / (10 * n))
```

- `txPower` = calibrated RSSI at 1 metre (e.g., -59 dBm).
- `n` = environmental factor (2.5 for indoor office).

**Flow**:
1. `ProximityAwareService` runs in foreground, reading RSSI every second.
2. Distance is fed into a hysteresis comparator:
   - If already “near”, switch to “far” only when distance > `far_threshold`.
   - If already “far”, switch to “near” only when distance < `near_threshold`.
3. On state change, the app sends a `proximity` message to the server.
4. Server triggers screen lock/unlock via OS commands.

**Calibration**:
- User places phone exactly 1 m away and taps “Calibrate” → adjusts `txPower`.

### 3.6 Custom Gesture Recognition (TFLite)

**Pipeline**:
1. **Recording** – `GestureRecorderService` collects gyroscope + accelerometer data at 50 Hz for a user‑defined gesture name. Exports CSV.
2. **Training** – On PC, Python script trains a 1D‑CNN (or LSTM) and exports `gesture_model.tflite` and `gesture_labels.json`.
3. **Inference** – `GestureInferenceService` loads the TFLite model, buffers 30 consecutive sensor samples, and runs inference every 500 ms (cooldown). When confidence > 0.7, it sends a `gesture` message.

**Model architecture** (recommended):
- Input: 30 × 6 (gyro X,Y,Z + accel X,Y,Z).
- Conv1D(64, 3) → MaxPool → Conv1D(128, 3) → MaxPool → Flatten → Dense(128) → Dropout(0.5) → Dense(num_classes).

### 3.7 Voice Commands (PocketSphinx)

**Why offline?** Privacy and no internet dependency.  
**Setup**:
- PocketSphinx assets (`en-us-ptm`, `cmudict-en-us.dict`) are bundled in `assets/`.
- Grammar file (`commands.gram`) defines the command set.

**Workflow**:
1. `VoiceCommandService` starts listening on button press.
2. Recogniser triggers on partial results (end‑point detection).
3. On full result, maps command to action and sends appropriate network message.
4. Commands: “click”, “double click”, “right click”, “scroll up”, “scroll down”, “stop listening”.

### 3.8 Edge Gestures (Accessibility Service)

**Concept**: Long‑press volume keys (up/down) trigger actions even when the app is in the background or screen off.

**Implementation**:
- Accessibility service listens for key events.
- When a volume key is held for >1 s, the service sends a `control` message to the server (or directly executes an action via `ConnectionManager`).
- Actions are user‑configurable (click, double click, scroll, media keys, etc.).

**Permission**: The user must grant accessibility permission in system settings.

### 3.9 Touchpad Mode

When enabled, the screen becomes a full‑surface touchpad:
- **Single‑finger drag** → send `move` events with delta = touch movement (scaled).
- **Tap** → left click.
- **Two‑finger drag** → vertical scroll.
- **Two‑finger tap** → right click.
- **Three‑finger tap** → configurable (e.g., back).

**Implementation** – `TouchpadFragment` (or Compose `TouchpadScreen`) uses `MotionEvent` and `detectDragGestures`. The movement is sent via `TcpClient` or `WebSocketManager`.

### 3.10 Bluetooth HID Mouse

**How it works** (Android 8+):
- App uses `BluetoothHidDevice` system service.
- Registers a HID application with a mouse report descriptor.
- When a computer pairs and connects, the app can send HID reports (`sendMouseReport`).
- The computer sees the phone as a standard Bluetooth mouse.

**Limitations**:
- Requires system permission `BLUETOOTH_CONNECT` and may not work on all devices (depends on OEM implementation).
- Fallback to USB HID is provided.

### 3.11 USB HID / Serial

**USB HID** (`UsbHidService`):
- Phone appears as a USB mouse when connected via OTG.
- Sends mouse reports over the USB interrupt endpoint.
- Works on any OS (Linux, Windows, macOS) without special drivers.

**USB Serial** (`UsbSerialService`):
- Emulates a CDC serial port.
- Sends JSON commands over bulk endpoints.
- Useful for debugging or for custom applications.

---

## User Interface (Jetpack Compose)

### 4.1 Navigation Graph

The app uses Compose Navigation with a sealed class `Destinations`. The `NavHost` defines all screens and transitions.

**Bottom navigation** – four main tabs (Home, Statistics, Settings, Help). Other screens are accessible via the navigation drawer.

**State‑driven UI** – each screen has a `ViewModel` exposing a `StateFlow` of UI state. The Compose UI observes the state and recomposes accordingly.

### 4.2 Theming & Dynamic Colors

- `AirMouseTheme` composable wraps the app.
- Supports 15+ static themes (dark, light, pure black, ocean, sunset, etc.).
- On Android 12+, dynamic color (Material You) is also supported (extracts colors from wallpaper).

**Theme switching**:
- Theme is stored in `DataStore`.
- When changed, `setContent` recomposes with the new theme.

### 4.3 Reusable Components

- `ConnectionCard` – IP/port input, connect/disconnect button.
- `SensorDataCard` – shows live yaw/pitch.
- `GestureStatsCard` – displays click, scroll, right‑click, double‑click counts.
- `CalibrationCard` – progress bars, attempt counter.
- `GestureChart` – bar chart using MPAndroidChart.

All components are fully `@Composable` and use Material 3.

---

## Data Persistence

### 5.1 Room Database

**Entities**:
- `GyroBias` – stores bias for each axis.
- `AccelCalibration` – offset and scale for accel.
- `MagCalibration` – offset and scale for mag.
- `CustomGestureTemplate` – name, serialised feature data, threshold.
- `Profile` – name, sensitivity, thresholds, theme, AI settings.

**DAOs**: Provide suspend functions for CRUD operations.

### 5.2 DataStore (Preferences)

Used for:
- Last connected IP/port.
- Sensitivity, click threshold, scroll threshold, double‑click interval, etc.
- Haptic feedback toggle.
- Theme name.
- Calibration attempts counter.
- Server logs (last 200 lines).

**Observing changes**: `Flow<Preferences>` for reactive UI updates.

---

## Background Services & Foreground Notifications

| Service | Foreground Type | Purpose |
|---------|----------------|---------|
| `SensorService` | `dataSync` | Collects sensor data, runs Madgwick filter, sends movements |
| `GestureInferenceService` | `dataSync` | Runs TFLite inference for custom gestures |
| `VoiceCommandService` | `microphone` | Offline speech recognition |
| `ProximityAwareService` | `location` | Monitors Bluetooth RSSI |
| `BluetoothMouseService` | `connectedDevice` | Bluetooth HID mouse emulation |
| `UsbHidService` | `dataSync` | USB HID mouse mode |
| `UsbSerialService` | `dataSync` | USB serial communication |

All services run as foreground services with a persistent notification, ensuring they are not killed by the system.

---

## Permission Handling & Security

**Required permissions** (declared in `AndroidManifest.xml`):
- `INTERNET`, `ACCESS_NETWORK_STATE` – for network communication.
- `CAMERA` – for QR scanning.
- `VIBRATE` – haptic feedback.
- `BLUETOOTH`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` – Bluetooth HID and proximity.
- `ACCESS_FINE_LOCATION` – required for Bluetooth scanning (Android 10+).
- `RECORD_AUDIO` – voice commands.
- `BODY_SENSORS` – accelerometer, gyroscope, magnetometer.
- `FOREGROUND_SERVICE` (and specific subtypes) – for background services.
- `SYSTEM_ALERT_WINDOW` – debug overlay.
- `WRITE_EXTERNAL_STORAGE` – exporting gesture datasets.

**Permission flow**:
- Request dangerous permissions at runtime using `ActivityResultContracts`.
- Show rationale dialogs when needed.

**Security**:
- No user data is sent to external servers (except the desktop server on the local network).
- The desktop server can optionally require JWT authentication (token embedded in WebSocket URL).

---

## Performance Optimizations

1. **Sensor sampling rate** – dynamically adjusted by `BatterySaver`:
   - When phone is stationary for 10 s, switch to `SENSOR_DELAY_NORMAL` (lower power).
   - On movement, revert to `SENSOR_DELAY_GAME` (high rate).

2. **Efficient JSON generation** – use `JSONObject` pooling (not implemented in current version but possible extension).

3. **Coalescing movement events** – In touchpad mode, movement events are batched and sent at 60 Hz (16 ms intervals).

4. **Background work** – All network and sensor processing runs on coroutines with `Dispatchers.IO`.

5. **TFLite inference** – Runs on a separate thread (`Dispatchers.Default`), not blocking the sensor callback.

6. **Logging** – Limited to last 500 entries, automatically trimmed.

---

## Testing Strategy

### 9.1 Unit Tests (JUnit + MockK + Kotlin Coroutines Test)

- **`ValidationUtilsTest`** – IP parsing, endpoint extraction.
- **`PreferencesManagerTest`** – save/load, increment counters.
- **`GestureDetectorTest`** – threshold detection, double‑click window.
- **`CalibrationHelperTest`** – bias calculation, 6‑point formula.
- **`MadgwickAHRSUnitTest`** – quaternion integration, Euler conversion.

**Mocking**: Use `mockk` for dependencies, `runTest` for coroutines.

### 9.2 Instrumentation Tests (Espresso + Compose UI Test)

- **`HomeScreenTest`** – verifies connection card, start/stop button.
- **`CalibrationScreenTest`** – checks step navigation, progress updates.
- **`GestureStudioActivityTest`** – records a gesture, exports dataset.

Use `HiltAndroidRule` to inject test dependencies.

### 9.3 UI Tests (Compose UI Test)

- Use `createComposeRule()` to test composables in isolation.
- Assert node existence, text content, click events.

**Continuous Integration** – GitHub Actions runs all tests on every push.

---

## Troubleshooting & Common Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Cannot connect | Wi‑Fi mismatch, firewall | Verify same network; disable firewall temporarily. |
| Cursor jumps erratically | Gyroscope bias not calibrated | Run gyroscope calibration on a flat surface. |
| Gestures not recognised | Threshold too high | Lower click/scroll thresholds in Settings. |
| Voice commands not working | Microphone permission denied | Grant permission; check PocketSphinx assets. |
| Bluetooth mouse not pairing | Android version / OEM limitation | Use USB HID mode instead. |
| Proximity lock never triggers | Bluetooth not paired or RSSI too low | Pair phone with computer; calibrate distance. |
| App crashes on opening | Missing TFLite model or labels | Ensure `gesture_model.tflite` and `gesture_labels.json` are in `assets/`. |

---

## Development & Contribution

### Building from Source

```bash
git clone https://github.com/yourusername/airmouse-android.git
cd airmouse-android
./gradlew assembleDebug
```

### Adding a New Screen

1. Create `MyScreen.kt` in `presentation/ui/mypackage/`.
2. Define a `@Composable` function and a `ViewModel` (if needed).
3. Add a destination in `Destinations.kt`.
4. Add an entry in `NavGraph.kt`.
5. Add a bottom nav item (if required) in `BottomNavItems.kt`.

### Code Style

- Use Ktlint (`./gradlew ktlintFormat`).
- Follow the official Kotlin coding conventions.

### Pull Request Process

1. Fork the repository.
2. Create a feature branch.
3. Write tests for new functionality.
4. Ensure all tests pass.
5. Submit a PR with a clear description.

---

## License

MIT License – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory.  
See [LICENSE](../LICENSE) for full text.

---

**Built with Kotlin, Jetpack Compose, and Hilt – turning your phone into a magic wand.**