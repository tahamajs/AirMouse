package com.airmouse.utils

import java.util.regex.Pattern

object ValidationUtils {
    private val IP_ADDRESS_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )

    fun isValidIp(ip: String): Boolean = IP_ADDRESS_PATTERN.matcher(ip).matches()
}