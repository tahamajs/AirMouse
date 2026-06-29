# 📘 Air Mouse Remote Data Sources – Complete Documentation

## 📁 Package Overview

The `com.airmouse.data.datasource.remote` package contains **remote data source implementations** that handle **network communication** with the PC server, Bluetooth devices, USB connections, and WebSocket endpoints. These data sources abstract away the underlying communication protocols and provide a clean, testable interface for the repository layer.

```
com.airmouse.data.datasource.remote/
├── IBluetoothDataSource.kt          # Bluetooth communication interface
├── IConnectionDataSource.kt         # Network connection interface
├── IUsbDataSource.kt                # USB communication interface
├── IWebSocketDataSource.kt          # WebSocket communication interface
├── BluetoothDataSourceImpl.kt       # Bluetooth implementation
├── ConnectionDataSourceImpl.kt      # Connection implementation (primary)
├── UsbDataSourceImpl.kt             # USB implementation
└── WebSocketDataSourceImpl.kt       # WebSocket implementation
```

---

## 1. IConnectionDataSource (Interface)

### Purpose
Defines the contract for **all network communication** with the Air Mouse server. This is the **primary** remote data source used by the repository layer.

### Interface Definition

```kotlin
interface IConnectionDataSource {
    
    // ============================================================
    // Connection Management
    // ============================================================
    
    /** Establish a connection to the server. */
    suspend fun connect(ip: String, port: Int, useSSL: Boolean = false): Boolean
    
    /** Disconnect from the server. */
    suspend fun disconnect()
    
    /** Attempt to reconnect. */
    suspend fun reconnect(): Boolean
    
    /** Check if currently connected. */
    suspend fun isConnected(): Boolean
    
    // ============================================================
    // Sending Messages
    // ============================================================
    
    /** Send a raw text message. */
    suspend fun sendMessage(message: String): Boolean
    
    /** Send a binary message. */
    suspend fun sendMessage(message: ByteArray): Boolean
    
    /** Send mouse movement. */
    suspend fun sendMove(dx: Float, dy: Float): Boolean
    
    /** Send a click command. */
    suspend fun sendClick(button: String): Boolean
    
    /** Send a double click command. */
    suspend fun sendDoubleClick(): Boolean
    
    /** Send a right click command. */
    suspend fun sendRightClick(): Boolean
    
    /** Send a scroll command. */
    suspend fun sendScroll(delta: Int): Boolean
    
    /** Send a gesture command. */
    suspend fun sendGesture(gesture: String, confidence: Float): Boolean
    
    /** Send proximity update. */
    suspend fun sendProximity(isNear: Boolean, distance: Float): Boolean
    
    /** Send a control command. */
    suspend fun sendControl(command: String): Boolean
    
    /** Send hello identification. */
    suspend fun sendHello(name: String, version: String): Boolean
    
    /** Send ping keep-alive. */
    suspend fun sendPing(): Boolean
    
    /** Send pong response. */
    suspend fun sendPong(): Boolean
    
    // ============================================================
    // Server Discovery (UDP)
    // ============================================================
    
    /** Get list of discovered servers. */
    suspend fun discoverServers(): List<DiscoveredServer>
    
    /** Start server discovery with callback. */
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit)
    
    /** Stop server discovery. */
    suspend fun stopDiscovery()
    
    // ============================================================
    // Connection Quality
    // ============================================================
    
    /** Get current connection quality. */
    suspend fun getConnectionQuality(): ConnectionQuality
    
    /** Observe connection quality changes. */
    fun observeConnectionQuality(): Flow<ConnectionQuality>
    
    // ============================================================
    // Callback Listeners
    // ============================================================
    
    /** Set a listener for incoming text messages. */
    fun setOnMessageListener(listener: (String) -> Unit)
    
    /** Set a listener for incoming binary messages. */
    fun setOnBinaryMessageListener(listener: (ByteArray) -> Unit)
    
    /** Set a listener for disconnection events. */
    fun setOnDisconnectedListener(listener: () -> Unit)
    
    /** Set a listener for connection events. */
    fun setOnConnectedListener(listener: () -> Unit)
}
```

