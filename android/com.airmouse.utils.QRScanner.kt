package com.airmouse.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QRScanner(private val activity: AppCompatActivity) {

    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScan()
        } else {
            // Show rationale dialog
            android.app.AlertDialog.Builder(activity)
                .setTitle("Camera permission needed")
                .setMessage("Please allow camera to scan QR codes.")
                .setPositiveButton("OK") { _, _ ->
                    activity.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${activity.packageName}")))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private val scanLauncher = activity.registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            onScanResult?.invoke(result.contents)
        } else {
            onScanFailed?.invoke()
        }
    }

    var onScanResult: ((String) -> Unit)? = null
    var onScanFailed: (() -> Unit)? = null

    fun startScan() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan server QR code")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        scanLauncher.launch(options)
    }
}