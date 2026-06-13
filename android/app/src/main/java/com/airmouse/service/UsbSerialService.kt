package com.airmouse.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.network.WebSocketManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.IOException
import javax.inject.Inject

/**
 * USB Serial Service – Provides communication over USB serial (CDC ACM / FTDI / CP210x / PL2303).
 *
 * Features:
 * - Automatic detection of connected USB serial devices
 * - Support for multiple baud rates (default 115200)
 * - Bi-directional communication (read/write)
 * - JSON message parsing and forwarding to WebSocket
 * - Beautiful animated notifications with real-time status
 * - Auto-reconnection on device disconnect
 * - Comprehensive error handling
 *
 * Supported chipsets:
 * - FTDI (FT232R, FT230X, etc.)
 * - Silabs CP210x
 * - Prolific PL2303
 * - Arduino (CDC ACM)
 * - CH340/CH341
 */
@AndroidEntryPoint
class UsbSerialService : Service() {

    @Inject lateinit var usbManager: UsbManager

    private var serialDriver: UsbSerialDriver? = null
    private var isConnected = false
    private var isReconnecting = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 2000L

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Performance tracking
    private var bytesReceived = 0L
    private var messagesReceived = 0
    private var lastReceiveTime = 0L
    private var bytesPerSecond = 0

    // Current connection parameters
    private var currentBaudRate = BAUD_RATE
    private var currentDataBits = 8
    private var currentStopBits = 1
    private var currentParity = 0

    companion object {
        private const val NOTIFICATION_ID = 2005
        private const val CHANNEL_ID = "usb_serial"

        // Default baud rate
        private const val BAUD_RATE = 115200

        // Available baud rates
        val SUPPORTED_BAUD_RATES = listOf(300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600)

        fun start(context: Context) {
            val intent = Intent(context, UsbSerialService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsbSerialService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createAnimatedNotification("Initializing", "Setting up USB Serial connection..."))
        connectToSerialDevice()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "USB Serial Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "USB serial communication service"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createAnimatedNotification(title: String, content: String): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        // Add progress bar for connecting state
        if (title.contains("Connecting") || title.contains("Initializing")) {
            builder.setProgress(0, 0, true)
        }

        // Add animated status indicator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setColor(0xFF2196F3.toInt())
        }

        return builder.build()
    }

    private fun updateNotification(title: String, content: String, isConnected: Boolean = false) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(if (isConnected) android.R.drawable.presence_online else android.R.drawable.presence_offline)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (isConnected && messagesReceived > 0) {
            builder.setSubText("📨 ${messagesReceived} msgs • ${bytesPerSecond} B/s")
        } else if (isConnected) {
            builder.setSubText("Ready • ${currentBaudRate} baud")
        }

