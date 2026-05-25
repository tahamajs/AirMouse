# Air Mouse – Complete Documentation

Welcome to the definitive guide for the University of Tehran Air Mouse embedded systems exercise.

This folder contains everything you need to understand, build, run, test, and submit the project.

## Quick Links

- [Setup Guide](SETUP_GUIDE.md) – install Android app and Python server on any OS
- [Calibration Deep Dive](CALIBRATION_DETAILS.md) – why and how to calibrate sensors
- [Sensor Fusion Explained](SENSOR_FUSION_EXPLAINED.md) – Madgwick algorithm, drift removal
- [Protocol Specification](PROTOCOL_SPECIFICATION.md) – TCP JSON format, ACKs, retransmission
- [Perfetto Answers](PERFETTO_ANSWERS.md) – complete answers to the 11 required questions
- [Troubleshooting](TROUBLESHOOTING.md) – every error and its fix
- [Video Script](VIDEO_SCRIPT.md) – 5‑minute demonstration script
- [Submission Checklist](SUBMISSION_CHECKLIST.md) – what to upload, grading rubrics
- [Advanced Customization](ADVANCED_CUSTOMIZATION.md) – tweak sensitivity, add gestures
- [Code Walkthrough](CODE_WALKTHROUGH.md) – line‑by‑line explanation of all source files

## Project Overview

**Air Mouse** turns an Android phone into a wireless motion‑controlled mouse.  
- Rotate phone → move cursor  
- Flick around Y‑axis → left click  
- Quick up/down linear motion → scroll  

**Core technologies:**  
- Android Sensor Framework (accelerometer, gyroscope, magnetometer)  
- Madgwick AHRS sensor fusion (quaternion‑based orientation)  
- TCP sockets with ACK for reliable click/scroll  
- Python + pyautogui for mouse control on PC  

**Repository structure:**  