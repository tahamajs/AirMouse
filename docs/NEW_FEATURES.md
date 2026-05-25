# New Features in Ultimate Air Mouse – Complete Explanation

The Air Mouse Ultimate project includes many advanced features beyond the basic exercise requirements. This document explains each feature in detail: what it does, how it is implemented, why it matters, and how to customise it.

---

## 1. Auto‑Reconnect

**Description:**  
When the WiFi connection between the phone and the PC server drops (e.g., router restart, signal loss), the Air Mouse app automatically attempts to reconnect every 5 seconds without requiring the user to manually restart the app.

**How it works:**
- The `DataSender` thread catches `IOException` during socket write operations and enters a retry loop.
- A separate `AutoReconnect` class (using a `Handler`) periodically checks the connection state and restarts the `DataSender` if needed.
- The retry uses the last known IP address (saved in preferences).

**Why it matters:**  
Users don’t have to constantly check the connection. The Air Mouse resumes control as soon as the network is restored.

**Customisation:**  
Adjust `RECONNECT_DELAY_MS` in `DataSender.kt` (default 5000 ms).

---

## 2. Sensitivity Slider

**Description:**  
A SeekBar on the main screen allows adjusting the cursor speed from 0.2× (very slow) to 2.0× (very fast) in real time. The value is saved and persists across app restarts.

**How it works:**
- The slider’s position (0–100) is linearly mapped to sensitivity: `sensitivity = 0.2 + (progress/100)*1.8`.
- On each orientation update, the delta (dx, dy) is multiplied by this sensitivity.
- When the user stops touching the slider, the value is saved to `PreferencesDataStore`.

**Why it matters:**  
Different users prefer different pointer speeds. This allows personalisation without code changes.

**Implementation snippet (`MainActivity.kt`):**
```kotlin
val sensitivity = 0.2f + (progress / 100f) * 1.8f
sensitivityText.text = String.format("%.2fx", sensitivity)
```

---

## 3. Double‑Click Gesture

**Description:**  
Two quick flicks (around Y‑axis) within a configurable interval (default 400 ms) generate a double‑click. The phone vibrates (50 ms) and the green square flashes red.

**How it works:**
- The `EnhancedGestureDetector` maintains a `potentialDoubleClick` flag and a timer.
- On the first flick, a timer starts. If a second flick occurs before the timer expires, a double‑click is triggered.
- The interval is adjustable in Settings (200–1000 ms).

**Why it matters:**  
Double‑click is a common desktop action (open files, select words). This gesture makes it natural.

**Customisation:**  
Change `doubleClickMaxInterval` in `PreferencesManager`.

---

## 4. Right‑Click (Long Tilt)

**Description:**  
Tilt the phone sideways (roll angle) beyond a threshold (default 45°) and hold for a duration (default 500 ms) to perform a right‑click. Haptic feedback (80 ms) and a visual flash confirm the action.

**How it works:**
- The detector monitors `|roll|` (absolute tilt).
- When the angle exceeds `rightClickTiltAngle`, a timer starts.
- If the angle remains above the threshold for `rightClickDuration` milliseconds, a right‑click message is sent to the PC.

**Why it matters:**  
Right‑click opens context menus – essential for many applications. The long hold distinguishes it from normal cursor movement (which also uses roll but does not hold).

**Customisation:**  
Adjust `rightClickTiltAngle` and `rightClickDuration` in Settings.

---

## 5. Debug Overlay

**Description:**  
A floating window that shows real‑time sensor values: roll (degrees), yaw (degrees), gyroY (rad/s), accelY (m/s²). It can be toggled on/off without restarting the app.

**How it works:**
- `DebugOverlayService` runs as a background service and uses `WindowManager` to add a view with `TYPE_APPLICATION_OVERLAY`.
- The service is a singleton; any part of the app can call `DebugOverlayService.update(roll, yaw, gyroY, accelY)`.
- The overlay is updated at the same rate as sensor events (≈50 Hz).

**Why it matters:**  
Invaluable for debugging gesture thresholds and verifying calibration. You can see exactly why a gesture isn’t detected (e.g., gyroY too low).

**Customisation:**  
Modify `debug_overlay.xml` for different colours, font, or position. Change `params.gravity` in `DebugOverlayService`.

---

## 6. Configurable Thresholds

**Description:**  
All gesture detection parameters can be changed through the Settings dialog – no code recompilation needed. Changes take effect immediately.

**Parameters adjustable:**
- Click speed threshold (rad/s)
- Double‑click interval (ms)
- Right‑click tilt angle (degrees)
- Right‑click duration (ms)
- Scroll speed threshold (m/s²)
- Scroll debounce (m/s²)
- Haptic feedback on/off

