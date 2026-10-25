---
name: cross-platform-release-builder
description: >-
  Orchestrating multi-platform release builds, compiling Go universal binaries (macOS lipo,
  Windows x64, Linux amd64/arm64), generating Android APKs via Gradle, producing SHA256 checksums,
  and verifying release artifacts.
---

# Cross-Platform Release Builder & Packaging Skill

Use this skill when compiling, packaging, verifying, or automating release artifacts across Android, macOS, Windows, and Linux for the AirMouse repository.

## 1. Release Architecture & Targets

The repository generates verified production artifacts using `scripts/build_all_releases.sh`:

| Platform | Target Artifact | Compilation Strategy |
| :--- | :--- | :--- |
| **Android** | `releases/android/AirMouse-v1.0.0.apk` | Gradle wrapper `:app:assembleRelease` or `:app:assembleDebug` |
| **macOS** | `releases/macos/AirMouse-v1.0.0-macos-universal.tar.gz` | Cross-compile Go for `arm64` and `amd64`, merge using `lipo -create` |
| **Windows** | `releases/windows/AirMouse-v1.0.0-windows-x64.zip` | Cross-compile Go with `GOOS=windows GOARCH=amd64` |
| **Linux** | `releases/linux/AirMouse-v1.0.0-linux-x64.tar.gz` | Cross-compile Go with `GOOS=linux GOARCH=amd64` |
| **Linux ARM** | `releases/linux/AirMouse-v1.0.0-linux-arm64.tar.gz` | Cross-compile Go with `GOOS=linux GOARCH=arm64` |

---

## 2. Compilation Workflows

### 1. macOS Universal Binary Generation
```bash
# Build Apple Silicon binary
GOOS=darwin GOARCH=arm64 go build -ldflags="-s -w" -o build/server-arm64 ./cmd/server
# Build Intel x86_64 binary
GOOS=darwin GOARCH=amd64 go build -ldflags="-s -w" -o build/server-amd64 ./cmd/server
# Stitch into a single Universal 2 Fat Binary
lipo -create -output releases/macos/AirMouse-Server-v1.0.0-macos-universal build/server-arm64 build/server-amd64
```

### 2. Windows Executable with App Metadata
```bash
GOOS=windows GOARCH=amd64 go build -ldflags="-s -w -H=windowsgui" -o releases/windows/AirMouse-Server-v1.0.0-windows-x64.exe ./cmd/server
```

### 3. Android APK Compilation
```bash
cd android
./gradlew assembleRelease
cp app/build/outputs/apk/release/app-release-unsigned.apk ../releases/android/AirMouse-v1.0.0.apk
```

---

## 3. Checksum Verification & Distribution
After building all artifacts, regenerate and verify SHA256 hashes:
```bash
cd releases
shasum -a 256 android/* windows/* macos/* linux/* > SHA256SUMS.txt
shasum -a 256 -c SHA256SUMS.txt
```
All release bundles must include their respective launch scripts (`start.sh`, `Start-AirMouse.bat`) and documentation assets.
