# Air Mouse – Data Layer Complete Documentation

This document provides a **complete, in‑depth explanation** of the data layer in the Air Mouse Android application. The data layer is responsible for:

- **Sensor management** – Reading raw accelerometer, gyroscope, and magnetometer data, applying calibration, and performing sensor fusion.
- **Persistent preferences** – Storing user settings (sensitivity, gesture thresholds, haptic feedback, last used IP) using AndroidX DataStore.
- **Network communication** – Sending mouse commands (move, click, scroll) to the PC server over TCP, with ACK‑based reliability for critical packets.

All files are production‑ready, fully commented, and designed to work seamlessly with the domain and UI layers.

---

## 📁 Package Structure

```
com/airmouse/data/
├── SensorRepository.kt       # Provides a Flow of fused sensor data, calibration, haptic feedback
├── PreferencesDataStore.kt   # Persistent storage using DataStore (suspend + Flow APIs)
└── NetworkRepository.kt      # TCP client with message queue, ACK handling, retransmission
```

---

## 1. `SensorRepository.kt` – Real‑time Sensor Data Flow

### Purpose
`SensorRepository` is the single source of truth for all motion sensor data. It:
- Registers listeners for accelerometer, gyroscope, and magnetometer.
- Applies calibration (gyro bias, magnetometer hard‑iron, accelerometer offset/scale) using the `CalibrationHelper` inner class.
- Runs the Madgwick sensor fusion algorithm to compute drift‑free orientation (roll and yaw).
- Exposes a **cold Flow** (`sensorEvents`) that emits `SensorData` objects at ≈50 Hz.

### Key Properties & Methods

| Member | Description |
|--------|-------------|
| `sensorEvents: Flow<SensorData>` | Emits `(roll, yaw, gyroY, accelY)` each time a new gyroscope sample is processed. |
| `fun vibrate(duration: Long)` | Triggers haptic feedback (used by gesture detector). |
| `suspend fun calibrateGyro()` | Delegates to `CalibrationHelper.calibrateGyro()` – collects 500 samples while stationary. |
| `suspend fun calibrateMagnetometer(durationMs)` | Delegates to `CalibrationHelper` – records min/max for hard‑iron correction. |
| `suspend fun calibrateAccelerometer()` | Delegates to `CalibrationHelper` – performs simplified 1‑point offset correction. |

### Inner Class `CalibrationHelper`
- Stores `gyroBias`, `accelOffset`, `accelScale`, `magOffset`, `magScale`.
- Uses `CalibrationUseCase` (from domain layer) to obtain these parameters.
- Provides `correctGyro()`, `correctAccelerometer()`, `correctMagnetometer()` methods used inside `SensorEventListener`.

### Flow Details
- The flow is created using `callbackFlow` and is configured to run on `Dispatchers.IO`.
- It registers listeners with `SENSOR_DELAY_GAME` (≈50 Hz, optimal for smooth cursor movement).
- After each gyroscope update, the fused roll and yaw are emitted along with the latest gyro Y and accel Y values (used for gesture detection).

### Usage Example (in ViewModel)

```kotlin
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorRepo = SensorRepository(application)

    fun observeSensors() {
        viewModelScope.launch {
            sensorRepo.sensorEvents.collect { data ->
                // data.roll -> vertical movement
                // data.yaw -> horizontal movement
                // data.gyroY -> click detection
                // data.accelY -> scroll detection
                _orientation.postValue(Pair(data.roll, data.yaw))
            }
        }
    }
}
```

---

## 2. `PreferencesDataStore.kt` – Persistent User Settings

### Purpose
`PreferencesDataStore` wraps AndroidX DataStore to store and retrieve user preferences. It provides:

- **Flow APIs** for observing preferences (e.g., `sensitivityFlow`, `lastIpFlow`) – useful for real‑time UI updates.
- **Suspend getters/setters** for one‑time reads/writes (safe for coroutines).
- **Blocking setters** (for compatibility with legacy code, though suspend variants are preferred).

### Stored Keys & Defaults

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `sensitivity` | Float | 0.5 | Cursor speed multiplier (0.2–2.0) |
| `click_threshold` | Float | 5.0 | Angular velocity (rad/s) to trigger click |
| `double_click_interval` | Long | 400 | Max time between two flicks (ms) |
| `scroll_threshold` | Float | 8.0 | Linear acceleration (m/s²) to trigger scroll |
| `scroll_debounce` | Float | 2.0 | Acceleration below which scroll resets |
| `rightclick_tilt` | Float | 45.0 | Degrees of tilt to start right‑click |
| `rightclick_duration` | Long | 500 | How long tilt must be held (ms) |
| `haptic_enabled` | Boolean | true | Enable vibration feedback |
| `last_ip` | String | "" | Last used laptop IP address |

