package com.airmouse.data.datasource.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val key: String,
    val value: String,

    val updatedAt: Long = System.currentTimeMillis()
)