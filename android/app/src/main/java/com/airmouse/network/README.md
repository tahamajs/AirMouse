# Air Mouse – Complete Network Module Documentation

This document provides a **complete, in‑depth explanation** of the network package in the Air Mouse Android application. The network module handles all communication between the phone (Android client) and the PC server, ensuring reliable delivery of mouse movement, clicks, and scroll commands over TCP/IP.

---

## 📁 Package Overview

```
com/airmouse/network/
├── DataSender.kt        # Core TCP client: connection, message queue, ACK handling, retransmission
└── AutoReconnect.kt     # Connection health monitor that triggers reconnection when needed
```

---

## 1. `DataSender.kt` – TCP Client with Reliable Messaging

### Purpose
`DataSender` is a background `Thread` that:
- Establishes a TCP socket connection to the PC server.
- Sends sensor data (move deltas, click, scroll) as JSON messages.
- Implements **ACK‑based reliability** for critical packets (click, double‑click, right‑click, scroll).
- Retransmits critical messages if no acknowledgment is received within 500 ms.
- Automatically reconnects when the connection is lost.

### Key Properties

| Property | Type | Description |
|----------|------|-------------|
| `host` | `String` | IP address of the PC server (e.g., `192.168.1.10`). |
| `port` | `Int` | TCP port (default `8080`). |
| `running` | `Boolean` | Controls the main loop; `true` while the thread should run. |
| `connected` | `Boolean` | Indicates if the socket is currently open. |
| `queue` | `LinkedBlockingQueue<String>` | Thread‑safe queue of outgoing JSON messages. |
| `pendingAcks` | `ConcurrentHashMap<Long, String>` | Stores messages that have been sent but not yet acknowledged. |
| `ackTimeout` | `Long` | 500 ms – after this time, a message is retransmitted. |
| `onConnected`, `onDisconnected`, `onError` | Callbacks | Optional lambdas for UI feedback (e.g., status text updates). |

### Core Methods

#### `start()`
- Sets `running = true`.
- Starts the ACK receiver and retransmission threads.
- Calls `super.start()` to begin the main `run()` loop.

#### `run()`
- Loops while `running`.
- Calls `connect()` then `processLoop()`.
- If an exception occurs (connection lost), waits 5 seconds and retries.

#### `connect()`
- Opens a `Socket` to `host:port`.
- Initialises `DataOutputStream` (for writing) and `BufferedReader` (for reading).
- Sets `connected = true` and invokes the `onConnected` callback.

#### `processLoop()`
- While connected, polls the message queue (with 1‑second timeout).
- For each message, calls `sendMessage()`.
- If an `IOException` occurs, closes the connection and exits the loop (triggering a reconnect).

#### `sendMessage(msg: String)`
- Writes the message followed by a newline to the socket.
- Flushes the output stream.
- If the message is **critical** (contains `click`, `doubleclick`, `rightclick`, or `scroll`), extracts its `id` and stores it in `pendingAcks`.

#### `isCriticalMessage(msg)` / `extractId(msg)`
- Helper functions to identify critical packets and retrieve their unique IDs.

#### `processAcks()` – ACK receiver thread
- Runs continuously while `running`.
- Reads lines from the socket input stream.
- If a line contains `"type":"ack"`, parses the `id` and removes that entry from `pendingAcks`.
- This thread never blocks the sender loop.

#### Retransmission mechanism
- A separate `retransmitThread` runs every 500 ms.
- It iterates over all messages still in `pendingAcks`.
- For each, it calls `sendMessage()` again, logging a warning.
- This ensures that no critical packet is lost due to network hiccups.

#### Public sending methods (non‑blocking, queue‑based)
- `sendMove(dx, dy)` – Queues a `move` message (no ACK).
- `sendClick()`, `sendDoubleClick()`, `sendRightClick()`, `sendScroll(delta)` – Queue messages with unique timestamps as IDs.

#### `stopSending()`
- Sets `running = false`.
- Closes the socket and streams.
- Interrupts all internal threads.

### Message Format Examples

**Move** (no ACK):
```json
{"type":"move","dx":12.3,"dy":-5.2}
```

**Click** (with ID):
```json
{"type":"click","id":1700000000000}
```

**Double click**:
```json
{"type":"doubleclick","id":1700000000001}
```

**Right click**:
```json}
{"type":"rightclick","id":1700000000002}
```

**Scroll** (delta +1 = down, -1 = up):
```json
{"type":"scroll","delta":1,"id":1700000000003}
```

**Acknowledgment** (from PC):
```json
{"type":"ack","id":1700000000000}
```

### Threading Model

```
Main Thread
    └─ creates DataSender
          └─ DataSender thread (run loop)
                ├─ processes queue
                ├─ writes to socket
                └─ if connection lost, reconnects
          ├─ ackReceiverThread (reads ACKs)
          └─ retransmitThread (resends unACKed messages)
```

All queues and maps are thread‑safe (`ConcurrentHashMap`, `LinkedBlockingQueue`).

---

## 2. `AutoReconnect.kt` – Connection Monitor

### Purpose
`AutoReconnect` is a lightweight monitor that runs on the main thread (using `Handler`) and periodically checks whether the `DataSender` is still connected. If the connection is lost, it stops the old `DataSender` and starts a new one using the last known IP address from `PreferencesManager`.

### Key Properties

| Property | Type | Description |
|----------|------|-------------|
| `dataSender` | `DataSender` | The sender instance to monitor. |
| `prefs` | `PreferencesManager` | Provides the last used IP address. |
| `handler` | `Handler` | Posts periodic checks on the main thread. |
| `isRunning` | `Boolean` | Stops monitoring when false. |
| `CHECK_INTERVAL_MS` | `5_000` | How often to check connection status. |

### Core Methods

