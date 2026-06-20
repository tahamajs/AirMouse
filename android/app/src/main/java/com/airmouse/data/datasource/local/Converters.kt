package com.airmouse.data.datasource.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.airmouse.domain.model.GestureType
import java.util.Date

// Import the moved data classes
import com.airmouse.data.datasource.local.CalibrationData
import com.airmouse.data.datasource.local.SensorData
import com.airmouse.data.datasource.local.GestureData
import com.airmouse.data.datasource.local.Quadruple

class Converters {

    private val gson = Gson()

    // ==================== Date Converters ====================

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // ==================== String List Converters ====================

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringListToString(list: List<String>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    // ==================== Float Array Converters ====================

    @TypeConverter
    fun fromFloatArray(value: String?): FloatArray? {
        if (value == null) return null
        val listType = object : TypeToken<List<Float>>() {}.type
        val list: List<Float> = gson.fromJson(value, listType)
        return list.toFloatArray()
    }

    @TypeConverter
    fun fromFloatArrayToString(array: FloatArray?): String? {
        if (array == null) return null
        return gson.toJson(array.toList())
    }

    // ==================== Float Array List Converters ====================

    @TypeConverter
    fun fromFloatArrayList(value: String?): List<FloatArray>? {
        if (value == null) return null
        val listType = object : TypeToken<List<List<Float>>>() {}.type
        val list: List<List<Float>> = gson.fromJson(value, listType)
        return list.map { it.toFloatArray() }
    }

    @TypeConverter
    fun fromFloatArrayListToString(list: List<FloatArray>?): String? {
        if (list == null) return null
        val outerList = list.map { it.toList() }
        return gson.toJson(outerList)
    }

    // ==================== Int List Converters ====================

    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromIntListToString(list: List<Int>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    // ==================== Long List Converters ====================

    @TypeConverter
    fun fromLongList(value: String?): List<Long>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromLongListToString(list: List<Long>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    // ==================== Double List Converters ====================

    @TypeConverter
    fun fromDoubleList(value: String?): List<Double>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Double>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromDoubleListToString(list: List<Double>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    // ==================== Map Converters ====================

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromStringMapToString(map: Map<String, String>?): String? {
        if (map == null) return null
        return gson.toJson(map)
    }

    @TypeConverter
    fun fromAnyMap(value: String?): Map<String, Any>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromAnyMapToString(map: Map<String, Any>?): String? {
        if (map == null) return null
        return gson.toJson(map)
    }

    // ==================== Gesture Type Converters ====================

    @TypeConverter
    fun fromGestureType(value: String?): GestureType? {
        return if (value == null) null else try {
            GestureType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            GestureType.NONE
        }
    }

    @TypeConverter
    fun fromGestureTypeToString(type: GestureType?): String? {
        return type?.name
    }

    // ==================== Boolean Converters ====================

    @TypeConverter
    fun fromBoolean(value: Int?): Boolean? {
        return value?.let { it == 1 }
    }

    @TypeConverter
    fun fromBooleanToInt(value: Boolean?): Int? {
        return value?.let { if (it) 1 else 0 }
    }

    // ==================== Float Converters ====================

    @TypeConverter
    fun fromFloat(value: String?): Float? {
        return value?.toFloatOrNull()
    }

    @TypeConverter
    fun fromFloatToString(value: Float?): String? {
        return value?.toString()
    }

    // ==================== Double Converters ====================

    @TypeConverter
    fun fromDouble(value: String?): Double? {
        return value?.toDoubleOrNull()
    }

    @TypeConverter
    fun fromDoubleToString(value: Double?): String? {
        return value?.toString()
    }

    // ==================== Pair Converters ====================

    @TypeConverter
    fun fromPair(value: String?): Pair<Float, Float>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Float>>() {}.type
        val list: List<Float> = gson.fromJson(value, listType)
        return if (list.size >= 2) Pair(list[0], list[1]) else null
    }

    @TypeConverter
    fun fromPairToString(pair: Pair<Float, Float>?): String? {
        if (pair == null) return null
        return gson.toJson(listOf(pair.first, pair.second))
    }

    // ==================== Triple Converters ====================

    @TypeConverter
    fun fromTriple(value: String?): Triple<Float, Float, Float>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Float>>() {}.type
        val list: List<Float> = gson.fromJson(value, listType)
        return if (list.size >= 3) Triple(list[0], list[1], list[2]) else null
    }

    @TypeConverter
    fun fromTripleToString(triple: Triple<Float, Float, Float>?): String? {
        if (triple == null) return null
        return gson.toJson(listOf(triple.first, triple.second, triple.third))
    }

    // ==================== Quadruple Converters ====================

    @TypeConverter
    fun fromQuadruple(value: String?): Quadruple<Float, Float, Float, Float>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Float>>() {}.type
        val list: List<Float> = gson.fromJson(value, listType)
        return if (list.size >= 4) Quadruple(list[0], list[1], list[2], list[3]) else null
    }

    @TypeConverter
    fun fromQuadrupleToString(quad: Quadruple<Float, Float, Float, Float>?): String? {
        if (quad == null) return null
        return gson.toJson(listOf(quad.first, quad.second, quad.third, quad.fourth))
    }

    // ==================== Calibration Data Converters ====================

    @TypeConverter
    fun fromCalibrationData(value: String?): CalibrationData? {
        if (value == null) return null
        return gson.fromJson(value, CalibrationData::class.java)
    }

    @TypeConverter
    fun fromCalibrationDataToString(data: CalibrationData?): String? {
        if (data == null) return null
        return gson.toJson(data)
    }

    // ==================== Sensor Data Converters ====================

    @TypeConverter
    fun fromSensorData(value: String?): SensorData? {
        if (value == null) return null
        return gson.fromJson(value, SensorData::class.java)
    }

    @TypeConverter
    fun fromSensorDataToString(data: SensorData?): String? {
        if (data == null) return null
        return gson.toJson(data)
    }

    // ==================== Gesture Data Converters ====================

    @TypeConverter
    fun fromGestureData(value: String?): GestureData? {
        if (value == null) return null
        return gson.fromJson(value, GestureData::class.java)
    }

    @TypeConverter
    fun fromGestureDataToString(data: GestureData?): String? {
        if (data == null) return null
        return gson.toJson(data)
    }
}
