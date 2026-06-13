#!/bin/bash

# Air Mouse Pro Server Uninstallation Script

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Variables
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
}

# Confirm uninstall
confirm_uninstall() {
    echo -e "${RED}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║     Air Mouse Pro Server Uninstallation                       ║${NC}"
    echo -e "${RED}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}This will remove Air Mouse Pro Server and all its data.${NC}"
    echo -e "${YELLOW}This action cannot be undone.${NC}"
    echo ""
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${GREEN}Uninstall cancelled.${NC}"
        exit 0
    fi
}

# Stop service
stop_service() {
    echo -e "${BLUE}Stopping service...${NC}"
    
    if [ "$OS" = "linux" ]; then
        if systemctl is-active --quiet ${SERVICE_NAME}; then
            sudo systemctl stop ${SERVICE_NAME}
            sudo systemctl disable ${SERVICE_NAME}
            echo -e "${GREEN}Systemd service stopped and disabled.${NC}"
        fi
    elif [ "$OS" = "darwin" ]; then
        if launchctl list | grep -q com.airmouse.server; then
            launchctl stop com.airmouse.server
            launchctl unload ~/Library/LaunchAgents/com.airmouse.server.plist
            echo -e "${GREEN}Launchd service stopped.${NC}"
        fi
    fi
}

# Remove binary
remove_binary() {
    echo -e "${BLUE}Removing binary...${NC}"
    
    if [ -f "${INSTALL_DIR}/airmouse-server" ]; then
        sudo rm -f "${INSTALL_DIR}/airmouse-server"
        echo -e "${GREEN}Binary removed.${NC}"
    fi
}

# Remove configuration
remove_config() {
    echo -e "${BLUE}Removing configuration...${NC}"
    
    read -p "Remove configuration files? (y/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [ -d "$CONFIG_DIR" ]; then
            sudo rm -rf "$CONFIG_DIR"
            echo -e "${GREEN}Configuration removed.${NC}"
        fi
    else
        echo -e "${YELLOW}Configuration kept at ${CONFIG_DIR}${NC}"
    fi
}

# Remove data
remove_data() {
    echo -e "${BLUE}Removing data...${NC}"
    
    read -p "Remove data directory? (y/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [ -d "$DATA_DIR" ]; then
            sudo rm -rf "$DATA_DIR"
            echo -e "${GREEN}Data removed.${NC}"
        fi
    else
        echo -e "${YELLOW}Data kept at ${DATA_DIR}${NC}"
    fi
}

# Remove logs
remove_logs() {
    echo -e "${BLUE}Removing logs...${NC}"
    
    read -p "Remove log files? (y/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [ -d "$LOG_DIR" ]; then
            sudo rm -rf "$LOG_DIR"
            echo -e "${GREEN}Logs removed.${NC}"
        fi
    else
        echo -e "${YELLOW}Logs kept at ${LOG_DIR}${NC}"
    fi
}

# Remove service files
remove_service_files() {
    echo -e "${BLUE}Removing service files...${NC}"
    
    if [ "$OS" = "linux" ]; then
        if [ -f "/etc/systemd/system/${SERVICE_NAME}.service" ]; then
            sudo rm -f "/etc/systemd/system/${SERVICE_NAME}.service"
            sudo systemctl daemon-reload
            echo -e "${GREEN}Systemd service file removed.${NC}"
        fi
    elif [ "$OS" = "darwin" ]; then
        if [ -f ~/Library/LaunchAgents/com.airmouse.server.plist ]; then
            rm -f ~/Library/LaunchAgents/com.airmouse.server.plist
            echo -e "${GREEN}Launchd service file removed.${NC}"
        fi
    fi
}

# Print completion message
print_completion() {
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     Air Mouse Pro Server uninstalled successfully!            ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Thank you for using Air Mouse Pro Server.${NC}"
    echo -e "${YELLOW}We hope to see you again!${NC}"
    echo ""
}

# Main execution
main() {
    detect_os
    confirm_uninstall
    stop_service
    remove_binary
    remove_service_files
    remove_config
    remove_data
    remove_logs
    print_completion
}

# Run main
main