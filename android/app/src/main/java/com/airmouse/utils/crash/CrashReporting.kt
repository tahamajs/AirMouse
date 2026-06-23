package com.airmouse.utils.crash

import android.content.Context
import timber.log.Timber

object CrashReporting {

    fun initialize(context: Context) {
        Timber.i("CrashReporting initialized successfully for %s", context.packageName)
    }

    fun logException(throwable: Throwable) {
        throwable.printStackTrace()
    }

    fun logMessage(message: String) {
        System.err.println("CRASH_LOG: $message")
    }
}
