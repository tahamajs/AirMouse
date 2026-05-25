
---

## `docs/SENSOR_FUSION_EXPLAINED.md`

```markdown
# Sensor Fusion – Madgwick AHRS

This document explains why raw sensors are insufficient and how the Madgwick algorithm combines them to produce stable, drift‑free orientation.

## Why Raw Sensors Fail Alone

| Sensor | Strength | Weakness |
|--------|----------|----------|
| Gyroscope | Fast response, no external interference | Drift over time (bias integration) |
| Accelerometer | Absolute gravity reference (no drift) | Noisy, affected by linear motion |
| Magnetometer | Absolute yaw reference | Easily disturbed by metal, slow |

**Conclusion:** No single sensor is enough. We need fusion.

## The Madgwick Algorithm

Developed by Sebastian Madgwick in 2010, this algorithm is lightweight (runs on embedded devices) and outputs a **quaternion** representing orientation.

### Quaternion refresher
A quaternion is a 4‑D vector `q = [q0, q1, q2, q3]` representing rotation. It avoids gimbal lock and is easy to interpolate.

### Steps

1. **Predict** orientation using gyroscope (integration).