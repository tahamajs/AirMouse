package com.airmouse.utils

import java.text.SimpleDateFormat
import java.util.*

object LogManager {
    private val listeners = mutableListOf<LogListener>()
    private var persistenceCallback: ((String) -> Unit)? = null

    fun setPersistenceCallback(callback: (String) -> Unit) {
        persistenceCallback = callback
    }

    fun add(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val entry = "$timestamp - $message"
        listeners.forEach { it.onNewLog(entry) }
        persistenceCallback?.invoke(entry)
    }

    fun addListener(listener: LogListener) { listeners.add(listener) }
    fun removeListener(listener: LogListener) { listeners.remove(listener) }

    interface LogListener {
        fun onNewLog(message: String)
    }
}