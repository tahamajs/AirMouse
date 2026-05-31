package com.airmouse

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airmouse.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CustomGestureRecorderFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private lateinit var preferences: PreferencesManager
    private lateinit var actionSpinner: Spinner
    private lateinit var recordButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var savedGesturesText: TextView

    private val recordedValues = mutableListOf<Float>()
    private var isRecording = false
    private var recordingJob: kotlinx.coroutines.Job? = null

    private val actions = listOf("Click", "Double Click", "Right Click", "Scroll Up", "Scroll Down")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_custom_gesture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())
        sensorManager = requireContext().getSystemService(SensorManager::class.java)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        actionSpinner = view.findViewById(R.id.action_spinner)
        recordButton = view.findViewById(R.id.record_gesture_btn)
        statusText = view.findViewById(R.id.gesture_status)
        savedGesturesText = view.findViewById(R.id.saved_gestures_list)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, actions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        actionSpinner.adapter = adapter

        recordButton.setOnClickListener { startRecording() }
        updateSavedGesturesDisplay()
    }

    private fun startRecording() {
        if (gyroscope == null) {
            statusText.text = "Gyroscope not available"
            return
        }
        recordedValues.clear()
        isRecording = true
        statusText.text = "Recording gesture... Move your phone"
        recordButton.isEnabled = false
        recordButton.text = "Recording..."
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)

        recordingJob = lifecycleScope.launch {
            delay(3000)
            stopRecording()
        }
    }

    private fun stopRecording() {
        sensorManager.unregisterListener(this)
        isRecording = false
        recordingJob?.cancel()

        if (recordedValues.isNotEmpty()) {
            val avg = recordedValues.average().toFloat()
            val action = actions[actionSpinner.selectedItemPosition]
            preferences.saveCustomGesture(action, avg)
            statusText.text = "Saved '$action' with value: %.2f".format(avg)
            Snackbar.make(requireView(), "Gesture saved for $action", Snackbar.LENGTH_SHORT).show()
            updateSavedGesturesDisplay()
        } else {
            statusText.text = "No data recorded. Please try again."
        }
        recordButton.isEnabled = true
        recordButton.text = "Start Recording"
    }

    private fun updateSavedGesturesDisplay() {
        val sb = StringBuilder()
        for (action in actions) {
            val value = preferences.getCustomGesture(action)
            if (value != 0f) {
                sb.append("• $action: ${String.format("%.2f", value)}\n")
            }
        }
        savedGesturesText.text = if (sb.isEmpty()) "No custom gestures saved yet." else sb.toString()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isRecording && event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            // Use Y‑axis rotation as gesture value
            recordedValues.add(event.values[1])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        if (isRecording) stopRecording()
    }
}