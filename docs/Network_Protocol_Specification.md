# Network Protocol Specification – Complete Explanation

This document provides a **complete, detailed specification** of the network protocol used between the Air Mouse Android app (client) and the PC server. It covers transport layer, message framing, JSON schemas, reliability mechanisms, retransmission logic, connection lifecycle, error handling, and example exchanges.

---

## 1. Transport Layer

| Property | Value |
|----------|-------|
| **Protocol** | TCP (Transmission Control Protocol) |
| **Port** | 8080 (configurable on both sides) |
| **IP address** | Client connects to the server’s IP (same local network) |
| **Message delimiter** | Newline character `\n` (ASCII 10) |
| **Character encoding** | UTF‑8 |
| **Connection direction** | Client (phone) initiates; server (PC) listens |

**Why TCP?**  
- Guarantees ordered, error‑free delivery.  
- Built‑in flow control and congestion avoidance.  
- Suitable for local network where packet loss is rare.

**Why newline delimiter?**  
- Simple to parse – each message ends with `\n`.  
- Allows streaming reads (`readline()` in Python, `BufferedReader.readLine()` in Java/Kotlin).  
- Human‑readable for debugging with tools like `netcat` or `telnet`.

---

## 2. Message Format

All messages are **JSON objects**, one per line. Example:
```json
{"type":"move","dx":12.3,"dy":-5.2}
```

### Common Fields

| Field | Type | Description | Present in |
|-------|------|-------------|-------------|
| `type` | string | Message type identifier | All messages |
| `id` | long (millisecond timestamp) | Unique identifier for ACK‑required messages | `click`, `doubleclick`, `rightclick`, `scroll` |
| `dx`, `dy` | float | Delta movement in pixels (horizontal, vertical) | `move` |
| `delta` | int | Scroll amount: `+1` = down, `-1` = up | `scroll` |

---

## 3. Message Types (Detailed)

### 3.1 `move` – Cursor movement

**Direction:** Phone → PC  
**ACK required:** No  
**Retransmission:** No  

**Purpose:** Continuously send relative cursor movement deltas.

**JSON schema:**
```json
{
  "type": "move",
  "dx": float,   // horizontal delta (positive = right)
  "dy": float    // vertical delta (positive = down)
}
```

**Example:** `{"type":"move","dx":5.2,"dy":-3.1}`

**Behaviour on PC:** Moves cursor by `(dx * sensitivity, dy * sensitivity)` with clipping to ±50 pixels per packet.

---

### 3.2 `click` – Left mouse click

**Direction:** Phone → PC  
**ACK required:** Yes  
**Retransmission:** Yes (once after 500 ms timeout)

**Purpose:** Signal a single left‑click.

**JSON schema:**
```json
{
  "type": "click",
  "id": long   // System.currentTimeMillis() on Android
}
```

**Example:** `{"type":"click","id":1700000000000}`

**Behaviour on PC:** Performs `pyautogui.click()`, then sends back an ACK with the same `id`. If no ACK received within 500 ms, the phone retransmits the same message once.

---

### 3.3 `doubleclick` – Double mouse click

**Direction:** Phone → PC  
**ACK required:** Yes  
**Retransmission:** Yes

**Purpose:** Signal a double‑click (two clicks within 400 ms).

**JSON schema:**
```json
{
  "type": "doubleclick",
  "id": long
}
```

**Behaviour on PC:** Performs `pyautogui.doubleClick()` and sends ACK.

---

### 3.4 `rightclick` – Right mouse click

**Direction:** Phone → PC  
**ACK required:** Yes  
**Retransmission:** Yes

**Purpose:** Signal a right‑click (long tilt >45° for 0.5 s).

**JSON schema:**
```json
{
  "type": "rightclick",
  "id": long
}
```

**Behaviour on PC:** Performs `pyautogui.click(button='right')` and sends ACK.

---

### 3.5 `scroll` – Mouse wheel scroll

**Direction:** Phone → PC  
**ACK required:** Yes  
**Retransmission:** Yes

