# Air Mouse PC Server

This folder contains the Python PC side of the Air Mouse project. It receives motion data from the Android app over TCP, moves the mouse with `pyautogui`, and supports click, double click, right click, and scroll actions. The package also includes a modern dark-mode GUI, UDP discovery, QR code pairing, persistent configuration, and a Perfetto trace analysis helper.

## Folder Contents

| File | Description |
| --- | --- |
| `gui.py` | Recommended GUI server with live log, start/stop controls, sensitivity slider, QR code preview, and IP selection. |
| `server.py` | Console server with the same TCP and UDP behavior, suitable for headless use. |
| `run.py` | Launcher that checks dependencies and starts the GUI. |
| `run.sh` | macOS/Linux helper script for launching the GUI. |
| `run.bat` | Windows helper script for launching the GUI. |
| `requirements.txt` | Python dependencies used by the server and analysis tools. |
| `config.json` | Persistent configuration for port, discovery port, sensitivity, selected IP, and logging. |
| `perfetto_analyzer.py` | Perfetto helper used to answer the tracing questions in the assignment. |

## Quick Start

### 1. Install Python

- Windows: download Python 3.8+ from [python.org](https://python.org) and enable Add to PATH.
- macOS: install with `brew install python` or use the official installer.
- Linux: install `python3` and `python3-pip` through your package manager.

### 2. Start the server

#### GUI server

```bash
cd pc
python run.py
```

You can also run `run.bat` on Windows or `./run.sh` on macOS/Linux.

The GUI includes:

- automatic local IPv4 detection,
- manual IP override with persistence,
- QR code preview for fast Android pairing,
- UDP discovery responses containing the selected IP and TCP port,
- live server logs and gesture counters,
- a sensitivity slider for cursor speed.

#### Console server

```bash
cd pc
pip install -r requirements.txt
python server.py
```

### 3. Pair with the Android app

- Connect the phone and PC to the same Wi-Fi network.
- Use the discovery screen or QR scanner in the Android app, or type the IP manually.
- Calibrate the phone before first use.
- Tap Start Air Mouse and move the phone to control the cursor.

## File Details

### `gui.py`

Features:

- dark theme with accent colour,
- start/stop controls,
- live connection log,
- sensitivity slider,
- automatic IP detection,
- manual IP override,
- QR code export,
- UDP discovery responder,
- support for move, click, double click, right click, and scroll.

### `server.py`

Features:

- reads `config.json`,
- logs to console and file,
- uses the same TCP command format as the GUI server,
- replies to discovery requests with the current IP and port.

Example `config.json`:

```json
{
  "host": "0.0.0.0",
  "port": 8080,
  "discovery_port": 8081,
  "sensitivity": 0.5,
  "selected_ip": "192.168.1.106",
  "log_level": "INFO",
  "log_file": "airmouse.log"
}
```

### `run.py`

Checks that the required Python packages are installed and launches the GUI.

### `perfetto_analyzer.py`

This script helps answer the Perfetto questions in the report by querying a trace file and printing human-readable summaries.

## Requirements

- `pyautogui` for mouse control.
- `pandas` for the trace analyzer.
- `perfetto` for trace processing.
- `qrcode` and `Pillow` for endpoint QR codes.
- `netifaces` for listing local network interfaces in the GUI.

## Testing

You can test the TCP server manually with `nc` or `telnet`:

```bash
echo '{"type":"click","id":1}' | nc localhost 8080
```

The server should send an ACK for the critical packet.

## Troubleshooting

- If Android cannot connect, verify the IP, port, and firewall rules.
- If the mouse does not move on macOS, grant Accessibility permission to Python or Terminal.
- If discovery does not work, make sure both devices are on the same Wi-Fi network.
- If the GUI says the camera is unavailable, the Android QR scanner should fall back to manual IP entry.

## Summary

The PC folder is now intended to be a release-ready desktop companion for the Android app: easy to launch, easy to configure, and easy to pair.
