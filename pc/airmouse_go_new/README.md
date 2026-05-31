# Air Mouse Go Server – Complete Documentation

> **Professional‑grade, AI‑powered remote control server** for the Air Mouse Android app.  
> Written in Go, with multi‑protocol support, real‑time gesture recognition, predictive smoothing, and proximity‑aware security.

[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Protocol Specification](#-protocol-specification)
- [API Endpoints](#-api-endpoints)
- [Development](#-development)
- [License](#-license)

---

## 🎯 Overview

**Air Mouse Go Server** is the desktop counterpart of the Air Mouse Android app. It receives movement, click, and gesture commands from one or more mobile devices over WebSocket (or TCP/UDP) and translates them into native mouse and keyboard actions on the host computer. The server is built with **Clean Architecture**, making it modular, testable, and easy to extend.

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

- **Domain** (internal/domain) – pure business logic, no external dependencies.
- **Repository** (internal/repository) – concrete implementations of repository interfaces.
- **Handler** (internal/handler) – WebSocket and HTTP endpoints, DTOs, message routing.
- **Infra** (internal/infra) – platform‑specific mouse control, Bluetooth, USB, logging.

All dependencies point inward. The `cmd/airmouse-server/main.go` wires everything together using **constructor injection**.

---

## 🚀 Quick Start

### Prerequisites
- Go 1.23+
- For AI smoothing: ONNX Runtime library (optional)
- For Linux mouse input: `/dev/uinput` writable (or install `xdotool`)
- For Bluetooth: BlueZ (Linux), CoreBluetooth (macOS)

### Build & Run

```bash
git clone https://github.com/yourusername/airmouse-go.git
cd airmouse-go
go mod tidy
go build -o airmouse-server ./cmd/airmouse-server
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

---

## ⚙️ Configuration

The server reads `~/.config/airmouse/config.json` (Linux/macOS) or `%APPDATA%\airmouse\config.json` (Windows).  
Example:

```json
{
  "port": "8080",
  "websocket_port": 8081,
  "sensitivity": 0.5,
  "predictive_blend_factor": 0.6,
  "gesture_confidence_threshold": 0.7,
  "enable_ai_smoothing": false,
  "ai_model_path": "models/mouse_smoothing.onnx",
  "enable_predictive": true,
  "auth_enabled": false,
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

## 🔌 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/ws` | WebSocket | Main WebSocket endpoint (pass `?token=xxx` if auth enabled) |
| `/health` | GET | Health check – returns `{"status":"ok"}` |
| `/metrics` | GET | Prometheus‑style metrics (clients, messages, etc.) |
| `/qr` | GET | Returns a QR code PNG for pairing (if `enable_pairing_ui` is true) |

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

### Cross‑compilation

```bash
# Windows
GOOS=windows GOARCH=amd64 go build -o airmouse-server.exe ./cmd/airmouse-server

# macOS (Intel)
GOOS=darwin GOARCH=amd64 go build -o airmouse-server ./cmd/airmouse-server

# Linux ARM (Raspberry Pi)
GOOS=linux GOARCH=arm64 go build -o airmouse-server ./cmd/airmouse-server
```

---

## 📄 License

MIT License – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory.  
See [LICENSE](LICENSE) for full text.

---

**Built with ❤️ and Go – ready to fly.**