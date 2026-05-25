
---

## `docs/CALIBRATION_DETAILS.md`

```markdown
# Calibration Deep Dive

Proper calibration is the most critical factor for smooth, drift‑free mouse control. This document explains **why** each calibration is needed, **how** it works mathematically, and **how to do it correctly**.

## Overview

Three sensors require calibration:

| Sensor | Imperfection | Calibration Type |
|--------|--------------|------------------|
| Gyroscope | Constant bias (nonzero output when still) | Bias removal (subtract mean) |
| Accelerometer | Offset and scale errors | 6‑point or 1‑point (simplified) |
| Magnetometer | Hard‑iron distortion from nearby magnets | Min/Max (ellipsoid fitting) |

---

## 1. Gyroscope Bias Removal

### Why needed
A gyroscope measures angular velocity. Even when perfectly still, it outputs a small non‑zero value (bias) due to manufacturing imperfections and temperature. When integrated over time to get orientation, this bias causes **continuous drift** – the cursor moves even when the phone is on a table.

### Mathematical method
We record `N` samples (e.g., 500) while the phone is stationary. For each axis: