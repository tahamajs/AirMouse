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
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.CalibrationManager
import com.airmouse.ui.CalibrationActivity
import java.util.Locale
import kotlin.math.abs

class AccelStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private data class Position(
        val description: String,
        val rotation: Float = 0f,
        val rotationX: Float = 0f,
        val scaleX: Float = 1f,
        val expectedVector: FloatArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Position) return false
            return description == other.description &&
                    rotation == other.rotation &&
                    rotationX == other.rotationX &&
                    scaleX == other.scaleX &&
                    expectedVector.contentEquals(other.expectedVector)
        }
        override fun hashCode(): Int {
            var result = description.hashCode()
            result = 31 * result + rotation.hashCode()
            result = 31 * result + rotationX.hashCode()
            result = 31 * result + scaleX.hashCode()
            result = 31 * result + expectedVector.contentHashCode()
            return result
        }
    }

    private val positions = listOf(
        Position("Flat on table, screen up", 0f, 0f, 1.0f, floatArrayOf(0f, 0f, 9.81f)),
        Position("Flat on table, screen down", 0f, 180f, 1.0f, floatArrayOf(0f, 0f, -9.81f)),
        Position("Vertical, top edge up", 0f, 90f, 0.8f, floatArrayOf(0f, 9.81f, 0f)),
        Position("Vertical, top edge down", 0f, -90f, 0.8f, floatArrayOf(0f, -9.81f, 0f)),
        Position("Left edge up", -90f, 0f, 0.8f, floatArrayOf(-9.81f, 0f, 0f)),
        Position("Right edge up", 90f, 0f, 0.8f, floatArrayOf(9.81f, 0f, 0f))
    )

    private var currentPosIndex = 0
    private val samplesPerPosition = mutableListOf<MutableList<FloatArray>>()
    private var sampleCount = 0
    private val targetSamples = 100
    private var collecting = false
    private var stepComplete = false
    private var dataValid = false

    private lateinit var instructionText: TextView
    private lateinit var statusText: TextView
    private lateinit var phoneAnimation: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null

    private var secondsLeft = ACCEL_TIME_LIMIT
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!collecting) return
            if (secondsLeft <= 0) {
                stopCollection()
                checkDataValidity()
                return
            }
            val mins = secondsLeft / 60; val secs = secondsLeft % 60
            (activity as? CalibrationActivity)?.setTimerText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs))
            secondsLeft--
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val ACCEL_TIME_LIMIT = 60
        private const val VECTOR_TOLERANCE = 1.5f
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_accel_step, container, false)
        instructionText = view.findViewById(R.id.instructionText)
        statusText = view.findViewById(R.id.statusText)
        phoneAnimation = view.findViewById(R.id.phoneAnimation)
        progressBar = view.findViewById(R.id.progressBar)
        startBtn = view.findViewById(R.id.startBtn)
        retryBtn = view.findViewById(R.id.retryBtn)

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
        if (accelSensor == null) return
        collecting = true; stepComplete = false; dataValid = false
        startBtn.visibility = View.GONE; retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE; progressBar.progress = 0

        (activity as? CalibrationActivity)?.setStatusHeader("Accelerometer: Position ${currentPosIndex + 1} of 6")
        instructionText.text = positions[currentPosIndex].description
        statusText.text = "Collecting... keep phone still"

        secondsLeft = ACCEL_TIME_LIMIT
        handler.post(countdownRunnable)
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (sampleCount < targetSamples) {
            samplesPerPosition[currentPosIndex].add(event.values.clone()); sampleCount++
            activity?.runOnUiThread { progressBar.progress = (sampleCount * 100) / targetSamples }
        }
        if (sampleCount >= targetSamples) {
            stopCollection()
            checkDataValidity()
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
        handler.removeCallbacks(countdownRunnable)
    }

    private fun checkDataValidity() {
        val list = samplesPerPosition[currentPosIndex]
        var meanX = 0f; var meanY = 0f; var meanZ = 0f
        for (s in list) {
            meanX += s[0]; meanY += s[1]; meanZ += s[2]
        }
        val n = list.size.toFloat().coerceAtLeast(1f)
        meanX /= n; meanY /= n; meanZ /= n

        val expected = positions[currentPosIndex].expectedVector
        val diff = abs(meanX - expected[0]) + abs(meanY - expected[1]) + abs(meanZ - expected[2])
        if (diff > VECTOR_TOLERANCE) {
            dataValid = false
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), R.string.accel_wrong_orientation, Toast.LENGTH_SHORT).show()
                statusText.text = getString(R.string.accel_retry_prompt)
                retryBtn.visibility = View.VISIBLE
            }
        } else {
            advanceOrFinish()
        }
    }

    private fun advanceOrFinish() {
        if (currentPosIndex < positions.size - 1) {
            currentPosIndex++; sampleCount = 0
            val target = positions[currentPosIndex]
            phoneAnimation.post {
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(phoneAnimation, "rotation", phoneAnimation.rotation, target.rotation),
                        ObjectAnimator.ofFloat(phoneAnimation, "rotationX", phoneAnimation.rotationX, target.rotationX),
                        ObjectAnimator.ofFloat(phoneAnimation, "scaleX", phoneAnimation.scaleX, target.scaleX),
                        ObjectAnimator.ofFloat(phoneAnimation, "scaleY", phoneAnimation.scaleY, target.scaleX)
                    )
                    duration = 600; start()
                }
            }
            instructionText.text = target.description
            statusText.text = "Position ${currentPosIndex + 1} of 6"
            startBtn.visibility = View.VISIBLE; startBtn.isEnabled = true
            retryBtn.visibility = View.GONE
            progressBar.visibility = View.INVISIBLE; progressBar.progress = 0
            secondsLeft = ACCEL_TIME_LIMIT
            (activity as? CalibrationActivity)?.setTimerText("01:00")
        } else {
            saveCalibrationData()
            dataValid = true; stepComplete = true
            (activity as? CalibrationActivity)?.onStepComplete()
            statusText.text = "✅ Accelerometer calibration complete!"
            instructionText.text = "All 6 positions recorded successfully"
        }
    }

    override fun saveCalibrationData() { CalibrationManager(requireContext()).setAccelCalibrated(true) }
    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid

    override fun resetUI() {
        stepComplete = false; dataValid = false
        startBtn.isEnabled = true; startBtn.visibility = View.VISIBLE; startBtn.text = "Record Position"
        retryBtn.visibility = View.GONE
        phoneAnimation.rotation = positions[currentPosIndex].rotation; phoneAnimation.rotationX = positions[currentPosIndex].rotationX
        phoneAnimation.scaleX = positions[currentPosIndex].scaleX; phoneAnimation.scaleY = positions[currentPosIndex].scaleX
        instructionText.text = positions[currentPosIndex].description
        statusText.text = "Ready to record position"
        progressBar.visibility = View.INVISIBLE
        secondsLeft = ACCEL_TIME_LIMIT; handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("01:00")
    }
    override fun getProgress(): Int = (sampleCount * 100) / targetSamples
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroyView() { super.onDestroyView(); sensorManager.unregisterListener(this) }
}