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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.CalibrationManager
import com.airmouse.ui.CalibrationActivity
import kotlin.math.sqrt

class GyroStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private lateinit var sensorManager: SensorManager
    private var gyroSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 150
    private var collecting = false
    private var stepComplete = false
    private var dataValid = false

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button
    private lateinit var phoneImage: ImageView

    // Stationarity check using accelerometer
    private val accelWindow = mutableListOf<FloatArray>()
    private var currentVariance = 0f
    private val VARIANCE_THRESHOLD = 0.15f

    private var secondsLeft = 45
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!collecting) return
            if (secondsLeft <= 0) {
                stopCollection()
                evaluateData()
                return
            }
            val mins = secondsLeft / 60
            val secs = secondsLeft % 60
            (activity as? CalibrationActivity)?.setTimerText(String.format("%02d:%02d", mins, secs))
            secondsLeft--
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_gyro_step, container, false)
        statusText = view.findViewById(R.id.statusText)
        progressBar = view.findViewById(R.id.progressBar)
        startBtn = view.findViewById(R.id.startBtn)
        retryBtn = view.findViewById(R.id.retryBtn)
        phoneImage = view.findViewById(R.id.phoneImage)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        startBtn.setOnClickListener { startCollection() }
        retryBtn.setOnClickListener {
            retryBtn.visibility = View.GONE
            startBtn.visibility = View.VISIBLE
            resetUI()
        }
        resetUI()
        return view
    }

    private fun startCollection() {
        if (gyroSensor == null || accelSensor == null) {
            Toast.makeText(requireContext(), "Required sensors not available", Toast.LENGTH_SHORT).show()
            return
        }
        collecting = true
        stepComplete = false
        dataValid = false
        samples.clear()
        accelWindow.clear()
        sampleCount = 0
        currentVariance = 0f

        startBtn.visibility = View.GONE
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        statusText.text = "Place phone on a flat surface and keep still..."

        (activity as? CalibrationActivity)?.setStatusHeader("Gyroscope Calibration")

        secondsLeft = 45
        handler.post(countdownRunnable)
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting) return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                if (sampleCount < targetSamples) {
                    samples.add(event.values.clone())
                    sampleCount++
                    activity?.runOnUiThread {
                        progressBar.progress = (sampleCount * 100) / targetSamples
                        statusText.text = "Collecting gyro data ($sampleCount/$targetSamples)"
                    }
                }
                if (sampleCount >= targetSamples) {
                    stopCollection()
                    evaluateData()
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelWindow.add(event.values.clone())
                if (accelWindow.size > 20) accelWindow.removeAt(0)
                if (accelWindow.size == 20) {
                    var sumX = 0f; var sumY = 0f; var sumZ = 0f
                    for (a in accelWindow) {
                        sumX += a[0]; sumY += a[1]; sumZ += a[2]
                    }
                    val meanX = sumX / 20
                    val meanY = sumY / 20
                    val meanZ = sumZ / 20
                    var varX = 0f; var varY = 0f; var varZ = 0f
                    for (a in accelWindow) {
                        varX += (a[0] - meanX) * (a[0] - meanX)
                        varY += (a[1] - meanY) * (a[1] - meanY)
                        varZ += (a[2] - meanZ) * (a[2] - meanZ)
                    }
                    currentVariance = (varX + varY + varZ) / (3 * 20)
                    val variancePercent = (1f - (currentVariance / VARIANCE_THRESHOLD).coerceIn(0f, 1f)) * 100
                    activity?.runOnUiThread {
                        if (currentVariance > VARIANCE_THRESHOLD) {
                            statusText.text = "Movement detected – please keep phone still"
                        } else {
                            statusText.text = "Stable – collecting data"
                        }
                    }
                }
            }
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
        handler.removeCallbacks(countdownRunnable)
    }

    private fun evaluateData() {
        if (currentVariance > VARIANCE_THRESHOLD || samples.size < targetSamples) {
            dataValid = false
            activity?.runOnUiThread {
                statusText.text = "Calibration failed due to movement. Tap Retry."
                retryBtn.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            }
        } else {
            var bx = 0f; var by = 0f; var bz = 0f
            for (s in samples) {
                bx += s[0]; by += s[1]; bz += s[2]
            }
            val n = samples.size.toFloat()
            val bias = floatArrayOf(bx / n, by / n, bz / n)
            CalibrationManager(requireContext()).saveGyroBias(bias)
            dataValid = true
            stepComplete = true
            (activity as? CalibrationActivity)?.onStepComplete()
            activity?.runOnUiThread {
                statusText.text = "Gyroscope calibrated!"
                startBtn.visibility = View.VISIBLE
                startBtn.text = "Recalibrate"
                retryBtn.visibility = View.GONE
            }
        }
    }

    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid
    override fun getProgress(): Int = if (targetSamples > 0) (sampleCount * 100) / targetSamples else 0

    override fun resetUI() {
        stepComplete = false
        dataValid = false
        collecting = false
        sampleCount = 0
        samples.clear()
        accelWindow.clear()
        currentVariance = 0f
        progressBar.progress = 0
        startBtn.visibility = View.VISIBLE
        startBtn.text = "Start Collection"
        retryBtn.visibility = View.GONE
        statusText.text = "Ready to calibrate gyroscope"
        handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("00:45")
    }

    override fun saveCalibrationData() = Unit
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }
}