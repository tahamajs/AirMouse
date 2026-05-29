package com.airmouse.network

data class ConnectionStore(
    val ip: String,
    val port: Int,
    val deviceName: String = "AirMouse"
)