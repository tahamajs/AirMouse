#!/usr/bin/env python3
"""
One‑command launcher for Air Mouse PC server.
Checks dependencies, installs if missing, then starts GUI.
"""

import subprocess
import sys
import os

def install_dependencies():
    print("📦 Installing required packages...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"])

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
    from gui import AirMouseGUI
    gui = AirMouseGUI()
    gui.run()

if __name__ == "__main__":
    main()
