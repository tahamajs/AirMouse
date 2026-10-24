# AirMouse Build & Compilation Guide

This document provides step-by-step instructions for compiling and packaging the AirMouse desktop server across all deployment modes: **Headless (Zero-CGO)**, **Desktop GUI (Fyne & Web)**, and **ONNX Predictive / ML Enhanced**.

---

## 1. System Requirements & Prerequisites

| Component | Minimum Version | Recommended | Notes |
|-----------|-----------------|-------------|-------|
| **Go** | 1.22 | 1.23+ | `go version` |
| **C Compiler** | GCC 9+ / Clang 12+ | Apple Clang / GCC 13 | Required for CGO (GUI & ONNX) |
| **Git** | 2.30+ | Latest | For version stamping |
| **Make** | GNU Make 3.81+ | GNU Make 4+ | Optional, wraps build scripts |

---

## 2. Build Targets Overview

AirMouse supports three compilation profiles depending on your target environment:

```
┌──────────────────────────────┬───────────────┬────────────┬─────────────────────────────┐
│ Profile                      │ CGO Required  │ Build Tags │ Primary Target              │
├──────────────────────────────┼───────────────┼────────────┼─────────────────────────────┤
│ 1. Headless (Zero-CGO)       │ CGO_ENABLED=0 │ noai       │ Docker, Cloud, CI/CD, Relays│
│ 2. Desktop GUI (Fyne / Web)  │ CGO_ENABLED=1 │ (default)  │ Workstations, macOS/Win/Lnx │
│ 3. ONNX Predictive & ML      │ CGO_ENABLED=1 │ ai ml      │ Pro users, AI smoothing     │
└──────────────────────────────┴───────────────┴────────────┴─────────────────────────────┘
```

---

## 3. Profile 1: Headless Server (Zero-CGO)

### Purpose
The headless profile creates a statically linked binary with **zero external C library dependencies**. It is optimized for Linux servers, Docker containers, remote gateways, or environments where no graphical display (X11/Wayland/Quartz) is present.

### Compilation Commands

```bash
cd pc/airmouse_go_new

# Download dependencies
go mod download

# Build zero-CGO binary
CGO_ENABLED=0 go build -tags noai -ldflags "-s -w -X main.version=2.0.0" -o bin/airmouse-headless ./cmd/airmouse-server
```

Using Makefile shortcut:
```bash
make quick
```

### Running Headless Mode
When starting the server on a remote or headless machine, pass the `--headless` flag to suppress opening local web browsers:
```bash
./bin/airmouse-headless --headless --config=config.json --log-level=info
```
The server will initialize TCP (8080), WebSocket (8081), and UDP discovery (8082). You can still access the Web Dashboard remotely via `http://<server-ip>:8081`.

---

## 4. Profile 2: Desktop GUI & Web Dashboard (Fyne)

### Purpose
Provides a native desktop interface with system tray integration, multi-tab dashboards, real-time device connection lists, QR code pairing, and automatic browser dashboard launching.

### Platform Prerequisites

#### Linux (Ubuntu / Debian)
```bash
sudo apt-get update
sudo apt-get install -y libgl1-mesa-dev xorg-dev libxcursor-dev libxrandr-dev libxinerama-dev libxi-dev libxxf86vm-dev
```

#### macOS
Install Xcode Command Line Tools:
```bash
xcode-select --install
```

#### Windows
Ensure a GCC toolchain is installed (e.g. MinGW-w64 or MSYS2) and added to your `PATH`.

### Compilation Commands

```bash
cd pc/airmouse_go_new

# Standard desktop build with CGO enabled
CGO_ENABLED=1 go build -ldflags "-X main.version=2.0.0" -o bin/airmouse-server ./cmd/airmouse-server
```

Using Makefile:
```bash
# Build for current host platform
make build

# Cross-platform builds (from Linux host or with cross-toolchains)
make build-linux
make build-windows
make build-mac
make build-mac-arm
```

### Packaging macOS DMG
To build a signed and notarized `.dmg` installer on macOS:
```bash
make dmg-mac
```
The output disk image will be placed in `dist/macos/AirMouseProServer.dmg`.

---

## 5. Profile 3: ONNX Predictive & ML Enhanced

### Purpose
Enables real-time LSTM neural network mouse trajectory smoothing, anti-jitter Kalman filtering, and online model inference via ONNX Runtime.

### Prerequisites: ONNX Runtime Shared Library

The runtime requires the native `onnxruntime` shared library installed on your system or placed in your library path:

- **macOS**: `libonnxruntime.dylib` in `/usr/local/lib` or alongside the binary
  ```bash
  brew install onnxruntime
  ```
- **Linux**: `libonnxruntime.so` in `/usr/lib` or `/usr/local/lib`
  ```bash
  # Download from https://github.com/microsoft/onnxruntime/releases
  sudo cp libonnxruntime.so* /usr/local/lib/
  sudo ldconfig
  ```
- **Windows**: `onnxruntime.dll` in the executable folder or `C:\Windows\System32`.

### Model Placement
Place the trained ONNX model at the expected model path (configurable in `config.json`):
```bash
mkdir -p models
cp /path/to/trained_model.onnx models/mouse_smoothing.onnx
```

### Compilation Command

```bash
cd pc/airmouse_go_new

CGO_ENABLED=1 go build -tags "ai ml" -ldflags "-X main.version=2.0.0" -o bin/airmouse-ai ./cmd/airmouse-server
```

### Verification
Run with debug logging to confirm the ONNX session initialization:
```bash
./bin/airmouse-ai --log-level=debug
```
Look for:
```text
[INFO] ONNX environment initialized successfully
[INFO] AI smoothing model loaded from models/mouse_smoothing.onnx
```

---

## 6. Docker Container Builds

AirMouse includes optimized multi-stage Docker builds:

```bash
cd pc/airmouse_go_new

# Build container
docker build -t airmouse/airmouse-server:latest .

# Run container exposing TCP, WS, and UDP ports
docker run -d \
  --name airmouse \
  -p 8080:8080 \
  -p 8081:8081 \
  -p 8082:8082/udp \
  -v airmouse-data:/var/lib/airmouse \
  airmouse/airmouse-server:latest
```

Or using Docker Compose:
```bash
docker-compose up -d
```

---

## 7. Troubleshooting & OS Permissions

### macOS: Accessibility & Input Monitoring
macOS requires explicit user permissions to post synthetic input events:
1. Open **System Settings** -> **Privacy & Security**.
2. Click **Accessibility** and ensure your terminal emulator or `airmouse-server` is checked.
3. Click **Input Monitoring** and ensure permission is granted.

### Linux: `/dev/uinput` Permissions
If cursor movements do not register under Linux:
```bash
# Add current user to input group
sudo usermod -aG input $USER

# Ensure uinput rule exists
echo 'KERNEL=="uinput", MODE="0660", GROUP="input", OPTIONS+="static_node=uinput"' | sudo tee /etc/udev/rules.d/99-uinput.rules
sudo udevadm control --reload-rules && sudo udevadm trigger
```

---

For architectural details and protocol specifications, consult [DOCUMENTATION.md](../DOCUMENTATION.md) and [docs/PROTOCOL_V2.md](./PROTOCOL_V2.md).
