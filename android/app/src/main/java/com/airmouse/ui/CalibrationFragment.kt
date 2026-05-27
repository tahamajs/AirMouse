package com.airmouse.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airmouse.R
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CalibrationFragment : Fragment() {

    private lateinit var calibrationHelper: CalibrationHelper
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var prefsManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_calibration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText = view.findViewById(R.id.calibration_status)
        progressBar = view.findViewById(R.id.calibration_progress)

        prefsManager = PreferencesManager(requireContext())

        calibrationHelper = CalibrationHelper(
            requireContext(),
            prefsManager
        )

        startCalibrationSequence()
    }

    private fun startCalibrationSequence() {
        lifecycleScope.launch {
            try {
                // STEP 1 — GYROSCOPE
                statusText.setText(R.string.calib_step1)
                progressBar.progress = 10
                calibrationHelper.calibrateGyro { instruction ->
                    statusText.text = instruction
                }

                delay(1000)

                // STEP 2 — MAGNETOMETER
                statusText.setText(R.string.calib_step2)
                progressBar.progress = 50
                calibrationHelper.calibrateMagnetometer(15000L) { instruction ->
                    statusText.text = instruction
                }

                delay(500)

                // STEP 3 — ACCELEROMETER
                statusText.setText(R.string.calib_step3)
                progressBar.progress = 80
                calibrationHelper.calibrateAccelerometer { instruction ->
                    statusText.text = instruction
                }

                // COMPLETE
                progressBar.progress = 100
                statusText.setText(R.string.calib_complete)

            } catch (e: Exception) {
                statusText.text = getString(
                    R.string.calib_failed,
                    e.message ?: "Unknown error"
                )
            }
        }
    }
}