package com.airmouse.domain.model

enum class ControlMode(val displayName: String) {
    GYRO("Gyroscope (Tilt)"),
    ACCEL("Accelerometer (Move)"),
    HYBRID("Hybrid")
}