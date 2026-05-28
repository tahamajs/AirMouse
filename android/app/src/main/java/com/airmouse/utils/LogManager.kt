package com.airmouse.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.*

object LogManager {
    private val _logEntries = MutableLiveData<List<LogEntry>>()
    val logEntries: LiveData<List<LogEntry>> = _logEntries
    private val entries = LinkedList<LogEntry>()
    private const val MAX_ENTRIES = 500
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun add(message: String, level: String = "info") {
        val entry = LogEntry(
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            message = message,
            level = level
        )
        synchronized(entries) {
            entries.addFirst(entry)
            if (entries.size > MAX_ENTRIES) entries.removeLast()
        }
        _logEntries.postValue(entries.toList())
    }

    fun clear() {
        entries.clear()
        _logEntries.postValue(emptyList())
    }

    data class LogEntry(
        val timestamp: String,
        val message: String,
        val level: String
    )
}