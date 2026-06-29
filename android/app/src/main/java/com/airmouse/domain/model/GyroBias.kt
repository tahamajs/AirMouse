package com.airmouse.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "gyro_bias")
@Parcelize
data class GyroBias(
    @PrimaryKey val id: Int = 1,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable