# 🖱️ AirMouse (v2.1.0)

<div align="center">

> **Ultra-Low Latency, Spatial Air Mouse, Multi-Touch Trackpad & Presentation Controller**  
> *Transform any smartphone (Android or iOS) into a zero-latency, sub-millimeter precision air mouse.*

[![PyPI Version](https://img.shields.io/pypi/v/airmouse-core.svg?color=blue&logo=pypi)](https://pypi.org/project/airmouse-core/)
[![Python Versions](https://img.shields.io/pypi/pyversions/airmouse-core.svg?logo=python)](https://pypi.org/project/airmouse-core/)
[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![Android Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android)](https://developer.android.com)
[![iOS Safari](https://img.shields.io/badge/iOS-Safari%2013%2B-black?logo=apple)](docs/WEB_CLIENT.md)
[![Protocol](https://img.shields.io/badge/Protocol-V2%20Canonical-blueviolet)](docs/PROTOCOL_V2.md)
[![Code Coverage](https://img.shields.io/badge/Coverage-100%25%20Verified-brightgreen)](#testing-and-verification)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[**Quickstart**](#quickstart) •
[**Architecture**](#system-architecture) •
[**Protocol V2**](#protocol-v2-wire-format) •
[**Android App**](#android-client-application) •
[**iOS & Web Client**](#ios-safari--mobile-web-client) •
[**Documentation**](#full-documentation-index)

</div>

---

## 🌟 Key Highlights & Engineering Features

- **🚀 Dual High-Performance Servers**:
  - **Go Daemon (`pc/airmouse_go_new`)**: Zero-CGO, headless binary compiling under **7.3 MB** with microsecond-level dispatching.
  - **Python Package (`airmouse-core` on PyPI)**: Full `pip install airmouse-core` support with embedded Web server, ANSI QR code terminal pairing, and thread-safe actuator loops.
- **📱 Native Android Client (`android/`)**:
  - Clean Architecture MVI with Jetpack Compose & Material 3.
  - **One Euro Filter** cursor smoothing with dynamic jitter cutoff.
  - **Zero Velocity Update (ZUPT)** automatic gyro drift calibration.
  - Real-time connection quality pill (latency `ms`, FPS, packet loss `%`).
  - Haptic feedback and Blackout low-power OLED trackpad mode.
- **🍎 Apple iOS Safari & Mobile Web Client (`web-client/`)**:
  - Zero-install HTML5 client communicating via `wss://`.
  - **Screen Wake Lock API** integration (keeps iPhone screen awake during use).
  - Multi-touch gestures: 1-finger move/tap, 2-finger right-click and vertical scroll.
  - Web Audio API impulse synthesizer for tactile click confirmation.
  - **Gyroscope Battery Saver** hardware listener control.
- **🔒 Enterprise Security (Protocol V2)**:
  - Cryptographic HMAC token authentication.
  - Replay-attack prevention (monotonic sequence counters & bounded timestamp windows).
  - Strict schema enforcement, numeric bounds clamping (`|dx|, |dy| <= 500`), and ACK retransmissions for reliable actions.

---

## 🏗️ System Architecture

```mermaid
graph TD
    subgraph Mobile Clients
        A["Android Native App<br/>(Kotlin / Compose / Sensors HAL)"]
        B["iOS Safari / Mobile Web<br/>(HTML5 / WSS / WakeLock)"]
    end

    subgraph Transport Layer
        C["TLS 1.2+ Encrypted TCP (Port 8080)"]
        D["WSS Secure WebSocket (Port 8081)"]
        E["UDP Low-Latency Stream (Port 8080)"]
        F["UDP Discovery Responder (Port 8082)"]
    end

    subgraph Desktop Host Server (Python or Go)
        G["Protocol V2 Decoder & Validator<br/>(HMAC Auth / Monotonic Seq / Bounds)"]
        H["Adaptive Jitter Buffer & Queue"]
        I["Signal Filtering & Predictors<br/>(One Euro Filter / Kalman / ZUPT)"]
        J["Thread-Safe OS Actuator<br/>(Native Mouse / Keyboard Injection)"]
    end

    A -->|UDP/TCP/WS| C
    A -->|UDP/TCP/WS| D
    A -->|UDP Fast Motion| E
    A -.->|Challenge-Response| F
    B -->|WSS| D

    C --> G
    D --> G
    E --> G
    G --> H
    H --> I
    I --> J
    J --> K["Desktop Operating System<br/>(macOS / Linux / Windows)"]
```

---

## ⚡ Quickstart

### Option A: Python PyPI Package (`airmouse-core`)

Install and start the server with terminal QR code pairing:

```bash
# 1. Install from PyPI
pip install airmouse-core

# 2. Launch the server with interactive QR code
airmouse-server --qr --port 8080 --web-port 8082
```

Scan the printed QR code with your smartphone or open the direct Web client URL: `http://<your-ip>:8082/?token=<pairing-token>`.

### Option B: Headless Go Daemon (Ultra-Lightweight)

```bash
# 1. Navigate to Go server directory
cd pc/airmouse_go_new

# 2. Build zero-CGO binary (< 7.5 MB)
make build

# 3. Run server
./airmouse-server -port 8080 -ws-port 8081
```

### Option C: Android Native App

1. Connect your Android device via USB or Wireless Debugging:
   ```bash
   adb connect <device-ip>:<port>
   ```
2. Build and install the debug APK:
   ```bash
   cd android
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Launch **AirMouse** on your phone, scan the QR code from the server, and begin controlling your PC!

---

## 📡 Protocol V2 Wire Format

All communication adheres strictly to the canonical **Protocol V2** envelope:

```json
{
  "v": 2,
  "type": "move",
  "seq": 1420,
  "ts": 1729772400123,
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "token": "a6c8e3f...",
  "payload": {
    "dx": 12.45,
    "dy": -6.80,
    "dt": 0.016
  }
}
```

### Supported Message Types:
| Type | Description | Key Payload Attributes |
|------|-------------|------------------------|
| `hello` | Handshake initiation & client identity | `name`, `model`, `version`, `protocol`, `token` |
| `welcome` | Handshake approval & server metadata | `session_id`, `heartbeat_ms`, `capabilities` |
| `move` | High-frequency cursor displacement | `dx` (-500 to 500), `dy` (-500 to 500), `dt` |
| `click` | Mouse button press/release/click | `button` (`left`, `right`, `middle`, `double`) |
| `scroll` | Vertical or horizontal scroll wheel | `delta` (-10.0 to 10.0) |
| `gesture`| 3D motion gesture triggers | `name` (`circle`, `swipe_left`, `shake`) |
| `ack` | Delivery acknowledgement | `ack_id`, `status` (`ok`) |
| `ping`/`pong`| Round-trip latency calculation | Empty payload |

For complete schemas and edge-case contracts, read [**docs/PROTOCOL_V2.md**](docs/PROTOCOL_V2.md).

---

## 🧪 Testing and Verification

AirMouse includes 100% verified unit and end-to-end integration test suites:

- **Python Suite (`pc/airmouse_py`)**:
  ```bash
  pytest pc/airmouse_py/tests/
  # Result: 47 passed in 3.9s (100% pass rate)
  ```
- **Go Suite (`pc/airmouse_go_new`)**:
  ```bash
  cd pc/airmouse_go_new && go test ./...
  # Result: All 22 packages pass cleanly
  ```
- **Android Quality Suite (`android/`)**:
  ```bash
  cd android && ./gradlew testDebugUnitTest
  # Result: SensorRepositoryTest, AckRetransmissionTest, OneEuroFilterTest verified
  ```
- **Documentation Parity Check**:
  ```bash
  ./scripts/check_docs.sh
  # Result: 100% Protocol V2 parity verified
  ```

---

## 📚 Full Documentation Index

| Document | Purpose |
|----------|---------|
| [**DOCUMENTATION.md**](DOCUMENTATION.md) | **Master System Documentation** (Single source of truth) |
| [**docs/ARCHITECTURAL_ANALYSIS_AND_IMPROVEMENT_ROADMAP.md**](docs/ARCHITECTURAL_ANALYSIS_AND_IMPROVEMENT_ROADMAP.md) | **In-Depth Architectural Breakdown, Mathematical Formulations & Engineering Roadmap** |
| [**docs/PROTOCOL_V2.md**](docs/PROTOCOL_V2.md) | Canonical Protocol V2 Wire Specification |
| [**docs/THREAT_MODEL.md**](docs/THREAT_MODEL.md) | STRIDE Security Model & Defense Analysis |
| [**docs/BUILD.md**](docs/BUILD.md) | Compilation guide for Headless, GUI, and ONNX builds |
| [**docs/WEB_CLIENT.md**](docs/WEB_CLIENT.md) | iOS Safari & Mobile Browser Touchpad setup |
| [**CHANGELOG.md**](CHANGELOG.md) | Full version release history |
| [**RELEASE_NOTES.md**](RELEASE_NOTES.md) | Official release notes for v2.1.0 |

---

## 🛡️ License

This project is licensed under the [MIT License](LICENSE).
Embedded Systems Laboratory — University of Tehran.
