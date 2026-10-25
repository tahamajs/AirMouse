---
name: go-desktop-daemon
description: >-
  Development, optimization, and debugging of the AirMouse Go high-performance desktop server:
  zero-CGO OS input synthesis (macOS CGEvent, Windows SendInput, Linux uinput), concurrency,
  TLS/WSS listeners, and Fyne desktop GUI.
---

# AirMouse Go Desktop Daemon Architecture & Development

Use this skill when developing, refactoring, building, or debugging the Go backend (`pc/airmouse_go_new`) responsible for accepting mobile connections and injecting native OS cursor/keyboard events.

## 1. Native OS Input Injection Architecture

AirMouse Go utilizes zero-CGO or pure syscall wrappers to eliminate external C-toolchain dependencies while achieving sub-millisecond OS event injection:

### macOS (`darwin`)
- Dispatches synthetic events through Quartz Display Services / CoreGraphics:
  - `CGEventCreateMouseEvent(...)`
  - `CGEventCreateScrollWheelEvent2(...)`
  - `CGEventPost(kCGHIDEventTap, event)`
- Requires Accessibility Permissions (`AXIsProcessTrusted()`). If untrusted, prompt user or guide to `System Settings > Privacy & Security > Accessibility`.

### Windows (`windows`)
- Uses Windows User32 `SendInput` API via `golang.org/x/sys/windows`:
  - `INPUT_MOUSE` with flags `MOUSEEVENTF_MOVE`, `MOUSEEVENTF_LEFTDOWN`, `MOUSEEVENTF_WHEEL`.
  - Coordinates normalized to 65535 space (`MOUSEEVENTF_ABSOLUTE`) or relative increments.

### Linux (`linux`)
- Priority 1: `/dev/uinput` kernel driver via ioctl (requires user in `input` group, works under Wayland & X11).
- Priority 2: X11 `XTest` extension via pure Go X11 wire protocol.

---

## 2. Server Concurrency & Event Pipeline

```
[WebSocket Client] ---> [TLS Transport Layer]
                               |
                        [JSON Decoder]
                               | (Validated Protocol V2 Message)
                        [Ring Buffer Queue] (Capacity: 256)
                               |
                        [Worker Event Loop] ---> [OS Input Injection]
```

### Critical Concurrency Rules
1. **Never block the network read loop**: Raw incoming packets are validated and immediately pushed onto a bounded non-blocking channel or ring buffer.
2. **Coalesce rapid micro-moves**: If multiple `move` events arrive faster than the display refresh rate (e.g., 60-144 Hz), accumulate `(dx, dy)` deltas into a single OS injection tick to prevent event tap saturation.
3. **Graceful Shutdown**: Intercept `SIGINT` / `SIGTERM` and close open WebSocket connections cleanly with standard WS 1000 closure codes.

---

## 3. Fyne GUI & Headless Execution Modes

- **GUI Mode**: Runs the 6-tab Fyne dashboard displaying connection status, real-time waveform analytics, pairing QR code, and configuration sliders.
- **Headless CLI Daemon Mode**:
  ```bash
  go run ./cmd/server -headless -port 8080 -tls-cert cert.pem -tls-key key.pem
  ```
- **Cross-Platform Compilation flags**:
  ```bash
  # Universal macOS
  GOOS=darwin GOARCH=arm64 go build -o bin/server-darwin-arm64
  GOOS=darwin GOARCH=amd64 go build -o bin/server-darwin-amd64
  lipo -create -output bin/AirMouse-Server-universal bin/server-darwin-arm64 bin/server-darwin-amd64

  # Windows x64
  GOOS=windows GOARCH=amd64 go build -ldflags="-H=windowsgui" -o bin/AirMouse-Server.exe
  ```

---

## 4. Verification & Testing
- Run test suite: `go test -v ./... -race`
- Test packet latency: Send 10,000 synthetic move events and assert that $p99 < 2\text{ ms}$.
