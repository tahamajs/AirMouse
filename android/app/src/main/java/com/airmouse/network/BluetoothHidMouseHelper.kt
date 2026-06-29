package com.airmouse.network

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.airmouse.utils.LogManager
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothHidMouseHelper @Inject constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        private const val TAG = "BluetoothHidMouse"
        private const val HID_PROFILE = 19 // BluetoothProfile.HID_DEVICE

        private val HID_DESCRIPTOR = byteArrayOf(
            0x05.toByte(), 0x01.toByte(),         // USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x02.toByte(),         // USAGE (Mouse)
            0xa1.toByte(), 0x01.toByte(),         // COLLECTION (Application)
            0x09.toByte(), 0x01.toByte(),         //   USAGE (Pointer)
            0xa1.toByte(), 0x00.toByte(),         //   COLLECTION (Physical)
            0x05.toByte(), 0x09.toByte(),         //     USAGE_PAGE (Button)
            0x19.toByte(), 0x01.toByte(),         //     USAGE_MINIMUM (Button 1)
            0x29.toByte(), 0x03.toByte(),         //     USAGE_MAXIMUM (Button 3)
            0x15.toByte(), 0x00.toByte(),         //     LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(),         //     LOGICAL_MAXIMUM (1)
            0x95.toByte(), 0x03.toByte(),         //     REPORT_COUNT (3)
            0x75.toByte(), 0x01.toByte(),         //     REPORT_SIZE (1)
            0x81.toByte(), 0x02.toByte(),         //     INPUT (Data,Var,Abs)
            0x95.toByte(), 0x01.toByte(),         //     REPORT_COUNT (1)
            0x75.toByte(), 0x05.toByte(),         //     REPORT_SIZE (5)
            0x81.toByte(), 0x03.toByte(),         //     INPUT (Cnst,Var,Abs)
            0x05.toByte(), 0x01.toByte(),         //     USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x30.toByte(),         //     USAGE (X)
            0x09.toByte(), 0x31.toByte(),         //     USAGE (Y)
            0x15.toByte(), 0x81.toByte(),         //     LOGICAL_MINIMUM (-127)
            0x25.toByte(), 0x7f.toByte(),         //     LOGICAL_MAXIMUM (127)
            0x75.toByte(), 0x08.toByte(),         //     REPORT_SIZE (8)
            0x95.toByte(), 0x02.toByte(),         //     REPORT_COUNT (2)
            0x81.toByte(), 0x06.toByte(),         //     INPUT (Data,Var,Rel)
            0x09.toByte(), 0x38.toByte(),         //     USAGE (Wheel)
            0x15.toByte(), 0x81.toByte(),         //     LOGICAL_MINIMUM (-127)
            0x25.toByte(), 0x7f.toByte(),         //     LOGICAL_MAXIMUM (127)
            0x75.toByte(), 0x08.toByte(),         //     REPORT_SIZE (8)
            0x95.toByte(), 0x01.toByte(),         //     REPORT_COUNT (1)
            0x81.toByte(), 0x06.toByte(),         //     INPUT (Data,Var,Rel)
            0xc0.toByte(),                        //   END_COLLECTION
            0xc0.toByte()                         // END_COLLECTION
        )
    }

    private var hidDeviceService: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isAppRegistered = false

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("NewApi")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == HID_PROFILE) {
                hidDeviceService = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == HID_PROFILE) {
                hidDeviceService = null
                isAppRegistered = false
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            isAppRegistered = registered
            Log.d(TAG, "HID app status: registered=$registered")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            super.onConnectionStateChanged(device, state)
            Log.d(TAG, "HID device connection state changed: $state")
            connectedDevice = if (state == BluetoothProfile.STATE_CONNECTED) {
                device
            } else {
                null
            }
        }
    }

    init {
        setup()
    }

    private fun setup() {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth not supported")
            return
        }
        try {
            bluetoothAdapter.getProfileProxy(context, profileListener, HID_PROFILE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get HID profile proxy", e)
        }
    }

    @SuppressLint("NewApi")
    private fun registerApp() {
        val service = hidDeviceService ?: return
        if (isAppRegistered) return

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "AirMouse",
            "Air Mouse Bluetooth HID",
            "Google DeepMind CA2",
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            HID_DESCRIPTOR
        )

        try {
            val registered = service.registerApp(
                sdpSettings,
                null,
                null,
                Executors.newSingleThreadExecutor(),
                callback
            )
            LogManager.info("HID App registered: $registered", TAG)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering HID App", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception registering HID App", e)
        }
    }

    @SuppressLint("NewApi")
    fun sendMouseReport(buttons: Byte, dx: Byte, dy: Byte, scroll: Byte): Boolean {
        val service = hidDeviceService ?: return false
        val device = connectedDevice ?: return false

        val report = byteArrayOf(buttons, dx, dy, scroll)
        return try {
            service.sendReport(device, 0, report)
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("NewApi")
    fun connect(device: BluetoothDevice): Boolean {
        val service = hidDeviceService ?: return false
        return try {
            service.connect(device)
        } catch (e: SecurityException) {
            false
        }
    }

    @SuppressLint("NewApi")
    fun disconnect(): Boolean {
        val service = hidDeviceService ?: return false
        val device = connectedDevice ?: return false
        return try {
            service.disconnect(device)
        } catch (e: SecurityException) {
            false
        }
    }

    fun isConnected(): Boolean = connectedDevice != null
}
