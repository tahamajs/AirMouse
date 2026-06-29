# Embedded Systems Project – Air Mouse
## University of Tehran, Faculty of Electrical and Computer Engineering
### Second Semester 1404-1405 (2025-2026)

---

## Group Information

| Full Name | Student ID | Primary Role |
|-----------|------------|--------------|
| Taha Majlesi | 810101504 | Android Developer, Sensor Processing |
| Javad Mohammadi | 810101510 | PC Server Developer, Mouse Control |
| Arman [Last Name] | 220701089 | UI/UX Designer, Data Transmission |
| Mehdi Mokhtari | 810101514 | Profiling, Documentation, Testing |

---

## Project Breakdown by Section

### 1. Android Side

#### 1-1. User Interface (UI)

| Task | Responsible | Status |
|------|-------------|--------|
| Design main screen (Calibrate button, Start button, orientation indicator) | Arman | Assigned |
| Design calibration screen with step‑by‑step instructions | Arman | Assigned |
| Display raw and filtered sensor values for debugging | Arman | Assigned |
| Visual feedback for click and scroll actions (e.g., flash or color change) | Arman | Assigned |
| IP address input field for laptop connection | Arman | Assigned |
| Responsive layout adapting to different screen sizes | Arman | Assigned |

#### 1-2. Sensors and Filters

| Task | Responsible | Status |
|------|-------------|--------|
| Read raw data from gyroscope, accelerometer, magnetometer | Taha | Assigned |
| Gyroscope bias calibration (stationary average) | Taha | Assigned |
| Accelerometer calibration (6 orientations, offset/scale) | Taha | Assigned |
| Magnetometer hard‑iron calibration (figure‑8 pattern, min/max) | Taha | Assigned |
| Implement Madgwick (or Mahony/Kalman) sensor fusion filter | Taha | Assigned |
| Implement low‑pass and high‑pass filters for noise reduction | Taha | Assigned |

#### 1-3. Motion to Commands

| Task | Responsible | Status |
|------|-------------|--------|
| Map Z‑axis rotation to horizontal cursor movement | Taha | Assigned |
| Map X‑axis rotation to vertical cursor movement | Taha | Assigned |
| Detect click via quick Y‑axis rotation (with speed threshold) | Taha | Assigned |
| Detect scroll via quick linear Y‑axis movement (with threshold) | Taha | Assigned |
| Implement hysteresis to prevent false detections | Taha | Assigned |
| Send delta (incremental) values instead of absolute positions | Taha | Assigned |

#### 1-4. Data Transmission to Laptop

| Task | Responsible | Status |
|------|-------------|--------|
| Implement TCP socket for data transmission | Arman | Assigned |
| Format data as JSON (fields: DeltaX, DeltaY, Click, Scroll) | Arman | Assigned |
| Implement ACK and Retransmit mechanism for click/scroll packets | Arman | Assigned |
| Add Internet permission and cleartext traffic in AndroidManifest | Arman | Assigned |

---

### 2. Laptop (PC) Side

| Task | Responsible | Status |
|------|-------------|--------|
| Implement TCP server in Python (or Go) to receive data | Javad | Assigned |
| Parse JSON and extract DeltaX, DeltaY, Click, Scroll | Javad | Assigned |
| Control mouse using PyAutoGUI (or equivalent library) | Javad | Assigned |
| Send ACK for received packets (click and scroll) | Javad | Assigned |
| Simple UI to show connection status and received data | Javad | Assigned |

---

### 3. Profiling with Perfetto

| Task | Responsible | Status |
|------|-------------|--------|
| Enable USB Debugging and run trace on device | Mehdi | Assigned |
| Save trace output and analyse with Perfetto UI | Mehdi | Assigned |
| Answer questions 1–11 (trace analysis) | Mehdi | Assigned |
| Write config.pbtx configuration file | Mehdi | Assigned |

---

### 4. Documentation and Submission

| Task | Responsible | Status |
|------|-------------|--------|
| Record final video (max 5 minutes) | All Members | Assigned |
| Write final report (including explanations and answers) | Mehdi | Assigned |
| Build APK and test on Android 10+ | Taha, Arman | Assigned |
| Upload project to GitLab/GitHub | Javad | Assigned |
| Final packaging (ZIP) and submission | Javad | Assigned |

---

## Main Challenges and Solutions

| Challenge | Solution |
|-----------|----------|
| Gyroscope noise and drift | Used Madgwick filter and bias calibration |
| Accurate click and scroll detection | Used speed thresholds and hysteresis |
| Latency in data transmission from phone to laptop | Optimised socket and reduced message size |
| Different send/receive rates | Used buffering and ACK mechanism |
| Running Perfetto without internet access | Used Docker Image and trace_processor_shell |

---

## Repository Link

[Link to GitHub/GitLab repository]

---

## Submission Deadline

22/03/1404 (June 12, 2025) – 23:59

---

## Group Members' Signatures

| Name            | Student ID |
|-----------------|------------|
| Taha Majlesi    | 810101504 |
| Javad Mohammadi | 810101510 |
| Arman Karami    | 220701089 |
| Mehdi Mokhtari  | 810101514 |

---

## Notes

1. All members contributed equally to the project.
2. Regular team meetings were held to coordinate efforts.
3. Code reviews were performed by all members to ensure quality.
4. The project was tested on multiple devices and platforms.

---

*Date: June 2025*