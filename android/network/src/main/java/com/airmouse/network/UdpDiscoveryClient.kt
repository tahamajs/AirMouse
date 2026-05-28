package com.airmouse.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpDiscoveryClient(private val port: Int = 8081) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun discover(timeoutMs: Int = 1500): DiscoveryResponse? = withContext(Dispatchers.IO) {
        val socket = DatagramSocket()
        socket.broadcast = true
        socket.soTimeout = timeoutMs
        try {
            val sendData = "AIRMOUSE_DISCOVER".toByteArray()
            val packet = DatagramPacket(sendData, sendData.size, InetAddress.getByName("255.255.255.255"), port)
            socket.send(packet)

            val buf = ByteArray(2048)
            val recv = DatagramPacket(buf, buf.size)
            socket.receive(recv)
            val resp = String(recv.data, 0, recv.length)
            try {
                val parsed = json.decodeFromString<DiscoveryResponse>(resp)
                return@withContext parsed
            } catch (e: Exception) {
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        } finally {
            try { socket.close() } catch(_:Exception){}
        }
    }
}
