# AirMouse Go — Open-Source SDK, Library & Desktop Server

> **High-Performance, Zero-CGO Compatible Spatial Control Library & Server in Go**  
> *Embedded Systems Laboratory — University of Tehran*  
> **Release:** v3.0.0 / Protocol v2.0.0

[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![Protocol](https://img.shields.io/badge/Protocol-V2%20Canonical-blueviolet)](../../docs/PROTOCOL_V2.md)
[![Tests](https://img.shields.io/badge/tests-100%25%20passing-brightgreen)]()
[![Zero-CGO](https://img.shields.io/badge/zero--CGO-supported-success)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## Overview

`airmouse-go` is both a production-ready desktop daemon and a modular, importable Go library. It translates high-frequency motion sensor streams (gyroscope, accelerometer, magnetometer), gestures, clicks, and keystrokes from mobile clients into smooth, sub-millisecond cursor motion and spatial actions.

Key features include:
- **Embeddable Server (`pkg/server`)**: Embed AirMouse capabilities directly into external Go applications with customizable lifecycle and motion listener hooks.
- **Standalone Signal Filters (`pkg/filter`)**: Pure Go implementations of the **1€ Filter** (adaptive jitter reduction), **2D Kalman Filter** (state tracking and forward prediction), and **Madgwick AHRS** (6-DOF / 9-DOF orientation quaternion sensor fusion).
- **Hardened Protocol V2 (`pkg/protocol`)**: Strict JSON schema decoding (`DisallowUnknownFields`), sliding-window replay protection, coordinate clamping, and typed payloads.
- **Zero-CGO Pure Go Compilation**: Default headless build produces a standalone, self-contained binary (<15MB) with zero C dependencies.

---

## Installation

### Add as a Go Dependency
To use `airmouse-go` in your Go project:
```bash
go get airmouse-go
```

Or reference by git repository:
```bash
go get github.com/tahamajs/AirMouse/pc/airmouse_go_new
```

### Install CLI Server Binary
```bash
go install ./cmd/airmouse-server
```

---

## Quickstart

### 1. Embedding the Server into a Go Application
Embed the AirMouse server into games, virtual displays, remote desktop applications, or custom robotic controllers:

```go
package main

import (
	"fmt"
	"log"
	"time"

	"airmouse-go/pkg/server"
)

func main() {
	// Initialize with functional options
	srv, err := server.New(
		server.WithTCPPort(8080),
		server.WithWebSocketPort(8081),
		server.WithUDPPort(8082),
		server.WithSensitivity(1.2),
		server.WithVirtualMouse(), // Pure event mode without moving the host OS cursor
	)
	if err != nil {
		log.Fatalf("Init failed: %v", err)
	}

	// Register listener hooks
	srv.OnClientConnected(func(client server.ClientInfo) {
		fmt.Printf("Client connected: %s (%s) from %s\n", client.Name, client.ID, client.IPAddress)
	})

	srv.OnMove(func(dx, dy float64) {
		fmt.Printf("Motion: dx=%.2f, dy=%.2f\n", dx, dy)
	})

	srv.OnClick(func(button string) {
		fmt.Printf("Click: %s\n", button)
	})

	// Start server protocols
	if err := srv.Start(); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
	defer srv.Stop()

	fmt.Println("AirMouse server is listening! Press Ctrl+C to terminate.")
	time.Sleep(30 * time.Second)
}
```

### 2. Standalone Signal Filtering & Sensor Fusion
Use the mathematical filters independently in any signal processing pipeline:

```go
package main

import (
	"fmt"
	"time"

	"airmouse-go/pkg/filter"
)

func main() {
	// 1. One Euro Filter (1D and 2D)
	f1d := filter.NewOneEuroFilter(1.0, 0.05, 1.0)
	smoothed := f1d.Filter(10.5, time.Now())
	fmt.Printf("1€ Filtered value: %.3f\n", smoothed)

	// 2. 2D Constant-Velocity Kalman Filter
	kf := filter.NewKalmanFilter2D(0.02)
	kf.Predict()
	kf.Update(12.0, -8.0)
	predDx, predDy := kf.GetPredictedMovement()
	fmt.Printf("Kalman predicted movement: (%.2f, %.2f)\n", predDx, predDy)

	// 3. Madgwick AHRS Orientation Sensor Fusion
	ahrs := filter.NewMadgwickFilter(0.1)
	// Update with Gyroscope (gx, gy, gz in rad/s) and Accelerometer (ax, ay, az in m/s²)
	ahrs.UpdateIMU(0.0, 0.0, 0.0, 0.0, 0.0, 9.81, 0.01)
	roll, pitch, yaw := ahrs.GetEulerAnglesDegrees()
	fmt.Printf("Attitude: Roll=%.2f°, Pitch=%.2f°, Yaw=%.2f°\n", roll, pitch, yaw)
}
```

### 3. Protocol V2 Encoding & Strict Decoding
```go
package main

import (
	"fmt"
	"airmouse-go/pkg/protocol"
)

func main() {
	// Encode a movement message
	msgBytes, err := protocol.NewMoveMessage(15.2, -8.4, 0.02, 101)
	if err != nil {
		panic(err)
	}

	// Strictly decode envelope and payload (rejects unknown fields & invalid values)
	env, err := protocol.DecodeWireMessageStrictV2(msgBytes)
	if err != nil {
		panic(err)
	}

	movePayload, err := protocol.DecodeMovePayloadV2(env.Payload)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Decoded movement: dx=%.2f, dy=%.2f (seq=%d)\n", movePayload.DX, movePayload.DY, env.Seq)
}
```

---

## Build Profiles & Tags

The codebase supports multiple build configurations depending on your runtime environment and features needed:

| Build Tag | Profile | Description | CGO Required |
|:---|:---|:---|:---:|
| *(none)* | **Default Headless** | Pure Go, statically linked server daemon with TCP, WebSocket, and UDP. | **No** (`CGO_ENABLED=0`) |
| `-tags noai` | **Zero-AI / Safe** | Disables all ONNX runtime bindings; falls back to pure Go 1€ and Kalman filters. | **No** (`CGO_ENABLED=0`) |
| `-tags gui` | **Desktop GUI** | Enables Fyne system tray, desktop status window, and settings dialog. | Yes (`CGO_ENABLED=1`) |
| `-tags "ai ml"` or `-tags onnx` | **ONNX Neural ML** | Links Microsoft ONNX Runtime C library for neural trajectory inference. | Yes (`CGO_ENABLED=1`) |

### Example Build Commands
```bash
# 1. Headless Zero-CGO Server (Recommended default for servers and containers)
CGO_ENABLED=0 go build -trimpath -ldflags "-s -w" -o airmouse-server ./cmd/airmouse-server

# 2. Build with safe No-AI fallback tag
CGO_ENABLED=0 go build -tags noai -o airmouse-server-noai ./cmd/airmouse-server

# 3. Build Desktop GUI version
CGO_ENABLED=1 go build -tags gui -o airmouse-server-gui ./cmd/airmouse-server

# 4. Build Full GUI + ONNX Predictive ML
CGO_ENABLED=1 go build -tags "gui onnx" -o airmouse-server-full ./cmd/airmouse-server
```

---

## Package Architecture

```text
pc/airmouse_go_new/
├── cmd/
│   ├── airmouse-server/        # Main executable entrypoint
│   └── integration-test/       # Multi-client throughput and stress tests
├── pkg/                        # Exported public Go library packages
│   ├── filter/                 # OneEuroFilter, KalmanFilter2D, MadgwickAHRS
│   ├── protocol/               # Protocol V2 wire envelope, strict codecs, replay detection
│   └── server/                 # Embeddable AirMouse server and event listener hooks
├── examples/                   # Runnable example applications
│   ├── embed_server/           # Embedded AirMouse server with custom callbacks
│   └── signal_filter/          # Standalone 1€, Kalman, and Madgwick filter demo
├── control/                    # Native mouse drivers & trajectory prediction
├── internal/                   # Modular internal implementation packages
│   ├── auth/                   # HMAC-SHA256 authentication manager
│   ├── config/                 # Dynamic JSON configuration manager
│   ├── device/                 # Device registry, pairing, and lifecycle events
│   ├── logger/                 # Thread-safe structured logging
│   ├── protocol/               # Internal protocol handlers (TCP, WS, UDP, USB, BLE)
│   ├── proximity/              # Bluetooth RSSI fusion and auto-lock
│   └── sysaction/              # Native OS commands (media keys, lock screen, typing)
├── web/                        # Embedded web dashboard frontend
├── go.mod                      # Module definition
└── Makefile                    # Cross-platform build automation
```

---

## Runnable Examples

The `examples/` directory contains complete, runnable programs demonstrating common integration patterns:

1. **`examples/embed_server/main.go`**:
   Demonstrates how to embed the AirMouse server, configure ports, register callbacks for client connections, motion, clicks, and scrolls, and perform graceful shutdown upon receiving OS signals.
   ```bash
   go run ./examples/embed_server
   ```

2. **`examples/signal_filter/main.go`**:
   Demonstrates standalone usage of the 1€ adaptive filter for low-speed jitter elimination, 2D Kalman filter for velocity prediction, and Madgwick filter for attitude orientation.
   ```bash
   go run ./examples/signal_filter
   ```

---

## Quality & Validation

Every package in `airmouse-go` is verified against race conditions, static analysis, and unit test suites:

```bash
# Run all package tests
go test -v ./...

# Run race detector
go test -race ./...

# Run go vet static analysis
go vet ./...

# Run filter benchmarks
go test -bench=. ./pkg/filter
```

---

## Contributing & License

Contributions are welcome! Please ensure all pull requests pass `go test ./...` and `go vet ./...`.

Licensed under the [MIT License](LICENSE).
