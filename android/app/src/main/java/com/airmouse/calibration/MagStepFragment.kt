// file: calibration/MagStepFragment.kt
package com.airmouse.calibration

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
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

    // UI
    private lateinit var instructionText: TextView
    private lateinit var coachMessage: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var coverageView: CoverageView
    private lateinit var startBtn: Button
    private lateinit var retryBtn: Button

    private var secondsLeft = MAG_TIME_LIMIT
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!collecting) return
            if (secondsLeft <= 0) {
                stopCollection()
                finishCalibration()
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
        private const val MAG_TIME_LIMIT = 60
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mag_step, container, false)
        instructionText = view.findViewById(R.id.instructionText)
        coachMessage = view.findViewById(R.id.coachMessage)
        progressBar = view.findViewById(R.id.progressBar)
        coverageView = view.findViewById(R.id.coverageView)
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
            startBtn.text = getString(R.string.mag_not_available)
        }
        resetUI()
        return view
    }

    private fun startCollection() {
        if (magSensor == null) return
        collecting = true
        stepComplete = false
        dataValid = false
        samples.clear()
        sampleCount = 0
        coverageView.reset()
        coverageView.startAnimation()

        startBtn.visibility = View.GONE
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        instructionText.text = getString(R.string.mag_instruction)
        coachMessage.text = getString(R.string.mag_coach_move)

        (activity as? CalibrationActivity)?.setStatusHeader(getString(R.string.mag_status_header))

        secondsLeft = MAG_TIME_LIMIT
        handler.post(countdownRunnable)
        (activity as? CalibrationActivity)?.changeState(com.airmouse.calibration.CalibrationState.COLLECTING)
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        samples.add(event.values.clone())
        sampleCount++
        activity?.runOnUiThread {
            val percent = (sampleCount * 100 / targetSamples).coerceAtMost(100)
            progressBar.progress = percent
            (activity as? com.airmouse.ui.CalibrationActivity)?.updateStepProgress(percent)
            coverageView.updateCoverage(percent)
            if (sampleCount >= targetSamples) {
                coachMessage.text = getString(R.string.mag_enough_data)
            } else {
                coachMessage.text = getString(R.string.mag_collecting_samples, sampleCount, targetSamples)
            }
        }
        if (sampleCount >= targetSamples) {
            stopCollection()
            finishCalibration()
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
        handler.removeCallbacks(countdownRunnable)
        coverageView.stopAnimation()
    }

    private fun finishCalibration() {
        if (samples.size < 50) {
            dataValid = false
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), R.string.mag_insufficient_data, Toast.LENGTH_LONG).show()
                retryBtn.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
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
        val offset = floatArrayOf((maxX + minX) / 2f, (maxY + minY) / 2f, (maxZ + minZ) / 2f)
        val scale = floatArrayOf((maxX - minX) / 2f, (maxY - minY) / 2f, (maxZ - minZ) / 2f)
        CalibrationManager(requireContext()).saveMagCalibration(offset, scale)
        dataValid = true
        stepComplete = true
        activity?.runOnUiThread {
            (activity as? CalibrationActivity)?.onStepComplete()
            (activity as? CalibrationActivity)?.changeState(com.airmouse.calibration.CalibrationState.STEP_COMPLETE)
            coachMessage.text = getString(R.string.mag_done)
            coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            instructionText.text = getString(R.string.mag_complete)
            startBtn.visibility = View.GONE
            retryBtn.visibility = View.GONE
        }
    }

    override fun isStepComplete() = stepComplete
    override fun isDataValid() = dataValid
    override fun resetUI() {
        stepComplete = false
        dataValid = false
        sampleCount = 0
        samples.clear()
        startBtn.isEnabled = true
        startBtn.visibility = View.VISIBLE
        startBtn.text = getString(R.string.btn_start_collection)
        retryBtn.visibility = View.GONE
        progressBar.visibility = View.INVISIBLE
        instructionText.text = getString(R.string.mag_ready)
        coachMessage.text = getString(R.string.mag_ready_desc)
        coachMessage.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        coverageView.reset()
        coverageView.stopAnimation()
        secondsLeft = MAG_TIME_LIMIT
        handler.removeCallbacks(countdownRunnable)
        (activity as? CalibrationActivity)?.setTimerText("01:00")
    }

    override fun saveCalibrationData() = Unit
    override fun getProgress(): Int = if (targetSamples > 0) (sampleCount * 100 / targetSamples) else 0
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }

    // Custom view for figure‑8 animation and coverage ring
    class CoverageView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3F51B5")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80CBC4")
            style = Paint.Style.FILL
        }
        private val path = Path()
        private var animProgress = 0f
        private var coveragePercent = 0f
        private var animator: ValueAnimator? = null

        init {
            val w = 400f
            val h = 300f
            for (t in 0..360 step 5) {
                val rad = Math.toRadians(t.toDouble())
                val x = (w * 0.4 * Math.sin(rad)).toFloat()
                val y = (h * 0.3 * Math.sin(2 * rad)).toFloat()
                if (t == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }

        fun startAnimation() {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 4000
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    animProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun stopAnimation() {
            animator?.cancel()
            animator = null
        }

        fun updateCoverage(percent: Int) {
            coveragePercent = percent / 100f
            invalidate()
        }

        fun reset() {
            coveragePercent = 0f
            animProgress = 0f
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            canvas.translate(width / 2f, height / 2f)
            canvas.scale(0.8f, 0.8f)

            canvas.drawPath(path, paint)

            val measure = PathMeasure(path, false)
            val pos = FloatArray(2)
            measure.getPosTan(measure.length * animProgress, pos, null)
            canvas.drawCircle(pos[0], pos[1], 16f, fillPaint)

            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF9800")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawCircle(0f, 0f, 80f + 40f * coveragePercent, ringPaint)

            canvas.restore()
        }
    }
}