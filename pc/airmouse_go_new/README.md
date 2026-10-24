# AirMouse Desktop Server (Go Pro Server)

> **Cross-Platform, Multi-Protocol, AI-Powered Remote Control Daemon**  
> *Embedded Systems Laboratory — University of Tehran*  
> **Release:** v1.0.0 / Protocol v2.0.0-remediated

[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![Protocol](https://img.shields.io/badge/Protocol-V2%20Canonical-blueviolet)](../../docs/PROTOCOL_V2.md)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

> [!IMPORTANT]
> **Consolidated Documentation**: For complete technical specifications, clean architecture package layout, protocol message catalogs, security threat model, build guides, and testing procedures, please refer directly to the master [**DOCUMENTATION.md**](../../DOCUMENTATION.md) and [**docs/BUILD.md**](../../docs/BUILD.md).

---

## Overview

The AirMouse Desktop Server is the host component of the AirMouse system. It receives motion data, clicks, keystrokes, and gestures from the Android mobile app and translates them into native operating system input events.

### Key Capabilities
- **Multi-Protocol Engine**: WebSocket (port 8081), Raw TCP (port 8080), UDP Discovery (port 8082), Bluetooth HID proxy, and USB serial.
- **Three Build Profiles**:
  1. **Headless (Zero-CGO)**: Statically linked, pure Go, zero runtime C dependencies.
  2. **Desktop GUI (Fyne)**: Cross-platform desktop interface with system tray and embedded Web Dashboard.
  3. **ONNX Predictive / ML**: Real-time neural network mouse trajectory smoothing and 2D Kalman filter lag compensation.
- **Proximity-Aware Security**: Auto-locks the desktop when the phone is carried away based on Bluetooth RSSI fusion.
- **Contextual Auto-Pause**: Pauses motion input when the phone is set down flat on a surface.

---

## Quick Build & Run

### Headless (Zero-CGO)
```bash
CGO_ENABLED=0 go build -tags noai -ldflags "-s -w -X main.version=2.0.0" -o airmouse-server-headless ./cmd/airmouse-server
./airmouse-server-headless --headless --config=config.json
```

### Desktop Server (with Web Dashboard)
```bash
make build
./airmouse-server
```

### Run Tests with Concurrency Race Detection
```bash
go test -v -race -timeout 30s ./...
```

For detailed compilation, cross-compilation, and ONNX library linking instructions, see [**docs/BUILD.md**](../../docs/BUILD.md).

---

## Directory Layout

```text
pc/airmouse_go_new/
├── cmd/airmouse-server/    # Main executable entrypoint
├── control/                # OS mouse control, Kalman filter, ONNX smoother
│   ├── mouse/              # Platform mouse backends (Darwin, Linux, Windows, No-CGO)
│   └── predict/            # Trajectory prediction & ONNX model inference
├── internal/               # Clean architecture internal packages
│   ├── auth/               # HMAC-SHA256 authentication manager
│   ├── config/             # JSON configuration manager
│   ├── device/             # Device registry and pairing state
│   ├── logger/             # Structured logging framework
│   ├── protocol/           # Protocol V2 codecs (TCP, WS, UDP, USB, BLE)
│   ├── proximity/          # Bluetooth RSSI fusion and auto-lock
│   └── sysaction/          # OS commands (screen lock, media keys)
├── web/                    # Embedded single-page Web Dashboard
└── Makefile                # Build automation
```

---

## Canonical Documentation Pointers

- **Master System Documentation**: [DOCUMENTATION.md](../../DOCUMENTATION.md)
- **Compilation Guide**: [docs/BUILD.md](../../docs/BUILD.md)
- **Protocol V2 Contract**: [docs/PROTOCOL_V2.md](../../docs/PROTOCOL_V2.md)
- **Quality & Parity Checker**: [scripts/check_docs.sh](../../scripts/check_docs.sh)
