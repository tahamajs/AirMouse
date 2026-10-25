---
name: airmouse-protocol-v2
description: >-
  Expert guidance, validation rules, and implementation patterns for the AirMouse Protocol V2
  canonical wire envelope, HMAC-SHA256 authentication, monotonic sequence tracking, replay
  protection, and bidirectional command dispatch across Go, Python, Android, and Web clients.
---

# AirMouse Protocol V2 Specification & Implementation Skill

Use this skill whenever authoring, modifying, testing, or debugging network communication, serialization, message validation, or command routing in AirMouse clients or servers.

## 1. Canonical Wire Envelope Contract

All messages across TCP, TLS, and WebSocket (`wss://`) MUST be single-line JSON strings terminated by `\n` adhering strictly to this envelope:

```json
{
  "v": 2,
  "type": "<message_type>",
  "seq": 1001,
  "ts": 1740000000000,
  "id": "urn:uuid:f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "payload": { ... }
}
```

### Strict Envelope Rules
- **`v`**: MUST be integer `2`. Any packet with `v != 2` or missing `v` MUST be rejected with error `unsupported_protocol_version`.
- **`type`**: String enum representing message type (`hello`, `welcome`, `move`, `click`, `scroll`, `key`, `ack`, `nack`, `ping`, `pong`, `calibrate`).
- **`seq`**: Monotonically increasing unsigned 64-bit integer (`uint64`). Per-connection sequence numbers.
- **`ts`**: Unix timestamp in milliseconds (`int64`).
- **`id`**: Unique string/UUID for correlating ACKs/NACKs.
- **`payload`**: JSON object containing type-specific parameters. Flat root keys (V1 style) are strictly forbidden and rejected with `protocol_v1_deprecated`.

---

## 2. Security & Bounds Enforcement

### 1. Replay Protection
- Verify timestamps against server clock: `|server_ts - packet_ts| <= 5000` (5-second sliding window).
- For sequenced control messages: `packet.seq > session.last_seen_seq`. Out-of-order or duplicate sequences must trigger `nack: replay_detected`.

### 2. Payload Sanitization & Bounds Checks
```python
# Absolute bounds enforcement:
assert abs(dx) <= 500.0 and abs(dy) <= 500.0, "Displacement exceeds maximum safe bound"
assert abs(delta) <= 10.0, "Scroll delta exceeds maximum safe bound"
assert not math.isnan(dx) and not math.isinf(dx), "NaN or Inf values rejected"
```
```go
// Go strict bounds verification:
if math.IsNaN(dx) || math.IsInf(dx, 0) || math.Abs(dx) > 500.0 {
    return errors.New("invalid or out-of-bounds dx value")
}
```

### 3. HMAC-SHA256 Authentication
- Pairing token generated during setup via shared 32-byte secret.
- In `hello` handshake:
  `token = HMAC_SHA256(secret, client_id + ":" + nonce + ":" + ts)`
- If signature check fails, terminate socket immediately without leaking internal details.

---

## 3. Core Message Catalog

### Handshake (`hello` -> `welcome`)
```json
// Client -> Server: hello
{
  "v": 2,
  "type": "hello",
  "seq": 1,
  "ts": 1740000000000,
  "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "payload": {
    "device_name": "Redmi Note 8T",
    "device_type": "android",
    "token": "a4f89d3c..."
  }
}

// Server -> Client: welcome
{
  "v": 2,
  "type": "welcome",
  "seq": 1,
  "ts": 1740000000005,
  "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "payload": {
    "session_id": "sess_8f3b41e",
    "screen_width": 1920,
    "screen_height": 1080,
    "rate_limit_hz": 120
  }
}
```

### Cursor Motion (`move`)
```json
{
  "v": 2,
  "type": "move",
  "seq": 102,
  "ts": 1740000000016,
  "payload": {
    "dx": -3.42,
    "dy": 1.85,
    "filtered": true
  }
}
```

### Click / Tap (`click`)
```json
{
  "v": 2,
  "type": "click",
  "seq": 103,
  "ts": 1740000000020,
  "payload": {
    "button": "left", // "left", "right", "middle"
    "state": "down"   // "down", "up", "tap"
  }
}
```

### Scroll (`scroll`)
```json
{
  "v": 2,
  "type": "scroll",
  "seq": 104,
  "ts": 1740000000025,
  "payload": {
    "delta_x": 0.0,
    "delta_y": -1.5
  }
}
```

---

## 4. Best Practices for Developers & Agents
1. Always maintain backward-compatible field parsing using safe struct tags (`json:",omitempty"` in Go, `@SerializedName` in Kotlin, `pydantic.BaseModel` in Python).
2. Never send unauthenticated packets after initial handshake; keep session state validated in memory.
3. For UDP fallback modes, integrate sequence gap tracking to calculate and report real-time packet loss percentage.
