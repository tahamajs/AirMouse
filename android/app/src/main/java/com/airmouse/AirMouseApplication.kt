package com.airmouse

import android.app.Application
import com.airmouse.utils.LogManager

class AirMouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LogManager.init(this)
    }
}