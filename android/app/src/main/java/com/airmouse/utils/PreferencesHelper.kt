
package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {

    private const val PREFS_NAME = "airmouse_prefs"

    
    private const val KEY_AUTO_PAUSE_ENABLED = "auto_pause_enabled"
    private const val KEY_LAST_USED_IP = "last_used_ip"
    fun getAllKeys(): Set<String> {
        return prefs.all.keys
    }

    private const val KEY_LAST_USED_PORT = "last_used_port"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val KEY_APP_VERSION = "app_version"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    
    fun isAutoPauseEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_PAUSE_ENABLED, true)
    fun setAutoPauseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PAUSE_ENABLED, enabled).apply()
    }

    
    fun getLastUsedIp(): String = prefs.getString(KEY_LAST_USED_IP, "") ?: ""
    fun setLastUsedIp(ip: String) {
        prefs.edit().putString(KEY_LAST_USED_IP, ip).apply()
    }

    fun getLastUsedPort(): Int = prefs.getInt(KEY_LAST_USED_PORT, 8080)
    fun setLastUsedPort(port: Int) {
        prefs.edit().putInt(KEY_LAST_USED_PORT, port).apply()
    }

    
    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    
    fun getAppVersion(): Int = prefs.getInt(KEY_APP_VERSION, 0)
    fun setAppVersion(version: Int) {
        prefs.edit().putInt(KEY_APP_VERSION, version).apply()
    }

    
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    
    fun contains(key: String): Boolean = prefs.contains(key)

    
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    
    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}