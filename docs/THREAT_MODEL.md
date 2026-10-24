# AirMouse Threat Model & Security Architecture

**Document Version:** 2.0.0  
**Status:** Canonical & Enforced  
**Protocol Version:** AirMouse Protocol V2  
**Target Architecture:** Desktop Server (Go) & Mobile Client (Android)

---

## 1. Executive Summary & Scope

AirMouse transforms mobile devices into low-latency wireless input controllers (mouse cursor, click events, scroll, gestures, presentation controls, and keystrokes) communicating with a desktop workstation host over local network transports (TCP, WebSocket, and UDP discovery). Because the host system executes synthetic input events directly at the operating system level (controlling mouse pointers, issuing clicks, and typing keystrokes), a security breach could yield arbitrary input injection, UI redressing, or desktop compromise.

This document formalizes the **Threat Model**, **Adversary Assumptions**, **STRIDE Analysis**, and **Security Controls** enforced across the Go desktop server and Protocol V2 wire specification.

---

## 2. System Architecture & Trust Boundaries

```
      +-------------------------------------------------------------+
      |                     Mobile Device (Android)                 |
      |   - AirMouse Client App                                     |
      |   - Secure Storage (Pairing Secret / HMAC Token)            |
      +-------------------------------------------------------------+
                                     |
                          [ Trust Boundary 1: Untrusted Network ]
                                     |
                     TLS 1.2+ (WSS / TCP-TLS) / UDP Broadcast
                                     |
      +-------------------------------------------------------------+
      |                   Desktop Host (Go Server)                  |
      |                                                             |
      |  [ Network Edge ]                                           |
      |   - TLS Listener (ECDSA P-256 Cert, Cleartext Rejected)    |
      |   - Packet Size Limit (1 MB ReadLimit)                      |
      |                                                             |
      |  [ Protocol V2 Parser ]                                     |
      |   - json.NewDecoder + DisallowUnknownFields()               |
      |   - Envelope Validation (v=2, type, seq, ts, id, payload)   |
      |                                                             |
      |  [ Security Validation ]                                    |
      |   - Replay Protection: Sliding Window (|ts-now| <= 5000ms)  |
      |   - Replay Protection: Monotonic Sequence (seq > last_seq)  |
      |   - Auth Engine: Mandatory HMAC-SHA256 Token on Handshake   |
      |   - Numeric Bounds Checks: |dx|,|dy| <= 500, |delta| <= 10  |
      |   - Float Sanitization: NaN, +Inf, -Inf Rejected           |
      |                                                             |
      |  [ Execution Core ]                                         |
      |   - Device Approval Gate (Approved == true)                |
      |   - OS Synthetic Input (robotgo / CoreGraphics / X11)       |
      +-------------------------------------------------------------+
```

### Trust Boundaries:
1. **Network Boundary (LAN / Wi-Fi):** The local network is considered hostile. Attackers may share the Wi-Fi network, attempt ARP spoofing, broadcast rogue discovery frames, or passively sniff traffic.
2. **Session & Authentication Boundary:** Connections are strictly unprivileged until mutually paired and authenticated via HMAC-SHA256 tokens and explicit user approval.
3. **OS Control Boundary:** Only validated, bounded, and authenticated actions are forwarded to OS synthetic event injectors.

---

## 3. Adversary Model & Assumptions

We assume an adversary with the following capabilities:
- **LAN Presence:** Can connect to the same local area network (home, office, university, or public Wi-Fi).
- **Traffic Interception (MITM):** Can eavesdrop on unencrypted network traffic, modify frames in flight, or attempt replay attacks.
- **Malformed Packet Injection:** Can send arbitrary JSON payloads, fuzz inputs, inject unexpected fields, send extreme float values (`NaN`, `Inf`, `1e308`), or flood connection sockets.
- **Rogue Client Simulation:** Can reverse-engineer client protocol formats and simulate handshake sequences.

The adversary **cannot**:
- Compromise the host operating system kernel or memory space directly without first finding a vulnerability in the Go server.
- Extract cryptographic keys stored securely in the host user configuration without host-level access.

---

## 4. STRIDE Threat Analysis

