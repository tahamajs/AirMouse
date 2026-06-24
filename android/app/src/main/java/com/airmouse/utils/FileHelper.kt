
package com.airmouse.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object FileHelper {

    private const val TAG = "FileHelper"
    private const val EXPORT_DIR = "AirMouse"

    fun createExportFile(context: Context, fileName: String, extension: String = "json"): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fullName = "${fileName}_$timestamp.$extension"

            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                EXPORT_DIR
            )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            File(directory, fullName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create export file", e)
            null
        }
    }

    fun createExportFileInCache(context: Context, fileName: String, extension: String = "json"): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fullName = "${fileName}_$timestamp.$extension"
            File(context.cacheDir, fullName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create export file in cache", e)
            null
        }
    }

    fun writeToFile(file: File, data: String): Boolean {
        return try {
            file.writeText(data)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file", e)
            false
        }
    }

    fun readFromFile(file: File): String? {
        return try {
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file", e)
            null
        }
    }

    fun copyInputStreamToFile(inputStream: InputStream, file: File): Boolean {
        return try {
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy input stream to file", e)
            false
        }
    }

    fun getFileSize(file: File): Long {
        return file.length()
    }

    fun getFileSizeFormatted(file: File): String {
        val size = file.length()
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file", e)
            false
        }
    }

    fun deleteDirectory(directory: File): Boolean {
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        deleteDirectory(file)
                    } else {
                        file.delete()
                    }
                }
            }
        }
        return directory.delete()
    }
}
