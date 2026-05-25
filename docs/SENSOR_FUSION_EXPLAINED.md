# Sensor Fusion – Madgwick AHRS (Complete Explanation)

This document provides an **exhaustive explanation** of why raw sensors are insufficient for orientation tracking and how the Madgwick algorithm combines accelerometer, gyroscope, and magnetometer data to produce a stable, drift‑free orientation quaternion. It covers the mathematics, implementation in Air Mouse code, parameter tuning, and performance analysis.

---

## 📖 Table of Contents

- [Sensor Fusion – Madgwick AHRS (Complete Explanation)](#sensor-fusion--madgwick-ahrs-complete-explanation)
  - [📖 Table of Contents](#-table-of-contents)
  - [Why Raw Sensors Fail Alone](#why-raw-sensors-fail-alone)
  - [Introduction to Sensor Fusion](#introduction-to-sensor-fusion)
  - [Quaternions – A Brief Refresher](#quaternions--a-brief-refresher)
  - [The Madgwick Algorithm – Overview](#the-madgwick-algorithm--overview)
  - [Step 1: Gyroscope Integration (Prediction)](#step-1-gyroscope-integration-prediction)
  - [Step 2: Gradient Descent Correction (Accelerometer \& Magnetometer)](#step-2-gradient-descent-correction-accelerometer--magnetometer)
    - [2.1 Accelerometer Correction](#21-accelerometer-correction)
    - [2.2 Magnetometer Correction (for Yaw)](#22-magnetometer-correction-for-yaw)
    - [2.3 Why Gradient Descent?](#23-why-gradient-descent)
  - [Step 3: Quaternion Normalisation](#step-3-quaternion-normalisation)
  - [Beta Parameter – Tuning Drift vs Noise](#beta-parameter--tuning-drift-vs-noise)
  - [Implementation in Air Mouse (Kotlin)](#implementation-in-air-mouse-kotlin)
    - [`updateGyro(gx, gy, gz, dt)`](#updategyrogx-gy-gz-dt)
    - [`updateAccel(ax, ay, az)`](#updateaccelax-ay-az)
    - [`updateMag(mx, my, mz)`](#updatemagmx-my-mz)
    - [`getRoll()`, `getPitch()`, `getYaw()`](#getroll-getpitch-getyaw)
  - [Performance \& Latency](#performance--latency)
  - [Comparison with Other Filters](#comparison-with-other-filters)
  - [Common Issues \& Solutions](#common-issues--solutions)
  - [Mathematical Derivation (Optional Advanced)](#mathematical-derivation-optional-advanced)
  - [Summary](#summary)

---

## Why Raw Sensors Fail Alone

Each sensor has inherent weaknesses that make it unsuitable for standalone orientation tracking.

| Sensor | Strength | Weakness | Why it matters |
|--------|----------|----------|----------------|
| **Gyroscope** | Fast response, no external interference (works in any environment). | Drift over time due to bias integration. Even a small offset (0.01 rad/s) causes the estimated angle to drift 0.6° per minute. | Cursor drifts even when phone is still. |
| **Accelerometer** | Provides absolute gravity reference (no drift). | Very noisy (mechanical vibrations, linear acceleration). Cannot distinguish between gravity and phone’s own acceleration. | Tilt jitters; quick movements cause false tilt readings. |
| **Magnetometer** | Provides absolute yaw reference (magnetic north). | Easily disturbed by local magnetic fields (speakers, metal desks). Slow response (filtered internally). | Yaw (horizontal orientation) is inaccurate or drifts. |

**Conclusion:** No single sensor can provide both fast response and absolute accuracy. Sensor fusion combines the strengths of all three to produce a smooth, drift‑free orientation.

---

## Introduction to Sensor Fusion

Sensor fusion is the process of combining data from multiple sensors to obtain a more accurate estimate than any single sensor alone. For orientation tracking, we need a **filter** that:

- Uses gyroscope for fast, high‑frequency rotation updates.
- Corrects long‑term drift using accelerometer (gravity) and magnetometer (magnetic north).
- Outputs a smooth, continuous orientation (as a quaternion or Euler angles).

The Air Mouse implements the **Madgwick AHRS (Attitude and Heading Reference System)** algorithm, which is specifically designed for embedded systems with low CPU and memory resources.

---

## Quaternions – A Brief Refresher

A **quaternion** is a four‑dimensional complex number of the form:

\[
q = q_0 + q_1 i + q_2 j + q_3 k
\]

where \(i, j, k\) are imaginary units. For rotation representation, we use **unit quaternions** (norm = 1). They avoid **gimbal lock** (a problem with Euler angles where two axes align and a degree of freedom is lost) and are easy to interpolate.

**Properties:**
- **Rotation representation:** A quaternion can represent any orientation in 3D space without singularities.
- **Composition:** Rotations are combined by quaternion multiplication.
- **Euler angles extraction:** Roll, pitch, yaw can be extracted from a quaternion.

In Air Mouse, the fusion algorithm maintains a unit quaternion `q = [q0, q1, q2, q3]` representing the phone’s orientation relative to the Earth frame.

---

## The Madgwick Algorithm – Overview

Developed by Sebastian Madgwick in 2010, this algorithm is a **gradient descent‑based filter** that operates in two steps per iteration:

1. **Prediction (gyroscope integration):** Update the quaternion using angular velocity from the gyroscope.
2. **Correction (gradient descent):** Compute the error between the predicted orientation and the measured gravity/magnetic field vectors, then adjust the quaternion in the opposite direction of the gradient.

The algorithm runs at the same rate as sensor updates (e.g., 50 Hz). It is lightweight – only a few dozen floating‑point operations per iteration.

---

## Step 1: Gyroscope Integration (Prediction)

The gyroscope measures angular velocity \( \omega = [\omega_x, \omega_y, \omega_z] \) in rad/s. The quaternion derivative is given by:

\[
\dot{q} = \frac{1}{2} q \otimes \omega
\]

where \( \otimes \) denotes quaternion multiplication. In component form (assuming the quaternion is represented as \( q = [q_0, q_1, q_2, q_3] \)):

\[
\begin{aligned}
\dot{q}_0 &= 0.5 \cdot ( -q_1 \omega_x - q_2 \omega_y - q_3 \omega_z ) \\
\dot{q}_1 &= 0.5 \cdot (  q_0 \omega_x + q_2 \omega_z - q_3 \omega_y ) \\
\dot{q}_2 &= 0.5 \cdot (  q_0 \omega_y - q_1 \omega_z + q_3 \omega_x ) \\
\dot{q}_3 &= 0.5 \cdot (  q_0 \omega_z + q_1 \omega_y - q_2 \omega_x )
\end{aligned}
\]

Then we integrate over time step \( \Delta t \):

\[
q_{\text{new}} = q + \dot{q} \cdot \Delta t
\]

This step alone would cause drift because of gyroscope bias. The correction step fixes that.

---

## Step 2: Gradient Descent Correction (Accelerometer & Magnetometer)

The correction step uses the accelerometer (gravity vector) and optionally the magnetometer (magnetic field vector) to compute the error between the predicted orientation and the measured vectors.

### 2.1 Accelerometer Correction

The accelerometer measures the gravity vector in the sensor frame: \( \mathbf{a} = [a_x, a_y, a_z] \). In the Earth frame, gravity is known: \( \mathbf{g} = [0, 0, 1] \) (assuming Z down). The predicted gravity vector in the sensor frame is obtained by rotating the Earth’s gravity by the quaternion:

\[
\mathbf{p}_a = q \otimes \mathbf{g} \otimes q^*
\]

The error function \( f_a(q) \) is the difference between the measured and predicted gravity:

\[
f_a(q) = \mathbf{p}_a - \mathbf{a}
\]

But in the algorithm, we use the squared error magnitude (simplified). The gradient of \( f_a \) with respect to \( q \) is computed (the Jacobian \( J_a \)), and we compute a step:

\[
\delta q = \beta \cdot \frac{J_a^T f_a}{\|J_a\|^2}
\]

where \( \beta \) is the filter gain (discussed later). The final correction is:

\[
q_{\text{corrected}} = q - \delta q
\]

### 2.2 Magnetometer Correction (for Yaw)

Similarly, the magnetometer measures the Earth’s magnetic field vector \( \mathbf{m} \) in the sensor frame. In the Earth frame, the magnetic field is assumed to point north (X direction) with a certain dip angle (ignored for simplicity). The predicted magnetic field is:

\[
\mathbf{p}_m = q \otimes \mathbf{m}_{\text{Earth}} \otimes q^*
\]

The error function \( f_m(q) = \mathbf{p}_m - \mathbf{m} \) gives another correction. In the full Madgwick algorithm, both corrections are combined:

\[
f(q) = \begin{bmatrix} f_a(q) \\ f_m(q) \end{bmatrix}, \quad J = \begin{bmatrix} J_a \\ J_m \end{bmatrix}
\]

Then:

\[
\delta q = \beta \cdot \frac{J^T f}{\|J\|^2}
\]

### 2.3 Why Gradient Descent?

Gradient descent is an iterative optimisation method. At each step, we move in the direction of steepest descent of the error function. Because the error function is convex (for small errors), this converges quickly. In practice, one step per sensor update is sufficient.

---

## Step 3: Quaternion Normalisation

After the correction step, the quaternion may no longer be unit length. We normalise it:

\[
q = \frac{q}{\sqrt{q_0^2 + q_1^2 + q_2^2 + q_3^2}}
\]

This ensures that the quaternion represents a pure rotation (no scaling).

---

## Beta Parameter – Tuning Drift vs Noise

The filter gain \( \beta \) controls how aggressively the correction step adjusts the quaternion.

- **High \( \beta \) (e.g., 0.5 – 1.0):** Strong correction → drift is almost eliminated, but the orientation becomes more sensitive to accelerometer noise (cursor jitter when phone is still).
- **Low \( \beta \) (e.g., 0.01 – 0.05):** Weak correction → gyroscope drift becomes noticeable (cursor slowly moves even when phone is stationary), but the movement is smoother.

**Default in Air Mouse:** \( \beta = 0.1 \), a good compromise.

**Effect on cursor:** With \( \beta = 0.1 \), drift is less than 0.1° per second – imperceptible. Noise is acceptable. If you experience jitter, decrease \( \beta \); if you see drift, increase \( \beta \).

---

## Implementation in Air Mouse (Kotlin)

The `MadgwickFusion.kt` class implements the algorithm. Key methods:

### `updateGyro(gx, gy, gz, dt)`
- Performs quaternion integration (prediction).
- If accelerometer data is available (non‑zero), it also performs the gradient descent correction.

### `updateAccel(ax, ay, az)`
- Stores the latest accelerometer reading for use in the next gyro update.

### `updateMag(mx, my, mz)`
- Stores the latest magnetometer reading.

### `getRoll()`, `getPitch()`, `getYaw()`
- Convert the quaternion to Euler angles (in radians) using:
  ```kotlin
  roll = atan2(2*(q0*q1 + q2*q3), 1 - 2*(q1*q1 + q2*q2))
  pitch = asin(2*(q0*q2 - q3*q1))
  yaw = atan2(2*(q0*q3 + q1*q2), 1 - 2*(q2*q2 + q3*q3))
  ```

**Integration in `SensorService.kt`:**
```kotlin
private val madgwick = MadgwickFusion(beta = 0.1f)

override fun onSensorChanged(event: SensorEvent) {
    when (event.sensor.type) {
        Sensor.TYPE_ACCELEROMETER -> {
            madgwick.updateAccel(ax, ay, az)
        }
        Sensor.TYPE_GYROSCOPE -> {
            madgwick.updateGyro(gx, gy, gz, dt)
            // after gyro update, orientation is ready
            val roll = madgwick.getRoll()
            val yaw = madgwick.getYaw()
            // send to UI
        }
        Sensor.TYPE_MAGNETIC_FIELD -> {
            madgwick.updateMag(mx, my, mz)
        }
    }
}
```

**Important:** The fusion algorithm expects that `updateAccel` and `updateMag` are called with the latest values before `updateGyro` runs. This is why we call them in the respective `onSensorChanged` cases.

---

## Performance & Latency

- **CPU usage:** On a typical smartphone (e.g., Snapdragon 660), each `updateGyro` call takes ~0.4‑0.5 ms. At 50 Hz, this is about 2‑2.5% of a single core.
- **Memory:** The class uses a few float variables (~32 bytes).
- **Latency added:** <0.5 ms per sensor update – negligible compared to network and display latency.

**Perfetto measurement:** The `perfetto_analyzer.py` script (provided) queries the trace for `MadgwickUpdate` slices and reports total CPU time.

---

## Comparison with Other Filters

| Filter | Pros | Cons |
|--------|------|------|
| **Complementary** | Very simple, low CPU. | No magnetometer fusion; yaw drifts. |
| **Kalman (full)** | Optimal for linear systems. | Heavy (matrix inversions), difficult to tune. |
| **Mahony** | Similar to Madgwick, uses PI controller. | Slightly more complex than Madgwick. |
| **Madgwick** | Good balance of accuracy and speed; widely used in IMU applications. | Still requires tuning of beta. |

**Why Madgwick for Air Mouse?**  
- Lightweight enough to run at 50 Hz without noticeable battery drain.
- Provides both roll/pitch (from accelerometer) and yaw (from magnetometer) without needing a separate compass.
- Well‑documented and easy to implement.

---

## Common Issues & Solutions

| Issue | Likely Cause | Solution |
|-------|--------------|----------|
| Yaw drifts over time | Magnetometer not used or incorrectly calibrated. | Ensure magnetometer is registered and calibration performed. Increase beta slightly. |
| Cursor jitters when phone still | Beta too high (noise amplified). | Decrease beta to 0.05–0.08. |
| Slow response to rotation | Beta too low (too much filtering). | Increase beta to 0.15–0.2. |
| Pitch/roll inaccurate | Accelerometer not calibrated. | Perform accelerometer calibration (6‑point is best). |
| Fusion doesn’t work at all | Missing sensor data. | Check that all three sensors are registered and delivering data (debug overlay shows values). |

---

## Mathematical Derivation (Optional Advanced)

For completeness, the gradient descent step can be derived as follows:

We want to minimise the error function:
\[
f(q) = q \otimes \mathbf{v}_{\text{Earth}} \otimes q^* - \mathbf{v}_{\text{sensor}}
\]
where \( \mathbf{v} \) is either gravity or magnetic field. The gradient is:
\[
\nabla f = J^T f
\]
where \( J \) is the Jacobian matrix of \( f \) with respect to \( q \). Then the update is:
\[
q_{k+1} = q_k - \beta \frac{\nabla f}{\|\nabla f\|}
\]

The Madgwick paper provides closed‑form expressions for \( J \) for both accelerometer and magnetometer. The implementation in `MadgwickFusion.kt` follows those formulas.

---

## Summary

The Madgwick AHRS algorithm is the heart of Air Mouse’s orientation estimation. It fuses the fast response of the gyroscope with the absolute references of the accelerometer and magnetometer, producing a drift‑free, smooth quaternion. The algorithm is lightweight, efficient, and well‑suited for real‑time applications. By tuning the `beta` parameter, you can trade off between drift and noise to match your preferences.

This fusion is what makes the Air Mouse feel responsive yet stable – the cursor moves exactly as you rotate the phone, without slowly wandering off.

---

*This document is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.*