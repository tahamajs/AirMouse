package com.airmouse.di

import android.content.Context
import android.bluetooth.BluetoothAdapter
import com.airmouse.network.ConnectionManager
import com.airmouse.network.BluetoothHidMouseHelper
import com.airmouse.data.repository.StatisticsRepositoryImpl
import com.airmouse.utils.PreferencesManager

class AppContainer(private val context: Context) {

    private val preferencesManager by lazy { PreferencesManager(context) }
    private val bluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val bluetoothHidMouseHelper by lazy { BluetoothHidMouseHelper(context, bluetoothAdapter) }
    private val statisticsRepository by lazy { StatisticsRepositoryImpl(preferencesManager) }

    private val usbDataSource by lazy { com.airmouse.data.datasource.remote.UsbDataSourceImpl(context) }
    
    val connectionManager by lazy { 
        ConnectionManager(
            context, 
            preferencesManager,
            bluetoothAdapter,
            bluetoothHidMouseHelper,
            statisticsRepository,
            usbDataSource
        ) 
    }

    fun cleanup() {
        connectionManager.cleanup()
    }
}