# Air Mouse Android ↔ Go Communication

This document describes the live communication path between the Android app and the Go desktop server. It is written to match the current codebase and focuses on the real data flow used for cursor movement, click, scroll, pairing, acknowledgements, and status updates.

## 1. Purpose

The Android app reads motion sensors, detects gestures, and sends control messages to the desktop server. The Go server receives those messages, validates them, and converts them into native mouse actions and device/session state updates.

The communication layer must support:
- low-latency cursor movement
- reliable click and scroll actions
- initial device identification
- pairing and connection status
- live status and acknowledgement feedback

## 2. Transports

The codebase currently supports these transport paths:

- `WebSocket` as the primary channel
- `TCP` as a fallback channel
- `UDP` for discovery

The Go UI exposes the active ports and protocol state, while the Android app chooses the endpoint during pairing or manual configuration.

## 3. Connection Flow

### 3.1 Discovery

The desktop server can advertise itself on the local network using UDP discovery.

Typical discovery flow:
1. The Android app sends a discovery packet on the configured UDP port.
2. The Go server responds with its address, server name, and version.
3. The Android app uses that information to populate the connection screen or QR pairing result.

### 3.2 Pairing

Pairing is usually done by QR code.

The QR payload may include:
- server IP
- TCP port
- WebSocket URL
- server name
- version
- optional token

Example:

```text
airmouse://pair?ip=192.168.1.10&port=8080&ws=ws://192.168.1.10:8081/ws&name=AirMouse&version=3.0&protocol=WEBSOCKET
```

### 3.3 Session startup

1. Android connects to the chosen server transport.
2. Android sends `hello`.
3. Go responds with `welcome`.
4. The Android app treats the connection as established after the welcome response.

## 4. Message Format

All messages are JSON objects.

The server accepts two shapes:

- flat messages:

```json
{"type":"move","dx":12.5,"dy":-3.2}
```

- nested messages:

```json
{"type":"move","payload":{"dx":12.5,"dy":-3.2}}
```

This dual support exists for compatibility and helps the Android and Go sides stay in sync even if one side emits the richer format.

## 5. Message Types

### 5.1 `hello`

Sent by Android after a connection is opened.

```json
{
  "type": "hello",
  "payload": {
    "name": "Pixel 8",
    "version": "3.0",
    "device": "Google Pixel 8",
    "android_version": "14",
    "protocol": "WEBSOCKET",
    "transport": "websocket"
  }
}
```

Purpose:
- identify the client device
- expose app version
- help the server update the Devices view

### 5.2 `welcome`

Sent by Go after a valid `hello`.

```json
{
  "type": "welcome",
  "payload": {
    "server": "AirMouse Pro",
    "version": "3.0"
  }
}
```

Purpose:
- confirms that the server accepted the client
- completes the connection handshake

### 5.3 `move`

Sent continuously for cursor movement.

```json
{
  "type": "move",
  "dx": 12.5,
  "dy": -3.2
}
```

Accepted aliases:
- `dx` / `dy`
- `DeltaX` / `DeltaY`
- `deltaX` / `deltaY`

Semantics:
- positive `dx` moves cursor right
- negative `dx` moves cursor left
- positive `dy` moves cursor down
- negative `dy` moves cursor up

Important:
- move messages are best-effort and are not ACKed
- they should be sent frequently but smoothed and filtered on the Android side

### 5.4 `click`

Sent for a left click or any other button click that should be reliable.

```json
{
  "type": "click",
  "button": "left",
  "id": "msg_12"
}
```

Accepted button values:
- `left`
- `right`
- `middle`

The Go server returns:

```json
{"type":"ack","id":"msg_12"}
```

### 5.5 `doubleclick`

```json
{
  "type": "doubleclick",
  "id": "msg_13"
}
```

This is handled as a reliable action and should be ACKed.

### 5.6 `rightclick`

```json
{
  "type": "rightclick",
  "id": "msg_14"
}
```

This is also reliable and should be ACKed.

### 5.7 `scroll`

```json
{
  "type": "scroll",
  "delta": -3,
  "id": "msg_15"
}
```

Accepted aliases:
- `delta`
- `Scroll`
- `scroll`

Semantics:
- positive delta scrolls up
- negative delta scrolls down

Scroll messages are reliable and should be ACKed.

### 5.8 `gesture`

```json
{
  "type": "gesture",
  "payload": {
    "gesture": "ThumbsUp",
    "confidence": 0.92
  }
}
```

Purpose:
- send higher-level gesture detections from Android
- optionally trigger media, presentation, or smart-home actions

### 5.9 `proximity`

```json
{
  "type": "proximity",
  "payload": {
    "device_id": "android-id-123",
    "is_near": true,
    "distance": 1.2
  }
}
```

Purpose:
- proximity-based lock or wake behavior
- device presence tracking

### 5.10 `control`

```json
{
  "type": "control",
  "payload": {
    "command": "pause_movement"
  }
}
```

Supported commands in the current code:
- `pause_movement`
- `resume_movement`
- additional system commands may be added later

### 5.11 `ping` and `pong`

Used for heartbeat and connection quality.

Android or Go may send:

```json
{"type":"ping"}
```

The receiver replies with:

```json
{"type":"pong"}
```

## 6. Reliability Rules

### 6.1 Reliable messages

The following actions are treated as reliable:
- `click`
- `doubleclick`
- `rightclick`
- `scroll`

Those messages may include an `id` and expect an `ack`.

### 6.2 Best-effort messages

The following actions are typically best-effort:
- `move`
- `gesture`
- `proximity`
- `ping`
- `pong`
- `control`

### 6.3 ACK behavior

When the Go server receives a reliable message with an `id`, it sends back:

