package com.airmouse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.airmouse.utils.PreferencesHelper

@HiltAndroidApp
class AirMouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferencesHelper.init(this)
    }
}