package com.airmouse

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airmouse.bluetooth.BluetoothMouseService
import com.airmouse.bluetooth.BtHidHelper
import com.airmouse.domain.GestureDetector
import com.airmouse.ConnectionManager
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.SensorService
import com.airmouse.touchpad.TouchpadFragment
import com.airmouse.ui.CalibrationActivity
import com.airmouse.ui.SettingsDialog
import com.airmouse.utils.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

class HomeFragment : Fragment() {

    // ---------- Core services ----------
    private lateinit var sensorService: SensorService
    private lateinit var calibrationHelper: CalibrationHelper
    private lateinit var gestureDetector: GestureDetector
    private lateinit var preferences: PreferencesManager
    private lateinit var batterySaver: BatterySaver
    private lateinit var debugOverlay: DebugOverlay
    private var vibrator: android.os.Vibrator? = null
    private var wifiJob: Job? = null
    private var sensorUpdateJob: Job? = null

    // ---------- Modern UI components ----------
    private lateinit var connectionCard: MaterialCardView
    private lateinit var ipEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var connectBtn: MaterialButton
    private lateinit var connectionStatusText: TextView
    private lateinit var connectionQualityIcon: ImageView
    private lateinit var scanQrBtn: ImageButton

    private lateinit var calibrationCard: MaterialCardView
    private lateinit var calibrationProgressBar: ProgressBar
    private lateinit var calibrationProgressText: TextView
    private lateinit var attemptsText: TextView
    private lateinit var calibrateFab: FloatingActionButton
    private lateinit var calibrateBtn: MaterialButton

    private lateinit var sensorDataCard: MaterialCardView
    private lateinit var gyroValue: TextView
    private lateinit var accelValue: TextView
    private lateinit var orientationIndicator: View

    private lateinit var gestureStatsCard: MaterialCardView
    private lateinit var clickCountText: TextView
    private lateinit var scrollCountText: TextView
    private lateinit var rightClickCountText: TextView
    private lateinit var doubleClickCountText: TextView

    private lateinit var controlsCard: MaterialCardView
    private lateinit var sensitivitySlider: Slider
    private lateinit var sensitivityValue: TextView
    private lateinit var startStopBtn: MaterialButton
    private lateinit var settingsBtn: MaterialButton
    private lateinit var debugToggleBtn: MaterialButton
    private lateinit var btMouseBtn: MaterialButton
    private lateinit var modeToggle: MaterialButton
    private lateinit var modeToggleTouchpad: MaterialButton

    private lateinit var liveLogText: TextView
    private lateinit var clearLogsBtn: MaterialButton

