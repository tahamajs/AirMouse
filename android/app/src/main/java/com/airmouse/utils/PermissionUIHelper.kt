
package com.airmouse.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

object PermissionUIHelper {

    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    fun shouldShowPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun requestOverlayPermission(activity: Activity, requestCode: Int) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivityForResult(intent, requestCode)
    }
}