```json
{"type":"ack","id":"same-id"}
```

Android removes the message from its pending queue after the ACK arrives.

If no ACK arrives in time, the Android client can resend the message.

## 7. Android Client Behavior

The Android app currently performs these steps:

1. Reads sensors for orientation and motion.
2. Applies filtering and calibration.
3. Maps motion to cursor deltas.
4. Detects click and scroll gestures.
5. Sends `move`, `click`, `doubleclick`, `rightclick`, and `scroll`.
6. Sends `hello` immediately after connection.
7. Waits for `welcome` to finalize the connection.
8. Tracks ACKs for reliable actions.

### 7.1 Cursor movement

The Android side computes smoothed `dx` and `dy` values and sends them at a high rate.

Best practice:
- filter sensor noise
- apply a dead zone
- avoid sending tiny jitter updates
- clamp output to safe limits

### 7.2 Click and scroll

Clicks and scrolls are triggered by gesture detection rather than raw sensor drift.

Recommended behavior:
- use thresholds
- add cooldowns
- make scroll directional
- keep click and scroll mutually exclusive when possible

## 8. Go Server Behavior

The Go side handles the message stream in two protocol implementations:

- `internal/protocol/websocket`
- `internal/protocol/tcp`

Current behavior:

- `move` is forwarded directly to the mouse controller
- `click`, `doubleclick`, `rightclick`, and `scroll` are forwarded and ACKed
- `hello` updates the device name and triggers `welcome`
- `ping` returns `pong`
- `control` toggles movement pause/resume

The Go protocol layer now accepts both canonical and alias motion fields:

- `dx`, `dy`
- `DeltaX`, `DeltaY`
- `deltaX`, `deltaY`
- `delta`, `Scroll`, `scroll`

This prevents the Android app and Go app from drifting apart if one side evolves faster than the other.

## 9. Example Message Exchange

### 9.1 Pairing and handshake

```text
Android -> Go: connect to ws://192.168.1.10:8081/ws
Android -> Go: {"type":"hello","payload":{"name":"Pixel 8","version":"3.0"}}
Go -> Android: {"type":"welcome","payload":{"server":"AirMouse Pro","version":"3.0"}}
```

### 9.2 Cursor movement

```text
Android -> Go: {"type":"move","dx":4.2,"dy":-1.7}
Go -> OS: move cursor
```

### 9.3 Click

```text
Android -> Go: {"type":"click","button":"left","id":"msg_21"}
Go -> Android: {"type":"ack","id":"msg_21"}
Go -> OS: left click
```

### 9.4 Scroll

```text
Android -> Go: {"type":"scroll","delta":-3,"id":"msg_22"}
Go -> Android: {"type":"ack","id":"msg_22"}
Go -> OS: scroll down
```

## 10. State Mapping

### Android app states

- disconnected
- connecting
- connected
- reconnecting
- error

### Go server states

- stopped
- starting
- running
- stopping
- error

The UI should only show `connected` after:
- the socket is open
- `hello` is sent
- `welcome` is received

That is the cleanest definition of a real successful session.

## 11. Why the protocol is tolerant

The code supports both nested and flat JSON to reduce breakage.

This matters because:
- Android code may send richer payloads
- Go handlers may read simpler fields
- future changes should not break pairing or control

This tolerance is intentional, but the canonical format should remain:

```json
{
  "type": "move",
  "dx": 1.5,
  "dy": -0.8
}
```

for movement, and

```json
{
  "type": "click",
  "button": "left",
  "id": "msg_1"
}
```

for reliable actions.

## 12. Implementation Notes

- Use WebSocket for the normal app experience.
- Use TCP only if you need a simpler fallback path.
- Keep `move` packets lightweight.
- Keep click and scroll reliable.
- Keep the server tolerant to flat and nested payloads.
- Keep the Android UI showing the real connection state, not just “ready”.

## 13. Summary Table

| Message | Direction | Reliable | Main Fields | Notes |
|---|---|---:|---|---|
| `hello` | Android → Go | Yes | `name`, `version` | Starts the session |
| `welcome` | Go → Android | Yes | `server`, `version` | Confirms pairing |
| `move` | Android → Go | No | `dx`, `dy` | Cursor movement |
| `click` | Android → Go | Yes | `button`, `id` | ACK expected |
| `doubleclick` | Android → Go | Yes | `id` | ACK expected |
| `rightclick` | Android → Go | Yes | `id` | ACK expected |
| `scroll` | Android → Go | Yes | `delta`, `id` | ACK expected |
| `gesture` | Android → Go | No | `gesture`, `confidence` | High-level actions |
| `proximity` | Android → Go | No | `device_id`, `is_near`, `distance` | Presence / lock logic |
| `control` | Android → Go | No | `command` | Pause/resume etc. |
| `ping` | Either direction | No | - | Heartbeat |
| `pong` | Either direction | No | - | Heartbeat response |
| `ack` | Go → Android | Yes | `id` | Confirms reliable action |

## 14. Recommended Canonical Payloads

### Cursor

```json
{"type":"move","dx":2.0,"dy":-1.0}
```

### Click

```json
{"type":"click","button":"left","id":"msg_100"}
```

### Scroll

```json
{"type":"scroll","delta":-2,"id":"msg_101"}
```

### Hello

```json
{"type":"hello","payload":{"name":"Pixel 8","version":"3.0"}}
```

### Welcome

```json
{"type":"welcome","payload":{"server":"AirMouse Pro","version":"3.0"}}
```

---

If you want, I can also add a second markdown file that is more “assignment report” style, with:
- architecture diagram
- sequence diagram
- state machine
- and a compact table of all message examples for submission.
