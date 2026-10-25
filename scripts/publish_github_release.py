#!/usr/bin/env python3
"""
AirMouse Pro — GitHub Release & Asset Publisher
Creates the official v1.0.0 Release on GitHub (tahamajs/AirMouse)
and uploads all binary artifacts for Android, Windows, macOS, and Linux.
"""

import json
import mimetypes
import os
from pathlib import Path
import sys
import urllib.request
import urllib.error

REPO = "tahamajs/AirMouse"
ROOT_DIR = Path(__file__).resolve().parent.parent
VERSION_FILE = ROOT_DIR / "VERSION"
VERSION = (
    sys.argv[1].lstrip("vV")
    if len(sys.argv) > 1 and not sys.argv[1].startswith("-")
    else (VERSION_FILE.read_text(encoding="utf-8").strip() if VERSION_FILE.exists() else "1.0.1")
)

TAG = f"v{VERSION}"
RELEASE_TITLE = f"AirMouse Pro v{VERSION} — Canonical Multi-Platform Release"

TOKEN = os.getenv("GITHUB_TOKEN")
if not TOKEN:
    # Try reading from git credential helper
    import subprocess
    try:
        p = subprocess.run(
            ["git", "credential", "fill"],
            input="protocol=https\nhost=github.com\n\n".encode("utf-8"),
            capture_output=True,
            check=True
        )
        for line in p.stdout.decode("utf-8").splitlines():
            if line.startswith("password="):
                TOKEN = line.split("=", 1)[1].strip()
                break
    except Exception:
        pass

if not TOKEN:
    print("❌ Error: GITHUB_TOKEN environment variable or git credential not found.")
    sys.exit(1)

HEADERS = {
    "Authorization": f"token {TOKEN}",
    "Accept": "application/vnd.github.v3+json",
    "User-Agent": "AirMouse-Release-Publisher",
}

def build_release_body(ver: str) -> str:
    sha_file = ROOT_DIR / "releases" / "SHA256SUMS.txt"
    sha_text = (
        sha_file.read_text(encoding="utf-8").strip()
        if sha_file.exists()
        else "# Checksums generated during release build"
    )
    return f"""# 🖱️ AirMouse Pro v1.0.1 (Canonical Production Release)

Welcome to the official **AirMouse Pro v{ver}** multi-platform release! AirMouse turns your smartphone or tablet into a sub-millisecond, low-latency spatial air mouse, presentation remote, and multi-touch trackpad for your computer.

---

### 📦 Release Assets & Downloads

| Platform | Installer / Package | Description |
| :--- | :--- | :--- |
| **Android** | [`AirMouse-v{ver}.apk`](https://github.com/tahamajs/AirMouse/releases/download/v{ver}/AirMouse-v{ver}.apk) | Signed Release APK for Android 8.0+ devices |
| **Windows** | [`AirMouse-v{ver}-windows-x64.zip`](https://github.com/tahamajs/AirMouse/releases/download/v{ver}/AirMouse-v{ver}-windows-x64.zip) | Portable bundle with GUI launcher, batch scripts & web-client |
| **Windows** | [`AirMouse-Server-v{ver}-windows-x64.exe`](https://github.com/tahamajs/AirMouse/releases/download/v{ver}/AirMouse-Server-v{ver}-windows-x64.exe) | Standalone portable Go server binary |
| **macOS** | [`AirMouse-v{ver}-macos-universal.tar.gz`](https://github.com/tahamajs/AirMouse/releases/download/v{ver}/AirMouse-v{ver}-macos-universal.tar.gz) | Universal native `AirMouse.app` for Apple Silicon & Intel |
| **macOS** | [`AirMouse-Server-v{ver}-macos-universal`](https://github.com/tahamajs/AirMouse/releases/download/v{ver}/AirMouse-Server-v{ver}-macos-universal) | Fat universal CLI server binary |
| **Linux (x64)** | [`AirMouse-v{ver}-linux-x64.tar.gz`](https://github.com/tahamajs/AirMouse/releases/download/v{ver}/AirMouse-v{ver}-linux-x64.tar.gz) | Complete Linux x86_64 bundle with systemd service |
| **Linux (ARM64)** | [`AirMouse-v{ver}-linux-arm64.tar.gz`](https://github.com/tahamajs/AirMouse/releases/download/v{ver}/AirMouse-v{ver}-linux-arm64.tar.gz) | Raspberry Pi 4/5 & Linux ARM64 bundle |
| **Integrity** | [`SHA256SUMS.txt`](https://github.com/tahamajs/AirMouse/releases/download/v{ver}/SHA256SUMS.txt) | Cryptographic verification hashes |

---

### 🚀 Key Capabilities & Architecture

- **Zero Latency Spatial Pipeline**:
  - Zero Velocity Update (**ZUPT**) drift cancellation.
  - **1€ Filter**: Speed-adaptive jitter low-pass filter for rock-solid stationary cursor stability.
  - **Kalman 2D Filter**: Real-time forward trajectory extrapolator.
  - **Madgwick AHRS**: 9-DOF MARG sensor fusion algorithm.
- **Wire Engine**: Canonical Protocol V2 over **TCP/TLS 1.3** and **WebSocket** with automatic UDP discovery.
- **Zero-Install Web Client**: Open any browser (Safari, Chrome, Edge) on your local network to use the multi-touch trackpad with synthesized audio haptic feedback on iOS!
- **Desktop Dashboard**: Dark-themed CustomTkinter GUI with one-click web browser launcher and instant mobile QR code pairing.
- **Automatic GitHub Updates**: Built-in update checker across Desktop GUI, Go server, and Android client.

---

### 🛡️ SHA-256 Checksums

```text
{sha_text}
```
"""


