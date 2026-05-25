# Submission Checklist – Complete Explanation

The submission checklist is your final step before uploading the Air Mouse project. It ensures that you have included every required component and that your submission meets the course’s expectations. This document explains each item in detail: what it is, how to prepare it, why it’s required, and common mistakes to avoid.

---

## 1. Required Files (in ZIP)

### Android Source Code – Entire `android/` Folder

**What to include:**  
- All Kotlin source files (`.kt`) under `android/app/src/main/java/com/airmouse/…`  
- All resource files (`res/` folder: layouts, drawables, values, etc.)  
- Gradle build files (`build.gradle`, `settings.gradle`, `gradle.properties`)  
- The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)

**Why it’s required:**  
The teaching assistants (TAs) must be able to inspect your code, verify that you implemented the required features, and check code style, comments, and architecture.

**How to prepare:**  
- Do **not** include build outputs (e.g., `build/`, `.gradle/`, `local.properties`).  
- Use `git archive` or manually create a zip of the `android` folder.  
- Ensure the project can be opened in Android Studio and compiles.

**Common mistake:**  
Zipping the entire Android Studio project directory including large intermediate files. Use `git clean -xdf` or exclude `build/` manually.

---

### APK File – `airmouse.apk`

**What it is:**  
The compiled Android application package that can be installed on a device.

**Why it’s required:**  
The TAs will install and test your app on a real Android device (emulator or physical phone). The APK must be signed (debug or release) and compatible with Android 10+.

**How to prepare:**  
- Build the APK:  
  - Android Studio: `Build → Build Bundle(s) / APK(s) → Build APK(s)`  
  - Command line: `./gradlew assembleDebug`  
- Rename the APK to `airmouse.apk` for clarity.  
- Test it on a clean device (uninstall any previous version first).

**Common mistake:**  
Submitting an unsigned APK or one built for a different architecture. Always build for your target (debug is fine).

---

### PC Server Code – `pc/` Folder

**What to include:**  
- `server.py`, `gui.py`, `run.py`, `run.sh`, `run.bat`, `requirements.txt`, `config.json`, `perfetto_analyzer.py` (if used).  
- Any additional Python files you created.

**Why it’s required:**  
The TAs need to run the server to test the complete system. They should be able to execute `python server.py` or `python gui.py` and see the mouse controlled by the phone.

**How to prepare:**  
- Ensure the server runs without errors on a fresh Python environment.  
- Include `requirements.txt` so that dependencies can be installed.  
- Do not include `__pycache__` or virtual environment folders.

**Common mistake:**  
Forgetting to include `requirements.txt` or `config.json`. The server may depend on them.

---

### Video – `demo.mp4`

**What it is:**  
A short video (maximum 5 minutes) demonstrating the working Air Mouse. Both the phone screen (or the phone itself) and the laptop screen must be visible simultaneously.

**Why it’s required:**  
The video provides proof of functionality. If the TAs cannot reproduce the behaviour, the video serves as evidence. It also shows the calibration process and all gestures.

**How to prepare:**  
- Use a second phone or a webcam to record both screens.  
- Alternatively, use screen mirroring (e.g., `scrcpy`) to show phone screen on the laptop, then record the laptop screen only.  
- Follow the script in `VIDEO_SCRIPT.md`.  
- Keep the file size reasonable (e.g., < 100 MB) by using H.264 compression.

**What to show:**  
- Calibration (gyro still, magnetometer figure‑8, accelerometer still)  
- Connection and status change  
- Cursor movement (horizontal and vertical)  
- Left click, double click, right click  
- Scroll up/down  
- Drift test (phone still on table, cursor does not move)  
- (Optional) Debug overlay

**Common mistake:**  
Video too long, poor lighting, not showing both screens, or missing a key gesture.

---

### Report PDF – `report.pdf`

**What it is:**  
A written document summarising your work. It must contain specific sections as listed.

**Required sections in detail:**

#### Answers to 11 Perfetto questions (with screenshots)
- Provide clear, concise answers for each of the 11 questions (explain sensor chain, fusion, sampling period, contention, wake‑up vs non‑wake‑up, CPU time, most power‑hungry sensor, sampling rate effect, latency, threading, slow vs sudden movement).  
- Include **screenshots** from Perfetto UI or from the output of `perfetto_analyzer.py`.  
- Each screenshot should be labelled (e.g., “Q3 – Sensor sampling periods”).

#### Explanation of sensor fusion method (Madgwick)
- Describe why raw sensors fail (drift, noise, offset).  
- Explain the Madgwick algorithm at a high level (quaternion, gyro prediction, gradient descent correction using accelerometer and magnetometer).  
- Mention the role of the `beta` parameter.  
- You can reference `MadgwickFusion.kt` and the code structure.

