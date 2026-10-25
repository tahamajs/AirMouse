#!/usr/bin/env bash
# ==============================================================================
# AirMouse Pro - Desktop GUI Window Launcher
# Launches the modern dark-mode GUI Desktop Dashboard with live controls & QR
# ==============================================================================
set -e

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PYTHONPATH="$REPO_DIR/pc/airmouse_py"

echo "=============================================================================="
echo "  🖥️   Launching AirMouse Pro Desktop Window Dashboard..."
echo "=============================================================================="

# Launch the CustomTkinter GUI Desktop Window
python3 -m ui.app "$@"
