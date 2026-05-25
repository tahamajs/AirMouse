# Air Mouse – PC Server Detailed Usage (Complete Guide)

This document provides a **complete, in‑depth explanation** of the Python PC server that receives motion data from the Android app and controls the mouse cursor. It covers both versions of the server (`gui.py` and `server.py`), the configuration file, and step‑by‑step instructions for running the server on different operating systems. All features, logging, and troubleshooting are explained in detail.

---

## 📖 Table of Contents

- [Air Mouse – PC Server Detailed Usage (Complete Guide)](#air-mouse--pc-server-detailed-usage-complete-guide)
  - [📖 Table of Contents](#-table-of-contents)
  - [Overview of the PC Server](#overview-of-the-pc-server)
  - [Version 1: `gui.py` – Dark Mode GUI Server (Recommended)](#version-1-guipy--dark-mode-gui-server-recommended)
    - [User Interface Elements](#user-interface-elements)
    - [Behaviour \& Features](#behaviour--features)
    - [Why Use the GUI Version](#why-use-the-gui-version)
  - [Version 2: `server.py` – Console Server](#version-2-serverpy--console-server)
    - [Configuration File (`config.json`)](#configuration-file-configjson)
    - [Logging](#logging)
    - [Why Use the Console Version](#why-use-the-console-version)
  - [Running the Server – Complete Instructions](#running-the-server--complete-instructions)
    - [Prerequisites](#prerequisites)
    - [Running on macOS / Linux](#running-on-macos--linux)
      - [Using the unified launcher (recommended)](#using-the-unified-launcher-recommended)
      - [Directly running the GUI server](#directly-running-the-gui-server)
      - [Directly running the console server](#directly-running-the-console-server)
    - [Running on Windows](#running-on-windows)
      - [Using the batch file (recommended)](#using-the-batch-file-recommended)
      - [Directly running the GUI server](#directly-running-the-gui-server-1)
      - [Directly running the console server](#directly-running-the-console-server-1)
    - [What to expect when the server starts](#what-to-expect-when-the-server-starts)
  - [Understanding the Server Log](#understanding-the-server-log)
  - [Common Operations \& Tips](#common-operations--tips)
    - [Changing the sensitivity while the server is running](#changing-the-sensitivity-while-the-server-is-running)
    - [Stopping the server](#stopping-the-server)
    - [Viewing the log file (console server only)](#viewing-the-log-file-console-server-only)
    - [Running the console server as a background service (Linux/macOS)](#running-the-console-server-as-a-background-service-linuxmacos)
    - [Testing the server without the Android app](#testing-the-server-without-the-android-app)
    - [Firewall considerations](#firewall-considerations)
    - [Performance notes](#performance-notes)
  - [Summary Table – Which Server to Choose?](#summary-table--which-server-to-choose)

---

## Overview of the PC Server

The PC server is a Python application that:
- Listens for incoming TCP connections on a configurable port (default 8080).
- Receives JSON messages from the Android app.
- Parses the messages and executes corresponding mouse actions using the `pyautogui` library.
- Sends back acknowledgements (ACK) for critical messages (click, double‑click, right‑click, scroll) to ensure reliable delivery.

The server is designed to be **lightweight** and **cross‑platform**. It uses `asyncio` for high‑performance asynchronous networking and, in the GUI version, `tkinter` for a simple graphical interface.

---

## Version 1: `gui.py` – Dark Mode GUI Server (Recommended)

### User Interface Elements

When you launch `gui.py`, a window appears with the following components:

| Element | Description |
|---------|-------------|
| **Title bar** | “✈️ Air Mouse Server” |
| **Status indicator** | A coloured dot (red = stopped, light green = running) with text. |
| **Connection Log** | A scrollable text area that shows all server events (connections, disconnections, clicks, scrolls, errors). |
| **Start Server button** | Green button (▶) – starts the TCP listener. |
| **Stop Server button** | Grey button (⏹) – stops the listener and closes all connections. |
| **Sensitivity slider** | Scale from 0.2 to 2.0, with a label showing the current value. Changes take effect immediately. |
| **Footer** | “University of Tehran – Embedded Systems Exercise” |

### Behaviour & Features

- **Start Server**:  
  When you click **Start Server**, the GUI launches an `asyncio` event loop in a background thread. The server begins listening on `0.0.0.0:8080` (all network interfaces). The status dot turns green, and the log shows `🚀 Server listening on 0.0.0.0:8080`.

- **Stop Server**:  
  Clicking **Stop Server** shuts down the event loop, closes the listening socket, and disconnects any active clients. The status returns to red, and the log shows `🛑 Server stopped by user`.

- **Connection Handling**:  
  When an Android client connects, the log displays `✅ Connected: (<IP>, <port>)`. The server then reads JSON lines indefinitely. For each message:
  - `move` – moves the mouse cursor using `pyautogui.moveRel(dx, dy)`.
  - `click` – performs a left click, sends an ACK, logs `🖱️ Click`.
  - `doubleclick` – performs a double click, sends an ACK, logs `🖱️🖱️ Double-click`.
  - `rightclick` – performs a right click, sends an ACK, logs `🖱️ Right-click`.
  - `scroll` – performs a scroll (positive delta = down), sends an ACK, logs `📜 Scroll 1` (or `-1`).

- **Sensitivity Slider**:  
  The slider value controls a multiplier applied to `dx` and `dy` before moving the cursor. The default is 0.5. Moving the slider updates `CONFIG["sensitivity"]` and the `mouse.sensitivity` attribute. The change is immediate and does not require a server restart.

- **Log Area**:  
  All logs are printed to the text area and automatically scrolled to the bottom. The log is **not** saved to a file automatically (unlike the console server).

- **Error Handling**:  
  If a client disconnects unexpectedly, the log shows `🔌 Disconnected: (<IP>, <port>)`. If the server encounters an internal error (e.g., malformed JSON), the error is logged and the connection is closed gracefully.

### Why Use the GUI Version

- **User‑friendly** – No need to remember command‑line arguments or edit configuration files.
- **Live feedback** – See every click and scroll as it happens.
- **Easy sensitivity tuning** – Slider provides instant visual feedback.
- **Dark theme** – Comfortable for extended use.
- **Ideal for demonstrations** – The log area clearly shows activity for video recording.

---

## Version 2: `server.py` – Console Server

The console server is a **headless** version that runs in a terminal. It reads settings from a JSON configuration file and logs to both the console and a file.

### Configuration File (`config.json`)

The file must be placed in the same directory as `server.py`. If it does not exist, the server creates it with default values.

```json
{
    "host": "0.0.0.0",
    "port": 8080,
    "sensitivity": 0.5,
    "log_level": "INFO",
    "log_file": "airmouse.log"
}
```

| Field | Type | Description | Allowed values |
|-------|------|-------------|----------------|
| `host` | string | IP address to bind to. `0.0.0.0` listens on all interfaces. | Any valid IP, or `0.0.0.0` |
| `port` | integer | TCP port number. | 1024–65535 (privileged ports <1024 may need admin) |
| `sensitivity` | float | Global mouse sensitivity multiplier (0.2–2.0). | 0.2, 0.5, 1.0, etc. |
| `log_level` | string | Logging verbosity. | `DEBUG`, `INFO`, `WARNING`, `ERROR` |
| `log_file` | string | Path to the log file (relative or absolute). | e.g., `"airmouse.log"` |

**Important:** After editing `config.json`, you must restart the server for changes to take effect (unlike the GUI version which applies sensitivity changes instantly).

### Logging

- **Console output**: All log messages are printed to the terminal with timestamps, level, and message.
- **File logging**: Messages are also appended to the file specified in `log_file` (default `airmouse.log`). The file grows indefinitely; you may rotate it manually.
- **Log levels**:
  - `DEBUG` – verbose, includes every received message (use only for debugging).
  - `INFO` – normal operation: connections, clicks, scrolls, disconnections.
  - `WARNING` – non‑critical errors (e.g., malformed JSON from a client).
  - `ERROR` – critical issues that may cause a client disconnection.

Example log entry:
```
2025-05-26 15:30:01,123 - INFO - Server listening on 0.0.0.0:8080
2025-05-26 15:30:05,456 - INFO - Connected: ('192.168.1.15', 54321)
2025-05-26 15:30:05,789 - INFO - Click: left
2025-05-26 15:30:06,012 - INFO - Scroll: 1
2025-05-26 15:30:10,234 - INFO - Disconnected: ('192.168.1.15', 54321)
```

### Why Use the Console Version

- **Lightweight** – No GUI dependencies (`tkinter` may not be installed on some headless servers).
- **Scriptable** – Can be run as a background service or in a Docker container.
- **Persistent logs** – All activity is saved to a file for later analysis.
- **Configurable** – Change host, port, log level, and file without modifying code.
- **Ideal for automated testing** – The console output can be parsed by other scripts.

---

## Running the Server – Complete Instructions

### Prerequisites

Before running the server, ensure you have:

- **Python 3.8+** installed.
- **`pyautogui`** installed (automatically by `run.py` or manually via `pip install pyautogui`).
- On **macOS**: granted Accessibility permission to your terminal app or Python.
- On **Linux**: `tkinter` may need to be installed (`sudo apt install python3-tk` for Ubuntu).
- On **Windows**: No special permissions needed (but firewall may ask to allow Python).

### Running on macOS / Linux

#### Using the unified launcher (recommended)
1. Open a terminal.
2. Navigate to the `pc` folder:
   ```bash
   cd AirMouse-Ultimate/pc
   ```
3. Make the launcher executable (one time):
   ```bash
   chmod +x run.sh
   ```
4. Run it:
   ```bash
   ./run.sh
   ```
   This script installs dependencies (if missing) and launches `gui.py`.

#### Directly running the GUI server
```bash
python3 gui.py
```

#### Directly running the console server
```bash
python3 server.py
```
The server will run until you press `Ctrl+C`.

### Running on Windows

#### Using the batch file (recommended)
1. Open File Explorer and navigate to the `pc` folder.
2. Double‑click **`run.bat`**.
   - A command prompt window opens, installs dependencies (if needed), and launches `gui.py`.

#### Directly running the GUI server
Open Command Prompt or PowerShell in the `pc` folder and type:
```cmd
python gui.py
```

#### Directly running the console server
```cmd
python server.py
```

### What to expect when the server starts

- **GUI version**: A window appears. Click **Start Server**. The log shows `🚀 Server listening on 0.0.0.0:8080`.
- **Console version**: The terminal shows `Server listening on 0.0.0.0:8080` (INFO level).

Now the server is ready to accept connections from the Android app.

---

## Understanding the Server Log

The log entries follow a consistent format with emojis (GUI version) or plain text (console version). Here is a breakdown of common log messages:

| Log entry | Meaning |
|-----------|---------|
| `✅ Connected: ('192.168.1.10', 54321)` | A client (phone) has established a TCP connection. The IP is the phone’s local IP. |
| `🖱️ Click` | A `click` message was received and executed. |
| `🖱️🖱️ Double-click` | A `doubleclick` message received. |
| `🖱️ Right-click` | A `rightclick` message received. |
| `📜 Scroll 1` | A `scroll` message with `delta=1` (scroll down). |
| `📜 Scroll -1` | Scroll up. |
| `❌ Error: ...` | An exception occurred while handling a client. The error message is included. |
| `🔌 Disconnected: ('192.168.1.10', 54321)` | The client closed the connection or the connection was lost. |
| `⚙️ Sensitivity changed to 1.20` (GUI only) | User adjusted the sensitivity slider. |
| `🚀 Server listening on 0.0.0.0:8080` | Server started successfully. |
| `🛑 Server stopped by user` (GUI) or `Shutting down...` (console) | Server was stopped gracefully. |

If you see `WARNING: Invalid JSON from ...`, the phone sent a malformed message – this may indicate a bug in the Android app or network corruption.

---

## Common Operations & Tips

### Changing the sensitivity while the server is running

- **GUI**: Move the slider – the new value is applied immediately.
- **Console**: You must edit `config.json` and restart the server.

### Stopping the server

- **GUI**: Click **Stop Server**, then close the window.
- **Console**: Press `Ctrl+C` in the terminal.

### Viewing the log file (console server only)

The log file is specified in `config.json` (default `airmouse.log`). Use any text editor or `tail -f airmouse.log` on Linux/macOS to watch live.

### Running the console server as a background service (Linux/macOS)

```bash
nohup python server.py > /dev/null 2>&1 &
```

To stop, find the process ID (`ps aux | grep server.py`) and `kill <PID>`.

### Testing the server without the Android app

You can use `telnet` or `netcat` to send test messages:

```bash
telnet localhost 8080
```
Then type a JSON message, e.g.:
```json
{"type":"move","dx":10,"dy":10}
```
Press Enter. The cursor should move.

### Firewall considerations

If the Android app cannot connect, ensure your firewall allows incoming connections on the configured port (8080). On Windows, you may see a pop‑up asking to allow Python – click “Allow”. On macOS, go to System Settings → Network → Firewall → Add Python. On Linux, you may need to open the port with `sudo ufw allow 8080` (if using UFW).

### Performance notes

- The server is non‑blocking and can handle multiple clients simultaneously, but Air Mouse is designed for a single client.
- On a modern PC, the server adds less than 1 ms of latency to the mouse movement.
- The GUI server uses slightly more CPU due to `tkinter` updates, but still negligible.

---

## Summary Table – Which Server to Choose?

| Feature | `gui.py` (GUI) | `server.py` (Console) |
|---------|----------------|------------------------|
| User interface | Dark mode window with start/stop, slider, log | Terminal only |
| Sensitivity change | Immediate via slider | Requires edit of `config.json` + restart |
| Log persistence | In‑memory only (not saved) | Logs to file + console |
| Configuration | Hardcoded in `CONFIG` dict | Via `config.json` |
| Best for | Daily use, demonstrations, debugging | Headless setups, servers, automation |

---

This guide covers **everything** about the PC server – from the visual elements of the GUI to the configuration options of the console version. Use it to understand, operate, and troubleshoot the Air Mouse server effectively.