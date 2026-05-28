package com.airmouse.network

interface ConnectionStore {
    fun getLastIp(): String
    fun setLastIp(ip: String)
    fun getLastPort(): Int
    fun setLastPort(port: Int)
}