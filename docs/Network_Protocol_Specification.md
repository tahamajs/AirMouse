# Network Protocol Specification – Complete Explanation

This document defines the **complete network protocol** used between the Air Mouse Android app (client) and the PC server. It covers the transport layer, message framing, JSON schemas, reliability mechanisms, retransmission logic, error handling, and example exchanges.

---

## 1. Transport Layer

| Property | Value | Explanation |
|----------|-------|-------------|
| **Protocol** | TCP | Provides ordered, error‑free, reliable delivery. Suitable for local network where packet loss is rare. |
| **Port** | 8080 (default) | Configurable on both sides. Must match between app and server. |
| **Encoding** | UTF‑8 | Ensures all characters (including JSON) are transmitted correctly. |
| **Message delimiter** | Newline (`\n`, ASCII 10) | Allows simple line‑based reading (`readline()` in Python, `BufferedReader.readLine()` in Kotlin). |
| **Connection direction** | Client (phone) initiates | Server listens passively on the configured port. |

**Why TCP?**  
- Guarantees that messages arrive in the same order they were sent.  
- Built‑in flow control and congestion avoidance.  
- No need to implement packet sequencing or reassembly.  
- The ACK‑based reliability for critical messages is an application‑layer addition; TCP already provides reliable delivery, but we add extra ACKs to ensure the server has *processed* the action (not just received the packet).

**Why newline delimiter?**  
- Simple and human‑readable.  
- Allows easy debugging with `telnet` or `nc`.  
- Works with streaming reads – no need to send message length prefixes.

---

## 2. Message Format

All messages are **JSON objects** terminated by a newline character.  
**Example:**  
```json
{"type":"move","dx":12.3,"dy":-5.2}
```

### Common Fields

| Field | Type | Description | Present in |
|-------|------|-------------|-------------|
| `type` | `string` | Message type identifier (`move`, `click`, `doubleclick`, `rightclick`, `scroll`, `ack`) | All messages |
| `id` | `long` (timestamp) | Unique identifier (usually `System.currentTimeMillis()`). Used for ACK matching. | `click`, `doubleclick`, `rightclick`, `scroll`, `ack` |
| `dx`, `dy` | `float` | Relative cursor movement in pixels (positive = right/down). | `move` |
| `delta` | `int` | Scroll amount: `+1` = scroll down, `-1` = scroll up. | `scroll` |

---

## 3. Message Types – Detailed Specifications

### 3.1 `move` – Cursor Movement

**Direction:** Phone → PC  
**ACK required:** No  
**Retransmission:** No  

**Purpose:** Continuously send relative cursor movement deltas. Loss of a single packet is acceptable because the next packet corrects the position.

**JSON Schema:**
```json
{
  "type": "move",
  "dx": float,   // horizontal delta (positive = right)
  "dy": float    // vertical delta (positive = down)
}
```

**Example:**  
```json
{"type":"move","dx":12.3,"dy":-5.2}
```

**Behaviour on PC:**  
- Applies sensitivity multiplier (from config or GUI slider).  
- Clips the result to ±50 pixels to prevent erratic jumps.  
- Calls `pyautogui.moveRel(dx, dy, duration=0.0)`.  

**Latency requirement:** The server should process this message as fast as possible – no blocking operations.

---

### 3.2 `click` – Left Mouse Click

**Direction:** Phone → PC  
**ACK required:** Yes  
**Retransmission:** Yes (once after 500 ms timeout)

**Purpose:** Signal a single left‑click.

**JSON Schema:**
```json
{
  "type": "click",
  "id": long   // unique timestamp, e.g., System.currentTimeMillis()
}
```

**Example:**  
```json
{"type":"click","id":1700000000000}
```

**Behaviour on PC:**  
- Performs `pyautogui.click()`.  
- Immediately sends an ACK with the same `id`.  
- Logs the action (e.g., "🖱️ Click").

**Retransmission logic on phone:**  
- After sending, the phone starts a 500 ms timer.  
- If no ACK is received, it sends the **exact same message** once more.  
- If still no ACK, the packet is dropped (user may need to repeat the gesture).  

**Why 500 ms?** Typical local network RTT is <10 ms; 500 ms gives plenty of margin for processing and any queuing.

---

### 3.3 `doubleclick` – Double Mouse Click

**Direction:** Phone → PC  
**ACK required:** Yes  
**Retransmission:** Yes (same as `click`)

**Purpose:** Signal a double‑click (two quick flicks within 400 ms).

**JSON Schema:**
```json
{
  "type": "doubleclick",
  "id": long
}
```

**Example:**  
```json
{"type":"doubleclick","id":1700000000001}
```

**Behaviour on PC:**  
- Performs `pyautogui.doubleClick()`.  
- Sends ACK.  

**Note:** The PC server does not perform any timing check – the phone already determined it was a double click.

---

### 3.4 `rightclick` – Right Mouse Click