    // ---------- Touchpad ----------
    private lateinit var touchpadContainer: FrameLayout
    private var isTouchpadMode = false
    private lateinit var motionControlsContainer: ViewGroup

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
        result.contents?.let { uri ->
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

    // ---------- Bluetooth permission launcher ----------
    private val btPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            BtHidHelper.startService(requireContext())
            btMouseBtn.text = getString(R.string.bt_mouse_stop)
        } else {
            Toast.makeText(requireContext(), getString(R.string.bluetooth_permission_required), Toast.LENGTH_LONG).show()
        }
    }

    private val btEnableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) checkBtPermissionAndStart()
        else Toast.makeText(requireContext(), getString(R.string.bluetooth_must_be_enabled), Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        initializeComponents()
        ConnectionManager.init(requireContext())

        ConnectionManager.tcpStatus.observe(viewLifecycleOwner) { status ->
            if (!isActive) connectionStatusText.text = status
        }
        ConnectionManager.tcpState.observe(viewLifecycleOwner) { state ->
            when (state) {
                ConnectionManager.ConnectionState.CONNECTED -> {
                    connectBtn.text = getString(R.string.disconnect)
                    connectionQualityIcon.setImageResource(android.R.drawable.presence_online)
                }
                ConnectionManager.ConnectionState.CONNECTING -> {
                    connectBtn.text = getString(R.string.connect)
                    connectionQualityIcon.setImageResource(android.R.drawable.presence_away)
                }
                else -> {
                    connectBtn.text = getString(R.string.connect)
                    connectionQualityIcon.setImageResource(android.R.drawable.presence_invisible)
                }
            }
        }
        ConnectionManager.dataSenderState.observe(viewLifecycleOwner) { state ->
            when (state) {
                ConnectionManager.ConnectionState.CONNECTED -> connectionStatusText.text = getString(R.string.status_active)
                ConnectionManager.ConnectionState.RECONNECTING -> connectionStatusText.text = getString(R.string.reconnecting)
                ConnectionManager.ConnectionState.DISCONNECTED -> connectionStatusText.text = getString(R.string.status_not_connected)
                else -> {}
            }
        }
        ConnectionManager.bluetoothRunning.observe(viewLifecycleOwner) { running ->
            btMouseBtn.text = if (running) getString(R.string.bt_mouse_stop) else getString(R.string.bt_mouse_start)
        }

        setupTouchpadMode(view)
        setupSensitivitySlider()
        setupClickListeners()
        checkSensorAvailability()
        startWifiMonitoring()
        startSensorDataUpdates()
        updateCalibrationStatusUi()

        clearLogsBtn.setOnClickListener {
            liveLogText.text = getString(R.string.log_placeholder)
            preferences.clearServerLogs()
            Toast.makeText(requireContext(), getString(R.string.log_cleared), Toast.LENGTH_SHORT).show()
        }

        if (requireActivity().getSystemService("bluetooth_hid_device") == null) {
            btMouseBtn.isEnabled = false
            btMouseBtn.text = getString(R.string.bt_hid_unsupported)
        } else {
            btMouseBtn.text = if (BluetoothMouseService.instance == null) getString(R.string.bt_mouse_start) else getString(R.string.bt_mouse_stop)
        }
    }

    private fun bindViews(view: View) {
        connectionCard = view.findViewById(R.id.connectionCard)
        ipEditText = view.findViewById(R.id.ip_edit_text)
        portEditText = view.findViewById(R.id.port_edit_text)
        connectBtn = view.findViewById(R.id.connect_btn)
        connectionStatusText = view.findViewById(R.id.status_text)
        connectionQualityIcon = view.findViewById(R.id.connection_quality_icon)
        scanQrBtn = view.findViewById(R.id.scan_qr_btn)

        calibrationCard = view.findViewById(R.id.calibrationCard)
        calibrationProgressBar = view.findViewById(R.id.calibration_progress_bar)
        calibrationProgressText = view.findViewById(R.id.calibration_progress_text)
        attemptsText = view.findViewById(R.id.attempts_text)
        calibrateFab = view.findViewById(R.id.calibrate_fab)
        calibrateBtn = view.findViewById(R.id.calibrate_btn)

        sensorDataCard = view.findViewById(R.id.sensorDataCard)
        gyroValue = view.findViewById(R.id.gyro_value)
        accelValue = view.findViewById(R.id.accel_value)
        orientationIndicator = view.findViewById(R.id.orientation_view)

        gestureStatsCard = view.findViewById(R.id.gestureStatsCard)
        clickCountText = view.findViewById(R.id.click_count)
        scrollCountText = view.findViewById(R.id.scroll_count)
        rightClickCountText = view.findViewById(R.id.right_click_count)
        doubleClickCountText = view.findViewById(R.id.double_click_count)

        controlsCard = view.findViewById(R.id.controlsCard)
        sensitivitySlider = view.findViewById(R.id.sensitivity_slider)
        sensitivityValue = view.findViewById(R.id.sensitivity_value)
        startStopBtn = view.findViewById(R.id.start_stop_btn)
        settingsBtn = view.findViewById(R.id.settings_btn)
        debugToggleBtn = view.findViewById(R.id.debug_toggle_btn)
        btMouseBtn = view.findViewById(R.id.bt_mouse_btn)
        modeToggle = view.findViewById(R.id.modeToggle)
        modeToggleTouchpad = view.findViewById(R.id.modeToggleTouchpad)

        liveLogText = view.findViewById(R.id.live_log_text)
        clearLogsBtn = view.findViewById(R.id.clear_logs_btn)

        touchpadContainer = view.findViewById(R.id.touchpadContainer)
        motionControlsContainer = view.findViewById(R.id.motionControlsContainer)
    }

    private fun initializeComponents() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
            requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        } else {
            @Suppress("DEPRECATION")
            requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
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

    private fun setupTouchpadMode(view: View) {
        val touchpadFragment = TouchpadFragment()
        touchpadFragment.tcpSender = { json ->
            ConnectionManager.sendTcpMessage(json)
        }
        childFragmentManager.beginTransaction()
            .add(R.id.touchpadContainer, touchpadFragment, "touchpad")
            .commit()

        fun showMotionMode() {
            isTouchpadMode = false
            modeToggle.text = getString(R.string.motion_mode)
            modeToggleTouchpad.text = getString(R.string.touchpad_mode)
            motionControlsContainer.visibility = View.VISIBLE
            touchpadContainer.visibility = View.GONE
            if (isActive) {
                sensorService.start()
                batterySaver.start(sensorService)
            }
        }

        fun showTouchpadMode() {
            isTouchpadMode = true
            modeToggle.text = getString(R.string.motion_mode)
            modeToggleTouchpad.text = getString(R.string.touchpad_mode)
            motionControlsContainer.visibility = View.GONE
            touchpadContainer.visibility = View.VISIBLE
            if (isActive) {
                sensorService.stop()
                batterySaver.stop()
            }
        }

        modeToggle.setOnClickListener { showMotionMode() }
        modeToggleTouchpad.setOnClickListener { showTouchpadMode() }

        showMotionMode()
    }

    private fun setupSensitivitySlider() {
        val savedSensitivity = preferences.getSensitivity()
        sensitivitySlider.value = (savedSensitivity - 0.2f) / 1.8f * 100f
        sensitivityValue.text = String.format(Locale.getDefault(), "%.2f", savedSensitivity)

        sensitivitySlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val sens = 0.2f + (value / 100f) * 1.8f
                sensitivityValue.text = String.format(Locale.getDefault(), "%.2f", sens)
                preferences.setSensitivity(sens)
            }
        }
    }

    private fun setupClickListeners() {
        calibrateBtn.setOnClickListener { openCalibrationWizard() }
        calibrateFab.setOnClickListener { openCalibrationWizard() }
        startStopBtn.setOnClickListener { if (isActive) stopAirMouse() else startAirMouse() }
        settingsBtn.setOnClickListener { showSettingsDialog() }
        debugToggleBtn.setOnClickListener { toggleDebugOverlay() }
        scanQrBtn.setOnClickListener { qrLauncher.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)) }
        connectBtn.setOnClickListener { if (isActive) disconnect() else connect() }
        btMouseBtn.setOnClickListener {
            if (BluetoothMouseService.instance == null) startBluetoothMouse()
            else {
                BtHidHelper.stopService(requireContext())
                btMouseBtn.text = getString(R.string.bt_mouse_start)
            }
        }
    }

    private fun connect() {
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
        preferences.setLastIp(ip)
        preferences.setLastPort(port)
        connectionStatusText.text = getString(R.string.connecting)
        try {
            ConnectionManager.connectTcp(ip, port)
            connectBtn.text = getString(R.string.disconnect)
            connectionQualityIcon.setImageResource(android.R.drawable.presence_away)
        } catch (e: Exception) {
            connectionStatusText.text = getString(R.string.connection_error, e.message)
            connectionQualityIcon.setImageResource(android.R.drawable.presence_invisible)
        }
    }

    private fun disconnect() {
        ConnectionManager.disconnectTcp()
        connectionStatusText.text = getString(R.string.status_not_connected)
        connectionQualityIcon.setImageResource(android.R.drawable.presence_invisible)
        connectBtn.text = getString(R.string.connect)
    }

    private fun startAirMouse() {
        if (!preferences.isCalibrated()) {
            Toast.makeText(requireContext(), getString(R.string.run_calibration_first), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), getString(R.string.wifi_required), Toast.LENGTH_SHORT).show()
            return
        }

        preferences.setLastIp(ip)
        preferences.setLastPort(port)
        smoothedMoveX = 0f
        smoothedMoveY = 0f
        lastMoveDispatchMs = 0L

        try {
            ConnectionManager.startDataSender(ip, port)
            attachSensorCallbacks()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.connection_error, e.message), Toast.LENGTH_LONG).show()
            return
        }

        sensorService.start()
        batterySaver.start(sensorService)
        isActive = true
        startStopBtn.text = getString(R.string.stop)
        calibrateBtn.isEnabled = false
        calibrateFab.hide()
        updateCalibrationStatusUi()
        ConnectionManager.connectTcp(ip, port)
        animateCardPulse(controlsCard)
    }

    private fun stopAirMouse() {
        stopAirMouseInternal()
        startStopBtn.text = getString(R.string.start)
        updateCalibrationStatusUi()
        calibrateBtn.isEnabled = true
        calibrateFab.show()
    }

    private fun stopAirMouseInternal() {
        isActive = false
        if (::sensorService.isInitialized) sensorService.stop()
        if (::batterySaver.isInitialized) batterySaver.stop()
        ConnectionManager.stopDataSender()
        ConnectionManager.disconnectTcp()
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
                    ConnectionManager.sendMove(sendX, sendY)
                    BluetoothMouseService.instance?.sendMouseReport(dx = sendX.toInt(), dy = sendY.toInt())
                }
            }

            val uiNow = System.currentTimeMillis()
            if (uiNow - lastUiUpdateMs >= UI_MIN_INTERVAL_MS) {
                lastUiUpdateMs = uiNow
                activity?.runOnUiThread {
                    updateOrientationIndicator(yaw)
                    debugOverlay.updateValues(roll, yaw, currentGyroY, currentAccelY)
                }
            }
            batterySaver.updateMovement(roll, yaw)
        }

        sensorService.setOnGestureDetected { gesture ->
            when (gesture) {
                GestureDetector.Gesture.CLICK -> {
                    ConnectionManager.sendClick()
                    BluetoothMouseService.instance?.sendMouseReport(buttons = 0x01)
                    if (preferences.isHapticEnabled()) vibrate(30)
                    preferences.incrementClick()
                }
                GestureDetector.Gesture.DOUBLE_CLICK -> {
                    ConnectionManager.sendDoubleClick()
                    BluetoothMouseService.instance?.sendMouseReport(buttons = 0x01)
                    if (preferences.isHapticEnabled()) vibrate(60)
                    preferences.incrementDoubleClick()
                }
                GestureDetector.Gesture.RIGHT_CLICK -> {
                    ConnectionManager.sendRightClick()
                    BluetoothMouseService.instance?.sendMouseReport(buttons = 0x02)
                    if (preferences.isHapticEnabled()) vibrate(80)
                    preferences.incrementRightClick()
                }
                GestureDetector.Gesture.SCROLL_UP, GestureDetector.Gesture.SCROLL_DOWN -> {
                    val delta = if (gesture == GestureDetector.Gesture.SCROLL_UP) -1 else 1
                    ConnectionManager.sendScroll(delta)
                    BluetoothMouseService.instance?.sendMouseReport(wheel = delta)
                    if (preferences.isHapticEnabled()) vibrate(20)
                    preferences.incrementScroll()
                }
                else -> {}
            }
        }

        sensorService.setOnGyroUpdate { currentGyroY = it }
        sensorService.setOnAccelUpdate { currentAccelY = it }
    }

    private fun openCalibrationWizard() {
        if (isActive) {
            Toast.makeText(requireContext(), getString(R.string.stop_before_calibration), Toast.LENGTH_SHORT).show()
            return
        }
        val attempts = preferences.getCalibrationAttempts()
        val remaining = MAX_CALIB_ATTEMPTS - attempts
        if (remaining <= 0) {
            Toast.makeText(requireContext(), getString(R.string.calibration_attempts_exhausted), Toast.LENGTH_LONG).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.start_calibration)
            .setMessage(getString(R.string.calibration_attempts_left, remaining))
            .setPositiveButton(R.string.yes) { _, _ ->
                preferences.incrementCalibrationAttempts()
                startActivity(Intent(requireContext(), CalibrationActivity::class.java))
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun updateCalibrationStatusUi() {
        val gyroCalibrated = preferences.getGyroBias().any { it != 0f }
        val accelCalibrated = preferences.getAccelScale().any { it != 1f }
        val magCalibrated = preferences.getMagScale().any { it != 1f }
        val calibratedCount = listOf(gyroCalibrated, accelCalibrated, magCalibrated).count { it }

        calibrationProgressBar.progress = (calibratedCount * 100) / 3
        calibrationProgressText.text = getString(R.string.calibrated_count, calibratedCount)

        val remaining = (MAX_CALIB_ATTEMPTS - preferences.getCalibrationAttempts()).coerceAtLeast(0)
        attemptsText.text = getString(R.string.attempts_remaining, remaining)
        attemptsText.setTextColor(
            if (remaining <= 1) ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            else ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        )

        if (remaining <= 0 && calibratedCount < 3) {
            calibrateBtn.isEnabled = false
            calibrateBtn.text = getString(R.string.calibration_locked)
            calibrateFab.hide()
            connectionStatusText.text = getString(R.string.calibration_exhausted)
        } else {
            calibrateBtn.isEnabled = !isActive
            calibrateBtn.text = getString(R.string.calibrate_sensors)
            calibrateFab.visibility = if (isActive) View.GONE else View.VISIBLE
            connectionStatusText.text = when {
                isActive -> getString(R.string.status_active)
                calibratedCount >= 3 -> getString(R.string.calib_ready_to_connect)
                else -> getString(R.string.calibration_needed_prompt)
            }
        }
    }

    private fun updateOrientationIndicator(yaw: Float) {
        orientationIndicator.animate()
            .rotation(MathUtils.radToDeg(yaw))
            .setDuration(50)
            .start()
    }

    private fun vibrate(durationMs: Long) {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    private fun showSettingsDialog() {
        SettingsDialog(requireContext(), preferences) { gestureDetector.reloadThresholds() }.show()
    }

    private fun toggleDebugOverlay() {
        if (Settings.canDrawOverlays(requireContext())) {
            debugOverlay.toggleVisibility()
            debugToggleBtn.text = if (debugOverlay.isVisible()) getString(R.string.hide_debug) else getString(R.string.debug)
        } else {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}")))
        }
    }

    private fun startBluetoothMouse() {
        val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BluetoothAdapter.getDefaultAdapter()
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }
        if (adapter == null) {
            Toast.makeText(requireContext(), getString(R.string.bluetooth_not_supported), Toast.LENGTH_SHORT).show()
            return
        }
        if (!adapter.isEnabled) {
            btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        checkBtPermissionAndStart()
    }

    private fun checkBtPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED -> {
                    BtHidHelper.startService(requireContext())
                    btMouseBtn.text = getString(R.string.bt_mouse_stop)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.bluetooth_permission_title)
                        .setMessage(R.string.bluetooth_permission_message)
                        .setPositiveButton(R.string.grant) { _, _ -> btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
                else -> btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            BtHidHelper.startService(requireContext())
            btMouseBtn.text = getString(R.string.bt_mouse_stop)
        }
    }

    private fun startWifiMonitoring() {
        wifiJob = lifecycleScope.launch {
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
            val qualityRes = when {
                rssi > -50 -> R.string.good
                rssi > -70 -> R.string.fair
                else -> R.string.poor
            }
            connectionStatusText.text = getString(R.string.signal_strength, getString(qualityRes), rssi)
            connectionQualityIcon.setImageResource(
                when {
                    rssi > -50 -> android.R.drawable.presence_online
                    rssi > -70 -> android.R.drawable.presence_away
                    else -> android.R.drawable.presence_busy
                }
            )
        } else {
            connectionStatusText.text = getString(R.string.status_not_connected)
            connectionQualityIcon.setImageResource(android.R.drawable.presence_invisible)
        }
    }

    private fun startSensorDataUpdates() {
        sensorUpdateJob = lifecycleScope.launch {
            while (true) {
                gyroValue.text = String.format(Locale.getDefault(), "Yaw: %.1f°", currentGyroY)
                accelValue.text = String.format(Locale.getDefault(), "Pitch: %.1f°", currentAccelY)
                updateGestureStats()
                delay(200)
            }
        }
    }

    private fun updateGestureStats() {
        clickCountText.text = getString(R.string.clicks, preferences.getClickCount())
        scrollCountText.text = getString(R.string.scrolls, preferences.getScrollCount())
        rightClickCountText.text = getString(R.string.right_clicks, preferences.getRightClickCount())
        doubleClickCountText.text = getString(R.string.double_clicks, preferences.getDoubleClickCount())
    }

    private fun checkSensorAvailability() {
        val hasCore = SensorUtils.hasGyroscope(requireContext()) && SensorUtils.hasAccelerometer(requireContext())
        if (!hasCore) {
            Toast.makeText(requireContext(), getString(R.string.sensor_warning_missing), Toast.LENGTH_LONG).show()
        }
    }

    private fun renderSavedLogs() {
        val saved = preferences.getServerLogs()
        liveLogText.text = if (saved.isNotEmpty()) saved.joinToString("\n") else getString(R.string.log_placeholder)
    }

    private fun animateCardPulse(card: CardView) {
        val anim = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.02f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = 1
        }
        val animY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.02f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = 1
        }
        anim.start()
        animY.start()
    }

    override fun onResume() {
        super.onResume()
        updateCalibrationStatusUi()
        if (isActive && !isTouchpadMode) {
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
        wifiJob?.cancel()
        sensorUpdateJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAirMouseInternal()
        debugOverlay.hide()
        wifiJob?.cancel()
        sensorUpdateJob?.cancel()
    }

    fun setServerAddress(ip: String, port: Int) {
        ConnectionManager.connectTcp(ip, port)
    }
}
