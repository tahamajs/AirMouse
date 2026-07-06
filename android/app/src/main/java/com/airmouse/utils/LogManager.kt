
package com.airmouse.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object LogManager {
    private val _logEntries = MutableLiveData<List<LogEntry>>()
    val logEntries: LiveData<List<LogEntry>> = _logEntries
    private val entries = ArrayList<LogEntry>()
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

    private val fileWriteExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    fun add(message: String, level: String = "INFO", tag: String = "AirMouse", throwable: Throwable? = null) {
        if (!isEnabled) return

        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val finalMessage = if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message

        val entry = LogEntry(
            timestampMs = System.currentTimeMillis(),
            timestamp = timestamp,
            message = finalMessage,
            level = level.uppercase(),
            tag = tag
        )

        val currentEntries = synchronized(entries) {
            entries.add(0, entry)
            if (entries.size > MAX_ENTRIES) {
                entries.removeAt(entries.size - 1)
            }
            ArrayList(entries)
        }
        _logEntries.postValue(currentEntries)

        
        when (level.uppercase()) {
            "ERROR" -> if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
            "WARN" -> if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
            "DEBUG" -> if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
            else -> if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
        }

        
        fileWriteExecutor.execute {
            writeToFile(entry)
        }
    }

    private fun writeToFile(entry: LogEntry) {
        try {
            logFile?.appendText("[${entry.timestamp}] [${entry.level}] [${entry.tag}] ${entry.message}\n")
        } catch (e: Exception) {
            
        }
    }

    fun debug(message: String, tag: String = "AirMouse") = add(message, "DEBUG", tag)
    fun info(message: String, tag: String = "AirMouse") = add(message, "INFO", tag)
    fun warn(message: String, tag: String = "AirMouse") = add(message, "WARN", tag)
    fun error(message: String, tag: String = "AirMouse") = add(message, "ERROR", tag)

    fun debug(message: String, throwable: Throwable?, tag: String = "AirMouse") = add(message, "DEBUG", tag, throwable)
    fun info(message: String, throwable: Throwable?, tag: String = "AirMouse") = add(message, "INFO", tag, throwable)
    fun warn(message: String, throwable: Throwable?, tag: String = "AirMouse") = add(message, "WARN", tag, throwable)
    fun error(message: String, throwable: Throwable?, tag: String = "AirMouse") = add(message, "ERROR", tag, throwable)

    fun clear() {
        synchronized(entries) {
            entries.clear()
            _logPostsValue(emptyList())
        }
    }

    fun getEntries(): List<LogEntry> = synchronized(entries) { ArrayList(entries) }

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