**How it works:**
- The Settings dialog writes to `PreferencesDataStore`.
- `EnhancedGestureDetector.reloadThresholds()` reads the new values.
- The detector is used in `SensorService` on every sensor update.

**Why it matters:**  
Every user has different movement patterns. This allows fine‑tuning without diving into code.

---

## 7. PC Server Configuration (`config.json`)

**Description:**  
The console server (`server.py`) reads settings from a `config.json` file, allowing you to change the listening port, mouse sensitivity, log level, and log file path without editing code.

**Example `config.json`:**
```json
{
    "host": "0.0.0.0",
    "port": 8080,
    "sensitivity": 0.5,
    "log_level": "INFO",
    "log_file": "airmouse.log"
}
```

**How it works:**  
`server.py` loads the JSON file at startup. If the file doesn’t exist, it creates one with defaults. The `MouseController` uses the sensitivity from the config.

**Why it matters:**  
Headless deployments, automated testing, or simply changing the port without modifying code.

**Customisation:**  
Edit the JSON file in any text editor; restart the server for changes to take effect.

---

## 8. Battery Saver Mode

**Description:**  
When the phone has been stationary for 10 seconds (no significant orientation change), the sensor sampling rate drops from 50 Hz (`SENSOR_DELAY_GAME`) to 20 Hz (`SENSOR_DELAY_NORMAL`). When movement resumes, the rate returns to 50 Hz.

**How it works:**
- `BatterySaver` monitors changes in roll and yaw.
- If the combined delta is less than a threshold for `idleThresholdMs` (10 s), it calls `sensorService.setSamplingRate(SENSOR_DELAY_NORMAL)`.
- On movement, it restores the game rate.

**Why it matters:**  
Significantly reduces battery drain when you leave the phone on the table but keep the app running.

**Customisation:**  
Change `idleThresholdMs` in `BatterySaver.kt`.

---

## 9. Haptic Feedback

**Description:**  
The phone vibrates briefly on successful gesture detection:
- Single click: 30 ms
- Double click: 50 ms
- Right click: 80 ms

The user can disable haptic feedback entirely via the Settings dialog.

**How it works:**  
`EnhancedGestureDetector` calls `vibrate(duration)` when a gesture is detected. The method checks `preferences.isHapticEnabled()` before vibrating.

**Why it matters:**  
Provides tactile confirmation, especially useful when you cannot see the screen (e.g., phone in your pocket). Disabling it saves battery.

---

## 10. Visual Click Flash

**Description:**  
The green square (orientation indicator) turns red for 100 ms every time any click (left, double, right) occurs. This gives immediate visual feedback.

**How it works:**  
In `MainActivity`, the gesture callback sets the square’s background colour to `R.color.red`, then posts a delayed runnable to set it back to `R.color.green` after 100 ms.

**Why it matters:**  
Reinforces that the gesture was recognised, especially when the PC is not visible (e.g., phone held away from screen).

---

## 11. Scroll Inertia (Bonus)

**Description:**  
A fast linear push (scrolling gesture) can generate multiple scroll events, simulating the effect of inertial scrolling – the page continues to scroll briefly after the push ends.

**How it works (simplified):**  
When a scroll is detected, the app sends the initial scroll packet. If the acceleration remains high for a few consecutive frames, it sends additional scroll packets. (In the provided code, this is a basic version; more advanced inertia would use velocity decay.)

**Why it matters:**  
Makes scrolling feel more natural, similar to touchpads or touchscreens.

**Note:** The basic implementation in the repository sends only one scroll per push. The “bonus” feature requires additional logic (e.g., using velocity to schedule follow‑up scrolls). This is left as an advanced exercise.

---

## Summary Table of Features

| Feature | Default | Adjustable? | Benefit |
|---------|---------|-------------|---------|
| Auto‑reconnect | 5 s delay | Yes (code) | Seamless recovery from network loss. |
| Sensitivity slider | 0.5x | Yes (UI) | Personalise cursor speed. |
| Double‑click | 400 ms interval | Yes (Settings) | Fast file/word selection. |
| Right‑click (tilt) | 45° / 500 ms | Yes (Settings) | Context menus. |
| Debug overlay | Always off | Toggle button | Diagnose sensor issues. |
| Configurable thresholds | Various | Yes (Settings) | Adapt to different users. |
| PC server config | Port 8080, log INFO | Yes (`config.json`) | Easy server customisation. |
| Battery saver | 10 s idle | Yes (code) | Extend battery life. |
| Haptic feedback | On | Yes (Settings) | Tactile confirmation. |
| Visual click flash | On (always) | Not adjustable | Immediate visual feedback. |

---

These features transform the basic Air Mouse into a polished, user‑friendly, and highly customisable tool. Each feature is implemented in the code with clean separation of concerns, making it easy to understand and modify.

*This document is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.*