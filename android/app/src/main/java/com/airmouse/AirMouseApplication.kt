// AirMouseApplication.kt
package com.airmouse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AirMouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Any global initialisation
    }
}