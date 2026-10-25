---
name: python-airmouse-core
description: >-
  Development, packaging, and maintenance of the airmouse-core Python package and server:
  asyncio event loops, WebSocket/UDP networking, PyAutoGUI/pynput input simulation, ANSI QR code
  pairing, and PyPI release workflows.
---

# Python AirMouse Core Architecture & Server Skill

Use this skill when developing or testing the Python implementation of the AirMouse server located in `pc/` or distributed on PyPI as `airmouse-core`.

## 1. Architecture Overview

The Python server operates on Python 3.9+ using asynchronous I/O (`asyncio`):
- **Network Engine**: `websockets` asynchronous server over secure TLS/WSS.
- **Discovery Daemon**: Background UDP broadcast/multicast beacon emitting server metadata (`airmouse-beacon:port:name`) for instant zero-configuration mobile discovery.
- **OS Input Dispatcher**: Cross-platform event injection using `pynput` with fallback to `pyautogui`.
- **Zero-Config Pairing**: Renders dynamic 2D ANSI QR codes directly in the terminal so mobile users can scan with their phone camera to pair instantly.

---

## 2. Core Patterns & Implementation Rules

### 1. Non-Blocking Event Loops
Never run blocking system calls inside the asyncio event loop. Delegate OS input synthesis to a worker thread if the OS API can block:
```python
import asyncio
from pynput.mouse import Controller

mouse = Controller()

async def handle_move(dx: float, dy: float):
    # Offload OS event if rate-limiting requires it
    loop = asyncio.get_running_loop()
    await loop.run_in_executor(None, mouse.move, dx, dy)
```

### 2. Monotonic Sequence & HMAC Validation
```python
import hmac
import hashlib
import time

def verify_packet(packet: dict, session: dict) -> bool:
    # 1. Version check
    if packet.get("v") != 2:
        return False
        
    # 2. Replay window
    now_ms = int(time.time() * 1000)
    if abs(now_ms - packet.get("ts", 0)) > 5000:
        return False
        
    # 3. Monotonic seq
    seq = packet.get("seq", 0)
    if seq <= session.get("last_seq", -1):
        return False
    session["last_seq"] = seq
    
    return True
```

### 3. Terminal QR Code Pairing
Uses `qrcode` package with ANSI escape blocks for cross-platform terminal display:
```python
import qrcode
import io

def print_terminal_qr(pairing_url: str):
    qr = qrcode.QRCode(border=1)
    qr.add_data(pairing_url)
    qr.make(fit=True)
    f = io.StringIO()
    qr.print_ascii(out=f, invert=True)
    print(f.getvalue())
```

---

## 3. Testing & PyPI Packaging
- Run test suite: `pytest -v pc/tests/`
- Build distribution wheels:
  ```bash
  python3 -m build
  twine check dist/*
  ```
- Ensure type annotations adhere to `mypy --strict`.
