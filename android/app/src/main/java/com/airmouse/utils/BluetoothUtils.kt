
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
import androidx.core.content.ContextCompat

class BluetoothUtils(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothUtils"
        private const val SCAN_DURATION_MS = 10000L
    }

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
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

    @Suppress("DEPRECATION")
    fun enableBluetooth(): Boolean {
        
        if (!hasBluetoothPermission()) return false
        return try {
            bluetoothAdapter?.enable() == true
        } catch (e: SecurityException) {
            LogManager.warn("SecurityException: Failed to programmatically enable bluetooth: ${e.message}", TAG)
            false
        }
    }

    @Suppress("DEPRECATION")
    fun disableBluetooth(): Boolean {
        if (!hasBluetoothPermission()) return false
        return try {
            bluetoothAdapter?.disable() == true
        } catch (e: SecurityException) {
            LogManager.warn("SecurityException: Failed to programmatically disable bluetooth: ${e.message}", TAG)
            false
        }
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun getDeviceName(device: BluetoothDevice): String? {
        if (!hasBluetoothPermission()) return null
        return try {
            device.name
        } catch (e: SecurityException) {
            null
        }
    }

    fun isBluetoothLeSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    @Suppress("DEPRECATION")
    fun getBluetoothAddress(): String? {
        if (!hasBluetoothPermission()) return null
        return try {
            bluetoothAdapter?.address
        } catch (e: SecurityException) {
            null
        }
    }

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

        
        startLeScan()

        handler.postDelayed({
            stopScanning()
        }, SCAN_DURATION_MS)
    }

    private fun startLeScan() {
        val scanner = try {
            bluetoothAdapter?.bluetoothLeScanner
        } catch (e: SecurityException) {
            null
        }

        if (scanner == null) {
            LogManager.warn("BLE scanner not available or restricted", TAG)
            stopScanning()
            return
        }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val rssi = result.rssi
                val name = try { device.name } catch (e: SecurityException) { null }
                name?.let {
                    onDeviceFoundListener?.invoke(device, rssi)
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { result ->
                    val name = try { result.device.name } catch (e: SecurityException) { null }
                    name?.let {
                        onDeviceFoundListener?.invoke(result.device, result.rssi)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                LogManager.warn("BLE scan failed with error: $errorCode", TAG)
                stopScanning()
            }
        }

        try {
            scanner.startScan(scanCallback)
        } catch (e: SecurityException) {
            LogManager.warn("Failed to initiate BLE scanning: ${e.message}", TAG)
            stopScanning()
        }
    }

    fun stopScanning() {
        if (!isScanning) return

        try {
            scanCallback?.let {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
            }
        } catch (e: SecurityException) {
            LogManager.warn("Security restriction encountered while terminating scanning infrastructure: ${e.message}", TAG)
        }

        isScanning = false
        scanCallback = null
        onScanCompleteListener?.invoke()
        onScanCompleteListener = null
        onDeviceFoundListener = null
    }

    fun getDeviceMacAddress(device: BluetoothDevice): String {
        if (!hasBluetoothPermission()) return "Unknown"
        return device.address
    }

    fun isScanning(): Boolean = isScanning

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
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