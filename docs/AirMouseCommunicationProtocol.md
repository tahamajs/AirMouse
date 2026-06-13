# Air Mouse Communication Protocol – Complete Documentation

## Table of Contents

1. [Protocol Overview](#protocol-overview)
2. [Connection & Pairing Flow](#connection--pairing-flow)
3. [Transport Protocols](#transport-protocols)
4. [Message Specification](#message-specification)
5. [Message Types Reference](#message-types-reference)
6. [Gesture Recognition](#gesture-recognition)
7. [Proximity Lock/Unlock](#proximity-lockunlock)
8. [Connection Quality Monitoring](#connection-quality-monitoring)
9. [Security & Authentication](#security--authentication)
10. [Error Handling](#error-handling)
11. [Protocol Flow Diagrams](#protocol-flow-diagrams)
12. [Android Implementation Guide](#android-implementation-guide)
13. [Go Server Implementation Guide](#go-server-implementation-guide)
14. [API Reference](#api-reference)
15. [Troubleshooting](#troubleshooting)

---

## Protocol Overview

The Air Mouse protocol is designed for **low-latency real-time control** between an Android device (client) and a desktop Go server. It supports multiple transport protocols, optional gesture recognition, and proximity-based auto-lock/unlock.

### Key Characteristics

| Property | Value |
|----------|-------|
| **Primary Transport** | WebSocket over TCP |
| **Fallback Transport** | Raw TCP |
| **Discovery Protocol** | UDP Broadcast |
| **Message Format** | JSON (UTF-8) |
| **Message Delimiter** | Newline (`\n`) |
| **Default Ports** | TCP: 8080, WebSocket: 8081, UDP: 8082 |
| **Heartbeat Interval** | 30 seconds |
| **Reconnection Strategy** | Exponential backoff (max 10 attempts) |

### Supported Platforms

| Platform | WebSocket | TCP | UDP Discovery | Bluetooth HID | USB |
|----------|-----------|-----|---------------|---------------|-----|
| **Android** | ✅ | ✅ | ✅ | ✅ (BLE) | ✅ (USB-C) |
| **Go Server** | ✅ | ✅ | ✅ | ✅ | ✅ (Linux) |
| **Windows Client** | ✅ | ✅ | ✅ | ❌ | ❌ |
| **macOS Client** | ✅ | ✅ | ✅ | ❌ | ❌ |

---

## Connection & Pairing Flow

### Standard Connection Flow

```
┌─────────────┐                    ┌─────────────┐
│   Android   │                    │   Go Server │
│    Client   │                    │             │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │ 1. WebSocket Upgrade Request     │
       │─────────────────────────────────>│
       │                                  │
       │ 2. 101 Switching Protocols       │
       │<─────────────────────────────────│
       │                                  │
       │ 3. Hello Message                 │
       │─────────────────────────────────>│
       │   {"type":"hello","payload":{    │
       │     "name":"Pixel 8","version":"3.0"}}│
       │                                  │
       │ 4. Welcome Message               │
       │<─────────────────────────────────│
       │   {"type":"welcome","payload":{  │
       │     "server":"AirMouse","version":"3.0"}}│
       │                                  │
       │ 5. Move/Click/Scroll Messages    │
       │<────────────────────────────────>│
       │                                  │
       │ 6. Periodic Ping/Pong (30s)      │
       │<────────────────────────────────>│
       │                                  │
```

### Pairing Flow (QR Code)

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Android   │         │   Server    │         │   Android   │
│    App      │         │   (QR)      │         │   Scanner   │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │
       │  1. Generate QR Code  │                       │
       │<──────────────────────│                       │
       │                       │                       │
       │  2. Scan QR Code      │                       │
       │──────────────────────────────────────────────>│
       │                       │                       │
       │  3. Extract URL & Token                       │
       │<──────────────────────────────────────────────│
       │                       │                       │
       │  4. Connect with Token                        │
       │──────────────────────>│                       │
       │   ws://server:8081/ws?token=<jwt>             │
       │                       │                       │
       │  5. Validate Token     │                       │
       │                       │                       │
       │  6. Connection Established                    │
       │<──────────────────────│                       │
       │                       │                       │
```

---

## Transport Protocols

### WebSocket (Primary)

**Endpoint:** `ws://<server-ip>:8081/ws`

**Features:**
- Full-duplex communication
- Built-in ping/pong (30s interval)
- Binary message support
- Automatic reconnection

**Connection Example:**
```kotlin
// Android
val client = OkHttpClient.Builder()
    .pingInterval(30, TimeUnit.SECONDS)
    .build()
val request = Request.Builder()
    .url("ws://192.168.1.100:8081/ws")
    .build()
client.newWebSocket(request, listener)
```

### TCP (Fallback)

**Endpoint:** `<server-ip>:8080`

**Features:**
- Simple text-based protocol
- Line-delimited messages
- No built-in heartbeat

**Connection Example:**
```kotlin
// Android
val socket = Socket().apply {
    connect(InetSocketAddress("192.168.1.100", 8080), 5000)
}
val writer = PrintWriter(socket.getOutputStream(), true)
val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
```

### UDP Discovery

**Endpoint:** Broadcast on port 8082

**Discovery Message:**
```
AIRMOUSE_DISCOVERY
```

**Response Format (Legacy):**
```
AIRMOUSE_SERVER:8080:AirMouse Pro
```

**Response Format (JSON):**
```json
{
  "type": "discovery_response",
  "port": 8080,
  "ip": "192.168.1.100",
  "name": "Air Mouse Pro",
  "version": "3.0"
}
```

---

## Message Specification

### Message Structure (Nested Format)

```json
{
  "type": "message_type",
  "payload": {
    "field1": "value1",
    "field2": "value2"
  },
  "id": "optional_request_id"
}
```

### Message Structure (Flat Format - Android Default)

```json
{
  "type": "message_type",
  "field1": "value1",
  "field2": "value2"
}
```

### Field Definitions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | String | **Yes** | Message type identifier |
| `payload` | Object | No | Nested payload (alternative to flat fields) |
| `id` | String/Number | No | Request ID for acknowledgment tracking |

---

## Message Types Reference

### 1. `move` – Cursor Movement

**Direction:** Client → Server

**Description:** Sends relative cursor movement delta.

**Flat Format (Android):**
```json
{
  "type": "move",
  "dx": 12.5,
  "dy": -3.2
}
```

**Nested Format:**
```json
{
  "type": "move",
  "payload": {
    "dx": 12.5,
    "dy": -3.2
  }
}
```

**Fields:**

| Field | Type | Range | Description |
|-------|------|-------|-------------|
| `dx` | Float | -32768 to 32767 | Horizontal movement (positive = right) |
| `dy` | Float | -32768 to 32767 | Vertical movement (positive = down) |

---

### 2. `click` – Mouse Click

**Direction:** Client → Server

**Description:** Simulates a mouse button click.

**Flat Format:**
```json
{
  "type": "click",
  "button": "left",
  "id": 12345
}
```

**Nested Format:**
```json
{
  "type": "click",
  "payload": {
    "button": "left"
  },
  "id": 12345
}
```

**Fields:**

| Field | Type | Values | Description |
|-------|------|--------|-------------|
| `button` | String | `left`, `right`, `middle` | Mouse button to click |
| `id` | Number/String | Optional | Request ID for ACK |

---

### 3. `doubleclick` – Double Click

**Direction:** Client → Server

**Description:** Simulates a double left click.

**Format:**
```json
{
  "type": "doubleclick",
  "id": 12346
}
```

---

### 4. `rightclick` – Right Click (Convenience)

**Direction:** Client → Server

**Description:** Simulates a right click.

**Format:**
```json
{
  "type": "rightclick",
  "id": 12347
}
```

---

### 5. `scroll` – Scroll Wheel

**Direction:** Client → Server

**Description:** Simulates scroll wheel movement.

**Flat Format:**
```json
{
  "type": "scroll",
  "delta": 3,
  "id": 12348
}
```

**Nested Format:**
```json
{
  "type": "scroll",
  "payload": {
    "delta": 3
  },
  "id": 12348
}
```

**Fields:**

| Field | Type | Range | Description |
|-------|------|-------|-------------|
| `delta` | Integer | -127 to 127 | Positive = scroll up, negative = scroll down |

---

### 6. `hello` – Device Identification

**Direction:** Client → Server

**Description:** Identifies the client device to the server.

**Flat Format:**
```json
{
  "type": "hello",
  "name": "Pixel 8 Pro",
  "version": "3.0",
  "device": "Google Pixel 8",
  "android_version": "14"
}
```

**Nested Format:**
```json
{
  "type": "hello",
  "payload": {
    "name": "Pixel 8 Pro",
    "version": "3.0",
    "device": "Google Pixel 8",
    "android_version": "14"
  }
}
```

**Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | User-defined device name |
| `version` | String | Yes | App version (e.g., "3.0") |
| `device` | String | No | Device model |
| `android_version` | String | No | Android OS version |

**Server Response:** `welcome`

---

### 7. `gesture` – Gesture Recognition

**Direction:** Client → Server

**Description:** Sends a recognised gesture from the Android ML model.

**Format:**
```json
{
  "type": "gesture",
  "payload": {
    "gesture": "ThumbsUp",
    "confidence": 0.92
  }
}
```

**Fields:**

| Field | Type | Range | Description |
|-------|------|-------|-------------|
| `gesture` | String | See [Gesture Types](#gesture-types) | Recognised gesture name |
| `confidence` | Float | 0.0 to 1.0 | Confidence score (≥0.7 recommended) |

---

### 8. `proximity` – Proximity Update

**Direction:** Client → Server

**Description:** Sends estimated distance to the computer for auto lock/unlock.

**Format:**
```json
{
  "type": "proximity",
  "payload": {
    "device_id": "abc123",
    "is_near": true,
    "distance": 1.23
  }
}
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `device_id` | String | Unique device identifier (Android ID) |
| `is_near` | Boolean | True if distance < near_threshold |
| `distance` | Float | Estimated distance in meters |

---

### 9. `control` – Control Command

**Direction:** Client → Server

**Description:** Sends system control commands.

**Format:**
```json
{
  "type": "control",
  "payload": {
    "command": "pause_movement"
  }
}
```

**Supported Commands:**

| Command | Description |
|---------|-------------|
| `pause_movement` | Pause cursor movement |
| `resume_movement` | Resume cursor movement |
| `lock_screen` | Lock the computer screen |
| `unlock_screen` | Unlock the computer screen |
| `calibrate` | Recalibrate sensors |
| `reset` | Reset movement state |
| `play_pause` | Play/Pause media |
| `next_track` | Next track |
| `prev_track` | Previous track |
| `volume_up` | Increase volume |
| `volume_down` | Decrease volume |
| `mute` | Mute audio |
| `show_desktop` | Show desktop |
| `task_view` | Open task view |
| `switch_window` | Switch between windows |
| `window_maximize` | Maximize window |
| `window_minimize` | Minimize window |
| `window_close` | Close window |
| `browser_back` | Browser back |
| `browser_forward` | Browser forward |
| `browser_refresh` | Refresh page |
| `browser_home` | Go to home page |
| `zoom_in` | Zoom in |
| `zoom_out` | Zoom out |

---

### 10. `ping` / `pong` – Heartbeat

**Direction:** Bidirectional

**Description:** Keep-alive messages to detect connection drops.

**Ping (Client → Server):**
```json
{
  "type": "ping"
}
```

**Pong (Server → Client):**
```json
{
  "type": "pong"
}
```

---

### 11. `welcome` – Server Welcome

**Direction:** Server → Client

**Description:** Sent after successful `hello` message.

**Format:**
```json
{
  "type": "welcome",
  "payload": {
    "server": "Air Mouse Pro",
    "version": "3.0",
    "id": "client-assigned-id"
  }
}
```

---

### 12. `ack` – Acknowledgment

**Direction:** Server → Client

**Description:** Acknowledges receipt of a command with an ID.

**Format:**
```json
{
  "type": "ack",
  "id": "12345",
  "status": "ok",
  "message": "optional message"
}
```

---

### 13. `error` – Error Message

**Direction:** Server → Client

**Description:** Reports an error condition.

**Format:**
```json
{
  "type": "error",
  "payload": {
    "code": 400,
    "message": "Invalid message format"
  }
}
```

---

## Gesture Recognition

### Gesture Types

| Gesture | Default Action | Confidence Threshold |
|---------|----------------|---------------------|
| `ThumbsUp` | Play/Pause | 0.7 |
| `ThumbsDown` | Stop | 0.7 |
| `SwipeLeft` | Previous Track | 0.6 |
| `SwipeRight` | Next Track | 0.6 |
| `SwipeUp` | Volume Up | 0.6 |
| `SwipeDown` | Volume Down | 0.6 |
| `CircleCW` | Volume Up | 0.65 |
| `CircleCCW` | Volume Down | 0.65 |
| `PinchIn` | Zoom Out | 0.7 |
| `PinchOut` | Zoom In | 0.7 |
| `DoubleTap` | Play/Pause | 0.65 |
| `LongPress` | Right Click | 0.7 |
| `Shake` | Undo/Reset | 0.75 |
| `Peace` | Lock Screen | 0.7 |
| `Fist` | Mute | 0.7 |
| `ZoomIn` | Zoom In | 0.7 |
| `ZoomOut` | Zoom Out | 0.7 |

### Gesture Pipeline

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Sensors    │───▶│  Gesture     │───▶│  Confidence  │───▶│  Send to     │
│  (Gyro/Accel)│    │  Detection   │    │   Filter     │    │   Server     │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
                                                                      │
                                                                      ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Action     │◀───│  Gesture     │◀───│  Confidence  │◀───│  Message     │
│  Execution   │    │   Mapping    │    │   Check      │    │  Reception   │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

---

## Proximity Lock/Unlock

### Overview

The proximity feature uses Bluetooth RSSI (Received Signal Strength Indicator) to estimate the distance between the phone and the computer. When the user walks away beyond the `far_threshold`, the screen locks automatically. When they return within the `near_threshold`, the screen unlocks.

### Distance Calculation

The distance is calculated using the path loss model:

```
distance = 10^((TxPower - RSSI) / (10 * n))
```

Where:
- `TxPower` = RSSI at 1 meter (calibrated, typically -59 dBm)
- `n` = Path loss exponent (2.0 to 4.0, default 2.5)

### Thresholds

| Threshold | Default | Description |
|-----------|---------|-------------|
| `near_threshold` | 1.5 m | Distance to unlock (when approaching) |
| `far_threshold` | 3.0 m | Distance to lock (when walking away) |

### Hysteresis

To prevent rapid toggling, the system uses hysteresis:

```
if (isNear) {
    // Already near - need to go beyond far_threshold to change
    isNear = distance < far_threshold
} else {
    // Currently far - need to go within near_threshold to change
    isNear = distance < near_threshold
}
```

### Proximity Flow

```
┌─────────────┐                    ┌─────────────┐
│   Android   │                    │   Go Server │
│    Client   │                    │             │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │ 1. RSSI Measurement              │
       │    (every 1 second)              │
       │                                  │
       │ 2. Distance Calculation          │
       │                                  │
       │ 3. Proximity Message             │
       │─────────────────────────────────>│
       │   {"type":"proximity",           │
       │    "payload":{                   │
       │      "is_near":false,            │
       │      "distance":4.2              │
       │    }}                            │
       │                                  │
       │ 4. Check Thresholds              │
       │    distance > far_threshold      │
       │                                  │
       │ 5. Lock Screen                   │
       │    (platform-specific)           │
       │                                  │
       │ 6. Optional: Lock Ack            │
       │<─────────────────────────────────│
       │                                  │
```

---

## Connection Quality Monitoring

### Quality Metrics

| Metric | Description | Good Range |
|--------|-------------|------------|
| **Ping** | Round-trip latency | < 50 ms |
| **Jitter** | Latency variance | < 10 ms |
| **Packet Loss** | Lost message percentage | < 1% |
| **RSSI** | Signal strength (Bluetooth) | > -60 dBm |

### Quality Categories

| Category | Ping | RSSI | Color |
|----------|------|------|-------|
| **Excellent** | < 30 ms | > -50 dBm | 🟢 Green |
| **Good** | 30-60 ms | -50 to -60 dBm | 🟢 Green |
| **Fair** | 60-100 ms | -60 to -70 dBm | 🟡 Yellow |
| **Poor** | 100-200 ms | -70 to -80 dBm | 🟠 Orange |
| **Very Poor** | > 200 ms | < -80 dBm | 🔴 Red |

### Quality Message (Server → Client)

```json
{
  "type": "quality",
  "payload": {
    "ping": 45,
    "jitter": 5,
    "packet_loss": 0.02,
    "signal_strength": "GOOD"
  }
}
```

---

## Security & Authentication

### Authentication Methods

#### 1. JWT Token (Recommended)

The server generates a JWT token embedded in the QR code.

**QR Code Format:**
```
airmouse://pair?token=<jwt>&ws=ws://<server-ip>:<port>/ws
```

**WebSocket Connection:**
```
ws://192.168.1.100:8081/ws?token=<jwt>
```

#### 2. Pre-shared Token

Configure static tokens in `config.json`:

```json
{
  "security": {
    "auth_enabled": true,
    "auth_tokens": ["token1", "token2"]
  }
}
```

#### 3. No Authentication (Development)

Set `auth_enabled: false` in configuration.

### Token Validation (Server Side)

```go
func (s *Server) validateToken(token string) bool {
    // Parse JWT
    claims, err := jwt.Parse(token, func(t *jwt.Token) (interface{}, error) {
        return s.secret, nil
    })
    if err != nil {
        return false
    }
    
    // Check expiry
    exp, ok := claims["exp"].(float64)
    if !ok || time.Now().Unix() > int64(exp) {
        return false
    }
    
    return true
}
```

---

## Error Handling

### Error Codes

| Code | Name | Description |
|------|------|-------------|
| 400 | `BAD_REQUEST` | Malformed JSON or missing required fields |
| 401 | `UNAUTHORIZED` | Invalid or expired authentication token |
| 404 | `NOT_FOUND` | Endpoint not found |
| 429 | `TOO_MANY_REQUESTS` | Rate limit exceeded |
| 500 | `INTERNAL_ERROR` | Server internal error |
| 503 | `SERVICE_UNAVAILABLE` | Server temporarily unavailable |

### Error Response Format

```json
{
  "type": "error",
  "payload": {
    "code": 400,
    "message": "Invalid message format: missing 'type' field",
    "details": "Expected 'type' to be one of: move, click, scroll, hello, gesture, proximity, control"
  }
}
```

### Client Error Handling Strategy

```kotlin
connectionManager.onError = { error ->
    when {
        error.contains("401") -> {
            // Token expired - re-pair
            showPairingDialog()
        }
        error.contains("429") -> {
            // Rate limited - reduce message frequency
            reduceMoveRate()
        }
        error.contains("500") -> {
            // Server error - retry with backoff
            scheduleReconnectWithBackoff()
        }
        else -> {
            // Generic error - show notification
            showToast("Connection error: $error")
        }
    }
}
```

---

## Protocol Flow Diagrams

### Complete Message Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MESSAGE FLOW DIAGRAM                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Client (Android)                      Server (Go)                          │
│  ────────────────                      ───────────                          │
│                                                                             │
│  ┌─────────────────┐                   ┌─────────────────┐                  │
│  │ 1. WebSocket    │──────────────────▶│                 │                  │
│  │    Upgrade      │                   │                 │                  │
│  └─────────────────┘                   │                 │                  │
│                                         │                 │                  │
│  ┌─────────────────┐                   │                 │                  │
│  │ 2. Hello        │──────────────────▶│                 │                  │
│  └─────────────────┘                   │                 │                  │
│                                         │                 │                  │
│                                         │ ┌─────────────┐ │                  │
│                                         │ │ Validate    │ │                  │
│                                         │ │ Client      │ │                  │
│                                         │ └─────────────┘ │                  │
│                                         │                 │                  │
│  ┌─────────────────┐                   │                 │                  │
│  │ 3. Welcome      │◀──────────────────│                 │                  │
│  └─────────────────┘                   │                 │                  │
│                                         │                 │                  │
│  ═══════════════════════════════════════════════════════════════════════════ │
│                          NORMAL OPERATION                                    │
│  ═══════════════════════════════════════════════════════════════════════════ │
│                                         │                 │                  │
│  ┌─────────────────┐                   │                 │                  │
│  │ 4. Move         │──────────────────▶│ ┌─────────────┐ │                  │
│  └─────────────────┘                   │ │ Move Mouse  │ │                  │
│                                         │ └─────────────┘ │                  │
│                                         │                 │                  │
│  ┌─────────────────┐                   │                 │                  │
│  │ 5. Click        │──────────────────▶│ ┌─────────────┐ │                  │
│  └─────────────────┘                   │ │ Click       │ │                  │
│                                         │ └─────────────┘ │                  │
│                                         │                 │                  │
│  ┌─────────────────┐                   │ ┌─────────────┐ │                  │
│  │ 6. ACK (opt)    │◀──────────────────│ │ Send ACK    │ │                  │
│  └─────────────────┘                   │ └─────────────┘ │                  │
│                                         │                 │                  │
│  ┌─────────────────┐┌─────────────────┐                   │                 │                  │
│ 7. Gesture      │──────────────────▶│ ┌─────────────┐ │                  │
│                 │                   │ │ Map Gesture │ │                  │
│                 │                   │ │ to Action   │ │                  │
│                 │                   │ └─────────────┘ │                  │
│                 │                   │                 │                  │
│                 │                   │ ┌─────────────┐ │                  │
│                 │                   │ │ Execute     │ │                  │
│                 │                   │ │ System      │ │                  │
│                 │                   │ │ Action      │ │                  │
│                 │                   │ └─────────────┘ │                  │
│                 │                   │                 │                  │
│  ┌─────────────────┐                │                 │                  │
│  │ 8. Proximity   │────────────────▶│ ┌─────────────┐ │                  │
│  │    Update      │                 │ │ Check       │ │                  │
│  └─────────────────┘                 │ │ Thresholds  │ │                  │
│                                      │ └─────────────┘ │                  │
│                                      │                 │                  │
│                                      │ ┌─────────────┐ │                  │
│                                      │ │ Lock/Unlock │ │                  │
│                                      │ │ Screen      │ │                  │
│                                      │ └─────────────┘ │                  │
│                                      │                 │                  │
│  ┌─────────────────┐                 │                 │                  │
│  │ 9. Ping         │────────────────▶│ ┌─────────────┐ │                  │
│  │    (every 30s)  │                 │ │ Send Pong   │ │                  │
│  └─────────────────┘                 │ └─────────────┘ │                  │
│                                      │                 │                  │
│  ┌─────────────────┐                 │                 │                  │
│  │ 10. Pong        │◀────────────────│                 │                  │
│  └─────────────────┘                 │                 │                  │
│                                      │                 │                  │
│  ═══════════════════════════════════════════════════════════════════════════ │
│                          RECONNECTION FLOW                                   │
│  ═══════════════════════════════════════════════════════════════════════════ │
│                                      │                 │                  │
│  ┌─────────────────┐                 │                 │                  │
│  │ Connection Lost │                 │                 │                  │
│  └─────────────────┘                 │                 │                  │
│                                      │                 │                  │
│  ┌─────────────────┐                 │                 │                  │
│  │ Exponential     │                 │                 │                  │
│  │ Backoff Delay   │                 │                 │                  │
│  │ (1s,2s,4s,8s)   │                 │                 │                  │
│  └─────────────────┘                 │                 │                  │
│                                      │                 │                  │
│  ┌─────────────────┐                 │                 │                  │
│  │ 11. Reconnect   │────────────────▶│ ┌─────────────┐ │                  │
│  │     Attempt     │                 │ │ Reconnect   │ │                  │
│  └─────────────────┘                 │ │ Accepted    │ │                  │
│                                      │ └─────────────┘ │                  │
│                                      │                 │                  │
│  ┌─────────────────┐                 │                 │                  │
│  │ 12. Re-Hello    │────────────────▶│                 │                  │
│  └─────────────────┘                 │                 │                  │
│                                      │                 │                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Android Implementation Guide

### Dependencies (build.gradle)

```groovy
dependencies {
    // WebSocket
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Dependency Injection
    implementation 'com.google.dagger:hilt-android:2.48'
    
    // TensorFlow Lite (for gesture recognition)
    implementation 'org.tensorflow:tensorflow-lite:2.14.0'
    
    // Bluetooth
    implementation 'androidx.core:core-ktx:1.12.0'
}
```

### ConnectionManager Usage Example

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var connectionManager: ConnectionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe connection status
        lifecycleScope.launch {
            connectionManager.connectionStatus.collect { status ->
                when (status) {
                    ConnectionManager.ConnectionStatus.CONNECTED -> {
                        showConnected()
                        connectionManager.sendHello(Build.MODEL, "3.0")
                    }
                    ConnectionManager.ConnectionStatus.CONNECTING -> showConnecting()
                    ConnectionManager.ConnectionStatus.DISCONNECTED -> showDisconnected()
                    ConnectionManager.ConnectionStatus.ERROR -> showError()
                    else -> {}
                }
            }
        }
        
        // Observe connection quality
        lifecycleScope.launch {
            connectionManager.connectionQuality.collect { quality ->
                updateSignalIcon(quality.level())
                updateLatencyDisplay("${quality.ping} ms")
            }
        }
        
        // Send movement (from sensor callback)
        fun onSensorChanged(dx: Float, dy: Float) {
            connectionManager.sendMove(dx, dy)
        }
        
        // Send click
        fun onClick() {
            connectionManager.sendClick("left")
        }
        
        // Send gesture
        fun onGestureDetected(gesture: String, confidence: Float) {
            if (confidence > 0.7f) {
                connectionManager.sendGesture(gesture, confidence)
            }
        }
        
        // Send proximity update
        fun onProximityChanged(distance: Float, isNear: Boolean) {
            connectionManager.sendProximity(isNear, distance)
        }
    }
}
```

### Hello Message with Device Info

```kotlin
fun sendHello() {
    val deviceInfo = JSONObject().apply {
        put("type", "hello")
        put("payload", JSONObject().apply {
            put("name", Build.MODEL)
            put("version", BuildConfig.VERSION_NAME)
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("android_version", Build.VERSION.RELEASE)
        })
    }
    connectionManager.send(deviceInfo.toString())
}
```

---

## Go Server Implementation Guide

### Protocol Server Structure

```go
type ProtocolServer struct {
    tcpServer    *TCPServer
    wsServer     *WebSocketServer
    udpServer    *UDPServer
    mouseCtrl    MouseController
    deviceMgr    *DeviceManager
    authMgr      *AuthManager
}

func (s *ProtocolServer) Start() error {
    // Start WebSocket server
    go s.wsServer.Start()
    
    // Start TCP fallback
    go s.tcpServer.Start()
    
    // Start UDP discovery
    go s.udpServer.Start()
    
    return nil
}
```

### Message Handler

```go
func (h *Handler) processMessage(client *Client, msgType string, payload map[string]interface{}, id *string) {
    switch msgType {
    case "move":
        dx := getFloat(payload, "dx")
        dy := getFloat(payload, "dy")
        h.mouse.Move(dx, dy)
        
    case "click":
        button := getString(payload, "button", "left")
        h.mouse.Click(button)
        h.sendAck(client, id)
        
    case "gesture":
        gesture := getString(payload, "gesture", "")
        confidence := getFloat(payload, "confidence")
        if confidence > 0.7 {
            h.executeGesture(gesture)
        }
        
    case "proximity":
        isNear := getBool(payload, "is_near")
        distance := getFloat(payload, "distance")
        h.proximityMgr.ProcessUpdate(isNear, distance)
        
    case "control":
        command := getString(payload, "command", "")
        h.executeControl(command)
        
    case "hello":
        name := getString(payload, "name", "")
        client.SetName(name)
        h.sendWelcome(client)
    }
}
```

### Welcome Message

```go
func (h *Handler) sendWelcome(client *Client) {
    welcome := map[string]interface{}{
        "type": "welcome",
        "payload": map[string]string{
            "server":  h.cfg.ServerName,
            "version": h.cfg.Version,
            "id":      client.ID,
        },
    }
    data, _ := json.Marshal(welcome)
    client.Send <- append(data, '\n')
}
```

---

## API Reference

### WebSocket Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/ws` | WebSocket | Primary WebSocket connection |
| `/ws?token=<jwt>` | WebSocket | Authenticated WebSocket connection |

### HTTP Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/api/status` | GET | Server status |
| `/api/stats` | GET | Statistics |
| `/api/devices` | GET | Connected devices list |
| `/api/qrcode` | GET | Generate pairing QR code |
| `/api/config` | GET/POST | Get/update configuration |
| `/api/start` | POST | Start server |
| `/api/stop` | POST | Stop server |

### Rate Limits

| Message Type | Rate Limit | Window |
|--------------|------------|--------|
| `move` | 120 msg/sec | 1 second |
| `click` | 20 msg/sec | 1 second |
| `scroll` | 20 msg/sec | 1 second |
| `gesture` | 10 msg/sec | 1 second |
| `proximity` | 5 msg/sec | 1 second |
| `control` | 10 msg/sec | 1 second |

---

## Troubleshooting

### Common Issues and Solutions

| Issue | Possible Cause | Solution |
|-------|----------------|----------|
| **Connection refused** | Server not running | Start server with `./airmouse-server` |
| | Firewall blocking port | Open ports 8080, 8081, 8082 |
| | Wrong IP address | Verify IP with `ipconfig` / `ifconfig` |
| **WebSocket handshake fails** | Version mismatch | Ensure server version ≥ 3.0 |
| | Invalid token | Regenerate pairing QR code |
| **High latency** | Network congestion | Use Ethernet or 5GHz WiFi |
| | Server overload | Reduce client count or upgrade hardware |
| **Gestures not recognized** | Low confidence threshold | Lower threshold or retrain model |
| | Insufficient training data | Record 10+ samples per gesture |
| **Proximity not working** | Bluetooth disabled | Enable Bluetooth |
| | Wrong device paired | Verify MAC address in settings |
| | Calibration needed | Run calibration routine |

### Debugging Commands

```bash
# Check if server is running
curl http://localhost:8081/health

# Get server status
curl http://localhost:8081/api/status

# List connected devices
curl http://localhost:8081/api/devices

# Test WebSocket connection (using wscat)
npm install -g wscat
wscat -c ws://localhost:8081/ws

# Monitor UDP discovery
sudo tcpdump -i any port 8082 -n

# Check open ports
lsof -i :8080
lsof -i :8081
lsof -i :8082
```

### Logging

**Android Logcat:**
```bash
adb logcat -s ConnectionManager:V SensorService:V WebSocketManager:V
```

**Server Logs:**
```bash
tail -f ~/.config/airmouse/airmouse.log
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 3.0.0 | 2025-01-15 | Initial protocol specification |
| | | Added WebSocket as primary transport |
| | | Added gesture recognition support |
| | | Added proximity lock/unlock |
| | | Added UDP discovery |

---

## Appendix

### A. Message Size Limits

| Message Type | Max Size (bytes) |
|--------------|------------------|
| `move` | 128 |
| `click` | 64 |
| `gesture` | 256 |
| `proximity` | 128 |
| `control` | 64 |
| `hello` | 256 |

### B. Recommended Sensor Rates

| Sensor | Recommended Rate | Purpose |
|--------|------------------|---------|
| Gyroscope | 100 Hz | Movement detection |
| Accelerometer | 100 Hz | Orientation detection |
| Rotation Vector | 60 Hz | Smooth orientation |
| Magnetometer | 20 Hz | Heading correction |

### C. Bluetooth RSSI to Distance Mapping (Approximate)

| RSSI (dBm) | Distance (meters) | Quality |
|------------|-------------------|---------|
| -30 to -40 | 0.2 - 0.5 | Excellent |
| -40 to -55 | 0.5 - 1.5 | Good |
| -55 to -65 | 1.5 - 3.0 | Fair |
| -65 to -75 | 3.0 - 5.0 | Poor |
| < -75 | > 5.0 | Very Poor |

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-15  
**Protocol Version:** 3.0  
**Maintainer:** Air Mouse Team <support@airmouse.io>