# AirMouse v1.0.0 Release Notes (Protocol v2.0.0-remediated)

**Release Date:** October 24, 2026  
**Status:** Official Production Release  
**Canonical Documentation:** [DOCUMENTATION.md](DOCUMENTATION.md)  
**Protocol Contract:** [docs/PROTOCOL_V2.md](docs/PROTOCOL_V2.md)

---

## Executive Summary

The AirMouse team is proud to announce the **v1.0.0 Production Release**, accompanied by the **Protocol v2.0.0-remediated** specification. This release consolidates the entire repository around a single canonical architecture, establishes strict cryptographic and input sanitization boundaries, introduces zero-CGO headless compilation for containerized server deployments, and eliminates all historical documentation drift.

---

## Key Highlights

### 1. Canonical Master Documentation (`DOCUMENTATION.md`)
All disparate and drifting guides across the project have been synthesized into a unified, authoritative **`DOCUMENTATION.md`** at the repository root. It provides exhaustive coverage of:
- End-to-end system topology and Mermaid dataflow diagrams
- Clean Architecture layered models for both Go server and Android client
- Protocol V2 specification with wire envelopes and sequence charts
- Security threat models under STRIDE with specific countermeasures
- Testing strategies including race-detector unit testing, sensor fusion convergence, and Perfetto tracing
- Contribution standards and PR checklists

### 2. Protocol V2 Frozen Specification (`docs/PROTOCOL_V2.md`)
Protocol V2 is now officially **frozen** as the canonical contract between mobile devices and host computers:
- **Strict Wire Envelope**: Enforces `{"v": 2, "type": "...", "seq": ..., "ts": ..., "payload": { ... }}` across all streaming connections.
- **39 Active Message Types**: Complete catalog spanning motion, mouse buttons, scrolling, keystrokes, gestures, orientation, calibration, presentation, media controls, proximity telemetry, and system controls.
- **Reliability Semantics**: Clear differentiation between high-frequency motion streams and ACK-acknowledged critical actions (`click`, `scroll`, `control`).

### 3. Three-Tier Build Architecture (`docs/BUILD.md`)
The Go desktop server (`pc/airmouse_go_new`) now formally supports three distinct build profiles:
1. **Headless (Zero-CGO)**: `CGO_ENABLED=0 go build -tags noai` produces a statically linked binary with zero C library dependencies, ideal for Docker containers and remote server gateways.
2. **Desktop GUI (Fyne)**: Cross-platform native interface with system tray integration, multi-tab dashboards, live statistics, and QR code pairing.
3. **ONNX Predictive & ML**: Enables real-time LSTM neural network mouse trajectory smoothing and 2D Kalman filter prediction via Microsoft ONNX Runtime (`-tags "ai ml"`).

### 4. Security Hardening & Input Sanitization
- **Mandatory Encryption**: TLS 1.2+ required on TCP (`tls.Listen`) and WebSocket (`wss://`).
- **HMAC Handshake Authentication**: Connection requests (`hello`) require a cryptographically random pairing secret and HMAC token.
- **Replay Protection**: Enforces a strict timestamp sliding window (`|current_time - ts| <= 5000 ms`) and monotonically increasing sequence numbers.
- **Coordinate Bounds Enforcement**: Motion deltas are strictly clamped (`|dx|, |dy| <= 500.0`), and non-finite floats (`NaN`, `+Inf`, `-Inf`) are rejected.

### 5. Automated Parity Verifier (`scripts/check_docs.sh`)
An automated bash verification harness scans the Go codebase and asserts complete 100% parity against `docs/PROTOCOL_V2.md` and `DOCUMENTATION.md`. Any introduced message type or envelope drift automatically fails CI/CD.

---

## Quick Verification & Installation

### Verify Documentation Parity
```bash
./scripts/check_docs.sh
```

### Build the Headless Server (Zero-CGO)
```bash
cd pc/airmouse_go_new
CGO_ENABLED=0 go build -tags noai -ldflags "-s -w -X main.version=2.0.0" -o bin/airmouse-headless ./cmd/airmouse-server
./bin/airmouse-headless --headless
```

### Build Desktop Server with Web Dashboard
```bash
cd pc/airmouse_go_new
make build
./airmouse-server
```

### Run Server Test Suite with Race Detection
```bash
cd pc/airmouse_go_new
go test -v -race -timeout 30s ./...
```

---

## Migration from Legacy Protocol (v1.x)

1. **Envelope Requirement**: Legacy flat JSON packets (e.g. `{"type": "move", "dx": 1.0, "dy": 2.0}`) must be migrated to the nested Protocol V2 format: `{"v": 2, "type": "move", "seq": 101, "ts": 1740000000000, "payload": {"dx": 1.0, "dy": 2.0}}`.
2. **Authentication**: Handshake packets must supply the HMAC token under `payload.token`.
3. **Build Flags**: When compiling without ONNX Runtime libraries, pass `-tags noai` or use `make quick`.

---

## Credits & Acknowledgments
Developed at the **Embedded Systems Laboratory, University of Tehran** under the supervision of Dr. Mohsen Shokri and Dr. Mehdi Kargahi.
