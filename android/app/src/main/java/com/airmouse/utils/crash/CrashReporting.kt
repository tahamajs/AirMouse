package com.airmouse.utils.crash

import android.content.Context
import timber.log.Timber

object CrashReporting {

    fun initialize(context: Context) {
        // Initialize your production crash handler tracking hub here (e.g., Firebase Crashlytics, Sentry)
        Timber.i("CrashReporting stub initialized initialized successfully.")
    }

    fun logException(throwable: Throwable) {
        // Send a non-fatal exception log event to your analytics platform
        throwable.printStackTrace()
    }

    fun logMessage(message: String) {
        // Log custom analytical trace milestones
        System.err.println("CRASH_LOG: $message")
    }
}