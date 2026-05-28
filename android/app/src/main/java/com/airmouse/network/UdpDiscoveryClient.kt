package com.airmouse.network

import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UdpDiscoveryClient {
    fun discover(onResult: (String, Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val request = "AIRMOUSE_DISCOVER".toByteArray()
                val packet = DatagramPacket(request, request.size, InetAddress.getByName("255.255.255.255"), 8081)
                socket.send(packet)

                // Wait for response
                socket.soTimeout = 2000
                val buffer = ByteArray(1024)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)
                val json = String(response.data, 0, response.length)
                // Simple parse: assume {"ip":"...", "port":8080}
                // In a real app, use JSON parser
                // For now, extract manually
                val ip = json.substring(json.indexOf("\"ip\":\"") + 6, json.indexOf("\",", json.indexOf("\"ip\":\"")))
                val portStr = json.substring(json.indexOf("\"port\":") + 7, json.indexOf("}")).trim()
                val port = portStr.toIntOrNull() ?: 8080
                onResult(ip, port)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}