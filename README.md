# ✈️ Air Mouse – Motion-Based Mouse Control using Android Sensors

**University of Tehran – Faculty of Electrical and Computer Engineering**  
*Embedded Systems Exercise – Second Semester 1404-1405 (2025-2026)*

**Designers:** Arian Firoozi, Arsalan Talaee  
**Instructors:** Dr. Mohsen Shokri, Dr. Mehdi Kargahi

---

## 📖 Overview

Air Mouse turns your Android smartphone into a wireless mouse using only the phone's built‑in sensors (accelerometer, gyroscope, magnetometer). By rotating and moving the phone in the air, you can:

- Move the computer cursor (horizontal & vertical)
- Perform left‑click
- Scroll up/down

The phone sends processed motion data over WiFi (TCP) to a Python server on your laptop, which controls the mouse using `pyautogui`.

---

## ✨ Features

- **Sensor fusion** – Madgwick AHRS algorithm for smooth, drift‑free orientation.
- **Calibration** – Gyro bias removal, accelerometer offset/scale, magnetometer hard‑iron correction.
- **Gesture detection**:
  - **Click** – quick rotation around Y‑axis (configurable threshold)
  - **Scroll** – fast linear movement along Y‑axis (up/down)
- **Network reliability** – ACK‑based retransmission for click/scroll packets.
- **Clean UI** – visual orientation indicator, status messages, IP entry.
- **Low latency** – optimized sensor sampling (SENSOR_DELAY_GAME).

---

## 🧰 Requirements

### Android Side
- Android device with **API level 29 (Android 10) or higher**
- Sensors: accelerometer, gyroscope, magnetometer (all required)
- WiFi connection (same network as laptop)

### PC Side
- Windows / Linux / macOS
- Python 3.8+
- `pyautogui` library (installed automatically)
- WiFi connection (same network as phone)

---

## 📲 Installation & Setup

### 1. Android App

#### Option A: Build from source (Android Studio)
```bash
git clone <your-repo-url>
cd AirMouse/android
```
- Open the `android` folder in **Android Studio**.
- Wait for Gradle sync to finish.
- Connect your phone via USB with **USB debugging** enabled.
- Click **Run** (green triangle) or build an APK:  
  `Build → Build Bundle(s) / APK(s) → Build APK(s)`
- Install the generated APK on your phone.

#### Option B: Use pre‑built APK (if provided)
- Download `airmouse.apk` and install it on your phone.

### 2. PC Server

```bash
cd AirMouse/pc
# Make the run script executable (Linux/macOS)
chmod +x run.sh
# Run the server
./run.sh
```
Or manually:
```bash
pip install pyautogui -i https://pypi.devneeds.ir/simple/
python server.py
```

The server will start listening on **port 8080** and show:
```
Server listening on 0.0.0.0:8080
```

---

## 🔧 Calibration (Important!)

Before first use, you **must** calibrate the sensors for accurate performance.

1. Open the Air Mouse app on your phone.
2. Enter your laptop's **IP address** (find it with `ipconfig` on Windows or `ifconfig`/`ip a` on Linux/macOS).
3. Tap **"Calibrate Sensors"** and follow the on‑screen instructions:
   - **Gyro calibration**: Keep the phone completely still for a few seconds.
   - **Magnetometer calibration**: Move the phone in a **figure‑8 pattern** for about 30 seconds (covers all orientations).
   - **Accelerometer calibration**: (Simplified in current version – place phone in 6 different orientations if full precision is needed).
4. Wait for the `Calibration complete!` toast message.

---

## 🖱️ Usage

1. **Start the PC server** (`python server.py`).
2. On the phone, tap **"Start Air Mouse"**.
3. The status changes to `Air Mouse Active`.
4. **Move the cursor** – Rotate the phone:
   - Around **Z‑axis** → horizontal movement
   - Around **X‑axis** → vertical movement
5. **Click** – Quickly rotate the phone around **Y‑axis** (like a flick to the left).
6. **Scroll** – Move the phone **linearly up/down** along the Y‑axis (like a quick push).

The green square in the app rotates to show the current orientation.

---

## 🌐 Network Troubleshooting

| Issue | Solution |
|-------|----------|
| `Connection refused` | Make sure the PC server is running. Check that both devices are on the **same WiFi** and that no firewall blocks port 8080. |
| Cursor moves erratically / drifts | Re‑calibrate the sensors. Make sure you performed the figure‑8 pattern correctly. |
| Click/scroll not detected | Adjust `clickSpeedThreshold` and `scrollSpeedThreshold` in `MotionDetector.kt`. |
| High latency | Use a 5 GHz WiFi network. Ensure no other heavy network usage. |
| App crashes on open | Check Android permissions (Internet, sensors). The phone must have all three sensors. |

---

## 📂 Project Structure

```
AirMouse/
├── android/                         # Android Studio project
│   └── app/src/main/
│       ├── java/com/airmouse/
│       │   ├── MainActivity.kt
│       │   ├── sensors/             # Sensor fusion, calibration, gesture detection
│       │   ├── network/             # TCP client & data sender
│       │   └── ui/                  # ViewModel (optional)
│       ├── res/                     # Layouts, drawables, strings
│       └── AndroidManifest.xml
├── pc/
│   ├── server.py                    # Python TCP server + mouse control
│   ├── requirements.txt
│   └── run.sh                       # One‑click start script
└── README.md                        # You are here
```

---

## 🧪 Testing & Verification

### Manual test
- Open **Notepad** on your laptop.
- Move the cursor using phone rotation → should track smoothly.
- Flick left → should type a character (clicks into Notepad).
- Quickly push phone up/down → page scrolls.

### Performance profiling (Perfetto)
The exercise requires answering 11 questions using Perfetto traces. To collect a trace:
```bash
python record_android_trace -o trace.perfetto-trace -t 10s sched freq idle wm gfx view
```
Then analyse with Perfetto UI or the `perfetto` Python library.

---

## ❓ Frequently Asked Questions

**Q: Why does the cursor drift when the phone is still?**  
A: This is due to gyro bias or magnetometer interference. Re‑calibrate and ensure no magnetic objects (like laptop speakers) are near the phone.

**Q: Can I change the sensitivity?**  
A: Yes – modify `SENSITIVITY` constants in `MainActivity.kt` (currently 0.5) or the `MouseController.sensitivity` in `server.py`.

**Q: Does the phone need to be rooted?**  
A: No. The app uses standard Android sensor APIs and network permissions.

**Q: Why is the app using cleartext traffic?**  
A: For simplicity in a controlled local network. Do not use this over the public internet.

---

## 👥 Contributors

- **Arian Firoozi** – Design & implementation (sensor fusion, gesture detection)
- **Arsalan Talaee** – Network protocol & PC server

---

## 📜 License

This project is for **educational purposes** as part of the University of Tehran's Embedded Systems course.  
Redistribution or commercial use is not permitted without explicit permission from the instructors.

---

## 🙏 Acknowledgements

- Sebastian Madgwick for the open‑source AHRS algorithm.
- Google for Android Sensor Framework and Perfetto.
- PyAutoGUI developers for cross‑platform mouse control.

# AirMouse
