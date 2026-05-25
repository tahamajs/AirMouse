
---

## `docs/VIDEO_SCRIPT.md`

```markdown
# Video Script – 5 Minutes Maximum

## Introduction (0:00 – 0:20)
- Show your face or just the screen. Say:
  > "Hello, I am [Name], student ID [SID]. This is my Air Mouse demonstration for the Embedded Systems course at University of Tehran."
- Show both devices: phone in hand, laptop screen visible.

## Calibration (0:20 – 1:00)
- Open the Air Mouse app on phone.
- Enter laptop IP (show IP on laptop via `ifconfig`).
- Tap **"Calibrate Sensors"**.
- **Gyro:** Place phone on table – say "Phone is still."
- **Magnetometer:** Pick up phone, move in figure‑8 pattern – say "I am now moving the phone in a figure‑8 for 30 seconds."
- Wait for "Calibration complete!" toast.

## Starting the Server (1:00 – 1:15)
- Show laptop terminal with `python3 server.py` running.
- Point to "Server listening on 0.0.0.0:8080".

## Connection (1:15 – 1:30)
- Tap **"Start Air Mouse"** on phone.
- Status changes to "Air Mouse Active".
- Laptop terminal may show "Connected: ..."

## Cursor Movement (1:30 – 2:30)
- **Horizontal:** Rotate phone around Z axis (like turning a doorknob). Cursor moves left/right.
- **Vertical:** Rotate around X axis (nodding up/down). Cursor moves up/down.
- Show smooth, no jumps.
- Then place phone on table – cursor stays still (no drift). Hold for 5 seconds.

## Click Gesture (2:30 – 3:15)
- Open Notepad (or any text field).
- Flick phone quickly left/right around Y axis.
- Click is performed – a character appears in Notepad.
- Do twice to show consistency.

## Scroll Gesture (3:15 – 4:00)
- Open a web page or long document.
- Push phone quickly **up** (linear motion along Y) – page scrolls down.
- Push phone quickly **down** – page scrolls up.
- (If your scroll direction is opposite, mention it's configurable.)

## Perfetto & Questions (4:00 – 4:30)
- Show a screenshot of Perfetto UI (optional if time).
- Say: "We have answered all 11 questions using Perfetto traces. The answers are in the report."

## Conclusion (4:30 – 5:00)
- "This demonstrates the Air Mouse working correctly: cursor movement, click, scroll, and no drift."
- Mention any challenge you faced (e.g., "I had to re‑calibrate because of laptop speakers").
- "Thank you for watching."

---

## Tips for Recording

- Use **OBS Studio** (free) to record laptop screen and a webcam (or phone camera) simultaneously.
- Or record with your phone (ask a friend to hold it) while you control the phone.
- Keep the phone screen visible – use a mirroring app like `scrcpy` to show phone screen on laptop, then record laptop screen only.
- Practice twice before final recording.
- Upload video in MP4 format, max 5 minutes.