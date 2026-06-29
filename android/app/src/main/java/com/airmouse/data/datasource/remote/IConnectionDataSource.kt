package com.airmouse.data.datasource.remote

import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.DiscoveredServer
import kotlinx.coroutines.flow.Flow

interface IConnectionDataSource {

    // ---- Connection Management ----
    suspend fun connect(ip: String, port: Int, useSSL: Boolean = false): Boolean
    suspend fun disconnect()
    suspend fun reconnect(): Boolean
    suspend fun isConnected(): Boolean

    // ---- Sending Messages ----
    suspend fun sendMessage(message: String): Boolean
    suspend fun sendMessage(message: ByteArray): Boolean
    suspend fun sendMove(dx: Float, dy: Float): Boolean
    suspend fun sendClick(button: String): Boolean
    suspend fun sendDoubleClick(): Boolean
    suspend fun sendRightClick(): Boolean
    suspend fun sendScroll(delta: Int): Boolean
    suspend fun sendGesture(gesture: String, confidence: Float): Boolean
    suspend fun sendProximity(isNear: Boolean, distance: Float): Boolean
    suspend fun sendControl(command: String): Boolean
    suspend fun sendHello(name: String, version: String): Boolean
    suspend fun sendPing(): Boolean
    suspend fun sendPong(): Boolean

    // ---- Server Discovery ----
    suspend fun discoverServers(): List<DiscoveredServer>
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit)
    suspend fun stopDiscovery()

    // ---- Quality ----
    suspend fun getConnectionQuality(): ConnectionQuality
    fun observeConnectionQuality(): Flow<ConnectionQuality>

    // ---- Callback Listeners ----
    fun setOnMessageListener(listener: (String) -> Unit)
    fun setOnBinaryMessageListener(listener: (ByteArray) -> Unit)
    fun setOnDisconnectedListener(listener: () -> Unit)
    fun setOnConnectedListener(listener: () -> Unit)
}