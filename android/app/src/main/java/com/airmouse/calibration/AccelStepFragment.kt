package com.airmouse.calibration

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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

class AccelStepFragment : Fragment(), SensorEventListener, CalibrationStepFragment {

    private data class Position(
        val description: String,
        val rotation: Float = 0f,
        val rotationX: Float = 0f,
        val scaleX: Float = 1f,
    )

    private val positions = listOf(
        Position("Flat on table, screen up", 0f, 0f, 1.0f),
        Position("Flat on table, screen down", 0f, 180f, 1.0f),
        Position("Vertical, top edge up", 0f, 90f, 0.8f),
        Position("Vertical, top edge down", 0f, -90f, 0.8f),
        Position("Left edge up", -90f, 0f, 0.8f),
        Position("Right edge up", 90f, 0f, 0.8f)
    )

    private var currentPosIndex = 0
    private val samplesPerPosition = mutableListOf<MutableList<FloatArray>>()
    private var sampleCount = 0
    private val targetSamples = 100
    private var collecting = false
    private var stepComplete = false

    private lateinit var instructionText: TextView
    private lateinit var phoneAnimation: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var startBtn: Button
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_accel_step, container, false)
        instructionText = view.findViewById(R.id.instructionText)
        phoneAnimation = view.findViewById(R.id.phoneAnimation)
        progressBar = view.findViewById(R.id.progressBar)
        startBtn = view.findViewById(R.id.startBtn)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        repeat(positions.size) {
            samplesPerPosition.add(mutableListOf())
        }

        startBtn.setOnClickListener {
            if (!collecting) startCollection()
        }

        resetUI()
        return view
    }

    private fun startCollection() {
        if (accelSensor == null) return
        collecting = true
        stepComplete = false
        startBtn.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        instructionText.text = "Collecting... position ${currentPosIndex + 1}/6"
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (sampleCount < targetSamples) {
            samplesPerPosition[currentPosIndex].add(event.values.clone())
            sampleCount++
            activity?.runOnUiThread {
                progressBar.progress = (sampleCount * 100) / targetSamples
            }
        }
        if (sampleCount >= targetSamples) {
            stopCollection()
            advanceOrFinish()
        }
    }

    private fun stopCollection() {
        sensorManager.unregisterListener(this)
        collecting = false
    }

    private fun advanceOrFinish() {
        if (currentPosIndex < positions.size - 1) {
            currentPosIndex++
            sampleCount = 0
            val target = positions[currentPosIndex]
            val animSet = AnimatorSet()
            animSet.playTogether(
                ObjectAnimator.ofFloat(phoneAnimation, "rotation", phoneAnimation.rotation, target.rotation),
                ObjectAnimator.ofFloat(phoneAnimation, "rotationX", phoneAnimation.rotationX, target.rotationX),
                ObjectAnimator.ofFloat(phoneAnimation, "scaleX", phoneAnimation.scaleX, target.scaleX),
                ObjectAnimator.ofFloat(phoneAnimation, "scaleY", phoneAnimation.scaleY, target.scaleX)
            )
            animSet.duration = 600
            animSet.start()
            instructionText.text = target.description
            startBtn.isEnabled = true
            progressBar.visibility = View.INVISIBLE
            progressBar.progress = 0
        } else {
            saveCalibrationData()
            stepComplete = true
            (activity as? CalibrationActivity)?.onStepComplete()
            instructionText.text = "Accelerometer calibrated!"
        }
    }

    override fun saveCalibrationData() {
        CalibrationManager(requireContext()).setAccelCalibrated(true)
    }

    override fun isStepComplete() = stepComplete

    override fun resetUI() {
        stepComplete = false
        currentPosIndex = 0
        sampleCount = 0
        startBtn.isEnabled = true
        phoneAnimation.rotation = positions[0].rotation
        phoneAnimation.rotationX = positions[0].rotationX
        phoneAnimation.scaleX = positions[0].scaleX
        phoneAnimation.scaleY = positions[0].scaleX
        instructionText.text = positions[0].description
        progressBar.visibility = View.INVISIBLE
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }
}