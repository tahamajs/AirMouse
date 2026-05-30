# Air Mouse Motion Smoothing and Android Sync Guide

This document replaces the earlier RNN-only draft with a **project-accurate implementation guide** for the current Air Mouse codebase.

The real system is split into two cooperating parts:

- **Android app**: detects motion, gestures, and optional on-device gesture inference.
- **Go desktop server**: receives commands, controls the mouse, and applies Kalman or ONNX-based smoothing.

The goal is to keep the Android and PC implementations aligned so the protocol, gestures, and smoothing behavior all match the code.

---

## 1. What the current project actually does

The previous version of this document described a hypothetical end-to-end RNN training pipeline. That is **not** the current implementation.

The code in this workspace currently supports:

- **Android gesture detection** from gyroscope, accelerometer, and roll values.
- **Delayed single-click / double-click handling** so a single flick is not emitted too early.
- **TFLite-based gesture inference** on Android using a sliding sensor window.
- **Gesture recording** to CSV for future training/fine-tuning.
- **Go-side predictive movement** using a Kalman filter.
- **Optional Go-side AI smoothing** using an ONNX model loaded with ONNX Runtime.
- **WebSocket message sending** from Android with `gesture`, `proximity`, and `control` payloads.

So the synced version of the project is best described as:

> **Android captures motion and gestures; the Go server smooths cursor motion and executes desktop actions.**

---

## 2. End-to-end data flow

```mermaid
flowchart LR
    A[Android sensors] --> B[GestureDetector.kt]
    A --> C[GestureInferenceService.kt]
    C --> D[WebSocketManager.sendGesture()]
    B --> D
    D --> E[Go protocol server]
    E --> F[MouseController]
    F --> G[Kalman predictor]
    F --> H[ONNX AI smoother]
    E --> I[Desktop mouse / scroll / click]
```

### Main flow

1. **Android** reads gyroscope and accelerometer data.
2. `GestureDetector` decides whether a motion is a click, double click, right click, or scroll.
3. `GestureInferenceService` can classify gestures using a TFLite model when that service is active.
4. `WebSocketManager` sends the gesture to the server as JSON.
5. **Go** receives the command and routes it to the mouse controller.
6. The mouse controller applies smoothing, predictive blending, and optional AI smoothing.
7. The desktop pointer moves or clicks on the host computer.

---

## 3. Android side: what is implemented

### 3.1 Threshold-based gesture detection

The core runtime detector is `android/app/src/main/java/com/airmouse/domain/GestureDetector.kt`.

It detects:

- `CLICK`
- `DOUBLE_CLICK`
- `RIGHT_CLICK`
- `SCROLL_UP`
- `SCROLL_DOWN`

#### Detection behavior

- **Click / double click**: uses `gyroY` angular speed and a delayed single-click callback.
- **Right click**: uses a long tilt (`roll`) held for a configured duration.
- **Scroll**: uses `accelY` and a debounce threshold to avoid repeated scroll spam.

This delayed callback pattern is important because it avoids emitting a single click before a double click can be confirmed.

### 3.2 TFLite gesture inference service

`android/app/src/main/java/com/airmouse/gesture/GestureInferenceService.kt` is the on-device classifier service.

It:

- loads `gesture_model.tflite`
- loads `gesture_labels.json`
- builds a sliding window of sensor samples
- predicts a gesture when the window is full
- reports the result through `onGestureDetected: ((String, Float) -> Unit)?`

Important implementation details from the code:

- window size: **30 samples**
- sampling context: roughly **1.5 seconds at 20 Hz**
- confidence threshold: **0.7f**
- cooldown: **500 ms**

This keeps the classifier from firing too frequently.

### 3.3 Gesture recording service

`android/app/src/main/java/com/airmouse/gesture/GestureRecorderService.kt` records raw motion sessions to CSV.

It writes files under an app-specific folder such as:

- `gesture_dataset/gestures_<timestamp>.csv`

The recorder is useful for:

- collecting user-specific gestures
- creating training data for the TFLite model
- debugging edge cases in gesture classification

### 3.4 WebSocket bridge to the PC

`android/app/src/main/java/com/airmouse/network/WebSocketManager.kt` is the network bridge.

It sends structured JSON messages such as:

- `gesture`
- `proximity`
- `control`

The gesture payload currently looks like this:

```json
{
  "type": "gesture",
  "payload": {
    "gesture": "CLICK",
    "confidence": 0.91
  }
}
```

The control payload is used for pause/resume behavior, and the proximity payload carries near/far state plus distance.

---

## 4. Go side: what is implemented

### 4.1 Server startup

The canonical entrypoint is:

- `pc/airmouse-go/cmd/airmouse-server/main.go`

It initializes:

- logging
- config
- device manager
- mouse controller
- protocol server
- GUI

The startup sequence in code is:

