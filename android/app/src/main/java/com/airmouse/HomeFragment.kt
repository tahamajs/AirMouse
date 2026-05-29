
    package com.airmouse

    import android.content.Context
    import android.content.Intent
    import android.os.Bundle
    import android.provider.Settings
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.*
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AlertDialog
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.Fragment
    import androidx.lifecycle.lifecycleScope
    import com.airmouse.domain.GestureDetector
    import com.airmouse.network.AutoReconnect
    import com.airmouse.network.DataSender
    import com.airmouse.sensors.CalibrationHelper
    import com.airmouse.sensors.SensorService
    import com.airmouse.DebugOverlay
    import com.airmouse.ui.CalibrationActivity
    import com.airmouse.ui.SettingsDialog
    import com.airmouse.utils.*
    import com.google.android.material.floatingactionbutton.FloatingActionButton
    import com.google.android.material.snackbar.Snackbar
    import com.journeyapps.barcodescanner.ScanContract
    import com.journeyapps.barcodescanner.ScanOptions
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch
    import kotlin.math.abs

    class HomeFragment : Fragment() {

        // ---------- Core services ----------
        private lateinit var sensorService: SensorService
        private lateinit var calibrationHelper: CalibrationHelper
        private lateinit var gestureDetector: GestureDetector
        private lateinit var dataSender: DataSender
        private lateinit var autoReconnect: AutoReconnect
        private lateinit var preferences: PreferencesManager
        private lateinit var batterySaver: BatterySaver
        private lateinit var debugOverlay: DebugOverlay
        private lateinit var vibrator: android.os.Vibrator

        // ---------- UI widgets ----------
        private lateinit var ipEditText: EditText
        private lateinit var portEditText: EditText
        private lateinit var statusText: TextView
        private lateinit var calibrationProgressBar: ProgressBar
        private lateinit var calibrationProgressText: TextView
        private lateinit var attemptsText: TextView
        private lateinit var orientationIndicator: View
        private lateinit var calibrateBtn: com.google.android.material.button.MaterialButton
        private lateinit var startBtn: com.google.android.material.button.MaterialButton
        private lateinit var sensitivitySlider: SeekBar
        private lateinit var sensitivityText: TextView
        private lateinit var settingsBtn: com.google.android.material.button.MaterialButton
        private lateinit var debugToggleBtn: com.google.android.material.button.MaterialButton
        private lateinit var sensorStatusText: TextView
        private lateinit var connectionQualityText: TextView
        private lateinit var sensorDataText: TextView
        private lateinit var liveLogText: TextView
        private lateinit var clearLogsBtn: com.google.android.material.button.MaterialButton
        private lateinit var fabCalibrate: FloatingActionButton
        private lateinit var scanQrBtn: ImageButton          // <-- FIXED: ImageButton, not MaterialButton

        // ---------- State ----------
        private var lastOrientation = Pair(0f, 0f)
        private var isActive = false
        private var currentGyroY = 0f
        private var currentAccelY = 0f
        private var smoothedMoveX = 0f
        private var smoothedMoveY = 0f
        private var lastMoveDispatchMs = 0L
        private var lastUiUpdateMs = 0L

        companion object {
            private const val MOVE_EMA_ALPHA = 0.25f
            private const val MOVE_DEADBAND = 0.8f
            private const val MOVE_MIN_INTERVAL_MS = 16L
            private const val UI_MIN_INTERVAL_MS = 50L
            private const val MAX_CALIB_ATTEMPTS = 5
        }

        // ---------- QR Scanner ----------
        private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                val uri = result.contents
                if (uri.startsWith("airmouse://")) {
                    val part = uri.removePrefix("airmouse://")
                    val colonIdx = part.lastIndexOf(":")
                    if (colonIdx > 0) {
                        ipEditText.setText(part.substring(0, colonIdx))
                        portEditText.setText(part.substring(colonIdx + 1))
                    }
                }
            }
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_home, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            bindViews(view)
            initializeComponents()
            setupSensitivitySlider()
            setupClickListeners()
            checkSensorAvailability()
            startWifiMonitoring()
            startSensorDataUpdates()
            updateCalibrationStatusUi()

            clearLogsBtn.setOnClickListener {
                liveLogText.text = getString(R.string.log_placeholder)
                preferences.clearServerLogs()
                Toast.makeText(requireContext(), R.string.log_cleared, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onResume() {
            super.onResume()
            updateCalibrationStatusUi()
            if (isActive) {
                sensorService.start()
                batterySaver.start(sensorService)
            }
        }

        override fun onPause() {
            super.onPause()
            if (isActive) {
                sensorService.stop()
                batterySaver.stop()
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            stopAirMouseInternal()
            debugOverlay.hide()
        }

        // ---------- Initialization ----------
        private fun bindViews(view: View) {
            ipEditText = view.findViewById(R.id.ip_edit_text)
            portEditText = view.findViewById(R.id.port_edit_text)
            statusText = view.findViewById(R.id.status_text)
            calibrationProgressBar = view.findViewById(R.id.calibration_progress_bar)
            calibrationProgressText = view.findViewById(R.id.calibration_progress_text)
            attemptsText = view.findViewById(R.id.attempts_text)
            orientationIndicator = view.findViewById(R.id.orientation_view)
            calibrateBtn = view.findViewById(R.id.calibrate_btn)
            startBtn = view.findViewById(R.id.start_btn)
            sensitivitySlider = view.findViewById(R.id.sensitivity_seekbar)
            sensitivityText = view.findViewById(R.id.sensitivity_text)
            settingsBtn = view.findViewById(R.id.settings_btn)
            debugToggleBtn = view.findViewById(R.id.debug_toggle_btn)
            sensorStatusText = view.findViewById(R.id.sensor_status_text)
            connectionQualityText = view.findViewById(R.id.connection_quality_text)
            sensorDataText = view.findViewById(R.id.sensor_data_text)
            liveLogText = view.findViewById(R.id.live_log_text)
            clearLogsBtn = view.findViewById(R.id.clear_logs_btn)
            fabCalibrate = view.findViewById(R.id.fab_calibrate)
            scanQrBtn = view.findViewById(R.id.scan_qr_btn)
        }

        private fun initializeComponents() {
            vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            preferences = PreferencesManager(requireContext())
            batterySaver = BatterySaver()
            debugOverlay = DebugOverlay(requireContext())
            LogManager.init(requireContext())

            calibrationHelper = CalibrationHelper(requireContext(), preferences)
            gestureDetector = GestureDetector(preferences)
            sensorService = SensorService(requireContext(), calibrationHelper, gestureDetector, preferences, batterySaver)
            debugOverlay.setSensorService(sensorService)

            ipEditText.setText(preferences.getLastIp())
            portEditText.setText(preferences.getLastPort().toString())
            renderSavedLogs()
        }

        private fun checkSensorAvailability() {
            val message = SensorUtils.getMissingSensorsMessage(requireContext())
            val hasCore = SensorUtils.hasGyroscope(requireContext()) && SensorUtils.hasAccelerometer(requireContext())
            sensorStatusText.text = message
            sensorStatusText.setTextColor(
                if (hasCore) ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                else ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
            if (!hasCore) {
                Toast.makeText(requireContext(), R.string.sensor_warning_missing, Toast.LENGTH_LONG).show()
            }
        }

        private fun setupSensitivitySlider() {
            val savedSensitivity = preferences.getSensitivity()
            val progress = ((savedSensitivity - 0.2f) / 1.8f * 100).toInt().coerceIn(0, 100)
            sensitivitySlider.progress = progress
            sensitivityText.text = getString(R.string.speed_label, savedSensitivity)

            sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val sensitivity = 0.2f + (progress / 100f) * 1.8f
                    sensitivityText.text = getString(R.string.speed_label, sensitivity)
                    if (fromUser) preferences.setSensitivity(sensitivity)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        private fun setupClickListeners() {
            calibrateBtn.setOnClickListener { openCalibrationWizard() }
            startBtn.setOnClickListener { if (isActive) stopAirMouse() else startAirMouse() }
            settingsBtn.setOnClickListener { showSettingsDialog() }
            debugToggleBtn.setOnClickListener {
                if (Settings.canDrawOverlays(requireContext())) {
                    debugOverlay.toggleVisibility()
                    debugToggleBtn.text = if (debugOverlay.isVisible()) getString(R.string.hide_debug) else getString(R.string.debug)
                } else {
                    requestOverlayPermission()
                }
            }
            fabCalibrate.setOnClickListener { openCalibrationWizard() }
            scanQrBtn.setOnClickListener {
                qrLauncher.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE))
            }
        }

        private fun requestOverlayPermission() {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${requireContext().packageName}"))
            startActivity(intent)
        }

        // ---------- WiFi & Sensor Monitoring ----------
        private fun startWifiMonitoring() {
            lifecycleScope.launch {
                while (true) {
                    updateWifiQuality()
                    delay(3000)
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun updateWifiQuality() {
            val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            if (caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                val rssi = wifiManager.connectionInfo.rssi
                val quality = when {
                    rssi > -50 -> getString(R.string.good)
                    rssi > -70 -> getString(R.string.fair)
                    else -> getString(R.string.poor)
                }
                connectionQualityText.text = getString(R.string.signal_strength, quality, rssi)
                connectionQualityText.setTextColor(
                    when {
                        rssi > -50 -> android.graphics.Color.parseColor("#4CAF50")
                        rssi > -70 -> android.graphics.Color.parseColor("#FFC107")
                        else -> android.graphics.Color.parseColor("#F44336")
                    }
                )
            } else {
                connectionQualityText.text = getString(R.string.status_not_connected)
                connectionQualityText.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            }
        }

        private fun startSensorDataUpdates() {
            lifecycleScope.launch {
                while (true) {
                    sensorDataText.text = getString(R.string.sensor_data_format, currentGyroY, currentAccelY)
                    delay(200)
                }
            }
        }

        // ---------- Connection & Sending ----------
        private fun startAirMouse() {
            if (!preferences.isCalibrated()) {
                Toast.makeText(requireContext(), R.string.run_calibration_first, Toast.LENGTH_SHORT).show()
                return
            }
            val ip = ipEditText.text.toString().trim()
            if (!ValidationUtils.isValidIp(ip)) {
                ipEditText.error = getString(R.string.invalid_ip)
                return
            }
            val port = portEditText.text.toString().trim().toIntOrNull()?.coerceIn(1, 65535)
            if (port == null) {
                portEditText.error = getString(R.string.invalid_port)
                return
            }
            if (!NetworkUtils.isWifiConnected(requireContext())) {
                Toast.makeText(requireContext(), R.string.wifi_required, Toast.LENGTH_SHORT).show()
                return
            }

            preferences.setLastIp(ip)
            preferences.setLastPort(port)
            smoothedMoveX = 0f
            smoothedMoveY = 0f
            lastMoveDispatchMs = 0L

            try {
                dataSender = DataSender.getInstance(ip, port) ?: return
                autoReconnect = AutoReconnect(dataSender, ip, port) { newSender ->
                    dataSender = newSender
                    attachSensorCallbacks()
                }
                dataSender.onConnected = {
                    requireActivity().runOnUiThread {
                        statusText.text = getString(R.string.status_active_format, "$ip:$port")
                        connectionQualityText.text = getString(R.string.status_connected_syncing)
                        Snackbar.make(requireView(), getString(R.string.connected_to, "$ip:$port"), Snackbar.LENGTH_SHORT).show()
                        if (preferences.isHapticEnabled()) vibrate(50)
                        LogManager.add("Connected to $ip:$port")
                    }
                }
                dataSender.onDisconnected = {
                    requireActivity().runOnUiThread {
                        statusText.text = getString(R.string.reconnecting)
                        connectionQualityText.text = getString(R.string.status_waiting_reconnect)
                        LogManager.add("Disconnected from $ip:$port, retrying...")
                    }
                }

                attachSensorCallbacks()
                dataSender.start()
                autoReconnect.start()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.connection_error, e.message), Toast.LENGTH_LONG).show()
                return
            }

            sensorService.start()
            batterySaver.start(sensorService)
            isActive = true
            startBtn.text = getString(R.string.stop)
            calibrateBtn.isEnabled = false
            fabCalibrate.hide()
            updateCalibrationStatusUi()
        }

        private fun attachSensorCallbacks() {
            sensorService.setOnOrientationChange { roll, yaw ->
                val sensitivity = preferences.getSensitivity()
                val rawDeltaX = (yaw - lastOrientation.second) * sensitivity * 120.0f
                val rawDeltaY = (roll - lastOrientation.first) * sensitivity * 120.0f
                lastOrientation = Pair(roll, yaw)

                smoothedMoveX += (rawDeltaX - smoothedMoveX) * MOVE_EMA_ALPHA
                smoothedMoveY += (rawDeltaY - smoothedMoveY) * MOVE_EMA_ALPHA

                val now = System.currentTimeMillis()
                if (now - lastMoveDispatchMs >= MOVE_MIN_INTERVAL_MS) {
                    lastMoveDispatchMs = now
                    val sendX = if (abs(smoothedMoveX) >= MOVE_DEADBAND) smoothedMoveX else 0f
                    val sendY = if (abs(smoothedMoveY) >= MOVE_DEADBAND) smoothedMoveY else 0f
                    if (sendX != 0f || sendY != 0f) {
                        dataSender.sendMove(sendX, sendY)
                    }
                }

                val uiNow = System.currentTimeMillis()
                if (uiNow - lastUiUpdateMs >= UI_MIN_INTERVAL_MS) {
                    lastUiUpdateMs = uiNow
                    activity?.runOnUiThread {
                        updateUIIndicator(yaw)
                        debugOverlay.updateValues(roll, yaw, currentGyroY, currentAccelY)
                    }
                }
                batterySaver.updateMovement(roll, yaw)
            }

            sensorService.setOnGestureDetected { gesture ->
                when (gesture) {
                    GestureDetector.Gesture.CLICK -> {
                        dataSender.sendClick()
                        if (preferences.isHapticEnabled()) vibrate(30)
                        preferences.incrementClick()
                    }
                    GestureDetector.Gesture.DOUBLE_CLICK -> {
                        dataSender.sendDoubleClick()
                        if (preferences.isHapticEnabled()) vibrate(60)
                        preferences.incrementDoubleClick()
                    }
                    GestureDetector.Gesture.RIGHT_CLICK -> {
                        dataSender.sendRightClick()
                        if (preferences.isHapticEnabled()) vibrate(80)
                        preferences.incrementRightClick()
                    }
                    GestureDetector.Gesture.SCROLL_UP, GestureDetector.Gesture.SCROLL_DOWN -> {
                        dataSender.sendScroll(if (gesture == GestureDetector.Gesture.SCROLL_UP) -1 else 1)
                        if (preferences.isHapticEnabled()) vibrate(20)
                        preferences.incrementScroll()
                    }
                    else -> {}
                }
            }

            sensorService.setOnGyroUpdate { currentGyroY = it }
            sensorService.setOnAccelUpdate { currentAccelY = it }
        }

        private fun stopAirMouse() {
            stopAirMouseInternal()
            startBtn.text = getString(R.string.start)
            updateCalibrationStatusUi()
            calibrateBtn.isEnabled = true
            fabCalibrate.show()
        }

        private fun stopAirMouseInternal() {
            isActive = false
            if (::sensorService.isInitialized) sensorService.stop()
            if (::batterySaver.isInitialized) batterySaver.stop()
            if (::autoReconnect.isInitialized) autoReconnect.stop()
            if (::dataSender.isInitialized) dataSender.stopSending()
        }

        // ---------- Calibration UI (progress + attempts) ----------
        private fun openCalibrationWizard() {
            if (isActive) {
                Toast.makeText(requireContext(), R.string.stop_before_calibration, Toast.LENGTH_SHORT).show()
                return
            }
            val attempts = preferences.getCalibrationAttempts()
            val remaining = MAX_CALIB_ATTEMPTS - attempts
            if (remaining <= 0) {
                Toast.makeText(requireContext(), "No calibration attempts left. Restart the app.", Toast.LENGTH_LONG).show()
                return
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Start Calibration?")
                .setMessage("You have $remaining attempt(s) remaining.\n\nThis will guide you through calibrating all sensors.")
                .setPositiveButton("Yes") { _, _ ->
                    preferences.incrementCalibrationAttempts()
                    startActivity(Intent(requireContext(), CalibrationActivity::class.java))
                }
                .setNegativeButton("No", null)
                .show()
        }

        private fun updateCalibrationStatusUi() {
            val gyroCalibrated = preferences.getGyroBias().any { it != 0f }
            val accelCalibrated = preferences.getAccelScale().any { it != 1f }
            val magCalibrated = preferences.getMagScale().any { it != 1f }
            val calibratedCount = (if (gyroCalibrated) 1 else 0) + (if (accelCalibrated) 1 else 0) + (if (magCalibrated) 1 else 0)

            calibrationProgressBar.progress = (calibratedCount * 100) / 3
            calibrationProgressText.text = "$calibratedCount / 3 sensors calibrated"

            val attempts = preferences.getCalibrationAttempts()
            val remaining = (MAX_CALIB_ATTEMPTS - attempts).coerceAtLeast(0)
            attemptsText.text = "Attempts remaining: $remaining"
            attemptsText.setTextColor(
                if (remaining <= 1) ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                else ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )

            if (remaining <= 0 && calibratedCount < 3) {
                calibrateBtn.isEnabled = false
                calibrateBtn.text = "Calibration locked – restart app"
                fabCalibrate.hide()
                statusText.text = "Calibration attempts exhausted. Restart the app to try again."
            } else {
                calibrateBtn.isEnabled = !isActive
                calibrateBtn.text = "Calibrate Sensors"
                fabCalibrate.visibility = if (isActive) View.GONE else View.VISIBLE
                val message = when {
                    isActive -> getString(R.string.status_active)
                    calibratedCount >= 3 -> getString(R.string.calib_ready_to_connect)
                    else -> getString(R.string.calibration_needed_prompt)
                }
                statusText.text = message
            }
        }

        private fun updateUIIndicator(yaw: Float) {
            orientationIndicator.animate()
                .rotation(MathUtils.radToDeg(yaw))
                .setDuration(50)
                .start()
        }

        private fun vibrate(durationMs: Long) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }

        private fun showSettingsDialog() {
            SettingsDialog(requireContext(), preferences) {
                gestureDetector.reloadThresholds()
            }.show()
        }

        private fun renderSavedLogs() {
            val saved = preferences.getServerLogs()
            liveLogText.text = if (saved.isNotEmpty()) saved.joinToString("\n") else getString(R.string.log_placeholder)
        }
    }