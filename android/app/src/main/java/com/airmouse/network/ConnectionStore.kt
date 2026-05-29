package com.airmouse.network

/**
 * Simple interface used by PreferencesManager to persist last-used connection info.
 */
interface ConnectionStore {
    fun getLastIp(): String
    fun setLastIp(ip: String)
    fun getLastPort(): Int
    fun setLastPort(port: Int)
}
