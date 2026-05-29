package com.airmouse.calibration

interface CalibrationStepFragment {
    fun isStepComplete(): Boolean
    fun resetUI()
    fun saveCalibrationData()
    fun getProgress(): Int
    fun isDataValid(): Boolean      // true if the collected data passes validation
}