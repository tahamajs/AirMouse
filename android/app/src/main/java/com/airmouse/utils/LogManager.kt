// app/src/main/java/com/airmouse/utils/LogManager.kt
package com.airmouse.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LogManager {
    private val _logEntries = MutableLiveData<List<LogEntry>>()
    val logEntries: LiveData<List<LogEntry>> = _logEntries
    private val entries = LinkedList<LogEntry>()
    private const val MAX_ENTRIES = 500
    private lateinit var appContext: Context
    private var isEnabled = true
    private var logFile: File? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        setupLogFile()
    }

    private fun setupLogFile() {
        val logDir = File(appContext.getExternalFilesDir(null), "logs")
        if (!logDir.exists()) logDir.mkdirs()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        logFile = File(logDir, "airmouse_${dateFormat.format(Date())}.log")
    }

    fun add(message: String, level: String = "INFO", tag: String = "AirMouse") {
        if (!isEnabled) return

        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val entry = LogEntry(
            timestampMs = System.currentTimeMillis(),
            timestamp = timestamp,
            message = message,
            level = level.uppercase(),
            tag = tag
        )

        synchronized(entries) {
            entries.addFirst(entry)
            if (entries.size > MAX_ENTRIES) entries.removeLast()
        }
        _logEntries.postValue(entries.toList())

        // Also write to Android log
        when (level.uppercase()) {
            "ERROR" -> Log.e(tag, message)
            "WARN" -> Log.w(tag, message)
            "DEBUG" -> Log.d(tag, message)
            else -> Log.i(tag, message)
        }

        // Write to file
        writeToFile(entry)
    }

    private fun writeToFile(entry: LogEntry) {
        try {
            logFile?.appendText("[${entry.timestamp}] [${entry.level}] [${entry.tag}] ${entry.message}\n")
        } catch (e: Exception) {
            // Ignore file write errors
        }
    }

    fun debug(message: String, tag: String = "AirMouse") = add(message, "DEBUG", tag)
    fun info(message: String, tag: String = "AirMouse") = add(message, "INFO", tag)
    fun warn(message: String, tag: String = "AirMouse") = add(message, "WARN", tag)
    fun error(message: String, tag: String = "AirMouse") = add(message, "ERROR", tag)

    fun clear() {
        entries.clear()
        _logPostsValue(emptyList())
    }

    fun getEntries(): List<LogEntry> = entries.toList()

    fun setEnabled(enabled: Boolean) { isEnabled = enabled }

    fun getLogFile(): File? = logFile

    data class LogEntry(
        val timestampMs: Long,
        val timestamp: String,
        val message: String,
        val level: String,
        val tag: String = "AirMouse"
    )

    private fun _logPostsValue(list: List<LogEntry>) {
        _logEntries.postValue(list)
    }
}
