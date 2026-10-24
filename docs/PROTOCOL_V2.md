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
- `type` (string, required): Message identifier enum (e.g. `hello`, `welcome`, `move`, `click`, `scroll`, `key`, `ack`, `nack`, `ping`, `pong`, `discover`, `offer`, etc.).
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

## 3. Complete Message Catalog & Payloads

### 3.1 Handshake & Authentication

#### `hello` / `device_info` (Client -> Server)
Initiates authenticated connection handshake. (The alias `device_info` is also accepted for backward compatibility.)
```json
{
  "v": 2,
  "type": "hello",
  "seq": 1,
  "ts": 1740000000000,
  "id": "c1f7b0b2-8f1b-43ec-84f9-2c70030dcb97",
  "payload": {
    "device_id": "device_fingerprint_hash",
    "name": "Pixel 8 Pro",
    "version": "2.0.0",
    "token": "HMAC_SHA256_TOKEN_OR_PAIRING_SECRET"
  }
}
```

#### `welcome` (Server -> Client)
Confirms successful authentication and session initialization.
```json
{
  "v": 2,
  "type": "welcome",
  "seq": 1,
  "ts": 1740000000010,
  "id": "s8a7b0b2-8f1b-43ec-84f9-2c70030dcb98",
  "payload": {
    "server": "AirMouse Server",
    "version": "2.0.0",
    "client_id": "session_client_id",
    "approved": true
  }
}
```

### 3.2 Motion & Pointer Input

#### `move` (Client -> Server)
Relative cursor displacement.
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
*Validation:* `|dx| <= 500.0`, `|dy| <= 500.0`, `dt > 0`. Out-of-order `seq` dropped.

#### `click` (Client -> Server)
Standard mouse button click.
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

#### `mouse_down` (Client -> Server)
Press mouse button down without releasing.
```json
{
  "v": 2,
  "type": "mouse_down",
  "seq": 104,
  "ts": 1740000000160,
  "id": "act-mdown-001",
  "payload": {
    "button": "left"
  }
}
```

#### `mouse_up` (Client -> Server)
Release mouse button.
```json
{
  "v": 2,
  "type": "mouse_up",
  "seq": 105,
  "ts": 1740000000170,
  "id": "act-mup-001",
  "payload": {
    "button": "left"
  }
}
```

#### `doubleclick` / `double_click` (Client -> Server)
Double left click convenience packet.
```json
{
  "v": 2,
  "type": "doubleclick",
  "seq": 106,
  "ts": 1740000000175,
  "id": "act-dclick-001",
  "payload": {}
}
```
*Note:* The alias `double_click` is accepted by servers for compatibility.

#### `rightclick` / `right_click` (Client -> Server)
Right click convenience packet.
```json
{
  "v": 2,
  "type": "rightclick",
  "seq": 107,
  "ts": 1740000000178,
  "id": "act-rclick-001",
  "payload": {}
}
```
*Note:* The alias `right_click` is accepted by servers for compatibility.

#### `scroll` (Client -> Server)
Scroll wheel displacement.
```json
{
  "v": 2,
  "type": "scroll",
  "seq": 108,
  "ts": 1740000000180,
  "id": "act-scroll-001",
  "payload": {
    "delta": 2.0
  }
}
```
*Validation:* `|delta| <= 10.0`. Requires immediate `ack`.

### 3.3 Keyboard & Input Injection

#### `key` / `keypress` (Client -> Server)
Key press / strike event.
```json
{
  "v": 2,
  "type": "key",
  "seq": 109,
  "ts": 1740000000210,
  "id": "act-key-001",
  "payload": {
    "key": "enter",
    "modifiers": ["ctrl"]
  }
}
```

#### `type` (Client -> Server)
Direct text string typing.
```json
{
  "v": 2,
  "type": "type",
  "seq": 110,
  "ts": 1740000000215,
  "id": "act-type-001",
  "payload": {
    "text": "Hello World"
  }
}
```

### 3.4 Gestures, Orientation & Proximity

