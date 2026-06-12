package com.airmouse.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val name: String,
    val sensitivity: Float = 0.5f,
    val clickThreshold: Float = 10f
)
