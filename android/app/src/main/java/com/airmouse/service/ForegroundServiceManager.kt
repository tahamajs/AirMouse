package com.airmouse.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class ForegroundServiceManager(private val context: Context) {
    private val runningServices = linkedSetOf<String>()

    fun markRunning(serviceName: String) {
        runningServices.add(serviceName)
    }

    fun markStopped(serviceName: String) {
        runningServices.remove(serviceName)
    }

    fun isRunning(serviceName: String): Boolean = serviceName in runningServices

    fun runningServiceNames(): Set<String> = runningServices.toSet()

    fun start(serviceIntent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stop(serviceIntent: Intent) {
        context.stopService(serviceIntent)
    }
}
