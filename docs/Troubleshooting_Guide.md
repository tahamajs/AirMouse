# Air Mouse – Complete Troubleshooting Guide

This document covers **every possible issue** you may encounter while building, installing, calibrating, and using the Air Mouse. Each problem is explained with its root cause, diagnostic steps, and a proven solution. Use this as your first resource when something doesn’t work.

---

## 📖 Table of Contents

1. [Installation & Dependency Issues](#installation--dependency-issues)
2. [Network & Connection Problems](#network--connection-problems)
3. [Android App Issues](#android-app-issues)
4. [Calibration & Sensor Problems](#calibration--sensor-problems)
5. [Cursor Movement & Gesture Problems](#cursor-movement--gesture-problems)
6. [Performance & Battery Issues](#performance--battery-issues)
7. [Build & Compilation Problems](#build--compilation-problems)
8. [Video Recording & Submission Issues](#video-recording--submission-issues)

---

## 1. Installation & Dependency Issues

### 1.1 `pip install pyautogui` fails with SOCKS proxy error

**Symptoms:**
```
ERROR: Could not install packages due to an OSError: Missing dependencies for SOCKS support.
```

**Root cause:** Your terminal or system has `http_proxy`/`https_proxy` environment variables set to a SOCKS proxy, but `pip` lacks the `PySocks` library.

**Diagnostic steps:**
```bash
echo $http_proxy
echo $https_proxy
```

If they return a value (e.g., `socks5://...`), the proxy is active.

**Solution (temporary – per session):**
```bash
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
pip install pyautogui
```

**Solution (permanent):** Remove the proxy lines from your shell config (`.bashrc`, `.zshrc`, `.bash_profile`).

**Alternative:** Install `PySocks` first:
```bash
pip install PySocks
pip install pyautogui
```

### 1.2 `pip install` fails with “Could not find a version that satisfies the requirement”

**Symptoms:** `ERROR: Could not find a version that satisfies the requirement pyautogui`

**Root cause:** The mirror URL (`https://pypi.devneeds.ir/simple/`) may be unreachable due to network restrictions, or the index is down.

**Solution:** Use the default PyPI (if internet is unrestricted):
```bash
pip install pyautogui
```

Or use a different mirror:
```bash
pip install pyautogui -i https://pypi.org/simple/
```

### 1.3 Java not found when running `build_apk.sh`

**Symptoms:** `[ERROR] Java not found. Please install Java 11 (JDK) and add it to PATH.`

**Root cause:** Java JDK 11 is not installed or not in PATH.

**Solution (macOS):**
```bash
brew install openjdk@11
export PATH="/usr/local/opt/openjdk@11/bin:$PATH"
```

**Solution (Ubuntu/Debian):**
```bash
sudo apt install openjdk-11-jdk
```

**Solution (Windows):** Download from [Adoptium](https://adoptium.net/temurin/releases/?version=11) and run the installer – check “Add to PATH”.

**Verify:**
```bash
java -version
# Should show openjdk version "11.0.x"
```

### 1.4 `sdkmanager` not found when building APK

**Symptoms:** `sdkmanager not found. Make sure Android SDK command-line tools are installed...`

**Root cause:** Android command‑line tools are missing or not extracted to the expected location (`~/android-sdk/cmdline-tools/latest/`).

**Solution:**

1. Download from [Google](https://developer.android.com/studio#command-line-tools-only).
2. Extract to `~/android-sdk/cmdline-tools/`.
3. Rename the extracted folder to `latest`:
   ```bash
   mkdir -p ~/android-sdk/cmdline-tools
   unzip commandlinetools-linux-*.zip -d ~/android-sdk/cmdline-tools
   mv ~/android-sdk/cmdline-tools/cmdline-tools ~/android-sdk/cmdline-tools/latest
   ```
4. Set `ANDROID_HOME` and update `PATH`:
   ```bash
   export ANDROID_HOME=~/android-sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
   ```

### 1.5 Android app installation fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

**Symptoms:** ADB says `INSTALL_FAILED_UPDATE_INCOMPATIBLE` or the phone says “App not installed”.

**Root cause:** An older version of Air Mouse is still installed, or the signatures don’t match.

**Solution:** Uninstall the existing version first:
```bash
adb uninstall com.airmouse
```
Then re‑install the APK.

---

## 2. Network & Connection Problems

### 2.1 `Connection refused` on phone when tapping “Start”

**Symptoms:** Toast message “Network error: Connection refused” or status stays “Not connected”.

**Root causes:**
- PC server is not running.
- IP address is wrong (e.g., `127.0.0.1` or a different subnet).
- Firewall blocks port 8080.
- Phone and PC are on different WiFi networks.

**Diagnostic steps:**

1. On PC, check if server is listening:
   ```bash
   netstat -an | grep 8080
   ```
   Should show `LISTENING` (Windows) or `LISTEN` (Linux/macOS).

2. On PC, find your correct IP:
   - **Windows:** `ipconfig` → look for `IPv4 Address` under your active WiFi adapter (e.g., `192.168.1.10`).
   - **macOS/Linux:** `ifconfig` or `ip addr` → look for `inet 192.168.x.x` or `10.x.x.x`.

3. Temporarily disable firewall:
   - **Windows:** Control Panel → Windows Defender Firewall → Turn off (test only).
   - **macOS:** System Settings → Network → Firewall → Off.
   - **Linux:** `sudo ufw disable` (Ubuntu) or `sudo systemctl stop firewalld`.

4. On phone, verify WiFi connection: Settings → WiFi → same network name as PC.

**Solution:** Ensure server is running (`python gui.py`), enter the correct IP, and re‑enable firewall after adding an exception for port 8080.

### 2.2 High latency / cursor lags behind phone movement

**Symptoms:** Cursor moves noticeably after you rotate the phone (delay >100 ms).

**Root causes:**
- WiFi congestion (many devices on same channel).
- Phone CPU overloaded (battery saver mode, background apps).
- PC server running on a slow machine or under heavy load.

**Diagnostic steps:**
- Test with a different WiFi network (e.g., phone hotspot).
- Close other apps on phone and PC.
- Check PC CPU usage (Task Manager / `top`).

**Solutions:**
- Use 5 GHz WiFi if available (less interference).
- Reduce sensor sampling rate (change to `SENSOR_DELAY_NORMAL` in `SensorService.kt`).
- Increase server sensitivity to reduce packet frequency? Not recommended. Instead, optimise PC code.

### 2.3 Connection drops frequently or auto‑reconnect fails

**Symptoms:** “Disconnected” appears in server log, then reconnects after a few seconds.

**Root causes:**
- Unstable WiFi (signal strength low).
- Router drops idle TCP connections.
- Phone goes into deep sleep (if battery saver interferes).

**Solutions:**
- Move phone and PC closer to router.
- On PC, add a TCP keep‑alive (not implemented in basic server, but you can add a periodic heartbeat message from phone).
- In Android, ensure `AutoReconnect` is enabled and working (it tries every 5 seconds).

---

## 3. Android App Issues

### 3.1 App crashes immediately on opening

**Symptoms:** The app shows a white screen for a second then closes.

**Root causes:**
- Missing sensor (phone lacks gyroscope or magnetometer).
- Missing permission (Internet, Vibrate).
- Resource not found (layout ID mismatch).

**Diagnostic steps:**
- Connect phone to PC, run `adb logcat | grep -i exception` to see the crash log.

**Common exceptions and fixes:**
| Exception | Cause | Solution |
|-----------|-------|----------|
| `SensorManager.getDefaultSensor returned null` | Phone lacks sensor | Use a different phone (or add fallback in code). |
| `Permission denied` for `INTERNET` | Permission not granted | Re‑install app and grant permissions manually. |
| `No view found for id` | XML ID typo | Check that all IDs in `MainActivity.kt` match `activity_main.xml`. |

### 3.2 Green square doesn’t rotate

**Symptoms:** The green square stays fixed, even when you rotate the phone.

**Root causes:**
- Sensor service not started.
- Orientation callback not set.
- Fusion algorithm not receiving data.

**Diagnostic steps:**
- Check status text: does it say “Air Mouse Active”?
- In debug overlay (if enabled), do roll/yaw values change?
- Look at `adb logcat` for “SensorService” logs.

**Solution:**
- Ensure you tapped “Start Air Mouse” after calibration.
- Re‑calibrate sensors (especially magnetometer).
- Restart the app.

### 3.3 App shows “Calibration complete” but calibration actually failed

**Symptoms:** Toast says “Calibration done” but cursor still drifts or gestures don’t work.

**Root cause:** The calibration functions may have thrown an exception that was caught silently, or the user did not follow the instructions (e.g., moved phone during gyro calibration).

**Solution:** Force re‑calibration with careful steps:
- **Gyro:** Place phone on a perfectly still table – do not hold it.
- **Magnetometer:** Move in a large figure‑8 pattern covering all axes, away from metal objects.
- **Accelerometer:** (Simplified) keep phone still on a flat surface.

### 3.4 Debug overlay not showing

**Symptoms:** Tapping “Show Debug” does nothing, or a toast says “Overlay permission required”.

**Root cause:** The app does not have `SYSTEM_ALERT_WINDOW` permission.

**Solution:** Grant overlay permission manually:
- Go to phone Settings → Apps → Air Mouse → Overlay permission → Allow.
- Or, on first tap of “Show Debug”, the app should open the permission screen automatically. If not, restart the app.

---

## 4. Calibration & Sensor Problems

### 4.1 Cursor drifts even when phone is perfectly still

**Root cause:** Gyro bias not properly removed – either calibration was skipped or phone moved during gyro calibration.

**Solution:** Re‑calibrate gyro **on a flat, stationary surface**. Do not hold the phone in your hand.

### 4.2 Yaw (horizontal orientation) drifts over time

**Root cause:** Magnetometer calibration was incomplete (hard‑iron offset not fully measured) or there is a magnetic disturbance (laptop speakers, metal desk).

**Solution:**
- Re‑calibrate magnetometer by moving the phone in a **large figure‑8** for the full 30 seconds, covering all orientations.
- Keep the phone away from laptop speakers, magnets, or metal surfaces during use.

### 4.3 Tilt (roll/pitch) is inaccurate (phone level not matching cursor level)

**Root cause:** Accelerometer calibration not performed or simplified 1‑point method insufficient.

**Solution:** Implement the full 6‑point accelerometer calibration (collect data in ±X, ±Y, ±Z orientations) – see `CalibrationHelper.kt` for the method.

### 4.4 Scroll triggers randomly when phone is still

**Root cause:** Hand tremor or sensor noise exceeds `scrollSpeedThreshold`.

**Solution:** Increase the scroll threshold in Settings (e.g., from 8.0 to 12.0 m/s²). Also, ensure the phone is held firmly.

### 4.5 Click / double‑click never triggers

**Root cause:** Threshold too high, or user not flicking fast enough.

**Diagnostic:** In debug overlay, watch `GyroY` value while flicking. It should exceed the `clickSpeedThreshold` (default 5.0 rad/s). If not, either lower the threshold or flick harder.

**Solution:** Go to Settings → lower `click speed threshold` to 3.0 or 4.0.

---

## 5. Cursor Movement & Gesture Problems

### 5.1 Cursor moves opposite to phone rotation

**Root cause:** Axis sign convention mismatch between phone and PC.

**Solution:** In `MainActivity.kt`, invert the delta signs:
```kotlin
val deltaX = -(yaw - lastYaw) * sensitivity * 0.8f
val deltaY = -(roll - lastRoll) * sensitivity * 0.8f
```
Rebuild and re‑install.

### 5.2 Cursor jumps erratically

**Root cause:** High sensitivity combined with high‑frequency noise; or network packet loss.

**Solutions:**
- Lower the sensitivity slider.
- Add a low‑pass filter on the deltas (simple moving average) in `MainActivity.kt`.
- Ensure stable WiFi.

### 5.3 Scroll direction is reversed

**Root cause:** Sign of accelerometer Y axis may be opposite on some phones.

**Solution:** In `SensorService.kt`, invert the scroll direction:
```kotlin
if (scroll != 0) gestureCallback?.invoke(
    if (scroll > 0) MotionDetector.Gesture.SCROLL_UP else MotionDetector.Gesture.SCROLL_DOWN
)
```

### 5.4 Right‑click triggers too easily or not at all

**Root cause:** Tilt threshold or hold duration not tuned.

**Solution:** Adjust in Settings:
- Increase `rightClickTilt` (e.g., to 60°) to require more tilt.
- Increase `rightClickDuration` (e.g., 700 ms) to require longer hold.

---

## 6. Performance & Battery Issues

### 6.1 Phone gets hot and battery drains fast

**Root cause:** Sensor sampling at 50 Hz continuously, even when phone is not moving.

**Solution:** Enable battery saver (it is on by default). The app reduces sampling rate to 20 Hz after 10 seconds of no movement. To verify, look at `BatterySaver` logs.

**Manual workaround:** Stop Air Mouse when not in use (tap “Stop” or close the app).

### 6.2 PC server uses high CPU

**Root cause:** The asyncio event loop is busy, or the GUI log area grows too large.

**Solution:**
- Limit log area size (in `gui.py`, set a maximum line count).
- Use console server (`server.py`) if you don’t need GUI.

### 6.3 Cursor movement is choppy / stuttering

**Root cause:** Network jitter or PC CPU spikes.

**Solutions:**
- Lower sensor sampling rate (change `SENSOR_DELAY_GAME` to `SENSOR_DELAY_NORMAL` in `SensorService.kt`).
- On PC, close other resource‑heavy applications.

---

## 7. Build & Compilation Problems

### 7.1 `./gradlew: Permission denied` (macOS/Linux)

**Solution:**
```bash
chmod +x gradlew
```

### 7.2 Gradle sync fails with “Could not resolve all dependencies”

**Root cause:** Internet restriction or missing repository.

**Solution:**
- Ensure you have internet access.
- If behind a proxy, set Gradle proxy settings in `~/.gradle/gradle.properties`:
  ```
  systemProp.http.proxyHost=proxy.youruniversity.edu
  systemProp.http.proxyPort=8080
  ```

### 7.3 `sdkmanager` fails with “java.net.SocketException: Permission denied”

**Root cause:** Firewall or antivirus blocking `sdkmanager`.

**Solution:** Temporarily disable firewall, or run the script with admin privileges.

### 7.4 APK build succeeds but APK size is very small (<1 MB)

**Root cause:** Build failed silently, producing an empty or corrupted APK.

**Check:** Look for error messages in the build output. Common cause: missing Java or incorrect `ANDROID_HOME`.

**Solution:** Run `./gradlew assembleDebug --stacktrace` to see full error.

---

## 8. Video Recording & Submission Issues

### 8.1 Video shows phone screen only, not laptop screen

**Root cause:** Only one camera used.

**Solution:**
- Use a second phone or a webcam to record both screens together.
- Or use screen mirroring software like `scrcpy` to show phone screen on laptop, then record the laptop screen only.

### 8.2 Video length exceeds 5 minutes

**Root cause:** Too much content.

**Solution:** Practice the script (provided in `VIDEO_SCRIPT.md`) – it fits within 5 minutes. Remove unnecessary pauses.

### 8.3 Perfetto trace file too large to submit

**Root cause:** Traces can be hundreds of MB.

**Solution:** Compress the trace (`gzip` or `zip`) before uploading. Or only submit a short trace (10 seconds) as requested.

---

## 9. Quick Reference – Common Solutions Summary

| Problem | Most Likely Fix |
|---------|----------------|
| `pip install` SOCKS error | `unset http_proxy https_proxy` |
| `Connection refused` | Check PC IP, firewall, server running |
| Cursor drifts | Re‑calibrate gyro (phone still on table) |
| Yaw drifts | Re‑calibrate magnetometer (figure‑8, 30s) |
| Click not detected | Lower `clickSpeedThreshold` in Settings |
| Scroll not working | Increase `scrollSpeedThreshold` or flick harder |
| App crashes on open | Check sensor availability, grant permissions |
| Green square doesn’t rotate | Ensure “Start Air Mouse” tapped |
| High battery drain | Enable battery saver (default on) |
| Build fails – Java not found | Install JDK 11, set `JAVA_HOME` |

---

## 10. Getting Further Help

If none of the above solves your issue:

1. **Collect logs:**
   - Android: `adb logcat -s AirMouse:* SensorService:* DataSender:*`
   - PC server: check `airmouse.log` (console server) or the GUI log area.

2. **Check the official exercise forum** (if available) – your problem may have been answered.

3. **Contact teaching assistants** with:
   - Phone model and Android version.
   - PC OS and Python version.
   - Exact error message (screenshot or copy‑paste).
   - Steps you have already tried.

---

**This troubleshooting guide is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.**  
*Keep it handy – it will save you hours of frustration.*