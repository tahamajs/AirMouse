#!/usr/bin/env bash
# ==============================================================================
# AirMouse Pro - Universal Cross-Platform Release Packager
# Builds and packages distribution-ready artifacts for:
#   1. Android (APK)
#   2. Windows (x64 Executable + Zip Bundle)
#   3. macOS (Apple Silicon + Intel Universal App Bundle & Binaries)
#   4. Linux (x64 + ARM64 Headless Daemons & Systemd Services)
# ==============================================================================
set -e

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$REPO_DIR/releases"
VERSION="1.0.0"

echo "=============================================================================="
echo "  🚀  Building AirMouse Pro v${VERSION} Release Packages for All Platforms"
echo "=============================================================================="
echo "Workspace: $REPO_DIR"
echo "Output:    $DIST_DIR"
echo ""

# Clean & initialize output directories
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/android"
mkdir -p "$DIST_DIR/windows"
mkdir -p "$DIST_DIR/macos"
mkdir -p "$DIST_DIR/linux"
mkdir -p "/tmp/airmouse_build"

# ------------------------------------------------------------------------------
# 1. WINDOWS PACKAGING (x64)
# ------------------------------------------------------------------------------
echo "📦 [1/4] Building Windows x64 distribution..."
WIN_BUILD_DIR="/tmp/airmouse_build/windows-x64"
rm -rf "$WIN_BUILD_DIR"
mkdir -p "$WIN_BUILD_DIR"

cd "$REPO_DIR/pc/airmouse_go_new"
CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -tags noai -ldflags="-s -w -X main.version=${VERSION}" -o "$WIN_BUILD_DIR/AirMouse-Server.exe" ./cmd/airmouse-server

# Standalone portable EXE
cp "$WIN_BUILD_DIR/AirMouse-Server.exe" "$DIST_DIR/windows/AirMouse-Server-v${VERSION}-windows-x64.exe"

# Copy Web Client for offline web UI serving
cp -R "$REPO_DIR/web-client" "$WIN_BUILD_DIR/web-client"

# Windows 1-click batch launcher
cat << 'EOF' > "$WIN_BUILD_DIR/Start-AirMouse.bat"
@echo off
title AirMouse Pro Server
echo ===================================================
echo     AirMouse Pro Server - Universal Wireless Mouse
echo ===================================================
echo.
echo Starting AirMouse background server...
start "" "%~dp0AirMouse-Server.exe"
echo Server started! Opening web client...
timeout /t 2 /nobreak >nul
start "" "http://localhost:8080/?autoconnect=1"
echo.
echo To pair phone, scan the QR code displayed in the console
echo or navigate to http://<YOUR_PC_IP>:8080 on your phone.
pause
EOF

# Quickstart README for Windows
cat << EOF > "$WIN_BUILD_DIR/README.txt"
AirMouse Pro for Windows (v${VERSION})
========================================

How to run:
1. Double-click "Start-AirMouse.bat" to start the server.
2. The Web Trackpad will open in your default browser.
3. To control from your phone:
   - Connect phone to the same Wi-Fi network.
   - Open browser on phone and go to http://<YOUR-PC-IP>:8080
   - Or install the Android APK from the releases folder!
EOF

# Create ZIP archive
cd "$WIN_BUILD_DIR"
zip -qr "$DIST_DIR/windows/AirMouse-v${VERSION}-windows-x64.zip" .
echo "  ✅ Windows package: releases/windows/AirMouse-v${VERSION}-windows-x64.zip"

# ------------------------------------------------------------------------------
# 2. LINUX PACKAGING (x64 & ARM64)
# ------------------------------------------------------------------------------
echo "📦 [2/4] Building Linux x64 & ARM64 distributions..."

build_linux_pkg() {
    local arch="$1"
    local goarch="$2"
    local target_dir="/tmp/airmouse_build/linux-$arch"
    rm -rf "$target_dir"
    mkdir -p "$target_dir"

    cd "$REPO_DIR/pc/airmouse_go_new"
    CGO_ENABLED=0 GOOS=linux GOARCH="$goarch" go build -tags noai -ldflags="-s -w -X main.version=${VERSION}" -o "$target_dir/airmouse-server" ./cmd/airmouse-server
    
    # Standalone binary
    cp "$target_dir/airmouse-server" "$DIST_DIR/linux/airmouse-server-v${VERSION}-linux-$arch"
    chmod +x "$DIST_DIR/linux/airmouse-server-v${VERSION}-linux-$arch"

    # Copy Web Client
    cp -R "$REPO_DIR/web-client" "$target_dir/web-client"

    # Linux launcher script
    cat << 'EOF' > "$target_dir/start-airmouse.sh"
#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Starting AirMouse Server on Linux..."
"$DIR/airmouse-server" "$@"
EOF
    chmod +x "$target_dir/start-airmouse.sh"

    # Systemd Service Unit
    cat << 'EOF' > "$target_dir/airmouse.service"
[Unit]
Description=AirMouse Pro Wireless Motion Server
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/airmouse-server
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target
EOF

    # Tar.gz archive
    cd "$target_dir"
    tar -czf "$DIST_DIR/linux/AirMouse-v${VERSION}-linux-$arch.tar.gz" .
    echo "  ✅ Linux ($arch) package: releases/linux/AirMouse-v${VERSION}-linux-$arch.tar.gz"
}

