# 🖱️ Air Mouse Pro – Complete Cross-Platform Remote Control System

> **University of Tehran – Embedded Systems Laboratory**  
> *Professional-grade, AI-powered, proximity-aware remote control for presentations, media centers, and everyday productivity*

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
- [Configuration](#-configuration)
- [Protocol Specification](#-protocol-specification)
- [AI-Powered Smoothing](#-ai-powered-smoothing)
- [Proximity-Aware Security](#-proximity-aware-security)
- [Device Management](#-device-management)
- [Statistics & Analytics](#-statistics--analytics)
- [Theming & Customization](#-theming--customization)
- [Troubleshooting](#-troubleshooting)
- [Development](#-development)
- [License](#-license)

---

## 🎯 Overview

**Air Mouse Pro** transforms your Android phone into a **smart, multi‑protocol remote control** for any computer (Windows, macOS, Linux). It combines:

- **Six connection methods** – TCP, WebSocket, UDP, Bluetooth LE, USB/Serial, Wi‑Fi Direct
- **AI‑powered mouse smoothing** – human‑like cursor movement using RNN models
- **Proximity‑aware auto lock/unlock** – using Bluetooth 6.0 Channel Sounding (centimeter‑level distance)
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
| Wi‑Fi Direct | - | Direct peer‑to‑peer |

### 🧠 AI‑Powered Mouse Smoothing
- Pre‑trained RNN (LSTM) model generates **natural velocity curves**
- **User fine‑tuning** – the model learns your personal movement style
- Smoothing + acceleration + gesture detection
- Falls back to traditional EMA smoothing if AI unavailable

### 🔐 Proximity‑Aware Security (Bluetooth 6.0)
- **Channel Sounding** – measures distance with **±30‑50 cm accuracy**
- Automatically **locks** screen when phone moves beyond threshold (e.g., 4 meters)
- Automatically **unlocks** when you return (optional, may require password)
- Hysteresis to avoid rapid toggling
- Falls back to RSSI‑based proximity on older devices

### 📊 Real‑Time Statistics Dashboard
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

### 🛠️ Additional Capabilities
- **Multi‑client** – control from multiple phones simultaneously
- **Device naming** – identify each connected client
- **Automatic reconnection** – watchdog with heartbeat
- **Log exporting** – save logs for debugging
- **Encrypted communication** (optional AES‑GCM)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ANDROID APP                                      │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ Motion      │  │ Touchpad    │  │ Voice       │  │ BLE HID     │     │
│  │ Sensors     │  │ Gestures    │  │ Commands    │  │ Proxy       │     │
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
git clone https://github.com/yourusername/airmouse-go.git
cd airmouse-go
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
- For Bluetooth support: `bluez` (Linux), `win32` Bluetooth stack (Windows), or CoreBluetooth (macOS)
- For AI smoothing: ONNX Runtime shared library (auto‑fetched by go module)

**Build**
```bash
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
git clone https://github.com/yourusername/airmouse-android.git
open in Android Studio → Build → Run
```

**Pre‑built APK**
Download from [Releases](https://github.com/yourusername/airmouse-android/releases)

---

## ⚙️ Configuration

The server stores settings in `~/.config/airmouse/config.json` (Linux/macOS) or `%APPDATA%\airmouse\config.json` (Windows).

### Example configuration

```json
{
  "host": "0.0.0.0",
  "port": 8080,
  "websocket_port": 8081,
  "udp_port": 8082,
  "enable_tcp": true,
  "enable_websocket": true,
  "enable_udp": true,
  "enable_bluetooth": true,
  "sensitivity": 0.5,
  "theme": "dark",
  "enable_ai_smoothing": true,
  "ai_model_path": "models/mouse_smoothing.onnx",
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
| `proximity` | `{"is_near": true, "distance": 1.23}`   | Distance update (Android 16+)      |

### Server → Client

| Type      | Payload                                      | Description              |
|-----------|----------------------------------------------|--------------------------|
| `welcome` | `{"server":"AirMouse","version":"2.0"}`      | Sent after hello         |
| `ping`    | `{}`                                         | Heartbeat request        |
| `pong`    | `{}`                                         | Heartbeat reply          |
| `ack`     | `{"id": "your_message_id"}`                  | Acknowledgment (optional)|

---

## 🧠 AI-Powered Smoothing

### How it works

1. **Pre‑trained RNN model** (LSTM) – trained on 25,000+ real human cursor movements
2. **ONNX Runtime** – fast inference (≈5ms on modern CPU)
3. **User fine‑tuning** – collects your movement data and retrains a personalized model in the background

### Enabling AI smoothing

- **Desktop GUI**: Settings tab → enable "AI Smoothing"
- **Config file**: `"enable_ai_smoothing": true`

### Performance impact

- CPU overhead: <2% on a Core i5
- Memory: ~50 MB for the ONNX model
- Latency added: <10ms (often hidden by network jitter)

### Fine‑tuning your model

The system automatically:
1. Buffers your movements (up to `personalization_buffer` samples)
2. Every `personalization_interval` seconds, launches the Python trainer service
3. Fine‑tunes the base model and swaps it in (if `auto_swap_model` is true)

To force a retrain manually, click "Force Retrain Now" in the Analytics tab.

---

## 🔐 Proximity-Aware Security

### Requirements

| Component          | Minimum Requirement                              |
|--------------------|--------------------------------------------------|
| Android            | Android 16+ (API 36) with Bluetooth 6.0 hardware|
| Desktop (reflector) | Linux kernel 6.15+ (Channel Sounding HCI)      |
| Alternative        | Legacy RSSI‑based proximity (less accurate)      |

### Setup

1. Pair your phone via Bluetooth (optional but recommended)
2. On the desktop server, enable "Auto Lock/Unlock" in Settings → Proximity
3. On the Android app, go to Proximity tab and **calibrate** (place phone at 0.5m, 1m, 2m, 5m)
4. Set your preferred near/far thresholds (default: 2m near, 4m far)

### How it works

- The phone acts as **initiator** (measures distance via Channel Sounding)
- The desktop server acts as **reflector** (responds to sounding packets)
- Every 1–2 seconds, distance is measured and reported over WebSocket
- When distance exceeds `far_threshold` for >3 consecutive readings, the screen locks
- When distance drops below `near_threshold` for >2 readings, it unlocks (if `auto_unlock_enabled`)

### Fallback mode

If Bluetooth 6.0 is not available, the system uses **RSSI‑based distance estimation** (less accurate, may vary by environment). You can still lock/unlock, but false triggers are more likely.

---

## 📊 Statistics & Analytics

The Dashboard tab displays:

| Statistic          | Description                                   |
|--------------------|-----------------------------------------------|
| Click count        | Total left clicks                             |
| Double click count | Total double clicks                           |
| Right click count  | Total right clicks                            |
| Scroll count       | Total scroll events (positive/negative combined)|
| Connected devices  | Number of active clients                      |
| Server uptime      | Time since last start                         |
| AI smoothing status| Enabled / Disabled / Personalizing            |

Additionally, the **Logs** tab shows real‑time events (info, warning, error) with filtering and export.

### Performance metrics (hidden in GUI, available via API)

- CPU usage (percent)
- Memory usage (MB)
- Active goroutines
- Incoming/outgoing bytes per protocol

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
- Enable AI smoothing for human‑like velocity
- Check Wi‑Fi signal strength (RSSI > -65dBm recommended)

### Bluetooth proximity doesn't work

- Verify both devices support Bluetooth 6.0 (Android 16+, Linux kernel 6.15+)
- Calibrate the Channel Sounding (Settings → Proximity → Calibrate)
- Try the RSSI fallback mode (disable "Use Channel Sounding")

### AI smoothing model not loading

- Ensure `models/mouse_smoothing.onnx` exists (download from Releases)
- Check that ONNX Runtime library is installed (`libonnxruntime.so` on Linux)
- For fine‑tuning, make sure Python trainer service is running (`python3 python_trainer/trainer_server.py`)

---

## 🔧 Development

### Building from source

```bash
# Desktop server
git clone https://github.com/yourusername/airmouse-go
cd airmouse-go
go mod download
go build -o airmouse-server ./cmd/airmouse-server

# Android app
git clone https://github.com/yourusername/airmouse-android
open in Android Studio → Build → Build APK
```

### Running tests

```bash
go test -v ./...
```

### Generating a new AI model

1. Collect mouse movement data (use `data_collector.py` in `python_trainer/`)
2. Train the model: `python python_trainer/train.py --data data/mouse_log.csv`
3. Export to ONNX: `python python_trainer/export_onnx.py`
4. Place the `.onnx` file in `models/`

### Adding a new theme

1. Define colors in `internal/ui/themes.go`
2. Add theme name to `getThemeByName()`
3. Add option to settings dropdown

---

## 📄 License

**MIT License** – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software.

---

## 🙏 Acknowledgements

- [Fyne](https://fyne.io/) – cross‑platform GUI toolkit
- [ONNX Runtime](https://onnxruntime.ai/) – high‑performance inference
- [robotgo](https://github.com/go-vgo/robotgo) – mouse control (fallback)
- [gorilla/websocket](https://github.com/gorilla/websocket) – WebSocket server
- [hashicorp/mdns](https://github.com/hashicorp/mdns) – Bonjour/Zeroconf
- [gopsutil](https://github.com/shirou/gopsutil) – system metrics
- All contributors and open‑source maintainers

---

**Built with ❤️ at University of Tehran – Winter 2025**