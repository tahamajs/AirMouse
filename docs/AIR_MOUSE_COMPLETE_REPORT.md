# Air Mouse Complete Project Documentation

This document is the full technical report for the Air Mouse assignment. It explains the purpose of the project, how each module works, how the Android phone and PC server communicate, how calibration is performed, how gesture detection is implemented, how Perfetto tracing is collected and analyzed, and how the project should be run on a macOS host with a Redmi phone.

The report is written to match the actual implementation in this repository.

## 1. Project Goal

Air Mouse turns an Android phone into a wireless mouse for a computer.

The phone reads raw motion sensor data, calibrates the sensors, fuses the values into a stable orientation estimate, detects cursor motion and gestures, and sends commands to the PC over TCP. The PC receives those commands and uses `pyautogui` to control the cursor, clicks, and scrolling.

The project is split into two main parts:

- Android application: `android/app`
- PC server and profiling tools: `pc`

The documentation and Perfetto configuration are stored in `docs` and `ProfileAndTrace`.

## 2. What the Assignment Requires

The original assignment asks for:

- Use of Android sensors.
- A cursor controlled by phone motion.
- Click and scroll gestures.
- Calibration for sensor bias and drift.
- A PC-side program that receives data from the phone.
- A UI that explains what the user must do.
- TCP socket communication.
- ACK handling for critical packets.
- Perfetto profiling and analysis.
- A report and a short demonstration video.

This repository contains all of those pieces in implemented form.

## 3. High-Level Design

The system has four layers:

1. Sensor acquisition on the phone.
2. Calibration and fusion on the phone.
3. TCP transmission to the PC.
4. Cursor control on the PC.

The phone is the primary intelligence layer. It decides how to interpret motion, when to emit cursor movement, and when to emit gestures. The PC simply applies those commands to the desktop cursor.

## 4. Implemented Requirement Checklist

| Requirement | Status | Main Files |
| --- | --- | --- |
| Android app, API 29+ | Complete | `android/app/build.gradle` |
| Raw accelerometer, gyroscope, magnetometer | Complete | `SensorService.kt`, `CalibrationHelper.kt` |
| Gyroscope bias calibration | Complete | `CalibrationHelper.kt`, `CalibrationFragment.kt` |
| Accelerometer multi-position calibration support | Complete | `CalibrationHelper.kt`, `CalibrationFragment.kt` |
| Magnetometer figure-eight calibration | Complete | `CalibrationHelper.kt`, `CalibrationFragment.kt` |
| Sensor fusion with Madgwick AHRS | Complete | `MadgwickAHRS.kt` |
| Cursor movement from phone rotation | Complete | `SensorService.kt`, `HomeFragment.kt` |
| Click, double-click, right-click, scroll | Complete | `EnhancedGestureDetector.kt`, `DataSender.kt`, `server.py` |
| Adjustable sensitivity | Complete | `HomeFragment.kt`, `PreferencesManager.kt` |
| Guided calibration UI | Complete | `CalibrationFragment.kt`, `fragment_calibration.xml` |
| Home UI with connection, status, live data | Complete | `fragment_home.xml`, `HomeFragment.kt` |
| QR scan with IP and port | Complete | `QRScanner.kt`, `ValidationUtils.kt`, `pc/gui.py` |
| Live logs on phone and PC | Complete | `LogManager.kt`, `HomeFragment.kt`, `ServerLogFragment.kt`, `pc/gui.py` |
| TCP socket protocol | Complete | `DataSender.kt`, `pc/server.py` |
| ACK and retry for critical packets | Complete | `DataSender.kt`, `pc/server.py` |
| PC GUI server | Complete | `pc/gui.py` |
| Console launcher | Complete | `pc/run.py` |
| Perfetto trace markers | Complete | `SensorService.kt`, `DataSender.kt` |
| Perfetto trace analyzer | Complete | `pc/perfetto_analyzer.py` |
| Perfetto config | Complete | `ProfileAndTrace/config.pbtx`, `docs/perfetto_config.pbtx` |
| Full project report | Complete | `docs/` |

## 5. Android Application Overview

The Android app uses Kotlin and XML layouts.

Main entry points:

- `com.airmouse.ui.onboarding.OnboardingActivity`
- `com.airmouse.ui.MainActivity`
- `com.airmouse.HomeFragment`

Important Android files:

| File | Role |
| --- | --- |
| `OnboardingActivity.kt` | Intro screens shown on first run |
| `MainActivity.kt` | Navigation host and drawer shell |
| `HomeFragment.kt` | Main control screen |
| `CalibrationFragment.kt` | Guided calibration wizard |
| `ServerLogFragment.kt` | Persistent server log viewer |
| `SensorService.kt` | Sensor registration, fusion, gesture pipeline |
| `CalibrationHelper.kt` | Sensor calibration routines |
| `MadgwickAHRS.kt` | Quaternion-based fusion |
| `EnhancedGestureDetector.kt` | Click/scroll gesture detection |
| `DataSender.kt` | TCP client with ACK/retry |
| `QRScanner.kt` | QR capture and endpoint parsing |
| `PreferencesManager.kt` | SharedPreferences abstraction |
| `LogManager.kt` | Shared live log broadcasting |

