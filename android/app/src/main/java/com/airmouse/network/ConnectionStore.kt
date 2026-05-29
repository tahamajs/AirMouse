package com.airmouse.network

/**
 * Data class for connection details. 
 * Renamed from ConnectionStore to avoid clash with the interface in :network.
 */
data class ConnectionInfo(
    val ip: String,
    val port: Int,
    val deviceName: String = "AirMouse"
)
