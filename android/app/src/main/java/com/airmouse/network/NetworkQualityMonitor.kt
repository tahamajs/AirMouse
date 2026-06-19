// app/src/main/java/com/airmouse/network/NetworkQualityMonitor.kt
package com.airmouse.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkQualityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkType = MutableStateFlow(NetworkType.UNKNOWN)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    private val _signalStrength = MutableStateFlow(0)
    val signalStrength: StateFlow<Int> = _signalStrength.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    enum class NetworkType {
        WIFI,
        CELLULAR,
        ETHERNET,
        BLUETOOTH,
        UNKNOWN
    }

    fun updateNetworkStatus() {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)

        _isConnected.value = network != null

        if (caps != null) {
            _networkType.value = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkType.BLUETOOTH
                else -> NetworkType.UNKNOWN
            }
            // Estimate signal strength from network capabilities
            _signalStrength.value = estimateSignalStrength(caps)
        }
    }

    private fun estimateSignalStrength(caps: NetworkCapabilities): Int {
        // This would require platform-specific implementation
        return 70 // Default value
    }

    fun getNetworkQuality(): NetworkQuality {
        return when {
            !_isConnected.value -> NetworkQuality.NONE
            _networkType.value == NetworkType.WIFI -> NetworkQuality.EXCELLENT
            _networkType.value == NetworkType.ETHERNET -> NetworkQuality.EXCELLENT
            _signalStrength.value > 70 -> NetworkQuality.GOOD
            _signalStrength.value > 40 -> NetworkQuality.FAIR
            else -> NetworkQuality.POOR
        }
    }

    enum class NetworkQuality {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        NONE
    }
}