**Direction:** Phone → PC  
**ACK required:** Yes  
**Retransmission:** Yes

**Purpose:** Signal a right‑click (long tilt >45° for 0.5 s).

**JSON Schema:**
```json
{
  "type": "rightclick",
  "id": long
}
```

**Example:**  
```json
{"type":"rightclick","id":1700000000002}
```

**Behaviour on PC:**  
- Performs `pyautogui.click(button='right')`.  
- Sends ACK.

---

### 3.5 `scroll` – Mouse Wheel Scroll

**Direction:** Phone → PC  
**ACK required:** Yes  
**Retransmission:** Yes

**Purpose:** Scroll up or down by one notch.

**JSON Schema:**
```json
{
  "type": "scroll",
  "delta": int,   // +1 = scroll down, -1 = scroll up
  "id": long
}
```

**Example:**  
```json
{"type":"scroll","delta":1,"id":1700000000003}
```

**Behaviour on PC:**  
- Calls `pyautogui.scroll(delta)`.  
- Sends ACK.  
- Logs the direction (e.g., "📜 Scroll 1").

---

### 3.6 `ack` – Acknowledgment

**Direction:** PC → Phone  
**ACK required:** No (this is the ACK itself)  
**Retransmission:** N/A

**Purpose:** Confirm receipt and processing of a critical message (click, doubleclick, rightclick, scroll).

**JSON Schema:**
```json
{
  "type": "ack",
  "id": long   // same id as the message being acknowledged
}
```

**Example:**  
```json
{"type":"ack","id":1700000000000}
```

**Behaviour on Phone:**  
- The phone’s ACK receiver thread reads incoming lines.  
- When it sees `"type":"ack"`, it extracts the `id` and removes the corresponding entry from the `pendingAcks` map.  
- This prevents retransmission.  

**Timing:** The server sends the ACK **immediately after executing the action** (no delay). The phone waits up to 500 ms for it.

---

## 4. Reliability Mechanism (ACK + Retransmission)

### 4.1 Why ACK for critical messages?
- Move packets can be lost without noticeable effect (cursor may jump, but next packet corrects it).  
- Click/scroll packets **must** be delivered reliably; a lost click means the user expects an action that doesn’t happen.  
- TCP only guarantees delivery to the kernel; the application may crash or be too slow. Our ACK confirms that the server *processed* the message.

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

**Timeout value:** 500 ms – long enough for local network and server processing.

**Retransmission limit:** **Once** only. A second failure indicates severe network issues; the auto‑reconnect mechanism will restore the connection.

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

**Important:** The PC server does **not** wait for any processing to complete before sending the ACK. This minimises latency and ensures the ACK arrives before the retransmission timer expires.

---

## 5. Connection Lifecycle

### 5.1 Establishment

1. **User starts Air Mouse** on Android and enters the PC’s IP address.  
2. **Android app creates a `Socket`** and calls `connect(ip, 8080)`.  
3. **PC server** (already listening on `0.0.0.0:8080`) accepts the connection.  
4. **Both sides log** the connection event (Android: `"Connected to ..."`, PC: `"✅ Connected: (ip, port)"`).

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

| Metric | Typical Value | Explanation |
|--------|----------------|-------------|
| Message rate (move) | 50 Hz (20 ms interval) | Set by `SENSOR_DELAY_GAME`. |
| Payload per move | ~30 bytes | `{"type":"move","dx":12.3,"dy":-5.2}` |
| Network bandwidth | ~12 Kbps | 30 bytes × 50 = 1.5 KB/s → ~12 Kbps. |
| ACK latency | <5 ms | Local network round‑trip time. |
| Retransmission timeout | 500 ms | Configurable in code. |

---

## 10. Configurability

| Parameter | Android constant | PC config | Default |
|-----------|------------------|------------|---------|
| Port | `PORT` in `MainActivity.kt` | `config.json` (`port`) or `CONFIG["port"]` in `gui.py` | 8080 |
| ACK timeout | `ACK_TIMEOUT_MS` in `DataSender.kt` | (not needed) | 500 ms |
| Reconnect delay | `RECONNECT_DELAY_MS` in `DataSender.kt` | (not needed) | 5000 ms |

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
| **Port** | 8080 (configurable) |
| **Framing** | Newline‑delimited JSON |
| **Move messages** | No ACK, no retransmission |
| **Click/scroll messages** | ACK required, one retransmission after 500 ms |
| **Connection recovery** | Automatic retry every 5 seconds + `AutoReconnect` monitor |
| **Idempotency** | Retransmitted messages are safe (repeat action, but acceptable given low probability) |
| **Threading** | Android: separate sender, ACK receiver, retransmit threads; PC: asyncio |

---

This specification ensures **reliable, low‑latency control** suitable for a wireless mouse application. All parts are implemented in the provided Android (`DataSender.kt`, `AutoReconnect.kt`) and Python (`server.py`, `gui.py`) code.