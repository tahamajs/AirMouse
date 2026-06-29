package com.airmouse.data.datasource.remote

import com.airmouse.network.ConnectionManager
import com.airmouse.network.ConnectionManager.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketDataSourceImpl @Inject constructor(
    private val connectionManager: ConnectionManager
) : IWebSocketDataSource {

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    private val statusChannel = Channel<Boolean>(Channel.BUFFERED)
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        connectionManager.onMessage = { message ->
            messageChannel.trySend(message)
        }
        connectionManager.onStatusChanged = { status ->
            statusChannel.trySend(status == ConnectionStatus.CONNECTED)
        }
    }

    override fun connect(url: String) {
        scope.launch {
            try {
                val cleanUrl = url.replace("ws://", "").replace("wss://", "")
                val parts = cleanUrl.split(":")
                val ip = parts[0]
                val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 8080 else 8080
                connectionManager.connect(ip, port)
            } catch (e: Exception) {
                statusChannel.trySend(false)
            }
        }
    }

    override fun disconnect() {
        scope.launch {
            connectionManager.disconnect()
        }
    }

    override fun send(message: String) {
        connectionManager.send(message)
    }

    override fun observeMessages(): Flow<String> = messageChannel.receiveAsFlow()
    override fun connectionStatus(): Flow<Boolean> = statusChannel.receiveAsFlow()
}