# Building the Air Mouse APK Without Android Studio – Complete Guide

This section provides an **exhaustive, step‑by‑step guide** to building the Air Mouse Android APK using only the command line. No Android Studio is required. You will learn how to set up the environment, run the provided scripts, and troubleshoot common issues.

---

## 📖 Table of Contents

- [Building the Air Mouse APK Without Android Studio – Complete Guide](#building-the-air-mouse-apk-without-android-studio--complete-guide)
  - [📖 Table of Contents](#-table-of-contents)
  - [Why Build Without Android Studio?](#why-build-without-android-studio)
  - [Prerequisites – Complete Setup](#prerequisites--complete-setup)
    - [Java Development Kit (JDK 11)](#java-development-kit-jdk-11)
    - [Android Command‑Line Tools](#android-commandline-tools)
    - [Setting Environment Variables (macOS/Linux/Windows)](#setting-environment-variables-macoslinuxwindows)
  - [The Build Scripts – Full Explanation](#the-build-scripts--full-explanation)
    - [`build_apk.sh` (macOS/Linux) – Line‑by‑Line](#build_apksh-macoslinux--linebyline)
      - [1. Configuration variables](#1-configuration-variables)
      - [2. Colour output helpers](#2-colour-output-helpers)
      - [3. Java version check](#3-java-version-check)
      - [4. SDK and sdkmanager existence](#4-sdk-and-sdkmanager-existence)
      - [5. License acceptance and component installation](#5-license-acceptance-and-component-installation)
      - [6. Gradle build](#6-gradle-build)
      - [7. Output](#7-output)
    - [`build_apk.bat` (Windows) – Equivalent but with Windows syntax](#build_apkbat-windows--equivalent-but-with-windows-syntax)
  - [Step‑by‑Step Build Process](#stepbystep-build-process)
  - [Common Errors and Solutions](#common-errors-and-solutions)
  - [Verifying the Build](#verifying-the-build)
  - [Installing the APK on Your Phone](#installing-the-apk-on-your-phone)
    - [Method 1: Using ADB (recommended)](#method-1-using-adb-recommended)
    - [Method 2: Manual transfer](#method-2-manual-transfer)
  - [Manual Gradle Build (Without Scripts)](#manual-gradle-build-without-scripts)
  - [Alternative: Using Docker for a Reproducible Build](#alternative-using-docker-for-a-reproducible-build)
  - [Summary Checklist](#summary-checklist)
  - [Final Notes](#final-notes)

---

## Why Build Without Android Studio?

- **Lightweight** – You don’t need to download a 2 GB IDE.
- **Automated** – Perfect for CI/CD pipelines or batch builds.
- **Offline‑friendly** – Once the SDK components are installed, no internet is needed for the build itself.
- **Scriptable** – The provided scripts can be integrated into any workflow.

The scripts automate everything: Java detection, SDK setup, license acceptance, dependency installation, and the actual Gradle build.

---

## Prerequisites – Complete Setup

Before running the scripts, you must install two components:

### Java Development Kit (JDK 11)

| OS | Installation |
|----|--------------|
| **Windows** | Download the [Eclipse Temurin 11 JDK](https://adoptium.net/temurin/releases/?version=11) (MSI installer). Run the installer and ensure “Add to PATH” is checked. |
| **macOS** | `brew install openjdk@11` (if using Homebrew) or download the .pkg from Adoptium. |
| **Linux (Ubuntu/Debian)** | `sudo apt install openjdk-11-jdk` |
| **Linux (Fedora)** | `sudo dnf install java-11-openjdk-devel` |

**Verify installation:**
```bash
java -version
```
Expected output contains `openjdk version "11.0.x"` or similar. **Java 8 will NOT work** – the scripts check for version 11.

### Android Command‑Line Tools

The Android SDK command‑line tools provide `sdkmanager`, `avdmanager`, and other utilities. They do **not** include Android Studio.

1. **Download** from [Google’s official page](https://developer.android.com/studio#command-line-tools-only).  
   Choose the version for your OS (e.g., `commandlinetools-mac-9477386_latest.zip`).

2. **Extract** to a permanent folder.  
   - **macOS/Linux:** `~/android-sdk` (recommended)  
   - **Windows:** `%USERPROFILE%\android-sdk`

3. **Create the required subdirectory structure:**  
   The scripts expect the `sdkmanager` binary to be located at:  
   ```
   ~/android-sdk/cmdline-tools/latest/bin/sdkmanager
   ```
   Therefore, after extracting, you must move the files.

   **Example for macOS/Linux:**
   ```bash
   mkdir -p ~/android-sdk/cmdline-tools
   unzip commandlinetools-mac-*.zip -d ~/android-sdk/cmdline-tools
   mv ~/android-sdk/cmdline-tools/cmdline-tools ~/android-sdk/cmdline-tools/latest
   ```

   **Example for Windows (PowerShell):**
   ```powershell
   New-Item -ItemType Directory -Force "$env:USERPROFILE\android-sdk\cmdline-tools"
   Expand-Archive .\commandlinetools-win-*.zip -DestinationPath "$env:USERPROFILE\android-sdk\cmdline-tools"
   Move-Item "$env:USERPROFILE\android-sdk\cmdline-tools\cmdline-tools" "$env:USERPROFILE\android-sdk\cmdline-tools\latest"
   ```

4. **Add to PATH (optional)** – Not required if you use the scripts’ hardcoded paths, but useful for manual commands.

### Setting Environment Variables (macOS/Linux/Windows)

The scripts use `ANDROID_HOME` to locate the SDK. You should set it permanently.

**macOS / Linux (add to `~/.bashrc` or `~/.zshrc`):**
```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

**Windows (System Environment Variables):**
- Variable name: `ANDROID_HOME`
- Value: `%USERPROFILE%\android-sdk`
- Also add `%ANDROID_HOME%\cmdline-tools\latest\bin` and `%ANDROID_HOME%\platform-tools` to your `PATH`.

After setting, restart your terminal or run `source ~/.bashrc`.

---

## The Build Scripts – Full Explanation

We provide two scripts – one for Unix‑like systems (macOS/Linux) and one for Windows. Both are self‑contained and include error checking.

### `build_apk.sh` (macOS/Linux) – Line‑by‑Line

```bash
#!/bin/bash
# =============================================================================
# Air Mouse Ultimate – APK Builder (No Android Studio)
# =============================================================================
set -e  # Exit immediately if any command fails
```

**Key sections explained:**

#### 1. Configuration variables
```bash
ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
SDK_MANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
BUILD_TOOLS_VERSION="29.0.3"
PLATFORM_VERSION="android-29"
```
- `ANDROID_HOME` defaults to `~/android-sdk` if not set.
- Path to `sdkmanager` is hardcoded to the expected location.
- Build tools version 29.0.3 and platform 29 (Android 10) match the project’s `minSdk`.

#### 2. Colour output helpers
```bash
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }
info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
```
Pretty‑prints messages.

#### 3. Java version check
```bash
if ! command -v java &> /dev/null; then
    error "Java not found. Please install Java 11 (JDK) and add it to PATH."
fi
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    warn "Java version $JAVA_VERSION detected. Recommended: Java 11 or higher."
fi
```
Checks both existence and version (expects ≥11).

#### 4. SDK and sdkmanager existence
```bash
if [ ! -d "$ANDROID_HOME" ]; then
    error "Android SDK not found at $ANDROID_HOME. Please install command-line tools first."
fi
if [ ! -f "$SDK_MANAGER" ]; then
    error "sdkmanager not found. Make sure Android SDK command-line tools are installed in $ANDROID_HOME/cmdline-tools/latest/"
fi
```
Validates the SDK structure.

#### 5. License acceptance and component installation
```bash
yes | $SDK_MANAGER --licenses > /dev/null 2>&1 || true
$SDK_MANAGER "build-tools;$BUILD_TOOLS_VERSION" \
             "platforms;$PLATFORM_VERSION" \
             "platform-tools" > /dev/null 2>&1
```
- `yes |` automatically answers “yes” to all license prompts.
- Output is redirected to `/dev/null` to keep the terminal clean.
- Installs three essential components.

#### 6. Gradle build
```bash
cd "$(dirname "$0")/android" || error "Cannot find android/ folder"
chmod +x gradlew
./gradlew clean
./gradlew assembleDebug
```
- Changes to the `android` folder (relative to script location).
- Makes `gradlew` executable.
- Runs clean and then the debug build.

#### 7. Output
```bash
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    info "✅ Build successful!"
    echo "APK location: $(pwd)/$APK_PATH"
else
    error "Build failed: APK not found at $APK_PATH"
fi
```
Reports success or failure.

### `build_apk.bat` (Windows) – Equivalent but with Windows syntax

Uses `setlocal`, `%USERPROFILE%`, `call gradlew`, and `if exist`. The logic is identical.

---

## Step‑by‑Step Build Process

1. **Open a terminal** (Command Prompt on Windows, or Terminal on macOS/Linux).
2. **Navigate to the Air Mouse project root** (where `build_apk.sh` or `build_apk.bat` is located).
3. **Run the script** as described.
4. **First run** – The script will download and install the required SDK components (about 500 MB). This happens only once.
5. **Subsequent runs** – The build takes about 30 seconds (no downloads).
6. **Upon success**, the script prints the full path to the APK.

---

## Common Errors and Solutions

| Error Message | Cause | Solution |
|---------------|-------|----------|
| `Java not found. Please install Java 11` | JDK missing or not in PATH | Install OpenJDK 11 and add `java` to PATH. |
| `Android SDK not found at /home/user/android-sdk` | `ANDROID_HOME` not set or wrong folder | Create the folder and extract command‑line tools there. |
| `sdkmanager not found` | Bad extraction (missing `cmdline-tools/latest/` structure) | Re‑extract and rename directory as shown above. |
| `Could not determine Java version` | Gradle cannot find Java | Ensure `JAVA_HOME` is also set (e.g., `/usr/lib/jvm/java-11-openjdk`). |
| `License agreements not accepted` | The `yes | ...` line failed | Run `sdkmanager --licenses` manually and accept. |
| `gradlew: Permission denied` (macOS/Linux) | Script not executable | Run `chmod +x gradlew` inside the `android` folder. |
| `Could not resolve all dependencies` | Internet missing or repository unreachable | Check your internet connection; the script uses Google’s Maven repository. |
| `adb: command not found` (when installing APK) | ADB not in PATH | The script does not require ADB. For installation, either copy the APK manually or add `platform-tools` to PATH. |

---

## Verifying the Build

After a successful build, you should see:
```
[INFO] ✅ Build successful!
APK location: /path/to/AirMouse-Ultimate/android/app/build/outputs/apk/debug/app-debug.apk
```

Check the file size – it should be around 5‑10 MB.

Optionally, you can inspect the APK contents using `unzip -l app-debug.apk | head` (Linux/macOS) or any zip tool.

---

## Installing the APK on Your Phone

### Method 1: Using ADB (recommended)

Ensure USB debugging is enabled on your phone (Developer options). Then:
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

If you get `adb: command not found`, add `$ANDROID_HOME/platform-tools` to your PATH or run:
```bash
$ANDROID_HOME/platform-tools/adb install <apk-path>
```

### Method 2: Manual transfer

Copy the APK to your phone via USB, cloud, or email. On the phone, open the file and allow “Install from unknown sources” (you may need to enable it in Settings → Security).

---

## Manual Gradle Build (Without Scripts)

If you prefer not to use the provided scripts, you can run the Gradle commands directly (after setting up the SDK environment):

```bash
cd android
export ANDROID_HOME=~/android-sdk   # set appropriately
./gradlew clean
./gradlew assembleDebug
```

This does **not** include automatic SDK installation or license acceptance. You would have to run `sdkmanager` commands manually first.

---

## Alternative: Using Docker for a Reproducible Build

You can build the APK inside a Docker container with all dependencies pre‑installed. This is ideal for CI/CD.

**Example `Dockerfile`:**
```dockerfile
FROM openjdk:11-jdk-slim
RUN apt-get update && apt-get install -y wget unzip
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/tools.zip
RUN mkdir -p /android-sdk/cmdline-tools && unzip /tmp/tools.zip -d /android-sdk/cmdline-tools && \
    mv /android-sdk/cmdline-tools/cmdline-tools /android-sdk/cmdline-tools/latest
ENV ANDROID_HOME=/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
RUN yes | sdkmanager --licenses && sdkmanager "build-tools;29.0.3" "platforms;android-29" "platform-tools"
COPY . /app
WORKDIR /app/android
RUN ./gradlew assembleDebug
```

Build and run:
```bash
docker build -t airmouse-builder .
docker run --rm -v $(pwd)/out:/app/android/app/build/outputs airmouse-builder
```

The APK will appear in the `out` folder.

---

## Summary Checklist

- [ ] Java 11 JDK installed and `java -version` shows 11.x.
- [ ] Android command‑line tools extracted to `~/android-sdk/cmdline-tools/latest/`.
- [ ] Environment variable `ANDROID_HOME` set correctly.
- [ ] The Air Mouse project’s `android` folder contains `gradlew` and `app/`.
- [ ] You have run the build script at least once.
- [ ] The APK is generated and can be installed on an Android 10+ device.

---

## Final Notes

Building without Android Studio is **faster**, **scriptable**, and **perfect for automation**. The provided scripts handle all the tricky parts – SDK downloads, license acceptance, and the Gradle build. Once set up, you can rebuild the APK in seconds after any code change.

Now you are ready to build the Air Mouse APK on any machine, without ever opening Android Studio. Proceed to install the APK and enjoy the Air Mouse experience!