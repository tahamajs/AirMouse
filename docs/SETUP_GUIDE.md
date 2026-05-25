# Complete Explanation of the Air Mouse Setup Guide

This document provides a **thorough, step‑by‑step explanation** of the official Air Mouse setup guide. It covers every command, every prerequisite, and every potential issue you may encounter while installing and configuring the Android app and PC server. Use this as a companion to the original `SETUP_GUIDE.md` to fully understand *why* each step is necessary.

---

## 1. PC Server Setup (All Operating Systems)

The PC server is a Python program that listens for TCP messages from your phone and controls the mouse cursor using the `pyautogui` library. It must be installed and running *before* you start the Android app.

### 1.1 Install Python 3.8 or Higher

**Why Python 3.8+?**  
- The server uses `asyncio.run()`, which was stabilised in Python 3.7. Python 3.8 is the minimum recommended for compatibility with the `perfetto` Python package (used for trace analysis).  
- `pyautogui` works on all Python 3 versions, but the code uses f‑strings and other features that require at least 3.6.

**Instructions per OS:**

| OS | Method | Verification |
|----|--------|--------------|
| **Windows** | Download installer from [python.org](https://python.org). **Check “Add to PATH”** during installation. | `python --version` or `py --version` |
| **macOS** | `brew install python@3.11` (Homebrew) or download the official installer. | `python3 --version` |
| **Linux (Debian/Ubuntu)** | `sudo apt update && sudo apt install python3 python3-pip` | `python3 --version` |

**Potential issue:** On some Linux distributions, `python` may refer to Python 2. Always use `python3` explicitly.

### 1.2 Fix Proxy Error (macOS / Linux)

**Symptom:** `pip install` fails with:
```
ERROR: Could not install packages due to an OSError: Missing dependencies for SOCKS support.
```

**Why this happens:**  
Your terminal environment has `http_proxy` or `https_proxy` variables set to a SOCKS proxy (e.g., `socks5://...`). The `pip` command does not have the `PySocks` library installed, so it cannot handle SOCKS proxies.

**Solution (temporary – per session):**
```bash
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY all_proxy ALL_PROXY
```
This removes all proxy environment variables for the current terminal session. After this, `pip` will use direct connection.

**Solution (permanent):**  
Edit your shell configuration file (`~/.bashrc`, `~/.zshrc`, etc.) and remove or comment out any lines that set `http_proxy`, `https_proxy`, or `all_proxy`. Then restart your terminal.

**Alternative (if you must keep the proxy):**  
Install `PySocks` first:
```bash
pip install PySocks
```
Then proceed with `pip install pyautogui`.

### 1.3 Install `pyautogui`

**Command:**
```bash
pip install pyautogui -i https://pypi.devneeds.ir/simple/
```
or, if the mirror is unavailable:
```bash
pip install pyautogui
```

**What `pyautogui` does:**  
It provides cross‑platform functions to move the mouse cursor, click, scroll, etc. The Air Mouse server uses it to simulate real mouse actions based on the JSON messages received from the phone.

**Why the special mirror?**  
The exercise may be conducted in an environment with restricted internet access. The mirror `https://pypi.devneeds.ir/simple/` is provided by the course to bypass international bandwidth limitations.

**Troubleshooting:**  
- If you see `Could not find a version that satisfies the requirement`, the mirror may be down. Fall back to the default PyPI (no `-i` flag).  
- On macOS, after installation, you must grant **Accessibility permission** to your terminal or Python app (`System Settings → Privacy & Security → Accessibility`). Otherwise `pyautogui` will not move the cursor.

### 1.4 Run the Server

**Command:**
```bash
cd pc
python server.py          # console version
# or
python gui.py             # GUI version
```
Or use the provided launcher:
```bash
python run.py
```

**What you should see:**  
```
Server listening on 0.0.0.0:8080
```
This means the server is waiting for connections from the Android app.

**Which version to use?**  
- **`server.py`** – lightweight, logs to file (`airmouse.log`) and console. Best for headless operation or when you don’t need a GUI.  
- **`gui.py`** – dark mode window with Start/Stop button, live log, and sensitivity slider. Recommended for everyday use and demonstrations.  
- **`run.py`** – automatically checks dependencies and launches `gui.py`.

**Finding your PC’s IP address (required for Android app):**

| OS | Command | Look for |
|----|---------|----------|
| Windows | `ipconfig` | `IPv4 Address` under your active network adapter (WiFi or Ethernet). |
| macOS / Linux | `ifconfig` or `ip addr` | `inet 192.168.x.x` or `10.x.x.x` (ignore `127.0.0.1`). |

**Example:** `192.168.1.10`

---

## 2. Android App Setup

You have three ways to get the Air Mouse app on your phone. Choose the one that fits your environment.

### Method 1: Android Studio (Easiest for Development)

**Prerequisites:**
- Android Studio installed (latest stable).
- USB debugging enabled on your phone (`Settings → Developer options → USB debugging`).

**Steps:**
1. Open Android Studio → **Open an Existing Project** → select the `android` folder.
2. Wait for Gradle sync (first time may take several minutes).
3. Connect your phone via USB. Accept the RSA key fingerprint.
4. Click the green **Run** button (▶).

**What happens behind the scenes:**  
- Gradle compiles all Kotlin code, resources, and the manifest into a debug APK.  
- The APK is signed with a debug key.  
- ADB installs the APK and launches the `MainActivity`.

**Troubleshooting:**  
- `Installation failed with error INSTALL_FAILED_UPDATE_INCOMPATIBLE` → Uninstall any previous version of Air Mouse from your phone first.  
- Gradle sync fails → Check your internet connection; you may need to use the offline mirror or configure a proxy in Android Studio.

### Method 2: Command Line (No Android Studio)

**Ideal for:** Automation, CI/CD, or when you cannot install Android Studio.

**Prerequisites:**  
- Java 11 JDK (not just JRE).  
- Android command‑line tools extracted to `~/android-sdk` (macOS/Linux) or `%USERPROFILE%\android-sdk` (Windows).  
- Environment variable `ANDROID_HOME` set.

**Steps:**
1. Open a terminal in the project root.
2. Run the build script:
   - macOS/Linux: `./build_apk.sh`
   - Windows: `build_apk.bat`
3. The script will:
   - Check for Java and SDK.
   - Accept SDK licenses automatically.
   - Install `build-tools;29.0.3`, `platforms;android-29`, and `platform-tools`.
   - Execute `./gradlew assembleDebug`.
4. The APK is created at `android/app/build/outputs/apk/debug/app-debug.apk`.

**Transfer to phone:**  
- Using ADB: `adb install android/app/build/outputs/apk/debug/app-debug.apk` (requires platform‑tools in PATH).  
- Manually: copy the APK to your phone via USB, cloud, or email, then open it and allow “Install from unknown sources”.

### Method 3: Pre‑built APK

**When to use:** Your group or instructor provides a ready‑made APK file.

**Steps:**
1. Download the `.apk` file to your phone.
2. Tap the file. You may be prompted to allow installation from unknown sources – grant it.
3. Follow the on‑screen instructions.

**Security note:** Only install APKs from trusted sources (e.g., the course website or your own built APK).

---

## 3. Network Configuration

The Android phone and the PC must be able to communicate over TCP. This requires:

1. **Same subnet** – both devices must be connected to the same WiFi network (or the PC can be on Ethernet, but the phone must be on a WiFi that routes to the same subnet).  
   - **Check:** On the PC, run `ipconfig` (Windows) or `ifconfig` (macOS/Linux). The phone’s IP should start with the same first three octets (e.g., `192.168.1.x`).  
   - **Common mistake:** Using `127.0.0.1` (localhost) – that is the phone itself, not the PC.

2. **Firewall allowance** – The PC must allow incoming connections on port `8080` (default).  
   - **Windows:** You may see a pop‑up when you first run the Python server – click “Allow”.  
   - **macOS:** Go to `System Settings → Network → Firewall` → add an exception for Python.  
   - **Linux (Ubuntu with UFW):** `sudo ufw allow 8080`.

3. **No client isolation** – Some public or guest WiFi networks prevent devices from talking to each other. Use a private home WiFi or a phone hotspot instead.

**Testing the network:**  
On the PC, run the server. On the phone, you can use a simple ping test (if you have a terminal app) or just trust that the app will show “Connection refused” if something is wrong.

---

## 4. First Run & Calibration

After installing the APK and starting the PC server, follow these steps on your phone:

### 4.1 Enter the PC’s IP Address

- Open the Air Mouse app.
- Tap the IP address field and type the PC’s IP (e.g., `192.168.1.10`).  
  The app saves the last used IP automatically.

### 4.2 Calibration (Mandatory First Step)

Calibration removes systematic errors from the sensors. **Without calibration, the cursor will drift and gestures will be unreliable.**

**Step 1 – Gyroscope Bias (3 seconds)**  
- **Action:** Place the phone on a flat, stationary surface (e.g., a desk). **Do not hold it** – your hand’s micro‑vibrations will be averaged into the bias.  
- **Why:** The gyroscope outputs a small offset even when still. Averaging 500 samples removes that offset.  
- **Failure symptom:** Cursor drifts even when phone is still.

**Step 2 – Magnetometer Hard‑Iron Calibration (30 seconds)**  
- **Action:** Pick up the phone and move it in a **continuous figure‑8 pattern**, covering all orientations (up, down, left, right, tilted, rotated).  
- **Why:** Hard‑iron distortion (metal interference) shifts the centre of the magnetic field sphere. Recording min/max values over all orientations computes the offset and scale.  
- **Failure symptom:** Yaw (horizontal orientation) drifts over time, or the cursor’s horizontal movement is not aligned with the phone’s rotation.

**Step 3 – Accelerometer (Simplified) (3 seconds)**  
- **Action:** Keep the phone still on a flat surface.  
- **Why:** Accelerometer offset is corrected by averaging stationary samples and subtracting gravity (9.81 m/s²) from the Z axis.  
- **Failure symptom:** Tilt (roll/pitch) is inaccurate – e.g., holding the phone level still causes vertical cursor drift.

### 4.3 Start Air Mouse

- Tap **Start Air Mouse**.  
- The status changes to “Air Mouse Active”.  
- The green square rotates as you turn the phone left/right (yaw).  
- Move your phone:  
  - Rotate left/right (Z‑axis) → cursor moves horizontally.  
  - Nod up/down (X‑axis) → cursor moves vertically.  
  - Quick flick (Y‑axis) → left click.  
  - Two quick flicks → double click.  
  - Tilt sideways >45° and hold → right click.  
  - Quick linear push up/down → scroll.

---

## 5. After Setup – What to Do If Something Fails

| Problem | First check | Next step |
|---------|-------------|-----------|
| `pip install` SOCKS error | Proxy variables | `unset http_proxy https_proxy` |
| `Connection refused` | Server running? IP correct? | `netstat -an | grep 8080`; verify IP; disable firewall |
| Cursor drifts | Calibration skipped or phone moved during gyro step | Re‑calibrate on a flat, still surface |
| No click | Flick too slow or threshold high | Lower `clickSpeedThreshold` in Settings |
| Scroll not working | Push too slow or threshold high | Lower `scrollSpeedThreshold` in Settings |
| APK build fails (sdkmanager not found) | Command‑line tools not in expected location | Extract to `~/android-sdk/cmdline-tools/latest/` |

---

## Summary

The setup guide provides a proven, step‑by‑step path to get Air Mouse running on any platform. By understanding the *why* behind each step – proxy cleaning, Python installation, calibration theory, network configuration – you can confidently troubleshoot any issues. After successful setup, you will have a fully functional motion‑controlled mouse that demonstrates the power of sensor fusion and real‑time communication.

*This explanation is part of the Air Mouse Ultimate documentation – University of Tehran, Embedded Systems Exercise.*