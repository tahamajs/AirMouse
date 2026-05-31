// app/src/main/java/com/airmouse/data/datasource/remote/IBluetoothDataSource.kt
package com.airmouse.data.datasource.remote

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.Flow

interface IBluetoothDataSource {
    fun startScanning()
    fun stopScanning()
    fun connect(device: BluetoothDevice)
    fun disconnect()
    fun send(data: ByteArray)
    fun observeDevices(): Flow<BluetoothDevice>
    fun observeRssi(): Flow<Int>
    fun connectionState(): Flow<Boolean>
}