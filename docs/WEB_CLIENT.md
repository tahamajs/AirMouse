# AirMouse iOS & Mobile Web Client Guide

This document describes the iOS Safari and mobile web client for AirMouse, operating under the frozen **Protocol V2** specification.

---

## 1. Overview & Architecture

The mobile web client (`web-client/`) allows any iOS device (iPhone/iPad) or mobile browser to function as an AirMouse controller without installing a native app.

### Key Capabilities:
- **Zero-Install**: Runs directly inside iOS Safari or mobile Chrome/Firefox.
- **Protocol V2 Compliant**: Sends structured JSON wire frames with `v: 2`, monotonically increasing `seq`, millisecond timestamps `ts`, correlation `id`, and nested `payload`.
- **Motion Control (Air Gyroscope)**: Uses the HTML5 `DeviceOrientation` event to capture physical pitch and roll (`beta`, `gamma`), moving the desktop cursor in real-time.
- **iOS 13+ Permission Flow**: Includes explicit user gesture prompt (`DeviceOrientationEvent.requestPermission()`) required by Apple WebKit.
- **Touchpad Gestures**:
  - **1-Finger Drag**: Relative cursor translation (`move`).
  - **1-Finger Tap**: Left mouse click (`click`, `button: "left"`).
  - **2-Finger Tap**: Right mouse click (`click`, `button: "right"`).
  - **2-Finger Drag**: Vertical scroll (`scroll`, `delta`).
- **Tactile Feedback**: Haptic pulses on tap/click using the Web Vibration API where supported.
- **Secure Transport**: Direct communication over Secure WebSockets (`wss://`).

---

## 2. Directory Structure

```text
AirMouse/
├── web-client/                   # Standalone Web Client source
│   ├── index.html                # Responsive mobile UI & Touchpad canvas
│   ├── style.css                 # Dark-mode glassmorphic mobile styling
│   └── app.js                    # Protocol v2 client, Touchpad, & Gyroscope logic
└── pc/airmouse_go_new/web/
    └── client/                   # Embedded into Go server binary (web.WebFS)
        ├── index.html
        ├── style.css
        └── app.js
```

---

## 3. Server Endpoints & Routing

When the AirMouse Go server runs, the mobile web client is hosted alongside the server dashboard:

| URL Endpoint | Description |
|---|---|
| `https://<server-ip>:8081/client/` or `/mobile` | Mobile Web Client UI |
| `wss://<server-ip>:8081/ws` | Protocol V2 WebSocket endpoint |
| `https://<server-ip>:8081/` | Desktop Admin Dashboard |

---

## 4. Setting Up & Testing on iOS Safari

### Step 1: Start the AirMouse Go Server
Ensure TLS is active or certificate is loaded:
```bash
cd pc/airmouse_go_new
go run cmd/airmouse-server/main.go
```
The server will print its local IP addresses and ports (e.g. `192.168.1.50:8081`).

### Step 2: iOS Certificate Trust (For Local HTTPS / WSS)
Because iOS Safari enforces strict security for device sensors and WebSockets:
1. Open Safari on iOS and navigate to `https://<server-ip>:8081/mobile`.
2. If using self-signed development certificates:
   - Safari will display *"This Connection Is Not Private"*. Tap **Show Details** -> **visit this website**.
   - Confirm with FaceID / Passcode.
3. This allows Safari to establish secure TLS connections to `wss://<server-ip>:8081/ws`.

### Step 3: Enter Pairing Token & Connect
1. Copy or inspect the pairing token from the desktop server terminal or QR code.
2. Enter the token in the **Pairing Token** input (or pass via URL: `https://<server-ip>:8081/client/?token=<YOUR_TOKEN>`).
3. Tap **Connect (wss://)**.
4. The status badge will change to green **Connected**.

### Step 4: Enable Air Gyroscope Mode
1. Tap the **Enable Gyro (iOS)** button.
2. An iOS system prompt will appear: *"“AirMouse” Would Like to Access Motion and Orientation"*. Tap **Allow**.
3. Toggle **Enable Air Gyroscope Mode** in settings to steer your cursor in 3D air space!

### Step 5: Using the Touchpad
- **Cursor Movement**: Slide one finger smoothly across the dark touchpad surface.
- **Left Click**: Briefly tap with one finger.
- **Right Click**: Tap with two fingers simultaneously.
- **Scrolling**: Place two fingers on the touchpad surface and drag up or down.
- **Quick Buttons**: Use the physical on-screen buttons below the touchpad (*Left Click*, *Middle*, *Right Click*) for single-hand convenience.

---

## 5. Protocol V2 Wire Format Examples

### Hello Handshake (Client -> Server)
```json
{
  "v": 2,
  "type": "hello",
  "seq": 1,
  "ts": 1740000000000,
  "id": "c1f7b0b2-8f1b-43ec-84f9-2c70030dcb97",
  "payload": {
    "device_id": "web-9f3k1-1740000000",
    "name": "iOS Safari AirMouse",
    "version": "2.0.0",
    "token": "YOUR_PAIRING_TOKEN"
  }
}
```

### Motion Frame (Client -> Server)
```json
{
  "v": 2,
  "type": "move",
  "seq": 2,
  "ts": 1740000000016,
  "id": "f82b7b21-4f11-482a-bc74-1293a9032049",
  "payload": {
    "dx": 3.42,
    "dy": -1.85,
    "dt": 0.016
  }
}
```

### Tap / Click Frame (Client -> Server)
```json
{
  "v": 2,
  "type": "click",
  "seq": 3,
  "ts": 1740000000100,
  "id": "847290bc-2710-4389-a312-928374019284",
  "payload": {
    "button": "left"
  }
}
```

### Two-Finger Scroll Frame (Client -> Server)
```json
{
  "v": 2,
  "type": "scroll",
  "seq": 4,
  "ts": 1740000000250,
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "payload": {
    "delta": 2.5
  }
}
```
