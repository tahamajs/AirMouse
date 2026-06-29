package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BluetoothDeviceInfo(
    val address: String = "",
    val name: String = "",
    val rssi: Int = 0,
    val txPower: Int = 0,
    val isConnected: Boolean = false,
    val isPaired: Boolean = false,
    val deviceType: String = "",
    val bondState: Int = 0
) : Parcelable

@Parcelize
data class BLEService(
    val uuid: String = "",
    val name: String = "",
    val characteristics: List<BLECharacteristic> = emptyList()
) : Parcelable

@Parcelize
data class BLECharacteristic(
    val uuid: String = "",
    val name: String = "",
    val properties: String = "",
    val value: ByteArray? = null,
    val isNotifiable: Boolean = false,
    val isReadable: Boolean = false,
    val isWritable: Boolean = false
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BLECharacteristic
        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (properties != other.properties) return false
        if (value != null) {
            if (other.value == null) return false
            if (!value.contentEquals(other.value)) return false
        } else if (other.value != null) return false
        if (isNotifiable != other.isNotifiable) return false
        if (isReadable != other.isReadable) return false
        if (isWritable != other.isWritable) return false
        return true
    }
    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + (value?.contentHashCode() ?: 0)
        result = 31 * result + isNotifiable.hashCode()
        result = 31 * result + isReadable.hashCode()
        result = 31 * result + isWritable.hashCode()
        return result
    }
}