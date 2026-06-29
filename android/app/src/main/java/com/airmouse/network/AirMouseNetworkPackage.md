# 📘 Air Mouse Network Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.network` package is the **core networking layer** of the Air Mouse application. It handles all communication between the Android device and the PC server, supporting multiple protocols (WebSocket, TCP, UDP) with automatic reconnection, heartbeat, and reliable message delivery.

```
com.airmouse.network/
├── ConnectionManager.kt              # Core network manager (primary)
├── UdpDiscovery.kt                   # UDP server discovery
├── AutoReconnectManager.kt           # Automatic reconnection logic
├── NetworkQualityMonitor.kt          # Network quality monitoring
├── MessageTypes.kt                   # Protocol message constants
├── AirMouseProtocolMessages.kt       # JSON message builders
├── ConnectionHelper.kt               # Extension functions for sending
├── TcpClient.kt                      # DEPRECATED – TCP client (legacy)
└── WebSocketManager.kt               # DEPRECATED – WebSocket client (legacy)
```

---

## 🌐 1. ConnectionManager (Core)

### Purpose
The **primary network manager** for the Air Mouse application. Handles WebSocket, TCP, and UDP connections with automatic reconnection, heartbeat, and reliable message delivery.

### Key Features

| Feature | Description |
|---------|-------------|
| **Multi-Protocol** | Supports WebSocket, TCP, and UDP |
| **Auto-Reconnection** | Exponential backoff reconnection logic |
| **Heartbeat** | Ping/Pong keep-alive mechanism |
| **Reliable Messages** | ACK-based retransmission for critical packets |
| **Connection Quality** | Real-time ping, jitter, signal strength monitoring |
| **Reactive State** | StateFlow for UI observation |
| **Server Discovery** | Integration with UdpDiscovery |

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CONNECTION MANAGER                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      STATE FLOWS                                 │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │  Status     │  │  Quality    │  │  Server Info            │ │   │
│  │  │  StateFlow  │  │  StateFlow  │  │  StateFlow              │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    TRANSPORT LAYER                               │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │  WebSocket  │  │     TCP     │  │          UDP            │ │   │
│  │  │  (Primary)  │  │  (Fallback) │  │     (Discovery)         │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    PROTOCOL LAYER                                │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │   Message   │  │  Reliable   │  │       Heartbeat         │ │   │
│  │  │   Types     │  │   Delivery  │  │      (Ping/Pong)        │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    RE-CONNECTION LAYER                           │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │     AutoReconnectManager (Exponential Backoff)              │ │   │
│  │  └─────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Message Types

```kotlin
object MessageTypes {
    // Client → Server
    const val TYPE_MOVE = "move"
    const val TYPE_CLICK = "click"
    const val TYPE_DOUBLE_CLICK = "doubleclick"
    const val TYPE_RIGHT_CLICK = "rightclick"
    const val TYPE_SCROLL = "scroll"
    const val TYPE_HELLO = "hello"
    const val TYPE_GESTURE = "gesture"
    const val TYPE_PROXIMITY = "proximity"
    const val TYPE_CONTROL = "control"
    const val TYPE_PING = "ping"
    
    // Server → Client
    const val TYPE_WELCOME = "welcome"
    const val TYPE_PONG = "pong"
    const val TYPE_ACK = "ack"
    const val TYPE_ERROR = "error"
    
    // Control Commands
    const val COMMAND_PAUSE_MOVEMENT = "pause_movement"
    const val COMMAND_RESUME_MOVEMENT = "resume_movement"
    const val COMMAND_LOCK_SCREEN = "lock_screen"
    const val COMMAND_UNLOCK_SCREEN = "unlock_screen"
    const val COMMAND_PLAY_PAUSE = "play_pause"
    const val COMMAND_VOLUME_UP = "volume_up"
    const val COMMAND_VOLUME_DOWN = "volume_down"
    const val COMMAND_MUTE = "mute"
}
```

### Key Methods