build_linux_pkg "x64" "amd64"
build_linux_pkg "arm64" "arm64"

# ------------------------------------------------------------------------------
# 3. macOS PACKAGING (Universal Apple Silicon + Intel & .app Bundle)
# ------------------------------------------------------------------------------
echo "📦 [3/4] Building macOS Universal app & binaries..."
MAC_BUILD_DIR="/tmp/airmouse_build/macos"
rm -rf "$MAC_BUILD_DIR"
mkdir -p "$MAC_BUILD_DIR"

cd "$REPO_DIR/pc/airmouse_go_new"
CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build -tags noai -ldflags="-s -w -X main.version=${VERSION}" -o "$MAC_BUILD_DIR/airmouse-arm64" ./cmd/airmouse-server
CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -tags noai -ldflags="-s -w -X main.version=${VERSION}" -o "$MAC_BUILD_DIR/airmouse-x64" ./cmd/airmouse-server

# Create Universal Fat Binary
lipo -create -output "$MAC_BUILD_DIR/AirMouse-Server-universal" "$MAC_BUILD_DIR/airmouse-arm64" "$MAC_BUILD_DIR/airmouse-x64"
cp "$MAC_BUILD_DIR/AirMouse-Server-universal" "$DIST_DIR/macos/AirMouse-Server-v${VERSION}-macos-universal"
chmod +x "$DIST_DIR/macos/AirMouse-Server-v${VERSION}-macos-universal"

# Construct native AirMouse.app bundle
APP_DIR="$MAC_BUILD_DIR/AirMouse.app"
mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$APP_DIR/Contents/Resources"

cp "$MAC_BUILD_DIR/AirMouse-Server-universal" "$APP_DIR/Contents/MacOS/AirMouse"
chmod +x "$APP_DIR/Contents/MacOS/AirMouse"
cp -R "$REPO_DIR/web-client" "$APP_DIR/Contents/Resources/web-client"

cat << EOF > "$APP_DIR/Contents/Info.plist"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>AirMouse</string>
    <key>CFBundleIdentifier</key>
    <string>com.airmouse.server</string>
    <key>CFBundleName</key>
    <string>AirMouse</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>${VERSION}</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
EOF

# Package macOS Archive
cd "$MAC_BUILD_DIR"
tar -czf "$DIST_DIR/macos/AirMouse-v${VERSION}-macos-universal.tar.gz" AirMouse.app
echo "  ✅ macOS package: releases/macos/AirMouse-v${VERSION}-macos-universal.tar.gz"

# ------------------------------------------------------------------------------
# 4. ANDROID PACKAGING (APK)
# ------------------------------------------------------------------------------
echo "📦 [4/4] Locating Android APK release package..."
APK_SRC=""
if [[ -f "$REPO_DIR/android/app/build/outputs/apk/release/app-release.apk" ]]; then
    APK_SRC="$REPO_DIR/android/app/build/outputs/apk/release/app-release.apk"
    echo "  Found Release APK!"
elif [[ -f "$REPO_DIR/android/app/build/outputs/apk/debug/app-debug.apk" ]]; then
    APK_SRC="$REPO_DIR/android/app/build/outputs/apk/debug/app-debug.apk"
    echo "  Found Debug Signed APK!"
fi

if [[ -n "$APK_SRC" ]]; then
    cp "$APK_SRC" "$DIST_DIR/android/AirMouse-v${VERSION}.apk"
    echo "  ✅ Android package: releases/android/AirMouse-v${VERSION}.apk"
else
    echo "  ⚠️ Warning: No pre-built APK found. Build with ./gradlew assembleDebug or assembleRelease"
fi

# ------------------------------------------------------------------------------
# 5. GENERATE CHECKSUMS & MANIFEST
# ------------------------------------------------------------------------------
echo ""
echo "🔐 Computing SHA256 Checksums..."
cd "$DIST_DIR"
find . -type f ! -name "SHA256SUMS.txt" ! -name "README.md" -exec shasum -a 256 {} + | sort -k 2 > "$DIST_DIR/SHA256SUMS.txt"

