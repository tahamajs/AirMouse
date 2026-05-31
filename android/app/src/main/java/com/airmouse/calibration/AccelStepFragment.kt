package com.airmouse.calibration

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.SpringAnimation
import android.animation.SpringForce
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.CalibrationManager
import com.airmouse.ui.CalibrationActivity
import kotlin.math.abs
import kotlin.math.sqrt

class AccelStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private data class Position(
        val nameRes: String,
        val descriptionRes: String,
        val rotationZ: Float = 0f,
        val rotationX: Float = 0f,
        val scale: Float = 1f,
        val expectedGravity: FloatArray
    )

    private val positions = listOf(
        Position("Screen Up", "Place phone flat, screen UP", 0f, 0f, 1.0f, floatArrayOf(0f, 0f, 9.81f)),
        Position("Screen Down", "Place phone flat, screen DOWN", 0f, 180f, 1.0f, floatArrayOf(0f, 0f, -9.81f)),
        Position("Vertical (Top Up)", "Hold vertically, top up", 0f, 90f, 0.8f, floatArrayOf(0f, 9.81f, 0f)),
        Position("Vertical (Top Down)", "Hold vertically, top down", 0f, -90f, 0.8f, floatArrayOf(0f, -9.81f, 0f)),
        Position("Left Edge Up", "Left edge pointing up", -90f, 0f, 0.8f, floatArrayOf(-9.81f, 0f, 0f)),
        Position("Right Edge Up", "Right edge pointing up", 90f, 0f, 0.8f, floatArrayOf(9.81f, 0f, 0f))
    )

    private var currentPosIndex = 0
    private val samplesPerPosition = mutableListOf<MutableList<FloatArray>>()
    private var sampleCount = 0
    private val targetSamples = 120
    private var collecting = false
    private var stepComplete = false
    private var dataValid = false

    private lateinit var instructionTitle: TextView
    private lateinit var instructionDesc: TextView
    private lateinit var phoneAnimation: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var stabilityBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button
    private lateinit var coachMessage: TextView
    private lateinit var realTimeVector: TextView
    private lateinit var positionBadge: TextView

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var vibrator: Vibrator? = null

    private val recentReadings = mutableListOf<FloatArray>()
    private var isStable = false
    private var stabilityCheckRunning = false

    private var secondsLeft = 75
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!collecting) return
            if (secondsLeft <= 0) {
                stopCollection()
                evaluateData()
                return
            }
            val mins = secondsLeft / 60; val secs = secondsLeft % 60
            (activity as? CalibrationActivity)?.setTimerText(String.format("%02d:%02d", mins, secs))
            secondsLeft--
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val VECTOR_TOLERANCE = 1.2f
        private const val STABILITY_WINDOW = 15
        private const val STABILITY_THRESHOLD = 0.35f
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_accel_step, container, false)
        instructionTitle = view.findViewById(R.id.instructionTitle)
        instructionDesc = view.findViewById(R.id.instructionDesc)
        phoneAnimation = view.findViewById(R.id.phoneAnimation)
        progressBar = view.findViewById(R.id.progressBar)
        stabilityBar = view.findViewById(R.id.stabilityBar)
        startBtn = view.findViewById(R.id.startBtn)
        retryBtn = view.findViewById(R.id.retryBtn)
        coachMessage = view.findViewById(R.id.coachMessage)
        realTimeVector = view.findViewById(R.id.realTimeVector)
        positionBadge = view.findViewById(R.id.positionBadge)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        repeat(positions.size) { samplesPerPosition.add(mutableListOf()) }

        startBtn.setOnClickListener { if (!collecting) startCollection() }
        retryBtn.setOnClickListener {
            retryBtn.visibility = View.GONE
            startBtn.visibility = View.VISIBLE
            resetUI()
        }
        resetUI()
        return view
    }

    private fun startCollection() {
        if (accelSensor == null) {
            Toast.makeText(requireContext(), "Accelerometer not available", Toast.LENGTH_SHORT).show()
            return
        }
        collecting = true
        stepComplete = false
        dataValid = false
        sampleCount = 0
        recentReadings.clear()
        isStable = false
        stabilityCheckRunning = true
        positionBadge.text = "Position ${currentPosIndex + 1}/6"

        startBtn.visibility = View.GONE
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        stabilityBar.visibility = View.VISIBLE
        progressBar.progress = 0
        stabilityBar.progress = 0
        coachMessage.text = "Keep the phone perfectly still…"

        (activity as? CalibrationActivity)?.setStatusHeader("Position ${currentPosIndex + 1} of 6")
        instructionTitle.text = positions[currentPosIndex].nameRes
        instructionDesc.text = positions[currentPosIndex].descriptionRes
        animatePhoneToPosition(positions[currentPosIndex])

        secondsLeft = 75
        handler.post(countdownRunnable)
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun animatePhoneToPosition(pos: Position) {
        // Spring animation for natural feel
        val animZ = ObjectAnimator.ofFloat(phoneAnimation, "rotation", phoneAnimation.rotation, pos.rotationZ)
        val animX = ObjectAnimator.ofFloat(phoneAnimation, "rotationX", phoneAnimation.rotationX, pos.rotationX)
        val animScaleX = ObjectAnimator.ofFloat(phoneAnimation, "scaleX", phoneAnimation.scaleX, pos.scale)
        val animScaleY = ObjectAnimator.ofFloat(phoneAnimation, "scaleY", phoneAnimation.scaleY, pos.scale)
        animZ.duration = 600
        animX.duration = 600
        animScaleX.duration = 600
        animScaleY.duration = 600
        animZ.interpolator = AccelerateDecelerateInterpolator()
        animX.interpolator = AccelerateDecelerateInterpolator()
        animScaleX.interpolator = AccelerateDecelerateInterpolator()
        animScaleY.interpolator = AccelerateDecelerateInterpolator()
        animZ.start()
        animX.start()
        animScaleX.start()
        animScaleY.start()
        vibrate(30)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        activity?.runOnUiThread { realTimeVector.text = String.format("X: %.2f  Y: %.2f  Z: %.2f", x, y, z) }

        if (stabilityCheckRunning) {
            recentReadings.add(event.values.clone())
            if (recentReadings.size > STABILITY_WINDOW) recentReadings.removeAt(0)
            if (recentReadings.size == STABILITY_WINDOW) {
                var sumX = 0f; var sumY = 0f; var sumZ = 0f
                for (r in recentReadings) { sumX += r[0]; sumY += r[1]; sumZ += r[2] }
                val meanX = sumX / STABILITY_WINDOW; val meanY = sumY / STABILITY_WINDOW; val meanZ = sumZ / STABILITY_WINDOW
                var varX = 0f; var varY = 0f; var varZ = 0f
                for (r in recentReadings) {
                    varX += (r[0] - meanX) * (r[0] - meanX)
                    varY += (r[1] - meanY) * (r[1] - meanY)
                    varZ += (r[2] - meanZ) * (r[2] - meanZ)
                }
                val stdDev = sqrt((varX + varY + varZ) / (3 * STABILITY_WINDOW))
                isStable = stdDev < STABILITY_THRESHOLD
                val stabilityPercent = (1f - (stdDev / STABILITY_THRESHOLD).coerceIn(0f, 1f)) * 100
                activity?.runOnUiThread {
                    stabilityBar.progress = stabilityPercent.toInt()
                    stabilityBar.progressTintList = ContextCompat.getColorStateList(
                        requireContext(),
                        when {
                            stabilityPercent > 80 -> android.R.color.holo_green_dark
                            stabilityPercent > 50 -> android.R.color.holo_orange_dark
                            else -> android.R.color.holo_red_dark
                        }
                    )
                    coachMessage.text = if (isStable) "✓ Stable – collecting data" else "✗ Phone moving – hold still"
                    coachMessage.setTextColor(ContextCompat.getColor(requireContext(), if (isStable) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
                }
            }
        }
        if (isStable && sampleCount < targetSamples) {
            samplesPerPosition[currentPosIndex].add(event.values.clone())
            sampleCount++
            activity?.runOnUiThread { progressBar.progress = (sampleCount * 100) / targetSamples }
        }
        if (sampleCount >= targetSamples) { stopCollection(); evaluateData() }
    }

    private fun stopCollection() { sensorManager.unregisterListener(this); collecting = false; stabilityCheckRunning = false; handler.removeCallbacks(countdownRunnable) }

    private fun evaluateData() {
        val samples = samplesPerPosition[currentPosIndex]
        if (samples.isEmpty()) { failCollection("No stable samples collected. Retry."); return }
        var meanX = 0f; var meanY = 0f; var meanZ = 0f
        for (s in samples) { meanX += s[0]; meanY += s[1]; meanZ += s[2] }
        val n = samples.size.toFloat(); meanX /= n; meanY /= n; meanZ /= n
        val expected = positions[currentPosIndex].expectedGravity
        val error = sqrt((meanX - expected[0])*(meanX - expected[0]) + (meanY - expected[1])*(meanY - expected[1]) + (meanZ - expected[2])*(meanZ - expected[2]))
        if (error > VECTOR_TOLERANCE) failCollection("Orientation mismatch (error: %.2f m/s²). Retry.".format(error))
        else advanceOrFinish()
    }

    private fun failCollection(msg: String) {
        dataValid = false; vibrate(200)
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            coachMessage.text = "Tap Retry to try this position again"
            coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            retryBtn.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE; stabilityBar.visibility = View.INVISIBLE
        }
    }

    private fun advanceOrFinish() {
        vibrate(50)
        if (currentPosIndex < positions.size - 1) {
            currentPosIndex++; sampleCount = 0; recentReadings.clear(); isStable = false
            activity?.runOnUiThread {
                startBtn.visibility = View.VISIBLE; startBtn.isEnabled = true
                retryBtn.visibility = View.GONE
                progressBar.visibility = View.INVISIBLE; stabilityBar.visibility = View.INVISIBLE
                coachMessage.text = "Great! Ready for next position."
                coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                instructionTitle.text = positions[currentPosIndex].nameRes
                instructionDesc.text = positions[currentPosIndex].descriptionRes
                positionBadge.text = "Position ${currentPosIndex + 1}/6"
                animatePhoneToPosition(positions[currentPosIndex])
                (activity as? CalibrationActivity)?.setTimerText("01:15")
            }
        } else {
            saveCalibrationData()
            dataValid = true; stepComplete = true
            vibrate(100)
            (activity as? CalibrationActivity)?.onStepComplete()
            activity?.runOnUiThread {
                coachMessage.text = "All positions recorded! Calibration saved."
                coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                instructionTitle.text = "Accelerometer Calibrated"
                instructionDesc.text = "You can now use motion control."
                startBtn.visibility = View.GONE; retryBtn.visibility = View.GONE
            }
        }
    }

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else { @Suppress("DEPRECATION") vibrator?.vibrate(duration) }
    }

    override fun saveCalibrationData() { CalibrationManager(requireContext()).setAccelCalibrated(true) }
    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid
    override fun resetUI() {
        stepComplete = false; dataValid = false; sampleCount = 0; currentPosIndex = 0
        samplesPerPosition.forEach { it.clear() }; recentReadings.clear(); isStable = false
        startBtn.isEnabled = true; startBtn.visibility = View.VISIBLE; startBtn.text = "Record Position"
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.INVISIBLE; stabilityBar.visibility = View.INVISIBLE
        coachMessage.text = "Ready to record position"
        coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        realTimeVector.text = ""
        instructionTitle.text = positions[0].nameRes
        instructionDesc.text = positions[0].descriptionRes
        positionBadge.text = "Position 1/6"
        animatePhoneToPosition(positions[0])
        secondsLeft = 75; handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("01:15")
    }
    override fun getProgress(): Int = (sampleCount * 100) / targetSamples
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroyView() { super.onDestroyView(); sensorManager.unregisterListener(this) }
}