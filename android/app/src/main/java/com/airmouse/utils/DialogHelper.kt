// app/src/main/java/com/airmouse/utils/DialogHelper.kt
package com.airmouse.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment

object DialogHelper {

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    fun showAlert(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "OK",
        negativeText: String? = null,
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ -> onPositive?.invoke() }

        if (negativeText != null) {
            builder.setNegativeButton(negativeText) { _, _ -> onNegative?.invoke() }
        }

        builder.show()
    }

    fun showConfirmation(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "Confirm",
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirmText) { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showLoading(activity: Activity, message: String = "Loading..."): AlertDialog {
        return AlertDialog.Builder(activity)
            .setMessage(message)
            .setCancelable(false)
            .create()
            .apply { show() }
    }
}