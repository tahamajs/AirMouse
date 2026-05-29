// file: calibration/CalibrationStepFragment.kt
package com.airmouse.calibration

interface CalibrationStepFragment {
    fun isStepComplete(): Boolean
    fun resetUI()
    fun saveCalibrationData()
    fun getProgress(): Int
    fun isDataValid(): Boolean
    fun onOrientationChanged(roll: Float, pitch: Float, yaw: Float) {} // optional
}