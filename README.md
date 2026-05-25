
# ✈️ Air Mouse – Ultimate Complete Guide

**University of Tehran – Faculty of Electrical and Computer Engineering**  
*Embedded Systems Exercise – Second Semester 1404-1405 (2025-2026)*

**Designers:** Arian Firoozi, Arsalan Talaee  
**Instructors:** Dr. Mohsen Shokri, Dr. Mehdi Kargahi

---

## 📖 Table of Contents

1. [Overview](#overview)
2. [How It Works (System Architecture)](#how-it-works)
3. [Requirements](#requirements)
4. [Complete Setup Guide](#complete-setup-guide)
   - [Android App – Three Ways to Build](#android-app--three-ways-to-build)
   - [PC Server – Installation & Proxy Fix](#pc-server--installation--proxy-fix)
5. [Calibration Deep Dive](#calibration-deep-dive)
6. [Gesture Detection & Sensitivity Tuning](#gesture-detection--sensitivity-tuning)
7. [Network Protocol & Reliability](#network-protocol--reliability)
8. [Usage Instructions](#usage-instructions)
9. [Troubleshooting – All Known Issues](#troubleshooting--all-known-issues)
10. [Performance Profiling with Perfetto](#performance-profiling-with-perfetto)
11. [Answers to the 11 Required Questions](#answers-to-the-11-required-questions)
12. [Advanced Customization](#advanced-customization)
13. [Project File Structure (Complete)](#project-file-structure-complete)
14. [Frequently Asked Questions](#frequently-asked-questions)
15. [Credits & License](#credits--license)

---

## Overview

Air Mouse transforms your Android phone into a **wireless, motion‑controlled mouse**. By rotating and moving the phone in the air, you can control your computer’s cursor, click, and scroll – no physical mouse needed.

The phone’s **accelerometer, gyroscope, and magnetometer** are fused using a **Madgwick AHRS filter** to obtain stable orientation. Gestures (click, scroll) are detected via threshold‑based algorithms. Data is sent over **TCP sockets** to a Python server that moves the actual mouse cursor using `pyautogui`.

---

## How It Works (System Architecture)

```
[Android Phone]                    [Laptop/PC]
     │                                    │
     │ 1. Raw sensors (100 Hz)            │
     │ 2. Calibration (bias/offset/scale) │
     │ 3. Madgwick fusion → quaternion    │
     │ 4. Convert to roll/pitch/yaw       │
     │ 5. Compute delta (dx, dy)          │
     │ 6. Detect click (gyro Y speed)     │
     │ 7. Detect scroll (accel Y speed)   │
     │ 8. TCP JSON packets                │
     └────────────────────────────────────►│
                                          │ 9. Parse JSON
                                          │10. Move/click/scroll using pyautogui
                                          │11. Send ACK for critical packets
```

---

## Requirements

### Hardware
- **Android phone** with Android 10 (API 29) or higher
- **Sensors required:** accelerometer, gyroscope, magnetometer (all modern phones have these)
- **Computer** (Windows/Linux/macOS) with Python 3.8+
- **WiFi network** (both devices on same network) or phone hotspot

### Software (Android)
- Option 1: Android Studio (for building from source)
- Option 2: Command line tools + Gradle (no Android Studio)
- Option 3: Pre‑built APK (if provided)

### Software (PC)
- Python 3.8+
- `pyautogui` library
- No additional drivers needed

---

## Complete Setup Guide

### Android App – Three Ways to Build

#### **Way 1: Using Android Studio (Easiest for beginners)**

1. Download and install [Android Studio](https://developer.android.com/studio) for your OS.
2. Clone or download the project code. Ensure the folder structure matches the one shown below.
3. Open Android Studio → **Open an Existing Project** → select the `android` folder.
4. Wait for Gradle sync to finish (may take a few minutes, requires internet).
5. Enable **USB debugging** on your phone:
   - Go to `Settings → About phone → Tap Build number 7 times` to unlock Developer options.
   - Then `Settings → Developer options → USB debugging → ON`.
6. Connect your phone via USB. Accept the RSA key fingerprint.
7. Click the green **Run** button (▶) in Android Studio. The app will be installed and launched.

#### **Way 2: Command Line (No Android Studio)**

Perfect if you have limited internet or want a lightweight build.

**Step 1: Install Android Command Line Tools**

- Download the `commandlinetools-mac-*` (or linux/windows) from [Google’s official site](https://developer.android.com/studio#command-line-tools-only).
- Extract to a folder, e.g., `~/android-sdk`.
- Set environment variables (add to `~/.bashrc` or `~/.zshrc`):
  ```bash
  export ANDROID_HOME=~/android-sdk
  export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
  ```
- Install required components:
  ```bash
  sdkmanager "build-tools;29.0.3" "platforms;android-29" "platform-tools"
  sdkmanager --licenses   # accept all
  ```

**Step 2: Build the APK**

- Navigate to the `android` folder (where `gradlew` is).
- Make gradlew executable: `chmod +x gradlew`
- Build debug APK: `./gradlew assembleDebug`
- Find your APK at: `app/build/outputs/apk/debug/app-debug.apk`
- Transfer to phone and install.

#### **Way 3: Use Pre‑built APK (if available)**

If your group provides an `airmouse.apk`, simply copy it to your phone and install. You may need to enable “Install from unknown sources” in settings.

---

### PC Server – Installation & Proxy Fix

Many users face a **SOCKS proxy error** on macOS/Linux when installing `pyautogui`. Here is the complete fix.

#### **Step 1: Remove Proxy Environment Variables**

Open a terminal and run:

```bash
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
```

To make this permanent, edit your shell config (`~/.zshrc` or `~/.bashrc`) and remove any lines that set these variables.

#### **Step 2: Install pyautogui**

Use the provided mirror (if internet is restricted) or the default PyPI:

```bash
# Using the course mirror (recommended for restricted networks)
pip3 install pyautogui -i https://pypi.devneeds.ir/simple/

# If mirror fails, try without proxy:
pip3 install pyautogui --proxy ""

# Or fallback to default PyPI:
pip3 install pyautogui
```

If you still get a SOCKS error, install `PySocks` first:

```bash
pip3 install PySocks
pip3 install pyautogui
```

#### **Step 3: Run the Server**

```bash
cd AirMouse/pc
python3 server.py
```

You should see:
```
Server listening on 0.0.0.0:8080
```

#### **Step 4: Find Your Computer’s IP Address**

- **macOS/Linux:** `ifconfig` or `ip addr show` → look for `inet 192.168.x.x` or `10.x.x.x`
- **Windows:** `ipconfig` → look for `IPv4 Address`

> Do **not** use `127.0.0.1` (localhost) – the phone cannot connect to that.

---

## Calibration Deep Dive

The accuracy of the Air Mouse depends entirely on proper calibration. Here is what each calibration does and why it is necessary.

### 1. Gyroscope Bias Removal
- **Why:** Gyroscopes have a constant offset (bias) even when stationary. Without removal, the cursor drifts.
- **How it works:** The app records 500 samples while the phone is still, averages them, and subtracts that bias from all future readings.
- **User action:** Keep phone absolutely still on a table for a few seconds when prompted.

### 2. Magnetometer Hard‑Iron Calibration
- **Why:** Magnetometers are affected by nearby magnetic fields (speakers, metal). This creates a “hard iron” offset.
- **How it works:** The app records min/max values for each axis while you move the phone in all orientations. Offset = (max+min)/2, scale = (max‑min)/2.
- **User action:** Move the phone in a **figure‑8 pattern** for 30 seconds, covering all 3D orientations.

### 3. Accelerometer Calibration (Simplified)
- **Why:** Accelerometer readings may have offset and scale errors. Ideally, when stationary, the magnitude should be 9.81 m/s².
- **How it works:** The current simplified version assumes factory calibration is sufficient. For full precision, you would collect data in 6 orientations (gravity along ±X, ±Y, ±Z) and solve for offset/scale.
- **User action:** None required beyond the simplified method. If you notice tilt errors, implement the 6‑orientation method from the exercise statement.

---

## Gesture Detection & Sensitivity Tuning

### Click Detection
- **Sensor:** Gyroscope Y‑axis angular velocity (rad/s).
- **Threshold:** Default `5.0 rad/s`. A quick flick around Y‑axis exceeding this triggers a click.
- **Cooldown:** 300 ms to prevent double clicks.
- **To adjust:** Edit `MotionDetector.kt` – change `clickSpeedThreshold`.

### Scroll Detection
- **Sensor:** Accelerometer Y‑axis linear acceleration (m/s²).
- **Threshold:** Default `8.0 m/s²`. A quick linear push up/down exceeding this triggers scroll.
- **Debounce:** After scroll, further motion ignored until acceleration falls below `2.0 m/s²`.
- **To adjust:** Edit `MotionDetector.kt` – change `scrollSpeedThreshold` and `scrollDebounceThreshold`.

### Cursor Movement Sensitivity
- **Mapping:** Roll (X rotation) → vertical movement; Yaw (Z rotation) → horizontal movement.
- **Sensitivity factor:** `0.5` in `MainActivity.kt`. Increase for faster cursor.
- **Adjust on PC:** Also change `MouseController.sensitivity` in `server.py` (default `0.5`).

---

## Network Protocol & Reliability

### Data Format (JSON)

**Move packet:**
```json
{"type": "move", "dx": 12.3, "dy": -5.2}
```

**Click packet:**
```json
{"type": "click", "id": 1700000000000}
```

**Scroll packet:**
```json
{"type": "scroll", "delta": 1, "id": 1700000000001}
```

**Acknowledgment (from PC to phone):**
```json
{"type": "ack", "id": 1700000000000}
```

### Reliability Mechanism
- **Move packets** are sent without ACK – loss is acceptable (just a small jump).
- **Click and scroll packets** include a unique `id`. The phone waits 500 ms for an ACK; if not received, it retransmits once.
- The PC server sends an ACK immediately after performing the click/scroll.

### Port and Protocol
- **Port:** 8080 (can be changed in both `DataSender.kt` and `server.py`).
- **Protocol:** TCP (ensures order and error‑free delivery).
- **Cleartext traffic:** Enabled in `AndroidManifest.xml` for local network only.

---

## Usage Instructions

### First Time (Calibration)
1. Ensure PC server is running (`python server.py`).
2. On phone, open Air Mouse app.
3. Enter PC’s IP address.
4. Tap **“Calibrate Sensors”**. Follow the on‑screen steps.
5. Wait for “Calibration complete!”.

### Normal Operation
1. Tap **“Start Air Mouse”**.
2. Status changes to “Air Mouse Active”.
3. **Cursor:** Rotate phone around Z (horizontal) and X (vertical) axes.
4. **Click:** Flick phone quickly left/right around Y axis.
5. **Scroll:** Push phone quickly up or down (linear movement).
6. The green square rotates in real time to show orientation.

### Stopping
- Close the app or tap the **back button** (the server will wait for reconnection).
- On PC, press `Ctrl+C` to stop the server.

---

## Troubleshooting – All Known Issues

| Problem | Likely Cause | Solution |
|---------|--------------|----------|
| `pip install` fails with “Missing dependencies for SOCKS support” | Proxy environment variables set | Run `unset http_proxy https_proxy` then reinstall. Also unset `all_proxy`. |
| `Connection refused` on phone | PC server not running, wrong IP, or firewall | Start server, verify IP, disable firewall temporarily. |
| Cursor drifts when phone is still | Gyro bias not removed or magnetic interference | Re‑calibrate carefully. Move phone away from speakers. |
| Click not detected | Threshold too high or flick too slow | Lower `clickSpeedThreshold` in code and rebuild. |
| Scroll triggers accidentally | Hand tremor exceeds threshold | Increase `scrollSpeedThreshold` or add low‑pass filter on accelerometer. |
| High latency / lag | WiFi congestion or slow phone | Use 5 GHz WiFi, close other apps, reduce sensor sampling rate. |
| App crashes on open | Missing sensor or permission | Check phone has gyro/accel/mag; grant internet permission. |
| Green square doesn’t move | Sensor fusion not receiving data | Verify sensor registration in `SensorService.kt`. Check logs. |
| Cursor moves opposite direction | Axis sign reversed | Swap sign in `MainActivity.kt` (e.g., use `-deltaX`). |
| APK build fails with “package R does not exist” | Missing resources or Gradle sync error | Run `./gradlew clean` then rebuild. Ensure all XML files are valid. |

---

## Performance Profiling with Perfetto

The exercise requires you to answer 11 questions using **Perfetto**, Google’s system tracing tool. Here is a complete guide.

### Enabling Perfetto on Android

1. On your phone, enable **Developer options** and **USB debugging**.
2. Download the `record_android_trace` script (provided in exercise attachments).
3. Run the trace (adjust duration as needed):

```bash
python record_android_trace -o trace.perfetto-trace -t 10s sched freq idle wm gfx view
```

### Analyzing the Trace

**Method 1: Perfetto UI (requires internet or Docker)**
- Upload the trace to [ui.perfetto.dev](https://ui.perfetto.dev) (if internet allowed).
- Or use the provided Docker image (see appendix).

**Method 2: Python Trace Processor (offline, recommended)**

Install the Perfetto Python library:
```bash
pip install perfetto -i https://pypi.devneeds.ir/simple/
```

Analyze the trace:
```python
from perfetto.trace_processor import TraceProcessor

tp = TraceProcessor(trace='trace.perfetto-trace')

# Example: get all sensor events
sensor_events = tp.query('''
    SELECT ts, name, dur
    FROM slice
    WHERE name LIKE '%sensor%'
    LIMIT 20
''')
for row in sensor_events:
    print(row.ts, row.name)
```

---

## Answers to the 11 Required Questions

Below are **model answers** based on the Air Mouse implementation. Use these as a guide; you must run your own traces to confirm.

### Q1: From request to sensor data – what happens at OS level?
**Answer:** The Android sensor framework uses a **HAL (Hardware Abstraction Layer)**. When an app registers a listener, the system creates a `SensorEventQueue`. The kernel driver polls the sensor hardware at the requested rate. On each hardware interrupt, data is copied to a shared memory region, then delivered via a `SensorService` thread to the app’s registered callback. Perfetto shows `systrace` events like `SensorService::batch`, `SensorService::flush`, and `SensorEventConnection::sendEvents`.

### Q2: Why do raw sensors have errors and how does fusion help?
**Answer:** Raw errors: gyro drift (integration error), accelerometer noise (vibrations), magnetometer offset (hard/soft iron). Fusion (Madgwick) combines their strengths: gyro for high‑frequency rotation, accelerometer for absolute gravity reference (removes drift), magnetometer for absolute yaw reference (removes yaw drift). The algorithm produces a stable quaternion that is not subject to long‑term drift.

### Q3: Compare configured vs actual sampling period (Perfetto)
**Answer:** The app requests `SENSOR_DELAY_GAME` (~20 ms period, 50 Hz). Perfetto shows actual intervals often longer due to system load. Use query:
```sql
SELECT ts, value FROM counter WHERE name = 'sensor_采样率' ...
```

### Q4: Contention between threads?
**Answer:** Yes – when a heavy operation (e.g., garbage collection, UI rendering) runs on the main thread, sensor events may be delayed because `onSensorChanged` is called on the same thread (by default). To avoid contention, the app should use a dedicated `HandlerThread` for sensors. Perfetto’s `sched_switch` events show waiting times.

### Q5: Wake‑up vs non‑wake‑up sensors
**Answer:** Wake‑up sensors (e.g., significant motion) can wake the device from sleep. Non‑wake‑up sensors only deliver events when the CPU is already awake. Wake‑up consumes more battery but ensures responsiveness. For Air Mouse, non‑wake‑up is sufficient because the screen is on.

### Q6: Average CPU time of filter function
**Answer:** Use Perfetto’s `trace_processor` to sum duration of `MadgwickAHRS.update*` calls. Average typically <0.5 ms per call on modern phones.

### Q7: Most processing‑intensive sensor?
**Answer:** Magnetometer – it often requires calibration (min/max search) and the Madgwick algorithm uses it in the gradient descent step. However, gyroscope integration is also heavy. In practice, accelerometer is cheapest.

### Q8: Effect of sampling rate on system processing
**Answer:** Higher rate → more CPU load, more battery drain, but smoother cursor. Lower rate → less load, more latency. The chosen `SENSOR_DELAY_GAME` (20 ms) is a good trade‑off.

### Q9: Latency from sensor to cursor movement
**Answer:** Measured via Perfetto by comparing sensor event timestamp to the time `pyautogui.moveRel()` is called (add network RTT). Typical total: 30–50 ms on same WiFi. Use `adb logcat` + `time` stamps for precise measurement.

### Q10: Thread usage and UI separation
**Answer:** Sensors: run on a separate `HandlerThread` (to not block UI). Network sending runs on `DataSender` thread (extends Thread). UI updates run on main thread via `runOnUiThread`. Events: each sensor produces `SensorEvent` objects. Main thread is only responsible for UI – heavy processing would cause jank.

### Q11: Slow vs sudden movement effect
**Answer:** Slow movement is filtered more heavily (Madgwick has low‑pass characteristics) – cursor moves smoothly. Sudden movement (click/scroll) is detected via thresholds and causes an immediate action. The main difference is that sudden movements may saturate the gyro, requiring clipping in the delta calculation. No significant difference in latency.

---

## Advanced Customization

### Changing Port
- Android: `DataSender.kt` – change `PORT` constant.
- PC: `server.py` – change `port` in `AirMouseServer` constructor.

### Adjusting Madgwick Filter Beta
- `MadgwickAHRS.kt` – constructor parameter `beta`. Increase for more trust in accelerometer (less drift, more jitter). Default `0.1` is fine.

### Using Only Gyro+Accel (No Magnetometer)
- Comment out magnetometer registration in `SensorService.kt`. The Madgwick algorithm will still work but yaw will drift.

### Adding Visual Feedback for Click/Scroll
- Modify `MainActivity.kt` – inside the gesture callback, change the green square color or show a toast.

---

## Project File Structure (Complete)

```
AirMouse/
├── android/
│   ├── app/
│   │   ├── build.gradle
│   │   └── src/
│   │       └── main/
│   │           ├── AndroidManifest.xml
│   │           ├── java/
│   │           │   └── com/airmouse/
│   │           │       ├── MainActivity.kt
│   │           │       ├── sensors/
│   │           │       │   ├── SensorService.kt
│   │           │       │   ├── MadgwickAHRS.kt
│   │           │       │   ├── CalibrationHelper.kt
│   │           │       │   └── MotionDetector.kt
│   │           │       ├── network/
│   │           │       │   └── DataSender.kt
│   │           │       └── ui/ (optional)
│   │           └── res/
│   │               ├── layout/activity_main.xml
│   │               ├── drawable/green_square.xml
│   │               └── values/strings.xml
│   └── gradle/ (wrapper files)
├── pc/
│   ├── server.py
│   ├── requirements.txt
│   └── run.sh
└── README.md
```

---

## Frequently Asked Questions

**Q: Do I need root?**  
No.

**Q: Can I use Bluetooth instead of WiFi?**  
The exercise requires TCP over WiFi. Bluetooth would require different protocol and is not allowed.

**Q: The app says “Calibration complete” but cursor still drifts.**  
Perform the magnetometer figure‑8 again, away from metal objects. Also, check that the gyro calibration was done on a completely still surface.

**Q: How do I get the IP address of my Mac/PC if it changes?**  
Use a static IP in your router settings, or check the IP each time and re‑enter it. The app has a text field for easy changes.

**Q: Can I use this on Linux?**  
Yes – the Python server works on Linux with `python3` and `pyautogui` (may need `xdotool` or `python3-xlib`). The Android app is unchanged.

**Q: The exercise requires answering questions about Perfetto – where do I submit them?**  
Include your answers in the final report PDF along with the trace files.

---

## Credits & License

- **Madgwick algorithm** – Sebastian Madgwick (open source)
- **Android Sensor Framework** – Google
- **PyAutoGUI** – Al Sweigart (BSD license)

This project is **for educational purposes only** at the University of Tehran. Redistribution or commercial use without permission is prohibited.

---

## Final Notes

You now have everything needed to build, run, test, profile, and understand the Air Mouse. Good luck with your embedded systems exercise!

If you encounter any issue not covered here, consult the teaching assistants or open an issue on your GitLab/GitHub repository.

**Happy flying! 🚀**