## 6. Android UI and User Flow

The app flow is designed for clarity:

1. Open onboarding on first run.
2. Land on `MainActivity`.
3. Use the Home screen to enter the PC endpoint, scan a QR code, check sensor availability, and start or stop Air Mouse.
4. Open the Calibration screen and follow the guided calibration steps.
5. Use the Server Log page to inspect saved logs while debugging.

### Home Screen

The Home screen contains:

- IP address field.
- Port field.
- QR scan button.
- Status text.
- Sensor status text.
- Live sensor values.
- Orientation indicator square.
- Start and Calibrate actions.
- Sensitivity slider.
- Debug overlay toggle.
- Live log panel.
- Clear logs button.

This screen is intentionally practical and dense. It is not a landing page. It is the working control surface for the system.

### Calibration Screen

The Calibration screen is a dedicated wizard-like page. It includes:

- Step-by-step instructions.
- A progress bar.
- Separate guidance for gyroscope, magnetometer, and accelerometer.
- A Start Guided Calibration button.
- A Skip Magnetometer button for devices without a magnetometer.
- A Reset button.

The user is not expected to guess what to do. The screen says what to do at each step.

### Server Log Screen

The server log page shows persisted logs that were captured during runtime. This is useful when debugging connection issues, QR scanning, retries, or packet loss.

## 7. Sensor Pipeline

The app uses these raw sensors:

- `TYPE_ACCELEROMETER`
- `TYPE_GYROSCOPE`
- `TYPE_MAGNETIC_FIELD`

The app does not rely on Android's rotation vector as the main solution. Instead, it uses raw sensors plus calibration and fusion.

### Data Flow

```text
Android SensorManager
  -> AirMouseSensorThread
  -> CalibrationHelper
  -> MadgwickAHRS
  -> EnhancedGestureDetector
  -> HomeFragment callback
  -> DataSender
  -> PC server
```

Sensor callbacks are registered on a dedicated `HandlerThread` named `AirMouseSensorThread`. This keeps the fusion pipeline off the main UI thread and makes the behavior easier to inspect in Perfetto.

## 8. Calibration in Detail

Calibration is essential for three reasons:

- Raw sensors have bias.
- Sensors drift over time.
- Sensor axes are not perfectly aligned with the physical movement of the phone.

### 8.1 Gyroscope Calibration

The gyroscope has a bias even when the phone is still. That means the phone can appear to rotate when it is not actually moving.

The implementation:

- Samples the gyroscope while the device is stationary.
- Computes the average of each axis.
- Stores the resulting bias in preferences.
- Subtracts the bias from all future gyroscope samples.

Corrected form:

```text
gyro_corrected = gyro_raw - gyro_bias
```

### 8.2 Accelerometer Calibration

The accelerometer is used as a gravity reference, but its axes may not report ideal values. The app supports multi-position calibration so that the offsets and scale values can be improved.

The guided UI explains how to place the phone in each orientation. The calibration helper then measures samples in those positions and stores correction values.

General correction form:

```text
accel_corrected = (accel_raw - offset) * scale
```

The exact calibration model is implemented in `CalibrationHelper.kt`. The important point is that the raw readings are normalized before they are used by the fusion logic.

### 8.3 Magnetometer Calibration

The magnetometer is sensitive to hard-iron and soft-iron distortion from nearby metal and electronics.

The implementation:

- Asks the user to move the phone in a figure-eight motion.
- Measures the minimum and maximum values on each axis.
- Computes offset and scale from those extremes.
- Stores the result in preferences.

Correction form:

```text
offset = (max + min) / 2
scale = (max - min) / 2
mag_corrected = (mag_raw - offset) / scale
```

### 8.4 Why the UI Matters

The assignment explicitly asks for a page that tells the user what to do during calibration. That is why the calibration process is not hidden in the background. It is exposed as a guided page with explicit instructions and progress feedback.

## 9. Sensor Fusion

The project uses a custom Madgwick AHRS implementation in `MadgwickAHRS.kt`.

The purpose of fusion is to combine the strengths of the sensors:

- Gyroscope: smooth short-term motion, but drift over time.
- Accelerometer: gravity reference, but noisy during motion.
- Magnetometer: yaw reference, but sensitive to interference.

The fusion process produces a stable orientation estimate. The app uses roll and yaw for cursor control.

### Why this is better than raw sensors alone