**Purpose:** Scroll up or down.

**JSON schema:**
```json
{
  "type": "scroll",
  "delta": int,   // +1 = scroll down, -1 = scroll up
  "id": long
}
```

**Example:** `{"type":"scroll","delta":1,"id":1700000000003}`

**Behaviour on PC:** Performs `pyautogui.scroll(delta)` and sends ACK.

---

### 3.6 `ack` – Acknowledgment

**Direction:** PC → Phone  
**ACK required:** No (this is the ACK itself)  
**Retransmission:** N/A

**Purpose:** Confirm receipt of a critical message (click, doubleclick, rightclick, scroll).

**JSON schema:**
```json
{
  "type": "ack",
  "id": long   // same id as the message being acknowledged
}
```

**Example:** `{"type":"ack","id":1700000000000}`

**Behaviour on Phone:** Upon receiving an ACK, the phone removes the corresponding message from its `pendingAcks` map, preventing retransmission.

---

## 4. Reliability Mechanism (ACK + Retransmission)

### 4.1 Why ACK for critical messages?
- Move packets can be lost without noticeable effect (cursor may jump, but next packet corrects it).
- Click/scroll packets **must** be delivered reliably; a lost click means the user expects an action that doesn’t happen.

### 4.2 Retransmission Algorithm (Android side)

```kotlin
// After sending a critical message (click, scroll, etc.)
pendingAcks[id] = jsonMessage
Thread.sleep(ACK_TIMEOUT_MS)  // 500 ms
if (pendingAcks.containsKey(id)) {
    // No ACK received – retransmit
    out?.writeBytes("$jsonMessage\n")
    pendingAcks.remove(id)
}
```

**Timeout value:** 500 ms is chosen because typical local network RTT is <10 ms; 500 ms gives plenty of margin.

**Retransmission limit:** **Once** only (to avoid infinite loops). If the second attempt also fails, the message is dropped. In practice, a second failure indicates severe network issues; the auto‑reconnect mechanism will restore the connection.

### 4.3 ACK Receiver Thread (Android side)

A background thread continuously reads lines from the socket:
```kotlin
val line = input?.readLine() ?: break
if (line.contains("\"type\":\"ack\"")) {
    val id = JSONObject(line).optLong("id", -1)
    pendingAcks.remove(id)  // prevents retransmission
}
```

### 4.4 PC Server ACK Sending (Python side)

Upon receiving a critical message, the server immediately sends the ACK:
```python
async def send_ack(self, msg_id, writer):
    if msg_id:
        ack = json.dumps({'type': 'ack', 'id': msg_id})
        writer.write(ack.encode() + b'\n')
        await writer.drain()
```

**Important:** The PC server does **not** wait for any processing to complete before sending the ACK. This minimises latency.

---

## 5. Connection Lifecycle

### 5.1 Establishment

1. **User starts Air Mouse** on Android and enters the PC’s IP address.
2. **Android app creates a `Socket`** and calls `connect(ip, 8080)`.
3. **PC server** (already listening on `0.0.0.0:8080`) accepts the connection.
4. **Both sides log** the connection event.

### 5.2 Active Communication

- Android sends **move messages** continuously (≈50 Hz) – no ACKs.
- When a gesture occurs, Android sends a **critical message** with an `id`.
- PC executes the action and immediately returns an **ACK**.
- Android removes the `id` from pending map.

### 5.3 Connection Loss & Reconnection

- If the socket throws an `IOException` on Android (e.g., WiFi drops), the `DataSender` thread catches it, logs the error, and waits 5 seconds before retrying.
- The `AutoReconnect` helper (optional) also monitors connection health and forces a reconnect if needed.
- The PC server will see the client disconnect (EOF) and log it.

### 5.4 Graceful Shutdown

- **Android side:** `stopSending()` sets `running = false`, closes the socket, and interrupts threads.
- **PC side:** The server’s `handle_client` loop detects `reader.readline()` returning empty, closes the writer, and logs the disconnection.

---

## 6. Error Handling

