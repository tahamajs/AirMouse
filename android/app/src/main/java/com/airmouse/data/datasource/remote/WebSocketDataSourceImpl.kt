// app/src/main/java/com/airmouse/data/datasource/remote/WebSocketDataSourceImpl.kt
package com.airmouse.data.datasource.remote

import com.airmouse.network.WebSocketManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketDataSourceImpl @Inject constructor(
    private val webSocketManager: WebSocketManager
) : IWebSocketDataSource {

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    private val statusChannel = Channel<Boolean>(Channel.BUFFERED)

    init {
        webSocketManager.onMessage = { message ->
            messageChannel.trySend(message)
        }
        webSocketManager.onConnected = {
            statusChannel.trySend(true)
        }
        webSocketManager.onDisconnected = {
            statusChannel.trySend(false)
        }
    }

    override fun connect(url: String) {
        webSocketManager.connect(url)
    }

    override fun disconnect() {
        webSocketManager.disconnect()
    }

    override fun send(message: String) {
        webSocketManager.send(message)
    }

    override fun observeMessages(): Flow<String> = messageChannel.receiveAsFlow()

    override fun connectionStatus(): Flow<Boolean> = statusChannel.receiveAsFlow()
}