---

## 2. ConnectionDataSourceImpl (Implementation)

### Purpose
The **primary remote data source** for the Air Mouse app. It delegates all network operations to `ConnectionManager` and `UdpDiscovery`, providing a clean, reactive interface with `StateFlow` for connection quality.

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    IConnectionDataSource                        │
│                          (Interface)                            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ConnectionDataSourceImpl                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  connectionManager: ConnectionManager  │  udpDiscovery  │   │
│  │  (WebSocket/TCP/UDP client)            │  (UDP scanner) │   │
│  └─────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  _quality: MutableStateFlow<ConnectionQuality>         │   │
│  │  _discoveredServers: MutableList<DiscoveredServer>     │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `ConnectionManager` | Core network client (WebSocket, TCP, UDP) |
| `UdpDiscovery` | UDP broadcast for server discovery |

### Implementation Details

#### 1. Quality Observation
```kotlin
private fun observeConnectionManagerQuality() {
    scope.launch {
        connectionManager.connectionQuality.collect { quality ->
            _quality.value = ConnectionQuality(
                ping = quality.ping,
                rssi = quality.rssi,
                jitter = quality.jitter,
                signalStrength = when (quality.signalStrength) {
                    ConnectionManager.ConnectionQuality.SignalStrength.EXCELLENT ->
                        ConnectionQuality.SignalStrength.EXCELLENT
                    // ... etc
                }
            )
        }
    }
}
```

#### 2. Connection Delegation
All connection methods delegate directly to `ConnectionManager`:

```kotlin
override suspend fun connect(ip: String, port: Int, useSSL: Boolean): Boolean {
    return connectionManager.connect(ip, port)
}

override suspend fun disconnect() {
    connectionManager.disconnect()
}

override suspend fun isConnected(): Boolean {
    return connectionManager.isConnected()
}
```

#### 3. Message Sending
All message sending methods delegate to `ConnectionManager`:

```kotlin
override suspend fun sendMove(dx: Float, dy: Float): Boolean {
    return connectionManager.sendMove(dx, dy)
}

override suspend fun sendClick(button: String): Boolean {
    return connectionManager.sendClick(button)
}
```

#### 4. Discovery Management
Server discovery delegates to `UdpDiscovery`:

```kotlin
override suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit) {
    udpDiscovery.onServerFound = { ip, port, name, version ->
        val server = DiscoveredServer(
            ip = ip,
            port = port,
            name = name,
            version = version,
            lastSeen = System.currentTimeMillis()
        )
        if (_discoveredServers.none { it.ip == ip && it.port == port }) {
            _discoveredServers.add(server)
            onServerFound(server)
        }
    }
    udpDiscovery.startDiscovery()
}
```

#### 5. Callback Listeners
Listeners are set directly on `ConnectionManager`:

```kotlin
override fun setOnMessageListener(listener: (String) -> Unit) {
    connectionManager.onMessage = listener
}
```

### Complete Source Code

