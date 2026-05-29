# Air Mouse – Embedded Systems Exercise  
**University of Tehran, Faculty of Electrical and Computer Engineering**  
*Second Semester 1404-1405 (2025-2026)*  

## Project Overview
An **Air Mouse** that uses an Android smartphone’s sensors to control a computer’s mouse cursor.  
The system consists of three main components:

- **Android App** – reads and fuses sensor data, detects gestures, and sends commands over TCP.
- **PC Server** (Python / Go) – receives commands, moves the mouse cursor, and sends ACK responses.
- **Perfetto Trace Analysis** – profiles the Android app for performance evaluation.

All communication follows a **standardised JSON‑line protocol** over TCP, with UDP used only for auto‑discovery and mDNS for effortless pairing.

---

## 🎯 Exercise Requirements (All Implemented)

| Requirement | Implementation |
|-------------|----------------|
| **Calibration UI** (gyro, accel, magnetometer) | `CalibrationActivity` with 3 tabs, animated phone orientations, timer, back/next/stop |
| **Sensor fusion** (Madgwick AHRS) | `SensorFusion.kt` – hand‑written, no external libraries |
| **Cursor movement** (Z/X axes) | Euler angles → dx,dy via `GestureDetector.kt` |
| **Click detection** (Y‑axis quick rotation) | Angular velocity threshold + cooldown |
| **Scroll detection** (Y‑axis linear movement) | `TYPE_LINEAR_ACCELERATION` → threshold + cooldown |
| **Visual indicator** (green square) | `fragment_home.xml` → `orientationIndicator` |
| **Click/scroll feedback** | Flashing label in HomeFragment |
| **TCP with JSON** (`DeltaX`, `DeltaY`, `Click`, `Scroll`) | `DataSender.kt` → PC server |
| **ACK & retransmission** | Client retries up to 3 times if no ACK within 500ms |
| **Auto‑reconnect** | `AutoReconnect.kt` |
| **QR code pairing** | `zxing` scanner + QR generation in PC server |
| **UDP discovery** | `UdpDiscoveryClient.kt` + server’s `udp_discovery.py` / `server/udp.go` |
| **mDNS (Bonjour)** | `server/mdns.go` (fixed) |
| **Perfetto tracing** | Tracepoints in sensor callback, filter, network send |
| **Perfetto analysis** | `perfetto_analyzer.py` answers all 11 questions |
| **Adaptive app icon** | `mipmap-anydpi-v26/ic_launcher.xml` with foreground/background vectors |

---

## 📡 Communication Protocol

### Transport
- **TCP** port `8080` – all mouse commands (reliable, ordered).
- **UDP** port `8081` – only for discovery broadcast (no reply needed for movement).

### Message Format
Every message is a single JSON line terminated by `\n`.

#### Client → Server
| Type | Fields | Description |
|------|--------|-------------|
| `move` | `dx` (float), `dy` (float) | Mouse displacement (no ACK required) |
| `click` | `id` (int) | Left click (ACK expected) |
| `doubleclick` | `id` (int) | Double click (ACK expected) |
| `rightclick` | `id` (int) | Right click (ACK expected) |
| `scroll` | `delta` (int), `id` (int) | Scroll amount (positive = up) |
| `hello` | `name` (string) | Device identification on connect |

#### Server → Client
| Type | Fields | Description |
|------|--------|-------------|
| `ack` | `id` (int) | Acknowledges a click/scroll |

### ACK & Retransmission
- The client stores every `click`/`scroll` message in a `PendingCommand` map.
- If no `ack` with matching `id` arrives within **500ms**, the message is resent up to **3 times**.
- `move` messages are **fire‑and‑forget** – they are dropped if the network is congested.

---

## 📁 Project Structure

```
code/
├── android/                  # Android Studio project (Kotlin)
│   ├── app/src/main/java/com/airmouse/
│   │   ├── ui/               # Activities & fragments
│   │   ├── network/          # DataSender, AutoReconnect, UdpDiscoveryClient
│   │   ├── sensors/          # SensorFusion, CalibrationManager, GestureDetector
│   │   ├── calibration/      # CalibrationPagerAdapter, step fragments
│   │   └── utils/            # LogManager, PreferencesManager, ValidationUtils
│   └── ...                   # layouts, drawables, manifest, etc.
├── pc/                       # PC server (Python)
│   ├── airmouse_server/      # Modular server package
│   │   ├── config.py
│   │   ├── mouse_controller.py
│   │   ├── tcp_server.py
│   │   ├── udp_discovery.py
│   │   ├── mdns_advertiser.py
│   │   ├── qr_manager.py
│   │   ├── tray_manager.py
│   │   ├── notification_manager.py
│   │   ├── performance_monitor.py
│   │   └── gui.py
│   └── run.py
├── pc/airmouse-go/           # PC server (Go)
│   ├── config/
│   ├── control/              # MouseController interface + darwin/other implementations
│   ├── server/               # tcp.go, udp.go, mdns.go
│   ├── ui/                   # app.go (Fyne multi‑tab GUI)
│   ├── main.go
│   └── go.mod / go.sum
└── profiling/                # Perfetto config & analysis
    ├── config.pbtx
    └── perfetto_analyzer.py
```

