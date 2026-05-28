# Air Mouse Complete Project Documentation

This document is the clean reference for the Air Mouse project. It explains what each part does, how to run it on macOS with a Redmi phone, how the Android app and Python server communicate, and how to collect Perfetto traces for the assignment questions.

## 1. Project Goal

Air Mouse turns an Android phone into a wireless mouse. The phone reads raw motion sensors, corrects calibration errors, fuses sensor data into orientation, detects movement/click/scroll gestures, and sends commands to a computer over TCP. The computer server receives those commands and controls the cursor.

The implementation has two main parts:

- Android app: `android/app`
- PC server and analysis tools: `pc`

Documentation and reports are in `docs`.

## 2. Implemented Requirements Checklist

| Requirement | Status | Location |
| --- | --- | --- |
| Android app, API 29+ | Complete | `android/app/build.gradle` |
| Raw accelerometer, gyroscope, magnetometer | Complete | `SensorService.kt`, `CalibrationHelper.kt` |
| Gyroscope bias calibration | Complete | `CalibrationHelper.kt` |
| Accelerometer six-position calibration support | Complete | `CalibrationHelper.kt` |
| Magnetometer figure-eight calibration | Complete | `CalibrationHelper.kt` |
| Sensor fusion | Complete | `MadgwickAHRS.kt` |
| Cursor movement from phone rotation | Complete | `HomeFragment.kt`, `SensorService.kt` |
| Click, double-click, right-click, scroll | Complete | `EnhancedGestureDetector.kt`, `DataSender.kt`, `server.py` |
| Sensitivity controls | Complete | `HomeFragment.kt`, `PreferencesManager.kt` |
| Android UI for IP, status, calibration, sensors | Complete | `fragment_home.xml`, `HomeFragment.kt` |
| QR server scanning | Complete | `QRScanner.kt`, `HomeFragment.kt` |
| TCP socket protocol | Complete | `DataSender.kt`, `pc/server.py` |
| ACK for critical commands | Complete | `DataSender.kt`, `pc/server.py` |
| Python PC server | Complete | `pc/server.py`, `pc/gui.py`, `pc/run.py` |
| Perfetto trace markers | Complete | `SensorService.kt`, `DataSender.kt` |
| Perfetto analyzer | Complete | `pc/perfetto_analyzer.py` |
| Perfetto config | Complete | `ProfileAndTrace/config.pbtx`, `docs/perfetto_config.pbtx` |
| Full documentation | Complete | `docs/` |

## 3. Android Architecture

The Android app uses classic XML views with Kotlin. The launcher activity is:

```text
com.airmouse.ui.onboarding.OnboardingActivity
```

After onboarding, the app opens:

```text
com.airmouse.ui.MainActivity
```

The main feature screen is:

```text
com.airmouse.HomeFragment
```

Important Android files:

| File | Responsibility |
| --- | --- |
| `OnboardingActivity.kt` | First-run intro screens |
| `MainActivity.kt` | Navigation host and app shell |
| `HomeFragment.kt` | Main Air Mouse controls and callbacks |
| `SensorService.kt` | Sensor registration, processing, Perfetto slices |
| `MadgwickAHRS.kt` | Sensor fusion |
| `CalibrationHelper.kt` | Gyro, accelerometer, magnetometer calibration |
| `EnhancedGestureDetector.kt` | Click and scroll gesture detection |
| `DataSender.kt` | TCP client and ACK/retry logic |
| `QRScanner.kt` | QR scan for server address |
| `PreferencesManager.kt` | Persistent app settings |

## 4. Sensor Processing

The app uses these raw Android sensors:

- `TYPE_ACCELEROMETER`
- `TYPE_GYROSCOPE`
- `TYPE_MAGNETIC_FIELD`

The app does not depend on Android's already-fused rotation vector as the main solution. It performs its own calibration and fusion.

Sensor processing flow:

```text
Android SensorManager
  -> AirMouseSensorThread
  -> CalibrationHelper
  -> MadgwickAHRS
  -> EnhancedGestureDetector
  -> HomeFragment callbacks
  -> DataSender
  -> PC server
```

Sensor callbacks run on a named `HandlerThread`:

```text
AirMouseSensorThread
```

This keeps sensor fusion away from the main UI thread and makes it easy to see thread separation in Perfetto.