#### Connection Management
```kotlin
suspend fun connect(ip: String, port: Int): Boolean
fun disconnect()
fun reconnect()
fun isConnected(): Boolean
fun setProtocol(protocol: ConnectionProtocol)
```

#### Sending Commands
```kotlin
// Mouse commands
fun sendMove(dx: Float, dy: Float): Boolean
fun sendClick(button: String): Boolean
fun sendDoubleClick(): Boolean
fun sendRightClick(): Boolean
fun sendScroll(delta: Int): Boolean

// Gesture commands
fun sendGesture(gesture: String, confidence: Float): Boolean

// Proximity commands
fun sendProximity(isNear: Boolean, distance: Float): Boolean

// Control commands
fun sendControl(command: String): Boolean

// Heartbeat
fun sendPing(): Boolean
fun sendPong(): Boolean
```

#### Observables
```kotlin
val connectionStatus: StateFlow<ConnectionStatus>
val connectionQuality: StateFlow<ConnectionQuality>
val currentIp: StateFlow<String>
val currentPort: StateFlow<Int>
val serverName: StateFlow<String>
val serverVersion: StateFlow<String>
val lastError: StateFlow<String?>
```

---

## 🔍 2. UdpDiscovery

### Purpose
Discovers Air Mouse servers on the local network using UDP broadcast.

### Key Features

| Feature | Description |
|---------|-------------|
| **Broadcast Discovery** | Sends UDP broadcast to find servers |
| **Multi-Format Parsing** | Supports legacy text and JSON responses |
| **Timeout Handling** | Configurable scan duration and timeout |
| **Server Probing** | Probe specific IP addresses |
| **Found Server Tracking** | Maintains list of discovered servers |

### Discovery Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         UDP DISCOVERY FLOW                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. startDiscovery() called                                            │
│         │                                                               │
│         ▼                                                               │
│  2. Send broadcast to multiple addresses:                              │
│     • 255.255.255.255                                                  │
│     • 192.168.1.255                                                    │
│     • 192.168.0.255                                                    │
│     • 10.0.0.255                                                       │
│         │                                                               │
│         ▼                                                               │
│  3. Listen for responses (5 seconds)                                   │
│         │                                                               │
│         ▼                                                               │
│  4. Parse responses:                                                   │
│     • JSON format: {"type":"discovery_response", "port":8080, ...}   │
│     • Legacy format: AIRMOUSE_SERVER:8080:AirMouse Pro:3.0           │
│         │                                                               │
│         ▼                                                               │
│  5. onServerFound callback for each server                             │
│         │                                                               │
│         ▼                                                               │
│  6. onScanComplete callback when done                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Response Formats

#### JSON Format (Recommended)
```json
{
    "type": "discovery_response",
    "port": 8080,
    "ip": "192.168.1.100",
    "name": "Air Mouse Pro",
    "version": "3.0"
}
```

#### Legacy Format
```
AIRMOUSE_SERVER:8080:Air Mouse Pro:3.0
```

### Callbacks

```kotlin
var onServerFound: ((ip: String, port: Int, name: String, version: String) -> Unit)? = null
var onScanComplete: (() -> Unit)? = null
var onError: ((String) -> Unit)? = null
var onScanStart: (() -> Unit)? = null
var onScanProgress: ((progress: Int, found: Int) -> Unit)? = null
```

---

## 🔄 3. AutoReconnectManager

### Purpose
Manages automatic reconnection with exponential backoff when the connection is lost.

### Key Features

| Feature | Description |
|---------|-------------|
| **Exponential Backoff** | Progressive delays between reconnection attempts |
| **Max Attempts** | Configurable maximum attempts (default: 10) |
| **State Monitoring** | Listens to ConnectionManager status |
| **Cancellation** | Manual cancellation support |

### Reconnection Algorithm

```kotlin
private fun calculateDelay(): Long {
    val exponential = BASE_DELAY_MS * (1L shl reconnectAttempts.coerceAtMost(4))
    return exponential.coerceAtMost(MAX_DELAY_MS)
}
```

