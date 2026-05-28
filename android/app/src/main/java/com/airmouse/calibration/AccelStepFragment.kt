package com.airmouse.calibration

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.CalibrationManager
import com.airmouse.ui.CalibrationActivity

class AccelStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 100
    private var collecting = false
    private var stepComplete = false

    private lateinit var instructionText: TextView
    private lateinit var phoneAnimation: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var startBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_accel_step, container, false)
        instructionText = view.findViewById(R.id.instructionText)
        phoneAnimation = view.findViewById(R.id.phoneAnimation)
        progressBar = view.findViewById(R.id.progressBar)
        startBtn = view.findViewById(R.id.startBtn)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        startBtn.setOnClickListener {
            if (!collecting) {
                startCollection()
            }
        }

        return view
    }

    private fun startCollection() {
        collecting = true
        stepComplete = false
        startBtn.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        instructionText.text = "Collecting samples... keep the phone still"
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)

        // Play animation (optional)
        (phoneAnimation.drawable as? AnimatedVectorDrawable)?.start()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (sampleCount < targetSamples) {
            samples.add(event.values.clone())
            sampleCount++
            activity?.runOnUiThread {
                progressBar.progress = (sampleCount * 100 / targetSamples)
                instructionText.text = "Collecting... ${sampleCount}/${targetSamples}"
            }
        }
        if (sampleCount >= targetSamples) {
            stopCollection()
            saveData()
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
    }

    private fun saveData() {
        // Compute mean and save via CalibrationManager
        val mean = FloatArray(3)
        for (s in samples) {
            mean[0] += s[0]; mean[1] += s[1]; mean[2] += s[2]
        }
        mean[0] /= samples.size; mean[1] /= samples.size; mean[2] /= samples.size
        // In a full implementation, you would collect 6 positions; for simplicity, we assume a single flat-up position.
        // You can expand to collect all 6 using the same pattern.
        CalibrationManager(requireContext()).saveAccelFlatUp(mean)  // you'd implement this
        stepComplete = true
        (activity as? CalibrationActivity)?.onStepComplete()
        activity?.runOnUiThread {
            instructionText.text = "Step complete!"
            startBtn.isEnabled = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun isStepComplete(): Boolean = stepComplete

    override fun resetUI() {
        stepComplete = false
        startBtn.isEnabled = true
        progressBar.visibility = View.INVISIBLE
        instructionText.text = "Place phone flat, screen up"
    }

    override fun saveCalibrationData() {
        // Already saved after each step, but you can do final calculations if needed.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }
}