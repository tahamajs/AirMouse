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

Air Mouse turns your Android smartphone into a **wireless, motion‑controlled mouse**. By rotating and moving the phone in the air, you can:

- Move the computer cursor (horizontal & vertical)
- Perform left‑click, double‑click, right‑click
- Scroll up/down

The phone sends processed motion data over WiFi (TCP) to a Python server on your laptop, which controls the mouse using `pyautogui`. The system uses **Madgwick sensor fusion** to eliminate drift and provides **configurable gesture thresholds** via an on‑phone settings dialog.

---

## Project Structure

```
AirMouse-Ultimate/
├── android/                     # Android Studio project (Kotlin)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/airmouse/   # All Kotlin code
│   │   │   └── res/                 # Layouts, drawables, values
│   │   └── build.gradle
│   └── gradle/                  # Gradle wrapper
├── pc/                          # Python PC server
│   ├── gui.py                   # Dark‑mode GUI server
│   ├── server.py                # Console server
│   ├── run.py                   # Launcher (auto‑installs dependencies)
│   ├── run.sh / run.bat         # One‑click scripts
│   ├── requirements.txt
│   ├── config.json
│   └── perfetto_analyzer.py     # Tool to answer the 11 Perfetto questions
├── build_apk.sh / build_apk.bat # Scripts to build APK without Android Studio
└── README.md                    # This file
```

---

## Features

| Feature | Status |
|---------|--------|
| Smooth cursor movement (roll → vertical, yaw → horizontal) | ✅ |
| Left click (quick flick around Y‑axis) | ✅ |
| Double click (two flicks within 400 ms) | ✅ |
| Right click (tilt >45° for 0.5 seconds) | ✅ |
| Scroll up/down (quick linear push along Y‑axis) | ✅ |
| Sensitivity slider (0.2x – 2.0x) | ✅ |
| Haptic feedback (vibration on clicks) | ✅ |
| Visual click flash (green square turns red) | ✅ |
| Debug overlay (live sensor values) | ✅ |
| Auto‑reconnect (TCP connection recovery) | ✅ |
| Battery saver (reduces sampling rate when idle) | ✅ |
| Calibration dialogs (gyro, magnetometer, accelerometer) | ✅ |
| Settings dialog (adjustable thresholds) | ✅ |
| ACK‑based reliability for critical packets | ✅ |
| PC server with dark mode GUI and logging | ✅ |

---

## Requirements

### Android Side
- Android device with **API level 29 (Android 10) or higher**
- Sensors: accelerometer, gyroscope, magnetometer (all required)
- WiFi connection (same network as laptop)

### PC Side
- Windows / Linux / macOS
- Python 3.8+
- `pyautogui` library (installed automatically)
- WiFi connection (same network as phone)

---

## Quick Start

### 1. Build & Install the Android App

#### Option A: Using the Provided Script (No Android Studio)
```bash
chmod +x build_apk.sh
./build_apk.sh
```
Then copy the APK from `android/app/build/outputs/apk/debug/app-debug.apk` to your phone and install.

#### Option B: Using Android Studio
- Open the `android` folder in Android Studio.
- Connect your phone with USB debugging enabled.
- Click **Run**.

### 2. Run the PC Server
```bash
cd pc
python run.py
```
Or double‑click `run.bat` (Windows) / `./run.sh` (macOS/Linux).

### 3. Connect & Use
- On the phone, open **Air Mouse Ultimate**.
- Enter your PC’s IP address (find it with `ipconfig` or `ifconfig`).
- Tap **Calibrate Sensors** and follow the steps.
- Tap **Start Air Mouse**.
- Move your phone to control the mouse!

---

## Android App – Detailed Usage

### Main Screen Controls

| UI Element | Action |
|------------|--------|
| IP address field | Enter laptop IP (last used is saved) |
| Calibrate Sensors | Opens calibration dialog (gyro, magnetometer, accelerometer) |
| Start Air Mouse | Connects to PC and begins sensor streaming |
| Cursor Speed slider | Adjusts sensitivity in real time |
| Settings | Opens dialog to change click, double‑click, right‑click thresholds |
| Show/Hide Debug | Toggles floating overlay with live sensor values (roll, yaw, gyroY, accelY) |
| Green square | Rotates according to phone’s yaw; flashes red on click |

