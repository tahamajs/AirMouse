package com.airmouse.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class UdpDiscoveryClient(
    private val port: Int = 8081,
    private val timeoutMs: Int = 2_000
) {
    suspend fun discover(): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val results = linkedSetOf<DiscoveredServer>()
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.soTimeout = timeoutMs

            val request = "AIRMOUSE_DISCOVER".toByteArray()
            socket.send(
                DatagramPacket(
                    request,
                    request.size,
                    InetAddress.getByName("255.255.255.255"),
                    port
                )
            )

            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val payload = String(packet.data, 0, packet.length)
                    val response = JSONObject(payload)
                    if (response.optString("type") == "discovery_response") {
                        val host = response.optString("ip").ifBlank { packet.address.hostAddress.orEmpty() }
                        if (host.isNotBlank()) {
                            results.add(DiscoveredServer(host, response.optInt("port", port)))
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    break
                } catch (_: Exception) {
                    break
                }
            }
        }
        results.toList()
    }
}
