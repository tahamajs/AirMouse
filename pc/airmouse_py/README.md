# AirMouse Core (`airmouse-core`)

Production-grade, secure, asynchronous Python library and server engine for AirMouse remote input devices.

## Features
- **Protocol V2 Conformance**: Fully compliant with the Android client protocol specification (`v=2`, strict typing, sequence tracking, replay attack window prevention).
- **Asynchronous Dual-Stack Networking**: Asyncio TCP & WebSocket servers with TLS 1.2+ encryption.
- **LAN Discovery**: Threaded UDP discovery responder with challenge-response nonces.
- **Thread-Safe Mouse Actuation**: Decoupled async I/O from hardware/OS cursor emulation using a worker thread and queue.
- **Adaptive Filtering**: 1€ (One Euro) adaptive low-pass filter for jitter-free cursor tracking and RFC 3550-style UDP Jitter Buffer.
- **Zero-Config Pairing**: Built-in CLI with terminal QR code generation for Android client pairing.
- **PEP 561 Compliant**: Includes `py.typed` marker for complete type safety.

## Installation

```bash
pip install airmouse-core
```

Or for local development:
```bash
cd pc/airmouse_py
pip install -e .
```

## Quick Start CLI

Launch the AirMouse server with QR pairing code in terminal:
```bash
airmouse-server --port 8080 --ws-port 8081 --udp-port 8083 --qr
```

## Python API Usage

```python
import asyncio
from airmouse import AirMouseServer, OneEuroFilter, UDPJitterBuffer, AuthManager, ProtocolParser

async def run():
    server = AirMouseServer(
        tcp_port=8080,
        ws_port=8081,
        udp_port=8083,
        enable_tls=True,
    )

    # Attach event callbacks
    server.on_connect(lambda session_id: print(f"Client connected: {session_id}"))
    server.on_move(lambda dx, dy: print(f"Move: dx={dx}, dy={dy}"))
    server.on_click(lambda btn: print(f"Click: {btn}"))
    server.on_disconnect(lambda session_id: print(f"Client disconnected: {session_id}"))

    await server.start()
    try:
        await asyncio.Event().wait()
    finally:
        await server.stop()

if __name__ == "__main__":
    asyncio.run(run())
```

## License
MIT
