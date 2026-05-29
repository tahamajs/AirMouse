package com.airmouse.bluetooth

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context                  // <-- ADD THIS IMPORT
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class BluetoothMouseService : Service() {

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isRegistered = false
    private var isSending = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private val handler = Handler(Looper.getMainLooper())

    // Standard HID mouse report descriptor (4 bytes: buttons, X, Y, wheel)
    private val mouseDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x02.toByte(),
        0xA1.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x01.toByte(),
        0xA1.toByte(), 0x00.toByte(),
        0x05.toByte(), 0x09.toByte(),
        0x19.toByte(), 0x01.toByte(),
        0x29.toByte(), 0x03.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x03.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x05.toByte(),
        0x81.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x30.toByte(),
        0x09.toByte(), 0x31.toByte(),
        0x09.toByte(), 0x38.toByte(),
        0x15.toByte(), 0x81.toByte(),
        0x25.toByte(), 0x7F.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x03.toByte(),
        0x81.toByte(), 0x06.toByte(),
        0xC0.toByte(),
        0xC0.toByte()
    )

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isRegistered = registered
            if (registered) {
                Log.d(TAG, "HID app registered, waiting for host...")
                reconnectAttempts = 0
            } else {
                Log.d(TAG, "HID app unregistered")
                tryReconnect()
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    isSending = true
                    Log.d(TAG, "Connected to ${device?.name ?: device?.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevice = null
                    isSending = false
                    Log.d(TAG, "Disconnected")
                    tryReconnect()
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {}
        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {}
        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {}
        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {}
    }

    private fun tryReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            handler.postDelayed({
                registerHidDevice()
            }, 3000L * reconnectAttempts)
        }
    }

    private fun registerHidDevice() {
        val device = hidDevice ?: return
        try {
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Air Mouse",
                "Phone as Bluetooth mouse",
                "Android",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                mouseDescriptor
            )
            device.registerApp(sdpSettings, null, null, Runnable::run, callback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission missing", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            stopSelf()
            return
        }

        // Get the HID device service – may return null if not supported
        val service = getSystemService("bluetooth_hid_device")
        if (service !is BluetoothHidDevice) {
            Log.e(TAG, "Bluetooth HID is not supported on this device")
            stopSelf()
            return
        }
        hidDevice = service
        registerHidDevice()
        instance = this
    }

    override fun onDestroy() {
        hidDevice?.unregisterApp()
        handler.removeCallbacksAndMessages(null)
        instance = null
        super.onDestroy()
    }

    fun sendMouseReport(buttons: Int = 0, dx: Int = 0, dy: Int = 0, wheel: Int = 0) {
        val device = hidDevice ?: return
        val target = connectedDevice ?: return
        if (!isSending) return
        val data = byteArrayOf(
            buttons.toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte()
        )
        device.sendReport(target, 0, data)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "BluetoothMouseService"
        @Volatile var instance: BluetoothMouseService? = null
            private set
    }
}