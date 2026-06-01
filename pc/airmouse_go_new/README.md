# Air Mouse Pro Server – Complete Documentation

> **Cross‑platform, AI‑powered remote control server** for the Air Mouse Android app.  
> Written in Go, supports multiple protocols, AI smoothing, gesture recognition, proximity lock/unlock, and a modern desktop GUI.

---

## Table of Contents

- [Air Mouse Pro Server – Complete Documentation](#air-mouse-pro-server--complete-documentation)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Features](#features)
    - [Connectivity](#connectivity)
    - [Mouse Control](#mouse-control)
    - [Gesture Recognition](#gesture-recognition)
    - [Proximity Lock/Unlock](#proximity-lockunlock)
    - [Personalisation](#personalisation)
    - [GUI (Fyne)](#gui-fyne)
  - [Architecture](#architecture)
  - [Installation](#installation)
    - [Prerequisites](#prerequisites)
    - [Build from source](#build-from-source)
    - [Run](#run)
    - [Cross‑compile](#crosscompile)
  - [Configuration](#configuration)
  - [Protocol Specification](#protocol-specification)
    - [Transport](#transport)
    - [Client → Server Messages](#client--server-messages)
    - [Server → Client Messages](#server--client-messages)
  - [Command‑Line Flags](#commandline-flags)
  - [GUI Tabs](#gui-tabs)
  - [AI \& Predictive Features](#ai--predictive-features)
    - [Kalman Predictor](#kalman-predictor)
    - [AI Smoother (ONNX)](#ai-smoother-onnx)
    - [Gesture Recognition](#gesture-recognition-1)
  - [Proximity Security](#proximity-security)
  - [Personalization \& Training](#personalization--training)
  - [System Actions](#system-actions)
  - [Cross‑Platform Builds](#crossplatform-builds)
  - [Troubleshooting](#troubleshooting)
    - [Server won’t start](#server-wont-start)
    - [WebSocket connection refused](#websocket-connection-refused)
    - [Mouse movement is jerky](#mouse-movement-is-jerky)
    - [Gesture not recognised](#gesture-not-recognised)
    - [Proximity lock doesn’t work](#proximity-lock-doesnt-work)
    - [AI smoother not loading](#ai-smoother-not-loading)
  - [License](#license)

---

## Overview

Air Mouse Pro Server turns your Android phone into a **smart remote control** for your computer.  
It receives movement, click, and gesture commands over WebSocket/TCP/UDP/Bluetooth/USB, translates them into native mouse/keyboard actions, and provides a rich desktop interface for monitoring and configuration.

Key highlights:
- **Zero‑configuration pairing** via QR code
- **Real‑time cursor control** with low latency (Kalman prediction + jitter buffer)
- **AI‑powered smoothing** (ONNX runtime) for natural‑looking movement
- **Gesture recognition** (swipes, circles, thumbs‑up) with custom templates
- **Proximity‑aware lock/unlock** using Bluetooth RSSI (or Channel Sounding)
- **User personalisation** – collect movement data and fine‑tune models
- **Cross‑platform** – Windows, macOS, Linux
- **Beautiful GUI** – 15+ themes, live stats, device list, log viewer

---

## Features

### Connectivity
| Protocol | Port | Use |
|----------|------|-----|
| WebSocket | 8080 (default) | Primary, low‑overhead |
| TCP | 8080 | Fallback for plain TCP clients |
| UDP discovery | 8082 | Auto‑detect server on LAN |
| Bluetooth LE | – | HID proxy (stub, ready for extension) |
| USB serial | – | Wired connection (Linux/macOS) |

### Mouse Control
- Sensitivity (0.2 – 2.0)
- Exponential moving average smoothing
- Acceleration curve (configurable)
- Deadband (ignores very small movements)
- **Kalman filter** for predictive movement (hides network jitter)
- **AI smoother** (ONNX LSTM model) – human‑like trajectory

### Gesture Recognition
- Pre‑defined: `LeftSwipe`, `RightSwipe`, `CircleCW`, `CircleCCW`, `ThumbsUp`
- Custom gestures via template matching (DTW + particle filter)
- On‑device classification (Android) or server‑side (ONNX)
- Configurable confidence threshold

### Proximity Lock/Unlock
- Estimates distance using BLE RSSI (path loss model)
- Auto‑locks screen when phone moves beyond `far_threshold` (default 4 m)
- Auto‑unlocks when returns within `near_threshold` (default 2 m)
- Works on Windows, Linux, macOS (requires `loginctl`, `gnome-screensaver`, etc.)

### Personalisation
- **Data collector** – records user movement (position + velocity)
- **Online training** – triggers Python fine‑tuning service (optional)
- **Auto‑swap model** – loads improved model without restart

### GUI (Fyne)
- **Dashboard** – start/stop server, live click stats, uptime, AI status
- **Devices** – list of connected clients (name, type, connection time)
- **Network** – IP selection, QR code for pairing, port configuration
- **Gestures** – manage gesture templates, train new ones
- **Proximity** – enable/disable lock/unlock, set thresholds, calibrate
- **Settings** – sensitivity, theme, smoothing, acceleration, AI, personalisation
- **Logs** – real‑time log viewer with filter (info/warn/error) and export
- **Status bar** – CPU, memory, goroutines, uptime

---

## Architecture

The server follows **Clean Architecture** (onion) with four layers:

```
┌─────────────────────────────────────────────────────────┐
│  Delivery Layer (WebSocket/HTTP handlers, DTOs)        │
├─────────────────────────────────────────────────────────┤
│  Use Case Layer (domain services – mouse, gesture, etc.)│
├─────────────────────────────────────────────────────────┤
│  Repository Layer (interfaces for data access)         │
├─────────────────────────────────────────────────────────┤
│  Infrastructure Layer (mouse control, Bluetooth, USB)  │
└─────────────────────────────────────────────────────────┘
```

- **Domain** (`internal/domain`) – pure business logic, no external dependencies.
- **Repository** (`internal/repository`) – implementations of repository interfaces (in‑memory).
- **Handler** (`internal/handler`) – WebSocket hub, HTTP routes, DTOs.
- **Infra** (`internal/infra`) – platform‑specific mouse control, Bluetooth stubs, logger.
- **UI** (`internal/ui`) – Fyne desktop interface.

All dependencies are injected in `cmd/airmouse-server/main.go`.

---

## Installation

### Prerequisites
- Go 1.23+
- For AI smoothing: ONNX Runtime shared library (auto‑fetched by go module)
- For Linux mouse control: `/dev/uinput` writable or `xdotool`
- For Bluetooth: BlueZ (Linux), CoreBluetooth (macOS), Windows Bluetooth stack
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

The GUI will appear. Use the **Network** tab to generate a QR code and pair your Android app.

### Cross‑compile

```bash
make build-windows   # airmouse-server.exe
make build-linux     # airmouse-server-linux
make build-mac       # airmouse-server-mac
```

---

## Configuration

Settings are stored in `~/.config/airmouse/config.json` (Linux/macOS) or `%APPDATA%\airmouse\config.json` (Windows).

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

All options can be changed via the **Settings** tab; changes are saved automatically.

---

## Protocol Specification

### Transport
- **WebSocket** endpoint: `ws://<server-ip>:8080/ws`
- **Message format**: JSON line (`\n` terminated)

### Client → Server Messages

| Type | Payload Example | Description |
|------|----------------|-------------|
| `move` | `{"dx":1.5,"dy":2.0}` | Relative movement |
| `click` | `{"button":"left"}` | Left/right click |
| `doubleclick` | `{}` | Double left click |
| `rightclick` | `{}` | Right click |
| `scroll` | `{"delta":1}` | Wheel scroll (+ = up) |
| `hello` | `{"name":"MyPhone","version":"3.0"}` | Identify device |
| `ping` | `{}` | Keep‑alive |
| `gesture` | `{"gesture":"ThumbsUp","confidence":0.92}` | Recognised gesture |
| `proximity` | `{"device_id":"...","is_near":true,"distance":1.23}` | Distance update |
| `control` | `{"command":"pause_movement"}` | Pause/resume cursor |

### Server → Client Messages

| Type | Payload | Description |
|------|---------|-------------|
| `welcome` | `{"server":"AirMouse","version":"3.0"}` | After `hello` |
| `ping` | `{}` | Heartbeat |
| `pong` | `{}` | Response to ping |
| `ack` | `{"id":"..."}` | Acknowledgment (if request included ID) |

---

## Command‑Line Flags

Not yet implemented; all configuration is via `config.json` or GUI.

---

## GUI Tabs

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

## AI & Predictive Features

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

## Proximity Security

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

## Personalization & Training

- **DataCollector** stores movement samples (position, velocity, timestamp) in a circular buffer.
- When buffer reaches `personalization_buffer` (default 2000), it triggers a fine‑tune request to an external Python service (default `http://localhost:5001`).
- The Python trainer must implement `/fine_tune` (POST) and `/health` (GET).
- After training, the new model can be automatically swapped if `auto_swap_model` is true.

---

## System Actions

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

## Cross‑Platform Builds

Use the provided `Makefile` targets:

```bash
make build-windows   # 64‑bit Windows executable
make build-linux     # 64‑bit Linux executable
make build-mac       # 64‑bit macOS executable
```

---

## Troubleshooting

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

## License

MIT License – Copyright (c) 2025 University of Tehran, Embedded Systems Laboratory.  
See [LICENSE](LICENSE) for full text.

---

**Built with Go, Fyne, and ONNX Runtime** – ready for production.