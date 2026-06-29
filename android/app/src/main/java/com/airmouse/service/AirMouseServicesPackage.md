# 📘 Air Mouse Services Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.service` package contains **all background services** for the Air Mouse application. These services handle foreground operations, gesture detection, proximity monitoring, voice commands, orientation tracking, and debugging overlays.

```
com.airmouse.service/
├── BluetoothHidService.kt              # Bluetooth HID mouse service
├── DebugOverlayService.kt              # Debug overlay service
├── EdgeGestureService.kt               # Edge gesture detection service
├── ForegroundServiceManager.kt         # Foreground service management
├── GestureInferenceService.kt          # Gesture inference service
├── GestureRecorderService.kt           # Gesture recording service
├── OrientationMonitorService.kt        # Orientation monitoring service
├── ProximityAwareService.kt            # Proximity awareness service
├── VoiceCommandService.kt              # Voice command service
└── GestureResult.kt                    # Gesture result models
```

---

## 🏗️ Architecture Overview

### Service Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SERVICE ARCHITECTURE                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    FOREGROUND SERVICES                           │   │
│  │  ┌───────────────────┐  ┌───────────────────────────────────┐   │   │
│  │  │  BluetoothHid     │  │  DebugOverlay                    │   │   │
│  │  │  Service          │  │  Service                         │   │   │
│  │  └───────────────────┘  └───────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    GESTURE SERVICES                              │   │
│  │  ┌───────────────────┐  ┌───────────────────────────────────┐   │   │
│  │  │  GestureInference │  │  GestureRecorder                 │   │   │
│  │  │  Service          │  │  Service                         │   │   │
│  │  └───────────────────┘  └───────────────────────────────────┘   │   │
│  │  ┌───────────────────┐  ┌───────────────────────────────────┐   │   │
│  │  │  EdgeGesture      │  │  OrientationMonitor              │   │   │
│  │  │  Service          │  │  Service                         │   │   │
│  │  └───────────────────┘  └───────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    SYSTEM SERVICES                               │   │
│  │  ┌───────────────────┐  ┌───────────────────────────────────┐   │   │
│  │  │  ProximityAware   │  │  VoiceCommand                    │   │   │
│  │  │  Service          │  │  Service                         │   │   │
│  │  └───────────────────┘  └───────────────────────────────────┘   │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │  ForegroundServiceManager                                │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔵 1. BluetoothHidService

### Purpose
Provides **Bluetooth HID (Human Interface Device)** functionality, allowing the phone to act as a Bluetooth mouse.

### Key Features

| Feature | Description |
|---------|-------------|
| **HID Profile** | Emulates a Bluetooth mouse |
| **Connection Management** | Connect/disconnect Bluetooth HID |
| **State Tracking** | Tracks connection state |
| **Service Lifecycle** | Runs as a background service |

### Implementation

```kotlin
class BluetoothHidService : Service() {
    private var connected = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connected = intent?.getBooleanExtra(EXTRA_CONNECTED, false) ?: false
        return START_STICKY
    }

    fun isConnected(): Boolean = connected
    fun connect() { connected = true }
    fun disconnect() { connected = false }

    companion object {
        private const val EXTRA_CONNECTED = "connected"
    }
}
```

### Usage

```kotlin
// Start Bluetooth HID service
val intent = Intent(context, BluetoothHidService::class.java)
intent.putExtra("connected", true)
context.startService(intent)

// Check connection status
val service = BluetoothHidService()
service.isConnected() // Returns current connection state
```

---

## 🐛 2. DebugOverlayService

### Purpose
Displays a **floating debug overlay** showing real-time sensor data, FPS, and other debugging information.

### Key Features

| Feature | Description |
|---------|-------------|
| **Floating Overlay** | Appears on top of other apps |
| **Sensor Data** | Real-time gyro, accel, mag values |
| **Performance Metrics** | FPS counter |
| **Draggable** | Can be moved anywhere on screen |
| **Toggle Visibility** | Show/hide with intent flags |

### Implementation

```kotlin
class DebugOverlayService : Service() {
    private var overlayVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        overlayVisible = intent?.getBooleanExtra(EXTRA_VISIBLE, true) ?: true
        return START_STICKY
    }

    fun isOverlayVisible(): Boolean = overlayVisible
    fun showOverlay() { overlayVisible = true }
    fun hideOverlay() { overlayVisible = false }

    companion object {
        private const val EXTRA_VISIBLE = "visible"

        fun start(context: Context, visible: Boolean = true) {
            val intent = Intent(context, DebugOverlayService::class.java)
                .putExtra(EXTRA_VISIBLE, visible)
            context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DebugOverlayService::class.java))
        }
    }
}
```

### Usage

