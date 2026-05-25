# Air Mouse – Complete Setup Guide

This guide walks you through installing and running Air Mouse on **Android** and **PC** (Windows, macOS, Linux).

## Table of Contents

1. [PC Server Setup (All OS)](#pc-server-setup-all-os)
   - [Fix Proxy Error (macOS/Linux)](#fix-proxy-error-macoslinux)
   - [Install pyautogui](#install-pyautogui)
   - [Run the Server](#run-the-server)
2. [Android App Setup](#android-app-setup)
   - [Method 1: Android Studio](#method-1-android-studio)
   - [Method 2: Command Line (No Android Studio)](#method-2-command-line-no-android-studio)
   - [Method 3: Pre‑built APK](#method-3-prebuilt-apk)
3. [Network Configuration](#network-configuration)
4. [First Run & Calibration](#first-run--calibration)

---

## PC Server Setup (All OS)

### Step 1: Install Python 3.8+

- **Windows:** Download from [python.org](https://python.org), check “Add to PATH”.
- **macOS:** `brew install python` or download installer.
- **Linux:** `sudo apt install python3 python3-pip` (Debian/Ubuntu) or equivalent.

Verify:
```bash
python3 --version   # Should be 3.8 or higher