package com.airmouse.usb

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.airmouse.R
import kotlinx.coroutines.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UsbSerialService : Service() {

    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var bulkInEndpoint: UsbEndpoint? = null
    private var bulkOutEndpoint: UsbEndpoint? = null
    private var isConnected = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 4002
        private const val CHANNEL_ID = "usb_serial_channel"
    }

    override fun onCreate() {
        super.onCreate()
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "CONNECT_USB" -> connectUsb()
            "DISCONNECT_USB" -> disconnectUsb()
            "SEND_MOVE" -> {
                val dx = intent.getIntExtra("dx", 0)
                val dy = intent.getIntExtra("dy", 0)
                sendMovement(dx, dy)
            }
            "SEND_CLICK" -> {
                val button = intent.getStringExtra("button") ?: "left"
                sendClick(button)
            }
        }
        return START_STICKY
    }

    private fun connectUsb() {
        val deviceList = usbManager.deviceList
        for (device in deviceList.values) {
            if (device.vendorId == 0x0403 || device.productId == 0x6001) { // FTDI / CDC serial
                usbDevice = device
                break
            }
        }
        usbDevice?.let { device ->
            val interface_ = device.getInterface(0)
            for (i in 0 until interface_.endpointCount) {
                val endpoint = interface_.getEndpoint(i)
                when (endpoint.type) {
                    UsbConstants.USB_ENDPOINT_XFER_BULK -> {
                        if (endpoint.direction == UsbConstants.USB_DIR_IN) {
                            bulkInEndpoint = endpoint
                        } else {
                            bulkOutEndpoint = endpoint
                        }
                    }
                }
            }
            usbConnection = usbManager.openDevice(device)
            usbConnection?.claimInterface(interface_, true)
            isConnected = true
            startReading()
        }
    }

    private fun startReading() {
        readJob = serviceScope.launch {
            val buffer = ByteArray(64)
            while (isConnected) {
                val bytesRead = usbConnection?.bulkTransfer(bulkInEndpoint, buffer, buffer.size, 100)
                if (bytesRead != null && bytesRead > 0) {
                    val response = String(buffer, 0, bytesRead)
                    // Process server response if needed
                }
                delay(10)
            }
        }
    }

    private fun sendMovement(dx: Int, dy: Int) {
        if (!isConnected || usbConnection == null || bulkOutEndpoint == null) return
        val json = "{\"type\":\"move\",\"dx\":$dx,\"dy\":$dy}\n"
        usbConnection?.bulkTransfer(bulkOutEndpoint, json.toByteArray(), json.length, 0)
    }

    private fun sendClick(button: String) {
        val btn = when (button) {
            "left" -> "click"
            "right" -> "rightclick"
            else -> "click"
        }
        val json = "{\"type\":\"$btn\"}\n"
        usbConnection?.bulkTransfer(bulkOutEndpoint, json.toByteArray(), json.length, 0)
    }

    private fun disconnectUsb() {
        readJob?.cancel()
        isConnected = false
        usbConnection?.close()
        usbConnection = null
        usbDevice = null
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("USB Serial Active")
            .setContentText("Phone connected via USB virtual serial")
            .setSmallIcon(R.drawable.ic_usb)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "USB Serial Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        disconnectUsb()
        serviceScope.cancel()
    }
}