#### Calibration process summary
- List the three steps: gyro bias (still, 3 sec), magnetometer (figure‑8, 30 sec), accelerometer (still, simplified).  
- Explain why each is needed (removes drift, hard‑iron offset, accelerometer offset).  
- Mention that you can also implement full 6‑point accelerometer calibration.

#### Team member contributions (who did what)
- Clearly state each member’s responsibility: e.g., “Arian Firoozi – sensor fusion, gesture detection; Arsalan Talaee – PC server, network protocol”.  
- If all contributed equally, say so.

#### Any challenges faced and solutions
- Describe at least one significant difficulty (e.g., drift, connection issues, false gestures) and how you solved it (e.g., calibration, threshold tuning, moving sensor processing to background thread).

**Formatting:**  
- Use clear headings, readable font (11–12 pt).  
- Keep the report to 5–10 pages.  
- Export as PDF (not Word or plain text).

**Common mistake:**  
Missing screenshots, vague answers, no mention of team contributions, or not including the calibration summary.

---

### Perfetto Trace File – `trace.perfetto-trace`

**Optional but recommended.**

**What it is:**  
A trace collected from the Android device using Perfetto. The trace captures system events (sensor events, CPU scheduling, etc.) while you are using the Air Mouse.

**Why recommended:**  
It allows the TAs to verify your Perfetto answers and to see that you actually performed the tracing.

**How to collect:**  
- Enable USB debugging.  
- Use the `record_android_trace` script (provided in the exercise attachments) or Perfetto UI.  
- Record for about 10 seconds while moving the phone.  
- Save the trace file.

**Common mistake:**  
Trace file too large (hundreds of MB). Compress it (e.g., `gzip`). A short trace of 10–20 MB is sufficient.

---

## 2. Naming Convention

**Format:**  
`CPS-CA2-<SID1>-<SID2>-<SID3>-<SID4>.zip`

**Example:**  
`CPS-CA2-401012345-401023456-401034567-401045678.zip`

**Why this format:**  
The course uses a standard naming scheme to automate grading and avoid conflicts. The prefix “CPS-CA2” identifies the exercise (Computer Projects – Computer Assignment 2). The SIDs are student IDs.

**How to prepare:**  
- Replace `<SID1>` etc. with the actual 9‑digit student ID numbers of your group members.  
- Order does not matter, but all four SIDs must be present if you have four members. For fewer members, adjust accordingly.  
- Use underscores or hyphens as shown (hyphens are used in the example).  
- Ensure the zip file contains the folder structure (not the folder itself? Usually you zip the contents; check course instructions). Typically, the zip should contain the `android/`, `pc/`, `demo.mp4`, and `report.pdf` at the root.

**Common mistake:**  
Using spaces, special characters, or wrong prefix. Also, forgetting to include all team members’ SIDs.

---

## 3. Grading Rubric (Self‑Assessment)

| Criteria | Points | What TAs will check |
|----------|--------|----------------------|
| Cursor movement smooth, no drift | 20% | App is calibrated; cursor tracks phone rotation without jitter or drift. |
| Click detection reliable | 10% | Flick consistently triggers left click. |
| Double‑click detection reliable | 5% | Two quick flicks produce double click. |
| Right‑click detection reliable | 5% | Tilt and hold triggers right click. |
| Scroll detection reliable | 5% | Linear push scrolls page. |
| UI complete (IP entry, buttons, green square) | 10% | All UI elements present and functional. |
| Calibration works (gyro+mag) | 10% | Calibration steps complete and improve accuracy. |
| Network communication (TCP, ACK) | 10% | Phone connects to PC server, moves cursor, and ACKs are exchanged. |
| Perfetto answers (Q1‑Q11) | 15% | Report includes correct answers with screenshots. |
| Code cleanliness and comments | 10% | Code is readable, commented, modular. |
| Video and report completeness | 5% | Video shows all features; report is well structured. |

**Self‑assessment:**  
Before submission, run through each criterion and honestly rate your project. Fix any deficiencies.

---

## 4. Before Final ZIP

- [ ] **Remove `build/` folders** – Use `./gradlew clean` or manually delete.  
- [ ] **Test APK on a clean phone** – Uninstall any previous version, install fresh.  
- [ ] **Test PC server on a different computer** – Ensure dependencies are listed in `requirements.txt`.  
- [ ] **Verify video shows both screens clearly** and the audio (if any) is audible.  
- [ ] **Check that all team members have contributed** – mention contributions in the report.  
- [ ] **Confirm the zip file name matches the pattern** and contains all required files at the correct level.

---

## 5. Upload

Upload the zip file to the course portal **before the deadline** (22/03/1404 at 23:59). Late submissions are not accepted. After uploading, verify that the file is accessible and not corrupted.

---

This explanation of the submission checklist ensures you don’t miss any component. Follow it carefully, and your Air Mouse project will be complete and ready for evaluation.

*Part of Air Mouse Ultimate – University of Tehran, Embedded Systems Exercise.*