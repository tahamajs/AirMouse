package com.airmouse.data.datasource.remote

import android.bluetooth.BluetoothDevice
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IBluetoothDataSource {

    private val deviceChannel = Channel<BluetoothDevice>(Channel.BUFFERED)
    private val rssiChannel = Channel<Int>(Channel.BUFFERED)
    private val stateChannel = Channel<Boolean>(Channel.BUFFERED)

    override fun startScanning() {
        // Bluetooth discovery is intentionally stubbed in the offline build.
        // The app keeps the API surface so the UI and docs remain complete.
        stateChannel.trySend(false)
    }

    override fun stopScanning() {
        stateChannel.trySend(false)
    }

    override fun connect(device: BluetoothDevice) {
        deviceChannel.trySend(device)
        stateChannel.trySend(true)
    }

    override fun disconnect() {
        stateChannel.trySend(false)
    }

    override fun send(data: ByteArray) {
        // No-op in the offline build.
    }

    override fun observeDevices(): Flow<BluetoothDevice> = deviceChannel.receiveAsFlow()
    override fun observeRssi(): Flow<Int> = rssiChannel.receiveAsFlow()
    override fun connectionState(): Flow<Boolean> = stateChannel.receiveAsFlow()
}