```kotlin
package com.airmouse.data.datasource.remote

import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.DiscoveredServer
import com.airmouse.network.ConnectionManager
import com.airmouse.network.UdpDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionDataSourceImpl @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val udpDiscovery: UdpDiscovery
) : IConnectionDataSource {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _quality = MutableStateFlow(ConnectionQuality())
    private val _discoveredServers = mutableListOf<DiscoveredServer>()

    init {
        observeConnectionManagerQuality()
    }

    private fun observeConnectionManagerQuality() {
        scope.launch {
            connectionManager.connectionQuality.collect { quality ->
                _quality.value = ConnectionQuality(
                    ping = quality.ping,
                    rssi = quality.rssi,
                    jitter = quality.jitter,
                    signalStrength = when (quality.signalStrength) {
                        ConnectionManager.ConnectionQuality.SignalStrength.EXCELLENT ->
                            ConnectionQuality.SignalStrength.EXCELLENT
                        ConnectionManager.ConnectionQuality.SignalStrength.GOOD ->
                            ConnectionQuality.SignalStrength.GOOD
                        ConnectionManager.ConnectionQuality.SignalStrength.FAIR ->
                            ConnectionQuality.SignalStrength.FAIR
                        ConnectionManager.ConnectionQuality.SignalStrength.POOR ->
                            ConnectionQuality.SignalStrength.POOR
                        else -> ConnectionQuality.SignalStrength.UNKNOWN
                    }
                )
            }
        }
    }

    // ---- Connection Management ----
    override suspend fun connect(ip: String, port: Int, useSSL: Boolean): Boolean =
        connectionManager.connect(ip, port)

    override suspend fun disconnect() = connectionManager.disconnect()

    override suspend fun reconnect(): Boolean {
        connectionManager.reconnect()
        return true
    }

    override suspend fun isConnected(): Boolean = connectionManager.isConnected()

    // ---- Sending Messages ----
    override suspend fun sendMessage(message: String): Boolean =
        connectionManager.send(message)

    override suspend fun sendMessage(message: ByteArray): Boolean =
        connectionManager.sendBinary(message)

    override suspend fun sendMove(dx: Float, dy: Float): Boolean =
        connectionManager.sendMove(dx, dy)

    override suspend fun sendClick(button: String): Boolean =
        connectionManager.sendClick(button)

    override suspend fun sendDoubleClick(): Boolean =
        connectionManager.sendDoubleClick()

    override suspend fun sendRightClick(): Boolean =
        connectionManager.sendRightClick()

    override suspend fun sendScroll(delta: Int): Boolean =
        connectionManager.sendScroll(delta)

    override suspend fun sendGesture(gesture: String, confidence: Float): Boolean =
        connectionManager.sendGesture(gesture, confidence)

    override suspend fun sendProximity(isNear: Boolean, distance: Float): Boolean =
        connectionManager.sendProximity(isNear, distance)

    override suspend fun sendControl(command: String): Boolean =
        connectionManager.sendControl(command)

    override suspend fun sendHello(name: String, version: String): Boolean =
        connectionManager.sendHello(name, version)

    override suspend fun sendPing(): Boolean =
        connectionManager.sendPing()

    override suspend fun sendPong(): Boolean =
        connectionManager.sendPong()

    // ---- Server Discovery ----
    override suspend fun discoverServers(): List<DiscoveredServer> =
        _discoveredServers.toList()

    override suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit) {
        udpDiscovery.onServerFound = { ip, port, name, version ->
            val server = DiscoveredServer(ip, port, name, version)
            if (_discoveredServers.none { it.ip == ip && it.port == port }) {
                _discoveredServers.add(server)
                onServerFound(server)
            }
        }
        udpDiscovery.startDiscovery()
    }

    override suspend fun stopDiscovery() {
        udpDiscovery.stopDiscovery()
        _discoveredServers.clear()
    }

    // ---- Quality ----
    override suspend fun getConnectionQuality(): ConnectionQuality = _quality.value

    override fun observeConnectionQuality(): Flow<ConnectionQuality> = _quality.asStateFlow()

    // ---- Callback Listeners ----
    override fun setOnMessageListener(listener: (String) -> Unit) {
        connectionManager.onMessage = listener
    }

    override fun setOnBinaryMessageListener(listener: (ByteArray) -> Unit) {
        connectionManager.onBinaryMessage = listener
    }

    override fun setOnDisconnectedListener(listener: () -> Unit) {
        connectionManager.onDisconnected = listener
    }

    override fun setOnConnectedListener(listener: () -> Unit) {
        connectionManager.onConnected = listener
    }
}
```

