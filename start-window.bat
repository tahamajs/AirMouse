@echo off
title AirMouse Pro - Desktop Window Dashboard
echo ==============================================================================
echo   AirMouse Pro - Desktop GUI Window Launcher
echo ==============================================================================
echo.
set "SCRIPT_DIR=%~dp0"
set "PYTHONPATH=%SCRIPT_DIR%pc\airmouse_py;%PYTHONPATH%"

python -m ui.app %*
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [AirMouse] CustomTkinter GUI failed or python not found.
    echo Starting AirMouse background server and opening Web Client window...
    start "" "%SCRIPT_DIR%releases\windows\AirMouse-Server-v1.0.0-windows-x64.exe"
    timeout /t 2 /nobreak >nul
    start msedge --app=http://localhost:8081/?autoconnect=1 || start chrome --app=http://localhost:8081/?autoconnect=1 || start http://localhost:8081/?autoconnect=1
)
pause
