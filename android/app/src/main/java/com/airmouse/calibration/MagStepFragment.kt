package com.airmouse.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.CalibrationManager
import com.airmouse.ui.CalibrationActivity
import java.util.Locale

class MagStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private lateinit var sensorManager: SensorManager
    private var magSensor: Sensor? = null
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 200
    private var collecting = false
    private var stepComplete = false
    private var dataValid = false
    private var timeUp = false

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button

    private var secondsLeft = MAG_TIME_LIMIT
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!collecting) return
            if (secondsLeft <= 0) {
                timeUp = true
                stopCollection()
                if (sampleCount >= 10) {
                    persistCalibrationData()
                } else {
                    activity?.runOnUiThread {
                        statusText.text = getString(R.string.mag_not_enough_samples)
                        retryBtn.visibility = View.VISIBLE
                    }
                }
                return
            }
            val mins = secondsLeft / 60; val secs = secondsLeft % 60
            (activity as? CalibrationActivity)?.setTimerText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs))
            secondsLeft--
            handler.postDelayed(this, 1000)
        }
    }

    companion object { private const val MAG_TIME_LIMIT = 45 }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mag_step, container, false)
        statusText = view.findViewById(R.id.statusText)
        progressBar = view.findViewById(R.id.progressBar)
        startBtn = view.findViewById(R.id.startBtn)
        retryBtn = view.findViewById(R.id.retryBtn)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        startBtn.setOnClickListener { startCollection() }
        retryBtn.setOnClickListener {
            retryBtn.visibility = View.GONE
            startBtn.visibility = View.VISIBLE
            resetUI()
        }

        if (magSensor == null) {
            startBtn.isEnabled = false
            startBtn.text = "Sensor not available"
        }
        resetUI()
        return view
    }

    private fun startCollection() {
        if (magSensor == null) return
        collecting = true; stepComplete = false; dataValid = false; timeUp = false
        startBtn.visibility = View.GONE; retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE; progressBar.progress = 0

        (activity as? CalibrationActivity)?.setStatusHeader(getString(R.string.mag_collecting_header))
        statusText.text = getString(R.string.mag_collecting, 0, targetSamples)

        secondsLeft = MAG_TIME_LIMIT
        handler.post(countdownRunnable)
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        if (sampleCount < targetSamples) {
            samples.add(event.values.clone()); sampleCount++
            activity?.runOnUiThread {
                progressBar.progress = (sampleCount * 100) / targetSamples
                statusText.text = getString(R.string.mag_collecting, sampleCount, targetSamples)
            }
        }
        if (sampleCount >= targetSamples && timeUp) {
            stopCollection()
            persistCalibrationData()
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
        handler.removeCallbacks(countdownRunnable)
    }

    private fun persistCalibrationData() {
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
        dataValid = true; stepComplete = true
        (activity as? CalibrationActivity)?.onStepComplete()
        activity?.runOnUiThread {
            statusText.text = getString(R.string.mag_done)
            startBtn.visibility = View.VISIBLE; startBtn.text = getString(R.string.btn_recalibrate); startBtn.isEnabled = true
        }
    }

    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid

    override fun resetUI() {
        stepComplete = false; dataValid = false; sampleCount = 0; samples.clear(); timeUp = false
        startBtn.isEnabled = true; startBtn.visibility = View.VISIBLE; startBtn.text = getString(R.string.btn_start_collection)
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.INVISIBLE; progressBar.progress = 0
        statusText.text = getString(R.string.mag_ready)
        secondsLeft = MAG_TIME_LIMIT; handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("00:45")
    }

    override fun saveCalibrationData() = Unit
    override fun getProgress(): Int = (sampleCount * 100) / targetSamples
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroyView() { super.onDestroyView(); sensorManager.unregisterListener(this) }
}