# Air Mouse – Android App Detailed Usage (Complete Explanation)

This document provides a **thorough, step‑by‑step explanation** of every user interface element and gesture in the Air Mouse Android app. It is intended for end users (students, testers) who need to understand exactly how to operate the app and what each control does. All features are explained in plain language, with technical details where helpful.

---

## 📖 Table of Contents

- [Air Mouse – Android App Detailed Usage (Complete Explanation)](#air-mouse--android-app-detailed-usage-complete-explanation)
  - [📖 Table of Contents](#-table-of-contents)
  - [Main Screen Overview](#main-screen-overview)
  - [UI Elements – Detailed Description](#ui-elements--detailed-description)
    - [IP Address Field](#ip-address-field)
    - [Calibrate Sensors Button](#calibrate-sensors-button)
    - [Start Air Mouse Button](#start-air-mouse-button)
    - [Cursor Speed Slider](#cursor-speed-slider)
    - [Settings Button](#settings-button)
    - [Show/Hide Debug Button](#showhide-debug-button)
    - [Green Square (Orientation Indicator)](#green-square-orientation-indicator)
    - [Status Text](#status-text)
  - [Gesture Mapping – Complete Guide](#gesture-mapping--complete-guide)
    - [Cursor Movement (Horizontal \& Vertical)](#cursor-movement-horizontal--vertical)
    - [Left Click (Single Flick)](#left-click-single-flick)
    - [Double Click (Two Quick Flicks)](#double-click-two-quick-flicks)
    - [Right Click (Long Tilt)](#right-click-long-tilt)
    - [Scroll Up / Down (Linear Push)](#scroll-up--down-linear-push)
  - [Tips for Best Experience](#tips-for-best-experience)
  - [Common Misunderstandings](#common-misunderstandings)

---

## Main Screen Overview

When you open the Air Mouse Ultimate app, you see a clean layout with several controls arranged vertically. At the top is the **IP address field**, followed by two large buttons (**Calibrate Sensors** and **Start Air Mouse**), a **status text**, a **sensitivity slider**, two more buttons (**Settings** and **Show/Hide Debug**), and finally a **green square** that rotates and flashes.

The entire app is designed to be used in **portrait orientation**. The phone should be held as shown in the exercise description (screen perpendicular to the keyboard, parallel to the table).

---

## UI Elements – Detailed Description

### IP Address Field

- **What it is:** A text input field where you type the IP address of your PC (the one running the Air Mouse server).
- **How to use:** Tap the field, type the IP address (e.g., `192.168.1.10`), then tap “Done” on the keyboard.
- **Persistence:** The last entered IP address is saved automatically and will appear the next time you open the app. This is done via `PreferencesDataStore`.
- **Why needed:** The phone must know where to send the motion data (TCP connection). The server listens on a specific port (8080) at that IP.
- **Typical mistake:** Using `127.0.0.1` (localhost) – that is the phone itself, not the PC. Always use the PC’s actual local network IP (e.g., `192.168.x.x` or `10.x.x.x`).

### Calibrate Sensors Button

- **What it is:** A button that opens a **three‑step calibration dialog**.
- **Why calibration is required:** Raw sensor readings contain errors (gyro bias, magnetometer offset, accelerometer offset). Calibration removes these errors so that the cursor moves correctly and does not drift.
- **What happens when you tap it:**
  1. A dialog appears with a progress bar and status text.
  2. **Step 1 – Gyro calibration:** “Keep phone still on a flat surface”. The app collects 500 gyroscope samples (≈2‑3 seconds) and calculates the bias. You must **not** move the phone during this step.
  3. **Step 2 – Magnetometer calibration:** “Move phone in figure‑8 pattern for 30 seconds”. The app records min/max values on each axis while you rotate the phone in all directions. This corrects hard‑iron distortion (metal interference).
  4. **Step 3 – Accelerometer calibration (simplified):** “Keep phone still”. The app assumes the phone is stationary and measures the gravity vector to correct offset.
- **After completion:** The dialog closes, and a toast says “Calibration complete”. The `isCalibrated` flag is set, enabling the Start button.
- **When to re‑calibrate:** Whenever you change location (different magnetic environment), after the phone has been subject to strong magnetic fields (e.g., near a speaker), or if you notice cursor drift.

### Start Air Mouse Button

- **What it is:** The main “go” button. It establishes the network connection and begins streaming sensor data.
- **Prerequisites:**
  - Calibration must have been performed at least once (`isCalibrated == true`).
  - A valid IP address must be entered.
  - The PC server must be running and reachable.
  - WiFi must be on and connected to the same network as the PC.
- **What happens internally:**
  1. The app reads the IP from the text field and saves it.
  2. It creates a `DataSender` (TCP client) and `AutoReconnect` thread.
  3. It starts the `SensorService`, which registers listeners for accelerometer, gyroscope, and magnetometer.
  4. When sensor data arrives, it runs the Madgwick fusion, computes deltas, and sends `move` messages.
  5. Gesture detection is activated (click, double‑click, right‑click, scroll).
- **Visual feedback:** Status text changes from “Not connected” to “Air Mouse Active”. The green square becomes responsive.
- **If connection fails:** A toast appears (“Network error: Connection refused”) and the status remains unchanged.

### Cursor Speed Slider

- **What it is:** A `SeekBar` that adjusts the sensitivity of cursor movement.
- **Range:** 0.2x (very slow) to 2.0x (very fast). Default is 0.5x.
- **How it works:** The slider position (0–100) is linearly mapped to the sensitivity value. The value is displayed in real time (e.g., “0.50x”).
- **When the value is saved:** Only when you stop touching the slider (debounced), to avoid excessive writes to DataStore.
- **Effect:** Higher sensitivity means a small rotation of the phone produces a larger cursor movement. Lower sensitivity means you need to rotate the phone more for the same cursor movement.
- **Tip:** Start with 0.5x and adjust gradually. A very high sensitivity may cause the cursor to feel jumpy.

### Settings Button

- **What it is:** Opens a dialog where you can adjust the thresholds for gesture detection.
- **Settings available:**

| Setting | Default | Description | Effect of increasing |
|---------|---------|-------------|----------------------|
| Click speed threshold | 5.0 rad/s | Minimum angular velocity to register a flick | Harder to trigger click |
| Double‑click interval | 400 ms | Maximum time between two flicks to count as double click | Longer interval makes double‑click easier, but may increase false positives |
| Right‑click tilt angle | 45° | How far you must tilt the phone sideways | Higher value requires more tilt |
| Right‑click hold duration | 500 ms | How long you must hold the tilt | Longer duration reduces accidental right‑clicks |
| Scroll speed threshold | 8.0 m/s² | Minimum linear acceleration to trigger scroll | Higher value requires faster push |
| Scroll debounce | 2.0 m/s² | Acceleration below which scroll resets | Higher value makes scroll reset faster (may cause repeated scrolls) |
| Haptic feedback | On | Vibration on click/double‑click/right‑click | Turn off to save battery or reduce distraction |

- **How to use:** Slide each seek bar left or right. The new value is applied immediately (no “Save” button). The dialog can be dismissed with the OK button.
- **Persistence:** All settings are saved in `PreferencesDataStore` and survive app restarts.

### Show/Hide Debug Button

- **What it is:** Toggles a **floating overlay** that displays real‑time sensor values on top of other apps (even outside the Air Mouse app).
- **Why use it:** Useful for debugging why gestures aren’t detected, or to see the effect of calibration.
- **What it shows:**
  - **Roll:** Rotation around X‑axis (degrees) – vertical cursor movement.
  - **Yaw:** Rotation around Z‑axis (degrees) – horizontal cursor movement.
  - **GyroY:** Angular velocity around Y‑axis (rad/s) – used for click detection.
  - **AccelY:** Linear acceleration along Y‑axis (m/s²) – used for scroll detection.
- **Permission required:** The app will request `SYSTEM_ALERT_WINDOW` overlay permission the first time you tap it. You must grant it in system settings.
- **Visual style:** A small black semi‑transparent box with white monospace text, positioned at the top‑left corner.
- **Performance impact:** Minimal – the overlay updates at the same rate as sensor events (≈50 Hz).

### Green Square (Orientation Indicator)

- **What it is:** A 100×100 dp green square (shape drawable) that visually reflects the phone’s horizontal orientation.
- **Behaviour:** It rotates around its centre according to the **yaw angle** (rotation around Z‑axis). When the phone is upright (yaw = 0°), the square is axis‑aligned. Rotate the phone left → square rotates left, etc.
- **Click flash:** When any click (left, double, right) occurs, the square turns red for 100 ms, then back to green. This gives immediate visual confirmation.
- **Technical note:** The rotation is applied via `View.setRotation(yawInDegrees)`. Yaw is in radians from the Madgwick fusion; the UI converts it to degrees.

### Status Text

- **What it is:** A `TextView` located above the green square that shows the current state of the app.
- **Possible states:**
  - “Not connected” – initial state, or after a failed connection.
  - “Calibrating...” – during calibration steps (temporary).
  - “Calibration complete!” – after successful calibration.
  - “Calibration failed” – if an exception occurred.
  - “Air Mouse Active” – when the app is connected and streaming.
- **Also used for errors:** “No WiFi connection”, “Enter laptop IP”, “Please calibrate first”, etc.
- **Why it matters:** Provides immediate feedback without needing to check logs.

---

## Gesture Mapping – Complete Guide

The following table summarises every gesture and the required phone movement. Gesture thresholds can be adjusted in the Settings dialog.

| Phone movement | Action | Sensor used | Key parameter |
|----------------|--------|-------------|----------------|
| Rotate around Z‑axis (left/right) | Move cursor horizontally | Yaw angle (from fusion) | Sensitivity slider |
| Rotate around X‑axis (nod up/down) | Move cursor vertically | Roll angle (from fusion) | Sensitivity slider |
| Quick flick around Y‑axis (like flicking a paper) | Left click | Gyroscope Y angular velocity | Click speed threshold |
| Two quick flicks within 400 ms | Double click | Gyroscope Y (two detections) | Click speed + double‑click interval |
| Tilt phone sideways >45° and hold for 0.5 seconds | Right click | Roll angle (tilt) | Right‑click tilt angle + duration |
| Quick linear push up/down along Y‑axis | Scroll up/down | Accelerometer Y linear acceleration | Scroll speed threshold |

### Cursor Movement (Horizontal & Vertical)

- **How it works:** The Madgwick fusion algorithm outputs a quaternion, from which we extract **roll** (X rotation) and **yaw** (Z rotation). These are in radians.
- **Delta calculation:** On each sensor update, the difference from the previous orientation is multiplied by the sensitivity factor (0.2‑2.0) and then by 0.8 (a fixed scaling factor to keep movement reasonable).
- **Sending:** The delta values (dx, dy) are sent as JSON `move` messages to the PC server. There is no ACK for move packets; if one is lost, the next packet corrects it.
- **Smoothness:** At 50 Hz, the movement feels smooth. You can increase the sampling rate in code if needed.

### Left Click (Single Flick)

- **How to perform:** Hold the phone normally, then flick it **quickly** to the left or right around the Y‑axis (as if you were turning a key). The flick should be sharp, not a smooth rotation.
- **Detection:** The app monitors the absolute angular velocity `|gyroY|`. If it exceeds `clickSpeedThreshold` (default 5 rad/s) and the time since the last click is greater than `doubleClickInterval` (to allow double‑click detection), a click is triggered.
- **Haptic feedback:** The phone vibrates for 30 ms (if haptic is enabled). The green square flashes red.
- **Delay:** The server receives a `click` message, performs the click, and sends an ACK. The total latency is about 20‑40 ms, which is imperceptible.

### Double Click (Two Quick Flicks)

- **How to perform:** Perform a single flick, then within 400 ms (default) perform another flick. Both flicks must exceed the click speed threshold.
- **Detection logic:** After the first flick, a timer starts. If a second flick occurs before the timer expires, a double‑click is triggered. If the timer expires, a single click is triggered (the single click is actually returned immediately, but the double‑click overrides it – this is a known trade‑off).
- **Haptic feedback:** A longer vibration (50 ms) indicates a double‑click.
- **Use case:** Quickly open files or select text (same as desktop double‑click).

### Right Click (Long Tilt)

- **How to perform:** Tilt the phone sideways to the left or right beyond the `rightClickTilt` angle (default 45°) and **hold** it there for the `rightClickDuration` (default 500 ms). You will feel a longer vibration (80 ms) when it triggers.
- **Detection:** The app monitors the absolute roll angle `|roll|`. When it exceeds the threshold, a timer starts. If the angle remains above the threshold for the whole duration, a right‑click is sent. If you tilt back before the time, nothing happens.
- **Why not use tilt then release?** The long‑hold distinguishes right‑click from normal tilting during cursor movement.
- **Tip:** You can customise the angle and duration in Settings. A larger angle or longer duration reduces accidental right‑clicks.

### Scroll Up / Down (Linear Push)

- **How to perform:** Hold the phone normally, then push it **linearly** up or down along the Y‑axis (the same axis as the phone’s height). This is a quick, short movement – like a “jerk” – not a rotation.
- **Detection:** The app monitors the linear acceleration `accelY`. If its absolute value exceeds `scrollSpeedThreshold` (default 8 m/s²) and scroll is not already in progress, a scroll command is sent (positive `accelY` = scroll down, negative = scroll up).
- **Debounce:** After a scroll, the app waits until `|accelY|` falls below `scrollDebounce` (default 2 m/s²) before allowing another scroll. This prevents repeated scrolls from a single push.
- **Scroll amount:** Each scroll packet has `delta = +1` (down) or `-1` (up). The PC server performs a single scroll step (e.g., 3 lines in most applications).
- **Tip:** To scroll continuously, you can repeat the push movement. The debounce time is not a timer; it’s based on acceleration returning to near zero, so you can push again as soon as the phone is stationary.

---

## Tips for Best Experience

1. **Always calibrate after first install and after changing location.** Magnetic fields vary; re‑calibrating the magnetometer ensures yaw accuracy.
2. **Hold the phone with a relaxed grip.** Death‑gripping the phone can introduce unwanted accelerations and make gestures harder.
3. **For clicks, practise quick, short flicks.** The gyro measures angular speed; a fast but small rotation works better than a slow large rotation.
4. **For scrolling, use a quick “jab” movement** – as if you were tapping the air with the phone. The linear acceleration should spike and return.
5. **Adjust sensitivity to your preference.** If the cursor moves too fast, lower the sensitivity; if you have to rotate too much, increase it.
6. **Use the debug overlay to see live values.** If a gesture isn’t working, check `GyroY` or `AccelY` to see if they exceed thresholds.
7. **Keep your phone away from laptop speakers and other magnets.** The magnetometer is sensitive; even a metal table can affect it.
8. **If the cursor drifts, re‑calibrate gyro and magnetometer.** Also check that the phone is not near a vibrating source.

---

## Common Misunderstandings

| Misunderstanding | Reality |
|------------------|---------|
| “I can just start without calibration.” | You can, but the cursor will drift and gestures may be unreliable. Calibration is essential. |
| “The green square should rotate exactly like my phone.” | It rotates according to yaw (horizontal orientation). Roll (nodding) does not rotate the square – only yaw. |
| “A slow rotation should also trigger a click.” | No – click requires high angular speed. A slow rotation moves the cursor, not click. |
| “Right‑click is the same as tilting during cursor movement.” | Right‑click requires holding the tilt for 0.5 seconds. A quick tilt is just cursor movement. |
| “Scroll works by rotating the phone.” | No – scroll is a linear push along the Y‑axis, not a rotation. |
| “The debug overlay consumes a lot of battery.” | Very little – it only updates when sensor data arrives. It’s fine to leave it on while testing. |
| “I need to keep the app in the foreground.” | Yes – sensor streaming stops when the app is in the background (onPause). This is intended to save battery. |

---

This complete guide to the Android app usage ensures that every user – from first‑time testers to advanced developers – can operate Air Mouse effectively. All controls, gestures, and configuration options are explained with their purpose and behaviour.