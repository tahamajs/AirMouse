# ✈️ Air Mouse Ultimate – Complete Documentation

**University of Tehran – Faculty of Electrical and Computer Engineering**  
*Embedded Systems Exercise – Second Semester 1404-1405 (2025-2026)*

**Designers:** Arian Firoozi, Arsalan Talaee  
**Instructors:** Dr. Mohsen Shokri, Dr. Mehdi Kargahi

---

## 📖 Table of Contents

1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Features](#features)
4. [Requirements](#requirements)
5. [Quick Start](#quick-start)
6. [Android App – Detailed Usage](#android-app--detailed-usage)
7. [PC Server – Detailed Usage](#pc-server--detailed-usage)
8. [Calibration Guide](#calibration-guide)
9. [Gesture Detection & Sensitivity](#gesture-detection--sensitivity)
10. [Network Protocol Specification](#network-protocol-specification)
11. [Building the APK Without Android Studio](#building-the-apk-without-android-studio)
12. [Perfetto Analysis – Answers to 11 Questions](#perfetto-analysis--answers-to-11-questions)
13. [Troubleshooting](#troubleshooting)
14. [Advanced Customisation](#advanced-customisation)
15. [Video Demonstration Script](#video-demonstration-script)
16. [Submission Checklist](#submission-checklist)
17. [Credits & License](#credits--license)

---

## Overview

Air Mouse transforms your Android smartphone into a **wireless, motion‑controlled mouse**. By rotating and moving the phone in the air, you can:

- Move the computer cursor (horizontal & vertical)
- Perform left‑click, double‑click, right‑click
- Scroll up/down

The phone sends processed motion data over WiFi (TCP) to a Python server on your laptop, which controls the mouse using `pyautogui`. The system uses **Madgwick sensor fusion** to eliminate drift and provides **configurable gesture thresholds** via an on‑phone settings dialog.

This project is designed for the University of Tehran’s Embedded Systems course. It demonstrates:

- Android sensor programming (accelerometer, gyroscope, magnetometer)
- Sensor fusion (Madgwick AHRS) for drift‑free orientation
- Gesture recognition from motion patterns
- TCP/IP communication with ACK‑based reliability
- Cross‑platform Python server with GUI
- Performance analysis using Perfetto

---

## Project Structure

```
AirMouse-Ultimate/
├── android/                     # Android Studio project (Kotlin)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/airmouse/   # All Kotlin source code
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── sensors/
│   │   │   │   │   ├── SensorService.kt
│   │   │   │   │   ├── MadgwickAHRS.kt
│   │   │   │   │   ├── CalibrationHelper.kt
│   │   │   │   │   ├── EnhancedGestureDetector.kt
│   │   │   │   │   └── SensorDataLogger.kt
│   │   │   │   ├── network/
│   │   │   │   │   ├── DataSender.kt
│   │   │   │   │   └── AutoReconnect.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainViewModel.kt
│   │   │   │   │   ├── CalibrationFragment.kt
│   │   │   │   │   ├── SettingsFragment.kt
│   │   │   │   │   ├── DebugOverlayService.kt
│   │   │   │   │   └── DebugOverlay.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── SensorRepository.kt
│   │   │   │   │   ├── PreferencesDataStore.kt
│   │   │   │   │   └── NetworkRepository.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── GestureDetector.kt
│   │   │   │   │   ├── MadgwickFusion.kt
│   │   │   │   │   └── CalibrationUseCase.kt
│   │   │   │   └── utils/
│   │   │   │       ├── BatterySaver.kt
│   │   │   │       ├── Constants.kt
│   │   │   │       ├── Extensions.kt
│   │   │   │       ├── PreferencesManager.kt
│   │   │   │       ├── PermissionHelper.kt
│   │   │   │       ├── NetworkUtils.kt
│   │   │   │       └── SensorUtils.kt
│   │   │   └── res/                 # Layouts, drawables, values
│   │   └── build.gradle
│   └── gradle/                  # Gradle wrapper
├── pc/                          # Python PC server
│   ├── gui.py                   # Dark‑mode GUI server
│   ├── server.py                # Console server with config file
│   ├── run.py                   # Launcher (auto‑installs dependencies)
│   ├── run.sh / run.bat         # One‑click scripts
│   ├── requirements.txt
│   ├── config.json
│   └── perfetto_analyzer.py     # Tool to answer the 11 Perfetto questions
├── build_apk.sh / build_apk.bat # Scripts to build APK without Android Studio
└── README.md                    # This file (complete documentation)
```

Each directory and file is described in detail in the respective sections of this README.

---

## Features

The Air Mouse project includes a rich set of features beyond the basic requirements. Below is a complete table with status and brief description.

| Feature | Status | Description |
|---------|--------|-------------|
| Smooth cursor movement (roll → vertical, yaw → horizontal) | ✅ | Uses Madgwick fusion to map orientation to cursor deltas. |
| Left click (quick flick around Y‑axis) | ✅ | Detected when gyro Y angular speed exceeds threshold. |
| Double click (two flicks within 400 ms) | ✅ | Time‑based double‑click detection with configurable interval. |
| Right click (tilt >45° for 0.5 seconds) | ✅ | Long‑hold tilt detection using roll angle and duration. |
| Scroll up/down (quick linear push along Y‑axis) | ✅ | Uses accelerometer Y linear acceleration to trigger scroll. |
| Sensitivity slider (0.2x – 2.0x) | ✅ | Live adjustment on the main screen, saved to preferences. |
| Haptic feedback (vibration on clicks) | ✅ | Vibrates for 30 ms (click), 50 ms (double), 80 ms (right). Can be disabled. |
| Visual click flash (green square turns red) | ✅ | Provides immediate visual confirmation of any click. |
| Debug overlay (live sensor values) | ✅ | Floating window showing roll, yaw, gyroY, accelY. Requires overlay permission. |
| Auto‑reconnect (TCP connection recovery) | ✅ | DataSender retries every 5 seconds; AutoReconnect monitors health. |
| Battery saver (reduces sampling rate when idle) | ✅ | After 10 seconds of no movement, sampling drops from 50 Hz to 20 Hz. |
| Calibration dialogs (gyro, magnetometer, accelerometer) | ✅ | Step‑by‑step guided calibration with progress bar. |
| Settings dialog (adjustable thresholds) | ✅ | All gesture thresholds can be changed without code changes. |
| ACK‑based reliability for critical packets | ✅ | Click/scroll packets wait 500 ms for ACK; retransmit once if missing. |
| PC server with dark mode GUI and logging | ✅ | Both GUI and console versions; logging to file and console. |
| Perfetto trace analysis script | ✅ | Python script that answers the 11 required questions from a trace. |
| Build APK without Android Studio | ✅ | Shell/batch scripts that install SDK, accept licenses, and build. |

---

## Requirements

### Android Side

| Requirement | Explanation | Why needed |
|-------------|-------------|------------|
| **Android device with API level 29+ (Android 10 or higher)** | The app uses modern sensor APIs and permissions (e.g., `SYSTEM_ALERT_WINDOW`). Lower versions may lack some features. | The exercise specifies minSdk 29. |
| **Accelerometer** (required) | Measures gravity and linear acceleration. Used for tilt (roll/pitch) and scroll detection. | Without it, tilt and scroll will not work. |
| **Gyroscope** (required) | Measures angular velocity. Used for flick (click/double‑click) detection and gyro integration in fusion. | Without gyro, click detection and orientation would be slow and noisy. |
| **Magnetometer** (required) | Measures Earth’s magnetic field. Used to correct yaw drift in sensor fusion. | Without magnetometer, yaw will drift over time (cursor slowly rotates). |
| **WiFi connection** (same network as laptop) | TCP requires both devices to be on the same local subnet. | The phone must reach the PC server’s IP address. |
| **USB debugging (optional for development)** | Needed to install APK via ADB or Android Studio. | Not required for end‑user installation (sideload APK). |

### PC Side

| Requirement | Explanation | Why needed |
|-------------|-------------|------------|
| **Windows / Linux / macOS** | The Python server is cross‑platform and tested on all three. | The exercise allows any OS. |
| **Python 3.8+** | Uses `asyncio`, `f-strings`, and other modern features. | `pyautogui` and `perfetto` packages require Python 3.8+. |
| **`pyautogui` library** | Controls mouse cursor, clicks, and scroll. | Essential for the server to simulate mouse actions. |
| **WiFi connection** (same network as phone) | The server listens on a local IP; the phone must reach it. | Without it, TCP connection fails. |
| **Accessibility permission (macOS only)** | On macOS, `pyautogui` cannot control the mouse unless the terminal or Python app has Accessibility permission. | Security restriction. |
| **Firewall allowance for port 8080** | The server listens on port 8080 (default). Firewall may block incoming connections. | Needed to allow the phone to connect. |

---

## Quick Start

This section gives the fastest path from zero to a working Air Mouse. Follow these steps exactly.

### 1. Build & Install the Android App

#### Option A: Using the Provided Script (No Android Studio)

This method is recommended if you don’t have Android Studio or want an automated build.

**Prerequisites:**
- Java 11 JDK installed (`java -version` should show 11.x)
- Android command‑line tools extracted to `~/android-sdk` (Linux/macOS) or `%USERPROFILE%\android-sdk` (Windows)

**Steps:**
```bash
# macOS / Linux
chmod +x build_apk.sh
./build_apk.sh

# Windows (Command Prompt or PowerShell)
build_apk.bat
```

**What the script does:**
1. Checks Java version and Android SDK location.
2. Automatically accepts all SDK licenses (using `yes | sdkmanager --licenses`).
3. Installs `build-tools;29.0.3`, `platforms;android-29`, and `platform-tools`.
4. Runs `./gradlew clean assembleDebug` in the `android` folder.
5. Prints the APK location: `android/app/build/outputs/apk/debug/app-debug.apk`

**After success:**
- Copy the APK to your phone via USB, cloud, or `adb install`.
- On the phone, open the APK and allow “Install from unknown sources” if prompted.

#### Option B: Using Android Studio

If you already have Android Studio installed, this is simpler for development.

**Steps:**
1. Open Android Studio → **Open an Existing Project** → select the `android` folder.
2. Wait for Gradle sync to complete.
3. Enable **USB debugging** on your phone (Settings → Developer options → USB debugging).
4. Connect your phone via USB. Accept the RSA key fingerprint.
5. Click the green **Run** button (▶). The app will be installed and launched automatically.

### 2. Run the PC Server

Navigate to the `pc` folder in a terminal.

**Using the unified launcher (recommended for first time):**
```bash
cd pc
python run.py
```
Or double‑click `run.bat` (Windows) / `./run.sh` (macOS/Linux).

**What `run.py` does:**
- Checks if `pyautogui` is installed; if not, runs `pip install -r requirements.txt`.
- Launches `gui.py` (dark mode GUI server).

**Alternative – console server (no GUI):**
```bash
python server.py
```

The server will print:
```
Server listening on 0.0.0.0:8080
```

**Finding your PC’s IP address:**
- Windows: `ipconfig` → look for `IPv4 Address` under your active WiFi adapter.
- macOS/Linux: `ifconfig` or `ip addr` → look for `inet 192.168.x.x` or `10.x.x.x` (ignore `127.0.0.1`).

### 3. Connect & Use

1. On the phone, open the **Air Mouse Ultimate** app.
2. Enter your PC’s IP address in the text field.
3. Tap **Calibrate Sensors** and follow the three steps:
   - **Gyro bias:** Place the phone flat on a stationary surface (table) – do **not** hold it.
   - **Magnetometer:** Move the phone in a figure‑8 pattern for 30 seconds, covering all orientations.
   - **Accelerometer (simplified):** Keep the phone still on a flat surface.
4. After calibration, tap **Start Air Mouse**.
5. The status changes to “Air Mouse Active”. Now rotate and move the phone to control the mouse!

---

## Android App – Detailed Usage

This section explains every user interface element and every gesture in exhaustive detail.

### Main Screen Controls

The main screen is defined in `res/layout/activity_main.xml`. It consists of the following elements (from top to bottom):

| UI Element | ID | Type | Action / Behaviour |
|------------|----|------|---------------------|
| **IP address field** | `ip_edit_text` | `TextInputEditText` | Enter the PC’s IP address. The last used IP is saved and pre‑filled next time. |
| **Sensor status text** | `sensor_status_text` | `TextView` | Shows “All sensors available” or “Missing sensors: ...”. Disappears after calibration. |
| **Calibrate Sensors** | `calibrate_btn` | `MaterialButton` | Opens the calibration dialog (see Calibration Guide). |
| **Start Air Mouse** | `start_btn` | `MaterialButton` | Attempts TCP connection to PC and starts sensor streaming. Disabled until calibration is done. |
| **Status text** | `status_text` | `TextView` | Displays “Not connected”, “Calibrating...”, “Air Mouse Active”, or error messages. |
| **Cursor Speed slider** | `sensitivity_seekbar` | `SeekBar` | Maps 0–100 to 0.2–2.0 sensitivity. Value is saved only when user stops touching (debounced). |
| **Speed value** | `sensitivity_text` | `TextView` | Shows the current speed as “0.50x”, “1.20x”, etc. |
| **Settings** | `settings_btn` | `MaterialButton` | Opens the Settings dialog to adjust gesture thresholds and haptic feedback. |
| **Show/Hide Debug** | `debug_toggle_btn` | `MaterialButton` | Toggles the floating debug overlay (requires overlay permission). |
| **Green square** | `orientation_view` | `View` | Rotates according to phone’s yaw angle (horizontal orientation). Flashes red on any click. |

### Gesture Mapping

All gestures are detected by `EnhancedGestureDetector.kt`. The mapping is as follows:

| Phone movement | Action | Sensor used | Detection condition |
|----------------|--------|-------------|----------------------|
| **Rotate around Z‑axis (left/right)** | Move cursor horizontally | Yaw angle (from Madgwick fusion) | Every orientation change generates `dx = (yaw_delta) * sensitivity * 0.8`. |
| **Rotate around X‑axis (nod up/down)** | Move cursor vertically | Roll angle (from Madgwick fusion) | Every orientation change generates `dy = (roll_delta) * sensitivity * 0.8`. |
| **Quick flick around Y‑axis (sharp rotation)** | Left click | Gyroscope Y angular velocity | `|gyroY| > clickSpeedThreshold` (default 5 rad/s). |
| **Two quick flicks within 400 ms** | Double click | Gyroscope Y (two events) | Second flick occurs before `doubleClickInterval` elapses. |
| **Tilt sideways >45° and hold for 0.5 seconds** | Right click | Roll angle (from fusion) | `|roll| > rightClickTiltAngle` for longer than `rightClickDuration`. |
| **Quick linear push up/down along Y‑axis** | Scroll up/down | Accelerometer Y linear acceleration | `|accelY| > scrollSpeedThreshold` (default 8 m/s²). Direction determines scroll up/down. |

#### Visual Feedback

- **Green square rotation:** The square’s rotation angle in degrees is set to the yaw angle (in degrees). This gives a direct visual indication of the phone’s horizontal orientation.
- **Click flash:** When any click occurs, the square’s background colour is changed to red for 100 ms, then back to green. This provides immediate, low‑latency feedback.
- **Haptic feedback:** The phone vibrates with different durations:
  - Single click: 30 ms
  - Double click: 50 ms
  - Right click: 80 ms
- **Toasts:** For double click and right click, a short toast message appears (“Double click”, “Right click”).

#### Debug Overlay

When you tap **Show Debug** (and grant overlay permission), a small floating window appears (top‑left corner). It displays:

- **Roll:** Current roll angle in degrees (vertical movement).
- **Yaw:** Current yaw angle in degrees (horizontal movement).
- **GyroY:** Angular velocity around Y‑axis in rad/s (click detection).
- **AccelY:** Linear acceleration along Y‑axis in m/s² (scroll detection).

This overlay is invaluable for tuning gesture thresholds and verifying that sensors are working. It updates at the same rate as sensor events (≈50 Hz).

---

## PC Server – Detailed Usage

The PC server is written in Python and runs on Windows, macOS, or Linux. It receives JSON messages over TCP and uses `pyautogui` to control the mouse.

### Two Versions

| Version | File | Description | Best for |
|---------|------|-------------|----------|
| GUI (recommended) | `gui.py` | Dark mode window with Start/Stop buttons, live log, sensitivity slider, status indicator. | Daily use, demonstrations, debugging. |
| Console | `server.py` | Reads `config.json`, logs to file and console, no GUI. | Headless setups, servers, automation. |

### GUI Server (`gui.py`) – In‑depth

#### User Interface

When you run `gui.py`, a window appears with:

- **Title bar:** “✈️ Air Mouse Server”
- **Status dot:** Red when stopped, light green when running.
- **Connection Log:** A scrollable text area showing all events: connections, disconnections, clicks, scrolls, errors, sensitivity changes.
- **Start Server button** (green, ▶) – starts the TCP listener.
- **Stop Server button** (grey, ⏹) – stops the server and disconnects all clients.
- **Mouse Sensitivity slider:** Range 0.2–2.0, with a label showing the current value. Moving the slider updates the sensitivity of the `MouseController` immediately.
- **Footer:** “University of Tehran – Embedded Systems Exercise”

#### Behaviour

- **Start Server:** Creates an asyncio event loop in a background thread and starts listening on `0.0.0.0:8080` (all network interfaces). The log shows `🚀 Server listening on 0.0.0.0:8080`.
- **Connection:** When a client (Android phone) connects, the log shows `✅ Connected: ('192.168.1.15', 54321)`.
- **Message processing:** For each incoming JSON line:
  - `move` → `pyautogui.moveRel(dx, dy)` with sensitivity applied. No ACK.
  - `click` → `pyautogui.click()`, sends ACK, logs `🖱️ Click`.
  - `doubleclick` → `pyautogui.doubleClick()`, sends ACK, logs `🖱️🖱️ Double-click`.
  - `rightclick` → `pyautogui.click(button='right')`, sends ACK, logs `🖱️ Right-click`.
  - `scroll` → `pyautogui.scroll(delta)`, sends ACK, logs `📜 Scroll 1` or `-1`.
- **Disconnection:** When the client closes the socket, the log shows `🔌 Disconnected: ('192.168.1.15', 54321)`.
- **Stop Server:** The server stops accepting new connections and closes the listening socket. The log shows `🛑 Server stopped by user`.

#### Sensitivity Slider

The slider controls a multiplier applied to `dx` and `dy` before moving the cursor. The default is 0.5. Changing the slider updates `CONFIG["sensitivity"]` and the `mouse.sensitivity` attribute of the running server. No restart is required.

### Console Server (`server.py`) – In‑depth

The console server is designed for lightweight or headless operation. It reads settings from `config.json` and logs to both the console and a file.

#### Configuration File (`config.json`)

If the file does not exist, it is created with default values.

```json
{
    "host": "0.0.0.0",
    "port": 8080,
    "sensitivity": 0.5,
    "log_level": "INFO",
    "log_file": "airmouse.log"
}
```

| Field | Type | Description | Valid values |
|-------|------|-------------|--------------|
| `host` | string | IP address to bind to. `0.0.0.0` listens on all interfaces. | Any IP, or `0.0.0.0` |
| `port` | integer | TCP port number. | 1024–65535 (privileged ports <1024 need admin) |
| `sensitivity` | float | Global sensitivity multiplier. | 0.2–2.0 |
| `log_level` | string | Logging verbosity. | `DEBUG`, `INFO`, `WARNING`, `ERROR` |
| `log_file` | string | Path to the log file (relative to the server’s directory). | e.g., `"airmouse.log"` |

#### Logging

- **Console:** All log messages are printed to `stderr`/`stdout` with timestamp, level, and message.
- **File:** Messages are appended to the file specified in `log_file`. The file grows indefinitely; you may rotate it manually.

**Example log output:**
```
2025-05-26 15:30:01,123 - INFO - Server listening on 0.0.0.0:8080
2025-05-26 15:30:05,456 - INFO - Connected: ('192.168.1.15', 54321)
2025-05-26 15:30:05,789 - INFO - Click: left
2025-05-26 15:30:06,012 - INFO - Scroll: 1
2025-05-26 15:30:10,234 - INFO - Disconnected: ('192.168.1.15', 54321)
```

#### Running the Console Server

```bash
python server.py
```

Press `Ctrl+C` to stop gracefully.

### Running the Server – OS‑Specific Instructions

| OS | GUI version | Console version |
|----|-------------|-----------------|
| **macOS/Linux** | `./run.sh` or `python gui.py` | `python server.py` |
| **Windows** | double‑click `run.bat` or `python gui.py` | `python server.py` |

**Note for macOS:** You must grant Accessibility permission to the terminal or Python app (`Settings → Privacy & Security → Accessibility`). Otherwise `pyautogui` will not move the cursor.

**Note for Linux:** If you get an error about `tkinter`, install `python3-tk` (Ubuntu/Debian) or the equivalent. On Wayland, `pyautogui` may not work – switch to X11 session.

---

## Calibration Guide

Calibration is **essential** for accurate, drift‑free performance. The app guides you through three steps.

### Step 1: Gyroscope Bias Calibration

**Purpose:** Remove the constant offset (bias) that causes the cursor to drift even when the phone is stationary.

**How it works:** The app collects 500 gyroscope samples (≈2‑3 seconds) while the phone is completely still. The average of these samples becomes the bias for each axis. During normal operation, each gyro reading is corrected as `corrected = raw - bias`.

**User action:**
- Place the phone on a **flat, stationary surface** (e.g., a table, a hardcover book).
- **Do not hold it** – your hand’s micro‑vibrations will be averaged into the bias, ruining calibration.
- Do not move or tap the phone during the 3 seconds.

**Common mistake:** Holding the phone in your hand. The result is that the bias includes hand tremor, and the cursor will still drift.

**How to verify success:** After calibration, place the phone still on the table. The cursor should not move. If it drifts, re‑do gyro calibration.

### Step 2: Magnetometer Hard‑Iron Calibration

**Purpose:** Remove the constant offset caused by nearby magnets (laptop speakers, metal table, phone’s own components). This offset makes the yaw (horizontal orientation) permanently wrong.

**How it works:** While you move the phone in a figure‑8 pattern for 30 seconds, the app records the minimum and maximum values for each axis. It then computes:
```
offset_i = (max_i + min_i) / 2
scale_i = (max_i - min_i) / 2
```
During normal operation: `corrected = (raw - offset) / scale`.

**User action:**
- Pick up the phone and move it in a **continuous figure‑8 pattern**.
- Imagine drawing a horizontal figure‑8 (∞ symbol) in the air.
- Rotate your wrist so that the phone’s axes point in all possible directions.
- Cover all orientations: up, down, left, right, tilt forward, tilt backward, rotate around each axis.
- Continue for the full 30 seconds (the progress bar shows time remaining).

**Why figure‑8?** A simple circular motion would only cover two axes; the figure‑8 forces the phone to pass through all three axes’ extreme values. This ensures accurate min/max for each axis.

**Common mistakes:**
- Moving only in a circle → some axes never reach extremes.
- Moving too slowly → may not cover all orientations within 30 seconds.
- Calibrating near a laptop speaker or metal object → the offset includes those external fields; yaw will still be affected when you move away.

**How to verify success:** Rotate the phone in a circle. The cursor should move horizontally without vertical drift. If the yaw drifts over time, re‑calibrate away from metal.

### Step 3: Accelerometer Calibration (Simplified)

**Purpose:** Correct offset errors in the accelerometer that affect tilt (roll/pitch). The simplified method assumes factory scale is correct and only corrects offset.

**How it works:** The app collects 200 stationary samples (≈3 seconds) and computes the average. For X and Y axes, the offset is the average. For Z, it’s `avgZ - 9.81` (because gravity should be 9.81 m/s² pointing down). No scale correction is applied.

**User action:** Keep the phone still on a flat surface for about 3 seconds.

**Limitations:** The simplified method works well for most phones. If you notice that the tilt is off (e.g., when you hold the phone level, the cursor still moves vertically), you should implement the **full 6‑point calibration** (see Advanced Customisation). The full method corrects both offset and scale by measuring the phone in six orientations (±X, ±Y, ±Z).

### When to Re‑calibrate

- After first installation – mandatory.
- When you change location (different magnetic environment).
- If you notice cursor drift (phone still but cursor moves) – re‑do gyro and magnetometer.
- If the yaw (horizontal orientation) seems incorrect – re‑do magnetometer.
- After the phone has been near strong magnets (e.g., placed on a speaker, near a fridge magnet).
- If the tilt seems off – re‑do accelerometer.

---

## Gesture Detection & Sensitivity

All gesture thresholds are adjustable via the **Settings** dialog. Changes take effect immediately – no need to restart the app.

### Settings Dialog Parameters

The dialog (implemented in `SettingsFragment.kt`) provides sliders for six parameters. Each slider maps a range of integer positions to the corresponding physical value.

| Parameter | Default | Range (slider min–max) | Description | How to adjust |
|-----------|---------|------------------------|-------------|----------------|
| **Click speed threshold** | 5.0 rad/s | 0–10 (0.1 rad/s per step) | Minimum absolute angular velocity around Y‑axis to register a flick. | Lower = easier to click; higher = harder, fewer false positives. |
| **Double‑click interval** | 400 ms | 200–1000 ms (10 ms per step) | Maximum time between two flicks to be considered a double‑click. | Increase if you need more time; decrease to avoid accidental double clicks. |
| **Right‑click tilt angle** | 45° | 0–90° (0.9° per step) | Absolute roll angle required to start the right‑click timer. | Decrease to trigger with less tilt; increase to require more tilt. |
| **Right‑click duration** | 500 ms | 100–1000 ms (10 ms per step) | How long the tilt must be held above the angle threshold. | Shorter = faster right‑click; longer = more deliberate, fewer accidents. |
| **Scroll speed threshold** | 8.0 m/s² | 0–15 m/s² (0.2 m/s² per step) | Minimum absolute linear acceleration along Y‑axis to trigger scroll. | Lower = scroll easier; higher = need faster push. |
| **Scroll debounce** | 2.0 m/s² | 0–5 m/s² (0.1 m/s² per step) | Acceleration below which the scroll flag resets, allowing another scroll. | Increase to prevent multiple scrolls from one push; decrease for quicker successive scrolls. |

### Technical Implementation

All parameters are stored in `PreferencesDataStore` (using AndroidX DataStore). The `EnhancedGestureDetector` reads them each time `reloadThresholds()` is called. The `detect()` method uses them in the following logic (simplified pseudocode):

```kotlin
fun detect(gyroY, accelY, roll): Gesture {
    if (abs(gyroY) > clickThreshold && (now - lastClickTime) > doubleClickInterval) {
        // click or double-click detection
    }
    if (abs(roll) > rightClickTiltAngle && !rightClickTriggered) {
        // right-click timer logic
    }
    if (abs(accelY) > scrollThreshold && !scrollInProgress) {
        scrollInProgress = true
        return if (accelY > 0) SCROLL_DOWN else SCROLL_UP
    } else if (abs(accelY) < scrollDebounce) {
        scrollInProgress = false
    }
    return NONE
}
```

### Recommended Settings for Different Users

| Profile | Click speed | Double‑click interval | Right‑click angle | Right‑click duration | Scroll speed | Scroll debounce |
|---------|-------------|----------------------|-------------------|----------------------|--------------|-----------------|
| **Default (balanced)** | 5.0 | 400 | 45° | 500 | 8.0 | 2.0 |
| **Sensitive (easy gestures)** | 3.0 | 600 | 35° | 300 | 6.0 | 1.5 |
| **Stable (avoid false positives)** | 7.0 | 300 | 60° | 700 | 10.0 | 3.0 |
| **Fast flicks / gamer** | 4.0 | 250 | 50° | 400 | 9.0 | 2.0 |
| **Large hands / strong movements** | 6.0 | 400 | 55° | 600 | 9.0 | 2.5 |

---

## Network Protocol Specification

The communication between the Android app and the PC server follows a simple TCP‑based protocol with JSON framing.

### Transport Layer

| Property | Value |
|----------|-------|
| **Protocol** | TCP (Transmission Control Protocol) |
| **Port** | 8080 (configurable) |
| **IP address** | Client (phone) connects to server (PC) using the PC’s local IP |
| **Message delimiter** | Newline character `\n` (ASCII 10) |
| **Character encoding** | UTF‑8 |
| **Connection direction** | Phone initiates; server listens |

### Message Types

All messages are JSON objects terminated by a newline. The following types are defined:

| Type | Direction | ACK required | Purpose | Example |
|------|-----------|--------------|---------|---------|
| `move` | Phone → PC | No | Send cursor movement delta | `{"type":"move","dx":12.3,"dy":-5.2}` |
| `click` | Phone → PC | Yes | Single left click | `{"type":"click","id":1700000000000}` |
| `doubleclick` | Phone → PC | Yes | Double click | `{"type":"doubleclick","id":1700000000001}` |
| `rightclick` | Phone → PC | Yes | Right click | `{"type":"rightclick","id":1700000000002}` |
| `scroll` | Phone → PC | Yes | Scroll (delta +1 down, -1 up) | `{"type":"scroll","delta":1,"id":1700000000003}` |
| `ack` | PC → Phone | – | Acknowledgment for a critical message | `{"type":"ack","id":1700000000000}` |

### Fields

| Field | Type | Description | Present in |
|-------|------|-------------|-------------|
| `type` | string | Message type identifier | All messages |
| `dx`, `dy` | float | Relative cursor movement in pixels | `move` |
| `delta` | int | Scroll direction: +1 (down), -1 (up) | `scroll` |
| `id` | long | Unique identifier (timestamp) | `click`, `doubleclick`, `rightclick`, `scroll`, `ack` |

### Reliability Mechanism

- **Move packets** are fire‑and‑forget. If a `move` packet is lost, the next packet will correct the cursor position. This keeps latency low.
- **Critical packets** (click, doubleclick, rightclick, scroll) require an ACK. The phone waits 500 ms after sending. If no ACK is received, it retransmits the packet **once**. After that, if still no ACK, the packet is dropped (the user may need to repeat the gesture).
- The PC server sends an ACK immediately after processing a critical packet, before any other action.

### Example Exchange

```
Phone → PC: {"type":"move","dx":5.0,"dy":2.0}
Phone → PC: {"type":"move","dx":3.0,"dy":1.0}
Phone → PC: {"type":"click","id":1001}
PC → Phone: {"type":"ack","id":1001}
Phone → PC: {"type":"scroll","delta":1,"id":1002}
PC → Phone: {"type":"ack","id":1002}
```

If the ACK for `click` is lost:
```
Phone → PC: {"type":"click","id":1001}
(no ACK after 500 ms)
Phone → PC: {"type":"click","id":1001}   // retransmission
PC → Phone: {"type":"ack","id":1001}
```

### Changing the Port

- **Android:** In `MainActivity.kt`, change the `PORT` constant.
- **PC (GUI):** In `gui.py`, change `CONFIG["port"]`.
- **PC (console):** In `config.json`, change `"port"`.

**Important:** Both sides must use the same port.

---

## Building the APK Without Android Studio

This section provides a complete walkthrough for building the APK using only the command line. It is useful for automation, CI/CD, or when you don’t have Android Studio installed.

### Prerequisites

1. **Java 11 JDK** installed and in `PATH`. Verify:
   ```bash
   java -version
   ```
   Output should contain `openjdk version "11.0.x"` or similar.

2. **Android Command‑Line Tools** downloaded and extracted.

   - Download from [Google’s official page](https://developer.android.com/studio#command-line-tools-only) (choose the version for your OS).
   - Extract to `~/android-sdk` (Linux/macOS) or `%USERPROFILE%\android-sdk` (Windows).
   - The directory structure must be: `~/android-sdk/cmdline-tools/latest/` containing `bin/`, `lib/`, etc.
   - Set `ANDROID_HOME` environment variable to that path.

   **Example for macOS/Linux:**
   ```bash
   mkdir -p ~/android-sdk/cmdline-tools
   unzip commandlinetools-mac-*.zip -d ~/android-sdk/cmdline-tools
   mv ~/android-sdk/cmdline-tools/cmdline-tools ~/android-sdk/cmdline-tools/latest
   export ANDROID_HOME=~/android-sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
   ```

   **Example for Windows (PowerShell):**
   ```powershell
   New-Item -ItemType Directory -Force "$env:USERPROFILE\android-sdk\cmdline-tools"
   Expand-Archive .\commandlinetools-win-*.zip -DestinationPath "$env:USERPROFILE\android-sdk\cmdline-tools"
   Move-Item "$env:USERPROFILE\android-sdk\cmdline-tools\cmdline-tools" "$env:USERPROFILE\android-sdk\cmdline-tools\latest"
   $env:ANDROID_HOME = "$env:USERPROFILE\android-sdk"
   $env:PATH += ";$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools"
   ```

### Build Scripts

We provide two scripts:

- `build_apk.sh` for macOS / Linux
- `build_apk.bat` for Windows

Both scripts perform the following steps:

1. Check that Java and Android SDK are available.
2. Accept all SDK licenses automatically (using `yes | sdkmanager --licenses`).
3. Install required SDK components: `build-tools;29.0.3`, `platforms;android-29`, `platform-tools`.
4. Navigate to the `android` folder and run `./gradlew clean assembleDebug`.
5. Output the APK location.

### Running the Script

**macOS / Linux:**
```bash
chmod +x build_apk.sh
./build_apk.sh
```

**Windows (Command Prompt or PowerShell):**
```batch
build_apk.bat
```

### Output

On success, the script prints:
```
APK created at android/app/build/outputs/apk/debug/app-debug.apk
```

You can install the APK using ADB:
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

Or copy it manually to your phone.

### Troubleshooting Build Script

| Error | Cause | Solution |
|-------|-------|----------|
| `Java not found` | JDK 11 not installed or not in PATH | Install JDK 11 and ensure `java -version` works. |
| `sdkmanager not found` | Android command‑line tools not extracted correctly | Re‑extract to `~/android-sdk/cmdline-tools/latest/`. |
| `Could not resolve all dependencies` | Internet connection issue or repository blocked | Check internet; the script uses Google’s Maven. |
| `gradlew: Permission denied` (macOS/Linux) | `gradlew` not executable | Run `chmod +x android/gradlew` manually. |
| `License agreements not accepted` | The `yes |` line failed | Run `sdkmanager --licenses` manually and accept. |

---

## Perfetto Analysis – Answers to 11 Questions

The exercise requires you to answer 11 questions using Perfetto traces. Below are **model answers** based on the Air Mouse implementation. You can also run the provided `perfetto_analyzer.py` script to extract data from your own traces.

### Q1: From sensor read request to data – what happens at OS level?

**Answer:** The Android sensor framework uses a **HAL (Hardware Abstraction Layer)**. The app registers a listener via `SensorManager`, which creates a `SensorEventQueue`. The kernel driver (e.g., IIO subsystem) polls the sensor hardware at the requested rate. On each hardware interrupt, data is copied to shared memory, then delivered via `SensorService` (system server) to the app’s `onSensorChanged`. Perfetto shows `SensorService::batch`, `SensorEventConnection::sendEvents`, and kernel `sensor_irq` events. The total latency from interrupt to callback is typically 2‑5 ms.

**Perfetto query:**
```sql
SELECT ts, dur, name FROM slice WHERE name GLOB '*Sensor*' LIMIT 10;
```

### Q2: Why do raw sensors have errors and how does fusion help?

**Answer:** Raw errors: gyro drift (integration of bias), accelerometer noise (vibrations), magnetometer offset (hard‑iron distortion). **Madgwick fusion** combines the fast response of the gyroscope with the absolute references of the accelerometer (gravity) and magnetometer (magnetic north). It uses gradient descent to correct the gyro integration, producing a quaternion that is drift‑free and smooth. The fusion algorithm runs at 50 Hz and adds negligible latency.

### Q3: Compare configured vs actual sampling period (from Perfetto)

**Answer:** The app requests `SENSOR_DELAY_GAME` (20 ms period, 50 Hz). Perfetto counters show actual intervals between 18‑25 ms, with occasional longer periods (up to 30 ms) when the CPU is busy. The difference is due to sensor batching (Android may batch multiple samples) and system load.

**Perfetto query:**
```sql
SELECT ts, value FROM counter WHERE name = 'sensor_sampling_period_ns' LIMIT 20;
```

### Q4: Does contention occur between time‑sensitive calls?

**Answer:** Yes – in the initial implementation, `onSensorChanged` runs on the main thread. If the UI is rendering or GC occurs, sensor events may be delayed. Perfetto `sched_switch` events show the main thread being preempted. In the final code, we use a dedicated `HandlerThread` for sensors, eliminating contention.

### Q5: Wake‑up vs non‑wake‑up sensors?

**Answer:** Wake‑up sensors can wake the device from sleep (e.g., significant motion, proximity); they consume more battery. Non‑wake‑up sensors only deliver events when the CPU is already awake. Air Mouse uses non‑wake‑up sensors (accelerometer, gyroscope, magnetometer) because the screen is on during use, saving battery.

### Q6: Average CPU time of the filter function (Madgwick)

**Answer:** From Perfetto, the total CPU time of `MadgwickFusion.update*` calls averages ~0.4‑0.5 ms per update. At 50 Hz, this is ~2‑2.5% of a single core.

**Perfetto query (with instrumented trace points):**
```sql
SELECT SUM(dur) / 1e6 FROM slice WHERE name = 'MadgwickUpdate';
```

### Q7: Which sensor consumes the most processing power?

**Answer:** Magnetometer – because it requires calibration (min/max search) and the Madgwick algorithm uses it in the gradient descent step, adding extra floating‑point operations. However, the difference is small; all three sensors contribute similar overhead.

### Q8: How does sampling rate affect system processing?

**Answer:** Higher rate (e.g., 200 Hz) increases CPU usage, battery drain, and network traffic (more move packets), but reduces latency and improves smoothness. The chosen `SENSOR_DELAY_GAME` (50 Hz) is a good trade‑off. Lower rates (e.g., 20 Hz) save battery but may cause noticeable lag.

### Q9: Latency from sensor to cursor movement

**Answer:** Total latency = sensor sampling (2‑5 ms) + fusion (0.5 ms) + network (2‑10 ms) + pyautogui (1‑5 ms) + display refresh (0‑16.6 ms) = **20‑40 ms** typical. This is imperceptible to most users.

### Q10: Threads and UI separation

**Answer:** Sensors run on a dedicated `HandlerThread` (`SensorThread`). Network sending runs on a separate `DataSender` thread. UI updates (status text, green square rotation) are posted to the main thread via `LiveData`. The main thread is never blocked by sensor processing or network I/O.

### Q11: Slow vs sudden movement – processing difference?

**Answer:** Sudden movements trigger gesture detection (click/scroll) which generates an extra network packet (with ACK). Slow movements only send `move` packets. The fusion algorithm runs identically for both; there is no difference in CPU load or latency.

### Running the Automated Analyzer

```bash
python pc/perfetto_analyzer.py trace.perfetto-trace
```

The script outputs sample data for Q1, Q3, Q6, Q7, and textual answers for the others.

---

## Troubleshooting

| Problem | Likely Cause | Solution |
|---------|--------------|----------|
| `pip install` fails with SOCKS error | Proxy environment variables | Run `unset http_proxy https_proxy` |
| `Connection refused` on phone | Server not running, wrong IP, firewall | Start server, verify IP, disable firewall |
| Cursor drifts when phone still | Gyro bias not removed | Re‑calibrate gyro on a flat surface (table) |
| Click not detected | Threshold too high | Lower `clickSpeedThreshold` in Settings |
| Right‑click not detected | Tilt angle threshold too high | Decrease `rightClickTilt` (e.g., 30°) |
| Double‑click not detected | Interval too short or flicks too slow | Increase `doubleClickInterval` or flick faster |
| Scroll triggers randomly | Hand tremor exceeds threshold | Increase `scrollSpeedThreshold` (e.g., 10.0) |
| High battery drain | Sampling rate always high | Enable battery saver (default on) |
| App crashes on open | Missing sensor | Check phone has gyro/accel/mag (most modern phones do) |
| Green square doesn’t rotate | Sensor fusion not receiving data | Check sensor permissions; restart app |
| Debug overlay not showing | Missing overlay permission | Grant `SYSTEM_ALERT_WINDOW` permission manually |
| Build script fails with `sdkmanager not found` | Command‑line tools not in correct path | Re‑extract to `~/android-sdk/cmdline-tools/latest/` |

---

## Advanced Customisation

### Changing TCP Port

- **Android:** In `MainActivity.kt`, modify `private const val PORT = 8080`.
- **PC (GUI):** In `gui.py`, modify `CONFIG["port"]`.
- **PC (console):** In `config.json`, modify `"port"`.

### Adjusting Madgwick Beta (Drift vs Noise Trade‑off)

In `MadgwickFusion.kt`, change the `beta` parameter in the constructor:
```kotlin
class MadgwickFusion(private val beta: Float = 0.1f)
```
- **Higher beta (e.g., 0.2–0.5):** More correction → less drift, but more noise (cursor may jitter when still).
- **Lower beta (e.g., 0.01–0.05):** Less correction → more drift, but smoother movement.

### Disabling Haptic Feedback

- **Via UI:** Settings dialog → uncheck “Enable haptic feedback”.
- **Permanently:** In `EnhancedGestureDetector.kt`, comment out the `vibrate()` calls.

### Adding a New Gesture (e.g., Long Press)

1. In `EnhancedGestureDetector.kt`, add a new `Gesture` enum value (e.g., `LONG_PRESS`).
2. Implement detection logic (e.g., holding the phone still for 1 second).
3. In `DataSender.kt`, add a `sendLongPress()` method that queues a JSON message with a new `type`.
4. In the PC server (`server.py` or `gui.py`), extend `process_message` to handle `"longpress"` and perform the desired action (e.g., middle click or volume control).
5. Call the new detector from `SensorService` and invoke the gesture callback.

### Changing Sensor Sampling Rate

In `SensorService.kt`, modify the delay parameter when registering listeners:
```kotlin
sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
```
Options: `SENSOR_DELAY_FASTEST` (≈200 Hz), `SENSOR_DELAY_GAME` (50 Hz), `SENSOR_DELAY_UI` (16 Hz), `SENSOR_DELAY_NORMAL` (5 Hz). Higher rate increases smoothness but drains battery.

### Modifying the Debug Overlay

- **Position:** In `DebugOverlayService.kt`, change `params.gravity` and `params.x`/`y`.
- **Appearance:** Edit `res/layout/debug_overlay.xml` (background, text colour, font size).
- **Additional values:** Extend `updateData()` to show more sensor readings.

### Customising the PC Server GUI

In `gui.py`, modify the colour palette:
```python
self.bg_color = "#1e1e1e"
self.accent = "#007acc"
```
You can also add new UI elements (e.g., a “Clear Log” button).

---

## Video Demonstration Script

A 5‑minute video is required for submission. Follow this script precisely.

| Time | Action | Narration |
|------|--------|------------|
| 0:00 – 0:20 | Show both devices (phone and laptop). | “Hello, I am [Name], student ID [SID]. This is my Air Mouse demonstration.” |
| 0:20 – 0:40 | Show PC server (GUI or console). | “Here is the Python server running on my laptop.” |
| 0:40 – 1:00 | Open app on phone, enter IP. | “I enter the laptop’s IP address.” |
| 1:00 – 1:30 | Tap **Calibrate Sensors**. Show gyro step (phone still on table). | “First, gyro calibration – phone must be completely still on a table.” |
| 1:30 – 2:00 | Show magnetometer step (figure‑8 for a few seconds). | “Now magnetometer calibration – moving in a figure‑8 pattern to cover all orientations.” |
| 2:00 – 2:15 | Show accelerometer step (briefly still). | “Finally, simplified accelerometer calibration.” |
| 2:15 – 2:30 | Tap **Start Air Mouse**. Show status changing to “Air Mouse Active”. | “The app connects and starts streaming.” |
| 2:30 – 3:00 | Rotate phone left/right → cursor moves horizontally. Nod up/down → vertical movement. | “Rotating around Z‑axis moves the cursor horizontally; nodding around X‑axis moves it vertically.” |
| 3:00 – 3:30 | Perform a flick around Y‑axis → click. Show a click in Notepad. | “A quick flick around the Y‑axis produces a left click.” |
| 3:30 – 3:50 | Perform two quick flicks → double click. Show text selection. | “Two flicks within 400 ms give a double click.” |
| 3:50 – 4:10 | Tilt phone sideways >45° and hold for 0.5 seconds → right click. | “Tilting sideways and holding triggers a right click.” |
| 4:10 – 4:30 | Push phone up/down linearly → scroll. | “A quick linear push along the Y‑axis scrolls the page.” |
| 4:30 – 4:50 | Place phone on table, show cursor stays still (drift test). | “Even when stationary, the cursor does not drift – thanks to proper calibration and fusion.” |
| 4:50 – 5:00 | Conclude. | “We have demonstrated all required features. Thank you.” |

---

## Submission Checklist

Before zipping your submission, verify each item.

- [ ] **Android source code** – complete `android/` folder (all `.kt`, `.xml`, `build.gradle`, etc.) with proper package structure.
- [ ] **APK** – `app-debug.apk` (or `app-release.apk`) built and tested on an Android 10+ device.
- [ ] **PC server code** – complete `pc/` folder (`gui.py`, `server.py`, `run.py`, `config.json`, `requirements.txt`, scripts).
- [ ] **Video** – max 5 minutes, showing both screens simultaneously, demonstrating all gestures, calibration, and drift test.
- [ ] **Report PDF** – contains:
  - [ ] Answers to the 11 Perfetto questions (with screenshots from Perfetto UI).
  - [ ] Explanation of sensor fusion (Madgwick) and calibration.
  - [ ] Team member contributions (who did what).
  - [ ] Challenges faced and how you solved them.
  - [ ] Any additional features or customisations.
- [ ] **Perfetto trace file** (optional but recommended) – a 10‑second trace showing sensor activity.

**Naming convention for zip file:**  
`CPS-CA2-<SID1>-<SID2>-<SID3>-<SID4>.zip`

**Example:** `CPS-CA2-401012345-401023456-401034567-401045678.zip`

---

## Credits & License

- **Madgwick algorithm** – Sebastian Madgwick (open source, used under academic license).
- **Android Sensor Framework** – Google (Apache 2.0).
- **PyAutoGUI** – Al Sweigart (BSD 3‑Clause License).
- **Icons** – Material Design icons (Apache 2.0).

This project is for **educational purposes only** as part of the University of Tehran’s Embedded Systems course. Redistribution or commercial use without explicit permission from the instructors is prohibited.

---

**Congratulations! You now have the complete Air Mouse Ultimate documentation.**  
*Good luck with your submission!*

---

*Last updated: May 2026*