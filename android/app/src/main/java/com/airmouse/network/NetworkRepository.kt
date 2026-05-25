package com.airmouse.data

import com.airmouse.network.DataSender
import com.airmouse.utils.PreferencesManager

class NetworkRepository(private val prefs: PreferencesManager) {

    private var dataSender: DataSender? = null

    suspend fun connect(ip: String) {
        dataSender = DataSender(ip, 8080, prefs).apply { start() }
    }

    fun sendMove(dx: Float, dy: Float) = dataSender?.sendMove(dx, dy)
    fun sendClick() = dataSender?.sendClick()
    fun sendDoubleClick() = dataSender?.sendDoubleClick()
    fun sendRightClick() = dataSender?.sendRightClick()
    fun sendScroll(delta: Int) = dataSender?.sendScroll(delta)

    fun disconnect() {
        dataSender?.stopSending()
        dataSender = null
    }
}