---
name: sensor-fusion-dsp
description: >-
  Digital signal processing and sensor fusion guidance for AirMouse: One Euro Filter tuning,
  Zero Velocity Update (ZUPT) drift cancellation, gyroscope bias calibration, quaternion-based
  spatial tracking, and low-latency cursor movement stabilization.
---

# Sensor Fusion & DSP for AirMouse Spatial Tracking

This skill guides the implementation, mathematical tuning, and debugging of digital signal processing (DSP) filters and sensor fusion algorithms used for inertial cursor control.

## 1. One Euro Filter ($1€$) Formulation

The One Euro Filter provides adaptive low-pass filtering: high smoothing (low cutoff) at low speeds to eliminate jitter, and low lag (high cutoff) during fast movements to prevent sluggishness.

### Formulation
For a signal $x_t$ sampled at timestamp $t$ with sampling period $T_e = t - t_{prev}$:

1. **Filtered Derivative (Speed Estimation):**
   $$\hat{\dot{x}}_t = \frac{x_t - \hat{x}_{t-1}}{T_e}$$
   $$\dot{x}_{filtered} = \alpha_d \hat{\dot{x}}_t + (1 - \alpha_d) \dot{x}_{filtered, t-1}$$
   $$\alpha_d = \frac{1}{1 + \frac{\tau_d}{T_e}}, \quad \tau_d = \frac{1}{2 \pi f_{c_d}}$$

2. **Adaptive Cutoff Frequency:**
   $$f_c = f_{c_{min}} + \beta |\dot{x}_{filtered}|$$

3. **Output Low-Pass Filter:**
   $$\alpha = \frac{1}{1 + \frac{\tau}{T_e}}, \quad \tau = \frac{1}{2 \pi f_c}$$
   $$\hat{x}_t = \alpha x_t + (1 - \alpha) \hat{x}_{t-1}$$

### Recommended Tuning Parameters
- **$f_{c_{min}}$ (Minimum Cutoff, e.g., 0.5 - 1.2 Hz)**: Controls stationary jitter suppression. Lower values eliminate tremor when aiming at small UI elements.
- **$\beta$ (Speed Coefficient, e.g., 0.005 - 0.08)**: Controls responsiveness during rapid motion. Higher values reduce latency during fast flick movements.
- **$f_{c_d}$ (Derivative Cutoff, default: 1.0 Hz)**: Filters out noise in velocity computation.

---

## 2. Zero Velocity Update (ZUPT) Drift Compensation

MEMS gyroscopes suffer from thermal drift and constant low-frequency bias, causing the mouse cursor to slowly crawl across the screen even when the phone rests stationary on a table.

### Static Detection Heuristic
Compute windowed magnitude variance of angular velocity $\omega$ and linear acceleration $a$:

$$\sigma_\omega^2 = \frac{1}{N} \sum_{i=1}^N \|\vec{\omega}_i - \bar{\vec{\omega}}\|^2 < \epsilon_\omega$$
$$\sigma_a^2 = \frac{1}{N} \sum_{i=1}^N \|\vec{a}_i - \bar{\vec{a}}\|^2 < \epsilon_a$$

- When both conditions hold for $t > 300\text{ ms}$:
  1. Flag state as `STATIONARY`.
  2. Clamp cursor delta: $dx = 0, dy = 0$.
  3. Update gyroscope bias offset via exponential moving average:
     $$\vec{b}_{gyro} \leftarrow (1 - \lambda) \vec{b}_{gyro} + \lambda \vec{\omega}_{measured}$$

---

## 3. Spatial Motion to 2D Cursor Mapping

For a phone held in hand pointing towards a monitor:
- **Yaw (rotation around device Y/Z depending on orientation)** $\rightarrow$ maps to horizontal cursor movement ($\Delta X$).
- **Pitch (rotation around device X)** $\rightarrow$ maps to vertical cursor movement ($\Delta Y$).

### Angular Velocity Integration
$$\Delta X = -K_x \cdot (\omega_{yaw} - b_{yaw}) \cdot \Delta t$$
$$\Delta Y = -K_y \cdot (\omega_{pitch} - b_{pitch}) \cdot \Delta t$$

- Add non-linear acceleration curves (ballistics):
  $$\text{speed} = \sqrt{\Delta X^2 + \Delta Y^2}$$
  $$\text{gain} = 1.0 + \gamma \cdot \text{speed}$$
  $$\Delta X \leftarrow \Delta X \cdot \text{gain}, \quad \Delta Y \leftarrow \Delta Y \cdot \text{gain}$$

---

## 4. Diagnostics & Testing Checklist
1. **Stationary Desk Test**: Lay the phone completely flat on a table for 30 seconds. The cursor MUST NOT drift by more than 1 pixel. If it drifts, check ZUPT threshold or gyro calibration.
2. **Circle Drawing Test**: Move the phone in a smooth circle in the air. The resulting path on the PC screen must remain circular without squishing, clipping, or phase lag.
3. **High Sampling Rate**: Android `SensorManager.SENSOR_DELAY_FASTEST` or `SENSOR_DELAY_GAME` should provide ~100 Hz to 200 Hz updates. Ensure timestamps use `event.timestamp` (nanoseconds) rather than `System.currentTimeMillis()` for jitter-free $\Delta t$.