| Threat Category | Description & Threat Scenario | Severity | Mitigation in AirMouse V2 |
|---|---|---|---|
| **Spoofing (S)** | An unauthorized phone or attacker script impersonates a trusted client to gain control of the desktop. | **Critical** | Mandatory HMAC-SHA256 handshake token verification. Unauthenticated connections are dropped immediately upon handshake attempt. |
| **Tampering (T)** | Attacker intercepts stream on Wi-Fi and modifies motion deltas or injects malicious click/key actions. | **Critical** | Mandatory TLS 1.2+ encryption on both TCP and WebSocket. Plaintext sockets are rejected by `tls.Listen` and `tls.NewListener`. |
| **Repudiation (R)** | Malicious actions taken via AirMouse cannot be correlated or traced to a specific device session. | **Medium** | Every packet contains a session sequence `seq`, timestamp `ts`, optional correlation `id`, and per-device audit logging. |
| **Information Disclosure (I)** | Passive eavesdropper intercepts sensitive keystrokes, clipboard content, or screen preview frames. | **High** | Full-session TLS encryption for TCP and WSS. No plaintext transport fallback is allowed in production. |
| **Denial of Service (D)** | Attacker floods server with oversized packets, endless loops, or millions of invalid connections. | **High** | 1 MB max packet size limit (`SetReadLimit`), idle connection timeouts (30s heartbeat, 120s timeout), connection rate throttling. |
| **Elevation of Privilege (E)** | Attacker injects NaN/Inf coordinates to crash pointer engine or sends unexpected fields to exploit unmarshaling. | **Critical** | Strict `json.NewDecoder.DisallowUnknownFields()`, NaN/Inf rejection, and rigid bounds checking (`\|dx\|, \|dy\| <= 500.0`, `\|delta\| <= 10.0`). |

---

## 5. Security Controls & Implementation Details

### 5.1 Transport Layer Security (TLS 1.2+)
- **Implementation:** `internal/tlsutil/tlsutil.go`
- **Certificate Standard:** Self-signed ECDSA with curve P-256 (`elliptic.P256()`).
- **Enforcement:**
  - TCP Server wraps listener via `tls.Listen` and performs immediate handshake validation during client accept. Cleartext connections fail the TLS handshake and are terminated without receiving any application data.
  - WebSocket Server wraps TCP listener via `tls.NewListener(l, tlsConfig)`. All incoming connections must negotiate TLS before HTTP upgrade or WebSocket framing occurs.
- **Cipher Suites:** Restrictive modern suites (`TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384`, `TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256`, `TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305`). Minimum TLS version: `tls.VersionTLS12`.

### 5.2 Mandatory Authentication & Pairing
- **Implementation:** `internal/auth/manager.go`, `internal/protocol/tcp/server.go`, `internal/protocol/websocket/server.go`
- **Mechanism:** HMAC-SHA256 token verification.
- **Handshake Enforcement:**
  - Every connection must transmit a `hello` handshake frame containing `"token": "<hmac_token>"`.
  - The token is verified against the server's 32-byte cryptographically secure secret.
  - If authentication is enabled and the token is missing or invalid, the server emits an error envelope and terminates the socket immediately.

### 5.3 Replay Protection
- **Implementation:** `internal/protocol/message.go` (`ReplayDetector`, `ValidateTimestamp`)
- **Dual Sliding Mechanism:**
  1. **Timestamp Sliding Window:**
     $$\left| \text{current\_time\_ms} - \text{packet\_ts\_ms} \right| \le 5000\text{ ms}$$
     Packets older than 5 seconds or more than 5 seconds in the future (skewed/replayed) are dropped.
  2. **Monotonically Increasing Sequence:**
     $$\text{seq}_{i} > \text{seq}_{i-1}$$
     Every session maintains a strictly monotonic sequence counter. Packets with repeated or retrogressive sequence numbers trigger `replay_detected` errors and are discarded.

### 5.4 Strict Schema & Bounds Enforcement
- **Implementation:** `internal/protocol/message.go`
- **Envelope V2 Enforcement:**
  - Messages must adhere to the V2 envelope containing:
    ```json
    {
      "v": 2,
      "type": "<type_string>",
      "seq": 1001,
      "ts": 1740000000000,
      "id": "optional-uuid",
      "payload": { ... }
    }
    ```
  - Decoded using `json.NewDecoder(r)` with `DisallowUnknownFields()`. Any unrecognized top-level or payload-level fields cause decoding failure.
  - Protocol version `v != 2` is rejected with `protocol_v1_deprecated`.
- **Numeric Limits & Sanitization:**
  - `|dx| <= 500.0`, `|dy| <= 500.0`
  - `|delta| <= 10.0`
  - Floats are checked for `math.IsNaN()` and `math.IsInf()`. Any special float value is discarded.
  - Mouse button strings are validated against a strict whitelist: `left`, `right`, `middle`, `double`.

---

## 6. Residual Risks & Operational Hardening

1. **Self-Signed Certificate Trust On First Use (TOFU):**
   - Because the desktop server uses self-signed ECDSA certificates by default, client apps should verify the certificate fingerprint during initial pairing (e.g. by comparing the QR code fingerprint against the discovery offer frame `SHA256:xx:xx:...`).
2. **Local Machine Privileges:**
   - The desktop Go server should run as a standard desktop user, not as root/Administrator, limiting the blast radius in the event of an OS-level synthetic input flaw.
3. **Secret Generation:**
   - On first installation, a unique 32-byte cryptographic random secret is generated and saved to the local user configuration directory with restricted file permissions (`0600`).
