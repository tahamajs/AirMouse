# Release Notes — AirMouse v2.1.0

**Release Tag:** `v2.1.0`  
**Date:** 2026-10-24  
**Release Type:** Major Architecture Overhaul & Production Release  
**PyPI Package:** [`airmouse-core` v2.1.0](https://pypi.org/project/airmouse-core/2.1.0/)

---

## 🚀 Overview

AirMouse **v2.1.0** represents a comprehensive, production-grade overhaul of the entire spatial air mouse and remote control ecosystem. It introduces the hardened **Protocol V2** wire specification, published PyPI library with embedded web server, zero-CGO lightweight Go server, enhanced Android client with Material 3 UI and One Euro Filter, and production-hardened iOS Safari web client.

---

## 🌟 Key New Features & Improvements

### 1. Python PyPI Standalone Library (`airmouse-core` v2.1.0)
- **PyPI Published**: Direct install via `pip install airmouse-core`.
- **Embedded Web Client**: Built-in HTTP server (`port 8082`) serving the zero-install mobile web client with instant pairing links.
- **Interactive CLI**: Terminal ANSI QR code rendering for one-second pairing with mobile devices.
- **Thread Safety**: Network coroutines strictly decoupled from native mouse actuation threads via `queue.Queue`.
- **Complete Test Coverage**: 47 unit and integration tests passing with 100% success rate.

### 2. Go Desktop Server (`pc/airmouse_go_new`)
- **Zero-CGO Headless Build**: Standalone executable compiling under **7.3 MB** without external C dependencies.
- **Adaptive Jitter Buffer**: Dynamically adjusts queue depth based on network variance to eliminate stutter.
- **Predictive Filtering**: Dual-engine smoothing incorporating One Euro Filter and Kalman filter.
- **Discovery**: UDP broadcast discovery with 3x retry and challenge-response deduplication.

### 3. Native Android Client (`android/`)
- **Protocol V2 Integration**: Monotonic sequence counters, UTC timestamp windows, and nested payload schemas.
- **One Euro Filter**: Low-pass adaptive filtering with dynamic cutoff eliminating trembling at rest while maintaining snappy responsiveness during fast flicks.
- **ZUPT Auto-Calibration**: Automatic zero-velocity updates to suppress gyroscope bias drift.
- **UI/UX Enhancements**:
  - Floating connection status pill showing protocol, RTT ping (ms), and packet loss percentage.
  - Interactive multi-step calibration guidance with haptic vibration feedback.
  - Blackout OLED battery saver mode with tactile click vibrations.

### 4. iOS Safari & Mobile Web Client (`web-client/`)
- **Screen Wake Lock API**: Prevents the iPhone display from sleeping during active touchpad sessions.
- **Certificate Warning Helper**: In-app guidance for accepting self-signed ECDSA certificates in iOS Safari.
- **Multi-Touch Gestures**: 1-finger move/tap, 2-finger right click and vertical scrolling.
- **Haptic Synthesis**: Low-latency Web Audio API impulse synthesizer delivering tactile click confirmation on WebKit.
- **Gyroscope Battery Saver**: Hardware listener detach toggle to preserve device battery.

---

## 🔒 Security & Protocol V2 Compliance

- **HMAC Authentication**: Mandatory per-session token validation for all incoming TCP and WebSocket connections.
- **Replay Protection**: Rejects out-of-order packets via strict sequence monotonicity and +/-5000ms timestamp bounding.
- **Schema Validation**: Strict field bounds checking (`|dx|, |dy| <= 500`, `|delta| <= 10.0`).
- **Cryptographic Guardrails**: Self-signed ECDSA P-256 certificates with TLS 1.2+ minimum negotiation.

---

## 📦 Verification Matrix

| Component | Test Suite | Pass Rate |
|-----------|------------|-----------|
| Python Server & Core | Pytest (47 tests) | **100%** |
| Go Server | `go test ./...` (22 packages) | **100%** |
| Protocol Parity | `check_docs.sh` | **100%** |
| Git History Health | Size < 20MB, No bloat | **Verified** |
