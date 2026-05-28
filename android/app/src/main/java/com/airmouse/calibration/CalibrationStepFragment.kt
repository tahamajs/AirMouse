package com.airmouse.calibration

interface CalibrationStepFragment {
    fun isStepComplete(): Boolean
    fun resetUI()
    fun saveCalibrationData()
}