---

## 3. IBluetoothDataSource (Interface)

### Purpose
Defines the contract for **Bluetooth Low Energy (BLE) communication**. Used for proximity detection, Bluetooth HID mouse mode, and device discovery.

### Interface Definition

```kotlin
interface IBluetoothDataSource {
    /** Start scanning for nearby Bluetooth devices. */
    fun startScanning()
    
    /** Stop scanning for Bluetooth devices. */
    fun stopScanning()
    
    /** Connect to a specific Bluetooth device. */
    fun connect(device: BluetoothDevice)
    
    /** Disconnect from the current Bluetooth device. */
    fun disconnect()
    
    /** Send data to the connected device. */
    fun send(data: ByteArray)
    
    /** Observe discovered devices as a Flow. */
    fun observeDevices(): Flow<BluetoothDevice>
    
    /** Observe RSSI (signal strength) changes. */
    fun observeRssi(): Flow<Int>
    
    /** Observe connection state changes. */
    fun connectionState(): Flow<Boolean>
}
```

---

## 4. BluetoothDataSourceImpl (Implementation)

### Purpose
Implements Bluetooth communication using Android's `BluetoothAdapter` and `BluetoothLeScanner`. It provides a reactive interface using `Channel` and `Flow`.

### Implementation Details

```kotlin
@Singleton
class BluetoothDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IBluetoothDataSource {

    private val deviceChannel = Channel<BluetoothDevice>(Channel.BUFFERED)
    private val rssiChannel = Channel<Int>(Channel.BUFFERED)
    private val stateChannel = Channel<Boolean>(Channel.BUFFERED)

    override fun startScanning() {
        // Implementation uses BluetoothLeScanner with ScanCallback
        // Emits discovered devices to deviceChannel
    }

    override fun observeDevices(): Flow<BluetoothDevice> = deviceChannel.receiveAsFlow()
    override fun observeRssi(): Flow<Int> = rssiChannel.receiveAsFlow()
    override fun connectionState(): Flow<Boolean> = stateChannel.receiveAsFlow()
}
```

### Key Features
- **Reactive**: Uses `Channel` and `Flow` for event streaming.
- **BLE Scanning**: Supports Bluetooth Low Energy scanning.
- **RSSI Monitoring**: Tracks signal strength changes.
- **Connection State**: Emits connection/disconnection events.

---

## 5. IUsbDataSource (Interface)

### Purpose
Defines the contract for **USB communication**. Used for USB HID mouse mode and data transfer over USB.

### Interface Definition

```kotlin
interface IUsbDataSource {
    /** Connect to USB device. */
    fun connect()
    
    /** Disconnect from USB device. */
    fun disconnect()
    
    /** Send data via USB. */
    fun send(data: ByteArray)
    
    /** Observe incoming USB messages. */
    fun observeMessages(): Flow<ByteArray>
    
    /** Observe USB connection status. */
    fun connectionStatus(): Flow<Boolean>
}
```

---

## 6. UsbDataSourceImpl (Implementation)

### Purpose
Implements USB communication using Android's `UsbManager`. It provides a reactive interface for USB data transfer.

### Implementation Details

```kotlin
@Singleton
class UsbDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IUsbDataSource {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val messageChannel = Channel<ByteArray>(Channel.BUFFERED)
    private val stateChannel = Channel<Boolean>(Channel.BUFFERED)

    override fun connect() {
        // Implementation using UsbManager with permission handling
        stateChannel.trySend(true)
    }

    override fun observeMessages(): Flow<ByteArray> = messageChannel.receiveAsFlow()
    override fun connectionStatus(): Flow<Boolean> = stateChannel.receiveAsFlow()
}
```

---

## 7. IWebSocketDataSource (Interface)

### Purpose
Defines the contract for **WebSocket communication** (now largely superseded by `ConnectionManager`, but kept for backward compatibility).