## 5. Calibration

### Gyroscope

The phone is kept still. The app samples gyroscope values and computes average bias. Later, it subtracts that bias from raw gyroscope readings.

Corrected value:

```text
gyro_corrected = gyro_raw - gyro_bias
```

### Accelerometer

The assignment asks for six-position calibration. The implementation supports offset and scale correction so readings can be mapped toward real acceleration values.

Correction model:

```text
accel_corrected = (accel_raw - offset) * scale
```

### Magnetometer

The user moves the phone in a figure-eight motion. The app tracks min/max values for each axis.

Correction model:

```text
offset = (max + min) / 2
scale = (max - min) / 2
mag_corrected = (mag_raw - offset) / scale
```

## 6. Sensor Fusion

`MadgwickAHRS.kt` keeps a quaternion orientation estimate.

Inputs:

- Gyroscope: fast angular velocity, good short-term response, but drifts over time.
- Accelerometer: gravity reference, helps stabilize roll/pitch, but noisy during movement.
- Magnetometer: heading reference, helps yaw, but is sensitive to magnetic interference.

The fusion algorithm combines these strengths:

- Gyroscope gives smooth fast motion.
- Accelerometer reduces drift.
- Magnetometer reduces heading/yaw drift.

The app exposes roll and yaw for cursor movement:

```text
deltaX = yaw change * sensitivity
deltaY = roll change * sensitivity
```

The app sends deltas, not absolute cursor coordinates, as required.

## 7. Gesture Detection

Gesture detection is based on calibrated/fused motion values:

| Action | Detection basis |
| --- | --- |
| Move cursor | Roll/yaw changes |
| Left click | Fast Y-axis related motion / configured threshold |
| Double click | Two click gestures within a time window |
| Right click | Roll threshold |
| Scroll up/down | Fast Y-axis movement with sign-based direction |

Noise control:

- Thresholds prevent small hand shake from becoming click/scroll.
- Cooldowns prevent the return movement from being interpreted as the opposite scroll.
- Sensitivity is configurable from the UI.

## 8. Network Protocol

The phone connects to the PC using TCP on port `8080`.

Movement packet:

```json
{"type":"move","dx":12.5,"dy":-3.2}
```

Click packet:

```json
{"type":"click","id":1}
```

Scroll packet:

```json
{"type":"scroll","delta":1,"id":2}
```

ACK packet from server:

```json
{"type":"ack","id":2}
```

Movement packets are allowed to be dropped because newer movement replaces older movement. Click and scroll packets are stored until ACK is received, and retransmitted on timeout.

## 9. PC Server

The Python server receives JSON lines from the phone and uses `pyautogui` to control the mouse.

Files:

| File | Responsibility |
| --- | --- |
| `pc/server.py` | Core TCP server and mouse execution |
| `pc/gui.py` | GUI server with QR code and settings |
| `pc/run.py` | Friendly launcher |
| `pc/config.json` | Saved server settings |
| `pc/requirements.txt` | Python dependencies |

Run on macOS:

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code
python3 -m venv .venv
source .venv/bin/activate
pip install -r pc/requirements.txt
python3 pc/run.py
```

macOS must allow the terminal/Python app in:

```text
System Settings -> Privacy & Security -> Accessibility
```

## 10. Android Build And Redmi Run

Build:

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code/android
./gradlew :app:assembleDebug
```

APK:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Install on Redmi:

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code
/Users/tahamajs/Library/Android/sdk/platform-tools/adb devices -l
/Users/tahamajs/Library/Android/sdk/platform-tools/adb install -r -g android/app/build/outputs/apk/debug/app-debug.apk
```

Launch:

```bash
/Users/tahamajs/Library/Android/sdk/platform-tools/adb shell am start -n com.airmouse/.ui.onboarding.OnboardingActivity
```

## 11. Perfetto Instrumentation

The Android code now emits named Perfetto slices with `android.os.Trace`.

Important slices:

| Slice | Meaning |
| --- | --- |
| `AirMouseSensorAccelerometer` | Accelerometer event processing |
| `AirMouseSensorGyroscope` | Gyroscope event processing |
| `AirMouseSensorMagnetometer` | Magnetometer event processing |
| `MadgwickAccelUpdate` | Accelerometer correction part of fusion |
| `MadgwickGyroUpdate` | Gyroscope integration part of fusion |
| `MadgwickMagUpdate` | Magnetometer correction part of fusion |
| `AirMouseOrientation` | Roll/yaw extraction and callback |
| `AirMouseGestureDetection` | Click/scroll gesture detection |
| `AirMouseNetworkSendMove` | Movement packet send |
| `AirMouseNetworkSendAckCommand` | Click/scroll packet send |

These names appear in Perfetto UI under the `com.airmouse` process.

## 12. Record A Perfetto Trace On macOS

Keep the Redmi connected with USB debugging or wireless debugging.

Short command:

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code
mkdir -p traces
cd android
./record_android_trace \
  --serial adb-1406c5db-mjONde._adb-tls-connect._tcp \
  -o ../traces/airmouse_trace.perfetto-trace \
  -t 15s \
  -b 64mb \
  -a com.airmouse \
  sched freq idle wm gfx view am input
```

