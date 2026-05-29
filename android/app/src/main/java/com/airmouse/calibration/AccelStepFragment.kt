// file: calibration/AccelStepFragment.kt
package com.airmouse.calibration

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.CalibrationManager
import com.airmouse.ui.CalibrationActivity
import kotlin.math.abs
import kotlin.math.sqrt

class AccelStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private data class Position(
        val nameRes: Int,
        val descriptionRes: Int,
        val rotationZ: Float = 0f,
        val rotationX: Float = 0f,
        val scale: Float = 1f,
        val expectedGravity: FloatArray
    )

    private val positions = listOf(
        Position(R.string.accel_flat_up_title, R.string.accel_flat_up_desc,
            0f, 0f, 1.0f, floatArrayOf(0f, 0f, 9.81f)),
        Position(R.string.accel_flat_down_title, R.string.accel_flat_down_desc,
            0f, 180f, 1.0f, floatArrayOf(0f, 0f, -9.81f)),
        Position(R.string.accel_vertical_top_title, R.string.accel_vertical_top_desc,
            0f, 90f, 0.8f, floatArrayOf(0f, 9.81f, 0f)),
        Position(R.string.accel_vertical_bottom_title, R.string.accel_vertical_bottom_desc,
            0f, -90f, 0.8f, floatArrayOf(0f, -9.81f, 0f)),
        Position(R.string.accel_left_title, R.string.accel_left_desc,
            -90f, 0f, 0.8f, floatArrayOf(-9.81f, 0f, 0f)),
        Position(R.string.accel_right_title, R.string.accel_right_desc,
            90f, 0f, 0.8f, floatArrayOf(9.81f, 0f, 0f))
    )

    private var currentPosIndex = 0
    private val samplesPerPosition = mutableListOf<MutableList<FloatArray>>()
    private var sampleCount = 0
    private val targetSamples = 120
    private var collecting = false
    private var stepComplete = false
    private var dataValid = false

    // UI
    private lateinit var instructionCard: CardView
    private lateinit var instructionTitle: TextView
    private lateinit var instructionDesc: TextView
    private lateinit var phoneAnimation: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var stabilityBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button
    private lateinit var coachMessage: TextView
    private lateinit var realTimeVector: TextView

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null

    // Stability check
    private val recentReadings = mutableListOf<FloatArray>()
    private var isStable = false
    private var stabilityCheckRunning = false

    private var secondsLeft = ACCEL_TIME_LIMIT
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

    companion object {
        private const val ACCEL_TIME_LIMIT = 75
        private const val VECTOR_TOLERANCE = 1.2f
        private const val STABILITY_WINDOW = 15
        private const val STABILITY_THRESHOLD = 0.35f
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_accel_step, container, false)
        instructionCard = view.findViewById(R.id.instructionCard)
        instructionTitle = view.findViewById(R.id.instructionTitle)
        instructionDesc = view.findViewById(R.id.instructionDesc)
        phoneAnimation = view.findViewById(R.id.phoneAnimation)
        progressBar = view.findViewById(R.id.progressBar)
        stabilityBar = view.findViewById(R.id.stabilityBar)
        startBtn = view.findViewById(R.id.startBtn)
        retryBtn = view.findViewById(R.id.retryBtn)
        coachMessage = view.findViewById(R.id.coachMessage)
        realTimeVector = view.findViewById(R.id.realTimeVector)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
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

        startBtn.visibility = View.GONE
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        stabilityBar.visibility = View.VISIBLE
        progressBar.progress = 0
        stabilityBar.progress = 0
        coachMessage.text = getString(R.string.accel_coach_keep_still)
        realTimeVector.text = ""

        (activity as? CalibrationActivity)?.setStatusHeader(
            getString(R.string.accel_position_header, currentPosIndex + 1, positions.size)
        )
        instructionTitle.text = getString(positions[currentPosIndex].nameRes)
        instructionDesc.text = getString(positions[currentPosIndex].descriptionRes)

        animatePhoneToPosition(positions[currentPosIndex])

        secondsLeft = ACCEL_TIME_LIMIT
        handler.post(countdownRunnable)
        (activity as? CalibrationActivity)?.changeState(com.airmouse.calibration.CalibrationState.COLLECTING)
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun animatePhoneToPosition(pos: Position) {
        phoneAnimation.post {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(phoneAnimation, "rotation", phoneAnimation.rotation, pos.rotationZ),
                    ObjectAnimator.ofFloat(phoneAnimation, "rotationX", phoneAnimation.rotationX, pos.rotationX),
                    ObjectAnimator.ofFloat(phoneAnimation, "scaleX", phoneAnimation.scaleX, pos.scale),
                    ObjectAnimator.ofFloat(phoneAnimation, "scaleY", phoneAnimation.scaleY, pos.scale)
                )
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        activity?.runOnUiThread {
            realTimeVector.text = String.format("X: %.2f  Y: %.2f  Z: %.2f", x, y, z)
        }

        if (stabilityCheckRunning) {
            recentReadings.add(event.values.clone())
            if (recentReadings.size > STABILITY_WINDOW) recentReadings.removeAt(0)
            if (recentReadings.size == STABILITY_WINDOW) {
                var sumX = 0f; var sumY = 0f; var sumZ = 0f
                for (r in recentReadings) {
                    sumX += r[0]; sumY += r[1]; sumZ += r[2]
                }
                val meanX = sumX / STABILITY_WINDOW
                val meanY = sumY / STABILITY_WINDOW
                val meanZ = sumZ / STABILITY_WINDOW
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
                    if (isStable) {
                        coachMessage.text = getString(R.string.accel_coach_stable)
                        coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                    } else {
                        coachMessage.text = getString(R.string.accel_coach_unstable)
                        coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    }
                }
            }
        }

        if (isStable && sampleCount < targetSamples) {
            samplesPerPosition[currentPosIndex].add(event.values.clone())
            sampleCount++
            activity?.runOnUiThread {
                    val p = (sampleCount * 100) / targetSamples
                    progressBar.progress = p
                    (activity as? com.airmouse.ui.CalibrationActivity)?.updateStepProgress(p)
            }
        }

        if (sampleCount >= targetSamples) {
            stopCollection()
            evaluateData()
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
        stabilityCheckRunning = false
        handler.removeCallbacks(countdownRunnable)
    }

    private fun evaluateData() {
        val samples = samplesPerPosition[currentPosIndex]
        if (samples.isEmpty()) {
            failCollection(getString(R.string.accel_no_samples))
            return
        }
        var meanX = 0f; var meanY = 0f; var meanZ = 0f
        for (s in samples) {
            meanX += s[0]; meanY += s[1]; meanZ += s[2]
        }
        val n = samples.size.toFloat()
        meanX /= n; meanY /= n; meanZ /= n

        val expected = positions[currentPosIndex].expectedGravity
        val error = sqrt(
            (meanX - expected[0]) * (meanX - expected[0]) +
            (meanY - expected[1]) * (meanY - expected[1]) +
            (meanZ - expected[2]) * (meanZ - expected[2])
        )
        if (error > VECTOR_TOLERANCE) {
            failCollection(getString(R.string.accel_wrong_orientation, error))
        } else {
            advanceOrFinish()
        }
    }

    private fun failCollection(message: String) {
        dataValid = false
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            coachMessage.text = getString(R.string.accel_retry_prompt)
            coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            retryBtn.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
            stabilityBar.visibility = View.INVISIBLE
        }
    }

    private fun advanceOrFinish() {
        if (currentPosIndex < positions.size - 1) {
            currentPosIndex++
            sampleCount = 0
            recentReadings.clear()
            isStable = false
            activity?.runOnUiThread {
                startBtn.visibility = View.VISIBLE
                startBtn.isEnabled = true
                retryBtn.visibility = View.GONE
                progressBar.visibility = View.INVISIBLE
                stabilityBar.visibility = View.INVISIBLE
                coachMessage.text = getString(R.string.accel_next_position_ready)
                coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                instructionTitle.text = getString(positions[currentPosIndex].nameRes)
                instructionDesc.text = getString(positions[currentPosIndex].descriptionRes)
                animatePhoneToPosition(positions[currentPosIndex])
                (activity as? CalibrationActivity)?.setTimerText("01:15")
            }
        } else {
            saveCalibrationData()
            dataValid = true
            stepComplete = true
            activity?.runOnUiThread {
                (activity as? CalibrationActivity)?.onStepComplete()
                coachMessage.text = getString(R.string.accel_done)
                coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                instructionTitle.text = getString(R.string.accel_complete_title)
                instructionDesc.text = getString(R.string.accel_complete_desc)
                startBtn.visibility = View.GONE
                retryBtn.visibility = View.GONE
            }
        }
    }

    override fun saveCalibrationData() {
        CalibrationManager(requireContext()).setAccelCalibrated(true)
    }

    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid

    override fun resetUI() {
        stepComplete = false
        dataValid = false
        sampleCount = 0
        currentPosIndex = 0
        samplesPerPosition.forEach { it.clear() }
        recentReadings.clear()
        isStable = false

        startBtn.isEnabled = true
        startBtn.visibility = View.VISIBLE
        startBtn.text = getString(R.string.btn_start_collection)
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.INVISIBLE
        stabilityBar.visibility = View.INVISIBLE
        coachMessage.text = getString(R.string.accel_ready)
        coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        realTimeVector.text = ""

        instructionTitle.text = getString(positions[0].nameRes)
        instructionDesc.text = getString(positions[0].descriptionRes)
        animatePhoneToPosition(positions[0])

        secondsLeft = ACCEL_TIME_LIMIT
        handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("01:15")
    }

    override fun getProgress(): Int = if (targetSamples > 0) (sampleCount * 100) / targetSamples else 0
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }
}