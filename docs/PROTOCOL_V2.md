# AirMouse Protocol V2 Specification
**Status: FROZEN (Canonical Contract)**
**Version: 2.0.0**

This document specifies the frozen communication protocol contract for AirMouse v2. All client and server components (Go server, Android app, Web client) MUST conform strictly to this specification.

---

## 1. Wire Envelope

All messages transmitted across TCP, WebSocket, or any stream transport MUST be formatted as single-line JSON objects ending with `\n` adhering to the following standard envelope:

```json
{
  "v": 2,
  "type": "<string>",
  "seq": 1001,
  "ts": 1740000000000,
  "id": "urn:uuid:f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "payload": { ... }
}
```

### Envelope Fields:
- `v` (integer, required): Protocol major version. MUST be `2`. Messages with missing `v` or `v != 2` are rejected.
- `type` (string, required): Message identifier enum (e.g. `hello`, `welcome`, `move`, `click`, `scroll`, `key`, `ack`, `nack`, `ping`, `pong`, `discover`, `offer`).
- `seq` (uint64, required for control/data streams): Monotonically increasing sequence number per session.
- `ts` (int64, required): Unix timestamp in milliseconds when packet was generated.
- `id` (string, optional/recommended): Unique UUID for action acknowledgement correlation.
- `payload` (object, required): Nested key-value map containing type-specific parameters. Flat payloads are rejected with error `protocol_v1_deprecated`.

---

## 2. Security & Session Rules

1. **Mandatory Encryption (TLS)**:
   - Transport MUST use TLS 1.2+ (`tls.Listen` for TCP, `wss://` for WebSocket). Plain unencrypted sockets are rejected.
   - Default certificates: ECDSA P-256 self-signed certificates generated into local secure configuration storage.
2. **Mandatory Pairing & HMAC Token**:
   - Pairing token is strictly mandatory on connection handshake (`hello`).
   - Pairing secret is 32 cryptographically random bytes. Tokens are HMAC-SHA256 signed.
   - Handshake without a valid signature or token is immediately dropped.
3. **Replay Protection**:
   - Replay window: `|current_time - ts| <= 5000 ms` (sliding window of ±5 seconds).
   - Monotonic `seq`: In a session, any packet with `seq <= last_received_seq` (for ordered control actions) is dropped or rejected with `nack: replay`.
4. **Strict Schema & Bounds Enforcement**:
   - Disallow unknown fields in message types.
   - Numeric limits:
     - `|dx|, |dy| <= 500.0`
     - `|delta| <= 10.0`
     - Special float values (`NaN`, `+Inf`, `-Inf`) MUST be rejected.
   - Transport max packet size limit: 1 MB (`SetReadLimit`).

---

## 3. Message Catalog & Payloads

### 3.1 Handshake (`hello` / `welcome`)

**Client -> Server (`hello`)**:
```json
{
  "v": 2,
  "type": "hello",
  "seq": 1,
  "ts": 1740000000000,
  "id": "c1f7b0b2-8f1b-43ec-84f9-2c70030dcb97",
  "payload": {
    "device_id": "device_fingerprint_hash",
    "name": "Pixel 7 Pro",
    "version": "2.0.0",
    "token": "HMAC_SHA256_TOKEN_OR_PAIRING_SECRET"
  }
}
```

**Server -> Client (`welcome`)**:
```json
{
  "v": 2,
  "type": "welcome",
  "seq": 1,
  "ts": 1740000000010,
  "id": "s8a7b0b2-8f1b-43ec-84f9-2c70030dcb98",
  "payload": {
    "server": "AirMouse Desktop",
    "version": "2.0.0",
    "client_id": "session_client_id",
    "approved": true
  }
}
```

### 3.2 Motion (`move`)
```json
{
  "v": 2,
  "type": "move",
  "seq": 102,
  "ts": 1740000000120,
  "payload": {
    "dx": 2.45,
    "dy": -1.18,
    "dt": 0.016
  }
}
```
*Validation:* `|dx| <= 500`, `|dy| <= 500`, `dt > 0`. Out-of-order `seq` dropped; sequence gaps trigger loss recovery.

### 3.3 Mouse Clicks (`click`, `mouse_down`, `mouse_up`)
```json
{
  "v": 2,
  "type": "click",
  "seq": 103,
  "ts": 1740000000150,
  "id": "act-click-001",
  "payload": {
    "button": "left"
  }
}
```
*Allowed buttons:* `"left"`, `"right"`, `"middle"`, `"double"`. Requires immediate `ack`.

### 3.4 Scroll (`scroll`)
```json
{
  "v": 2,
  "type": "scroll",
  "seq": 104,
  "ts": 1740000000180,
  "id": "act-scroll-001",
  "payload": {
    "delta": 2.0
  }
}
```
*Validation:* `|delta| <= 10.0`. Requires immediate `ack`.

### 3.5 Keystrokes & Text (`key`)
```json
{
  "v": 2,
  "type": "key",
  "seq": 105,
  "ts": 1740000000210,
  "id": "act-key-001",
  "payload": {
    "key": "enter",
    "modifiers": ["ctrl"]
  }
}
```

### 3.6 Acknowledgment & Error (`ack` / `nack`)
**Server -> Client (`ack`)**:
```json
{
  "v": 2,
  "type": "ack",
  "seq": 106,
  "ts": 1740000000220,
  "payload": {
    "ack_id": "act-click-001",
    "status": "ok"
  }
}
```

**Server -> Client (`nack`)**:
```json
{
  "v": 2,
  "type": "nack",
  "seq": 107,
  "ts": 1740000000230,
  "payload": {
    "nack_id": "act-key-001",
    "error": "replay_detected",
    "expected_seq": 106,
    "missing_from": 102,
    "missing_to": 105
  }
}
```

### 3.7 Discovery (`discover` / `offer`)
UDP Broadcast on port 8083:
**Client -> Broadcast (`discover`)**:
```json
{
  "v": 2,
  "type": "discover",
  "seq": 1,
  "ts": 1740000000000,
  "payload": {
    "client_name": "Pixel 7 Pro",
    "nonce": "random_discovery_nonce_12345"
  }
}
```

**Server -> Client Unicast (`offer`)**:
```json
{
  "v": 2,
  "type": "offer",
  "seq": 1,
  "ts": 1740000000020,
  "payload": {
    "server_name": "AirMouse Server",
    "instance_id": "inst_0a1b2c3d",
    "tcp_port": 8080,
    "ws_port": 8081,
    "tls": true,
    "cert_fingerprint": "SHA256:7b:19:...",
    "echo_nonce": "random_discovery_nonce_12345"
  }
}
```

---
*Protocol V2 is officially frozen and constitutes the canonical contract for all subsequent remediation phases.*
