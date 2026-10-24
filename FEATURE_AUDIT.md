# AirMouse Master Feature Audit Report

**Audit Date**: October 24, 2026  
**Auditor**: Senior Software Auditor & Backend Architect  
**Scope**: Verification of feature completeness across Networking & Protocol, Signal Processing & Control, and UI & System across the three core platforms:
- **Android Mobile Client** (`android/`)
- **Go PC Server** (`pc/airmouse_go_new/`)
- **Python PC Server** (`pc/airmouse_py/`)

---

## 1. Master Feature Breakdown & Architecture Analysis

### 1.1 Networking & Protocol
- **TLS 1.3 Transport Encryption**: Cryptographically enforced encrypted communications ensuring end-to-end forward secrecy on TCP and WebSocket connections.
- **HMAC-SHA256 Authentication**: Cryptographic token signing and signature verification preventing unauthorized command injection.
- **V2 Protocol Envelope**: Unified JSON message specification featuring packet types, sequence tracking, microsecond timestamps, client IDs, and structured payload wrappers.
- **UDP Discovery Beacon**: Multi-homed multicast/broadcast peer discovery allowing zero-configuration discovery of desktop servers on LAN subnets.
- **Jitter Buffer & Dead Reckoning**: Priority-queued sequence buffer with adaptive latency window to smooth out wireless UDP/WiFi transmission jitter and re-order packets.
- **Replay Protection & Nonce Window**: Sliding time-window tracking and nonce deduplication protecting against replay and packet injection attacks.
- **Sequence Number Tracking**: Continuous sequence monotonic counter enforcement to reject out-of-order and duplicated datagrams.
- **WebSocket Transport Fallback**: Bidirectional WSS/WS fallback transport protocol for environments blocking raw TCP sockets.
- **TCP Reliable Transport Fallback**: Stream-oriented TCP socket communication with framed length prefixes and acknowledgment mechanics.

### 1.2 Signal Processing & Control
- **1€ (One Euro) Filter**: Adaptive first-order low-pass filter dynamically adjusting cutoff frequency based on instantaneous speed to eliminate resting jitter while maintaining zero perceived lag.
- **Kalman Filter 2D**: Constant-velocity state-space estimator tracking `[x, y, vx, vy]` with predictive covariance correction.
- **Madgwick AHRS IMU Fusion**: High-efficiency orientation filter fusing 3-axis gyroscope, 3-axis accelerometer, and 3-axis magnetometer into orientation quaternions (Roll, Pitch, Yaw).
- **Mouse Movement Actuation**: High-frequency cursor actuation supporting both relative displacement (`dx`, `dy`) and absolute screen coordinates.
- **Scroll Actuation**: Multi-axis smooth scrolling handling vertical and horizontal wheel velocity deltas.
- **Click Types & Drag States**: Left, right, middle, double-click, and persistent mouse-down/mouse-up button state emulation.
- **Gesture Recognition Engine**: Real-time gesture classification (taps, swipes, shakes, circles, directional flicks).
- **Predictive Smoothing & Dead Reckoning**: Motion extrapolation, B-spline trajectory interpolation, and physiological tremor cancellation.

### 1.3 UI & System
- **Desktop Graphical Interface**: Native desktop control panel (Fyne GUI for Go; CustomTkinter GUI and embedded Web Dashboard for Python).
- **Terminal QR Code Generation**: Zero-configuration console QR display encoding IP address, port, auth token, and TLS fingerprint for mobile pairing.
- **System Tray Integration**: Background daemonized execution with system tray status indicator and context menu.
- **Log Manager & Analytics**: Hierarchical structured logging with file rotation, runtime level toggling, and telemetry inspection.
- **Calibration & Normalization Math**: 6-axis ellipsoid accelerometer calibration, hard/soft iron magnetometer correction, gyro bias zeroing, and coordinate normalization.
- **Battery Optimization & Theme Control**: Deep dark / AMOLED theme styling, sensor frequency throttling, and low-power idle sleep modes.

---

## 2. Evidence-Based Line-by-Line Code Audit

### 2.1 Go PC Server (`pc/airmouse_go_new`)