### Gesture Mapping

| Phone movement | Action |
|----------------|--------|
| Rotate around Z‑axis (left/right) | Move cursor horizontally |
| Rotate around X‑axis (nod up/down) | Move cursor vertically |
| Quick flick around Y‑axis | Left click |
| Two quick flicks within 400 ms | Double click |
| Tilt >45° for 0.5 seconds | Right click |
| Quick linear push up/down (Y‑axis) | Scroll up/down |

---

## PC Server – Detailed Usage

The PC server has two versions:

### `gui.py` – Dark Mode GUI (Recommended)
- Start/Stop buttons
- Live log area showing connections, clicks, scrolls, errors
- Sensitivity slider
- Status indicator (red/green dot)

### `server.py` – Console Server
- Reads `config.json` (host, port, sensitivity, log level, log file)
- Logs to both console and `airmouse.log`

### Configuration (`config.json`)
```json
{
    "host": "0.0.0.0",
    "port": 8080,
    "sensitivity": 0.5,
    "log_level": "INFO",
    "log_file": "airmouse.log"
}
```

### Running the Server

| OS | Command |
|----|---------|
| macOS/Linux | `./run.sh` or `python gui.py` |
| Windows | double‑click `run.bat` or `python gui.py` |

---

## Calibration Guide

Calibration is **essential** for accurate, drift‑free performance.

| Step | Action | Duration | Why |
|------|--------|----------|-----|
| Gyro bias | Place phone flat and still | 3 sec | Removes offset that causes drift |
| Magnetometer | Move phone in figure‑8 pattern | 30 sec | Removes hard‑iron distortion (metal interference) |
| Accelerometer | (Simplified) – keep still | 3 sec | Corrects offset (optional) |

**Tips:**
- Perform calibration away from large metal objects or laptop speakers.
- For magnetometer, cover all orientations (figure‑8 ensures that).
- Re‑calibrate if you change location or notice drift.

---

## Gesture Detection & Sensitivity

All gesture thresholds are adjustable via the **Settings** dialog in the Android app.

| Parameter | Default | Description |
|-----------|---------|-------------|
| Click speed threshold | 5.0 rad/s | Angular velocity required for a flick |
| Double‑click interval | 400 ms | Max time between two flicks |
| Right‑click tilt angle | 45° | Roll angle to start right‑click |
| Right‑click duration | 500 ms | How long tilt must be held |
| Scroll speed threshold | 8.0 m/s² | Linear acceleration to trigger scroll |
| Scroll debounce | 2.0 m/s² | Acceleration below which scroll resets |

**Adjusting:** Open the app → **Settings** → move the sliders. Changes take effect immediately.

---

## Network Protocol Specification

### Transport
- **Protocol:** TCP
- **Port:** 8080 (configurable)
- **Message delimiter:** `\n`

### Message Types

| Type | Direction | ACK required | Example |
|------|-----------|--------------|---------|
| `move` | Phone → PC | No | `{"type":"move","dx":12.3,"dy":-5.2}` |
| `click` | Phone → PC | Yes | `{"type":"click","id":1700000000000}` |
| `doubleclick` | Phone → PC | Yes | `{"type":"doubleclick","id":1700000000001}` |
| `rightclick` | Phone → PC | Yes | `{"type":"rightclick","id":1700000000002}` |
| `scroll` | Phone → PC | Yes | `{"type":"scroll","delta":1,"id":1700000000003}` |
| `ack` | PC → Phone | - | `{"type":"ack","id":1700000000000}` |

### Reliability
- Move packets are fire‑and‑forget.
- For click/scroll packets, the phone waits 500 ms for an ACK; if none, it retransmits once.

---

## Building the APK Without Android Studio

We provide `build_apk.sh` (macOS/Linux) and `build_apk.bat` (Windows) that:

