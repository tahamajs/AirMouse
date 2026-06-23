package com.airmouse.di

import android.content.Context
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager

class AppContainer(private val context: Context) {

    private val preferencesManager by lazy { PreferencesManager(context) }
    val connectionManager by lazy { ConnectionManager(context, preferencesManager) }

    fun cleanup() {
        connectionManager.cleanup()
    }
}
