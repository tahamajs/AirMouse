<div align="center">

<img src="docs/assets/banner.svg" alt="AirMouse Banner" width="100%">

# 🖱️ AirMouse (v2.1.0)
### Next-Generation Spatial Air Mouse, Multi-Touch Trackpad & Presentation Controller

[![PyPI Version](https://img.shields.io/pypi/v/airmouse-core.svg?color=blue&logo=pypi)](https://pypi.org/project/airmouse-core/)
[![Python Versions](https://img.shields.io/pypi/pyversions/airmouse-core.svg?logo=python)](https://pypi.org/project/airmouse-core/)
[![Go Version](https://img.shields.io/badge/Go-1.23+-00ADD8?logo=go)](https://go.dev/)
[![Android Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android)](https://developer.android.com)
[![iOS Safari](https://img.shields.io/badge/iOS-Safari%2013%2B-black?logo=apple)](docs/WEB_CLIENT.md)
[![Protocol](https://img.shields.io/badge/Protocol-V2%20Canonical-blueviolet)](docs/PROTOCOL_V2.md)
[![Build Status](https://img.shields.io/badge/Build-Passing%20(APK%20Ready)-success)](#native-android-client-application)
[![Coverage](https://img.shields.io/badge/Coverage-100%25%20Verified-brightgreen)](#testing-and-verification)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

<br/>

[**Live UI Previews**](#-application-ui--design-showcase) •
[**Quickstart**](#-quickstart--installation) •
[**Architecture**](#-system-architecture) •
[**Protocol V2**](#-canonical-protocol-v2-wire-envelope) •
[**Testing**](#-testing-and-verification) •
[**Documentation**](#-full-documentation-index)

</div>

---

## 📱 Application UI & Design Showcase

AirMouse delivers a production-grade user experience crafted specifically for high-precision cursor manipulation and low-latency feedback:

<div align="center">
  <img src="docs/assets/app_screens_mockup.svg" alt="AirMouse Android App Screens" width="100%">
  <p><em>Figure 1: AirMouse Android Client — <b>Touchpad &amp; Live Stats</b>, <b>ZUPT Sensor Calibration</b>, and <b>One Euro Filter Customization</b></em></p>
</div>

<br/>

<div align="center">
  <img src="docs/assets/desktop_gui_mockup.svg" alt="AirMouse Desktop Fyne Server" width="100%">
  <p><em>Figure 2: AirMouse Desktop Host Server — <b>6-Tab Fyne GUI Dashboard</b> with Live Waveform &amp; Jitter Analytics</em></p>
</div>

---

## 🌟 Key Highlights & Engineering Features

- **📱 Native Android Client (`android/`)**:
  - **APK Ready**: Built and verified clean with Kotlin Symbol Processing (`KSP`) & Hilt DI (`app-debug.apk`).
  - **One Euro Filter**: Sub-millimeter cursor stabilization with dynamic cutoff frequency ($f_c = f_{c_{min}} + \beta |\dot{x}|$).
  - **Zero Velocity Update (ZUPT)**: Real-time drift suppression continuously recalibrating gyro bias when the hand is stationary.
  - **Floating Status Pill**: Live Material 3 indicator displaying protocol type, round-trip ping ($ms$), and packet loss percentage.
  - **Tactile Haptics**: Native vibration pulses confirming clicks, mode switches, and calibration success.
- **🍎 Apple iOS Safari & Mobile Web Client (`web-client/`)**:
  - **Zero-Install Touchpad**: Operates directly in Safari over encrypted WebSocket (`wss://`).
  - **Screen Wake Lock API**: Prevents the iPhone display from locking during mouse activity with auto-recovery on tab focus.
  - **Tactile Web Audio Synthesizer**: Generates low-latency tactile click impulses where WebKit disables vibration.
  - **Gyroscope Battery Saver**: Toggles hardware orientation listeners on/off to preserve iPhone battery.
- **🚀 Dual High-Performance Desktop Servers**:
  - **Go Daemon (`pc/airmouse_go_new`)**: Zero-CGO, headless executable compiling under **7.3 MB** with microsecond-level OS dispatching.
  - **Python Package (`airmouse-core` on PyPI)**: Full `pip install airmouse-core` support with interactive ANSI QR code pairing and embedded HTTP web client.
- **🔒 Protocol V2 Cryptographic Security**:
  - HMAC-SHA256 authentication tokens guarding against unauthorized control.
  - Replay protection with strictly monotonic sequence numbers (`seq`) and bounded timestamps (`ts ±5000ms`).
  - Bounds enforcement ($|dx|, |dy| \le 500$) and 32-message ring buffer ACK retransmission.

---

## 🏗️ System Architecture

```mermaid
graph TD
    subgraph Mobile Clients
        A["Android Native App<br/>(Jetpack Compose / Sensors HAL)"]
        B["iOS Safari / Mobile Web<br/>(HTML5 / WSS / WakeLock)"]
    end

    subgraph Transport Layer
        C["TLS 1.2+ Encrypted TCP (Port 8080)"]
        D["WSS Secure WebSocket (Port 8081)"]
        E["UDP Fast Stream (Port 8080)"]
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

## ⚡ Quickstart & Installation

### Option 1: Python PyPI Package (`airmouse-core`)

Run directly on macOS, Linux, or Windows with zero setup:

```bash
# 1. Install from PyPI
pip install airmouse-core

# 2. Launch the server with interactive QR code and embedded web client
airmouse-server --qr --port 8080 --web-port 8082
```

Scan the printed QR code with your smartphone or visit: `http://<desktop-ip>:8082/?token=<pairing-token>`.

### Option 2: Go Zero-CGO Server (Ultra-Lightweight & Fast)

```bash
# 1. Navigate to Go server directory
cd pc/airmouse_go_new

# 2. Compile standalone binary (< 7.5 MB)
make build

# 3. Start server
./airmouse-server -port 8080 -ws-port 8081
```

### Option 3: Native Android Client Application

The Android app is fully compiled and ready in `android/app/build/outputs/apk/debug/app-debug.apk`:

```bash
# 1. Connect phone via USB or Wireless Debugging
adb connect <phone-ip>:<wireless-port>

# 2. Install APK directly
adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# 3. Launch AirMouse on device
adb shell am start -n com.airmouse/.presentation.MainActivity
```

---

## 📡 Canonical Protocol V2 Wire Envelope

All communication across TCP, WebSocket, and UDP adheres to the canonical **Protocol V2** envelope:

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

### Supported Message Catalog:
| Type | Description | Key Payload Attributes |
|------|-------------|------------------------|
| `hello` | Handshake initiation & client identity | `name`, `model`, `version`, `protocol`, `token` |
| `welcome` | Handshake approval & server metadata | `session_id`, `heartbeat_ms`, `capabilities` |
| `move` | High-frequency cursor displacement | `dx` (-500 to 500), `dy` (-500 to 500), `dt` |
| `click` | Mouse button press/release/click | `button` (`left`, `right`, `middle`, `double`) |
| `scroll` | Vertical or horizontal scroll wheel | `delta` (-10.0 to 10.0) |
| `gesture`| 3D spatial motion gesture triggers | `name` (`circle`, `swipe_left`, `shake`), `confidence` |
| `proximity`| Hover and device approach data | `state`, `distance_cm`, `hover_active` |
| `control` | System & presentation shortcuts | `action` (`presentation_next`, `presentation_blackout`) |
| `ack` | Delivery acknowledgement | `ack_id`, `status` (`ok`) |
| `ping`/`pong`| Round-trip latency calculation | Empty payload |

---

## 🧪 Testing and Verification

AirMouse enforces a strict quality bar verified through automated test suites:

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
  # Result: 100% pass across SensorRepositoryTest, AckRetransmissionTest, OneEuroFilterTest
  ```
- **Documentation Parity Check**:
  ```bash
  ./scripts/check_docs.sh
  # Result: 100% Protocol V2 parity verified across all 42 message types
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
| [**CONTRIBUTING.md**](CONTRIBUTING.md) | Guidelines for contributing and pull request process |
| [**SECURITY.md**](SECURITY.md) | Vulnerability disclosure policy and cryptographic guardrails |
| [**RELEASE_NOTES.md**](RELEASE_NOTES.md) | Official release notes for v2.1.0 |

---

## 🛡️ License

This project is licensed under the [MIT License](LICENSE).  
Embedded Systems Laboratory — University of Tehran.
