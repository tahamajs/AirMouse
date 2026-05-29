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

class GyroStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private lateinit var sensorManager: SensorManager
    private var gyroSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 100
    private var collecting = false
    private var stepComplete = false
    private var dataValid = false

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button
    private lateinit var phoneImage: ImageView
    private var breathAnimator: ValueAnimator? = null

    // stationarity check
    private val accelSamples = mutableListOf<FloatArray>()
    private var phoneMoving = false

    private var secondsLeft = GYRO_TIME_LIMIT
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!collecting) return
            if (secondsLeft <= 0) {
                stopCollection()
                checkDataValidity()
                return
            }
            val mins = secondsLeft / 60
            val secs = secondsLeft % 60
            (activity as? CalibrationActivity)?.setTimerText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs))
            secondsLeft--
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val GYRO_TIME_LIMIT = 30
        private const val ACCEL_VARIANCE_THRESHOLD = 0.1f
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
        if (gyroSensor == null || accelSensor == null) return
        collecting = true; stepComplete = false; dataValid = false
        startBtn.visibility = View.GONE; retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE; progressBar.progress = 0
        accelSamples.clear()

        (activity as? CalibrationActivity)?.setStatusHeader(getString(R.string.gyro_collecting_header))
        statusText.text = getString(R.string.gyro_collecting)
        startBreathing()

        secondsLeft = GYRO_TIME_LIMIT
        handler.post(countdownRunnable)
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting) return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                if (sampleCount < targetSamples) {
                    samples.add(event.values.clone()); sampleCount++
                    activity?.runOnUiThread {
                        progressBar.progress = (sampleCount * 100) / targetSamples
                        statusText.text = getString(R.string.gyro_progress, sampleCount, targetSamples)
                    }
                }
                if (sampleCount >= targetSamples) {
                    stopCollection()
                    checkDataValidity()
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (accelSamples.size < 20) {
                    accelSamples.add(event.values.clone())
                } else {
                    var meanX = 0f; var meanY = 0f; var meanZ = 0f
                    for (s in accelSamples) { meanX += s[0]; meanY += s[1]; meanZ += s[2] }
                    val n = accelSamples.size.toFloat()
                    meanX /= n; meanY /= n; meanZ /= n
                    var varX = 0f; var varY = 0f; var varZ = 0f
                    for (s in accelSamples) {
                        varX += (s[0] - meanX) * (s[0] - meanX)
                        varY += (s[1] - meanY) * (s[1] - meanY)
                        varZ += (s[2] - meanZ) * (s[2] - meanZ)
                    }
                    varX /= n; varY /= n; varZ /= n
                    phoneMoving = varX > ACCEL_VARIANCE_THRESHOLD || varY > ACCEL_VARIANCE_THRESHOLD || varZ > ACCEL_VARIANCE_THRESHOLD
                    if (phoneMoving) {
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), R.string.gyro_phone_moving, Toast.LENGTH_SHORT).show()
                        }
                    }
                    accelSamples.clear()
                }
            }
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
        stopBreathing()
        handler.removeCallbacks(countdownRunnable)
    }

    private fun checkDataValidity() {
        if (phoneMoving || samples.size < targetSamples) {
            dataValid = false; stepComplete = false
            activity?.runOnUiThread {
                statusText.text = getString(R.string.gyro_failed)
                retryBtn.visibility = View.VISIBLE
                startBtn.visibility = View.GONE
            }
        } else {
            saveData()
        }
    }

    private fun saveData() {
        var bx = 0f; var by = 0f; var bz = 0f
        for (s in samples) { bx += s[0]; by += s[1]; bz += s[2] }
        val count = samples.size.toFloat().coerceAtLeast(1f)
        val bias = floatArrayOf(bx / count, by / count, bz / count)
        CalibrationManager(requireContext()).saveGyroBias(bias)
        dataValid = true; stepComplete = true
        (activity as? CalibrationActivity)?.onStepComplete()
        activity?.runOnUiThread {
            statusText.text = getString(R.string.gyro_done)
            startBtn.visibility = View.VISIBLE; startBtn.text = getString(R.string.btn_recalibrate); startBtn.isEnabled = true
            retryBtn.visibility = View.GONE
        }
    }

    private fun startBreathing() {
        phoneImage.post {
            breathAnimator = ValueAnimator.ofFloat(1f, 1.08f).apply {
                duration = 800; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    val scale = it.animatedValue as Float
                    phoneImage.scaleX = scale; phoneImage.scaleY = scale
                }
                start()
            }
        }
    }

    private fun stopBreathing() {
        breathAnimator?.cancel()
        phoneImage.animate().scaleX(1f).scaleY(1f).duration = 200
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid

    override fun resetUI() {
        stepComplete = false; dataValid = false
        startBtn.isEnabled = true; startBtn.visibility = View.VISIBLE; startBtn.text = getString(R.string.btn_start_collection)
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.INVISIBLE
        statusText.text = getString(R.string.gyro_ready)
        secondsLeft = GYRO_TIME_LIMIT; handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("00:30")
    }

    override fun saveCalibrationData() = Unit
    override fun getProgress(): Int = (sampleCount * 100) / targetSamples

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }
}