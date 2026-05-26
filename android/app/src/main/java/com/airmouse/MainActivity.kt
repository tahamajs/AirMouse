package com.airmouse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
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
import com.airmouse.utils.NetworkUtils
import com.airmouse.utils.PreferencesManager
import com.airmouse.utils.SensorUtils
import kotlinx.coroutines.launch

/**
 * Main Activity for the Air Mouse application.
 */
class MainActivity : AppCompatActivity() {

    // ---------- Core components ----------
    private lateinit var sensorService: SensorService
    private lateinit var calibrationHelper: CalibrationHelper
    private lateinit var gestureDetector: EnhancedGestureDetector
    private lateinit var dataSender: DataSender
    private lateinit var autoReconnect: AutoReconnect
    private lateinit var preferences: PreferencesManager
    private lateinit var batterySaver: BatterySaver
    private lateinit var debugOverlay: DebugOverlay
    private lateinit var vibrator: Vibrator

    // ---------- UI elements ----------
    private lateinit var ipEditText: EditText
    private lateinit var statusText: TextView
    private lateinit var orientationIndicator: View
    private lateinit var calibrateBtn: Button
    private lateinit var startBtn: Button
    private lateinit var sensitivitySeekBar: SeekBar
    private lateinit var sensitivityText: TextView
    private lateinit var settingsBtn: Button
    private lateinit var debugToggleBtn: Button
    private lateinit var sensorStatusText: TextView

    // ---------- State ----------
    private var lastOrientation = Pair(0f, 0f)   // (roll, yaw)
    private var isCalibrated = false
    private var isActive = false

    // For debug overlay (latest sensor values)
    private var currentGyroY = 0f
    private var currentAccelY = 0f

    companion object {
        private const val PORT = 8080
        private const val OVERLAY_PERMISSION_REQUEST = 100
        private const val PERMISSIONS_REQUEST_CODE = 1
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        initializeComponents()
        setupSensitivitySlider()
        setupClickListeners()
        requestPermissionsAndOverlay()
        checkSensorAvailability()
    }

    override fun onResume() {
        super.onResume()
        if (isActive) {
            sensorService.start()
            batterySaver.start(sensorService)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorService.stop()
        batterySaver.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorService.stop()
        if (::dataSender.isInitialized) dataSender.stopSending()
        if (::autoReconnect.isInitialized) autoReconnect.stop()
        batterySaver.stop()
        debugOverlay.hide()
    }

    // ----------------------------------------------------------------------
    // Initialisation
    // ----------------------------------------------------------------------

    private fun bindViews() {
        ipEditText = findViewById(R.id.ip_edit_text)
        statusText = findViewById(R.id.status_text)
        orientationIndicator = findViewById(R.id.orientation_view)
        calibrateBtn = findViewById(R.id.calibrate_btn)
        startBtn = findViewById(R.id.start_btn)
        sensitivitySeekBar = findViewById(R.id.sensitivity_seekbar)
        sensitivityText = findViewById(R.id.sensitivity_text)
        settingsBtn = findViewById(R.id.settings_btn)
        debugToggleBtn = findViewById(R.id.debug_toggle_btn)
        sensorStatusText = findViewById(R.id.sensor_status_text)
    }

    private fun initializeComponents() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        preferences = PreferencesManager(this)
        batterySaver = BatterySaver()
        debugOverlay = DebugOverlay(this)

        calibrationHelper = CalibrationHelper(this)
        gestureDetector = EnhancedGestureDetector(this, preferences, vibrator)
        sensorService = SensorService(this, calibrationHelper, gestureDetector, preferences, batterySaver)
        debugOverlay.setSensorService(sensorService)

        // Load last used IP into the text field
        ipEditText.setText(preferences.getLastIp())
    }

    private fun checkSensorAvailability() {
        val missing = SensorUtils.getMissingSensorsMessage(this)
        if (!SensorUtils.hasGyroscope(this) || !SensorUtils.hasAccelerometer(this) || !SensorUtils.hasMagnetometer(this)) {
            sensorStatusText.text = missing
            sensorStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            Toast.makeText(this, "Warning: $missing", Toast.LENGTH_LONG).show()
        } else {
            sensorStatusText.text = "All sensors available"
            sensorStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        }
    }

