# Air Mouse Network Communication - Complete Documentation

## Table of Contents
1. [Architecture Overview](#1-architecture-overview)
2. [Core Components](#2-core-components)
3. [Connection Manager](#3-connection-manager)
4. [Message Protocol](#4-message-protocol)
5. [Message Types Reference](#5-message-types-reference)
6. [Connection Flow](#6-connection-flow)
7. [Error Handling & Recovery](#7-error-handling--recovery)
8. [UDP Discovery](#8-udp-discovery)
9. [Connection Quality](#9-connection-quality)
10. [Migration Guide](#10-migration-guide)

---

## 1. Architecture Overview

The Air Mouse network layer is designed for **low-latency real-time communication** between Android devices and a Go server. It provides a unified interface for both WebSocket and TCP protocols with automatic reconnection, quality monitoring, and comprehensive error handling.

### 1.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ANDROID APP                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐   │
│  │  Services   │    │ Activities  │    │     ViewModels      │   │
│  │ (Sensor,    │    │ (Main,      │    │   (Home, Settings,  │   │
│  │  Proximity, │    │  Settings)  │    │    Network, etc.)   │   │
│  │  Voice,     │    └─────────────┘    └─────────────────────┘   │
│  │  Edge,      │           │                     │                │
│  │  etc.)      │           └─────────────────────┘                │
│  └─────────────┘                     │                            │
│         │                            │                            │
│         ▼                            ▼                            │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                   ConnectionManager                         │  │
│  │  - Unified API for WebSocket & TCP                          │  │
│  │  - Auto-reconnection with exponential backoff               │  │
│  │  - Connection quality monitoring                            │  │
│  │  - StateFlow for reactive UI updates                        │  │
│  └─────────────────────────────────────────────────────────────┘  │
│         │                            │                            │
│         ├────────────────────────────┼────────────────────────────┤
│         ▼                            ▼                            ▼
│  ┌─────────────┐            ┌─────────────┐            ┌─────────┐│
│  │ WebSocket   │            │    TCP      │            │   UDP   ││
│  │ (Primary)   │            │  (Fallback) │            │Discovery││
│  └─────────────┘            └─────────────┘            └─────────┘│
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
                    ┌─────────────────────┐
                    │    GO SERVER         │
                    │  (Air Mouse Pro)     │
                    │  Port 8080/8081      │
                    └─────────────────────┘
```

### 1.2 Key Design Principles

| Principle | Description |
|-----------|-------------|
| **Unified API** | Single interface for both WebSocket and TCP |
| **Reactive UI** | StateFlow for automatic UI updates |
| **Auto-Recovery** | Automatic reconnection with exponential backoff |
| **Quality Monitoring** | Real-time ping, jitter, and signal strength |
| **Message Standardization** | Centralized message types and helpers |
| **Clean Architecture** | Separation of concerns with dependency injection |

---

## 2. Core Components

### 2.1 Component Overview

| Component | Purpose | Protocol |
|-----------|---------|----------|
| `ConnectionManager` | Main entry point for all network communication | WebSocket + TCP |
| `UdpDiscovery` | Find servers on local network | UDP |
| `MessageTypes` | Centralized message constants | N/A |
| `ConnectionHelper` | JSON message builders | N/A |
| `TcpClient` | (Deprecated) TCP-only client | TCP |
| `WebSocketManager` | (Deprecated) WebSocket-only client | WebSocket |

### 2.2 Dependency Injection with Hilt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideConnectionManager(
        @ApplicationContext context: Context,
        prefs: PreferencesManager
    ): ConnectionManager {
        return ConnectionManager(context, prefs)
    }

    @Provides
    @Singleton
    fun provideUdpDiscovery(): UdpDiscovery {
        return UdpDiscovery()
    }
}
```

---

## 3. Connection Manager

### 3.1 Overview

`ConnectionManager` is the **single entry point** for all network communication in the Air Mouse Android app.

```kotlin
@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
)
```

### 3.2 Connection Status States

```kotlin
enum class ConnectionStatus {
    DISCONNECTED,   // Not connected
    CONNECTING,     // Attempting to connect
    CONNECTED,      // Successfully connected
    RECONNECTING,   // Reconnecting after disconnect
    ERROR           // Connection error
}
```

### 3.3 Connection Quality

```kotlin
data class ConnectionQuality(
    val rssi: Int = 0,              // Signal strength (dBm)
    val ping: Int = 0,              // Round-trip latency (ms)
    val jitter: Int = 0,            // Latency variance (ms)
    val packetLoss: Float = 0f,     // Packet loss percentage
    val signalStrength: SignalStrength = SignalStrength.UNKNOWN
) {
    enum class SignalStrength {
        EXCELLENT,  // ping < 30ms
        GOOD,       // ping < 60ms
        FAIR,       // ping < 100ms
        POOR,       // ping < 200ms
        VERY_POOR,  // ping > 200ms
        UNKNOWN
    }
    
    fun level(): Int  // Returns 0-100
    fun isHealthy(): Boolean  // ping < 200ms and packetLoss < 10%
}
```

### 3.4 State Flows (Reactive UI)

```kotlin
// Observables for UI
val connectionStatus: StateFlow<ConnectionStatus>
val currentIp: StateFlow<String>
val currentPort: StateFlow<Int>
val connectionQuality: StateFlow<ConnectionQuality>
val serverName: StateFlow<String>
val serverVersion: StateFlow<String>
val lastError: StateFlow<String?>
```

### 3.5 Key Methods

#### Connection Management
```kotlin
suspend fun connect(ip: String, port: Int): Boolean
suspend fun connectWebSocket(ip: String, port: Int): Boolean
suspend fun connectTcp(ip: String, port: Int): Boolean
fun disconnect()
fun reconnect()
fun isConnected(): Boolean
```

#### Sending Commands
```kotlin
// Send raw message
fun send(message: String): Boolean
fun sendBinary(data: ByteArray): Boolean

// Mouse commands
fun sendMove(dx: Float, dy: Float): Boolean
fun sendClick(button: String = "left"): Boolean
fun sendDoubleClick(): Boolean
fun sendRightClick(): Boolean
fun sendScroll(delta: Int): Boolean

// Gesture commands
fun sendGesture(gesture: String, confidence: Float): Boolean

// Proximity commands
fun sendProximity(isNear: Boolean, distance: Float): Boolean

// Control commands
fun sendControl(command: String): Boolean
fun sendPauseMovement(): Boolean
fun sendResumeMovement(): Boolean
fun sendLockScreen(): Boolean
fun sendUnlockScreen(): Boolean
fun sendCalibrate(): Boolean
fun sendReset(): Boolean

// System commands
fun sendKeyPress(keyCode: Int): Boolean
fun sendWindowCommand(action: String): Boolean

// Media commands
fun sendPlayPause(): Boolean
fun sendNextTrack(): Boolean
fun sendPrevTrack(): Boolean
fun sendStop(): Boolean
fun sendVolumeUp(): Boolean
fun sendVolumeDown(): Boolean
fun sendMute(): Boolean

// Browser commands
fun sendBrowserBack(): Boolean
fun sendBrowserForward(): Boolean
fun sendBrowserRefresh(): Boolean
fun sendBrowserHome(): Boolean

// Heartbeat
fun sendPing(): Boolean
fun sendPong(): Boolean

// Identification
fun sendHello(name: String = Build.MODEL, version: String = "3.0"): Boolean
```

### 3.6 Callbacks

```kotlin
var onMessage: ((String) -> Unit)? = null
var onBinaryMessage: ((ByteArray) -> Unit)? = null
var onDisconnected: (() -> Unit)? = null
var onConnected: (() -> Unit)? = null
var onError: ((String) -> Unit)? = null
var onQualityChanged: ((ConnectionQuality) -> Unit)? = null
var onStatusChanged: ((ConnectionStatus) -> Unit)? = null
```

### 3.7 Usage Examples

#### Basic Connection
```kotlin
@Inject lateinit var connectionManager: ConnectionManager

// Connect
lifecycleScope.launch {
    connectionManager.connect("192.168.1.100", 8080)
}

// Observe connection status
lifecycleScope.launch {
    connectionManager.connectionStatus.collect { status ->
        when (status) {
            ConnectionManager.ConnectionStatus.CONNECTED -> showConnected()
            ConnectionManager.ConnectionStatus.CONNECTING -> showConnecting()
            ConnectionManager.ConnectionStatus.DISCONNECTED -> showDisconnected()
            ConnectionManager.ConnectionStatus.ERROR -> showError()
            else -> {}
        }
    }
}

// Observe connection quality
lifecycleScope.launch {
    connectionManager.connectionQuality.collect { quality ->
        updateSignalIcon(quality.level())
        showLatency("${quality.ping} ms")
    }
}
```

#### Sending Commands
```kotlin
// Mouse movement
connectionManager.sendMove(12.5f, -3.2f)

// Click
connectionManager.sendClick("left")

// Double click
connectionManager.sendDoubleClick()

// Scroll
connectionManager.sendScroll(3)

// Gesture
connectionManager.sendGesture("ThumbsUp", 0.95f)

// Control command
connectionManager.sendLockScreen()
connectionManager.sendPlayPause()
connectionManager.sendVolumeUp()
```

#### Hello Message
```kotlin
// Send device identification
connectionManager.sendHello(
    name = Build.MODEL,
    version = "3.0"
)
```

---

## 4. Message Protocol

### 4.1 Protocol Specification

| Property | Value |
|----------|-------|
| **Transport** | WebSocket (primary) / TCP (fallback) |
| **Message Format** | JSON (UTF-8) |
| **Delimiter** | Newline (`\n`) |
| **Default Ports** | TCP: 8080, WebSocket: 8081, UDP: 8082 |

### 4.2 Message Structure

#### Nested Format (Recommended)
```json
{
    "type": "message_type",
    "payload": {
        "field1": "value1",
        "field2": "value2"
    },
    "id": "optional_request_id"
}
```

#### Flat Format (Alternative)
```json
{
    "type": "message_type",
    "field1": "value1",
    "field2": "value2"
}
```

### 4.3 Message Examples

#### Move Message
```json
{
    "type": "move",
    "dx": 12.5,
    "dy": -3.2
}
```

#### Click Message
```json
{
    "type": "click",
    "button": "left"
}
```

#### Gesture Message
```json
{
    "type": "gesture",
    "payload": {
        "gesture": "ThumbsUp",
        "confidence": 0.95
    }
}
```

#### Proximity Message
```json
{
    "type": "proximity",
    "payload": {
        "device_id": "abc123",
        "is_near": true,
        "distance": 1.23
    }
}
```

#### Control Message
```json
{
    "type": "control",
    "payload": {
        "command": "lock_screen"
    }
}
```

#### Hello Message
```json
{
    "type": "hello",
    "payload": {
        "name": "Pixel 8 Pro",
        "version": "3.0",
        "device": "Google Pixel 8",
        "android_version": "14"
    }
}
```

---

## 5. Message Types Reference

### 5.1 Client → Server Messages

| Type | Constant | Description |
|------|----------|-------------|
| `move` | `TYPE_MOVE` | Mouse movement delta |
| `click` | `TYPE_CLICK` | Mouse click |
| `doubleclick` | `TYPE_DOUBLE_CLICK` | Double click |
| `rightclick` | `TYPE_RIGHT_CLICK` | Right click |
| `scroll` | `TYPE_SCROLL` | Scroll wheel |
| `hello` | `TYPE_HELLO` | Device identification |
| `gesture` | `TYPE_GESTURE` | Recognised gesture |
| `proximity` | `TYPE_PROXIMITY` | Proximity update |
| `control` | `TYPE_CONTROL` | Control command |
| `ping` | `TYPE_PING` | Keep-alive heartbeat |
| `orientation` | `TYPE_ORIENTATION` | Orientation data |
| `battery` | `TYPE_BATTERY` | Battery status |
| `status` | `TYPE_STATUS` | Device status |
| `log` | `TYPE_LOG` | Log message |
| `custom` | `TYPE_CUSTOM` | Custom data |

### 5.2 Server → Client Messages

| Type | Constant | Description |
|------|----------|-------------|
| `welcome` | `TYPE_WELCOME` | Welcome after hello |
| `pong` | `TYPE_PONG` | Response to ping |
| `ack` | `TYPE_ACK` | Command acknowledgment |
| `error` | `TYPE_ERROR` | Error message |
| `stats` | `TYPE_STATS` | Server statistics |
| `notification` | `TYPE_NOTIFICATION` | Server notification |
| `config` | `TYPE_CONFIG` | Configuration update |

### 5.3 Control Commands

| Command | Constant | Description |
|---------|----------|-------------|
| `pause_movement` | `COMMAND_PAUSE_MOVEMENT` | Pause cursor |
| `resume_movement` | `COMMAND_RESUME_MOVEMENT` | Resume cursor |
| `reset` | `COMMAND_RESET` | Reset state |
| `calibrate` | `COMMAND_CALIBRATE` | Calibrate sensors |
| `lock_screen` | `COMMAND_LOCK_SCREEN` | Lock screen |
| `unlock_screen` | `COMMAND_UNLOCK_SCREEN` | Unlock screen |
| `show_desktop` | `COMMAND_SHOW_DESKTOP` | Show desktop |
| `task_view` | `COMMAND_TASK_VIEW` | Task view |
| `switch_window` | `COMMAND_SWITCH_WINDOW` | Switch windows |
| `start_recording` | `COMMAND_START_RECORDING` | Start recording |
| `stop_recording` | `COMMAND_STOP_RECORDING` | Stop recording |
| `screenshot` | `COMMAND_SCREENSHOT` | Take screenshot |
| `sleep` | `COMMAND_SLEEP` | Sleep device |
| `restart` | `COMMAND_RESTART` | Restart device |
| `shutdown` | `COMMAND_SHUTDOWN` | Shutdown device |

### 5.4 Media Commands

| Command | Constant | Description |
|---------|----------|-------------|
| `play_pause` | `COMMAND_PLAY_PAUSE` | Play/Pause |
| `next_track` | `COMMAND_NEXT_TRACK` | Next track |
| `prev_track` | `COMMAND_PREV_TRACK` | Previous track |
| `stop` | `COMMAND_STOP` | Stop media |
| `volume_up` | `COMMAND_VOLUME_UP` | Volume up |
| `volume_down` | `COMMAND_VOLUME_DOWN` | Volume down |
| `mute` | `COMMAND_MUTE` | Mute audio |
| `seek_forward` | `COMMAND_SEEK_FORWARD` | Seek forward |
| `seek_backward` | `COMMAND_SEEK_BACKWARD` | Seek backward |
| `repeat` | `COMMAND_REPEAT` | Repeat toggle |
| `shuffle` | `COMMAND_SHUFFLE` | Shuffle toggle |

### 5.5 Gesture Types

| Gesture | Constant | Default Action |
|---------|----------|----------------|
| `ThumbsUp` | `GESTURE_THUMBS_UP` | Play/Pause |
| `ThumbsDown` | `GESTURE_THUMBS_DOWN` | Stop |
| `SwipeLeft` | `GESTURE_SWIPE_LEFT` | Previous Track |
| `SwipeRight` | `GESTURE_SWIPE_RIGHT` | Next Track |
| `SwipeUp` | `GESTURE_SWIPE_UP` | Volume Up |
| `SwipeDown` | `GESTURE_SWIPE_DOWN` | Volume Down |
| `CircleCW` | `GESTURE_CIRCLE_CW` | Volume Up |
| `CircleCCW` | `GESTURE_CIRCLE_CCW` | Volume Down |
| `PinchIn` | `GESTURE_PINCH_IN` | Zoom Out |
| `PinchOut` | `GESTURE_PINCH_OUT` | Zoom In |
| `DoubleTap` | `GESTURE_DOUBLE_TAP` | Play/Pause |
| `LongPress` | `GESTURE_LONG_PRESS` | Right Click |
| `Shake` | `GESTURE_SHAKE` | Undo |
| `Peace` | `GESTURE_PEACE` | Lock Screen |
| `Fist` | `GESTURE_FIST` | Mute |
| `Wave` | `GESTURE_WAVE` | Hello |
| `Ok` | `GESTURE_OK` | Confirm |
| `Point` | `GESTURE_POINT` | Select |

### 5.6 Gesture Action Mapping

```kotlin
object GestureActionMap {
    val defaultActions: Map<String, String> = mapOf(
        MessageTypes.GESTURE_THUMBS_UP to MessageTypes.COMMAND_PLAY_PAUSE,
        MessageTypes.GESTURE_THUMBS_DOWN to MessageTypes.COMMAND_STOP,
        MessageTypes.GESTURE_SWIPE_LEFT to MessageTypes.COMMAND_PREV_TRACK,
        // ... etc
    )
    
    fun getActionForGesture(gesture: String): String
    fun getConfidenceThreshold(gesture: String): Float
}
```

---

## 6. Connection Flow

### 6.1 Connection Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CONNECTION FLOW                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────┐                                               │
│  │  1. User Opens  │                                               │
│  │    App          │                                               │
│  └────────┬────────┘                                               │
│           │                                                        │
│           ▼                                                        │
│  ┌─────────────────┐                                               │
│  │  2. Check for   │                                               │
│  │   Last IP       │                                               │
│  └────────┬────────┘                                               │
│           │                                                        │
│           ▼                                                        │
│  ┌─────────────────┐                                               │
│  │  3. Connect     │                                               │
│  │   WebSocket     │                                               │
│  └────────┬────────┘                                               │
│           │                                                        │
│           ▼                                                        │
│  ┌─────────────────┐     ┌─────────────────┐                       │
│  │  4. Connected?  │────▶│  5. Send Hello  │                       │
│  └────────┬────────┘     └────────┬────────┘                       │
│           │                       │                                │
│           ▼                       ▼                                │
│  ┌─────────────────┐     ┌─────────────────┐                       │
│  │  6. Fallback     │     │  7. Receive     │                       │
│  │   TCP           │     │   Welcome       │                       │
│  └────────┬────────┘     └────────┬────────┘                       │
│           │                       │                                │
│           ▼                       ▼                                │
│  ┌─────────────────┐     ┌─────────────────┐                       │
│  │  8. Start        │     │  9. Normal      │                       │
│  │   Heartbeat      │     │   Operation     │                       │
│  └─────────────────┘     └─────────────────┘                       │
│                                                                     │
│  ══════════════════════════════════════════════════════════════════ │
│                     RECONNECTION FLOW                               │
│  ══════════════════════════════════════════════════════════════════ │
│                                                                     │
│  ┌─────────────────┐                                               │
│  │  10. Connection  │                                               │
│  │    Lost          │                                               │
│  └────────┬────────┘                                               │
│           │                                                        │
│           ▼                                                        │
│  ┌─────────────────┐                                               │
│  │  11. Exponential │                                               │
│  │    Backoff       │                                               │
│  └────────┬────────┘                                               │
│           │                                                        │
│           ▼                                                        │
│  ┌─────────────────┐                                               │
│  │  12. Reconnect   │                                               │
│  │    Attempt       │                                               │
│  └────────┬────────┘                                               │
│           │                                                        │
│           ▼                                                        │
│  ┌─────────────────┐                                               │
│  │  13. Max         │                                               │
│  │    Attempts?     │                                               │
│  └────────┬────────┘                                               │
│      Yes  │  No                                                    │
│           ▼                                                        │
│  ┌─────────────────┐     ┌─────────────────┐                       │
│  │  14. ERROR       │     │  15. Back to    │                       │
│  │   State          │     │   Step 3        │                       │
│  └─────────────────┘     └─────────────────┘                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 Connection States

```
DISCONNECTED  ──connect()──▶  CONNECTING  ──success──▶  CONNECTED
      ▲                           │      │                     │
      │                           │      │                     │
      │                           │      ▼                     │
      │                           └──failure──▶  ERROR         │
      │                                                        │
      │                     ┌──────────────────────────────────┘
      │                     │
      └─────────────────────┼──────────────────────────────────┘
                            │
                   RECONNECTING  ◀──disconnect()──
```

### 6.3 Reconnection Logic

```kotlin
private fun scheduleReconnect() {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        _connectionStatus.value = ConnectionStatus.ERROR
        return
    }
    
    val delay = RECONNECT_DELAY_MS * (reconnectAttempts + 1)
    reconnectAttempts++
    delay(delay)
    connect()
}
```

### 6.4 Heartbeat Mechanism

```kotlin
private fun startHeartbeat() {
    heartbeatJob = scope.launch {
        while (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            delay(HEARTBEAT_INTERVAL_MS)
            if (!sendPing()) {
                consecutiveFailures++
                if (consecutiveFailures >= MAX_HEARTBEAT_FAILURES) {
                    handleDisconnect()
                }
            } else {
                consecutiveFailures = 0
            }
        }
    }
}
```

---

## 7. Error Handling & Recovery

### 7.1 Error States

| Error | Cause | Recovery |
|-------|-------|----------|
| `ERROR_INVALID_JSON` | Malformed JSON | Validate message format |
| `ERROR_MISSING_FIELD` | Required field missing | Add missing field |
| `ERROR_INVALID_TYPE` | Unknown message type | Use correct type |
| `ERROR_AUTH_FAILED` | Invalid token | Re-pair device |
| `ERROR_RATE_LIMIT` | Too many messages | Reduce message rate |
| `ERROR_INTERNAL` | Server error | Retry with backoff |
| `ERROR_SERVICE_UNAVAILABLE` | Server down | Reconnect |

### 7.2 Error Handling Strategy

```kotlin
connectionManager.onError = { error ->
    when {
        error.contains("auth") -> {
            // Token expired - re-pair
            showPairingDialog()
        }
        error.contains("rate") -> {
            // Rate limited - reduce frequency
            reduceMoveRate()
        }
        error.contains("internal") -> {
            // Server error - retry with backoff
            scheduleReconnectWithBackoff()
        }
        else -> {
            // Generic error - show notification
            showToast("Connection error: $error")
        }
    }
}
```

### 7.3 Error Codes

| Code | Name | Description |
|------|------|-------------|
| 400 | `ERROR_INVALID_JSON` | Malformed JSON |
| 401 | `ERROR_MISSING_FIELD` | Missing required field |
| 402 | `ERROR_INVALID_TYPE` | Invalid message type |
| 403 | `ERROR_AUTH_FAILED` | Authentication failed |
| 404 | `ERROR_PERMISSION_DENIED` | Permission denied |
| 429 | `ERROR_RATE_LIMIT` | Rate limit exceeded |
| 500 | `ERROR_INTERNAL` | Internal server error |
| 503 | `ERROR_SERVICE_UNAVAILABLE` | Service unavailable |

---

## 8. UDP Discovery

### 8.1 Overview

`UdpDiscovery` finds Air Mouse servers on the local network using UDP broadcast.

### 8.2 Discovery Flow

```
┌─────────────┐                    ┌─────────────┐
│   Android   │                    │   Server    │
│    Client   │                    │             │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │ 1. UDP Broadcast                 │
       │ "AIRMOUSE_DISCOVERY"             │
       │─────────────────────────────────>│
       │                                  │
       │ 2. Server Responds               │
       │ "AIRMOUSE_SERVER:8080:AirMouse"  │
       │<─────────────────────────────────│
       │                                  │
       │ 3. Parse Response                │
       │    Extract IP, Port, Name        │
       │                                  │
       │ 4. Connect to Server             │
       │─────────────────────────────────>│
       │                                  │
```

### 8.3 Response Formats

#### Legacy Format
```
AIRMOUSE_SERVER:8080:AirMouse Pro:3.0
```

#### JSON Format
```json
{
    "type": "discovery_response",
    "port": 8080,
    "ip": "192.168.1.100",
    "name": "Air Mouse Pro",
    "version": "3.0"
}
```

### 8.4 Usage

```kotlin
@Inject lateinit var udpDiscovery: UdpDiscovery

// Set up callbacks
udpDiscovery.onServerFound = { ip, port, name, version ->
    Log.i("Discovery", "Found $name at $ip:$port (v$version)")
    // Connect to server
    lifecycleScope.launch {
        connectionManager.connect(ip, port)
    }
}

udpDiscovery.onScanComplete = {
    Log.i("Discovery", "Scan complete")
}

udpDiscovery.onScanProgress = { progress, found ->
    // Update UI
    updateProgress(progress, found)
}

// Start discovery
udpDiscovery.startDiscovery()

// Or quick scan
udpDiscovery.quickScan()

// Probe specific server
lifecycleScope.launch {
    val found = udpDiscovery.probeServer("192.168.1.100")
}
```

---

## 9. Connection Quality

### 9.1 Quality Metrics

| Metric | Description | Good Range |
|--------|-------------|------------|
| **Ping** | Round-trip latency | < 30 ms |
| **Jitter** | Latency variance | < 10 ms |
| **Packet Loss** | Lost messages | < 1% |
| **RSSI** | Signal strength | > -60 dBm |

### 9.2 Quality Categories

| Category | Ping | RSSI | Color |
|----------|------|------|-------|
| **EXCELLENT** | < 30 ms | > -50 dBm | 🟢 Green |
| **GOOD** | 30-60 ms | -50 to -60 dBm | 🟢 Green |
| **FAIR** | 60-100 ms | -60 to -70 dBm | 🟡 Yellow |
| **POOR** | 100-200 ms | -70 to -80 dBm | 🟠 Orange |
| **VERY_POOR** | > 200 ms | < -80 dBm | 🔴 Red |

### 9.3 Quality Monitoring

```kotlin
// Observe quality changes
lifecycleScope.launch {
    connectionManager.connectionQuality.collect { quality ->
        when {
            quality.isHealthy() -> showGoodConnection()
            quality.ping > 200 -> showPoorConnection()
            else -> showFairConnection()
        }
        
        // Update UI
        updateSignalIcon(quality.level())
        showLatency("${quality.ping} ms")
        showSignalStrength("${quality.rssi} dBm")
    }
}
```

### 9.4 Quality Factors

| Factor | Effect |
|--------|--------|
| Network congestion | Higher ping and jitter |
| Distance from router | Lower RSSI |
| Interference | Packet loss |
| Server load | Higher ping |

---

## 10. Migration Guide

### 10.1 From WebSocketManager to ConnectionManager

#### Before (WebSocketManager)
```kotlin
WebSocketManager.connect("192.168.1.100", 8081)
WebSocketManager.onMessage = { message ->
    // handle message
}
WebSocketManager.sendMove(10f, 20f)
WebSocketManager.sendClick("left")
WebSocketManager.disconnect()
```

#### After (ConnectionManager)
```kotlin
@Inject lateinit var connectionManager: ConnectionManager

// Connect
lifecycleScope.launch {
    connectionManager.connect("192.168.1.100", 8081)
}

// Observe status
lifecycleScope.launch {
    connectionManager.connectionStatus.collect { status ->
        when (status) {
            ConnectionManager.ConnectionStatus.CONNECTED -> showConnected()
            else -> showDisconnected()
        }
    }
}

// Send messages
connectionManager.sendMove(10f, 20f)
connectionManager.sendClick("left")

// Disconnect
connectionManager.disconnect()
```

### 10.2 From TcpClient to ConnectionManager

#### Before (TcpClient)
```kotlin
@Inject lateinit var tcpClient: TcpClient

tcpClient.connect("192.168.1.100", 8080)
tcpClient.onMessage = { message ->
    // handle message
}
tcpClient.sendMove(10, 20)
tcpClient.disconnect()
```

#### After (ConnectionManager)
```kotlin
@Inject lateinit var connectionManager: ConnectionManager

lifecycleScope.launch {
    connectionManager.connect("192.168.1.100", 8080)
}

connectionManager.sendMove(10f, 20f)
connectionManager.disconnect()
```

### 10.3 Migration Checklist

- [ ] Replace `WebSocketManager` imports with `ConnectionManager`
- [ ] Add `@Inject lateinit var connectionManager: ConnectionManager`
- [ ] Replace `WebSocketManager.connect()` with `connectionManager.connect()`
- [ ] Replace `WebSocketManager.send*()` with `connectionManager.send*()`
- [ ] Replace callbacks with StateFlow observation
- [ ] Remove `WebSocketManager` and `TcpClient` providers from DI
- [ ] Update `ConnectionRepositoryImpl` to use `ConnectionManager`

### 10.4 Deprecated Classes

| Class | Replacement | Status |
|-------|-------------|--------|
| `WebSocketManager` | `ConnectionManager` | Deprecated |
| `TcpClient` | `ConnectionManager` | Deprecated |

---

## 11. Testing

### 11.1 Unit Testing

```kotlin
@Test
fun testConnectionManagerConnect() = runTest {
    val manager = ConnectionManager(context, mockPrefs)
    val result = manager.connect("192.168.1.100", 8080)
    assertTrue(result)
    assertEquals(ConnectionStatus.CONNECTED, manager.connectionStatus.value)
}
```

### 11.2 Integration Testing

```kotlin
@Test
fun testFullConnectionFlow() = runTest {
    // Start mock server
    startMockServer()
    
    // Connect
    val result = connectionManager.connect("localhost", 8080)
    assertTrue(result)
    
    // Send message
    val sent = connectionManager.sendMove(10f, 20f)
    assertTrue(sent)
    
    // Verify message received
    val received = mockServer.getLastMessage()
    assertEquals("move", received.type)
    assertEquals(10f, received.dx)
    assertEquals(20f, received.dy)
}
```

---

## 12. Performance Considerations

### 12.1 Optimization Tips

| Tip | Description |
|-----|-------------|
| **Batch Messages** | Use `sendBatch()` for multiple commands |
| **Reduce Frequency** | Don't send move messages faster than 60Hz |
| **Compression** | Use binary messages for large data |
| **Keep-Alive** | Heartbeat interval 30 seconds |
| **Connection Pooling** | Reuse OkHttpClient instance |

### 12.2 Performance Metrics

| Metric | Target |
|--------|--------|
| Connection Time | < 2 seconds |
| Message Latency | < 20 ms |
| Message Throughput | > 1000 msg/sec |
| Heartbeat Interval | 30 seconds |
| Reconnection Delay | 3-30 seconds |

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-15  
**Protocol Version:** 3.0  
**Maintainer:** Air Mouse Team