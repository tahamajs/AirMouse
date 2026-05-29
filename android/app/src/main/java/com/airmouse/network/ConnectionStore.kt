package com.airmouse.network

/**
 * Lightweight data holder for connection information used by the app UI.
 * Renamed to ConnectionInfo to avoid colliding with the ConnectionStore interface
 * defined in the `network` Gradle module.
 */
data class ConnectionInfo(
    val ip: String,
    val port: Int,
    val deviceName: String = "AirMouse"
)