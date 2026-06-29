package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Application-wide preferences and settings.
 */
@Parcelize
data class AppPreferences(
    val theme: String = "dark",
    val language: String = "en",
    val autoStart: Boolean = false,
    val showTrayIcon: Boolean = true,
    val soundEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true
) : Parcelable {

    /**
     * Check if the theme is dark.
     */
    fun isDarkTheme(): Boolean {
        return theme == "dark" || theme == "pure_black"
    }

    /**
     * Check if the theme is light.
     */
    fun isLightTheme(): Boolean {
        return theme == "light"
    }

    /**
     * Check if the theme follows the system.
     */
    fun isSystemTheme(): Boolean {
        return theme == "system"
    }

    /**
     * Get the theme name for display.
     */
    fun getThemeDisplayName(): String {
        return when (theme) {
            "dark" -> "Dark"
            "light" -> "Light"
            "pure_black" -> "Pure Black"
            "high_contrast" -> "High Contrast"
            "ocean" -> "Ocean"
            "sunset" -> "Sunset"
            "forest" -> "Forest"
            "purple" -> "Purple"
            "system" -> "System Default"
            else -> theme.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
    }

    /**
     * Get the language display name.
     */
    fun getLanguageDisplayName(): String {
        return when (language) {
            "en" -> "English"
            "fa" -> "فارسی"
            "es" -> "Español"
            "fr" -> "Français"
            "de" -> "Deutsch"
            "zh" -> "中文"
            else -> language
        }
    }

    /**
     * Create a copy with a different theme.
     */
    fun withTheme(theme: String): AppPreferences {
        return copy(theme = theme)
    }

    /**
     * Create a copy with a different language.
     */
    fun withLanguage(language: String): AppPreferences {
        return copy(language = language)
    }

    /**
     * Create a copy toggling analytics.
     */
    fun withAnalytics(enabled: Boolean): AppPreferences {
        return copy(analyticsEnabled = enabled)
    }

    /**
     * Create a copy toggling crash reporting.
     */
    fun withCrashReporting(enabled: Boolean): AppPreferences {
        return copy(crashReportingEnabled = enabled)
    }

    companion object {
        /**
         * Default preferences.
         */
        fun default(): AppPreferences {
            return AppPreferences()
        }

        /**
         * Privacy-focused preferences.
         */
        fun privacyFocused(): AppPreferences {
            return AppPreferences(
                analyticsEnabled = false,
                crashReportingEnabled = false,
                soundEnabled = false,
                notificationsEnabled = false
            )
        }

        /**
         * Battery-friendly preferences.
         */
        fun batterySaver(): AppPreferences {
            return AppPreferences(
                autoStart = false,
                showTrayIcon = false,
                soundEnabled = false,
                notificationsEnabled = false
            )
        }
    }
}

/**
 * User-specific preferences for server connection.
 */
@Parcelize
data class UserPreferences(
    val username: String = "",
    val serverName: String = "Air Mouse Pro",
    val serverIp: String = "",
    val serverPort: Int = 8080,
    val autoConnect: Boolean = true,
    val rememberCredentials: Boolean = true
) : Parcelable {

    /**
     * Check if the server configuration is valid.
     */
    fun isValid(): Boolean {
        return serverIp.isNotBlank() && serverPort in 1..65535
    }

    /**
     * Check if the user has entered credentials.
     */
    fun hasCredentials(): Boolean {
        return username.isNotBlank() && serverIp.isNotBlank()
    }

    /**
     * Get the server URL.
     */
    fun getServerUrl(): String {
        return "http://$serverIp:$serverPort"
    }

    /**
     * Get the WebSocket URL.
     */
    fun getWebSocketUrl(): String {
        return "ws://$serverIp:$serverPort/ws"
    }

    /**
     * Create a copy with updated server IP.
     */
    fun withServerIp(ip: String): UserPreferences {
        return copy(serverIp = ip)
    }

    /**
     * Create a copy with updated server port.
     */
    fun withServerPort(port: Int): UserPreferences {
        return copy(serverPort = port.coerceIn(1, 65535))
    }

    /**
     * Create a copy toggling auto-connect.
     */
    fun withAutoConnect(enabled: Boolean): UserPreferences {
        return copy(autoConnect = enabled)
    }

    /**
     * Create a copy with updated username.
     */
    fun withUsername(username: String): UserPreferences {
        return copy(username = username)
    }

    companion object {
        /**
         * Default user preferences.
         */
        fun default(): UserPreferences {
            return UserPreferences()
        }

        /**
         * Create preferences for a specific server.
         */
        fun forServer(ip: String, port: Int = 8080, name: String = "Air Mouse Pro"): UserPreferences {
            return UserPreferences(
                serverIp = ip,
                serverPort = port,
                serverName = name,
                autoConnect = true
            )
        }
    }
}