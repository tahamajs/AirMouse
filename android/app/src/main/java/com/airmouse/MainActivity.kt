package com.airmouse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airmouse.network.AutoReconnect
import com.airmouse.network.DataSender
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.sensors.SensorService
import com.airmouse.ui.DebugOverlay
import com.airmouse.ui.SettingsDialog
import com.airmouse.utils.BatterySaver
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var sensorService: SensorService
    private lateinit var calibrationHelper: CalibrationHelper
    private lateinit var gestureDetector: EnhancedGestureDetector
    private lateinit var dataSender: DataSender
    private lateinit var autoReconnect: AutoReconnect
    private lateinit var preferences: PreferencesManager
    private lateinit var batterySaver: BatterySaver
    private lateinit var debugOverlay: DebugOverlay
    private lateinit var vibrator: Vibrator

    // UI elements
    private lateinit var ipEditText: EditText
    private lateinit var statusText: TextView
    private lateinit var orientationIndicator: View
    private lateinit var calibrateBtn: Button
    private lateinit var startBtn: Button
    private lateinit var sensitivitySeekBar: SeekBar
    private lateinit var sensitivityText: TextView
    private lateinit var settingsBtn: Button
    private lateinit var debugToggleBtn: Button

    private var lastOrientation = Pair(0f, 0f)
    private var isCalibrated = false
    private var isActive = false

    companion object {
        private const val PORT = 8080
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI bindings
        ipEditText = findViewById(R.id.ip_edit_text)
        statusText = findViewById(R.id.status_text)
        orientationIndicator = findViewById(R.id.orientation_view)
        calibrateBtn = findViewById(R.id.calibrate_btn)
        startBtn = findViewById(R.id.start_btn)
        sensitivitySeekBar = findViewById(R.id.sensitivity_seekbar)
        sensitivityText = findViewById(R.id.sensitivity_text)
        settingsBtn = findViewById(R.id.settings_btn)
        debugToggleBtn = findViewById(R.id.debug_toggle_btn)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        preferences = PreferencesManager(this)
        batterySaver = BatterySaver()
        debugOverlay = DebugOverlay(this)

        requestPermissions()

        // Sensitivity slider
        val savedSensitivity = preferences.getSensitivity()
        sensitivitySeekBar.progress = (savedSensitivity * 50).toInt() // 0.2..2.0 -> 0..90
        sensitivityText.text = "Speed: ${String.format("%.1f", savedSensitivity)}x"
        sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = 0.2f + (progress / 90f) * 1.8f
                sensitivityText.text = "Speed: ${String.format("%.1f", sensitivity)}x"
                if (fromUser) preferences.setSensitivity(sensitivity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        calibrationHelper = CalibrationHelper(this)
        gestureDetector = EnhancedGestureDetector(this, preferences, vibrator)
        sensorService = SensorService(this, calibrationHelper, gestureDetector, preferences, batterySaver)
        debugOverlay.setSensorService(sensorService)

        calibrateBtn.setOnClickListener { startCalibration() }
        startBtn.setOnClickListener { startAirMouse() }
        settingsBtn.setOnClickListener { showSettingsDialog() }
        debugToggleBtn.setOnClickListener {
            debugOverlay.toggleVisibility()
            debugToggleBtn.text = if (debugOverlay.isVisible()) "Hide Debug" else "Show Debug"
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.INTERNET)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.VIBRATE)
        if (permissions.isNotEmpty())
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
    }

    private fun startCalibration() {
        statusText.text = "Calibrating... Move phone in figure-8"
        lifecycleScope.launch {
            calibrationHelper.calibrateMagnetometer(30000)
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
        preferences.setLastIp(ip)

        dataSender = DataSender(ip, PORT, preferences)
        autoReconnect = AutoReconnect(dataSender, preferences)
        dataSender.start()
        autoReconnect.start()

        sensorService.start()
        sensorService.setOnOrientationChange { roll, yaw ->
            val sensitivity = preferences.getSensitivity()
            val deltaX = (yaw - lastOrientation.second) * sensitivity * 0.8f
            val deltaY = (roll - lastOrientation.first) * sensitivity * 0.8f
            lastOrientation = Pair(roll, yaw)
            dataSender.sendMove(deltaX, deltaY)
            updateUIIndicator(roll, yaw)
            debugOverlay.updateValues(roll, yaw, sensorService.getGyroY(), sensorService.getAccelY())
        }

        sensorService.setOnGestureDetected { gesture ->
            when (gesture) {
                EnhancedGestureDetector.Gesture.CLICK -> {
                    dataSender.sendClick()
                    runOnUiThread { flashClick() }
                }
                EnhancedGestureDetector.Gesture.DOUBLE_CLICK -> {
                    dataSender.sendDoubleClick()
                    runOnUiThread { flashClick() }
                    Toast.makeText(this, "Double-click", Toast.LENGTH_SHORT).show()
                }
                EnhancedGestureDetector.Gesture.RIGHT_CLICK -> {
                    dataSender.sendRightClick()
                    runOnUiThread { flashClick() }
                    Toast.makeText(this, "Right-click", Toast.LENGTH_SHORT).show()
                }
                EnhancedGestureDetector.Gesture.SCROLL_UP -> dataSender.sendScroll(-1)
                EnhancedGestureDetector.Gesture.SCROLL_DOWN -> dataSender.sendScroll(1)
            }
        }

        isActive = true
        statusText.text = "Air Mouse Active"
        batterySaver.start(sensorService)
    }

    private fun updateUIIndicator(roll: Float, yaw: Float) {
        orientationIndicator.rotation = yaw
    }

    private fun flashClick() {
        orientationIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        orientationIndicator.postDelayed({
            orientationIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        }, 100)
    }

    private fun showSettingsDialog() {
        val dialog = SettingsDialog(this, preferences) {
            // After settings changed, reapply thresholds to gesture detector
            gestureDetector.reloadThresholds()
        }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorService.stop()
        dataSender.stopSending()
        autoReconnect.stop()
        batterySaver.stop()
        debugOverlay.hide()
    }
}