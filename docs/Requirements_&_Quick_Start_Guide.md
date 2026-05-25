# Air Mouse – Complete Requirements & Quick Start Guide

This document provides a **detailed, step‑by‑step explanation** of everything you need to run Air Mouse – from hardware and software requirements to building and starting the system. Each requirement is explained in depth, and every quick‑start step is broken down with prerequisites, expected outcomes, and common pitfalls.

---

## 📖 Table of Contents

1. [Requirements – Detailed Breakdown](#requirements--detailed-breakdown)
   - [Android Side Requirements](#android-side-requirements)
   - [PC Side Requirements](#pc-side-requirements)
2. [Quick Start – Complete Walkthrough](#quick-start--complete-walkthrough)
   - [Step 1: Build & Install the Android App](#step-1-build--install-the-android-app)
   - [Step 2: Run the PC Server](#step-2-run-the-pc-server)
   - [Step 3: Connect & Use](#step-3-connect--use)

---

## Requirements – Detailed Breakdown

### Android Side Requirements

#### 1. Android device with API level 29 (Android 10) or higher

**What does this mean?**  
Android API level 29 corresponds to **Android 10** (released September 2019). Any device running Android 10, 11, 12, 13, 14, or later is compatible.

**Why is this required?**  
- The app uses modern sensor features and permissions (e.g., `SYSTEM_ALERT_WINDOW` for debug overlay) that are fully supported from API 29 onward.
- The TCP socket implementation and background threading work reliably on these versions.
- The minimum SDK is set to 29 in `build.gradle` to avoid compatibility issues with older devices.

**How to check your Android version:**  
`Settings → About phone → Android version`

**If your device has lower than Android 10:**  
The app may still run, but some features (especially overlay) might not work. You can try lowering the `minSdk` in `build.gradle` to 24 (Android 7) and recompile, but this is not officially supported.

#### 2. Sensors: accelerometer, gyroscope, magnetometer (all required)

**Why all three are required:**  
- **Accelerometer** – measures gravity and linear acceleration. Used for tilt (roll/pitch) and scroll detection.
- **Gyroscope** – measures angular velocity. Used for fast flick (click/double‑click) detection.
- **Magnetometer** – measures Earth’s magnetic field. Used to correct yaw drift (horizontal orientation) via sensor fusion.

**What happens if a sensor is missing?**  
- The app will still launch but will show a warning in the sensor status text (e.g., “Missing sensors: Gyroscope”).
- Without a gyroscope, flick detection will not work (no click/double‑click).  
- Without a magnetometer, yaw will drift over time (cursor will slowly rotate horizontally when phone is still).  
- The app uses `uses-feature` with `android:required="true"` in the manifest, so Google Play (or a direct APK install) will not prevent installation on devices without these sensors, but the user will experience reduced functionality.

**How to verify sensors on your phone:**  
- Download a free “Sensor Test” app from the Play Store.
- Or check the phone’s specifications online – most mid‑range and flagship phones since 2016 have all three.

**If your phone lacks a magnetometer:**  
You can still use the app – yaw drift can be minimised by re‑calibrating frequently, or you can disable magnetometer usage (comment out its registration in `SensorService.kt`). However, the exercise explicitly requires all three sensors for full marks.

#### 3. WiFi connection (same network as laptop)

**Why same network?**  
The Android app communicates with the PC server via TCP/IP. Both devices must be on the **same subnet** (e.g., both connected to the same home WiFi router). A phone hotspot can also work if the PC connects to the phone’s hotspot.

**What about cellular data?**  
No – the app uses local network addresses (e.g., `192.168.x.x`). The PC server typically listens on a private IP, not a public one. Using a cellular network would require port forwarding and a public IP, which is not supported.

**Troubleshooting WiFi issues:**  
- Ensure both devices have **WiFi enabled** and are connected to the same network name (SSID).
- If the PC uses Ethernet, it may still be on the same subnet as the WiFi (check IP address ranges). If not, connect the PC to the same WiFi instead.
- Disable **client isolation** (a feature on some routers that prevents devices from talking to each other).

---

### PC Side Requirements

#### 1. Windows / Linux / macOS

**Supported operating systems:**  
- **Windows** – 10 or 11 (64‑bit recommended).  
- **macOS** – 10.15 (Catalina) or newer.  
- **Linux** – any distribution with Python 3.8+ (Ubuntu 20.04+, Fedora 34+, etc.).

**Why not older versions?**  
The Python `asyncio` library and `pyautogui` have been tested primarily on these versions. Older Windows 7 may work but is not officially supported.

**Special notes for macOS:**  
You must grant **Accessibility permission** to the terminal or Python app (Settings → Privacy & Security → Accessibility → add your terminal). Otherwise `pyautogui` cannot control the mouse.

**Special notes for Linux:**  
You may need to install `python3-tk` (for the GUI) and `xdotool` (for `pyautogui` fallback). Also, on Wayland, `pyautogui` may not work – switch to X11 session.

#### 2. Python 3.8+

**Why Python 3.8 or higher?**  
- The code uses `asyncio.run()`, `asyncio.to_thread`, and other features introduced in Python 3.7+ (3.8 recommended for stability).
- The `perfetto` Python package requires Python 3.8+.

**How to check your Python version:**  
```bash
python3 --version
```
If the version is lower (e.g., 3.6), you must upgrade.

**Installing Python 3.8+:**  
- **Windows:** Download from [python.org](https://python.org) – check “Add to PATH”.  
- **macOS:** `brew install python@3.11` (or download installer).  
- **Linux:** `sudo apt install python3.11` (Ubuntu) or use `deadsnakes` PPA.

#### 3. `pyautogui` library (installed automatically)

**What is `pyautogui`?**  
A cross‑platform Python library that programmatically controls the mouse and keyboard. Air Mouse uses it to move the cursor, click, and scroll.

**How it is installed:**  
The provided `run.py` script automatically checks for `pyautogui` and runs `pip install -r requirements.txt` if missing. You can also install manually:
```bash
pip install pyautogui
```

**Potential installation issues (SOCKS proxy):**  
If you see a “Missing dependencies for SOCKS support” error, refer to the troubleshooting section (unset proxy variables).

**Note for macOS:** After installing `pyautogui`, you must still grant Accessibility permission as described above.

#### 4. WiFi connection (same network as phone)

Same requirement as the Android side – both devices must be on the same local network. The PC may use Ethernet, but it must be on the same IP subnet (e.g., IP `192.168.1.x` on both). If your PC uses Ethernet and the phone uses WiFi, they are usually on the same subnet if the router bridges them. Test with `ping <phone-ip>` from the PC.

---

## Quick Start – Complete Walkthrough

### Step 1: Build & Install the Android App

You have two options. Choose the one that suits your workflow.

#### Option A: Using the Provided Script (No Android Studio)

**Why use this method?**  
- Lightweight – no need to download a 2 GB IDE.  
- Automated – the script handles SDK installation and license acceptance.  
- Reproducible – can be run on CI/CD systems.

**Prerequisites (before running the script):**  
- Java 11 JDK installed and `java -version` shows 11.x.  
- Android command‑line tools extracted to `~/android-sdk` (or `%USERPROFILE%\android-sdk` on Windows).  

**Step‑by‑step:**

1. **Open a terminal** in the Air Mouse project root (the folder containing `build_apk.sh` or `build_apk.bat`).

2. **Make the script executable (macOS/Linux):**  
   ```bash
   chmod +x build_apk.sh
   ```

3. **Run the script:**  
   - **macOS/Linux:** `./build_apk.sh`  
   - **Windows:** double‑click `build_apk.bat` or run in Command Prompt.

4. **What happens during the script execution:**  
   - Checks Java version and Android SDK location.  
   - Automatically accepts SDK licenses (no user input needed).  
   - Downloads `build-tools;29.0.3`, `platforms;android-29`, and `platform-tools` (first run only, about 500 MB).  
   - Runs `./gradlew clean assembleDebug`.  
   - Prints the APK location upon success.

5. **Locate the APK:**  
   The file is at `android/app/build/outputs/apk/debug/app-debug.apk`.

6. **Transfer to your phone:**  
   - Using ADB: `adb install android/app/build/outputs/apk/debug/app-debug.apk` (requires USB debugging).  
   - Or copy via USB file transfer, email, or cloud storage, then open the APK on the phone and allow “Install from unknown sources”.

**Common errors & fixes:**  
- `Java not found` → Install JDK 11.  
- `sdkmanager not found` → Re‑extract command‑line tools to the correct folder structure.  
- `Could not resolve dependencies` → Check internet connection; the script uses Google’s Maven repository.

#### Option B: Using Android Studio

**Why use this method?**  
- Easier if you already have Android Studio installed.  
- Provides direct debugging and live code editing.

**Prerequisites:**  
- Android Studio installed (latest stable version).  
- USB debugging enabled on your phone.

**Step‑by‑step:**

1. **Open Android Studio** and select **Open an Existing Project**. Choose the `android` folder.

2. **Wait for Gradle sync** – this may take a few minutes on first open.

3. **Connect your phone** via USB. Ensure USB debugging is on (`Settings → Developer options → USB debugging`).

4. **Click the green Run button** (▶) or press `Control + R`.

5. **Select your device** from the list. The app will be compiled, installed, and launched automatically.

**Note:** Android Studio will download the necessary SDK components automatically if missing.

---

### Step 2: Run the PC Server

**Why two server versions?**  
- `gui.py` – beautiful dark mode GUI with start/stop buttons and live log (recommended for everyday use).  
- `server.py` – lightweight console server that reads `config.json` and logs to file (best for headless or automated setups).

#### Recommended: Using the GUI server

1. **Open a terminal** and navigate to the `pc` folder inside the Air Mouse project.

2. **Run the unified launcher:**  
   ```bash
   python run.py
   ```
   Or double‑click `run.bat` (Windows) or `./run.sh` (macOS/Linux).

3. **What happens automatically:**  
   - The script checks if `pyautogui` is installed. If not, it runs `pip install -r requirements.txt`.  
   - Then it launches `gui.py`.

4. **The GUI window appears:**  
   - Click **Start Server** – the log will show “Server listening on 0.0.0.0:8080”.  
   - Adjust sensitivity with the slider (optional).  
   - Keep this window open while using Air Mouse.

#### Alternative: Using the console server

1. **Install dependencies manually (one time):**  
   ```bash
   pip install -r requirements.txt
   ```

2. **Run the server:**  
   ```bash
   python server.py
   ```
   You will see log output in the terminal.

3. **To stop,** press `Ctrl+C`.

**Finding your PC’s IP address:**  
- **Windows:** Open Command Prompt → `ipconfig` → look for `IPv4 Address` under your active WiFi adapter (e.g., `192.168.1.10`).  
- **macOS/Linux:** `ifconfig` or `ip addr` → look for `inet 192.168.x.x` or `10.x.x.x` (ignore `127.0.0.1`).

---

### Step 3: Connect & Use

#### 3.1 On the phone, open the Air Mouse Ultimate app

- You should see the main screen with:
  - IP address input field
  - Calibrate Sensors button
  - Start Air Mouse button
  - Cursor speed slider
  - Settings and Debug buttons
  - A green square (orientation indicator)

#### 3.2 Enter your PC’s IP address

Type the IP you found in the previous step (e.g., `192.168.1.10`). The app saves the last used IP for convenience.

#### 3.3 Tap Calibrate Sensors (critical first step!)

**Why calibration is essential:**  
Without calibration, the gyroscope bias will cause drift, and the magnetometer offset will make yaw inaccurate. The accelerometer may also have offset errors.

**The calibration process (three steps):**

| Step | Displayed message | User action | Duration |
|------|------------------|-------------|----------|
| 1 | “Gyro calibration – keep phone still” | Place the phone on a **flat, stationary surface** (table). Do not hold it. | ~3 seconds |
| 2 | “Magnetometer – move in figure‑8” | Pick up the phone and move it in a **figure‑8 pattern** for 30 seconds, covering all orientations. | 30 seconds |
| 3 | “Accelerometer – simplified” | Keep the phone still (same as step 1). | ~3 seconds |

After completion, you will see “Calibration complete!” and a toast message.

**What happens if you skip calibration?**  
The cursor may drift, yaw may be incorrect, and gestures may be unreliable. The app will still try to work, but you should always calibrate after installation or when you change location.

#### 3.4 Tap Start Air Mouse

- The app tries to connect to the PC server at the IP address and port 8080.
- If successful, the status changes to “Air Mouse Active”, and the green square becomes responsive.
- On the PC server GUI (or console), you should see a log entry: `✅ Connected: (phone_IP, port)`.

**If connection fails:**  
Check that the PC server is running, the IP is correct, and no firewall blocks port 8080.

#### 3.5 Use the Air Mouse

Now you can control the mouse by moving your phone:

| Phone movement | Action |
|----------------|--------|
| Rotate left/right around Z‑axis (like turning a doorknob) | Move cursor horizontally |
| Rotate up/down around X‑axis (nodding) | Move cursor vertically |
| Quick flick left or right around Y‑axis | Left click |
| Two quick flicks within 400 ms | Double click |
| Tilt phone sideways (>45°) and hold for 0.5 seconds | Right click |
| Quick linear push up or down along Y‑axis | Scroll up/down |

**Visual feedback:**  
- The green square rotates according to the phone’s yaw (horizontal orientation).  
- When a click occurs, the square flashes red briefly.

**Optional – Debug overlay:**  
Tap **Show Debug** to see real‑time values for roll (degrees), yaw (degrees), gyro Y (rad/s), and accel Y (m/s²). This is helpful for tuning thresholds or verifying sensor functionality.

**Stopping:**  
To stop, simply close the app or press the back button. The PC server will remain listening for new connections.

---

## Summary of Success Criteria

After following the quick start steps, you should observe:

- [ ] The Android app builds and installs without errors.
- [ ] The PC server runs and shows “Server listening...”.
- [ ] Calibration completes without error messages.
- [ ] When you tap “Start”, the status changes to “Air Mouse Active”.
- [ ] Moving the phone moves the cursor smoothly.
- [ ] Flicking produces a click on the PC (e.g., in Notepad, a character appears).
- [ ] Pushing the phone up/down scrolls a web page.

If all of these work, you have successfully set up Air Mouse!

---

## Quick Troubleshooting for Quick Start

| Problem | Solution |
|---------|----------|
| APK build fails (Java not found) | Install JDK 11 and set `JAVA_HOME`. |
| APK build fails (sdkmanager missing) | Re‑extract Android command‑line tools to `~/android-sdk/cmdline-tools/latest`. |
| `python run.py` fails with “No module named pyautogui” | Run `pip install pyautogui` manually. |
| Connection refused on phone | Ensure PC server is running, IP is correct, and firewall allows port 8080. |
| Cursor drifts even after calibration | Re‑calibrate gyro on a perfectly flat surface – do not hold it. |
| Click/scroll not working | Check thresholds in Settings; flick/push faster or lower thresholds. |

---

This guide covers every detail of the requirements and quick start. Follow it carefully, and you will have Air Mouse running in minutes. Good luck!