# AirMouse

> **High-Precision, Low-Latency Spatial Air Mouse & Remote Control System**  
> *Embedded Systems Laboratory — University of Tehran*  
> **Release:** v1.0.0 / Protocol v2.0.0-remediated

[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android)](https://developer.android.com)
[![Protocol](https://img.shields.io/badge/Protocol-V2%20Canonical-blueviolet)](docs/PROTOCOL_V2.md)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

> [!IMPORTANT]
> **Consolidated Documentation**: For complete technical specifications, system architecture diagrams, protocol message catalogs, security threat model, build guides, and testing procedures, please refer directly to the master [**DOCUMENTATION.md**](DOCUMENTATION.md).

---

## Overview

AirMouse turns an Android smartphone into a high-precision wireless air mouse, multi-touch trackpad, presentation clicker, and gesture controller for desktop computers (macOS, Linux, Windows).

- **Mobile Client (`android/`)**: Reads IMU sensors (gyroscope, accelerometer, magnetometer), performs Madgwick AHRS sensor fusion, detects gestures, and streams encrypted Protocol V2 JSON packets over WebSocket/TCP/UDP. Built with Jetpack Compose & Clean Architecture MVI.
- **Desktop Server (`pc/airmouse_go_new/`)**: High-performance Go background daemon supporting Headless (zero-CGO), Desktop GUI (Fyne), and ONNX neural trajectory smoothing profiles. Injects native OS mouse/keyboard actions.
- **Protocol V2 (`docs/PROTOCOL_V2.md`)**: Frozen, canonical protocol specification with HMAC authentication, replay defense, strict schema bounds, and ACK reliability.

---

## Quick Navigation

| Document | Purpose |
|----------|---------|
| [**DOCUMENTATION.md**](DOCUMENTATION.md) | **Master System Documentation** (Architecture, Protocol V2, Security, Testing, Contributing) |
| [**docs/ARCHITECTURAL_ANALYSIS_AND_IMPROVEMENT_ROADMAP.md**](docs/ARCHITECTURAL_ANALYSIS_AND_IMPROVEMENT_ROADMAP.md) | **Deep-Dive Architectural Analysis & Strategic Engineering Roadmap** |
| [**docs/BUILD.md**](docs/BUILD.md) | Detailed build instructions for Headless (zero-CGO), GUI (Fyne), and ONNX builds |
| [**docs/PROTOCOL_V2.md**](docs/PROTOCOL_V2.md) | Canonical Protocol V2 Wire Envelope & Complete Message Catalog |
| [**docs/THREAT_MODEL.md**](docs/THREAT_MODEL.md) | STRIDE Threat Model & Cryptographic Security Controls |
| [**docs/WEB_CLIENT.md**](docs/WEB_CLIENT.md) | iOS Safari & Mobile Browser Touchpad / Orientation Client |
| [**CHANGELOG.md**](CHANGELOG.md) | Version history and remediation logs |
| [**RELEASE_NOTES.md**](RELEASE_NOTES.md) | Official release notes for v1.0.0 / v2.0.0-remediated |

---

## Repository Layout

```text
AirMouse/
├── DOCUMENTATION.md       # MASTER SYSTEM DOCUMENTATION (Single Source of Truth)
├── CHANGELOG.md           # Version changelog
├── RELEASE_NOTES.md       # Release notes for v1.0.0 / v2.0.0-remediated
├── docs/                  # Technical documentation & guides
│   ├── BUILD.md           # Headless, GUI, and ONNX compilation instructions
│   ├── PROTOCOL_V2.md     # Frozen Protocol V2 wire specification
│   └── SENSOR_FUSION_EXPLAINED.md
├── scripts/               # Automation & quality validation scripts
│   └── check_docs.sh      # Protocol parity checker against Go codebase
├── pc/
│   └── airmouse_go_new/   # Canonical Go desktop server
│       ├── cmd/           # Server entrypoint
│       ├── control/       # Mouse controller, Kalman filter, ONNX smoother
│       ├── internal/      # Clean architecture packages (protocol, auth, device)
│       └── web/           # Embedded web dashboard
└── android/               # Android Jetpack Compose client application
    ├── app/               # Main application module
    └── network/           # Networking and message definitions
```

---

## Quick Start

### 1. Build and Run Server (Desktop or Headless)

```bash
cd pc/airmouse_go_new

# Option A: Headless (zero-CGO, ideal for servers & Docker)
CGO_ENABLED=0 go build -tags noai ./cmd/airmouse-server
./airmouse-server --headless

# Option B: Standard Desktop Server (with Web Dashboard)
go run ./cmd/airmouse-server
```

### 2. Verify Protocol Documentation Parity

```bash
./scripts/check_docs.sh
```

### 3. Build Android App

Open the `android/` directory in Android Studio or compile via Gradle:
```bash
cd android
./gradlew assembleDebug
```

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
