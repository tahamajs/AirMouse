package com.airmouse.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.CalibrationManager
import com.airmouse.ui.CalibrationActivity

class MagStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private lateinit var sensorManager: SensorManager
    private var magSensor: Sensor? = null
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 200
    private var stepComplete = false

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mag_step, container, false)
        statusText = view.findViewById(R.id.statusText)
        progressBar = view.findViewById(R.id.progressBar)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Auto-start when this fragment becomes visible
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST)
        return view
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        if (sampleCount < targetSamples) {
            samples.add(event.values.clone())
            sampleCount++
            activity?.runOnUiThread {
                progressBar.progress = (sampleCount * 100) / targetSamples
                statusText.text = "Collecting... move phone in figure‑8 ($sampleCount/$targetSamples)"
            }
        }
        if (sampleCount >= targetSamples) {
            sensorManager.unregisterListener(this)
            saveCalibrationData()
            stepComplete = true
            (activity as? CalibrationActivity)?.onStepComplete()
            activity?.runOnUiThread {
                statusText.text = "Magnetometer calibrated!"
            }
        }
    }

    private fun saveCalibrationData() {
        val min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val max = floatArrayOf(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
        for (s in samples) {
            for (i in 0..2) {
                if (s[i] < min[i]) min[i] = s[i]
                if (s[i] > max[i]) max[i] = s[i]
            }
        }
        val offset = FloatArray(3) { (max[it] + min[it]) / 2f }
        val scale = FloatArray(3) { (max[it] - min[it]) / 2f }
        CalibrationManager(requireContext()).saveMagCalibration(offset, scale)
    }

    override fun isStepComplete() = stepComplete
    override fun resetUI() {
        stepComplete = false
        sampleCount = 0
        samples.clear()
        progressBar.progress = 0
        statusText.text = "Move phone in figure‑8 pattern..."
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }
    override fun saveCalibrationData() {} // already saved
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onDestroyView() { super.onDestroyView(); sensorManager.unregisterListener(this) }
}