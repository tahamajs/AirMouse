package com.airmouse.ui.gesture

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GestureTrainingFragment : Fragment() {

    private lateinit var preferences: PreferencesManager
    private lateinit var statusText: TextView
    private lateinit var trainClickBtn: Button
    private lateinit var trainScrollBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gesture_training, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())
        statusText = view.findViewById(R.id.training_status)
        trainClickBtn = view.findViewById(R.id.train_click_btn)
        trainScrollBtn = view.findViewById(R.id.train_scroll_btn)

        trainClickBtn.setOnClickListener { startTraining("click") }
        trainScrollBtn.setOnClickListener { startTraining("scroll") }
    }

    private fun startTraining(type: String) {
        statusText.text = "Training $type gesture... Perform the gesture now"
        // Here you would read sensor data for a few seconds and compute thresholds
        // For demo, we simulate and save default values
        lifecycleScope.launch {
            delay(3000)
            when (type) {
                "click" -> {
                    preferences.setClickThreshold(12.0f) // example
                    statusText.text = "Click gesture learned!"
                }
                "scroll" -> {
                    preferences.setScrollThreshold(8.0f)
                    statusText.text = "Scroll gesture learned!"
                }
            }
            delay(1500)
            statusText.text = "Ready for next training"
        }
    }
}