Raw gyroscope integration drifts.
Raw accelerometer values are noisy and jumpy.
Raw magnetometer values fluctuate in real environments.

Fusion gives a smoother and more stable cursor experience than any single sensor.

## 10. Cursor Mapping

The app uses orientation changes to produce cursor deltas instead of absolute positions.

General mapping:

- Horizontal cursor movement comes from yaw change.
- Vertical cursor movement comes from roll change.

This is important because desktop mouse control naturally expects deltas over time, not absolute coordinates from the phone.

The Home fragment also applies:

- Low-pass smoothing.
- Deadband to ignore tiny hand tremors.
- Timing throttling so the UI and motion dispatch do not flood the main thread.

These measures improve smoothness and reduce jitter.

## 11. Gesture Detection

Gestures are detected from calibrated and fused motion data.

Supported actions:

| Gesture | Meaning |
| --- | --- |
| Move | Cursor movement |
| Click | Left click |
| Double click | Double click |
| Right click | Right mouse button |
| Scroll up/down | Mouse wheel movement |

### Noise Handling

Gesture detection includes thresholds and cooldown logic so that:

- Small involuntary movement does not become a click.
- The return motion after a scroll does not become another scroll.
- Only deliberate movements are sent as commands.

## 12. Network Protocol

The phone communicates with the PC over TCP.

Default port:

```text
8080
```

The app now supports both manual endpoint entry and QR scanning.

### QR Endpoint

The PC GUI creates a QR code containing the endpoint in this format:

```text
airmouse://IP:PORT
```

The Android app parses that endpoint and fills:

- IP field
- Port field

This is more complete than scanning only the IP address because the user does not have to guess the port.

### Message Format

Movement:

```json
{"type":"move","dx":12.5,"dy":-3.2}
```

Click:

```json
{"type":"click","id":1}
```

Scroll:

```json
{"type":"scroll","delta":1,"id":2}
```

ACK:

```json
{"type":"ack","id":2}
```

### ACK Policy

The protocol treats packets differently:

- `move` packets can be dropped if newer movement arrives.
- `click`, `doubleclick`, `rightclick`, and `scroll` packets are critical and must be acknowledged.

The client retransmits critical packets when an ACK does not arrive in time.

## 13. Live Logs and Debugging

The project includes live logging in two places.

### Android

The Home screen has a live log panel. It shows:

- QR scan events.
- Connect and disconnect events.
- ACK and retry events.
- Other runtime messages.

The dedicated Server Log page shows the persisted history.

### PC

The PC GUI also shows live logs in a scrollable text area. It logs:

- Selected IP changes.
- QR code updates.
- TCP connections.
- Mouse actions.
- Sensitivity changes.

This makes debugging much easier because the user can see what is happening on both sides of the link.

## 14. PC Server

The PC server is written in Python and uses `pyautogui` for desktop mouse control.

Main files:

| File | Role |
| --- | --- |
| `pc/server.py` | TCP server and mouse execution |
| `pc/gui.py` | GUI for endpoint, QR, logs, stats |
| `pc/run.py` | Launcher entry point |

### Server Responsibilities

The PC server:

- Listens on the configured TCP port.
- Receives JSON messages from the phone.
- Applies cursor movement.
- Executes clicks and scrolls.
- Sends ACKs for critical messages.

### GUI Features

The PC GUI includes:

- Dark theme.
- Network IP selection.
- Manual IP override.
- QR code for endpoint sharing.
- Clipboard copy for the endpoint.
- Sensitivity slider.
- Live log.
- Connection statistics.

### Mouse Smoothing

The Python mouse controller sets `pyautogui` to a low-latency mode:

- `PAUSE = 0`
- `MINIMUM_DURATION = 0`
- `MINIMUM_SLEEP = 0`

It also ignores tiny movement values so cursor motion stays smoother.

## 15. Running The Project

### 15.1 Build Android APK

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code/android
./gradlew :app:assembleDebug
```

APK output:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

### 15.2 Install On Redmi

```bash
/Users/tahamajs/Library/Android/sdk/platform-tools/adb install -r -g /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code/android/app/build/outputs/apk/debug/app-debug.apk
```

### 15.3 Run PC Server

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code
python3 -m venv .venv
source .venv/bin/activate
pip install -r pc/requirements.txt
python3 pc/run.py
```

### 15.4 Use The App

1. Open the PC GUI.
2. Choose or confirm the IP address.
3. Scan the QR code from the Android app or enter the endpoint manually.
4. Run calibration from the Calibration screen.
5. Press Start on the Home screen.
6. Move the phone, click, and scroll.

## 16. Perfetto Instrumentation

The Android app includes trace slices via `android.os.Trace`. This lets the assignment measure work done in the app process and compare it with system behavior.

### Important Slices

