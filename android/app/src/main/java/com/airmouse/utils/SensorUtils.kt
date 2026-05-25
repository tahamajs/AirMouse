package com.airmouse.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

/**
 * Utilities for checking sensor presence and obtaining sensor features.
 */
object SensorUtils {

    /**
     * Checks if the device has a gyroscope.
     * @return true if TYPE_GYROSCOPE is available.
     */
    fun hasGyroscope(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    /**
     * Checks if the device has an accelerometer (almost all phones do).
     */
    fun hasAccelerometer(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    /**
     * Checks if the device has a magnetometer (for compass).
     */
    fun hasMagnetometer(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    /**
     * Returns a user‑friendly message if any required sensor is missing.
     */
    fun getMissingSensorsMessage(context: Context): String {
        val missing = mutableListOf<String>()
        if (!hasGyroscope(context)) missing.add("Gyroscope")
        if (!hasAccelerometer(context)) missing.add("Accelerometer")
        if (!hasMagnetometer(context)) missing.add("Magnetometer")
        return if (missing.isEmpty()) {
            "All sensors present"
        } else {
            "Missing sensors: ${missing.joinToString(", ")}"
        }
    }
}