### Reconnection Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    RECONNECTION FLOW                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Connection Lost                                                       │
│         │                                                               │
│         ▼                                                               │
│  Attempt 1: Wait 3s                                                    │
│         │                                                               │
│         ▼                                                               │
│  Attempt 2: Wait 6s                                                    │
│         │                                                               │
│         ▼                                                               │
│  Attempt 3: Wait 12s                                                   │
│         │                                                               │
│         ▼                                                               │
│  Attempt 4: Wait 24s                                                   │
│         │                                                               │
│         ▼                                                               │
│  Attempt 5: Wait 48s                                                   │
│         │                                                               │
│         ▼                                                               │
│  Max attempts reached → Give up                                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 4. NetworkQualityMonitor

### Purpose
Monitors network quality and signal strength for the current connection.

### Key Features

| Feature | Description |
|---------|-------------|
| **Network Type Detection** | WiFi, Cellular, Ethernet, Bluetooth |
| **Signal Strength Estimation** | Approximate signal strength |
| **Quality Classification** | Excellent, Good, Fair, Poor, None |

### Quality Levels

```kotlin
enum class NetworkQuality {
    EXCELLENT,    // WiFi or Ethernet with strong signal
    GOOD,         // WiFi or Cellular with good signal
    FAIR,         // Cellular with moderate signal
    POOR,         // Weak signal
    NONE          // No connection
}
```

---

## 📋 5. MessageTypes

### Purpose
Centralizes all protocol constants to ensure consistency across the app.

### Categories

| Category | Constants |
|----------|-----------|
| **Protocol** | `PROTOCOL_VERSION = "3.0"` |
| **Ports** | `DEFAULT_TCP_PORT = 8080`, `DEFAULT_WEBSOCKET_PORT = 8081`, `DEFAULT_UDP_PORT = 8082` |
| **Client Messages** | `TYPE_MOVE`, `TYPE_CLICK`, `TYPE_SCROLL`, `TYPE_HELLO`, `TYPE_GESTURE`, `TYPE_PING` |
| **Server Messages** | `TYPE_WELCOME`, `TYPE_PONG`, `TYPE_ACK`, `TYPE_ERROR` |
| **Commands** | `COMMAND_PAUSE_MOVEMENT`, `COMMAND_LOCK_SCREEN`, `COMMAND_PLAY_PAUSE`, `COMMAND_VOLUME_UP` |
| **Gestures** | `GESTURE_THUMBS_UP`, `GESTURE_SWIPE_LEFT`, `GESTURE_CIRCLE_CW` |
| **Priority** | `PRIORITY_HIGH`, `PRIORITY_MEDIUM`, `PRIORITY_LOW` |
| **Error Codes** | `ERROR_INVALID_JSON = 400`, `ERROR_AUTH_FAILED = 403` |

---

## 📋 6. AirMouseProtocolMessages

### Purpose
Builds JSON messages for the Air Mouse protocol.

### Message Builders

```kotlin
object AirMouseProtocolMessages {
    fun move(dx: Float, dy: Float): String
    fun reliableClick(type: String, id: String, button: String): String
    fun reliableScroll(id: String, delta: Int): String
    fun gesture(gesture: String, confidence: Float): String
    fun proximity(deviceId: String, isNear: Boolean, distance: Float): String
    fun control(command: String): String
    fun ping(): String
    fun pong(): String
    fun hello(name: String, version: String, ...): String
}
```

### Example Usage

```kotlin
// Move message
val moveMsg = AirMouseProtocolMessages.move(12.5f, -3.2f)
// {"type":"move","dx":12.5,"dy":-3.2}

// Gesture message
val gestureMsg = AirMouseProtocolMessages.gesture("ThumbsUp", 0.95f)
// {"type":"gesture","payload":{"gesture":"ThumbsUp","confidence":0.95}}
```

---

## 📋 7. ConnectionHelper (Extensions)

### Purpose
Provides extension functions for sending commands via ConnectionManager.

### Sending Extensions

