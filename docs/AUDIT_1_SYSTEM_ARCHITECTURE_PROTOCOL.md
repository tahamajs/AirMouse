# Deep Architectural & Protocol V2 Audit: AirMouse

**Audit Date**: October 2026  
**Auditor**: Principal Systems & Protocol Architect  
**Scope**: End-to-End System Architecture, Protocol V2 Specification, Inter-Client State Synchronization, and High-Throughput Packet Transport.

---

## 1. Executive Summary

AirMouse is designed as a ultra-low-latency, multi-platform input synthesis system converting mobile device sensor data (IMU, Touch, Proximity) into standard desktop OS mouse and keyboard events. The system spans:
- **Clients**: Android Native (Compose/Kotlin), Mobile Web Client (Vanilla JS/PWA for iOS/cross-platform).
- **Servers**: Go Desktop Server (Zero-CGO, high performance), Python Core Server (Asyncio fallback).
- **Transport**: WebSockets (TCP) and UDP/WebRTC data channels under Protocol V2.

While the architectural vision is modular and ambitious, this audit identifies **structural weaknesses**, **concurrency traps**, and **protocol design compromises** that impair stability, deterministic packet delivery, and security under adverse network conditions.

---

## 2. Protocol V2 Wire Format & Envelope Weaknesses

### 2.1 JSON Payload Overhead vs. High-Frequency IMU Telemetry
* **Weakness**: Protocol V2 utilizes a JSON envelope for high-frequency motion packets (60 Hz – 120 Hz):
  ```json
  {
    "v": 2,
    "seq": 10429,
    "type": "motion",
    "ts": 1729858291042,
    "payload": {
      "dx": -3.42,
      "dy": 1.18,
      "roll": 0.04,
      "pitch": -0.12,
      "mode": "AIR"
    },
    "sig": "a5c7f8e..."
  }
  ```
* **Impact**:
  - Each motion packet averages **180–240 bytes** uncompressed. At 120 Hz, this consumes **~28.8 KB/s (230 kbps)** of continuous parsing and allocation overhead.
  - JSON serialization in Kotlin (`kotlinx.serialization`) and deserialization in Go/Python creates sustained heap allocation churn and trigger periodic garbage collection (GC) sweeps, introducing **5–15 ms latency spikes** (jitter) incompatible with competitive gaming or precision tracking.
* **Remediation**:
  - Transition high-frequency packets (`motion`, `touch_move`, `gyro_raw`) to a compact binary wire format (FlatBuffers or Protobuf) reserving JSON strictly for control/handshake frames (`auth`, `config`, `ping`).

### 2.2 Monotonic Sequence Tracking & Out-of-Order UDP Hazards
* **Weakness**: Protocol V2 specifies monotonic `seq` increments for replay protection and packet ordering. However, when streaming over UDP:
  - Dropped packets cause sequence gaps.
  - Out-of-order packets arriving with `seq < last_seen_seq` are discarded immediately by default rules.
  - In burst packet scenarios (e.g. Wi-Fi packet aggregation / frame bursts), legitimate late motion updates are dropped, causing cursor stuttering.
* **Remediation**:
  - Implement a bounded jitter buffer (sliding window of 3–5 packets) with sliding timestamp interpolation rather than hard sequential dropping.

### 2.3 Shared Secret Exchange & HMAC Vulnerabilities
* **Weakness**: Authentication relies on a pre-shared secret (via QR code or PIN) and HMAC-SHA256 signatures:
  - The shared secret is long-lived and stored in plain memory on both client and server.
  - Absence of an ephemeral key exchange (e.g., ECDH via Curve25519) means compromise of the initial secret compromises all past and future sessions.
  - Replay protection relies on timestamp windows (`|ts - now| < 3000ms`). If client and server clocks drift by >3 seconds (common on isolated LANs or unsynced offline machines), authentication fails completely.

---

## 3. Multiplatform State Synchronization Weaknesses

### 3.1 Split-Brain Mode Transitions
* **Weakness**: AirMouse supports multiple operating modes:
  - `TOUCHPAD` (trackpad style, absolute or relative)
  - `AIR_MOUSE` (gyroscope orientation / velocity pointing)
  - `PRESENTATION` (slides, laser pointer overlay)
  - `GAMING` (low-filter zero-latency direct mapping)
* When switching modes from the Android UI, the client immediately updates its local `UiState` optimistically before receiving acknowledgment from the Go/Python server. If the server rejects or drops the mode switch packet, the client UI displays controls for a mode the server is not executing.

### 3.2 Bidirectional Telemetry Feedback Loop
* **Weakness**: Battery level, CPU temperature, and ping latency packets sent from client to server are not throttled independently of motion packets. Under high network congestion, telemetry packets contend with motion vectors on the same WebSocket queue, increasing input delay.

---

## 4. Architectural Decoupling & Inconsistency (Go vs. Python)

| Feature / Behavior | Go Desktop Server (`cmd/server`) | Python Core Server (`airmouse-core`) | Architectural Risk |
|---|---|---|---|
| **OS Input Backend** | Native OS APIs (CGEvent, SendInput, uinput) | PyAutoGUI / pynput | Divergent cursor acceleration curves |
| **Concurrency Model** | Goroutines + Channels | Single-threaded Asyncio Event Loop | Python blocks input during disk I/O |
| **TLS / WSS Support** | Native crypto/tls self-signed certs | Optional via ssl context | Python setups often run unencrypted WS |
| **System Tray / GUI** | Fyne UI desktop integration | Headless or CLI curses | Inconsistent user configuration access |

* **Audit Verdict**: Dual-server maintenance has created feature disparity. Enhancements made to Go input smoothing are frequently omitted or differently implemented in Python, creating an unpredictable cross-platform user experience.

---

## 5. Architectural Recommendations & Action Items

1. **Protocol V3 Migration Path**: Adopt a hybrid transport:
   - WebSocket (WSS) on TCP for session lifecycle, handshakes, key exchange, and commands.
   - UDP (DTLS or authenticated binary frames) for motion and sensor vectors.
2. **Clock Drift Resilience**: Replace raw timestamp checks with a nonced challenge-response handshake that establishes an epoch baseline offset upon connection.
3. **State Machine Convergence**: Enforce strict Two-Way Handshake for mode switching:
   `MODE_CHANGE_REQUEST` $\rightarrow$ `SERVER_ACK` $\rightarrow$ `UI_STATE_COMMITTED`.
