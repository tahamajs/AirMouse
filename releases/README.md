# AirMouse Pro v1.0.1 — Official Multi-Platform Releases

This directory contains standalone, installable, production-ready distributions of **AirMouse Pro** across all supported platforms.

## 📥 Downloads Matrix

| Platform | Target Architecture | Artifact Name | Description |
| :--- | :--- | :--- | :--- |
| **Android** | ARM64 / ARMv7 / x86_64 | [`AirMouse-v1.0.1.apk`](./android/AirMouse-v1.0.1.apk) | Full Android Application (Sensors, Gestures, Room DB, Dark M3 UI) |
| **Windows** | x86_64 (64-bit) | [`AirMouse-v1.0.1-windows-x64.zip`](./windows/AirMouse-v1.0.1-windows-x64.zip) | Standalone package with 1-click batch launcher and bundled Web Client |
| **Windows** | x86_64 (64-bit) | [`AirMouse-Server-v1.0.1-windows-x64.exe`](./windows/AirMouse-Server-v1.0.1-windows-x64.exe) | Portable zero-dependency executable |
| **macOS** | Universal (Apple Silicon + Intel) | [`AirMouse-v1.0.1-macos-universal.tar.gz`](./macos/AirMouse-v1.0.1-macos-universal.tar.gz) | Native `AirMouse.app` Application Bundle |
| **macOS** | Universal (Apple Silicon + Intel) | [`AirMouse-Server-v1.0.1-macos-universal`](./macos/AirMouse-Server-v1.0.1-macos-universal) | Command-line daemon running natively on M1/M2/M3/M4 & Intel |
| **Linux** | x86_64 (PC / Server) | [`AirMouse-v1.0.1-linux-x64.tar.gz`](./linux/AirMouse-v1.0.1-linux-x64.tar.gz) | Complete Linux tarball with systemd service and launcher script |
| **Linux** | ARM64 (Raspberry Pi 4/5) | [`AirMouse-v1.0.1-linux-arm64.tar.gz`](./linux/AirMouse-v1.0.1-linux-arm64.tar.gz) | ARM64 Linux build for embedded devices / single-board computers |

---

## 🛠️ Installation & Usage Guide

### 📱 Android
1. Download [`AirMouse-v1.0.1.apk`](./android/AirMouse-v1.0.1.apk) directly to your device or install via ADB:
   ```bash
   adb install -r AirMouse-v1.0.1.apk
   ```
2. Launch **AirMouse** on your phone.
3. The app will auto-discover your desktop server on the local Wi-Fi network via UDP.

### 🪟 Windows
1. Download and extract [`AirMouse-v1.0.1-windows-x64.zip`](./windows/AirMouse-v1.0.1-windows-x64.zip).
2. Double-click `Start-AirMouse.bat`.
3. The server starts immediately and opens the Web Trackpad in your browser.

### 🍎 macOS
1. Download and extract [`AirMouse-v1.0.1-macos-universal.tar.gz`](./macos/AirMouse-v1.0.1-macos-universal.tar.gz):
   ```bash
   tar -xzf AirMouse-v1.0.1-macos-universal.tar.gz
   ```
2. Move `AirMouse.app` to your `/Applications` folder.
3. Launch `AirMouse.app` or run the CLI daemon:
   ```bash
   ./AirMouse-Server-v1.0.1-macos-universal
   ```

### 🐧 Linux
1. Download and unpack the archive for your architecture:
   ```bash
   tar -xzf AirMouse-v1.0.1-linux-x64.tar.gz
   cd AirMouse-v1.0.1-linux-x64
   ./start-airmouse.sh
   ```
2. (Optional) Install as a system service:
   ```bash
   sudo cp airmouse-server /usr/local/bin/
   sudo cp airmouse.service /etc/systemd/system/
   sudo systemctl enable --now airmouse
   ```

---

## 🔐 Checksum Verification

Verify the integrity of downloaded artifacts using SHA-256:
```bash
shasum -a 256 -c SHA256SUMS.txt
```