```kotlin
// Mouse commands
fun ConnectionManager.sendMove(dx: Float, dy: Float, id: String? = null): Boolean
fun ConnectionManager.sendClick(button: String = "left", id: String? = null): Boolean
fun ConnectionManager.sendDoubleClick(): Boolean
fun ConnectionManager.sendRightClick(): Boolean
fun ConnectionManager.sendScroll(delta: Int): Boolean

// Control commands
fun ConnectionManager.sendPauseMovement(): Boolean
fun ConnectionManager.sendResumeMovement(): Boolean
fun ConnectionManager.sendLockScreen(): Boolean
fun ConnectionManager.sendUnlockScreen(): Boolean
fun ConnectionManager.sendCalibrate(): Boolean
fun ConnectionManager.sendReset(): Boolean
fun ConnectionManager.sendShowDesktop(): Boolean

// Media commands
fun ConnectionManager.sendPlayPause(): Boolean
fun ConnectionManager.sendNextTrack(): Boolean
fun ConnectionManager.sendPrevTrack(): Boolean
fun ConnectionManager.sendStop(): Boolean
fun ConnectionManager.sendVolumeUp(): Boolean
fun ConnectionManager.sendVolumeDown(): Boolean
fun ConnectionManager.sendMute(): Boolean

// Browser commands
fun ConnectionManager.sendBrowserBack(): Boolean
fun ConnectionManager.sendBrowserForward(): Boolean
fun ConnectionManager.sendBrowserRefresh(): Boolean
fun ConnectionManager.sendBrowserHome(): Boolean

// Key press
fun ConnectionManager.sendKeyPress(keyCode: Int): Boolean
```

---

## 🗑️ 8. Deprecated Classes

### TcpClient (Deprecated)
Legacy TCP client. **Use ConnectionManager instead.**

```kotlin
@Deprecated("Use ConnectionManager instead")
class TcpClient @Inject constructor() {
    // All methods deprecated
}
```

### WebSocketManager (Deprecated)
Legacy WebSocket manager. **Use ConnectionManager instead.**

```kotlin
@Deprecated("Use ConnectionManager instead")
object WebSocketManager {
    // All methods deprecated
}
```

---

## 🔗 Network Layer Integration

### Dependency Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient { ... }
    
    @Provides
    @Singleton
    fun provideConnectionManager(...): ConnectionManager { ... }
    
    @Provides
    @Singleton
    fun provideUdpDiscovery(): UdpDiscovery { ... }
}
```

### Data Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    NETWORK DATA FLOW                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  UI/ViewModel ──sendMove()──▶ ConnectionManager                        │
│                                    │                                    │
│                                    ▼                                    │
│                         sendRawString()                                │
│                                    │                                    │
│                                    ▼                                    │
│                    WebSocket/TCP/UDP Transport                         │
│                                    │                                    │
│                                    ▼                                    │
│                         PC Server                                      │
│                                    │                                    │
│                                    ▼                                    │
│                         Response/ACK                                  │
│                                    │                                    │
│                                    ▼                                    │
│                    handleServerMessage()                              │
│                                    │                                    │
│                                    ▼                                    │
│                    updateConnectionQuality()                          │
│                                    │                                    │
│                                    ▼                                    │
│                    StateFlow update → UI                              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Summary

| Component | Purpose | Status |
|-----------|---------|--------|
| **ConnectionManager** | Core network manager | ✅ Active |
| **UdpDiscovery** | Server discovery | ✅ Active |
| **AutoReconnectManager** | Reconnection logic | ✅ Active |
| **NetworkQualityMonitor** | Quality monitoring | ✅ Active |
| **MessageTypes** | Protocol constants | ✅ Active |
| **AirMouseProtocolMessages** | Message builders | ✅ Active |
| **ConnectionHelper** | Extension functions | ✅ Active |
| **TcpClient** | Legacy TCP client | ⚠️ Deprecated |
| **WebSocketManager** | Legacy WebSocket | ⚠️ Deprecated |

---

**The Network Layer provides a robust, reliable, and feature-rich communication system for the Air Mouse application, supporting multiple protocols with automatic reconnection and quality monitoring.**