| Error Scenario | Android Behaviour | PC Behaviour |
|----------------|------------------|---------------|
| Connection refused (server not running) | Exception caught, retry after 5 seconds | (none) |
| Socket write failure (network down) | Exception caught, close socket, retry after 5 seconds | Connection dropped; logs error |
| Malformed JSON from Android | (not applicable – we generate valid JSON) | Log warning, ignore message |
| ACK timeout (500 ms) | Retransmit once | (none – server already sent ACK; maybe lost) |
| Duplicate ACK (rare) | Second ACK ignored | - |
| Server overloaded | Retransmission may occur; no special handling | Process queue normally |

---

## 7. Example Message Exchange

```
[Phone] → PC: {"type":"move","dx":5.0,"dy":2.0}
[Phone] → PC: {"type":"move","dx":3.0,"dy":1.0}
[Phone] → PC: {"type":"click","id":1001}
[PC]   → Phone: {"type":"ack","id":1001}
[Phone] → PC: {"type":"scroll","delta":1,"id":1002}
[PC]   → Phone: {"type":"ack","id":1002}
[Phone] → PC: {"type":"move","dx":1.0,"dy":0.5}
```

If the ACK for `click` (id=1001) is lost:
```
[Phone] → PC: {"type":"click","id":1001}
(no ACK after 500 ms)
[Phone] → PC: {"type":"click","id":1001}   // retransmission
[PC]   → Phone: {"type":"ack","id":1001}
```

---

## 8. Security Considerations

- **Cleartext traffic** – used because the app operates only on trusted local networks.
- **No authentication** – any client on the network can connect to the server. This is acceptable for the exercise, but for real deployment, add a simple token or restrict by IP.
- **Firewall** – The PC must allow incoming connections on port 8080.

**Recommendation:** Do **not** expose this server to the public internet.

---

## 9. Performance Metrics

| Metric | Typical Value |
|--------|----------------|
| Message rate (move) | 50 Hz (20 ms interval) |
| Payload per move | ~30 bytes |
| Network bandwidth | ~12 Kbps |
| ACK latency | <5 ms (local network) |
| Retransmission timeout | 500 ms |

---

## 10. Configurability

| Parameter | Android constant | PC config |
|-----------|------------------|------------|
| Port | `PORT = 8080` in `MainActivity.kt` | `config.json` or `CONFIG["port"]` |
| ACK timeout | `ACK_TIMEOUT_MS = 500L` in `DataSender.kt` | (not needed) |
| Reconnect delay | `RECONNECT_DELAY_MS = 5000L` | (not needed) |

To change the port, modify **both sides** to match.

---

## 11. Testing the Protocol Manually

You can test the server using `telnet` or `netcat`:

```bash
# Connect to server
telnet localhost 8080
# Send a move message
{"type":"move","dx":10,"dy":0}
# Send a click
{"type":"click","id":12345}
# Expected ACK back: {"type":"ack","id":12345}
```

Or with Python:
```python
import socket, json
s = socket.socket()
s.connect(("192.168.1.10", 8080))
s.sendall(b'{"type":"click","id":999}\n')
print(s.recv(1024))  # Should receive ACK
```

---

## 12. Summary Table

| Feature | Specification |
|---------|---------------|
| **Protocol** | TCP |
| **Port** | 8080 |
| **Framing** | Newline‑delimited JSON |
| **Move messages** | No ACK, no retransmission |
| **Click/scroll messages** | ACK required, one retransmission after 500 ms |
| **Connection recovery** | Automatic retry every 5 seconds + `AutoReconnect` monitor |
| **Idempotency** | Retransmitted messages are safe (repeat action, but user may notice duplicate click; acceptable given low probability) |
| **Threading** | Android: separate sender, ACK receiver, retransmit threads; PC: asyncio |

---

This specification ensures **reliable, low‑latency control** suitable for a wireless mouse application. All parts are implemented in the provided Android (`DataSender.kt`, `AutoReconnect.kt`) and Python (`server.py`, `gui.py`) code.