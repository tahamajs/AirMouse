
package com.airmouse.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QRScanner(private val fragment: Fragment) {

    companion object {
        private const val TAG = "QRScanner"
        const val SCHEME_AIRMOUSE = "airmouse"
        const val HOST_CONNECT = "connect"

        fun parseConnectionDataStatic(qrData: String): ConnectionData? {
            val uri = Uri.parse(qrData)
            if (uri.scheme != SCHEME_AIRMOUSE || (uri.host != HOST_CONNECT && uri.host != "pair")) return null
            val ip = uri.getQueryParameter("ip") ?: return null
            val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 8081
            val name = uri.getQueryParameter("name") ?: "Air Mouse Server"
            val ws = uri.getQueryParameter("ws")
            val token = uri.getQueryParameter("token")
            val protocol = uri.getQueryParameter("protocol") ?: when {
                !ws.isNullOrBlank() -> "WEBSOCKET"
                uri.getQueryParameter("udp")?.equals("true", ignoreCase = true) == true -> "UDP"
                port == 8082 -> "UDP"
                else -> "TCP"
            }
            val useSSL = uri.getQueryParameter("ssl")?.equals("true", ignoreCase = true) == true
            return ConnectionData(ip, port, name, ws, token, protocol, useSSL)
        }
    }

    private val requestPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScan()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val scanLauncher = fragment.registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedData = result.contents
            LogManager.info("QR scan successful: $scannedData", TAG)
            onScanResult?.invoke(scannedData)
            onScanComplete?.invoke(scannedData, true)
        } else {
            LogManager.warn("QR scan cancelled or failed", TAG)
            onScanFailed?.invoke()
            onScanComplete?.invoke(null, false)
        }
    }

    var onScanResult: ((String) -> Unit)? = null
    var onScanFailed: (() -> Unit)? = null
    var onScanComplete: ((String?, Boolean) -> Unit)? = null
    var onPermissionDenied: (() -> Unit)? = null

    fun startScan() {
        val context = fragment.requireContext()

        
        if (!hasCameraHardware(context)) {
            showCameraUnavailableDialog()
            onScanFailed?.invoke()
            return
        }

        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Position the QR code within the frame")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
            setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity::class.java)
        }

        scanLauncher.launch(options)
    }

    private fun hasCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun showPermissionDeniedDialog() {
        val context = fragment.requireContext()
        AlertDialog.Builder(context)
            .setTitle("Camera Permission Required")
            .setMessage("Camera permission is needed to scan QR codes for connecting to your PC.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                fragment.startActivity(intent)
                onPermissionDenied?.invoke()
            }
            .setNegativeButton("Cancel") { _, _ ->
                onPermissionDenied?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    private fun showCameraUnavailableDialog() {
        val context = fragment.requireContext()
        AlertDialog.Builder(context)
            .setTitle("Camera Unavailable")
            .setMessage("This device does not have a camera. Please enter the server IP address manually.")
            .setPositiveButton("OK", null)
            .show()
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    data class ConnectionData(
        val ip: String,
        val port: Int,
        val name: String,
        val wsUrl: String? = null,
        val token: String? = null,
        val protocol: String = "WEBSOCKET",
        val useSSL: Boolean = false
    )
}