#### `gesture` (Client -> Server)
Recognized motion or ML-based gesture.
```json
{
  "v": 2,
  "type": "gesture",
  "seq": 111,
  "ts": 1740000000220,
  "id": "act-gest-001",
  "payload": {
    "gesture": "SwipeLeft",
    "confidence": 0.94
  }
}
```

#### `proximity` (Client -> Server)
Proximity distance update from BLE Channel Sounding or RSSI fusion.
```json
{
  "v": 2,
  "type": "proximity",
  "seq": 112,
  "ts": 1740000000230,
  "payload": {
    "device_id": "device_fingerprint_hash",
    "is_near": true,
    "distance": 1.25
  }
}
```

#### `orientation` (Client -> Server)
Raw or fused 3D orientation angles.
```json
{
  "v": 2,
  "type": "orientation",
  "seq": 113,
  "ts": 1740000000240,
  "payload": {
    "pitch": 12.4,
    "roll": -3.1,
    "yaw": 85.0
  }
}
```

#### `calibration_data` (Client -> Server)
Sensor calibration parameters and quality.
```json
{
  "v": 2,
  "type": "calibration_data",
  "seq": 114,
  "ts": 1740000000250,
  "payload": {
    "gyro": {"x": 0.01, "y": -0.02, "z": 0.005},
    "accel": {"x": 0.0, "y": 0.0, "z": 9.81},
    "mag": {"x": 22.1, "y": -15.4, "z": 40.2},
    "quality": "high",
    "timestamp": 1740000000250
  }
}
```

### 3.5 System & Application Control

#### `control` (Client -> Server)
System control commands (movement pause, window management, lock screen).
```json
{
  "v": 2,
  "type": "control",
  "seq": 115,
  "ts": 1740000000260,
  "id": "act-ctrl-001",
  "payload": {
    "command": "pause_movement"
  }
}
```
*Supported commands:* `pause_movement`, `resume_movement`, `lock_screen`, `unlock_screen`, `show_desktop`, `task_view`, `switch_window`, `window_close`, `zoom_in`, `zoom_out`, `zoom_reset`, `calibrate`, `mode_change`, `theme_change`.

#### `presentation` (Client -> Server)
Slide deck presentation controls.
```json
{
  "v": 2,
  "type": "presentation",
  "seq": 116,
  "ts": 1740000000270,
  "id": "act-pres-001",
  "payload": {
    "action": "next"
  }
}
```
*Supported actions:* `prev`, `next`, `fullscreen`, `exit`.

#### `media` (Client -> Server)
Media playback controls.
```json
{
  "v": 2,
  "type": "media",
  "seq": 117,
  "ts": 1740000000280,
  "id": "act-media-001",
  "payload": {
    "action": "playpause"
  }
}
```
*Supported actions:* `playpause`, `prev`, `next`, `volumeup`, `volumedown`, `stop`.

#### `laser` (Client -> Server)
Virtual laser pointer and annotations.
```json
{
  "v": 2,
  "type": "laser",
  "seq": 118,
  "ts": 1740000000290,
  "payload": {
    "active": true,
    "x": 0.52,
    "y": 0.38,
    "color": "#FF0000"
  }
}
```

#### `voice_command` (Client -> Server)
Transcribed voice intent.
```json
{
  "v": 2,
  "type": "voice_command",
  "seq": 119,
  "ts": 1740000000300,
  "payload": {
    "command": "click",
    "confidence": 0.95
  }
}
```

#### `steering` (Client -> Server)
Game steering wheel emulation.
```json
{
  "v": 2,
  "type": "steering",
  "seq": 120,
  "ts": 1740000000310,
  "payload": {
    "angle": -15.5,
    "throttle": 0.8,
    "brake": 0.0
  }
}
```

#### `shooter` (Client -> Server)
Motion aiming and trigger firing.
```json
{
  "v": 2,
  "type": "shooter",
  "seq": 121,
  "ts": 1740000000320,
  "payload": {
    "aim_x": 0.2,
    "aim_y": -0.1,
    "trigger": true
  }
}
```

#### `smarthome` (Client -> Server)
Smart device integration trigger.
```json
{
  "v": 2,
  "type": "smarthome",
  "seq": 122,
  "ts": 1740000000330,
  "payload": {
    "device": "desk_lamp",
    "state": "toggle"
  }
}
```