1. create the mouse controller
2. enable standard smoothing
3. enable acceleration
4. optionally enable predictive movement
5. optionally load ONNX AI smoothing
6. start the protocol server
7. launch the UI

### 4.2 Runtime config

`pc/airmouse-go/internal/config/config.go` stores the relevant settings.

The smoothing-related fields are:

- `EnableAISmoothing`
- `AIModelPath`
- `AIBlendFactor`
- `EnablePredictive`
- `PredictiveBlendFactor`
- `PredictiveDt`

Default values from the code:

- `EnableAISmoothing = false`
- `AIModelPath = "models/mouse_smoothing.onnx"`
- `AIBlendFactor = 0.6`
- `EnablePredictive = true`
- `PredictiveBlendFactor = 0.6`
- `PredictiveDt = 0.02`

### 4.3 Kalman-based predictive movement

`pc/airmouse-go/internal/predictive/predictor.go` implements the prediction layer.

It uses a `KalmanFilter2D` to:

- ingest raw movement deltas
- predict the next delta
- blend raw and predicted values

The blend factor controls how much prediction influences the final movement.

This is the current lightweight smoothing path and is the safest default for most setups.

### 4.4 ONNX AI smoothing

`pc/airmouse-go/internal/control/ai_smoothing.go` loads a pre-trained ONNX model with ONNX Runtime.

The smoother:

- stores a fixed-size history buffer of `[x, y, vx, vy]`
- feeds the history into the model as `input_sequence`
- reads back `movement_delta`
- returns the predicted cursor delta

Current code details:

- history length is passed in at construction time
- the smoother expects a full history before it predicts
- AI smoothing is optional and can be disabled at runtime

### 4.5 What the Go server does with Android messages

The protocol server receives Android messages and maps them to desktop actions.

In practice, this means the Android side can send a gesture event and the Go server can decide whether it should:

- click
- double click
- right click
- scroll
- ignore the event if it is low confidence or unsupported

### 4.6 Desktop UI overview

The Go project is not just a headless server. It launches a **Fyne desktop UI** from `internal/ui/app.go` with a window titled **Air Mouse Pro Server**.

The UI is designed as a multi-tab control center with a top menu bar and a fixed-size primary window.

#### Window behavior

- Initial size: **1100 × 720**
- The window is marked as the **master window**
- If `AlwaysOnTop` is enabled in config, the window stays above other windows
- The app applies the selected theme before the main window is shown

#### Main menu

The top menu bar has three menus:

- **File**
  - Start Server
  - Stop Server
  - Quit
- **View**
  - Refresh
- **Help**
  - About

The About dialog shows the app name, version, university lab text, and a short protocol summary.

#### Tabs in the main window

The tab strip is created with `container.NewAppTabs(...)` and includes:

- Dashboard
- Devices
- Network
- Settings
- Analytics
- Logs

Each tab is a live view backed by the current runtime state.

### 4.7 Dashboard tab

`internal/ui/dashboard.go` is the operational summary panel.

It shows:

- server status: Running / Stopped
- click counts
- double-click counts
- right-click counts
- scroll counts
- connected device count
- endpoint string
- uptime timer
- AI smoothing state

#### Dashboard actions

- **Start Server** starts the protocol server and updates endpoint/uptime labels
- **Stop Server** stops the server and resets the status labels

The endpoint label is populated with the local IP and configured ports so the Android client can connect manually if needed.

### 4.8 Devices tab

`internal/ui/devices.go` lists the registered devices from the device manager.

Each row shows:

- device name
- device type
- connection timestamp

The list refreshes automatically every 2 seconds so the operator can watch devices appear and disappear in real time.

### 4.9 Network tab

`internal/ui/network.go` is the pairing and connection tab.

It provides:

- a selectable list of available local IPv4 addresses
- an editable IP address field
- an editable port field
- a **Refresh IPs** button
- a **Copy Endpoint** button
- a **Generate QR** button
- a **Save QR** button
- a QR preview image

#### Network workflow

1. Choose a local IP from the list or type one manually.
2. Set the TCP port.
3. Copy the endpoint or generate the QR code.
4. Scan that QR from Android or paste the endpoint manually.

The tab encodes the endpoint as `airmouse://IP:PORT`.

### 4.10 Settings tab

`internal/ui/settings.go` contains the runtime controls for motion and prediction.

#### Available controls

- **Sensitivity slider**
  - Range: 0.2 to 2.0
  - Updates both config and mouse controller live
- **Theme selector**
  - dark
  - light
  - pure_black
  - high_contrast
  - ocean
  - sunset
  - forest
  - purple
  - cherry
  - neon
  - lavender
  - mint
  - peach
  - sky
