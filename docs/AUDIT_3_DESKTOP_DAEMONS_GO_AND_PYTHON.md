# Deep Technical Audit: Desktop Daemons (Go & Python)

**Audit Date**: October 2026  
**Auditor**: Principal Desktop Systems & OS Kernel Engineer  
**Components**: `server/` (Go High-Performance Server) & `airmouse-core/` (Python Asyncio Server)

---

## 1. Executive Summary

AirMouse provides two desktop server implementations:
1. **Go Desktop Server (`cmd/server`)**: Designed for high throughput, minimal memory footprint (<25 MB RSS), and direct CGO-less OS input synthesis.
2. **Python Server (`airmouse-core`)**: A lightweight reference implementation providing rapid deployment and prototyping.

This audit evaluates the reliability, OS-level security permissions, input synthesis latency, and platform-specific limitations across macOS, Windows, and Linux.

---

## 2. Low-Level OS Input Synthesis Audit

### 2.1 macOS (CoreGraphics & CGEvent Synthesis)
* **Mechanism**: Input injection leverages macOS CoreGraphics C-APIs (`CGEventCreateMouseEvent`, `CGEventPost`, `CGEventSetIntegerValueField`).
* **Vulnerabilities & Limitations**:
  - **TCC Accessibility Permissions**: macOS requires explicit permission in `System Settings -> Privacy & Security -> Accessibility`. If the user updates or rebuilds the Go binary, macOS invalidates the TCC cache entry based on code signature changes, causing silent input failures without error codes.
  - **Retina Scaling & Multi-Monitor Cursor Coordinates**: Direct `CGEventPost` uses points rather than pixels. When spanning multiple displays with mismatched DPI scalings (e.g., 200% Retina built-in display + 100% 1440p external monitor), coordinate transforms in relative mouse mode experience non-linear jumps across monitor boundaries.
  - **Secure Input Mode**: When the user enters a password in macOS (Terminal, Safari password fields), macOS activates `SecureInput`. During this state, synthesized mouse clicks and keypresses are rejected by design. The Go server currently lacks detection for `IsSecureEventInputEnabled()`, leaving the mobile client without visual notification.

### 2.2 Windows (SendInput API & UAC Elevation Boundary)
* **Mechanism**: Windows uses `user32.dll` `SendInput` with `MOUSEINPUT` structures containing `MOUSEEVENTF_MOVE`, `MOUSEEVENTF_ABSOLUTE`, or relative flags.
* **Vulnerabilities & Limitations**:
  - **UIPI (User Interface Privilege Isolation)**: If an administrative or elevated window (e.g., Task Manager, Registry Editor, elevated cmd/PowerShell) has focus, non-elevated processes cannot synthesize inputs into it. The Go server, running with standard user privileges, fails to click or drag inside elevated apps.
  - **Solution**: AirMouse must provide an optional Windows Service wrapper or request `uiAccess=true` in its application manifest signed with a trusted code-signing certificate.

### 2.3 Linux (X11 vs. Wayland Protocol Schism)
* **Mechanism**: Direct `/dev/uinput` kernel module interaction or X11 `XTestFakeMotionEvent`.
* **Critical Flaw**:
  - **Wayland Security Model**: Under modern Linux desktop environments (Ubuntu GNOME 22.04+, Fedora KDE Plasma), Wayland intentionally prohibits unprivileged clients from synthesizing synthetic global input events or capturing screen coordinates.
  - While `/dev/uinput` bypasses Wayland by acting as a virtual hardware device at the kernel level, it requires the user to have read/write access to `/dev/uinput` (custom udev rules), which standard desktop users rarely have configured out of the box. Without udev setup, AirMouse fails immediately on Linux launch.

---

## 3. Concurrency & Networking Performance (Go vs. Python)

### 3.1 Go Daemon Concurrency Architecture
* **Strengths**: The Go server utilizes an event-driven worker pool with bounded channels:
  ```
  WebSocket Ingest ──► [Packet Buffer (1024)] ──► Input Processor ──► OS Event Dispatch
  ```
* **Weaknesses**:
  - **Channel Contention**: In high-frequency multi-touch gestures (e.g., two-finger scroll alongside motion orientation), the ingest channel can fill if the OS event queue is backlogged.
  - **Unbuffered System Call Stalls**: When `CGEventPost` or `SendInput` blocks momentarily on window manager thread locks, the Go scheduler worker thread stalls, propagating latency upstream to the network socket.

### 3.2 Python Asyncio & GIL Limitations
* **Weaknesses**:
  - In `airmouse-core`, incoming packets are handled on the single-threaded Python `asyncio` event loop.
  - The use of `PyAutoGUI` executes synchronous, blocking `time.sleep()` calls inside click handlers (`click()`, `doubleClick()`), starving the event loop and delaying subsequent motion packets.
  - Python's Global Interpreter Lock (GIL) and garbage collector cycles induce periodic **20–60 ms jitter spikes**, rendering the Python server unsuitable for fast gesture tracking or gaming.

---

## 4. Desktop GUI & Tray Architecture (Fyne Framework)

* **Weakness**: The Go GUI utilizes Fyne for cross-platform settings and QR code display. Fyne requires OpenGL/Metal context initialization.
  - On headless remote servers or virtual machines without hardware GPU acceleration, the server crashes on launch unless run with the `--headless` CLI flag.
  - QR code rendering inside the Fyne window lacks high-DPI scaling, occasionally causing mobile cameras to fail barcode scanning on 4K monitors.

---

## 5. Prioritized Recommendations for Desktop Engine

1. **macOS**: Integrate `CGEventPostToPid` or detect `IsSecureEventInputEnabled()` to notify clients when input is blocked.
2. **Linux**: Package a pre-configured `99-airmouse-uinput.rules` udev file and deb/rpm post-install scripts.
3. **Windows**: Add an explicit UAC manifest indicator and support a lightweight Windows Service background daemon.
4. **Python**: Deprecate PyAutoGUI in favor of non-blocking `pynput` direct controller threads with a dedicated queue.
