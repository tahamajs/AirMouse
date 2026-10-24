# Comprehensive Remediation Execution Log (AGENT_LOG.md)

## Phase 0: Setup, Inventory, & Baseline Audit

### 1. Baseline Environment & Metadata
- **Baseline Commit**: `2154e023ce4ee178007d3e9cbe28d354f58c7358`
- **Mirror Backup Location**: `../airmouse-backup.git` (Created and verified via `git clone --mirror`)
- **Git Repository Size (`du -sh .git`)**: `625 MB`
- **Working Tree Branch**: `main`

### 2. Complete Repository Inventory
The codebase is structured into two primary environments: Go Desktop Server and Android Client, complemented by extensive documentation.

#### A. Go Server (`pc/airmouse_go_new/`):
- `cmd/airmouse-server/main.go`: Application entrypoint, CLI flags (`--headless`, `--port`, etc.), subsystem initialization.
- `cmd/integration-test/main.go`: Standalone integration test launcher.
- `internal/protocol/tcp/`: TCP server listener, client connection handler, wire message decoding loop, auto-approval and ACK dispatch.
- `internal/protocol/websocket/`: Gorilla WebSocket server, HTTP administration endpoints (`/api/stats`, `/api/devices`, `/api/config`), screen mirroring and file transfer handlers.
- `internal/protocol/udp/`: UDP discovery broadcast listener and beacon responder.
- `internal/protocol/message.go`: Low-level wire frame JSON encoding/decoding supporting legacy flat and nested payloads.
- `internal/auth/manager.go`: HMAC-based token issuance, TTL eviction, and claim validation.
- `internal/config/config.go`: Global configuration struct loaded from `config.json`.
- `internal/jitter/`: `buffer.go`, `kalman1d.go`, `deadreckoning.go` implementing delay compensation.
- `internal/adaptivesmoothing/`: B-spline, velocity, and tremor smoothing implementations.
- `internal/mouse/`: Platform-specific mouse controllers (`mouse_darwin.go`, `mouse_linux.go`, `mouse_windows.go`).
- `internal/predictiveml/`: ONNX runtime inference bindings and LSTM prediction models.
- `web/`: Embedded web dashboard (`index.html`, `static/app.js`, `style.css`).

#### B. Android App (`android/`):
- `network/ConnectionManager.kt`: Primary network transport manager for Android, handling TCP, WebSocket, reconnection, and state flows.
- `network/UdpDiscovery.kt`: LAN broadcast scanner for server auto-discovery.
- `sensors/CalibrationHelper.kt`: Sensor bias accumulator and static calibration wizard.
- `sensors/SensorFusion.kt`: Complementary filter combining accelerometer and gyroscope.
- `sensors/GestureDetector.kt`: Threshold-based gesture recognition (tap, double tap, shake, tilt).
- `presentation/`: Jetpack Compose UI screens (Dashboard, Calibration, Pairing, Settings).

#### C. Documentation & Specifications (`docs/`, `pc/docs/`, `android/docs/`):
- `docs/PROTOCOL_SPECIFICATION.md`, `pc/airmouse_go_new/CommunicationProtocol.md`: Wire protocol documents.
- Over 30 specialized Markdown guides (calibration, troubleshooting, threat considerations, architecture).

---

### 3. Baseline Test Results & Statement Coverage
`cd pc/airmouse_go_new && go test -cover ./...` results:
- `airmouse-go/internal/jitter`: **82.7%** statement coverage (PASS)
- `airmouse-go/internal/protocol/tcp`: **54.5%** statement coverage (PASS)
- `airmouse-go/internal/protocol/udp`: **47.4%** statement coverage (PASS)
- `airmouse-go/internal/protocol/websocket`: **4.7%** statement coverage (PASS)
- `airmouse-go/internal/utils`: **8.3%** statement coverage (PASS)
- `internal/auth`, `internal/protocol`, `internal/mouse`, `internal/config`, `internal/adaptivesmoothing`, `internal/predictiveml`: **0.0%** coverage (`[no test files]`).
- **Overall System Statement Coverage**: ~**14.8%** (Far below the required ≥65% CI gate).

---