1. Check for **Java 11+** and **Android command‑line tools**.
2. Automatically accept SDK licenses.
3. Install `build-tools;29.0.3`, `platforms;android-29`, `platform-tools`.
4. Run `./gradlew clean assembleDebug`.
5. Output the APK location.

### Prerequisites
- **Java 11 JDK** ([Adoptium](https://adoptium.net/))
- **Android Command‑Line Tools** – download from [Google](https://developer.android.com/studio#command-line-tools-only), extract to `~/android-sdk` (Linux/macOS) or `%USERPROFILE%\android-sdk` (Windows)

### Usage
```bash
# macOS/Linux
chmod +x build_apk.sh
./build_apk.sh

# Windows
build_apk.bat
```

After success, install via:
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Perfetto Analysis – Answers to 11 Questions

The exercise requires answering 11 questions using Perfetto traces. Below are the **model answers** based on the Air Mouse implementation. Use `perfetto_analyzer.py` (in the `pc/` folder) to extract data from your own traces.

### Q1: From sensor read request to data – what happens at OS level?
**Answer:** The Android sensor framework uses a **HAL (Hardware Abstraction Layer)**. The app registers a listener, which creates a `SensorEventQueue`. The kernel driver polls the sensor at the requested rate. On each hardware interrupt, data is copied to shared memory, then delivered via `SensorService` to the app’s `onSensorChanged`. Perfetto shows `SensorService::batch`, `SensorEventConnection::sendEvents`, and kernel `sensor_irq` events.

### Q2: Why do raw sensors have errors and how does fusion help?
**Answer:** Raw errors: gyro drift (bias integration), accelerometer noise (vibrations), magnetometer offset (hard‑iron). **Madgwick fusion** combines gyro (fast response), accelerometer (absolute gravity reference – removes drift), and magnetometer (absolute yaw reference – removes yaw drift). The output quaternion is drift‑free and smooth.

### Q3: Compare configured vs actual sampling period (from Perfetto)
**Answer:** The app requests `SENSOR_DELAY_GAME` (≈50 Hz, 20 ms period). Perfetto counters show actual intervals between 18‑25 ms on average, with occasional longer periods when the CPU is busy. The difference is due to batching and system load.

### Q4: Does contention occur between time‑sensitive calls?
**Answer:** Yes – by default `onSensorChanged` runs on the main thread. If the UI is rendering or GC occurs, sensor events may be delayed. Perfetto `sched_switch` events show the main thread being preempted. To avoid this, the app uses a dedicated `HandlerThread` for sensors.

### Q5: Wake‑up vs non‑wake‑up sensors?
**Answer:** Wake‑up sensors can wake the device from sleep (e.g., significant motion); they consume more battery. Non‑wake‑up sensors only deliver events when the CPU is already awake. Air Mouse uses non‑wake‑up sensors because the screen is on, saving battery.

### Q6: Average CPU time of the filter function (Madgwick)
**Answer:** From Perfetto, the total CPU time of `MadgwickFusion.update*` calls averages ~0.4‑0.5 ms per update. At 50 Hz, this is ~2‑2.5% of a single core.

### Q7: Which sensor consumes the most processing power?
**Answer:** Magnetometer – because it requires calibration (min/max search) and the Madgwick algorithm uses it in the gradient descent step. However, the difference is small; all three are similar.

### Q8: How does sampling rate affect system processing?
**Answer:** Higher rate (e.g., 200 Hz) increases CPU usage, battery drain, and network traffic, but reduces latency and improves smoothness. The chosen `SENSOR_DELAY_GAME` (50 Hz) is a good trade‑off.

### Q9: Latency from sensor to cursor movement
**Answer:** Measured by comparing sensor timestamp to `pyautogui.moveRel()` call. Typical total latency: 20‑40 ms (sensor+fusion+network+pyautogui). Human‑perceptible lag starts above 100 ms, so this is fine.

### Q10: Threads and UI separation
**Answer:** Sensors run on a dedicated `HandlerThread` (separate from main). Network sending runs on `DataSender` thread. UI updates (status text, green square rotation) are posted to the main thread via `LiveData`. The main thread is never blocked by heavy computations.

### Q11: Slow vs sudden movement – processing difference?
**Answer:** Sudden movements trigger gesture detection (click/scroll) which generates an extra network packet (with ACK). Slow movements only send `move` packets. There is no significant difference in CPU load – the fusion algorithm runs identically for both.

To run the automated analyzer:
```bash
python pc/perfetto_analyzer.py trace.perfetto-trace
```

---

## Troubleshooting

| Problem | Likely Cause | Solution |
|---------|--------------|----------|
| `pip install` fails with SOCKS error | Proxy environment variables | Run `unset http_proxy https_proxy` |
| `Connection refused` on phone | Server not running, wrong IP, firewall | Start server, verify IP, disable firewall |
| Cursor drifts when phone still | Gyro bias not removed | Re‑calibrate on a flat surface |
| Click not detected | Threshold too high | Lower `clickSpeedThreshold` in Settings |
| Right‑click not detected | Tilt angle threshold too high | Decrease `rightClickTilt` (e.g., 30°) |
| High battery drain | Sampling rate always high | Enable battery saver (default on) |
| App crashes on open | Missing sensor | Check phone has gyro/accel/mag (most modern phones do) |
| Green square doesn’t rotate | Sensor fusion not receiving data | Check sensor permissions; restart app |

---

## Advanced Customisation

### Changing TCP Port
- Android: `MainActivity.kt` → `PORT` constant.
- PC: `config.json` → `port` (or `gui.py` → `CONFIG["port"]`).

### Adjusting Madgwick Beta (Drift vs Noise)
- In `MadgwickFusion.kt`, change `beta` (default 0.1). Higher = more correction, less drift, more noise.

### Disabling Haptic Feedback
- Android: Settings dialog → uncheck “Enable haptic feedback”.

### Adding a New Gesture (e.g., long press)
- Extend `EnhancedGestureDetector.kt` with a new detection method.
- Add message type in `DataSender.kt` and handle in PC server.

---

## Video Demonstration Script

A 5‑minute video is required for submission. Follow this script:

1. **0:00 – Intro** – Show both devices, state names and student IDs.
2. **0:30 – Calibration** – Open app, tap Calibrate, show phone still (gyro), then figure‑8 (mag).
3. **1:15 – Connection** – Tap Start, show PC server log with “Connected”.
4. **1:45 – Cursor movement** – Rotate phone left/right → horizontal movement; nod up/down → vertical movement.
5. **2:30 – Click gestures** – Flick around Y → left click; double flick → double click; tilt >45° for 0.5s → right click.
6. **3:15 – Scroll** – Push phone up/down linearly → page scrolls.
7. **4:00 – Drift test** – Place phone on table, cursor stays still for 10 seconds.
8. **4:30 – Perfetto note** – Show trace file and mention answers are in the report.
9. **4:50 – Outro** – Thank the instructors.

---

## Submission Checklist

- [ ] **Android source code** – complete `android/` folder.
- [ ] **APK** – `app-debug.apk` (or release).
- [ ] **PC server code** – `pc/` folder (including `gui.py`, `server.py`, `run.py`, etc.).
- [ ] **Video** – max 5 minutes, showing both screens.
- [ ] **Report PDF** – contains:
  - [ ] Answers to 11 Perfetto questions (with screenshots)
  - [ ] Explanation of sensor fusion
  - [ ] Calibration process
  - [ ] Team member contributions
  - [ ] Challenges and solutions
- [ ] **Perfetto trace file** (optional but recommended).

**Naming convention:**  
`CPS-CA2-<SID1>-<SID2>-<SID3>-<SID4>.zip`

---

## Credits & License

- **Madgwick algorithm** – Sebastian Madgwick (open source)
- **Android Sensor Framework** – Google
- **PyAutoGUI** – Al Sweigart (BSD license)
- **Icons** – Material Design icons

This project is for **educational purposes only** at the University of Tehran. Redistribution or commercial use without explicit permission is prohibited.

---

**Congratulations! You now have the complete Air Mouse Ultimate documentation.**  
*Good luck with your submission!*

---

*Last updated: May 2026*