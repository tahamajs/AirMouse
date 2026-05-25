
---

## `docs/CODE_WALKTHROUGH.md`

```markdown
# Code Walkthrough – Line by Line

This document explains every important file in the Air Mouse project.

## 1. `MainActivity.kt`

**Purpose:** UI handling, user interaction, starting/stopping services.

**Key sections:**
- `onCreate`: binds UI elements, initialises helper classes, sets button listeners.
- `requestPermissions`: asks for Internet permission.
- `startCalibration`: launches calibration coroutine; updates UI status.
- `startAirMouse`: validates IP, creates `DataSender`, starts `SensorService`, sets callbacks.
- `updateUIIndicator`: rotates the green square based on yaw.

**Important:** The delta calculation uses `lastOrientation` to compute relative change, not absolute position.

## 2. `sensors/SensorService.kt`

**Purpose:** Manages sensors, runs Madgwick fusion, exposes orientation and gesture callbacks.

**Key methods:**
- `start()`: registers listeners with `SENSOR_DELAY_GAME` (~20 ms).
- `stop()`: unregisters.
- `onSensorChanged`: dispatches to correct sensor, updates Madgwick, computes roll/yaw, calls callbacks.

**Note:** `timestamp` is used to compute `dt` (delta time) for gyro integration. Without it, integration would be wrong.

## 3. `sensors/MadgwickAHRS.kt`

**Purpose:** Implements the Madgwick algorithm.

**Key variables:**
- `q0..q3`: quaternion representing orientation.
- `beta`: filter gain (default 0.1).

**Key methods:**
- `updateGyro`: predicts orientation using gyroscope.
- `updateAccel` / `updateMag`: store latest values for correction.
- `getRoll`, `getYaw`: convert quaternion to Euler angles.

**Math inside:** Gradient descent to correct gyro integration using accelerometer and magnetometer.

## 4. `sensors/CalibrationHelper.kt`

**Purpose:** Collects and applies bias/offset/scale for all three sensors.

**Methods:**
- `calibrateGyro`: averages 500 samples while stationary.
- `calibrateMagnetometer`: finds min/max over 30 seconds, computes offset/scale.
- `calibrateAccelerometer`: placeholder (simplified).

**Usage:** Corrected values are obtained via `correctGyro`, `correctMagnetometer`, etc.

## 5. `sensors/MotionDetector.kt`

**Purpose:** Detects click and scroll gestures from raw sensor values.

**Click detection:** Checks angular velocity of gyro Y against threshold, with cooldown.
**Scroll detection:** Checks linear acceleration of accelerometer Y against threshold, with debounce.

**Return values:** Boolean for click, Int (-1,0,1) for scroll.

## 6. `network/DataSender.kt`

**Purpose:** TCP client that sends JSON packets to PC, with ACK and retransmission for critical packets.

**Key components:**
- `queue`: `LinkedBlockingQueue` for thread‑safe message passing.
- `pendingAcks`: `ConcurrentHashMap` storing messages awaiting ACK.
- **Sender loop:** takes messages from queue, writes to socket. For click/scroll, starts a 500 ms timer; if no ACK, retransmits.
- **ACK receiver thread:** reads incoming lines, looks for `"ack"`, removes from `pendingAcks`.

## 7. `pc/server.py` (or `gui_server.py`)

**Purpose:** Receives TCP messages, controls mouse using `pyautogui`.

**Key classes:**
- `MouseController`: wraps `pyautogui` with sensitivity and clipping.
- `AirMouseServer`: asyncio TCP server; `handle_client` reads lines, parses JSON, calls mouse methods, sends ACK.

**GUI version:** uses `tkinter` for start/stop and log display.

## 8. UI Layouts (`activity_main.xml`, `fragment_calibration.xml`)

**activity_main.xml:** Contains IP input, buttons, status text, and a green square (`View` with background drawable).
**fragment_calibration.xml:** Progress bar and status text for calibration steps.

## 9. AndroidManifest.xml

**Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE`.  
**Features:** `accelerometer`, `gyroscope`, `magnetometer` required.  
**Cleartext traffic:** allowed for local network.

## 10. Build Files (`build.gradle`)

**Project‑level:** declares Android Gradle plugin.  
**Module‑level:** sets `minSdk 29`, `targetSdk 34`, dependencies (Kotlin, JSON library).

---

## Common Code Flow

1. User opens app → `MainActivity.onCreate`.
2. User taps "Calibrate" → `startCalibration` → `CalibrationHelper` methods run.
3. User enters IP, taps "Start" → `startAirMouse` → creates `DataSender` thread, starts `SensorService`.
4. `SensorService` registers sensors → `onSensorChanged` called at ~50 Hz.
5. Raw sensors → calibration → Madgwick fusion → orientation (roll, yaw) → delta computed → sent via `DataSender`.
6. `DataSender` sends JSON over TCP.
7. PC server receives, moves cursor, sends ACK for clicks.
8. User stops app → `onDestroy` stops services.

---

**This walkthrough should make you comfortable modifying any part of the code.**