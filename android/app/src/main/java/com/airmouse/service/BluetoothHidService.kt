//// app/src/main/java/com/airmouse/service/BluetoothHidService.kt
//package com.airmouse.service
//
//import android.app.*
//import android.bluetooth.*
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.IBinder
//import androidx.core.app.NotificationCompat
//import androidx.core.content.ContextCompat
//import com.airmouse.R
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.*
//import timber.log.Timber
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class BluetoothHidService : Service() {
//
//    @Inject lateinit var bluetoothAdapter: BluetoothAdapter
//    private var hidDevice: BluetoothHidDevice? = null
//    private var isRegistered = false
//    private var isConnected = false
//    private var connectedDevice: BluetoothDevice? = null
//    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//
//    companion object {
//        private const val NOTIFICATION_ID = 1005
//        private const val CHANNEL_ID = "bluetooth_hid_channel"
//        private const val TAG = "BluetoothHidService"
//
//        fun start(context: Context) {
//            val intent = Intent(context, BluetoothHidService::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(intent)
//            } else {
//                context.startService(intent)
//            }
//        }
//
//        fun stop(context: Context) {
//            context.stopService(Intent(context, BluetoothHidService::class.java))
//        }
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        startForeground(NOTIFICATION_ID, createNotification("Initializing", "Setting up HID profile"))
//        initHid()
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                "Bluetooth HID Mouse",
//                NotificationManager.IMPORTANCE_LOW
//            ).apply {
//                description = "Bluetooth HID Mouse Service"
//            }
//            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
//        }
//    }
//
//    private fun createNotification(title: String, content: String): Notification {
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle(title)
//            .setContentText(content)
//            .setSmallIcon(R.drawable.ic_bluetooth)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//    }
//
//    @android.annotation.SuppressLint("MissingPermission")
//    private fun initHid() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
//            Timber.w("HID Device requires Android 10+")
//            return
//        }
//
//        // Check if Bluetooth is enabled
//        if (!bluetoothAdapter.isEnabled) {
//            Timber.w("Bluetooth is not enabled")
//            updateNotification("Bluetooth Off", "Please enable Bluetooth")
//            return
//        }
//
//        // Check if HID Device profile is supported
//        val mainExecutor = ContextCompat.getMainExecutor(this)
//
//        bluetoothAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
//            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
//                if (profile == BluetoothProfile.HID_DEVICE) {
//                    hidDevice = proxy as BluetoothHidDevice
//                    Timber.i("HID Device service connected")
//                    registerHidApp()
//                }
//            }
//
//            override fun onServiceDisconnected(profile: Int) {
//                if (profile == BluetoothProfile.HID_DEVICE) {
//                    hidDevice = null
//                    isRegistered = false
//                    Timber.i("HID Device service disconnected")
//                    updateNotification("Disconnected", "HID profile unavailable")
//                }
//            }
//        }, BluetoothProfile.HID_DEVICE)
//    }
//
//    @android.annotation.SuppressLint("MissingPermission")
//    private fun registerHidApp() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
//
//        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
//            "Air Mouse Pro",
//            "Air Mouse Pro HID",
//            "Air Mouse",
//            BluetoothHidDevice.SUBCLASS1_COMBO,
//            byteArrayOf()
//        )
//
//        val qosSettings = BluetoothHidDeviceAppQosSettings(100, 100, 0, 0, 0, 0)
//
//        hidDevice?.registerApp(sdpSettings, qosSettings, null, object : BluetoothHidDevice.Callback() {
//            override fun onAppStatusChanged(registered: Boolean, appId: Int) {
//                isRegistered = registered
//                if (registered) {
//                    Timber.i("HID App registered successfully")
//                    updateNotification("Ready", "HID profile registered")
//                } else {
//                    Timber.w("HID App registration failed")
//                    updateNotification("Error", "Failed to register HID profile")
//                }
//            }
//
//            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
//                super.onConnectionStateChanged(device, state)
//                when (state) {
//                    BluetoothProfile.STATE_CONNECTED -> {
//                        isConnected = true
//                        connectedDevice = device
//                        Timber.i("Device connected: ${device?.name}")
//                        updateNotification("Connected", "Mouse connected to ${device?.name}")
//                    }
//                    BluetoothProfile.STATE_DISCONNECTED -> {
//                        isConnected = false
//                        connectedDevice = null
//                        Timber.i("Device disconnected")
//                        updateNotification("Disconnected", "Waiting for connection")
//                    }
//                    BluetoothProfile.STATE_CONNECTING -> {
//                        Timber.d("Connecting to device: ${device?.name}")
//                    }
//                    BluetoothProfile.STATE_DISCONNECTING -> {
//                        Timber.d("Disconnecting from device: ${device?.name}")
//                    }
//                }
//            }
//        })
//    }
//
//    @android.annotation.SuppressLint("MissingPermission")
//    fun sendMouseReport(dx: Int, dy: Int, buttons: Byte) {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
//        if (!isRegistered || hidDevice == null) return
//        if (!isConnected || connectedDevice == null) return
//
//        val report = byteArrayOf(buttons, dx.toByte(), dy.toByte())
//        runCatching {
//            hidDevice?.sendReport(connectedDevice, 1, report)
//        }.onFailure {
//            Timber.e(it, "Failed to send report")
//        }
//    }
//
//    fun sendClick(button: Int) {
//        // Left click: 0x01, Right click: 0x02, Middle click: 0x04
//        sendMouseReport(0, 0, button.toByte())
//        serviceScope.launch {
//            delay(50)
//            sendMouseReport(0, 0, 0)
//        }
//        Timber.d("Click sent: $button")
//    }
//
//    fun sendMove(dx: Int, dy: Int) {
//        sendMouseReport(dx, dy, 0)
//    }
//
//    fun sendScroll(delta: Int) {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
//        if (!isRegistered || hidDevice == null || !isConnected) return
//
//        val scroll = when {
//            delta > 0 -> byteArrayOf(0x00, delta.toByte(), 0x00)
//            delta < 0 -> byteArrayOf(0x00, 0x00, (-delta).toByte())
//            else -> return
//        }
//        runCatching {
//            hidDevice?.sendReport(connectedDevice, 1, scroll)
//        }.onFailure {
//            Timber.e(it, "Failed to send scroll")
//        }
//    }
//
//    @android.annotation.SuppressLint("MissingPermission")
//    fun disconnect() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hidDevice != null) {
//            runCatching {
//                hidDevice?.unregisterApp()
//                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
//            }
//            isRegistered = false
//            isConnected = false
//            connectedDevice = null
//            Timber.i("Disconnected and unregistered")
//        }
//    }
//
//    private fun updateNotification(title: String, content: String) {
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(NOTIFICATION_ID, createNotification(title, content))
//    }
//
//    fun isConnected(): Boolean = isConnected
//    fun isRegistered(): Boolean = isRegistered
//    fun getConnectedDeviceName(): String? = connectedDevice?.name
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        disconnect()
//        serviceScope.cancel()
//        Timber.i("Service destroyed")
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//}