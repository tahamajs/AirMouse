# Complete Explanation of the Air Mouse Troubleshooting Guide

This document provides an **exhaustive explanation** of the troubleshooting guide for the Air Mouse project. Each issue is dissected: why it happens, how to diagnose it, and why the suggested fix works. This guide complements the original `TROUBLESHOOTING.md` and is intended to help you understand the underlying causes so you can prevent or quickly resolve problems.

---

## 1. PC Server Issues

### 1.1 `pip install pyautogui` fails with SOCKS error

**Symptom:**
```
ERROR: Could not install packages due to an OSError: Missing dependencies for SOCKS support.
```

**Why it happens:**  
Your terminal or system has `http_proxy`, `https_proxy`, or `all_proxy` environment variables set to a **SOCKS proxy** (e.g., `socks5://127.0.0.1:1080`). `pip` attempts to use that proxy, but the `PySocks` library is not installed. SOCKS proxies require additional Python packages (`PySocks`) that are not part of the standard `pip`.

**Diagnosis:**  
Run `echo $http_proxy` (macOS/Linux) or `echo %http_proxy%` (Windows). If it returns a value starting with `socks`, the problem is confirmed.

**Fix:**  
```bash
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
```
This removes all proxy variables for the current terminal session. `pip` then uses a direct connection.  
**Why this works:** `unset` deletes the environment variables, so `pip` no longer tries to use a proxy.

**Alternative (if you must keep the proxy):**  
Install `PySocks` first: `pip install PySocks`. Then `pip` can handle SOCKS proxies.

**Prevention:**  
If you don’t need a proxy, remove the lines that set these variables from your shell configuration file (`.bashrc`, `.zshrc`, etc.).

---

### 1.2 `pip install pyautogui` fails with “Could not find a version that satisfies the requirement”

**Symptom:**  
`ERROR: Could not find a version that satisfies the requirement pyautogui`

**Why it happens:**  
The provided mirror URL (`https://pypi.devneeds.ir/simple/`) is unreachable (network restriction, mirror down, or URL typo). `pip` cannot locate the package.

**Fix:**  
Use the default PyPI (no `-i` flag):  
```bash
pip install pyautogui
```
Or try a different mirror if allowed.

**Why the mirror is used:**  
The exercise may be conducted in environments with limited international bandwidth. The course provides a local mirror to speed up downloads. If the mirror fails, fall back to the default PyPI.

---

### 1.3 Server won’t start – “Address already in use”

**Symptom:**  
`OSError: [Errno 48] Address already in use` (macOS/Linux) or `OSError: [WinError 10048] Only one usage of each socket address is normally permitted` (Windows).

**Why it happens:**  
Another process is already listening on port 8080 (the default port for Air Mouse). Common culprits: an earlier instance of the server that wasn’t closed properly, a web server, or a different application.

**Fix:**  
- On macOS/Linux: `lsof -i :8080` to find the PID, then `kill -9 <PID>`.  
- On Windows: `netstat -ano | findstr :8080` to get the PID, then `taskkill /PID <PID> /F`.  
- Or change the port (see “Changing TCP Port” in advanced customisation).

**Why this works:** Killing the conflicting process releases the port. Changing the port avoids the conflict entirely.

---

### 1.4 Server runs but phone cannot connect (Connection refused)

**Symptom:**  
Android app shows “Network error: Connection refused” or similar.

**Checklist:**  
1. **Server running?** Confirm the terminal shows “Server listening on 0.0.0.0:8080”.  
2. **Same WiFi?** Both devices must be on the same subnet.  
3. **IP address correct?** On the phone, you must enter the PC’s local IP (e.g., `192.168.1.10`), not `127.0.0.1` or the public IP.  
4. **Firewall blocking?** Temporarily disable firewall to test. If it works, add an exception for port 8080.  
5. **Client isolation?** Some public WiFi networks prevent device‑to‑device communication. Use a private WiFi or phone hotspot.

**Why `127.0.0.1` doesn’t work:** That IP always refers to the same device (the phone). The phone cannot reach the PC via localhost.

---

## 2. Android App Issues

### 2.1 App crashes immediately on opening

**Symptom:**  
The app shows a white screen for a second then closes.

**Why it happens:**  
- Missing sensor (phone lacks gyroscope, accelerometer, or magnetometer).  
- Missing permission (Internet, Vibrate, or overlay).  
- Resource not found (typo in layout ID).

**Diagnosis:**  
Connect the phone to a PC and run `adb logcat | grep -i exception`. Look for a stack trace.

