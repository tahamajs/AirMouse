package com.airmouse.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QRScanner(private val fragment: Fragment) {

    private val requestPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScan()
        } else {
            val context = fragment.requireContext()
            android.app.AlertDialog.Builder(context)
                .setTitle("Camera permission needed")
                .setMessage("Please allow camera to scan QR codes.")
                .setPositiveButton("OK") { _, _ ->
                    fragment.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${context.packageName}")))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private val scanLauncher = fragment.registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            onScanResult?.invoke(result.contents)
        } else {
            onScanFailed?.invoke()
        }
    }

    var onScanResult: ((String) -> Unit)? = null
    var onScanFailed: (() -> Unit)? = null

    fun startScan() {
        val context = fragment.requireContext()
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            AlertDialog.Builder(context)
                .setTitle("Camera unavailable")
                .setMessage("This device does not have a camera available for QR scanning.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            onScanFailed?.invoke()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan the PC endpoint QR code")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        scanLauncher.launch(options)
    }
}