---

## 🔧 Calibration System

### Gyroscope
1. Keep the phone perfectly still on a flat surface.
2. Press **Start** – 100 samples are collected.
3. The mean bias is computed and subtracted from all future readings.

### Accelerometer (6‑position)
- The user is guided through **6 orientations** with an animated phone graphic.
- For each position, 100 samples are collected.
- Offsets and scale factors are calculated so that gravity reads exactly 9.81 m/s².

### Magnetometer (hard‑iron)
- The user moves the phone in a **figure‑8** pattern until 200 samples are collected.
- Min/max per axis are used to compute hard‑iron offset and soft‑iron scale.

All calibration data is saved in `SharedPreferences` and applied before sensor fusion.

---

## ⚙️ Sensor Fusion & Gestures

- **Madgwick AHRS** fuses gyroscope, accelerometer, and magnetometer to produce a drift‑free orientation.
- **Euler angles** (pitch, roll, yaw) are extracted from the quaternion.
- **Gesture mapping**:
  - **Horizontal movement** ← pitch (Z‑axis rotation)
  - **Vertical movement** ← roll (X‑axis rotation)
  - **Click** ← quick yaw left (angular velocity > threshold)
  - **Scroll** ← rapid linear acceleration along Y‑axis

All thresholds and the sensitivity multiplier (0.2–2.0) are configurable in the app or in the PC server.

---

## 🖥️ PC Server (Python / Go)

### Python Server
- Professional dark‑mode GUI built with `tkinter`.
- QR code generation, live log, client list, tray icon, etc.
- Run: `cd pc && pip install -r requirements.txt && python run.py`

### Go Server
- Beautiful multi‑tab GUI built with **Fyne** (Dashboard, Network, Clients, Settings, Logs).
- Statically linked binary – no runtime dependencies.
- Cross‑platform (Windows, macOS, Linux) thanks to platform‑specific mouse controllers.
- Run: `cd pc/airmouse-go && go run .`

Both servers implement the **same protocol**, so you can use either one with the Android app.

---

## 📱 Android App

### Key Features
- **Home screen**: IP/port entry, QR scan button, device name, sensitivity slider.
- **Calibration wizard**: step‑by‑step with visual animations.
- **Live log**: in‑app and full‑screen view with filtering.
- **Auto‑reconnect**: seamless recovery after network disruptions.
- **ACK/retransmission**: reliable delivery of clicks and scrolls.

### Build & Install
1. Open `android/` in Android Studio.
2. Ensure `gradle.properties` has `android.useAndroidX=true`.
3. Build APK: `./gradlew assembleDebug`
4. Install on device: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

---

## 📊 Profiling (Perfetto)

1. Record a trace while using the app:
   ```
   python record_android_trace -o trace_file.perfetto-trace -t 15s sched freq idle wm gfx view
   ```
2. Run the analysis script:
   ```
   python profiling/perfetto_analyzer.py trace_file.perfetto-trace
   ```
3. The script outputs answers to all 11 required questions, including average filter CPU time, sampling period, thread contention, and end‑to‑end latency.

---

## 🧪 Testing

### Go Server
```bash
cd pc/airmouse-go
go test ./...
```

### Android
- Manual testing on a physical device (API 29+).
- UI test (`CalibrationUITest.kt`) can be added to verify calibration workflow.

---

## 📦 Deliverables

- `CPS-CA2-<SID1>-<SID2>-<SID3>-<SID4>.zip`
  - Source code (Android + PC server + profiling tools)
  - APK file
  - Perfetto trace and analysis results
  - Report (with screenshots and answers to all questions)
  - Short video (≤5 min) showing phone and laptop simultaneously

---

## 👥 Authors
- **Arian Firoozi**
- **Arsalan Talaee**
- *(Your group members)*

Supervised by **Dr. Mohsen Shokri** and **Dr. Mehdi Kargahi**.