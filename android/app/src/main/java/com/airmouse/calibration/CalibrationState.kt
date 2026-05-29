package com.airmouse.calibration

/**
 * Lightweight state machine for calibration flow so fragments and activity can stay in sync.
 */
enum class CalibrationState {
    IDLE,
    COLLECTING,
    EVALUATING,
    STEP_COMPLETE,
    COMPLETE,
    ABORTED
}
