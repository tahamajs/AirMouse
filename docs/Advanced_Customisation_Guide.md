# Air Mouse – Complete Advanced Customisation Guide

This document provides **exhaustive instructions** for customising every aspect of the Air Mouse system – from simple settings like TCP port and haptic feedback to advanced modifications like adjusting the Madgwick fusion parameter and adding entirely new gestures (e.g., long press). All changes are explained with code snippets, configuration files, and their effects on performance and user experience.

---

## 📖 Table of Contents

1. [Changing the TCP Port](#changing-the-tcp-port)
2. [Adjusting Madgwick Beta (Drift vs Noise Trade‑off)](#adjusting-madgwick-beta-drift-vs-noise-trade-off)
3. [Disabling Haptic Feedback](#disabling-haptic-feedback)
4. [Changing Sensitivity & Gesture Thresholds](#changing-sensitivity--gesture-thresholds)
5. [Adding a New Gesture (e.g., Long Press)](#adding-a-new-gesture-eg-long-press)
6. [Changing Sensor Sampling Rate](#changing-sensor-sampling-rate)
7. [Modifying the Debug Overlay](#modifying-the-debug-overlay)
8. [Customising the PC Server](#customising-the-pc-server)
9. [Applying Custom Icons & Theme](#applying-custom-icons--theme)

---

## 1. Changing the TCP Port

### Why change the port?
- The default port **8080** may already be in use by another application (e.g., a web server, proxy).
- Your network firewall may block port 8080, but allow others.
- You may want to run multiple instances of Air Mouse on the same PC (using different ports).

### Step‑by‑step modification

#### On the Android side (`MainActivity.kt`)

Locate the `companion object` block:
```kotlin
companion object {
    private const val PORT = 8080   // Change this value
}
```
Example: change to `8081`:
```kotlin
private const val PORT = 8081
```

#### On the PC server

**If using `server.py` (console server):**  
Edit `config.json`:
```json
{
    "host": "0.0.0.0",
    "port": 8081,
    ...
}
```

**If using `gui.py` (GUI server):**  
Change the `CONFIG` dictionary at the top:
```python
CONFIG = {
    "host": "0.0.0.0",
    "port": 8081,
    "sensitivity": 0.5,
}
```

**Important:** After changing the port, you must **restart both the Android app and the PC server**.

### Verifying the change
- On PC: `netstat -an | grep 8081` should show `LISTENING`.
- On Android: the logcat should show `Connected to <IP>:8081`.

### Firewall considerations
If you change to a non‑standard port, ensure your firewall allows inbound connections on that port.

---

## 2. Adjusting Madgwick Beta (Drift vs Noise Trade‑off)

### What is `beta`?
The Madgwick AHRS algorithm uses a gain parameter `beta` that controls how aggressively the accelerometer and magnetometer correct the gyroscope integration.

- **High `beta` (e.g., 0.5 – 1.0):**  
  Strong correction → less drift, but more sensitivity to accelerometer noise (the cursor may jitter when the phone is perfectly still).
- **Low `beta` (e.g., 0.01 – 0.05):**  
  Weak correction → gyro drift becomes noticeable (cursor slowly moves even when phone is stationary), but the movement is smoother (less noise).

### Where to change it
In `MadgwickFusion.kt`, find the constructor:
```kotlin
class MadgwickFusion(private val beta: Float = 0.1f) { ... }
```
Change the default value, or create a custom instance in `SensorService.kt`:
```kotlin
private val madgwick = MadgwickFusion(beta = 0.2f)   // stronger correction
```

### Empirical tuning
- **If you experience drift** (cursor moves when phone is still), **increase `beta`** (e.g., 0.15 – 0.25).
- **If the cursor shakes or jitters** when stationary, **decrease `beta`** (e.g., 0.05 – 0.08).

### Perfetto verification
You can trace the effect of `beta` by logging the angular error before and after correction (requires instrumenting the code). In general, a well‑tuned `beta` makes the cursor stable when still and responsive when moved.

---

## 3. Disabling Haptic Feedback

### Why disable?
- To save battery (vibration consumes power).
- If you find the vibration distracting or noisy (e.g., in a quiet room).

### Method 1 – Using the Settings Dialog (runtime)
In the Android app, tap **Settings** → uncheck **“Enable haptic feedback”**. This change is saved immediately and persists across app restarts.

### Method 2 – Permanently (by code)
In `EnhancedGestureDetector.kt`, the `vibrate()` function checks `preferences.isHapticEnabled()`. You can hardcode it to always return `false`:
```kotlin
private fun vibrate(duration: Long) {
    // if (preferences.isHapticEnabled()) {
    //     vibrator.vibrate(...)
    // }
    // Disabled permanently
}
```
Or simply comment out the call to `vibrate()` in the gesture detection methods.

### Method 3 – Remove vibration permission from manifest
In `AndroidManifest.xml`, remove the line:
```xml
<uses-permission android:name="android.permission.VIBRATE" />
```
This will also disable all vibrations (but may cause the app to crash if vibration is attempted – so better to wrap in try‑catch or keep the condition).

---

## 4. Changing Sensitivity & Gesture Thresholds

### Available settings (already in the UI)

| Setting | Range (default) | Where to change |
|---------|----------------|----------------|
| Cursor speed | 0.2 – 2.0 (0.5) | Main screen slider |
| Click speed threshold | 0 – 10 rad/s (5.0) | Settings dialog |
| Double‑click interval | 200 – 1000 ms (400) | Settings dialog |
| Right‑click tilt angle | 0 – 90° (45°) | Settings dialog |
| Right‑click hold duration | 100 – 1000 ms (500) | Settings dialog |
| Scroll speed threshold | 0 – 15 m/s² (8.0) | Settings dialog |
| Scroll debounce | 0 – 5 m/s² (2.0) | Settings dialog |

All these are stored in `PreferencesDataStore` and can be changed programmatically:
```kotlin
prefs.setClickThreshold(4.5f)
prefs.setSensitivity(1.2f)
```

### Changing default values (initial installation)
If you want different defaults for new users, modify the `PreferencesDataStore` default values:
```kotlin
private val SENSITIVITY = floatPreferencesKey("sensitivity")
// In the flow, change the default:
dataStore.data.map { it[SENSITIVITY] ?: 0.8f }   // new default 0.8
```
Also update the corresponding getters (`getSensitivity()` etc.) and the `Constants.kt`.

---

## 5. Adding a New Gesture (e.g., Long Press)

### Overview
Adding a custom gesture involves three parts:
1. **Detection** – in `EnhancedGestureDetector.kt`.
2. **Messaging** – in `DataSender.kt` (add new message type).
3. **PC handling** – in `server.py` or `gui.py`.

Let’s implement a **long press** gesture: user holds the phone still for 1 second while it is in a certain orientation (e.g., roll > 30°). This could trigger a middle‑click or a custom action.

### Step 1: Detect the gesture in `EnhancedGestureDetector.kt`

Add a new `Gesture` enum value:
```kotlin
enum class Gesture {
    NONE, CLICK, DOUBLE_CLICK, RIGHT_CLICK, SCROLL_UP, SCROLL_DOWN, LONG_PRESS
}
```

Add state variables and detection logic:
```kotlin
private var longPressStartTime = 0L
private val longPressDuration = 1000L  // 1 second

fun detectLongPress(roll: Float, isMoving: Boolean): Boolean {
    val now = System.currentTimeMillis()
    // Condition: phone tilted more than 30° and not moving much
    if (kotlin.math.abs(roll) > 30f && !isMoving) {
        if (longPressStartTime == 0L) {
            longPressStartTime = now
        } else if (now - longPressStartTime > longPressDuration) {
            longPressStartTime = 0L
            return true
        }
    } else {
        longPressStartTime = 0L
    }
    return false
}
```
The `isMoving` flag can be derived from gyro magnitude (sum of absolute angular velocities) – you may need to pass it in.

Integrate into the main `detect()` method or call it separately from `SensorService`.

### Step 2: Add message type in `DataSender.kt`

Add a new method:
```kotlin
fun sendLongPress() {
    val json = JSONObject().apply {
        put("type", "longpress")
        put("id", System.currentTimeMillis())
    }
    queue.offer(json.toString())
}
```

Update `isCriticalMessage()` to include `"longpress"` so that ACK and retransmission are applied.

### Step 3: Handle on PC server

In `server.py` (or `gui.py`), extend the `process_message` method:
```python
elif t == 'longpress':
    self.mouse.click(button='middle')   # or any action
    await self.send_ack(msg.get('id'), writer)
    self.log("🖱️ Long press (middle click)")
```

If you want a different action (e.g., volume up/down), you can use `pyautogui.hotkey('ctrl', 'c')` etc.

### Step 4: Call the new detector from `SensorService`

In `SensorService.onSensorChanged()`, after computing roll and before sending orientation, call:
```kotlin
val isMoving = kotlin.math.abs(gyroX) + kotlin.math.abs(gyroY) + kotlin.math.abs(gyroZ) > 1.0f
if (gestureDetector.detectLongPress(roll, isMoving)) {
    gestureCallback?.invoke(Gesture.LONG_PRESS)
}
```

### Testing
- Build and run the Android app.
- Tilt the phone sideways (>30°) and hold still for 1 second.
- Observe the PC server log: should show “Long press (middle click)”.

---

## 6. Changing Sensor Sampling Rate

### Why change?
- **Higher rate** (e.g., 100 Hz) – smoother cursor but more battery drain.
- **Lower rate** (e.g., 20 Hz) – better battery, but cursor may feel less responsive.

### Where to change
In `SensorService.kt`, when registering listeners:
```kotlin
sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
```
Replace `SENSOR_DELAY_GAME` with one of:
| Constant | Period | Approx. Hz |
|----------|--------|-------------|
| `SENSOR_DELAY_FASTEST` | 0 ms (as fast as possible) | up to 200 Hz |
| `SENSOR_DELAY_GAME` | 20 ms | 50 Hz |
| `SENSOR_DELAY_UI` | 60 ms | 16 Hz |
| `SENSOR_DELAY_NORMAL` | 200 ms | 5 Hz |

**Caution:** `SENSOR_DELAY_FASTEST` may cause high CPU and battery drain. Test on your device.

### Dynamic rate switching (battery saver)
The existing `BatterySaver` already switches between `GAME` and `NORMAL`. You can modify its thresholds (`idleThresholdMs`) in `BatterySaver.kt`.

---

## 7. Modifying the Debug Overlay

### What you can change
- Position (top‑left, bottom‑right, etc.)
- Text size, font, colours
- Which sensor values are displayed

### Position
In `DebugOverlayService.kt`, modify the `WindowManager.LayoutParams`:
```kotlin
params.gravity = Gravity.TOP or Gravity.END   // top‑right
params.x = 10
params.y = 100
```

### Appearance
In `debug_overlay.xml`, change the background, text colour, text size:
```xml
<TextView
    android:background="#AA000000"   // semi‑transparent black
    android:textColor="#00FF00"      // green text
    android:textSize="14sp"
    ... />
```

### Additional data
To show more sensor values (e.g., roll in degrees, battery percentage), modify `updateData()` in `DebugOverlayService` and the corresponding call from `MainActivity` or `SensorService`.

---

## 8. Customising the PC Server

### Changing GUI theme
In `gui.py`, the colour palette is defined at the top:
```python
self.bg_color = "#1e1e1e"    # dark background
self.accent = "#007acc"      # blue accent
```
Change these to any hex colour.

### Adding more log detail
In `AirMouseServer.log()`, you can add timestamps, client IP, etc. The existing logging already includes it.

### Changing the mouse controller library
By default, Air Mouse uses `pyautogui`. You could replace it with:
- `pynput` – more advanced input simulation.
- `win32api` (Windows only) – lower latency.
- `Xlib` (Linux) – direct X11 calls.

Modify `MouseController` class accordingly.

### Running as a background service (headless)
On Linux/macOS, you can run `server.py` with `nohup`:
```bash
nohup python server.py &
```

Or create a systemd service (Linux) for auto‑start.

---

## 9. Applying Custom Icons & Theme

### Changing the app icon
Replace the images in `res/mipmap-*` folders (e.g., `ic_launcher.png`). Use different resolutions:
- `mipmap-mdpi` (48×48)
- `mipmap-hdpi` (72×72)
- `mipmap-xhdpi` (96×96)
- `mipmap-xxhdpi` (144×144)
- `mipmap-xxxhdpi` (192×192)

### Changing the app theme
In `res/values/themes.xml`, modify the colour attributes:
```xml
<item name="colorPrimary">#FF6200EE</item>  <!-- purple -->
<item name="colorPrimaryVariant">#FF3700B3</item>
<item name="colorOnPrimary">#FFFFFFFF</item>
```
To use a light theme, change the parent:
```xml
<style name="Theme.AirMouse" parent="Theme.MaterialComponents.DayNight.LightActionBar">
```

### Renaming the app
Change `app_name` in `res/values/strings.xml`.

---

## 10. Summary of Customisation Options

| Customisation | File(s) to edit | Skill level |
|---------------|----------------|-------------|
| TCP port | `MainActivity.kt`, `config.json` / `gui.py` | Easy |
| Madgwick beta | `MadgwickFusion.kt` | Medium |
| Haptic feedback | Settings dialog (no code) / `EnhancedGestureDetector.kt` | Easy |
| Sensitivity & thresholds | Settings dialog (UI) / `PreferencesDataStore.kt` | Easy |
| New gesture | `EnhancedGestureDetector.kt`, `DataSender.kt`, `server.py` | Hard |
| Sampling rate | `SensorService.kt` | Medium |
| Debug overlay appearance | `DebugOverlayService.kt`, `debug_overlay.xml` | Easy |
| PC server theme | `gui.py` (colours) | Easy |
| App icon & theme | `res/mipmap-*`, `themes.xml`, `strings.xml` | Easy |

---

## Final Notes

- **Always back up** your code before making advanced changes.
- **Test incrementally** – change one parameter at a time and verify behaviour.
- **Document your customisations** – they may be required in your project report.

This guide equips you to tailor Air Mouse to your exact needs – whether for better performance, new features, or a personalised user interface. Enjoy hacking!