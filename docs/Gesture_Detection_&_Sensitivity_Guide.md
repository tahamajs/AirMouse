# Air Mouse – Complete Gesture Detection & Sensitivity Guide

This document provides an **exhaustive explanation** of all gesture detection parameters, how they work under the hood, and how to adjust them for optimal performance. The Air Mouse recognises five distinct gestures: **move** (cursor), **click**, **double‑click**, **right‑click**, and **scroll**. Each gesture relies on a specific sensor (gyroscope or accelerometer) and configurable thresholds. All parameters can be fine‑tuned via the in‑app Settings dialog without modifying code.

---

## 📖 Table of Contents

- [Air Mouse – Complete Gesture Detection \& Sensitivity Guide](#air-mouse--complete-gesture-detection--sensitivity-guide)
  - [📖 Table of Contents](#-table-of-contents)
  - [Overview of Gesture Detection](#overview-of-gesture-detection)
  - [Settings Dialog – How to Access](#settings-dialog--how-to-access)
  - [Cursor Movement (Not Adjustable via Settings)](#cursor-movement-not-adjustable-via-settings)
  - [Click \& Double‑Click Parameters](#click--doubleclick-parameters)
    - [Click Speed Threshold (rad/s)](#click-speed-threshold-rads)
    - [Double‑Click Interval (ms)](#doubleclick-interval-ms)
  - [Right‑Click Parameters](#rightclick-parameters)
    - [Right‑Click Tilt Angle (degrees)](#rightclick-tilt-angle-degrees)
    - [Right‑Click Duration (ms)](#rightclick-duration-ms)
  - [Scroll Parameters](#scroll-parameters)
    - [Scroll Speed Threshold (m/s²)](#scroll-speed-threshold-ms)
    - [Scroll Debounce (m/s²)](#scroll-debounce-ms)
  - [How Thresholds Are Applied (Code Internals)](#how-thresholds-are-applied-code-internals)
  - [Recommended Settings for Different Users](#recommended-settings-for-different-users)
  - [Troubleshooting Gesture Detection](#troubleshooting-gesture-detection)
  - [Summary Table of Parameters](#summary-table-of-parameters)

---

## Overview of Gesture Detection

The Air Mouse processes raw sensor data at 50 Hz (every 20 ms). For each update, the following happens:

| Sensor | Value used | Gesture | Condition |
|--------|------------|---------|-----------|
| Gyroscope (Y‑axis) | `gyroY` (rad/s) | Click / double‑click | `|gyroY| > Click speed threshold` and timing window |
| Fused orientation (roll) | `roll` (radians) → converted to degrees | Right‑click | `|roll| > Right‑click tilt angle` for `Right‑click duration` |
| Accelerometer (Y‑axis) | `accelY` (m/s²) | Scroll | `|accelY| > Scroll speed threshold` and not debouncing |

The thresholds are stored in `PreferencesDataStore` and are read each time the gesture detector runs (so changes take effect immediately, no restart needed).

---

## Settings Dialog – How to Access

1. Open the Air Mouse app on your phone.
2. Tap the **Settings** button (labelled “Settings”).
3. A dialog appears with six sliders (and a checkbox for haptic feedback).
4. Move each slider left/right to decrease/increase the respective threshold.
5. Changes are saved instantly – no “Save” button required. You can close the dialog by tapping outside or the OK button.

The dialog is implemented in `SettingsFragment.kt` (or `SettingsDialog.kt`). All values are displayed as numbers next to the sliders.

---

## Cursor Movement (Not Adjustable via Settings)

Cursor movement is controlled by **sensitivity** (separate slider on the main screen, not in Settings). The movement deltas are:

```
deltaX = (yaw_change) * sensitivity * 0.8
deltaY = (roll_change) * sensitivity * 0.8
```

This is not a gesture, but a continuous mapping. The sensitivity slider ranges from 0.2 to 2.0. No threshold is used – every tiny orientation change generates a move packet.

---

## Click & Double‑Click Parameters

These two gestures share the same angular velocity threshold but are separated by time.

### Click Speed Threshold (rad/s)

- **Default:** 5.0 rad/s
- **Range:** 0–10 rad/s (slider maps 0–100 to 0–10)
- **What it does:** The absolute angular velocity around the Y‑axis `|gyroY|` is compared to this threshold. If exceeded, a **potential click** is recorded.
- **How it works in code (`EnhancedGestureDetector.detectClick()`):**
  1. If `|gyroY| > clickSpeedThreshold` AND the time since the last click is greater than `doubleClickInterval`:
     - If `potentialDoubleClick` is already true → **double click**.
     - Else, set a timer for `doubleClickInterval` and return **single click** (or wait, depending on implementation).
- **Effect of increasing the threshold:**
  - You need a **faster flick** to trigger a click.
  - Reduces false positives (accidental clicks from small hand movements).
- **Effect of decreasing the threshold:**
  - Easier to click (even small flicks register).
  - May cause accidental clicks during normal cursor movement.

**Recommendation:** Start with 5.0. If you frequently get unwanted clicks, increase to 6.0–7.0. If you struggle to click, decrease to 3.0–4.0.

### Double‑Click Interval (ms)

- **Default:** 400 ms
- **Range:** 200–1000 ms (slider 0–80 maps to 200–1000)
- **What it does:** The maximum time allowed between two consecutive flicks to count as a double‑click.
- **How it works:** After the first flick, a timer starts. If a second flick occurs before the timer expires, a double‑click is sent. If the timer expires first, a single click is sent.
- **Effect of increasing the interval:** Easier to perform double‑click (more time between flicks), but may cause false double‑clicks if you flick twice slowly by accident.
- **Effect of decreasing the interval:** Harder to double‑click (needs two very quick flicks), reduces false positives.

**Recommendation:** 400 ms is standard. If you have fast fingers, 300 ms works. If you need more time, 500–600 ms.

---

## Right‑Click Parameters

Right‑click is detected by holding a significant tilt (roll angle) for a certain duration. This distinguishes it from normal cursor movement (which also uses roll but does not hold).

### Right‑Click Tilt Angle (degrees)

- **Default:** 45°
- **Range:** 0–90° (slider 0–100 maps to 0–90)
- **What it does:** The absolute roll angle `|roll|` (converted from radians to degrees) must exceed this value to start the right‑click timer.
- **How it works:** When `|roll| > rightClickTiltAngle`, the app records the start time. If the angle remains above the threshold for `rightClickDuration` milliseconds, a right‑click is triggered. If the angle drops below before the time elapses, the timer resets.
- **Effect of increasing the angle:** Requires more tilt (more extreme orientation) to trigger right‑click. Reduces accidental right‑clicks during normal movement.
- **Effect of decreasing the angle:** Right‑click triggers with less tilt – easier but may happen when you don’t intend it.

**Recommendation:** 45° is a good balance. For people with large hands or who tilt a lot, increase to 60°. For users who find it hard to tilt enough, decrease to 30°.

### Right‑Click Duration (ms)

- **Default:** 500 ms
- **Range:** 100–1000 ms (slider 0–90 maps to 100–1000)
- **What it does:** How long the tilt must be held above the angle threshold to trigger a right‑click.
- **How it works:** The phone must remain tilted beyond the angle for this entire duration. If you tilt and release quickly, no right‑click.
- **Effect of increasing duration:** More deliberate hold required – fewer accidental right‑clicks, but slower to trigger.
- **Effect of decreasing duration:** Faster right‑click, but may trigger if you briefly tilt during normal cursor movement.

**Recommendation:** 500 ms is standard. For faster response, 300 ms. For safety (avoid accidental triggers), 700 ms.

---

## Scroll Parameters

Scroll is detected from linear acceleration along the Y‑axis (push up/down). It uses a threshold for activation and a debounce to prevent repeated scrolls from a single push.

### Scroll Speed Threshold (m/s²)

- **Default:** 8.0 m/s²
- **Range:** 0–15 m/s² (slider 0–75 maps to 0–15)
- **What it does:** The absolute linear acceleration `|accelY|` must exceed this value to trigger a scroll.
- **How it works:** When a scroll is not already in progress, if `|accelY| > scrollSpeedThreshold`, a scroll event is sent (positive acceleration → scroll down, negative → scroll up). The `scrollInProgress` flag is set to true.
- **Effect of increasing threshold:** Requires a harder, faster push to scroll – reduces accidental scrolls from hand tremors.
- **Effect of decreasing threshold:** Very easy to scroll – may happen when you only meant to tilt.

**Recommendation:** 8.0 m/s² works for most people. If you get unwanted scrolling, increase to 10–12. If you find scrolling difficult, lower to 6.

### Scroll Debounce (m/s²)

- **Default:** 2.0 m/s²
- **Range:** 0–5 m/s² (slider 0–50 maps to 0–5)
- **What it does:** After a scroll is triggered, the acceleration must fall below this value before another scroll can be detected. Prevents multiple scrolls from a single push.
- **How it works:** After a scroll, `scrollInProgress = true`. While `|accelY| > scrollDebounce`, no new scroll is detected. When `|accelY|` drops below the debounce threshold, `scrollInProgress` is reset to false.
- **Effect of increasing debounce:** Requires the phone to become very still before another scroll can happen – may feel sluggish.
- **Effect of decreasing debounce:** You can scroll repeatedly more quickly, but may get double scrolls from one push if the acceleration oscillates.

**Recommendation:** 2.0 m/s² is fine. If you get double scrolls from a single push, increase to 3.0. If you find scrolling sluggish, decrease to 1.0.

---

## How Thresholds Are Applied (Code Internals)

All parameters are stored in `PreferencesDataStore` and accessed via `prefs.getClickThreshold()`, `prefs.getDoubleClickInterval()`, etc. The `EnhancedGestureDetector` class reads them each time `reloadThresholds()` is called (which happens after the Settings dialog closes, and also at initialisation).

The actual detection logic (simplified):

```kotlin
fun detect(gyroY: Float, accelY: Float, roll: Float): Gesture {
    val now = System.currentTimeMillis()
    val angularSpeed = abs(gyroY)
    if (angularSpeed > prefs.getClickThreshold() && 
        now - lastClickTime > prefs.getDoubleClickInterval()) {
        // click / double-click logic
    }
    if (abs(roll) > prefs.getRightClickTilt() && !rightClickTriggered) {
        // right-click timer logic
    }
    if (abs(accelY) > prefs.getScrollThreshold() && !scrollInProgress) {
        scrollInProgress = true
        return if (accelY > 0) SCROLL_DOWN else SCROLL_UP
    } else if (abs(accelY) < prefs.getScrollDebounce()) {
        scrollInProgress = false
    }
    return NONE
}
```

All comparisons use absolute values, so direction (left/right, up/down) is handled separately.

---

## Recommended Settings for Different Users

| User profile | Click speed | Double‑click interval | Right‑click angle | Right‑click duration | Scroll speed | Scroll debounce |
|--------------|-------------|----------------------|-------------------|----------------------|--------------|-----------------|
| **Default (balanced)** | 5.0 | 400 | 45° | 500 | 8.0 | 2.0 |
| **Sensitive (easy gestures)** | 3.0 | 600 | 35° | 300 | 6.0 | 1.5 |
| **Stable (avoid false positives)** | 7.0 | 300 | 60° | 700 | 10.0 | 3.0 |
| **Fast flicks / gamer** | 4.0 | 250 | 50° | 400 | 9.0 | 2.0 |
| **Large hands / strong movements** | 6.0 | 400 | 55° | 600 | 9.0 | 2.5 |

**Note:** These are starting points. You should experiment to find what feels natural.

---

## Troubleshooting Gesture Detection

| Problem | Likely cause | Solution |
|---------|--------------|----------|
| No click when you flick | Click speed threshold too high | Lower it (e.g., 4.0) or flick harder |
| Click triggers accidentally during normal rotation | Threshold too low | Increase it (e.g., 6.0) |
| Double‑click never works | Double‑click interval too short or flicks not fast enough | Increase interval (e.g., 500 ms) or practise faster flicks |
| Right‑click doesn’t trigger | Tilt angle too high or duration too long | Lower angle (e.g., 35°) or shorten duration (e.g., 300 ms) |
| Right‑click triggers when you don’t want it | Angle too low or duration too short | Increase angle to 50–60° or duration to 700 ms |
| No scroll when you push | Scroll speed threshold too high | Lower to 6.0 or push harder |
| Scroll triggers multiple times from one push | Debounce too low | Increase to 3.0–4.0 |
| Scroll feels sluggish (slow to reset) | Debounce too high | Lower to 1.0–1.5 |
| Cursor movement is too fast/slow | Sensitivity (not in Settings) | Adjust main screen slider |

---

## Summary Table of Parameters

| Parameter | Default | Range | What it controls |
|-----------|---------|-------|------------------|
| Click speed threshold | 5.0 rad/s | 0–10 | Minimum flick speed to register a click |
| Double‑click interval | 400 ms | 200–1000 | Max time between two flicks for double click |
| Right‑click tilt angle | 45° | 0–90 | How far to tilt sideways to start right‑click |
| Right‑click duration | 500 ms | 100–1000 | How long to hold that tilt |
| Scroll speed threshold | 8.0 m/s² | 0–15 | Minimum push acceleration to scroll |
| Scroll debounce | 2.0 m/s² | 0–5 | Acceleration below which scroll resets |

All settings are **live** – no restart required. Use the **Settings** dialog in the app to fine‑tune the Air Mouse to your personal preferences.

---

This complete guide gives you full understanding of every gesture parameter, enabling you to customise the Air Mouse for optimal performance. Adjust, test, and enjoy a personalised motion‑controlled mouse!