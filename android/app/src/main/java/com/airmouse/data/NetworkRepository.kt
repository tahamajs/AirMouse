package com.airmouse.data

import com.airmouse.data.PreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.PrintWriter
import java.net.Socket
import org.json.JSONObject

class NetworkRepository(private val prefs: PreferencesDataStore) {

    private var socket: Socket? = null
    private var out: PrintWriter? = null

    suspend fun connect(ip: String) = withContext(Dispatchers.IO) {
        try {
            socket = Socket(ip, 8080)
            out = PrintWriter(DataOutputStream(socket!!.getOutputStream()), true)
            prefs.setLastIp(ip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMove(dx: Float, dy: Float) {
        val json = JSONObject().apply {
            put("type", "move")
            put("dx", dx)
            put("dy", dy)
        }
        out?.println(json.toString())
    }

    fun sendClick() {
        val json = JSONObject().apply {
            put("type", "click")
            put("id", System.currentTimeMillis())
        }
        out?.println(json.toString())
    }

    fun sendDoubleClick() {
        val json = JSONObject().apply {
            put("type", "doubleclick")
            put("id", System.currentTimeMillis())
        }
        out?.println(json.toString())
    }

    fun sendRightClick() {
        val json = JSONObject().apply {
            put("type", "rightclick")
            put("id", System.currentTimeMillis())
        }
        out?.println(json.toString())
    }

    fun sendScroll(delta: Int) {
        val json = JSONObject().apply {
            put("type", "scroll")
            put("delta", delta)
            put("id", System.currentTimeMillis())
        }
        out?.println(json.toString())
    }
}