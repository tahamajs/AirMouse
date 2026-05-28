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
import com.airmouse.utils.SensorUtils
import com.airmouse.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class CalibrationFragment : Fragment() {

    private lateinit var calibrationHelper: CalibrationHelper
    private lateinit var prefsManager: PreferencesManager
    private lateinit var statusText: TextView
    private lateinit var instructionText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: MaterialButton
    private lateinit var skipMagButton: MaterialButton
    private lateinit var resetButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_calibration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefsManager = PreferencesManager(requireContext())
        calibrationHelper = CalibrationHelper(requireContext(), prefsManager)

        statusText = view.findViewById(R.id.calibration_status)
        instructionText = view.findViewById(R.id.calibration_instruction)
        progressBar = view.findViewById(R.id.calibration_progress)
        startButton = view.findViewById(R.id.start_guided_btn)
        skipMagButton = view.findViewById(R.id.skip_mag_btn)
        resetButton = view.findViewById(R.id.reset_btn)

        resetUi()
        val hasCoreSensors = SensorUtils.hasGyroscope(requireContext()) && SensorUtils.hasAccelerometer(requireContext())
        if (!hasCoreSensors) {
            statusText.setText(R.string.sensor_warning_missing)
            startButtonsEnabled(false)
        }

        startButton.setOnClickListener {
            runCalibration(includeMagnetometer = true)
        }
        skipMagButton.setOnClickListener {
            runCalibration(includeMagnetometer = false)
        }
        resetButton.setOnClickListener {
            resetUi()
        }
    }

    private fun resetUi() {
        progressBar.progress = 0
        statusText.setText(R.string.calibration_status_idle)
        instructionText.setText(R.string.calibration_ready_prompt)
        startButtonsEnabled(true)
    }

    private fun startButtonsEnabled(enabled: Boolean) {
        startButton.isEnabled = enabled
        skipMagButton.isEnabled = enabled
        resetButton.isEnabled = enabled
    }

    private fun runCalibration(includeMagnetometer: Boolean) {
        lifecycleScope.launch {
            startButtonsEnabled(false)
            try {
                statusText.setText(R.string.calibration_status_gyro)
                instructionText.setText(R.string.calib_step1_detail)
                progressBar.progress = 10
                calibrationHelper.calibrateGyro { instruction ->
                    requireActivity().runOnUiThread { instructionText.text = instruction }
                }

                if (includeMagnetometer) {
                    statusText.setText(R.string.calibration_status_mag)
                    instructionText.setText(R.string.calib_step2_detail)
                    progressBar.progress = 50
                    calibrationHelper.calibrateMagnetometer(15000L) { instruction ->
                        requireActivity().runOnUiThread { instructionText.text = instruction }
                    }
                }

                statusText.setText(R.string.calibration_status_accel)
                instructionText.setText(R.string.calib_step3_detail)
                progressBar.progress = if (includeMagnetometer) 80 else 60
                calibrationHelper.calibrateAccelerometer { instruction ->
                    requireActivity().runOnUiThread { instructionText.text = instruction }
                }

                progressBar.progress = 100
                statusText.setText(R.string.calibration_status_done)
                instructionText.setText(R.string.calibrated_ready)
                prefsManager.setCalibrated(true)
            } catch (e: Exception) {
                progressBar.progress = 0
                statusText.text = getString(R.string.calibration_status_error)
                instructionText.text = getString(R.string.calib_failed, e.message ?: "Unknown")
            } finally {
                startButtonsEnabled(true)
            }
        }
    }
}