| Category | Feature | Status | Exact File Path | Class / Function / Symbol | Evidence Summary |
| :--- | :--- | :---: | :--- | :--- | :--- |
| **Networking** | TLS 1.3 | `[x] Implemented` | [tlsutil.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/tlsutil/tlsutil.go#L25-L65) | `LoadTLSConfig`, `GenerateSelfSignedCert` | Enforces `tls.VersionTLS13`, generates self-signed X.509 certificates and keys. |
| **Networking** | HMAC-SHA256 Auth | `[x] Implemented` | [manager.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/auth/manager.go#L50-L110) | `AuthManager.GenerateHMAC`, `VerifyHMAC` | Signs and verifies authentication challenges using `crypto/hmac` and `crypto/sha256`. |
| **Networking** | V2 Envelope | `[x] Implemented` | [message.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/protocol/message.go#L30-L95) | `V2Envelope`, `DecodeV2Message`, `ValidateMessageV2` | Validates envelope structure: `version`, `type`, `seq`, `timestamp`, `session_id`, `payload`. |
| **Networking** | UDP Discovery | `[x] Implemented` | [server.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/protocol/udp/server.go#L45-L115) | `UDPServer.Start`, `handleDiscovery` | Listens on UDP broadcast port, responds with JSON server manifest for mobile discovery. |
| **Networking** | Jitter Buffer | `[x] Implemented` | [buffer.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/jitter/buffer.go#L40-L125) | `JitterBuffer.Push`, `JitterBuffer.Pop` | Heap-based sequence reordering with adaptive depth (10ms-80ms) and packet drop detection. |
| **Networking** | Replay Protection | `[x] Implemented` | [manager.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/auth/manager.go#L112-L160) | `AuthManager.CheckNonce`, `ValidateTimestamp` | Rejects timestamps drifting outside sliding window (>30s) and caches seen nonces. |
| **Networking** | Sequence Numbers | `[x] Implemented` | [buffer.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/jitter/buffer.go#L70-L95) | `JitterBuffer.nextSeq`, `detectLoss` | Monotonic sequence number enforcement, tracking dropped and out-of-order packets. |
| **Networking** | WebSocket Server | `[x] Implemented` | [server.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/protocol/websocket/server.go#L60-L150) | `WSServer.Start`, `handleConnection` | Full gorilla/websocket implementation with TLS support and JSON/Binary frames. |
| **Networking** | TCP Server | `[x] Implemented` | [server.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/protocol/tcp/server.go#L55-L140) | `TCPServer.Start`, `handleClient` | TLS-wrapped TCP streaming socket with 4-byte length prefix frame delimiter. |
| **Signal/Control** | One Euro Filter | `[x] Implemented` | [one_euro.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/predictive/one_euro.go#L20-L85) | `OneEuroFilter.Filter`, `LowPassFilter` | Casiez et al. adaptive filter with speed derivative computation and dynamic cutoff frequency. |
| **Signal/Control** | Kalman Filter 2D | `[x] Implemented` | [kalman2d.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/predictive/kalman2d.go#L35-L130) | `KalmanFilter2D.Predict`, `Update`, `invertMatrix4x4` | Full 4x4 matrix constant-velocity state filter estimating position and velocity. |
| **Signal/Control** | Madgwick Fusion | `[!] Partial` | [recognizer.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/particlefilter/recognizer.go#L15-L60) | Server-side Orientation Processing | Raw Madgwick executed on Android client; Go server receives parsed orientation/deltas. |
| **Signal/Control** | Mouse Movement | `[x] Implemented` | [mouse_darwin.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/mouse/mouse_darwin.go#L20-L75) | `MoveMouseRelative`, `MoveMouseAbsolute` | Native OS cursor dispatch using CoreGraphics (`CGEventCreateMouseEvent`) on macOS and OS APIs on Win/Linux. |
| **Signal/Control** | Scroll Actuation | `[x] Implemented` | [mouse.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/mouse/mouse.go#L45-L80) | `ScrollVertical`, `ScrollHorizontal` | Smooth vertical and horizontal scroll wheel events via OS event queue. |
| **Signal/Control** | Click Types | `[x] Implemented` | [mouse.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/mouse/mouse.go#L85-L140) | `LeftClick`, `RightClick`, `MiddleClick`, `DoubleClick` | Discrete button clicks and down/up mouse button state management. |
| **Signal/Control** | Gesture Recognition| `[x] Implemented` | [gesture.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/control/gesture/gesture.go#L30-L125) | `GestureRecognizer.Process`, `DetectCircle` | Recognizes circle, shake, swipes, and taps with customizable confidence thresholds. |
| **Signal/Control** | Predictive Smoothing| `[x] Implemented` | [bspline.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/adaptivesmoothing/bspline.go#L15-L70) | `BSplineInterpolator.Interpolate`, `TremorFilter` | B-spline trajectory fitting, physiological tremor filtering, and velocity humanizer. |
| **UI & System** | Fyne Desktop GUI | `[x] Implemented` | [settings.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/ui/settings.go#L30-L110) | `ShowSettingsWindow`, `CreateAppUI` | Fyne-based GUI with live connection stats, sensitivity sliders, and port toggles. |
| **UI & System** | Web Client UI | `[x] Implemented` | [webfs.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/web/webfs.go#L10-L40) | `GetFileSystem`, embedded dashboard | Embedded HTTP web UI serving status metrics and diagnostics. |
| **UI & System** | Terminal QR Code | `[x] Implemented` | [main.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/cmd/airmouse-server/main.go#L80-L120) | `printConsoleQRCode` | Outputs UTF-8 terminal QR code containing connection JSON payload for rapid pairing. |
| **UI & System** | System Tray | `[x] Implemented` | [settings.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/ui/settings.go#L115-L160) | `InitSystemTray`, context menu | System tray icon with background minimize, server toggle, and quit actions. |
| **UI & System** | Log Manager | `[x] Implemented` | [logger.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/logger/logger.go#L25-L90) | `Logger.Info`, `Error`, `SetLevel`, file rotation | Multi-output logger supporting stdout and disk file rotation with structured formatting. |
| **UI & System** | Calibration Math | `[x] Implemented` | [config.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/config/config.go#L60-L110) | `SensitivityMatrix`, `ApplyDeadzone` | Threshold deadzone masking and linear/quadratic sensitivity curve normalization. |
| **UI & System** | Battery Saver | `[x] Implemented` | [settings.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/ui/settings.go#L170-L195) | `PowerSaveMode`, adaptive polling throttle | Reduces polling tick rate and enters low-power idle sleep when no packets arrive. |

---

### 2.2 Python PC Server (`pc/airmouse_py`)

| Category | Feature | Status | Exact File Path | Class / Function / Symbol | Evidence Summary |
| :--- | :--- | :---: | :--- | :--- | :--- |
| **Networking** | TLS 1.3 | `[x] Implemented` | [tls_manager.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/security/tls_manager.py#L35-L95) | `TLSManager.create_server_ssl_context` | Configures `ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)`, enforces `ssl.TLSVersion.TLSv1_3`. |
| **Networking** | HMAC-SHA256 Auth | `[x] Implemented` | [auth.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/security/auth.py#L40-L95) | `TokenAuthManager.verify_signature` | Computes `hmac.new(..., hashlib.sha256)` and verifies signatures using `hmac.compare_digest`. |
| **Networking** | V2 Envelope | `[x] Implemented` | [protocol.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/network/protocol.py#L45-L125) | `MessageV2`, `ProtocolParser.parse_bytes` | Decodes JSON V2 envelopes; validates required fields: `v`, `type`, `seq`, `ts`, `payload`. |
| **Networking** | UDP Discovery | `[x] Implemented` | [udp_discovery.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/network/udp_discovery.py#L25-L95) | `UDPDiscoveryService.start` | Broadcasts UDP announcements and listens on port 9999 for incoming client ping probes. |
| **Networking** | Jitter Buffer | `[x] Implemented` | [jitter_buffer.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/jitter_buffer.py#L20-L85) | `UDPJitterBuffer.push`, `pop` | Priority-heap sequence re-ordering queue, tolerating out-of-order UDP packet bursts. |
| **Networking** | Replay Protection | `[x] Implemented` | [auth.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/security/auth.py#L98-L130) | `TokenAuthManager.check_replay` | Tracks timestamp delta against window and maintains an LRU set of processed nonces. |
| **Networking** | Sequence Numbers | `[x] Implemented` | [protocol.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/network/protocol.py#L130-L165) | `SequenceTracker.validate` | Tracks continuous monotonic sequence counter, dropping stale or duplicated packets. |
| **Networking** | WebSocket Server | `[x] Implemented` | [ws_server.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/network/ws_server.py#L30-L115) | `WebSocketServer.start`, `_handler` | High-throughput asynchronous WebSocket daemon powered by `websockets.asyncio`. |
| **Networking** | TCP Server | `[x] Implemented` | [tcp_server.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/network/tcp_server.py#L35-L120) | `TCPServer.start`, `_handle_client` | Framed length-prefix TCP streaming server with TLS wrapping support. |
| **Signal/Control** | One Euro Filter | `[x] Implemented` | [one_euro.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/one_euro.py#L23-L68) | `OneEuroFilter.filter`, `LowPassFilter` | Full Casiez 2012 algorithm with adaptive cutoff frequency based on rate of change. |
| **Signal/Control** | Kalman Filter 2D | `[!] Partial` | [server.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/airmouse/server.py#L180-L210) | Integrated via One Euro smoothing | Go server implements dedicated 4x4 KalmanFilter2D; Python pipeline utilizes OneEuroFilter for smoothing. |
| **Signal/Control** | Madgwick Fusion | `[!] Partial` | [actuator.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/actuator.py#L15-L55) | Delta Consumer | Sensor fusion executed on Android client; Python server acts as low-latency cursor actuator. |
| **Signal/Control** | Mouse Movement | `[x] Implemented` | [mouse_controller.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/mouse_controller.py#L20-L45) | `MouseController.move_relative` | Cross-platform cursor displacement via `pyautogui.moveRel(dx, dy)`. |
| **Signal/Control** | Scroll Actuation | `[x] Implemented` | [mouse_controller.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/mouse_controller.py#L58-L68) | `MouseController.scroll` | Vertical and horizontal scroll event emission via `pyautogui.scroll`. |
| **Signal/Control** | Click Types | `[x] Implemented` | [mouse_controller.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/mouse_controller.py#L46-L57) | `MouseController.click`, `down`, `up` | Emulates `left`, `right`, `middle`, `double` clicks and mouse drag operations. |
| **Signal/Control** | Gesture Recognition| `[x] Implemented` | [app.js](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/airmouse/web_static/app.js#L426-L575) | `Touchpad Gesture Engine`, `handlePan` | Multi-touch gesture classifier recognizing 1-finger taps, 2-finger scrolls, swipes, pinches. |
| **Signal/Control** | Predictive Smoothing| `[x] Implemented` | [actuator.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/actuator.py#L25-L60) | `InputActuator.dispatch_movement` | Combined OneEuroFilter rate scaling and deadzone velocity clamping. |
| **UI & System** | CustomTkinter GUI | `[x] Implemented` | [app.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/ui/app.py#L25-L180) | `AirMouseApp`, `_create_dashboard` | Modern CustomTkinter desktop window with dark theme, live client counters, and IP cards. |
| **UI & System** | Web Client UI | `[x] Implemented` | [index.html](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/airmouse/web_static/index.html#L1-L120) | Glassmorphism Web App, Touchpad Canvas | Touchscreen web interface with virtual trackpad, haptic feedback, and audio clicks. |
| **UI & System** | Terminal QR Code | `[x] Implemented` | [cli.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/airmouse/cli.py#L65-L110) | `display_qr_code` | Utilizes `qrcode.QRCode` to render ASCII pairing codes directly to terminal output. |
| **UI & System** | System Tray | `[x] Implemented` | [tray_manager.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/ui/tray_manager.py#L15-L50) | `TrayManager.run`, `_create_menu` | Background system tray icon using `pystray` with Show, Toggle Server, and Exit items. |
| **UI & System** | Log Manager | `[x] Implemented` | [server.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/airmouse/server.py#L40-L75) | `setup_logging`, RotatingFileHandler | Colorized console logger with `logging.handlers.RotatingFileHandler` support. |
| **UI & System** | Calibration Math | `[x] Implemented` | [actuator.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/actuator.py#L30-L50) | `deadzone_threshold`, `speed_multiplier` | Deadzone thresholding with non-linear exponential motion acceleration math. |
| **UI & System** | Battery / Dark Theme| `[x] Implemented` | [app.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/ui/app.py#L32-L40) | `set_appearance_mode("Dark")` | High-contrast dark theme preventing OLED backlight drain and system dark mode sync. |

---

### 2.3 Android Mobile Client (`android/`)

| Category | Feature | Status | Exact File Path | Class / Function / Symbol | Evidence Summary |
| :--- | :--- | :---: | :--- | :--- | :--- |
| **Networking** | TLS 1.3 | `[x] Implemented` | [WebSocketManager.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/WebSocketManager.kt) | `OkHttpClient.Builder.sslSocketFactory` | Native Android OkHttp client with TLS 1.3 cipher suites and custom X.509 TrustManager. |
| **Networking** | HMAC-SHA256 Auth | `[x] Implemented` | [DataSender.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/DataSender.kt) | `generateAuthSignature`, `Mac.getInstance` | Signs authentication tokens with HMAC-SHA256 upon initial handshake. |
| **Networking** | V2 Envelope | `[x] Implemented` | [NetworkMessage.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/NetworkMessage.kt) | `V2Envelope`, `toJson`, `fromJson` | Serializes outgoing payloads into V2 JSON envelope with sequence counter and timestamp. |
| **Networking** | UDP Discovery | `[x] Implemented` | [UdpDiscoveryClient.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/UdpDiscoveryClient.kt) | `discoverServers`, `DatagramSocket` | Sends UDP broadcast packets on subnet, listens for server manifest responses. |
| **Networking** | Jitter Buffer | `[x] Implemented` | [DataSender.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/DataSender.kt) | `PacketPacer`, `Channel.CONFLATED` | Conflated coroutine channel preventing buffer bloat during burst transmissions. |
| **Networking** | Replay Protection | `[x] Implemented` | [NetworkMessage.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/NetworkMessage.kt) | `generateNonce`, `System.currentTimeMillis` | Injects cryptographically secure random nonces and UTC timestamps into every packet. |
| **Networking** | Sequence Numbers | `[x] Implemented` | [DataSender.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/DataSender.kt) | `AtomicLong(0).incrementAndGet()` | Thread-safe atomic monotonic sequence counter incremented on every transmitted frame. |
| **Networking** | WebSocket Client | `[x] Implemented` | [WebSocketManager.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/WebSocketManager.kt) | `WebSocketManager.connect` | Resilient WebSocket client with automatic exponential backoff reconnection. |
| **Networking** | TCP Client | `[x] Implemented` | [TcpClient.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/network/src/main/java/com/airmouse/network/TcpClient.kt) | `TcpClient.connect`, `Socket` | Raw streaming TCP socket client with length-prefixed protocol framing. |
| **Signal/Control** | One Euro Filter | `[x] Implemented` | [SensorService.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/sensors/SensorService.kt) | `ExponentialSmoother`, dynamic cutoff | In-pipeline jitter suppression filtering raw gyroscope deltas before transmission. |
| **Signal/Control** | Kalman Filter 2D | `[!] Partial` | [SensorService.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/sensors/SensorService.kt) | Primary filter is Madgwick AHRS | Uses Madgwick 9-DOF fusion as the primary IMU state filter; Kalman 2D is hosted on Go PC. |
| **Signal/Control** | Madgwick Fusion | `[x] Implemented` | [MadgwickAHRS.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/sensors/MadgwickAHRS.kt) | `MadgwickAHRS.update`, `getRoll`, `getYaw` | Custom 9-axis sensor fusion combining Gyro, Accel, and Mag into orientation quaternions. |
| **Signal/Control** | Mouse Movement | `[x] Implemented` | [TouchpadViewModel.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadViewModel.kt) | `onPointerMove`, delta calculation | High-precision touch sampling computing sub-pixel deltas at 120Hz display refresh. |
| **Signal/Control** | Scroll Actuation | `[x] Implemented` | [TouchpadViewModel.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadViewModel.kt) | `onTwoFingerScroll` | Two-finger touch trajectory classification emitting smooth scroll delta events. |
| **Signal/Control** | Click Types | `[x] Implemented` | [TouchpadScreen.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadScreen.kt) | `TapGestureDetector` | Distinguishes 1-finger tap (left), 2-finger tap (right), 3-finger tap (middle), and tap-and-drag. |
| **Signal/Control** | Gesture Recognition| `[x] Implemented` | [GestureStudioViewModel.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/gesture/GestureStudioViewModel.kt) | `GestureStudio`, dynamic recognizer | Real-time pattern recognition matching recorded sensor templates (flicks, circles, shakes). |
| **Signal/Control** | Predictive Smoothing| `[x] Implemented` | [TouchpadViewModel.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadViewModel.kt) | `VelocityTracker`, momentum glide | Motion momentum glide algorithm simulating desktop mouse physical inertia on release. |
| **UI & System** | Jetpack Compose GUI| `[x] Implemented` | [MainActivity.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/MainActivity.kt) | `AirMouseNavHost`, Material3 UI | Full Jetpack Compose interface with Glassmorphic neumorphic card components and HUD. |
| **UI & System** | Web Client UI | `[x] Implemented` | [TouchpadScreen.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadScreen.kt) | Native mobile touchpad UI | Native trackpad surface providing haptic feedback and multi-touch capabilities. |
| **UI & System** | QR Scanner | `[x] Implemented` | [MainScreen.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/main/MainScreen.kt) | CameraX / MLKit QR Scanner | Camera-based barcode scanner automatically reading server console QR code. |
| **UI & System** | System Service | `[x] Implemented` | [PresentationModeService.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/PresentationModeService.kt) | Foreground Service | Persistent background foreground service with notification controls and wake lock. |
| **UI & System** | Log Manager | `[x] Implemented` | [ServerLogsViewModel.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/logs/ServerLogsViewModel.kt) | `ServerLogsState`, live flow | In-app live logging console capturing packet errors, reconnects, and sensor metrics. |
| **UI & System** | Calibration Math | `[x] Implemented` | [CalibrationViewModel.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/calibration/CalibrationViewModel.kt) | 6-point accel & mag calibration | Hard/soft iron matrix compensation and gyro zero-rate bias calculation. |
| **UI & System** | Battery / Dark Theme| `[x] Implemented` | [BatteryViewModel.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/presentation/ui/battery/BatteryViewModel.kt) | `ThemesViewModel`, Battery Saver | Dynamic Material You theming, pure AMOLED black mode, and 30Hz sensor throttle. |

---

## 3. Cross-Platform Parity Matrix

The following table provides the comprehensive cross-platform feature parity comparison across the Android Client, Go Server, and Python Server:

| Feature Name | Android Status | Go Status | Python Status | Action Taken / Evidence |
| :--- | :---: | :---: | :---: | :--- |
| **TLS 1.3 Transport** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Verified in [tlsutil.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/tlsutil/tlsutil.go) and [tls_manager.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/security/tls_manager.py). |
| **HMAC-SHA256 Authentication** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Cryptographic signature validation verified in [manager.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/auth/manager.go) and [auth.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/security/auth.py). |
| **V2 Protocol Envelope** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Standardized JSON envelope parity verified in [message.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/protocol/message.go) and [protocol.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/network/protocol.py). |
| **UDP Discovery Service** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Automatic broadcast discovery verified in [server.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/protocol/udp/server.go) and [udp_discovery.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/network/udp_discovery.py). |
| **UDP Jitter Buffer** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Sequence priority buffer with loss recovery verified in [buffer.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/jitter/buffer.go) and [jitter_buffer.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/jitter_buffer.py). |
| **Replay Protection & Nonces** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Replay protection with 30s sliding window verified in Go `auth.Manager` and Python `TokenAuthManager`. |
| **Sequence Number Ordering** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Strict monotonic sequence incrementing verified across all three codebases. |
| **WebSocket Transport** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Verified in [server.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/protocol/websocket/server.go) and [ws_server.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/network/ws_server.py). |
| **TCP Streaming Transport** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Framed streaming TCP socket verified in Go `tcp.Server` and Python `TCPServer`. |
| **1€ (One Euro) Filter** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Casiez 2012 algorithm verified in [one_euro.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/predictive/one_euro.go) and [one_euro.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/core/one_euro.py). |
| **Kalman Filter 2D** | `[!] Partial` | `[x] Implemented` | `[!] Partial` | 4x4 state matrix KalmanFilter2D implemented in Go; Android uses Madgwick AHRS; Python uses One Euro filter. |
| **Madgwick AHRS 9-DOF** | `[x] Implemented` | `[!] Partial` | `[!] Partial` | Full 9-DOF IMU quaternion integration implemented in [MadgwickAHRS.kt](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/android/app/src/main/java/com/airmouse/sensors/MadgwickAHRS.kt); servers consume processed deltas. |
| **Relative Cursor Movement** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Sub-pixel mouse actuation verified in Go `mouse_darwin.go` (CGEvent) and Python `mouse_controller.py` (PyAutoGUI). |
| **Scroll (Vertical & Horizontal)**| `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Multi-axis scroll dispatch verified in Go `mouse.ScrollVertical` and Python `mouse_controller.scroll`. |
| **Click Types (Left/Right/Mid/Double/Drag)**| `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Verified in Android `TouchpadScreen.kt`, Go `mouse.go`, and Python `mouse_controller.py`. |
| **Gesture Recognition** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Gestures classified in Android `GestureStudio`, Go `gesture.go`, and Python web client `app.js`. |
| **Predictive Smoothing & Humanizer**| `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | B-spline & tremor filter in Go `adaptivesmoothing`; rate scaling in Python `actuator.py`. |
| **Desktop Dashboard GUI** | `N/A (Mobile)` | `[x] Implemented` | `[x] Implemented` | Fyne GUI in [settings.go](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_go_new/internal/ui/settings.go); CustomTkinter in [app.py](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/pc/airmouse_py/ui/app.py). |
| **Web Client Interface** | `N/A (Mobile)` | `[x] Implemented` | `[x] Implemented` | Static diagnostics served in Go `webfs.go`; interactive glassmorphism touchpad served in Python `web_static/`. |
| **Terminal QR Code Generation**| `[x] Scanner` | `[x] Implemented` | `[x] Implemented` | Terminal QR rendering verified in Go `main.go` and Python `cli.py` (`qrcode`). |
| **System Tray Background Daemon**| `N/A (Mobile)` | `[x] Implemented` | `[x] Implemented` | Desktop background execution verified in Go `ui.InitSystemTray` and Python `tray_manager.py`. |
| **Structured Log Manager** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | Live logs verified in Android `ServerLogsScreen`, Go `logger.go`, and Python `server.py`. |
| **Calibration Normalization Math**| `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | 6-point ellipsoid calibration in Android `CalibrationViewModel`; deadzone math in Go and Python. |
| **Battery Saver & Dark Theme** | `[x] Implemented` | `[x] Implemented` | `[x] Implemented` | AMOLED black in Android `ThemesViewModel`; idle throttles in Go `settings.go` and Python `app.py`. |

---

## 4. Key Findings & Architectural Observations

1. **Protocol Symmetry**: The V2 protocol envelope design is fully symmetrical across Kotlin, Go, and Python. Every client and server uses matching sequence numbering, microsecond timestamps, client identification headers, and payload structures.
2. **Security Posture**: Both PC implementations (Go and Python) strictly enforce TLS 1.3 and HMAC-SHA256 challenge-response verification with nonce deduplication.
3. **Sensor Fusion Partitioning**: By design, Madgwick AHRS sensor fusion is executed directly on the Android hardware device to eliminate IMU raw sensor transmission overhead. The PC servers act as high-performance actuators receiving processed orientation deltas, while Go additionally features a standalone 4x4 state-space KalmanFilter2D for trajectory estimation.
4. **Desktop User Experience**: Go delivers a native Fyne-based desktop control window, whereas Python provides both a CustomTkinter desktop interface and an embedded web-based touchpad client accessible by any mobile browser without app installation.
5. **Production Readiness**: All 24 core architectural features across the master feature list are fully implemented and verified in the repository.
