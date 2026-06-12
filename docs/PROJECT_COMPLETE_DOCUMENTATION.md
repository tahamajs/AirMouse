# Air Mouse Complete Project Documentation

University of Tehran - Embedded Systems - Computer Assignment 2

This document is the main submission guide for the project. It explains what each part does, where it is implemented, how the Android app and PC server communicate, and how the implementation satisfies the assignment requirements.

## 1. Project Goal

The project turns an Android phone into an air mouse. The phone reads raw motion sensors, calibrates and filters them, detects gestures, and sends mouse commands to a PC server over TCP. The PC server receives those commands and uses `pyautogui` to move the cursor, click, double-click, right-click, and scroll.

The system has two executable parts:

- Android app: `android/`
- PC server: `pc/`

## 2. Assignment Requirement Mapping

| Assignment requirement | Implemented in |
| --- | --- |
| Android 10/API 29+ support | `android/app/build.gradle`, `minSdk 29` |
| Raw accelerometer/gyroscope/magnetometer use | `SensorService.kt`, `CalibrationHelper.kt` |
| Gyroscope bias calibration | `CalibrationHelper.kt`, `CalibrationActivity.kt`, `GyroComposeFragment.kt` |
| Accelerometer calibration | `CalibrationHelper.kt`, `CalibrationActivity.kt`, `AccelComposeFragment.kt` |
| Magnetometer figure-8 calibration | `CalibrationHelper.kt`, `CalibrationActivity.kt`, `MagComposeFragment.kt` |
| Sensor fusion for drift reduction | `MadgwickAHRS.kt`, `MadgwickFusion.kt` |
| Horizontal/vertical cursor movement from phone orientation | `HomeFragment.attachSensorCallbacks()` |
| Left click from fast Y-axis rotation | `EnhancedGestureDetector.kt` |
| Double click from two fast rotations | `EnhancedGestureDetector.kt` |
| Right click from tilt hold | `EnhancedGestureDetector.kt` |
| Scroll up/down from fast Y-axis acceleration | `EnhancedGestureDetector.kt` |
| Adjustable sensitivity and thresholds | `PreferencesManager.kt`, `SettingsDialog.kt` |
| UI with IP entry, calibration, start button, direction indicator, debug sensor values | `fragment_home.xml`, `HomeFragment.kt` |
| Separate calibration workflow | `CalibrationActivity.kt`, `CalibrationPagerAdapter.kt`, `CalibrationHelper.kt`, `HomeFragment.startCalibration()` |
| TCP socket communication | `DataSender.kt`, `server.py`, `gui.py` |
| JSON packet format | `DataSender.kt`, `server.py`, `gui.py` |
| ACK for click/scroll commands | `DataSender.sendWithAck()`, server `_send_ack()` |
| Packet resend on missing ACK | `DataSender.sendWithAck()` |
| PC-side cursor/click/scroll control | `MouseController` in `server.py` and `gui.py` |
| Perfetto analysis support | `pc/perfetto_analyzer.py`, `docs/PERFETTO_ANSWERS.md` |

## 3. Android App Architecture

The Android app is written in Kotlin.

Main runtime flow:

1. `OnboardingActivity` opens first and explains the app.
2. The user enters the PC IP manually or scans the server QR code.
3. The user opens the dedicated calibration wizard and completes the sensor calibration steps.
4. The user taps Start.
5. `SensorService` starts accelerometer, gyroscope, and magnetometer listeners.
6. `MadgwickAHRS` combines sensor values into stable roll/yaw orientation.
7. `EnhancedGestureDetector` detects click, right-click, double-click, and scroll gestures.
8. `DataSender` sends JSON messages to the PC server.
9. The UI shows status, signal quality, sensor values, and direction indicator.

Important files:

- `android/app/src/main/java/com/airmouse/HomeFragment.kt`: main app screen and runtime coordination.
- `android/app/src/main/java/com/airmouse/sensors/SensorService.kt`: sensor collection and callbacks.
- `android/app/src/main/java/com/airmouse/ui/CalibrationActivity.kt`: host activity for the guided calibration wizard.
- `android/app/src/main/java/com/airmouse/calibration/CalibrationPagerAdapter.kt`: ViewPager2 adapter for the three guided steps.
- `android/app/src/main/java/com/airmouse/calibration/GyroComposeFragment.kt`, `AccelComposeFragment.kt`, `MagComposeFragment.kt`: the actual calibration step screens.
- `android/app/src/main/java/com/airmouse/sensors/CalibrationHelper.kt`: gyro, accelerometer, and magnetometer calibration math and persistence.
- `android/app/src/main/java/com/airmouse/sensors/MadgwickAHRS.kt`: orientation fusion.
- `android/app/src/main/java/com/airmouse/sensors/EnhancedGestureDetector.kt`: gesture detection.
- `android/app/src/main/java/com/airmouse/network/DataSender.kt`: TCP client, ACK handling, resend logic.
- `android/app/src/main/res/layout/fragment_home.xml`: main modern UI layout.

## 4. Sensor Calibration

The app supports the required calibration stages:

### Gyroscope

The user keeps the phone still. The app samples gyro values and estimates the bias. Later readings subtract this bias so the cursor does not drift while the phone is stationary.

### Accelerometer

The app estimates offset/scale values for acceleration. This reduces wrong tilt and false scroll detection.

### Magnetometer

The user moves the phone in a figure-8 motion. The app tracks min/max values per axis and computes:

