
---

## `docs/PERFETTO_ANSWERS.md`

```markdown
# Answers to the 11 Perfetto Questions

This document provides **complete, ready‑to‑submit answers** for the Perfetto analysis required in the exercise. Each answer includes:
- Explanation
- Relevant Perfetto queries
- Expected observations

---

## Q1. From the moment a sensor read request is made until data is obtained, what happens at the OS level?

**Answer:**  
The Android sensor framework operates through the following layers:

1. **App registers listener** → creates a `SensorEventQueue` via `SensorManager`.
2. **SensorService** (system service) receives the request and calls into the **HAL** (Hardware Abstraction Layer) via `activate()` and `batch()`.
3. **HAL** communicates with the kernel driver (e.g., `iio` subsystem for many sensors) to set sampling rate and enable the sensor.
4. **Kernel driver** configures the sensor hardware (often an I2C/SPI device) and starts interrupt‑driven sampling.
5. **Hardware** generates data at the requested rate, triggering an interrupt.
6. **Kernel driver** reads data via I2C/SPI, copies to a shared memory region, and wakes up the `SensorService`.
7. **SensorService** retrieves data from shared memory and enqueues it into the app’s `SensorEventQueue`.
8. **App’s `onSensorChanged`** is called on the thread that registered the listener (or a dedicated `HandlerThread`).

**Perfetto evidence:** Look for `sensor` related slices:
```sql
SELECT ts, dur, name FROM slice WHERE name GLOB '*Sensor*' LIMIT 10;