    private fun setupSensitivitySlider() {
        val savedSensitivity = preferences.getSensitivity()
        // Map 0.2..2.0 to 0..100
        val progress = ((savedSensitivity - 0.2f) / 1.8f * 100).toInt().coerceIn(0, 100)
        sensitivitySeekBar.progress = progress
        sensitivityText.text = String.format("Speed: %.2fx", savedSensitivity)

        var pendingSave = false
        sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = 0.2f + (progress / 100f) * 1.8f
                sensitivityText.text = String.format("Speed: %.2fx", sensitivity)
                if (fromUser) pendingSave = true
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (pendingSave) {
                    val sensitivity = 0.2f + ((seekBar?.progress ?: 0) / 100f) * 1.8f
                    preferences.setSensitivity(sensitivity)
                    pendingSave = false
                }
            }
        })
    }

    private fun setupClickListeners() {
        calibrateBtn.setOnClickListener { startCalibration() }
        startBtn.setOnClickListener { startAirMouse() }
        settingsBtn.setOnClickListener { showSettingsDialog() }
        debugToggleBtn.setOnClickListener {
            if (checkOverlayPermission()) {
                debugOverlay.toggleVisibility()
                debugToggleBtn.text = if (debugOverlay.isVisible()) "Hide Debug" else "Show Debug"
            } else {
                Toast.makeText(this, "Overlay permission required for debug", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
        }
    }

    // ----------------------------------------------------------------------
    // Permissions
    // ----------------------------------------------------------------------

    private fun requestPermissionsAndOverlay() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.INTERNET)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.VIBRATE)
        if (permissions.isNotEmpty())
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
            requestOverlayPermission()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = android.net.Uri.parse("package:$packageName")
        }
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(this)
        else true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions denied – app may not work correctly", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST && checkOverlayPermission()) {
            Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------------------------------------------------
    // Calibration
    // ----------------------------------------------------------------------

    private fun startCalibration() {
        if (!NetworkUtils.isWifiConnected(this)) {
            Toast.makeText(this, "No WiFi connection – calibration still possible but server connection may fail later", Toast.LENGTH_LONG).show()
        }
        statusText.text = "Calibrating... (gyro: keep still)"
        lifecycleScope.launch {
            try {
                calibrationHelper.calibrateGyro()
                statusText.text = "Calibrating magnetometer – move in figure-8 (30s)"
                calibrationHelper.calibrateMagnetometer(30000)
                statusText.text = "Calibrating accelerometer (simplified)"
                calibrationHelper.calibrateAccelerometer()
                isCalibrated = true
                statusText.text = "Calibration complete!"
                Toast.makeText(this@MainActivity, "Calibration successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                statusText.text = "Calibration failed"
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ----------------------------------------------------------------------
    // Air Mouse activation
    // ----------------------------------------------------------------------

    private fun startAirMouse() {
        if (!isCalibrated) {
            Toast.makeText(this, "Please calibrate sensors first", Toast.LENGTH_SHORT).show()
            return
        }
        val ip = ipEditText.text.toString().trim()
        if (ip.isEmpty()) {
            Toast.makeText(this, "Enter laptop IP address", Toast.LENGTH_SHORT).show()
            return
        }
        if (!NetworkUtils.isWifiConnected(this)) {
            Toast.makeText(this, "No WiFi connection – cannot connect to PC", Toast.LENGTH_LONG).show()
            return
        }
        preferences.setLastIp(ip)

        try {
            dataSender = DataSender(ip, PORT, preferences)
            autoReconnect = AutoReconnect(dataSender, preferences)
            dataSender.start()
            autoReconnect.start()
        } catch (e: Exception) {
            Toast.makeText(this, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        // Set up sensor callbacks
        sensorService.setOnOrientationChange { roll, yaw ->
            val sensitivity = preferences.getSensitivity()
            val deltaX = (yaw - lastOrientation.second) * sensitivity * 0.8f
            val deltaY = (roll - lastOrientation.first) * sensitivity * 0.8f
            lastOrientation = Pair(roll, yaw)
            dataSender.sendMove(deltaX, deltaY)
            updateUIIndicator(roll, yaw)
            debugOverlay.updateValues(roll, yaw, currentGyroY, currentAccelY)
            // Battery saver uses orientation changes to detect movement
            batterySaver.updateMovement(roll, yaw)
        }

        sensorService.setOnGyroUpdate { gyroY -> currentGyroY = gyroY }
        sensorService.setOnAccelUpdate { accelY -> currentAccelY = accelY }

        sensorService.setOnGestureDetected { gesture ->
            when (gesture) {
                EnhancedGestureDetector.Gesture.CLICK -> {
                    dataSender.sendClick()
                    runOnUiThread { flashClick() }
                }
                EnhancedGestureDetector.Gesture.DOUBLE_CLICK -> {
                    dataSender.sendDoubleClick()
                    runOnUiThread { flashClick() }
                    Toast.makeText(this, "Double‑click", Toast.LENGTH_SHORT).show()
                }
                EnhancedGestureDetector.Gesture.RIGHT_CLICK -> {
                    dataSender.sendRightClick()
                    runOnUiThread { flashClick() }
                    Toast.makeText(this, "Right‑click", Toast.LENGTH_SHORT).show()
                }
                EnhancedGestureDetector.Gesture.SCROLL_UP -> dataSender.sendScroll(-1)
                EnhancedGestureDetector.Gesture.SCROLL_DOWN -> dataSender.sendScroll(1)
                else -> { /* NONE – do nothing */ }
            }
        }

        sensorService.start()
        batterySaver.start(sensorService)
        isActive = true
        statusText.text = "Air Mouse Active – connected to $ip"
    }

    // ----------------------------------------------------------------------
    // UI helpers
    // ----------------------------------------------------------------------

    private fun updateUIIndicator(roll: Float, yaw: Float) {
        orientationIndicator.rotation = yaw   // yaw corresponds to horizontal orientation
    }

    private fun flashClick() {
        orientationIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        orientationIndicator.postDelayed({
            orientationIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        }, 100)
    }

    private fun showSettingsDialog() {
        val dialog = SettingsDialog(this, preferences) {
            gestureDetector.reloadThresholds()
        }
        dialog.show()
    }
}