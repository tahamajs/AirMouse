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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CalibrationFragment : Fragment() {
    private lateinit var calibrationHelper: CalibrationHelper
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calibration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusText = view.findViewById(R.id.calibration_status)
        progressBar = view.findViewById(R.id.calibration_progress)
        calibrationHelper = CalibrationHelper(requireContext())

        startCalibrationSequence()
    }

    private fun startCalibrationSequence() {
        lifecycleScope.launch {
            statusText.text = "Step 1/3: Gyro calibration – keep phone still"
            progressBar.progress = 10
            calibrationHelper.calibrateGyro()
            delay(1000)
            statusText.text = "Step 2/3: Magnetometer – move in figure-8"
            progressBar.progress = 50
            calibrationHelper.calibrateMagnetometer(30000)
            statusText.text = "Step 3/3: Accelerometer – simplified"
            progressBar.progress = 90
            calibrationHelper.calibrateAccelerometer()
            progressBar.progress = 100
            statusText.text = "Calibration complete! Return to main."
        }
    }
}