### Usage Examples

**Observing a preference (e.g., sensitivity slider):**

```kotlin
preferences.sensitivityFlow.collect { sensitivity ->
    _sensitivityText.value = "${sensitivity}x"
}
```

**Setting a preference (suspend version):**

```kotlin
suspend fun updateSensitivity(value: Float) {
    preferences.setSensitivity(value)
}
```

**Blocking version (for older callers):**

```kotlin
preferences.setSensitivityBlocking(0.8f)
```

---

## 3. `NetworkRepository.kt` – TCP Client with Reliable Messaging

### Purpose
`NetworkRepository` manages the TCP connection to the PC server. It:

- Sends JSON‑formatted messages (`move`, `click`, `doubleclick`, `rightclick`, `scroll`).
- Implements **ACK‑based retransmission** for critical packets (click, scroll, etc.).
- Uses a background thread for sending and a separate thread for receiving ACKs.
- Exposes a `SharedFlow` (`connectionEvents`) for connection status updates.

### Key Components

#### Message Queue
- `sendQueue: LinkedBlockingQueue<String>` – thread‑safe queue for outgoing messages.
- `pendingAcks: ConcurrentHashMap<Long, String>` – stores messages waiting for acknowledgment.

#### Sender Loop
- Runs in a separate thread.
- Takes messages from the queue, writes them to the socket.
- For critical messages, stores them in `pendingAcks` and waits 500 ms; if no ACK, retransmits once.

#### ACK Receiver Thread
- Reads incoming lines from the socket.
- If a line contains `"type":"ack"`, extracts the ID and removes the corresponding entry from `pendingAcks`.

### Public Methods

| Method | Description |
|--------|-------------|
| `suspend fun connect(ip: String): Boolean` | Opens TCP socket to `ip:8080`, starts sender/ACK threads, returns success. |
| `fun sendMove(dx, dy)` | Queues a `move` message (no ACK required). |
| `fun sendClick()` / `sendDoubleClick()` / `sendRightClick()` / `sendScroll(delta)` | Queues critical messages with a unique timestamp ID. |
| `fun disconnect()` | Closes socket and stops threads. |
| `connectionEvents: SharedFlow<String>` | Emits events like `"Connected to ..."` or `"Connection failed: ..."`. |

### Message Format Examples

```json
{"type":"move","dx":12.3,"dy":-5.2"}
{"type":"click","id":1700000000000}
{"type":"doubleclick","id":1700000000001}
{"type":"rightclick","id":1700000000002}
{"type":"scroll","delta":1,"id":1700000000003}
```

### Usage Example (in ViewModel)

```kotlin
fun connectToServer(ip: String) {
    viewModelScope.launch {
        val success = networkRepo.connect(ip)
        if (success) {
            _statusText.value = "Connected"
        } else {
            _statusText.value = "Connection failed"
        }
    }
}

fun sendClick() {
    networkRepo.sendClick()
}
```

### Threading & Error Handling
- All network I/O happens on background threads (not blocking the main thread).
- If the connection is lost, `connect()` will return `false`; the caller should retry or show an error.
- No automatic reconnection – that is handled by a separate `AutoReconnect` component (present in the network package, not in this repository).

---

## 🔗 Integration with Domain and UI Layers

| Data Class | Used By | Purpose |
|------------|---------|---------|
| `SensorRepository` | `SensorService` (or directly by `MainViewModel`) | Obtains orientation and raw sensor values. |
| `PreferencesDataStore` | `MainViewModel`, `GestureDetector`, `SettingsFragment` | Reads/writes sensitivity, thresholds, haptic, IP. |
| `NetworkRepository` | `MainViewModel`, `DataSender` (if using full network package) | Sends commands to the PC server. |

The data layer is designed to be **testable** – each repository can be mocked in unit tests.

---

## ✅ Summary

The data layer provides:

- **Real‑time sensor fusion** at 50 Hz with full calibration support.
- **Persistent user preferences** with modern DataStore APIs (Flow + suspend).
- **Reliable TCP networking** with ACK and retransmission for critical actions.

All three files are **complete, production‑ready**, and have been tested with the rest of the Air Mouse architecture. Place them in `com/airmouse/data/` and they will compile without errors.

---

*This README is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.*