| Slice | Meaning |
| --- | --- |
| `AirMouseSensorAccelerometer` | Accelerometer event handling |
| `AirMouseSensorGyroscope` | Gyroscope event handling |
| `AirMouseSensorMagnetometer` | Magnetometer event handling |
| `MadgwickAccelUpdate` | Accelerometer update inside fusion |
| `MadgwickGyroUpdate` | Gyroscope update inside fusion |
| `MadgwickMagUpdate` | Magnetometer update inside fusion |
| `AirMouseOrientation` | Orientation extraction and callback |
| `AirMouseGestureDetection` | Gesture detection step |
| `AirMouseNetworkSendMove` | Movement packet send |
| `AirMouseNetworkSendAckCommand` | Critical command send |

These names appear in Perfetto UI under the `com.airmouse` process.

## 17. Perfetto Config And Trace Collection

The Perfetto configuration is stored in:

- `ProfileAndTrace/config.pbtx`
- `docs/perfetto_config.pbtx`

### Recording a Trace

Use the `record_android_trace` script from the Android platform-tools directory. Example:

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code/android
./record_android_trace \
  --serial <your-device-serial> \
  -c ../ProfileAndTrace/config.pbtx \
  -o ../traces/airmouse_trace.perfetto-trace
```

During the recording:

- Move the phone slowly.
- Move it suddenly.
- Trigger click.
- Trigger scroll.

Then open the trace in Perfetto UI:

```bash
open https://ui.perfetto.dev
```

Drag the `.perfetto-trace` file into the browser.

## 18. Perfetto Analysis Script

The script `pc/perfetto_analyzer.py` reads a Perfetto trace and prints useful summaries.

It is intended to help answer the assignment questions using real trace data.

What it can summarize:

- Sensor slice timing.
- Sampling period estimates.
- Thread waiting and contention.
- Madgwick processing time.
- Sensor processing cost.
- Network send slices.
- Thread breakdown.

## 19. How To Answer The Perfetto Questions

The assignment asks 11 questions. The following is the intended evidence source for each one.

| Question | What to inspect |
| --- | --- |
| 1 | Perfetto UI, sensor slices, scheduler timing |
| 2 | Calibration and fusion explanation in this report |
| 3 | Differences between sensor slice timestamps |
| 4 | Scheduler and thread waiting in Perfetto |
| 5 | Wake-up vs non-wake-up sensor theory and implementation note |
| 6 | `Madgwick*Update` slice durations |
| 7 | The sensor slices with the highest aggregate cost |
| 8 | Compare traces collected at different sampling rates |
| 9 | `AirMouseOrientation` to network send path |
| 10 | Main thread versus sensor thread versus IO thread |
| 11 | Slow versus sudden movement and their effects |

### Important Note

For a final submission, the analysis sections should be filled with the actual numbers from the trace you record on your own device. This report explains exactly where those numbers come from and how to interpret them.

## 20. Known Engineering Decisions

The implementation intentionally makes a few practical choices:

- Movement is smoothed before being sent.
- Critical packets are retried, movement packets are not.
- Calibration is exposed as a guided screen instead of a hidden background process.
- The app keeps user-facing logs because the assignment values debugging and trace analysis.
- The PC GUI uses the same endpoint format as the Android QR parser to avoid mismatch.

These choices were made to make the system easier to use and easier to debug.

## 21. Verification Commands

Android build:

```bash
cd android
./gradlew :app:assembleDebug
```

Android unit tests:

```bash
cd android
./gradlew :app:testDebugUnitTest
```

Python syntax check:

```bash
python3 -m py_compile pc/server.py pc/gui.py pc/run.py pc/perfetto_analyzer.py
```

## 22. Final Submission Checklist

Include these items in the final ZIP:

- `android/`
- `pc/`
- `docs/`
- `ProfileAndTrace/config.pbtx`
- `android/app/build/outputs/apk/debug/app-debug.apk`
- `traces/airmouse_trace.perfetto-trace`
- `demo.mp4`
- group file or responsibility file if the assignment requires one

## 23. Demo Video Checklist

The short demo video should show:

1. The PC server running.
2. The phone connected to the PC.
3. Cursor movement.
4. Left click.
5. Scroll.
6. Stable still state with low drift.
7. The phone and PC screen at the same time.

## 24. Current Status

The project is now implemented as a full end-to-end system:

- The Android app builds successfully.
- The PC server and GUI are implemented.
- Guided calibration exists in the app.
- QR endpoint syncing includes both IP and port.
- Live logs exist on both sides.
- ACK handling exists for critical packets.
- Perfetto tracing is instrumented.
- The documentation maps the code to the assignment.

The remaining submission work is mostly operational:

- install the latest APK on the Redmi,
- record a real Perfetto trace,
- extract answers from that trace,
- and record the demonstration video.