RELEASE_BODY = build_release_body(VERSION)


def get_or_create_release():
    print(f"🔍 Checking existing releases for {REPO}...")
    url = f"https://api.github.com/repos/{REPO}/releases"
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req) as resp:
        releases = json.loads(resp.read().decode("utf-8"))
        for r in releases:
            if r.get("tag_name") == TAG:
                print(f"✅ Found existing release for {TAG} (ID: {r['id']})")
                return r

    # Create new release
    print(f"🚀 Creating new GitHub Release {TAG} on {REPO}...")
    payload = {
        "tag_name": TAG,
        "target_commitish": "main",
        "name": RELEASE_TITLE,
        "body": RELEASE_BODY,
        "draft": False,
        "prerelease": False,
    }
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={**HEADERS, "Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        rel = json.loads(resp.read().decode("utf-8"))
        print(f"🎉 Created Release: {rel.get('html_url')} (ID: {rel.get('id')})")
        return rel


def upload_asset(release_id: int, file_path: Path):
    if not file_path.exists():
        print(f"⚠️ Asset file not found: {file_path}")
        return

    name = file_path.name
    size = file_path.stat().st_size
    print(f"📤 Uploading {name} ({size / (1024*1024):.2f} MB)...")

    mime_type, _ = mimetypes.guess_type(str(file_path))
    if not mime_type:
        mime_type = "application/octet-stream"
    if name.endswith(".apk"):
        mime_type = "application/vnd.android.package-archive"

    upload_url = f"https://uploads.github.com/repos/{REPO}/releases/{release_id}/assets?name={name}"

    with open(file_path, "rb") as f:
        data = f.read()

    req = urllib.request.Request(
        upload_url,
        data=data,
        headers={
            "Authorization": f"token {TOKEN}",
            "Content-Type": mime_type,
            "Content-Length": str(size),
            "User-Agent": "AirMouse-Release-Publisher",
        }
    )

    try:
        with urllib.request.urlopen(req) as resp:
            res = json.loads(resp.read().decode("utf-8"))
            print(f"   ✅ Uploaded: {res.get('browser_download_url')}")
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8")
        if "already_exists" in err_msg:
            print(f"   ℹ️ Asset {name} already uploaded.")
        else:
            print(f"   ❌ Upload failed for {name}: {e.code} {err_msg}")


def main():
    root = Path(__file__).resolve().parent.parent
    rel_dir = root / "releases"

    release = get_or_create_release()
    rel_id = release["id"]

    # Gather all assets to upload
    assets = [
        rel_dir / "android" / f"AirMouse-v{VERSION}.apk",
        rel_dir / "windows" / f"AirMouse-v{VERSION}-windows-x64.zip",
        rel_dir / "windows" / f"AirMouse-Server-v{VERSION}-windows-x64.exe",
        rel_dir / "macos" / f"AirMouse-v{VERSION}-macos-universal.tar.gz",
        rel_dir / "macos" / f"AirMouse-Server-v{VERSION}-macos-universal",
        rel_dir / "linux" / f"AirMouse-v{VERSION}-linux-x64.tar.gz",
        rel_dir / "linux" / f"AirMouse-v{VERSION}-linux-arm64.tar.gz",
        rel_dir / "SHA256SUMS.txt",
    ]

    for asset in assets:
        upload_asset(rel_id, asset)

    print("\n🎉 GitHub Release published successfully!")
    print(f"🔗 View Release: https://github.com/{REPO}/releases/tag/{TAG}")


if __name__ == "__main__":
    main()
