// app/src/main/java/com/airmouse/utils/PermissionHelper.kt
package com.airmouse.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Helper for requesting and checking runtime permissions.
 */
object PermissionHelper {

    private const val PERMISSION_REQUEST_CODE = 100
    private const val OVERLAY_REQUEST_CODE = 101
    private const val BLUETOOTH_REQUEST_CODE = 102
    private const val LOCATION_REQUEST_CODE = 103

    // Required permissions for the app
    val requiredPermissions = listOf(
        Manifest.permission.INTERNET,
        Manifest.permission.VIBRATE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    // Bluetooth permissions (Android 12+)
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    // Location permission (required for Bluetooth scanning on older Android)
    val locationPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Checks if all required permissions are granted.
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if Bluetooth permissions are granted.
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if location permissions are granted (required for Bluetooth on older Android).
     */
    fun hasLocationPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ doesn't need location for Bluetooth
            return true
        }
        return locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests all required permissions from an Activity.
     */
    fun requestPermissions(activity: Activity, requestCode: Int = PERMISSION_REQUEST_CODE) {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, requestCode)
        }
    }

    /**
     * Requests permissions from a Fragment.
     */
    fun requestPermissions(fragment: Fragment, requestCode: Int = PERMISSION_REQUEST_CODE) {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(fragment.requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            fragment.requestPermissions(permissionsToRequest, requestCode)
        }
    }

    /**
     * Requests Bluetooth permissions.
     */
    fun requestBluetoothPermissions(activity: Activity, requestCode: Int = BLUETOOTH_REQUEST_CODE) {
        val permissionsToRequest = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, requestCode)
        }
    }

    /**
     * Requests location permissions (for Bluetooth on older Android).
     */
    fun requestLocationPermissions(activity: Activity, requestCode: Int = LOCATION_REQUEST_CODE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return

        val permissionsToRequest = locationPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, requestCode)
        }
    }

    /**
     * Checks if overlay permission (for debug overlay) is granted.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    /**
     * Requests overlay permission from an Activity.
     */
    fun requestOverlayPermission(activity: Activity, requestCode: Int = OVERLAY_REQUEST_CODE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }

    /**
     * Show permission denied dialog with option to go to settings.
     */
    fun showPermissionDeniedDialog(
        context: Context,
        title: String = "Permission Required",
        message: String = "This permission is required for the app to function properly.",
        onConfirm: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                onConfirm?.invoke()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Check if we should show permission rationale.
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return requiredPermissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    /**
     * Get denied permissions list.
     */
    fun getDeniedPermissions(activity: Activity): List<String> {
        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
    }
}