package com.airmouse.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airmouse.R
import kotlin.math.*

class SensorVisualizerFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private lateinit var cubeView: SensorCubeView
    private lateinit var gyroX: TextView
    private lateinit var gyroY: TextView
    private lateinit var gyroZ: TextView
    private lateinit var accelX: TextView
    private lateinit var accelY: TextView
    private lateinit var accelZ: TextView
    private lateinit var magX: TextView
    private lateinit var magY: TextView
    private lateinit var magZ: TextView

    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sensor_visualizer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cubeView = view.findViewById(R.id.sensorCubeView)
        gyroX = view.findViewById(R.id.gyro_x)
        gyroY = view.findViewById(R.id.gyro_y)
        gyroZ = view.findViewById(R.id.gyro_z)
        accelX = view.findViewById(R.id.accel_x)
        accelY = view.findViewById(R.id.accel_y)
        accelZ = view.findViewById(R.id.accel_z)
        magX = view.findViewById(R.id.mag_x)
        magY = view.findViewById(R.id.mag_y)
        magZ = view.findViewById(R.id.mag_z)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val roll = orientation[2]   // rotation around X (radians)
                val pitch = orientation[1]  // rotation around Y (radians)
                val yaw = orientation[0]    // rotation around Z (radians)
                cubeView.updateOrientation(roll, pitch, yaw)
            }
            Sensor.TYPE_GYROSCOPE -> {
                activity?.runOnUiThread {
                    gyroX.text = String.format("%.2f", event.values[0])
                    gyroY.text = String.format("%.2f", event.values[1])
                    gyroZ.text = String.format("%.2f", event.values[2])
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                activity?.runOnUiThread {
                    accelX.text = String.format("%.2f", event.values[0])
                    accelY.text = String.format("%.2f", event.values[1])
                    accelZ.text = String.format("%.2f", event.values[2])
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                activity?.runOnUiThread {
                    magX.text = String.format("%.2f", event.values[0])
                    magY.text = String.format("%.2f", event.values[1])
                    magZ.text = String.format("%.2f", event.values[2])
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}