package com.airmouse.domain.model

enum class CalibrationStatus {
    NOT_STARTED,
    IN_PROGRESS,
    GYRO_COMPLETE,
    MAG_COMPLETE,
    ACCEL_COMPLETE,
    COMPLETED,
    FAILED,
    SKIPPED,
    IDLE
}
