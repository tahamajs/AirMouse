package com.airmouse.service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.airmouse.R
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow

@AndroidEntryPoint
class ProximityService : Service() {

    @Inject lateinit var prefs: PreferencesManager
    @Inject lateinit var connectionManager: ConnectionManager

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isRunning = false
    private var isScanning = false
    private var targetDevice: BluetoothDevice? = null
    private var targetAddress = ""
    private var currentRssi = -100
    private val rssiHistory = mutableListOf<Int>()
    private val historySize = 10
    private var currentDistance = 3.0f
    private var isNear = false
    private var lastLockState = false
    private val calibrationSamples = mutableListOf<Pair<Int, Double>>()

    private val smoothingFactor = 0.7f
    private var nearThreshold = 1.5f
    private var farThreshold = 3.0f
    private var txPower = -59
    private var pathLossExponent = 2.5

    // State flows for UI observation
    private val _distance = MutableStateFlow(3.0f)
    val distance: StateFlow<Float> = _distance.asStateFlow()

    private val _rssi = MutableStateFlow(-100)
    val rssi: StateFlow<Int> = _rssi.asStateFlow()

    companion object {
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "proximity_service_channel"
        const val ACTION_START = "START_PROXIMITY"
        const val ACTION_STOP = "STOP_PROXIMITY"
        const val ACTION_CALIBRATE = "CALIBRATE"
        const val ACTION_SET_DEVICE = "SET_DEVICE"
        const val ACTION_UPDATE_THRESHOLDS = "UPDATE_THRESHOLDS"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_NEAR_THRESHOLD = "near_threshold"
        const val EXTRA_FAR_THRESHOLD = "far_threshold"
        const val BROADCAST_PROXIMITY_UPDATE = "com.airmouse.PROXIMITY_UPDATE"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_IS_NEAR = "is_near"
        const val EXTRA_RSSI = "rssi"
        const val EXTRA_IS_LOCKED = "is_locked"
        private const val TAG = "ProximityService"

        fun start(context: Context) {
            val intent = Intent(context, ProximityService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProximityService::class.java))
        }

        fun setDevice(context: Context, deviceAddress: String) {
            val intent = Intent(context, ProximityService::class.java).apply {
                action = ACTION_SET_DEVICE
                putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
            }
            context.startService(intent)
        }

        fun calibrate(context: Context) {
            val intent = Intent(context, ProximityService::class.java).apply {
                action = ACTION_CALIBRATE
            }
            context.startService(intent)
        }

