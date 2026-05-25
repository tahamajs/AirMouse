package com.airmouse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airmouse.network.DataSender
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.MotionDetector
import com.airmouse.sensors.SensorService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var sensorService: SensorService
    private lateinit var calibrationHelper: CalibrationHelper
    private lateinit var motionDetector: MotionDetector
    private lateinit var dataSender: DataSender
    private lateinit var ipEditText: EditText
    private lateinit var statusText: TextView
    private lateinit var orientationIndicator: View
    private lateinit var calibrateBtn: Button
    private lateinit var startBtn: Button

    private var isCalibrated = false
    private var lastOrientation = Pair(0f, 0f)

    companion object {
        private const val PORT = 8080
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipEditText = findViewById(R.id.ip_edit_text)
        statusText = findViewById(R.id.status_text)
        orientationIndicator = findViewById(R.id.orientation_view)
        calibrateBtn = findViewById(R.id.calibrate_btn)
        startBtn = findViewById(R.id.start_btn)

        requestPermissions()

        calibrationHelper = CalibrationHelper(this)
        motionDetector = MotionDetector()
        sensorService = SensorService(this, calibrationHelper)

        calibrateBtn.setOnClickListener { startCalibration() }
        startBtn.setOnClickListener { startAirMouse() }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 1)
        }
    }

    private fun startCalibration() {
        statusText.text = "Calibrating... Move phone in figure-8"
        lifecycleScope.launch {
            calibrationHelper.calibrateMagnetometer(30_000)
            calibrationHelper.calibrateGyro()
            calibrationHelper.calibrateAccelerometer()
            isCalibrated = true
            statusText.text = "Calibration complete!"
            Toast.makeText(this@MainActivity, "Calibration done", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAirMouse() {
        if (!isCalibrated) {
            Toast.makeText(this, "Please calibrate first", Toast.LENGTH_SHORT).show()
            return
        }
        val ip = ipEditText.text.toString().trim()
        if (ip.isEmpty()) {
            Toast.makeText(this, "Enter laptop IP", Toast.LENGTH_SHORT).show()
            return
        }

        dataSender = DataSender(ip, PORT)
        dataSender.start()

        sensorService.start()
        sensorService.setOnOrientationChange { roll, yaw ->
            val deltaX = (yaw - lastOrientation.second) * 0.5f
            val deltaY = (roll - lastOrientation.first) * 0.5f
            lastOrientation = Pair(roll, yaw)
            dataSender.sendMove(deltaX, deltaY)
            updateUIIndicator(roll, yaw)
        }

        sensorService.setOnGestureDetected { gesture ->
            when (gesture) {
                MotionDetector.Gesture.CLICK -> dataSender.sendClick()
                MotionDetector.Gesture.SCROLL_UP -> dataSender.sendScroll(-1)
                MotionDetector.Gesture.SCROLL_DOWN -> dataSender.sendScroll(1)
            }
        }
        statusText.text = "Air Mouse Active"
    }

    private fun updateUIIndicator(roll: Float, yaw: Float) {
        orientationIndicator.rotation = yaw
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorService.stop()
        dataSender?.stopSending()
    }
}