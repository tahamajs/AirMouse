// app/src/main/java/com/airmouse/data/datasource/remote/BluetoothDataSourceImpl.kt
package com.airmouse.data.datasource.remote

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IBluetoothDataSource {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var gatt: BluetoothGatt? = null

    private val deviceChannel = Channel<BluetoothDevice>(Channel.BUFFERED)
    private val rssiChannel = Channel<Int>(Channel.BUFFERED)
    private val stateChannel = Channel<Boolean>(Channel.BUFFERED)

    override fun startScanning() {
        if (bluetoothAdapter?.isEnabled == true) {
            bluetoothAdapter?.startLeScan { device, rssi, _ ->
                deviceChannel.trySend(device)
                rssiChannel.trySend(rssi)
            }
        }
    }

    override fun stopScanning() {
        bluetoothAdapter?.stopLeScan()
    }

    override fun connect(device: BluetoothDevice) {
        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                val connected = newState == BluetoothGatt.STATE_CONNECTED
                stateChannel.trySend(connected)
            }
        })
    }

    override fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        stateChannel.trySend(false)
    }

    override fun send(data: ByteArray) {
        val characteristic = BluetoothGattCharactericate(...)
        gatt?.writeCharacteristic(characteristic)
    }

    override fun observeDevices(): Flow<BluetoothDevice> = deviceChannel.receiveAsFlow()
    override fun observeRssi(): Flow<Int> = rssiChannel.receiveAsFlow()
    override fun connectionState(): Flow<Boolean> = stateChannel.receiveAsFlow()
}