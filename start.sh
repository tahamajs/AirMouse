#!/usr/bin/env bash
# ============================================================================
# AirMouse Pro - Instant Zero-Config QuickStart Launcher
# ============================================================================
set -e

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_DIR"

# 1. Detect Host Local IP
detect_local_ip() {
    local ip=""
    if [[ "$OSTYPE" == "darwin"* ]]; then
        ip=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)
    fi
    if [[ -z "$ip" ]]; then
        ip=$(python3 -c "import socket; s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM); (s.connect(('8.8.8.8', 80)), print(s.getsockname()[0]), s.close())" 2>/dev/null || true)
    fi
    if [[ -z "$ip" ]]; then
        ip="127.0.0.1"
    fi
    echo "$ip"
}

LOCAL_IP=$(detect_local_ip)
HTTP_PORT=8080
WS_PORT=8081
CLIENT_URL="http://${LOCAL_IP}:${HTTP_PORT}/?autoconnect=1"

# 2. Print Welcome Banner
clear 2>/dev/null || true
echo "============================================================================"
echo "  🖱️   AirMouse Pro — Instant Zero-Config QuickStart"
echo "============================================================================"
echo ""
echo "  👉 Connect instantly from ANY smartphone (iPhone / Android)!"
echo "     Scan the QR code or open this link in Safari or Chrome:"
echo ""
echo "     🔗  ${CLIENT_URL}"
echo ""
echo "  ⚡ Active Endpoints:"
echo "     • Web Trackpad & UI:    http://${LOCAL_IP}:${HTTP_PORT}"
echo "     • WebSocket Stream:     ws://${LOCAL_IP}:${WS_PORT}"
echo "     • Protocol V2 TCP/UDP:  ${LOCAL_IP}:${HTTP_PORT} / 8082"
echo "============================================================================"

# 3. Print QR Code if python qrcode or curl qrenco.de is available
echo ""
echo "  📱 QR Code for Instant Mobile Pairing:"
if python3 -c "import qrcode" 2>/dev/null; then
    python3 -c "import qrcode; qr = qrcode.QRCode(); qr.add_data('${CLIENT_URL}'); qr.print_ascii(invert=True)"
elif command -v curl &>/dev/null; then
    curl -s -m 2 "https://qrenco.de/${CLIENT_URL}" 2>/dev/null || true
fi
echo ""
echo "  Press Ctrl+C at any time to gracefully stop AirMouse."
echo "============================================================================"
echo ""

# 4. Launch Preferred Server (Go by default, fallback to Python)
ENGINE="${1:-go}"

cleanup() {
    echo ""
    echo "[AirMouse] Shutting down background processes..."
    if [[ -n "$HTTP_PID" ]]; then kill "$HTTP_PID" 2>/dev/null || true; fi
    if [[ -n "$SRV_PID" ]]; then kill "$SRV_PID" 2>/dev/null || true; fi
    exit 0
}
trap cleanup SIGINT SIGTERM EXIT

# Start Web Static File Server for web-client/ on HTTP_PORT
python3 -m http.server "$HTTP_PORT" --directory "$REPO_DIR/web-client" &>/dev/null &
HTTP_PID=$!

if [[ "$ENGINE" == "go" && -d "$REPO_DIR/pc/airmouse_go_new" ]]; then
    echo "[AirMouse] Starting high-performance Go Server..."
    cd "$REPO_DIR/pc/airmouse_go_new"
    if [[ -f "bin/airmouse-headless" ]]; then
        ./bin/airmouse-headless -port "$HTTP_PORT" -ws-port "$WS_PORT" &
        SRV_PID=$!
    else
        go run ./cmd/airmouse-server -port "$HTTP_PORT" -ws-port "$WS_PORT" &
        SRV_PID=$!
    fi
else
    echo "[AirMouse] Starting Python Server..."
    cd "$REPO_DIR/pc/airmouse_py"
    python3 -m airmouse.server --host "0.0.0.0" --port "$HTTP_PORT" --ws-port "$WS_PORT" &
    SRV_PID=$!
fi

# Wait for server
wait "$SRV_PID"
