// app/src/main/java/com/airmouse/data/datasource/local/Converters.kt
package com.airmouse.data.datasource.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val type = object : TypeToken<FloatArray>() {}.type
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromListOfFloatArrays(value: List<FloatArray>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toListOfFloatArrays(value: String): List<FloatArray> {
        val type = object : TypeToken<List<FloatArray>>() {}.type
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val type = object : TypeToken<List<Long>>() {}.type
        return Gson().fromJson(value, type)
    }
}