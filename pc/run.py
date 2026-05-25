#!/usr/bin/env python3
import subprocess
import sys
import os

def install_dependencies():
    print("📦 Installing required packages...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"])

def main():
    try:
        import pyautogui
        print("✅ Dependencies satisfied.")
    except ImportError:
        print("⚠️ Missing dependencies. Installing...")
        install_dependencies()

    from gui import AirMouseGUI
    gui = AirMouseGUI()
    gui.run()

if __name__ == "__main__":
    main()