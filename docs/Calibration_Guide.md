# Air Mouse – Complete Calibration Guide

This document provides an **exhaustive explanation** of the calibration process required for the Air Mouse. Calibration is the most critical step to achieve accurate, drift‑free cursor control. Here you will learn:

- **Why each calibration is necessary** (physics of sensors)
- **Step‑by‑step instructions** (what to do and what not to do)
- **How calibration works internally** (the mathematics behind bias removal, hard‑iron correction, and offset compensation)
- **Troubleshooting calibration issues** (what to do if calibration fails or results are poor)

---

## 📖 Table of Contents

- [Air Mouse – Complete Calibration Guide](#air-mouse--complete-calibration-guide)
  - [📖 Table of Contents](#-table-of-contents)
  - [Why Calibration Is Essential](#why-calibration-is-essential)
  - [Overview of the Three Calibration Steps](#overview-of-the-three-calibration-steps)
  - [Step 1: Gyroscope Bias Calibration](#step-1-gyroscope-bias-calibration)
    - [What It Does](#what-it-does)
    - [How to Perform](#how-to-perform)
    - [Under the Hood](#under-the-hood)
    - [Common Mistakes](#common-mistakes)
  - [Step 2: Magnetometer Hard‑Iron Calibration](#step-2-magnetometer-hardiron-calibration)
    - [What It Does](#what-it-does-1)
    - [How to Perform (Figure‑8 Pattern)](#how-to-perform-figure8-pattern)
    - [Under the Hood](#under-the-hood-1)
    - [Why Figure‑8?](#why-figure8)
    - [Common Mistakes](#common-mistakes-1)
  - [Step 3: Accelerometer Calibration (Simplified)](#step-3-accelerometer-calibration-simplified)
    - [What It Does](#what-it-does-2)
    - [How to Perform](#how-to-perform-1)
    - [Limitations and When to Use Full 6‑Point Calibration](#limitations-and-when-to-use-full-6point-calibration)
  - [When to Re‑calibrate](#when-to-recalibrate)
  - [Advanced: Full Accelerometer 6‑Point Calibration](#advanced-full-accelerometer-6point-calibration)
    - [Principle](#principle)
    - [Ideal gravity vectors:](#ideal-gravity-vectors)
    - [Measured values:](#measured-values)
    - [Solving for offset and scale:](#solving-for-offset-and-scale)
    - [Implementation](#implementation)
  - [Troubleshooting Calibration Issues](#troubleshooting-calibration-issues)
  - [Summary](#summary)

---

## Why Calibration Is Essential

Raw sensor readings contain systematic errors that, if left uncorrected, cause:

- **Gyroscope** – Bias (offset) makes the cursor drift even when the phone is perfectly still.
- **Magnetometer** – Hard‑iron distortion (offset from nearby metals) makes the yaw (horizontal orientation) permanently wrong.
- **Accelerometer** – Offset and scale errors make the tilt (roll/pitch) inaccurate, causing the cursor to move diagonally when you expect pure horizontal/vertical movement.

Calibration measures these errors and stores correction parameters (bias, offset, scale) that are applied to every subsequent sensor reading. Without calibration, the Air Mouse is unusable for precise pointing.

---

## Overview of the Three Calibration Steps

The Android app guides you through three steps. They must be performed **in order** because gyro bias is independent, magnetometer correction requires full‑range motion, and accelerometer offset is best measured after the phone is stationary (which is already true after the previous steps).

| Step | Name | Duration | User Action | Corrects |
|------|------|----------|-------------|----------|
| 1 | Gyro bias | ~3 sec | Place phone flat on a stationary surface (table) | Gyro offset (rad/s) |
| 2 | Magnetometer hard‑iron | 30 sec | Move phone in a continuous figure‑8 pattern | Magnetometer offset (µT) |
| 3 | Accelerometer (simplified) | ~3 sec | Keep phone still on a flat surface | Accelerometer offset (m/s²) |

After successful completion, the app sets a flag `isCalibrated = true` and the **Start Air Mouse** button becomes active.

---

## Step 1: Gyroscope Bias Calibration

### What It Does

The gyroscope measures angular velocity (rate of rotation) in rad/s. Even when the phone is perfectly stationary, the sensor outputs a small non‑zero value due to manufacturing imperfections and temperature drift. This **bias** is typically in the range of 0.01–0.1 rad/s. When integrated over time to obtain orientation, bias causes continuous drift – the cursor moves slowly even when the phone is on a table.

**Calibration** collects 500 gyroscope samples (≈2‑3 seconds) and computes the average value for each axis. These averages become the **bias** that will be subtracted from all future readings.

### How to Perform

1. **Place the phone on a flat, stationary surface** (e.g., a desk, a table, or a hardcover book).  
   → Do **not** hold it in your hand – your hand’s micro‑vibrations will be averaged into the bias, ruining the calibration.
2. **Ensure the phone does not move** during the entire 3 seconds. No tapping, no sliding, no rotating.
3. Wait for the app to automatically proceed to step 2.

### Under the Hood

The code in `CalibrationHelper.kt` (or `CalibrationUseCase.kt`) does:

```kotlin
val samples = mutableListOf<FloatArray>()
// collect 500 events
// after collection:
gyroBias[0] = samples.map { it[0] }.average().toFloat()
gyroBias[1] = samples.map { it[1] }.average().toFloat()
gyroBias[2] = samples.map { it[2] }.average().toFloat()
```

Later, during normal operation, every gyro reading is corrected as:

```
corrected = raw - bias
```

### Common Mistakes

| Mistake | Consequence | Fix |
|---------|-------------|-----|
| Holding the phone in your hand | Bias includes hand tremor → drift remains | Place on table, re‑calibrate |
| Phone on an unstable surface (e.g., sofa) | Movement during collection → wrong bias | Use a rigid surface |
| Moving the phone too early (before the step finishes) | Calibration interrupted; step repeats? No, the step will end but with bad data | Let it finish; if cursor drifts, re‑do calibration |

---

## Step 2: Magnetometer Hard‑Iron Calibration

### What It Does

The magnetometer measures the Earth’s magnetic field (in microtesla). Ideally, when you rotate the phone in all directions, the measured field vectors should form a sphere centred at (0,0,0). However, nearby magnets (laptop speakers, metal table, ferromagnetic materials in the phone itself) add a constant offset to each axis – this is called **hard‑iron distortion**. The result is that the sphere is shifted away from the origin. Without correction, the yaw (compass direction) is permanently wrong, and the cursor’s horizontal orientation will be inaccurate.

**Calibration** records the minimum and maximum values for each axis while you move the phone in all orientations. The offset is computed as `(max + min) / 2` and the scale as `(max - min) / 2` (if the ellipsoid is not axis‑aligned, a full 3x3 matrix would be needed, but this simplified method works well for hard‑iron only).

### How to Perform (Figure‑8 Pattern)

1. **Pick up the phone** after the gyro step finishes.
2. **Move it in a continuous figure‑8 pattern** for the full 30 seconds.
   - Imagine drawing a horizontal figure‑8 (∞ symbol) in the air.
   - Rotate your wrist so that the phone’s axes point in all possible directions.
   - Do not just move in a circle – the figure‑8 ensures that each axis reaches its extreme positive and negative values.
3. **Cover all orientations**: up, down, left, right, tilt forward, tilt backward, rotate around each axis.
4. Continue until the progress bar reaches 100% and the app moves to step 3.

### Under the Hood

The code collects min and max for each axis:

```kotlin
if (event.values[i] < min[i]) min[i] = event.values[i]
if (event.values[i] > max[i]) max[i] = event.values[i]
```

After 30 seconds:

```kotlin
offset[i] = (min[i] + max[i]) / 2
scale[i] = (max[i] - min[i]) / 2
if (scale[i] == 0f) scale[i] = 1f
```

During normal operation:

```
corrected = (raw - offset) / scale
```

### Why Figure‑8?

A figure‑8 motion naturally forces the phone to pass through all three axes’ extreme values. A simple circular motion would only cover two axes, leaving the third axis’s min/max unrecorded. The figure‑8 is the most efficient way to sample the full 3D space.

### Common Mistakes

| Mistake | Consequence | Fix |
|---------|-------------|-----|
| Moving only in a circle | Some axes never reach extremes → incorrect offset | Use a deliberate figure‑8 |
| Moving too slowly | May not cover all orientations within 30 seconds | Move at moderate speed (1 figure‑8 per 2‑3 seconds) |
| Staying near metal objects (laptop, speaker, metal table) | The measured offsets include those external fields; after calibration, yaw will still be affected when you move away | Calibrate away from metal, then use the phone away from metal |
| Stopping before 30 seconds | Incomplete data → wrong offset | Let the timer run fully |

---

## Step 3: Accelerometer Calibration (Simplified)

### What It Does

The accelerometer measures linear acceleration plus gravity. When the phone is stationary, the magnitude of the reading should be exactly 9.81 m/s² and the direction should be down (negative Z if the phone is lying flat). However, due to offset and scale errors, the readings may be slightly off (e.g., `(0.1, 0.2, 9.7)`). This causes the computed tilt (roll/pitch) to be inaccurate.

The **simplified** calibration assumes that the factory scale is correct and only corrects the offset. It collects 200 stationary samples and computes the average. It then sets the offset for each axis as `avg[i]` for X and Y, and `avg[Z] - 9.81` for Z (because gravity should be 9.81 in Z when the phone is flat).

### How to Perform

- **Keep the phone still on a flat surface** for about 3 seconds.
- No special movement required.
- The app will automatically proceed and show “Calibration complete!”

### Limitations and When to Use Full 6‑Point Calibration

The simplified method works well for most phones, but if you notice that the tilt (e.g., when you hold the phone level, the cursor still moves vertically) is off, you need the **full 6‑point calibration** (described in the Advanced section). The full method corrects both offset and scale errors by measuring the phone in six orientations (±X, ±Y, ±Z).

---

## When to Re‑calibrate

- **After first installation** – mandatory.
- **When you change location** – especially if moving between rooms or buildings (magnetic environment changes).
- **If you notice cursor drift** (phone still but cursor moves) – re‑do gyro and magnetometer.
- **If the yaw (horizontal orientation) seems incorrect** – re‑do magnetometer (figure‑8).
- **After the phone has been near strong magnets** (e.g., placed on a speaker, near a fridge magnet).
- **If the accelerometer tilt seems off** (phone level but cursor drifts vertically) – re‑do accelerometer.

**Tip:** You can re‑calibrate at any time; it does not affect your settings (sensitivity, thresholds).

---

## Advanced: Full Accelerometer 6‑Point Calibration

If you are not satisfied with the simplified calibration, implement the full 6‑point method. This is **not part of the default app** but can be added by extending `CalibrationHelper.kt`.

### Principle

You place the phone in **six orientations** such that each axis (±X, ±Y, ±Z) is aligned with gravity. For each orientation, you collect 100–200 samples and average them. Then you solve for offset and scale per axis using two equations (positive and negative gravity).

### Ideal gravity vectors:

| Orientation | Ideal vector (X, Y, Z) in m/s² |
|-------------|-------------------------------|
| +X | (9.81, 0, 0) |
| -X | (-9.81, 0, 0) |
| +Y | (0, 9.81, 0) |
| -Y | (0, -9.81, 0) |
| +Z | (0, 0, 9.81) |
| -Z | (0, 0, -9.81) |

### Measured values:

For each orientation you collect the average of raw accelerometer readings (after gyro and mag calibration, but without accelerometer correction). Let’s call them `(mx_pos, my_pos, mz_pos)` for the +X orientation, etc.

### Solving for offset and scale:

For the X axis:
```
posMeas_X = scale_X * 9.81 + offset_X
negMeas_X = scale_X * (-9.81) + offset_X
```
Subtract the two equations:
```
posMeas_X - negMeas_X = scale_X * 19.62
scale_X = (posMeas_X - negMeas_X) / 19.62
```
Then:
```
offset_X = posMeas_X - scale_X * 9.81
```
Similarly for Y and Z axes.

### Implementation

You would need to create a UI that guides the user through the six orientations. The provided `CalibrationUseCase` already has a function `calibrateAccelerometer(measuredOrientations: List<FloatArray>)` that does the computation. You would call it with your collected data.

---

## Troubleshooting Calibration Issues

| Problem | Likely Cause | Solution |
|---------|--------------|----------|
| Cursor still drifts after calibration | Gyro bias not measured correctly (phone moved) | Re‑calibrate gyro on a flat, still surface. Use a table, not your hand. |
| Yaw drifts over time (horizontal orientation changes) | Magnetometer calibration incomplete or performed near metal | Re‑calibrate magnetometer away from metal. Use a larger figure‑8. |
| Tilt (roll/pitch) is off – phone level but cursor moves vertically | Accelerometer not calibrated (simplified not enough) | Implement full 6‑point accelerometer calibration. |
| Calibration fails with an error toast | Sensor not available or exception in calibration code | Check that the phone has all three sensors; look at logcat for stack trace. |
| The app skips magnetometer calibration (no figure‑8 prompt) | `calibrateMagnetometer` was not called or duration was 0 | Ensure the code in `MainActivity` calls `calibrateHelper.calibrateMagnetometer(30000)`. |
| Progress bar jumps from 10% to 100% instantly | The `progressBar.progress` values are set directly without incremental updates | The provided code sets discrete steps (10, 50, 90, 100) – this is intentional to simplify UI. Not a problem. |

---

## Summary

Calibration is the foundation of a reliable Air Mouse. Perform the three steps carefully:

1. **Gyro bias** – phone still on table (3 sec) → removes drift.
2. **Magnetometer** – figure‑8 for 30 sec → corrects yaw.
3. **Accelerometer** – still on table (3 sec) → corrects tilt.

Re‑calibrate when you change location or notice any anomaly. For best results, follow the tips: calibrate away from metal, cover all orientations, and never move during gyro step.

This guide provides everything you need to understand and perform calibration correctly. Use it as a reference for your report and for daily operation.