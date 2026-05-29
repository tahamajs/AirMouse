// app/src/main/java/com/airmouse/utils/PreferencesHelper.kt
package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {
    private const val PREFS_NAME = "airmouse_prefs"
    private const val KEY_AUTO_PAUSE_ENABLED = "auto_pause_enabled"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoPauseEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_PAUSE_ENABLED, false)

    fun setAutoPauseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PAUSE_ENABLED, enabled).apply()
    }
}