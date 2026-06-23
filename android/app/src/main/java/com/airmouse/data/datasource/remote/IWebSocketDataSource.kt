
package com.airmouse.data.datasource.remote

import kotlinx.coroutines.flow.Flow

interface IWebSocketDataSource {
    fun connect(url: String)
    fun disconnect()
    fun send(message: String)
    fun observeMessages(): Flow<String>
    fun connectionStatus(): Flow<Boolean>
}