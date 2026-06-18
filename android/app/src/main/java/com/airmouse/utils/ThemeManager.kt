// app/src/main/java/com/airmouse/utils/ThemeManager.kt
package com.airmouse.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore("theme_prefs")

class ThemeManager(private val context: Context) {

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
    }

    /**
     * Exposes the theme state as a clean observable cold stream flow
     * for seamless Jetpack Compose integrations.
     */
    val themeModeFlow: Flow<String> = context.themeDataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "system"
    }

    suspend fun setTheme(theme: String) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
        applyTheme(theme)
    }

    suspend fun getTheme(): String {
        return themeModeFlow.first()
    }

    fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark", "pure_black" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "high_contrast" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}