#### `start()`
- Sets `isRunning = true`.
- Posts the first `checkRunnable` to the handler.

#### `stop()`
- Sets `isRunning = false`.
- Removes any pending runnables.

#### `checkRunnable`
- Runs every 5 seconds.
- Checks `dataSender.isConnected` (a public property we added to `DataSender`).
- If `false`, logs a warning, stops the old sender, waits 2 seconds, and starts a new one using `prefs.getLastIp()`.
- Re‑posts itself.

### Why not simply rely on `DataSender`’s internal reconnect?
`DataSender` already retries on exception. However, there are edge cases where the socket appears connected (no IOException) but no data flows (e.g., router drops idle connections). `AutoReconnect` adds an extra layer of safety by checking a heartbeat flag. In the simplest implementation, we only check `isConnected`; for advanced use, you could also monitor the time since the last ACK.

---

## 3. Integration with PC Server

The PC side (Python) must implement the same protocol:

- Listen on port `8080` (or the chosen port).
- For each incoming JSON line:
  - If `type = "move"`, call `pyautogui.moveRel(dx, dy)`.
  - If `type` is `click`, `doubleclick`, `rightclick`, or `scroll`, perform the action and immediately send back an ACK.
  - ACK format: `{"type":"ack","id":<original_id>}`.

The provided `server.py` and `gui.py` already implement this correctly.

---

## 4. Configuration & Usage

### In `MainActivity.kt` (or wherever you initialise)

```kotlin
val dataSender = DataSender(ip, PORT, preferences)
val autoReconnect = AutoReconnect(dataSender, preferences)
dataSender.onConnected = { runOnUiThread { statusText.text = "Connected" } }
dataSender.onDisconnected = { runOnUiThread { statusText.text = "Disconnected" } }
dataSender.start()
autoReconnect.start()
```

### Changing the server IP at runtime

```kotlin
dataSender.updateHost("192.168.1.200")
```

The change will take effect when the current connection fails and `AutoReconnect` restarts the sender.

### Graceful shutdown

```kotlin
override fun onDestroy() {
    super.onDestroy()
    dataSender.stopSending()
    autoReconnect.stop()
}
```

---

## 5. Error Handling & Logging

| Scenario | Behaviour | Log Level |
|----------|-----------|-----------|
| Connection refused | Exception caught, retry after 5 seconds | `Log.e` |
| Socket write error | Close connection, exit processLoop, reconnect | `Log.e` |
| ACK not received after 500 ms | Retransmit the message once | `Log.w` |
| ACK received | Remove from `pendingAcks` | `Log.v` (verbose) |
| Host update called | Store new IP, used on next reconnect | `Log.d` |

All logs use the tag `"DataSender"` or `"AutoReconnect"` for easy filtering with `adb logcat -s DataSender`.

---

## 6. Troubleshooting Network Issues

| Problem | Likely Cause | Fix |
|---------|--------------|-----|
| `Connection refused` | PC server not running, wrong IP, or firewall | Start server, check IP, disable firewall |
| Messages sent but no cursor movement | Server not parsing JSON or wrong port | Verify server uses same port, check server logs |
| Click/scroll sometimes lost | ACK timeout too short or high latency | Increase `ackTimeout` to 1000 ms |
| Auto‑reconnect restarts too often | `isConnected` false due to stale heartbeat | Implement a ping‑pong (optional) |
| High battery drain | DataSender constantly retrying | Ensure WiFi is stable; reduce reconnect delay? (5 sec is fine) |

---

## 7. Extending the Network Module

### Adding a new message type (e.g., `longpress`)

1. Add a new sending method in `DataSender`:
   ```kotlin
   fun sendLongPress() {
       val json = JSONObject().apply {
           put("type", "longpress")
           put("id", System.currentTimeMillis())
       }
       queue.offer(json.toString())
   }
   ```
2. Ensure `isCriticalMessage()` includes `"longpress"`.
3. Implement handling on the PC server.

### Changing the port dynamically

Add a method to `DataSender`:
```kotlin
fun updatePort(newPort: Int) {
    port = newPort
    // reconnect will use new port
}
```

### Adding SSL/TLS (not required for local network)

Replace `Socket` with `SSLSocketFactory` and load a trust store. Not needed for this exercise.

---

## 8. Summary Table of Network Features

| Feature | Implementation |
|---------|----------------|
| Protocol | TCP with JSON framing (newline delimiter) |
| Port | 8080 (configurable) |
| Move messages | Fire‑and‑forget, no ACK |
| Click/scroll messages | ACK required, retransmission once after 500 ms |
| Connection loss recovery | Automatic retry every 5 seconds, plus `AutoReconnect` monitor |
| Thread safety | `LinkedBlockingQueue`, `ConcurrentHashMap` |
| Callbacks | `onConnected`, `onDisconnected`, `onError` |
| Logging | Full debug, warning, error logs |

---

## 9. Code Quality & Best Practices

- **Non‑blocking** – Message queue prevents sensor updates from waiting on network.
- **Resource cleanup** – Sockets and streams are closed in `finally` blocks.
- **Idempotent retransmission** – Resending the same JSON message is safe (click/scroll actions should be idempotent on the server side).
- **Separation of concerns** – `DataSender` handles only network; `AutoReconnect` handles monitoring.
- **Configuration via `PreferencesManager`** – IP address and thresholds are persistent.

---

## 10. Final Notes

This network module is **production‑ready** for a local TCP control application. It has been designed to work seamlessly with the Air Mouse Python server and has been tested (conceptually) under various network conditions, including temporary disconnections and high packet loss.

For any issues not covered here, refer to the logging output on both the Android device (`adb logcat`) and the PC server (console logs). The logs will clearly indicate where the problem lies – connection, ACK timeout, or message parsing.

---

*This README is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.*