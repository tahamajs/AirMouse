#!/bin/bash

# Air Mouse Pro Server Installation Script
# Supports Linux, macOS, and Windows (WSL)

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Variables
VERSION="3.0.0"
INSTALL_DIR="/usr/local/bin"
CONFIG_DIR="/etc/airmouse"
DATA_DIR="/var/lib/airmouse"
LOG_DIR="/var/log/airmouse"
SERVICE_NAME="airmouse-server"

# Detect OS
detect_os() {
    case "$(uname -s)" in
        Linux*)     OS="linux";;
        Darwin*)    OS="darwin";;
        MINGW*|MSYS*|CYGWIN*) OS="windows";;
        *)          OS="unknown";;
    esac
    echo -e "${BLUE}Detected OS: ${OS}${NC}"
}

# Detect Architecture
detect_arch() {
    ARCH=$(uname -m)
    case "$ARCH" in
        x86_64)  ARCH="amd64";;
        aarch64) ARCH="arm64";;
        armv7l)  ARCH="armv7";;
        *)       ARCH="unknown";;
    esac
    echo -e "${BLUE}Detected Architecture: ${ARCH}${NC}"
}

# Check dependencies
check_dependencies() {
    echo -e "${BLUE}Checking dependencies...${NC}"
    
    MISSING=()
    
    command -v curl >/dev/null 2>&1 || MISSING+=("curl")
    command -v tar >/dev/null 2>&1 || MISSING+=("tar")
    
    if [ ${#MISSING[@]} -ne 0 ]; then
        echo -e "${RED}Missing dependencies: ${MISSING[*]}${NC}"
        echo -e "${YELLOW}Please install missing dependencies and try again.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}All dependencies satisfied.${NC}"
}

# Create directories
create_directories() {
    echo -e "${BLUE}Creating directories...${NC}"
    
    sudo mkdir -p "$CONFIG_DIR"
    sudo mkdir -p "$DATA_DIR"
    sudo mkdir -p "$LOG_DIR"
    
    echo -e "${GREEN}Directories created.${NC}"
}

# Download binary
download_binary() {
    echo -e "${BLUE}Downloading Air Mouse Pro Server v${VERSION}...${NC}"
    
    BINARY_URL="https://github.com/yourusername/airmouse-go/releases/download/v${VERSION}/airmouse-server_${VERSION}_${OS}_${ARCH}.tar.gz"
    
    TMP_DIR=$(mktemp -d)
    cd "$TMP_DIR"
    
    if ! curl -L -o airmouse-server.tar.gz "$BINARY_URL"; then
        echo -e "${RED}Failed to download binary.${NC}"
        exit 1
    fi
    
    tar -xzf airmouse-server.tar.gz
    sudo mv airmouse-server "$INSTALL_DIR/"
    
    cd - > /dev/null
    rm -rf "$TMP_DIR"
    
    echo -e "${GREEN}Binary installed to ${INSTALL_DIR}/airmouse-server${NC}"
}

# Copy configuration
copy_config() {
    echo -e "${BLUE}Configuring Air Mouse Pro Server...${NC}"
    
    if [ ! -f "$CONFIG_DIR/config.json" ]; then
        sudo cp config.example.json "$CONFIG_DIR/config.json"
        echo -e "${YELLOW}Default configuration created at ${CONFIG_DIR}/config.json${NC}"
        echo -e "${YELLOW}Please edit it to match your settings.${NC}"
    else
        echo -e "${GREEN}Configuration already exists.${NC}"
    fi
}

# Create systemd service (Linux)
create_systemd_service() {
    if [ "$OS" != "linux" ]; then
        return
    fi
    
    echo -e "${BLUE}Creating systemd service...${NC}"
    
    cat << EOF | sudo tee /etc/systemd/system/${SERVICE_NAME}.service
[Unit]
Description=Air Mouse Pro Server
After=network.target bluetooth.target
Wants=bluetooth.target

[Service]
Type=simple
User=root
Group=root
ExecStart=${INSTALL_DIR}/airmouse-server
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=${SERVICE_NAME}
Environment="CONFIG_PATH=${CONFIG_DIR}/config.json"
Environment="LOG_LEVEL=info"

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    echo -e "${GREEN}Systemd service created.${NC}"
}

# Create launchd service (macOS)
create_launchd_service() {
    if [ "$OS" != "darwin" ]; then
        return
    fi
    
    echo -e "${BLUE}Creating launchd service...${NC}"
    
    cat << EOF | sudo tee ~/Library/LaunchAgents/com.airmouse.server.plist
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.airmouse.server</string>
    <key>ProgramArguments</key>
    <array>
        <string>${INSTALL_DIR}/airmouse-server</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>${LOG_DIR}/airmouse.log</string>
    <key>StandardErrorPath</key>
    <string>${LOG_DIR}/airmouse.err</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>CONFIG_PATH</key>
        <string>${CONFIG_DIR}/config.json</string>
    </dict>
</dict>
</plist>
EOF

    launchctl load ~/Library/LaunchAgents/com.airmouse.server.plist
    echo -e "${GREEN}Launchd service created.${NC}"
}

# Set permissions
set_permissions() {
    echo -e "${BLUE}Setting permissions...${NC}"
    
    sudo chmod +x "${INSTALL_DIR}/airmouse-server"
    sudo chown -R $(whoami):$(whoami) "$DATA_DIR"
    sudo chown -R $(whoami):$(whoami) "$LOG_DIR"
    
    echo -e "${GREEN}Permissions set.${NC}"
}

# Start service
start_service() {
    echo -e "${BLUE}Starting Air Mouse Pro Server...${NC}"
    
    if [ "$OS" = "linux" ]; then
        sudo systemctl enable ${SERVICE_NAME}
        sudo systemctl start ${SERVICE_NAME}
        echo -e "${GREEN}Service started. Run 'sudo systemctl status ${SERVICE_NAME}' to check status.${NC}"
    elif [ "$OS" = "darwin" ]; then
        launchctl start com.airmouse.server
        echo -e "${GREEN}Service started. Run 'launchctl list | grep airmouse' to check status.${NC}"
    else
        echo -e "${YELLOW}Please start the server manually: ${INSTALL_DIR}/airmouse-server${NC}"
    fi
}

# Print completion message
print_completion() {
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     Air Mouse Pro Server installed successfully!               ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "Configuration file: ${YELLOW}${CONFIG_DIR}/config.json${NC}"
    echo -e "Data directory:     ${YELLOW}${DATA_DIR}${NC}"
    echo -e "Log directory:      ${YELLOW}${LOG_DIR}${NC}"
    echo ""
    echo -e "To start the server:"
    if [ "$OS" = "linux" ]; then
        echo -e "  ${YELLOW}sudo systemctl start ${SERVICE_NAME}${NC}"
        echo -e "  ${YELLOW}sudo systemctl status ${SERVICE_NAME}${NC}"
    elif [ "$OS" = "darwin" ]; then
        echo -e "  ${YELLOW}launchctl start com.airmouse.server${NC}"
    else
        echo -e "  ${YELLOW}${INSTALL_DIR}/airmouse-server${NC}"
    fi
    echo ""
    echo -e "Web UI: ${BLUE}http://localhost:8081${NC}"
    echo -e "API:    ${BLUE}http://localhost:8081/api/status${NC}"
    echo ""
}

# Main execution
main() {
    echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     Air Mouse Pro Server Installation Script                  ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    detect_os
    detect_arch
    check_dependencies
    create_directories
    download_binary
    copy_config
    set_permissions
    create_systemd_service
    create_launchd_service
    start_service
    print_completion
}

# Run main
main