package com.airmouse.utils

import java.util.regex.Pattern
import java.net.URI

object ValidationUtils {
    private val IP_ADDRESS_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )

    data class Endpoint(val host: String, val port: Int)

    fun isValidIp(ip: String): Boolean = IP_ADDRESS_PATTERN.matcher(ip).matches()

    fun extractIpAddress(value: String): String? {
        val trimmed = value.trim()
        if (isValidIp(trimmed)) return trimmed

        val hostPart = trimmed
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore(":")

        return hostPart.takeIf { isValidIp(it) }
    }

    fun parseEndpoint(value: String, defaultPort: Int = 8080): Endpoint? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null

        val normalized = when {
            trimmed.startsWith("airmouse://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "airmouse://$trimmed"
        }

        return try {
            val uri = URI.create(normalized)
            val host = when {
                uri.host != null -> uri.host
                else -> trimmed.substringBefore(":").substringBefore("/").substringBefore("?")
            }
            if (!isValidIp(host)) return null
            val port = if (uri.port > 0) uri.port else {
                val portText = trimmed.substringAfter(":", "").substringBefore("/").substringBefore("?")
                portText.toIntOrNull() ?: defaultPort
            }
            Endpoint(host, port.coerceIn(1, 65535))
        } catch (_: Exception) {
            val host = trimmed.substringBefore(":").substringBefore("/").substringBefore("?")
            if (!isValidIp(host)) return null
            val portText = trimmed.substringAfter(":", "").substringBefore("/").substringBefore("?")
            val port = portText.toIntOrNull() ?: defaultPort
            Endpoint(host, port.coerceIn(1, 65535))
        }
    }
}
