# Air Mouse Pro Server – Complete Documentation

> **Cross‑platform, AI‑powered remote control server** for the Air Mouse Android app.  
> Written in Go, supports multiple protocols, AI smoothing, gesture recognition, proximity lock/unlock, and a modern desktop GUI.

[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

---

## Table of Contents

- [Air Mouse Pro Server – Complete Documentation](#air-mouse-pro-server--complete-documentation)
  - [Table of Contents](#table-of-contents)
  - [🎯 Overview](#-overview)
  - [✨ Features](#-features)
    - [📡 Connectivity](#-connectivity)
    - [🧠 AI \& Prediction](#-ai--prediction)
    - [🔐 Proximity‑Aware Security](#-proximityaware-security)
    - [📊 Management \& Monitoring](#-management--monitoring)
    - [🎨 User Interface (Fyne)](#-user-interface-fyne)
  - [🏗️ Architecture](#️-architecture)
  - [📦 Installation](#-installation)
    - [Prerequisites](#prerequisites)
    - [Build from source](#build-from-source)
    - [Run](#run)
    - [Docker (optional)](#docker-optional)
    - [Cross‑compile](#crosscompile)
  - [⚙️ Configuration](#️-configuration)
  - [📡 Protocol Specification](#-protocol-specification)
    - [Transport](#transport)
    - [Client → Server Messages](#client--server-messages)
    - [Server → Client Messages](#server--client-messages)
    - [Proximity (Optional Extension)](#proximity-optional-extension)
  - [🔌 Command‑Line Flags](#-commandline-flags)
  - [🖥️ GUI Tabs](#️-gui-tabs)
  - [🧠 AI \& Predictive Features](#-ai--predictive-features)
    - [Kalman Predictor](#kalman-predictor)
    - [AI Smoother (ONNX)](#ai-smoother-onnx)
    - [Gesture Recognition](#gesture-recognition)
  - [🔐 Proximity Security](#-proximity-security)
  - [🤖 Personalization \& Training](#-personalization--training)
  - [⌨️ System Actions](#️-system-actions)
  - [🚀 Cross‑Platform Builds](#-crossplatform-builds)
  - [🐛 Troubleshooting](#-troubleshooting)
    - [Server won’t start](#server-wont-start)
    - [WebSocket connection refused](#websocket-connection-refused)
    - [Mouse movement is jerky](#mouse-movement-is-jerky)
    - [Gesture not recognised](#gesture-not-recognised)
    - [Proximity lock doesn’t work](#proximity-lock-doesnt-work)
    - [AI smoother not loading](#ai-smoother-not-loading)
  - [🧪 Development](#-development)
    - [Project Structure](#project-structure)
    - [Adding a New Gesture Type](#adding-a-new-gesture-type)
    - [Running Tests](#running-tests)
    - [Building Without AI Features](#building-without-ai-features)
  - [🤝 Contributing](#-contributing)
  - [📄 License](#-license)

---

## 🎯 Overview

**Air Mouse Pro Server** is the desktop counterpart of the Air Mouse Android app. It receives movement, click, and gesture commands from one or more mobile devices over WebSocket (or TCP/UDP) and translates them into native mouse and keyboard actions on the host computer. The server is built with **Clean Architecture**, making it modular, testable, and easy to extend.

Key capabilities include:

- **Multi‑protocol** – WebSocket (primary), TCP (fallback), UDP discovery, Bluetooth (HID), USB serial.
- **Low latency** – Predictive Kalman filtering and jitter buffer hide network lag.
- **AI gestures** – Recognise custom gestures (swipes, circles, thumbs‑up) with on‑device TensorFlow Lite or server‑side ONNX models.
- **Proximity lock/unlock** – Uses Bluetooth RSSI (or Channel Sounding) to lock the screen when you walk away.
- **Client registry** – Track connected devices, their names, capabilities, and statistics.
- **Modern GUI** – Fyne‑based dashboard with real‑time stats, device list, QR pairing, and log viewer.

---

## ✨ Features

### 📡 Connectivity
| Protocol | Port | Use |
|----------|------|-----|
| **WebSocket** | 8080 | Primary, low‑overhead, real‑time |
| **TCP** | 8080 | Plain TCP (fallback) |
| **UDP discovery** | 8082 | Auto‑detect server on LAN |
| **Bluetooth HID** | – | Emulate a Bluetooth mouse (Linux/macOS) |
| **USB serial** | – | Wired connection via USB‑CDC |

### 🧠 AI & Prediction
- **Kalman filter** for movement prediction – reduces perceived lag.
- **ONNX runtime** integration for AI‑based trajectory smoothing (LSTM model).
- **Gesture recognition** – classify pre‑recorded or custom gestures (DTW + particle filter).
- **Online personalisation** – collect user movement and fine‑tune models via external Python service.

### 🔐 Proximity‑Aware Security
- **RSSI‑based distance estimation** – approximate distance from phone to PC.
- **Auto lock** – lock screen when phone moves beyond a threshold (e.g., 4 m).
- **Auto unlock** – optional (requires OS support).
- **Bluetooth MAC whitelist** – restrict to authorised devices.

### 📊 Management & Monitoring
- **Client dashboard** – list all connected devices with uptime, idle time, bytes transferred.
- **Statistics** – total clicks, double clicks, scrolls, movement distance.
- **Health metrics** – per‑client ping latency, jitter, message counts.
- **Structured logging** – JSON logs, levels (info/warn/error), hookable.

### 🎨 User Interface (Fyne)
- **Tabs**: Dashboard, Devices, Network, Settings, Logs.
- **QR code generation** – one‑click pairing for the Android app.
- **Theme support** – 15+ built‑in themes (dark, light, pure black, ocean, etc.).
- **Real‑time updates** – stats and device list refresh automatically.

---

## 🏗️ Architecture

The project follows **Clean Architecture** (Onion) with four concentric layers:

```
┌─────────────────────────────────────────────────────────────┐
│                     Delivery Layer                          │
│  (WebSocket/HTTP handlers, DTOs, middleware)               │
├─────────────────────────────────────────────────────────────┤
│                      Use Case Layer                         │
│  (domain services – mouse, gesture, connection)            │
├─────────────────────────────────────────────────────────────┤
│                     Repository Layer                        │
│  (interfaces for data access – client, gesture, mouse)     │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                      │
│  (mouse controller, Bluetooth, USB, logger, etc.)          │
└─────────────────────────────────────────────────────────────┘
```

- **Domain** (`internal/domain`) – pure business logic, no external dependencies.
- **Repository** (`internal/repository`) – concrete implementations of repository interfaces.
- **Handler** (`internal/handler`) – WebSocket and HTTP endpoints, DTOs, message routing.
- **Infra** (`internal/infra`) – platform‑specific mouse control, Bluetooth, USB, logging.

All dependencies point inward. The `cmd/airmouse-server/main.go` wires everything together using **constructor injection**.

---

## 📦 Installation

### Prerequisites
- Go 1.23+
- For AI smoothing: ONNX Runtime shared library (optional, auto‑fetched by go module)
- For Linux mouse input: `/dev/uinput` writable (or install `xdotool`)
- For Bluetooth: BlueZ (Linux), CoreBluetooth (macOS)
- For building the GUI: `gcc` (required by Fyne)

### Build from source

```bash
git clone https://github.com/yourusername/airmouse-go.git
cd airmouse-go
make deps
make build
```

### Run

```bash
./airmouse-server
```

The GUI will open. You can then:

1. **Connect the Android app** – scan the QR code from the **Network** tab.
2. **Start the server** – click “Start Server” in the **Dashboard** tab.
3. **Control** – move your phone; the cursor follows.

### Docker (optional)

```bash
docker build -t airmouse-server .
docker run -p 8080:8080 -p 8081:8081 airmouse-server
```

### Cross‑compile

```bash
make build-windows   # airmouse-server.exe
make build-linux     # airmouse-server-linux
make build-mac       # airmouse-server-mac
```

---

## ⚙️ Configuration

The server reads `~/.config/airmouse/config.json` (Linux/macOS) or `%APPDATA%\airmouse\config.json` (Windows).  
Example:

```json
{
  "host": "0.0.0.0",
  "port": 8080,
  "websocket_port": 8081,
  "udp_port": 8082,
  "enable_tcp": true,
  "enable_websocket": true,
  "enable_udp": true,
  "enable_bluetooth": false,
  "sensitivity": 0.5,
  "enable_ai_smoothing": false,
  "ai_model_path": "models/mouse_smoothing.onnx",
  "enable_predictive": true,
  "predictive_blend_factor": 0.6,
  "theme": "dark",
  "log_level": "info"
}
```

All settings can also be changed via the **Settings** tab in the GUI.  
The server applies changes immediately and saves them to disk.

---

## 📡 Protocol Specification

### Transport
- **Primary**: WebSocket over TCP (port 8080).  
- **Alternative**: Raw TCP (same port) – for simple clients.
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
| `welcome` | `{"server":"AirMouse","version":"3.0"}` | Sent after `hello` |
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

## 🔌 Command‑Line Flags

Not yet implemented; all configuration is via `config.json` or GUI.

---

## 🖥️ GUI Tabs

1. **Dashboard**  
   - Start/stop server  
   - Live click, double‑click, right‑click, scroll counters  
   - Connected device count  
   - Server uptime  
   - AI smoothing status

2. **Devices**  
   - List of all connected clients  
   - Shows name, protocol (websocket/tcp), connection time  
   - Auto‑refreshes every 2 seconds

3. **Network**  
   - Select local IP from detected addresses  
   - Set WebSocket port  
   - Generate QR code for Android app pairing  
   - Copy endpoint to clipboard

4. **Gestures**  
   - Predefined gesture templates  
   - Import/export templates (JSON)  
   - Train new gestures (recording interface)

5. **Proximity**  
   - Enable/disable automatic lock/unlock  
   - Adjust near/far thresholds (sliders)  
   - Calibrate RSSI‑to‑distance mapping  
   - Show current estimated distance

6. **Settings**  
   - Cursor sensitivity  
   - Theme selector (15+ themes)  
   - Smoothing (EMA) toggle  
   - Acceleration toggle & factor  
   - AI smoothing toggle (requires ONNX model)  
   - Predictive movement toggle & blend factor  
   - Personalisation: buffer size, retrain interval, auto‑swap

7. **Logs**  
   - Real‑time log messages (info, warn, error)  
   - Filter by level  
   - Export logs to file  
   - Clear logs

---

## 🧠 AI & Predictive Features

### Kalman Predictor
- Filters incoming movement deltas to reduce jitter.
- Blend factor (0–1) controls how much prediction affects output.
- Enabled by default (`enable_predictive: true`).

### AI Smoother (ONNX)
- Uses a pre‑trained LSTM model to generate human‑like trajectories.
- Expects model at `models/mouse_smoothing.onnx` (download from releases or train your own).
- Requires ONNX Runtime – use `go build -tags ai` to enable real inference; otherwise a stub is used.

### Gesture Recognition
- **Pre‑defined gestures** are mapped to system actions (media keys, volume).
- **Custom gestures** can be added via template matching (DTW).  
  Use the **Gestures** tab to import a JSON template.

---

## 🔐 Proximity Security

- The Android app periodically reports RSSI (or distance if Channel Sounding is available).
- Server converts RSSI to distance using a path‑loss model (`TxPower` and `EnvFactor`).
- Two thresholds:
  - `near_threshold` (default 2 m) – unlocks when phone comes closer.
  - `far_threshold` (default 4 m) – locks when phone moves away.
- Hysteresis prevents rapid toggling.
- Lock/unlock commands are OS‑specific:
  - **Windows**: `LockWorkStation`
  - **macOS**: `CGSession -suspend`
  - **Linux**: `loginctl lock-session` (or `gnome-screensaver-command`)

---

## 🤖 Personalization & Training

- **DataCollector** stores movement samples (position, velocity, timestamp) in a circular buffer.
- When buffer reaches `personalization_buffer` (default 2000), it triggers a fine‑tune request to an external Python service (default `http://localhost:5001`).
- The Python trainer must implement `/fine_tune` (POST) and `/health` (GET).
- After training, the new model can be automatically swapped if `auto_swap_model` is true.

---

## ⌨️ System Actions

The server maps recognised gestures and commands to system actions via the `sysaction` package.

| Action | Effect |
|--------|--------|
| `play_pause` | Media play/pause |
| `next_track` | Next track |
| `prev_track` | Previous track |
| `volume_up` | Increase volume |
| `volume_down` | Decrease volume |
| `mute` | Mute |
| `stop` | Stop media |
| `lock_screen` | Lock workstation |
| `browser_back` | Browser back |
| `browser_forward` | Browser forward |

Platform‑specific implementations use:
- **Windows**: `keybd_event`, `mouse_event`
- **Linux**: `xdotool`
- **macOS**: AppleScript / CoreGraphics (stub, ready for full CGO)

---

## 🚀 Cross‑Platform Builds

Use the provided `Makefile` targets:

```bash
make build-windows   # 64‑bit Windows executable
make build-linux     # 64‑bit Linux executable
make build-mac       # 64‑bit macOS executable
```

---

## 🐛 Troubleshooting

### Server won’t start
- Check that the port is not already in use (`lsof -i :8080`).
- Ensure you have write permission to the config directory.

### WebSocket connection refused
- Verify firewall allows incoming connections on port 8080.
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
- Adjust `tx_power` and `env_factor` in the Android app’s calibration.
- On Linux, verify that `loginctl lock-session` works from terminal.

### AI smoother not loading
- The model file must be placed at `models/mouse_smoothing.onnx`.
- Build with `-tags ai` to include real ONNX support.
- Check that ONNX Runtime shared library is installed (`libonnxruntime.so` on Linux).

---

## 🧪 Development

### Project Structure

```
airmouse-go/
├── cmd/airmouse-server/          # Entry point
├── internal/
│   ├── domain/                   # Entities, repository interfaces, services
│   ├── repository/               # Repository implementations
│   ├── handler/                  # WebSocket, HTTP, DTOs
│   ├── infra/                    # Mouse, Bluetooth, USB, logger
│   └── pkg/                      # Utilities (crypto, net, random)
├── go.mod
└── go.sum
```

### Adding a New Gesture Type

1. Extend `internal/domain/entity/gesture.go` – add new `GestureType` constant.
2. Update the gesture service `Recognize()` logic (or add a template in `gesture_repository_impl.go`).
3. Modify the WebSocket handler to map the incoming gesture to an action (optional).
4. (Optional) Add a system action in `internal/infra/sysaction/`.

### Running Tests

```bash
go test -v ./...
```

### Building Without AI Features

If you don’t have ONNX runtime, use the stub build:

```bash
go build -tags noai -o airmouse-server ./cmd/airmouse-server
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

Please ensure your code follows the existing style and passes all tests.

---

## 📄 License

MIT License – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory.  
See [LICENSE](LICENSE) for full text.

---

**Built with Go, Fyne, and ONNX Runtime – ready for production.**