**Fixes:**  
- If `SensorManager.getDefaultSensor` returns null → use a different phone (or add fallback code).  
- If `Permission denied` → re‑install the app and grant permissions manually.  
- If `No view found for id` → check that all IDs in `MainActivity.kt` match `activity_main.xml`.

---

### 2.2 Cursor drifts when phone is still

**Symptom:**  
Phone resting on a table, but cursor slowly moves.

**Why it happens:**  
Gyroscope bias not properly removed. Either calibration was skipped, or the phone moved during the gyro calibration step (e.g., you held it in your hand).

**Fix:**  
Re‑calibrate the gyroscope **on a flat, stationary surface** (table). Do not hold the phone. The calibration process averages 500 samples; any movement introduces error.

**Why this works:** Subtracting the average of stationary samples removes the constant offset (bias) from all future readings.

---

### 2.3 Click not detected

**Symptom:**  
You flick the phone but no click occurs.

**Why it happens:**  
The angular speed `|gyroY|` did not exceed the `clickSpeedThreshold` (default 5.0 rad/s). Either the flick was too slow, or the threshold is too high.

**Fix:**  
- Lower the threshold in Settings (e.g., to 3.0 rad/s).  
- Or flick harder – practice a quick, sharp rotation.

**Why the threshold exists:** To prevent accidental clicks from normal rotation. Lowering it makes the gesture more sensitive.

---

### 2.4 Right‑click not detected

**Symptom:**  
Tilting the phone sideways does nothing.

**Why it happens:**  
- The tilt angle `|roll|` did not exceed `rightClickTiltAngle` (default 45°).  
- Or you did not hold the tilt long enough for `rightClickDuration` (500 ms).

**Fix:**  
- In Settings, decrease the tilt angle (e.g., to 30°) or increase the duration.  
- Practise tilting further and holding steady.

---

### 2.5 Scroll triggers randomly when phone is still

**Symptom:**  
Page scrolls even though you are not moving the phone.

**Why it happens:**  
Hand tremor or sensor noise causes `|accelY|` to exceed `scrollSpeedThreshold` (8.0 m/s²) momentarily.

**Fix:**  
Increase the scroll threshold (e.g., to 10.0 or 12.0 m/s²) in Settings. This requires a more deliberate push.

**Why this works:** Raising the threshold filters out small accidental movements.

---

### 2.6 Green square doesn’t rotate

**Symptom:**  
The green square stays fixed, even when you rotate the phone.

**Why it happens:**  
- The orientation callback is not set, or the sensor service is not running.  
- Fusion algorithm not receiving data (e.g., missing sensor permission).

**Fix:**  
- Ensure you tapped “Start Air Mouse” after calibration.  
- Check the debug overlay (if enabled) – roll/yaw values should change.  
- Re‑calibrate sensors.  
- Restart the app.

---

### 2.7 App shows “Calibration complete” but calibration actually failed

**Symptom:**  
Toast says “Calibration done” but cursor still drifts or gestures don’t work.

**Why it happens:**  
The calibration functions may have thrown an exception that was caught silently, or the user did not follow instructions (e.g., moved during gyro step).

**Fix:**  
Force re‑calibration with careful steps:  
- **Gyro:** Place phone on a perfectly still table – do not hold it.  
- **Magnetometer:** Move in a large figure‑8 pattern covering all axes, away from metal objects.  
- **Accelerometer:** (Simplified) keep phone still on a flat surface.

---

## 3. Network & Connection Problems

### 3.1 `Connection refused` on phone

(Already covered in 1.4, but here with more depth)

**Why “Connection refused” appears:**  
- The PC server is not running (no one listening on port 8080).  
- The IP address is wrong or the subnet is different.  
- A firewall is blocking the connection.  
- The phone’s WiFi is in client isolation mode.

**Fix steps:**  
1. Confirm server is listening: `netstat -an | grep 8080` should show `LISTEN`.  
2. On PC, run `ifconfig` (macOS/Linux) or `ipconfig` (Windows) to get the correct IP.  
3. Temporarily disable the firewall.  
4. Move both devices to the same private WiFi (e.g., home router) or use the phone’s hotspot.

---

### 3.2 High latency / cursor lags behind phone movement

**Symptom:**  
Cursor moves noticeably after you rotate the phone (delay >100 ms).

**Why it happens:**  
- WiFi congestion (many devices on same channel).  
- Phone CPU overloaded (battery saver mode, background apps).  
- PC server running on a slow machine or under heavy load.