Config-file command:

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code/android
./record_android_trace \
  --serial adb-1406c5db-mjONde._adb-tls-connect._tcp \
  -c ../ProfileAndTrace/config.pbtx \
  -o ../traces/airmouse_trace.perfetto-trace
```

During the 15-second recording:

1. Start Air Mouse.
2. Move phone slowly.
3. Move phone suddenly.
4. Trigger click.
5. Trigger scroll.
6. Stop recording and save trace.

Open the trace:

```bash
open https://ui.perfetto.dev
```

Then drag `traces/airmouse_trace.perfetto-trace` into the browser.

## 13. Analyze Trace With Python

Install dependencies:

```bash
cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code
python3 -m venv .venv
source .venv/bin/activate
pip install -r pc/requirements.txt
```

Run analyzer:

```bash
python3 pc/perfetto_analyzer.py traces/airmouse_trace.perfetto-trace
```

The analyzer prints:

- Sensor event samples.
- Sampling period estimates.
- Thread waiting information.
- Madgwick CPU time.
- Sensor processing cost.
- Network send timing.
- Thread breakdown.

## 14. Perfetto Questions: What To Use

| Question | Evidence |
| --- | --- |
| 1. Sensor request to data delivery | Perfetto UI: SensorService/system slices, `AirMouseSensor*` app slices |
| 2. Sensor behavior and raw errors | Documentation plus calibration/fusion implementation |
| 3. Sampling period comparison | Analyzer `q3_sampling_periods`, gyroscope slice deltas |
| 4. Thread/system-call contention | Analyzer `q4_thread_waiting`, Perfetto scheduler view |
| 5. Wake-up vs non-wake-up | Android sensor concept answer; app uses normal active sensors |
| 6. Filter CPU time | Analyzer `q6_filter_cpu_time` using `Madgwick*Update` slices |
| 7. Most expensive sensor | Analyzer `q7_sensor_processing_cost` |
| 8. Sampling-rate effect | Compare trace at `SENSOR_DELAY_GAME` vs low-power mode |
| 9. Sensor-to-network/cursor latency | `AirMouseOrientation` and `AirMouseNetworkSend*` slices |
| 10. Thread separation | `AirMouseSensorThread`, main thread, Dispatchers.IO/network |
| 11. Slow vs sudden movement | Compare trace sections with slow movement and sudden gestures |

## 15. Verification Commands

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

Python syntax:

```bash
python3 -m py_compile pc/server.py pc/gui.py pc/run.py pc/perfetto_analyzer.py
```

Crash check after launch:

```bash
/Users/tahamajs/Library/Android/sdk/platform-tools/adb logcat -d -t 250 AndroidRuntime:E TransactionExecutor:E '*:S'
```

## 16. Submission Checklist

Include:

```text
android/
pc/
docs/
README.md
android/app/build/outputs/apk/debug/app-debug.apk
traces/airmouse_trace.perfetto-trace
demo.mp4
```

For the demo video, show:

1. PC server running.
2. Phone connected to server.
3. Cursor movement.
4. Left click.
5. Scroll up/down.
6. Stable no-drift state when phone is still.
7. Perfetto trace file or analyzer output.

## 17. Current Status

The app builds, installs on Redmi Note 8T, opens onboarding, opens MainActivity, and includes trace instrumentation for the assignment profiling questions. The Python server and analyzer are included, and the documentation maps the implementation to the assignment requirements.
