package com.airmouse.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.*

class ThemeManager(private val context: Context) {
    private val dataStore = context.getDataStore("theme_prefs")
    private val THEME_KEY = preferencesKey<String>("theme_mode")

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[THEME_KEY] = theme }
        applyTheme(theme)
    }

    suspend fun getTheme(): String = dataStore.data.map { it[THEME_KEY] ?: "auto" }.first()

    fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}