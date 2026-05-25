
---

## `docs/PROTOCOL_SPECIFICATION.md`

```markdown
# Network Protocol Specification

This document defines the communication protocol between the Android app and the PC server.

## Transport Layer

- **Protocol:** TCP (ensures order and error‑free delivery)
- **Port:** 8080 (configurable)
- **Encoding:** UTF‑8
- **Message delimiter:** newline (`\n`)

## Message Format (JSON)

All messages are JSON objects terminated by newline.

### 1. Move Message (phone → PC)

Sent continuously as the user moves the phone. Loss is acceptable.

```json
{"type": "move", "dx": 12.3, "dy": -5.2}