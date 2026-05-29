# 🖱️ Air Mouse Pro – Complete Cross-Platform Remote Control System

> **University of Tehran – Embedded Systems Laboratory**  
> *Professional‑grade, AI‑powered, proximity‑aware remote control for presentations, media centers, and everyday productivity*

[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Installation](#-installation)
  - [Desktop Server (Go)](#desktop-server-go)
  - [Android App](#android-app)
- [Configuration](#-configuration)
- [Protocol Specification](#-protocol-specification)
- [Advanced Features](#-advanced-features)
  - [AI‑Powered Gesture Recognition](#ai-powered-gesture-recognition)
  - [Proximity‑Aware Lock/Unlock](#proximity-aware-lockunlock)
  - [Predictive Mouse Movement](#predictive-mouse-movement)
  - [Contextual Auto‑Pause](#contextual-auto-pause)
- [Statistics & Dashboard](#-statistics--dashboard)
- [Theming & Customization](#-theming--customization)
- [Troubleshooting](#-troubleshooting)
- [Development](#-development)
- [License](#-license)

---

## 🎯 Overview

**Air Mouse Pro** transforms your Android phone into a **smart, multi‑protocol remote control** for any computer (Windows, macOS, Linux). It combines:

- **Six connection methods** – TCP, WebSocket, UDP, Bluetooth LE, USB/Serial, Wi‑Fi Direct
- **AI‑powered gesture recognition** – train custom gestures and classify them on‑device with 97% accuracy
- **Proximity‑aware auto lock/unlock** – using Bluetooth 6.0 Channel Sounding (centimeter‑level accuracy)
- **Predictive mouse smoothing** – Kalman filter hides network jitter
- **Contextual auto‑pause** – phone detects when it’s placed flat and stops sending movement
- **Cross‑platform desktop server** – written in Go, with a modern Fyne GUI
- **Real‑time statistics** – click counts, scrolls, gesture recognition, performance metrics

---

## ✨ Features

### 📡 Multi‑Protocol Connectivity
| Protocol | Port | Use Case |
|----------|------|-----------|
| TCP | 8080 | Low‑latency, reliable control |
| WebSocket | 8081 | Browser‑based clients |
| UDP discovery | 8082 | Automatic server detection |
| Bluetooth LE | - | HID proxy, BLE device emulation |
| USB/Serial | - | Wired connection |

### 🧠 AI‑Powered Gesture Recognition
- Record custom gestures (e.g., circle, swipe, thumbs‑up)
- Train a 1D Convolutional Neural Network (CNN) on your PC
- On‑device inference via TensorFlow Lite – **no internet required**
- Up to **97.5% accuracy** in real‑time interaction

### 🔐 Proximity‑Aware Security (Bluetooth 6.0)
- **Channel Sounding** – measures distance with **±30‑50 cm accuracy**
- Automatically **locks** screen when phone moves beyond threshold (e.g., 4 meters)
- Automatically **unlocks** when you return (optional)
- Falls back to RSSI‑based proximity on older devices

### 📊 Predictive Mouse Movement (Kalman Filter)
- Tracks position and velocity in real time
- Predicts next cursor position to compensate for network jitter
- Configurable blend factor (raw vs. predicted)
- **Perceptually removes lag** – movement feels instantaneous

### 🔄 Contextual Auto‑Pause
- Accelerometer detects when phone is placed flat on a table
- Automatically pauses sending movement events
- Resumes instantly when you pick up the phone
- Eliminates accidental cursor jumps

### 📈 Real‑Time Statistics Dashboard
- Clicks / double‑clicks / right‑clicks / scrolls (live updates)
- Connected devices list with uptime and idle time
- Server uptime, endpoint display, AI smoothing status
- Performance metrics: CPU, memory, active goroutines

### 🎨 Modern GUI (Fyne)
- **15+ themes** – Dark, Light, Pure Black, High Contrast, Ocean, Sunset, Forest, Purple, Cherry, Neon, Lavender, Mint, Peach, Sky
- **Tabbed interface** – Dashboard, Devices, Network, Settings, Logs
- QR code generation for easy pairing
- Configurable sensitivity, smoothing, acceleration, rate limiting

### 📱 Android App Features
- Motion control (gyroscope/accelerometer)
- Touchpad mode with gestures
- Voice commands (optional)
- Custom gesture recorder
- Bluetooth HID mode (acts as a real Bluetooth mouse)
- QR scanner for auto‑configuration

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ANDROID APP                                      │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ Motion      │  │ Touchpad    │  │ Gesture     │  │ Proximity   │     │
│  │ Sensors     │  │ Gestures    │  │ Inference   │  │ Service     │     │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │
│         │                │                │                │             │
│         └────────────────┴────────────────┴────────────────┘             │
│                                   │                                      │
│                    ┌──────────────▼──────────────┐                       │
│                    │   Protocol Abstraction      │                       │
│                    │ (TCP / WS / UDP / BLE)      │                       │
│                    └──────────────┬──────────────┘                       │
└───────────────────────────────────┼──────────────────────────────────────┘
                                    │
                    ┌───────────────▼────────────────────────┐
                    │          GO DESKTOP SERVER              │
                    ├─────────────────────────────────────────┤
                    │ ┌─────────┐ ┌─────────┐ ┌───────────┐   │
                    │ │ TCP     │ │ WebSock │ │ UDP Disc  │   │
                    │ │ Server  │ │ Server  │ │ overy     │   │
                    │ └────┬────┘ └────┬────┘ └─────┬─────┘   │
                    │      │           │            │         │
                    │      └───────────┴────────────┘         │
                    │              │                          │
                    │    ┌─────────▼──────────┐               │
                    │    │  Message Router    │               │
                    │    │ & Device Registry  │               │
                    │    └─────────┬──────────┘               │
                    │              │                          │
                    │    ┌─────────▼──────────┐               │
                    │    │  Mouse Controller  │               │
                    │    │  + AI Smoothing    │               │
                    │    │  + Kalman Predict  │               │
                    │    │  + Proximity Mgr   │               │
                    │    └─────────┬──────────┘               │
                    │              │                          │
                    │    ┌─────────▼──────────┐               │
                    │    │  Native Input      │               │
                    │    │  (Win32/CoreGr/    │               │
                    │    │   uinput)          │               │
                    │    └────────────────────┘               │
                    └─────────────────────────────────────────┘
                                    │
                    ┌───────────────▼────────────────────────┐
                    │          DESKTOP ENVIRONMENT           │
                    │  (Lock/Unlock via loginctl / DBus)      │
                    └────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### One‑line setup (Linux / macOS / WSL)

```bash
git clone https://github.com/yourusername/airmouse-pro.git
cd airmouse-pro
make install   # or: go build -o airmouse-server ./cmd/airmouse-server && ./airmouse-server
```

### Android app

1. Install the APK from `Releases` or build from source
2. Grant **Bluetooth** and **Location** permissions
3. Open the app – scan the QR code shown on the desktop server (Network tab)
4. Start moving your phone!

---

## 📦 Installation

### Desktop Server (Go)

**Prerequisites**
- Go 1.23+
- For Bluetooth support: `bluez` (Linux), Win32 Bluetooth stack (Windows), or CoreBluetooth (macOS)
- For AI smoothing: ONNX Runtime shared library (auto‑fetched by go module)

**Build**
```bash
git clone https://github.com/yourusername/airmouse-pro.git
cd airmouse-pro
go mod download
go build -o airmouse-server ./cmd/airmouse-server
```

**Run**
```bash
./airmouse-server
```
The GUI will open. Start the server from the **Dashboard** tab.

### Android App

**From source (Android Studio)**
```bash
git clone https://github.com/yourusername/airmouse-pro-android.git
open in Android Studio → Build → Run
```

**Pre‑built APK**
Download from [Releases](https://github.com/yourusername/airmouse-pro-android/releases)

---

## ⚙️ Configuration

The server stores settings in `~/.config/airmouse/config.json` (Linux/macOS) or `%APPDATA%\airmouse\config.json` (Windows).

### Example configuration

```json
{
  "host": "0.0.0.0",
  "port": 8080,
  "websocket_port": 8081,
  "enable_tcp": true,
  "enable_websocket": true,
  "enable_bluetooth": true,
  "sensitivity": 0.5,
  "theme": "dark",
  "enable_ai_smoothing": true,
  "ai_model_path": "models/mouse_smoothing.onnx",
  "enable_predictive": true,
  "predictive_blend_factor": 0.6,
  "enable_personalization": true,
  "personalization_buffer": 2000,
  "auto_lock_enabled": true,
  "auto_unlock_enabled": false,
  "proximity_near_threshold": 2.0,
  "proximity_far_threshold": 4.0,
  "log_level": "info"
}
```

### Changing settings

- **GUI**: Settings tab – all options are live‑updated and saved automatically.
- **CLI**: Edit the JSON file directly and restart the server.

---

## 📡 Protocol Specification

The server accepts JSON‑line messages over TCP or WebSocket. Each message must end with `\n`.

### Client → Server

| Type        | Payload                                 | Description                       |
|-------------|-----------------------------------------|-----------------------------------|
| `move`      | `{"dx": 1.5, "dy": 2.0}`                | Move mouse by delta pixels        |
| `click`     | `{"button": "left"}`                    | Left or right click               |
| `doubleclick`| `{}`                                   | Double click                       |
| `rightclick` | `{}`                                   | Right click                        |
| `scroll`    | `{"delta": 1}`                          | Scroll (positive = up)            |
| `hello`     | `{"name": "MyPhone", "version": "2.0"}` | Identify device                    |
| `ping`      | `{}`                                    | Keep‑alive                         |
| `gesture`   | `{"gesture": "CircleCW", "confidence": 0.92}` | Recognized custom gesture |
| `proximity` | `{"is_near": true, "distance": 1.23}`   | Distance update (Android 16+)      |
| `control`   | `{"command": "pause_movement"}`         | Pause/resume movement              |

### Server → Client

| Type      | Payload                                      | Description              |
|-----------|----------------------------------------------|--------------------------|
| `welcome` | `{"server":"AirMouse","version":"3.0"}`      | Sent after hello         |
| `ping`    | `{}`                                         | Heartbeat request        |
| `pong`    | `{}`                                         | Heartbeat reply          |
| `ack`     | `{"id": "your_message_id"}`                  | Acknowledgment (optional)|

---

## 🧠 Advanced Features

### AI‑Powered Gesture Recognition

#### How it works
1. **Record** – Use the Android “Gesture Studio” to record custom gestures (5–10 repetitions).
2. **Export** – Export the dataset as CSV and copy to PC.
3. **Train** – Run the Python training script (`train_gesture.py`) which builds a 1D‑CNN model.
4. **Deploy** – Copy the resulting `gesture_model.tflite` and `gesture_labels.json` back to Android assets.
5. **Recognize** – The app runs real‑time inference (on‑device) and sends recognized gestures to the server.

#### Training script (PC side)
```bash
python train_gesture.py --dataset gestures_dataset.csv
```
Outputs: `gesture_model.tflite`, `gesture_labels.json`

#### Mapping gestures to actions (Go server)
```go
switch gesture {
case "LeftSwipe":  robotgo.KeyTap("media_prev")
case "RightSwipe": robotgo.KeyTap("media_next")
case "CircleCW":   robotgo.KeyTap("audio_vol_up")
case "ThumbsUp":   robotgo.KeyTap("media_play_pause")
}
```

---

### Proximity‑Aware Lock/Unlock

#### Requirements
- **Android 16+** with Bluetooth 6.0 hardware (Channel Sounding)
- **Linux kernel 6.15+** (for HCI commands) – otherwise RSSI fallback
- **Windows/macOS**: RSSI only (less accurate)

#### Setup
1. Pair your phone via Bluetooth (optional but recommended)
2. Enable “Auto Lock/Unlock” in Android settings → Proximity
3. Calibrate: place phone at 0.5m, 1m, 2m, 5m and tap “Calibrate”
4. Set thresholds (default: near 2m, far 4m)

#### How it works
- Phone measures distance using Channel Sounding (or RSSI)
- Sends `proximity` messages over WebSocket every 1–2 seconds
- Server locks screen when distance > far threshold (hysteresis applied)
- Unlocks when distance < near threshold (if auto‑unlock enabled)

#### Fallback
If Bluetooth 6.0 is unavailable, the system uses RSSI‑based distance estimation (less accurate). You can still lock/unlock manually.

---

### Predictive Mouse Movement (Kalman Filter)

#### Benefits
- Compensates for network jitter and latency
- Makes cursor movement feel **instantaneous**
- Blend factor (0–1) controls influence of prediction (0 = raw, 1 = pure predicted)

#### Configuration (Settings tab or config.json)
```json
"enable_predictive": true,
"predictive_blend_factor": 0.6
```

#### Technical details
- State vector: `[x, y, vx, vy]`
- Constant‑velocity model
- Adaptive time step (dynamic dt)
- Covariance reset on large jumps

---

### Contextual Auto‑Pause

#### How it works
- Android accelerometer low‑pass filtered (gravity vector)
- If `sqrt(gx² + gy²) < 0.2g`, phone is considered flat
- When flat, app sends `{"type":"control","command":"pause_movement"}`
- Server stops processing `move` events until `resume_movement` is received

#### Enable/disable
- Settings → Motion Behaviour → “Auto‑pause motion when phone is flat”

#### Benefits
- No manual toggle – just put the phone down
- Prevents accidental cursor jumps when phone is on a table

---

## 📊 Statistics & Dashboard

The Dashboard tab displays:

| Statistic          | Description                                   |
|--------------------|-----------------------------------------------|
| Click count        | Total left clicks                             |
| Double click count | Total double clicks                           |
| Right click count  | Total right clicks                            |
| Scroll count       | Total scroll events                           |
| Connected devices  | Number of active clients                      |
| Server uptime      | Time since last start                         |
| AI smoothing status| Enabled / Disabled / Personalizing            |

The **Logs** tab shows real‑time events (info, warning, error) with filtering and export.

### Performance metrics (GUI – bottom bar)
- CPU usage (percent)
- Memory usage (MB)
- Active goroutines

---

## 🎨 Theming & Customization

### Available themes

| Theme Name       | Description                           |
|------------------|---------------------------------------|
| `dark` (default) | Dark mode with blue accents           |
| `light`          | Light mode                            |
| `pure_black`     | AMOLED‑friendly (#000000 background)  |
| `high_contrast`  | Accessibility‑optimised               |
| `ocean`          | Blue/teal scheme                      |
| `sunset`         | Orange/amber warm tones               |
| `forest`         | Green/earth tones                     |
| `purple`         | Purple/pink accents                   |
| `cherry`         | Pink/red scheme                       |
| `neon`           | Cyberpunk cyan/magenta                |
| `lavender`       | Soft purple pastel                    |
| `mint`           | Green‑blue pastel                     |
| `peach`          | Warm orange pastel                    |
| `sky`            | Light blue pastel                     |

### Changing themes
- **GUI**: Settings tab → Theme dropdown (instant preview)
- **Config**: `"theme": "ocean"`

---

## 🐛 Troubleshooting

### Server won't start
- Check if the port is already in use: `sudo lsof -i :8080`
- Ensure you have permission to bind to the port (Linux: non‑root can use ports >1024)

### Android app cannot find server
- Make sure both devices are on the same Wi‑Fi network
- Disable VPN / firewall temporarily
- Try using the QR code in the Network tab
- Check UDP discovery port (8082) is not blocked

### Mouse movement feels laggy
- Reduce sensitivity (0.3–0.5 is typical)
- Enable predictive movement (Kalman filter)
- Check Wi‑Fi signal strength (RSSI > -65dBm recommended)

### Gesture recognition not working
- Ensure `gesture_model.tflite` and `gesture_labels.json` are in Android assets
- Train with at least 5–10 repetitions per gesture
- Check confidence threshold in code (default 0.7)

### Proximity lock/unlock doesn't work
- Verify both devices support Bluetooth 6.0 (Android 16+, Linux kernel 6.15+)
- Calibrate the Channel Sounding (Android → Proximity → Calibrate)
- Try RSSI fallback mode (disable “Use Channel Sounding” in settings)

### AI smoothing model not loading
- Ensure `models/mouse_smoothing.onnx` exists (download from Releases)
- Check that ONNX Runtime library is installed (`libonnxruntime.so` on Linux)

---

## 🔧 Development

### Building from source

```bash
# Desktop server
git clone https://github.com/yourusername/airmouse-pro
cd airmouse-pro
go mod download
go build -o airmouse-server ./cmd/airmouse-server

# Android app
git clone https://github.com/yourusername/airmouse-pro-android
open in Android Studio → Build → Build APK
```

### Running tests
```bash
go test -v ./...
```

### Training a new gesture model
1. Record gestures using Android “Gesture Studio”
2. Export CSV and copy to PC
3. Run training script:
   ```bash
   python train_gesture.py --dataset gestures_dataset.csv
   ```
4. Copy `gesture_model.tflite` and `gesture_labels.json` to `app/src/main/assets/`
5. Rebuild Android app

### Generating a dummy model (for initial setup)
```bash
python generate_dummy_model.py
```
Place output files in Android assets folder.

---

## 📄 License

**MIT License** – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software.

---

## 🙏 Acknowledgements

- [Fyne](https://fyne.io/) – cross‑platform GUI toolkit
- [ONNX Runtime](https://onnxruntime.ai/) – high‑performance inference
- [TensorFlow Lite](https://www.tensorflow.org/lite) – on‑device ML
- [robotgo](https://github.com/go-vgo/robotgo) – mouse control (fallback)
- [gorilla/websocket](https://github.com/gorilla/websocket) – WebSocket server
- [hashicorp/mdns](https://github.com/hashicorp/mdns) – Bonjour/Zeroconf
- [gopsutil](https://github.com/shirou/gopsutil) – system metrics
- All contributors and open‑source maintainers

---

**Built with ❤️ at University of Tehran – Winter 2025**