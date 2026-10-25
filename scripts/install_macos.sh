#!/bin/bash
set -e

# ==============================================================================
# AirMouse Pro — Automated macOS Installer
# Supports Apple Silicon (M1/M2/M3/M4) and Intel (x86_64) Macs
# ==============================================================================

BOLD='\033[1m'
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo -e "${CYAN}${BOLD}🖱️  AirMouse Pro — macOS Automated Installer${NC}"
echo -e "${CYAN}====================================================${NC}"
echo ""

# 1. Detect OS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo -e "${RED}❌ Error: This installer is designed specifically for macOS.${NC}"
    exit 1
fi

ARCH=$(uname -m)
echo -e "🖥️  Detected Architecture: ${BOLD}${ARCH}${NC}"

VERSION="v1.0.1"
TAG_NAME="v1.0.1"
RELEASE_URL="https://github.com/tahamajs/AirMouse/releases/download/${TAG_NAME}"
TAR_NAME="AirMouse-${TAG_NAME}-macos-universal.tar.gz"
DOWNLOAD_URL="${RELEASE_URL}/${TAR_NAME}"

TMP_DIR=$(mktemp -d /tmp/airmouse_install_XXXXXX)
cd "$TMP_DIR"

echo -e "⬇️  Downloading AirMouse Universal Release (${BOLD}${VERSION}${NC})..."
if ! curl -fSL --progress-bar "$DOWNLOAD_URL" -o "$TAR_NAME"; then
    echo -e "${RED}❌ Download failed from: ${DOWNLOAD_URL}${NC}"
    rm -rf "$TMP_DIR"
    exit 1
fi

echo -e "📦 Extracting package..."
tar -xzf "$TAR_NAME"

if [[ ! -d "AirMouse.app" ]]; then
    echo -e "${RED}❌ Error: AirMouse.app bundle not found in archive.${NC}"
    rm -rf "$TMP_DIR"
    exit 1
fi

# 2. Clear Apple Gatekeeper Quarantine attribute
echo -e "🛡️  Clearing macOS Gatekeeper Quarantine attributes..."
xattr -dr com.apple.quarantine AirMouse.app 2>/dev/null || true
chmod +x AirMouse.app/Contents/MacOS/AirMouse 2>/dev/null || true

# 3. Install to /Applications
APP_DEST="/Applications/AirMouse.app"
if [[ -d "$APP_DEST" ]]; then
    echo -e "${YELLOW}⚠️  Existing AirMouse found in /Applications. Updating...${NC}"
    rm -rf "$APP_DEST"
fi

echo -e "🚀 Installing to ${BOLD}/Applications/AirMouse.app${NC}..."
cp -R AirMouse.app /Applications/

# Clear quarantine again on target
xattr -dr com.apple.quarantine "/Applications/AirMouse.app" 2>/dev/null || true

# 4. Optional CLI symlink
CLI_SRC="/Applications/AirMouse.app/Contents/MacOS/AirMouse"
CLI_DEST="/usr/local/bin/airmouse"

if [[ -d "/usr/local/bin" && -w "/usr/local/bin" ]]; then
    ln -sf "$CLI_SRC" "$CLI_DEST"
    echo -e "🔗 CLI link created at: ${BOLD}${CLI_DEST}${NC}"
fi

# Cleanup
rm -rf "$TMP_DIR"

echo ""
echo -e "${GREEN}${BOLD}✅ AirMouse Pro installed successfully!${NC}"
echo -e "${CYAN}====================================================${NC}"
echo ""
echo -e "${YELLOW}${BOLD}⚠️  CRITICAL STEP: macOS Accessibility Permissions${NC}"
echo -e "macOS requires you to grant Accessibility permissions so AirMouse can move the cursor."
echo ""
echo -e "1. Open ${BOLD}System Settings → Privacy & Security → Accessibility${NC}"
echo -e "2. Click the '+' or enable the toggle next to ${BOLD}AirMouse${NC} (or your Terminal)"
echo ""

# Prompt to open System Settings
read -p "Would you like to open Privacy & Security settings now? (y/N) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    open "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility"
fi

echo ""
read -p "Would you like to launch AirMouse now? (Y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    echo -e "🚀 Launching AirMouse..."
    open "/Applications/AirMouse.app"
fi

echo ""
echo -e "${GREEN}🎉 All set! Open AirMouse on your Android phone or browser to connect.${NC}"
echo ""
