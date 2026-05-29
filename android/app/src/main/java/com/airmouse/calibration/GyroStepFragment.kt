package com.airmouse.calibration

import android.animation.ValueAnimator
import android.content.Context
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

class GyroStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private lateinit var sensorManager: SensorManager
    private var gyroSensor: Sensor? = null
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 100
    private var collecting = false
    private var stepComplete = false

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var phoneImage: ImageView
    private var breathAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gyro_step, container, false)
        statusText = view.findViewById(R.id.statusText)
        progressBar = view.findViewById(R.id.progressBar)
        startBtn = view.findViewById(R.id.startBtn)
        phoneImage = view.findViewById(R.id.phoneImage)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        startBtn.setOnClickListener { startCollection() }
        return view
    }

    private fun startCollection() {
        if (gyroSensor == null) return
        collecting = true
        stepComplete = false
        startBtn.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        statusText.text = "Keep phone still..."
        startBreathing()
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        if (sampleCount < targetSamples) {
            samples.add(event.values.clone())
            sampleCount++
            activity?.runOnUiThread {
                progressBar.progress = (sampleCount * 100) / targetSamples
                statusText.text = "Collecting... $sampleCount/$targetSamples"
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
        stopBreathing()
    }

    private fun saveData() {
        var biasX = 0f
        var biasY = 0f
        var biasZ = 0f
        for (sample in samples) {
            biasX += sample[0]
            biasY += sample[1]
            biasZ += sample[2]
        }
        val count = samples.size.toFloat().coerceAtLeast(1f)
        val bias = floatArrayOf(biasX / count, biasY / count, biasZ / count)

        CalibrationManager(requireContext()).saveGyroBias(bias)
        stepComplete = true
        (activity as? CalibrationActivity)?.onStepComplete()
        activity?.runOnUiThread {
            statusText.text = "Gyroscope calibrated!"
            startBtn.isEnabled = false
        }
    }

    private fun startBreathing() {
        breathAnimator = ValueAnimator.ofFloat(1f, 1.08f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val scale = it.animatedValue as Float
                phoneImage.scaleX = scale
                phoneImage.scaleY = scale
            }
            start()
        }
    }

    private fun stopBreathing() {
        breathAnimator?.cancel()
        phoneImage.animate().scaleX(1f).scaleY(1f).duration = 200
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun isStepComplete() = stepComplete

    override fun resetUI() {
        stepComplete = false
        startBtn.isEnabled = true
        progressBar.visibility = View.INVISIBLE
        statusText.text = "Place phone on flat surface and press Start"
    }

    override fun saveCalibrationData() = Unit

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }
}