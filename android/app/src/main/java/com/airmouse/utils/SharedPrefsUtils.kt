// app/src/main/java/com/airmouse/utils/SharedPrefsUtils.kt
package com.airmouse.utils

import android.content.SharedPreferences

inline fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.block()
    editor.apply()
}

fun SharedPreferences.getFloatOrNull(key: String): Float? {
    return if (contains(key)) getFloat(key, 0f) else null
}

fun SharedPreferences.getIntOrNull(key: String): Int? {
    return if (contains(key)) getInt(key, 0) else null
}

fun SharedPreferences.getLongOrNull(key: String): Long? {
    return if (contains(key)) getLong(key, 0L) else null
}

fun SharedPreferences.getStringOrNull(key: String): String? {
    return if (contains(key)) getString(key, null) else null
}

fun SharedPreferences.getBooleanOrNull(key: String): Boolean? {
    return if (contains(key)) getBoolean(key, false) else null
}// app/src/main/java/com/airmouse/utils/SharedPrefsUtils.kt
package com.airmouse.utils

import android.content.SharedPreferences

inline fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.block()
    editor.apply()
}

fun SharedPreferences.getFloatOrNull(key: String): Float? {
    return if (contains(key)) getFloat(key, 0f) else null
}

fun SharedPreferences.getIntOrNull(key: String): Int? {
    return if (contains(key)) getInt(key, 0) else null
}

fun SharedPreferences.getLongOrNull(key: String): Long? {
    return if (contains(key)) getLong(key, 0L) else null
}

fun SharedPreferences.getStringOrNull(key: String): String? {
    return if (contains(key)) getString(key, null) else null
}

fun SharedPreferences.getBooleanOrNull(key: String): Boolean? {
    return if (contains(key)) getBoolean(key, false) else null
}