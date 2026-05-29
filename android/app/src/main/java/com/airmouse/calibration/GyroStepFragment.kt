// file: calibration/GyroStepFragment.kt
package com.airmouse.calibration

import android.animation.ValueAnimator
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
import kotlin.math.sqrt

class GyroStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private lateinit var sensorManager: SensorManager
    private var gyroSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private val gyroSamples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 150
    private var collecting = false
    private var stepComplete = false
    private var dataValid = false

    // UI
    private lateinit var instructionCard: CardView
    private lateinit var instructionText: TextView
    private lateinit var coachMessage: TextView
    private lateinit var varianceMeter: ProgressBar
    private lateinit var progressBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button
    private lateinit var phoneImage: ImageView
    private var breathAnimator: ValueAnimator? = null

    // Stationarity check using accelerometer
    private val accelWindow = mutableListOf<FloatArray>()
    private var currentVariance = 0f
    private val VARIANCE_THRESHOLD = 0.15f

    private var secondsLeft = GYRO_TIME_LIMIT
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
        private const val GYRO_TIME_LIMIT = 45
        private const val WINDOW_SIZE = 20
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_gyro_step, container, false)
        instructionCard = view.findViewById(R.id.instructionCard)
        instructionText = view.findViewById(R.id.instructionText)
        coachMessage = view.findViewById(R.id.coachMessage)
        varianceMeter = view.findViewById(R.id.varianceMeter)
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
            Toast.makeText(requireContext(), "Gyroscope or accelerometer missing", Toast.LENGTH_SHORT).show()
            return
        }
        collecting = true
        stepComplete = false
        dataValid = false
        gyroSamples.clear()
        accelWindow.clear()
        sampleCount = 0
        currentVariance = 0f

        startBtn.visibility = View.GONE
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        varianceMeter.visibility = View.VISIBLE
        varianceMeter.progress = 0
        progressBar.progress = 0

        instructionText.text = getString(R.string.gyro_instruction)
        coachMessage.text = getString(R.string.gyro_coach_keep_still)
        startBreathingAnimation()

        (activity as? CalibrationActivity)?.setStatusHeader(getString(R.string.gyro_status_header))

        secondsLeft = GYRO_TIME_LIMIT
        handler.post(countdownRunnable)
        (activity as? CalibrationActivity)?.changeState(com.airmouse.calibration.CalibrationState.COLLECTING)
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun startBreathingAnimation() {
        phoneImage.post {
            breathAnimator = ValueAnimator.ofFloat(1f, 1.06f).apply {
                duration = 1200
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val scale = it.animatedValue as Float
                    phoneImage.scaleX = scale
                    phoneImage.scaleY = scale
                }
                start()
            }
        }
    }

    private fun stopBreathingAnimation() {
        breathAnimator?.cancel()
        phoneImage.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting) return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                if (sampleCount < targetSamples) {
                    gyroSamples.add(event.values.clone())
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
            Sensor.TYPE_ACCELEROMETER -> {
                accelWindow.add(event.values.clone())
                if (accelWindow.size > WINDOW_SIZE) accelWindow.removeAt(0)
                if (accelWindow.size == WINDOW_SIZE) {
                    var sumX = 0f; var sumY = 0f; var sumZ = 0f
                    for (a in accelWindow) {
                        sumX += a[0]; sumY += a[1]; sumZ += a[2]
                    }
                    val meanX = sumX / WINDOW_SIZE
                    val meanY = sumY / WINDOW_SIZE
                    val meanZ = sumZ / WINDOW_SIZE
                    var varX = 0f; var varY = 0f; var varZ = 0f
                    for (a in accelWindow) {
                        varX += (a[0] - meanX) * (a[0] - meanX)
                        varY += (a[1] - meanY) * (a[1] - meanY)
                        varZ += (a[2] - meanZ) * (a[2] - meanZ)
                    }
                    currentVariance = (varX + varY + varZ) / (3 * WINDOW_SIZE)
                    val variancePercent = (1f - (currentVariance / VARIANCE_THRESHOLD).coerceIn(0f, 1f)) * 100
                    activity?.runOnUiThread {
                        varianceMeter.progress = variancePercent.toInt()
                        if (currentVariance > VARIANCE_THRESHOLD) {
                            coachMessage.text = getString(R.string.gyro_coach_moving)
                            coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                        } else {
                            coachMessage.text = getString(R.string.gyro_coach_stable)
                            coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                        }
                    }
                }
            }
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
        stopBreathingAnimation()
        handler.removeCallbacks(countdownRunnable)
    }

    private fun evaluateData() {
        (activity as? CalibrationActivity)?.changeState(com.airmouse.calibration.CalibrationState.EVALUATING)
        if (currentVariance > VARIANCE_THRESHOLD || gyroSamples.size < targetSamples) {
            dataValid = false
            activity?.runOnUiThread {
                coachMessage.text = getString(R.string.gyro_failed_movement)
                coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                retryBtn.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
                varianceMeter.visibility = View.INVISIBLE
            }
        } else {
            saveCalibrationData()
            dataValid = true
            stepComplete = true
            activity?.runOnUiThread {
                (activity as? CalibrationActivity)?.onStepComplete()
                (activity as? CalibrationActivity)?.changeState(com.airmouse.calibration.CalibrationState.STEP_COMPLETE)
                coachMessage.text = getString(R.string.gyro_done)
                coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                instructionText.text = getString(R.string.gyro_complete)
                startBtn.visibility = View.GONE
                retryBtn.visibility = View.GONE
            }
        }
    }

    private fun saveCalibrationData() {
        var bx = 0f; var by = 0f; var bz = 0f
        for (s in gyroSamples) {
            bx += s[0]; by += s[1]; bz += s[2]
        }
        val n = gyroSamples.size.toFloat()
        val bias = floatArrayOf(bx / n, by / n, bz / n)
        CalibrationManager(requireContext()).saveGyroBias(bias)
    }

    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid
    override fun resetUI() {
        stepComplete = false
        dataValid = false
        sampleCount = 0
        gyroSamples.clear()
        accelWindow.clear()
        currentVariance = 0f

        startBtn.isEnabled = true
        startBtn.visibility = View.VISIBLE
        startBtn.text = getString(R.string.btn_start_collection)
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.INVISIBLE
        varianceMeter.visibility = View.INVISIBLE
        instructionText.text = getString(R.string.gyro_ready)
        coachMessage.text = getString(R.string.gyro_ready_desc)
        coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        stopBreathingAnimation()
        phoneImage.scaleX = 1f
        phoneImage.scaleY = 1f

        secondsLeft = GYRO_TIME_LIMIT
        handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("00:45")
    }

    override fun saveCalibrationData() = Unit
    override fun getProgress(): Int = if (targetSamples > 0) (sampleCount * 100) / targetSamples else 0
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }
}