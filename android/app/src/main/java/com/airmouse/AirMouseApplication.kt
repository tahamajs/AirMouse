package com.airmouse

import android.app.Application
import com.airmouse.utils.PreferencesHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AirMouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferencesHelper.init(this)
    }
}