cat << EOF > "$DIST_DIR/README.md"
# AirMouse Pro v${VERSION} — Official Multi-Platform Releases

This directory contains standalone, installable, production-ready distributions of **AirMouse Pro** across all supported platforms.

## 📥 Downloads Matrix

| Platform | Target Architecture | Artifact Name | Description |
| :--- | :--- | :--- | :--- |
| **Android** | ARM64 / ARMv7 / x86_64 | [\`AirMouse-v${VERSION}.apk\`](./android/AirMouse-v${VERSION}.apk) | Full Android Application (Sensors, Gestures, Room DB, Dark M3 UI) |
| **Windows** | x86_64 (64-bit) | [\`AirMouse-v${VERSION}-windows-x64.zip\`](./windows/AirMouse-v${VERSION}-windows-x64.zip) | Standalone package with 1-click batch launcher and bundled Web Client |
| **Windows** | x86_64 (64-bit) | [\`AirMouse-Server-v${VERSION}-windows-x64.exe\`](./windows/AirMouse-Server-v${VERSION}-windows-x64.exe) | Portable zero-dependency executable |
| **macOS** | Universal (Apple Silicon + Intel) | [\`AirMouse-v${VERSION}-macos-universal.tar.gz\`](./macos/AirMouse-v${VERSION}-macos-universal.tar.gz) | Native \`AirMouse.app\` Application Bundle |
| **macOS** | Universal (Apple Silicon + Intel) | [\`AirMouse-Server-v${VERSION}-macos-universal\`](./macos/AirMouse-Server-v${VERSION}-macos-universal) | Command-line daemon running natively on M1/M2/M3/M4 & Intel |
| **Linux** | x86_64 (PC / Server) | [\`AirMouse-v${VERSION}-linux-x64.tar.gz\`](./linux/AirMouse-v${VERSION}-linux-x64.tar.gz) | Complete Linux tarball with systemd service and launcher script |
| **Linux** | ARM64 (Raspberry Pi 4/5) | [\`AirMouse-v${VERSION}-linux-arm64.tar.gz\`](./linux/AirMouse-v${VERSION}-linux-arm64.tar.gz) | ARM64 Linux build for embedded devices / single-board computers |

---

## 🛠️ Installation & Usage Guide

### 📱 Android
1. Download [\`AirMouse-v${VERSION}.apk\`](./android/AirMouse-v${VERSION}.apk) directly to your device or install via ADB:
   \`\`\`bash
   adb install -r AirMouse-v${VERSION}.apk
   \`\`\`
2. Launch **AirMouse** on your phone.
3. The app will auto-discover your desktop server on the local Wi-Fi network via UDP.

### 🪟 Windows
1. Download and extract [\`AirMouse-v${VERSION}-windows-x64.zip\`](./windows/AirMouse-v${VERSION}-windows-x64.zip).
2. Double-click \`Start-AirMouse.bat\`.
3. The server starts immediately and opens the Web Trackpad in your browser.

### 🍎 macOS
1. Download and extract [\`AirMouse-v${VERSION}-macos-universal.tar.gz\`](./macos/AirMouse-v${VERSION}-macos-universal.tar.gz):
   \`\`\`bash
   tar -xzf AirMouse-v${VERSION}-macos-universal.tar.gz
   \`\`\`
2. Move \`AirMouse.app\` to your \`/Applications\` folder.
3. Launch \`AirMouse.app\` or run the CLI daemon:
   \`\`\`bash
   ./AirMouse-Server-v${VERSION}-macos-universal
   \`\`\`

### 🐧 Linux
1. Download and unpack the archive for your architecture:
   \`\`\`bash
   tar -xzf AirMouse-v${VERSION}-linux-x64.tar.gz
   cd AirMouse-v${VERSION}-linux-x64
   ./start-airmouse.sh
   \`\`\`
2. (Optional) Install as a system service:
   \`\`\`bash
   sudo cp airmouse-server /usr/local/bin/
   sudo cp airmouse.service /etc/systemd/system/
   sudo systemctl enable --now airmouse
   \`\`\`

---

## 🔐 Checksum Verification

Verify the integrity of downloaded artifacts using SHA-256:
\`\`\`bash
shasum -a 256 -c SHA256SUMS.txt
\`\`\`
EOF

echo ""
echo "=============================================================================="
echo "  🎉 All Release Artifacts Built Successfully!"
echo "=============================================================================="
ls -lh "$DIST_DIR"/*
