#!/usr/bin/env python3
"""
One‑command launcher for Air Mouse PC server.
Checks dependencies, installs if missing, then starts GUI.
"""

import subprocess
import sys
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
REQUIREMENTS_FILE = BASE_DIR / "requirements.txt"

def install_dependencies():
    print("📦 Installing required packages...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", str(REQUIREMENTS_FILE)])

def main():
    try:
        import pyautogui
        import qrcode
        import PIL
        import netifaces
        print("✅ Dependencies satisfied.")
    except ImportError:
        print("⚠️ Missing dependencies. Installing...")
        install_dependencies()

    # Launch GUI
    sys.path.insert(0, str(BASE_DIR))
    from gui import AirMouseGUI
    gui = AirMouseGUI()
    gui.run()

if __name__ == "__main__":
    main()
