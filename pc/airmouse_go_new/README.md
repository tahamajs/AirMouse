# Air Mouse Pro Server

> **Cross‑platform, AI‑powered remote control server** for the Air Mouse Android app.  
> Turn your phone into a wireless mouse with advanced features.

[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Build Status](https://github.com/yourusername/airmouse-go/workflows/Build%20and%20Release/badge.svg)](https://github.com/yourusername/airmouse-go/actions)
[![Docker Pulls](https://img.shields.io/docker/pulls/airmouse/airmouse-server)](https://hub.docker.com/r/airmouse/airmouse-server)
[![Go Report Card](https://goreportcard.com/badge/github.com/yourusername/airmouse-go)](https://goreportcard.com/report/github.com/yourusername/airmouse-go)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Protocol Specification](#-protocol-specification)
- [Configuration](#-configuration)
- [Android App](#-android-app)
- [Development](#-development)
- [API Reference](#-api-reference)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

**Air Mouse Pro Server** is the desktop counterpart of the Air Mouse Android app. It receives movement, click, and gesture commands from one or more mobile devices over **WebSocket** (primary), **TCP** (fallback), **UDP** (discovery), **Bluetooth** (HID), or **USB** (serial/gadget) and translates them into native mouse and keyboard actions on the host computer.

The server is built with **Clean Architecture**, making it modular, testable, and easy to extend. It includes a modern **Fyne‑based GUI**, a responsive **web dashboard**, and a **system tray** application for quick access.

Key capabilities include:

- **Multi‑protocol** – WebSocket (primary), TCP, UDP discovery, Bluetooth HID, USB serial/gadget.
- **Low latency** – Predictive Kalman filtering, AI smoothing, and jitter buffer hide network lag.
- **AI gestures** – Recognise custom gestures (swipes, circles, thumbs‑up) with on‑device TensorFlow Lite or server‑side ONNX models.
- **Proximity lock/unlock** – Uses Bluetooth RSSI to lock the screen when you walk away.
- **Client registry** – Track connected devices, their names, capabilities, and statistics.
- **Modern UI** – Fyne‑based dashboard with real‑time stats, device list, QR pairing, and log viewer, plus a web interface.

---

## ✨ Features

### 📡 Connectivity
| Protocol | Port | Use |
|----------|------|-----|
| **WebSocket** | 8081 | Primary, low‑overhead, real‑time |
| **TCP** | 8080 | Plain TCP (fallback) |
| **UDP discovery** | 8082 | Auto‑detect server on LAN |
| **Bluetooth HID** | – | Emulate a Bluetooth mouse (Linux/macOS/Windows) |
| **USB serial** | – | Wired connection via USB‑CDC |

### 🧠 AI & Prediction
- **Kalman filter** – Movement prediction to reduce perceived lag.
- **ONNX runtime** integration for AI‑based trajectory smoothing (LSTM model).
- **Gesture recognition** – Classify pre‑recorded or custom gestures (DTW + particle filter).
- **Online personalisation** – Collect user movement and fine‑tune models via external Python service.

### 🔐 Proximity‑Aware Security
- **RSSI‑based distance estimation** – Approximate distance from phone to PC.
- **Auto lock** – Lock screen when phone moves beyond a threshold (e.g., 3 m).
- **Auto unlock** – Optional (requires OS support).
- **Bluetooth MAC whitelist** – Restrict to authorised devices.

### 📊 Management & Monitoring
- **Client dashboard** – List all connected devices with uptime, idle time, bytes transferred.
- **Statistics** – Total clicks, double clicks, scrolls, movement distance.
- **Health metrics** – Per‑client ping latency, jitter, message counts.
- **Structured logging** – JSON logs, levels (debug/info/warn/error), hookable.

### 🎨 User Interface
- **Desktop GUI (Fyne)** – Tabs: Dashboard, Devices, Network, Gestures, Proximity, Analytics, Settings, Logs.
- **Web Dashboard** – Real‑time charts, device management, settings, logs.
- **System Tray** – Quick start/stop, QR code, status indicators.
- **QR code generation** – One‑click pairing for the Android app.
- **Theme support** – 8+ built‑in themes (dark, light, pure black, high contrast, ocean, sunset, forest, purple).

---

## 🏗️ Architecture

The project follows **Clean Architecture** (Onion) with four concentric layers:

```
┌─────────────────────────────────────────────────────────┐
│                    Delivery Layer                       │
│         (WebSocket/HTTP handlers, DTOs)                │
├─────────────────────────────────────────────────────────┤
│                    Use Case Layer                       │
│         (mouse, gesture, connection services)          │
├─────────────────────────────────────────────────────────┤
│                   Repository Layer                      │
│         (client, gesture, mouse repositories)          │
├─────────────────────────────────────────────────────────┤
│                 Infrastructure Layer                    │
│      (mouse control, Bluetooth, USB, logging)          │
└─────────────────────────────────────────────────────────┘
```

- **Domain** (`internal/domain`) – pure business logic, no external dependencies.
- **Repository** (`internal/repository`) – concrete implementations of repository interfaces.
- **Handler** (`internal/handler`) – WebSocket and HTTP endpoints, DTOs, message routing.
- **Infra** (`internal/infra`) – platform‑specific mouse control, Bluetooth, USB, logging.

All dependencies point inward. The `cmd/airmouse-server/main.go` wires everything together using **constructor injection**.

---

## 🚀 Quick Start

### Prerequisites
- Go 1.23+ (if building from source)
- For AI smoothing: ONNX Runtime shared library (optional, auto‑fetched by go module)
- For Linux mouse input: `/dev/uinput` writable (or install `xdotool`)
- For Bluetooth: BlueZ (Linux), CoreBluetooth (macOS), Windows Bluetooth stack
- For building the GUI: `gcc` (required by Fyne)

### Installation

#### macOS (Homebrew)
```bash
brew tap yourusername/airmouse
brew install airmouse-server
```

#### Linux (APT)
```bash
sudo apt update
sudo apt install ./airmouse-server.deb
```

#### Windows (Scoop)
```bash
scoop bucket add airmouse https://github.com/yourusername/airmouse-scoop
scoop install airmouse-server
```

#### Docker
```bash
# Pull and run
docker run -d --name airmouse-server \
  -p 8080:8080 -p 8081:8081 -p 8082:8082/udp \
  -v airmouse-data:/var/lib/airmouse \
  -v airmouse-logs:/var/log/airmouse \
  airmouse/airmouse-server:latest

# Or use docker-compose
docker-compose up -d
```

#### From Source
```bash
git clone https://github.com/yourusername/airmouse-go.git
cd airmouse-go
make deps
make build
./airmouse-server
```

### First Run

1. **Start the server** – click “Start Server” in the Dashboard tab or use the system tray.
2. **Connect the Android app** – scan the QR code from the **Network** tab or the system tray menu.
3. **Control** – move your phone; the cursor follows.
4. **Explore** – check connected devices in the **Devices** tab, view statistics, configure gestures, and set up proximity lock.

---

## 📡 Protocol Specification

### Transport
- **Primary**: WebSocket over TCP (port 8081).  
- **Alternative**: Raw TCP (port 8080) – for simple clients.
- **Discovery**: UDP broadcast on port 8082.
- **Message format**: JSON‑line, each message terminated by `\n`.

### Client → Server Messages

| Type | Payload Example | Description |
|------|----------------|-------------|
| `move` | `{"dx": 1.5, "dy": 2.0}` | Relative cursor movement |
| `click` | `{"button": "left"}` | Left, right, or middle click |
| `doubleclick` | `{}` | Double left click |
| `rightclick` | `{}` | Right click |
| `scroll` | `{"delta": 1}` | Scroll up (positive) or down (negative) |
| `hello` | `{"name": "MyPhone", "version": "3.0"}` | Identify device (name appears in dashboard) |
| `ping` | `{}` | Keep‑alive (server responds with `pong`) |
| `gesture` | `{"gesture": "ThumbsUp", "confidence": 0.92}` | Recognised gesture (sent by Android app) |
| `proximity` | `{"device_id": "abc123", "is_near": true, "distance": 1.23}` | Distance update (for auto lock/unlock) |
| `control` | `{"command": "pause_movement"}` | Temporarily stop/resume mouse movement |

### Server → Client Messages

| Type | Payload | Description |
|------|---------|-------------|
| `welcome` | `{"server":"AirMouse","version":"3.0","id":"..."}` | Sent after `hello` |
| `ping` | `{}` | Heartbeat request |
| `pong` | `{}` | Heartbeat response |
| `ack` | `{"id": "..."}` | Acknowledgement of a command (optional) |

### Proximity (Optional Extension)
When the Android app’s proximity service is active, it sends:
```json
{
  "type": "proximity",
  "payload": {
    "is_near": true,
    "distance": 1.23
  }
}
```
The server evaluates thresholds and locks/unlocks the screen accordingly.

---

## ⚙️ Configuration

The server reads `~/.config/airmouse/config.json` (Linux/macOS) or `%APPDATA%\airmouse\config.json` (Windows).  
Example:

```json
{
  "server": {
    "host": "0.0.0.0",
    "port": 8080,
    "websocket_port": 8081,
    "udp_port": 8082,
    "enable_tcp": true,
    "enable_websocket": true,
    "enable_udp": true,
    "enable_bluetooth": true,
    "enable_serial": false,
    "server_name": "Air Mouse Pro",
    "user_name": "User",
    "version": "3.0.0",
    "language": "en",
    "auto_start_server": false
  },
  "mouse": {
    "sensitivity": 1.0,
    "smoothing_enabled": true,
    "acceleration_enabled": true,
    "click_threshold": 5.0,
    "scroll_threshold": 3.0,
    "double_click_interval": 300,
    "right_click_tilt": 45.0,
    "haptic_enabled": true
  },
  "ai": {
    "enable_ai_smoothing": false,
    "ai_model_path": "models/mouse_smoothing.onnx",
    "ai_blend_factor": 0.6,
    "enable_predictive": true,
    "predictive_blend_factor": 0.6,
    "enable_jitter_compensation": true,
    "jitter_max_latency_ms": 100
  },
  "gesture": {
    "gesture_confidence_threshold": 0.7,
    "gesture_cooldown_ms": 500
  },
  "proximity": {
    "proximity_enabled": false,
    "proximity_near_threshold": 1.0,
    "proximity_far_threshold": 3.0
  },
  "bluetooth": {
    "bluetooth_adapter": "default",
    "ble_enabled": true,
    "hid_proxy_enabled": false
  },
  "usb_gadget": {
    "usb_gadget_enabled": false,
    "usb_vendor_id": "0x1d6b",
    "usb_product_id": "0x0104"
  },
  "logging": {
    "log_level": "info",
    "log_file": "",
    "log_color": true
  },
  "ui": {
    "theme": "dark",
    "accent_color": "#6366f1",
    "always_on_top": false,
    "show_tray_icon": true,
    "window_width": 1400,
    "window_height": 900
  }
}
```

All settings can also be changed via the **Settings** tab in the GUI or the web interface.  
The server applies changes immediately and saves them to disk.

---

## 📱 Android App

The Android app is written in **Kotlin** with **Coroutines**, **OkHttp** for WebSocket, and **TensorFlow Lite** for gesture classification. It uses the device’s sensors (gyroscope, accelerometer) to generate movements and recognises gestures using a custom TFLite model.

### Download
- [Google Play Store](https://play.google.com/store/apps/details?id=com.airmouse)
- [GitHub Releases](https://github.com/yourusername/airmouse-android/releases)
- [APK Direct Download](https://github.com/yourusername/airmouse-android/releases/latest)

### Features
- 📱 **Cursor control** – Move your phone to control the mouse cursor.
- 🖱️ **Click & scroll** – Tap for left click, two‑finger tap for right click, swipe for scroll.
- ✋ **Gesture recognition** – Thumbs up/down, swipes, circles, pinch.
- 📡 **Proximity lock** – Auto‑lock/unlock based on Bluetooth distance.
- 🔐 **QR pairing** – Secure one‑time token exchange.
- 📊 **Connection stats** – Latency, jitter, battery status.

---

## 🛠️ Development

### Project Structure

```
airmouse-go/
├── cmd/airmouse-server/          # Entry point
│   ├── main.go                   # Main application
│   └── icons.go                  # Embedded icons
├── internal/
│   ├── domain/                   # Entities, repositories, services
│   │   ├── entity/               # Domain models
│   │   ├── repository/           # Repository interfaces
│   │   └── service/              # Business logic services
│   ├── infra/                    # Infrastructure
│   │   ├── logger/               # Logging
│   │   └── mouse/                # Platform mouse control
│   ├── handler/                  # Delivery layer
│   │   ├── websocket/            # WebSocket hub, handler, client
│   │   ├── http/                 # HTTP router, middleware, static
│   │   └── dto/                  # Data transfer objects
│   ├── ui/                       # Fyne GUI
│   │   ├── app.go                # Main window
│   │   ├── dashboard.go          # Dashboard tab
│   │   ├── devices.go            # Devices tab
│   │   ├── network.go            # Network tab
│   │   ├── gestures.go           # Gestures tab
│   │   ├── proximity.go          # Proximity tab
│   │   ├── analytics.go          # Analytics tab
│   │   ├── settings.go           # Settings tab
│   │   ├── logs.go               # Logs tab
│   │   ├── themes.go             # Theme management
│   │   ├── tray.go               # System tray
│   │   ├── about.go              # About dialog
│   │   ├── helpers.go            # Helper functions
│   │   ├── icons.go              # Icon resources
│   │   ├── pairing.go            # Pairing wizard
│   │   ├── charts.go             # Chart components
│   │   ├── connection.go         # Connection quality widget
│   │   └── shortcuts.go          # Keyboard shortcuts
│   ├── protocol/                 # Protocol implementations
│   │   ├── tcp/                  # TCP server
│   │   ├── udp/                  # UDP discovery
│   │   ├── websocket/            # WebSocket server
│   │   ├── bluetooth/            # BLE manager, HID, serial
│   │   └── usb/                  # USB serial, gadget
│   ├── control/                  # Mouse control, AI, ML
│   │   ├── mouse.go              # Mouse controller
│   │   ├── predictor.go          # Movement predictor
│   │   ├── ai_smoother.go        # ONNX AI smoother
│   │   ├── ml_predictor.go       # LSTM predictor
│   │   ├── gesture.go            # Gesture detection
│   │   └── pause.go              # Movement pause
│   ├── auth/                     # Authentication manager
│   ├── config/                   # Configuration management
│   ├── device/                   # Device manager
│   ├── proximity/                # Proximity manager
│   ├── sysaction/                # System actions (lock, media keys)
│   ├── sensorfusion/             # Madgwick, Mahony filters
│   ├── jitter/                   # Jitter buffer, Kalman
│   ├── particlefilter/           # Particle filter gesture recognition
│   ├── predictiveml/             # ML predictor, trainer
│   ├── personalization/          # Data collector, trainer client
│   ├── adaptivesmoothing/        # Humanizer, B‑spline, tremor
│   └── utils/                    # Utilities (crypto, net, random)
├── web/                          # Web interface assets
│   └── static/
│       ├── index.html            # Web dashboard
│       ├── style.css             # Styles
│       └── app.js                # JavaScript
├── scripts/                      # Installation scripts
├── config.example.json           # Example configuration
├── docker-compose.yml            # Docker Compose setup
├── Dockerfile                    # Container definition
├── Makefile                      # Build automation
├── go.mod                        # Go module
├── go.sum                        # Dependency checksums
├── README.md                     # This file
└── LICENSE                       # MIT License
```

### Build Commands

```bash
make build        # Build for current platform
make build-windows # Build for Windows
make build-linux   # Build for Linux
make build-mac     # Build for macOS Intel
make build-mac-arm # Build for macOS Apple Silicon
make build-all     # Build all platforms
make run           # Run the application
make run-debug     # Run with debug logging
make clean         # Clean build artifacts
```

### Testing

```bash
make test          # Run all tests
make test-coverage # Run tests with coverage report
make bench         # Run benchmarks
```

### Code Quality

```bash
make lint          # Run golangci-lint
make fmt           # Format code
```

### Docker

```bash
make docker-build  # Build Docker image
make docker-run    # Run Docker container
docker-compose up -d  # Run full stack (server + redis + postgres + trainer + monitoring)
```

---

## 📚 API Reference

### REST Endpoints (HTTP)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/api/status` | GET | Server status (running, uptime, clients) |
| `/api/stats` | GET | Statistics (clicks, scrolls, movements) |
| `/api/devices` | GET | List connected devices |
| `/api/qrcode` | GET | Generate pairing QR code |
| `/api/config` | GET/POST | Get/update configuration |
| `/api/gestures` | GET/POST | List/add gestures |
| `/api/proximity` | GET/POST | Get/update proximity settings |
| `/api/logs` | GET | Get system logs |
| `/api/start` | POST | Start server |
| `/api/stop` | POST | Stop server |
| `/api/stats/reset` | POST | Reset statistics |

### WebSocket Endpoint

- **URL**: `ws://<server-ip>:8081/ws`
- **Query params**: `?token=<auth_token>` (if authentication enabled)

---

## 🐛 Troubleshooting

### Server won’t start
- Check that the port is not already in use (`lsof -i :8080`).
- Ensure you have write permission to the config directory.

### WebSocket connection refused
- Verify firewall allows incoming connections on port 8081.
- On Linux, check that `ufw` or `iptables` is not blocking.

### Mouse movement is jerky
- Increase `predictive_blend_factor` (0.7 – 0.9).
- Enable AI smoothing if you have a good ONNX model.
- Reduce network latency (use Ethernet or 5 GHz Wi‑Fi).

### Gesture not recognised
- Record more training samples (5–10 repetitions).
- Lower `gesture_confidence_threshold` (e.g., 0.6).
- Check that the gesture template is properly formatted.

### Proximity lock doesn’t work
- Ensure Bluetooth is enabled and the phone is paired.
- Adjust `near_threshold` and `far_threshold` in config.
- On Linux, verify that `loginctl lock-session` works from terminal.

### AI smoother not loading
- The model file must be placed at `models/mouse_smoothing.onnx`.
- Build with `-tags ai` to include real ONNX support.
- Check that ONNX Runtime shared library is installed (`libonnxruntime.so` on Linux).

### USB gadget not working (Linux)
- Ensure configfs is mounted: `mount | grep configfs`
- Load the libcomposite kernel module: `modprobe libcomposite`
- Run the server as root or with appropriate permissions.

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

Please ensure your code follows the existing style and passes all tests.

### Development Guidelines
- Use `gofmt` for formatting.
- Add tests for new features.
- Update documentation when changing behaviour.
- Follow Clean Architecture principles.

---

## 📄 License

MIT License – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory.  
See [LICENSE](LICENSE) for full text.

---

## 🙏 Acknowledgements

- [Fyne](https://fyne.io/) – Cross-platform GUI toolkit
- [Gorilla WebSocket](https://github.com/gorilla/websocket) – WebSocket implementation
- [ONNX Runtime](https://onnxruntime.ai/) – AI model inference
- [GoQR](https://github.com/skip2/go-qrcode) – QR code generation
- [systray](https://github.com/getlantern/systray) – System tray support

---

## 📧 Contact

- **Issues**: [GitHub Issues](https://github.com/yourusername/airmouse-go/issues)
- **Discord**: [Air Mouse Community](https://discord.gg/airmouse)
- **Email**: support@airmouse.io

---

**Built with Go, Fyne, and ONNX Runtime – ready for production.**
