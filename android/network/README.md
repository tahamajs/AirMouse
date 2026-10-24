# AirMouse :network module

> **Android Protocol V2 Network Transport Engine**

---

> [!IMPORTANT]
> **Canonical Protocol Specification**: For the complete Protocol V2 wire envelope and message specifications, consult [**docs/PROTOCOL_V2.md**](../../docs/PROTOCOL_V2.md) and the master [**DOCUMENTATION.md**](../../DOCUMENTATION.md).

---

## Overview

Provides coroutine-based WebSocket client, TCP client, and UDP discovery client for the AirMouse Android app adhering strictly to **Protocol V2**:

- `Message.kt` - Serializable Protocol V2 message DTOs (`v: 2`, `type`, `seq`, `ts`, `id`, `payload`)
- `MessageTypes.kt` - Central catalog of all protocol message types and commands
- `ConnectionManager.kt` - Reactive WebSocket/TCP connection state machine
- `UdpDiscoveryClient.kt` - Auto-discovery client broadcasting on port 8082
- `NetworkQualityMonitor.kt` - Latency and jitter telemetry