### Interface Definition

```kotlin
interface IWebSocketDataSource {
    /** Connect to a WebSocket URL. */
    fun connect(url: String)
    
    /** Disconnect the WebSocket. */
    fun disconnect()
    
    /** Send a text message. */
    fun send(message: String)
    
    /** Observe incoming WebSocket messages. */
    fun observeMessages(): Flow<String>
    
    /** Observe WebSocket connection status. */
    fun connectionStatus(): Flow<Boolean>
}
```

---

## 8. WebSocketDataSourceImpl (Implementation)

### Purpose
Implements WebSocket communication using the deprecated `WebSocketManager`. **Note:** This is kept for backward compatibility; new code should use `ConnectionManager` directly.

### Implementation Details

```kotlin
@Singleton
class WebSocketDataSourceImpl @Inject constructor(
    private val webSocketManager: WebSocketManager
) : IWebSocketDataSource {

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    private val statusChannel = Channel<Boolean>(Channel.BUFFERED)

    init {
        webSocketManager.onMessage = { message -> messageChannel.trySend(message) }
        webSocketManager.onConnected = { statusChannel.trySend(true) }
        webSocketManager.onDisconnected = { statusChannel.trySend(false) }
    }

    override fun connect(url: String) = webSocketManager.connect(url)
    override fun disconnect() = webSocketManager.disconnect()
    override fun send(message: String) = webSocketManager.send(message)
    override fun observeMessages(): Flow<String> = messageChannel.receiveAsFlow()
    override fun connectionStatus(): Flow<Boolean> = statusChannel.receiveAsFlow()
}
```

---

## 🔗 Remote Data Sources in the Architecture

### Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│                          ViewModel                                 │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Use Case                                   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Repository (Interface)                       │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Repository Implementation                       │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Remote Data Source (Interface)                   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  Remote Data Source Implementation                 │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────────────────┐│
│  │ConnectionMgr  │ │  UdpDiscovery │ │ BluetoothAdapter / UsbMgr ││
│  └───────────────┘ └───────────────┘ └───────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

### Injection in DI Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RemoteDataSourceModule {
    
    @Provides
    @Singleton
    fun provideConnectionDataSource(
        connectionManager: ConnectionManager,
        udpDiscovery: UdpDiscovery
    ): IConnectionDataSource {
        return ConnectionDataSourceImpl(connectionManager, udpDiscovery)
    }
    
    @Provides
    @Singleton
    fun provideBluetoothDataSource(
        @ApplicationContext context: Context
    ): IBluetoothDataSource {
        return BluetoothDataSourceImpl(context)
    }
    
    // ... etc
}
```

---

## ✅ Summary

| Data Source | Interface | Implementation | Used For |
|-------------|-----------|----------------|----------|
| **Connection** | `IConnectionDataSource` | `ConnectionDataSourceImpl` | Primary network communication (WebSocket/TCP/UDP) |
| **Bluetooth** | `IBluetoothDataSource` | `BluetoothDataSourceImpl` | Proximity detection, HID mouse |
| **USB** | `IUsbDataSource` | `UsbDataSourceImpl` | USB HID mouse mode |
| **WebSocket** | `IWebSocketDataSource` | `WebSocketDataSourceImpl` | Legacy WebSocket (deprecated) |

### Key Design Principles

| Principle | How It's Applied |
|-----------|------------------|
| **Interface Segregation** | Separate interfaces for each communication type. |
| **Dependency Inversion** | Repositories depend on abstractions, not concrete implementations. |
| **Reactive Programming** | `StateFlow` and `Flow` for reactive data streams. |
| **Single Responsibility** | Each data source handles one type of communication. |
| **Testability** | Interfaces allow easy mocking for unit tests. |

---

**These remote data sources provide the foundation for all external communication in the Air Mouse app, abstracting away the complexities of network protocols, Bluetooth, and USB.**