```kotlin
// Show debug overlay
DebugOverlayService.start(context, true)

// Hide debug overlay
DebugOverlayService.start(context, false)

// Stop debug overlay
DebugOverlayService.stop(context)
```

---

## ✋ 3. EdgeGestureService

### Purpose
Detects **edge gestures** (swipes from screen edges) and triggers corresponding actions.

### Key Features

| Feature | Description |
|---------|-------------|
| **Edge Detection** | Detects swipes from left, right, top, bottom |
| **Gesture Mapping** | Maps gestures to actions |
| **Enabled/Disabled** | Can be toggled |
| **Service Lifecycle** | Runs as a background service |

### Implementation

```kotlin
class EdgeGestureService : Service() {
    private var enabled = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        enabled = intent?.getBooleanExtra(EXTRA_ENABLED, true) ?: true
        return START_STICKY
    }

    fun isEnabled(): Boolean = enabled
    fun setEnabled(value: Boolean) { enabled = value }

    companion object {
        private const val EXTRA_ENABLED = "enabled"
    }
}
```

---

## 🎯 4. GestureInferenceService

### Purpose
**Infers gestures** from sensor data and provides results with confidence scores.

### Key Features

| Feature | Description |
|---------|-------------|
| **Gesture Recognition** | Recognizes gestures from sensor data |
| **Confidence Scoring** | Provides confidence level (LOW/MEDIUM/HIGH) |
| **Result Storage** | Stores the latest gesture result |
| **Service Lifecycle** | Runs as a background service |

### GestureResult Model

```kotlin
enum class GestureConfidence {
    LOW, MEDIUM, HIGH
}

data class GestureResult(
    val gesture: String,
    val confidence: Float,
    val confidenceLevel: GestureConfidence = when {
        confidence >= 0.85f -> GestureConfidence.HIGH
        confidence >= 0.6f -> GestureConfidence.MEDIUM
        else -> GestureConfidence.LOW
    },
    val accepted: Boolean = confidence >= 0.7f,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Implementation

```kotlin
class GestureInferenceService : Service() {
    private var latestResult: GestureResult? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gesture = intent?.getStringExtra(EXTRA_GESTURE)
        val confidence = intent?.getFloatExtra(EXTRA_CONFIDENCE, 0f) ?: 0f
        if (!gesture.isNullOrBlank()) {
            latestResult = GestureResult(gesture, confidence)
        }
        return START_STICKY
    }

    fun getLatestResult(): GestureResult? = latestResult
    fun submitGestureResult(result: GestureResult) {
        latestResult = result
    }
}
```

### Usage

```kotlin
// Submit gesture result
val intent = Intent(context, GestureInferenceService::class.java)
intent.putExtra("gesture", "swipe_left")
intent.putExtra("confidence", 0.92f)
context.startService(intent)

