package com.airmouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class NetworkDiscoveryFragment : Fragment() {

    private data class DiscoveredServer(val host: String, val port: Int) {
        override fun toString(): String = "$host:$port"
    }

    private lateinit var preferences: PreferencesManager
    private lateinit var scanButton: Button
    private lateinit var statusText: TextView
    private lateinit var serverList: ListView
    private val servers = mutableListOf<DiscoveredServer>()
    private lateinit var adapter: ArrayAdapter<DiscoveredServer>

    companion object {
        private const val DISCOVERY_PORT = 8081
        private const val DISCOVERY_TIMEOUT_MS = 2500
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_network_discovery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())
        scanButton = view.findViewById(R.id.scan_network_btn)
        statusText = view.findViewById(R.id.network_status)
        serverList = view.findViewById(R.id.discovered_servers_list)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, servers)
        serverList.adapter = adapter
        serverList.setOnItemClickListener { _, _, position, _ ->
            val server = servers[position]
            preferences.setLastIp(server.host)
            preferences.setLastPort(server.port)
            statusText.text = getString(R.string.found_servers, server.toString())
            findNavController().navigate(R.id.homeFragment)
        }

        scanButton.setOnClickListener { startScan() }
    }

    private fun startScan() {
        scanButton.isEnabled = false
        statusText.text = getString(R.string.scanning_network)
        lifecycleScope.launch {
            servers.clear()
            val found = withContext(Dispatchers.IO) { scanLocalNetwork() }
            if (found.isNotEmpty()) {
                servers.addAll(found)
                statusText.text = getString(R.string.found_servers_count, found.size)
            } else {
                statusText.text = getString(R.string.no_servers_found)
            }
            adapter.notifyDataSetChanged()
            scanButton.isEnabled = true
        }
    }

    private fun scanLocalNetwork(): List<DiscoveredServer> {
        val found = linkedSetOf<DiscoveredServer>()
        val request = "AIRMOUSE_DISCOVER".toByteArray()
        val broadcastAddress = InetAddress.getByName("255.255.255.255")
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.soTimeout = DISCOVERY_TIMEOUT_MS
            socket.send(DatagramPacket(request, request.size, broadcastAddress, DISCOVERY_PORT))

            val responseBuffer = ByteArray(512)
            while (true) {
                try {
                    val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                    socket.receive(responsePacket)
                    val response = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
                    val json = JSONObject(response)
                    if (json.optString("type") == "discovery_response") {
                        val host = json.optString("ip", responsePacket.address.hostAddress ?: "")
                        val port = json.optInt("port", preferences.getLastPort())
                        if (host.isNotBlank()) {
                            found.add(DiscoveredServer(host, port))
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    break
                }
            }
        }
        return found.toList()
    }
}