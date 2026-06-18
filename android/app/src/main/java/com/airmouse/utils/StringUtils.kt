// app/src/main/java/com/airmouse/utils/StringUtils.kt
package com.airmouse.utils

import java.util.regex.Pattern

object StringUtils {

    private val ipPattern = Pattern.compile(
        "^(([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.){3}([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$"
    )

    fun isValidIp(ip: String): Boolean {
        return ipPattern.matcher(ip).matches()
    }

    fun isValidPort(port: Int): Boolean {
        return port in 1..65535
    }

    fun truncate(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, (maxLength - 3).coerceAtLeast(0)) + "..."
        } else {
            text
        }
    }

    fun capitalizeFirst(str: String): String {
        if (str.isEmpty()) return str
        return str.substring(0, 1).uppercase() + str.substring(1).lowercase()
    }

    fun toCamelCase(str: String): String {
        val words = str.split("_", " ", "-")
        // Fixed: Call capitalizeFirst by passing the string as an argument
        return words.joinToString("") { capitalizeFirst(it) }
    }

    fun toSnakeCase(str: String): String {
        // Fixed: Corrected the regex pattern that was split by the error text
        return str.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }
}