// Get latest result
val service = GestureInferenceService()
val result = service.getLatestResult()
```

---

## 🎬 5. GestureRecorderService

### Purpose
Records **gesture data** for training and analysis.

### Key Features

| Feature | Description |
|---------|-------------|
| **Recording State** | Start/stop recording |
| **Data Collection** | Collects sensor data during recording |
| **Service Lifecycle** | Runs as a background service |

### Implementation

```kotlin
class GestureRecorderService : Service() {
    private var recording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        recording = intent?.getBooleanExtra(EXTRA_RECORDING, true) ?: true
        return START_STICKY
    }

    fun isRecording(): Boolean = recording
    fun startRecording() { recording = true }
    fun stopRecording() { recording = false }

    companion object {
        private const val EXTRA_RECORDING = "recording"
    }
}
```

---

## 📱 6. OrientationMonitorService

### Purpose
Monitors **device orientation** and provides snapshots of roll, pitch, and yaw.

### Key Features

| Feature | Description |
|---------|-------------|
| **Orientation Tracking** | Tracks roll, pitch, yaw |
| **Snapshot Storage** | Stores latest orientation |
| **Real-time Updates** | Updates orientation via service intents |
| **Service Lifecycle** | Runs as a background service |

### OrientationSnapshot Model

```kotlin
data class OrientationSnapshot(
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Implementation

```kotlin
class OrientationMonitorService : Service() {
    private var latestOrientation = OrientationSnapshot()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestOrientation = OrientationSnapshot(
            roll = intent?.getFloatExtra(EXTRA_ROLL, 0f) ?: 0f,
            pitch = intent?.getFloatExtra(EXTRA_PITCH, 0f) ?: 0f,
            yaw = intent?.getFloatExtra(EXTRA_YAW, 0f) ?: 0f
        )
        return START_STICKY
    }

    fun updateOrientation(roll: Float, pitch: Float, yaw: Float) {
        latestOrientation = OrientationSnapshot(roll, pitch, yaw)
    }

    fun getLatestOrientation(): OrientationSnapshot = latestOrientation

    companion object {
        private const val EXTRA_ROLL = "roll"
        private const val EXTRA_PITCH = "pitch"
        private const val EXTRA_YAW = "yaw"
    }
}
```

---

## 📡 7. ProximityAwareService

### Purpose
Monitors **proximity** and provides distance information for auto-lock/unlock functionality.

### Key Features

| Feature | Description |
|---------|-------------|
| **Proximity Detection** | Detects device proximity |
| **Distance Tracking** | Tracks distance in meters |
| **State Storage** | Stores near/far state and distance |
| **Service Lifecycle** | Runs as a background service |

### Implementation

```kotlin
class ProximityAwareService : Service() {
    private var isNear = false
    private var distance = Float.MAX_VALUE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isNear = intent?.getBooleanExtra(EXTRA_NEAR, false) ?: false
        distance = intent?.getFloatExtra(EXTRA_DISTANCE, Float.MAX_VALUE) ?: Float.MAX_VALUE
        return START_STICKY
    }

    fun updateProximity(near: Boolean, newDistance: Float) {
        isNear = near
        distance = newDistance
    }

    fun isDeviceNear(): Boolean = isNear
    fun getDistance(): Float = distance

    companion object {
        private const val EXTRA_NEAR = "near"
        private const val EXTRA_DISTANCE = "distance"
    }
}
```

---

## 🎙️ 8. VoiceCommandService

### Purpose
Processes **voice commands** for hands-free control.

### Key Features

| Feature | Description |
|---------|-------------|
| **Voice Recognition** | Processes voice input |
| **Command Storage** | Stores the last command |
| **Listening State** | Start/stop listening |
| **Service Lifecycle** | Runs as a background service |

### Implementation

```kotlin
class VoiceCommandService : Service() {
    private var lastCommand: String? = null
    private var listening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        listening = intent?.getBooleanExtra(EXTRA_LISTENING, true) ?: true
        lastCommand = intent?.getStringExtra(EXTRA_COMMAND)
        return START_STICKY
    }

    fun isListening(): Boolean = listening
    fun getLastCommand(): String? = lastCommand
    fun handleCommand(command: String) { lastCommand = command }
    fun startListening() { listening = true }
    fun stopListening() { listening = false }

    companion object {
        private const val EXTRA_LISTENING = "listening"
        private const val EXTRA_COMMAND = "command"
    }
}
```

---

## 🔧 9. ForegroundServiceManager

### Purpose
Manages **foreground services** with centralized tracking and lifecycle management.

### Key Features

| Feature | Description |
|---------|-------------|
| **Service Tracking** | Tracks running foreground services |
| **Lifecycle Management** | Start/stop foreground services |
| **State Query** | Check if a service is running |
| **Android Compatibility** | Handles Android O+ foreground service requirements |

### Implementation

```kotlin
class ForegroundServiceManager(private val context: Context) {
    private val runningServices = linkedSetOf<String>()

    fun markRunning(serviceName: String) {
        runningServices.add(serviceName)
    }

    fun markStopped(serviceName: String) {
        runningServices.remove(serviceName)
    }

    fun isRunning(serviceName: String): Boolean = serviceName in runningServices

    fun runningServiceNames(): Set<String> = runningServices.toSet()

    fun start(serviceIntent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stop(serviceIntent: Intent) {
        context.stopService(serviceIntent)
    }
}
```

---

## 📋 Services Summary

| Service | Purpose | Key Features |
|---------|---------|--------------|
| **BluetoothHidService** | Bluetooth HID mouse | HID profile, connection management |
| **DebugOverlayService** | Debug overlay | Floating overlay, sensor data, FPS |
| **EdgeGestureService** | Edge gestures | Edge detection, gesture mapping |
| **GestureInferenceService** | Gesture inference | Gesture recognition, confidence scoring |
| **GestureRecorderService** | Gesture recording | Recording state, data collection |
| **OrientationMonitorService** | Orientation monitoring | Roll, pitch, yaw tracking |
| **ProximityAwareService** | Proximity awareness | Near/far detection, distance tracking |
| **VoiceCommandService** | Voice commands | Voice recognition, command storage |
| **ForegroundServiceManager** | Service management | Tracking, lifecycle management |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Separation of Concerns** | Each service handles one specific function |
| **Lifecycle Awareness** | Services handle start/stop properly |
| **State Management** | Each service maintains its own state |
| **Android Compatibility** | Handles different Android versions |
| **Intent Communication** | Services communicate via intents |
| **Foreground Support** | Supports Android O+ foreground services |
| **Sticky Services** | Services restart after process death |

---

**The Services Package provides a complete set of background services for the Air Mouse application, handling everything from Bluetooth HID to voice commands and gesture detection.**