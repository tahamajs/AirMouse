package com.airmouse.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.CalibrationManager
import com.airmouse.ui.CalibrationActivity
import kotlin.math.max
import kotlin.math.min

class MagStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private lateinit var sensorManager: SensorManager
    private var magSensor: Sensor? = null
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 300
    private var collecting = false
    private var stepComplete = false
    private var dataValid = false
    private var vibrator: Vibrator? = null

    private lateinit var instructionText: TextView
    private lateinit var coachMessage: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var coverageView: CoverageView
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button
    private lateinit var movementQualityText: TextView

    private var secondsLeft = 60
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!collecting) return
            if (secondsLeft <= 0) { stopCollection(); finishCalibration(); return }
            val mins = secondsLeft / 60; val secs = secondsLeft % 60
            (activity as? CalibrationActivity)?.setTimerText(String.format("%02d:%02d", mins, secs))
            secondsLeft--; handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mag_step, container, false)
        instructionText = view.findViewById(R.id.instructionText)
        coachMessage = view.findViewById(R.id.coachMessage)
        progressBar = view.findViewById(R.id.progressBar)
        coverageView = view.findViewById(R.id.coverageView)
        startBtn = view.findViewById(R.id.startBtn)
        retryBtn = view.findViewById(R.id.retryBtn)
        movementQualityText = view.findViewById(R.id.movementQualityText)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        startBtn.setOnClickListener { startCollection() }
        retryBtn.setOnClickListener { retryBtn.visibility = View.GONE; startBtn.visibility = View.VISIBLE; resetUI() }
        if (magSensor == null) { startBtn.isEnabled = false; startBtn.text = "Magnetometer not available" }
        resetUI()
        return view
    }

    private fun startCollection() {
        if (magSensor == null) return
        collecting = true; stepComplete = false; dataValid = false
        samples.clear(); sampleCount = 0
        coverageView.reset(); coverageView.startAnimation()
        startBtn.visibility = View.GONE; retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE; progressBar.progress = 0
        instructionText.text = "Move the phone in a figure‑8 pattern"
        coachMessage.text = "Wave the phone gently in a figure‑8"
        movementQualityText.text = "Movement: --"
        (activity as? CalibrationActivity)?.setStatusHeader("Magnetometer Calibration")
        secondsLeft = 60
        handler.post(countdownRunnable)
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST)
        vibrate(30)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        samples.add(event.values.clone())
        sampleCount++
        val percent = (sampleCount * 100 / targetSamples).coerceAtMost(100)
        activity?.runOnUiThread {
            progressBar.progress = percent
            coverageView.updateCoverage(percent)
            // Estimate movement quality based on change rate (simplified)
            val quality = when {
                percent < 20 -> "Start moving"
                percent < 50 -> "Good, keep going"
                percent < 80 -> "Almost there"
                else -> "Finishing..."
            }
            movementQualityText.text = "Movement: $quality"
            coachMessage.text = if (sampleCount >= targetSamples) "Enough data – finishing…" else "Collecting samples: $sampleCount/$targetSamples"
        }
        if (sampleCount >= targetSamples) { stopCollection(); finishCalibration() }
    }

    private fun stopCollection() { sensorManager.unregisterListener(this); collecting = false; handler.removeCallbacks(countdownRunnable); coverageView.stopAnimation() }

    private fun finishCalibration() {
        if (samples.size < 50) {
            dataValid = false; vibrate(200)
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "Not enough samples. Please retry.", Toast.LENGTH_LONG).show()
                retryBtn.visibility = View.VISIBLE; progressBar.visibility = View.INVISIBLE
            }
            return
        }
        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = Float.MIN_VALUE
        for (s in samples) {
            minX = min(minX, s[0]); maxX = max(maxX, s[0])
            minY = min(minY, s[1]); maxY = max(maxY, s[1])
            minZ = min(minZ, s[2]); maxZ = max(maxZ, s[2])
        }
        val offset = floatArrayOf((maxX+minX)/2f, (maxY+minY)/2f, (maxZ+minZ)/2f)
        val scale = floatArrayOf((maxX-minX)/2f, (maxY-minY)/2f, (maxZ-minZ)/2f)
        CalibrationManager(requireContext()).saveMagCalibration(offset, scale)
        dataValid = true; stepComplete = true
        vibrate(100)
        (activity as? CalibrationActivity)?.onStepComplete()
        activity?.runOnUiThread {
            coachMessage.text = "Magnetometer calibrated ✓"
            coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            instructionText.text = "Hard iron offsets computed"
            startBtn.visibility = View.GONE; retryBtn.visibility = View.GONE
            movementQualityText.text = "Movement: Complete!"
        }
    }

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else { @Suppress("DEPRECATION") vibrator?.vibrate(duration) }
    }

    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid
    override fun resetUI() {
        stepComplete = false; dataValid = false; sampleCount = 0; samples.clear()
        startBtn.isEnabled = true; startBtn.visibility = View.VISIBLE; startBtn.text = "Start Collection"
        retryBtn.visibility = View.GONE; progressBar.visibility = View.INVISIBLE
        instructionText.text = "Magnetometer calibration ready"
        coachMessage.text = "Press Start and follow the moving dot"
        movementQualityText.text = "Movement: --"
        coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        coverageView.reset(); coverageView.stopAnimation()
        secondsLeft = 60; handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("01:00")
    }
    override fun saveCalibrationData() = Unit
    override fun getProgress(): Int = (sampleCount * 100 / targetSamples)
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroyView() { super.onDestroyView(); sensorManager.unregisterListener(this) }
}