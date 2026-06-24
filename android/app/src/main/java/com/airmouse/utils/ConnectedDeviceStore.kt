package com.airmouse.utils

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ConnectedDeviceStore {
    private const val SAVED_SERVERS_KEY = "saved_servers_v2"
    private const val CONNECTION_HISTORY_KEY = "connection_history"
    private const val MAX_HISTORY_ITEMS = 50

    fun rememberConnection(
        prefs: PreferencesManager,
        serverName: String,
        ip: String,
        port: Int,
        protocol: String,
        version: String = "3.0.0"
    ) {
        val normalizedName = serverName.ifBlank { "Air Mouse Server" }
        val normalizedProtocol = protocol.uppercase()
        val serverId = "$ip:$port"

        val savedServers = loadSavedServers(prefs).toMutableList()
        val existingIndex = savedServers.indexOfFirst { it.optString("ip") == ip && it.optInt("port") == port }
        val serverJson = JSONObject().apply {
            put("id", if (existingIndex >= 0) savedServers[existingIndex].optString("id", serverId) else UUID.randomUUID().toString())
            put("ip", ip)
            put("port", port)
            put("name", normalizedName)
            put("version", version)
            put("isFavorite", if (existingIndex >= 0) savedServers[existingIndex].optBoolean("isFavorite", false) else false)
            put("notes", if (existingIndex >= 0) savedServers[existingIndex].optString("notes", "") else "")
            put("deviceType", if (existingIndex >= 0) savedServers[existingIndex].optString("deviceType", "UNKNOWN") else "UNKNOWN")
            put("protocol", normalizedProtocol)
            put("lastSeen", System.currentTimeMillis())
        }
        if (existingIndex >= 0) {
            savedServers[existingIndex] = serverJson
        } else {
            savedServers.add(serverJson)
        }
        prefs.putString(
            SAVED_SERVERS_KEY,
            JSONArray().apply { savedServers.forEach { put(it) } }.toString()
        )

        val history = loadConnectionHistory(prefs).toMutableList()
        history.add(
            JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("serverId", serverId)
                put("serverName", normalizedName)
                put("ip", ip)
                put("port", port)
                put("timestamp", System.currentTimeMillis())
                put("status", "Connected")
                put("protocol", normalizedProtocol)
            }
        )
        while (history.size > MAX_HISTORY_ITEMS) {
            history.removeAt(0)
        }
        prefs.putString(
            CONNECTION_HISTORY_KEY,
            JSONArray().apply { history.forEach { put(it) } }.toString()
        )
    }

    private fun loadSavedServers(prefs: PreferencesManager): List<JSONObject> {
        val raw = prefs.getString(SAVED_SERVERS_KEY, "")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getJSONObject(i))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadConnectionHistory(prefs: PreferencesManager): List<JSONObject> {
        val raw = prefs.getString(CONNECTION_HISTORY_KEY, "")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getJSONObject(i))
                }
            }
        }.getOrDefault(emptyList())
    }
}