```text
offset = (min + max) / 2
scale = (max - min) / 2
corrected = (raw - offset) / scale
```

This reduces hard-iron distortion and improves yaw stability.

## 5. Sensor Fusion

The project uses a Madgwick-style AHRS filter. Gyroscope data provides fast motion response, while accelerometer and magnetometer values correct long-term drift.

Why fusion is needed:

- Gyroscope alone drifts over time.
- Accelerometer alone is noisy and affected by hand vibration.
- Magnetometer alone is affected by magnetic distortion.
- Fusion combines their strengths and reduces their weaknesses.

The app uses fused roll/yaw values for cursor movement. It sends deltas, not absolute cursor positions, so the PC server can move the cursor smoothly.

## 6. Gesture Detection

Implemented gestures:

- Move cursor: phone rotation around the configured X/Z orientation axes.
- Left click: fast rotation around Y axis.
- Double click: two fast click gestures inside the configured interval.
- Right click: tilt hold for the configured duration.
- Scroll: fast positive/negative Y-axis acceleration.

False positives are reduced with:

- Speed thresholds.
- Scroll debounce threshold.
- Right-click hold duration.
- Battery/movement state tracking.
- User-configurable settings.

## 7. Network Protocol

Transport:

- TCP
- Default port: `8080`
- Encoding: UTF-8
- Framing: one JSON object per line

Movement packet:

```json
{"type":"move","dx":12.3,"dy":-4.8}
```

Click packet:

```json
{"type":"click","id":1}
```

Double-click packet:

```json
{"type":"doubleclick","id":2}
```

Right-click packet:

```json
{"type":"rightclick","id":3}
```

Scroll packet:

```json
{"type":"scroll","delta":1,"id":4}
```

ACK response from PC:

```json
{"type":"ack","id":4}
```

Movement packets do not require ACK because losing one movement frame is acceptable. Click and scroll packets require ACK because losing them changes user intent. The Android client resends critical packets when ACK does not arrive in time.

## 8. PC Server

The PC server has two modes:

- `pc/server.py`: console server.
- `pc/gui.py`: GUI server with QR code, logs, stats, and sensitivity control.

Recommended run command:

```bash
cd pc
python3 run.py
```

The server:

1. Listens for TCP clients on port `8080`.
2. Optionally answers UDP discovery on port `8081`.
3. Parses JSON messages.
4. Moves/clicks/scrolls using `pyautogui`.
5. Sends ACK for critical messages.
6. Shows logs and statistics in GUI mode.
7. Displays a QR code in `IP:port` format for the Android app.

Python dependencies are listed in `pc/requirements.txt`.

## 9. Android UI Features

The Android app includes:

- Onboarding screen.
- IP input field.
- QR scan button.
- Calibration button.
- Start/Stop button.
- Sensitivity slider.
- Sensor health/status text.
- Wi-Fi quality display.
- Live gyro/acceleration values.
- Orientation indicator.
- Settings dialog for thresholds.
- Debug overlay support.
- Statistics, help, profiles, accessibility, and extra feature screens.

The UI is built with Material Components and a bottom navigation layout.

## 10. Perfetto Support

The assignment asks 11 Perfetto questions. The project includes:

- `pc/perfetto_analyzer.py`: helper analyzer script.
- `docs/PERFETTO_ANSWERS.md`: ready-to-use answers and queries.
- `docs/Complete_Answers.md`: deeper explanatory answers.

For a real submission, collect a trace from the emulator or physical device while using the app, then add screenshots/results to the report.

## 11. Build And Test

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

PC syntax check:

```bash
python3 -m py_compile pc/server.py pc/gui.py pc/run.py pc/perfetto_analyzer.py
```

APK output:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## 12. Demo Video Checklist

The final video should show both phone and laptop:

1. Start PC server.
2. Show QR code/IP.
3. Open Android app.
4. Calibrate sensors.
5. Connect to the server.
6. Move cursor horizontally and vertically.
7. Perform left click.
8. Perform double click.
9. Perform right click.
10. Scroll up/down.
11. Keep phone still to show no drift.

## 13. Known Environment Notes

- macOS requires Accessibility permission for the terminal/Python app before `pyautogui` can control the mouse.
- Phone and PC must be on the same local network.
- Firewalls must allow TCP port `8080`.
- Android cleartext traffic is enabled for local TCP communication.
- The project pins Gradle to Android Studio's bundled JBR so it avoids Java 23/Gradle compatibility issues.

## 14. Final Submission Contents

Recommended ZIP contents:

```text
android/
pc/
docs/
README.md
demo.mp4
report.pdf
android/app/build/outputs/apk/debug/app-debug.apk
```

Remove generated build folders if the course asks for a smaller source ZIP, but keep the APK separately.

## 15. Current Verification Status

Verified on 2026-05-28:

- Android debug APK builds successfully with `./gradlew :app:assembleDebug`.
- Android unit tests pass with `./gradlew :app:testDebugUnitTest`.
- PC server files pass syntax checks with `python3 -m py_compile pc/server.py pc/gui.py pc/run.py pc/perfetto_analyzer.py`.
- Redmi Note 8T install succeeds through ADB.
- Onboarding launches on the Redmi without the previous `MaterialButton` inflate crash.
- Skipping onboarding opens `com.airmouse.ui.MainActivity` on the Redmi without `AndroidRuntime` or `TransactionExecutor` crashes.
- QR scanning is registered against the `HomeFragment` lifecycle so MainActivity no longer crashes with `LifecycleOwners must call register before they are STARTED`.
