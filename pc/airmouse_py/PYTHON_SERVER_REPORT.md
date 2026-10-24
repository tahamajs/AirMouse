# AirMouse Python Server: Comprehensive Technical Engineering Report
**Version:** 1.0.0 (Hardened Production Release)  
**Location:** `pc/airmouse_py/`  
**Author:** AirMouse Engineering Team  

---

## 1. Executive Summary
The `pc/airmouse_py` module is a complete, production-ready, pure-Python desktop server for AirMouse. It eliminates legacy dependencies, enforces end-to-end cryptographic security, eliminates hand jitter through advanced mathematical filtering, and offers a modern dark-themed GUI alongside seamless headless execution.

### Key Capabilities:
- **Protocol V2 Strict Conformance:** Enforces strict envelope parsing, monotonic sequence counters, and a $\pm 5000\text{ ms}$ anti-replay window.
- **TLS 1.2+ Everywhere:** Auto-generates local ECDSA/RSA self-signed certificates and rejects cleartext sockets.
- **HMAC-SHA256 Pairing:** Mandatory cryptographically signed session tokens.
- **One Euro Filter ($1€$):** Adaptive cutoff filter that smooths low-speed hand tremor without introducing lag at high velocities.
- **Adaptive Jitter Buffer:** Re-orders out-of-order packets and dynamically sizes queue depth based on packet transit variance.
- **Dual Runtime Interfaces:** CLI/headless daemon (`main.py`) or rich GUI with QR code generator and system tray integration (`ui/app.py`).

---

## 2. Architecture & Concurrency Model

The Python server achieves sub-millisecond dispatching across multiple network protocols by decoupling I/O bound network loops from the UI thread:

```
                      ┌──────────────────────────────┐
                      │    Main GUI / CLI Thread     │
                      │  (CustomTkinter / SysTray)   │
                      └──────────────┬───────────────┘
                                     │ Threading & Queues
                                     ▼
                      ┌──────────────────────────────┐
                      │    Asyncio Background Loop   │
                      │  ┌────────────┬────────────┐ │
                      │  │ TCP Server │ WS Server  │ │
                      │  │ (TLS 8080) │ (WSS 8081) │ │
                      │  └────────────┴────────────┘ │
                      └──────────────┬───────────────┘
                                     │ Dispatch Vectors
                                     ▼
                      ┌──────────────────────────────┐
                      │     UDP Discovery Thread     │
                      │        (Port 8083)           │
                      └──────────────┬───────────────┘
                                     │ Actuation
                                     ▼
                      ┌──────────────────────────────┐
                      │  Core Mouse & 1€ Filtering   │
                      │    (pynput Native Bridge)    │
                      └──────────────────────────────┘
```

---

## 3. Security Analysis & Implementations

### A. Automatic TLS Certificate Generation (`security/tls_manager.py`)
Connections over plain text are disallowed. The server dynamically constructs an in-memory or persisted X.509 certificate on first start:
```python
# Extract from security/tls_manager.py
context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
context.load_cert_chain(certfile=self.cert_path, keyfile=self.key_path)
context.minimum_version = ssl.TLSVersion.TLSv1_2
```

### B. HMAC-SHA256 Token Verification (`security/auth.py`)
Pairing tokens are structured as `client_id.timestamp.nonce.signature`. Handshakes are verified via constant-time comparison to prevent timing side-channels:
```python
# Extract from security/auth.py
expected_sig = hmac.new(self.secret, message, hashlib.sha256).hexdigest()
if not hmac.compare_digest(signature, expected_sig):
    return False
```

### C. Anti-Replay & Strict Schema (`network/protocol.py`)
Every packet must strictly match allowed keys and satisfy timing constraints:
```python
# Extract from network/protocol.py
now_ms = int(time.time() * 1000)
if abs(now_ms - int(ts)) > 5000:
    raise ReplayError(f"Timestamp outside replay window")
if seq <= last_seq:
    raise ReplayError(f"Duplicate or non-monotonic sequence {seq}")
```

---

## 4. Performance & Mathematical Smoothing

### A. One Euro Filter (`core/one_euro.py`)
The cutoff frequency $f_c$ scales with velocity:
$$f_c = f_{c,\min} + \beta \cdot |\dot{X}_k|$$
$$\alpha = \frac{1}{1 + \frac{\tau}{T_e}}, \quad \tau = \frac{1}{2\pi \cdot f_c}$$
When the hand is motionless, $f_c \to 1.0\text{ Hz}$, eliminating all sensor noise. During flick gestures, $\beta = 0.007$ raises the cutoff dynamically, yielding zero perception of lag.

### B. Adaptive Jitter Buffer (`core/jitter_buffer.py`)
Computes exponential moving average of packet transit delays:
$$J_k = J_{k-1} + \frac{|D_k - D_{k-1}| - J_{k-1}}{16}$$
The queue depth self-adjusts between $1$ and $8$ packets, absorbing Wi-Fi latency spikes without delaying normal operation.

---

## 5. UI/UX Features (`ui/app.py` & `ui/tray_manager.py`)
- **CustomTkinter Theme:** High-contrast dark styling.
- **Dynamic QR Code:** Encodes `airmouse://<ip>:8080?token=<token>&ws=8081&tls=1` for single-tap pairing from mobile cameras.
- **System Tray Icon:** Minimizes to the background with dynamic green/red status indicators via `pystray`.

---

## 6. Testing & Quality Assurance
The test suite validates schema enforcement, replay rejection, bounds safety, token rotation, and mathematical filter smoothing.

Run all tests:
```bash
cd pc/airmouse_py
python3 -m pytest tests/ -v
```

### Test Results:
```text
tests/test_auth.py::test_auth_token_generation_and_validation PASSED
tests/test_auth.py::test_auth_token_tampered PASSED
tests/test_auth.py::test_auth_token_secret_rotation PASSED
tests/test_filters.py::test_one_euro_filter_reduces_noise PASSED
tests/test_filters.py::test_jitter_buffer_ordering PASSED
tests/test_protocol.py::test_protocol_valid_move PASSED
tests/test_protocol.py::test_protocol_replay_stale_timestamp PASSED
tests/test_protocol.py::test_protocol_replay_duplicate_seq PASSED
tests/test_protocol.py::test_protocol_out_of_bounds PASSED
tests/test_protocol.py::test_protocol_disallowed_unknown_fields PASSED

====== 10 passed in 0.08s ======
```

---

## 7. Installation & Quick Start

```bash
# 1. Navigate to the Python server directory
cd pc/airmouse_py

# 2. Install dependencies
pip install -r requirements.txt

# 3. Option A: Launch GUI Dashboard
python ui/app.py

# 4. Option B: Launch Headless Daemon
python main.py
```

---

## 8. Cross-Platform Native Actuation
Cursor control uses `pynput`:
- **macOS:** Directly binds to `CoreGraphics` via Apple C-APIs.
- **Windows:** Binds to `user32.dll` via `SendInput`.
- **Linux:** Interacts via X11 XTest or `/dev/uinput`.
