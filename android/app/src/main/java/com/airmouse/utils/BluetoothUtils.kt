// app/src/main/java/com/airmouse/utils/BluetoothUtils.kt
package com.airmouse.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.airmouse.utils.LogManager

/**
 * Bluetooth utilities for device discovery and connection management.
 */
class BluetoothUtils(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothUtils"
        private const val SCAN_DURATION_MS = 10000L
    }

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private val handler = Handler(Looper.getMainLooper())
    private var scanCallback: ScanCallback? = null
    private var isScanning = false
    private var onDeviceFoundListener: ((BluetoothDevice, Int) -> Unit)? = null
    private var onScanCompleteListener: (() -> Unit)? = null

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun enableBluetooth(): Boolean {
        return bluetoothAdapter?.enable() == true
    }

    fun disableBluetooth(): Boolean {
        return bluetoothAdapter?.disable() == true
    }

    @Suppress("MissingPermission")
    fun getBondedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun getDeviceName(address: String): String? {
        return try {
            bluetoothAdapter?.getRemoteDevice(address)?.name
        } catch (e: Exception) {
            null
        }
    }

    fun isBluetoothLeSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun getBluetoothAddress(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasBluetoothPermission()) {
                @Suppress("DEPRECATION")
                bluetoothAdapter?.address
            } else null
        } else {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.address
        }
    }

    /**
     * Start scanning for Bluetooth devices
     */
    @Suppress("MissingPermission")
    fun startScanning(
        onDeviceFound: (BluetoothDevice, Int) -> Unit,
        onComplete: () -> Unit
    ) {
        if (!hasBluetoothPermission()) {
            LogManager.warn("Bluetooth permission not granted", TAG)
            onComplete()
            return
        }

        if (!isBluetoothEnabled()) {
            LogManager.warn("Bluetooth is not enabled", TAG)
            onComplete()
            return
        }

        if (isScanning) {
            stopScanning()
        }

        onDeviceFoundListener = onDeviceFound
        onScanCompleteListener = onComplete
        isScanning = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startLeScan()
        } else {
            startClassicScan()
        }

        // Stop scanning after duration
        handler.postDelayed({
            stopScanning()
        }, SCAN_DURATION_MS)
    }

    @Suppress("MissingPermission")
    private fun startLeScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            LogManager.warn("BLE scanner not available", TAG)
            stopScanning()
            return
        }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val rssi = result.rssi
                device.name?.let {
                    onDeviceFoundListener?.invoke(device, rssi)
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { result ->
                    result.device.name?.let {
                        onDeviceFoundListener?.invoke(result.device, result.rssi)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                LogManager.warn("BLE scan failed with error: $errorCode", TAG)
                stopScanning()
            }
        }

        scanner.startScan(scanCallback)
    }

    @Suppress("MissingPermission")
    private fun startClassicScan() {
        bluetoothAdapter?.startDiscovery()
    }

    @Suppress("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanCallback?.let {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
            }
        } else {
            bluetoothAdapter?.cancelDiscovery()
        }

        isScanning = false
        scanCallback = null
        onScanCompleteListener?.invoke()
        onScanCompleteListener = null
        onDeviceFoundListener = null
    }

    fun getDeviceMacAddress(device: BluetoothDevice): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires permission
            if (hasBluetoothPermission()) device.address else "Unknown"
        } else {
            device.address
        }
    }

    fun isScanning(): Boolean = isScanning

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getBluetoothStatus(): BluetoothStatus {
        return when {
            !isBluetoothEnabled() -> BluetoothStatus.DISABLED
            isScanning -> BluetoothStatus.SCANNING
            else -> BluetoothStatus.READY
        }
    }

    enum class BluetoothStatus {
        DISABLED, READY, SCANNING
    }
}