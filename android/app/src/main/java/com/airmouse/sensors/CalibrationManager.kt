package com.airmouse.sensors

import android.content.Context
import android.content.SharedPreferences

class CalibrationManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("calib", Context.MODE_PRIVATE)

    fun saveGyroBias(bias: FloatArray) {
        prefs.edit()
            .putFloat("gyro_bias_x", bias[0])
            .putFloat("gyro_bias_y", bias[1])
            .putFloat("gyro_bias_z", bias[2])
            .apply()
    }

    fun getGyroBias(): FloatArray = floatArrayOf(
        prefs.getFloat("gyro_bias_x", 0f),
        prefs.getFloat("gyro_bias_y", 0f),
        prefs.getFloat("gyro_bias_z", 0f)
    )

    fun saveMagCalibration(offset: FloatArray, scale: FloatArray) {
        prefs.edit()
            .putFloat("mag_off_x", offset[0])
            .putFloat("mag_off_y", offset[1])
            .putFloat("mag_off_z", offset[2])
            .putFloat("mag_scale_x", scale[0])
            .putFloat("mag_scale_y", scale[1])
            .putFloat("mag_scale_z", scale[2])
            .apply()
    }

    fun getMagOffset() = floatArrayOf(
        prefs.getFloat("mag_off_x", 0f),
        prefs.getFloat("mag_off_y", 0f),
        prefs.getFloat("mag_off_z", 0f)
    )
    fun getMagScale() = floatArrayOf(
        prefs.getFloat("mag_scale_x", 1f),
        prefs.getFloat("mag_scale_y", 1f),
        prefs.getFloat("mag_scale_z", 1f)
    )

    // Placeholder for accelerometer calibration
    fun setAccelCalibrated(calibrated: Boolean) {
        prefs.edit().putBoolean("accel_calibrated", calibrated).apply()
    }

    fun isAccelCalibrated(): Boolean = prefs.getBoolean("accel_calibrated", false)
}