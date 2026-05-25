@echo off
setlocal enabledelayedexpansion

:: ============================================================================
:: Air Mouse Ultimate – APK Builder (No Android Studio) for Windows
:: ============================================================================
:: This script builds the Air Mouse Android app using only the command line.
:: It requires:
::   - Java 11 (JDK) installed and in PATH
::   - Android SDK command-line tools installed at %USERPROFILE%\android-sdk
:: ============================================================================

set ANDROID_HOME=%USERPROFILE%\android-sdk
set SDK_MANAGER=%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat
set BUILD_TOOLS_VERSION=29.0.3
set PLATFORM_VERSION=android-29

echo [INFO] Checking prerequisites...

:: Check Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install Java 11 (JDK) and add to PATH.
    exit /b 1
)

:: Check Android SDK
if not exist "%ANDROID_HOME%" (
    echo [ERROR] Android SDK not found at %ANDROID_HOME%.
    echo Please install command-line tools first.
    exit /b 1
)

:: Check sdkmanager
if not exist "%SDK_MANAGER%" (
    echo [ERROR] sdkmanager not found.
    echo Make sure Android SDK command-line tools are installed in %ANDROID_HOME%\cmdline-tools\latest\
    exit /b 1
)

:: ----------------------------------------------------------------------------
:: Install required SDK components
:: ----------------------------------------------------------------------------
echo [INFO] Installing required SDK components (this may take a moment)...

:: Accept licenses (automatically)
echo y | %SDK_MANAGER% --licenses > nul 2>&1

:: Install build tools, platform, platform-tools
%SDK_MANAGER% "build-tools;%BUILD_TOOLS_VERSION%" "platforms;%PLATFORM_VERSION%" "platform-tools" > nul 2>&1

echo [INFO] SDK components installed.

:: ----------------------------------------------------------------------------
:: Build the APK
:: ----------------------------------------------------------------------------
echo [INFO] Building APK...

cd android
call gradlew clean
call gradlew assembleDebug

:: ----------------------------------------------------------------------------
:: Output result
:: ----------------------------------------------------------------------------
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if exist "%APK_PATH%" (
    echo [SUCCESS] Build successful!
    echo APK location: %cd%\%APK_PATH%
    echo.
    echo To install on your phone:
    echo   adb install %APK_PATH%
) else (
    echo [ERROR] Build failed: APK not found.
    exit /b 1
)