        startForeground(NOTIFICATION_ID, builder.build())
    }

    /**
     * Connect to the first available USB serial device.
     */
    private fun connectToSerialDevice() {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            updateNotification(
                "No Device Found",
                "Please connect a USB serial device (FTDI, CP210x, PL2303, Arduino)",
                false
            )
            android.util.Log.w("UsbSerialService", "No USB serial devices found")
            scheduleReconnect()
            return
        }

        val driver = availableDrivers[0]
        val deviceName = driver.device.productName ?: driver.device.deviceName ?: "Unknown device"
        android.util.Log.i("UsbSerialService", "Found device: $deviceName")

        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            updateNotification("Permission Required", "Please grant USB permission for $deviceName", false)
            android.util.Log.w("UsbSerialService", "USB permission not granted for device")
            return
        }

        serialDriver = driver
        try {
            serialDriver?.open()
            serialDriver?.setParameters(currentBaudRate, currentDataBits, currentStopBits, currentParity)
            isConnected = true
            reconnectAttempts = 0
            isReconnecting = false
            bytesReceived = 0
            messagesReceived = 0
            lastReceiveTime = System.currentTimeMillis()

            startReading()
            updateNotification(
                "Connected",
                "📱 $deviceName • ${currentBaudRate} baud • ${currentDataBits}N${currentStopBits}",
                true
            )
            android.util.Log.i("UsbSerialService", "USB serial device connected successfully")

            // Send connection notification via WebSocket
            WebSocketManager.send("""{"type":"serial","status":"connected","device":"$deviceName","baud":$currentBaudRate}""")

        } catch (e: IOException) {
            updateNotification("Connection Error", "Failed to open serial device: ${e.message}", false)
            android.util.Log.e("UsbSerialService", "Failed to open serial device: ${e.message}")
            serialDriver = null
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (isReconnecting) return
        if (reconnectAttempts >= maxReconnectAttempts) {
            updateNotification(
                "Connection Failed",
                "Max reconnection attempts reached. Please reconnect manually.",
                false
            )
            return
        }

        isReconnecting = true
        reconnectAttempts++

        updateNotification(
            "Reconnecting",
            "Attempt ${reconnectAttempts}/$maxReconnectAttempts in ${reconnectDelayMs / 1000}s...",
            false
        )

        mainHandler.postDelayed({
            isReconnecting = false
            connectToSerialDevice()
        }, reconnectDelayMs)
    }

    /**
     * Start reading data from the serial device.
     */
    private fun startReading() {
        serviceScope.launch {
            val buffer = ByteArray(4096)
            while (isConnected) {
                try {
                    val len = serialDriver?.read(buffer, 1000) ?: 0
                    if (len > 0) {
                        bytesReceived += len
                        messagesReceived++

                        // Update performance metrics
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastReceiveTime
                        if (elapsed >= 1000) {
                            bytesPerSecond = (bytesReceived * 1000 / elapsed).toInt()
                            bytesReceived = 0
                            lastReceiveTime = now
                        }

                        val data = String(buffer, 0, len)
                        processSerialData(data)

                        // Update notification periodically
                        if (messagesReceived % 10 == 0) {
                            updateNotification("Connected", "Receiving data (${messagesReceived} msgs)", true)
                        }
                    }
                } catch (e: IOException) {
                    android.util.Log.e("UsbSerialService", "Read error: ${e.message}")
                    if (isConnected) {
                        isConnected = false
                        scheduleReconnect()
                    }
                    break
                }
            }
        }
    }

    /**
     * Process incoming serial data.
     * Supports JSON messages and raw text.
     */
    private fun processSerialData(data: String) {
        val trimmed = data.trim()
        if (trimmed.isEmpty()) return

        try {
            // Try to parse as JSON
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                // Forward JSON messages to WebSocket
                WebSocketManager.send(trimmed)
                android.util.Log.d("UsbSerialService", "JSON message forwarded: $trimmed")
            } else {
                // For raw data, wrap in a serial event
                val json = """{"type":"serial_data","data":"${trimmed.replace("\"", "\\\"")}"}"""
                WebSocketManager.send(json)
                android.util.Log.d("UsbSerialService", "Raw data received: $trimmed")
            }
        } catch (e: Exception) {
            android.util.Log.e("UsbSerialService", "Failed to process serial data: ${e.message}")
            // Send as raw text
            WebSocketManager.send("""{"type":"serial_data","raw":"${trimmed.replace("\"", "\\\"")}"}""")
        }
    }

    /**
     * Send data to the serial device.
     * @param data String data to send
     * @return true if sent successfully, false otherwise
     */
    fun sendSerialData(data: String): Boolean {
        if (!isConnected || serialDriver == null) {
            android.util.Log.w("UsbSerialService", "Cannot send data: not connected")
            return false
        }

        return try {
            val bytes = data.toByteArray()
            val result = serialDriver?.write(bytes, 1000) ?: 0
            if (result > 0) {
                android.util.Log.d("UsbSerialService", "Sent ${result} bytes")
                updateNotification("Connected", "📤 Sent: ${data.take(30)}...", true)
                true
            } else {
                false
            }
        } catch (e: IOException) {
            android.util.Log.e("UsbSerialService", "Write error: ${e.message}")
            if (isConnected) {
                isConnected = false
                scheduleReconnect()
            }
            false
        }
    }

    /**
     * Send binary data to the serial device.
     * @param data ByteArray to send
     * @return true if sent successfully, false otherwise
     */
    fun sendBinaryData(data: ByteArray): Boolean {
        if (!isConnected || serialDriver == null) return false

        return try {
            val result = serialDriver?.write(data, 1000) ?: 0
            result > 0
        } catch (e: IOException) {
            android.util.Log.e("UsbSerialService", "Binary write error: ${e.message}")
            false
        }
    }

    /**
     * Change the baud rate dynamically.
     * @param baudRate New baud rate (must be one of SUPPORTED_BAUD_RATES)
     * @return true if changed successfully
     */
    fun setBaudRate(baudRate: Int): Boolean {
        if (baudRate !in SUPPORTED_BAUD_RATES) {
            android.util.Log.w("UsbSerialService", "Unsupported baud rate: $baudRate")
            return false
        }

        currentBaudRate = baudRate
        if (isConnected && serialDriver != null) {
            return try {
                serialDriver?.setParameters(currentBaudRate, currentDataBits, currentStopBits, currentParity)
                updateNotification("Connected", "Baud rate changed to $baudRate", true)
                android.util.Log.i("UsbSerialService", "Baud rate changed to $baudRate")
                true
            } catch (e: IOException) {
                android.util.Log.e("UsbSerialService", "Failed to change baud rate: ${e.message}")
                false
            }
        }
        return true
    }

    /**
     * Get the current baud rate.
     */
    fun getCurrentBaudRate(): Int = currentBaudRate

    /**
     * Check if the service is connected to a serial device.
     */
    fun isSerialConnected(): Boolean = isConnected

    /**
     * Get the number of messages received.
     */
    fun getMessagesReceived(): Int = messagesReceived

    /**
     * Get the current data rate (bytes per second).
     */
    fun getDataRate(): Int = bytesPerSecond

    /**
     * Get the connected device name.
     */
    fun getDeviceName(): String? = serialDriver?.device?.productName ?: serialDriver?.device?.deviceName

    /**
     * Get a list of all connected USB serial devices.
     */
    fun getAvailableDevices(): List<UsbSerialDriver> {
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
    }

    /**
     * Disconnect and release USB interface.
     */
    fun disconnect() {
        isConnected = false
        isReconnecting = false
        reconnectAttempts = 0

        try {
            serialDriver?.close()
        } catch (e: IOException) {
            android.util.Log.e("UsbSerialService", "Error during disconnect: ${e.message}")
        }

        serialDriver = null
        updateNotification("Disconnected", "USB Serial device disconnected", false)
        android.util.Log.i("UsbSerialService", "USB Serial device disconnected")

        // Send disconnection notification via WebSocket
        WebSocketManager.send("""{"type":"serial","status":"disconnected"}""")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        serviceScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        android.util.Log.i("UsbSerialService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}