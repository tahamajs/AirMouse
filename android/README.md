# Air Mouse Pro Android App – Complete Documentation

> **Smartphone‑based remote control** using motion sensors, custom gestures, proximity lock, and multiple connectivity protocols.  
> University of Tehran – Embedded Systems Laboratory

[![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Installation](#-installation)
- [User Guide](#-user-guide)
- [Protocol Specification](#-protocol-specification)
- [Advanced Features](#-advanced-features)
- [Troubleshooting](#-troubleshooting)
- [Development](#-development)
- [License](#-license)

---

## 🎯 Overview

The **Air Mouse Pro Android app** turns your phone into a wireless remote control for your computer. It uses the phone’s gyroscope, accelerometer, and magnetometer to detect orientation changes, fast rotations, and linear movements, then translates them into mouse cursor movements, clicks, scrolls, and custom gestures. The app connects to a desktop server (written in Go) over WebSocket, TCP, UDP, Bluetooth, or USB.

Key capabilities:
- **Cursor movement** by rotating the phone (pitch → vertical, yaw → horizontal).
- **Left‑click** by a quick twist around the Y axis.
- **Scroll** by a fast linear movement along the Y axis.
- **Right‑click** by holding the phone tilted.
- **Double‑click** by two quick twists.
- **Custom gesture recording & recognition** using a TensorFlow Lite model.
- **Proximity lock/unlock** based on Bluetooth RSSI (auto‑lock when you walk away).
- **Voice commands** (click, double click, right click, scroll up/down, stop listening).
- **Edge gestures** (long press volume keys) for quick actions even when screen is off.
- **Touchpad mode** – use the screen as a touchpad.
- **Bluetooth HID mouse** – emulate a real Bluetooth mouse.
- **USB HID / serial** – wired connection.

---

## ✨ Features

### 📡 Connectivity
| Protocol | Port | Use |
|----------|------|-----|
| WebSocket | 8080 (default) | Primary, low‑latency |
| TCP | 8080 | Fallback |
| UDP discovery | 8082 | Auto‑find server on LAN |
| Bluetooth HID | – | Emulate a Bluetooth mouse |
| USB HID | – | Act as a USB mouse (wired) |
| USB serial | – | JSON over USB virtual COM port |

### 🧠 Sensor‑Based Control
- **Gyroscope** – detects orientation changes and fast twists.
- **Accelerometer** – detects linear movements (scroll) and tilt (right‑click).
- **Magnetometer** – used for sensor fusion (optional).
- **Madgwick AHRS filter** – fuses gyro + accel + mag into stable orientation (roll, pitch, yaw).

### 🎮 Gesture Recognition
- **Click** – quick twist around Y axis (angular speed > threshold).
- **Double‑click** – two twists within a configurable time window.
- **Right‑click** – tilt phone (roll > threshold) and hold.
- **Scroll up/down** – fast vertical linear movement (acceleration > threshold).
- **Custom gestures** – record your own (e.g., circle, thumbs‑up) and classify with a TensorFlow Lite model.

### 🔐 Proximity Lock
- Uses Bluetooth RSSI to estimate distance to the PC.
- Locks the computer screen when you walk away (distance > far threshold).
- Unlocks when you return (distance < near threshold).
- Works with the server’s lock/unlock commands.

### 🗣️ Voice Commands
- Built with PocketSphinx (offline, no internet required).
- Commands: click, double click, right click, scroll up, scroll down, stop listening.
- Triggered via a button in the app or by voice keyword.

### 📱 Additional Modes
- **Touchpad mode** – full‑screen touchpad with swipe, tap, two‑finger scroll.
- **Bluetooth HID mode** – phone appears as a standard Bluetooth mouse (no server required).
- **USB HID mode** – phone appears as a USB mouse when connected via OTG cable.

---

## 🏗️ Architecture

The app follows **Clean Architecture** with **MVVM** and **Jetpack Compose** for the UI. Dependencies are injected with **Hilt**.

```
┌─────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                       │
│  (Jetpack Compose Screens, ViewModels, UI State)                │
├─────────────────────────────────────────────────────────────────┤
│                         Domain Layer                             │
│  (Use Cases, Entities, Repository Interfaces)                   │
├─────────────────────────────────────────────────────────────────┤
│                         Data Layer                               │
│  (Repository Implementations, Data Sources: WebSocket,          │
│   Bluetooth, USB, Room, DataStore)                              │
├─────────────────────────────────────────────────────────────────┤
│                         Infrastructure                           │
│  (SensorService, CalibrationHelper, GestureDetector,            │
│   PocketSphinx, TFLite, OkHttp, Bluetooth HID, USB)             │
└─────────────────────────────────────────────────────────────────┘
```

### Key Components

- **SensorService** – foreground service that collects accelerometer, gyroscope, and magnetometer data, runs Madgwick filter, and emits orientation and gesture events.
- **GestureDetector** – thresholds‑based detection of clicks, scrolls, double‑clicks, right‑clicks.
- **CalibrationHelper** – gyroscope bias removal, accelerometer 6‑point calibration, magnetometer hard‑iron calibration.
- **WebSocketManager** / **DataSender** – send movement, click, scroll, and gesture commands to the desktop server.
- **ProximityAwareService** – monitors Bluetooth RSSI, estimates distance, and sends proximity updates.
- **VoiceCommandService** – offline speech recognition.
- **GestureInferenceService** – runs TFLite model to recognise custom gestures.
- **BluetoothMouseService** – HID device emulation (requires system permissions).
- **UsbHidService** / **UsbSerialService** – USB gadget and virtual serial modes.

---

## 🚀 Quick Start

### Prerequisites
- Android 8.0 (API 26) or higher.
- A computer running the [Air Mouse Go Server](https://github.com/yourusername/airmouse-go) (same local network).
- For Bluetooth HID: Android 8+ (system permission may be required).
- For USB HID: USB‑OTG cable and a computer that supports USB HID.

### First Run
1. Install the APK (from `Releases` or build from source).
2. Grant **camera** (for QR scan), **sensors**, **Bluetooth**, **location**, and **overlay** permissions as prompted.
3. On the home screen, enter your computer’s IP address and port (default 8080) or tap the QR icon to scan the QR code from the desktop server.
4. Tap **Connect**. Once connected, the status changes to “Connected”.
5. Tap **Start** to begin sending movement data.
6. Move your phone:
   - Rotate left/right → cursor moves horizontally.
   - Rotate up/down → cursor moves vertically.
   - Quick twist left → left click.
   - Fast upward/downward motion → scroll.
   - Tilt phone sideways and hold → right click.

---

## 📦 Installation

### From APK
Download the latest APK from the [Releases](https://github.com/yourusername/airmouse-android/releases) page and install it.

### Build from Source
```bash
git clone https://github.com/yourusername/airmouse-android.git
cd airmouse-android
# Open in Android Studio or build via command line
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 👤 User Guide

### Home Screen
- **Connection card** – enter server IP/port, connect/disconnect.
- **Calibration card** – shows calibration progress (gyro, accel, mag).
- **Sensor data card** – live yaw and pitch.
- **Gesture stats card** – click, scroll, right‑click, double‑click counters.
- **Controls card** – switch between motion and touchpad mode, toggle AI smoothing and predictive movement (server side).
- **FAB** – quick start/stop.
- **Live log** – shows connection and server logs.

### Calibration Wizard
Access via **Calibrate Sensors** button.

1. **Gyroscope** – place phone on a flat, stationary surface until the progress bar completes.
2. **Accelerometer** – hold phone in the six orientations (screen up, screen down, vertical top, vertical bottom, left edge up, right edge up) one by one.
3. **Magnetometer** – move the phone in a figure‑8 pattern until the ring fills.

After successful calibration, the app saves the parameters and marks the sensors as calibrated.

### Gesture Studio
Record custom gestures for later recognition:

1. Open **Gesture Studio** from the drawer.
2. Enter a gesture name (e.g., `CircleCW`).
3. Tap **Start Recording** and perform the gesture 5‑10 times.
4. Tap **Stop Recording**.
5. Repeat for other gestures.
6. Tap **Export Dataset** to save the CSV file.
7. On your PC, run the Python training script (provided in the server repository) to generate a TFLite model.
8. Copy the `gesture_model.tflite` and `gesture_labels.json` back to the phone’s `assets` folder.
9. The app will now recognise the new gestures and send them to the server (which can be mapped to actions).

### Proximity Lock
1. Pair your phone with the computer via Bluetooth (optional but recommended).
2. Open **Proximity** from the drawer.
3. Enable the service.
4. Set the near (unlock) and far (lock) thresholds (default 2m and 4m).
5. Calibrate by placing the phone exactly 1 metre away and tapping **Calibrate**.
6. When you walk away, the computer screen locks; when you return, it unlocks.

> **Note:** Unlocking may require the computer to be configured to allow automatic unlock (most OSes require password). The lock command works on Windows (LockWorkStation), macOS (CGSession), and Linux (loginctl).

### Voice Commands
1. Open **Voice Commands** from the drawer.
2. Tap **Start Listening**.
3. Say one of the commands: “click”, “double click”, “right click”, “scroll up”, “scroll down”, or “stop listening”.
4. The app will send the corresponding command to the server.

### Edge Gestures
1. Open **Edge Gestures** from the drawer.
2. Enable the toggle and grant accessibility permission when prompted.
3. Choose the action for long‑press of volume up and volume down.
4. The gestures work even when the screen is off.

### Touchpad Mode
- Toggle **Touchpad Mode** on the home screen or in the drawer.
- The screen becomes a touchpad:
  - Single‑finger drag → move cursor.
  - Tap → left click.
  - Two‑finger drag → scroll.
  - Two‑finger tap → right click.
  - Three‑finger tap → (configurable, e.g., back).

### Bluetooth HID Mouse
- Tap **Start Bluetooth Mouse** on the home screen.
- The phone will appear as a Bluetooth mouse named “Air Mouse Pro”.
- Pair with your computer and use it as a standard Bluetooth mouse – no server needed.

### USB HID / Serial
- Connect the phone via USB‑OTG cable to the computer.
- Enable USB tethering or select “USB tethering” mode (the app will automatically detect the connection).
- The phone acts as a USB mouse (HID) or as a virtual serial port sending JSON commands.

---

## 📡 Protocol Specification

The app communicates with the desktop server using JSON‑line messages over WebSocket (or TCP). Each message is terminated by `\n`.

### Client → Server Messages

| Type | Example | Description |
|------|---------|-------------|
| `move` | `{"type":"move","dx":1.5,"dy":2.0}` | Relative cursor movement. |
| `click` | `{"type":"click","button":"left"}` | Left, right, or middle click. |
| `doubleclick` | `{"type":"doubleclick"}` | Double left click. |
| `rightclick` | `{"type":"rightclick"}` | Right click. |
| `scroll` | `{"type":"scroll","delta":1}` | Scroll (positive = up). |
| `hello` | `{"type":"hello","name":"MyPhone","version":"3.0"}` | Identify device (name appears in server’s device list). |
| `ping` | `{"type":"ping"}` | Keep‑alive. |
| `gesture` | `{"type":"gesture","gesture":"ThumbsUp","confidence":0.92}` | Recognised custom gesture. |
| `proximity` | `{"type":"proximity","is_near":true,"distance":1.23}` | Distance update for auto lock/unlock. |
| `control` | `{"type":"control","command":"pause_movement"}` | Temporarily pause/resume movement. |

### Server → Client Messages

| Type | Example | Description |
|------|---------|-------------|
| `welcome` | `{"type":"welcome","server":"AirMouse","version":"3.0"}` | Sent after `hello`. |
| `ping` | `{"type":"ping"}` | Heartbeat request. |
| `pong` | `{"type":"pong"}` | Heartbeat response. |
| `ack` | `{"type":"ack","id":"123"}` | Acknowledgment of a previous message (used for critical commands). |

---

## ⚙️ Advanced Features

### AI Smoothing (Server side)
The desktop server can apply a Kalman filter or an ONNX LSTM model to smooth the cursor movement and compensate for network jitter. Enable it in the server’s settings.

### Predictive Movement (Server side)
The server can predict the next cursor position using a Kalman filter, blending raw and predicted movements for lower latency.

### Personalisation (Server side)
The server collects movement samples and can call an external Python service to fine‑tune the AI model based on the user’s own movement patterns.

### Custom Gesture Training (App side)
As described in the Gesture Studio section – users can record their own gestures and train a TFLite model.

### Accessibility Announcements
The app can announce movement and clicks via TalkBack (enable in Accessibility settings).

### Themes
Choose from 15+ themes (dark, light, pure black, ocean, sunset, forest, purple, cherry, neon, lavender, mint, peach, sky, high contrast). Themes are applied instantly.

---

## 🐛 Troubleshooting

### Cannot connect to server
- Make sure both devices are on the same Wi‑Fi network.
- Disable any firewall that might block port 8080.
- Verify the server is running and listening on the correct IP.
- Use the QR code in the server’s **Network** tab for automatic configuration.

### Cursor moves erratically or drifts
- Run the calibration wizard (gyroscope + accelerometer).
- Place the phone on a flat table and let the gyroscope bias calibration run.
- Increase the dead zone in the server settings.

### Gesture not detected
- Check that the gyroscope and accelerometer are working (sensor data should change when you move the phone).
- Adjust the click threshold in **Settings** (lower = more sensitive).
- For custom gestures, train with more samples (10‑15 repetitions).

### Proximity lock doesn’t work
- Ensure Bluetooth is enabled and the phone is paired with the computer.
- Calibrate the distance (place phone exactly 1 metre away and tap Calibrate).
- On Linux, verify that `loginctl lock-session` works in a terminal.

### Bluetooth HID mouse not pairing
- Android 8+ requires granting the `BLUETOOTH_CONNECT` permission.
- Some devices may not support Bluetooth HID (e.g., some custom ROMs). Use USB HID as an alternative.

### Voice commands not working
- Ensure the app has microphone permission.
- The PocketSphinx models are included in the assets; if missing, reinstall the app.
- The offline recogniser works best in quiet environments.

---

## 🔧 Development

### Project Structure

```
app/src/main/java/com/airmouse/
├── di/                  # Hilt modules
├── domain/              # Entities, repositories, use cases
├── data/                # Repository implementations, data sources
├── presentation/        # Compose UI, ViewModels, navigation
├── service/             # Foreground services (Sensor, Gesture, Voice, Proximity)
├── gesture/             # Gesture recorder and inference
├── orientation/         # Orientation monitor (auto‑pause)
├── bluetooth/           # Bluetooth HID service
├── usb/                 # USB HID and serial services
├── utils/               # Utilities (PreferencesManager, LogManager, etc.)
└── ui/                  # Activities and legacy fragments (for calibration)
```

### Adding a new screen
1. Create a new `@Composable` function in `presentation/ui/`.
2. Add a ViewModel (if needed) and inject repositories.
3. Add a destination in `Destinations.kt`.
4. Add a composable entry in `NavGraph.kt`.
5. Add a navigation action in `NavigationActions.kt`.

### Building and testing
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

### Signing a release
```bash
# Generate a keystore
keytool -genkey -v -keystore airmouse.jks -alias airmouse -keyalg RSA -keysize 2048 -validity 10000

# Build release APK
./gradlew assembleRelease
```

---

## 📄 License

MIT License – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory.  
See [LICENSE](LICENSE) for full text.

---

**Built with ❤️ and Kotlin – turn your phone into a magic wand.**# Smartphone-based_remote_control
