# AirMouse :network module

Provides coroutine-based TCP client and UDP discovery client for the AirMouse Android app.

Files:
- `Message.kt` - serializable message DTOs
- `TcpClient.kt` - coroutine-friendly TCP client (basic)
- `UdpDiscoveryClient.kt` - sends `AIRMOUSE_DISCOVER` and waits for `DiscoveryResponse` JSON

Usage (example):

```kotlin
val client = TcpClient("192.168.1.10", 8080, onMessage = { line -> /* handle */ })
client.start()
// send: use coroutine scope to call sendMessage
```

The module is intentionally small and focused; further features (ACK handling, pending receipts, reconnection policy) will be added in follow-up steps.
