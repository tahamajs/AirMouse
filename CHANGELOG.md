# Changelog

All notable changes to the AirMouse project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0-final] - 2026-10-25

### 🚀 Production Release

#### Added
- **Release APK**: Signed release build with R8 minification and resource shrinking
- **V2 Protocol Handshake**: Full `hello`/`welcome` exchange with HMAC-SHA256 token auth and sequence tracking
- **Comprehensive Mouse Movement Tests**: `ComprehensiveMouseMovementPipelineTest.kt` — 4/4 passing (deadband, intentional move, cardinal directions, One Euro jitter rejection)
- **Network Discovery Empty State**: "No servers found. Make sure Wi-Fi is on." message when scan returns no results
- **Connection Error Snackbar**: User-facing error message on connection failure

#### Fixed
- **UI Color Chaos**: Purged hardcoded `Color(0xFF...)` in screen files; HomeScreen now uses `Success`/`Warning`/`Error`/`Info` semantic constants
- **Floating FAB Overlap**: Orange Calibrate FAB removed from MainScreen Home route
- **Notification Spam**: Conflicting notifications debounced and cancelled properly
- **Calibration Math**: `calculateAxisScale()` normalized to `0.85f..1.15f` range

#### Removed
- 56 legacy dead code files (~8,300 lines)
- `.md` documentation stubs accidentally placed in Java source directories
- Stale test files referencing V1 APIs (`IntegrationTest.kt`, `WebSocketManagerTest.kt`, etc.)
- Registration/login dialog on first launch

#### Testing
- Python: 47/47 tests passing
- Go: All packages build clean
- Android: Unit tests 100% pass rate

---

## [2.0.0-remediated] / [1.0.0] - 2026-10-24

### Added
- **Canonical Master Documentation (`DOCUMENTATION.md`)**: Single source of truth at repository root covering System Architecture, Protocol V2, Build Guides, Security Threat Model, Testing Strategy, and Contributing.
- **Dedicated Build Guide (`docs/BUILD.md`)**: Detailed step-by-step instructions for:
  - Headless (Zero-CGO) profile (`CGO_ENABLED=0 go build -tags noai`)
  - Desktop GUI profile (Fyne & Web Dashboard)
  - ONNX Predictive & ML Enhanced profile (`-tags "ai ml"`)
  - Multi-stage Docker container builds
- **Frozen Protocol V2 Specification (`docs/PROTOCOL_V2.md`)**: Authoritative wire format contract featuring:
  - Canonical wire envelope (`v: 2`, `type`, `seq`, `ts`, `id`, `payload`)
  - Complete catalog of 39 verified message types
  - Bounds checking (`|dx|, |dy| <= 500.0`, `|delta| <= 10.0`, NaN/Inf rejection)
  - Sliding replay protection window (±5000 ms) and monotonic sequence checks
- **Documentation Parity Verifier (`scripts/check_docs.sh`)**: Automated test script validating that every active message type in the Go server codebase matches `docs/PROTOCOL_V2.md` and `DOCUMENTATION.md`.
- **Zero-CGO Hardware Mouse Fallback (`control/mouse/mouse_nocgo.go`)**: Pure Go stub allowing headless zero-CGO compilation on macOS and unsupported platforms without CGO dependencies.
- **Release Documentation (`RELEASE_NOTES.md`)**: Comprehensive release overview and deployment instructions for v1.0.0 / v2.0.0-remediated.

### Changed
- **Consolidated Repository READMEs**: Replaced obsolete and drifting README files across `README.md`, `pc/README.md`, `pc/airmouse_go_new/README.md`, `android/README.md`, `android/network/README.md`, and `README_extra.md` with clear, uniform pointers to `DOCUMENTATION.md`.
- **Fixed Web Asset Embedding**: Removed empty directory `web/client` which caused Go 1.16+ embed failures during static compilation.

### Security
- **Mandatory Transport Encryption**: TLS 1.2+ required across TCP (`tls.Listen`) and WebSocket (`wss://`).
- **HMAC Handshake Authentication**: Enforced cryptographic pairing tokens on connection handshake (`hello`).
- **Input Sanitization**: Rejection of malformed coordinates, `NaN`, and `+/-Infinity` values to prevent denial-of-service in OS event taps.
- **Replay Window Enforcement**: Monotonic `seq` validation and timestamp sliding window checks.

---

## [1.0.0-rc1] - 2025-01-15

### Added
- Multi-protocol Go server engine supporting WebSocket, TCP, and UDP discovery.
- Android Jetpack Compose MVI complete rewrite.
- Madgwick AHRS 9-axis sensor fusion algorithm.
- 2D Kalman filter for motion prediction.
- Bluetooth RSSI fusion proximity auto-lock service.
- Fyne desktop user interface and embedded web dashboard.

---

## [0.1.0] - 2024-11-20

### Added
- Initial experimental Python server prototype (`pc/airmouse_server`).
- Basic Android accelerometer/gyroscope cursor streaming.
- Perfetto trace analysis script (`perfetto_analyzer.py`).
