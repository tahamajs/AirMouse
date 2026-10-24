# AirMouse PC Server

> **Cross-Platform Remote Control Server Component**  
> *Embedded Systems Laboratory — University of Tehran*

---

> [!IMPORTANT]
> **Documentation Notice**: All desktop server architecture, protocol specifications, build guides, and security models have been consolidated into the project-wide master documentation.
>
> - **System Architecture & Design**: [**DOCUMENTATION.md**](../DOCUMENTATION.md)
> - **Compilation Instructions (Headless, GUI, ONNX)**: [**docs/BUILD.md**](../docs/BUILD.md)
> - **Protocol V2 Specification**: [**docs/PROTOCOL_V2.md**](../docs/PROTOCOL_V2.md)

---

## Server Implementations

### 1. Canonical Go Server (`pc/airmouse_go_new/`)
The production, multi-protocol Go desktop daemon supporting:
- **WebSocket (8081)**, **TCP (8080)**, and **UDP Discovery (8082)**
- **Headless Build (Zero-CGO)** for containers, Linux servers, and cloud relays
- **Fyne Desktop GUI** and embedded Web Dashboard
- **ONNX Runtime** LSTM neural mouse smoothing and 2D Kalman filters
- **Proximity Lock/Unlock** via Bluetooth RSSI fusion and OS session controls

To build and run:
```bash
cd airmouse_go_new
make build
./airmouse-server
```
For zero-CGO headless builds:
```bash
cd airmouse_go_new
CGO_ENABLED=0 go build -tags noai ./cmd/airmouse-server
./airmouse-server --headless
```

### 2. Legacy Python Prototype (`pc/airmouse_server/`)
The original experimental Python/Tkinter prototype used during initial exploratory phases. Retained for historical verification and trace comparison.

---

For complete details, please see [**DOCUMENTATION.md**](../DOCUMENTATION.md).
