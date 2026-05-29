package com.airmouse.bluetooth

import android.content.Context
import android.content.Intent

object BtHidHelper {
    fun startService(context: Context) {
        val intent = Intent(context, BluetoothMouseService::class.java)
        context.startService(intent)
    }

    fun stopService(context: Context) {
        val intent = Intent(context, BluetoothMouseService::class.java)
        context.stopService(intent)
    }
}