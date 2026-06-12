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

class UsbHidService : Service() {

    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var controlEndpoint: UsbEndpoint? = null
    private var interruptEndpoint: UsbEndpoint? = null
    private var isConnected = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var moveJob: Job? = null
    private var pendingDx: Int = 0
    private var pendingDy: Int = 0

    // HID Report Descriptor for mouse (standard 3-button mouse)
    private val HID_REPORT_DESCRIPTOR = byteArrayOf(
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x02, // Usage (Mouse)
        0xA1, 0x01, // Collection (Application)
        0x09, 0x01, // Usage (Pointer)
        0xA1, 0x00, // Collection (Physical)
        0x05, 0x09, // Usage Page (Button)
        0x19, 0x01, // Usage Minimum (Button 1)
        0x29, 0x03, // Usage Maximum (Button 3)
        0x15, 0x00, // Logical Minimum (0)
        0x25, 0x01, // Logical Maximum (1)
        0x95, 0x03, // Report Count (3)
        0x75, 0x01, // Report Size (1)
        0x81, 0x02, // Input (Data, Var, Abs)
        0x95, 0x01, // Report Count (1)
        0x75, 0x05, // Report Size (5)
        0x81, 0x01, // Input (Const, Array, Abs)
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x30, // Usage (X)
        0x09, 0x31, // Usage (Y)
        0x15, 0x81, // Logical Minimum (-127)
        0x25, 0x7F, // Logical Maximum (127)
        0x75, 0x08, // Report Size (8)
        0x95, 0x02, // Report Count (2)
        0x81, 0x06, // Input (Data, Var, Rel)
        0xC0,       // End Collection
        0xC0        // End Collection
    )

    companion object {
        private const val NOTIFICATION_ID = 4001
        private const val CHANNEL_ID = "usb_hid_channel"
        private const val ACTION_USB_PERMISSION = "com.airmouse.USB_PERMISSION"
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
        }
        return START_STICKY
    }

    private fun connectUsb() {
        val permissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        val deviceList = usbManager.deviceList
        for (device in deviceList.values) {
            if (device.vendorId == 0x18D1 || device.productId == 0x4EE1) { // Google / Android accessory
                usbManager.requestPermission(device, permissionIntent)
                usbDevice = device
                break
            }
        }
        usbDevice?.let { device ->
            val interface_ = device.getInterface(0)
            for (i in 0 until interface_.endpointCount) {
                val endpoint = interface_.getEndpoint(i)
                when (endpoint.type) {
                    UsbConstants.USB_ENDPOINT_XFER_CONTROL -> controlEndpoint = endpoint
                    UsbConstants.USB_ENDPOINT_XFER_INT -> interruptEndpoint = endpoint
                }
            }
            usbConnection = usbManager.openDevice(device)
            usbConnection?.claimInterface(interface_, true)
            isConnected = true
            startSendingMovement()
        }
    }

    private fun startSendingMovement() {
        moveJob = serviceScope.launch {
            while (isConnected) {
                // Wait for movement data from the main app
                delay(16) // ~60 FPS
                sendMovement(pendingDx, pendingDy)
            }
        }
    }

    fun updateMovement(dx: Int, dy: Int) {
        pendingDx = dx
        pendingDy = dy
    }

    private fun sendMovement(dx: Int, dy: Int) {
        if (!isConnected || usbConnection == null || interruptEndpoint == null) return
        val report = byteArrayOf(
            0x00,           // Buttons (none)
            dx.toByte(),    // X movement
            dy.toByte()     // Y movement
        )
        usbConnection?.bulkTransfer(interruptEndpoint, report, report.size, 0)
    }

    private fun sendClick(button: Int) {
        // button: 1 = left, 2 = right, 3 = middle
        val report = byteArrayOf(
            button.toByte(), // Buttons
            0, 0             // No movement
        )
        usbConnection?.bulkTransfer(interruptEndpoint, report, report.size, 0)
        // Release after 50ms
        Handler(Looper.getMainLooper()).postDelayed({
            val releaseReport = byteArrayOf(0, 0, 0)
            usbConnection?.bulkTransfer(interruptEndpoint, releaseReport, releaseReport.size, 0)
        }, 50)
    }

    private fun disconnectUsb() {
        moveJob?.cancel()
        isConnected = false
        usbConnection?.close()
        usbConnection = null
        usbDevice = null
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("USB HID Mouse Active")
            .setContentText("Phone is acting as a USB mouse")
            .setSmallIcon(R.drawable.ic_usb)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "USB HID Service",
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