### 4. Top 50 Largest Blobs in Git History
1. `8569cc9f35...` (35.86 MB) `pc/airmouse_go_new/airmouse-server`
2. `d42fc99078...` (35.82 MB) `pc/airmouse_go_new/airmouse-server`
3. `a0e7864ddf...` (35.82 MB) `pc/airmouse_go_new/airmouse-server`
4. `33773ab9d4...` (35.82 MB) `pc/airmouse_go_new/airmouse-server`
5. `55a9529646...` (35.82 MB) `pc/airmouse_go_new/airmouse-server`
6. `4443838f59...` (35.66 MB) `pc/airmouse_go_new/airmouse-server`
7. `054a832105...` (35.66 MB) `pc/airmouse_go_new/airmouse-server`
8. `98782fa6be...` (35.66 MB) `pc/airmouse_go_new/airmouse-server`
9. `c0e4ae5a16...` (35.63 MB) `pc/airmouse_go_new/airmouse-server`
10. `d4df8b1a0b...` (35.61 MB) `pc/airmouse_go_new/airmouse-server`
11. `ad37c9a9fc...` (35.61 MB) `pc/airmouse_go_new/airmouse-server`
12. `03dd702bfd...` (35.61 MB) `pc/airmouse_go_new/airmouse-server`
13. `741280aad3...` (35.61 MB) `pc/airmouse_go_new/airmouse-server`
14. `94815e9feb...` (35.57 MB) `pc/airmouse_go_new/airmouse-server`
15. `3791af3ea9...` (35.57 MB) `pc/airmouse_go_new/airmouse-server`
16. `77afe3fa62...` (35.54 MB) `pc/airmouse_go_new/airmouse-server`
17. `1f573d18f3...` (35.54 MB) `pc/airmouse_go_new/airmouse-server`
18. `1faa86b8d3...` (33.05 MB) `pc/airmouse_go_new/airmouse-server`
19. `924e616d9e...` (33.05 MB) `pc/airmouse_go_new/airmouse-server`
20. `f3c5731a07...` (33.03 MB) `pc/airmouse_go_new/airmouse-server`
21. `f05711ec4b...` (32.91 MB) `pc/airmouse_go_new/airmouse-server`
22. `0195e5f0d3...` (32.91 MB) `pc/airmouse_go_new/airmouse-server`
23. `b84b658211...` (32.90 MB) `pc/airmouse_go_new/airmouse-server`
24. `3792fc56b3...` (32.88 MB) `pc/airmouse_go_new/airmouse-server`
25. `2006f838e9...` (32.88 MB) `pc/airmouse_go_new/airmouse-server`
26. `31a7d69af4...` (32.88 MB) `pc/airmouse_go_new/airmouse-server`
27. `0a991fd0f7...` (32.88 MB) `pc/airmouse_go_new/airmouse-server`
28. `d4c02d0e94...` (32.88 MB) `pc/airmouse_go_new/airmouse-server`
29. `48ebc1b39c...` (32.88 MB) `pc/airmouse_go_new/airmouse-server`
30. `51a000983e...` (32.86 MB) `pc/airmouse_go_new/airmouse-server`
31. `8fa1d27045...` (32.86 MB) `pc/airmouse_go_new/airmouse-server`
32. `668655e547...` (32.83 MB) `pc/airmouse_go_new/airmouse-server`
33. `efa414ee4a...` (32.83 MB) `pc/airmouse_go_new/airmouse-server`
34. `1bfd9742a8...` (32.83 MB) `pc/airmouse_go_new/airmouse-server`
35. `f635ef40cb...` (32.80 MB) `pc/airmouse_go_new/airmouse-server`
36. `6b1d320e18...` (32.80 MB) `pc/airmouse_go_new/airmouse-server`
37. `06d5f81cad...` (32.80 MB) `pc/airmouse_go_new/airmouse-server`
38. `921272ef9b...` (32.80 MB) `pc/airmouse_go_new/airmouse-server`
39. `4ed4930833...` (32.80 MB) `pc/airmouse_go_new/airmouse-server`
40. `006e3d3bd1...` (32.78 MB) `pc/airmouse_go_new/airmouse-server`
41. `40c3390011...` (32.78 MB) `pc/airmouse_go_new/airmouse-server`
42. `e1ec476465...` (32.74 MB) `pc/airmouse_go_new/airmouse-server`
43. `9b056b50de...` (32.69 MB) `pc/airmouse_go_new/dist/macos/Air Mouse Pro Server.app/Contents/MacOS/airmouse-server`
44. `30f179c329...` (19.37 MB) `pc/airmouse_go_new/dist/macos/AirMouseProServer.dmg`
45. `cb071314dc...` (13.25 MB) `android/app/build/outputs/apk/debug/app-debug.apk`
46. `54c6ca6a99...` (10.31 MB) `android/app/build/intermediates/dex/debug/mergeExtDexDebug/classes.dex`
47. `fc34c6d2b3...` (10.19 MB) `android/app/build/intermediates/dex/debug/mergeExtDexDebug/classes.dex`
48. `6fdba3efa0...` (10.13 MB) `pc/airmouse_go_new/airmouse-server`
49. `12d6b1f870...` (10.13 MB) `pc/airmouse_go_new/airmouse-server`
50. `9142c5fbdc...` (10.13 MB) `pc/airmouse_go_new/airmouse-server`