- **Always on Top** checkbox
- **Mouse Smoothing (EMA)** checkbox
- **Mouse Acceleration** checkbox
- **AI Smoothing** checkbox
- **Predictive Movement** checkbox
- **Prediction blend** slider
- **Enable Personalization** checkbox
- **Buffer size** slider for personalization samples
- **Retrain interval** slider
- **Auto-swap trained model** checkbox

#### Why this tab matters

This is where the live desktop behavior is tuned without restarting the app. It is the main UI bridge between config and runtime mouse behavior.

### 4.11 Analytics tab

`internal/ui/analytics.go` is the personalization dashboard.

It shows:

- the number of collected samples
- a refresh button
- a force fine-tune action

This tab is the UI surface for the user data collector and future personalization flow.

### 4.12 Logs tab

`internal/ui/logs.go` provides live application logs inside the UI.

It supports:

- a scrolling multi-line log view
- **Clear Logs**
- **Export Logs**

The log tab is fed by a log hook, so runtime messages appear as they are produced by the app.

### 4.13 Theme and visual consistency

`internal/ui/themes.go` applies a named theme from config.

The UI currently supports a custom dark-style variant for `pure_black`, while other names fall back to the standard Fyne light/dark themes or default dark theme.

This keeps the server UI consistent with the selected appearance preference.

---

## 5. Compatibility rules between Android and Go

These are the rules that must stay synchronized across both codebases.

### 5.1 Gesture names must match

Android gesture names sent over the wire should stay aligned with server-side handlers.

Expected names include:

- `CLICK`
- `DOUBLE_CLICK`
- `RIGHT_CLICK`
- `SCROLL_UP`
- `SCROLL_DOWN`

### 5.2 JSON contract must stay stable

Gesture messages should keep the same shape:

```json
{
  "type": "gesture",
  "payload": {
    "gesture": "CLICK",
    "confidence": 0.91
  }
}
```

If you change the Android payload, the Go protocol parser must be updated too.

### 5.3 Double-click timing must stay delayed

The Android detector deliberately delays a single click until the double-click window expires.

That contract should not be broken, otherwise the user gets accidental single clicks followed by a late double click. Nobody likes surprise mouse drama.

### 5.4 Smoothing should remain optional

The Go server currently supports:

- standard smoothing
- predictive smoothing
- AI smoothing

If AI smoothing fails to load, the code should fall back to the standard controller instead of failing startup.

---

## 6. Actual files involved in the sync

### Android files

- `domain/GestureDetector.kt` – runtime gesture rules
- `gesture/GestureInferenceService.kt` – TFLite classifier
- `gesture/GestureRecorderService.kt` – data collection
- `network/WebSocketManager.kt` – outbound messages
- `sensors/SensorService.kt` – sensor feed into gesture detection

### Go server files

- `cmd/airmouse-server/main.go` – entrypoint
- `internal/config/config.go` – config and defaults
- `internal/predictive/predictor.go` – Kalman predictor
- `internal/control/ai_smoothing.go` – ONNX smoother
- `internal/control/mouse.go` – mouse controller integration
- `internal/ui/app.go` – window, tabs, and menu bar
- `internal/ui/dashboard.go` – live server status and counters
- `internal/ui/devices.go` – connected devices list
- `internal/ui/network.go` – endpoint and QR pairing tools
- `internal/ui/settings.go` – runtime controls and theme selection
- `internal/ui/analytics.go` – personalization overview
- `internal/ui/logs.go` – log viewer and export
- `internal/ui/themes.go` – named theme selection and custom dark variant
- `internal/protocol/*` – message routing and protocol handling

---

## 7. Recommended implementation summary

If you want the document to reflect the current project faithfully, describe it this way:

- **Android** handles motion capture, gesture detection, and gesture classification.
- **Go** handles desktop control, smoothing, and optional AI-based prediction.
- **WebSocket / protocol messages** keep both sides synchronized.
- **ONNX AI smoothing** is an optional enhancement, not the core dependency.
- **Kalman prediction** is the production-safe fallback and default smoothing strategy.

---

## 8. Build and test notes

### Android

Build the APK from the Android project using the included Gradle wrapper.

### Go server

From `pc/airmouse-go`, build or run the canonical entrypoint under `cmd/airmouse-server`.

### Verification checklist

- Android gesture names match server handlers
- WebSocket payloads are unchanged
- Go config keys match the UI/settings code
- Predictive smoothing still falls back cleanly when AI loading fails
- The document no longer claims an RNN training flow that is absent from the repo

---

## 9. Bottom line

This project is **already more complete than the original RNN draft suggested** — it just needed the documentation to stop inventing a model pipeline that the code does not use.

The current implementation is best understood as a synchronized Android + Go system with:

- real gesture detection on Android
- optional on-device gesture inference
- Kalman-based desktop prediction
- optional ONNX AI smoothing
- a shared message contract that both sides must keep in sync

That is the version of the project that now matches the code in this workspace.
