# Air Mouse Go Server

![Go Version](https://img.shields.io/badge/Go-1.21+-00ADD8?style=flat&logo=go)
![License](https://img.shields.io/badge/License-MIT-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

A sleek, high‑performance desktop server for the [Air Mouse Android app](https://github.com/yourusername/airmouse-android).  
It receives motion and gesture commands over TCP and translates them into real‑time mouse movements, clicks, and scrolls on your computer.

---

## ✨ Features

- **TCP Command Server** – Asynchronous, multi‑client, with automatic ACK responses.
- **UDP Auto‑Discovery** – Responds to `AIRMOUSE_DISCOVER` broadcasts so the phone can find the server without manual IP entry.
- **mDNS (Bonjour/Zeroconf)** – Advertises itself as `airmouse.local` on the local network.
- **QR Code Pairing** – Generates a QR code containing the endpoint – scan with the Android app to connect instantly.
- **Professional Multi‑Tab GUI** built with [Fyne](https://fyne.io/):
  - **Dashboard** – Server status, quick stats, recent activity
  - **Network** – IP selection, manual override, QR code generation & export
  - **Clients** – List of connected devices with disconnect button
  - **Settings** – Cursor sensitivity, always‑on‑top, theme selection
  - **Logs** – Full live log with filtering (Info/Warn/Error) and export
- **System Tray** – Minimises to tray; dynamic icon colour shows server state.
- **Performance Monitor** – Live CPU and memory usage displayed in the status bar.
- **Connection Watchdog** – Automatically drops idle clients.
- **Graceful Error Handling** – Every network exception is caught and logged; the server never crashes.
- **Cross‑Platform** – Works on Windows, macOS, and Linux.

---

## 🖥️ Screenshots

> *Replace these placeholders with actual screenshots of your app.*

![Dashboard](docs/dashboard.png)
![Network Tab](docs/network.png)
![Clients Tab](docs/clients.png)
![Settings Tab](docs/settings.png)
![Logs Tab](docs/logs.png)

---

## 🚀 Quick Start

### Prerequisites
- **Go 1.21** or later
- **Git** (optional, for cloning)

### Run directly
```bash
git clone https://github.com/yourusername/airmouse-go.git
cd airmouse-go
go mod tidy
go run .
```

The GUI will open, and you can start the server from the Dashboard tab or the tray menu.

---

## 📦 Installation

### From Source
```bash
git clone https://github.com/yourusername/airmouse-go.git
cd airmouse-go
go build -o airmouse-server .
./airmouse-server
```

### Pre‑built Binaries
Download the latest release for your OS from the [Releases page](https://github.com/yourusername/airmouse-go/releases).

---

## ⚙️ Configuration

All settings are stored in a `config.json` file created on first run (or you can create one manually). Example:

```json
{
  "host": "0.0.0.0",
  "port": 8080,
  "discovery_port": 8081,
  "sensitivity": 0.5,
  "accent_color": "#007acc",
  "selected_ip": "",
  "manual_ip_enabled": false,
  "manual_ip_value": "",
  "mDNS_name": "airmouse",
  "always_on_top": false
}
```

| Key | Description |
|-----|-------------|
| `host` | TCP server bind address (usually `0.0.0.0`) |
| `port` | TCP port for command traffic |
| `discovery_port` | UDP port for auto‑discovery |
| `sensitivity` | Mouse movement multiplier (0.2 – 2.0) |
| `mDNS_name` | Hostname advertised via Bonjour |
| `always_on_top` | Keep the window above others |

Settings can also be changed from the **Settings tab** inside the GUI.

---

## 🧱 Architecture

```
airmouse-go/
├── main.go              # Entry point
├── go.mod / go.sum      # Dependencies
├── config/
│   └── config.go        # Loads/saves JSON configuration
├── control/
│   └── mouse.go         # Mouse movement, clicks, scrolling
├── server/
│   ├── tcp.go           # TCP server with ACK & watchdog
│   ├── udp.go           # UDP discovery responder
│   └── mdns.go          # mDNS (Bonjour) advertiser
└── ui/
    └── app.go           # Fyne GUI – all tabs, widgets, callbacks
```

Each module is independent and can be reused in other projects.

---

## 📡 Protocol (Android ↔ Server)

The server speaks a simple JSON‑line protocol over TCP. Every message is a single line:

```json
{"type":"move","dx":1.2,"dy":-0.5}
```

Supported message types:

| Type | Direction | Payload | Description |
|------|-----------|---------|-------------|
| `move` | Phone → Server | `dx`, `dy` | Cursor displacement |
| `click` | Phone → Server | `id` | Left click (ACK expected) |
| `doubleclick` | Phone → Server | `id` | Double click |
| `rightclick` | Phone → Server | `id` | Right click |
| `scroll` | Phone → Server | `id`, `delta` | Scroll (positive = up) |
| `hello` | Phone → Server | `name` | Device identification |
| `ack` | Server → Phone | `id` | Acknowledgment |

The server includes a 10‑second idle watchdog; if no data is received from a client, it is disconnected.

---

## 🔧 Building from Source

```bash
go mod tidy
go build -ldflags="-s -w" -o airmouse-server .
```

This produces a single, statically‑linked binary.

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 🙏 Acknowledgements

- [Fyne](https://fyne.io/) for the beautiful cross‑platform GUI toolkit
- [robotgo](https://github.com/go-vgo/robotgo) for mouse control
- [hashicorp/mdns](https://github.com/hashicorp/mdns) for Zeroconf support
- [skip2/go-qrcode](https://github.com/skip2/go-qrcode) for QR code generation
- [getlantern/systray](https://github.com/getlantern/systray) for the system tray
- [gopsutil](https://github.com/shirou/gopsutil) for performance metrics

---

**University of Tehran – Embedded Systems**