---

### 5. Verified Code-Audit Findings (Evidence & Line References)

| Finding ID | Severity | File Reference | Code Evidence Confirmed |
|:---|:---:|:---|:---|
| **SEC-01** | CRITICAL | `pc/airmouse_go_new/internal/protocol/tcp/server.go:71`, `websocket/server.go:124` | Plain unencrypted `net.Listen("tcp", addr)` and `http.Serve(listener)` without TLS wrapper. Cleartext LAN snooping is trivially possible. |
| **SEC-02** | HIGH | `pc/airmouse_go_new/internal/config/config.go:259`, `tcp/server.go:348` | `AuthEnabled: false` by default. Anyone connecting to the TCP port can issue system clicks without pairing. |
| **SEC-03** | HIGH | `pc/airmouse_go_new/internal/protocol/message.go:12-16`, `tcp/server.go:242` | `WireMessage` lacks mandatory `seq` and `ts`. Server does not check sequence monotonicity or timestamp freshness, making replay attacks feasible. |
| **SEC-04** | MEDIUM | `pc/airmouse_go_new/internal/protocol/message.go:47-64` | Arbitrary JSON keys unmarshalled into untyped `map[string]any`; no numeric bounds checking for `dx`, `dy`, or NaN/Inf validation. |
| **SEC-05** | LOW | `pc/airmouse_go_new/CommunicationProtocol.md` | Protocol documentation contains no formal threat model, security posture assumptions, or replay specifications. |
| **REL-01** | HIGH | `pc/airmouse_go_new/internal/protocol/tcp/server.go:266` | `move` events are handled instantaneously without sequence tracking or gap detection; lost packets cause cursor jumps. |
| **REL-02** | MEDIUM | `pc/airmouse_go_new/internal/jitter/buffer.go:35` | Jitter buffer exists in `internal/jitter/` but is not attached to the TCP incoming event loop in `tcp/server.go`. |
| **REL-03** | MEDIUM | `pc/airmouse_go_new/internal/protocol/udp/server.go:174`, `android/.../UdpDiscovery.kt` | UDP broadcast discovery lacks deduplication, exponential retries, and challenge nonce verification. |
| **REL-04** | LOW | `pc/airmouse_go_new/internal/protocol/tcp/integration_test.go` | Existing integration tests only cover loopback happy-paths and do not verify behavior under packet drop or latency. |
| **UX-01** | MEDIUM | `android/app/src/main/java/com/airmouse/sensors/CalibrationHelper.kt:40` | Manual calibration requires user to hold phone completely still for fixed time; lacks automated Zero-Velocity Updates (ZUPT). |
| **UX-02** | HIGH | `pc/airmouse_go_new/internal/adaptivesmoothing/` | Absence of 1€ (One Euro) Filter algorithm; existing moving averages produce cursor lag at high velocity and jitter at rest. |
| **UX-03** | MEDIUM | `pc/airmouse_go_new/internal/mouse/mouse.go` | No fatigue dampener or resting state detection; minor hand tremor while holding position causes continuous pointer wobble. |
| **UX-04** | LOW | `android/app/src/main/java/com/airmouse/presentation/calibration/` | Static calibration UI lacks real-time sensor validity color indicators and 3-step dynamic guidance cards. |
| **ARCH-01** | MEDIUM | `pc/airmouse_go_new/internal/protocol/message.go:53` | Support for legacy flat JSON alongside nested payloads adds parsing ambiguity and computational overhead. |
| **ARCH-02** | MEDIUM | `docs/`, `pc/docs/`, `android/docs/` | Over 30 fragmented and drifting Markdown files without a unified root document or automated consistency validation. |
| **ARCH-03** | HIGH | `pc/airmouse_go_new/` | Core packages (`auth`, `protocol`, `mouse`, `predictiveml`) have 0% statement coverage. |
| **DEP-01** | HIGH | `pc/airmouse_go_new/go.mod:6-8` | Heavy dependencies (`fyne.io/fyne/v2`, `onnxruntime_go`) coupled without build tags; prevents building a clean, lightweight, zero-CGO headless server binary. |
| **DEP-02** | LOW | Entire repo | No web-based client fallback for iOS devices relying on HTML5 DeviceOrientation API. |

---

### Phase 0 Gate Assessment
- **Inventory Complete**: YES.
- **Audit Findings Verified with Line References**: YES.
- **Baseline Metrics Recorded**: YES (`du -sh .git`: 625 MB, baseline coverage: 14.8%).
- **Gate 0 Status**: **PASS**
