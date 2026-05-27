package com.airmouse.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper for requesting and checking runtime permissions.
 */
object PermissionHelper {

    private const val PERMISSION_REQUEST_CODE = 100
    private const val OVERLAY_REQUEST_CODE = 101

    /**
     * Checks if all required permissions are granted.
     * @param context Application context.
     * @return true if Internet and Vibrate permissions are granted.
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        val internetGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
        val vibrateGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
        return internetGranted && vibrateGranted
    }

    /**
     * Requests Internet and Vibrate permissions from an Activity.
     */
    fun requestPermissions(activity: Activity) {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.INTERNET)
        }
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.VIBRATE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.VIBRATE)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
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
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = android.net.Uri.parse("package:${activity.packageName}")
            }
            activity.startActivityForResult(intent, OVERLAY_REQUEST_CODE)
        }
    }
}