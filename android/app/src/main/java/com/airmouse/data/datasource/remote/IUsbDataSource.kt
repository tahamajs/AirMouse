
package com.airmouse.data.datasource.remote

import kotlinx.coroutines.flow.Flow

interface IUsbDataSource {
    fun connect()
    fun disconnect()
    fun send(data: ByteArray)
    fun observeMessages(): Flow<ByteArray>
    fun connectionStatus(): Flow<Boolean>
}