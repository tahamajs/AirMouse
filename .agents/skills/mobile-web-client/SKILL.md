---
name: mobile-web-client
description: >-
  Developing, testing, and optimizing the zero-install AirMouse Mobile Web Client (iOS Safari
  and mobile browsers): touch gestures, Screen Wake Lock API, iOS motion permissions, Web Audio
  haptic click synthesis, and WebSocket connection reliability.
---

# AirMouse Mobile Web Client Development Skill

Use this skill when modifying, debugging, or enhancing the client-side web application in `web-client/` used to turn smartphones into trackpads and air mice via their web browser.

## 1. Multi-Touch Gesture Mapping

The web client operates as a precision trackpad on touch screens:

| Gesture | User Action | Generated Protocol V2 Event |
| :--- | :--- | :--- |
| **Cursor Move** | 1 finger slide across surface | `type: "move"` with relative `dx`, `dy` |
| **Left Click** | 1 finger quick tap (< 250ms, < 10px move) | `type: "click", payload: { button: "left", state: "tap" }` |
| **Right Click** | 2 finger tap OR long press (> 500ms) | `type: "click", payload: { button: "right", state: "tap" }` |
| **Two-Finger Scroll**| 2 fingers sliding vertically | `type: "scroll", payload: { delta_y: ... }` |
| **Drag & Drop** | Double tap and hold, then slide | `type: "click", state: "down"` $\rightarrow$ `move` $\rightarrow$ `click", state: "up"` |

### Touch Event Coordinates Handling
- Use `touch.clientX` and `touch.clientY`.
- Calculate differential movement:
  ```javascript
  const dx = currentTouch.clientX - lastTouch.clientX;
  const dy = currentTouch.clientY - lastTouch.clientY;
  lastTouch = { clientX: currentTouch.clientX, clientY: currentTouch.clientY };
  ```
- Always call `e.preventDefault()` on `touchstart`, `touchmove`, and `touchend` to prevent native browser scrolling and pull-to-refresh gestures.

---

## 2. iOS Safari & Mobile Quirks

### 1. Screen Wake Lock API
Prevents the phone screen from sleeping while using the mouse:
```javascript
let wakeLock = null;
async function requestWakeLock() {
  if ('wakeLock' in navigator) {
    try {
      wakeLock = await navigator.wakeLock.request('screen');
    } catch (err) {
      console.warn('Wake Lock request failed:', err);
    }
  }
}

// Re-acquire lock when user switches back to Safari tab
document.addEventListener('visibilitychange', () => {
  if (wakeLock !== null && document.visibilityState === 'visible') {
    requestWakeLock();
  }
});
```

### 2. iOS 13+ DeviceOrientation Permission
Motion sensors on iOS require explicit user permission triggered directly by a user gesture (button tap):
```javascript
async function enableGyroAirMouse() {
  if (typeof DeviceMotionEvent.requestPermission === 'function') {
    const permission = await DeviceMotionEvent.requestPermission();
    if (permission !== 'granted') {
      alert('Motion sensor permission is required for Air Mouse mode.');
      return;
    }
  }
  window.addEventListener('devicemotion', handleDeviceMotion);
}
```

### 3. Web Audio API Tactile Haptics (WebKit Workaround)
iOS Safari blocks `navigator.vibrate()`. To provide tactile feedback on clicks, synthesize an ultra-short low-frequency audio click impulse:
```javascript
const audioCtx = new (window.AudioContext || window.webkitAudioContext)();

function playTactileClick() {
  if (audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  const osc = audioCtx.createOscillator();
  const gain = audioCtx.createGain();
  osc.type = 'triangle';
  osc.frequency.setValueAtTime(120, audioCtx.currentTime);
  osc.frequency.exponentialRampToValueAtTime(30, audioCtx.currentTime + 0.02);

  gain.gain.setValueAtTime(1.0, audioCtx.currentTime);
  gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.02);

  osc.connect(gain);
  gain.connect(audioCtx.destination);
  osc.start();
  osc.stop(audioCtx.currentTime + 0.02);
}
```

---

## 3. WebSocket Resilience & Reconnection
- Use binary or minified JSON messages.
- Maintain exponential backoff retry loop (`min: 500ms`, `max: 5000ms`, `multiplier: 1.5`).
- Send keepalive `ping` every 5 seconds; if no `pong` within 10 seconds, drop connection and reconnect.
