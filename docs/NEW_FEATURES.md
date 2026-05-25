# New Features in Ultimate Air Mouse

## 1. Auto‑Reconnect
- The app continuously tries to reconnect to the PC if WiFi is lost.
- No need to restart the app.

## 2. Sensitivity Slider
- Adjust cursor speed from 0.2x to 2.0x in real time.
- Saves preference across app restarts.

## 3. Double‑Click Gesture
- Two quick flicks around Y‑axis within 400 ms (adjustable).
- Haptic feedback and visual flash.

## 4. Right‑Click (Long Tilt)
- Tilt phone left or right past 45° for 0.5 seconds.
- Triggers right‑click on PC.

## 5. Debug Overlay
- Shows real‑time sensor values (roll, yaw, gyroY, accelY).
- Toggle on/off without restarting.

## 6. Configurable Thresholds
- All gesture thresholds adjustable via Settings dialog.
- No code changes needed.

## 7. PC Server Configuration
- `config.json` allows changing port, sensitivity, log level.
- All actions logged to `airmouse.log`.

## 8. Battery Saver Mode
- After 10 seconds of phone being still, sampling rate reduces to 20 Hz.
- Saves battery during idle periods.

## 9. Haptic Feedback
- Phone vibrates on click, double‑click, right‑click.
- Can be disabled in settings.

## 10. Visual Click Flash
- Green square turns red briefly when any click occurs.
- Provides immediate visual confirmation.

## 11. Scroll Inertia (bonus)
- Fast scroll pushes generate multiple scroll events (simulated inertia).