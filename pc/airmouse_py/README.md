<div align="center">

# 🖱️ AirMouse Core (`airmouse-core`)

[![PyPI version](https://img.shields.io/pypi/v/airmouse-core.svg?color=blue)](https://pypi.org/project/airmouse-core/)
[![Python Versions](https://img.shields.io/pypi/pyversions/airmouse-core.svg)](https://pypi.org/project/airmouse-core/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Code Style](https://img.shields.io/badge/code%20style-PEP%20561-green.svg)](https://peps.python.org/pep-0561/)
[![Tests Coverage](https://img.shields.io/badge/coverage-88%25-brightgreen.svg)]()

**High-performance, ultra-low-latency remote mouse and input server engine for Python.**  
Seamlessly turns Android, iOS, or web client devices into responsive air mice with hardware-grade cursor smoothing, robust replay-attack mitigation, and TLS 1.2+ encryption.

[Key Features](#-key-features) • [Installation](#-installation) • [Quick Start CLI](#-quick-start-cli) • [Python API](#-python-api) • [Architecture](#-architecture) • [Protocol V2](#-protocol-v2-specification) • [Testing](#-test-suite)

</div>

---

## ⚡ Key Features

- 🔒 **Enterprise-Grade TLS Security**:
  - Full TLS 1.2+ encryption on both TCP and WebSocket connections.
  - Automatic self-signed X.509 certificate provisioning with persistent key storage (`0600` permissions) to prevent TLS pinning breaks on server reboot.
- 📱 **Zero-Friction Terminal QR Pairing**:
  - Out-of-the-box CLI displays ANSI QR codes right in your terminal for 1-second camera scan pairing from mobile devices.
- 🎯 **1€ (One Euro) Adaptive Smoothing**:
  - Implements Casiez et al. adaptive low-pass filter algorithm to eliminate micro-jitter during slow precision movements while eliminating lag during fast sweeps.
- 🌐 **LAN Auto-Discovery**:
  - Multithreaded UDP broadcast responder with cryptographic challenge-response nonces for instantaneous zero-configuration LAN discovery.
- 🧵 **Thread-Safe Actuation Decoupling**:
  - Completely decouples high-frequency network I/O (`asyncio`) from OS windowing/mouse controller drivers (`pynput`) via an internal thread-safe queue.
- 🛡️ **Anti-Replay Attack Protection**:
  - Enforces sliding timestamp freshness windows and per-session monotonic sequence numbering.
- 📦 **PEP 561 Compliant**:
  - Full typing support with `py.typed` marker for static analysis with mypy, pyright, and modern IDE auto-complete.

---

## 📥 Installation

Install the stable release directly from PyPI:

```bash
pip install airmouse-core
```

Or install with development dependencies:

```bash
pip install airmouse-core[dev]
```

---

## 🚀 Quick Start CLI

Run the production server directly from the command line:

```bash
airmouse-server --port 8080 --ws-port 8081 --udp-port 8083 --qr
```

### CLI Options:

| Flag | Description | Default |
|------|-------------|---------|
| `--host` | Bind interface address | `0.0.0.0` |
| `--port` | TCP listening port | `8080` |
| `--ws-port` | WebSocket listening port | `8081` |
| `--udp-port` | UDP LAN discovery broadcast port | `8083` |
| `--qr` | Render terminal ANSI QR code for mobile pairing | `False` |
| `--no-tls` | Disable TLS encryption | `False` (TLS enabled) |
| `--no-smoothing` | Disable 1€ adaptive filter | `False` (Smoothing enabled) |
| `--sensitivity` | Mouse cursor speed multiplier | `1.0` |
| `--name` | Friendly server display name | `AirMouse Server` |

---

## 💻 Python API

`airmouse-core` exposes a clean, intuitive public API:

```python
import asyncio
from airmouse import (
    AirMouseServer,
    OneEuroFilter,
    UDPJitterBuffer,
    AuthManager,
    ProtocolParser,
)

async def main():
    # 1. Initialize server
    server = AirMouseServer(
        host="0.0.0.0",
        tcp_port=8080,
        ws_port=8081,
        udp_port=8083,
        enable_tls=True,
        sensitivity=1.2,
        smoothing=True,
    )

    # 2. Register lifecycle and input event callbacks
    server.on_connect(lambda session_id: print(f"Client connected: {session_id}"))
    server.on_disconnect(lambda session_id: print(f"Client disconnected: {session_id}"))
    server.on_move(lambda dx, dy: print(f"Motion: dx={dx:.2f}, dy={dy:.2f}"))
    server.on_click(lambda button: print(f"Click: {button}"))

    # 3. Start server
    await server.start()
    print("AirMouse Server is running. Press Ctrl+C to exit.")

    try:
        await asyncio.Event().wait()
    finally:
        await server.stop()

if __name__ == "__main__":
    asyncio.run(main())
```

---

## 🏗️ Architecture

```
 Mobile / Web Client (Android / iOS / Browser)
                     │
         ┌───────────┴───────────┐
         ▼                       ▼
   [TCP TLS 1.2+]         [WebSocket TLS]
         │                       │
         └───────────┬───────────┘
                     │  (Asyncio Network Loop)
                     ▼
             ProtocolParser (V2)
       [Replay & Bounds Validation]
                     │
                     ▼
          Thread-Safe Command Queue
                     │
                     ▼
            MouseActuatorThread
         [OneEuroFilter Smoothing]
                     │
                     ▼
       OS Cursor Actuation (pynput)
```

---

## 📡 Protocol V2 Specification

Every wire packet follows the strict canonical V2 envelope:

```json
{
  "v": 2,
  "type": "move",
  "seq": 1024,
  "ts": 1729771200000,
  "id": "optional-request-id",
  "payload": {
    "dx": 12.5,
    "dy": -3.2,
    "dt": 0.016
  }
}
```

### Supported Message Types:
- **`hello`**: Initial handshake pairing request carrying HMAC token.
- **`welcome`**: Server approval response.
- **`move`**: Relative mouse coordinates (`dx`, `dy`, `dt`).
- **`click`**: Mouse button clicks (`left`, `right`, `middle`, `double`).
- **`scroll`**: Wheel scroll offset (`delta`).
- **`key`**: Keyboard input events (`key`, `modifiers`).
- **`discover` / `offer`**: UDP LAN pairing handshake.
- **`ping` / `pong`**: Heartbeat and connection keep-alive.

---

## 🧪 Test Suite

The test suite covers 100% of the core functionality with high test coverage:

```bash
cd pc/airmouse_py
pytest --cov=. --cov-report=term-missing tests
```

**Results:**
- **46 passing unit and integration tests**
- **88% code coverage**
- Automated testing for replay protection, crypto HMAC validation, TLS X.509 generation, and socket concurrency.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
