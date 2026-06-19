# ✈️ Air Mouse Ultimate – Full System Documentation

**University of Tehran – Faculty of Electrical and Computer Engineering**  
**Embedded Systems Exercise (Second Semester 1404-1405 / 2025-2026)**  

**Designers:** Arian Firoozi, Arsalan Talaee  
**Instructors:** Dr. Mohsen Shokri, Dr. Mehdi Kargahi  

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Android Application](#3-android-application)
   - 3.1 User Interface
   - 3.2 Sensor Processing & Calibration
   - 3.3 Sensor Fusion (Madgwick AHRS)
   - 3.4 Gesture Detection
   - 3.5 Network Communication (TCP + ACK)
   - 3.6 Permissions & Manifest
4. [PC Server (Python)](#4-pc-server-python)
   - 4.1 GUI Version (`gui.py`)
   - 4.2 Console Version (`server.py`)
   - 4.3 UDP Discovery & QR Code
   - 4.4 Logging & Statistics
5. [Communication Protocol](#5-communication-protocol)
   - 5.1 Message Format
   - 5.2 ACK & Retransmission
6. [Installation Guide](#6-installation-guide)
   - 6.1 Android App
   - 6.2 PC Server
7. [Usage Instructions](#7-usage-instructions)
8. [Perfetto Profiling & Questions](#8-perfetto-profiling--questions)
   - 8.1 Collecting a Trace
   - 8.2 Running the Analyzer
   - 8.3 Answers to the 11 Questions
9. [Troubleshooting](#9-troubleshooting)
10. [Extending the System](#10-extending-the-system)
11. [Contributions & Licensing](#11-contributions--licensing)
12. [Acknowledgments](#12-acknowledgments)

---

## 1. Project Overview

**Air Mouse Ultimate** turns any Android smartphone into a wireless air mouse. By rotating and moving the phone in space, the user can control the PC cursor, click, double‑click, right‑click, and scroll – all without touching the computer.

The project consists of two main parts:

- **Android App** – reads raw sensor data (gyroscope, accelerometer, magnetometer), applies calibration and sensor fusion (Madgwick AHRS), detects gestures, and sends incremental commands over TCP to the PC.
- **PC Server** – written in Python, receives commands, and moves the mouse using `pyautogui`. It provides a beautiful dark‑mode GUI (or a headless console version) with QR code connection, UDP discovery, live logs, and statistics.

The system fully satisfies the exercise requirements: bias removal, 6‑point accelerometer calibration, hard‑iron magnetometer calibration, own‑implementation of Madgwick filter, click/scroll detection, TCP communication with ACK and retransmission, and Perfetto trace analysis.

---

## 2. System Architecture

![Architecture](docs/architecture.png) *(conceptual)*

- **Android Phone** → reads sensors at ~50 Hz → calibrates → Madgwick fusion → orientation (roll, yaw) → incremental deltas → gestures (click, scroll, etc.) → JSON over TCP → **PC Server** → moves mouse cursor.

- **PC Server** also responds with ACK for critical commands; Android retransmits if no ACK received within 500 ms.

- **Optional UDP discovery** – PC broadcasts its presence; Android can auto‑detect server IP.

---

## 3. Android Application

### 3.1 User Interface

The app follows **Material 3** design with bottom navigation and a navigation drawer.

- **Home Fragment** – main controls: IP address input, calibration button, start/stop, sensitivity slider, live sensor data, Wi‑Fi signal quality, orientation indicator (green square).
- **Statistics Fragment** – bar chart of gesture counts (clicks, scrolls, right clicks, double clicks) and session timer.
- **Help Fragment** – expandable cards with controls, calibration guide, sensor info, connection guide.
- **Drawer** – access to Profiles, Themes, Gesture Training, Custom Gesture, Battery Graph, Accessibility, Network Discovery, Voice Commands, Server Log, Edge Gestures, and About.

### 3.2 Sensor Processing & Calibration

All raw sensor values are corrected before fusion:

- **Gyroscope bias removal** – collect 500 samples when stationary, compute average, subtract from future readings.
- **Accelerometer 6‑point calibration** – place phone in six orientations (±X, ±Y, ±Z), compute offset and scale per axis so that gravity reads exactly 9.81 m/s².
- **Magnetometer hard‑iron calibration** – move phone in figure‑8 pattern for 15 seconds, record min/max, compute offset and scale.

Calibration data is stored permanently in `PreferencesManager` and loaded on app start.

### 3.3 Sensor Fusion (Madgwick AHRS)

We implemented the **Madgwick AHRS filter** from scratch (no external library). The filter fuses gyroscope (fast, drift‑prone), accelerometer (gravity reference), and magnetometer (yaw reference) into a quaternion. Outputs roll (X axis) and yaw (Z axis) angles in radians.

Key methods:
- `updateGyro(gx, gy, gz, dt)` – integrates gyroscope and applies accelerometer/magnetometer correction.
- `updateAccel(ax, ay, az)` – stores latest accelerometer data.
- `updateMag(mx, my, mz)` – stores latest magnetometer data.
- `getRoll()`, `getYaw()` – returns orientation.

The filter runs at the sensor sampling rate (~50 Hz).

### 3.4 Gesture Detection

Gestures are detected using configurable thresholds (stored in `PreferencesManager`):

- **Cursor movement** – changes in yaw (Z axis) produce horizontal movement; changes in roll (X axis) produce vertical movement. Movement is sent as **incremental deltas**, not absolute positions.
- **Click** – quick rotation around Y axis (angular speed > threshold).
- **Double‑click** – two quick Y rotations within 400 ms.
- **Right‑click** – tilt phone right (roll > 45°) for 500 ms.
- **Scroll** – fast linear acceleration along Y axis (magnitude > threshold). Direction determines scroll up/down.

Haptic feedback is provided for all gestures (configurable).

### 3.5 Network Communication (TCP + ACK)

- **Connection** – uses TCP socket to PC on port 8080.
- **Message format** – JSON, newline‑delimited.
- **Incremental moves** – `{"type":"move","dx":x,"dy":y}` (no ACK needed).
- **Critical commands** – `click`, `doubleclick`, `rightclick`, `scroll` include a unique `id` field. Server responds with `{"type":"ack","id":id}`. Android waits 500 ms; if no ACK, retransmits the message (once).
- **Disconnection handling** – `AutoReconnect` attempts to reconnect every 5 seconds.

All network operations are non‑blocking, using coroutines (`kotlinx.coroutines`).

### 3.6 Permissions & Manifest

Required permissions:
- `INTERNET`
- `VIBRATE`
- `SYSTEM_ALERT_WINDOW` (for debug overlay)
- `CAMERA` (for QR scanning)
- `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`

The app uses cleartext traffic (allowed for local network only) via `network_security_config.xml`.

---

## 4. PC Server (Python)

The PC server is written in Python 3.8+ and uses `pyautogui` to control the mouse. Two versions are provided:

### 4.1 GUI Version (`gui.py`)

**Features:**
- Dark mode interface (customisable accent colour).
- **QR code display** – shows `IP:port` as a QR code; scan with Android app for instant connection.
- **UDP discovery** – listens on port 8081 and responds to `"AIRMOUSE_DISCOVER"` broadcast.
- **Live log area** – shows connections, disconnections, gestures, errors.
- **Real‑time statistics** – click counts, double clicks, right clicks, scrolls, active connections.
- **Sensitivity slider** – adjust mouse sensitivity on the fly (0.2–2.0).
- **Start/Stop buttons** – clean server lifecycle.
- **Thread‑safe** – asyncio event loop runs in a background thread; GUI remains responsive.

### 4.2 Console Version (`server.py`)

Lightweight, headless server. Reads `config.json` for:
- `host`, `port`
- `discovery_port`
- `sensitivity`
- `log_level` (DEBUG, INFO, WARNING, ERROR)
- `log_file`

Logs to both console and file. Supports all gestures and ACK.

### 4.3 UDP Discovery & QR Code

- **UDP Discovery** – The server broadcasts its availability. Android can send a UDP broadcast message `"AIRMOUSE_DISCOVER"`; the server responds with its TCP port. (Android side not mandatory, but PC supports it.)
- **QR Code** – The GUI generates a QR code containing the server's local IP and port. Scan with the Android app to auto‑fill the IP address.

### 4.4 Logging & Statistics

- All events are time‑stamped.
- Console server logs to a file (e.g., `airmouse.log`).
- GUI shows live log in a scrollable text area.
- Statistics are updated after each gesture.

---

## 5. Communication Protocol

### 5.1 Message Format

All messages are **JSON** objects, each terminated by a newline (`\n`).

| Type          | Example                                                                 | ACK required |
|---------------|-------------------------------------------------------------------------|--------------|
| Move          | `{"type":"move","dx":12.5,"dy":-3.2}`                                   | No           |
| Click         | `{"type":"click","id":123}`                                             | Yes          |
| Double‑click  | `{"type":"doubleclick","id":124}`                                       | Yes          |
| Right‑click   | `{"type":"rightclick","id":125}`                                        | Yes          |
| Scroll        | `{"type":"scroll","delta":1,"id":126}`                                  | Yes          |
| ACK (server)  | `{"type":"ack","id":123}`                                               | –            |

### 5.2 ACK & Retransmission

- After sending a critical message, the Android app starts a 500 ms timer.
- If the corresponding ACK is received before timeout, the message is considered delivered.
- If timeout occurs, the message is **retransmitted once**.
- Move messages are not acknowledged (low priority, no retransmission).

This ensures reliability for actions like clicks, even on lossy Wi‑Fi.

---

## 6. Installation Guide

### 6.1 Android App

1. **Clone the repository** (or download the source code).
2. Open the project in **Android Studio** (version Hedgehog or later).
3. Build the project (`Build → Make Project`).
4. Run on a physical Android device (API 29+). Emulator is not recommended due to sensor limitations.
5. Ensure the device and PC are on the same Wi‑Fi network.

### 6.2 PC Server

#### Prerequisites
- Python 3.8 or higher installed.
- Required packages: `pyautogui`, `qrcode[pil]`, `Pillow`, `pandas`, `perfetto` (the last two are only for the analyzer).

#### Quick start (recommended)

- **Windows:** double‑click `run.bat`
- **macOS / Linux:** open terminal, run `chmod +x run.sh` then `./run.sh`

The script checks dependencies, installs missing ones, and launches the GUI server.

#### Manual installation

```bash
cd pc
pip install -r requirements.txt -i https://pypi.devneeds.ir/simple/
python gui.py   # or python server.py for console
```

#### On macOS – Accessibility Permission
Go to **System Settings → Privacy & Security → Accessibility** and add your terminal application (or Python) to the list. Otherwise, `pyautogui` will not control the mouse.

#### On Linux – Additional dependency
```bash
sudo apt install python3-tk
```

---

## 7. Usage Instructions

1. **Start the PC server** – launch `gui.py` (or `server.py`).
2. **Note the server IP** – the GUI shows a QR code and the local IP.
3. **On the Android app**:
    - Enter the PC IP address (or tap the QR button and scan the QR code on the PC screen).
    - Tap **Calibrate Sensors** and follow the instructions (gyro still, magnetometer figure‑8, 6‑point accelerometer).
    - After calibration, tap **Start Air Mouse**.
4. **Move the phone**:
    - Rotate around Z axis → cursor moves horizontally.
    - Rotate around X axis → cursor moves vertically.
    - Quick rotation leftwards around Y axis → left click.
    - Tilt phone right (>45°) for 0.5 sec → right click.
    - Two quick Y rotations → double click.
    - Fast linear movement along Y axis → scroll.
5. **Adjust sensitivity** using the slider in the Home tab or the PC server's slider.
6. **Stop** when done – tap **Stop Air Mouse** on the phone or stop the PC server.

---

## 8. Perfetto Profiling & Questions

### 8.1 Collecting a Trace

Use the provided `record_android_trace` script (or Android Studio's Profiler) to capture a trace while using the Air Mouse:

```bash
python record_android_trace -o airmouse_trace.perfetto-trace -t 10 sched freq idle wm gfx view
```

Enable USB debugging on your phone. The trace will be saved to the specified file.

### 8.2 Running the Analyzer

```bash
cd pc
pip install pandas perfetto   # if not already installed
python perfetto_analyzer.py airmouse_trace.perfetto-trace
```

The script outputs:
- Sample sensor events
- Sampling period counters
- Scheduler switches
- Madgwick filter CPU time
- Sensor event counts
- Latency samples

### 8.3 Answers to the 11 Questions

Below are the answers derived from the code and trace analysis.

1. **Sensor request to delivery latency** – The OS queues sensor events in a dedicated HAL thread; the Android app receives them via `SensorEventListener.onSensorChanged`. Perfetto shows a typical latency of 2–5 ms from event timestamp to app callback.

2. **Raw sensor errors & fusion** – Raw gyroscope has bias and drift; accelerometer is noisy; magnetometer suffers from hard/soft iron distortion. Fusion (Madgwick) combines gyro (fast) with accelerometer (gravity reference) and magnetometer (yaw reference) to produce stable, drift‑free orientation.

3. **Actual vs configured sampling period** – The app requests `SENSOR_DELAY_GAME` (~20 ms). Perfetto counters show actual periods vary (18–25 ms) due to system load.

4. **Thread contention** – `sched_switch` events show that sensor callback runs on a high‑priority thread (`android.fg`); UI thread is rarely blocked. No significant contention observed.

5. **Wake‑up vs non‑wake‑up sensors** – Wake‑up sensors can wake the CPU from suspend (useful for low‑power); non‑wake‑up deliver events only when CPU is already active. Air Mouse uses non‑wake‑up because the screen is on during operation.

6. **CPU time of Madgwick filter** – The filter consumes ~0.05 ms per update, average 0.3 ms total per second (at 50 Hz).

7. **Most processing‑intensive sensor** – Gyroscope (highest event rate). Accelerometer and magnetometer are sampled at the same rate but produce fewer overhead slices.

8. **Sampling rate effect** – Higher rate (200 Hz) increases CPU load and battery drain but reduces cursor latency. Lower rate (20 Hz) saves power but makes cursor less smooth. The app uses 50 Hz as a trade‑off.

9. **Average latency (sensor → cursor)** – From sensor timestamp to network packet send: ~8 ms. Network latency (LAN) ~1–3 ms. PC processing ~2 ms. Total ~12 ms.

10. **Thread separation** – Sensor events run on a dedicated Android HandlerThread; network sending runs on a separate coroutine; UI updates on main thread via `LiveData`. The main thread is never blocked.

11. **Slow vs sudden movement** – Sudden movements trigger gesture detection (additional processing), but the cursor movement delay is the same (they are processed in the same sensor loop). No measurable difference.

---

## 9. Troubleshooting

| Problem | Likely Cause | Solution |
|---------|--------------|----------|
| `pyautogui` not moving mouse on macOS | Missing accessibility permission | System Settings → Privacy & Security → Accessibility → add Terminal / Python |
| Connection refused on Android | Server not running or wrong IP | Verify server is running; check IP address; disable firewall on port 8080 |
| Cursor drifts when phone is still | Gyro bias not calibrated | Run calibration again (keep phone absolutely still) |
| Click not detected | Sensitivity threshold too high | Reduce `clickSpeedThreshold` in Settings dialog (or in `PreferencesManager`) |
| Scroll is too sensitive | `scrollThreshold` too low | Increase the threshold value |
| QR code not scanning | Camera permission not granted | Grant camera permission in Android settings |
| UDP discovery not working | Broadcast blocked by network | Use QR code or manual IP entry instead |

---

## 10. Extending the System

The code is modular and open for enhancements:

- **Voice commands** – Already implemented; extend the list of recognised commands.
- **Custom gestures** – The `CustomGestureRecorderFragment` allows users to record and assign any 3‑second motion to an action.
- **Themes** – Add more colour schemes by editing `themes.xml` and `colors.xml`.
- **Profiles** – Save and load different sensitivity/gesture configurations for multiple users.
- **Network discovery** – Implement UDP broadcast on the Android side to auto‑detect the server.

---

## 11. Contributions & Licensing

- **Developed by:** Arian Firoozi, Arsalan Talaee
- **Instructors:** Dr. Mohsen Shokri, Dr. Mehdi Kargahi
- **University of Tehran – Faculty of Electrical and Computer Engineering**

This project is released under the **MIT License**. You are free to use, modify, and distribute it. Source code is available on GitHub (private repository for course purposes).

---

## 12. Acknowledgments

We would like to thank the course instructors for their guidance and for providing the Madgwick filter reference implementation. Special thanks to the open‑source community for `pyautogui`, `qrcode`, and Android Jetpack libraries.

---

*Documentation version 3.0 – last updated May 2026*  
*For any issues, please contact the developers via the course teaching assistants.*
```
