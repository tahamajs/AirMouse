# 🖱️ Air Mouse Go Server

[![Go Report Card](https://goreportcard.com/badge/github.com/yourusername/airmouse-go)](https://goreportcard.com/report/github.com/yourusername/airmouse-go)
[![Go Version](https://img.shields.io/badge/Go-1.21+-00ADD8?logo=go)](https://go.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/yourusername/airmouse-go)](https://github.com/yourusername/airmouse-go/releases)

A **complete desktop server** for the [Air Mouse Android app](https://github.com/yourusername/airmouse-android), written in pure Go.  
It turns your computer into a remote‑controllable pointer device, perfect for presentations, media centres, or embedded systems.

---

## ✨ Highlights

- 🚀 **High‑performance TCP server** – handles multiple clients concurrently with goroutines.
- 🔍 **Zero‑configuration discovery** – UDP broadcast + mDNS (Bonjour) so phones find the server automatically.
- 📱 **QR code pairing** – generate and save a QR code for instant connection.
- 🎨 **Beautiful multi‑tab GUI** built with [Fyne](https://fyne.io/) – Dashboard, Network, Clients, Settings, Logs.
- 🔔 **System tray** with real‑time status icon (green/red).
- 📊 **Live performance monitor** (CPU / memory).
- ⚙️ **Fully configurable** via JSON file or GUI.
- 🧱 **Modular architecture** – use it as a standalone app **or** as a library in your own Go project.
- 🛡️ **Robust error handling** – never crashes on network failures, watchdog disconnects idle clients.

---

## 📦 Installation

### As a standalone application

```bash
go install github.com/yourusername/airmouse-go@latest
```

Then run:

```bash
airmouse-go
```

### As a library

```bash
go get github.com/yourusername/airmouse-go
```

Then import the packages you need in your own code.

---

## 🚀 Quick Start

1. **Start the server** – launch the application or run `go run .`.
2. **Open the Android app** – scan the QR code from the **Network** tab, or enter the IP:port manually.
3. **Move your phone** – it controls the mouse cursor instantly.
4. **Gestures**: quick twist → click, fast up/down → scroll.

---

## 🧱 Library Usage

You can embed the TCP server and mouse controller directly into your own Go program without the GUI.

```go
package main

import (
    "airmouse-go/config"
    "airmouse-go/control"
    "airmouse-go/server"
)

func main() {
    cfg := &config.Config{
        Host: "0.0.0.0",
        Port: 8080,
        Sensitivity: 0.5,
    }

    mouse := control.NewMouseController(cfg.Sensitivity)
    tcp := server.NewTCPServer(
        cfg.Host, cfg.Port, mouse,
        logFunc,    // func(string)
        statsFunc,  // func(int,int,int,int)
        connCb,     // func([]string)
    )
    tcp.Start()

    // Start UDP discovery as well
    go server.StartUDPDiscovery(8081, func() string { return "192.168.1.10" }, logFunc)

    // Block forever
    select {}
}
```

All modules (`config`, `control`, `server`) are self‑contained and can be used independently.

---

## 📡 Protocol

The server speaks a simple JSON‑line protocol over TCP.  
Each message is a single line terminated by `\n`.

### Client → Server

| Type | Payload | Description |
|------|---------|-------------|
| `move` | `dx` (float), `dy` (float) | Move the mouse cursor |
| `click` | `id` (string) | Left click (ACK expected) |
| `doubleclick` | `id` (string) | Double click |
| `rightclick` | `id` (string) | Right click |
| `scroll` | `id` (string), `delta` (int) | Scroll (positive = up) |
| `hello` | `name` (string) | Identify the device |

### Server → Client

| Type | Payload | Description |
|------|---------|-------------|
| `ack` | `id` (string) | Confirmation of a command |

---

## ⚙️ Configuration

A `config.json` file is created automatically on first run.  
You can also provide it manually:

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

All settings can be changed from the **Settings** tab in the GUI.

---

## 🏗️ Building from Source

```bash
git clone https://github.com/yourusername/airmouse-go.git
cd airmouse-go
go mod tidy
go build -o airmouse-server .
```

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!  
Feel free to check the [issues page](https://github.com/yourusername/airmouse-go/issues).

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📜 License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

- [Fyne](https://fyne.io/) – cross‑platform GUI
- [robotgo](https://github.com/go-vgo/robotgo) – mouse control
- [hashicorp/mdns](https://github.com/hashicorp/mdns) – Zeroconf/Bonjour
- [skip2/go-qrcode](https://github.com/skip2/go-qrcode) – QR code generation
- [getlantern/systray](https://github.com/getlantern/systray) – system tray
- [gopsutil](https://github.com/shirou/gopsutil) – performance metrics

---

**University of Tehran – Embedded Systems**