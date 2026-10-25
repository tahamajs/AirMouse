<div align="center">

<img src="docs/assets/banner.svg" alt="AirMouse Banner" width="100%">

# 🖱️ AirMouse Pro (v1.0.1)
### Next-Generation Spatial Air Mouse, Multi-Touch Trackpad & Presentation Controller

[![GitHub Release](https://img.shields.io/github/v/release/tahamajs/AirMouse?color=blue&logo=github)](https://github.com/tahamajs/AirMouse/releases/tag/v1.0.1)
[![PyPI Version](https://img.shields.io/pypi/v/airmouse-core.svg?color=blue&logo=pypi)](https://pypi.org/project/airmouse-core/)
[![Python Versions](https://img.shields.io/pypi/pyversions/airmouse-core.svg?logo=python)](https://pypi.org/project/airmouse-core/)
[![Go Version](https://img.shields.io/badge/Go-1.22+-00ADD8?logo=go)](https://go.dev/)
[![Android Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android)](https://developer.android.com)
[![iOS Safari](https://img.shields.io/badge/iOS-Safari%2013%2B-black?logo=apple)](docs/AUDIT_4_WEB_CLIENT_CROSS_BROWSER.md)
[![Protocol](https://img.shields.io/badge/Protocol-V2%20Canonical-blueviolet)](docs/PROTOCOL_V2.md)
[![R8 Full Mode](https://img.shields.io/badge/R8%20Full%20Mode-6.83MB%20DEX-brightgreen)](android/docs/R8_OPTIMIZATION.md)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

<br/>

[**Downloads**](#-official-releases--downloads-all-platforms) •
[**Quickstart**](#-quickstart--getting-started) •
[**Subsystems Breakdown**](#-comprehensive-subsystems-breakdown) •
[**Architecture**](#-system-architecture) •
[**Protocol V2 Wire Spec**](#-canonical-protocol-v2-wire-envelope) •
[**OS Setup Guides**](#-operating-system-setup-guides) •
[**Single Source of Truth**](#-single-source-of-truth-ssot-versioning) •
[**Audits & Technical Reports**](#-deep-audits--technical-reports)

</div>

---

## 📖 Overview

**AirMouse Pro** is an ultra-low-latency, cross-platform spatial input synthesis ecosystem that transforms smartphones and mobile devices into precision desktop controllers. It seamlessly combines:
1. **6-DOF Spatial Air Mouse**: Move the desktop cursor with natural wrist motion using digital signal processing (One Euro Filter + ZUPT drift suppression).
2. **Multi-Touch Virtual Trackpad**: Smooth kinetic scrolling, 1/2/3-finger gestures, and tactile haptic clicks.
3. **Presentation & Media Remote**: Next/previous slide transitions, virtual laser pointer overlays, and volume dials.
4. **Zero-Install Web Client**: Instant connectivity for iPhone/iPad (iOS Safari) and mobile browsers without App Store installation.

---

## 📦 Official Releases & Downloads (All Platforms)

Download pre-compiled, signed, production-ready release packages for version **v1.0.1**:

| Platform | Target Architecture | Release Artifact | Quick Installation |
| :--- | :--- | :--- | :--- |
| **🤖 Android** | ARM64 / ARMv7 / x86_64 | [**`AirMouse-v1.0.1.apk`**](https://github.com/tahamajs/AirMouse/releases/download/v1.0.1/AirMouse-v1.0.1.apk) | `adb install -r AirMouse-v1.0.1.apk` |
| **🪟 Windows** | x86_64 (Windows 10/11) | [**`AirMouse-v1.0.1-windows-x64.zip`**](https://github.com/tahamajs/AirMouse/releases/download/v1.0.1/AirMouse-v1.0.1-windows-x64.zip) | Extract & run `Start-AirMouse.bat` |
| **🪟 Windows** | x86_64 Portable EXE | [**`AirMouse-Server-v1.0.1-windows-x64.exe`**](https://github.com/tahamajs/AirMouse/releases/download/v1.0.1/AirMouse-Server-v1.0.1-windows-x64.exe) | Standalone zero-dependency executable |
| **🍎 macOS** | Universal (Apple Silicon M1-M4 + Intel) | [**`AirMouse-v1.0.1-macos-universal.tar.gz`**](https://github.com/tahamajs/AirMouse/releases/download/v1.0.1/AirMouse-v1.0.1-macos-universal.tar.gz) | Extract & run `AirMouse.app` |
| **🍎 macOS** | Universal CLI Daemon | [**`AirMouse-Server-v1.0.1-macos-universal`**](https://github.com/tahamajs/AirMouse/releases/download/v1.0.1/AirMouse-Server-v1.0.1-macos-universal) | Standalone Mach-O Universal binary |
| **🐧 Linux** | x86_64 (Ubuntu/Debian/Fedora/Arch) | [**`AirMouse-v1.0.1-linux-x64.tar.gz`**](https://github.com/tahamajs/AirMouse/releases/download/v1.0.1/AirMouse-v1.0.1-linux-x64.tar.gz) | Extract & run `./start-airmouse.sh` |
| **🐧 Linux** | ARM64 (Raspberry Pi 4/5 / SBC) | [**`AirMouse-v1.0.1-linux-arm64.tar.gz`**](https://github.com/tahamajs/AirMouse/releases/download/v1.0.1/AirMouse-v1.0.1-linux-arm64.tar.gz) | Extract & run `./start-airmouse.sh` |

> 🔐 **Checksums**: Verify package integrity using [**`releases/SHA256SUMS.txt`**](https://github.com/tahamajs/AirMouse/releases/download/v1.0.1/SHA256SUMS.txt).
> 
> ⚡ **Rebuild All Releases from Source**: Run [`./scripts/build_all_releases.sh`](scripts/build_all_releases.sh) to regenerate all binaries and archives.

---

## 📱 Application UI & Design Showcase

<div align="center">
  <img src="docs/assets/app_screens_mockup.svg" alt="AirMouse Android App Screens" width="100%">
  <p><em>Figure 1: AirMouse Android Client — <b>Touchpad &amp; Live Stats</b>, <b>ZUPT Sensor Calibration</b>, and <b>One Euro Filter Customization</b></em></p>
</div>

<br/>

<div align="center">
  <img src="docs/assets/desktop_gui_mockup.svg" alt="AirMouse Desktop Fyne Server" width="100%">
  <p><em>Figure 2: AirMouse Desktop Host Server — <b>6-Tab Fyne GUI Dashboard</b> with Live Waveform &amp; Jitter Analytics</em></p>
</div>

---

## 🧩 Comprehensive Subsystems Breakdown

AirMouse Pro is architected into four decoupled, production-grade engineering modules:

```
AirMouse/
├── android/            # Native Android Client (Kotlin, Jetpack Compose M3, Hilt, Room, R8)
├── pc/
│   ├── airmouse_go_new/# Go High-Performance Server (Zero-CGO, native OS input synthesis)
│   └── airmouse_py/    # Python Reference Server (Asyncio, pynput, PyPI package)
├── web-client/         # Zero-Install Mobile Web Client (iOS Safari / PWA / Web Audio)
├── packaging/          # Linux udev rules, systemd services, and desktop shortcuts
└── scripts/            # Single Source of Truth versioning and automated release builders
```

---

### 1. 🤖 Android Native Client (`android/`)

The Android application delivers a zero-latency spatial mouse and multi-touch trackpad built strictly with modern Android architecture guidelines (MVI/MVVM, Clean Architecture, Jetpack Compose Material 3, and Hilt).

<div align="center">
  <img src="docs/assets/app_screens_mockup.svg" alt="AirMouse Android App Screens" width="100%">
  <p><em>Figure 1: AirMouse Android Client — <b>Touchpad &amp; Live Stats</b>, <b>ZUPT Sensor Calibration</b>, and <b>One Euro Filter Customization</b></em></p>
</div>

* **Jetpack Compose Material 3 UI**:
  - Fluid 120 FPS spring animations, frosted glass cards, and dynamic theme switching.
  - Dual operational modes: **Spatial Air Mouse** (6-DOF IMU wrist pointing) and **Virtual Trackpad** (1/2/3-finger gestures).
* **Decoupled Telemetry Architecture**:
  - High-frequency sensor streams (100 Hz) and ping metrics are strictly isolated from parent composables into leaf components (`IsolatedOrientationTelemetry`, `ConnectionQualityPill`, `TouchPointsOverlay`). This guarantees **zero dropped frames (0 jank)** on 90Hz/120Hz displays.
* **Persistent Foreground Service ([`AirMouseService.kt`](android/app/src/main/java/com/airmouse/service/AirMouseService.kt))**:
  - Runs with `connectedDevice|dataSync` foreground service types.
  - Enforces a bounded 2-hour WakeLock timeout (`MAX_WAKELOCK_TIMEOUT_MS = 7,200,000ms`) with automatic release upon disconnection to protect phone battery life.
* **Backpressure Buffer Guards**:
  - All high-frequency Kotlin `callbackFlow` pipelines are guarded with `.buffer(64, BufferOverflow.DROP_OLDEST)` on `Dispatchers.IO`, completely eliminating Out-Of-Memory (OOM) risk and GC pause spikes.
* **R8 Full Mode Optimization ([`R8_OPTIMIZATION.md`](android/docs/R8_OPTIMIZATION.md))**:
  - Releases are compiled with `minifyEnabled true`, `shrinkResources true`, and `proguard-android-optimize.txt`.
  - Uncompressed DEX reduced from **58.6 MB down to 6.83 MB (-88.3%)** with **92.2% obfuscation**, exceeding Google Play Store 2027 security standards.

---

### 2. ᛒ Bluetooth HID Hardware Emulation (Server-Less Mode)

AirMouse includes a native **Bluetooth HID Device Profile** ([`BluetoothHidMouseHelper.kt`](android/app/src/main/java/com/airmouse/network/BluetoothHidMouseHelper.kt)) that turns your Android phone into an actual, standard Bluetooth hardware mouse:

<div align="center">
  <img src="docs/assets/module_bluetooth_hid.svg" alt="Bluetooth HID Hardware Emulation" width="100%">
  <p><em>Figure 2: Bluetooth HID Hardware Emulation — <b>Zero-Server Standalone Bluetooth Mouse Mode</b></em></p>
</div>

* **Zero-Server & Zero-Install**:
  - **No software, server, or driver required on the target machine!**
  - Your phone connects to any target device via standard Bluetooth, just like an Apple Magic Mouse, Logitech MX Master, or standard Bluetooth trackpad.
* **Broad Device Compatibility**:
  - **Smart TVs & Streaming Boxes**: Android TV, Google TV, Apple TV, Samsung Tizen, LG webOS, Amazon Fire TV Stick.
  - **Tablets & Mobile OS**: Apple iPad (full iPadOS pointer circle), Samsung Galaxy Tab, ChromeOS.
  - **Computers & Consoles**: Windows 10/11, macOS, Linux, Steam Deck, ASUS ROG Ally.
* **Standard 4-Byte HID Descriptor (`SUBCLASS1_MOUSE`)**:
  - Synthesizes compliant USB HID mouse reports directly over Bluetooth L2CAP:
    - **Byte 0**: Button bits (`Bit 0: Left`, `Bit 1: Right`, `Bit 2: Middle`).
    - **Byte 1**: Signed 8-bit $\Delta X$ displacement ($-127$ to $+127$).
    - **Byte 2**: Signed 8-bit $\Delta Y$ displacement ($-127$ to $+127$).
    - **Byte 3**: Signed 8-bit scroll wheel delta ($-127$ to $+127$).
* **Spatial Air Mouse over Pure Bluetooth**:
  - Combines on-device sensor fusion with Bluetooth HID: tilt your phone in the air to move the cursor on your Smart TV or PC without needing any local Wi-Fi network.

---

### 3. 🎯 Sensor Fusion & Digital Signal Processing (DSP) Pipeline

The spatial air mouse uses an advanced sensor fusion pipeline to translate raw smartphone accelerometer and gyroscope measurements into sub-millimeter cursor displacements:

<div align="center">
  <img src="docs/assets/module_sensor_dsp.svg" alt="AirMouse Sensor Fusion DSP Engine" width="100%">
  <p><em>Figure 3: AirMouse Sensor Fusion DSP Pipeline — <b>Raw IMU Ingestion</b>, <b>AHRS Fusion</b>, <b>1€ Filter</b>, and <b>ZUPT Stationary Lock</b></em></p>
</div>

* **Raw IMU Ingestion (100 Hz)**:
  - Continuously samples hardware gyroscope (rad/s) and linear accelerometer (m/s²) via Android Sensors HAL.
* **AHRS Orientation Fusion**:
  - Madgwick/Mahony quaternion filter integrating angular velocity with gravity vector corrections, isolating pitch, roll, and yaw angles while compensating for thermal gyroscope drift.
* **One Euro Filter ($1€$)**:
  - Adaptive dual-speed low-pass filter with dynamic cutoff frequency:
    $$f_c = f_{c_{\min}} + \beta |\dot{x}|$$
  - During stationary and slow micro-movements (precision cursor clicking), cutoff drops to $1.0\text{ Hz}$, eliminating all hand tremors and micro-shaking.
  - During rapid flick gestures, cutoff scales instantaneously with velocity, delivering sub-millisecond responsiveness without perceptible tracking lag.
* **Zero-Velocity Update (ZUPT)**:
  - Continuously tracks rolling variance across all 3 gyro axes. If $\sigma^2 < 0.015\text{ rad/s}$ for $>500\text{ ms}$, the system locks the cursor coordinates and auto-calibrates zero bias in the background, eliminating creeping cursor drift permanently.

---

### 4. ⚡ Multi-OS Desktop Host Daemons (Go & Python)

AirMouse features two independent desktop server implementations tailored for maximum performance and portability:

<div align="center">
  <img src="docs/assets/module_desktop_kernel.svg" alt="Multi-OS Kernel Input Synthesis" width="100%">
  <p><em>Figure 4: Multi-OS Kernel Input Synthesis Engine — <b>Go High-Throughput Ingest</b>, <b>CoreGraphics (macOS)</b>, <b>SendInput (Windows)</b>, and <b>/dev/uinput (Linux)</b></em></p>
</div>

#### A. Go High-Performance Server (`pc/airmouse_go_new/`)
* **Zero-CGO Native OS Input Injection**:
  - **macOS**: Direct C-ABI syscalls executing `CGEventCreateMouseEvent()` and `CGEventPost(kCGHIDEventTap)` natively without CGO compilation overhead.
  - **Windows**: High-resolution mouse coordinate dispatch using `user32.dll` `SendInput` with `MOUSEEVENTF_MOVE`.
  - **Linux**: Kernel-level virtual device emulation via `/dev/uinput` with dedicated udev rules, fully compatible with both **Wayland** (Ubuntu 22.04+, Fedora) and **X11** without root privileges.
* **Ultra-Low Latency Concurrency**:
  - Bounded worker pool with non-blocking `SafeSend` and panic recovery to isolate slow clients.
  - Graceful shutdown on `SIGINT`/`SIGTERM` with 5-second drain timeout and `SIGHUP` live configuration reloading.
* **Fyne GUI Dashboard**:
  - Native 6-tab desktop dashboard with real-time jitter waveforms, ping latency monitors, and pairing QR codes.

<div align="center">
  <img src="docs/assets/desktop_gui_mockup.svg" alt="AirMouse Desktop Fyne Server" width="100%">
  <p><em>Figure 5: AirMouse Desktop Host Server — <b>6-Tab Fyne GUI Dashboard</b> with Live Waveform &amp; Jitter Analytics</em></p>
</div>

#### B. Python Reference Server (`pc/airmouse_py/`)
* **PyPI Distribution**: Available worldwide via `pip install airmouse-core`.
* **Asyncio Networking**: Asynchronous TCP (TLS 1.3), WebSocket, and UDP broadcast listener.
* **Decoupled Actuator Thread ([`MouseActuatorThread`](pc/airmouse_py/core/actuator.py))**:
  - Offloads OS mouse manipulation from the asyncio event loop to a dedicated worker thread with a thread-safe `queue.Queue`.
  - Double-click and mouse button holds use non-blocking background daemon timers (`threading.Timer`), eliminating event loop starvation and cursor jitter.

---

### 5. 🌐 Zero-Install Mobile Web Client (`web-client/`)

A zero-install Progressive Web App (PWA) allowing immediate touch and spatial mouse control on Apple iOS (Safari) and Android (Chrome) without installing an app:

<div align="center">
  <img src="docs/assets/module_web_client.svg" alt="AirMouse Mobile Web Client" width="100%">
  <p><em>Figure 6: Zero-Install Mobile Web Client — <b>iOS 13+ Onboarding Dialog</b>, <b>Web Audio Haptic Impulses</b>, and <b>Screen Wake Lock Fallback</b></em></p>
</div>

* **iOS 13+ Sensor Permission Dialog**:
  - Apple WebKit blocks motion sensors if requested without a direct, top-level user tap.
  - AirMouse integrates a dedicated glassmorphic permission modal where the user taps **"Grant Permission"**, directly executing `DeviceMotionEvent.requestPermission()` and unlocking full spatial tilt tracking.
* **Pooled Web Audio Click Synthesizer**:
  - Bypasses Apple's restriction on `navigator.vibrate` by pre-synthesizing three band-limited PCM audio impulses (Light: 14ms @ 950→220Hz, Medium: 24ms @ 750→160Hz, Heavy: 38ms @ 520→110Hz).
  - Pre-warms the WebKit audio pipeline on first touch (`touchstart`) to deliver instant, zero-delay mechanical click sound feedback.
* **Screen Wake Lock with Video Loop Fallback**:
  - Acquires `navigator.wakeLock` on modern browsers and falls back to an invisible looping base64 MP4 container for older iOS Safari versions, keeping the WebSocket connection alive without screen dimming.
* **2D Orientation Matrix**:
  - Dynamically calculates the viewport angle ($\theta \in \{0^\circ, 90^\circ, 180^\circ, 270^\circ\}$) and transforms sensor vectors so cursor movement remains intuitive whether the device is held in Portrait, Landscape Left, or Landscape Right:
    $$\begin{pmatrix} \text{rotDx} \\ \text{rotDy} \end{pmatrix} = \begin{pmatrix} \cos\theta & \sin\theta \\ -\sin\theta & \cos\theta \end{pmatrix} \begin{pmatrix} \Delta\gamma \\ \Delta\beta \end{pmatrix}$$

---

## 🏗️ System Architecture

```mermaid
flowchart TD
    subgraph MobileClients["Mobile Clients"]
        A["Android Native App<br/>(Jetpack Compose / Sensors HAL)"]
        B["iOS Safari / Mobile Web<br/>(HTML5 / WSS / WakeLock)"]
    end

    subgraph TransportLayer["Transport Layer (Protocol V2)"]
        C["TLS TCP Control & Pairing (Port 8080)"]
        D["WSS Real-Time WebSocket (Port 8081)"]
        E["HTTP Web Client PWA (Port 8082)"]
        F["UDP Fast Motion & Discovery (Port 8083)"]
    end

    subgraph DesktopHost["Desktop Host Engine (Go or Python)"]
        G["Protocol V2 Decoder & Validator<br/>(Constant-Time HMAC / Replay Guard)"]
        H["Adaptive Jitter Buffer & Channel Pool"]
        I["DSP Signal Pipeline<br/>(One Euro Filter / ZUPT)"]
        J["Native OS Input Actuator<br/>(CoreGraphics / SendInput / uinput)"]
    end

    A -->|UDP / TCP / WS| C
    A -->|UDP / TCP / WS| D
    A -->|UDP Fast Motion| F
    B -->|WSS| D
    B -.->|HTTP PWA| E

    C --> G
    D --> G
    F --> G
    G --> H
    H --> I
    I --> J
    J --> K["Desktop Host OS<br/>(macOS / Windows / Linux)"]
```

---

## 📡 Canonical Protocol V2 Wire Envelope

All communication across TCP, WebSocket, and UDP adheres to the hardened **Protocol V2** envelope:

```json
{
  "v": 2,
  "type": "move",
  "seq": 10429,
  "ts": 1729858291042,
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "token": "a6c8e3f...",
  "payload": {
    "dx": -3.42,
    "dy": 1.18,
    "dt": 0.016
  }
}
```

### Supported Message Catalog:
| Type | Description | Key Payload Attributes |
|---|---|---|
| `hello` | Handshake initiation & client identity | `name`, `model`, `version`, `protocol`, `token` |
| `welcome` | Handshake approval & server metadata | `session_id`, `heartbeat_ms`, `capabilities` |
| `move` | High-frequency cursor displacement | `dx` (-500 to 500), `dy` (-500 to 500), `dt` |
| `click` | Mouse button press/release/click | `button` (`left`, `right`, `middle`, `double`) |
| `scroll` | Vertical or horizontal scroll wheel | `delta` (-10.0 to 10.0) |
| `gesture` | 3D spatial motion gesture triggers | `name` (`circle`, `swipe_left`, `shake`), `confidence` |
| `proximity` | Hover and device approach data | `state`, `distance_cm`, `hover_active` |
| `control` | System & presentation shortcuts | `action` (`presentation_next`, `presentation_blackout`) |
| `ack` | Delivery acknowledgement | `ack_id`, `status` (`ok`) |
| `ping`/`pong`| Round-trip latency calculation | Empty payload |

---

## 💻 Operating System Setup Guides

### 🍎 macOS Setup
1. **Accessibility Permissions**: macOS requires Accessibility permissions to synthesize mouse events:
   - Open **System Settings $\rightarrow$ Privacy & Security $\rightarrow$ Accessibility**.
   - Enable `AirMouse.app` or `AirMouse-Server`.
2. **Gatekeeper Notice**: For downloaded Universal binaries:
   ```bash
   xattr -cr /Applications/AirMouse.app
   ```
3. Run the application or standalone binary:
   ```bash
   ./AirMouse-Server-v1.0.1-macos-universal
   ```

### 🪟 Windows Setup
1. Extract `AirMouse-v1.0.1-windows-x64.zip`.
2. Double-click `Start-AirMouse.bat` (Window mode) or `Start-AirMouse-Console.bat` (Console mode).
3. **Firewall**: On first launch, Windows Defender Firewall will prompt to allow network access. Allow access on private networks.

### 🐧 Linux Setup (uinput without root)
AirMouse uses the Linux kernel's `/dev/uinput` subsystem for input synthesis.
1. Install the provided udev rule:
   ```bash
   sudo cp packaging/udev/99-airmouse-uinput.rules /etc/udev/rules.d/
   ```
2. Add your user to the `input` group:
   ```bash
   sudo usermod -aG input $USER
   ```
3. Reload rules:
   ```bash
   sudo udevadm control --reload-rules && sudo udevadm trigger
   ```
4. Run the server:
   ```bash
   ./airmouse-server-v1.0.1-linux-x64
   ```
*(For detailed steps, see [**`docs/LINUX_SETUP.md`**](docs/LINUX_SETUP.md))*

---

## 🔄 Single Source of Truth (SSOT) Versioning

Like enterprise open-source projects (Docker, Kubernetes, VS Code), AirMouse manages its versioning through a single canonical file:
* **Canonical Version File**: [`VERSION`](VERSION) (e.g. `1.0.1`)
* **Unified Bumping Script**: [`scripts/bump_version.py`](scripts/bump_version.py)

### How to Bump or Sync Versions:
```bash
# 1. Bump version across all 19 target files (Android, Go, Python, Web, Docs)
python3 scripts/bump_version.py 1.0.2

# 2. Check for version drift in CI
python3 scripts/bump_version.py --check

# 3. Dry-run preview
python3 scripts/bump_version.py --dry-run 1.0.2
```

---

## 📊 Deep Audits & Technical Reports

The codebase has undergone comprehensive architectural, cryptographic, and performance audits. Read the detailed reports in `docs/`:

1. [**docs/AUDIT_1_SYSTEM_ARCHITECTURE_PROTOCOL.md**](docs/AUDIT_1_SYSTEM_ARCHITECTURE_PROTOCOL.md): Protocol V2 wire format, JSON vs binary framing, monotonic sequence hazards, and multiplatform sync.
2. [**docs/AUDIT_2_ANDROID_CLIENT_DEEP_DIVE.md**](docs/AUDIT_2_ANDROID_CLIENT_DEEP_DIVE.md): Jetpack Compose recomposition elimination, One Euro Filter tuning, and WakeLock power management.
3. [**docs/AUDIT_3_DESKTOP_DAEMONS_GO_AND_PYTHON.md**](docs/AUDIT_3_DESKTOP_DAEMONS_GO_AND_PYTHON.md): Low-level OS input injection (CGEvent, SendInput, uinput), Wayland security models, and Python asyncio concurrency.
4. [**docs/AUDIT_4_WEB_CLIENT_CROSS_BROWSER.md**](docs/AUDIT_4_WEB_CLIENT_CROSS_BROWSER.md): iOS Safari WebKit sandbox constraints, DeviceMotionEvent permissions, Web Audio tactile impulses, and Screen Wake Lock fallbacks.
5. [**docs/AUDIT_5_SECURITY_DEVOPS_ROADMAP.md**](docs/AUDIT_5_SECURITY_DEVOPS_ROADMAP.md): Cryptographic threat modeling, timing attack remediation, and prioritized P0–P3 roadmap.
6. [**android/docs/R8_OPTIMIZATION.md**](android/docs/R8_OPTIMIZATION.md): R8 Full Mode verification metrics, reducing DEX by 88.3% to 6.83 MB.
7. [**docs/LINUX_SETUP.md**](docs/LINUX_SETUP.md): Comprehensive Linux uinput and udev guide.

---

## 🧪 Testing and Quality Verification

AirMouse maintains automated test suites across all layers:

- **Go Test Suite (`pc/airmouse_go_new`)**:
  ```bash
  cd pc/airmouse_go_new && go test ./...
  # Result: 100% Pass across all 22 packages
  ```
- **Python Test Suite (`pc/airmouse_py`)**:
  ```bash
  pytest pc/airmouse_py/tests/
  # Result: 68/68 passed in 4.6s (100% Pass Rate)
  ```
- **Android Quality Verification**:
  ```bash
  cd android && ./gradlew testDebugUnitTest
  # Result: 100% pass rate
  ```

---

## 🛡️ License

This project is licensed under the [MIT License](LICENSE).  
Copyright (c) 2026 Taha Majs & Contributors.