**Fix:**  
- Use 5 GHz WiFi if available (less interference).  
- Close other apps on phone and PC.  
- Reduce sensor sampling rate to `SENSOR_DELAY_NORMAL` in `SensorService.kt` (reduces data rate, but also reduces smoothness).  
- On PC, ensure no other CPU‑intensive tasks.

---

### 3.3 Connection drops frequently or auto‑reconnect fails

**Symptom:**  
“Disconnected” appears in server log, then reconnects after a few seconds.

**Why it happens:**  
- Unstable WiFi (signal strength low).  
- Router drops idle TCP connections.  
- Phone goes into deep sleep (if battery saver interferes).

**Fix:**  
- Move phone and PC closer to the router.  
- On PC, add a TCP keep‑alive (not in basic server, but you can modify the code).  
- In Android, ensure `AutoReconnect` is enabled and working (it tries every 5 seconds).

---

## 4. Calibration & Sensor Problems

### 4.1 Cursor drifts even when phone perfectly still

**Cause:** Gyro bias not removed – phone moved during calibration or calibration skipped.

**Fix:** Re‑calibrate gyro on a flat, stationary surface. Do not hold the phone.

### 4.2 Yaw drifts over time (horizontal orientation changes)

**Cause:** Magnetometer calibration incomplete (hard‑iron offset not fully measured) or magnetic disturbance.

**Fix:** Re‑calibrate magnetometer by moving the phone in a large figure‑8 for the full 30 seconds, away from metal objects.

### 4.3 Tilt (roll/pitch) is inaccurate – phone level but cursor moves vertically

**Cause:** Accelerometer not calibrated or simplified 1‑point method insufficient.

**Fix:** Implement the full 6‑point accelerometer calibration (collect data in ±X, ±Y, ±Z orientations).

---

## 5. Performance & Battery Issues

### 5.1 Phone gets hot and battery drains fast

**Cause:** Sensor sampling at 50 Hz continuously, even when phone is not moving.

**Fix:** Enable battery saver (default on). It reduces sampling rate to 20 Hz after 10 seconds of no movement. If not working, check `BatterySaver.kt` logs.

### 5.2 Cursor movement is choppy / stuttering

**Cause:** Network jitter or PC CPU spikes.

**Fix:** Lower sensor sampling rate (change `SENSOR_DELAY_GAME` to `SENSOR_DELAY_NORMAL` in `SensorService.kt`). On PC, close other resource‑heavy applications.

---

## 6. Build & Compilation Problems

### 6.1 `./gradlew: Permission denied` (macOS/Linux)

**Fix:** `chmod +x gradlew`

### 6.2 Gradle sync fails with “Could not resolve all dependencies”

**Cause:** Internet restriction or missing repository.

**Fix:** Ensure internet access; if behind a proxy, set Gradle proxy settings in `~/.gradle/gradle.properties`.

### 6.3 `sdkmanager` fails with “java.net.SocketException: Permission denied”

**Cause:** Firewall or antivirus blocking `sdkmanager`.

**Fix:** Temporarily disable firewall, or run the script with admin privileges.

### 6.4 APK build succeeds but APK size is very small (<1 MB)

**Cause:** Build failed silently, producing an empty or corrupted APK.

**Check:** Run `./gradlew assembleDebug --stacktrace` to see full error. Common cause: missing Java or incorrect `ANDROID_HOME`.

---

## 7. Video Recording & Submission Issues

### 7.1 Video shows phone screen only, not laptop screen

**Fix:** Use a second phone or webcam to record both screens together, or use screen mirroring software like `scrcpy` to show phone screen on laptop, then record laptop screen only.

### 7.2 Video length exceeds 5 minutes

**Fix:** Practice the script provided in `VIDEO_SCRIPT.md` – it fits within 5 minutes. Remove unnecessary pauses.

### 7.3 Perfetto trace file too large to submit

**Fix:** Compress the trace (`gzip` or `zip`) before uploading. The exercise only requires a short trace (10 seconds), which should be manageable.

---

## Summary of Quick Fixes

| Problem | Most Likely Fix |
|---------|----------------|
| `pip install` SOCKS error | `unset http_proxy https_proxy` |
| `Connection refused` | Check PC IP, firewall, server running |
| Cursor drifts | Re‑calibrate gyro (phone still on table) |
| Click not detected | Lower `clickSpeedThreshold` in Settings |
| Right‑click not detected | Lower `rightClickTilt` or increase duration |
| High battery drain | Enable battery saver (default on) |
| Build fails – Java not found | Install JDK 11, set `JAVA_HOME` |

---

This explanation of the troubleshooting guide should enable you to diagnose and fix almost any issue with Air Mouse. Keep it handy during development and testing.