        fun updateThresholds(context: Context, near: Float, far: Float) {
            val intent = Intent(context, ProximityService::class.java).apply {
                action = ACTION_UPDATE_THRESHOLDS
                putExtra(EXTRA_NEAR_THRESHOLD, near)
                putExtra(EXTRA_FAR_THRESHOLD, far)
            }
            context.startService(intent)
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                    if (device?.address == targetAddress) {
                        updateRssi(rssi)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (isScanning && isRunning) {
                        startScanning()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeBluetooth()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        loadSettings()
        registerReceivers()
        acquireWakeLock()
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Timber.w("Bluetooth not available or disabled")
        }
    }

    private fun loadSettings() {
        targetAddress = prefs.getString("proximity_device_mac", "")
        nearThreshold = prefs.getFloat("proximity_near_threshold", 1.5f)
        farThreshold = prefs.getFloat("proximity_far_threshold", 3.0f)
        txPower = prefs.getInt("proximity_tx_power", -59)
        pathLossExponent = prefs.getFloat("proximity_path_loss", 2.5f)

        if (targetAddress.isNotEmpty() && ::bluetoothAdapter.isInitialized) {
            try {
                targetDevice = bluetoothAdapter.getRemoteDevice(targetAddress)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get remote device")
            }
        }
    }

    private fun saveSettings() {
        prefs.putString("proximity_device_mac", targetAddress)
        prefs.putFloat("proximity_near_threshold", nearThreshold)
        prefs.putFloat("proximity_far_threshold", farThreshold)
        prefs.putInt("proximity_tx_power", txPower)
        prefs.putFloat("proximity_path_loss", pathLossExponent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proximity Lock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors distance to PC for auto lock/unlock"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String = "Proximity Lock", content: String = "Monitoring distance..."): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProximityService::WakeLock")
        wakeLock.acquire(10 * 60 * 1000L)
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun startMonitoring() {
        if (isRunning) return
        if (targetAddress.isEmpty()) {
            updateNotification("No Device", "Please select a device first")
            Timber.w("No target device configured")
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            updateNotification("Bluetooth Off", "Please enable Bluetooth")
            Timber.w("Bluetooth is disabled")
            return
        }

        isRunning = true
        startScanning()
        updateNotification("Active", "Monitoring distance to device")
        Timber.i("Proximity monitoring started for $targetAddress")
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun startScanning() {
        if (isScanning) return
        if (bluetoothAdapter == null) return

        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            isScanning = true
            bluetoothAdapter.startDiscovery()
            Timber.d("Bluetooth discovery started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start discovery")
            isScanning = false
        }
    }

    private fun updateRssi(rssi: Int) {
        currentRssi = rssi
        _rssi.value = rssi

        rssiHistory.add(rssi)
        while (rssiHistory.size > historySize) {
            rssiHistory.removeAt(0)
        }

        val smoothedRssi = getSmoothedRssi()

        // ✅ FIX: Convert to Double for calculation, then back to Float
        val distanceDouble = calculateDistance(smoothedRssi.toDouble())
        val distance = distanceDouble.toFloat()

        currentDistance = distance
        _distance.value = distance

        val previousNear = isNear
        checkProximityState(distance)

        broadcastUpdate(distance, isNear, rssi)

        updateNotification(
            if (isNear) "Near" else "Far",
            String.format(Locale.US, "Distance: %.1fm | Signal: %d dBm", distance, rssi)
        )

        sendProximityToServer(distance, isNear)

        if (previousNear != isNear) {
            Timber.i("Proximity changed: near=$isNear, distance=${distance}m")
        }
    }

    private fun getSmoothedRssi(): Int {
        if (rssiHistory.isEmpty()) return -100

        var smoothed = rssiHistory.first().toFloat()
        for (i in 1 until rssiHistory.size) {
            smoothed = smoothingFactor * rssiHistory[i] + (1 - smoothingFactor) * smoothed
        }
        return smoothed.toInt()
    }

    // ✅ FIX: Takes Double, returns Double
    private fun calculateDistance(rssi: Double): Double {
        val ratio = (txPower - rssi) / (10.0 * pathLossExponent)
        return 10.0.pow(ratio).coerceIn(0.3, 15.0)
    }

    // ✅ FIX: Takes Float for comparison
    private fun checkProximityState(distance: Float) {
        val wasNear = isNear
        isNear = if (wasNear) {
            distance < farThreshold
        } else {
            distance < nearThreshold
        }

        if (wasNear != isNear) {
            onProximityChanged(isNear, distance)
        }
    }

    private fun onProximityChanged(near: Boolean, distance: Float) {
        vibrate(if (near) 100 else 200)

        if (near) {
            unlockComputer()
        } else {
            lockComputer()
        }

        Timber.i("Proximity changed: near=$near, distance=${distance}m")
    }

    private fun lockComputer() {
        if (lastLockState) return

        connectionManager.sendLockScreen()
        sendBroadcast(Intent("com.airmouse.LOCK_SCREEN"))

        lastLockState = true

        updateNotification("Locked", "Device locked - you walked away")
        Timber.i("Lock command sent")
    }

    private fun unlockComputer() {
        if (!lastLockState) return

        connectionManager.sendUnlockScreen()

        lastLockState = false

        updateNotification("Unlocked", "Device unlocked - you returned")
        Timber.i("Unlock command sent")
    }

    private fun sendProximityToServer(distance: Float, isNear: Boolean) {
        connectionManager.sendProximity(isNear, distance)
    }

    private fun broadcastUpdate(distance: Float, isNear: Boolean, rssi: Int) {
        val intent = Intent(BROADCAST_PROXIMITY_UPDATE).apply {
            putExtra(EXTRA_DISTANCE, distance)
            putExtra(EXTRA_IS_NEAR, isNear)
            putExtra(EXTRA_RSSI, rssi)
            putExtra(EXTRA_IS_LOCKED, lastLockState)
        }
        sendBroadcast(intent)
    }

    private fun vibrate(duration: Long) {
        if (!prefs.getBoolean("proximity_vibration", true)) return

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun stopMonitoring() {
        isRunning = false
        isScanning = false
        rssiHistory.clear()

        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error stopping discovery")
        }

        updateNotification("Stopped", "Proximity monitoring stopped")
        Timber.i("Proximity monitoring stopped")
    }

    fun setTargetDevice(deviceAddress: String) {
        targetAddress = deviceAddress
        targetDevice = if (deviceAddress.isNotEmpty() && ::bluetoothAdapter.isInitialized) {
            try {
                bluetoothAdapter.getRemoteDevice(deviceAddress)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get remote device")
                null
            }
        } else null
        saveSettings()

        if (isRunning) {
            stopMonitoring()
            startMonitoring()
        }
    }

    fun calibrate() {
        serviceScope.launch {
            calibrationSamples.clear()
            updateNotification("Calibrating", "Move 1 meter away from device...")

            val distances = listOf(1f, 2f, 3f, 4f, 5f)
            for (i in distances.indices) {
                val distance = distances[i]
                updateNotification("Calibrating", "Step ${i + 1}/${distances.size}: Stand ${distance}m away")
                delay(5000)

                val avgRssi = getAverageRssi()
                if (avgRssi != -100) {
                    calibrationSamples.add(Pair(avgRssi, distance.toDouble()))
                    Timber.d("Calibration sample: distance=${distance}m, RSSI=$avgRssi")
                }
            }

            if (calibrationSamples.size >= 3) {
                calculateCalibrationParameters()
                saveSettings()
                updateNotification("Calibrated", "Calibration complete! Proximity detection improved.")
                vibrate(200)
                Timber.i("Calibration completed successfully")
            } else {
                updateNotification("Calibration Failed", "Insufficient data. Please try again.")
                Timber.w("Calibration failed: insufficient samples")
            }
        }
    }

    private suspend fun getAverageRssi(): Int {
        var samples = 0
        var sum = 0

        repeat(10) {
            delay(100)
            if (currentRssi != -100) {
                sum += currentRssi
                samples++
            }
        }

        return if (samples > 0) sum / samples else -100
    }

    private fun calculateCalibrationParameters() {
        val rssiValues = calibrationSamples.map { it.first.toDouble() }
        val distanceValues = calibrationSamples.map { it.second }

        val n = calibrationSamples.size
        val sumRssi = rssiValues.sum()
        val sumRssiLog = rssiValues.zip(distanceValues).sumOf { it.first * log10(it.second) }
        val sumLogDist = distanceValues.sumOf { log10(it) }
        val sumLogDistSq = distanceValues.sumOf { log10(it) * log10(it) }

        val numerator = n * sumRssiLog - sumRssi * sumLogDist
        val denominator = n * sumLogDistSq - sumLogDist * sumLogDist
        val pathLoss = if (denominator != 0.0) -10 * numerator / denominator else 2.5

        val avgRssiAt1m = calibrationSamples.filter { it.second == 1.0 }.map { it.first }.average()
        val calculatedTxPower = if (avgRssiAt1m > 0) avgRssiAt1m else -59.0

        pathLossExponent = pathLoss.coerceIn(1.5, 4.0).toFloat()
        txPower = calculatedTxPower.toInt()

        Timber.i("Calibration: TxPower=$txPower, PathLoss=$pathLossExponent")
    }

    fun updateThresholds(near: Float, far: Float) {
        nearThreshold = near.coerceIn(0.5f, 3.0f)
        farThreshold = far.coerceIn(1.0f, 5.0f)
        saveSettings()
        Timber.d("Thresholds updated: near=$nearThreshold, far=$farThreshold")
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }

    fun getCurrentDistance(): Float = currentDistance
    fun getCurrentRssi(): Int = currentRssi
    fun isNearDevice(): Boolean = isNear
    fun isLocked(): Boolean = lastLockState
    fun isMonitoring(): Boolean = isRunning
    fun getTargetDeviceName(): String = targetDevice?.name ?: "Unknown"
    fun getTargetDeviceAddress(): String = targetAddress

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
            ACTION_CALIBRATE -> calibrate()
            ACTION_SET_DEVICE -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: ""
                setTargetDevice(deviceAddress)
            }
            ACTION_UPDATE_THRESHOLDS -> {
                val near = intent.getFloatExtra(EXTRA_NEAR_THRESHOLD, nearThreshold)
                val far = intent.getFloatExtra(EXTRA_FAR_THRESHOLD, farThreshold)
                updateThresholds(near, far)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()

        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver already unregistered
        }

        Timber.i("Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}