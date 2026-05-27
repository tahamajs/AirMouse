package com.airmouse.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

/**
 * Utilities for checking sensor presence and obtaining sensor features.
 */
object SensorUtils {

    fun hasGyroscope(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    fun hasAccelerometer(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    fun hasMagnetometer(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    fun getMissingSensorsMessage(context: Context): String {
        val missing = mutableListOf<String>()
        if (!hasGyroscope(context)) missing.add("Gyroscope")
        if (!hasAccelerometer(context)) missing.add("Accelerometer")
        if (!hasMagnetometer(context)) missing.add("Magnetometer")
        return if (missing.isEmpty()) "All sensors present" else "Missing sensors: ${missing.joinToString(", ")}"
    }
}