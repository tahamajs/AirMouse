# Deep Technical Audit: Mobile Web Client & iOS Safari

**Audit Date**: October 2026  
**Auditor**: Principal Web & Mobile Standards Specialist  
**Component**: `web/` (Zero-Install Mobile Web Client, PWA, WebSockets, Web Sensors API)

---

## 1. Executive Summary

The AirMouse Mobile Web Client provides a zero-install alternative for iOS (iPhone/iPad) and Android users without requiring app store installation. It serves a responsive Progressive Web App (PWA) over HTTP/HTTPS from the local desktop daemon.

While the web client offers exceptional accessibility and immediate onboarding via QR code, browser sandbox restrictions—particularly on Apple's WebKit/iOS Safari—impose severe constraints on motion telemetry, audio feedback, power management, and background network persistence.

---

## 2. iOS Safari & WebKit Sandbox Constraints

### 2.1 `DeviceMotionEvent` Permission Flow (iOS 13+ Breaking Behavior)
* **Weakness**:
  - In iOS 13+, `DeviceMotionEvent.requestPermission()` is strictly gated behind an explicit user gesture (direct tap on a button). It cannot be called programmatically upon page load or WebSocket handshake.
  - If the user skips or declines the prompt, iOS permanently denies sensor access for that origin in that tab session without providing a programmatic way to re-trigger the permission dialog.
* **Audit Finding**: In the current implementation, if the WebSocket connects before the user presses "Enable Motion", motion streaming starts transmitting zero vectors (`dx=0, dy=0`), confusing users into thinking the connection has stalled.
* **Remediation**: Implement a mandatory, visually prominent onboarding splash modal that halts connection attempts until the user taps an explicit "Calibrate & Grant Motion Access" button that invokes `requestPermission()`.

### 2.2 Gyroscope Coordinate Frame & Orientation Mismatch
* **Weakness**:
  - WebKit's `DeviceOrientationEvent` reports orientation angles ($\alpha, \beta, \gamma$) relative to the device's physical screen orientation, which flips when rotating between portrait and landscape.
  - When auto-rotate is enabled, rotating the phone 90 degrees causes the X and Y cursor axes to invert or swap unless `window.screen.orientation.type` or `window.orientation` is tracked with real-time matrix transforms.
  - Android Chrome and iOS Safari use opposite coordinate signs for $\gamma$ (tilt left/right), leading to inverted horizontal cursor movement if browser user-agent sniffing is flawed.

---

## 3. WakeLock API & Screen Dimming Vulnerabilities

```
Mobile Web Client Active
           │
  ┌────────┴────────┐
  ▼                 ▼
[Screen WakeLock]  [NoSleep.js Fallback]
  Works on Chrome    Required for iOS Safari
  (Native API)       (Invisible Loop Video)
```

### 3.1 The iOS Safari WakeLock Deficiency
* **Weakness**:
  - Apple only introduced standard `navigator.wakeLock` support in iOS 16.4. Older iOS devices (e.g. iPhone 7, 8, or devices locked to iOS 15) fail to maintain screen state.
  - Without a wake lock, iOS dims the screen after 30 seconds of inactivity (since touch events on a virtual touchpad do not reset the OS display sleep timer in fullscreen web apps). Once the screen locks, iOS Safari immediately suspends the WebSocket connection, abruptly severing input control mid-task.
* **Remediation**:
  - Deploy a dual-strategy wake lock: use the native `navigator.wakeLock.request('screen')` where supported, paired with an automated fallback using an invisible looping `<video>` element (`playsinline`, muted, base64 data URI) for older WebKit engines.

---

## 4. Haptic Feedback & Audio Click Synthesis

### 4.2 Web Vibration API Non-Support on iOS
* **Weakness**:
  - `navigator.vibrate([15])` works reliably on Android Chrome to simulate physical mouse button clicks with crisp tactile haptics.
  - **Apple explicitly blocks `navigator.vibrate` on all iOS browsers** for privacy reasons.
  - To compensate, AirMouse uses an HTML5 `AudioContext` synthesizer generating a 40ms 150Hz sine burst with exponential decay to mimic a mechanical click sound.
* **Flaw**:
  - In iOS Safari, the `AudioContext` initializes in the `"suspended"` state. Unless resumed within a direct touch event handler (`onTouchStart`), no audio plays.
  - Under fast clicking (double clicks or rapid scrolling), scheduling multiple audio buffer nodes creates audio clipping and latency lag in the web thread.

---

## 5. Web Client Security & Transport Reliability

### 5.1 Mixed Content & Self-Signed HTTPS Hurdles
* **Weakness**:
  - To access `navigator.mediaDevices` (for QR scanner) and `DeviceMotionEvent` in modern browsers, the page **must be served over a secure origin (`https://` or `localhost`)**.
  - Local desktop servers serving self-signed SSL certificates cause modern browsers to display high-friction "Untrusted Connection / Security Risk Ahead" warning screens that scare non-technical users.
  - Serving over plain `http://` causes mobile browsers to silently neuter sensor and wake lock APIs.

---

## 6. Recommendations for Web Experience

1. **Deterministic Permission Modal**: Do not attempt motion streaming until `DeviceMotionEvent.requestPermission()` resolves successfully.
2. **Dynamic Coordinate Transform Matrix**: Apply device orientation offsets ($\pm 90^\circ, 180^\circ$) to sensor vectors before packet construction.
3. **AudioContext Buffer Pooling**: Pre-decode a single click impulse PCM buffer during the initial user tap, avoiding node allocation churn on every click.
