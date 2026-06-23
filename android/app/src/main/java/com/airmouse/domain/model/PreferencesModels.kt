
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
) : Parcelable

@Parcelize
data class UserPreferences(
    val username: String = "",
    val serverName: String = "Air Mouse Pro",
    val serverIp: String = "",
    val serverPort: Int = 8080,
    val autoConnect: Boolean = true,
    val rememberCredentials: Boolean = true
) : Parcelable