### 3.6 Diagnostics & Telemetry

#### `battery` (Client -> Server)
Battery level and charging state.
```json
{
  "v": 2,
  "type": "battery",
  "seq": 123,
  "ts": 1740000000340,
  "payload": {
    "level": 85,
    "charging": false
  }
}
```

#### `log` (Client -> Server)
Client runtime logs forwarded to server log viewer.
```json
{
  "v": 2,
  "type": "log",
  "seq": 124,
  "ts": 1740000000350,
  "payload": {
    "level": "info",
    "tag": "SensorService",
    "message": "Gyroscope calibrated"
  }
}
```

#### `status` (Bidirectional)
Runtime status report.
```json
{
  "v": 2,
  "type": "status",
  "seq": 125,
  "ts": 1740000000360,
  "payload": {
    "running": true,
    "clients": 1
  }
}
```

#### `stats` (Server -> Client)
Server performance and input statistics.
```json
{
  "v": 2,
  "type": "stats",
  "seq": 126,
  "ts": 1740000000370,
  "payload": {
    "clicks": 45,
    "scrolls": 12,
    "uptime_seconds": 3600
  }
}
```

#### `notification` (Server -> Client)
Push notification to client screen.
```json
{
  "v": 2,
  "type": "notification",
  "seq": 127,
  "ts": 1740000000380,
  "payload": {
    "title": "AirMouse",
    "message": "Device paired successfully"
  }
}
```

#### `config` (Bidirectional)
Configuration sync or adjustment.
```json
{
  "v": 2,
  "type": "config",
  "seq": 128,
  "ts": 1740000000390,
  "payload": {
    "sensitivity": 1.2,
    "smoothing": true
  }
}
```

#### `custom` (Bidirectional)
User-defined extension payloads.
```json
{
  "v": 2,
  "type": "custom",
  "seq": 129,
  "ts": 1740000000400,
  "payload": {
    "action": "custom_macro",
    "data": {}
  }
}
```

#### `file` (Bidirectional)
Chunked file transfer handshake and progress.
```json
{
  "v": 2,
  "type": "file",
  "seq": 130,
  "ts": 1740000000410,
  "payload": {
    "action": "start",
    "filename": "screenshot.png",
    "size": 1048576
  }
}
```

### 3.7 Heartbeats & Acknowledgments

#### `ping` / `pong` (Bidirectional)
Keep-alive heartbeat.
```json
{
  "v": 2,
  "type": "ping",
  "seq": 131,
  "ts": 1740000000420,
  "payload": {}
}
```
Response:
```json
{
  "v": 2,
  "type": "pong",
  "seq": 131,
  "ts": 1740000000425,
  "payload": {}
}
```

#### `ack` (Server -> Client)
Success confirmation for reliable actions.
```json
{
  "v": 2,
  "type": "ack",
  "seq": 132,
  "ts": 1740000000430,
  "payload": {
    "ack_id": "act-click-001",
    "status": "ok"
  }
}
```

#### `nack` (Server -> Client)
Rejection or failure notification for a command.
```json
{
  "v": 2,
  "type": "nack",
  "seq": 133,
  "ts": 1740000000435,
  "payload": {
    "nack_id": "act-key-001",
    "error": "replay_detected",
    "expected_seq": 106,
    "missing_from": 102,
    "missing_to": 105
  }
}
```

#### `error` (Server -> Client)
General error notification.
```json
{
  "v": 2,
  "type": "error",
  "seq": 134,
  "ts": 1740000000440,
  "payload": {
    "error": "unauthorized_token"
  }
}
```

### 3.8 LAN Discovery (`discover` / `offer`)
UDP Broadcast on port 8082 / 8083:

#### `discover` (Client -> Broadcast)
```json
{
  "v": 2,
  "type": "discover",
  "seq": 1,
  "ts": 1740000000000,
  "payload": {
    "client_name": "Pixel 8 Pro",
    "nonce": "random_discovery_nonce_12345"
  }
}
```

#### `offer` (Server -> Client Unicast)
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
*Protocol V2 is officially frozen and constitutes the canonical contract for all components.*
